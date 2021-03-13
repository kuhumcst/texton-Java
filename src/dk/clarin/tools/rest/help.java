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
import java.io.File;
import java.io.IOException;
import java.security.AccessControlException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.Enumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@SuppressWarnings("serial")
public class help extends HttpServlet 
    {
    // Static logger object.  
    private static final Logger logger = LoggerFactory.getLogger(help.class);

    private bracmat BracMat;

    public void init(javax.servlet.ServletConfig config) throws javax.servlet.ServletException 
        {
        logger.debug("init tools servlet");
        InputStream fis = config.getServletContext().getResourceAsStream("/WEB-INF/classes/properties.xml");
        ToolsProperties.readProperties(fis);        
        BracMat = new bracmat(ToolsProperties.bootBracmat);
        super.init(config);
        }

    public void doGet(HttpServletRequest request,HttpServletResponse response)
        throws ServletException, IOException 
        {
        logger.info("Calling texton/help");
        response.setContentType("text/html; charset=UTF-8");
        response.setStatus(200);
        if(!BracMat.loaded())
            {
            response.setStatus(500);
            throw new ServletException("Bracmat is not loaded. Reason:" + BracMat.reason());
            }
        // https://clarin.dk/texton/help?UIlanguage=da
        /*Test:*/
        @SuppressWarnings("unchecked")
        Enumeration<String> parmNames = (Enumeration<String>)request.getParameterNames();
        /*String UIlanguage = null;
        String usedonly = "Y";*/
        String arg = "";
        for (Enumeration<String> e = parmNames ; e.hasMoreElements() ;) 
            {
            String parmName = e.nextElement();
            String vals[] = request.getParameterValues(parmName);
            for(int j = 0;j < vals.length;++j)
                {
                arg = "(" + util.quote(parmName) + "." + util.quote(vals[j]) + ") " + arg;
            /*  if(parmName.equals("UIlanguage"))
                    {
                    UIlanguage = vals[j];
                    if(UIlanguage == null || UIlanguage.equals("null"))
                        {
                        UIlanguage="";
                        }
                    }
                else if(parmName.equals("usedonly"))
                    {
                    usedonly = vals[j];
                    if(usedonly == null || usedonly.equals("null"))
                        {
                        usedonly="j";
                        }
                    }*/
                }
            }
            /*
        if(UIlanguage == null || UIlanguage.equals("null"))
            {
            UIlanguage="";
            }*/

        PrintWriter out = response.getWriter();
        /**
          * help$
          */
//        String svar = BracMat.Eval("help$((UIlanguage." + util.quote(UIlanguage) + ") (usedonly." + util.quote(usedonly) + "))");
        String svar = BracMat.Eval("help$(" + arg + ")");
        out.println(svar);
        }
    }

