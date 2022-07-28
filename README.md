texton-Java
===========

Part of the Text Tonsorium, https://github.com/kuhumcst/texton

The Text Tonsorium is a web application that computes candidate tool workflows given input file(s) and the user's requirements regarding the output. Afterwards, the application runs the workflow that the user has selected from the list of candidates.

The Text Tonsorium composes workflows from building blocks. Each building block encapsulates a Natural Language Processing tool and is implemented as a web service wrapper around a command line tool.

The web application that the user directly interacts with, is called texton.

The original version of the Text Tonsorium was made during the [DK-Clarin project](https://dkclarin.ku.dk/). The application is written in the Bracmat programming language, except for the communication with the user's browser and with the tools (web services in their own right), which is implemented in Java. The latter, the Java part, is in this repositorium. By building running the compileTomcat.sh script a WAR-file is created, war/texton.war. This WAR-file must be deployed to the webapps catalogue of a modern Tomcat installation.

To successfully run compileTomcat.sh, it is necessary to also have Bracmat installed (https://github.com/BartJongejan/Bracmat) in a sister catalogue to texton-Java, since it needs bracmat.jar in its class path.

Bracmat.jar is created by running the script compileAndTestJNI.sh in the folder ../Bracmat/java-JNI/, situated in the aforementioned Bracmat repositorium. That script also generates and installs a JNI (Java Native Interface) for the Bracmat language, which is needed since the Bracmat interpreter is written in C, not in Java.  

The Bracmat script that implements almost all of the workflow management logic of the Text Tonsorium is in the https://github.com/kuhumcst/texton repositorium, in the folder called texton/BASE.

If you want to run Text Tonsorium on anything else but a personal computer, you must set an administrator password in the properties_ubuntu.xml, in the entry element with the attribute `key="password"' and a 'salt' string in the entry element with the atribute `key="salt"'.
Such a pair can be created in the following way:

1. On your development machine, go to http://localhost/texton/admin
2. Enter the password that you want to use on your production system in the password field below the `Evaluate program code' text area field.
3. Press the `Bracmat' button.
4. Open a linux terminal, so you can change directory to /opt/texton/BASE
5. Open the log file for the java part of Text Tonsorium
  $> sudo less textonJava.log
6. Go to the end of this file and find the log statement that contains the string `XMLprop`. Copy everything between `[` and `]` to the file properties_ubuntu.xml, replacing the two same named elements.
7. Save properties_ubuntu.xml
8. Recompile. The resulting .war file can be deployed on the server.

Notice that you also need to replace the fields &lt;entry key="www-server"&gt;http://localhost:8080&lt;/entry&gt; and <entry key="baseUrlTools">http://localhost</entry> into something that is meaningful for your server. E.g. if Text Tonsorium runs as https://me.nu/texton/, then you should change these fields to <entry key="www-server">https://me.nu</entry> and <entry key="baseUrlTools">https://me.nu</entry>.
