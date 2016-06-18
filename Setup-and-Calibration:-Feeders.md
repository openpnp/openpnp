# Feeders

Feeders are sources of parts for OpenPnP to place. A feeder can be as simple as a strip of parts taped to the machine or as complex as a fully automated motorized feeder.

There are currently four types of feeders supported Strip Feeder, Drag Feeder, Tray Feeder, and Tube Feeder. The differences between each type will be covered below.

## Concepts
### Pick Location
Every feeder has a pick location, which is where the nozzle will go when it needs to pick a part from that feeder. Some feeders change their pick location over time; for instance, a Strip Feeder will tell OpenPnP that the pick location has moved down the tape after each pick.

### Setting Z
It's important to set the right Z height for the feeder so that the nozzle just barely touches the part before it picks. Each feeder has a Z setting in it's configuration panel, although some of them look different than others. In all cases, just look for the Z field in the settings and make sure it's set to where the nozzle needs to go to pick the part.

## Adding a Feeder
Let's add a Strip Feeder to the machine so that we can test the feeder system.

1. Use your [Double Sided Tape](http://amzn.to/1Xw7XMA) to tape a small [Part Tape Strip](http://www.digikey.com/product-search/en/resistors/chip-resistor-surface-mount/65769?k=0805%20resistor%2010k) to the bed of the machine. You can cut off a small section of 10 or so parts. These parts will be wasted, so don't use anything important. Resistors are good for this.
2. Once securely taped down, remove the cover film from the cut strip using [Tweezers](http://amzn.to/1UUx9ZN).
3. Open the Feeders tab and click the green plus button ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/file-add.svg) to open the add feeder dialog.
4. Select ReferenceStripFeeder from the dialog and press Accept.
5. The new feeder is added to the table below and selected automatically. Double click the Name field to give it a descriptive name. Let's call this one "Test Feeder".
6.

## Choosing a Feeder
Here is a short description of the other feeder types supported by OpenPnP:
 
* ReferenceStripFeeder: A feeder that allows the user to place a cut piece of SMT tape on the machine. The feeder will advance along the tape and pick parts from it. Cover film must be removed manually. Supports vision for setup and part centering by referencing the holes in the tape.

  See [[ReferenceStripFeeder]]'s page for more information and help.

* ReferenceDragFeeder: A "drag" feeder which allows the use of an Actuator, typically a solenoid with a pin, to advance the tape by dragging it. Basic vision for part center detection is also supported.

* ReferenceTrayFeeder: Supports 2D arrays of parts in trays. Currently limited to trays that are aligned at 90 degrees in X and Y. Simple incremental pick, no vision.

* ReferenceTubeFeeder: The simplest feeder which picks from the same location every time. Intended to be used with a vibratory tube feeder that presents a part at the same location repeatedly.

## Common Settings
### Part Selection
## Feeder Specifics

## Pick a Part

Now that you've configured a feeder, click the Feed and Pick button ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/nozzletip-load.svg) to perform a pick operation. If all goes well the machine should feed a part and pick it up with the nozzle.

***

| Previous Step                 | Jump To                 | Next Step                                   |
| ----------------------------- | ----------------------- | ------------------------------------------- |
| [[Bottom Camera Setup|Setup and Calibration: Bottom Camera Setup]] | [[Table of Contents|Setup and Calibration]] | [[Discard Location|Setup and Calibration: Discard Location]] |