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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
//import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.Properties;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
* util.java
* Sundry handy functions
*/
public class util 
    {
    // Static logger object.  
    private static final Logger logger = LoggerFactory.getLogger(util.class);

    public static String PBKDF2string(String password)
        {
        String str;
        str = "";
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        logger.debug("PBKDF2stringpassword=["+password+"]");
        try
            {
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 512);
            try
                {
                SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
                byte[] hash = factory.generateSecret(spec).getEncoded();
                String encodedHash = Base64.getEncoder().encodeToString(hash);
                String encodedSalt = Base64.getEncoder().encodeToString(salt);
                str = "<entry key=\"password\">" + encodedHash + "</entry><entry key=\"salt\">" + encodedSalt + "</entry>";
                logger.debug("XMLprop=["+str+"]");
                //String decoded = new String(Base64.getDecoder().decode(encodedSalt.getBytes()));
                //logger.debug("decoded:{" + decoded + "}");
                }
            catch(InvalidKeySpecException e)
                {
                }
            }
        catch(NoSuchAlgorithmException e)
            {
            }
        return str;        
        }

    public static boolean goodToPass(String GivenPassword,String StoredPassword,String StoredSalt)
        {
        boolean result;
        result = false;
        //logger.debug("GivenPassword["+GivenPassword+"] StoredPassword["+StoredPassword+"] StoredSalt["+StoredSalt+"]");
        byte[] salt = Base64.getDecoder().decode(StoredSalt);
        //logger.debug("salt:" + Integer.toString(salt.length));
        try
            {
            KeySpec spec = new PBEKeySpec(GivenPassword.toCharArray(), salt, 65536, 512);
            try
                {
                SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
                byte[] hash = factory.generateSecret(spec).getEncoded();
                String encodedHash = Base64.getEncoder().encodeToString(hash);
//                logger.debug("encodedHash={"+encodedHash+"}");
                result = encodedHash.equals(StoredPassword);
                }
            catch(InvalidKeySpecException e)
                {
                }
            }
        catch(NoSuchAlgorithmException e)
            {
            }
        return result;
        }

    public static String escape(String str)
        {
        int len = str.length();
        StringBuilder sb = new StringBuilder((3 * len)/ 2); 
        for(int i = 0;i < str.length();++i)
            {
            if(str.charAt(i) == '\\' || str.charAt(i) == '"')
                {
                sb.append('\\');
                }
            sb.append(str.charAt(i));
            }
        return sb.toString();
        }
/*
    public static String hexDigest(String str, String digestName)
        {
        try 
            {
            MessageDigest md = MessageDigest.getInstance(digestName);
            byte[] digest = md.digest(str.getBytes(StandardCharsets.UTF_8));
            char[] hex = new char[digest.length * 2];
            for (int i = 0; i < digest.length; i++) 
                {
                hex[2 * i] = "0123456789abcdef".charAt((digest[i] & 0xf0) >> 4);
                hex[2 * i + 1] = "0123456789abcdef".charAt(digest[i] & 0x0f);
                }
            String result = new String(hex);
            logger.debug("hex=["+result+"]");
            return result;
            }
        catch (NoSuchAlgorithmException e) 
            {
            throw new IllegalStateException(e);
            }
        }
*/
    public static String quote(String str)
        {
        return "\"" + escape(str) + "\"";
        }
/*
    static private String FilenameRelations(String name,bracmat BracMat)
        {
        return BracMat.Eval("FilenameRelations$(" + util.quote(name) + ")"); 
        }
*/


    /**
     * Check whether the new data is TEI data. If that is the case, a new 
     * that adds metadata to the new data.
     * Another task is to create relation files, telling how the new data
     * is related to previously created data or to the input.
     * result   - a string that ends with a Job number (JobNr).
     * jobID    - identifies the step in the current Job.
     * path     - Complete path to te new data, including the file name.
     */
    public static void gotToolOutputData(String result, String jobID, bracmat BracMat, String path)
        {
        logger.debug("path="+path);
        try
            {
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            String date = sdf.format(cal.getTime());

            String destdir = BracMat.Eval("toolsdata$");
            String newResource;
            String TEIformat = BracMat.Eval("isTEIoutput$(" + result + "." + jobID + ")");
            /**
             * doneJob$
             *
             * Marks a job as 'done' in jobs.table in jboss/server/default/data/tools
             * Constructs a CTBID from date, JobNr and jobID
             * Makes sure there is a row in table CTBs connecting
             *      JobNr, jobID, email and CTBID
             * Creates isDependentOf and isAnnotationOf relations
             * Affected tables:
             *      jobs.table
             *      CTBs.table
             *      relations.table
             * Arguments: jobNR, JobID, spangroup with annotation and date. 
             */
            if(TEIformat.equals(""))
                {
                newResource = BracMat.Eval("doneJob$(" + result + "." + jobID +               ".."               + util.quote(date) + ")"); 
                }
            else
                {
                logger.debug("result="+result);
                logger.debug("jobID="+jobID);
                logger.debug("date="+date);
                String toEval =            "doneJob$(" + result + "." + jobID + "." + util.quote(path) + "." + util.quote(date) + ")";
                logger.debug(toEval);
                newResource = BracMat.Eval(toEval); 
                logger.debug("doneJob-result");
                logger.debug(newResource);
                }

            /**
             * relationFile$
             *
             * Create a relation file ready for deposition together with an annotation.
             *
             * Input: JobNr and jobID
             * Output: String that can be saved as a semicolon separated file.
             * Consulted tables:
             *      relations.table     (for relation type, ctb and ctbid
             *      CTBs.table          (for ContentProvider and CTBID)
             */
             /*
            String relations = BracMat.Eval("relationFile$(" + result + "." + jobID + ")"); 
            // Create relation file
            BufferedWriter bufferedWriter = Files.newBufferedWriter(Paths.get(FilenameRelations(path,BracMat)), StandardCharsets.UTF_8);
            bufferedWriter.write(relations);
            bufferedWriter.close();
            */
            }
        catch (Exception e)
            {//Catch exception if any
            logger.error("Could not write result to file. Aborting job " + jobID + ". Reason:" + e.getMessage());
            /**
             * abortJob$
             *
             * Abort, given a JobNr and a jobID, the specified job and all
             * pending jobs that depend on the output from the (now aborted) job.
             * Rather than removing the aborted jobs from the jobs.table list, they are
             * marked 'aborted'.
             * Result (as XML): a list of (JobNr, jobID, toolName, items)
             */
            BracMat.Eval("abortJob$(" + result + "." + jobID + ")"); 
            }
        }                    
    }


