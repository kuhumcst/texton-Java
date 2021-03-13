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

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;


@SuppressWarnings("serial")
public class createByGoalChoice extends HttpServlet 
    {
    private dk.clarin.tools.enact Enact;
    public void init(javax.servlet.ServletConfig config) throws javax.servlet.ServletException 
        {
        super.init(config);
        Enact = new dk.clarin.tools.enact();
        Enact.init(config);
        }

    public void doGet(HttpServletRequest request,HttpServletResponse response)
        throws ServletException, IOException 
        {
        Enact.Workflow(request,response,"createByGoalChoice");
        }
        
    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException 
        {
        Enact.PostWorkflow(request,response,"createByGoalChoice");
        }        
    }

