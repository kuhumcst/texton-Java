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
import dk.clarin.tools.util;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
@MultipartConfig(fileSizeThreshold=1024*1024*10,  // 10 MB 
                 maxFileSize=1024*1024*50,       // 50 MB
                 maxRequestSize=1024*1024*100)    // 100 MB

public class importMetadata extends HttpServlet 
    {
    private bracmat BracMat;

    public void init(ServletConfig config) throws ServletException 
        {
        BracMat = new bracmat(ToolsProperties.bootBracmat);
        super.init(config);
        }

    public void doGet(HttpServletRequest request,HttpServletResponse response)
        throws ServletException, IOException 
        {
        response.setContentType("text/html; charset=UTF-8");
        response.setStatus(200);
        PrintWriter out = response.getWriter();
        
        String password = request.getParameter("password");
        if(BracMat.loaded())
            {
            if(password == null || !util.goodToPass(password,BracMat))
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
                            +"<p>When attempting to evaluate Bracmat code</p>\n" 
                            +"</body></html>\n"
                           );
                return;
                }

            @SuppressWarnings("unchecked")
            /*
             * importTables$
             *
             * importMetadata all tables from a file. 
             * Default alltables.GPL
             *
             */

            String expression = request.getParameter("import");
            String proddata = request.getParameter("proddata");
            if(proddata == null)
                proddata = "";
            else
                proddata = "proddata";

            if(expression != null && !expression.equals(""))
                {
                String result = BracMat.Eval("importTables$("+util.quote(proddata)+"."+util.quote(expression)+")");
                response.setContentType("text/plain");
                out.println(result);
                }
            else
                {
                response.setContentType("text/plain");
                out.println("NO INPUT");
                }
            }
        else
            {
            throw new ServletException("Bracmat is not loaded. Reason:" + BracMat.reason());
            }
        }
    }

