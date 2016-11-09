Installation as Tomcat servlet
==============================
This  has been tested with Windows 7 Enterprise and Tomcat 6 and 7, with Windows 8.1 Pro and Tomcat 8 and with Xubuntu 14.04 with Tomcat 7. There are a couple of cotcha's.

Server
------
If you install Tomcat on a machine that has no visibility as a web server, you cannot execute any workflows, because each workflow step involves sending a request to a web service that incorporates a tool. Such requests do, however, not include the input data. Instead, the requested web service has to fetch (HTTP GET) the data from the machine where the workflow is executed from. Still, a lot a interesting things can be done with an installation of the workflow engine on a machine that has no webserver status.

Logging
-------
You have to configure Tomcat to use slf4j and logback. How to do this is explained at http://adfinmunich.blogspot.de/2012/03/how-to-configure-tomcat-to-use-slf4j.html:

1. Download tomcat-juli.jar and tomcat-juli-adapters.jar that are available as an "extras" component for Tomcat. I downloaded these files from http://ftp.download-by.net/apache/tomcat/tomcat-7/v7.0.72/bin/extras/. 

From http://www.slf4j.org/dist/ I downloaded slf4j-api-1.6.4.jar (I did not try newer versions.) I downloaded logback-1.1.7.tar.gz from http://logback.qos.ch/download.html, at that time the newest version.

2. Put the following jars into $CATALINA_HOME/lib.

        log4j-over-slf4j-1.6.4.jar
        logback-classic-1.0.0.jar      (or the version you downloaded)
        logback-core-1.0.0.jar         (see above)
        slf4j-api-1.6.4.jar
        tomcat-juli-adapters.jar

3. Replace $CATALINA_HOME/bin/tomcat-juli.jar with tomcat-juli.jar from "extras"

4. Delete $CATALINA_BASE/conf/logging.properties to prevent java.util.logging generating zero length log files.

5. Add logback.xml into $CATALINA_HOME/lib.

    Example:

        <configuration>
         <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
           <!-- encoders are assigned the type
                ch.qos.logback.classic.encoder.PatternLayoutEncoder by default -->
           <encoder>
           <!--
             <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
           -->
             <pattern>%d{HH:mm:ss.SSS} %-5level %logger{50} - %msg%n</pattern>
           </encoder>
         </appender>
     
         <root level="INFO">
           <appender-ref ref="STDOUT" />
         </root>
        </configuration>

Sending mail
------------
I am not sure about what I did to make it work. I added both mail.jar (from javamail1_4_7.zip) and commons-email-1.3.3.jar (from commons-email-1.3.3-bin.zip) to $CATALINA_HOME/lib (C:\Program Files\Apache Software Foundation\Tomcat 8.0\lib on my system). That solved the compilation issues. As my machine is not expected to send me any emails, I do not know whether these actions are necessary and sufficient on a production system.

How?
----

1. Install apache-ant

2. Install a JDK. Make sure that the environment variable JAVA_HOME points to the JDK and not to e.g., a JRE. Also, the PATH environment variable should point to the bin-directory of the JDK, so javac etc. can be found.

3. Install Tomcat. For Windows, look for the  "32-bit/64-bit Windows Service Installer"at http://tomcat.apache.org/download-80.cgi. In Linux, you may have to install two packages: tomcat7 and tomcat7-admin  

