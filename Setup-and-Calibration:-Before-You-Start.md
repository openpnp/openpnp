# Before You Start

## Required Tools
* [Ruler](https://www.amazon.com/s/ref=nb_sb_noss?url=search-alias%3Daps&field-keywords=millimeter+steel+ruler): A precision steel ruler is recommended, in the units you intend to work in.
* [Double Sided Tape](https://www.amazon.com/gp/product/B0000DH8IT/ref=oh_aui_detailpage_o03_s00?ie=UTF8&psc=1): You'll use this to hold down your first tape strips and to provide a sticky surface on your PCB to place parts on.
* [Test PCB](https://github.com/openpnp/openpnp/tree/develop/samples/Demo%20Board): This directory contains a test PCB you can have made, or any PCB you already have will work.
* [Part Tape Strips](http://www.digikey.com/product-search/en/resistors/chip-resistor-surface-mount/65769?k=0805%20resistor%2010k): Some parts in tape strips to test with. Use something cheap, like an 0805 resistor.

## Machine Working Outside OpenPnP
Before you start trying to make your machine work with OpenPnP you should first make sure all of it's major functionality is working outside of OpenPnP.

You should be able to jog the machine in X and Y, rotate your nozzle, and move the head up and down in Z. Ideally you will also be able to test that any pumps or solenoids can be controlled.

If your machine uses Gcode, you can use a tool like [Printrun](https://github.com/kliment/Printrun) to test the machine.

## Install OpenPnP

If you haven't already, [download and install OpenPnP](http://openpnp.org/downloads) and start it up for the first time. Run through the [[Quick Start]] to get a basic understanding of the interface and the major controls.

## Configuration and Log Locations

Get familiar with the locations of configuration and log files for OpenPnP. You will likely need to edit the `machine.xml` configuration file and you may need to look at the log files if things go wrong.

See [Where are configuration and log files located?](https://github.com/openpnp/openpnp/wiki/FAQ#where-are-configuration-and-log-files-located) in the [[FAQ]] for more information.

***

| Previous Step                 | Jump To                 | Next Step                                   |
| ----------------------------- | ----------------------- | ------------------------------------------- |
| [[Setup and Calibration]] | [[Top of Page|Setup and Calibration: Before You Start]] or [[Table of Contents|Setup and Calibration]] | [[Driver Setup|Setup and Calibration: Driver Setup]] |