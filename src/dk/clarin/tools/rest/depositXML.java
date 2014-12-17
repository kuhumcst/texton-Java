package dk.clarin.tools.rest;

import dk.cst.*;
import dk.clarin.tools.ToolsProperties;
import dk.clarin.tools.workflow;
import dk.clarin.tools.userhandle;
import java.io.*;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;

/**
 * Create complete and valid metadata for deposition. 
 * All information is collected from the tables tooladm.table and toolprop.table in jboss/server/default/data/tools
 * Activated from a dynamically created form (see deposit function in toolsProg.bra), and not explicitly from a jsp-page.
 * Argument: the unique tool-id of the tool to deposit.
 * Returns: 200 OK, 404 if the tool-id is unknown.
 */      
@SuppressWarnings("serial")
public class depositXML extends HttpServlet 
    {
    //private static final String TMP_DIR_PATH = "/tmp";
    // Static logger object.  
    private static final Logger logger = LoggerFactory.getLogger(depositXML.class);

    private bracmat BracMat;
    private File destinationDir;

    /// The eSciDoc userHandle
    private String userHandle = null;
    /// The eSciDoc id of the user
    private String userId;
    /// The users email
    private String userEmail;


    public void init(ServletConfig config) throws ServletException 
        {
        InputStream fis = config.getServletContext().getResourceAsStream("/WEB-INF/classes/properties.xml");
        ToolsProperties.readProperties(fis);		
        BracMat = new bracmat(ToolsProperties.bootBracmat);
        destinationDir = new File(ToolsProperties.documentRoot /*+ ToolsProperties.stagingArea*/);
        if(!destinationDir.isDirectory())
            {
            try
                {
                destinationDir.mkdir();
                }
            catch(Exception e)
                {
                throw new ServletException("Trying to create \"" + ToolsProperties.documentRoot /*+ ToolsProperties.stagingArea */+ "\" as directory for temporary storing intermediate and final results, but this is not a valid directory. Error:" + e.getMessage());
                }
            }
        if(!destinationDir.isDirectory()) 
            {
            throw new ServletException("Trying to set \"" + ToolsProperties.documentRoot /*+ ToolsProperties.stagingArea */+ "\" as directory for temporary storing intermediate and final results, but this is not a valid directory.");
            }

        super.init(config);
        }





    private String addFile(ZipOutputStream zout, File file) 
        {
        logger.debug("Zipping file " + file.getName());

        /*
        * add it to the zip file
        */
        try 
            {
            // create byte buffer
            byte[] buffer = new byte[1024];

            // create object of FileInputStream
            FileInputStream fin = new FileInputStream(file);

            // create the zip-entry. We replace ":" with "-" since WinZip claims that filenames
            // that includes those are invalid...
            String entryPath = new String(file.getName()).replaceAll(":", "-");
            logger.info("entryPath:" + entryPath);
            zout.putNextEntry(new ZipEntry(entryPath));

            /*
            * After creating entry in the zip file, actually
            * write the file.
            */
            int length;

            while((length = fin.read(buffer)) > 0)
                {
                zout.write(buffer, 0, length);
                }
            /*
            * After writing the file to ZipOutputStream, use
            * void closeEntry() method of ZipOutputStream class to
            * close the current entry and position the stream to
            * write the next entry.
            */

            zout.closeEntry();

            // close the InputStream
            fin.close();
            } 
        catch(IOException ioe)
            {
            logger.error("An error occurred while creating the zip archive");
            return "An error occurred while creating the zip archive";
            }
        return null;
        }


    /**
    * Do the work: download the items and store in matching folder
    */ 
    public String makeZip(File metadata, File data) 
        {
        String zipname = metadata.getName();
        logger.info("zipname:" + zipname);
        if(zipname.endsWith(".xml"))
            zipname = zipname.substring(0,zipname.length() - 3);
        logger.info("zipname:" + zipname);
        zipname = zipname + "zip";
        logger.info("zipname:" + zipname);
        String fullPath = ToolsProperties.documentRoot /*+ ToolsProperties.stagingArea */+ zipname;
        logger.debug("Zipping to file " + fullPath);

        try 
            {
            // create object of FileOutputStream
            FileOutputStream fout = new FileOutputStream(fullPath);

            // create object of ZipOutputStream from FileOutputStream
            ZipOutputStream zout = new ZipOutputStream(fout);

            String ret = addFile(zout, metadata);
            if (ret != null) 
                return null;
            ret = addFile(zout, data);
            if (ret != null) 
                return null;

            zout.close();
            } 
        catch(IOException ioe)
            {
            return null;
            }

        logger.debug("Wrote zipfile: " + fullPath);
        return zipname;
        }

    /**
    *
    * Pipes everything from the reader to the writer via a buffer
    */
    private static void pipe(Reader reader, Writer writer) throws IOException
        {
        char[] buf = new char[1024];
        int read = 0;
        while ((read = reader.read(buf)) >= 0)
            {
            logger.info("read=" + read);
            writer.write(buf, 0, read);
            }
        writer.flush();
        }

    public static int sendPostRequest(String endpoint, String requestString)
        {        
        logger.info("sendPostRequest(" + endpoint + ", " + requestString + ")");
        int code = 0;
        //String message = "";
        if(  endpoint.startsWith("http://") 
            || endpoint.startsWith("https://")
            )
            {
            // Send a GET or POST request to the servlet
            try
                {
                // Construct data
                String requestResult = "";
                String urlStr = endpoint;
                logger.info("HTTP POST");
                logger.info("endpoint=" + endpoint);
                logger.info("requestString=" + requestString);
                StringReader input = new StringReader(requestString);
                StringWriter output = new StringWriter();
                URL endp = new URL(endpoint);
                //code = postData(input, endp, output);

                HttpURLConnection urlc = null;
                try
                    {
                    urlc = (HttpURLConnection) endp.openConnection();
                    try
                        {
                        urlc.setRequestMethod("POST");
                        }
                    catch (ProtocolException e)
                        {
                        throw new Exception("Shouldn't happen: HttpURLConnection doesn't support POST??", e);
                        }
                    urlc.setDoOutput(true);
                    urlc.setDoInput(true);
                    urlc.setUseCaches(false);
                    urlc.setAllowUserInteraction(false);
                    urlc.setRequestProperty("Content-type", "application/x-www-form-urlencoded; charset=" + "UTF-8");

                    OutputStream out = urlc.getOutputStream();

                    try
                        {
                        Writer writer = new OutputStreamWriter(out, "UTF-8");
                        pipe(input, writer);
                        writer.close();
                        } 
                    catch (IOException e)
                        {
                        throw new Exception("IOException while posting data", e);
                        } 
                    finally
                        {
                        if (out != null)
                            out.close();
                        }

                    InputStream in = urlc.getInputStream();
                    try
                        {
                        Reader reader = new InputStreamReader(in);
                        pipe(reader, output);
                        reader.close();
                        } 
                    catch (IOException e)
                        {
                        throw new Exception("IOException while reading response", e);
                        } 
                    finally
                        {
                        if (in != null)
                            in.close();
                        }

                    } 
                catch (IOException e)
                    {
                    throw new Exception("Connection error (is server running at " + endp + " ?): " + e);
                    } 
                finally
                    {
                    if (urlc != null)
                        {
                        code = urlc.getResponseCode();
                        logger.info("urlc.getResponseCode() == {}",code);
                        //message = urlc.getResponseMessage();
                        urlc.disconnect();
                        }
                    }
                requestResult = output.toString();
                logger.info("postData returns " + requestResult);
                logger.info("deposit receives status code [" + code + "] from tool.");
                if(code == 200)
                    {
                    }
                else if(code == 0)
                    {
                    logger.warn("Deposit tool cannot open connection to URL " + urlStr);
                    }
                else
                    {
                    logger.warn("Got status code [" + code + "]. Deposit tool  is aborted.");
                    }
                } 
            catch (Exception e)
                {
                //jobs = 0; // No more jobs to process now, probably the tool is not reachable
                logger.warn("Deposit tool aborted. Reason:" + e.getMessage());
                }
            }
        else
            {
            //jobs = 0; // No more jobs to process now, probably the tool is not integrated at all
            logger.warn("Deposit tool aborted. Endpoint must start with 'http://' or 'https://'. (" + endpoint + ")");
            }
        return code;
        }


    public void doGet(HttpServletRequest request,HttpServletResponse response)
        throws ServletException, IOException 
        {
        if(userHandle == null)
            userHandle = userhandle.getUserHandle(request,null);
        if(userId == null && userHandle != null)
            userId = userhandle.getUserId(request,null,userHandle);
        if(userEmail == null && userId != null)
            userEmail = userhandle.getEmailAddress(request,null,userHandle,userId);
        response.setContentType("text/xml");
        //response.setContentType("text/html; charset=UTF-8");
        response.setStatus(200);
        PrintWriter out = response.getWriter();
        if(BracMat.loaded())
            {
            @SuppressWarnings("unchecked")
            Enumeration<String> parmNames = (Enumeration<String>)request.getParameterNames();

            String arg = "";
            String name = "";
            String licence = "public";

            for (Enumeration<String> e = parmNames ; e.hasMoreElements() ;) 
                {
                String parmName = e.nextElement();
                arg = arg + " (\"" + workflow.escape(parmName) + "\".";
                String vals[] = request.getParameterValues(parmName);
                for(int j = 0;j < vals.length;++j)
                    {
                    arg += " \"" + workflow.escape(vals[j]) + "\"";
                    if(parmName.equals("name"))
                        name = vals[j];
                    if(parmName.equals("licence"))
                        licence = vals[j];
                    }
                arg += ")";
                }

            String result = BracMat.Eval("depositXML$(" + arg + ")");
            if(result == null || result.equals(""))
                {
                response.setStatus(404);
                /**
                 * getStatusCode$
                 *
                 * Given a HTTP status code and an informatory text, return an HTML-file
                 * with a heading containing the status code and the official short description
                 * of the status code, a paragraph containing the informatory text and a 
                 * paragraph displaying a longer text explaining the code (From wikipedia).
                 * 
                 * This function could just as well have been written in Java.
                 */
                String messagetext = BracMat.Eval("getStatusCode$(\"404\".\"Deposition of tool failed\")");
                out.println(messagetext);
                return;
                }
            if(!name.equals("") && !result.equals(""))
                {
                String LocalFileName = name + ".xml";
                try
                    {
                    File file = new File(destinationDir,LocalFileName);
                    FileUtils.writeStringToFile(file, result);
                    }
                catch (IOException e)
                    {
                    logger.error("Cannot write file. Reason:" + e.getMessage());
                    }
                String md5 = workflow.MD5(destinationDir + "/" + LocalFileName);
                logger.info("md5(" + destinationDir + "/" + LocalFileName + ")=" + md5);
                String endpoint = ToolsProperties.baseUrlTools + "/deposit/deposit-controller-servlet";
                if(userHandle != null)
                    {
                    out.println(result);
                    logger.info("The eSciDoc userHandle:" + userHandle + " The eSciDoc id of the user:" + userHandle + " The users email:" + userHandle);

                    String requestString = "checksum1=" + md5 + "&mimetype1=uncompressed&url1=" + ToolsProperties.baseUrlTools+ToolsProperties.stagingArea+LocalFileName+"&access="+licence+"&workflow=10&email=" + userEmail + "&localAccess1=&handle="+userHandle+"&name=Dem";
                    logger.info("requestString:" + requestString);
                    sendPostRequest(endpoint, requestString);
                    }
                else
                    {
                    response.setStatus(401);
                    response.setContentType("text/html; charset=UTF-8");
                    StringBuilder html = new StringBuilder();
                    html.append("<html>");
                    html.append("<head>");
                    html.append("<title>Deponering af Tools-metadata</title>");
                    html.append("</head>");
                    html.append("<body>");
                    html.append("<h1>Login krævet</h1>");
                    html.append("<p>Du skal være logget ind for at kunne deponere Tools-metadata i repositoriet. <a href=\"" + 
                        ToolsProperties.baseUrlTools + "/aa/login?target=" + ToolsProperties.baseUrlTools + 
                        "/clarindk/login?target=" + "/tools/deposit" + "\">Klik her for at logge ind</a>.</p>");
                        //"/tools/deposit" + "\">Klik her for at logge ind</a>.</p>");
                    html.append("</body>");
                    html.append("</html>");

                    out.println(html.toString());
                    return;
                    }
                }

            }
        else
            {
            throw new ServletException("Bracmat is not loaded. Reason:" + BracMat.reason());
            }
        }

    public void doPost(HttpServletRequest request,HttpServletResponse response)  throws ServletException, IOException 
        {
        List<FileItem> items = userhandle.getParmList(request);
        if(userHandle == null)
            userHandle = userhandle.getUserHandle(request,items);
        if(userId == null && userHandle != null)
            userId = userhandle.getUserId(request,items,userHandle);
        if(userEmail == null && userId != null)
            userEmail = userhandle.getEmailAddress(request,items,userHandle,userId);
        logger.info("doPost");
        response.setContentType("text/xml");
        //response.setContentType("text/html; charset=UTF-8");
        response.setStatus(200);
        PrintWriter out = response.getWriter();
        if(BracMat.loaded())
            {
            logger.info("BracMat.loaded");
/*
            DiskFileItemFactory  fileItemFactory = new DiskFileItemFactory ();
            / *
            *Set the size threshold, above which content will be stored on disk.
            * /
            fileItemFactory.setSizeThreshold(1*1024*1024); //1 MB
            / *
            * Set the temporary directory to store the uploaded files of size above threshold.
            * /
            logger.info("making tmpDir in " + ToolsProperties.tempdir);
            File tmpDir = new File(ToolsProperties.tempdir);
            if(!tmpDir.isDirectory()) 
                {
                logger.info("!tmpDir.isDirectory()");
                throw new ServletException("Trying to set \"" + ToolsProperties.tempdir + "\" as temporary directory, but this is not a valid directory.");
                }
            fileItemFactory.setRepository(tmpDir);

            ServletFileUpload uploadHandler = new ServletFileUpload(fileItemFactory);
*/
            String arg = "";
            String name = null;
            String data = null;
            String licence = "public";
            logger.info("Entering loop");

            try 
                {
                /*
                * Parse the request
                */
                logger.info("In try");

                @SuppressWarnings("unchecked")
                Enumeration<String> parmNames = (Enumeration<String>)request.getParameterNames();

                for (Enumeration<String> e = parmNames ; e.hasMoreElements() ;) 
                    {
                    // Well, you don't get here AT ALL if enctype='multipart/form-data'
                    String parmName = e.nextElement();
                    logger.info("parmName:"+parmName);
                    String vals[] = request.getParameterValues(parmName);
                    for(int j = 0;j < vals.length;++j)
                        {
                        logger.info("val:"+vals[j]);
                        }
                    }

                logger.debug("Now uploadHandler.parseRequest");
  //              List items = uploadHandler.parseRequest(request);
                logger.info("items:"+items);
                Iterator<FileItem> itr = items.iterator();
                logger.info("itr:"+itr);
                while(itr.hasNext()) 
                    {
                    logger.info("in loop");
                    FileItem item = (FileItem) itr.next();
                    /*
                    * Handle Form Fields.
                    */
                    if(item.isFormField()) 
                        {
                        logger.info("Field Name = "+item.getFieldName()+", String = "+item.getString());
                        if(userHandle == null && item.getFieldName().equals("handle"))
                            {
                            userHandle = item.getString();
                            if(userId == null && userHandle != null)
                                userId = userhandle.getUserId(request,items,userHandle);
                            if(userEmail == null && userId != null)
                                userEmail = userhandle.getEmailAddress(request,items,userHandle,userId);
                            }
                        arg = arg + " (\"" + workflow.escape(item.getFieldName()) + "\".";
                        arg += " \"" + workflow.escape(item.getString()) + "\"";
                        if(item.getFieldName().equals("name"))
                            name = item.getString();
                        else if(item.getFieldName().equals("licence"))
                            licence = item.getString();
                        
                        arg += ")";

                        }
                    else if(item.getName() != "")
                        {
                        /*
                        * Write file to the ultimate location.
                        */
                        logger.info("File = "+item.getName());
                        data = item.getName();
                        File file = new File(destinationDir,item.getName());
                        item.write(file);
                        logger.info("FieldName = "+item.getFieldName());
                        logger.info("Name = "+item.getName());
                        logger.info("ContentType = "+item.getContentType());
                        logger.info("Size = "+item.getSize());
                        logger.info("DestinationDir = "+ToolsProperties.documentRoot /*+ ToolsProperties.stagingArea*/);
                        }
                    }
                }
            catch(FileUploadException ex) 
                {
                logger.debug("uploadHandler.parseRequest FileUploadException. Reason:" + ex.getMessage());
                log("Error encountered while parsing the request",ex);
                }
            catch(Exception ex) 
                {
                logger.debug("uploadHandler.parseRequest Exception");
                log("Error encountered while uploading file",ex);
                out.close();
                }


            String result = BracMat.Eval("depositXML$(" + arg + ")");
            if(result == null || result.equals(""))
                {
                response.setStatus(404);
                String messagetext = BracMat.Eval("getStatusCode$(\"404\".\"Deposition of tool failed\")");
                logger.debug(messagetext);
                return;
                }
            if(name != null && !name.equals("") && !result.equals(""))
                {
                logger.info("name="+name);
                String LocalFileName = name + ".xml";
                logger.info("LocalFileName="+LocalFileName);
                try
                    {
                    File file = new File(destinationDir,LocalFileName);
                    FileUtils.writeStringToFile(file, result);
                    }
                catch (IOException e)
                    {
                    logger.error("Cannot write file. Reason:" + e.getMessage());
                    }

                if(data == null || data.equals(""))
                    {
                    logger.info("No file uploaded");
                    String md5 = workflow.MD5(destinationDir + "/" + LocalFileName);
                    logger.info("md5(" + destinationDir + "/" + LocalFileName + ")=" + md5);
                    String endpoint = ToolsProperties.baseUrlTools + "/deposit/deposit-controller-servlet";
                    logger.info("endpoint:"+endpoint);

                    if(userHandle != null)
                        {
                        out.println(result);
                        logger.info("The eSciDoc userHandle:" + userHandle + " The eSciDoc id of the user:" + userId + " The users email:" + userEmail);
                        String requestString = "checksum1=" + md5 + "&mimetype1=uncompressed&url1=" + ToolsProperties.baseUrlTools+ToolsProperties.stagingArea+LocalFileName+"&access="+licence+"&workflow=10&email=" + userEmail + "&localAccess1=&handle="+userHandle+"&name=Dem";
                        logger.info("requestString:" + requestString);
                        sendPostRequest(endpoint, requestString);
                        logger.info("No file uploaded, PostRequest Sent");
                        }
                    else
                        {
                        response.setStatus(401);
                        response.setContentType("text/html; charset=UTF-8");
                        StringBuilder html = new StringBuilder();
                        html.append("<html>");
                        html.append("<head>");
                        html.append("<title>Deponering af Tools-metadata</title>");
                        html.append("</head>");
                        html.append("<body>");
                        html.append("<h1>Login krævet</h1>");
                        html.append("<p>Du skal være logget ind for at kunne deponere Tools-metadata i repositoriet. <a href=\"" + 
                            ToolsProperties.baseUrlTools + "/aa/login?target=" + ToolsProperties.baseUrlTools + 
                            "/clarindk/login?target=" + "/tools/deposit" + "\">Klik her for at logge ind</a>.</p>");
                            //"/tools/deposit" + "\">Klik her for at logge ind</a>.</p>");
                        html.append("</body>");
                        html.append("</html>");

                        out.println(html.toString());
                        return;
                        }
                    }
                else
                    {
                    logger.info("File was uploaded");
                    File datafile = new File(destinationDir,data);
                    logger.info("datafile:"+datafile.getName());
                    File metadatafile = new File(destinationDir,LocalFileName);
                    logger.info("metadatafile:"+metadatafile.getName());
                    String zipfile = makeZip(metadatafile, datafile);
                    logger.info("zipfile:"+zipfile);
                    String md5 = workflow.MD5(destinationDir + "/" + zipfile);
                    logger.info("md5(" + destinationDir + "/" + zipfile + ")=" + md5);
                    String endpoint = ToolsProperties.baseUrlTools + "/deposit/deposit-controller-servlet";
                    logger.info("endpoint:"+endpoint);

                    if(userHandle != null)
                        {
                        out.println(result);
                        logger.info("The eSciDoc userHandle:" + userHandle + " The eSciDoc id of the user:" + userHandle + " The users email:" + userHandle);
                        String requestString = "checksum1=" + md5 + "&mimetype1=zip&url1=" + ToolsProperties.baseUrlTools+ToolsProperties.stagingArea+zipfile+"&access="+licence+"&workflow=10&email=" + userEmail + "&localAccess1=&handle="+userHandle+"&name=Dem";
                        logger.info("requestString:" + requestString);
                        sendPostRequest(endpoint, requestString);
                        logger.info("File uploaded, PostRequest Sent");
                        }
                    else
                        {
                        response.setStatus(401);
                        response.setContentType("text/html; charset=UTF-8");
                        StringBuilder html = new StringBuilder();
                        html.append("<html>");
                        html.append("<head>");
                        html.append("<title>Deponering af Tools-metadata</title>");
                        html.append("</head>");
                        html.append("<body>");
                        html.append("<h1>Login krævet</h1>");
                        html.append("<p>Du skal være logget ind for at kunne deponere Tools-metadata i repositoriet. <a href=\"" + 
                            ToolsProperties.baseUrlTools + "/aa/login?target=" + ToolsProperties.baseUrlTools + 
                            "/clarindk/login?target=" + "/tools/deposit" + "\">Klik her for at logge ind</a>.</p>");
                            //"/tools/deposit" + "\">Klik her for at logge ind</a>.</p>");
                        html.append("</body>");
                        html.append("</html>");

                        out.println(html.toString());
                        return;
                        }
                    }
                }
            }
        else
            {
            throw new ServletException("Bracmat is not loaded. Reason:" + BracMat.reason());
            }
        }
    }

