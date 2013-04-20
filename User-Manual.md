# Introduction

OpenPnP is an Open Source SMT pick and place system designed and built with the hobbyist in mind but with the features and power to run commercial pick and place operations. It's goal is to bring pick and place to the desktop of anyone who needs to make more than a few of something.

OpenPnP is made up of three components. The hardware, the firmware and the software. This User Manual focuses only on the software. To get information about the hardware and firmware please visit http://openpnp.org.

The purpose of this manual is to help you get the software up and running, and to teach you how to configure and operate it.

# Getting Started

OpenPnP is currently considered Alpha level software. This means that it is still under heavy development, may have major bugs and may be entirely unreliable. If you are still interested in continuing...

## Installation

### Prerequisites

OpenPnP runs on the Java platform and requires the Java runtime version 6 or higher to run. You can download the latest version of Java from http://java.com/getjava.

OpenPnP is designed to run on Mac, Windows and Linux. Other platforms may be supported due to the nature of Java, but they are not recommended. OpenPnp is written and tested on Mac and used regularly on Windows. These two platforms are recommended for the best compatibility.

### Download

Visit http://openpnp.org to find out how to download the latest snapshot or release of OpenPnP.

### Install

Unzip the software into a directory of your choosing. Typically this would be the same place you keep your other applications.

## Running OpenPnP

Inside the folder you unzipped OpenPnP to there is an `openpnp.sh` and `openpnp.bat` script. These should work for Windows, Mac and Linux. For Mac and Linux, run `openpnp.sh` and for Windows run `openpnp.bat`. After a short wait you should see the OpenPnP Main Window. If something goes wrong, visit the Troubleshooting section of this document for help.

# The User Interface

OpenPnP is primarily a single window interface, broken up into multiple sections. Those sections will be explained in detail below.

## The Main Window
## Machine Controls
## The Camera Panel
## The Tabs
## Shortcuts

There are a few important keyboard shortcuts that are critical to know to use OpenPnP. All of the shortcuts use Ctrl + another key. They are:

* Ctrl+Tab: Open the jog controls window which gives you visible buttons for most of the other shortcuts.
* Ctrl+Arrow Key: Jog the currently selected Nozzle in X and Y. Up and Down arrows jog in Y and Left and Right arrows jog in X.
* Ctrl+PageDn, PageUp: Jog the currently selected Nozzle up and down in Z.
* Ctrl+>, Ctrl+<: Rotate the currently selected Nozzle clockwise and counterclockwise.
* Ctrl+Plus, Ctrl+Minus: Change the jog increment slider. This changes how far each jog key will move the Nozzle.

# Setup
## The Driver
## Heads
## Actuators
## Cameras
## Feeders

# Operation
## Parts
## Packages
## Boards
## Jobs
## Your First Job

# Troubleshooting

# FAQ

# Advanced Topics
## Configuration Files
## Custom Implementations
## Debugging

# Getting Help