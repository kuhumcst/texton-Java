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
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Iterator;
//import java.util.List;
import java.util.Collection;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import org.apache.commons.fileupload.FileItem;
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

    private static String reason;// = null;

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

//    public static String getPOSTarg(HttpServletRequest request, List<FileItem> items, String name)
    public static String getPOSTarg(HttpServletRequest request, Collection<Part> items, String name)
        {
        /*
        * Parse the request
        */
        if(name != null)
            {
//            @SuppressWarnings("unchecked")
            //boolean is_multipart_formData = ServletFileUpload.isMultipartContent(request);
            //logger.debug("is_multipart_formData:"+(is_multipart_formData ? "ja" : "nej"));
            //if(is_multipart_formData)
            try 
                {
                Iterator<Part> itr = items.iterator();
                while(itr.hasNext()) 
                    {
                    Part item = /*(FileItem)*/ itr.next();
                    //if(item.isFormField()) 
                        {
                        if(name.equals(item.getName()/*getFieldName()*/))
                            {
                            String fiel = IOUtils.toString(item.getInputStream(),StandardCharsets.UTF_8);
                            return fiel;
//                            return item.getString("UTF-8").trim();
                            }
                        }
                    }
                }
            catch(Exception ex) 
                {
                logger.error("uploadHandler.parseRequest Exception");
                }
            //return getGETarg(request,name);
            }
        return null;
        }

    @SuppressWarnings("unchecked")
    public static Collection<Part> getParmList(HttpServletRequest request) throws ServletException
        {
        Collection<Part> items = null;
        //boolean is_multipart_formData = ServletFileUpload.isMultipartContent(request);

        //if(is_multipart_formData)
            {
            DiskFileItemFactory  fileItemFactory = new DiskFileItemFactory ();
            /*
            *Set the size threshold, above which content will be stored on disk.
            */
            fileItemFactory.setSizeThreshold(1*1024*1024); //1 MB
            /*
            * Set the temporary directory to store the uploaded files of size above threshold.
            */
            File tmpDir = new File(ToolsProperties.tempdir);
            if(!tmpDir.isDirectory()) 
                {
                throw new ServletException("Trying to set \"" + ToolsProperties.tempdir + "\" as temporary directory, but this is not a valid directory. See `conf/properties.xml.");
                }
            fileItemFactory.setRepository(tmpDir);
            ServletFileUpload uploadHandler = new ServletFileUpload(fileItemFactory);
            try 
                {
            //    items = (List<FileItem>)uploadHandler.parseRequest(request);
                items = request.getParts(); // throws ServletException if this request is not of type multipart/form-data
                }
     /*       catch(FileUploadException ex) 
                {
                logger.error("Error encountered while parsing the request: "+ex.getMessage());
                }*/
            catch(IOException ex) 
                {
                logger.error("Error encountered while parsing the request: "+ex.getMessage());
                return null;
                }
            catch(ServletException ex) 
                {
                logger.error("Error encountered while parsing the request: "+ex.getMessage());
                return null;
                }
            }
        return items;
        }

    public static String getAllGETArgsBracmatFormat(HttpServletRequest request)
        {
        String arg = "";
        @SuppressWarnings("unchecked")
        Enumeration<String> parmNames = (Enumeration<String>)request.getParameterNames();
        logger.debug("Got some parmNames");

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

//    public static String getargsBracmatFormat(HttpServletRequest request, List<FileItem> items)
    public static String getargsBracmatFormat(HttpServletRequest request, Collection<Part> items)
        {
        String arg = "";
        /*
        * Parse the request
        */

        @SuppressWarnings("unchecked")
        Enumeration<String> parmNames = (Enumeration<String>)request.getParameterNames();
        //boolean is_multipart_formData = ServletFileUpload.isMultipartContent(request);

        //logger.debug("is_multipart_formData:"+(is_multipart_formData ? "ja" : "nej"));
        
        //if(is_multipart_formData)
            {
            try 
                {
                Iterator<Part> itr = items.iterator();
                while(itr.hasNext()) 
                    {
                    Part item = /*(FileItem)*/ itr.next();
                    //if(item.isFormField()) 
                        {
                        arg = arg + " (" + util.quote(item.getName()/*getFieldName()*/) + "." + util.quote(IOUtils.toString(item.getInputStream(),StandardCharsets.UTF_8)/*item.getString("UTF-8")*/.trim()) + ")";
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
            arg = arg + " (" + util.quote(parmName) + ".";
            String vals[] = request.getParameterValues(parmName);
            for(String val : vals)
                {
                arg += " " + util.quote(val);
                }
            arg += ")";
            }
        //logger.debug("arg = [" + arg + "]"); DON'T DO THIS
        return arg;
        }

    public static String getParmFromFormData(HttpServletRequest request,Collection<Part> items,String parm) 
        {
        String value = null;
        //boolean is_multipart_formData = ServletFileUpload.isMultipartContent(request);

        //if(is_multipart_formData)
            {
            if(items == null)
                {
                try
                    {
                    items = getParmList(request);
                    }
                catch(ServletException e)
                    {
                    logger.error("Error encountered while getting items from request: "+e.getMessage());
                    }
                }
            if(items != null)
                {
                try 
                    {
                    Iterator<Part> itr = items.iterator();
                    while(itr.hasNext()) 
                        {
                        Part item = /*(FileItem)*/ itr.next();
                        /*
                        * Handle Form Fields.
                        */
                       // if(item.isFormField()) 
                            {
                            if(item.getName()/*getFieldName()*/.equals(parm))
                                {
                                value = /*item.getString()*/IOUtils.toString(item.getInputStream(),StandardCharsets.UTF_8);
                                break; // currently not interested in other fields than parm
                                }
                            }
                        /*
                        else if(item.getName() != "")
                            {
                            // We don't handle file upload here
                            }
                        */
                        }
                    }
                catch(Exception ex) 
                    {
                    logger.error("uploadHandler.parseRequest Exception");
                    }
                }
            }
            
        if(value == null)
            {
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
                        //break;
                        }
                    }
                }
            }
        return value;
        }                        

//    public static String getPreferredLocale(HttpServletRequest request,List<FileItem> items)
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
