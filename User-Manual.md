Introduction
============

OpenPnP is an Open Source SMT pick and place system designed and built with the hobbyist in mind but with the features and power to run commercial pick and place operations. It's goal is to bring pick and place to the desktop of anyone who needs to make more than a few of something.

OpenPnP is made up of three components. The hardware, the firmware and the software. This User Manual focuses only on the software. To get information about the hardware and firmware please visit http://openpnp.org.

The purpose of this manual is to help you get the software up and running, and to teach you how to configure and operate it.

Pre-Release Software Warning
============================

OpenPnP is currently considered Alpha level software. This means that it is still under heavy development, may have major bugs and may be entirely unreliable. If you are still interested in continuing...

Installation
============

Prerequisites
-------------

OpenPnP runs on the Java platform and requires the Java runtime version 7 or higher to run. You can download the latest version of Java from http://java.com/getjava.

OpenPnP is designed to run on Mac, Windows and Linux. Other platforms may be supported due to the nature of Java, but they are not recommended. OpenPnp is written and tested on Mac and used regularly on Windows. These two platforms are recommended for the best compatibility.

Download
--------

Visit http://openpnp.org to find out how to download the latest snapshot or release of OpenPnP.

Install
-------

Unzip the software into a directory of your choosing. Typically this would be the same place you keep your other applications.

Running OpenPnP
---------------

Inside the folder you unzipped OpenPnP to there is an `openpnp.sh` and `openpnp.bat` script. These should work for Windows, Mac and Linux. For Mac and Linux, run `openpnp.sh` and for Windows run `openpnp.bat`. After a short wait you should see the OpenPnP Main Window. If something goes wrong, visit the Troubleshooting section of this document for help.

The User Interface
==================

OpenPnP is primarily a single window interface, broken up into multiple sections. Those sections will be explained in detail below.

The Main Window
---------------

Machine Controls
----------------

The Camera Panel
----------------

The Tabs
--------

Shortcuts
---------

There are a few important keyboard shortcuts that are critical to know to use OpenPnP. All of the shortcuts use Ctrl + another key. They are:

* Ctrl+Tab: Open the jog controls window which gives you visible buttons for most of the other shortcuts.
* Ctrl+Arrow Key: Jog the currently selected Nozzle in X and Y. Up and Down arrows jog in Y and Left and Right arrows jog in X.
* Ctrl+/, Ctrl+': Jog the currently selected Nozzle down and up in Z.
* Ctrl+>, Ctrl+<: Rotate the currently selected Nozzle clockwise and counterclockwise.
* Ctrl+Plus, Ctrl+Minus: Change the jog increment slider. This changes how far each jog key will move the Nozzle.
* Shift+Left Mouse Click: Hold Shift and left click the mouse anywhere in the camera view to move the camera to that position.

Setup
=====

The Driver
----------

Heads
-----

### Head Offsets

Head offsets are a complex and important concept in OpenPnP. Head offsets allow you to tell
OpenPnP about the layout of your machine's head and the distances between the various movable
objects on it.

A typical head will have, at least a nozzle and a camera. Some may have multiple nozzles and may add actuators for tape drag feeders. Each of these objects have an associated head offsets configuration property.

The easiest way to think of head offsets is to imagine the distance that any object on the head must travel to be exactly where another object is. For instance, if the nozzle is touching the bed of the machine and you want to have the camera focused on that exact same spot the head offsets are the distance in X, Y, and Z that the head must move to center the camera, in focus, where the nozzle was touching.

It is recommended that your primary nozzle be considered the center of the head. This means it's offsets will be set to 0, 0, 0. All other head objects will have offsets based on their distance from this nozzle.

To set up a head with a nozzle and a camera, follow these steps. The same steps can be used to set up any additional objects mounted to your head:

1. Place an object that is easy to center on on the machine's bed. Something as simple as a dot from a pen will work. This is our target.
1. In OpenPnP, on the Nozzles tab, select the nozzle from the table on the left and then choose the Nozzle Specific tab on the right. You will see the current offsets. Set them all to 0 and press Apply.
1. Using the jog controls, center the camera over the target and use the Z axis to make sure it's perfectly in focus. Record the X, Y, and Z values shown in OpenPnP.
1. Using the jog controls, center the nozzle over the target and lower the head until the nozzle just touches the top of the target.
1. Subtract the first set of X, Y, Z coordinates from the ones currently shown. These are the head offsets for the camera.
1. In OpenPnP, on the Cameras tab, select the camera from the table on the left and then choose the Camera Specific tab on the right. Fill in the calculated offsets and press Apply.

To test that your offsets are correct you can use the red targetting buttons next to the coordinates shown in OpenPnP:

1. One again, move your nozzle so it is just touching the target.
2. Press the lower red targetting button which looks like a broken square inside a circle. OpenPnP will move the head so that the target is now centered in the camera and in perfect focus.
3. If you then press the upper targetting button, which looks like a broken crosshair in a circle, OpenPnP will move the nozzle back to be touching the target. By swapping back and forth between these buttons you can easily test that the camera and nozzle are configured properly.

Actuators
---------

Cameras
-------

Feeders
-------

Operation
=========

Parts
-----

Packages
--------

Boards
------

Jobs
----

Your First Job
--------------

Troubleshooting
===============

FAQ
===

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

Debugging
---------

Getting Help
============

Discussion Group
----------------

There is an active discussion group at http://groups.google.com/group/openpnp. This will typically be the best place to get help.

IRC
---

We also have an IRC channel on Freenode IRC at #openpnp. If you don't have an IRC client, you can use [this web based one](http://webchat.freenode.net/?channels=openpnp).