4. (Windows:) Create (if it doesn't exist) an environment variable CATALINA_HOME that points to e.g., "C:\Program Files\Apache Software Foundation\Tomcat 8.0". This variable is used by scripts discussed below.

5. Create a manager-gui. See Apache Software Foundation\Tomcat 8.0\conf\tomcat-users.xml 

6. If you use Windows, install Visual Studio. E.g. a community edition. Linux users can use the GNU C/C++ compiler.

7. You can either download the module from SVN or from GitHub. The latter option offers a somewhat smaller package, without files that are very clarin.dk specific.

        svn checkout http://devtools.clarin.dk/svn/clarin/trunk/dkclarin-services/tools 
   
    For GitHub, follow these steps:

        git clone https://github.com/kuhumcst/DK-ClarinTools.git
        cd DK-ClarinTools/
        git clone https://github.com/BartJongejan/Bracmat.git

8. If you plan to let the tools webservice use the 'work' folder (a folder in the DK-ClarinTools) as work folder, then make sure that Tomcat has read and write persmissions in 'work' and its sub-folders.

9. Compile and link the bracmat JNI (Java Native Interface). Linux users are adviced to follow the steps in compileAndTestJNI.sh. The easiest way for Windows users is to run makeJNI.bat. You may have to edit this script to correct the path to vcvarsall.bat. If you don't want the tools' log file to become /var/log/clarin/tools.log, you must define an environment variable TOOLSLOG with a value that is the full path (including the file name) of the log file. If instead of using the batch file you want to use the Visual Studio IDE, you will build the dll and the jar component of the JNI in separate steps.

    How to build bracmat.dll in Visual Studio? You need to add these source files to the DLL project: bracmatdll.cpp bracmatso.c dk_cst_bracmat.c json.c xml.c. The file bracmat.c is included by bracmatso.c after a number of #DEFINEs that turn off 'dangerous' functionality, such as the ability to run system commands or to open a file and not close it. Make sure to define BRACMATDLL_EXPORTS in the C/C++ Preprocessor settings. The name of the library should be bracmat.dll. Linux users are advised to follow the steps in compileAndTestJNI.txt.
   
    Put the shared bracmat library (bracmat.dll in Windows) in Tomcat's bin directory. This is the last step in makeJNI.bat, but requires that you run this script as administrator. In Linux, you can put libbracmatso in /usr/lib. Again, you need to have administrator rights.
   
10. Edit the file properties_local.xml (Windows) or properties_local_ubuntu.xml (Linux Ubuntu). Adapt "Email properties", "Location of staging area" and <entry key="password"></entry>. Do not mind "accepted-workflow-starter", it is not used in this version.

11. Create the tools module. This module consists of a number of java webservices. Almost each of them links to the bracmat JNI. You can run one of the batch files (Windows users), such as compileTomcat.bat. These batch files automate everything in the following steps. 

    In ivy.xml, comment out the line 
   
        <dependency org="javax.servlet" name="servlet-api" rev="2.4"/>
    
    Also in ivy.xml, change the version of two jar files to make them consistent with the actions described under "Logging" above.
   
        <dependency org="org.slf4j" name="slf4j-api" rev="1.6.4"/>
        <!--dependency org="org.slf4j" name="slf4j-api" rev="1.5.11"/-->
        <dependency org="org.slf4j" name="slf4j-log4j12" rev="1.6.4"/>
        <!--dependency org="org.slf4j" name="slf4j-log4j12" rev="1.5.11"/-->

    Copy servlet-api.jar from tomcat's lib folder to the tool project's lib folder.
   
    Delete servlet-api-2.4.jar from the tool project's lib folder
   
    To create the module, run, depending on your OS

    Ubuntu, Tomcat:   
        ant -Divy=true -DtomcatU=true war  
    Linux, JBoss, production server:
        ant -Divy=true -Dprod=true war  
    Linux, JBoss, test server:
        ant -Divy=true -Ddev=true war  
    Windows, Tomcat on C:
        ant -Divy=true -Dtomcat=true war  
    Windows, Tomcat on D:
        ant -Divy=true -DtomcatD=true war  
    
    Please check and if necessary edit build.xml before running ant.

12. Deploy the new war file.

13. Follow the link http://localhost/tools

14. If all goes well, you see a page called CLARIN-DK tools (provisory UI, idea testing). This is an unofficial interface to the tools module. As some tables are either in English or in Danish, you first have to set the language.

    Go to the 'Set language' form some way down the window. Choose Danish or English and press the 'set language' button. Go back to the screen were you came from with the browser's 'back' button.
   
    Go to the 'Evaluate program code' form near the bottom of the window. Now we need to create working copies of metadata for tools. These metadata can be read from file alltables.bra. In the text area, write the following text and press the 'Bracmat' button.

        readTable$"alltables.bra"

(or readTable$"alltables.GPL")

15. Now you can try to create a workflow for a file that you upload or for a text that you write. You do this in one of the two forms close to the top of the window. ('Apply workflow to uploaded file(s)' and 'Apply workflow to typed-in text only.')
