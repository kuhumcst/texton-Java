/*
    Copyright 2014, Bart Jongejan
    This file is part of the DK-ClarinTools.

    DK-ClarinTools is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DK-ClarinTools is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with DK-ClarinTools.  If not, see <http://www.gnu.org/licenses/>.
*/
package dk.clarin.tools;

import dk.clarin.tools.ToolsProperties;
import dk.clarin.tools.workflow;
import dk.clarin.tools.parameters;
import dk.cst.*;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.text.SimpleDateFormat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;

//import org.apache.commons.mail.SimpleEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

//import org.overviewproject.mime_types.MimeTypeDetector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Prepare and run a workflow.
 * Functions as a wizard, taking input from various stages.
 * The function handles two scenarios:
 *      create a resource by choice of goal ('kunde')
 *      create a resource by choice of tool ('stenhugger')
 * Arguments:
 *      - http parameters
 *      - directedBy=  GoalChoice (default) 
 * http parameters:
 *      mail2=<address>
 *      action=batch | batch=on | batch=off
 *      TOOL=<toolname>
 *      bsubmit=nextStep|prev<N>|next<N>|Submit|View"
      "details
 *      (item=<itemid>)+
 *      (Item=<itemid>)+
 *      I<feature>=<featurevalue>
 *      O<feature>=<featurevalue>
 * itemid's are of the form dkclarin:188028
 * features are (currently): facet, format and lang
 * feature values are complex strings consisting of a feature value and,
 * optionally, a specialisation of the feature values, separated from the
 * former by a caret.
 */      
