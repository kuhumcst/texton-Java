package dk.clarin.tools.rest;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;

@SuppressWarnings("serial")
public class createByToolChoice extends HttpServlet 
    {
    private dk.clarin.tools.create Create;
    public void init(javax.servlet.ServletConfig config) throws javax.servlet.ServletException 
        {
        super.init(config);
        Create = new dk.clarin.tools.create();
        Create.init(config);
        }

    public void doGet(HttpServletRequest request,HttpServletResponse response)
        throws ServletException, IOException 
        {
        Create.Workflow(request,response,"ToolChoice");
        }
        
    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException 
        {
        Create.PostWorkflow(request,response,"ToolChoice");
        }        
    }

