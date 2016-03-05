## About Fiducials

Fiducials, or fiduciary marks, are small identifiers designers place on PCBs specifically to help pick and place machines and other processes to visually align the board to a known reference point. A typical PCB that is designed for manufacture will have at least 3 fiducials and may have many more.

Fiducials are typically created by leaving a blank spot of copper on the PCB surrounded by a bare area called a keepout. This creates a bright, shiny mark on the PCB that is easy for computer vision algorithms to identify. The most common mark is a 1mm circle surrounded by a 2mm keepout.

## Fiducials in OpenPnP

OpenPnP has the ability to use 2 fiducials to detect the position of a PCB on the bed of the machine. This allows for automated, accurate, board locating either during job setup or during job run.

A fiducial in OpenPnP is defined by a package with a footprint that specifies what the fiducial looks like. For instance, a 1mm round fiducial is simply a footprint containing a 1x1mm pad with 100% roundness.

## Creating a Fiducial Package

Currently, creating fiducials in OpenPnP is a manual process that requires editing the `packages.xml` configuration file. You only need to perform this process once per type of fiducial you use. This experience will improve in the future, but for now here is how you do it:

1. Close OpenPnP if it's running.
2. Find your `packages.xml` and open it in your favorite text editor. For help finding the file see the [FAQ](https://github.com/openpnp/openpnp/wiki/FAQ#where-are-configuration-and-log-files-located)
3. Add a package definition to the file, following the syntax of the other packages in the file. Here is an example for a 1mm circle fiducial.
    ```
    <package id="FIDUCIAL-1X2">
        <outline units="Millimeters"/>
        <footprint units="Millimeters">
          <pad name="1" x="0.0" y="0.0" width="1.0" height="1.0" rotation="0.0" roundness="100.0"/>
        </footprint>
    </package>
    ```
    This example contains a single pad, centered at 0,0 with a width of 1mm and a height of 1mm and a roundness of 100%, making this a circle.

4. Once your package is defined, save the file and start OpenPnP. In the Packages tab you should see your new fiducial package. 
5. Go to the Parts tab and create a new Part to represent your fiducial. This is the part you will assign to placements to represent fiducials on boards. Set it's package to the fiducial package you created.

## Using Fiducials in Boards

Once you have defined your fiducial part, package and footprint you are ready to use it. You can assign that part to any placement in your board and set the placement type to Fiducial. This will tell OpenPnP to consider this placement for any fiducial operation it needs. You should set at least two placements as Fiducials.

For additional information on using your fiducials, please watch this video tutorial showing how to use fiducials: https://www.youtube.com/watch?v=xvmdvTroZj8

## Debugging

If OpenPnP is not finding your fiducials, try the following:

1. Look at https://github.com/openpnp/openpnp/wiki/FAQ#how-do-i-turn-on-debug-logging to see how to turn on debug logging.
2. Add 2 new lines to your log4j.properties file:

    ```
    log4j.logger.org.openpnp.machine.reference.vision=debug
    log4j.logger.org.openpnp.vision=debug
    ```

3. Restart OpenPnP and try to run your fidicual check again.
4. After you run it there will be a new directory in your `.openpnp` [directory](https://github.com/openpnp/openpnp/wiki/FAQ#where-are-configuration-and-log-files-located) called `org.openpnp.machine.reference.vision.OpenCvVisionProvider` and under that directory will be some images. The images show the process of trying to find the fiducials.

  If you understand computer vision a bit, take a look at the images and see if you can find any problems. Common problems are lighting and template size related. If it doesn't help, please post your images to [the OpenPnP mailing list](http://groups.google.com/group/openpnp) and someone will try to help.

