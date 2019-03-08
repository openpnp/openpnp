# Nozzle Setup

## Concepts
In OpenPnP we use a couple names for things that might not be familiar. These will become important down the road, so it's good to have an understanding of them now.

### Heads
In OpenPnP, the Head is the part of the machine that actually moves. The Head can have many things attached to it, and these may move independently, typically in Z. A common machine might have 1 Head, 1 Head mounted Camera and 2 Nozzles.

### Nozzles and Nozzle Tips
In OpenPnP, the thing that actually touches parts is called a Nozzle Tip. A Nozzle Tip is attached to a Nozzle and a Nozzle is attached to a Head. The reason for this distinction is that OpenPnP supports the ability to automatically change Nozzle Tips depending on the size of the parts you are picking up. The Nozzle stays the same, the Nozzle Tip is changed.

You may have any number of Nozzles on a Head, and any number of Nozzle Tips assigned to a Nozzle.

## Adding Nozzles
If you have more than one nozzle on your machine:

1. Open the Machine Setup Tab.
2. Find the head you want to add a nozzle to and look for the Nozzles item below it. Select it.
3. Add the nozzle by pressing the add nozzle button ![](https://rawgit.com/openpnp/openpnp/6b20cb121e36ec8b0eecdf6190aee5f448c51c41/src/main/resources/icons/nozzle-add.svg).  
4. Select a nozzle type from the dialog. ReferenceNozzle is good for most setups.
5. Press Accept and the new nozzle will appear in the list. Select it to open it's properties.

## Adding Nozzle Tips
If you have more than one nozzle tip on your machine:

1. Open the Machine Setup Tab.
2. Find the nozzle you want to add a nozzle tip to and look for the Nozzle Tips item below it. Select it.
3. Add the nozzle tip by pressing the add button ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/general-add.svg).  
4. Select the new nozzle tip to open it's properties.
5. To use the machine with the nozzle tip you just created you need to load it onto the nozzle. Press the load button ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/nozzletip-load.svg) to do this.

## Head Offsets
Nozzle head offsets tell OpenPnP where the nozzles are in relation to the other objects on your head. In an earlier step we set the Camera Head Offsets to zero, making the top camera the origin of the head. Now we'll tell OpenPnP how far the nozzles are from the camera in real world units. The following steps have to be repeated for each nozzle attached to the head.

![nozzle offset in machine configuration](https://user-images.githubusercontent.com/3868450/50729987-9910b200-1143-11e9-9d13-dd234b91d97e.PNG)

The offsets have to be determined very accurate in order for best placement results. There is a wizard integrated into OpenPnP that guides you through the process.

![openpnp nozzle offset wizard](https://user-images.githubusercontent.com/3868450/50702952-c29de080-1052-11e9-9d0d-324d68daa401.PNG)
1. In the wizard select the downlooking camera you are referring to.
2. Put a little clay, [Silly Putty](http://amzn.to/263ZnKm) or flour on an old PCB that can be marked by the nozzle. A piece of a small, flattened blob of putty or clay will work as well as flour. Whatever object you have chosen, this is our target.
3. Jog the nozzle tip so that the primary nozzle tip is over the target.
4. Lower the nozzle until it makes a clear mark on the target.
5. Now rotate the nozzle by 360°, to exclude possible nozzle tip runout from the measurement. Raise the nozzle tip to park position in z direction. Store the nozzle mark position by clicking the button.
6. Jog the machine so that the camera is over the marked target, perfectly centered and in focus. In that position click the button "calculate nozzle offset". It will be displayed in the textfields below.
7. Confirm that the calculated offsets are valid.
8. Hit apply. You may want to repeat that whole process with a new mark to validate measurements.

For each nozzle you need to setup, follow this same process.
IMPORTANT! Do not set any extruder or tool offsets in your motion controller for the nozzle(s), this is handled by OpenPnP as it needs to know these distances anyway.

## Nozzle Tip Runout Calibration
If you encounter runout on nozzle tips, this may lead to parts placed at offset and mispicks. Nozzle Tip Runout Compensation may help to improve the machines placement accuracy by measuring the offset and apply it dynamically while moving the nozzle tip at any angle. See the comparison in the following animations. Left side is compensation disabled, right side enabled.

![nozzle tip without compensation](https://user-images.githubusercontent.com/3868450/51180932-110c7400-18ca-11e9-8518-aff180ec30d5.gif)
![runout compensated](https://user-images.githubusercontent.com/3868450/51181050-5df04a80-18ca-11e9-887b-b25f2942505b.gif)

Setup of the [runout compensation feature is described on a separate page](https://github.com/openpnp/openpnp/wiki/Nozzle-Tip-Runout-Compensation-Setup)


## Selecting a Nozzle
If you have multiple nozzles on your machine, you can select the one you want to work with in OpenPnP by selecting it from the tools dropdown in the Machine Controls panel. The nozzle that is selected here is the one that will be used whenever you tell the machine to move a nozzle.

![screen shot 2017-04-14 at 6 12 12 pm 2](https://cloud.githubusercontent.com/assets/1182323/25058368/2b3754c4-213e-11e7-9e48-3c984b33d678.png)

## Advanced
On more advanced setups, especially those with multiple nozzles and nozzle tips, you may need to setup package compatibility and the nozzle tip changer.

### Package Compatibility
Package Compatibility lets OpenPnP know which Nozzle Tips you want to use for each package that you'll place. If you want to use a Nozzle Tip for every type of package you can just select "Allow Incompatible Packages?" and OpenPnP will consider that nozzle tip to be available for all parts. If you want to limit the parts that a particular nozzle tip works with, just uncheck the "Allow Incompatible Packages?" checkbox and check the packages you want to enable in the table below. Don't forget to hit Apply.

![screen shot 2016-06-18 at 11 29 46 am](https://cloud.githubusercontent.com/assets/1182323/16172939/fed3b05e-3547-11e6-8db8-c4cac423a34f.png)

### Nozzle Tip Changer
The Nozzle Tip Changer allows OpenPnP to automatically choose the best nozzle tip for a given part. Currently the only supported style of Nozzle Tip Changer is the so called "Four Position" changer. You can configure four motions that insert a nozzle tip. To return a nozzle the four position moves are executed in reverse before picking up a new one. 
Note that the _First Location_ is always approached from Z0 above and the last move is always returning from _Last Location_ to Z0 so the four moves are actually six moves.

TODO: This is an advanced topic and further documentation is needed.

![screen shot 2017](https://cloud.githubusercontent.com/assets/4028409/22084570/eced8ab8-ddd0-11e6-9e53-a3dcf60e647b.JPG)

## Nozzle Jogging
Now that your nozzle is configured, let's move it around and make sure it's working.

![screen shot 2017-04-14 at 6 12 12 pm 3](https://cloud.githubusercontent.com/assets/1182323/25058367/2b3680f8-213e-11e7-9616-fc062c82d442.png)

1. Make sure you've selected the nozzle as discussed in Selecting a Nozzle above.
2. Move the camera to an identifiable spot on the machine bed.
3. Click the position tool button ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/position-nozzle.svg) in the Machine Controls panel.
4. The nozzle should move to where the camera was previously looking. You can click the position camera button ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/position-camera.svg)  below to move the Camera back.

The Position Tool and Position Camera buttons can be used to switch back and forth between what the camera is pointing at and what the nozzle is pointing at.

***

| Previous Step                 | Jump To                 | Next Step                                   |
| ----------------------------- | ----------------------- | ------------------------------------------- |
| [Steps Per Mm](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Steps-Per-Mm) | [Table of Contents](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration) | [Actuators](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Actuators) |