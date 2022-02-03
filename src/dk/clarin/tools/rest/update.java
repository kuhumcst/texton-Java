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
import dk.clarin.tools.parameters;
import java.io.*;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class update extends HttpServlet 
    {
    private static final Logger logger = LoggerFactory.getLogger(register.class);
    private bracmat BracMat;

    public void init(ServletConfig config) throws ServletException 
        {
        InputStream fis = config.getServletContext().getResourceAsStream("/WEB-INF/classes/properties.xml");
        ToolsProperties.readProperties(fis);        
        BracMat = new bracmat(ToolsProperties.bootBracmat);
        super.init(config);
        }

    public void doGet(HttpServletRequest request,HttpServletResponse response)
        throws ServletException, IOException 
        {
        response.setContentType("text/html; charset=iso-8859-1");//UTF-8");
        response.setStatus(200);
        PrintWriter out = response.getWriter();
        if(BracMat.loaded())
            {
            /**
             * update$
             *
             * Create an HTML form that allows a user to choose a tool from a pick list.
             * The user must also fill out a password.
             *
             * The input can either be an empty string or two fields: a message to be 
             * displayed above the form and the name of a tool, which will then be 
             * selected when the browser shows the pick list.
             */

            String userEmail = null;
            String passwordAsHandle = null;
            String arg = "";
            passwordAsHandle = parameters.getGETarg(request,"passwordAsHandle");
            logger.debug("getGETarg(request,\"passwordAsHandle\") returns:" + (passwordAsHandle == null ? "not found" : passwordAsHandle));
            if(passwordAsHandle != null && util.hexDigest(passwordAsHandle,"SHA-256").equals(ToolsProperties.password))
                {
                logger.debug("Password ok for activating registered tools. Add 'handle' to list of arguments");
                userEmail = parameters.getGETarg(request,"mail2");
                arg += " (handle." + util.quote(passwordAsHandle) + ")";
                }
            else
                {
                logger.debug("Password [{}] not ok for activating registered tools. Must be [{}]",
                passwordAsHandle,ToolsProperties.password);
                }
                
            logger.debug("userEmail = {}",userEmail);

            if(userEmail != null && parameters.getGETarg(request,"contactEmail") == null)
                arg += " (contactEmail." + util.quote(userEmail) + ")";
            arg += parameters.getAllGETArgsBracmatFormat(request);

            String result = BracMat.Eval("update$(" + arg + ")");
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
                String messagetext = BracMat.Eval("getStatusCode$(\"404\".\"Update failed\")");
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

