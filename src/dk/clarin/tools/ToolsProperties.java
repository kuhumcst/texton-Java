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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
* ToolsProperties.java
* Read properties file and sets 
* properties accordingly
*/
public class ToolsProperties 
    {
    // Static logger object.  
    private static final Logger logger = LoggerFactory.getLogger(ToolsProperties.class);

    /**
    * This is where all configuration goes. 
    * each of these strings should have an entry in properties_*.xml
    **/

    // Host names
    //public static String wwwServer                    = "";
    
    // names used by Tools
    //public static String baseUrlTools               = "";
    public static String documentRoot               = "";
    public static String stagingArea                = "";
    public static String deleteAfterMillisec        = "";
    public static String tempdir                    = "";
    //public static String password                   = "";
    //public static String salt                       = "";

    public static String bootBracmat                = "";

    public static void readProperties()
        {
        try
            {
            //FileInputStream fis = new java.io.FileInputStream("conf/properties.xml");
            InputStream fis = Files.newInputStream(Paths.get("conf/properties.xml"));
            readProperties(fis);
            }
        catch (IOException io)
            {
            logger.warn("could not read , using default values" + " message is " + io.getMessage()); 
            }    
        }

    public static void readProperties(InputStream fis)
        {
        Properties prop = new Properties();
        try 
            {
            prop.loadFromXML(fis);

            // Host names
            //infraServer            = prop.getProperty("infra-server");
            //wwwServer              = prop.getProperty("www-server");  // Set in Bracmat table 'meta/properties'

            // Host names used by Tools
//            baseUrlTools           = prop.getProperty("baseUrlTools");// Set in Bracmat table 'meta/properties'
            stagingArea            = prop.getProperty("stagingArea"); // Used in Java code



            // System paths used by Tools
            documentRoot           = prop.getProperty("documentRoot");  // Used in Java code
            String toolsHome       = prop.getProperty("toolsHome");     // Used in Java code (here!)
            String bracmatCode     = prop.getProperty("toolsProg");     // Used in Java code (here!)
            deleteAfterMillisec    = prop.getProperty("deleteAfterMillisec");  // Used in Java code
            bootBracmat            =  "get$\"" 
                                    + toolsHome                         // Used in Java code (here!)
                                    + bracmatCode                       // Used in Java code (here!)
                                    + "\"&!toolsProg&(\""
                                    + toolsHome                         // Used in Java code (here!)
                                    + "\":?toolshome)&(\"" 
//                                    + baseUrlTools                      // Set in Bracmat table 'meta/properties'
//                                    + "\":?baseUrlTools)&(\"" 
                                    + documentRoot                      // Used in Java code
                                    + "\":?documentRoot)&(\""
//                                    + wwwServer                         // Set in Bracmat table 'meta/properties'
//                                    + "\":?wwwServer)&(\""
                                    + deleteAfterMillisec               // Used in Java code
                                    + "\":?deleteAfterMillisec)&(\""
                                    + stagingArea                       // Used in Java code
                                    + "\":?stagingArea)&ok|fail"
                                    ;
            tempdir                = prop.getProperty("tempdir");   // Used in Java code
            /*
            password               = prop.getProperty("password");  // Used in Java code, but set in Bracmat table 'meta/properties'
            salt                   = prop.getProperty("salt");      // Used in Java code, but set in Bracmat table 'meta/properties'
            */
            fis.close();
            } 
        catch (IOException io)
            {
            logger.warn("could not read , using default values" + " message is " + io.getMessage()); 
            }
        }
    }


