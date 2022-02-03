if  [ -d /opt/tomcat-texton/lib ]; then
    CATALINA_BASE=/opt/tomcat-texton
    CATALINA_HOME=/opt/tomcat-texton
elif  [ -d /opt/latestTomcat/lib ]; then
    CATALINA_BASE=/opt/latestTomcat
    CATALINA_HOME=/opt/latestTomcat
elif [ -d /opt/tomcat/latest/lib ]; then
    CATALINA_BASE=/opt/tomcat/latest
    CATALINA_HOME=/opt/tomcat/latest
elif [ -d /usr/share/tomcat9/lib ]; then
    CATALINA_BASE=/var/lib/tomcat9
    CATALINA_HOME=/usr/share/tomcat9
fi
export CATALINA_BASE
export CATALINA_HOME
ant clean-all
ant -Divy=true download-ivy 
ant -DCATALINA_HOME=${CATALINA_HOME} -DUbuntu=true war
sudo cp war/texton.war $CATALINA_BASE/webapps
