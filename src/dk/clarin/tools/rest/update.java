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
import dk.clarin.tools.userhandle;
import java.io.*;
import java.util.Enumeration;
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

    public String getarg(HttpServletRequest request, String name)
        {
        @SuppressWarnings("unchecked")
        Enumeration<String> parmNames = (Enumeration<String>)request.getParameterNames();
        logger.debug("Got some parmNames");

        for (Enumeration<String> e = parmNames ; e.hasMoreElements() ;) 
            {
            String parmName = e.nextElement();
            String vals[] = request.getParameterValues(parmName);
            for(int j = 0;j < vals.length;++j)
                {
                if(name != null && name.equals(parmName))
                    {
                    return vals[j];
                    }
                }
            }
        return null;
        }

    public String getAllArgs(HttpServletRequest request)
        {
        String arg = "";
        @SuppressWarnings("unchecked")
        Enumeration<String> parmNames = (Enumeration<String>)request.getParameterNames();
        logger.debug("Got some parmNames");

        for (Enumeration<String> e = parmNames ; e.hasMoreElements() ;) 
            {
            String parmName = e.nextElement();
            arg = arg + " (" + workflow.quote(parmName) + ".";
            String vals[] = request.getParameterValues(parmName);
            for(int j = 0;j < vals.length;++j)
                {
                arg += " " + workflow.quote(vals[j]) + "";
                }
            arg += ")";
            }
        return arg;
        }
        
    public void doGet(HttpServletRequest request,HttpServletResponse response)
        throws ServletException, IOException 
        {

        response.setContentType("text/html; charset=UTF-8");
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
            String userHandle = userhandle.getUserHandle(request,null);
            String userEmail = null;
            String passwordAsHandle = null;
            if(userHandle == null)
                {
                passwordAsHandle = getarg(request,"passwordAsHandle");
                logger.debug("getarg(request,\"passwordAsHandle\") returns:" + (passwordAsHandle == null ? "not found" : passwordAsHandle));
                /* 20140514 It is allowed to register a tool without being logged in or using a password, 
                            but the tool can only be made non-"Inactive" by you if you are logged-in.
                if(passwordAsHandle != null && passwordAsHandle.equals(ToolsProperties.password))
                */
                    {
                    //userEmail = request.getParameter("mail2");
                    userEmail = getarg(request,"mail2");
                    logger.debug("getarg(request,\"mail2\") returns:" + (userEmail == null ? "not found" : userEmail));
                    }                
                if(userEmail == null)
                    {
                    response.setStatus(401);
                    response.setContentType("text/html; charset=UTF-8");
                    StringBuilder html = new StringBuilder();
                    html.append("<html>");
                    html.append("<head>");
                    html.append("<title>Opdatering af registrerede oplysninger for et værktøj</title>");
                    html.append("</head>");
                    html.append("<body>");
                    html.append("<h1>Login krævet</h1>");
                    html.append("<p>Du skal være logget ind for at kunne opdatere oplysninger for et værktøj.<a href=\"" + 
                        ToolsProperties.baseUrlTools + "/aa/login?target=" + ToolsProperties.baseUrlTools + 
                        "/clarindk/login?target=" + "/tools/update" + "\">Klik her for at logge ind</a>.</p>");
                        //"/tools/update" + "\">Klik her for at logge ind</a>.</p>");
                    html.append("</body>");
                    html.append("</html>");

                    out.println(html.toString());
                    return;
                    }
                }
            else
                {
                logger.debug("userHandle = {}",userHandle);

                String userId = userhandle.getUserId(request,null,userHandle);
                userEmail = userhandle.getEmailAddress(request,null,userHandle,userId);
                }
                
            logger.info("userHandle = {}",userHandle);

            String arg = "";
            if(userEmail != null)
                arg = " (contactEmail." + workflow.quote(userEmail) + ")";
            arg += getAllArgs(request);

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

