# Scripting
OpenPnP has a built in scripting engine that allows you to add new functionality to the program without having to modify the source code. Scripts that you place in your `.openpnp/scripts` directory will appear in the Scripts menu and then it's just a click to run the script.

Scripts get full access to the OpenPnP program and can read and modify any data in it. You can use scripts to do things like move the machine, reset feeders, print out board information, etc.

OpenPnP comes with several example scripts that are automatically installed. These can be used as starting points for your own scripts.

# Language Support
OpenPnP supports scripts written in JavaScript out of the box and it's easy to add support for other languages. In most cases to add a language it's just a matter of finding a JAR file that implements that language and adding it to your Java classpath. See below for specifics on languages.

## JavaScript
JavaScript is supported out of the box, there is nothing you need to add. The engine is called Nashorn. To see more information about the integration between Java and JavaScript in scripts, see the following links:

http://winterbe.com/posts/2014/04/05/java8-nashorn-tutorial/
https://docs.oracle.com/javase/8/docs/technotes/guides/scripting/nashorn/toc.html
http://www.oracle.com/technetwork/articles/java/jf14-nashorn-2126515.html

## Python
You can add Python support by adding the [Jython](http://www.jython.org/downloads.html) package to your classpath. Make sure to download the Standalone Jar and add it to your classpath. Once added you can add `.py` files to your scripts directory and run them from inside OpenPnP. 

More information about Jython can be found at http://www.jython.org/currentdocs.html.

# Global Variables
OpenPnP exposes several global variables to the scripting environment for use in your scripts. They are:

| Name  | Type | Description |
| ------------- | ------------- |
| config  | org.openpnp.model.Configuration | The current OpenPnP Configuration object. Provides access to Parts, Packages, Resources, etc. |
| machine | org.openpnp.machine.reference.ReferenceMachine | The machine object declared in the configuration. Provides access to Nozzles, Cameras, Feeders, etc. This can be used to move the machine and perform machine operations. |
| gui | org.openpnp.gui.MainFrame | The top level window in the OpenPnP GUI. From here you can access any part of the GUI |
| scripting | org.openpnp.Scripting | The OpenPnP scripting engine. Can be used to find information about scripts and the scripting environment. |