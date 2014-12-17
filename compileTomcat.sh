CATALINA_BASE=/var/lib/tomcat7
CATALINA_HOME=/usr/share/tomcat7
export CATALINA_BASE
export CATALINA_HOME_
ant -Divy=true download-ivy 
ant -DCATALINA_HOME=$CATALINA_HOME -DtomcatU=true war
sudo cp war/tools.war $CATALINA_BASE/webapps
