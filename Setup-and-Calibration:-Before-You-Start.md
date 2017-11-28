# Before You Start

## Required Tools
* [Ruler](http://amzn.to/2642K3R): A precision steel ruler is recommended, in the units you intend to work in.
* [Small Electronics Tweezers](http://amzn.to/1UUx9ZN): Any small tweezers will do. It's worth having a few sets because you will use these a lot.
* [Double Sided Tape](http://amzn.to/1ZYSbbe): You'll use this to hold down your first tape strips and to provide a sticky surface on your PCB to place parts on.
* [Test PCB](https://github.com/openpnp/openpnp/tree/develop/samples/Demo%20Board): This directory contains a test PCB you can have made, or any PCB you already have will work.
* [Part Tape Strips](http://www.digikey.com/product-search/en/resistors/chip-resistor-surface-mount/65769?k=0805%20resistor%2010k): Some parts in tape strips to test with. Use something cheap, like an 0805 resistor.

**Note:** Some of the links above are Amazon Affiliate links. If you buy something from one of the links it will help fund development of OpenPnP.

## Machine Working Outside OpenPnP
Before you start trying to make your machine work with OpenPnP you should first make sure all of its major functionality is working outside of OpenPnP.

You should be able to jog the machine in X and Y, rotate your nozzle, and move the head up and down in Z. Ideally you will also be able to test that any pumps or solenoids can be controlled.

If your machine uses Gcode, you can use a tool like [Printrun](https://github.com/kliment/Printrun) to test the machine.

If you have not yet chosen a motion controller, see [[Motion Controllers]] for some helpful advice.

## Install OpenPnP

If you haven't already, [download and install OpenPnP](http://openpnp.org/downloads) and start it up for the first time. Run through the [[Quick Start]] to get a basic understanding of the interface and the major controls.

## Configuration and Log Locations

Get familiar with the locations of configuration and log files for OpenPnP. You will likely need to edit the `machine.xml` configuration file and you may need to look at the log files if things go wrong.

See [Where are configuration and log files located?](https://github.com/openpnp/openpnp/wiki/FAQ#where-are-configuration-and-log-files-located) in the [[FAQ]] for more information.

***

| Previous Step                 | Jump To                 | Next Step                                   |
| ----------------------------- | ----------------------- | ------------------------------------------- |
| [[Setup and Calibration]] | [Table of Contents](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration) | [Driver Setup](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Driver-Setup) |