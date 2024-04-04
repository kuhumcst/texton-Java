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

import dk.cst.bracmat;

import jakarta.servlet.ServletConfig; 
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.InputStream;

import java.security.AccessControlException;

import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Called as cron job:
 *      0 5 * * * curl https://clarin.dk/texton/cleanup > /dev/null
 */
@SuppressWarnings("serial")
@MultipartConfig(fileSizeThreshold=1024*1024*10,  // 10 MB 
                 maxFileSize=-1/*1024*1024*50*/,       // 50 MB
                 maxRequestSize=-1/*1024*1024*100*/)    // 100 MB

public class cleanup extends HttpServlet 
    {
    // Static logger object.  
    private static final Logger logger = LoggerFactory.getLogger(cleanup.class);

    private bracmat BracMat;

    public void init(ServletConfig config) throws ServletException 
        {
        InputStream fis = config.getServletContext().getResourceAsStream("/WEB-INF/classes/properties.xml");
        ToolsProperties.readProperties(fis);        
        //Calendar cal = Calendar.getInstance();
        //SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        //date = sdf.format(cal.getTime());
        BracMat = new bracmat(ToolsProperties.bootBracmat);
        super.init(config);
        }

    public void doGet(HttpServletRequest request,HttpServletResponse response)
        throws ServletException, IOException 
        {
        logger.info("Calling tools/cleanup");
        response.setContentType("text/plain; charset=UTF-8");
        response.setStatus(200);
        if(!BracMat.loaded())
            {
            response.setStatus(500);
            throw new ServletException("Bracmat is not loaded. Reason:" + BracMat.reason());
            }

        File destinationDir = new File(ToolsProperties.documentRoot /*+ ToolsProperties.stagingArea*/);
        if(!destinationDir.isDirectory()) 
            {
            throw new ServletException("Trying to set \"" + ToolsProperties.documentRoot /*+ ToolsProperties.stagingArea*/ + "\" as directory for temporary storing intermediate and final results, but this is not a valid directory.");
            }

        PrintWriter out = response.getWriter();
        String[] chld = null;
        try
            {
            chld = destinationDir.list();
            }
        catch(AccessControlException e)
            {
            logger.error("destinationDir.list() causes java.security.AccessControlException, error is: " + e.getMessage());
            }
        catch(SecurityException e)
            {
            logger.error("destinationDir.list() causes SecurityException, error is: " + e.getMessage());
            }
        catch(Exception e)
            {
            logger.error("destinationDir.list() causes Exception, error is: " + e.getMessage());
            }
        if(chld == null)
            {
            response.setStatus(404);
            //response.sendError(404,"File " + request.getPathInfo() + " does not exist.");
            throw new ServletException("destinationDir " + destinationDir.getPath() + ": list() returns NULL.");
            }
        else
            {
            @SuppressWarnings("unchecked")
            Enumeration<String> parmNames = (Enumeration<String>)request.getParameterNames();

            String jobNr = "";
            for (Enumeration<String> e = parmNames ; e.hasMoreElements() ;) 
                {
                String parmName = e.nextElement();
                if(parmName.equals("JobNr"))
                    jobNr = request.getParameterValues(parmName)[0];
                }
            BracMat.Eval("readJobTables$");
            out.println("Files in staging area:");
            for(String fileName : chld)
                {
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
                else if(jobNr.equals(""))
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
                            String svar = BracMat.Eval("keep$("+util.quote(fileName) + ".)");
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
                            if(count >= 2)
                                out.println("Delete: file " + fileName + " not older than " + count + " days");
                            else
                                {
                                count = lifetime / 3600000;
                                if(count >= 3)
                                    out.println("Delete: file " + fileName + " not older than " + count + " hours");
                                else
                                    {
                                    count = lifetime / 60000;
                                    if(count >= 5)
                                        out.println("Delete: file " + fileName + " not older than " + count + " minutes");
                                    else
                                        {
                                        count = lifetime / 1000;
                                        if(count >= 5)
                                            out.println("Delete: file " + fileName + " not older than " + count + " seconds");
                                        else
                                            out.println("Delete: file " + fileName + " not older than " + lifetime + " milliseconds");
                                        }
                                    }
                                }
                            }
                        }
                    else
                        out.println("Delete: file's modifiedTime == 0 " + fileName);
                    }
                else
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
                        */
                    String svar = BracMat.Eval("keep$("+util.quote(fileName) + "." + util.quote(jobNr) + ")");
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
                    }
                }
            out.println("END");
            BracMat.Eval("saveJobTables$");
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
            for(String fileName : chld)
                {
                arg += " " + util.quote(fileName);
                }
            /**
             * cleanup$
             *
             * Delete all references to files that no longer exist.
             * Argument: a list of the files that still exist in the Staging area.
             * Affected tables in BASE/job:
             *      Uploads
             *      jobs
             *      CTBs
             *      jobAbout
             *      ItemGroupsCache
             */
            BracMat.Eval("cleanup$("+ arg + ")");
            }
        }
    }

