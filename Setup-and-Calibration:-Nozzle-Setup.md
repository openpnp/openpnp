# Nozzle Setup

## Concepts
In OpenPnP we use a couple names for things that might not be familiar. These will become important down the road, so it's good to have an understanding of them now.

### Heads
In OpenPnP, the Head is the part of the machine that actually moves. The Head can have many things attached to it, and these may move independently, typically in Z. A common machine might have 1 Head, 1 Head mounted Camera and 2 Nozzles.

### Nozzles and Nozzle Tips
In OpenPnP, the thing that actually touches parts is called a Nozzle Tip. A Nozzle Tip is attached to a Nozzle and a Nozzle is attached to a Head. The reason for this distinction is that OpenPnP supports the ability to automatically change Nozzle Tips depending on the size of the parts you are picking up. The Nozzle stays the same, the Nozzle Tip is changed.

You may have any number of Nozzles on a Head, and any number of Nozzle Tips assigned to a Nozzle.

## Adding Nozzles
Currently there is no way to add nozzles using the GUI. You have to do it by hand by editing the [machine.xml](https://github.com/openpnp/openpnp/wiki/FAQ#where-are-configuration-and-log-files-located) configuration file.

You can edit configuration files in any text editor. We recommend [Sublime Text](https://www.sublimetext.com/) or [Atom](https://atom.io/).

**Important:** Make sure OpenPnP is not running when you edit the configuration files, or you will lose your changes when you exit OpenPnP.

The default configuration file in OpenPnP defines one nozzle and one nozzle tip. If this matches your machine you don't need to do this step. If you have additional nozzles or nozzle tips:

![screen shot 2016-06-18 at 11 15 41 am](https://cloud.githubusercontent.com/assets/1182323/16172859/06fbef82-3546-11e6-9666-73288748f2a3.png)

1. Open `machine.xml` in your favorite text editor.
2. Find the `nozzles` section, and within it the `nozzle` definition.
3. Copy the text from `<nozzle>` through `</nozzle>` to your clipboard.
4. After the `</nozzle>` line, paste the text you copied.
5. In the new `<nozzle>` section you just added, find the `id=` attribute and change the value to something random. It can be anything, you can even just change one character. It just needs to be different from the other one.
6. In the new `<nozzle-tip>` section you added as part of the `<nozzle>`, find the `id=` attribute and change it as above. It just has to be unique, it doesn't matter what the value is.
7. Save the file and start OpenPnP to check if your changes were okay.

## Adding Nozzle Tips
Adding Nozzle Tips is the same process as above, but instead of copying the entire `<nozzle>` section you just copy the `<nozzle-tip>` section contained within. Don't forget to change the id.

## Head Offsets
Nozzle Head Offsets tell OpenPnP where your nozzles are in relation to the other objects on your head. In an earlier step we set the Camera Head Offsets to zero, making the top camera the origin of the head. Now we'll tell OpenPnP how far the nozzle is from the camera in real world units.

![screen shot 2016-06-18 at 11 24 59 am](https://cloud.githubusercontent.com/assets/1182323/16172909/5247ebd4-3547-11e6-9dfa-8d63af3d66cd.png)

1. Place something on the bed of the machine that can be marked by the nozzle. A piece of a small, flattened blob of putty or clay will work. [Silly Putty](http://amzn.to/263ZnKm) works perfectly. This is our target.
2. Jog the machine so that the primary Nozzle is over the target and then lower the nozzle until it makes a clear mark on the target.
3. Click the X, Y and Z, DRO one time each. They will turn blue and show 0.000. They are now in relative coordinate mode and will show the distance you have moved since clicking them until you click them again.
4. Jog the machine so that the top camera is over the mark on the target, perfectly centered and in focus.
5. Find the Nozzle in the Machine Setup tab and find the Offsets fields in the panel on the right. It's on the second tab titled "Untitled".
6. Set the offsets to the X, Y and Z shown in the DROs. Press Apply. Note: For many machines it's not necessary to set the Z offset. It's only used in more complex setups.
7. For each additional Nozzle you need to setup, follow this same process.

## Selecting a Nozzle
If you have multiple nozzles on your machine, you can select the one you want to work with in OpenPnP by selecting it from the tools dropdown in the Machine Controls panel. The nozzle that is selected here is the one that will be used whenever you tell the machine to move a nozzle.

![screen shot 2016-06-18 at 11 26 13 am](https://cloud.githubusercontent.com/assets/1182323/16172925/a0c8edf8-3547-11e6-95cf-13d5cfba0c11.png)

## Advanced
On more advanced setups, especially those with multiple nozzles and nozzle tips, you may need to setup package compatibility and the nozzle tip changer.

### Package Compatibility
Package Compatibility lets OpenPnP know which Nozzle Tips you want to use for each package that you'll place. If you want to use a Nozzle Tip for every type of package you can just select "Allow Incompatible Packages?" and OpenPnP will consider that nozzle tip to be available for all parts. If you want to limit the parts that a particular nozzle tip works with, just uncheck the "Allow Incompatible Packages?" checkbox and check the packages you want to enable in the table below. Don't forget to hit Apply.

![screen shot 2016-06-18 at 11 29 46 am](https://cloud.githubusercontent.com/assets/1182323/16172939/fed3b05e-3547-11e6-8db8-c4cac423a34f.png)

### Nozzle Tip Changer
The Nozzle Tip Changer allows OpenPnP to automatically choose the best nozzle tip for a given part. Currently the only supported style of Nozzle Tip Changer is the so called "Three Position" changer. You can configure three motions that insert or remove a nozzle tip. 

TODO: This is an advanced topic and further documentation is needed.

![screen shot 2016-06-18 at 11 31 15 am](https://cloud.githubusercontent.com/assets/1182323/16172950/319d6534-3548-11e6-8a3a-ad975e4a6ec4.png)

## Nozzle Jogging
Now that your nozzle is configured, let's move it around and make sure it's working.

![screen shot 2016-06-18 at 11 33 19 am](https://cloud.githubusercontent.com/assets/1182323/16172974/8bf8e774-3548-11e6-8f5f-9608bc603bb0.png)

1. Make sure you've selected the nozzle as discussed in Selecting a Nozzle above.
2. Move the camera to an identifiable spot on the machine bed.
3. Click the position tool button ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/position-nozzle.svg) in the Machine Controls panel.
4. The nozzle should move to where the camera was previously looking. You can click the position camera button ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/position-camera.svg)  below to move the Camera back.

The Position Tool and Position Camera buttons can be used to switch back and forth between what the camera is pointing at and what the nozzle is pointing at.

***

| Previous Step                 | Jump To                 | Next Step                                   |
| ----------------------------- | ----------------------- | ------------------------------------------- |
| [[Top Camera Setup|Setup and Calibration: Top Camera Setup]] | [[Table of Contents|Setup and Calibration]] | [[Actuators and Other Head Objects|Setup and Calibration: Actuators and Other Head Objects]] |