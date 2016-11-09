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

import java.io.FileInputStream;
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

    // Email properties
    public static String mailFromName               = "";
    public static String mailPort                   = "";
    public static String mailServer                 = "";
    public static String mailFrom                   = "";
    public static String admEmail                   = "";

    // eSciDoc core properties
    public static String repoServiceUrl             = "";
    public static String adminUserHandle            = "";

    // Host names
    public static String coreServer                 = "";
    public static String wwwServer                    = "";
    
    // names used by Tools
    public static String baseUrlTools               = "";
    public static String documentRoot               = "";
    public static String stagingArea                = "";
    public static String deleteAfterMillisec        = "";
    public static String acceptedWorkflowStarter    = "";
    public static String tempdir                    = "";
    public static String password                   = "";

    public static String bootBracmat                = "";

    public static void readProperties()
        {
        try
            {
            FileInputStream fis = new java.io.FileInputStream("conf/properties.xml");
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

            // Email properties
            mailFromName           = prop.getProperty("mail-from-name");
            mailPort               = prop.getProperty("mail-port");
            mailServer             = prop.getProperty("mail-server");
            mailFrom               = prop.getProperty("mail-from");
            admEmail               = prop.getProperty("adm-email");

            // eSciDoc core properties
            repoServiceUrl         = prop.getProperty("repoServiceUrl");
            adminUserHandle        = prop.getProperty("admin-user-handle");

            // Host names
            //infraServer            = prop.getProperty("infra-server");
            coreServer             = prop.getProperty("core-server");
            wwwServer              = prop.getProperty("www-server");

            // Host names used by Tools
            baseUrlTools           = prop.getProperty("baseUrlTools");
            stagingArea            = prop.getProperty("stagingArea");



            // System paths used by Tools
            documentRoot           = prop.getProperty("documentRoot");
            String toolsHome       = prop.getProperty("toolsHome");
            String bracmatCode     = prop.getProperty("toolsProg");
            bootBracmat            =  "get$\"" 
                                    + toolsHome 
                                    + bracmatCode 
                                    + "\"&!toolsProg&(\""
                                    + toolsHome
                                    + "\":?toolshome)&(\"" 
                                    + baseUrlTools
                                    + "\":?baseUrlTools)&(\"" 
                                    + repoServiceUrl
                                    + "\":?repoServiceUrl)&(\""
                                    + documentRoot
                                    + "\":?documentRoot)&(\""
                                    + wwwServer
                                    + "\":?wwwServer)&(\""
                                    + stagingArea
                                    + "\":?stagingArea)&ok|fail"
                                    ;
            deleteAfterMillisec    = prop.getProperty("deleteAfterMillisec");
            acceptedWorkflowStarter= prop.getProperty("accepted-workflow-starter");
            tempdir                = prop.getProperty("tempdir");
            password               = prop.getProperty("password");
            fis.close();
            } 
        catch (IOException io)
            {
            logger.warn("could not read , using default values" + " message is " + io.getMessage()); 
            }
        }
    }


