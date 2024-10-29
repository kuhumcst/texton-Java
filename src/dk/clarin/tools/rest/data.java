/**
 * Return result from Tools. E.g. in response to this URL:
 *      https://clarin.dk/texton/data/3892126799-323-step1.xml
 * A call to the 'keep' function in TexTon.bra checks that by looking for
 * outstanding jobs that take the result as input.
 * Directory listings are forbidden, so the URL
 *      https://clarin.dk/texton/data/
 * returns an informational text.
 * Return codes 200 
 *              404 if a directorly listing is attempted or if the file is no
 *                  longer accessible.
 *              500 if Bracmat could not be loaded
 */

package dk.clarin.tools.rest;

import dk.clarin.tools.ToolsProperties;

import dk.cst.bracmat;

import jakarta.servlet.ServletConfig; 
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
@MultipartConfig(fileSizeThreshold=1024*1024*10,  // 10 MB 
                 maxFileSize=-1/*1024*1024*50*/,       // 50 MB
                 maxRequestSize=-1/*1024*1024*100*/)    // 100 MB

public class data extends HttpServlet 
    {
    private static final Logger logger = LoggerFactory.getLogger(data.class);
    private File destinationDir;
    private bracmat BracMat;

    public void init(ServletConfig config) throws ServletException 
        {
        InputStream fis = config.getServletContext().getResourceAsStream("/WEB-INF/classes/properties.xml");
        ToolsProperties.readProperties(fis);        
        BracMat = new bracmat(ToolsProperties.bootBracmat);
        super.init(config);
        destinationDir = new File(ToolsProperties.documentRoot);
        if(!destinationDir.isDirectory()) 
            {
            throw new ServletException("Trying to set \"" + ToolsProperties.documentRoot + "\" as directory for temporary storing intermediate and final results, but this is not a valid directory.");
            }
        }

    public void doGet(HttpServletRequest request,HttpServletResponse response)
        throws ServletException, IOException 
        {
        response.setStatus(200);
        if(!BracMat.loaded())
            {
            response.setStatus(500);
            throw new ServletException("Bracmat is not loaded. Reason:" + BracMat.reason());
            }
        /*Test:
        @SuppressWarnings("unchecked")
        Enumeration<String> parmNames = (Enumeration<String>)request.getParameterNames();
        logger.debug("show parms");
        for (Enumeration<String> e = parmNames ; e.hasMoreElements() ;) 
            {
            String parmName = e.nextElement();
            logger.debug("parmName:"+parmName);
            String vals[] = request.getParameterValues(parmName);
            for(int j = 0;j < vals.length;++j)
                {
                logger.debug("val:"+vals[j]);
                }
            }
        :Test*/
        if(request.getPathInfo() == null || request.getPathInfo().equals("/"))
            {
            response.setContentType("text/html; charset=UTF-8");
            response.setStatus(404);
            PrintWriter out = response.getWriter();
            out.println("Sorry, no directory listing.");
            }
        else
            {
            response.setContentType("text/plain");
            String name = request.getPathInfo();
            try
                {
                // With ContentType("text/xml") the md5 checksum for the sender isn't the same as for the receiver.
                // (If sent as ContentType("text/plain"), an XML-file doesn't look nice in the receiver's browser.)
                if(name.endsWith(".xml"))
                    response.setContentType("text/xml");
                else if(name.endsWith(".csv"))
                    response.setContentType("text/csv");
                else if(name.endsWith(".htm"))
                    response.setContentType("text/html");
                else if(name.endsWith(".html"))
                    response.setContentType("text/html");
                else if(name.endsWith(".docx"))
                    response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                else if(name.endsWith(".ppt"))
                    response.setContentType("application/application/vnd.ms-powerpoint");
                else if(name.endsWith(".pptx"))
                    response.setContentType("application/application/vnd.openxmlformats-officedocument.presentationml.presentation");
                else if(name.endsWith(".odp"))
                    response.setContentType("application/vnd.oasis.opendocument.presentation");
                else if(name.endsWith(".ods"))
                    response.setContentType("application/vnd.oasis.opendocument.spreadsheet");
                else if(name.endsWith(".odt"))
                    response.setContentType("application/vnd.oasis.opendocument.text");
                else if(name.endsWith(".json"))
                    response.setContentType("application/json");
                else if(name.endsWith(".rtf"))
                    response.setContentType("application/rtf");
                else if(name.endsWith(".zip"))
                    response.setContentType("application/zip");
                else if(name.endsWith(".doc"))
                    response.setContentType("application/msword");
                else if(name.endsWith(".pdf"))
                    response.setContentType("application/pdf");
                else if(name.endsWith(".xhtml"))
                    response.setContentType("application/xhtml+xml");
                else if(name.endsWith(".wav"))
                    response.setContentType("audio/wav");                    
                else
                    response.setContentType("text/plain; charset=UTF-8");
    
                String fileName = destinationDir + name;
                try {
                    File f = new File(fileName);
                    int nLen = 0;
                    OutputStream outstrm;
                    try {
                        InputStream in = Files.newInputStream(f.toPath());
                        outstrm = response.getOutputStream();
                        byte[] bBuf = new byte[1024];
                        try
                            {
                            while ((nLen = in.read(bBuf, 0, 1024)) != -1)
                                {
                                outstrm.write(bBuf, 0, nLen);
                                }
                            }
                        finally
                            {
                            in.close();
                            }
                        }
                    catch (IllegalArgumentException e)
                        {
                        logger.error("An invalid combination of options is specified. (fileName=" + fileName + ") Reason:" + e.getMessage());
                        response.setContentType("text/html; charset=UTF-8");
                        response.setStatus(404);
                        PrintWriter out = response.getWriter();
                        out.println("IllegalArgumentException:" + e.getMessage());
                        }
                    catch (UnsupportedOperationException e)
                        {
                        logger.error("An unsupported option is specified. (fileName=" + fileName + ") Reason:" + e.getMessage());
                        response.setContentType("text/html; charset=UTF-8");
                        response.setStatus(404);
                        PrintWriter out = response.getWriter();
                        out.println("UnsupportedOperationException:" + e.getMessage());
                        }
                    catch (IOException e)
                        {
                        logger.error("An I/O error occurs. (fileName=" + fileName + ") Reason:" + e.getMessage());
                        response.setContentType("text/html; charset=UTF-8");
                        response.setStatus(404);
                        PrintWriter out = response.getWriter();
                        out.println("IOException:" + e.getMessage());
                        }
                    catch (SecurityException e)
                        {
                        logger.error("In the case of the default provider, and a security manager is installed, the checkRead method is invoked to check read access to the file. (fileName=" + fileName + ") Reason:" + e.getMessage());
                        response.setContentType("text/html; charset=UTF-8");
                        response.setStatus(404);
                        PrintWriter out = response.getWriter();
                        out.println("SecurityException:" + e.getMessage());
                        }
                    }
                catch (NullPointerException e)
                    {
                    logger.error("NullPointerException. (fileName=" + fileName + ") Reason:" + e.getMessage());
                    response.setContentType("text/html; charset=UTF-8");
                    response.setStatus(404);
                    PrintWriter out = response.getWriter();
                    out.println("The pathname argument (variable 'fileName') is null.");
                    }
                }
            catch (java.nio.file.NoSuchFileException e)
                {
                logger.error("java.nio.file.NoSuchFileException. Reason:" + e.getMessage());
                response.setContentType("text/html; charset=UTF-8");
                response.setStatus(404);
                PrintWriter out = response.getWriter();
                if(name.charAt(0) == '/')
                    name = name.substring(1);
                out.println("File " + name + " is no longer accessible.");
                }
            catch (FileNotFoundException e)
                {
                logger.error("FileNotFoundException. Reason:" + e.getMessage());
                response.setContentType("text/html; charset=UTF-8");
                response.setStatus(404);
                PrintWriter out = response.getWriter();
                if(name.charAt(0) == '/')
                    name = name.substring(1);
                out.println("File " + name + " is no longer accessible.");
                }
            }
        }
    }

