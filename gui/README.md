# Prerequisites

## JDK or JRE 6

OpenPnP is written in Java and requires Java to run. Currently only version 6
of the Java runtime is supported. If you want to do development on OpenPnP or
recompile it, you should install the JDK. If you just want to run it you can
install the smaller JRE.

You can download the latest revision of the JDK or JRE 6 at:

http://www.oracle.com/technetwork/java/javasebusiness/downloads/java-archive-downloads-javase6-419409.html


## OpenCV

OpenCV 2.4.2+ is required for some features of OpenPnP. Binaries should be
installed and accessible to Java. This usually means making sure the libraries
are in your path.

http://opencv.willowgarage.com/wiki/


# Running

## Mac

Open a Terminal window and run the openpnp.sh script from the directory this
file is in.

## Windows

Open a Command Prompt and run the openpnp.bat batch file from the directory
this file is in.

## Linux

Open a Terminal window and run the openpnp.sh script from the directory this
file is in.


# Configuration

Configuration files are created during the first run and are stored in
$HOME/.openpnp/. 

If you run into configuration problems, delete the files in that directory
and they will be recreated.


# Development

OpenPnP builds with Maven. There is an included pom.xml that should build
the project and will build a proper Eclipse project using the command
mvn eclipse:eclipse

To build the jar package from the command line, use the command
mvn package
