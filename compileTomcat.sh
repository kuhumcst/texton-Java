set CATALINA_BASE=/var/lib/tomcat7
call ant -Divy=true download-ivy 
call ant -DCATALINA_BASE=$CATALINA_BASE -Dtomcat=true war
sudo cp war/tools.war %CATALINA_BASE%/webapps
