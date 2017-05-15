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
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.apache.commons.mail.SimpleEmail;

@SuppressWarnings("serial")
public class register extends HttpServlet 
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

    public String getarg(HttpServletRequest request, List<FileItem> items, String name)
        {
        /*
        * Parse the request
        */

        @SuppressWarnings("unchecked")
        boolean is_multipart_formData = ServletFileUpload.isMultipartContent(request);

        logger.debug("is_multipart_formData:"+(is_multipart_formData ? "ja" : "nej"));
        
        if(is_multipart_formData)
            {
            try 
                {
                Iterator<FileItem> itr = items.iterator();
                while(itr.hasNext()) 
                    {
                    FileItem item = (FileItem) itr.next();
                    if(item.isFormField()) 
                        {
                        if(name.equals(item.getFieldName()))
                            return item.getString("UTF-8").trim();
                        }
                    }
                }
            catch(Exception ex) 
                {
                logger.error("uploadHandler.parseRequest Exception");
                }
            }
        
        @SuppressWarnings("unchecked")
        Enumeration<String> parmNames = (Enumeration<String>)request.getParameterNames();
        for (Enumeration<String> e = parmNames ; e.hasMoreElements() ;) 
            {
            String parmName = e.nextElement();
            String vals[] = request.getParameterValues(parmName);
            for(int j = 0;j < vals.length;++j)
                {
                if(name.equals(parmName))
                    {
                    logger.debug("parmName:"+parmName+" equals:"+name+" , return "+vals[j]);
                    return vals[j];
                    }
                }
            }
        return null;
        }
        
    public String getargs(HttpServletRequest request, List<FileItem> items)
        {
        String arg = "";
        /*
        * Parse the request
        */

        @SuppressWarnings("unchecked")
        Enumeration<String> parmNames = (Enumeration<String>)request.getParameterNames();
        boolean is_multipart_formData = ServletFileUpload.isMultipartContent(request);

        logger.debug("is_multipart_formData:"+(is_multipart_formData ? "ja" : "nej"));
        
        if(is_multipart_formData)
            {
            try 
                {
                Iterator<FileItem> itr = items.iterator();
                while(itr.hasNext()) 
                    {
                    FileItem item = (FileItem) itr.next();
                    if(item.isFormField()) 
                        {
                        arg = arg + " (" + workflow.quote(item.getFieldName()) + "." + workflow.quote(item.getString("UTF-8").trim()) + ")";
                        }
                    }
                }
            catch(Exception ex) 
                {
                logger.error("uploadHandler.parseRequest Exception");
                }
            }
        
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
        logger.debug("arg = [" + arg + "]");
        return arg;
        }

    public void doPost(HttpServletRequest request,HttpServletResponse response)
        throws ServletException, IOException 
        {
        logger.debug("doPost");
        PrintWriter out = response.getWriter();
        if(BracMat.loaded())
            {
            List<FileItem> items = userhandle.getParmList(request);
            


            
            
            
            
            

            String userEmail = null;
            String passwordAsHandle = null;
            String arg = "";
            passwordAsHandle = getarg(request,items,"passwordAsHandle");
            logger.debug("getarg(request,items,\"passwordAsHandle\") returns:" + (passwordAsHandle == null ? "not found" : passwordAsHandle));
            if(passwordAsHandle != null && passwordAsHandle.equals(ToolsProperties.password))
                {
	            logger.debug("Password ok for activating registered tools. Add 'handle' to list of arguments");
                userEmail = getarg(request,items,"mail2");
	            arg += " (handle." + workflow.quote(passwordAsHandle) + ")";
                }
			else
				{
	            logger.debug("Password [{}] not ok for activating registered tools. Must be [{}]",
					passwordAsHandle,ToolsProperties.password);
				}
                
            logger.debug("userEmail = {}",userEmail);

            if(userEmail != null && getarg(request,items,"contactEmail") == null)
                arg += " (contactEmail." + workflow.quote(userEmail) + ")";
            arg += getargs(request,items);
            /**
              * register$
              *
              * Register a tool - integrated or not.
              * Produces an initially empty html form that dynamically adapts to user's
              * need to register multiple values for the same feature.
              * The form consists of two parts: a part for general information, most of
              * which can be deposited in the repository in a later phase, and a part that
              * is mainly used for integrated tools. The second part collects very precise
              * and formalised information that enables the Tools module to compute
              * workflows with tools and resources that fit together. Only some of the
              * information from this part can be deposited in the repository: a condensed
              * list of supported languages, dataformats and facets, in both input and 
              * output. Missing in the deposited metadata are the restrictions on 
              * combinations of input and output features and between features. 
              * For example there is no formal way for the deposited metadata to express
              * that the input facet 'Part Of Speech' can be combined with input languages
              * Danish and English, but not with Russian as an input language. Nor can
              * deposited metadata formally express that input language Danish implies 
              * output language Danish, and input lnaguage Russian either output language
              * Russian or English. (Implying that the tool can translate from Russian to
              * English.) 
              * Such relations must be written in natural language in the description of
              * the tool.
              *
              * Affected tables in jboss/server/default/data/tools: 
              *         tooladm.table   (general part), 
              *         toolprop.table  (for integrated tools)
              * Input: a list of HTTP-parameters converted to the form 
              *     (<parameter>.<value>) (<parameter>.<value>) (<parameter>.<value>) ...
              *
              * Output: a XHTML-form
              *
              * Input and output form a closed circuit: the form is generated by the 
              * register function and the input from the filled-out form is sent to the
              * register function.
              */
            logger.debug("Calling register$(" + arg + ")");
            String result = BracMat.Eval("register$(" + arg + ")");
            if(result == null || result.equals(""))
                {
                response.setContentType("text/html; charset=UTF-8");
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
                String messagetext = BracMat.Eval("getStatusCode$(\"404\".\"Registration failed\")");
                out.println(messagetext);
                return;
                }
            else if(result.startsWith("<?\nheader") || arg.contains("PHP"))
                { /* php wrapper */
                logger.debug("PHP wrapper {}",result);
                response.setContentType("text/plain; charset=UTF-8");
                response.setStatus(200);
                out.println(result);
                }
            else
                {
                response.setContentType("text/html; charset=UTF-8");
                response.setStatus(200);
                out.println(result);
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
           logger.debug("doGet");
            doPost(request, response);
            }
    }

