# Feeders

Feeders are sources of parts for OpenPnP to place. A feeder can be as simple as a strip of parts taped to the machine or as complex as a fully automated motorized feeder.

There are currently four types of feeders supported Strip Feeder, Drag Feeder, Tray Feeder, and Tube Feeder. The differences between each type will be covered below.

## Concepts
### Pick Location
Every feeder has a pick location, which is where the nozzle will go when it needs to pick a part from that feeder. Some feeders change their pick location over time; for instance, a Strip Feeder will tell OpenPnP that the pick location has moved down the tape after each pick.

### Setting Z
It's important to set the right Z height for the feeder so that the nozzle just barely touches the part before it picks. Each feeder has a Z setting in it's configuration panel, although some of them look different than others. In all cases, just look for the Z field in the settings and make sure it's set to where the nozzle needs to go to pick the part.

## Choosing a Feeder
Here is a short description of the feeder types supported by OpenPnP:
 
* ReferenceStripFeeder: A feeder that allows the user to place a cut piece of SMT tape on the machine. The feeder will advance along the tape and pick parts from it. Cover film must be removed manually. Supports vision for setup and part centering by referencing the holes in the tape.

  See [[ReferenceStripFeeder]]'s page for more information and help.

* ReferenceDragFeeder: A "drag" feeder which allows the use of an Actuator, typically a solenoid with a pin, to advance the tape by dragging it. Basic vision for part center detection is also supported.

* ReferenceTrayFeeder: Supports 2D arrays of parts in trays. Currently limited to trays that are aligned at 90 degrees in X and Y. Simple incremental pick, no vision.

* ReferenceTubeFeeder: The simplest feeder which picks from the same location every time. Intended to be used with a vibratory tube feeder that presents a part at the same location repeatedly.

* ReferenceAutoFeeder: A basic auto feeder controller for feeders that feed on their own using hardware. Uses a number of Actuators to perform feed operations.

  See [[ReferenceAutoFeeder]]'s page for more information and help.

* ReferenceSlotAutoFeeder: A feeder slot system masquerading as a regular feeder. Using ReferenceSlotAutoFeeder you can configure slots on advanced feeder systems so that you can easily move feeders from slot to slot. Similar to the ReferenceAutoFeeder, it uses Actuators to trigger feeders.

  See [[ReferenceSlotAutoFeeder]]'s page for more information and help.

## Adding a Feeder
Let's add a Strip Feeder to the machine so that we can test the feeder system.

1. Use your [Double Sided Tape](http://amzn.to/1Xw7XMA) to tape a small [Part Tape Strip](http://www.digikey.com/product-search/en/resistors/chip-resistor-surface-mount/65769?k=0805%20resistor%2010k) to the bed of the machine. You can cut off a small section of 10 or so parts. These parts will be wasted, so don't use anything important. Chip resistors are good for this.
2. Once securely taped down, remove the cover film from the cut strip using [Tweezers](http://amzn.to/1UUx9ZN).
3. Open the Feeders tab and click the green plus button ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/general-add.svg) to open the add feeder dialog.
4. Select ReferenceStripFeeder from the dialog and press Accept.
5. The new feeder is added to the table below and selected automatically. Double click the Name field to give it a descriptive name. Let's call this one "Test Feeder".
6. On the right side of the screen, in the feeder settings, find the Part dropdown and select the "R0805-1K" part. It's okay if this doesn't match the part you are using for now.
7. In the Tape Settings section below, fill in the Tape Width and Part Pitch fields. If you are using chip resistors larger than 0402 you can use the default settings of 8mm tape width and 4mm part pitch.
8. Click the Auto Setup button and follow the on screen instructions to finish setting up the feeder. There is more information on the [[ReferenceStripFeeder]] page about this process, and it's worth taking a look at it to help you.
9. When the Auto Setup process finishes the camera should be centered perfectly over the first part in the tape. If it's not, try the Auto Setup process again. Sometimes lighting conditions can cause it to make mistakes.
10. Jog the nozzle so that it is just touching the first part in the tape.
11. Fill in the value shown in the Z DRO in the Z fields in the strip feeder setup. This will set the height of the feeder so that OpenPnP knows where to pick parts.

## Common Settings
### Part Selection
Every feeder has a Part field that you must fill in. By telling OpenPnP which part a feeder is holding OpenPnP can make intelligent decisions about which feeders to use for a job. You can even have multiple feeders for the same part, in case it's a part you use many of.

## Locate a part
To test your settings for a feeder you can make the camera or the nozzle go to the location of the part that's going to be fed next. To position the camera on the part, click the ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/position-camera-on-feeder.svg) button. To position the nozzle, click the ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/position-nozzle-on-feeder.svg) button.

## Pick a Part

Now that you've configured a feeder, click the Feed and Pick button ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/pick.svg) to perform a pick operation. If all goes well the machine should feed a part and pick it up with the nozzle. You can use the Discard button we set up previously to drop the part.

***

| Previous Step                 | Jump To                 | Next Step                                   |
| ----------------------------- | ----------------------- | ------------------------------------------- |
| [[Discard Location|Setup and Calibration: Discard Location]] | [[Table of Contents|Setup and Calibration]] | [[Bottom Vision|Setup and Calibration: Bottom Vision]] |