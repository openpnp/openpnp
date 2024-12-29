This file lists major or notable changes to OpenPnP in chronological order. This is not
a complete change list, only those that may directly interest or affect users.

# 2024 Q4

## New Features

* Optimize fiducial checking in job using travelling salesman. #1707
* I&S supports controllers which send compressed position reports without whitespaces between the axes. #1705
* An option to skip auto-focus calibration of the up looking camera via issues & solutions. #1700
* Improved Chinese translation. #1701 #1703 #1696
* Tooltip delay timeout is prolonged to 1min. The text are rather long and require focus on info so long dismiss timeout. #1691
* On job error: Automatically select the nozzle, and update all linked tables for the cause. #1678
* Add Feeder.Before/AfterFeed scripts, and Feeder.Before/AfterTakeBack. These events can be used for stock control purposes. #1685
* Add Machine.AfterDriverHoming scripting event which gets called after homing all the drivers, and before calibration using the homing fiducial. #1681
* Pandaplacer Feeder - use full camera image for auto setup #1686
* A new I&S solution to warn if safe z is not in the conventional negative range #1682
* Improve usability of I&S solutions that suggest gcode changes #1682
* Nozzle tip loading strategy options in the job processor #1659

## Bug Fixes

* Fix the Nozzle background calibration diagnostics in case there is not a single pixel matching the worst-case limits #1709
* Main frame window divider does not work correctly when changing window size. Eg. when switching to multiwindow and back. #1689
* The Z offset of the second, third, etc. nozzle is now definitively calibrated by I&S solution #1680
* Fix bug in BlindsFeeder where the nozzle tip was moving in the wrong direction. #1679

## Installation and distribution changes

