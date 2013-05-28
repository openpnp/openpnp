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

Open a Terminal window and run the `openpnp.sh` script from the directory this
file is in.

## Windows

Open a Command Prompt and run the `openpnp.bat` batch file from the directory
this file is in.

## Linux

Open a Terminal window and run the `openpnp.sh` script from the directory this
file is in.

# Configuration

Configuration files are created during the first run and are stored in
`$HOME/.openpnp/`. The most important file is `machine.xml`, which contains
all the settings and information for the program and the machine that it
should run.

If you run into configuration problems, delete the files in that directory
and they will be recreated.

# Integration

If you are interested in having OpenPnP work with a machine that is not currently supported
you will need an OpenPnP driver that can talk to your hardware and you will need to
configure it in the `machine.xml`.

To get started, look at the documentation in the package below to see what drivers are
available and determine if one will meet your needs.

http://openpnp.org/doc/javadoc/org/openpnp/machine/reference/driver/package-summary.html

If none of those will work for your machine, you will need to write one. Once
you have a driver, you can specify it's classname and configuration parameters
in `machine.xml`.

See the Development section for more information if you decide you need to write code.

# Development

To build the entire package so that it can be run from the command line or distributed,
run the command `mvn package`. Once this is complete you can use the `openpnp.sh` or
`openpnp.bat` scripts to start the program.

For more information about developing OpenPnP, especially regarding contributing, please see
https://github.com/openpnp/openpnp/wiki/Developers-Guide.
