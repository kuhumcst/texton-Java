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
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.InvalidPathException;

import java.util.zip.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

public class workflow implements Runnable 
    {
    private static final Logger logger = LoggerFactory.getLogger(workflow.class);

    private bracmat BracMat;
    private String JobNR;

    public workflow(String Result)
        {
        BracMat = new bracmat(ToolsProperties.bootBracmat);
        JobNR = Result; // Ends with the Job Number 
        }

    static private String writeStream(String JobNR, bracmat BracMat, String filename, String jobID, InputStream input)
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
                logger.error("Could not write result to file. Aborting job " + jobID + ". Reason:" + e.getMessage());
                BracMat.Eval("abortJob$(" + JobNR + "." + jobID + ")"); 
                }
            }
        catch(InvalidPathException e)
            {//Catch exception if any
            logger.error("Could not find path to file. Aborting job " + jobID + ". Reason:" + e.getMessage());
            BracMat.Eval("abortJob$(" + JobNR + "." + jobID + ")"); 
            }
        if(path != null)
            return path.toString();
        else
            return "";
        }                    

    private void didnotget200(int code,String JobNR, bracmat BracMat, String jobID)
        {
        String filelist;
        logger.warn("DIDNOTGET200.Code="+Integer.toString(code)+", JobNR="+JobNR+", jobID="+jobID);
        if(code == 202)
            {
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
            BracMat.Eval("waitingJob$(" + JobNR + "." + jobID + ")"); 
            }
        else
            {
            logger.warn("didnotget200.Code="+Integer.toString(code)+", JobNR="+JobNR+", jobID="+jobID);
            if(code == 0)
                {
                filelist = BracMat.Eval("abortJob$(" + JobNR + "." + jobID + ")"); 
                logger.warn("Job " + jobID + " cannot open connection to URL ");
                }
            else
                {
                filelist = BracMat.Eval("abortJob$(" + JobNR + "." + jobID + ")"); 
                logger.warn("Got status code [" + code + "]. Job " + jobID + " is aborted.");
                }
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

    private int sendRequest(String JobNR, String endpoint, String requestString, bracmat BracMat, String filename, String jobID, boolean postmethod)
        {        
        int code = 0;
        String message = "";
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
                        throw new Exception("Connection error (is server running at " + endp + " ?): " + e.getMessage());
                        } 
                    finally
                        {
                        if (urlc != null)
                            {
                            code = urlc.getResponseCode();
                            
                            if(code == 200)
                                {
                                String path = writeStream(JobNR, BracMat, filename, jobID, urlc.getInputStream());
                                if(!path.equals(""))
                                    util.gotToolOutputData(JobNR, jobID, BracMat, path);
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
                                didnotget200(code,JobNR,BracMat,jobID);
                                }                            
                            urlc.disconnect();
                            }
                        else
                            {
                            code = 0;
                            didnotget200(code,JobNR,BracMat,jobID);
                            }
                        }
                    }
                else // HTTP GET
                    {
                    // Send data
                    if (requestString != null && requestString.length () > 0)
                        {
                        urlStr += "?" + requestString;
                        }
                    URL url = new URL(urlStr);
                    URLConnection conn = url.openConnection ();
                    try
                        {
                        conn.connect();
                    
                        // Cast to a HttpURLConnection
                        if(conn instanceof HttpURLConnection)
                            {
                            HttpURLConnection httpConnection = (HttpURLConnection) conn;
                            code = httpConnection.getResponseCode();
                            message = httpConnection.getResponseMessage();
                            BufferedReader rd;
                            StringBuilder sb = new StringBuilder();;
                            if(code == 200)
                                {
                                String path = writeStream(JobNR, BracMat, filename, jobID, httpConnection.getInputStream());
                                if(!path.equals(""))
                                    util.gotToolOutputData(JobNR, jobID, BracMat, path);
                                }
                            else
                                {
                                // Get the error response
                                InputStream error = httpConnection.getErrorStream();
                                if(error != null)
                                    {
                                    InputStreamReader inputstreamreader = new InputStreamReader(error);
                                    rd = new BufferedReader(inputstreamreader);
                                    int nextChar;
                                    while(( nextChar = rd.read()) != -1) 
                                        {
                                        sb.append((char)nextChar);
                                        }
                                    rd.close();
                                    }
                                else
                                    {
                                    }
                                didnotget200(code,JobNR,BracMat,jobID);
                                }
                            }
                        else
                            {
                            code = 0;
                            didnotget200(code,JobNR,BracMat,jobID);
                            }
                        }
                    catch (SocketTimeoutException e)
                        {
                        logger.warn("Job " + jobID + " got SocketTimeoutException. Aborted. Reason:" + e.getMessage());
                        BracMat.Eval("abortJob$(" + JobNR + "." + jobID + ")"); 
                        }
                    catch (IOException e)
                        {
                        logger.warn("Job " + jobID + " got IOException. Aborted. Reason:" + e.getMessage());
                        BracMat.Eval("abortJob$(" + JobNR + "." + jobID + ")"); 
                        }
                    }
                } 
            catch (Exception e)
                {
                logger.warn("Job " + jobID + " aborted. Reason:" + e.getMessage());
                BracMat.Eval("abortJob$(" + JobNR + "." + jobID + ")"); 
                }
            }
        else
            {
            logger.warn("Job " + jobID + " aborted. Endpoint must start with 'http://' or 'https://'. (" + endpoint + ")");
            BracMat.Eval("abortJob$(" + JobNR + "." + jobID + ")"); 
            }
        return code;
        }

    private void processPipeLine(String JobNR)
        {
        int jobs = 10; // Brake (or break) after 10 failed iterations. Then something is possibly wrong
        int code = 0;
        boolean asynchronous = false;
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
            String jobID = BracMat.Eval("getNextJobID$(" + JobNR + ".)"); // second argument (between '.' and ')' ) is empty!
            String requestString = "";
            // Now we have a job that must be launched
            if(jobID.equals(""))
                jobs = 0; // No more jobs on job list, quit from loop
            else
                {
                if(jobID.startsWith("-"))
                    {
                    int err = 0;
                    try
                        {
                        err = Integer.parseInt(jobID);
                        }
                    catch (NumberFormatException ex)
                        {
                        logger.warn("err " + err + " cannot be parsed as integer:" + ex.getMessage());
                        }
                    logger.warn("getNextJobID$(" + JobNR + ") returned:" + err);
                    jobs = 0;
                    }
                else
                    {
                    // getJobArg looks for the number in the JobNR string, e.g. "55" or "step55"

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
                    String endpoint      = BracMat.Eval("getJobArg$(" + JobNR + "." + jobID + ".endpoint)");

                    // requestString = arguments sent to the tool webservice
                    requestString = BracMat.Eval("getJobArg$(" + JobNR + "." + jobID + ".requestString)");

                    // filename = name of the file where the tool output will be stored when the GET gets back.
                    String filename      = BracMat.Eval("getJobArg$(" + JobNR + "." + jobID + ".filename)"); 
                    String method        = BracMat.Eval("getJobArg$(" + JobNR + "." + jobID + ".method)"); 
                    boolean postmethod = method.equals("POST");
                    if(!postmethod)
                        requestString = BracMat.Eval("percentEncodeURL$("+util.quote(requestString) + ")");
                    code = sendRequest(JobNR, endpoint, requestString, BracMat, filename, jobID, postmethod);
                    if(code == 202)
                        asynchronous = true;
                    }
                if(code != 200 && code != 202)
                    {
                    --jobs;
                    logger.warn("processPipeLine aborts for requestString ["
                               +requestString
                               +"]. jobs="
                               +Integer.toString(jobs)
                               +", code="
                               +Integer.toString(code)
                               +", jobID="
                               +jobID
                               +", JobNR ["
                               +JobNR
                               +"]"
                               );
                    }
                }
            }
        }

    public void run() // because it implements Runnable
        {
        processPipeLine(JobNR);
        }

    }

