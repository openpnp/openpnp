# Scripting

OpenPnP has a built in scripting engine that allows you to add new functionality to the program without having to modify the source code.

Scripts get full access to the OpenPnP program and can read and modify any data in it. You can use scripts to do things like move the machine, reset feeders, print out board information, etc.

OpenPnP comes with several example scripts that are automatically installed. These can be used as starting points for your own scripts.

## Usage
Scripts placed in the `.openpnp/scripts` directory will be automatically added to the Scripts top level menu. Directories are respected and will be added as sub-menus so that you can organized your scripts as you see fit. OpenPnP will recognize scripts by their file extensions based on the scripting engines that are installed. By default scripts with the extension `.js` are loaded.

To run a script simply click it's name from the menu.

The Scripts menu also includes a Refresh Scripts item that will manually update the menu and a Open Scripts Directory item that will open the scripts directory using your computer's file manager.

## Language Support
OpenPnP supports scripts written in JavaScript out of the box and it's easy to add support for other languages. In most cases to add a language it's just a matter of finding a JAR file that implements that language and adding it to your Java classpath. See below for specifics on languages.

### JavaScript
JavaScript support is included with OpenPnP, there is nothing you need to add. The engine is called Nashorn. Script files with the extension `.js` will be run by the Nashorn / JavaScript engine.

For more information about the integration between Java and JavaScript in scripts, see the following links:

http://winterbe.com/posts/2014/04/05/java8-nashorn-tutorial/
https://docs.oracle.com/javase/8/docs/technotes/guides/scripting/nashorn/toc.html
http://www.oracle.com/technetwork/articles/java/jf14-nashorn-2126515.html

### Python
Python support is included with OpenPnP, there is nothing you need to add. The engine is called Jython. Script files with the extension `.py` will be run by the Jython engine.

More information about Jython can be found at http://www.jython.org/currentdocs.html.

## Global Variables
OpenPnP exposes several global variables to the scripting environment for use in your scripts. They are:

| Name  | Type | Description |
| ------------- | ------------- | -------------- |
| config  | [org.openpnp.model.Configuration](https://github.com/openpnp/openpnp/blob/develop/src/main/java/org/openpnp/model/Configuration.java) | The current OpenPnP Configuration object. Provides access to Parts, Packages, Resources, etc. |
| machine | [org.openpnp.machine.reference.ReferenceMachine](https://github.com/openpnp/openpnp/blob/develop/src/main/java/org/openpnp/machine/reference/ReferenceMachine.java) | The machine object declared in the configuration. Provides access to Nozzles, Cameras, Feeders, etc. This can be used to move the machine and perform machine operations. Note that the type of this object is dependent on how your machine is configured but it will typically be ReferenceMachine. |
| gui | [org.openpnp.gui.MainFrame](https://github.com/openpnp/openpnp/blob/develop/src/main/java/org/openpnp/gui/MainFrame.java) | The top level window in the OpenPnP GUI. From here you can access any part of the GUI |
| scripting | [org.openpnp.Scripting](https://github.com/openpnp/openpnp/blob/develop/src/main/java/org/openpnp/Scripting.java) | The OpenPnP scripting engine. Can be used to find information about scripts and the scripting environment. |
