DK-ClarinTools
==============

Part of the Text Tonsorium, https://github.com/kuhumcst/texton

The Text Tonsorium is a web application that computes candidate tool workflows given input file(s) and the user's requirements regarding the output. Afterwards, the application runs the workflow that the user has selected from the list of candidates.

The Text Tonsorium composes workflows from building blocks. Each building block encapsulates a Natural Language Processing tool and is implemented as a web service wrapper around a command line tool.

The web application that the user directly interacts with, is called DK-ClarinTools.

The original version of DK-ClarinTools was made during the [DK-Clarin project](https://dkclarin.ku.dk/). The application is written in the Bracmat programming language, except for the communication with the user's browser and with the tools (web services in their own right), which is implemented in Java. The latter, the Java part, is in this repositorium. By building running the compileTomcat.sh script a WAR-file is created, war/texton.war. This WAR-file must be deployed to the webapps catalogue of a modern Tomcat installation.

To successfully run compileTomcat.sh, it is necessary to also have Bracmat installed (https://github.com/BartJongejan/Bracmat) in a sister catalogue to DK-ClarinTools, since it needs bracmat.jar in its class path.

Bracmat.jar is created by running the script compileAndTestJNI.sh in the folder ../Bracmat/java-JNI/, situated in the aforementioned Bracmat repositorium. That script also generates and installs a JNI (Java Native Interface) for the Bracmat language, which is needed since the Bracmat interpreter is written in C, not in Java.  

The Bracmat script that implements almost all of the workflow management logic of the Text Tonsorium is in the https://github.com/kuhumcst/texton repositorium, in the folder called texton/DK-ClarinTools/work.

