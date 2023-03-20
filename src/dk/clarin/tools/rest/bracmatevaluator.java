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

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
@MultipartConfig(fileSizeThreshold=1024*1024*10,  // 10 MB 
                 maxFileSize=-1/*1024*1024*50*/,       // 50 MB
                 maxRequestSize=-1/*1024*1024*100*/)    // 100 MB

public class bracmatevaluator extends HttpServlet 
    {
    private static final Logger logger = LoggerFactory.getLogger(bracmatevaluator.class);

    private bracmat BracMat;

    public void init(jakarta.servlet.ServletConfig config) throws jakarta.servlet.ServletException 
        {
        InputStream fis = config.getServletContext().getResourceAsStream("/WEB-INF/classes/properties.xml");
        ToolsProperties.readProperties(fis);    
        super.init(config);
        }


/* 20220811 Remove evaluation of general Bracmat expressions. Show Bracmat version instead. */

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException 
        {
        response.setContentType("text/xml");
        response.setStatus(200);
        bracmat BracMat = new bracmat(ToolsProperties.bootBracmat);
        if(BracMat.loaded())
            {
            String password = request.getParameter("password");
            util.PBKDF2string(password);
            if(password == null || !util.goodToPass(password,BracMat))
                {
                response.setStatus(401);
                PrintWriter out = response.getWriter();
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
//            String expression = request.getParameter("expression");
            String expression = "!v"; // show Bracmat version
            if(expression != null && !expression.equals(""))
                {
                response.setContentType("text/html;charset=UTF-8");
                logger.info("Eval:"+expression);
                String result = BracMat.Eval(expression);
                logger.info("Result:"+result);
                PrintWriter out = response.getWriter();
                out.println(result);
                }
            else
                {
                response.setContentType("text/plain");
                PrintWriter out = response.getWriter();
                out.println("NO INPUT");
                }
            }
        else
            {
            response.setStatus(503);
            PrintWriter out = response.getWriter();
            out.println( "<?xml version=\"1.0\"?>\n"
                        +"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" 
                        +"<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"da\" lang=\"da\">\n" 
                        +"<head>\n" 
                        +"<title>DK-Clarin: Tools</title>\n" 
                        +"<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />\n" 
                        +"</head>\n" 
                        +"<body>\n" 
                        +"<h1>503 Service Unavailable</h1>\n" 
                        +"<p>Reason: " + BracMat.reason() + "</p>\n" 
                        +"</body></html>\n"
                       );
            }
        }
    }
