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
import java.io.DataOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.InetSocketAddress;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
//import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import static org.junit.Assert.*;



import dk.clarin.tools.ToolsProperties;
import dk.clarin.tools.workflow;
import dk.clarin.tools.parameters;
import dk.cst.bracmat;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.regex.PatternSyntaxException;
import java.text.SimpleDateFormat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Prepare and run a workflow.
 * Functions as a wizard, taking input from various stages.
 * The function creates a resource by choice of goal
 * Arguments:
 *      - http parameters
 *      - directedBy=  GoalChoice (default) 
 * http parameters:
 *      mail2=<address>
 *      action=batch | batch=on | batch=off
 *      TOOL=<toolname>
 *      bsubmit="next step"|"Submit"|"View details"
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
public class compute extends HttpServlet 
    {

    // Static logger object.  
    private static final Logger logger = LoggerFactory.getLogger(compute.class);

    private File destinationDir;
    private bracmat BracMat;
    /// The user's email
    private String userEmail;// = null;
    /// The user's preferred interface language
    private String UIlanguage;// = null;

    private String date;

    private DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private DocumentBuilder builder;// = null;
        public static final int CHUNK_SIZE = 2048;
    private static final byte[] INSTREAM = "zINSTREAM\0".getBytes();

    // Virus scanning using ClamAV inspired by https://github.com/philvarner/clamavj
    /**
     * virus scan a byte array.
     */
    public boolean scan(byte[] in) throws IOException {
                logger.debug("scan!");

        return scan(new ByteArrayInputStream(in));
    }

    /**
     * virus scan an InputStream
     */
    public boolean scan(InputStream in) {
        logger.debug("A");
        Socket socket = new Socket();
        logger.debug("B");

        try {
        logger.debug("C");
            socket.connect(new InetSocketAddress("localhost", 3310));
        logger.debug("D");
        } catch (IOException e) {
            logger.error("could not connect to clamd server", e);
            return false;
        }

        try {
        logger.debug("E");
            socket.setSoTimeout(60000);
        logger.debug("F");
        } catch (SocketException e) {
            logger.error("Could not set socket timeout to " + 60000 + "ms", e);
        }

        DataOutputStream dos = null;
        String response = "";
        try {  

            try {
        logger.debug("G");
                dos = new DataOutputStream(socket.getOutputStream());
        logger.debug("H");
            } catch (IOException e) {
                logger.error("could not open socket OutputStream", e);
                return false;
            }

            try {
        logger.debug("I");
                dos.write(INSTREAM);
        logger.debug("J");
            } catch (IOException e) {
                logger.debug("error writing INSTREAM command", e);
                return false;
            }

            int read = CHUNK_SIZE;
            byte[] buffer = new byte[CHUNK_SIZE];
            while (read == CHUNK_SIZE) {
                try {
        logger.debug("K");
                    read = in.read(buffer);
        logger.debug("L");
                } catch (IOException e) {
                    logger.debug("error reading from InputStream", e);
                    return false;
                }
        logger.debug("M");

                if (read > 0) {
        logger.debug("N");
                    try {
        logger.debug("O");
                        dos.writeInt(read);
        logger.debug("P");
                        dos.write(buffer, 0, read);
        logger.debug("Q");
        String input = new String(buffer, 0, read);
                    logger.debug(input);
                    } catch (IOException e) {
                        logger.debug("error writing data to socket", e);
                        break;
                    }
                }
            }

            try {
        logger.debug("R");
                dos.writeInt(0);
        logger.debug("S");
                dos.flush();
        logger.debug("T");
            } catch (IOException e) {
                logger.debug("error writing zero-length chunk to socket", e);
            }

            try {
        logger.debug("U");
                read = socket.getInputStream().read(buffer);
        logger.debug("V");
            } catch (IOException e) {
                logger.debug("error reading result from socket", e);
                read = 0;
            }

            if (read > 0)
            {
        logger.debug("W");
            response = new String(buffer, 0, read);
        logger.debug("X");
            }

        } finally {
        logger.debug("Y");
            if (dos != null) try {
        logger.debug("Z");
                dos.close();
        logger.debug("ZA");
            } catch (IOException e) {
                logger.debug("exception closing DOS", e);
            }
            try {
        logger.debug("ZB");
                socket.close();
        logger.debug("ZC");
            } catch (IOException e) {
                logger.debug("exception closing socket", e);
            }
        }

        if (logger.isDebugEnabled()) logger.debug("Response: " + response.trim());

        if ("stream: OK".equals(response.trim()))
            return true;
        else
            return false;
    }

    public boolean virusscan(String name) 
    {

        int byteCount;
        byte bytes[];
        logger.debug("AA");
        File f = new File(destinationDir,name);
        logger.debug("BB");
        try               
            {
        logger.debug("CC");
            FileInputStream fis = new FileInputStream(f);
        logger.debug("DD");
            bytes = new byte[(int) f.length()];
        logger.debug("EE");
            byteCount = fis.read(bytes);
        logger.debug("FF");
                String input = new String(bytes, 0, byteCount);
                    logger.debug(input);

            boolean result;
        logger.debug("GG");
            result = scan(bytes);
        logger.debug("HH");
            return result;        
            }
        catch(Exception e)
            {
            return false;
            }
    }

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

    private String theMimeType(String urladdr)
        {
        try{
            URL url = new URL(urladdr);
            URLConnection urlConnection = url.openConnection();
            String mimeType = urlConnection.getContentType();
            logger.info("mimeType according to getContentType() {} is {}",urladdr,mimeType);
            return mimeType;
            }
        catch(java.net.SocketTimeoutException e) 
            {
            logger.error("SocketTimeoutException in theMimeType: {}",e.getMessage());
            return "";        
            }
        catch(IOException e)
            {
            logger.error("IOException in theMimeType: {}",e.getMessage());
            return "";
            }
        }

    private int webPageBinary(String urladdr, File file)
        {
        try
            {
            //The following url is downloaded by wget, which is much better at handling 303's and 302's.
            //download("https://www.lesoir.be/185755/article/2018-10-21/footbelgate-le-beerschot-wilrijk-jouera-contre-malines-sous-reserve");
          
            HttpURLConnection.setFollowRedirects(true); // defaults to true
            CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
            URL url = new URL(urladdr);
            HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
            urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.1");
            int status = urlConnection.getResponseCode();
            urlConnection.connect();
            status = urlConnection.getResponseCode();
            InputStream input = urlConnection.getInputStream();
            status = urlConnection.getResponseCode();
            if(status == 200)
                {
                byte[] buffer = new byte[4096];
                int n = - 1;
                int N = 0;
                OutputStream output = Files.newOutputStream(file.toPath());
                while ( (n = input.read(buffer)) != -1) 
                    {
                    output.write(buffer, 0, n);
                    N += 1;
                    }
                output.close();
                return N;
                }
            else
                {
                logger.warn("URL {} returns {} in webPageBinary: {}",urladdr,Integer.toString(status));
                return 0;
                }
            }
        catch(java.net.SocketTimeoutException e) 
            {
            logger.error("SocketTimeoutException in webPageBinary: {}",e.getMessage());
            return -1;        
            }
        catch(IOException e)
            {
            logger.error("IOException in webPageBinary: {}",e.getMessage());
            return -1;
            }
        }
    

    private void createAndProcessPipeLine(HttpServletResponse response,String arg,PrintWriter out,String BracmatFunc)
        {
        String result;
        // BracmatFunc == specifyGoal | showworkflows | chosenworkflow | usermsg
        result = BracMat.Eval(BracmatFunc+"$(" + arg + ")");
        if(result == null || result.equals(""))
            {
            response.setStatus(404);
            logger.info("Did not create any workflow. (404)");
            return;
            }

        String StatusJobNrJobIdResponse[] = result.split("~", 4);
        if(StatusJobNrJobIdResponse.length == 4)
            { // toolsProg.bra: chosenworkflow.ApplyThePipelineToTheInput
            String Status = StatusJobNrJobIdResponse[0];
            String JobNr  = StatusJobNrJobIdResponse[1];
            String JobId  = StatusJobNrJobIdResponse[2];
            String output = StatusJobNrJobIdResponse[3];
            response.setStatus(Integer.parseInt(Status));
            if(BracMat.Eval("goodRunningThreads$").equals("y"))
                {
                if(!JobId.equals("")) // empty JobId means: there is nothing to do, goal is already fulfilled
                    {
                    // Asynkron håndtering:
                    Runnable runnable = new workflow(JobNr); // start the workflow in a new thread.
                    Thread thread = new Thread(runnable);
                    thread.start();
                    }
                }
            out.println(output); // poll until workflow is finished, page set to reload every N seconds
            }
        else
            {
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
                out.println(result.substring(start));
                try
                    {
                    int status = Integer.parseInt(result.substring(0,start));
                    response.setStatus(status);
                    }
                catch(NumberFormatException e)
                    {
                    response.setStatus(404);
                    logger.warn("NumberFormatException. Could not parse initial integer in {}. Message: {}",result,e.getMessage());
                    }
                return;
                }

            out.println(result);
            }
        }

    private String makeLocalCopyOfRemoteFile(String val)
        {
        if(!val.equals(""))
            {
            String PercentEncodedURL = BracMat.Eval("percentEncodeURL$("+util.quote(val) + ")");
            String LocalFileName = BracMat.Eval("storeUpload$("+util.quote(val) + "." + util.quote(date) + ")");
            
            File file = new File(destinationDir,LocalFileName);

            int textLength = webPageBinary(PercentEncodedURL,file);
            logger.debug("Avirusscan "+LocalFileName);
            if(true/* virusscan(LocalFileName)*/)
                {
                String ContentType = theMimeType(PercentEncodedURL);
                if(textLength > 0 && !ContentType.equals(""))
                    {
                    boolean hasNoPDFfonts = PDFhasNoFonts(file,ContentType);
                    return " (FieldName,"      + util.quote("input")
                         + ".Name,"            + util.quote(PercentEncodedURL)
                         + ".ContentType,"     + util.quote(ContentType) + (hasNoPDFfonts ? " true" : "")
                         + ".Size,"            + Long.toString(textLength)
                         + ".DestinationDir,"  + util.quote(ToolsProperties.documentRoot)
                         + ".LocalFileName,"   + util.quote(LocalFileName)
                         + ")";                
                    }
                else BracMat.Eval("unstore$("+util.quote(LocalFileName)+")");
                }
            else 
                {
                logger.debug("Error encountered while uploading file. ");
                BracMat.Eval("unstore$("+util.quote(LocalFileName)+")");
                }
            }
        return "";
        }
        
    public void Workflow(HttpServletRequest request,HttpServletResponse response,String BracmatFunc)
        throws ServletException, IOException 
        {
        // Check if it is the allowed server that tries to start a workflow
        if(UIlanguage == null)
            UIlanguage = parameters.getPreferredLocale(request,null);
        
        response.setContentType("application/xhtml+xml; charset=iso-8859-1");//UTF-8");
        response.setStatus(200);
        PrintWriter out = response.getWriter();
        if(!BracMat.loaded())
            {
            response.setStatus(500);
            throw new ServletException("Bracmat is not loaded. Reason:" + BracMat.reason());
            }

        @SuppressWarnings("unchecked")
        Enumeration<String> parmNames = (Enumeration<String>)request.getParameterNames();

        String arg = "(method.GET) (DATE." + util.quote(date) + ")";
        boolean OK = true;
        for (Enumeration<String> e = parmNames ; e.hasMoreElements() ;) 
            {
            String parmName = e.nextElement();
            String vals[] = request.getParameterValues(parmName);
            if(parmName.equals("mail2"))
                {
                for(String val : vals)
                    {
                    if(!val.equals(""))
                        {
                        userEmail = null;
                        arg = arg + " (" + util.quote(parmName) + ".";
                        arg += " " + util.quote(val);
                        arg += ")";
                        }
                    }
                }
            else if(parmName.equals("text"))
                {
                for(String val : vals)
                    {
                    if(!val.equals(""))
                        {
                        int textLength = val.length();
                        if(textLength > 0)
                            {
                            String LocalFileName = BracMat.Eval(BracmatFunc+"$("+util.quote("text") + "." + util.quote(date) + ")");
                            File file = new File(destinationDir,LocalFileName);
                            arg = arg + " (FieldName,"      + util.quote("text")
                                      + ".Name,"            + util.quote(LocalFileName/*"text"*/)
                                      + ".ContentType,"     + util.quote("text/plain")
                                      + ".Size,"            + Long.toString(textLength)
                                      + ".DestinationDir,"  + util.quote(ToolsProperties.documentRoot)
                                      + ".LocalFileName,"   + util.quote(LocalFileName)
                                      + ")";
                        
                            PrintWriter outf = new PrintWriter(file);
                            outf.println(val); 
                            outf.close();  
                            /*
                            try 
                                {
                                virusscan(LocalFileName);
                                }
                            catch(Exception ex) 
                                {
                                logger.debug("Error encountered while uploading file. ",ex);
                                out.close();
                                }
                                */
                            }
                        }
                    }
                }
            else if(parmName.equals("URL"))
                {
                for(String val : vals)
                    {
                    arg = arg + makeLocalCopyOfRemoteFile(val);
                    }
                }
            else if(parmName.equals("URLS"))
                {
                for(String val : vals)
                    {
                    if(!val.equals(""))
                        {
                        int textLength = val.length();
                        if(textLength > 0)
                            {
                            try {
                                String[] splitArray = val.split("\\r?\\n");
                                for(String line : splitArray)
                                    {
                                    arg = arg + makeLocalCopyOfRemoteFile(line);
                                    }
                                } 
                            catch (PatternSyntaxException ex)
                                {
                                // 
                                }
                            }
                        }
                    }
                }
            else
                {
                for(String val : vals)
                    {
                    arg = arg + " (" + util.quote(parmName) + ".";
                    arg += " " + util.quote(val);
                    arg += ")";
                    }
                }
            }
        if(userEmail != null)
            {
            arg = arg + " (" + util.quote("mail2") + ".";
            arg += " " + util.quote(userEmail);
            arg += ")";
            }

        arg = assureArgHasUIlanguage(request,null,arg);

        if(OK)
            {
            createAndProcessPipeLine(response,arg,out,BracmatFunc);
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

                //String command = "/usr/local/bin/pdffonts " + pdfFile.getAbsolutePath();
                //String command = "/usr/bin/pdffonts " + pdfFile.getAbsolutePath();
                String command = "pdffonts " + pdfFile.getAbsolutePath();

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
            
    public String getParmsAndFiles(List<FileItem> items,HttpServletResponse response,PrintWriter out) throws ServletException
        {        
        if(!BracMat.loaded())
            {
            response.setStatus(500);
            throw new ServletException("Bracmat is not loaded. Reason:" + BracMat.reason());
            }

        String arg = "(method.POST) (DATE." + util.quote(date) + ")";

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
                        int textLength = item.getString("UTF-8").length();
                        if(textLength > 0)
                            {
                            String LocalFileName = BracMat.Eval("storeUpload$("+util.quote("text") + "." + util.quote(date) + ")");
                            File file = new File(destinationDir,LocalFileName);
                            arg = arg + " (FieldName,"      + util.quote("text")
                                      + ".Name,"            + util.quote(LocalFileName/*"text"*/)
                                      + ".ContentType,"     + util.quote("text/plain")
                                      + ".Size,"            + Long.toString(textLength)
                                      + ".DestinationDir,"  + util.quote(ToolsProperties.documentRoot)
                                      + ".LocalFileName,"   + util.quote(LocalFileName)
                                      + ")";
                            item.write(file);
//                            virusscan(LocalFileName);
                            }
                        }
                    else if(item.getFieldName().equals("URL"))
                        {
                        arg = arg + makeLocalCopyOfRemoteFile(item.getString("UTF-8"));
                        }
                    else if(item.getFieldName().equals("URLS"))
                        {
                        String val = item.getString("UTF-8");
                        try {
                            String[] splitArray = val.split("\\r?\\n");
                            for(String line : splitArray)
                                {
                                if(!line.equals(""))
                                    arg = arg + makeLocalCopyOfRemoteFile(line);
                                }
                            } 
                        catch (PatternSyntaxException ex)
                            {
                            // 
                            }
                        }
                    else
                        arg = arg + " (" + item.getFieldName() + "." + util.quote(item.getString("UTF-8")) + ")";
                    }
                else if(item.getName() != "")
                    {
                    //Handle Uploaded files.
                    String LocalFileName = BracMat.Eval("storeUpload$("+util.quote(item.getName()) + "." + util.quote(date) + ")");
                    /*
                    * Write file to the ultimate location.
                    */
                    File file = new File(destinationDir,LocalFileName);
                    item.write(file);
                    //virusscan(LocalFileName);
                    String ContentType = item.getContentType();
                    boolean hasNoPDFfonts = PDFhasNoFonts(file,ContentType);
                    arg = arg + " (FieldName,"      + util.quote(item.getFieldName())
                              + ".Name,"            + util.quote(item.getName())
                              + ".ContentType,"     + util.quote(ContentType) + (hasNoPDFfonts ? " true" : "")
                              + ".Size,"            + Long.toString(item.getSize())
                              + ".DestinationDir,"  + util.quote(ToolsProperties.documentRoot)
                              + ".LocalFileName,"   + util.quote(LocalFileName)
                              + ")";
                    }
                }
            }
        catch(Exception ex) 
            {
            logger.debug("Error encountered while uploading file. ",ex);
            out.close();
            }
        return arg;
        }


    public void PostWorkflow(HttpServletRequest request,HttpServletResponse response,String BracmatFunc) throws ServletException, IOException 
        {
        List<FileItem> items = parameters.getParmList(request);

        PrintWriter out = response.getWriter();

        //response.setContentType("text/html; charset=iso-8859-1");//UTF-8");
        response.setContentType("application/xhtml+xml; charset=iso-8859-1");//UTF-8");

        response.setStatus(200);

        String arg  = getParmsAndFiles(items,response,out);
        arg = assureArgHasUIlanguage(request,items,arg);
        createAndProcessPipeLine(response,arg,out,BracmatFunc);
        }
    }
