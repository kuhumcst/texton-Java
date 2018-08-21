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

import dk.cst.bracmat;
import dk.clarin.tools.ToolsProperties;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.zip.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

public class workflow implements Runnable 
    {
    private static final Logger logger = LoggerFactory.getLogger(workflow.class);

    //private File destinationDir;
    private bracmat BracMat;
    private String result;

    private static final int BUFFER = 2048;

    public static final int ACCEPT       = 1; //We have accepted your request for applying tools to resources.
    public static final int WRAPUP       = 2; //The results from the tool-workflow are ready to inspect
    public static final int ERROR        = 3; //Something went wrong
    public static final int ERRORUSER    = 4; //Something went wrong

//    public workflow(String Result, File DestinationDir)
    public workflow(String Result)
        {
        BracMat = new bracmat(ToolsProperties.bootBracmat);
        result = Result; // Ends with the Job Number 
        //destinationDir = DestinationDir;
        }

    public void run() 
        {
        processPipeLine(result);
        }

    /**
    * Generate an md5 checksum
    **/
    static public String MD5(String fileName)
        {
        try
            {
            //FileInputStream fis = new java.io.FileInputStream( new java.io.File( fileName ) );
            InputStream fis = Files.newInputStream(Paths.get(fileName));
            String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex( fis );
            return md5;
            } 
        catch(IOException io)
            {
            logger.error("IO error while calculating checksum, message is: " + io.getMessage());
            return "";
            }
        }
        
    static public void zip(String path,String name,ZipOutputStream out)
        {
        try
            {
            BufferedInputStream origin = null;
            byte data[] = new byte[BUFFER];
            //FileInputStream fi = new FileInputStream(path);
            InputStream fi = Files.newInputStream(Paths.get(path));
            origin = new BufferedInputStream(fi, BUFFER);
            ZipEntry entry = new ZipEntry(name);
            out.putNextEntry(entry);
            int count;
            while((count = origin.read(data, 0, BUFFER)) != -1) 
                {
                out.write(data, 0, count);
                }
            origin.close();
            }
        catch(FileNotFoundException e)
            {
            logger.error("zip:FileNotFoundException {}",e.getMessage());
            }            
        catch(IOException e)
            {
            logger.error("zip:IOException {}",e.getMessage());
            }
        }
        
    static public void zipstring(String name,ZipOutputStream out,String str)
        {
        try
            {
            byte data[] = str.getBytes();
            ZipEntry entry = new ZipEntry(name);
            out.putNextEntry(entry);
            int count = data.length;
            out.write(data, 0, count);
            }
        catch(IOException e)
            {
            logger.error("zipstring:IOException {}",e.getMessage());
            }
        }
        
    static public String Filename(String name,bracmat BracMat)
        {
        return BracMat.Eval("Filename$(" + workflow.quote(name) + ")"); 
        /*
        String filenameWithoutXMLextension = name;
        int lastdot = name.lastIndexOf('.');
        if(lastdot > 0)
            {
            String extension = name.substring(lastdot);
            filenameWithoutXMLextension = filenameWithoutXMLextension.substring(0,filenameWithoutXMLextension.lastIndexOf(extension));
            return filenameWithoutXMLextension + ".withmetadata.xml";
            }
        return filenameWithoutXMLextension + ".withmetadata.xml";
        */
        }

    static public String FilenameNoMetadata(String name,bracmat BracMat)
        {
        return BracMat.Eval("FilenameNoMetadata$(" + workflow.quote(name) + ")"); 
        }

    static public String FilenameRelations(String name,bracmat BracMat)
        {
        return BracMat.Eval("FilenameRelations$(" + workflow.quote(name) + ")"); 
        /*
        String filenameWithoutXMLextension = name;
        int lastdot = name.lastIndexOf('.');
        if(lastdot > 0)
            {
            String extension = name.substring(lastdot);
            filenameWithoutXMLextension = filenameWithoutXMLextension.substring(0,filenameWithoutXMLextension.lastIndexOf(extension));
            return filenameWithoutXMLextension + ".relations.csv";
            }
        return filenameWithoutXMLextension + ".relations.csv";
        */
        }

    public static String getCharacterDataFromElement(Element e) 
        {
        Node child = e.getFirstChild();
        if (child instanceof CharacterData) 
            {
            CharacterData cd = (CharacterData) child;
            return cd.getData();
            }
        return "";
        }

    public static String errorInfo(String toolsandfiles)
        {
        String body = "";
        try 
            {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(toolsandfiles));

            Document doc = db.parse(is);
            NodeList nodes = doc.getElementsByTagName("step");

            // iterate the results
            for (int i = 0; i < nodes.getLength(); ++i) 
                {
                Element element = (Element) nodes.item(i);

                //NodeList JobNrlist = element.getElementsByTagName("JobNr");
                //Element JobNrelement = (Element) JobNrlist.item(0);
                //String JobNr = getCharacterDataFromElement(JobNrelement);

                NodeList JobIDlist = element.getElementsByTagName("JobId");
                Element JobIDelement = (Element) JobIDlist.item(0);
                String JobID = getCharacterDataFromElement(JobIDelement);

                NodeList toollist = element.getElementsByTagName("tool");
                Element toolelement = (Element) toollist.item(0);
                String tool = getCharacterDataFromElement(toolelement);

                body += "Fejlen skete i trin " + JobID + " (værktøj: " + tool + ")" +  ":<br />\n";
                
                NodeList itemslist = element.getElementsByTagName("item");
                if(itemslist.getLength() > 0)
                    {
                    body += "Værktøjet havde disse resurser som input:<br />\n";
                    for (int j = 0; j < itemslist.getLength(); ++j) 
                        {
                        Element item = (Element) itemslist.item(j);

                        NodeList idlist = item.getElementsByTagName("id");
                        Element idelement = (Element) idlist.item(0);
                        String id = getCharacterDataFromElement(idelement);

                        NodeList titlelist = item.getElementsByTagName("title");
                        Element titleelement = (Element) titlelist.item(0);
                        String title = getCharacterDataFromElement(titleelement);
                        body += id + " \'" + title + "\'<br />\n";
                        }
                    if(i > 0)
                        body += "<br />\n(Input fra eventuelt foregående trin er ikke nævnt.)<br />\n";
                    else
                        body += "<br />\n";
                    }
                }
            }
        catch (Exception e) 
            {
            logger.error(e.getMessage());
            }
        return body;
        }

    public void sendMail(int status, String name, String string1,String toolErrorMessage,String toolsandfiles,String mail2)
        throws org.apache.commons.mail.EmailException 
        {
/*
        try
            { 
            logger.debug("sendMail("  + status + ", " + name + ", ... , ... , " + mail2 + ")");

            SimpleEmail email = new SimpleEmail();
            email.setHostName(ToolsProperties.mailServer);
            email.setFrom(ToolsProperties.mailFrom, ToolsProperties.mailFromName);
            email.setSmtpPort(Integer.parseInt(ToolsProperties.mailPort));
            email.setCharset("UTF-8");

            String body = "some body";
            String subject = "some subject ﷰ";
            switch(status){
            case ACCEPT: 
                subject = "[clarin.dk] Ny data fra integrerede værktøjer";
                body = "<html><body><p>" 
                    + "Vi har modtaget dit ønske om at oprette ny data ved hjælp af integrerede værktøjer.<br /><br />\n\n"
                    + "Når oprettelsen er færdig, vil du modtage en email igen, der bekræfter at "
                    + "oprettelsen gik godt, samt en liste over URL'er hvor du vil kunne finde dine data<br /><br />\n\n"
                    + "Du kan ikke svare på denne email. Hvis ovenstående oplysninger ikke er rigtige, "
                    + "eller du har spørgsmål, kan du henvende dig på mail-adressen admin@clarin.dk<br /><br />\n\n"
                    + "Venlig hilsen \nclarin.dk</p></body></html>";    
                break;
            case WRAPUP:
                logger.debug("sendMail("  + status + ", " + name + ", " + string1 + ", " + toolErrorMessage + ", " + toolsandfiles + ", " + mail2 + ")");
                subject = "[clarin.dk]  Samlet output fra integrerede værktøjer - success";
                body = BracMat.Eval("WRAPUPbody$(" + workflow.quote(toolsandfiles) + ")"); 
                break;
            case ERRORUSER: 
                subject = "[clarin.dk] Integreret værktøj melder fejl";
                body = "<html><body><p>"
                    + string1 
                    + (toolErrorMessage.equals("") ? "" : "<br /><br />\n\nClarin.dk har modtaget denne besked fra værktøjet:<br /><br />\n\n" + toolErrorMessage)
                    + "<br /><br />\n\n"
                    + errorInfo(toolsandfiles)
                    + "<br /><br />\n\nDu kan ikke svare på denne email. Fejlbeskeden er også sendt til systemadministratoren."
                    + "<br /><br />\n\nVenlig hilsen\nclarin.dk</p></body></html>";
                break;
            default: //ERROR
                subject = "[clarin.dk] Integreret værktøj melder fejl";
                body = "<html><body><p>"
                    + string1 
                    + (toolErrorMessage.equals("") ? "" : "<br /><br />\n\nClarin.dk har modtaget denne besked fra værktøjet:<br /><br />\n\n" + toolErrorMessage)
                    + "<br /><br />\n\n"
                    + errorInfo(toolsandfiles)
                    + "<br /><br />\n\nVenlig hilsen\nclarin.dk</p></body></html>";
                break;
                }
            email.setSubject(subject);
            email.setMsg(body);
            email.updateContentType("text/html; charset=UTF-8");
            email.addTo(mail2,name);
            email.send();
            } 
        catch (org.apache.commons.mail.EmailException m)
            {
            logger.error
                ("[Tools generated org.apache.commons.mail.EmailException] mailServer:"  + ToolsProperties.mailServer
                + ", mailFrom:"         + ToolsProperties.mailFrom
                + ", mailFromName:"     + ToolsProperties.mailFromName
                + ", mailPort:"         + Integer.parseInt(ToolsProperties.mailPort)
                + ", mail2:"            + mail2
                + ", name:"             + name
                );
            //m.printStackTrace();
            logger.error("{} Error sending email. Message is: {}","Tools", m.getMessage());
            }
        catch (Exception e)
            {//Catch exception if any
            logger.error
                ("[Tools generated Exception] mailServer:"  + ToolsProperties.mailServer
                + ", mailFrom:"         + ToolsProperties.mailFrom
                + ", mailFromName:"     + ToolsProperties.mailFromName
                + ", mailPort:"         + Integer.parseInt(ToolsProperties.mailPort)
                + ", mail2:"            + mail2
                + ", name:"             + name
                );
            logger.error("{} Exception:{}","Tools",e.getMessage());
            }
*/
        }



    public static String escape(String str)
        {
        int len = str.length();
        StringBuilder sb = new StringBuilder((3 * len)/ 2); 
        for(int i = 0;i < str.length();++i)
            {
            if(str.charAt(i) == '\\' || str.charAt(i) == '"')
                {
                sb.append('\\');
                }
            sb.append(str.charAt(i));
            }
        return sb.toString();
        }

    public static String quote(String str)
        {
        return "\"" + escape(str) + "\"";
        }

    public static String escapedquote(String str)
        {
        return "\\\"" + escape(str) + "\\\"";
        }


    private void processPipeLine(String result)
        {
        int jobs = 10; // Brake (or break) after 10 failed iterations. Then something is possibly wrong
        int code = 0;
        boolean asynchronous = false;
        while(jobs > 0)
            {
            // Jobs are hierarchically structured:
            //    All jobs with the same job number belong together. They constitute a pipeline.
            //    (Amendment 20110606: if many similar resources are processed batch-wise, 
            //     they share the same job number, whereas the Job ID is incremented monotonically.
            //     In this way, many independent pipelines are made into one big pipeline.)
            //    All jobs with the same job number must be performed sequentially, in increasing order of their job id.
            // getNextJobID looks for the trailing number of the result string, e.g. job number "55" if result is "Submitted55"
            // Each job number represents one pipeline. A pipeline consists of one or more jobs, each with a jobID that is unique within the job.
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
            // Now we have a job that must be launched
            
            if(jobID.equals(""))
                jobs = 0; // No more jobs on job list, quit from loop
            else
                {
                // getJobArg looks for the trailing number of the result string, e.g. job number "55" if result is "Submitted55"

                /**
                 * getJobArg$
                 *
                 * Consults tables jobs.table and tooladm.table in jboss/server/default/data/tools to answer several requests
                 * Arguments: jobNr, jobID and one of the following requests:
                 *      endpoint        the URL where the integrated tool lives
                 *      filename        the name to be given to the output
                 *      method          POST or GET
                 *      requestString   the request string as HTTP-parameters or as XML
                 */
                // endpoint = entry point for tool webservice
                String endpoint      = BracMat.Eval("getJobArg$(" + result + "." + jobID + ".endpoint)");

                // requestString = arguments sent to the tool webservice
                String requestString = BracMat.Eval("getJobArg$(" + result + "." + jobID + ".requestString)");

                // filename = name of the file where the tool output will be stored when the GET gets back.
                String filename      = BracMat.Eval("getJobArg$(" + result + "." + jobID + ".filename)"); 
                String method        = BracMat.Eval("getJobArg$(" + result + "." + jobID + ".method)"); 
                boolean postmethod = method.equals("POST");
				logger.debug("requestStringA:"+requestString);
                requestString = BracMat.Eval("percentEncodeURL$("+workflow.quote(requestString) + ")");
				logger.debug("requestStringB:"+requestString);
                code = sendRequest(result, endpoint, requestString, BracMat, filename, jobID, postmethod);
                if(code == 202)
                    asynchronous = true;
                }
            if(code != 200 && code != 202)
                {
                --jobs;
                logger.info("processPipeLine aborts");
                }
            }
        if(!asynchronous)
            {
            /**
             * mail2$
             *
             * Find the mail address associated with a given workflow in the table
             * jobAbout.table in jboss/server/default/data/tools
             */
            String mail2 = BracMat.Eval("mail2$(" + result + ")"); 
            /**
             * doneAllJob$
             *
             * Do the administration when a workflow has completed.
             * Affected tables in jboss/server/default/data/tools:
             *      jobs.table
             *      CTBs.table
             *      relations.table
             *      jobAbout.table
             * The output from this function (XML) is parsed in function 
             * sendMail, and lists the items that the user can download from
             * the staging area.
             */
            try
                {
                if(!mail2.equals(""))
                    {
                    sendMail(WRAPUP
                        ,"dig"
                        ,ToolsProperties.baseUrlTools + "/tools/zipresults"
                        ,""
                        ,result
                        ,mail2
                        );
                    }
                }
            catch (Exception e)
                {//Catch exception if any
                logger.warn("Could not send mail to " + mail2 + ". Reason:" + e.getMessage());
                }
            }
        }

    public static void got200(String result, bracmat BracMat, String filename, String jobID, InputStream input)
        {
        /**
         * toolsdata$
         *
         * Return the full file system path to Tool's staging area.
         * The input can be a file name: this name is appended to the returned value.
         */
        String destdir = BracMat.Eval("toolsdata$");
        /**
         * toolsdataURL$
         *
         * Return the full URL to Tool's staging area.
         * The input can be a file name: this name is appended to the returned value.
         */
        try
            {
            String TEIformat = BracMat.Eval("isTEIoutput$(" + result + "." + jobID + ")");
            
            byte[] buffer = new byte[4096];
            int n = - 1;
            int N = 0;
            //boolean isBasisText = TEIformat.equals("txtbasis");
            StringWriter outputM = new StringWriter();
            
//          OutputStream outputF = new FileOutputStream( destdir+FilenameNoMetadata(filename,BracMat) );
            OutputStream outputF = Files.newOutputStream(Paths.get(destdir+FilenameNoMetadata(filename,BracMat)));

            boolean isTextual = false;        
            String textable = BracMat.Eval("getJobArg$(" + result + "." + jobID + ".isText)");
            if(textable.equals("y"))
                isTextual = true;
                    
            while((n = input.read(buffer)) != -1)
                {
                if (n > 0)
                    {
                    N = N + n;
                    outputF.write(buffer, 0, n);
                    if(isTextual)
                        {
                        String toWrite = new String(buffer,0,n);
                        try {
                            outputM.write(toWrite);
                            }
                        catch (Exception e)
                            {
                            logger.error("Could not write to StringWriter. Reason:" + e.getMessage());
                            }
                        }
                    }
                }
            outputF.close();   
                
            String requestResult = outputM.toString();
                
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            String date = sdf.format(cal.getTime());
            /**
             * doneJob$
             *
             * Marks a job as 'done' in jobs.table in jboss/server/default/data/tools
             * Constructs a CTBID from date, JobNr and jobID
             * Makes sure there is a row in table CTBs connecting
             *      JobNr, jobID, email and CTBID
             * Creates isDependentOf and isAnnotationOf relations
             * Affected tables:
             *      jobs.table
             *      CTBs.table
             *      relations.table
             * Arguments: jobNR, JobID, spangroup with annotation and date. 
             *
             * Notice that this function currently only can generate output of type 
             * TEIDKCLARIN_ANNO
             */
            String newResource;
            if(TEIformat.equals(""))
                {
                newResource = BracMat.Eval("doneJob$(" + result + "." + jobID +               ".."               + quote(date) + ")"); 
                }
            else
                {
                newResource = BracMat.Eval("doneJob$(" + result + "." + jobID + "." + quote(requestResult) + "." + quote(date) + ")"); 
                // Create file plus metadata
                //FileWriter fstream = new FileWriter(destdir+Filename(filename,BracMat));
                //BufferedWriter Out = new BufferedWriter(fstream);
                BufferedWriter Out = Files.newBufferedWriter(Paths.get(destdir+Filename(filename,BracMat)), StandardCharsets.UTF_8);
                Out.write(newResource);
                Out.close();
                }
            /**
             * relationFile$
             *
             * Create a relation file ready for deposition together with an annotation.
             *
             * Input: JobNr and jobID
             * Output: String that can be saved as a semicolon separated file.
             * Consulted tables:
             *      relations.table     (for relation type, ctb and ctbid
             *      CTBs.table          (for ContentProvider and CTBID)
             */
            String relations = BracMat.Eval("relationFile$(" + result + "." + jobID + ")"); 
            // Create relation file
//            FileWriter fstream = new FileWriter(destdir+FilenameRelations(filename,BracMat));
//            BufferedWriter Out = new BufferedWriter(fstream);
            BufferedWriter Out = Files.newBufferedWriter(Paths.get(destdir+FilenameRelations(filename,BracMat)), StandardCharsets.UTF_8);
            Out.write(relations);
            Out.close();
            }
        catch (Exception e)
            {//Catch exception if any
            logger.error("Could not write result to file. Aborting job " + jobID + ". Reason:" + e.getMessage());
            /**
             * abortJob$
             *
             * Abort, given a JobNr and a jobID, the specified job and all
             * pending jobs that depend on the output from the (now aborted) job.
             * Rather than removing the aborted jobs from the jobs.table list, they are
             * marked 'aborted'.
             * Result (as XML): a list of (JobNr, jobID, toolName, items)
             */
            /*filelist =*/ BracMat.Eval("abortJob$(" + result + "." + jobID + ")"); 
            }
        }                    

    public void didnotget200(int code,String result, String endpoint, String requestString, bracmat BracMat, String filename, String jobID, boolean postmethod,String urlStr,String message, String requestResult)
        {
        String filelist;
        String mail2 = BracMat.Eval("mail2$(" + result + ")"); 
        try
            {
            if(code == 202)
                {
                if(!mail2.equals(""))
                    {
                    //String toolsdataURL = BracMat.Eval("toolsdataURL$");
                    sendMail(ACCEPT
                            ,"dig"
                            ,""
                            ,""
                            ,""
                            ,mail2
                            );
                    }
                logger.warn("Got status code 202. Job " + jobID + " is set to wait for asynchronous result.");
                /**
                  * waitingJob$
                  *
                  * Make a job 'waiting'.
                  * 
                  * Input: JobNr and jobID
                  *
                  * Affected tables in jboss/server/default/data/tools:
                  *     jobs.table
                  */
                BracMat.Eval("waitingJob$(" + result + "." + jobID + ")"); 
                //jobs = 0; // No more jobs to process now, quit from loop and wait for result to be sent
                }
            else if(code == 0)
                {
                //jobs = 0; // No more jobs to process now, probably the tool is not integrated at all
                filelist = BracMat.Eval("abortJob$(" + result + "." + jobID + ")"); 
                logger.warn("abortJob returns " + filelist);
                if(!mail2.equals(""))
                    {
                    //String toolsdataURL = BracMat.Eval("toolsdataURL$");

                    sendMail(ERROR
                        ,""
                        ,"endpoint:" + endpoint + " requestString:" + requestString + " filename:" + filename + " jobID:" + jobID + " postmethod:" + (postmethod ? "y" : "n")
                        ,"No more jobs to process now, probably the tool is not integrated at all"
                        ,filelist
                        ,ToolsProperties.admEmail
                        );
                    sendMail(ERRORUSER
                        ,""
                        ,"endpoint:" + endpoint
                        ,"probably the tool is not integrated at all"
                        ,filelist
                        ,mail2
                        );
                    }
                logger.warn("Job " + jobID + " cannot open connection to URL " + urlStr);
                }
            else
                {
                filelist = BracMat.Eval("abortJob$(" + result + "." + jobID + ")"); 
                logger.warn("abortJob returns " + filelist);
                if(!mail2.equals(""))
                    {
                    //String toolsdataURL = BracMat.Eval("toolsdataURL$");
                    sendMail(ERROR
                        ,""
                        ,"Clarin.dk har modtaget en statuskode " + code + " (" + message + ") fra " + endpoint + ".<br />\n (Request:<br />\n" + requestString + ")<br /><br />\n\n"
                        ,requestResult
                        ,filelist
                        ,ToolsProperties.admEmail
                        );

                    sendMail(ERRORUSER
                        ,""
                        ,"Clarin.dk har modtaget en statuskode som indikerer at der er sket en fejl: " + code + " (" + message + ") fra " + endpoint + ".<br />\n<br /><br />\n\n"
                        ,requestResult
                        ,filelist
                        ,mail2
                        );
                    }
                logger.warn("Got status code [" + code + "]. Job " + jobID + " is aborted.");
                }
            }
        catch(org.apache.commons.mail.EmailException e)
            {
            logger.error("EmailException. Reason:" + e.getMessage());
            }
        }

    
    /**
    * Sends an HTTP GET request to a url
    *
    * @param endpoint - The URL of the server. (Example: " http://www.yahoo.com/search")
    * @param requestString - all the request parameters (Example: "param1=val1&param2=val2"). Note: This method will add the question mark (?) to the request - DO NOT add it yourself
    * @return - The response from the end point
    */

    public int sendRequest(String result, String endpoint, String requestString, bracmat BracMat, String filename, String jobID, boolean postmethod)
        {        
        int code = 0;
        String message = "";
        //String filelist;
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
                if(postmethod) // HTTP POST
                    {
                    StringReader input = new StringReader(requestString);
                    StringWriter output = new StringWriter();
                    URL endp = new URL(endpoint);

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
                        urlc.setRequestProperty("Content-type", "text/xml; charset=" + "UTF-8");

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
                            
                            if(code == 200)
                                {
                                got200(result, BracMat, filename, jobID, urlc.getInputStream());
                                }
                            else
                                {
                                InputStream in = urlc.getInputStream();
                                try
                                    {
                                    Reader reader = new InputStreamReader(in);
                                    pipe(reader, output);
                                    reader.close();
                                    requestResult = output.toString();
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
                                message = urlc.getResponseMessage();
                                didnotget200(code,result,endpoint,requestString,BracMat,filename,jobID,postmethod,urlStr,message,requestResult);
                                }                            
                            urlc.disconnect();
                            }
                        else
                            {
                            code = 0;
                            didnotget200(code,result,endpoint,requestString,BracMat,filename,jobID,postmethod,urlStr,message,requestResult);
                            }
                        }
                    }
                else // HTTP GET
                    {
                    // Send data
                    
                    if (requestString != null && requestString.length () > 0)
                        {
                        urlStr += "?" + requestString;
                        }
                    URL url = new URL(urlStr);
                    URLConnection conn = url.openConnection ();
                    conn.connect();

                    // Cast to a HttpURLConnection
                    if(conn instanceof HttpURLConnection)
                        {
                        HttpURLConnection httpConnection = (HttpURLConnection) conn;
                        code = httpConnection.getResponseCode();
                        message = httpConnection.getResponseMessage();
                        BufferedReader rd;
                        StringBuilder sb = new StringBuilder();;
                        //String line;
                        if(code == 200)
                            {
                            got200(result, BracMat, filename, jobID, httpConnection.getInputStream());
                            }
                        else
                            {
                            // Get the error response
                            rd = new BufferedReader(new InputStreamReader(httpConnection.getErrorStream()));
                            int nextChar;
                            while(( nextChar = rd.read()) != -1) 
                                {
                                sb.append((char)nextChar);
                                }
                            rd.close();
                            requestResult = sb.toString();
                            didnotget200(code,result,endpoint,requestString,BracMat,filename,jobID,postmethod,urlStr,message,requestResult);
                            }
                        }
                    else
                        {
                        code = 0;
                        didnotget200(code,result,endpoint,requestString,BracMat,filename,jobID,postmethod,urlStr,message,requestResult);
                        }
                    }
                } 
            catch (Exception e)
                {
                //jobs = 0; // No more jobs to process now, probably the tool is not reachable
                logger.warn("Job " + jobID + " aborted. Reason:" + e.getMessage());
                /*filelist =*/ BracMat.Eval("abortJob$(" + result + "." + jobID + ")"); 
                }
            }
        else
            {
            //jobs = 0; // No more jobs to process now, probably the tool is not integrated at all
            logger.warn("Job " + jobID + " aborted. Endpoint must start with 'http://' or 'https://'. (" + endpoint + ")");
            /*filelist =*/ BracMat.Eval("abortJob$(" + result + "." + jobID + ")"); 
            }
        return code;
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
            writer.write(buf, 0, read);
            }
        writer.flush();
        }

    }

