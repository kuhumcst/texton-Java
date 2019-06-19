if [ -d /usr/share/tomcat9/lib ]; then
    CATALINA_BASE=/var/lib/tomcat9
    CATALINA_HOME=/usr/share/tomcat9
elif  [ -d /opt/tomcat/latest/lib ]; then
    CATALINA_BASE=/opt/tomcat/latest
    CATALINA_HOME=/opt/tomcat/latest
fi
export CATALINA_BASE
export CATALINA_HOME_
ant -Divy=true download-ivy 
ant -DCATALINA_HOME=$CATALINA_HOME -DtomcatU=true war
sudo cp war/tools.war $CATALINA_BASE/webapps
