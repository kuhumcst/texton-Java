# Text Tonsorium - Java component

This document explains how you can install the Java part of the Text Tonsorium under Linux Ubuntu.

The instructions are valid under the following assumptions:

  * The software is installed under Linux or in the Windows Subsystem for Linux
  * The OS is Ubuntu 24.04 or higher

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
$> sudo useradd -r -m -U -d /opt/tomcat11 -s /bin/false tomcat
$> cd ~
$> wget https://dlcdn.apache.org/tomcat/tomcat-11/v11.0.1/bin/apache-tomcat-11.0.1.tar.gz -P .
$> sudo tar -xvzf apache-tomcat-11.0.1.tar.gz -C /opt/tomcat11
$> sudo ln -s /opt/tomcat11/apache-tomcat-11.0.1 /opt/tomcat-texton
$> sudo chown -RH tomcat: /opt/tomcat-texton
$> sudo chmod o+x /opt/tomcat-texton/bin/
```
systemd:

```bash
$> sudo vi /etc/systemd/system/tomcat-texton.service
```
Enter
```
[Unit]
Description=Apache Tomcat Web Application Container
After=network.target
    
[Service]
Type=forking
    
User=tomcat
Group=tomcat
UMask=0007
    
Environment="JAVA_HOME=/usr/lib/jvm/default-java"
Environment="CATALINA_PID=/opt/tomcat-texton/temp/tomcat.pid"
Environment="CATALINA_HOME=/opt/tomcat-texton"
Environment="CATALINA_BASE=/opt/tomcat-texton"
Environment='CATALINA_OPTS=-Xms7168M -Xmx7168M -server -XX:+UseG1GC'
    
Environment='JAVA_OPTS=-Djava.security.egd=file:/dev/./urandom'
Environment="CLASSPATH=$CLASSPATH:$CATALINA_HOME/lib/bracmat.jar"
    
ExecStart=/opt/tomcat-texton/bin/startup.sh
ExecStop=/opt/tomcat-texton/bin/shutdown.sh
SuccessExitStatus=0 143
    
Restart=always
RestartSec=5
ReadWritePaths=/opt/texton/BASE/ /var/log/texton/

[Install]
WantedBy=multi-user.target
```
```bash
$> sudo vi /opt/tomcat-texton/conf/server.xml
```

change
```
<Connector port="8080" protocol="HTTP/1.1"
```
to
```
<Connector address="127.0.0.1" port="8080" protocol="HTTP/1.1"
```
### Install Bracmat JNI

Create the Tomcat lib bracmat.jar and the shared library libbracmat.so.1.0.
The script compileAndTestJNI.sh assumes that the folder /opt/tomcat-texton/ exists and that the tomcat binaries are in the bin subfolder. Edit compileAndTestJNI.sh if necessary.

```bash
$> git clone https://github.com/BartJongejan/Bracmat.git
$> cd Bracmat/java-JNI
$> sudo chmod ugo+x compileAndTestJNI.sh
$> sudo ./compileAndTestJNI.sh
```

### Install texton-Java (this repo) 

The repo https://github.com/kuhumcst/texton-Java contains the Java code of the central hub.
Make sure that texton-Java/ and Bracmat/ (see below) share the same parent folder. You can clone whereever you want, e.g. in your home folder.
It is important that the script can `see' ../Bracmat/java-JNI/java. See the build.xml file.

```bash
$> sudo chown -RH tomcat: /opt/tomcat-texton
$> git clone https://github.com/kuhumcst/texton-Java.git
$> cd texton-Java
$> sudo chmod ugo+x compileTomcat.sh
$> sudo ./compileTomcat.sh
$> cd ..
```

### Setting environment

Add "bracmat.jar" to the classpath.

```bash
$> vi /opt/tomcat-texton/bin/setenv.sh
```
```
CLASSPATH=$CLASSPATH:$CATALINA_HOME/lib/bracmat.jar
```
If there are several java versions, enter the path to the version of java that tomcat must use in setenv.sh, e.g.
```
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
```
If your computer has more than 8 GB RAM, you can add

```
JAVA_OPTS="-Djava.awt.headless=true -XX:+UseG1GC -Xms7168m -Xmx7168m"
```
Make the file executable

```bash
$> sudo chmod ugo+x /opt/tomcat-texton/bin/setenv.sh
```

### Stopping/starting if systemd

Restart Tomcat

```bash
$> sudo systemctl restart tomcat-texton.service
```

### Stopping/starting if not systemd

Start Tomcat

```bash
$> sudo /opt/tomcat-texton/bin/startup.sh
```

Stop Tomcat

```bash
$> sudo /opt/tomcat-texton/bin/shutdown.sh
```

