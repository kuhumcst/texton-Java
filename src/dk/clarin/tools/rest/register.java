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

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.*;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
@MultipartConfig(fileSizeThreshold=1024*1024*10,  // 10 MB 
                 maxFileSize=-1/*1024*1024*50*/,       // 50 MB
                 maxRequestSize=-1/*1024*1024*100*/)    // 100 MB

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

    public void doPost(HttpServletRequest request,HttpServletResponse response)
        throws ServletException, IOException 
  //  public void doGet(HttpServletRequest request, HttpServletResponse response)
    //    throws ServletException, IOException
        {
        logger.debug("register doPost.");

        PrintWriter out = response.getWriter();
        if(BracMat.loaded())
            {
            
            Collection<Part> items = null;
            logger.debug("request.getParts");
            try 
                {
                items = request.getParts(); // throws ServletException if this request is not of type multipart/form-data
                }
            catch(IOException ex) 
                {
                logger.error("IOException:Error encountered while parsing the request: "+ex.getMessage());
                return;
                }
            catch(ServletException ex) 
                {
                logger.error("ServletException:Error encountered while parsing the request: "+ex.getMessage());
                return;
                }

            String userEmail = null;
            String passwordAsHandle = null;
            String arg = "";
            passwordAsHandle = parameters.getPOSTarg(request,items,"passwordAsHandle");
            //passwordAsHandle = parameters.getGETarg(request,"passwordAsHandle");

            //logger.debug("getPOSTarg(request,items,\"passwordAsHandle\") returns:" + (passwordAsHandle == null ? "not found" : passwordAsHandle)); DON'T DO THIS
            String StoredPassword = BracMat.Eval("getProperty$password");
            if(StoredPassword.equals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")) // empty string, default, for development on local machine.
                {
                userEmail = parameters.getPOSTarg(request,items,"mail2");
                //userEmail = parameters.getGETarg(request,"mail2");
                arg += " (handle.LOCALMACHINEDUMMY)";
                }
            else if(passwordAsHandle != null && util.goodToPass(passwordAsHandle,BracMat))
                {
                logger.debug("Password ok for activating registered tools. Add 'handle' to list of arguments");
                userEmail = parameters.getPOSTarg(request,items,"mail2");
                //userEmail = parameters.getGETarg(request,"mail2");
                arg += " (handle.OK)";
                }
            else
                {
                logger.debug("Password not OK for activating registered tools.");
                }
                
            if(userEmail != null && parameters.getPOSTarg(request,items,"contactEmail") == null)
                arg += " (contactEmail." + util.quote(userEmail) + ")";
            //if(userEmail != null && parameters.getGETarg(request,"contactEmail") == null)
            //    arg += " (contactEmail." + util.quote(userEmail) + ")";


            arg += parameters.getargsBracmatFormat(request,items);
            //arg += parameters.getAllGETArgsBracmatFormat(request);

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
            //logger.debug("Calling register$(" + arg + ")"); DON'T DO THIS
            logger.debug("Calling register");
            String result = BracMat.Eval("register$(" + arg + ")");
            logger.debug("Calling registerDONE");
            if(result == null || result.equals(""))
                {
                logger.debug("No result");
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
            else if(result.startsWith("<?\nheader") 
                   ||result.startsWith("<?PHP\nheader") 
                   ||result.startsWith("<?php\nheader") 
                   || arg.contains("PHP") 
                   || arg.contains("php")
                   )
                { /* php wrapper */
                logger.debug("PHP wrapper");
                //response.setContentType("text/plain; charset=iso8859-1");//UTF-8");
                response.setContentType("text/plain; charset=iso8859-1");
                response.setStatus(200);
                out.println(result);
                }
            else
                {
                logger.debug("HTML:"+result);
                response.setContentType("text/html; charset=iso8859-1");//UTF-8");
//                response.setContentType("application/xhtml+xml; charset=iso8859-1");//UTF-8");

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
        doPost(request, response);
        }
    }

