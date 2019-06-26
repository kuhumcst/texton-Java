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
import dk.clarin.tools.workflow;
import dk.cst.bracmat;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.Enumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Poll status of workflow. E.g. in response to this URL:
 *      https://clarin.dk/texton/poll/3892126799-323
 * Directory listings are forbidden, so the URL
 *      https://clarin.dk/texton/poll/
 * returns an informational text.
 * Return codes 200 
 *              404 if a directorly listing is attempted or if the file is no
 *                  longer accessible.
 *              500 if Bracmat could not be loaded
 */
@SuppressWarnings("serial")
public class poll extends HttpServlet 
    {
    private File destinationDir;
    private bracmat BracMat;
    private static final Logger logger = LoggerFactory.getLogger(workflow.class);

    public void init(javax.servlet.ServletConfig config) throws javax.servlet.ServletException 
        {
        InputStream fis = config.getServletContext().getResourceAsStream("/WEB-INF/classes/properties.xml");
        ToolsProperties.readProperties(fis);        
        BracMat = new bracmat(ToolsProperties.bootBracmat);
        super.init(config);
        destinationDir = new File(ToolsProperties.documentRoot /*+ ToolsProperties.stagingArea*/);
        if(!destinationDir.isDirectory()) 
            {
            throw new ServletException("Trying to set \"" + ToolsProperties.documentRoot /*+ ToolsProperties.stagingArea */+ "\" as directory for temporary storing intermediate and final results, but this is not a valid directory.");
            }
        }

    public void doGet(HttpServletRequest request,HttpServletResponse response)
        throws ServletException, IOException 
        {
        PrintWriter out = response.getWriter();
        response.setStatus(200);
        if(!BracMat.loaded())
            {
            response.setStatus(500);
            throw new ServletException("Bracmat is not loaded. Reason:" + BracMat.reason());
            }
        // https://clarin.dk/texton/poll?job=12345-678
        /*Test:*/
        @SuppressWarnings("unchecked")
        Enumeration<String> parmNames = (Enumeration<String>)request.getParameterNames();
        String job = null;
        for (Enumeration<String> e = parmNames ;  job == null && e.hasMoreElements() ;) 
            {
            String parmName = e.nextElement();
            String vals[] = request.getParameterValues(parmName);
            for(int j = 0;j < vals.length && job == null;++j)
                {
                if(parmName.equals("job"))
                    {
                    job = vals[j];
                    if(job == null || job.equals("null"))
                        {
                        response.setStatus(418);
                        out.println("poll: no job number found");
                        return;
                        }
                    }
                }
            }
        /*:Test*/
        /*
        logger.info("getPathInfo() returns {}",request.getPathInfo());
        if(request.getPathInfo() == null || request.getPathInfo().equals("/"))
            {
            response.setContentType("text/html; charset=UTF-8");
            response.setStatus(404);
            out.println("Sorry, no directory listing.");
            }
        else*/ if(job != null)
            {
            response.setContentType("text/html; charset=UTF-8");

            String svar = BracMat.Eval("poll$("+workflow.quote(job) + ")");
            out.println(svar);            
            }
        }
    }

