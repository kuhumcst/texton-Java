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

import dk.cst.*;
import dk.clarin.tools.ToolsProperties;
import dk.clarin.tools.workflow;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.zip.*;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


/**
  * results
  *
  * Show links to all results, intermediary or not.
  *
  */

@SuppressWarnings("serial")
public class zipresults extends HttpServlet 
    {
    private static final Logger logger = LoggerFactory.getLogger(zipresults.class);
    private String date;
    private bracmat BracMat;

    public void init(ServletConfig config) throws ServletException 
        {
        InputStream fis = config.getServletContext().getResourceAsStream("/WEB-INF/classes/properties.xml");
        ToolsProperties.readProperties(fis);        
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        date = sdf.format(cal.getTime());
        BracMat = new bracmat(ToolsProperties.bootBracmat);
        super.init(config);
        }

    public void doGetZip(String localFilePath, String fileName,HttpServletResponse response)
        throws ServletException, IOException 
        {
        response.setStatus(200);

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition","attachment;filename=\"" + fileName + "\"");
        try
            {
            File f = new File(localFilePath+fileName);
            int nLen = 0;
            OutputStream out;
            FileInputStream in;
            in = new FileInputStream(f);
            out = response.getOutputStream();
            byte[] bBuf = new byte[1024];
            try
                {
                while ((nLen = in.read(bBuf, 0, 1024)) != -1)
                    {
                    out.write(bBuf, 0, nLen);
                    }
                }
            finally
                {
                in.close();
                }
            /**
             * keep$
             * 
             * Check whether a result from a tool in the staging area can be
             * deleted.
             * 
             * Results that for some reason are needed by other tasks must
             * be kept. The function looks for outstanding jobs that take
             * the argument as input. Argument: file name, may be preceded
             * by a slash /19231210291
             * 
             * NOTICE: If the file need not be kept, the file's name is
             * deleted from several tables, so calling keep has side
             * effects! Affected tables in jboss/server/default/data/tools:
             * jobs.table Uploads.table CTBs.table relations.table
             * jobAbout.table
             */
            /* 2016.06.22 We decided not to delete results from the server 
               immediately after they have been fetched by the user.
               The cleanup service will delete the data.
            
            String svar = BracMat.Eval("keep$("+workflow.quote(fileName) + ")");
            if(svar.equals("no"))
                {
                boolean success = f.delete();
                }
            */
            }
        catch (FileNotFoundException e)
            {
            response.setContentType("text/html; charset=UTF-8");
            response.setStatus(404);
            PrintWriter out = response.getWriter();
            if(fileName.startsWith("/"))
                fileName = fileName.substring(1);
            out.println("File " + fileName + " is no longer accessible.");
            }
            
        }

    public void doPost(HttpServletRequest request,HttpServletResponse response)
        throws ServletException, IOException 
        {
        response.setContentType("text/html; charset=UTF-8");
        response.setStatus(200);
        if(BracMat.loaded())
            {
            @SuppressWarnings("unchecked")
            Enumeration<String> parmNames = (Enumeration<String>)request.getParameterNames();

            String job;
            for (Enumeration<String> e = parmNames ; e.hasMoreElements() ;) 
                {
                String parmName = e.nextElement();
                if(parmName.equals("JobNr"))
                    {
                    job = request.getParameterValues(parmName)[0];
                    String metadata = BracMat.Eval("jobMetaDataAsHTML$(" + job + ")");
                    String filelist = "";
                    String letter = BracMat.Eval("letter$(" + job + ")");
                    String readme = BracMat.Eval("readme$(" 
                                                + job 
                                                + "." 
                                                + workflow.quote(date) 
                                                + "." 
                                                + workflow.quote(letter) 
                                                + ")");
                    String localFilePath = ToolsProperties.documentRoot;
                    String toolsdataURL = BracMat.Eval("toolsdataURL$");
                    String Body = null;
                    
                    FileOutputStream zipdest = null;
                    ZipOutputStream zipout = null;
                    boolean hasFiles = false;
                    if(letter.startsWith("file:"))
                        {
                        hasFiles = true;
                        zipdest = new FileOutputStream(localFilePath + job + ".zip");
                        zipout = new ZipOutputStream(new BufferedOutputStream(zipdest));
                        while(letter.startsWith("file:"))
                            {
                            int end = letter.indexOf(";");
                            String filename = letter.substring(5,end);
                            String zipname = filename;
                            int zipnameStart = filename.indexOf("*");
                            if(zipnameStart > 0)
                                {
                                zipname = filename.substring(zipnameStart+1);
                                filename = filename.substring(0,zipnameStart);
                                }
                            workflow.zip(localFilePath + filename,zipname,zipout);
                            letter = letter.substring(end+1);
                            }
                        }
                    else if(letter.startsWith("metadata:"))
                        {
                        String MetaData = BracMat.Eval("metadataAsXML$(" + job + "." + workflow.quote(date) + ")");
                        hasFiles = true;
                        zipdest = new FileOutputStream(localFilePath + job + ".zip");
                        zipout = new ZipOutputStream(new BufferedOutputStream(zipdest));
                        int end = letter.indexOf(";");
                        String type = letter.substring(9,end);
                        workflow.zipstring(type + ".xml",zipout,MetaData);
                        letter = letter.substring(end+1);
                        }

                    if(hasFiles)
                        {
                        workflow.zipstring("readme.txt",zipout,readme);
                        workflow.zipstring("index.html",zipout,letter);
                        zipout.close();
                        }
                    doGetZip(localFilePath, job + ".zip",response);                            
                    
                    break;
                    }
                }
            }
        else
            {
            throw new ServletException("Bracmat is not loaded. Reason:" + BracMat.reason());
            }
        }

        public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
            {        
            doPost(request, response);
            }
    }

