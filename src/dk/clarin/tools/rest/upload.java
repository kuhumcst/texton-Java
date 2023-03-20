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

import dk.clarin.tools.ToolsProperties;
import dk.clarin.tools.util;
import dk.clarin.tools.workflow;

import dk.cst.bracmat;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.Part;

import java.io.*;
import java.nio.charset.StandardCharsets;

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*

Method where asynchronous webservices can upload their output.

*/
@SuppressWarnings("serial")
@MultipartConfig(fileSizeThreshold=1024*1024*10,  // 10 MB 
                 maxFileSize=-1/*1024*1024*50*/,       // 50 MB
                 maxRequestSize=-1/*1024*1024*100*/)    // 100 MB

public class upload extends HttpServlet
    {
    private File tmpDir;
    private bracmat BracMat;
    private File destinationDir;
    private static final Logger logger = LoggerFactory.getLogger(upload.class);

    public void init(ServletConfig config) throws ServletException 
        {
        InputStream fis = config.getServletContext().getResourceAsStream("/WEB-INF/classes/properties.xml");
        ToolsProperties.readProperties(fis);        
        BracMat = new bracmat(ToolsProperties.bootBracmat);
        super.init(config);
        destinationDir = new File(ToolsProperties.documentRoot);
        if(!destinationDir.isDirectory()) 
            {
            throw new ServletException("Trying to set \"" + ToolsProperties.documentRoot + "\" as directory for temporary storing intermediate and final results, but this is not a valid directory.");
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
            Collection<Part> items = request.getParts();
            Iterator<Part> itr = items.iterator();
            Part theFile = null;
            while(itr.hasNext()) 
                {
                Part item = itr.next();
                /*
                * Handle Form Fields.
                */
                if(item.getSubmittedFileName() == null || item.getSubmittedFileName().equals("")) 
                    {
                    // We need the job parameter that indirectly tells us what local file name to give to the uploaded file.
                    arg = arg + " (\"" + util.escape(item.getName()) + "\".\"" + util.escape(IOUtils.toString(item.getInputStream(),StandardCharsets.UTF_8)) + "\")";
                    }
                else
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
                 *      201     Created
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
                    String messagetext = BracMat.Eval("getStatusCode$(\"404\".\"doPost:" + util.escape(LocalFileName) + "\")");
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
                    String statusCode = BracMat.Eval("parseStatusCode$(\"" + util.escape(LocalFileName) + "\")");
                    response.setStatus(Integer.parseInt(statusCode));
                    /**
                     * parsemessage$
                     *
                     * Find the text following the number greater than 100 immediately following the string 
                     * 'HTTP-status-code'
                     */
                    String messagetext = BracMat.Eval("parsemessage$(\"" + util.escape(LocalFileName) + "\")");
                    messagetext = BracMat.Eval("getStatusCode$(\"" + util.escape(statusCode) + "\".\"" + util.escape(messagetext) + "\")");
                    out.println(messagetext);
                    }
                else
                    {
                    try
                        {
                        theFile.write(ToolsProperties.documentRoot + LocalFileName);
                        }
                    catch(Exception ex) 
                        {
                        response.setStatus(500);
                        String messagetext = BracMat.Eval("getStatusCode$(\"500\".\"Tools cannot save uploaded file to " + util.escape(ToolsProperties.documentRoot + LocalFileName) + "\")");
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
                    String JobID = BracMat.Eval("uploadJobID$(" + arg + ")");
                    logger.info("JobNr {} JobID {}",JobNr,JobID);
                    util.gotToolOutputData(JobNr, JobID, BracMat, ToolsProperties.documentRoot + LocalFileName);
                    String result;
                    result = BracMat.Eval("goodRunningThreads$");
                    if(result.equals("y"))
                        {
                        Runnable runnable = new workflow(JobNr);
                        Thread thread = new Thread(runnable);
                        thread.start();
                        }
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
        catch(Exception ex) 
            {
            logger.error("An Exception {}",util.escape(ex.toString()));
            response.setStatus(500);
            String messagetext = BracMat.Eval("getStatusCode$(\"500\".\"doPost: Exception " + util.escape(ex.toString()) + "\")");
            out.println(messagetext);
            }
        }
    }
