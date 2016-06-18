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

## Head Offsets (link to general page on setting head offsets?)

## Selecting a Nozzle

## Advanced

### Package Compatibility

### Nozzle Tip Changer

## Nozzle Jogging


***

| Previous Step                 | Jump To                 | Next Step                                   |
| ----------------------------- | ----------------------- | ------------------------------------------- |
| [[Top Camera Setup|Setup and Calibration: Top Camera Setup]] | [[Table of Contents|Setup and Calibration]] | [[Actuators and Other Head Objects|Setup and Calibration: Actuators and Other Head Objects]] |