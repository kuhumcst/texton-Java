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
            UIlanguage = userhandle.getPreferredLocale(request,items);
            if(UIlanguage != null && !UIlanguage.equals(""))
                arg = "(UIlanguage." + UIlanguage + ") " + arg;
            }
        
        return arg;
        }        

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
        logger.debug("getResourceAsString: methodString [{}]",methodString);
        org.apache.commons.httpclient.methods.GetMethod method = new org.apache.commons.httpclient.methods.GetMethod(methodString);
        //method.setFollowRedirects(false); // We don't want WAYF involved here
        method.setFollowRedirects(true); // 20150331

        // simuler du er en bruger med en handle, ved at sætte en cookie

        if((handle != null) && !handle.equals(""))
            {
            method.setRequestHeader("Cookie", "escidocCookie=" + handle);
            }

        try 
            {
            httpClient.executeMethod(method);
            int responseCode = method.getStatusCode();
            if(responseCode != 200) // 20161011
                {
                method.setFollowRedirects(false); // We don't want WAYF involved here
                httpClient.executeMethod(method);
                responseCode = method.getStatusCode();
                }

            if(responseCode == 200)
                {
                // request went OK
                try
                    {
                    //out.println("request went OK\n");
                    retval = method.getResponseBodyAsString();
                    }
                catch (IOException e)
                    {
                    response.setStatus(500);
                    String messagetext = BracMat.Eval("getStatusCode$(\"500\".\"I/O error while reading response stream after succesfully attempting method " + workflow.escapedquote(methodString) + " \")");
                    out.println(messagetext);
                    }
                }
            else 
                {
                response.setStatus(responseCode);
                String messagetext = BracMat.Eval("getStatusCode$("+workflow.quote(Integer.toString(responseCode))+".\"In getResourceAsString: When attempting method " + workflow.escapedquote(methodString) + " \")");
                out.println(messagetext);
                }
            }
        catch (IOException io)
            {
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
            // request went wrong
            try
                {
                retval = method.getResponseBodyAsString();
                if(retval != null)
                    out.println("method.getResponseBodyAsString returns [" + retval + "]");
                }
            catch (IOException e)
                {
                out.println("method.getResponseBodyAsString generates exception");
                }
            
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

        String[] idvisref = component.split("\\.");
        String id = idvisref[0];
        String visibility = idvisref[1];

        // Nyt HttpClient object
        org.apache.commons.httpclient.HttpClient httpClient = new org.apache.commons.httpclient.HttpClient();

        // Sæt timeoutværdi i millisekunder
        httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(600000);
        // Definer om det skal være POST, GET, PUT eller DELETE
        String methodString = ToolsProperties.repoServiceUrl + id;
        org.apache.commons.httpclient.methods.GetMethod method = new org.apache.commons.httpclient.methods.GetMethod(methodString);
        //method.setFollowRedirects(false); // We don't want WAYF involved here
        // simuler du er en bruger med en handle, ved at sætte en cookie
        if((handle != null) && !handle.equals(""))
            {
            method.setRequestHeader("Cookie", "escidocCookie=" + handle);
            }
        try 
            {
            httpClient.executeMethod(method);
            int responseCode = method.getStatusCode();
            if(responseCode == 200)
                {
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
                        File f = new File(destinationDir,LocalFileName);
                        OutputStream fout=new FileOutputStream(f);
                        byte buf[]=new byte[1024];
                        int len;
                        while((len=stroem.read(buf))>0)
                            fout.write(buf,0,len);
                        fout.close();
                        stroem.close();
                        }
                    catch (IOException e)
                        {
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

    public String gemLocalCopyOfResource(HttpServletRequest request,String id,PrintWriter out,HttpServletResponse response) // "59003"
        {
        String item = getResourceAsString(userHandle,id,out,response);
        if(item == null)
            {
            logger.debug("getResourceAsString({}) returns null",id);
            return null;
            }
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
            logger.debug("gemLocalCopyOfResource: getResourceAsString({}) returned non-null item",id);

            String component = BracMat.Eval("getComponentRef$(" + workflow.quote(item) + ")");
            //logger.debug("BracMat.Eval({}) returns component [{}]",workflow.quote(item),component);
            if(!component.equals(""))
                {
                if(getResourceAsStream(userHandle,component,out,response))
                    {
                    return item;
                    }
                else
                    {
                    logger.debug("getResourceAsStream({}) returns null",component);
                    return null;
                    }
                }
            else
                {
                logger.debug("component is null",component);
                return null;
                }
            }
        return null;
        }

    public boolean RightToLocalCopyOfResource(HttpServletRequest request,String id,PrintWriter out,HttpServletResponse response) // "59003"
        {
        String item = getResourceAsString(userHandle,id,out,response);
        if(item != null)
            {
            String component = BracMat.Eval("getComponentRef$(" + workflow.quote(item) + ")");
            return checkAccessRights(component);
            }
        else
            logger.debug("getResourceAsString( {}, {}, ...) has returned null",userHandle,id);
        return false;
        }

	private String theMimeType(String urladdr){
		try{
			URL url = new URL(urladdr);
			URLConnection urlConnection = url.openConnection();
		    String mimeType = urlConnection.getContentType();
            logger.debug("mimeType according to getContentType() {} is {}",urladdr,mimeType);
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
			return -1;
		}
	}
	

    private void createAndProcessPipeLine(HttpServletResponse response,String arg,PrintWriter out,String workflowRequest)
        {
        String result;
        /*
        String examplePage = webPage(ToolsProperties.wwwServer + "/clarindk/tools-" + (workflowRequest.equals("MetadataOnly") ? "metadata" : "upload" ) + ".jsp?lang=" + UIlanguage);
        arg = arg + " (examplePage." + workflow.quote(examplePage) + ")";
        //*/
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
            /*
            workflowRequest = GoalChoice | ToolChoice | MetadataOnly
            */
            result = BracMat.Eval("create" + workflowRequest + "$(" + arg + ")");
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
           
            
        logger.debug("Result from BracMat create$: ["+result+"]");
        int start = result.indexOf("<?"); // XML-output (XHTML)
        if(start < 0)
            start = result.indexOf("<!"); // HTML5-output
        if(start > 0)
            {
            logger.debug("Funny?");
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
    * Contructs an XACML document with request for each item we whish to access
    */
    private boolean checkAccessRights(String component) 
        {
        String[] idvisref = component.split("\\.");
        String textUrl = idvisref[2];
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

        // send the request to eSciDocs Policy Decision Point
        Document xacmlResponce = putToPDP(header + xacmlRequests + footer);
        if (xacmlResponce == null) 
            {
            // Some error happened, so we return null
            return false;
            }

        // Parse the XML to see if the user has access...
        NodeList results = xacmlResponce.getElementsByTagName("result");
        Element result = (Element) results.item(0);
        String decision = result.getAttribute("decision");
        return decision.equals("permit");
        }

    private Document putToPDP(String request) 
        {
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
        String seen = "hasCopy$(" + workflow.quote(val) + "." + workflow.quote(date) + ")";
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
        Workflow(request,response,"");
        }

    private String makeLocalCopyOfRemoteFile(String val)
        {
        if(!val.equals(""))
            {
            logger.debug("val == {}",val);

            String LocalFileName = BracMat.Eval("storeUpload$("+workflow.quote(val) + "." + workflow.quote(date) + ")");

            logger.debug("LocalFileName == {}",LocalFileName);

            File file = new File(destinationDir,LocalFileName);

            int textLength = webPageBinary(val,file);
            logger.debug("file size == {}",textLength);
            String ContentType = theMimeType(val);
            logger.debug("ContentType == {}",ContentType);
            if(!ContentType.equals(""))
                {
                    boolean hasNoPDFfonts = PDFhasNoFonts(file,ContentType);
                    return      " (FieldName,"      + workflow.quote("input")
                              + ".Name,"            + workflow.quote(val)
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
        if(userHandle == null)
            userHandle = userhandle.getUserHandle(request,null);
        if(userId == null && userHandle != null)
            userId = userhandle.getUserId(request,null,userHandle);
        if(userEmail == null && userId != null)
            userEmail = userhandle.getEmailAddress(request,null,userHandle,userId);
        if(UIlanguage == null)
            UIlanguage = userhandle.getPreferredLocale(request,null);
        
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
            if(parmName.equals("ids"))
                {
                logger.debug("ids");
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
                logger.debug("item");
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
            else if(parmName.equals("mail2"))
                {
                logger.debug("mail2");
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
                logger.debug("text");
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
                logger.debug("parmName.equals {}",parmName);
                for(int j = 0;j < vals.length;++j)
                    {
                    arg = arg + makeLocalCopyOfRemoteFile(vals[j]);
                    }
                }
            else
                {
                for(int j = 0;j < vals.length;++j)
                    {
                    logger.debug("({})",parmName);
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
                        logger.debug("item.getFieldName().equals {}",item.getFieldName());
                        arg = arg + makeLocalCopyOfRemoteFile(item.getString());
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
        List<FileItem> items = userhandle.getParmList(request);
        if(userHandle == null)
            userHandle = userhandle.getUserHandle(request,items);
        if(userId == null && userHandle != null)
            userId = userhandle.getUserId(request,items,userHandle);
        if(userEmail == null && userId != null)
            userEmail = userhandle.getEmailAddress(request,items,userHandle,userId);
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
        List<FileItem> items = userhandle.getParmList(request);
        if(userHandle == null)
            userHandle = userhandle.getUserHandle(request,items);
        if(userId == null && userHandle != null)
            userId = userhandle.getUserId(request,items,userHandle);
        if(userEmail == null && userId != null)
            userEmail = userhandle.getEmailAddress(request,items,userHandle,userId);
        if(UIlanguage == null)
            UIlanguage = userhandle.getPreferredLocale(request,items);
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

