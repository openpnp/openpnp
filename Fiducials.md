## About Fiducials

Fiducials, or fiduciary marks, are small identifiers designers place on PCBs specifically to help pick and place machines and other processes to visually align the board to a known reference point. A typical PCB that is designed for manufacture will have at least 3 fiducials and may have many more.

Fiducials are typically created by leaving a blank spot of copper on the PCB surrounded by a bare area called a keepout. This creates a bright, shiny mark on the PCB that is easy for computer vision algorithms to identify. The most common mark is a 1mm circle surrounded by a 2mm keepout.

## Fiducials in OpenPnP

OpenPnP has the ability to use 2 fiducials to detect the position of a PCB on the bed of the machine. This allows for automated, accurate, board locating either during job setup or during job run.

A fiducial in OpenPnP is defined by a package with a footprint that specifies what the fiducial looks like. For instance, a 1mm round fiducial is simply a footprint containing a 1x1mm pad with 100% roundness.

## Creating a Fiducial Package

You only need to perform this process once per type of fiducial you use.

1. Create or select an existing Package from the Packages tab.
2. On the right of the window you should see a Footprint tab. Select this and you will see the Footprint editor.
3. Set the Body Width and Body Height to 0, and set the Units to the units of your Fiducial.
4. Click the Add button ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/file-add.svg) to add a new Pad. A Fiducial will typically have just one pad.
5. Set the name of the Pad to anything you like. I typically just use "1".
6. Set the X and Y position of the Pad to 0, and set the Width and Height to the diameter of the Fiducial. If the Fidicual is round set the Roundness to 100%. For example, a 1mm round fiducial would be defined as X = 0, Y = 0, Width = 1, Height = 1, Roundness = 100%.
7. Go to the Parts tab and create a new Part to represent your fiducial. This is the part you will assign to placements to represent fiducials on boards. Set it's package to the fiducial package you created.

## Using Fiducials in Boards

Once you have defined your fiducial part, package and footprint you are ready to use it. You can assign that part to any placement in your board and set the placement type to Fiducial. This will tell OpenPnP to consider this placement for any fiducial operation it needs. You should set at least two placements as Fiducials.

For additional information on using your fiducials, please watch this video tutorial showing how to use fiducials: https://www.youtube.com/watch?v=xvmdvTroZj8

## Troubleshooting

If OpenPnP is not finding your fiducials, try the following:

1. Make sure you have set the Units Per Pixel values for your camera. A quick way to test this is to right click the camera view, turn on the Ruler Reticle and see if it matches a ruler placed on the machine.
2. Look at https://github.com/openpnp/openpnp/wiki/FAQ#how-do-i-turn-on-debug-logging to see how to turn on debug logging.
3. Add 2 new lines to your log4j.properties file:

    ```
    log4j.logger.org.openpnp.machine.reference.vision=debug
    log4j.logger.org.openpnp.vision=debug
    ```

4. Restart OpenPnP and try to run your fidicual check again.
5. After you run it there will be a new directory in your `.openpnp` [directory](https://github.com/openpnp/openpnp/wiki/FAQ#where-are-configuration-and-log-files-located) called `org.openpnp.machine.reference.vision.OpenCvVisionProvider` and under that directory will be some images. The images show the process of trying to find the fiducials.

  If you understand computer vision a bit, take a look at the images and see if you can find any problems. Common problems are lighting and template size related. If it doesn't help, please post your images to [the OpenPnP mailing list](http://groups.google.com/group/openpnp) and someone will try to help.

