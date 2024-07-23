/*
    Copyright 2024, Bart Jongejan
    This file is part of Text Tonsorium (AKA the DK-ClarinTools).

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

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Enumeration;

import java.io.IOException;

@SuppressWarnings("serial")
@MultipartConfig(fileSizeThreshold=1024*1024*10,  // 10 MB 
                 maxFileSize=-1/*1024*1024*50*/,       // 50 MB
                 maxRequestSize=-1/*1024*1024*100*/)    // 100 MB

public class dialog extends HttpServlet 
    {
    private dk.clarin.tools.compute Compute;
    private dk.clarin.tools.rest.poll Poll;
    public void init(ServletConfig config) throws ServletException 
        {
        super.init(config);
        Compute = new dk.clarin.tools.compute();
        Compute.init(config);
        Poll = new dk.clarin.tools.rest.poll();
        Poll.init(config);
        }

    public String whichDialog(HttpServletRequest request)
        {
        Enumeration<String> parmNames = (Enumeration<String>)request.getParameterNames();

        for (Enumeration<String> e = parmNames ; e.hasMoreElements() ;) 
            {
            String parmName = e.nextElement();
            String vals[] = request.getParameterValues(parmName);
            if(parmName.equals("dialog"))
                {
                for(String val : vals)
                    {
                    if(!val.equals(""))
                        {
                        return val;
                        }
                    }
                }
            }
        return "";
        }

    public void doGet(HttpServletRequest request,HttpServletResponse response)
        throws ServletException, IOException 
        {
        String dialog = whichDialog(request);
        if(dialog.equals("poll"))
            {
            Poll.doGet(request,response);
            }
        else
            Compute.Workflow(request,response,dialog/*"specifyGoal"*/);
        }
        
    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException 
        {
        String dialog = whichDialog(request);
        Compute.PostWorkflow(request,response,dialog/*"specifyGoal"*/);
        }        
    }

