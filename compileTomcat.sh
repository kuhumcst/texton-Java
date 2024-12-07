if [ -d /opt/tomcat11/latest/lib ]; then
    CATALINA_BASE=/opt/tomcat11/latest
    CATALINA_HOME=/opt/tomcat11/latest
elif [ -d /opt/tomcat10/latest/lib ]; then
    CATALINA_BASE=/opt/tomcat10/latest
    CATALINA_HOME=/opt/tomcat10/latest
elif [ -d /opt/tomcat/latest/lib ]; then
    CATALINA_BASE=/opt/tomcat/latest
    CATALINA_HOME=/opt/tomcat/latest
elif [ -d /opt/tomcat-texton/lib ]; then
    CATALINA_BASE=/opt/tomcat-texton
    CATALINA_HOME=/opt/tomcat-texton
fi
export CATALINA_BASE
export CATALINA_HOME
ant clean-all
ant -Divy=true download-ivy 
ant -DCATALINA_HOME=${CATALINA_HOME} -DUbuntu=true war
sudo cp war/texton.war $CATALINA_BASE/webapps
