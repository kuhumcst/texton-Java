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

import dk.clarin.tools.ToolsProperties;
import java.io.*;
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

/**
 * get escidoc userhandle and e-mail address
 */      

public class userhandle
    {
    //private static final String TMP_DIR_PATH = "/tmp";
    // Static logger object.  
    private static final Logger logger = LoggerFactory.getLogger(userhandle.class);

    /// The eSciDoc userHandle
    //private static String userHandle = null;
    /// The eSciDoc id of the user
    //private static String userId;
    /// The user's email
    //private static String userEmail;

    private static String reason = null;

    public void init(ServletConfig config) throws ServletException 
        {
        InputStream fis = config.getServletContext().getResourceAsStream("/WEB-INF/classes/properties.xml");
        ToolsProperties.readProperties(fis);		
        }

    public static String getReason()
        {
        return reason;
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
                throw new ServletException("Trying to set \"" + ToolsProperties.tempdir + "\" as temporary directory, but this is not a valid directory.");
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

    public static String getParmFromMultipartFormData(HttpServletRequest request,List<FileItem> items,String parm)
        {
        logger.debug("parm:["+parm+"]");
        String userHandle = "";
        boolean is_multipart_formData = ServletFileUpload.isMultipartContent(request);

        if(is_multipart_formData)
            {
            try 
                {
                /*
                logger.debug("In try");
                DiskFileItemFactory  fileItemFactory = new DiskFileItemFactory ();
                / *
                *Set the size threshold, above which content will be stored on disk.
                * /
                fileItemFactory.setSizeThreshold(1*1024*1024); //1 MB
                / *
                * Set the temporary directory to store the uploaded files of size above threshold.
                * /
                logger.debug("making tmpDir in " + ToolsProperties.tempdir);
                File tmpDir = new File(ToolsProperties.tempdir);
                if(!tmpDir.isDirectory()) 
                    {
                    logger.debug("!tmpDir.isDirectory()");
                    throw new ServletException("Trying to set \"" + ToolsProperties.tempdir + "\" as temporary directory, but this is not a valid directory.");
                    }
                fileItemFactory.setRepository(tmpDir);
                * /
                
                ServletFileUpload uploadHandler = new ServletFileUpload(fileItemFactory);
                logger.debug("Now uploadHandler.parseRequest");
                List items<FileItem> = uploadHandler.parseRequest(request);
                */
                logger.debug("items:"+items);
                Iterator<FileItem> itr = items.iterator();
                logger.debug("itr:"+itr);
                while(itr.hasNext()) 
                    {
                    logger.debug("in loop");
                    FileItem item = (FileItem) itr.next();
                    /*
                    * Handle Form Fields.
                    */
                    if(item.isFormField()) 
                        {
                        logger.debug("Field Name = "+item.getFieldName()+", String = "+item.getString());
                        if(item.getFieldName().equals(parm))
                            {
                            userHandle = item.getString();
                            logger.debug("Found " + parm + " = " + userHandle);
                            /*
                            if(userId == null && userHandle != null)
                                userId = userhandle.getUserId(request,userHandle);
                            if(userEmail == null && userId != null)
                                userEmail = userhandle.getEmailAddress(request,userHandle,userId);
                            */
                            break; // currently not interested in other fields than parm
                            }
                        }
                    else if(item.getName() != "")
                        {
                        /*
                        * Write file to the ultimate location.
                        */
                        logger.debug("File = "+item.getName());
                        /* We don't handle file upload here
                        data = item.getName();
                        File file = new File(destinationDir,item.getName());
                        item.write(file);
                        */
                        logger.debug("FieldName = "+item.getFieldName());
                        logger.debug("Name = "+item.getName());
                        logger.debug("ContentType = "+item.getContentType());
                        logger.debug("Size = "+item.getSize());
                        logger.debug("DestinationDir = "+ToolsProperties.documentRoot + ToolsProperties.stagingArea);
                        }
                    }
                }
            catch(Exception ex) 
                {
                logger.error("uploadHandler.parseRequest Exception");
                }
            }
        else
            {
            @SuppressWarnings("unchecked")
            Enumeration<String> parmNames = (Enumeration<String>)request.getParameterNames();

            for (Enumeration<String> e = parmNames ; e.hasMoreElements() ;) 
                {
                // Well, you don't get here AT ALL if enctype='multipart/form-data'
                String parmName = e.nextElement();
                logger.debug("parmName:"+parmName);
                String vals[] = request.getParameterValues(parmName);
                for(int j = 0;j < vals.length;++j)
                    {
                    logger.debug("val:"+vals[j]);
                    }
                }
            }
        logger.debug("value["+parm+"]="+userHandle);
        return userHandle;
        }                        


    public static String getUserHandle(HttpServletRequest request,List<FileItem> items)
        {
        /// The eSciDoc userHandle
        String userHandle = null;
        logger.debug("getUserHandle");

        //if(userHandle == null || userHandle.equals(""))
            {
            logger.debug("userHandle == null");
            userHandle = "";
/*
            HttpSession session = request.getSession(true);
            UserSessionBean bean = (UserSessionBean) session.getAttribute("user");

            if (bean != null) 
                {
                logger.debug("userHandle from bean: {}",bean.getUserHandle());
                return bean.getUserHandle();
                }
            logger.debug("bean == null, trying to use parameter 'handle'");
*/
            userHandle = request.getParameter("handle");
            if(userHandle == null)
                {
                logger.debug("No 'handle' parameter, trying to use cookie 'escidocCookie'");

                String cookieName = "escidocCookie";
                javax.servlet.http.Cookie cookies [] = request.getCookies();
                if(cookies != null)
                    {
                    logger.debug("cookies != null");
                    for(int i = 0 ;i < cookies.length;i++)
                        {
                        logger.debug("cookie {} = {}",cookies[i].getName(),cookies[i].getValue());
                        if(cookies[i].getName().equals(cookieName))
                            {
                            userHandle = cookies[i].getValue();
                            logger.debug("userHandle == {}",userHandle);
                            if(userHandle != null && userHandle.equals("0"))
                                userHandle = null;
                             //break;
                            }
                        }
                    }
                else
                    logger.debug("cookies == null");
                    
                if(userHandle == null || userHandle.equals(""))
                    {
                    /*
                    * Parse the request
                    */
                    userHandle = getParmFromMultipartFormData(request,items,"handle");
                    }
                }
            }
        /*else
            {
            logger.debug("userHandle already set to {}",userHandle);
            }*/
        if(userHandle != null && !userHandle.equals(""))
            {
            logger.info("userHandle = " + userHandle);
            return userHandle;
            }
        else
            {
            logger.info("userHandle = null");
            return null;
            }
        }

    public static String getUserId(HttpServletRequest request,List<FileItem> items,String userHandle)
        {
        /// The eSciDoc id of the user
        String userId = null;
        // First get the user's escidoc-id and email
        org.apache.commons.httpclient.HttpClient httpClient = new org.apache.commons.httpclient.HttpClient();

        org.apache.commons.httpclient.methods.GetMethod method;
        httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(500000);
        logger.debug("getUserId( {} )",userHandle);
        String res = null;

        method =  new org.apache.commons.httpclient.methods.GetMethod(ToolsProperties.coreServer 
            + "/aa/user-account/" 
            + userHandle);
        try 
            {
            method.setRequestHeader("Cookie", "escidocCookie=" + userHandle);
            method.setFollowRedirects(false);
            httpClient.executeMethod(method);
            if (method.getStatusCode() != 200) 
                { // May be 302 Found. This is the most popular redirect code, 
                  // but also an example of industrial practice contradicting
                  // the standard. HTTP/1.0 specification (RFC 1945) required
                  // the client to perform a temporary redirect (the original
                  // describing phrase was \"Moved Temporarily\"), but popular
                  // browsers implemented 302 with the functionality of a 
                  // 303 See Other. Therefore, HTTP/1.1 added status codes 303
                  // and 307 to distinguish between the two behaviours.
                  // However, the majority of Web applications and frameworks
                  // still use the 302 status code as if it were the 303.
                logger.error("Wrong return code [" + method.getStatusCode() + "]. The user could not be identified!");
                reason = "Wrong return code. The user could not be identified!";
                userId = null;
                }
            else
                {
                res = method.getResponseBodyAsString();

                // Extract the user's id
                int startIdx = res.lastIndexOf("xlink:href");
                if (startIdx > 1)
                    userId = res.substring(startIdx+29, res.indexOf("/", startIdx+29));
                if (userId == null || userId.length() < 3) 
                    {
                    logger.error("The user could not be identified! reply: " + res);
                    reason = "The user could not be identified!";
                    userId = null;
                    }
                else
                    logger.debug("userId = {}",userId);
                }
            }
        catch (IOException e) 
            {
            logger.error("Could not contact the eSciDoc server");
            reason = "Could not contact the eSciDoc server";
            userId = null;
            }
        finally 
            {
            method.releaseConnection();
            }
/*
Dangerous, can be forged!
        if(userId == null)
            {
            String [] tmp = request.getParameterValues("id");
            if(tmp != null)
                userId = tmp[0];
            }
*/

        if(userId != null)
            {
            reason = null;
            }

        logger.debug("user ID: " + userId);

        return userId;
        }

    public static String getEmailAddress(HttpServletRequest request,List<FileItem> items,String userHandle,String userId)
        {
        /// The user's email
        String userEmail;
        org.apache.commons.httpclient.HttpClient httpClient = new org.apache.commons.httpclient.HttpClient();
        org.apache.commons.httpclient.methods.GetMethod method;
        String res = null;
        method =  null;
        try
            {
            method = new org.apache.commons.httpclient.methods.GetMethod(ToolsProperties.coreServer 
                + "/aa/user-account/" 
                + userId 
                + "/resources/attributes/email");
            method.setRequestHeader("Cookie", "escidocCookie=" + userHandle);
            method.setFollowRedirects(false);
            httpClient.executeMethod(method);
            if (method.getStatusCode() != 200) 
                {
                logger.error("Wrong return code. The user's email could not be found! reply: " + res);
                reason = "The user email could not be found!";
                userEmail = null;
                }
            else
                {
                String ResponseBody = method.getResponseBodyAsString();
                userEmail = ResponseBody.replaceAll("\r\n","").replaceAll("<.+?>","").trim();
                if (userEmail == null || userEmail.length() < 3) 
                    {
                    logger.error("The user's email could not be found! reply: " + res);
                    logger.error("userHandle "+userHandle+", userId "+userId+", ResponseBody "+ResponseBody);
                    reason = "The user email could not be found!";
                    userEmail = null;
                    }
                }
            }
        catch (IOException e) 
            {
            logger.error("Could not contact the eSciDoc server");
            reason = "Could not contact the eSciDoc server";
            userEmail = null;
            }
        finally 
            {
            if(method != null)
                method.releaseConnection();
            }
/*
Dangerous, can be forged!
        if(userEmail == null)
            {
            String [] tmp = request.getParameterValues("ContactEmail");
            if(tmp != null)
                userEmail = tmp[0];
            }
*/
/*
        if(userEmail == null)
            {
            userEmail = getParmFromMultipartFormData(request,"ContactEmail");
            }
*/            
        if(userEmail != null)
            {
            reason = null;
            logger.debug("user email: " + userEmail);
            }

        return userEmail;
        }
    }

