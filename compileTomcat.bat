set JAVA_HOME=C:\Program Files\Java\jdk-12.0.2
set ANT_HOME=C:\apache-ant-1.10.7
set CATALINA_BASE="C:\Program Files\Apache Software Foundation\Tomcat 9.0"
set CATALINA_HOME=C:\Program Files\Apache Software Foundation\Tomcat 9.0
call ant -Divy=true download-ivy 
call ant -DCATALINA_BASE=%CATALINA_BASE% -Dwindows=true war
copy war\texton.war %CATALINA_BASE%\webapps
