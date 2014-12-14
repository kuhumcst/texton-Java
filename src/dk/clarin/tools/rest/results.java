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

import dk.cst.*;
import dk.clarin.tools.ToolsProperties;
import dk.clarin.tools.workflow;
import java.io.*;
import java.util.Enumeration;
import java.util.zip.*;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


/**
  * results
  *
  * Show links to all results, intermediary or not.
  *
  */

@SuppressWarnings("serial")
public class results extends HttpServlet 
    {
    private static final Logger logger = LoggerFactory.getLogger(results.class);
    private static final int BUFFER = 2048;
    private bracmat BracMat;

    public void init(ServletConfig config) throws ServletException 
        {
        InputStream fis = config.getServletContext().getResourceAsStream("/WEB-INF/classes/properties.xml");
        ToolsProperties.readProperties(fis);		
        BracMat = new bracmat(ToolsProperties.bootBracmat);
        super.init(config);
        }


    private void zip(String path,String name,ZipOutputStream out)
        {
        logger.debug("zip({}, {})",path,name);
        try
            {
            BufferedInputStream origin = null;
            byte data[] = new byte[BUFFER];
            FileInputStream fi = new FileInputStream(path+name);
            origin = new BufferedInputStream(fi, BUFFER);
            ZipEntry entry = new ZipEntry(name);
            out.putNextEntry(entry);
            int count;
            while((count = origin.read(data, 0, BUFFER)) != -1) 
                {
                out.write(data, 0, count);
                }
            origin.close();
            }
        catch(FileNotFoundException e)
            {
            logger.error("zip:FileNotFoundException {}",e.getMessage());
            }            
        catch(IOException e)
            {
            logger.error("zip:IOException {}",e.getMessage());
            }
        }
        
    private void zipstring(String name,ZipOutputStream out,String str)
        {
        logger.debug("zipstring({}, {})",name,str);
        try
            {
            byte data[] = str.getBytes();
            logger.debug("has data");
            ZipEntry entry = new ZipEntry(name);
            logger.debug("has entry");
            out.putNextEntry(entry);
            logger.debug("entry put");
            int count = data.length;
            logger.debug("count {}",count);
            out.write(data, 0, count);
            logger.debug("data written");
            }
        catch(IOException e)
            {
            logger.error("zipstring:IOException {}",e.getMessage());
            }
        logger.debug("zipstring ok");
        }

    public void doPost(HttpServletRequest request,HttpServletResponse response)
        throws ServletException, IOException 
        {
        response.setContentType("text/html; charset=UTF-8");
        response.setStatus(200);
        PrintWriter out = response.getWriter();
        if(BracMat.loaded())
            {
            @SuppressWarnings("unchecked")
            Enumeration<String> parmNames = (Enumeration<String>)request.getParameterNames();

            //String arg;
            //arg = "";
            String job;
            for (Enumeration<String> e = parmNames ; e.hasMoreElements() ;) 
                {
                String parmName = e.nextElement();
                logger.debug("parm {} = {}",parmName,request.getParameterValues(parmName)[0]);
                if(parmName.equals("JobNr"))
                    {
                    job = request.getParameterValues(parmName)[0];
                    String filelist = BracMat.Eval("doneAllJob$(" + job + ")");
                    logger.debug("filelist:" + filelist);
    		        String localFilePath = ToolsProperties.documentRoot + ToolsProperties.stagingArea;
                    String toolsdataURL = BracMat.Eval("toolsdataURL$");
                    String body = "<h1>Resultater</h1>";
                    String Body = null;
                    
                    try 
                        {
                        FileOutputStream zipdest = null;
                        ZipOutputStream zipout = null;
                        Element workflowElement = null;
                        String workflowString = null;

                        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                        DocumentBuilder db = dbf.newDocumentBuilder();
                        InputSource is = new InputSource();
                        is.setCharacterStream(new StringReader(filelist));

                        Document doc = db.parse(is);
                        NodeList steps = doc.getElementsByTagName("steps");
                        NodeList nodes = ((Element)steps.item(0)).getElementsByTagName("step");

                        logger.debug("nodes.getLength:" + nodes.getLength());
                        if(nodes.getLength() > 0)
                            {
                            NodeList workflowlist = doc.getElementsByTagName("workflow");
                        logger.debug("workflowlist done");
                            workflowElement = (Element)workflowlist.item(0);
                        logger.debug("workflowElemet done {}",workflowElement);
                            workflowString = workflow.getCharacterDataFromElement(workflowElement);
                        logger.debug("workflowString {}", workflowString);
                            Body = "<h1>Resultater</h1><h2>Workflow</h2><p>"+workflowString+"</p>";
                        logger.debug("Body {}", Body);
                            body += "<h2>Workflow</h2><p>"+workflowString+"</p>";
                            body += "<h2>Produceret data</h2><p><a target=\"_blank\" href=\"" + toolsdataURL + job + ".zip\">Alt i en zip-fil</a></p>";
                        logger.debug("body {}", body);
                            zipdest = new FileOutputStream(localFilePath + job + ".zip");
                            zipout = new ZipOutputStream(new BufferedOutputStream(zipdest));
                        logger.debug("zipout {}", zipdest);
                            }
                        else
                            {
                            boolean exists = (new File(localFilePath + job + ".zip")).exists();
                            if(exists)
                                body += "<p><a target=\"_blank\" href=\"" + toolsdataURL + job + ".zip\">Alt i en zip-fil</a></p>";
                            else                                
                                body += "<p>Vi beklager, zip-filen er allerede blevet hentet og er derfor slettet fra serveren.</p>";
                            }
                        body += "<h3>Bemærk!</h3><ul>\n"
                             +  "<li>Zip-filen kan hentes én gang, hvorefter den straks slettes fra serveren.</li>\n"
                             +  "<li>Ikke-hentede resultaterne slettes efter et par dage.</li></ul>\n";
                        String resources = "";
                        if(nodes.getLength() > 0)
                            {
                            body += "<h2>Trin</h2>";
                            Body += "<h2>Trin</h2>";
                            }
                        // iterate over the results
                        for (int i = 0; i < nodes.getLength(); ++i) 
                            {
                            Element element = (Element) nodes.item(i);

                            NodeList JobNrlist = element.getElementsByTagName("JobNr");
                            Element JobNrelement = (Element) JobNrlist.item(0);
                            String JobNr = workflow.getCharacterDataFromElement(JobNrelement);
                        logger.debug("JobNr:" + JobNr);

                            NodeList JobIDlist = element.getElementsByTagName("JobId");
                            Element JobIDelement = (Element) JobIDlist.item(0);
                            String JobID = workflow.getCharacterDataFromElement(JobIDelement);
                        logger.debug("JobID:" + JobID);

                            NodeList namelist = element.getElementsByTagName("name");
                            Element line = (Element) namelist.item(0);
                            String filename = workflow.getCharacterDataFromElement(line);
                        logger.debug("filename:" + filename);

                            NodeList toollist = element.getElementsByTagName("tool");
                            Element toolelement = (Element) toollist.item(0);
                            String tool = workflow.getCharacterDataFromElement(toolelement);

                            NodeList Ilist = element.getElementsByTagName("input");
                            Element Ielement = (Element) Ilist.item(0);
                            String I = workflow.getCharacterDataFromElement(Ielement);

                            NodeList Olist = element.getElementsByTagName("output");
                            Element Oelement = (Element) Olist.item(0);
                            String O = workflow.getCharacterDataFromElement(Oelement);

                            logger.debug("files[" + i + "]=" + filename);
                            if(filename.startsWith("fejl"))
						        {
						        body += "<h2>Følgende trin fejlede:</h2>\n<dl><dt>";
						        body += "Trin " + JobID + "</dt><dd>" + tool + "</dd></dl>\n";
						        Body += "<h2>Følgende trin fejlede:</h2>\n<dl><dt>";
						        Body += "Trin " + JobID + "</dt><dd>" + tool + "</dd></dl>\n";
						        }
					        else
						        {
						        //String href2 = toolsdataURL + workflow.Filename(filename);
						        //String href2nometa = toolsdataURL + workflow.FilenameNoMetadata(filename);
						        //String href2relations = toolsdataURL + workflow.FilenameRelations(filename);
						        String Href2 = workflow.Filename(filename);
						        String Href2nometa = workflow.FilenameNoMetadata(filename);
						        String Href2relations = workflow.FilenameRelations(filename);

                                zip(localFilePath,workflow.Filename(filename),zipout);
                                zip(localFilePath,workflow.FilenameNoMetadata(filename),zipout);
                                zip(localFilePath,workflow.FilenameRelations(filename),zipout);


						        NodeList itemslist = element.getElementsByTagName("item");
						        if(itemslist.getLength() > 0)
							        {
							        for (int j = 0; j < itemslist.getLength(); ++j) 
								        {
								        Element item = (Element) itemslist.item(j);

								        NodeList idlist = item.getElementsByTagName("id");
								        Element idelement = (Element) idlist.item(0);
								        String id = workflow.getCharacterDataFromElement(idelement);

								        NodeList titlelist = item.getElementsByTagName("title");
								        Element titleelement = (Element) titlelist.item(0);
								        String title = workflow.getCharacterDataFromElement(titleelement);
								        resources += id + " \'" + title + "\'<br />\n";
								        }
							        }
							    if(O.indexOf("TEI") >= 0)
							        {
						            body += "<dl><dt>Trin " + JobID + "</dt><dd>" + tool +  ": ";
						            Body += "<dl><dt>Trin " + JobID + "</dt><dd>" + tool 
							             +  ": <a href=\"" + Href2          + "\">inklusiv metadata</a>, "
							             +  "<a href=\"" + Href2relations + "\">relationsfil</a>, "
							             +  "<a href=\"" + Href2nometa    + "\">uden metadata</a>";
							        }
							    else
							        {
						            body += "<dl><dt>Trin " + JobID + "</dt><dd>" + tool +  ": "; 
						            Body += "<dl><dt>Trin " + JobID + "</dt><dd>" + tool 
							             +  "<a href=\"" + Href2nometa    + "\">data</a>, "
							             +  "<a href=\"" + Href2relations + "\">relationsfil</a>";
							        }
						        if(!(I.equals("")))
						            {
						            body += "<dl><dt>Input</dt><dd>" + I + "</dd></dl>";
						            Body += "<dl><dt>Input</dt><dd>" + I + "</dd></dl>";
						            }
						        if(!(O.equals("")))
						            {
						            body += "<dl><dt>Output</dt><dd>" + O + "</dd></dl>";
						            Body += "<dl><dt>Output</dt><dd>" + O + "</dd></dl>";
						            }
    				            body +=  "</dd></dl>\n";
    				            Body +=  "</dd></dl>\n";
						        }
                            }
                        if(!resources.equals(""))
                            {
				            body += "<h2>Workflowets input:</h2><p>" + resources + "</p>";
				            Body += "<h2>Workflowets input:</h2><p>" + resources + "</p>";
                            }
                        body += "<p>Hvis ovenstående oplysninger ikke er rigtige, eller du har spørgsmål, "
                             +  "kan du henvende dig på mail-adressen admin@clarin.dk</p>\n";
                        out.println("<html><head><meta charset=\"UTF-8\"><title>Resultater fra Tools</title></head><body>");
                       /* Next line is not formatted so nice. dl's are not properly layed out.
                       // body = BracMat.Eval("HTMLbodyContainer$(" + workflow.quote(body) + ")");
                       */
                        out.println(body);
                        out.println("</body></html>");
                        if(nodes.getLength() > 0)
                            {                        
                            Body = "<html><head><meta charset=\"UTF-8\"><title>Resultater fra Tools</title></head><body>" + Body + "</body></html>";
                            logger.debug("Body {}",Body);
                            zipstring("index.html",zipout,Body);
                            zipout.close();
                            }
                        }
                    catch (Exception er) 
                        {
                        logger.error(er.getMessage());
                        }
                    break;
                    }
                }
            }
        else
            {
            throw new ServletException("Bracmat is not loaded. Reason:" + BracMat.reason());
            }
        }

        public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
            {        
            doPost(request, response);
            }
    }

