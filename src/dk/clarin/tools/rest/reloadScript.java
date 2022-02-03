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
//package dk.clarin.tools.rest;
// Make sure that bracmatjni.dll is in java.library.path
// Compile with
//      javac bracmatjni.java
// Create header file with
//      javah -jni bracmatjni
// compile and link project D:\projects\Bracmat\vc\bracmatdll\bracmatdll.vcproj
// Run with
//      java bracmatjni
package dk.clarin.tools.rest;

import dk.clarin.tools.ToolsProperties;
import dk.clarin.tools.util;
import dk.cst.bracmat;
import java.io.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Called when a new version of toolsProg.bra is available in 
 * jboss/server/default/data/tools
 * Calling this service hot-deploys the Bracmat code and does not affect
 * pending jobs.
 * As a side effect, all variables that are not properly declared as local
 * variables and that therefore exist as global variables, are deleted.
 */      

@SuppressWarnings("serial")
public class reloadScript extends HttpServlet 
    {
    //private static final Logger logger = LoggerFactory.getLogger(reloadScript.class);

    public void init(javax.servlet.ServletConfig config) throws javax.servlet.ServletException 
        {
        InputStream fis = config.getServletContext().getResourceAsStream("/WEB-INF/classes/properties.xml");
        ToolsProperties.readProperties(fis);    
        super.init(config);
        }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException 
        {
        response.setContentType("text/xml");
        response.setStatus(200);
        PrintWriter out = response.getWriter();
        String password = request.getParameter("password");
        if(password == null || !util.hexDigest(password,"SHA-256").equals(ToolsProperties.password))
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
        bracmat BracMat = new bracmat(ToolsProperties.bootBracmat);
        if(BracMat.loaded())
            {
            String result = BracMat.Eval("("+ToolsProperties.bootBracmat+")&clean$");
            response.setContentType("text/plain");
            out.println(result);
            }
        else
            {
            response.setStatus(503);
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
