Important information for anyone interested in hacking on or contributing to any of the OpenPnP modules.

# Getting Started

Before starting to develop for OpenPnP you should review the [User Manual](User Manual) to become familiar with the interface and major components of the system.

# Licensing and Copyright

OpenPnP uses different licenses for different components. Before submitting changes, please review the license for that module by looking at the LICENSE file in the root directory of the module and decide if it is acceptable to you. Changes under a different license will not be accepted for merge into the repository.

Contributors submitting large pieces of functionality, i.e. whole source files, whole part designs, major rewrites, etc. may assign their own copyright to the submission as long as you are willing to license it under the existing license for that module.

# Committing

Aside from the project maintainers, OpenPnP uses a Fork and Pull style of development. If you would like to submit changes you should fork the project, make your changes and submit a Pull Request. More information about that process is available at https://help.github.com/articles/using-pull-requests.

## Reviews

All requests will be reviewed by the maintainers and will either be merged or comments will be provided as to why not.

## Granularity

Try to commit as little as possible in a given commit. A single small feature, a single bug fix, etc. The smaller a commit is the easier it is to integrate into the project and the less likely it is to contain a looked over problem.

# Code Style

Please try to adhere to the existing code style as much as possible. Understand that your code is likely to be reformatted to fit the project standard if it doesn't follow it.

# GUI Module

The GUI is the core user facing component of OpenPnP. OpenPnP is, for all intents and purposes, the GUI. When the term "OpenPnP" is used without referencing the hardware or firmware it should be taken to mean the GUI and the rest of this document will refer to it in that manner.

The GUI is meant to be generally useful for a variety of PnP machines and users. When making changes, consider how those changes will affect other users and other machines.

## Building OpenPnP

OpenPnP is written in Java and is built with Maven. To begin hacking on OpenPnP you
should make sure you have the Java JDK 6 or greater installed, along with
Maven which you can get at http://maven.apache.org/download.cgi#Installation. You should
be able to run `mvn --version` from your command line to make sure everything is working
as expected.

OpenPnP is developed in Eclipse, but you can use any environment you like. If you do want
to use Eclipse you can generate an Eclipse compatible project file using the command
`mvn eclipse:eclipse` and then open the project using Eclipse's "Import Existing Project"
command.

To build the entire package so that it can be run from the command line or distributed,
run the command `mvn package`. Once this is complete you can use the `openpnp.sh` or
`openpnp.bat` scripts to start the program.

## Architecture

### Framework

OpenPnP is written in Java using Swing and WindowBuilder. Dependency and build management are performed using Maven. The majority of the code has been written using the Eclipse IDE but the system uses no Eclipse specific components and any IDE can be used.

Bindings and property change listeners are used extensively wherever possible. Bindings are provided by JBindings with some custom helper code that is part of OpenPnP.

### System Overview

OpenPnP is made up of 5 core components: Configuration, Service Provider Interface, Model, User Interface and Reference Implementation.

#### Configuration

Configuration of OpenPnP is managed with the [Configuration class](http://openpnp.org/doc/javadoc/org/openpnp/model/Configuration.html). The Configuration class is used to query and store to two configuration stores.

The primary one is a set of XML configuration files stored under the user's home directory in a subdirectory called `.openpnp`. These XML files are read and written using the Simple XML framework and are generally pretty transparent to developers. Reviewing the sample configuration files that come with OpenPnP is a good place to start when thinking about adding functionality.

The second configuration store is the user specific Java preferences data store. This is used to store user preferences for the application itself such as window size, units preference, locations of split windows, etc.

The Configuration class is a singleton accessed with Configuration.get() throughout the system. It has a listener system that can be used to be notified of configuration state changes.


#### Service Provider Interface

The [Service Provider Interface (SPI)](http://openpnp.org/doc/javadoc/org/openpnp/spi/package-frame.html) is a set of Java interfaces that specify OpenPnP's interface to the real world. Examples of things in the SPI are things like Machine definitions, Camera drivers, Vision Providers, Nozzles, Actuators, etc.

In general, unless you are adding a new type of hardware to OpenPnP you don't need to implement the SPI but in developing features for OpenPnP you will be interacting deeply with objects that themselves implement the SPI.

The Reference Implementation, discussed later, is the core implementation of the OpenPnP SPI.

#### Model

Like most MVC based systems, [The Model](http://openpnp.org/doc/javadoc/org/openpnp/model/package-frame.html) makes up the domain for data storage in OpenPnP.

The model includes all of the classes that users use to store their data such as Jobs, Boards, Parts, Packages, etc. but the SPI also provides models for storing configuration information.

The root of the model in OpenPnP is the Configuration object and it is responsible for creating all of the rest of the models in the application.

#### User Interface

The user interface is the heart of OpenPnP. It starts with `MainFrame` in [the gui package](http://openpnp.org/doc/javadoc/org/openpnp/gui/package-frame.html) and branches out from there.

The user interface is primarily a single window application with 3 main content areas. These are Machine Controls, Cameras, and Jobs and Configuration.

It should be noted that the user interface is in transition. I am not very happy with the currently model driven user interface and will be working to make it more of a workflow based interface in the future. For the time being, most of the things you see in the user interface map very closely to one of the model classes.

#### Reference Implementation

The reference implementation is the default implementation of the SPI. It has two main purposes. The most important one is to be the implementation that runs the actual OpenPnP machine and the second is to serve as an example for people wanting to write their own custom implementations.

Whenever possible, the reference implementation is written to be compatible with a wide class of machines and hardware.

The core of the reference implementation is the [ReferenceMachine class](http://openpnp.org/doc/javadoc/org/openpnp/machine/reference/ReferenceMachine.html). Serialization of this class and all it's children becomes the `machine.xml` configuration file. If you want to add hardware to the system you will want to become intimately familiar with this class and the classes it references.

### Component Specifics

#### Vision

Vision tasks in OpenPnP are specified by the [VisionProvider SPI class](http://openpnp.org/doc/javadoc/org/openpnp/spi/VisionProvider.html). This area is still in it's infancy and suggestions on it's architecture are welcome.

Currently, the concept is that VisionProviders provide data about images they receive from a camera. A VisionProvider has a reference to a Camera and can use it to capture images as needed. When other parts of the system have a need for a vision task, they query the involved Camera for it's VisionProvider and then ask the VisionProvider to provide vision services.

The VisionProvider interface specifies a few methods that are used by existing vision systems to get data but these are fairly rigidly defined and change may be needed to make this system more generic.

For the time being, if you are adding vision capabilities to OpenPnP you will need to add methods to the VisionProvider SPI and then implement those methods in the reference classes.


#### Wizards

The concept of a "wizard" is used quite often in OpenPnP. A wizard in OpenPnP is a simple interface that provides a JPanel for display to the user and allows a class to communicate back to a caller. Wizards are typically used for providing user interfaces to configuration tasks and many SPI and Model classes will implement WizardConfigurable, showing that they are able to provide a wizard to be configured.

The Wizard and WizardConfigurable interfaces are provided so that SPI providers can supply user interface without having to modify the core OpenPnP system.


# FAQ