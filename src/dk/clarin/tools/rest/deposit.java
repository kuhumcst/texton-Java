package dk.clarin.tools.rest;


import dk.cst.*;
import dk.clarin.tools.ToolsProperties;
import dk.clarin.tools.workflow;
import java.io.*;
import java.util.Enumeration;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * First step in deposition of a tool. The service generates a simple form
 * where the user chooses a tool from a pick list, optionally adds a file to
 * be stored as data (an installation file, for example), and chooses under
 * which licence the tool must be distributed.
 * The next step is implemented in depositXML.java.
 * No arguments.
 * Status code is normally 200. If Bracmat cannot be loaded, 404 is returned.
 */
@SuppressWarnings("serial")
public class deposit extends HttpServlet 
    {
    private bracmat BracMat;

    public void init(ServletConfig config) throws ServletException 
        {
        InputStream fis = config.getServletContext().getResourceAsStream("/WEB-INF/classes/properties.xml");
        ToolsProperties.readProperties(fis);        
        BracMat = new bracmat(ToolsProperties.bootBracmat);
        super.init(config);
        }

    public void doGet(HttpServletRequest request,HttpServletResponse response)
        throws ServletException, IOException 
        {
        //response.setContentType("text/xml");
        response.setContentType("text/html; charset=UTF-8");
        response.setStatus(200);
        PrintWriter out = response.getWriter();
        if(BracMat.loaded())
            {
            @SuppressWarnings("unchecked")
            Enumeration<String> parmNames = (Enumeration<String>)request.getParameterNames();
            String arg = "";

            for (Enumeration<String> e = parmNames ; e.hasMoreElements() ;) 
                {
                String parmName = e.nextElement();
                arg = arg + " (\"" + workflow.escape(parmName) + "\".";
                String vals[] = request.getParameterValues(parmName);
                for(int j = 0;j < vals.length;++j)
                    {
                    arg += " \"" + workflow.escape(vals[j]) + "\"";
                    }
                arg += ")";
                }
            String result = BracMat.Eval("deposit$(" + arg + ")");
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
                String messagetext = BracMat.Eval("getStatusCode$(\"404\".\"Deposition of tool failed\")");
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

