# Before You Start

## Required Tools
* Ruler
* Double Sided Tape
* Test PCB
* Part Tape Strips

## Machine Working Outside OpenPnP
Before you start trying to make your machine work with OpenPnP you should first make sure all of it's major functionality is working outside of OpenPnP.

You should be able to jog the machine in X and Y, rotate your nozzle, and move the head up and down in Z. Ideally you will also be able to test that any pumps or solenoids can be controlled.

If your machine uses Gcode, you can use a tool like [Printrun](https://github.com/kliment/Printrun) to test the machine.

## Install OpenPnP

If you haven't already, [download and install OpenPnP](http://openpnp.org/downloads) and start it up for the first time. Run through the [[Quick Start]] to get a basic understanding of the interface and the major controls.

## Configuration and Log Locations

Get familiar with the locations of configuration and log files for OpenPnP. You will likely need to edit the `machine.xml` configuration file and you may need to look at the log files if things go wrong.

See [Where are configuration and log files located?](https://github.com/openpnp/openpnp/wiki/FAQ#where-are-configuration-and-log-files-located) in the [[FAQ]] for more information.

## UI Introduction