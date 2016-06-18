Introduction
============

OpenPnP is an Open Source SMT pick and place system designed and built with the hobbyist in mind but with the features and power to run commercial pick and place operations. It's goal is to bring pick and place to the desktop of anyone who needs to make more than a few of something.

OpenPnP is made up of three components. The hardware, the firmware and the software. This User Manual focuses only on the software. To get information about the hardware and firmware please visit http://openpnp.org.

The purpose of this manual is to help you get the software up and running, and to teach you how to configure and operate it.

Quick Start
===========

If you are brand new to OpenPnP have a look at the [[Quick Start]] guide to quickly get up and running and comfortable with the software. Once you are done there it will guide you back here to get into the details.

Installation
============

Prerequisites
-------------

OpenPnP runs on the Java platform and requires the Java runtime version 7 or higher to run. You can download the latest version of Java from http://java.com/getjava.

OpenPnP is designed to run on Mac, Windows and Linux. Other platforms may be supported due to the nature of Java, but they are not recommended. OpenPnp is written and tested on Mac and used regularly on Windows. These two platforms are recommended for the best compatibility.

Download
--------

Visit http://openpnp.org/downloads to find out how to download the latest snapshot or release of OpenPnP.

Install and Run
---------------

If you are using one of the binary installers from the website, just run the installer and follow the instructions. After installation you can run OpenPnP from your operating system's applications list, i.e. Start Menu, Applications folder, etc.

If you are using an archive version of OpenPnP, unzip the software into a directory of your choosing. Typically this would be the same place you keep your other applications. Inside the folder you unzipped OpenPnP to there is an `openpnp.sh` and `openpnp.bat` script. These should work for Windows, Mac and Linux. For Mac and Linux, run `openpnp.sh` and for Windows run `openpnp.bat`. After a short wait you should see the OpenPnP Main Window. If something goes wrong, visit the Troubleshooting section of this document for help.

Coordinate System
=================

OpenPnP uses the right handed coordinate system which is also used in physics, math, 3D graphics and many CAD packages.

In this coordinate system we are standing above the machine, looking down at it.

The X axis moves right and left. Right is positive.
The Y axis moves forward and back. Forward is positive.
The Z axis moves up and down. Up is positive.
The C, or rotation, axis rotates clockwise and counter-clockwise. Counter-clockwise is positive.

