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
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
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

    public static String getReason()
        {
        return reason;
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

    public static String getPOSTorGETarg(HttpServletRequest request, List<FileItem> items, String name)
        {
        /*
        * Parse the request
        */
        if(name != null)
            {
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
        
            return getGETarg(request,name);
            }
        return null;
        }

    @SuppressWarnings("unchecked")
    public static List<FileItem> getParmList(HttpServletRequest request) throws ServletException
        {
        List<FileItem> items = null;
        boolean is_multipart_formData = ServletFileUpload.isMultipartContent(request);

        if(is_multipart_formData)
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
                items = (List<FileItem>)uploadHandler.parseRequest(request);
                }
            catch(FileUploadException ex) 
                {
                logger.error("Error encountered while parsing the request: "+ex.getMessage());
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
            arg = arg + " (" + workflow.quote(parmName) + ".";
            String vals[] = request.getParameterValues(parmName);
            for(String val : vals)
                {
                arg += " " + workflow.quote(val);
                }
            arg += ")";
            }
        return arg;
        }

    public static String getargsBracmatFormat(HttpServletRequest request, List<FileItem> items)
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
            for(String val : vals)
                {
                arg += " " + workflow.quote(val);
                }
            arg += ")";
            }
        logger.debug("arg = [" + arg + "]");
        return arg;
        }

    public static String getParmFromFormData(HttpServletRequest request,List<FileItem> items,String parm) 
        {
        String value = null;
        boolean is_multipart_formData = ServletFileUpload.isMultipartContent(request);

        if(is_multipart_formData)
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
                    Iterator<FileItem> itr = items.iterator();
                    while(itr.hasNext()) 
                        {
                        FileItem item = (FileItem) itr.next();
                        /*
                        * Handle Form Fields.
                        */
                        if(item.isFormField()) 
                            {
                            if(item.getFieldName().equals(parm))
                                {
                                value = item.getString();
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

    public static String getPreferredLocale(HttpServletRequest request,List<FileItem> items)
        {
        String UIlanguage = null;
        /* First check whether there is a UIlanguage parameter */
        UIlanguage = getParmFromFormData(request,items,"UIlanguage");
        if(UIlanguage != null && !UIlanguage.equals("da") && !UIlanguage.equals("en"))
            UIlanguage = "da";
        return UIlanguage;
        }
    }
