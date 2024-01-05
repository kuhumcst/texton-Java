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
package dk.clarin.tools;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;

import java.nio.charset.StandardCharsets;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Collection;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class parameters
    {
    // Static logger object.  
    private static final Logger logger = LoggerFactory.getLogger(parameters.class);

    private static String reason;

    public void init(ServletConfig config) throws ServletException 
        {
        InputStream fis = config.getServletContext().getResourceAsStream("/WEB-INF/classes/properties.xml");
        ToolsProperties.readProperties(fis);        
        }

    public static String getGETarg(HttpServletRequest request, String name)
        {
        if(name != null)
            {
            @SuppressWarnings("unchecked")
            Enumeration<String> parmNames = (Enumeration<String>)request.getParameterNames();
            for (Enumeration<String> e = parmNames ; e.hasMoreElements() ;) 
                {
                String parmName = e.nextElement();
                String vals[] = request.getParameterValues(parmName);
                for(String val : vals)
                    {
                    if(name.equals(parmName))
                        {
                        return val;
                        }
                    }
                }
            }
        return null;
        }

    public static String getPOSTarg(HttpServletRequest request, Collection<Part> items, String name)
        {
        /*
        * Parse the request
        */
        if(name != null)
            {
            try 
                {
                Iterator<Part> itr = items.iterator();
                while(itr.hasNext()) 
                    {
                    Part item = itr.next();
                    if(name.equals(item.getName()))
                        {
                        return IOUtils.toString(item.getInputStream(),StandardCharsets.UTF_8);
                        }
                    }
                }
            catch(Exception ex) 
                {
                logger.error("uploadHandler.parseRequest Exception");
                }
            }
        return null;
        }

    public static String getAllGETArgsBracmatFormat(HttpServletRequest request)
        {
        String arg = "";
        @SuppressWarnings("unchecked")
        Enumeration<String> parmNames = (Enumeration<String>)request.getParameterNames();
        
        for (Enumeration<String> e = parmNames ; e.hasMoreElements() ;) 
            {
            String parmName = e.nextElement();
            arg = arg + " (" + util.quote(parmName) + ".";
            String vals[] = request.getParameterValues(parmName);
            for(String val : vals)
                {
                arg += " " + util.quote(val);
                }
            arg += ")";
            }
        return arg;
        }

    public static String getargsBracmatFormat(HttpServletRequest request, Collection<Part> items)
        {
        String arg = "";
        /*
        * Parse the request
        */

        @SuppressWarnings("unchecked")
        Enumeration<String> parmNames = (Enumeration<String>)request.getParameterNames();
        try 
            {
            Iterator<Part> itr = items.iterator();
            while(itr.hasNext()) 
                {
                Part item = itr.next();
                arg = arg + " (" + util.quote(item.getName()) + "." + util.quote(IOUtils.toString(item.getInputStream(),StandardCharsets.UTF_8).trim()) + ")";
                }
            }
        catch(Exception ex) 
            {
            logger.error("uploadHandler.parseRequest Exception");
            }
        /* This seems to merely redo the argument parsing, causing all args to occur twice!
        for (Enumeration<String> e = parmNames ; e.hasMoreElements() ;) 
            {
            String parmName = e.nextElement();
            arg = arg + " (" + util.quote(parmName) + ".";
            String vals[] = request.getParameterValues(parmName);
            for(String val : vals)
                {
                arg += " " + util.quote(val);
                }
            arg += ")";
            logger.debug("arg++="+arg);
            }
            */
        return arg;
        }

    public static String getParmFromFormData(HttpServletRequest request,Collection<Part> items,String parm) 
        {
        String value = null;
        @SuppressWarnings("unchecked")
        Enumeration<String> parmNames = (Enumeration<String>)request.getParameterNames();

        for (Enumeration<String> e = parmNames ; e.hasMoreElements() ;) 
            {
            String parmName = e.nextElement();
            if(parmName.equals(parm))
                {
                String vals[] = request.getParameterValues(parmName);
                for(String val : vals)
                    {
                    value = val;
                    }
                }
            }
        return value;
        }                        

    public static String getPreferredLocale(HttpServletRequest request,Collection<Part> items)
        {
        String UIlanguage = null;
        /* First check whether there is a UIlanguage parameter */
        
        UIlanguage = getParmFromFormData(request,items,"UIlanguage");
        if(UIlanguage != null && !UIlanguage.equals("da") && !UIlanguage.equals("en"))
            UIlanguage = "da";
        return UIlanguage;
        }
    }
