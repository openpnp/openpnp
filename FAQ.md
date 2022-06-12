* [How do I let OpenPnP help me?](#how-do-i-let-openpnp-help-me)
* [How do I get help?](#how-do-i-get-help)
* [Where are configuration and log files located?](#where-are-configuration-and-log-files-located)
* [How can I go back to an old/working configuration?](#how-can-i-go-back-to-an-oldworking-configuration)
* [How do I reset my configuration?](#how-do-i-reset-my-configuration)
* [How do I start OpenPnP with a JAR File?](#how-do-i-start-openpnp-with-a-jar-file)
* [How do I use a different config directory](#how-do-i-use-a-different-config-directory)
* [How do I turn on debug logging?](#how-do-i-turn-on-debug-logging)
* [What are the newest Features and Bugfixes in OpenPnP?](#what-are-the-newest-features-and-bugfixes-in-openpnp)
* [How do I troubleshoot GcodeDriver?](#how-do-i-troubleshoot-gcodedriver)
* [I'm having trouble connecting multiple USB cameras.](#im-having-trouble-connecting-multiple-usb-cameras)
* [I need help configuring GcodeDriver.](#i-need-help-configuring-gcodedriver)
* [My nozzle is not turning or going up/down when I jog C or Z](#my-nozzle-is-not-turning-or-going-updown-when-i-jog-c-or-z)
* [My nozzle moves (in Z) when the camera is moved.](#my-nozzle-moves-in-z-when-the-camera-is-moved)
* ["It would be faster to do it by hand."](#it-would-be-faster-to-do-it-by-hand)
* [What Should I Build?](#what-should-i-build)
* [How can I improve placement accuracy?](#how-can-i-improve-placement-accuracy)
* [OpenPnP is not working in Windows 7](#openpnp-is-not-working-in-windows-7)
* [What can cause apparent X-Y movement backlash type errors?](#What-can-cause-apparent-X-Y-movement-backlash-type-errors)

## How do I let OpenPnP help me?

Check out the [[Issues and Solutions]] system. It can often diagnose issues and sometimes even suggest solutions. Issues are also most of the times linked to the relevant pages in this Wiki. 

## How do I get help?

Check out the [[Getting Help]] page for information on getting help and supplying good information.

## Where are configuration and log files located?

The configuration and log files are located in a subdirectory of your user's home directory called `.openpnp` (Version 1) or `.openpnp2` (Version 2).

To find your home directory, check out this Wikipedia article that explains where the home directory lives on various operating systems: http://en.wikipedia.org/wiki/Home_directory

Examples:

```
Windows: C:\Users\your_username\.openpnp2
Linux: /home/your_username/.openpnp2
Mac OS X: /Users/your_username/.openpnp2
```

Configuration files are `machine.xml`, `parts.xml` `packages.xml` and `vision-settings.xml` along with other plugin specific files.

Log files are under the `log` subdirectory and the current file is always called `OpenPnP.log`.

If you double-click your camera view, a snapshot gets written to the `snapshots` subfolder.

Some Computer Vision pipelines also write debug images into various subfolders here.

## How can I go back to an old/working configuration?

OpenPnP creates a backup of your configuration, each time a new one is saved, which also happens automatically, each time you exit OpenPnP. 

The backups are located in the `backup` sub-directory, see [Where are configuration and log files located?](#where-are-configuration-and-log-files-located).

## How do I reset my configuration?

Sometimes it's easiest just to completely reset your configuration and start over. To do that, just delete the whole OpenPnP configuration directory. See [Where are configuration and log files located?](#where-are-configuration-and-log-files-located) for it's location.

## How do I start OpenPnP with a JAR File?

There might be situations where you want to start an OpenPnP .jar file directly, use the following command-line:

```
java -jar target/openpnp-gui-0.0.1-alpha-SNAPSHOT.jar
```

## How do I use a different config directory

It's possible to use command line argument for selecting the config directory, example below:

```
java -DconfigDir=src/main/resources/config -jar target/openpnp-gui-0.0.1-alpha-SNAPSHOT.jar
```

## How do I turn on debug logging?

1. Select the Log tab from the main window.
2. Locate the Log Level dropdown.
3. Click the dropdown and select Debug or Trace, depending on how much output you want.

Note: Debug is good for general debugging. Trace is good for when you want very low level information such as Gcode commands being sent to the controller.

## What are the newest Features and Bugfixes in OpenPnP?

For the **testing** branch use this link:

https://github.com/openpnp/openpnp/pulls?q=is%3Apr+is%3Amerged+base%3Atest+

Note, these are sorted by opening date, not by merge date, so the sequence might sometimes not reflect the order of adoption. On larger changes, the Description and subsequent discussions might sometimes be a bit "evolving". Look for links to Wiki pages that describe the final instruction for use (they are sometimes at the end of the discussion).

A high level summary is in the Change Log file which you can access from the Help menu, but it is usually not up to date in the testing version, i.e. it is only finalized for the develop version.

After an upgrade, always have a look at [[Issues and Solutions]]. Sometimes new issues/solutions will pop up, hinting at new features.

## How do I troubleshoot GcodeDriver?

See https://github.com/openpnp/openpnp/wiki/GcodeDriver#troubleshooting

## I'm having trouble connecting multiple USB cameras.

See https://github.com/openpnp/openpnp/wiki/USB-Camera-Troubleshooting-FAQ

## I need help configuring GcodeDriver.

See https://github.com/openpnp/openpnp/wiki/GcodeDriver#asking-for-help

## My nozzle is not turning or going up/down when I jog C or Z

Make sure you have the nozzle as the selected tool in the machine controls:

![selected-tool](https://user-images.githubusercontent.com/9963310/173219345-afbab8fd-323f-4c01-a1ac-122bd61a3097.gif)

Often, the camera is selected, especially if you use the [Auto tool select option](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration:-Machine-Setup#the-machine-setup-tree) on the machine (as is recommended) and then performed an explicit camera operation. So you need to select the wanted nozzle, before you can jog C or Z.

The camera has its own _virtual C and Z axes_, i.e. they move "invisibly" when you jog them. Read [here](https://github.com/openpnp/openpnp/wiki/Machine-Axes#use-case--example), why this is useful.

If a problem persists, also read the next section.

## My nozzle moves (in Z) when the camera is moved.

For newer version of OpenPnP, just use [[Issues and Solutions]]. It will point you to the mistake. 

Most likely you have the _real_ Z and/or C axis set on the camera, instead of [virtual axes](https://github.com/openpnp/openpnp/wiki/Machine-Axes#use-case--example).

For very old versions of OpenPnP look here:
See https://github.com/openpnp/openpnp/wiki/GcodeDriver#nozzle-is-moving-or-turning-when-camera-is-moved

## "It would be faster to do it by hand."

This comes up so often on the mailing list I thought it would be worth addressing as a FAQ. Often, when someone is talking about building a machine or discussing requirements, someone will comment with things along the lines of:

* "Running a pick and place is more work than you think."
* "For small runs it's faster just to do it by hand."
* "You will spend more time setting it up than using it."
* "It will cost you more in the end than just sending the work out, hiring someone, etc."

While some of these reasons might fit your situation, it is important to keep in mind that everyone has their own reasons for wanting to build a pick and place, and very often those reasons have nothing to do with time or money. Here are some other good reasons to build a pick and place machine, even if it's comparatively slow or costs a lot of money:

* Inability to place by hand due to age, illness, disability or disease.
* Live in a place where it's cost prohibitive or just impossible to send work out.
* Want to learn more about CNC, computer vision, pick and place, electronics assembly, etc.
* Just doing it for fun!
* Strange or complex jobs that contract assembly houses don't want to deal with.
* Want to try a new or novel idea for placing components.
* Any other reason you like!

It's okay to let people know about the complexities of running a machine, but I've found that this topic tends to come up over and over again and people seem to feel the need to really pound the point home that it might cost money or time to run the machine. Make your point, but remember that there are lots of reasons to run a machine other than just building boards for a business.

## What Should I Build?

This answer has been moved to the [[Build FAQ]].

## How can I improve placement accuracy?

Getting reliable, fast and accurate results is the ultimate goal but even for sophisticated machines quite a challenge to achieve all the time.

This guide tries to give an overview of the things to consider:
https://github.com/openpnp/openpnp/wiki/Troubleshooting-Placement-Accuracy

## OpenPnP is not working in Windows 7

Windows 7 is missing some libraries that OpenPnP relies on. If you can upgrade to Windows 10, that is the quickest solution. If that is not an option see [this Github comment](https://github.com/opencv/opencv/issues/12010#issuecomment-420640169) about a possible solution, and [this discussion thread](https://groups.google.com/d/msgid/openpnp/f14a436d-5b3e-4e21-b2cf-dee1f8222cd6%40googlegroups.com) for more information.

---
Table of Contents Created by [gh-md-toc](https://github.com/ekalinin/github-markdown-toc)