@SuppressWarnings("serial")
public class create extends HttpServlet 
    {
    // Static logger object.  
    private static final Logger logger = LoggerFactory.getLogger(create.class);

    private File destinationDir;
    private bracmat BracMat;
    /// The user's email
    private String userEmail = null;
    /// The user's preferred interface language
    private String UIlanguage = null;

    public static final int ACCEPT=1;       //We have accepted your request for applying tools to resources.
    public static final int CONFIRMATION=2; //The results from the tool-workflow are ready to inspect
    public static final int ERROR=3;        //Something went wrong
    private String date;
    //private String toolsdataURL;

    private DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private DocumentBuilder builder = null;


    public String assureArgHasUIlanguage(HttpServletRequest request,List<FileItem> items, String arg)
        {
        if(!arg.contains("UIlanguage"))
            {
            UIlanguage = parameters.getPreferredLocale(request,items);
            if(UIlanguage != null && !UIlanguage.equals(""))
                arg = "(UIlanguage." + UIlanguage + ") " + arg;
            }
        
        return arg;
        }        

    public void init(javax.servlet.ServletConfig config) throws javax.servlet.ServletException 
        {
        InputStream fis = config.getServletContext().getResourceAsStream("/WEB-INF/classes/properties.xml");
        ToolsProperties.readProperties(fis);        
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        date = sdf.format(cal.getTime());
        BracMat = new bracmat(ToolsProperties.bootBracmat);
        //toolsdataURL = ToolsProperties.baseUrlTools + ToolsProperties.stagingArea;
        super.init(config);
        destinationDir = new File(ToolsProperties.documentRoot /*+ ToolsProperties.stagingArea*/);
        if(!destinationDir.isDirectory())
            {
            try
                {
                destinationDir.mkdir();
                }
            catch(Exception e)
                {
                throw new ServletException("Trying to create \"" + ToolsProperties.documentRoot /*+ ToolsProperties.stagingArea*/ + "\" as directory for temporary storing intermediate and final results, but this is not a valid directory. Error:" + e.getMessage());
                }
            }
        if(!destinationDir.isDirectory()) 
            {
            throw new ServletException("Trying to set \"" + ToolsProperties.documentRoot /*+ ToolsProperties.stagingArea*/ + "\" as directory for temporary storing intermediate and final results, but this is not a valid directory.");
            }
        // Ready the Document builder
        try 
            {
            builder = factory.newDocumentBuilder();
            } 
        catch (javax.xml.parsers.ParserConfigurationException e) 
            {
            logger.error("ParserConfigurationException during creation of DocumentBuilder");
            }
        }

	private String theMimeType(String urladdr){
		try{
			URL url = new URL(urladdr);
			URLConnection urlConnection = url.openConnection();
		    String mimeType = urlConnection.getContentType();
            logger.info("mimeType according to getContentType() {} is {}",urladdr,mimeType);
		    return mimeType;
		}catch(IOException e){
			return "error connecting to server.";
		}
	}

	private String webPage(String urladdr){
		try{
			URL url = new URL(urladdr);
			URLConnection urlConnection = url.openConnection();

			BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
			StringBuilder contents = new StringBuilder();
			char[] buffer = new char[4096];
			int read = 0;
			do  {
			    contents.append(buffer,0,read);
			    read = in.read(buffer);
			    }
			while(read >= 0);
			String Return = contents.toString();
			return Return;
		}catch(IOException e){
			return "error connecting to server.";
		}
	}
	
	private int webPageBinary(String urladdr, File file){
		try{
		    logger.debug("urladdr:"+urladdr);
			URL url = new URL(urladdr);
			URLConnection urlConnection = url.openConnection();

            InputStream input = urlConnection.getInputStream();
            byte[] buffer = new byte[4096];
            int n = - 1;
            int N = 0;
            OutputStream output = new FileOutputStream( file );
            while ( (n = input.read(buffer)) != -1) 
            {
                output.write(buffer, 0, n);
                N += 1;
            }
            output.close();
            return N;
		}catch(IOException e){
		    logger.debug("IOException in webPageBinary");
			return -1;
		}
	}
	

    private void createAndProcessPipeLine(HttpServletResponse response,String arg,PrintWriter out,String workflowRequest)
        {
        String result;
        if(workflowRequest.equals(""))
            {
            /**
             * create$
             *
             * Prepare and run a workflow.
             * Functions as a wizard, taking input from various stages.
             * The function handles two scenarios:
             *      create a resource by choice of goal ('kunde')
             *      create a resource by choice of tool ('stenhugger')
             * Arguments:
             *      - http parameters
             *      - directedBy=  GoalChoice (default) 
             * http parameters:
             *      mail2=<address>
             *      action=batch | batch=on | batch=off
             *      TOOL=<toolname>
             *      bsubmit=nextStep|prev<N>|next<N>|Submit|"View details"
             *      (item=<itemid>)+
             *      (Item=<itemid>)+
             *      I<feature>=<featurevalue>
             *      O<feature>=<featurevalue>
             * itemid's are of the form dkclarin:188028
             * features are (currently): facet, format and lang
             * feature values are complex strings consisting of a feature value and,
             * optionally, a specialisation of the feature values, separated from the
             * former by a caret.
             */
            result = BracMat.Eval("create$(" + arg + ".)");
			logger.debug("result is Γροεκσ:"+result);
            }
        else
            {
            /*
            workflowRequest = GoalChoice
            */
            result = BracMat.Eval("create" + workflowRequest + "$(" + arg + ")");
			logger.debug("result is Γοαλ:"+result);
            }
        if(result == null || result.equals(""))
            {
            response.setStatus(404);
            logger.info("Did not create any workflow. (404)");
            return;
            }

        String StatusJobNrJobIdResponse[] = result.split("~", 4);
        if(StatusJobNrJobIdResponse.length == 4)
            {
            String Status = StatusJobNrJobIdResponse[0];
            String JobNr  = StatusJobNrJobIdResponse[1];
            String JobId  = StatusJobNrJobIdResponse[2];
            String output = StatusJobNrJobIdResponse[3];
            response.setStatus(Integer.parseInt(Status));
            if(!JobId.equals(""))
                {
                // Asynkron håndtering:
                Runnable runnable = new workflow(JobNr, destinationDir);
                Thread thread = new Thread(runnable);
                thread.start();
                }
            out.println(output);
            return;
            }
           
            
        int start = result.indexOf("<?"); // XML-output (XHTML)
        if(start < 0)
            start = result.indexOf("<!"); // HTML5-output
        if(start > 0)
            {
            /* Something went wrong, e.g.:
            
            400<!DOCTYPE html>
            <html>
            <head>
             ... PDF-files that only consist of image data cannot be handled by this work flow. ...
                
                
            404<!DOCTYPE html>
            <html>
            <head>
             ... Your goal cannot be fulfilled with the currently integrated tools. ...
                
            */
            //response.setContentType("text/plain; charset=UTF-8");
            out.println(result.substring(start));
            try
                {
                int status = Integer.parseInt(result.substring(0,start));
                response.setStatus(status);
                }
            catch(NumberFormatException e)
                {
                response.setStatus(404);
                logger.info("NumberFormatException. Could not parse initial integer in {}",result);
                }
            return;
            }
        if(result.startsWith("Submitted"))
            {
            /**
             * getNextJobID$
             *
             * Given the jobNr of a workflow, return the next job that is not pending 
             * (=waiting for an another job to produce some of its inputs).
             * Argument: jobNr
             * Returns: jobID (if job found in jobs.table in jboss/server/default/data/tools)
             *          empty string (if job not found in jobs.table)
             */
            String jobID = BracMat.Eval("getNextJobID$(" + result + ")");
            if(jobID.equals(""))
                {
                out.println("<?xml version=\"1.0\"?><!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
                out.println("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"da\" lang=\"da\">");
                out.println("<head><title>DK-Clarin: Tools</title><meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" /></head>");
                out.println("<body><p>Der er intet at lave. Gå tilbage med knappen \"Forrige\".</p></body></html>");
                }
            else
                {
                // Asynkron håndtering:
                Runnable runnable = new workflow(result, destinationDir);
                Thread thread = new Thread(runnable);
                thread.start();
                out.println("<?xml version=\"1.0\"?><!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
                out.println("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"da\" lang=\"da\">");
                out.println("<head><title>DK-Clarin: Tools</title><meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" /></head>");
                if(UIlanguage != null && UIlanguage.equals("da"))
                    out.println("<body><p>Du vil få mail når der er resultater. <a href=\"" + ToolsProperties.wwwServer + "/tools/mypoll?job=" + result + "\">Følg status af job [" + result + "].</a></p></body></html>");
                else
                    out.println("<body><p>You will receive email when there are results. <a href=\"" + ToolsProperties.wwwServer + "/tools/mypoll?job=" + result + "\">Follow status of job [" + result + "].</a></p></body></html>");
                }
            response.setStatus(202);

            // Synkron håndtering:
            //processPipeLine(response,result,out);
            }
        else
            {
            out.println(result);
            }
        }

    /**
    * Helper function to convert a stream to a xml document
    * @param xml  The Xml in a stream
    * @return the xml document 
    */
    private Document streamToXml(InputStream stream) 
        {
        Document doc;
        try 
            {
            // Reading a XML file
            doc = builder.parse(stream);
            stream.close();
            }
        catch (Exception e) 
            {
            return null;
            }
        return doc;
        }

    private String makeLocalCopyOfRemoteFile(String val)
        {
        if(!val.equals(""))
            {
            logger.debug("val == {}",val);

            String PercentEncodedURL = BracMat.Eval("percentEncodeURL$("+workflow.quote(val) + ")");
            String LocalFileName = BracMat.Eval("storeUpload$("+workflow.quote(val) + "." + workflow.quote(date) + ")");

            logger.debug("LocalFileName == {}",LocalFileName);
            logger.debug("PercentEncodedURL == {}",PercentEncodedURL);

            File file = new File(destinationDir,LocalFileName);

            int textLength = webPageBinary(PercentEncodedURL,file);
            logger.debug("file size == {}",textLength);
            String ContentType = theMimeType(PercentEncodedURL);
            logger.debug("ContentType == {}",ContentType);
            if(!ContentType.equals(""))
                {
                    boolean hasNoPDFfonts = PDFhasNoFonts(file,ContentType);
                    return      " (FieldName,"      + workflow.quote("input")
                              + ".Name,"            + workflow.quote(PercentEncodedURL)
                              + ".ContentType,"     + workflow.quote(ContentType) + (hasNoPDFfonts ? " true" : "")
                              + ".Size,"            + Long.toString(textLength)
                              + ".DestinationDir,"  + workflow.quote(ToolsProperties.documentRoot)
                              + ".LocalFileName,"   + workflow.quote(LocalFileName)
                              + ")";                
                }
            }
        return "";
        }
        
    public void Workflow(HttpServletRequest request,HttpServletResponse response,String workflowRequest)
        throws ServletException, IOException 
        {
        // Check if it is the allowed server that tries to start a workflow
        if(UIlanguage == null)
            UIlanguage = parameters.getPreferredLocale(request,null);
        
        response.setContentType("text/html; charset=UTF-8");
        response.setStatus(200);
        PrintWriter out = response.getWriter();
        if(!BracMat.loaded())
            {
            response.setStatus(500);
            throw new ServletException("Bracmat is not loaded. Reason:" + BracMat.reason());
            }

        @SuppressWarnings("unchecked")
        Enumeration<String> parmNames = (Enumeration<String>)request.getParameterNames();

        String arg = "(method.GET) (DATE." + workflow.quote(date) + ")";
        boolean OK = true;
        for (Enumeration<String> e = parmNames ; e.hasMoreElements() ;) 
            {
            String parmName = e.nextElement();
            String vals[] = request.getParameterValues(parmName);
            if(parmName.equals("mail2"))
                {
                for(int j = 0;j < vals.length;++j)
                    {
                    if(!vals[j].equals(""))
                        {
                        userEmail = null;
                        arg = arg + " (" + workflow.quote(parmName) + ".";
                        arg += " " + workflow.quote(vals[j]);
                        arg += ")";
                        }
                    }
                }
            else if(parmName.equals("text"))
                {
                for(int j = 0;j < vals.length;++j)
                    {
                    if(!vals[j].equals(""))
                        {
                        int textLength = vals[j].length();
                        if(textLength > 0)
							{
							String LocalFileName = BracMat.Eval("storeUpload$("+workflow.quote("text") + "." + workflow.quote(date) + ")");
							File file = new File(destinationDir,LocalFileName);
							arg = arg + " (FieldName,"      + workflow.quote("text")
									  + ".Name,"            + workflow.quote("text")
									  + ".ContentType,"     + workflow.quote("text/plain")
									  + ".Size,"            + Long.toString(textLength)
									  + ".DestinationDir,"  + workflow.quote(ToolsProperties.documentRoot)
									  + ".LocalFileName,"   + workflow.quote(LocalFileName)
									  + ")";
                        
							PrintWriter outf = new PrintWriter(file);
							outf.println(vals[j]); 
							outf.close();                       
							}
                        }
                    }
                }
            else if(parmName.equals("URL"))
                {
				logger.debug("Going to call makeLocalCopyOfRemoteFile A");
                for(int j = 0;j < vals.length;++j)
                    {
                    arg = arg + makeLocalCopyOfRemoteFile(vals[j]);
                    }
				logger.debug("in Workflow URLargs:"+arg);
                }
            else
                {
                for(int j = 0;j < vals.length;++j)
                    {
                    arg = arg + " (" + workflow.quote(parmName) + ".";
                    arg += " " + workflow.quote(vals[j]);
                    arg += ")";
                    }
                }
            }
        if(userEmail != null)
            {
            arg = arg + " (" + workflow.quote("mail2") + ".";
            arg += " " + workflow.quote(userEmail);
            arg += ")";
            }

        arg = assureArgHasUIlanguage(request,null,arg);

        if(OK)
            {
            createAndProcessPipeLine(response,arg,out,workflowRequest);
            }
        }

    public boolean PDFhasNoFonts(File pdfFile,String ContentType)
        {
        if  (  ContentType.equals("application/pdf") 
            || ContentType.equals("application/x-download") 
            || ContentType.equals("application/octet-stream") 
            )
            {
            String lastline = "";
            String lasterrline = "";
            try 
                {
                String line;
                OutputStream stdin = null;
                InputStream stderr = null;
                InputStream stdout = null;

                String command = "/usr/bin/pdffonts " + pdfFile.getAbsolutePath();

                final Process process = Runtime.getRuntime().exec(command);
                stdin = process.getOutputStream ();
                stderr = process.getErrorStream ();
                stdout = process.getInputStream ();
                stdin.close();

                // clean up if any output in stdout
                BufferedReader brCleanUp = new BufferedReader (new InputStreamReader (stdout));
                while ((line = brCleanUp.readLine ()) != null)
                    {
                    lastline = line;
                    }
                brCleanUp.close();

                // clean up if any output in stderr
                brCleanUp = new BufferedReader (new InputStreamReader (stderr));
                while ((line = brCleanUp.readLine ()) != null)
                    {
                    lasterrline = line;
                    }
                brCleanUp.close();
                } 
            catch (Exception e) 
                {
                logger.error("cannot analyse: " + pdfFile.getName() + ", error is: " + e.getMessage());
                }
            return lasterrline.equals("") && (lastline.endsWith("---------"));
            }
        return false;
        }
            
    /**
    * Timeout for the process to assist workflow
    * @param delay time in milliseconds
    */
    private static void wait(int delay)
        {
        try {
            Thread.sleep(delay);
            } 
        catch(InterruptedException ex)
            {
            logger.error(ex.getMessage());
            }
        }

    public String getParmsAndFiles(List<FileItem> items,HttpServletResponse response,PrintWriter out) throws ServletException
        {        
        if(!BracMat.loaded())
            {
            response.setStatus(500);
            throw new ServletException("Bracmat is not loaded. Reason:" + BracMat.reason());
            }

        String arg = "(method.POST) (DATE." + workflow.quote(date) + ")";

        try 
            {
            /*
            * Parse the request
            */
            Iterator<FileItem> itr = items.iterator();
            while(itr.hasNext()) 
                {
                FileItem item = itr.next();
                /*
                * Handle Form Fields.
                */
                if(item.isFormField()) 
                    {
                    if(item.getFieldName().equals("text"))
                        {
                        int textLength = item.getString().length();
						if(textLength > 0)
							{
							String LocalFileName = BracMat.Eval("storeUpload$("+workflow.quote("text") + "." + workflow.quote(date) + ")");
							File file = new File(destinationDir,LocalFileName);
							arg = arg + " (FieldName,"      + workflow.quote("text")
									  + ".Name,"            + workflow.quote("text")
									  + ".ContentType,"     + workflow.quote("text/plain")
									  + ".Size,"            + Long.toString(textLength)
									  + ".DestinationDir,"  + workflow.quote(ToolsProperties.documentRoot)
									  + ".LocalFileName,"   + workflow.quote(LocalFileName)
									  + ")";
							item.write(file);
							}
						}
                    else if(item.getFieldName().equals("URL"))
                        {
        				logger.debug("Going to call makeLocalCopyOfRemoteFile B");
                        arg = arg + makeLocalCopyOfRemoteFile(item.getString());
						logger.debug("in getParmsAndFiles URLargs is now:"+arg);
                        }
                    else
                        arg = arg + " (" + item.getFieldName() + "." + workflow.quote(item.getString()) + ")";
                    }
                else if(item.getName() != "")
                    {
                    //Handle Uploaded files.
                    String LocalFileName = BracMat.Eval("storeUpload$("+workflow.quote(item.getName()) + "." + workflow.quote(date) + ")");
                    /*
                    * Write file to the ultimate location.
                    */
                    File file = new File(destinationDir,LocalFileName);
                    item.write(file);

                    String ContentType = item.getContentType();
                    boolean hasNoPDFfonts = PDFhasNoFonts(file,ContentType);
                    arg = arg + " (FieldName,"      + workflow.quote(item.getFieldName())
                              + ".Name,"            + workflow.quote(item.getName())
                              + ".ContentType,"     + workflow.quote(ContentType) + (hasNoPDFfonts ? " true" : "")
                              + ".Size,"            + Long.toString(item.getSize())
                              + ".DestinationDir,"  + workflow.quote(ToolsProperties.documentRoot)
                              + ".LocalFileName,"   + workflow.quote(LocalFileName)
                              + ")";
                    }
                }
            }
        catch(Exception ex) 
            {
            log("Error encountered while uploading file",ex);
            out.close();
            }
        return arg;
        }


    public void PostWorkflow(HttpServletRequest request,HttpServletResponse response,String workflowRequest) throws ServletException, IOException 
        {
        List<FileItem> items = parameters.getParmList(request);
        PrintWriter out = response.getWriter();


        response.setContentType("text/html; charset=UTF-8");

        response.setStatus(200);
        String referer = request.getHeader("referer");
        /* TODO Add DASISH server
        if(  referer == null
          || (  !referer.startsWith(ToolsProperties.wwwServer) 
             && !referer.startsWith(ToolsProperties.wwwServer.replaceFirst("^(https|http)://", "$1://www."))
             )
          )
            {
            logger.info("POST: Request sent from Unauthorized Client! (referer: {})",referer == null ? "not set" : referer);
            response.setStatus(403);
            throw new ServletException("Unauthorized");
            }
        */
        
        String arg  = getParmsAndFiles(items,response,out);
        arg = assureArgHasUIlanguage(request,items,arg);
        createAndProcessPipeLine(response,arg,out,workflowRequest);
        }

    /*
    Method doPost is called if Tools is used to process uploaded files.
     (In contrast to processing files copied from the repository.)
    */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
        {
        List<FileItem> items = parameters.getParmList(request);
        if(UIlanguage == null)
            UIlanguage = parameters.getPreferredLocale(request,items);
        PrintWriter out = response.getWriter();
        response.setContentType("text/xml");

        response.setStatus(200);
        // Check if it is the allowed server that tries to start a workflow
        
        if(  !request.getRemoteAddr().equals(ToolsProperties.acceptedWorkflowStarter) 
          && !request.getRemoteAddr().equals("127.0.0.1")
          )
            {
            logger.info("POST: Request sent from Unauthorized Client!");
            response.setStatus(403);
            throw new ServletException("Unauthorized");
            }
        String arg  = getParmsAndFiles(items,response,out);
        arg = assureArgHasUIlanguage(request,items,arg);
        createAndProcessPipeLine(response,arg,out,"");
        }
    }

