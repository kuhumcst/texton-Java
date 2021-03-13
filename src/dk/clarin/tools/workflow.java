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

import dk.cst.bracmat;
import dk.clarin.tools.ToolsProperties;
import dk.clarin.tools.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.InvalidPathException;
import java.util.zip.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

public class workflow implements Runnable 
    {
//    private static final Logger logger = LoggerFactory.getLogger(workflow.class);

    private bracmat BracMat;
    private String result;

    public workflow(String Result)
        {
        BracMat = new bracmat(ToolsProperties.bootBracmat);
        result = Result; // Ends with the Job Number 
        }

    static private String writeStream(String result, bracmat BracMat, String filename, String jobID, InputStream input)
        {
        /**
         * toolsdata$
         *
         * Return the full file system path to Tool's staging area.
         * The input can be a file name: this name is appended to the returned value.
         */
        String destdir = BracMat.Eval("toolsdata$");
        Path path = null;
        try
            {
            path = Paths.get(destdir+filename);
            try
                {
                byte[] buffer = new byte[4096];
                int n = - 1;
                OutputStream outputF = Files.newOutputStream(path);
                while((n = input.read(buffer)) != -1)
                    {
                    if (n > 0)
                        {
                        outputF.write(buffer, 0, n);
                        }
                    }
                outputF.close();
                }
            catch (Exception e)
                {//Catch exception if any
                //logger.error("Could not write result to file. Aborting job " + jobID + ". Reason:" + e.getMessage());
                BracMat.Eval("abortJob$(" + result + "." + jobID + ")"); 
                }
            }
        catch(InvalidPathException e)
            {//Catch exception if any
            //logger.error("Could not find path to file. Aborting job " + jobID + ". Reason:" + e.getMessage());
            BracMat.Eval("abortJob$(" + result + "." + jobID + ")"); 
            }
        if(path != null)
            return path.toString();
        else
            return "";
        }                    

    private void didnotget200(int code,String result, bracmat BracMat, String jobID)
        {
        String filelist;
        //logger.debug("didnotget200.Code="+Integer.toString(code)+", result="+result+", jobID="+jobID);
        if(code == 202)
            {
            //logger.warn("Got status code 202. Job " + jobID + " is set to wait for asynchronous result.");
            /**
                * waitingJob$
                *
                * Make a job 'waiting'.
                * 
                * Input: JobNr and jobID
                *
                * Affected tables in jboss/server/default/data/tools:
                *     jobs.table
                */
            BracMat.Eval("waitingJob$(" + result + "." + jobID + ")"); 
            //jobs = 0; // No more jobs to process now, quit from loop and wait for result to be sent
            }
        else if(code == 0)
            {
            //jobs = 0; // No more jobs to process now, probably the tool is not integrated at all
            filelist = BracMat.Eval("abortJob$(" + result + "." + jobID + ")"); 
            //logger.warn("abortJob returns " + filelist);
            //logger.warn("Job " + jobID + " cannot open connection to URL ");
            }
        else
            {
            filelist = BracMat.Eval("abortJob$(" + result + "." + jobID + ")"); 
            //logger.warn("abortJob returns " + filelist);
            //logger.warn("Got status code [" + code + "]. Job " + jobID + " is aborted.");
            }
        }

    /**
    *
    * Pipes everything from the reader to the writer via a buffer
    */
    private static void pipe(Reader reader, Writer writer) throws IOException
        {
        char[] buf = new char[1024];
        int read = 0;
        while ((read = reader.read(buf)) >= 0)
            {
            writer.write(buf, 0, read);
            }
        writer.flush();
        }
    
    /**
    * Sends an HTTP GET request to a url
    *
    * @param endpoint - The URL of the server. (Example: " http://www.yahoo.com/search")
    * @param requestString - all the request parameters (Example: "param1=val1&param2=val2"). Note: This method will add the question mark (?) to the request - DO NOT add it yourself
    * @return - The response from the end point
    */

    private int sendRequest(String result, String endpoint, String requestString, bracmat BracMat, String filename, String jobID, boolean postmethod)
        {        
        int code = 0;
        String message = "";
        //String filelist;
        if(  endpoint.startsWith("http://") 
          || endpoint.startsWith("https://")
          )
            {
            // Send a GET or POST request to the servlet
            try
                {
                // Construct data
                String urlStr = endpoint;
                if(postmethod) // HTTP POST
                    {
                    StringReader input = new StringReader(requestString);
                    StringWriter output = new StringWriter();
                    URL endp = new URL(endpoint);

                    HttpURLConnection urlc = null;
                    try
                        {
                        urlc = (HttpURLConnection) endp.openConnection();
                        try
                            {
                            urlc.setRequestMethod("POST");
                            }
                        catch (ProtocolException e)
                            {
                            throw new Exception("Shouldn't happen: HttpURLConnection doesn't support POST??", e);
                            }
                        urlc.setDoOutput(true);
                        urlc.setDoInput(true);
                        urlc.setUseCaches(false);
                        urlc.setAllowUserInteraction(false);
                        urlc.setRequestProperty("Content-type", "text/xml; charset=" + "UTF-8");

                        OutputStream out = urlc.getOutputStream();

                        try
                            {
                            Writer writer = new OutputStreamWriter(out, "UTF-8");
                            pipe(input, writer);
                            writer.close();
                            } 
                        catch (IOException e)
                            {
                            throw new Exception("IOException while posting data", e);
                            } 
                        finally
                            {
                            if (out != null)
                                out.close();
                            }
                        } 
                    catch (IOException e)
                        {
                        throw new Exception("Connection error (is server running at " + endp + " ?): " + e);
                        } 
                    finally
                        {
                        if (urlc != null)
                            {
                            code = urlc.getResponseCode();
                            
                            if(code == 200)
                                {
                                String path = writeStream(result, BracMat, filename, jobID, urlc.getInputStream());
                                if(!path.equals(""))
                                    util.gotToolOutputData(result, jobID, BracMat, path);
                                }
                            else
                                {
                                InputStream in = urlc.getInputStream();
                                try
                                    {
                                    Reader reader = new InputStreamReader(in);
                                    pipe(reader, output);
                                    reader.close();
                                    } 
                                catch (IOException e)
                                    {
                                    throw new Exception("IOException while reading response", e);
                                    } 
                                finally
                                    {
                                    if (in != null)
                                        in.close();
                                    }
                                message = urlc.getResponseMessage();
                                didnotget200(code,result,BracMat,jobID);
                                }                            
                            urlc.disconnect();
                            }
                        else
                            {
                            code = 0;
                            didnotget200(code,result,BracMat,jobID);
                            }
                        }
                    }
                else // HTTP GET
                    {
                    // Send data
                    //logger.debug("GET");
                    if (requestString != null && requestString.length () > 0)
                        {
                        urlStr += "?" + requestString;
                        }
                    URL url = new URL(urlStr);
                    //logger.debug("urlStr="+urlStr);
                    URLConnection conn = url.openConnection ();
                    conn.connect();
                    //logger.debug("connected");
        
                    // Cast to a HttpURLConnection
                    if(conn instanceof HttpURLConnection)
                        {
                        //logger.debug("Cast to HttpURLConnection OK");
                        HttpURLConnection httpConnection = (HttpURLConnection) conn;
                        //logger.debug("hvae HttpURLConnection");
                        code = httpConnection.getResponseCode();
                        //logger.debug("code = "+Integer.toString(code));
                        message = httpConnection.getResponseMessage();
                        //logger.debug("message="+message);
                        BufferedReader rd;
                        StringBuilder sb = new StringBuilder();;
                        //String line;
                        if(code == 200)
                            {
                            //logger.debug("code 200");
                            String path = writeStream(result, BracMat, filename, jobID, httpConnection.getInputStream());
                            if(!path.equals(""))
                                util.gotToolOutputData(result, jobID, BracMat, path);
                            }
                        else
                            {
                            // Get the error response
                            //logger.debug("Get the error response");
                            InputStream error = httpConnection.getErrorStream();
                            //logger.debug("got errorStream");
                            if(error != null)
                                {
                                InputStreamReader inputstreamreader = new InputStreamReader(error);
                                //logger.debug("got inputstreamreader");
                                rd = new BufferedReader(inputstreamreader);
                                //logger.debug("have BufferedReader");
                                int nextChar;
                                while(( nextChar = rd.read()) != -1) 
                                    {
                                    sb.append((char)nextChar);
                                    }
                                rd.close();
                                }
                            else
                                {
                                //logger.debug("error == null");
                                }
                            didnotget200(code,result,BracMat,jobID);
                            //logger.debug("called didnotget200");
                            }
                        }
                    else
                        {
                        //logger.debug("set code = 0");
                        code = 0;
                        didnotget200(code,result,BracMat,jobID);
                        //logger.debug("called didnotget200 (2)");
                        }
                    }
                } 
            catch (Exception e)
                {
                //jobs = 0; // No more jobs to process now, probably the tool is not reachable
                //logger.warn("Job " + jobID + " aborted. Reason:" + e.getMessage());
                /*filelist =*/ BracMat.Eval("abortJob$(" + result + "." + jobID + ")"); 
                }
            }
        else
            {
            //jobs = 0; // No more jobs to process now, probably the tool is not integrated at all
            //logger.warn("Job " + jobID + " aborted. Endpoint must start with 'http://' or 'https://'. (" + endpoint + ")");
            /*filelist =*/ BracMat.Eval("abortJob$(" + result + "." + jobID + ")"); 
            }
        return code;
        }

    private void processPipeLine(String result)
        {
        int jobs = 10; // Brake (or break) after 10 failed iterations. Then something is possibly wrong
        int code = 0;
        boolean asynchronous = false;
        //logger.debug("processPipeLine("+result+")");
        while(jobs > 0 && BracMat.Eval("goodRunningThreads$").equals("y"))
            {
            // Jobs are hierarchically structured:
            //    All jobs with the same job number belong together. They constitute a pipeline.
            //    (Amendment 20110606: if many similar resources are processed batch-wise, 
            //     they share the same job number, whereas the Job ID is incremented monotonically.
            //     In this way, many independent pipelines are made into one big pipeline.)
            //    All jobs with the same job number must be performed sequentially, in increasing order of their job id.
            // Each job number represents one pipeline. A pipeline consists of one or more jobs, each with a jobID that is unique within the job.
            /**
             * getNextJobID$
             *
             * Given the jobNr of a workflow, return the next job that is not pending 
             * (=waiting for an another job to produce some of its inputs).
             * Argument: jobNr
             * Returns: jobID (if job found in jobs.table in jboss/server/default/data/tools)
             *          empty string (if job not found in jobs.table)
             */
            String jobID = BracMat.Eval("getNextJobID$(" + result + ".)"); // second argument (between '.' and ')' ) is empty!
            //logger.debug("getNextJobID returns:"+jobID);
            // Now we have a job that must be launched
            if(jobID.equals(""))
                jobs = 0; // No more jobs on job list, quit from loop
            else
                {
                // getJobArg looks for the trailing number of the result string, e.g. job number "55" if result is "55"

                /**
                 * getJobArg$
                 *
                 * Consults tables jobs.table and tooladm.table in jboss/server/default/data/tools to answer several requests
                 * Arguments: jobNr, jobID and one of the following requests:
                 *      endpoint        the URL where the integrated tool lives
                 *      filename        the name to be given to the output
                 *      method          POST or GET
                 *      requestString   the request string as HTTP-parameters or as XML
                 */
                // endpoint = entry point for tool webservice
                String endpoint      = BracMat.Eval("getJobArg$(" + result + "." + jobID + ".endpoint)");

                // requestString = arguments sent to the tool webservice
                String requestString = BracMat.Eval("getJobArg$(" + result + "." + jobID + ".requestString)");

                // filename = name of the file where the tool output will be stored when the GET gets back.
                String filename      = BracMat.Eval("getJobArg$(" + result + "." + jobID + ".filename)"); 
                String method        = BracMat.Eval("getJobArg$(" + result + "." + jobID + ".method)"); 
                boolean postmethod = method.equals("POST");
                requestString = BracMat.Eval("percentEncodeURL$("+util.quote(requestString) + ")");
                //logger.debug("sendRequest("+requestString+")");
                code = sendRequest(result, endpoint, requestString, BracMat, filename, jobID, postmethod);
                //logger.debug("sendRequest returns code "+Integer.toString(code));
                if(code == 202)
                    asynchronous = true;
                }
            if(code != 200 && code != 202)
                {
                --jobs;
                //logger.info("processPipeLine aborts. jobs=="+Integer.toString(jobs));
                }
            }
        //logger.info("processPipeLine returns");
        }

    public void run() // because it implements Runnable
        {
        processPipeLine(result);
        }

    }

