# Text Tonsorium - Java component

This document explains how you can install the Java part of the Text Tonsorium under Linux Ubuntu.

The instructions are valid under the following assumptions:

  * The software is installed under Linux or in the Windows Subsystem for Linux
  * The OS is Ubuntu 18.04 or higher

Installation requires 
  * Java
  * ant
  * Tomcat  
  * bracmat  
   Interpreters are installed in two locations:  
   as a JNI (Java Native Interface) inside Tomcat  
   and as a command line tool in `/opt/texton/bin/`
  * texton - Java  - Bracmat part (this repo)
   This is the central hub in the Text Tonsorium. It communicates with the user via a
   browser and communicates with the tools using HTTP `GET` or `POST` requests.

## java

Text Tonsorium and Tomcat requires version 11 or later.

```bash
$> sudo apt install default-jdk
```

## ant

Ant is needed if you want to build the texton.war file from source.

```bash
$> sudo apt install ant
```

## Tomcat

Text Tonsorium requires Tomcat version 10 or later.

### Install Tomcat

Tomcat can be installed using `apt install`, or from a downloaded archive.

Visit https://tomcat.apache.org/ to obtain a link to a recent archive.

```bash
$> sudo mkdir /opt/tomcat10
$> sudo ln -s /opt/tomcat10 /opt/tomcat
$> sudo ln -s /opt/apache-tomcat-10.1.7 /opt/tomcat10/latest
$> sudo useradd -r -m -U -d /opt/tomcat -s /bin/false tomcat
$> wget https://downloads.apache.org/tomcat/tomcat-10/v10.1.7/bin/apache-tomcat-10.1.7.tar.gz
$> sudo tar xf apache-tomcat-10*.tar.gz -C /opt/
$> sudo chown -RH tomcat: /opt/tomcat10/latest
```
Enter

    [Unit]
    Description=Apache Tomcat Web Application Container
    After=network.target
    
    [Service]
    Type=forking
    
    User=tomcat
    Group=tomcat
    UMask=0007
    
    Environment="JAVA_HOME=/usr/lib/jvm/default-java"
    Environment="CATALINA_PID=/opt/tomcat10/latest/temp/tomcat.pid"
    Environment="CATALINA_HOME=/opt/tomcat10/latest"
    Environment="CATALINA_BASE=/opt/tomcat10/latest"
    Environment='CATALINA_OPTS=-Xms7168M -Xmx7168M -server -XX:+UseG1GC'
    
    Environment='JAVA_OPTS=-Djava.security.egd=file:/dev/./urandom'
    Environment="CLASSPATH=$CLASSPATH:$CATALINA_HOME/lib/bracmat.jar"
    
    ExecStart=/opt/tomcat10/latest/bin/startup.sh
    ExecStop=/opt/tomcat10/latest/bin/shutdown.sh
    SuccessExitStatus=0 143
    
    Restart=always
    RestartSec=5
    ReadWritePaths=/opt/texton/BASE/ /var/log/texton/
    
    [Install]
    WantedBy=multi-user.target

```bash
$> sudo vi /opt/tomcat10/latest/conf/server.xml
```

change

    <Connector port="8080" protocol="HTTP/1.1"

to

    <Connector address="127.0.0.1" port="8080" protocol="HTTP/1.1"

### Install Bracmat JNI

Create the Tomcat lib bracmat.jar and the shared library libbracmat.so.1.0


```bash
$> git clone https://github.com/BartJongejan/Bracmat.git
$> cd Bracmat/java-JNI
$> sudo chmod ugo+x compileAndTestJNI.sh
$> sudo ./compileAndTestJNI.sh
$> sudo chown -RH tomcat: /opt/tomcat10/latest
```

### Install texton-Java (this repo) 

The repo https://github.com/kuhumcst/texton-Java contains the Java code of the central hub.
Make sure that texton-Java/ and Bracmat/ (see below) share the same parent folder. You can clone whereever you want, e.g. in your home folder.
It is important that the script can `see' ../Bracmat/java-JNI/java. See the build.xml file.

```bash
$> sudo chown -RH tomcat: /opt/tomcat10/latest
$> git clone https://github.com/kuhumcst/texton-Java.git
$> cd texton-Java
$> sudo chmod ugo+x compileTomcat.sh
$> sudo ./compileTomcat.sh
$> cd ..
```

### Setting environment

Add "bracmat.jar" to the classpath.

```bash
$> vi /opt/tomcat/latest/bin/setenv.sh
```

    CLASSPATH=$CLASSPATH:$CATALINA_HOME/lib/bracmat.jar

If there are several java versions, enter the path to the version of java that tomcat must use in setenv.sh, e.g.

    export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64

If your computer has more than 8 GB RAM, you can add

    JAVA_OPTS="-Djava.awt.headless=true -XX:+UseG1GC -Xms7168m -Xmx7168m"

Make the file executable

```bash
$> sudo chmod ugo+x /opt/tomcat/latest/bin/setenv.sh
```

### Stopping/starting if systemd

Restart Tomcat

```bash
$> # Skip next line if installing in WSL, because no systemd.
$> sudo systemctl restart tomcat-texton.service
```

### Stopping/starting if not systemd

Start Tomcat

```bash
$> sudo /opt/tomcat/latest/bin/startup.sh
```

Stop Tomcat

```bash
$> sudo /opt/tomcat/latest/bin/shutdown.sh
```

