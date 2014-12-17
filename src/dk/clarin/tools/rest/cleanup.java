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
import dk.clarin.tools.workflow;
import dk.cst.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Called as cron job:
 *      0 5 * * * curl https://clarin.dk/tools/cleanup > /dev/null
 */
@SuppressWarnings("serial")
public class cleanup extends HttpServlet 
    {
    // Static logger object.  
    private static final Logger logger = LoggerFactory.getLogger(cleanup.class);

    private File destinationDir;
    private bracmat BracMat;
    /// The eSciDoc userHandle
    //private String userHandle = null;
    /// The eSciDoc id of the user
    //private String userId;
    /// The users email
    //private String userEmail;

    //private String date;
    //private String toolsdataURL;



    public void init(javax.servlet.ServletConfig config) throws javax.servlet.ServletException 
        {
        logger.debug("init tools servlet");
        InputStream fis = config.getServletContext().getResourceAsStream("/WEB-INF/classes/properties.xml");
        ToolsProperties.readProperties(fis);		
        //Calendar cal = Calendar.getInstance();
        //SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        //date = sdf.format(cal.getTime());
        BracMat = new bracmat(ToolsProperties.bootBracmat);
        //toolsdataURL = ToolsProperties.baseUrlTools + ToolsProperties.stagingArea;
        super.init(config);
        destinationDir = new File(ToolsProperties.documentRoot /*+ ToolsProperties.stagingArea*/);
        if(!destinationDir.isDirectory()) 
            {
            throw new ServletException("Trying to set \"" + ToolsProperties.documentRoot /*+ ToolsProperties.stagingArea*/ + "\" as directory for temporary storing intermediate and final results, but this is not a valid directory.");
            }
        }

    public void doGet(HttpServletRequest request,HttpServletResponse response)
        throws ServletException, IOException 
        {
        //userHandle = null;
        //userId = null;
        //userEmail = null;
        logger.info("Calling tools/cleanup");
        response.setContentType("text/plain; charset=UTF-8");
        response.setStatus(200);
        if(!BracMat.loaded())
            {
            response.setStatus(500);
            throw new ServletException("Bracmat is not loaded. Reason:" + BracMat.reason());
            }

        PrintWriter out = response.getWriter();
        String[] chld = destinationDir.list();
        if(chld == null)
            {
            response.setStatus(404);
            //response.sendError(404,"File " + request.getPathInfo() + " does not exist.");
            throw new ServletException("File " + request.getPathInfo() + " does not exist.");
            }
        else
            {
            out.println("Files in staging area:");
            for(int i = 0; i < chld.length; i++)
                {
                String fileName = chld[i];

                File f = new File(destinationDir,fileName);

                // Make sure the file or directory exists and isn't write protected
                if (!f.exists())
                    {
                    out.println("Delete: no such file or directory: " + fileName);
                    }
                else if (!f.canWrite())
                    {
                    out.println("Delete: write protected: " + fileName);
                    }
                // If it is a directory, make sure it is empty
                else if (f.isDirectory()) 
                    {
                    out.println("Delete: directory not empty: " + fileName);
                    }
                else 
                    {
                    // Get the last modified time
                    long modifiedTime = f.lastModified();
                    // 0L is returned if the file does not exist
                    if(modifiedTime != 0L)
                        {
                        long now = System.currentTimeMillis();
                        long lifetime = Long.parseLong(ToolsProperties.deleteAfterMillisec.trim());
                        if(now - modifiedTime > lifetime)
                            {
                            /**
                             * keep$
                             *
                             * Check whether a result from a tool in the staging area can be deleted.
                             *
                             * Results that for some reason are needed by other tasks must be kept.
                             * The function looks for outstanding jobs that take the argument as input.
                             * Argument: file name, may be preceded by a slash
                             *      /19231210291
                             *
                             * NOTICE: If the file need not be kept, the file's name is deleted from
                             * several tables, so calling keep has side effects!
                             * Affected tables in jboss/server/default/data/tools:
                             *      jobs.table
                             *      Uploads.table
                             *      CTBs.table
                             *      relations.table
                             *      jobAbout.table
                             */
                            String svar = BracMat.Eval("keep$("+workflow.quote(fileName) + ")");
                            if(svar.equals("no"))
                                {
                                boolean success = f.delete();
                                if (success)
                                    {
                                    out.println(fileName + ": deleted");
                                    }
                                else
                                    {
                                    out.println(fileName + ": deletion failed");
                                    }
                                }
                            else
                                out.println(fileName + ": kept");
                            }
                        else
                            {
                            long count = lifetime / 86400000;
                            out.println("Delete: file " + fileName + " not older than " + count + " days");
                            }
                        }
                    else
                        out.println("Delete: file's modifiedTime == 0 " + fileName);
                    }
                }
            out.println("END");
            }
        chld = destinationDir.list();
        if(chld == null)
            {
            response.setStatus(404);
            //response.sendError(404,"File " + request.getPathInfo() + " does not exist.");
            throw new ServletException("File " + request.getPathInfo() + " does not exist.");
            }
        else
            {
            String arg = "";
            for(int i = 0; i < chld.length; i++)
                {
                String fileName = chld[i];
                arg += " " + workflow.quote(fileName);
                }
            /**
             * cleanup$
             *
             * Delete all references to files that no longer exist.
             * Argument: a list of the files that still exist in the Staging area.
             * Affected tables in jboss/server/default/data/tools:
             *      Uploads.table
             *      jobs.table
             *      CTBs.table
             *      relations.table
             *      jobAbout.table 
             */
            BracMat.Eval("cleanup$("+ arg + ")");
            }
        }
    }

