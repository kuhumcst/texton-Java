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
import dk.clarin.tools.userhandle;
import dk.cst.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
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

import org.apache.commons.mail.SimpleEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

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
                     | ToolChoice
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
    /// The eSciDoc userHandle
    private String userHandle = null;
    /// The eSciDoc id of the user
    private String userId = null;
    /// The users email
    private String userEmail = null;

    public static final int ACCEPT=1;       //We have accepted your request for applying tools to resources.
    public static final int CONFIRMATION=2; //The results from the tool-workflow are ready to inspect
    public static final int ERROR=3;        //Something went wrong
    private String date;
    //private String toolsdataURL;

	private DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	private DocumentBuilder builder = null;

    public static void sendMail(int status, String name, String href,String mail2)
        throws org.apache.commons.mail.EmailException 
        {
        try
            { 
            SimpleEmail email = new SimpleEmail();
            email.setHostName(ToolsProperties.mailServer);
            email.setFrom(ToolsProperties.mailFrom, ToolsProperties.mailFromName);
            email.setSmtpPort(Integer.parseInt(ToolsProperties.mailPort));
            email.setCharset("UTF-8");

            String body = "some body";
            String subject = "some subject";
            switch(status){
            case ACCEPT: 
                //body = "<html><body><p>Til " + name + ".<br><br>\n" 
                body = "<html><body><p>" 
                    + "Vi har modtaget dit &oslash;nske om at oprette ny data ved hjælp af integrerede v&aelig;rkt&oslash;jer.<br><br>\n\n"
                    + "N&aring;r oprettelsen er f&aelig;rdig, vil du modtage en email igen, der bekr&aelig;fter at "
                    + "oprettelsen gik godt, samt en liste over URL'er hvor du vil kunne finde dine data<br><br>\n\n"
                    + "Du kan ikke svare p&aring; denne email. Hvis ovenst&aring;ende oplysninger ikke er rigtige, "
                    + "eller du har sp&oslash;rgsm&aring;l, kan du henvende dig p&aring; mail-adressen admin@clarin.dk<br><br>\n\n"
                    + "Venlig hilsen \nclarin.dk</p></body></html>";    
                break;
            case CONFIRMATION: 
                subject = "[clarin.dk] Oprettelse af ny data ved hj&aelig;lp af integrerede v&aelig;rkt&oslash;jer - success";
                //body = "<html><body><p>Til " + name + ".<br><br>\n" 
                body = "<html><body><p>"
                    + "Vi har modtaget dit &oslash;nske om at oprette ny data ved hj&aelig;lp af integrerede v&aelig;rkt&oslash;jer.<br>\n\n"
                    + "Oprettelsen er g&aring;et godt, og du kan nu hente resultatet p&aring; denne adresse:<br><br>\n\n"
                    + "<a href=\"" + href + "\">" + href + "</a><br><br>"
                    + "\n\nDu kan ikke svare p&aring; denne email. Hvis ovenst&aring;ende oplysninger ikke er rigtige, "
                    + "eller du har sp&oslash;rgsm&aring;l, kan du henvende dig p&aring; mail-adressen admin@clarin.dk<br><br>\n\n"
                    + "Venlig hilsen \nclarin.dk</p></body></html>";    
                break;
            default: //ERROR
                subject = "[clarin.dk] Oprettelse af ny data ved hj&aelig;lp af integrerede v&aelig;rkt&oslash;jer - FEJL";
                //body = "<html><body><p>Til " + name + ".<br><br>\n"
                body = "<html><body><p>"
                    + "Der skete en fejl under oprettelsen af data.<br><br>\n\n"
                    + "DU SKAL IKKE FORETAGE DIG NOGET<br><br>\n\n" 
                    + "Du har modtaget en mail der beskriver fejlen."
                    + "Nogle typer af fejl kan systemet selv h&aring;ndtere, og andre typer skal vi l&oslash;se sammen med dig.<br>\n"
                    + "Under alle omst&aelig;ndigheder sender vi en mail til dig p&aring; " + mail2 + ".<br><br>\n\nVenlig hilsen\nclarin.dk</p></body></html>";
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
        }

    /*private static String unquote(String str)
        {
        while(str.startsWith("\"") && str.endsWith("\""))
            str = str.substring(1,str.length() - 1);
        return str;
        }*/


    /*
    Get the metadata of a resource itself - straight from the repository.
    Return them in a string.
    (Caller probably wants to parse it.)
    */
    public String getResourceAsString(String handle, String id,PrintWriter out,HttpServletResponse response)
        {
        String retval = null;
        // Nyt HttpClient object
        org.apache.commons.httpclient.HttpClient httpClient = new org.apache.commons.httpclient.HttpClient();

        // Sæt timeoutværdi i millisekunder
        httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(600000);
        // Definer om det skal være POST, GET, PUT eller DELETE
        String methodString = ToolsProperties.repoServiceUrl + id;
        logger.debug("methodString:{}",methodString);
        org.apache.commons.httpclient.methods.GetMethod method = new org.apache.commons.httpclient.methods.GetMethod(methodString);
        method.setFollowRedirects(false); // We don't want WAYF involved here

        // simuler du er en bruger med en handle, ved at sætte en cookie

        if((handle != null) && !handle.equals(""))
			{
	        logger.debug("We have a handle: {}",handle);
            method.setRequestHeader("Cookie", "escidocCookie=" + handle);
            }

        try 
            {
	        logger.debug("Execute request");
            httpClient.executeMethod(method);
	        logger.debug("request executed");
            int responseCode = method.getStatusCode();
	        logger.debug("method.getStatusCode() done");
            if(responseCode == 200)
                {
                // request went OK
		        logger.debug("request went OK");
                try
                    {
                    //out.println("request went OK\n");
                    retval = method.getResponseBodyAsString();
			        logger.debug("getResponseBodyAsString went OK");
                    }
                catch (IOException e)
                    {
			        logger.debug("IOException {}",e.getMessage());            
                    response.setStatus(500);
                    String messagetext = BracMat.Eval("getStatusCode$(\"500\".\"I/O error while reading response stream after succesfully attempting method " + workflow.escapedquote(methodString) + " \")");
                    out.println(messagetext);
                    }
                }
            else 
                {
		        logger.debug("Something bad happened");
                response.setStatus(responseCode);
                String messagetext = BracMat.Eval("getStatusCode$("+workflow.quote(Integer.toString(responseCode))+".\"In getResourceAsString: When attempting method " + workflow.escapedquote(methodString) + " \")");
                out.println(messagetext);
                }
            }
        catch (IOException io)
            {
	        logger.debug("IOException {}",io.getMessage());            
            response.setStatus(400);
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
            String messagetext = BracMat.Eval("getStatusCode$(\"400\".\"I/O error when attempting method " + workflow.escapedquote(methodString) + " \")");
            out.println(messagetext);
            }
        finally 
            {
            method.releaseConnection();
            }
        return retval;
        }

    /*
    Get the resource itself - straight from the repository. (Not its metadata) 
    */
    public boolean /*InputStream*/ getResourceAsStream(String handle, String component,PrintWriter out,HttpServletResponse response)
        {
        boolean retval = true;

        logger.debug("getResourceAsStream:component= {}",component);

        String[] idvisref = component.split("\\.");
        String id = idvisref[0];
        logger.debug("getResourceAsStream:id= {}",id);
        String visibility = idvisref[1];
        logger.debug("getResourceAsStream:visibility= {}",visibility);

        // Nyt HttpClient object
        org.apache.commons.httpclient.HttpClient httpClient = new org.apache.commons.httpclient.HttpClient();
       // org.apache.http.client.HttpClient        httpClient = new org.apache.http.client.HttpClient();

        // Sæt timeoutværdi i millisekunder
        httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(600000);
        // Definer om det skal være POST, GET, PUT eller DELETE
        String methodString = ToolsProperties.repoServiceUrl + id;
        logger.debug("methodString:{}",methodString);
        org.apache.commons.httpclient.methods.GetMethod method = new org.apache.commons.httpclient.methods.GetMethod(methodString);
        //method.setFollowRedirects(false); // We don't want WAYF involved here
        // simuler du er en bruger med en handle, ved at sætte en cookie
        if((handle != null) && !handle.equals(""))
            {
            logger.debug("handle= {}",handle);
            method.setRequestHeader("Cookie", "escidocCookie=" + handle);
            }
        try 
            {
	        logger.debug("Execute request");
            httpClient.executeMethod(method);
	        logger.debug("request executed");
            int responseCode = method.getStatusCode();
            if(responseCode == 200)
                {
		        logger.debug("request went OK");
                // request went OK
                try
                    {
                    // return the response as a stream
                    InputStream stroem;
                    stroem = method.getResponseBodyAsStream();
                    try
                        {
                        // LocalFileName is just a number
                        /*
                        * storeUpload$
                        *
                        * Administrate the storage of a copy of a file from repositorium in a place 
                        * where a webservice can fetch it - the staging area of the Tools service.
                        *
                        * Adds an entry to a seven-column table Uploads.table. 
                        * The first column is unique number generated by adding 1 to the highest 
                        * currently occupied number, second is the resource identifier of a 
                        * resource that has been copied to a local place, where a webservice can GET
                        * it. This resource identifier points at a resource's metadata, which also 
                        * shows the way to the resource itself - the third field. The fourth field 
                        * contains the name of the resource as the webservice perceives it. It is
                        * a concatenation of a number based on current date and cpu-time and the
                        * value in the first field. (If the file is uploaded, the name is the 
                        * concatenation of 'Uploaded' and the number in the first field.)
                        * The seventh field states whether the resource is public, academic or 
                        * restricted.
                        * E.g. 
                        *      ( 9
                        *      . dkclarin:59004
                        *      . dkclarin:59004/components/component/dkclarin:59003/content
                        *      . 19325730679
                        *      . 
                        *      .
                        *      . public
                        *      )
                        * The resource identifier is going to be used in the new resource's metadata.
                        * The missing fifth and sixth fields are later filled out in the setFeatures
                        * function, which analyses the XML metadata.
                        * If the user provides a file by file upload, the second and third column are the same.
                        *
                        * Input: 
                        *      item id (like dkclarin:59004/components/component/dkclarin:59003/content)
                        *      date
                        *      visibility
                        *
                        * Affected tables:
                        *      Uploads.table
                        *
                        * Output:
                        *      The local file name of the resource (= fourth field)
                        */
                        String LocalFileName = BracMat.Eval("storeUpload$(" + workflow.quote(id) + "." + workflow.quote(date) + "." + workflow.quote(visibility) + ")");
				        logger.debug("LocalFileName: {} destinationDir: {}",LocalFileName,destinationDir);
                        File f = new File(destinationDir,LocalFileName);
                        OutputStream fout=new FileOutputStream(f);
				        logger.debug("fout created");
                        byte buf[]=new byte[1024];
                        int len;
                        while((len=stroem.read(buf))>0)
                            fout.write(buf,0,len);
				        logger.debug("while loop done");
                        fout.close();
				        logger.debug("fout closed");
                        stroem.close();
				        logger.debug("stroem closed");
                        }
                    catch (IOException e)
                        {
				        logger.debug("IOException {}",e.getMessage());            
                        response.setStatus(500);
                        String messagetext = BracMat.Eval("getStatusCode$(\"500\".\"I/O error while reading response stream after succesfully attempting method " + workflow.escapedquote(methodString) + " \")");
                        out.println(messagetext);
                        retval = false;
                        }
                    }
                catch (IOException e)
                    {
                    response.setStatus(500);
                    String messagetext = BracMat.Eval("getStatusCode$(\"500\".\"I/O error while reading response stream after succesfully attempting method " + workflow.escapedquote(methodString) + " \")");
                    out.println(messagetext);
                    retval = false;
                    }
                }
            else 
                {
                response.setStatus(responseCode);
                String messagetext = BracMat.Eval("getStatusCode$("+workflow.quote(Integer.toString(responseCode))+".\"In getResourceAsStream: When attempting method " + workflow.escapedquote(methodString) + " \")");
                out.println(messagetext);
                retval = false;
                }
            }
        catch (IOException io)
            {
            // håndter at der er sket en IO fejl
	        logger.debug("IOException {}",io.getMessage());            
            response.setStatus(400);
            String messagetext = BracMat.Eval("getStatusCode$(\"400\".\"I/O error when attempting method " + workflow.escapedquote(methodString) + " \")");
            out.println(messagetext);
            retval = false;
            }
        finally 
            {
            method.releaseConnection();
            }

        return retval;// null;
        }
/* JSP code
    String cookieName = "escidocCookie";
    String handle = "";
    Cookie cookies [] = request.getCookies();
    Cookie myCookie = null;
    if ( cookies != null )
        {
        for ( int i = 0 ; i < cookies.length ; i++ )
            {
            if ( cookies[ i ].getName().equals( cookieName ) )
                {
                handle = cookies[ i ].getValue():
                }
            }
        }
*/


    public void init(javax.servlet.ServletConfig config) throws javax.servlet.ServletException 
        {
        logger.debug("init tools servlet");
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

    public String gemLocalCopyOfResource(HttpServletRequest request,String id,PrintWriter out,HttpServletResponse response) // "59003"
        {
        String item = getResourceAsString(userHandle,id,out,response);
        if(item != null)
            {
            /**
             * getComponentRef$ 
             *
             * Parses metadata for a resource.
             * Finds the item id, the value of the visibility property and the 
             * value of the xlink:href attribute in the 
             * escidocComponents:component element with mime-type 
             * text/xml, application/xml or text/plain
             * 
             * No reference is made to any of the tables in 
             * jboss/server/default/data/tools, so this function could just as
             * well have been implemented in Java.
             */
            String component = BracMat.Eval("getComponentRef$(" + workflow.quote(item) + ")");
            logger.debug("getComponentRef:component= {}",component);

            if(!component.equals("") && getResourceAsStream(userHandle,component,out,response))
                return item;
            else
                return null;
            }
        return null;
        }

    public boolean RightToLocalCopyOfResource(HttpServletRequest request,String id,PrintWriter out,HttpServletResponse response) // "59003"
        {
        logger.debug("RightToLocalCopyOfResource= {}",id);
        String item = getResourceAsString(userHandle,id,out,response);
        if(item != null)
            {
            String component = BracMat.Eval("getComponentRef$(" + workflow.quote(item) + ")");
            logger.debug("getComponentRef:component= {}",component);
            return checkAccessRights(component);
            }
        else
            logger.debug("getResourceAsString( {}, {}, ...) has returned null",userHandle,id);
        return false;
        }

    private void createAndProcessPipeLine(HttpServletResponse response,String arg,PrintWriter out,String workflowRequest)
        {
        String result;
        if(workflowRequest.equals(""))
			{
			logger.debug("workflowRequest.equals(\"\")");
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
                                 | ToolChoice
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
            }
        else
			{
			logger.debug("!workflowRequest.equals(\"\")");
            /*
            workflowRequest = GoalChoice | ToolChoice
            */
            result = BracMat.Eval("create" + workflowRequest + "$(" + arg + ")");
			}
        if(result == null || result.equals(""))
            {
            response.setStatus(404);
			logger.info("Did not create any workflow. (404)");
            return;
            }
        int start = result.indexOf("<?");
        if(start < 0)
			start = result.indexOf("<!");
        if(start > 0)
            {
            //response.setContentType("text/plain; charset=UTF-8");
            out.println(result.substring(start));
            try
                {
                int status = Integer.parseInt(result.substring(0,start));
                response.setStatus(status);
				logger.debug("status {}",status);
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
			logger.debug("Result starts with \"Submitted\"");
            String jobID = BracMat.Eval("getNextJobID$(" + result + ")");
			logger.debug("jobID == {}",jobID);
            if(jobID.equals(""))
                {
				logger.debug("Nothing to do");
                out.println("<?xml version=\"1.0\"?><!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
                out.println("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"da\" lang=\"da\">");
                out.println("<head><title>DK-Clarin: Tools</title><meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" /></head>");
                out.println("<body><p>Der er intet at lave. Gå tilbage med knappen \"Forrige\".</p></body></html>");
                }
            else
                {
                // Asynkron håndtering:
				logger.debug("starting workflow");
                Runnable runnable = new workflow(result, destinationDir);
                Thread thread = new Thread(runnable);
                thread.start();
				logger.debug("started workflow");
                out.println("<?xml version=\"1.0\"?><!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
                out.println("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"da\" lang=\"da\">");
                out.println("<head><title>DK-Clarin: Tools</title><meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" /></head>");
                out.println("<body><p>Du vil få mail når der er resultater. <a href=\"https://www.clarin.dk/tools/poll?job=" + result + "\">Følg jobstatus af job [" + result + "].</a></p></body></html>");
                }
            response.setStatus(202);

            // Synkron håndtering:
            //processPipeLine(response,result,out);
            }
        else
			{
//			logger.debug("Something funny. result from Bracmat == {}",result);
            out.println(result);
            }
        }

/* // Synkron håndtering:

    private void processPipeLine(HttpServletResponse response,String result,PrintWriter out)
        {
        out.println("<?xml version=\"1.0\"?><!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
        out.println("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"da\" lang=\"da\">");
        out.println("<head>");
        out.println("<title>DK-Clarin: Tools</title>");
        out.println("<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />");
        out.println("</head>");
        out.println("<body>");

        out.println("result:" + result + "<br />\n");
        int jobs = 10; // Brake after 10 iterations. Then something is possibly wrong
        while(jobs > 0)
            {
            --jobs;
            // Jobs are hierarchically structured:
            //    All jobs with the same job number belong together. They constitute a pipeline.
            //    All jobs with the same job number must be performed sequentially, in increasing order of their job id.

            // getNextJobID looks for the trailing number of the result string, e.g. job number "55" if result is "Submitted55"
            // Each job number represents one pipeline. A pipeline consists of one or more jobs, each with a jobID that is unique within the job.
            String jobID = BracMat.Eval("getNextJobID$(" + result + ")");
            out.println("jobID:[" + jobID + "]<br />\n");
            // Now we have a job that must be launched

            if(jobID.equals(""))
                jobs = 0; // No more jobs on job list, quit from loop
            else
                {
                // getJobArg looks for the trailing number of the result string, e.g. job number "55" if result is "Submitted55"

                // dependencies is a list of files created by earlier jobs in the pipeline that the current job depends on.
                // The first job in the pipeline has no dependencies.
                String dependencies  = BracMat.Eval("getJobArg$(" + result + "." + jobID + ".dep)");

                // endpoint = entry point for tool webservice
                String endpoint      = BracMat.Eval("getJobArg$(" + result + "." + jobID + ".endpoint)");

                // requestString = arguments sent to the tool webservice
                String requestString = BracMat.Eval("getJobArg$(" + result + "." + jobID + ".requestString)");

                // filename = name of the file where the tool output will be stored when the GET gets back.
                String filename      = BracMat.Eval("getJobArg$(" + result + "." + jobID + ".filename)"); 

                out.println("dependencies:" + dependencies + "<br />\n");
                out.println("endpoint:" + endpoint + "<br />\n");
                out.println("requestString:" + requestString.replace("&", "&amp;") + "<br />\n");
                out.println("filename:" + filename + "<br />\n");
                sendGetRequest(result, endpoint, requestString, out, BracMat, filename, jobID, response);
                }
            }
        BracMat.Eval("doneAllJob$(" + result + ")"); 
        out.println("<br /></body></html>\n");
        }
*/

    /**
    * Contructs an XACML document with request for each item we whish to access
    */
    private boolean checkAccessRights(String component) 
        {
        String[] idvisref = component.split("\\.");
        String textUrl = idvisref[2];
        logger.debug("checkAccessRights of {}",textUrl);
        // Build a xacml document to send to eSciDocs PDP
        String header = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" 
            + "<requests:requests xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " 
            + "xsi:schemaLocation=\"http://www.escidoc.de/schemas/pdp/0.3/requests https://www.escidoc.org/schemas/rest/pdp/0.3/requests.xsd\" "
            + "xmlns:requests=\"http://www.escidoc.de/schemas/pdp/0.3/requests\" "
            + "xmlns:xacml-context=\"urn:oasis:names:tc:xacml:1.0:context\">";
        String footer = "</requests:requests>";

        String requestStaticHeader = "<xacml-context:Request>" 
            + "<xacml-context:Subject SubjectCategory=\"urn:oasis:names:tc:xacml:1.0:subject-category:access-subject\">" 
            + "<xacml-context:Attribute AttributeId=\"urn:oasis:names:tc:xacml:1.0:subject:subject-id\" DataType=\"http://www.w3.org/2001/XMLSchema#string\">" 
            + "<xacml-context:AttributeValue>" 
            + userId
            + "</xacml-context:AttributeValue>" 
            + "</xacml-context:Attribute>" 
            + "</xacml-context:Subject>" 
            + "<xacml-context:Resource>" 
            + "<xacml-context:Attribute AttributeId=\"urn:oasis:names:tc:xacml:1.0:resource:resource-id\" DataType=\"http://www.w3.org/2001/XMLSchema#string\">" 
            + "<xacml-context:AttributeValue>";
        String requestStaticFooter = "</xacml-context:AttributeValue>" 
            + "</xacml-context:Attribute>" 
            + "</xacml-context:Resource>" 
            + "<xacml-context:Action>" 
            + "<xacml-context:Attribute AttributeId=\"urn:oasis:names:tc:xacml:1.0:action:action-id\" DataType=\"http://www.w3.org/2001/XMLSchema#string\">" 
            + "<xacml-context:AttributeValue>info:escidoc/names:aa:1.0:action:retrieve-content</xacml-context:AttributeValue>" 
            + "</xacml-context:Attribute>" 
            + "</xacml-context:Action>" 
            + "</xacml-context:Request>";

        // Build the request for the resource
        StringBuilder xacmlRequests = new StringBuilder();
        xacmlRequests.append(requestStaticHeader);
        xacmlRequests.append(textUrl);
        xacmlRequests.append(requestStaticFooter);

        logger.debug("putToPDP( {} )",header + xacmlRequests + footer);


        // send the request to eSciDocs Policy Decision Point
        Document xacmlResponce = putToPDP(header + xacmlRequests + footer);
        if (xacmlResponce == null) 
            {
            // Some error happened, so we return null
            logger.debug("checkAccessRights got xacmlResponce == null");
            return false;
            }

        // Parse the XML to see if the user has access...
        NodeList results = xacmlResponce.getElementsByTagName("result");
        Element result = (Element) results.item(0);
        String decision = result.getAttribute("decision");
        logger.debug("decision == {}",decision);
        return decision.equals("permit");
        }

    private Document putToPDP(String request) 
        {
        //logger.debug("PDP request: " + request);
        // for downloading...
        org.apache.commons.httpclient.HttpClient httpClient = new org.apache.commons.httpclient.HttpClient();
        org.apache.commons.httpclient.methods.PutMethod method;
        httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(500000);

        method =  new org.apache.commons.httpclient.methods.PutMethod(ToolsProperties.coreServer + "/aa/pdp");
        try 
            {
            method.setRequestEntity(new org.apache.commons.httpclient.methods.StringRequestEntity(request, "application/xml", "UTF-8"));
            //if (userHandle != null && userHandle.trim().length() > 0)
                method.setRequestHeader("Cookie", "escidocCookie=" + ToolsProperties.adminUserHandle /*userHandle*/);
            method.setFollowRedirects(false);
            // Download the item
            httpClient.executeMethod(method);
            if (method.getStatusCode() != 200) 
                {
                if (method.getStatusCode() == 302) 
                    {
                    logger.error("The userhandle has expired!");
                    return null;
                    }
                else 
                    {
                    logger.warn("Unknown error while trying to use the PDP! Request: " + request + "\nResponse: "+ method.getResponseBodyAsString() );
                    return null;
                    }
                }
            //logger.debug("PDP responce: " + method.getResponseBodyAsString() );

            InputStream in = method.getResponseBodyAsStream();
            Document ret = streamToXml(in);
            return ret;
            }
        catch (UnsupportedEncodingException e)
            {
            logger.error("An error occured while using the PDP! " + e.getMessage());
            return null;
            }
        catch (Exception e) 
            {
            logger.error("An error occured while using the PDP!");
            return null;
            }
        finally 
            {
            method.releaseConnection();
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

    private String addInput(String val,HttpServletRequest request,PrintWriter out,HttpServletResponse response,String parmName)
        {
        logger.debug("addInput({},..)",val);
        logger.debug("hasCopy {} . {}",val,date);

        String seen = "hasCopy$(" + workflow.quote(val) + "." + workflow.quote(date) + ")";
        logger.debug("seen = [{}]",seen);
        /**
         * hasCopy$
         *
         * Check whether the Tools staging area already has a copy of a given
         * resource. In that case, the slow process of copying the resource from the
         * repo and the analysis of this resource can be skipped.
         * If there is a copy, its time stamp is refreshed so as to extend its right
         * to live.
         * Arguments: item id and current date.
         * Side effect: the table Uploads.table is updated if the resource is found.
         */
        seen = BracMat.Eval("hasCopy$(" + workflow.quote(val) + "." + workflow.quote(date) + ")");
        logger.debug("hasCopy responds {}",seen);
        if(seen.equals("no"))
            {
            String item = gemLocalCopyOfResource(request,val,out,response);

            if(item == null)
                {
                logger.info("No access to {}",val);
                response.setStatus(404);
                String messagetext = BracMat.Eval("getStatusCode$(\"404\".\"Der er ingen tilgængelige data tilknyttet " + workflow.escapedquote(val) + " \")");
                out.println(messagetext);
                return null;
                }
            else
                {
                logger.debug("Created local copy of {}",val);
                String qitem = workflow.quote(item);
                String qitemid = workflow.quote(val);
                /**
                 * setFeatures$
                 *
                 * Parse metadata (XML) to find information about the resource's
                 *      facet       (like tok)
                 *      format      (like xm^txtann for TEIP5DKCLARIN_ANNOTATION)
                 *      language    (like da)
                 *      Olac title  like 
                 *          Annotation: JRC-ACQUIS 31981R3557 Danish, CstClarinSentenceAndParagraphSegmenter
                 *      base item   (like https://clarin.dk/ir/item/dkclarin:188006)
                 * The found information is added to the provisory record in the Uploads table
                 * that already contains 
                 *      an ordinal number for the resource for internal use by Tools
                 *      the resource's path (like dkclarin:188024/components/component/dkclarin:188023/content)
                 *      staging-area-filename (like 19325730672)
                 *      visibility (public, academic, restricted)
                 * Input:
                 *      The metadata (string containing XML)
                 *      the item (like dkclarin:188028)
                 *      the date (like 20110723)
                 * Affected tables in jboss/server/default/data/tools:
                 *      Uploads.table
                 *
                 * The helper functions in toolsProg.bra are very resource-type specific and must 
                 * be elaborated for each new resource type:
                 *      getFacet
                 *      getFormat
                 *      getLanguage
                 *      getOlacTitle
                 *      getBaseItem
                 */
                BracMat.Eval("setFeatures$(" + qitem + "." + qitemid + "." + workflow.quote(date) + ")");
                }
            }
        else if(!seen.equals("public"))
            { // check that user has the rights to use the cached copy of the resource.
            if(!RightToLocalCopyOfResource(request,val,out,response))
                {
                logger.info("Not authorized to use resource {}",val);
                response.setStatus(403);
                String messagetext = BracMat.Eval("getStatusCode$(\"403\".\"Not authorized to use resource " + workflow.escapedquote(val) + " \")");
                out.println(messagetext);
                return null;
                }
            }
        return " (" + workflow.quote(parmName) + ". (" + workflow.quote("\"" + val + "\"") + "))";
        }

    /* Same as addInput, apart from asking whether existing copy may be used.
       If a cleanup has wiped out the copy in the middle of the wizard-process,
       a copy is re-created silently
    */
    /*
    private String checkInput(String val,HttpServletRequest request,PrintWriter out,HttpServletResponse response,String parmName)
        {
        logger.debug("checkInput({},..)",val);
        logger.debug("hasCopy {} . {}",val,date);

        String seen = "hasCopy$(" + workflow.quote(val) + "." + workflow.quote(date) + ")";
        logger.debug("seen = [{}]",seen);
        seen = BracMat.Eval("hasCopy$(" + workflow.quote(val) + "." + workflow.quote(date) + ")");
        logger.debug("hasCopy responds {}",seen);
        if(seen.equals("no"))
            {
            String item = gemLocalCopyOfResource(request,val,out,response);

            if(item == null)
                {
                logger.info("No access allowed to {}",val);
                //out.println("Ikke autoriseret til at hente resurse " + val);
                response.setStatus(403);
                String messagetext = BracMat.Eval("getStatusCode$(\"403\".\"Not authorized to use resource " + workflow.escapedquote(val) + " \")");
                out.println(messagetext);
                return null;
                }
            else
                {
                logger.debug("Created local copy of {}",val);
                String qitem = workflow.quote(item);
                String qitemid = workflow.quote(val);
                BracMat.Eval("setFeatures$(" + qitem + "." + qitemid + "." + workflow.quote(date) + ")");
                }
            }
        / *
        else if(!seen.equals("public"))
            { // check that user has the rights to use the cached copy of the resource.
            if(!RightToLocalCopyOfResource(request,val,out,response))
                {
                out.println("Ikke autoriseret til at benytte resurse " + val);
                return null;
                }
            }
        * /
        return " (" + workflow.quote(parmName) + ". (" + workflow.quote("\"" + val + "\"") + "))";
        }
*/


    /*
    Method doGet is called if Tools is used to process resources copied from 
    the repository.
     (In contrast to processing uploaded files.)
    Each resource is represented as an 'item' parameter, e.g.
    https://clarin.dk/tools/create?item=dkclarin:111012&item=dkclarin:111016
    Alternatively, all resources can be listed in a single 'ids' parameter, e.g.
    https://clarin.dk/tools/create?ids=dkclarin:168004,dkclarin:168028,dkclarin:168332 
    */
    public void doGet(HttpServletRequest request,HttpServletResponse response)
        throws ServletException, IOException 
        {
        //userHandle = null;
        //userId = null;
        //userEmail = null;
        //logger.debug("doGet, calling Workflow, userHandle = {}",userHandle);
        Workflow(request,response,"");
        //logger.debug("doGet, called Workflow, userHandle = {}",userHandle);
        }
/*
    public boolean goodPassword(HttpServletRequest request,HttpServletResponse response,List<FileItem> items,PrintWriter out)
        {
        String password = request.getParameter("password");
        if(password == null && items != null)
            {
            password = userhandle.getParmFromMultipartFormData(request,items,"password");
            }
        logger.debug("password = {}",password);
        if(password == null || !password.equals("teeqzif55"))
            {
            response.setStatus(401);
            out.println( "<?xml version=\"1.0\"?>\n"
                        +"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" 
                        +"<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"da\" lang=\"da\">\n" 
                        +"<head>\n" 
                        +"<title>DK-Clarin: Tools</title>\n" 
                        +"<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />\n" 
                        +"</head>\n" 
                        +"<body>\n" 
                        +"<h1>401 Unauthorized</h1>\n" 
                        +"</body></html>\n"
                       );
            return false;
            }
        return true;
        }
*/

    public void Workflow(HttpServletRequest request,HttpServletResponse response,String workflowRequest)
        throws ServletException, IOException 
        {
        //userHandle = null;
        //userId = null;
        //userEmail = null;

        // Check if it is the allowed server that tries to start a workflow
        if(userHandle == null)
            userHandle = userhandle.getUserHandle(request,null/*items*/);
        if(userId == null && userHandle != null)
            userId = userhandle.getUserId(request,null/*items*/,userHandle);
        if(userEmail == null && userId != null)
            userEmail = userhandle.getEmailAddress(request,null/*items*/,userHandle,userId);
        
        logger.debug("RemoteAddr == {}",request.getRemoteAddr());
        logger.debug("Referer == {}",request.getHeader("referer"));
        logger.debug("ToolsProperties.wwwServer == {}",ToolsProperties.wwwServer);
        /* 20140918, to see whether AJAX calls come through.
        if(  request.getHeader("referer") == null 
          || (  !request.getHeader("referer").startsWith(ToolsProperties.wwwServer) 
             && !request.getHeader("referer").startsWith(ToolsProperties.wwwServer.replaceFirst("^(https|http)://", "$1://www."))
             )
          )
        //if(  !request.getRemoteAddr().equals(ToolsProperties.acceptedWorkflowStarter) 
        //  && !request.getRemoteAddr().equals("127.0.0.1")
         // )
            {
            logger.info("GET: Request sent from Unauthorized Client!");
            response.setStatus(403);
            throw new ServletException("Unauthorized");
            }
        */
        
        logger.debug("Calling tools, userHandle == {}",userHandle);
        response.setContentType("text/html; charset=UTF-8");
        response.setStatus(200);
        PrintWriter out = response.getWriter();
        /* 20130423
        if(userHandle == null && !goodPassword(request,response,null,out))
            return;
        */    
        if(!BracMat.loaded())
            {
            response.setStatus(500);
            throw new ServletException("Bracmat is not loaded. Reason:" + BracMat.reason());
            }

        //String workflowId = java.util.UUID.randomUUID().toString();

        @SuppressWarnings("unchecked")
        Enumeration<String> parmNames = (Enumeration<String>)request.getParameterNames();

        String arg = "(method.GET) (DATE." + workflow.quote(date) + ")"; // bj 20120801 "(action.GET)";
        boolean OK = true;
        for (Enumeration<String> e = parmNames ; e.hasMoreElements() ;) 
            {
            String parmName = e.nextElement();
            String vals[] = request.getParameterValues(parmName);
/*
https://clarin.dk/tools/start-job?workflow=SOMETHING&ids=dkclarin:168004,dkclarin:168028,dkclarin:168332 

I tools servicen vil du kunne udtrække parameteren "ids" som String, og ved at
bruge String indbyggede "spilt" funktion kan du så få et array af id'er. 
eSciDoc userhandlen vil så være gemt i http headeren, men kunne i princippet
også inkluderes som en parameter i url'en
*/
            if(parmName.equals("ids"))
                {
                for(int j = 0;j < vals.length;++j)
                    {
                    if(!vals[j].equals(""))
                        {
                        String[] items = vals[j].split(",");
                        int size = items.length;
                        for (int i=0; i<size; i++)
                            {
                            String resource = addInput(items[i],request,out,response,"item");
                            if(resource == null)
                                {
                                OK = false;
                                break;
                                }
                            else
                                arg = arg + resource;
                            }
                        }
                    }
                }
            else if(parmName.equals("item"))
                {
                for(int j = 0;j < vals.length;++j)
                    {
                    if(!vals[j].equals(""))
                        {
                        String resource = addInput(vals[j],request,out,response,parmName);
                        if(resource == null)
                            {
                            OK = false;
                            break;
                            }
                        else
                            arg = arg + resource;
                        }
                    }
                }
            /*
            else if(parmName.equals("Item"))
                {
                for(int j = 0;j < vals.length;++j)
                    {
                    String Val = unquote(vals[j]);
                    logger.debug("Item {} -> {}",vals[j],Val);
                    if(Val.equals(""))
                        {
                        String resource = checkInput(Val,request,out,response,parmName);
                        // should be a bit more quicker than addInput. Here we
                        // are already past the first post, but a cleanup
                        // theoretically could have wiped out the item, e.g.
                        // if the user is slowly progressing through the
                        // wizardry.
                        if(resource == null)
                            {
                            OK = false;
                            break;
                            }
                        else
                            arg = arg + resource;
                        }
                    }
                }
                */
            else if(parmName.equals("mail2"))
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
                        String LocalFileName = BracMat.Eval("storeUpload$("+workflow.quote("text") + "." + workflow.quote(date) + ")");
                        int textLength = vals[j].length();
                        
                        File file = new File(destinationDir,LocalFileName);
                        
                        PrintWriter outf = new PrintWriter(file);
                        outf.println(vals[j]); 
                        outf.close();                       
                        
                        arg = arg + " (FieldName,"      + workflow.quote("text")
                                  + ".Name,"            + workflow.quote("text")
                                  + ".ContentType,"     + workflow.quote("text/plain")
                                  + ".Size,"            + Long.toString(textLength)
                                  + ".DestinationDir,"  + workflow.quote(ToolsProperties.documentRoot /*+ ToolsProperties.stagingArea*//*DESTINATION_DIR_PATH*/)
                                  + ".LocalFileName,"   + workflow.quote(LocalFileName)
                                  + ")";
                        }
                    }
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

        if(OK)
            {
            createAndProcessPipeLine(response,arg,out,workflowRequest);
            }
        logger.debug("Calling tools DONE, userHandle == {}",userHandle);
        }

    public boolean PDFhasNoFonts(File pdfFile)
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
        logger.debug("lasterrline [" + lasterrline + "] lastline [" + lastline + "]");
        return lasterrline.equals("") && (lastline.endsWith("---------"));
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

        String arg = "(method.POST) (DATE." + workflow.quote(date) + ")"; // bj 20120801 "(action.POST)";

        try 
            {
            /*
            * Parse the request
            */
            Iterator<FileItem> itr = items.iterator();
            while(itr.hasNext()) 
                {
                logger.debug("in loop");
                FileItem item = itr.next();
                /*
                * Handle Form Fields.
                */
                if(item.isFormField()) 
                    {
                    logger.debug("Field Name = "+item.getFieldName()+", String = "+item.getString());
                    if(item.getFieldName().equals("text"))
                        {
                        String LocalFileName = BracMat.Eval("storeUpload$("+workflow.quote("text") + "." + workflow.quote(date) + ")");
                        int textLength = item.getString().length();
                        
                        File file = new File(destinationDir,LocalFileName);
                        item.write(file);
                        arg = arg + " (FieldName,"      + workflow.quote("text")
                                  + ".Name,"            + workflow.quote("text")
                                  + ".ContentType,"     + workflow.quote("text/plain")
                                  + ".Size,"            + Long.toString(textLength)
                                  + ".DestinationDir,"  + workflow.quote(ToolsProperties.documentRoot /*+ ToolsProperties.stagingArea*//*DESTINATION_DIR_PATH*/)
                                  + ".LocalFileName,"   + workflow.quote(LocalFileName)
                                  + ")";
                        }
                    else
                        arg = arg + " (" + workflow.quote(item.getFieldName()) + "." + workflow.quote(item.getString()) + ")";
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
                    logger.debug("hasNoPDFfonts ?");
                    logger.debug("ContentType :" + ContentType);
                    boolean hasNoPDFfonts = false;
                    if(  ContentType.equals("application/pdf") 
                      || ContentType.equals("application/x-download") 
                      || ContentType.equals("application/octet-stream") 
                      )
                        {
                        logger.debug("calling PDFhasNoFonts");
                        hasNoPDFfonts = PDFhasNoFonts(file);
                        logger.debug("hasNoPDFfonts " + (hasNoPDFfonts?"true":"false"));
                        }
                    arg = arg + " (FieldName,"      + workflow.quote(item.getFieldName())
                              + ".Name,"            + workflow.quote(item.getName())
                              + ".ContentType,"     + workflow.quote(item.getContentType()) + (hasNoPDFfonts ? " true" : "")
                              + ".Size,"            + Long.toString(item.getSize())
                              + ".DestinationDir,"  + workflow.quote(ToolsProperties.documentRoot /*+ ToolsProperties.stagingArea*//*DESTINATION_DIR_PATH*/)
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
        logger.debug("arg " + arg);
        return arg;
        }


    public void PostWorkflow(HttpServletRequest request,HttpServletResponse response,String workflowRequest) throws ServletException, IOException 
        {
        List<FileItem> items = userhandle.getParmList(request);
        if(userHandle == null)
            userHandle = userhandle.getUserHandle(request,items);
        if(userId == null && userHandle != null)
            userId = userhandle.getUserId(request,items,userHandle);
        if(userEmail == null && userId != null)
            userEmail = userhandle.getEmailAddress(request,items,userHandle,userId);
        PrintWriter out = response.getWriter();


        response.setContentType("text/html; charset=UTF-8");
        /*20130423
        if(userHandle == null && !goodPassword(request,response,items,out))
            return;
        */

        response.setStatus(200);
        logger.debug("doPost, RemoteAddr == {}",request.getRemoteAddr());
        String referer = request.getHeader("referer");
        logger.debug("Referer == {}",request.getHeader("referer"));
        logger.debug("ToolsProperties.wwwServer == {}",ToolsProperties.wwwServer);
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
        createAndProcessPipeLine(response,arg,out,workflowRequest);
        }

    /*
    Method doPost is called if Tools is used to process uploaded files.
     (In contrast to processing files copied from the repository.)
    */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
        {
        List<FileItem> items = userhandle.getParmList(request);
        if(userHandle == null)
            userHandle = userhandle.getUserHandle(request,items);
        if(userId == null && userHandle != null)
            userId = userhandle.getUserId(request,items,userHandle);
        if(userEmail == null && userId != null)
            userEmail = userhandle.getEmailAddress(request,items,userHandle,userId);
        PrintWriter out = response.getWriter();
        response.setContentType("text/xml");

        /*20130423
        if(userHandle == null && !goodPassword(request,response,items,out))
            return;
        */

        response.setStatus(200);
        // Check if it is the allowed server that tries to start a workflow
        logger.debug("doPost, RemoteAddr == {}",request.getRemoteAddr());
        
        if(  !request.getRemoteAddr().equals(ToolsProperties.acceptedWorkflowStarter) 
          && !request.getRemoteAddr().equals("127.0.0.1")
          )
            {
            logger.info("POST: Request sent from Unauthorized Client!");
            response.setStatus(403);
            throw new ServletException("Unauthorized");
            }
        String arg  = getParmsAndFiles(items,response,out);
        createAndProcessPipeLine(response,arg,out,"");
        }
    }