* Disabled bundling a JDK with the Win32 build. It's no longer available for download.
* Update install4j bundled JDK version from 17 to 23. 17 is no longer supported, and the minimum is 21, which is already considered out of date. 23 is the current supported version.
* Permanent fix for MacOS builds (#1653)

## Internal Changes

* Design cleanup for ReferenceHeadMountable #1687



# 2024 Q3

## New Features

* Allow use of an arbitrary gstreamer pipeline as a video source. This can be for example a v4l2src, nvarguscamerasrc, rpicamsrc, rtsp receiver and decoder, media file reader and playbin, etc., etc #1665
* Avoid accidental leading or trailing whitespace in Part IDs etc #1668
* I&S works when using a generic G-code setup instead of using M115 (auto-discover known controller firmwares) #1663
* Avoid inefficient Z moves #1656 #1657
* Allow some movement around the bottom camera to move without going via safe Z #1657

## Bug Fixes

* ReferencePushPullFeeder - use full camera image for auto setup #1673
* Fix nozzle calibration when using greyscale method #1676
* Default bottom vision pipeline size and position accuracy impovement #1672
* Various fixes for part size checking, and part size measurement using vision compositing. #1671
* Fix mirrored vision compositing preview. #1670
* Fix searchAngle parameter in minAreaRect vision pipeline stage #1667
* Fix ReferenceStripFeeder vision. #1660
* Fix exception handler that suppressed movement exceptions during vision #1657
* BambooFeeder removes the unwanted reset of a custom vision pipeline during Auto Setup #1651



# 2024 Q2

## New Features

* Improved masking for Multi-Shot vision pipeline. #1638
* BambooFeederAutoVision #1622
* If using a manual nozzle tip change: Jobs continue with just a single click after performing the requested nozzle tip change #1617
* Optimize placements of multi nozzle machines #1574 #1614
* Changes the actuator usage for automatic nozzle tip changers to use False for unload and True for load. Previously it was using True for both load and unload. #1620

## Bug Fixes

## Installation and distribution changes

* Modernize macOS app icon #1633
* Fix broken Camera permissions on MacOS Monterey #1628

## Internal Changes

* Update ReferencePushPullFeeder to use FeederVisionHelper #1623



# 2024 Q1

## New Features

* Add an option to automatically load the most recent job #1616
* If using a manual nozzle tip change: Combine the interruptions for unload and load. #1609
* Enhanced UI for Manual Change Location #1611
* Linux support for Neoden 4 cameras #1604
* Send FeedRate and Acceleration on change only #1600
* Optimize placements of multi nozzle machines #1574
* Allow children of panels to be replaced #1598

## Bug Fixes

* Fix pick location for ReferenceRotatedTrayFeeder #1607 and ReferenceTrayFeeder #1606
* Fix manual change location shifted when recalibrating the camera to nozzle offset using I&S #1612 #1613



# 2023 Q4

## New Features

* Log the time it takes to get a response from the write queue #1595

## Bug Fixes

* Fix erroneous reset of placement transform when cell selected not edited #1597



# 2023 Q3

## New Features

* Better Test Alignment. Take another picture after the part has been centered, making sure the lights are switched on and the camera image settled. Plus it also displays the final (overall) offsets result. #1584
* On a job error: select the object which was the cause of the error #1577
* Enhance the Job Placements table by immediately updating the Status column if a placement is enabled or disabled using the mouse #1576
* CSV UFT-16 support #1573

## Bug Fixes

* Proper condition for SimulationModeMachine simulated actuator delay #1596
* ReferenceRotatedTrayFeeder pick from correct location #1451 #1581

## Installation and distribution changes

* It appears AdoptOpenJDK released a new version that only has builds for a few archs. Changed the version spec from "latest" the most recent with all the archs.
* Switch to the release jSerialComm 2.10.2 #1571

## Internal Changes

* AdvancedCameraCalibration aids in offline-debugging #1583
* ImageCamera fixes for using a picture of a real machine in simulation #1579



# 2023 Q2

## New Features

* Parallax fiducial locator #1565

## Bug Fixes

* Make sure the NashornScriptEngineFactory is always loaded #1564

## Installation and distribution changes

* Update Nashorn to version 15.4 for supporting Java 17 & 19 #1563
* Update openpnp-capture-java to 0.028 #1562



# 2023-05-03

Removed state from AbstractMachine. This might cause problems loading machine.xml in
the unlikely event that you configured a ActuatorSignaler with a non empty machine state.
To fix this, either remove the binding to machine state be setting it to empty before 
the upgrade or remove "MachineState" manually from the signalers section of your machine.xml.

Behaviour of ActuatorSignaler changed to only call the actuator if the job state has changed.

# 2023-05-02

Named CSV importer renamed to Reference CSV importer

Altium CSV importer added which accepts the default center-x/center-y columns and
correctly handles the rotation of bottom side parts.

# 2023-03-14

## Java 17+ Support

OpenPnP is now compatible with Java versions 11, 17, and 19. Thank you to @lags
and others! See the PR at https://github.com/openpnp/openpnp/pull/1493 for more
details.

Other versions of Java are no longer explicity supported or tested but they may
still work. In general, any version 11+ should work.

The installers now include a current version of OpenJDK 17, rather than a very
out of date JDK 8.

## MacOS Silicon Support and Fixes

OpenPnP now supports Apple Silicon natively, including in openpnp-capture and
openpnp-opencv.

OpenPnP Capture Camera is now fixed on MacOS and should work correctly on both
x86 and Apple Silicon.

This version of OpenPnP changes from a installer to a single app archive.
You can install it by dragging the app to your /Applications folder.

The application and supporting files are now Code Signed so that they should
run without having to disable security.


# 2023-02-26

## Board Z

Changed the Capture Tool Location button on the Job table to only update the Z
and not the X, Y, or Rotation of the selected board. Also added the capability
to update multiple boards to the same Z value.

https://github.com/openpnp/openpnp/pull/1527

# 2023-02-14

## Panelization and other UI changes/improvements

Panels are now stand-alone entities much like boards. They are now stored in *.panel.xml files
rather than being "built-into" the job file. Panels can now have arbitrary layouts and can consist
of any number of different boards and/or subpanels. Many of the issues issues that have been
reported with the legacy panelization method have been fixed.

Two new tabs have been added to the UI. The Panels tab is the primary area for creating and editing
panels. The Boards tab is now the primary areas for creating and editing boards. The Job tab is now
primarily for selecting boards and/or panels (defined on the aforementioned tabs) to be assembled, 
setting their location and orientation on the machine and, of course, executing the job.

There is now a button on the Job tab (the Panels and Boards tabs have one as well) that opens a 
graphical viewer that displays the physical layout of the job (or Panel or Board).

The column widths on the Job, Panels, and Boards tabs are now remembered between OpenPnP sessions.
Numeric columns on those tabs are also now aligned on their decimal points. 

See also:
https://github.com/openpnp/openpnp/pull/1507

