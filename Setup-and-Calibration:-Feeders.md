# Feeders

Feeders are sources of parts for OpenPnP to place. A feeder can be as simple as a strip of parts taped to the machine or as complex as a fully automated motorized feeder.

There are currently four types of feeders supported Strip Feeder, Drag Feeder, Tray Feeder, and Tube Feeder. The differences between each type will be covered below.

## Concepts
### Pick Location
### Setting Z
## Choosing a Feeder Type
* ReferenceStripFeeder: A feeder that allows the user to place a cut piece of SMT tape on the machine. The feeder will advance along the tape and pick parts from it. Cover film must be removed manually. Supports vision for setup and part centering by referencing the holes in the tape.

  See [[ReferenceStripFeeder]]'s page for more information and help.

* ReferenceDragFeeder: A "drag" feeder which allows the use of an Actuator, typically a solenoid with a pin, to advance the tape by dragging it. Basic vision for part center detection is also supported.

* ReferenceTrayFeeder: Supports 2D arrays of parts in trays. Currently limited to trays that are aligned at 90 degrees in X and Y. Simple incremental pick, no vision.

* ReferenceTubeFeeder: The simplest feeder which picks from the same location every time. Intended to be used with a vibratory tube feeder that presents a part at the same location repeatedly.

## Common Settings
### Part Selection
## Feeder Specifics

## Pick a Part

Now that you've configured a feeder, click the Feed and Pick button ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/load.svg) to perform a pick operation. If all goes well the machine should feed a part and pick it up with the nozzle.

***

| Previous Step                 | Jump To                 | Next Step                                   |
| ----------------------------- | ----------------------- | ------------------------------------------- |
| [[Bottom Camera Setup|Setup and Calibration: Bottom Camera Setup]] | [[Table of Contents|Setup and Calibration]] | [[Discard Location|Setup and Calibration: Discard Location]] |