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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.InputStream;

import java.util.Enumeration;

@SuppressWarnings("serial")
@MultipartConfig(fileSizeThreshold=1024*1024*10,  // 10 MB 
                 maxFileSize=-1/*1024*1024*50*/,       // 50 MB
                 maxRequestSize=-1/*1024*1024*100*/)    // 100 MB

public class exportMetadata extends HttpServlet 
    {
    private bracmat BracMat;

    public void init(ServletConfig config) throws ServletException 
        {
        InputStream fis = config.getServletContext().getResourceAsStream("/WEB-INF/classes/properties.xml");
        ToolsProperties.readProperties(fis);    
        super.init(config);
        }

    public void doGet(HttpServletRequest request,HttpServletResponse response)
        throws ServletException, IOException 
        {
        response.setContentType("text/html; charset=UTF-8");
        response.setStatus(200);
        PrintWriter out = response.getWriter();        
        String password = request.getParameter("password");
        bracmat BracMat = new bracmat(ToolsProperties.bootBracmat);
        if(BracMat.loaded())
            {
            if(password == null || !util.goodToPass(password,BracMat))
                {
                response.setStatus(401);
                out.println( "<!DOCTYPE html>\n" 
                            +"<html xml:lang=\"en\" lang=\"en\">\n" 
                            +"<head>\n" 
                            +"<title>DK-Clarin: Tools</title>\n" 
                            +"<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />\n" 
                            +"</head>\n" 
                            +"<body>\n" 
                            +"<h1>401 Unauthorized</h1>\n" 
                            +"<p>When attempting to export data.</p>\n" 
                            +"</body></html>\n"
                           );
                return;
                }

            @SuppressWarnings("unchecked")
            /*
             * exportMetadata$
             *
             * exportMetadata all tables to a file called  alltables.....
             * (Where ..... can be chosen freely.)
             *
             */
            String expression = request.getParameter("export");
            if(expression == null)
                expression = "";
            String proddata = request.getParameter("proddata");
            if(proddata == null)
                proddata = "";
            else
                proddata = "proddata";
            String result = BracMat.Eval("exportTables$("+util.quote(proddata)+"."+util.quote(expression)+")");
            if(result == null || result.equals(""))
                {
                response.setStatus(404);
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
                String messagetext = BracMat.Eval("getStatusCode$(\"404\".\"exportMetadata failed\")");
                out.println(messagetext);
                return;
                }
            out.println(result);
            }
        else
            {
            throw new ServletException("Bracmat is not loaded. Reason:" + BracMat.reason());
            }
        }
    }

