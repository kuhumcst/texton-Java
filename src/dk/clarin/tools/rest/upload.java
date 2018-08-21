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
package dk.clarin.tools.rest;

import dk.cst.bracmat;
import dk.clarin.tools.ToolsProperties;
import dk.clarin.tools.workflow;
import java.io.*;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/*

Method where asynchronous webservices can upload their output.

*/
@SuppressWarnings("serial")
public class upload extends HttpServlet
    {
    private File tmpDir;
    private bracmat BracMat;
    private File destinationDir;

    public void init(ServletConfig config) throws ServletException 
        {
        InputStream fis = config.getServletContext().getResourceAsStream("/WEB-INF/classes/properties.xml");
        ToolsProperties.readProperties(fis);        
        BracMat = new bracmat(ToolsProperties.bootBracmat);
        super.init(config);
        destinationDir = new File(ToolsProperties.documentRoot /*+ ToolsProperties.stagingArea*/);
        if(!destinationDir.isDirectory()) 
            {
            throw new ServletException("Trying to set \"" + ToolsProperties.documentRoot /*+ ToolsProperties.stagingArea*/ + "\" as directory for temporary storing intermediate and final results, but this is not a valid directory.");
            }
        }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
        {
        PrintWriter out = response.getWriter();
        response.setContentType("text/xml");
        response.setStatus(200);
        if(!BracMat.loaded())
            {
            response.setStatus(500);
            throw new ServletException("Bracmat is not loaded. Reason:" + BracMat.reason());
            }

        DiskFileItemFactory  fileItemFactory = new DiskFileItemFactory ();
        /*
        *Set the size threshold, above which content will be stored on disk.
        */
        fileItemFactory.setSizeThreshold(1*1024*1024); //1 MB
        /*
        * Set the temporary directory to store the uploaded files of size above threshold.
        */
        fileItemFactory.setRepository(tmpDir);

        String arg = "(method.POST)"; // bj 20120801 "(action.POST)";

        ServletFileUpload uploadHandler = new ServletFileUpload(fileItemFactory);
        try 
            {
            /*
            * Parse the request
            */
            @SuppressWarnings("unchecked")
            List<FileItem> items = (List<FileItem>)uploadHandler.parseRequest(request);
            Iterator<FileItem> itr = items.iterator();
            FileItem theFile = null;
            while(itr.hasNext()) 
                {
                FileItem item = (FileItem) itr.next();
                /*
                * Handle Form Fields.
                */
                if(item.isFormField()) 
                    {
                    // We need the job parameter that indirectly tells us what local file name to give to the uploaded file.
                    arg = arg + " (\"" + workflow.escape(item.getFieldName()) + "\".\"" + workflow.escape(item.getString()) + "\")";
                    }
                else if(item.getName() != "")
                    {
                    //Handle Uploaded file.
                    if(theFile != null)
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
                        String messagetext = BracMat.Eval("getStatusCode$(\"400\".\"Too many files uploaded\")");
                        out.println(messagetext);
                        return;
                        }
                    theFile = item;
                    }
                }
            if(theFile != null)
                {
                /*
                * Write file to the ultimate location.
                */
                /**
                 * upload$
                 *
                 * Make a waiting job non-waiting upon receipt of a result from an 
                 * asynchronous tool.
                 *
                 * Analyze the job parameter. It tells to which job the sent file belongs.
                 * The jobs table knows the file name and location for the uploaded file.
                 *              (Last field)
                 * Input:
                 *      List of HTTP request parameters.
                 *      One of the parameters must be (job.<jobNr>-<jobID>)
                 *
                 * Output:
                 *      The file name that must be given to the received file when saved in
                 *      the staging area.
                 *
                 * Status codes:
                 *      200     ok
                 *      400     'job' parameter does not contain hyphen '-' or
                 *              'job' parameter missing altogether.
                 *      404     Job is not expecting a result (job is not waiting)
                 *              Job is unknown
                 *      500     Job list could not be read
                 *
                 * Affected tables:
                 *      jobs.table
                 */
                String LocalFileName = BracMat.Eval("upload$(" + arg + ")");
                if(LocalFileName == null)
                    {
                    response.setStatus(404);
                    String messagetext = BracMat.Eval("getStatusCode$(\"404\".\"doPost:" + workflow.escape(LocalFileName) + "\")");
                    out.println(messagetext);
                    }
                else if(LocalFileName.startsWith("HTTP-status-code"))
                    {
                    /**
                     * parseStatusCode$
                     *
                     * Find the number greater than 100 immediately following the string 
                     * 'HTTP-status-code'
                     */
                    String statusCode = BracMat.Eval("parseStatusCode$(\"" + workflow.escape(LocalFileName) + "\")");
                    response.setStatus(Integer.parseInt(statusCode));
                    /**
                     * parsemessage$
                     *
                     * Find the text following the number greater than 100 immediately following the string 
                     * 'HTTP-status-code'
                     */
                    String messagetext = BracMat.Eval("parsemessage$(\"" + workflow.escape(LocalFileName) + "\")");
                    messagetext = BracMat.Eval("getStatusCode$(\"" + workflow.escape(statusCode) + "\".\"" + workflow.escape(messagetext) + "\")");
                    out.println(messagetext);
                    }
                else
                    {
                    File file = new File(destinationDir,LocalFileName);
                    try
                        {
                        theFile.write(file);
                        }
                    catch(Exception ex) 
                        {
                        response.setStatus(500);
                        String messagetext = BracMat.Eval("getStatusCode$(\"500\".\"Tools cannot save uploaded file to " + workflow.escape(destinationDir + LocalFileName) + "\")");
                        out.println(messagetext);
                        return;
                        }
                    /**
                     * uploadJobNr$
                     *
                     * Return the string preceding the hyphen in the input.
                     *
                     * Input: <jobNr>-<jobID>
                     */
                    String JobNr = BracMat.Eval("uploadJobNr$(" + arg + ")");
                    //Runnable runnable = new workflow(JobNr, destinationDir);
                    Runnable runnable = new workflow(JobNr);
                    Thread thread = new Thread(runnable);
                    thread.start();
                    response.setStatus(201);
                    String messagetext = BracMat.Eval("getStatusCode$(\"201\".\"\")");
                    out.println(messagetext);
                    }
                }
            else
                {
                response.setStatus(400);
                String messagetext = BracMat.Eval("getStatusCode$(\"400\".\"No file uploaded\")");
                out.println(messagetext);
                }
            }
        catch(FileUploadException ex) 
            {
            response.setStatus(500);
            String messagetext = BracMat.Eval("getStatusCode$(\"500\".\"doPost: FileUploadException " + workflow.escape(ex.toString()) + "\")");
            out.println(messagetext);
            }
        catch(Exception ex) 
            {
            response.setStatus(500);
            String messagetext = BracMat.Eval("getStatusCode$(\"500\".\"doPost: Exception " + workflow.escape(ex.toString()) + "\")");
            out.println(messagetext);
            }
        }
    }
