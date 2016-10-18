The Tools webservice is implemented in Java and in Bracmat.

Bracmat is a programming language. It is also the name of its interpreter, 
which is a command line program written in standard-C.

Most of the webservice's logic is written in Bracmat. Java code takes care
of interfacing between Bracmat and the rest of the world.

Bracmat, in this application, runs as a native library and is callable from
Java code. No attempt has been made to make Java callable from Bracmat.
A peculiarity of a Java native library (JNI) is that it runs in a single
thread. Once loaded and initialised by a Java thread, it stays in memory
until the Java application is terminated. In DK-Clarin, the Java application
is JBoss.

As a consequence of Bracmat running in a single thread, loading and saving
data from and to disk is very straightforward, and no high-level DBMS is
needed to safeguard the Tools-webservice's persistent data, such as
information about available tools. Also less permanent data, such as job
status information, can easily be stored to disk and consulted after a JBoss
restart.

The JNI only needs to be created once from the latest Bracmat source code.
Bracmat changes little - it is more than 20 years old and pretty mature.
There is no Makefile for creating the JNI. Instead, the file 
Bracmat/src/compileAndTestJNI.txt explains step by step how to compile the
C-source code and the accompying Java source code and how to create a shared
library. It also describes the tests that can be made as the progress is made.

The Bracmat code that implements the Tools-webservice's logic is in the file
"toolsProg.bra". The contents of toolsProg.bra must be read when (1) the native
library is loaded an initialised and (2) when changes have been made to the
source code.

Ad (1). The "current working directory" of the native library is the "bin"
directory of JBoss or Tomcat. The initialisation of the native library is
bootstrapped relative to this working directory, declaring the path to all of
the webservice's source code ("toolsProg.bra") and data. The bootstrapping code
also reads and interprets the source code of the webservice.

Ad (2). A single function can be changed on the fly by copying its source code
to Bracmat's text-entry window and submitting it to the webservice, provided
that such a text area is made accessible. (Which is not a very clever thing
to do in a production environment.)

LOCATIONS
---------

On the server, Tools-stuff is spread:

/srv/dkclarin-infra/jboss-4.2.3.GA/server/default/deploy/tools.war

                        The Java component that exposes the webservice's
                        methods.

/usr/lib/libbracmat.so.1.0
/usr/lib/libbracmat.so.1 --> /usr/lib/libbracmat.so.1.0
/usr/lib/libbracmat.so --> /usr/lib/libbracmat.so.1

                        The shared Bracmat library. (Must be somewhere in
                        JBoss's PATH.)

/srv/dkclarin-infra/jboss-4.2.3.GA/server/all/lib/bracmat.jar
/srv/dkclarin-infra/jboss-4.2.3.GA/server/default/lib/bracmat.jar

                        The Java component that encapsulates the JNI
                        (Probably only the second copy is needed.)

/srv/dkclarin-infra/jboss-4.2.3.GA/server/default/data/tools

                        Folder with the logic-related source code and data
                        files:

                            toolsProg.bra (source code)
                            toolprop.table  (data)
                            tooladm.table   (data)
                            ...                "
                            
                            