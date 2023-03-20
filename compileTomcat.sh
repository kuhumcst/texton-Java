if  [ -d /opt/tomcat-texton/lib ]; then
    CATALINA_BASE=/opt/tomcat-texton
    CATALINA_HOME=/opt/tomcat-texton
elif  [ -d /opt/latestTomcat/lib ]; then
    CATALINA_BASE=/opt/latestTomcat
    CATALINA_HOME=/opt/latestTomcat
elif [ -d /opt/tomcat10/latest/lib ]; then
    CATALINA_BASE=/opt/tomcat10/latest
    CATALINA_HOME=/opt/tomcat10/latest
fi
export CATALINA_BASE
export CATALINA_HOME
ant clean-all
ant -Divy=true download-ivy 
ant -DCATALINA_HOME=${CATALINA_HOME} -DUbuntu=true war
sudo cp war/texton.war $CATALINA_BASE/webapps