![screen shot 2016-06-18 at 12 56 07 pm](https://cloud.githubusercontent.com/assets/1182323/16173361/0e54935c-3554-11e6-9cf6-caf13e6d4a65.png)

More information can be found at:
http://www.evl.uic.edu/ralph/508S98/coordinates.html
http://en.wikipedia.org/wiki/Right-hand_rule

The units for the X, Y and Z axises are set in the GUI, the default is mm.  The units for the C axises is degrees, OpenPnP measures rotation in degrees and treats them like mm from the perspective of the controller.

The User Interface
==================

OpenPnP is primarily a single window interface, broken up into multiple sections. Those sections will be explained in detail below.

The Main Window
---------------
![screen shot 2016-06-18 at 10 34 20 am](https://cloud.githubusercontent.com/assets/1182323/16172608/8dad6e20-3541-11e6-84ab-5a87c3bfdc91.png)

Machine Controls
----------------
![screen shot 2016-06-18 at 12 59 52 pm](https://cloud.githubusercontent.com/assets/1182323/16173384/98b58fc4-3554-11e6-9382-819aa1d82c11.png)

The Machine Controls are your interface to interacting with the machine. You can see the current position in the DROs, move the machine using the Jog Controls and perform special operations with the Special Commands section.

Digital Read Outs (DROs)
------------------------
![screen shot 2016-06-18 at 1 01 43 pm](https://cloud.githubusercontent.com/assets/1182323/16173392/d36f81ce-3554-11e6-9eab-b6e6794f913b.png)

The DROs show the current position of the selected nozzle in your preferred units. You can click the DROs to set them to relative mode which will zero them out and turn them blue. You can use this to measure distances; the DROs will show the distance from where you first clicked them. Click again to go back to normal mode.

Jog Controls
------------
![screen shot 2016-06-18 at 10 33 18 am](https://cloud.githubusercontent.com/assets/1182323/16172512/1cf472b0-3540-11e6-987a-fff822524944.png)

The Jog Controls are where you can move the selected nozzle. The Distance slider sets the distance each click of the buttons will move the machine.

The Camera Panel
----------------
![Camera Window](https://globedrop.com/wiki/_media/openpnp:camerawindow.png)



The Tabs
--------
![The Tabs](https://globedrop.com/wiki/_media/openpnp:tabswindow.png)

Shortcuts
---------
There are a few important keyboard shortcuts that are critical to know to use OpenPnP. They are:

* Ctrl+Arrow Key: Jog the currently selected Nozzle in X and Y. Up and Down arrows jog in Y and Left and Right arrows jog in X.
* Ctrl+/, Ctrl+': Jog the currently selected Nozzle down and up in Z.
* Ctrl+<, Ctrl+>: Rotate the currently selected Nozzle counter-clockwise and clockwise.
* Ctrl+Plus, Ctrl+Minus: Change the jog distance slider. This changes how far each jog key will move the Nozzle.
* Shift+Left Mouse Click: Hold Shift and left click the mouse anywhere in the camera view to move the camera to that position.

Configuration and Operation
===========================

Packages
--------
The only thing you need to configure in packages is the pads for fiducials. Normal parts / packages don't need any of the footprint data set. They may in the future, but it will likely always be optional.

eg. the Body Height field in the Footprint tab is not used at all currently. You can leave it blank.

Feeders
-------

Feeder locations are simple locations.  In general no math is applied. The location you set is where the nozzle will go to pick.  The part height is not used here.

Parts
-----

Part height is simply the height of the part as measured, typically with calipers or from a data sheet. Part height should only need to be measured and set once per part. A Part should represent an actual physical SKU. If you have a 47uF cap that is 6mm high and one that is 4mm high, those are different parts.

Parts refer to a feeder from which they are picked and a package.  The package is currently only used for fiducials.


Boards
------

Boards tell OpenPnP which parts to place and where to place them. Boards are stored in files with the extension `.board.xml`. A Board contains a list of Placements. A Placement tells OpenPnP which part to place at what coordinates and rotation.

Board files are independent from any user or machine. You can share Board files for a given PCB design and use the file to build that particular PCB.

You will typically create a new Board file by importing data from your CAD software such as Eagle or KiCAD. Once you've created a Board file for a design there's no need to change it unless the design changes.

PCB locations represent the 0,0,0 origin of the top of the PCB. This tells the machine where to find 0,0,0 on the board and it performs the math needed to find the individual placements from there. Part height is added when placing a part so that the needle stops at "part height" above the board.

Jobs
----

Job files tell OpenPnP where to find one or more Boards on the machine. A Job might consist of a single Board or of many of the same or even different Boards. Each line in a Job tells OpenPnP where to find one particular Board using machine coordinates. When you run the Job OpenPnP will process all of the Placements for each Board in the Job.

Since a Job can contain many of the same Board you can use it to process a full panel of PCBs. Just add the same Board to the Job for as many as the panel contains and set the position of each in the Job.

Your First Job
--------------

This is a brief explanation of how to setup and run your first job. For details on any step you can refer to other parts of this page, but be warned that it hasn't all been documented yet. This is meant to serve as a jumping off point.

1. Define packages used in the PCB in the Packages tab.
2. Define parts used in the PCB in the Parts tab. Reference the previously created Packages.
3. Setup feeders in the Feeders tab for each unique Part being used in the job.
4. Create a new board by pressing ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/file-new.svg) in the Job tab and add Placements to it for each Placement in the PCB.
5. Set the position of the board in the Job tab. You can use capture camera ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/capture-camera.svg) to align it to the corner, use fiducial locate ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/board-fiducial-locate.svg) to find it automatically or use the two placement manual process ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/board-two-placement-locate.svg).

    For additional information on using fiducials, see [[Fiducials]].
6. You'll need to set the Z position of the board, too. Touch the nozzle tip to the board and use capture nozzle ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/capture-nozzle.svg) to set it.
7. Run the job by clicking Start ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/control-start.svg).

Advanced Topics
===============

Configuration Files
-------------------

Configuration files are located in your home directory, under a subdirectory called `.openpnp`.

* On Mac this will typically be `/Users/[username]/.openpnp`.
* On Windows 2000, XP and 2003 it will be `C:\Documents and Settings\[username]\.openpnp`.
* On Windows Vista and above it's `C:\Users\[username]\.openpnp`.

Configuration files are in XML format and can be edited by hand in a text editor. You should shutdown OpenPnP before editing files by hand as OpenPnP will rewrite the configuration files on exit.

There are three primary configuration files. They are:

1. `machine.xml`: Contains the primary configuration for the entire system, including information about the machine, cameras, feeders, nozzles, etc.
2. `parts.xml`: A portable parts database. As you define parts (components) in OpenPnP they are stored here.
3. `packages.xml`: A portable packages database. Component package information including shape and dimensions are stored here.

Custom Implementations and Integration
--------------------------------------

If you are interested in having OpenPnP work with a machine that is not currently supported
you will need an OpenPnP driver that can talk to your hardware and you will need to
configure it in the `machine.xml`.

To get started, look at the list of drivers in the package below to see what drivers are
available and determine if one will meet your needs.

https://github.com/openpnp/openpnp/tree/develop/src/main/java/org/openpnp/machine/reference/driver

If none of those will work for your machine, you will need to write one. Once
you have a driver, you can specify it's classname and configuration parameters
in `machine.xml`.

See the Development section for more information if you decide you need to write code.

Development
-----------

For more information about developing OpenPnP, especially regarding contributing, please see
https://github.com/openpnp/openpnp/wiki/Developers-Guide.

Getting Help
============

Discussion Group
----------------

There is an active discussion group at http://groups.google.com/group/openpnp. This will typically be the best place to get help.

IRC
---

We also have an IRC channel on Freenode IRC at #openpnp. If you don't have an IRC client, you can use [this web based one](http://webchat.freenode.net/?channels=openpnp).