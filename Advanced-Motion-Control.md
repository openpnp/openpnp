## What is it?
Advanced Motion Control is the chosen label for a feature-set that aims at improving Motion Control. Because Motion Control is such a fundamental function inside OpenPnP (and because it is not new but only advanced), the feature-set has touched many parts in a Machine Setup: Axes, Drivers, Nozzles, Cameras, Actuators and the new Motion Planners. To put it all together, this page must also touch and connect many of these topics. 

Note: this guide assumes some minimal capabilities in the G-code motion controller. It is best to check out this guide first, before ripping up half your configuration. 

## Optional and Step-by-Step

It is one design goal of the Advanced Motion Control feature set to be _optional _in OpenPnP and to provide a continuous experience for those who don't want to use it. Almost all the setting of a previous OpenPnP machine configuration should automatically be migrated to the new version. The machine should work the same way as before. 

Some features are still clearly visible in the GUI, such as the GUI-based [[Machine-Axes]]/[[Axis-Mapping]] setup that was formerly done by hacking the `machine.xml` file. Some features have moved to different parts of the GUI such as [[Backlash-Compensation]]. But all the really "advanced" features are initially inactive or even hidden. This guide aims to document these features so you can enable them step-by-step. 

Note: as long as Advanced Motion Control is only available in the testing version and the older OpenPnP 2.0 versions are still in use, this guide also acts as a repository for instructions that have completely changed from the previous ways. Some parts may later be incorporated into existing Wiki pages to replace obsolete instructions. 

## Migration from a previous Version 

OpenPnP should migrate all but the most exotic machine setups automatically from previous OpenPnP 2.0 versions. After the migration, follow these initial steps to prepare for the Advanced Motion Control features:

1. If you use more than four axes on one controller: check out if you can use it **without** pre-move commands (the `T` letter commands used to switch between extruder `E` axes). Smoothieware, Duet3D, [Bill's fork of Marlin 2.0](https://github.com/bilsef/Marlin/tree/Teensy4.1_PnP_6axis) and possibly others can be used with proper axis letters `A`, `B`, `C` instead. 
**Warning**: if this is not the case, only a limited number of advanced features will be available. **This guide assumes you do not have pre-move commands!** 

2. Go to each of your GcodeDrivers, enable **Letter Variables?** and disable **Pre-Move Commands?** (other settings will be explained later).

   ![GcodeDriver Migration](https://user-images.githubusercontent.com/9963310/96035272-1746d000-0e63-11eb-8ff8-94f3a0c7a67d.png)

3. The Axes are now in the GUI (formerly a proprietary part of the GcodeDriver). This guide assumes that you checked them out and read about the setup as needed for your machine. See the [[Machine-Axes]], [[Transformed-Axes]], [[Linear-Transformed-Axes]] pages and the part about [[Backlash-Compensation]].
4. Make sure you have assigned the correct **Axis Letter** to each controller axis as described in the [[Controller Settings|Machine-Axes#controller-settings]].
5. Go to each of the GcodeDrivers and create a **Default** `MOVE_TO_COMMAND` that moves **all** the axes of your controller **at once**, using the **Axis Letters** as the variable names. Add the acceleration command in front. Make sure to move any G-code letter inside the curly brackets (including the `F` letter, formerly outside). Remove any `Backlash` variables and extra commands if present. Best to start fresh:

    `{Acceleration:M204 S%.2f} G1 {X:X%.2f} {Y:Y%.2f} {Z:Z%.2f} {A:A%.2f} {B:B%.2f} {FeedRate:F%.2f} ; move to target`

6. Remove any `MOVE_TO_COMMAND`s from the other Head Mountables. They are no longer needed.

7. Create a **Default** `SET_GLOBAL_OFFSETS_COMMAND`, again for all the axes of that controller at once and using the **Axis Letters** as the variable names:

    `G92 {X:X%.4f} {Y:Y%.4f} {Z:Z%.4f} {A:A%.4f} {B:B%.4f} ; reset coordinates in the controller` 

8. Delete any `POST_VISION_HOME_COMMAND`.

9. Create a `GET_POSITION_COMMAND`. The command must report all axes (for Smoothieware, try M114.2 if it does not work and/or use [my special firmware](https://makr.zone/smoothieware-new-firmware-for-pnp/500/)).

    `M114 ; get position`

10. Create or change the `POSITION_REPORT_REGEX`, again for all the axes of that controller at once and using the **Axis Letters** as the regex group names:

    `^ok C: X:(?<X>-?\d+\.\d+) Y:(?<Y>-?\d+\.\d+) Z:(?<Z>-?\d+\.\d+) A:(?<A>-?\d+\.\d+) B:(?<B>-?\d+\.\d+).*`



### Replace existing GcodeDrivers with the GcodeAsyncDriver

1. Close OpenPnP. 
2. Open the `machine.xml` found [here](https://github.com/openpnp/openpnp/wiki/FAQ#where-are-configuration-and-log-files-located).
3. Search and replace GcodeDriver with GcodeAsyncDriver. Save.
4. Restart OpenPnP.



## GcodeDriver new and moved Settings

Many of the GcodeDriver settings have moved to new locations in the Machine Setup. 
