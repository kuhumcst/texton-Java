set CATALINA_BASE="C:\Program Files\Apache Software Foundation\Tomcat 8.5"
set CATALINA_HOME=C:\Program Files\Apache Software Foundation\Tomcat 8.5
call ant -Divy=true download-ivy 
call ant -DCATALINA_BASE=%CATALINA_BASE% -Dtomcat=true war
copy war\tools.war %CATALINA_BASE%\webapps
