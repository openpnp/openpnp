Cobalt is the name of the first OpenPnP machine. It's a work in progress and this document will chronicle it's design and build.

## April 20, 2013

This is the first entry in this build log, so I will detail where things stand now and then future updates will just talk about progress.

Cobalt is the name I gave to a hardware design I have been working on for quite some time. Unfortunately, the design turned out to be flawed and difficult to machine, so I have basically abandoned it. Now I am focusing on getting *any* machine up and running for Maker Faire. What I am working on now will eventually become the new Cobalt.

The old design used linear bearings and shafts and these turned out to be a lot tricker than I thought they would be. It also used a single sided drive on the Y axis. Those two things combined made for a very inaccurate machine.

For the time being, I am using some nice linear rails I got on eBay a long time ago for Y. X will still be linear bearings since the drive will be symmetric.

Last weekend I made two bearing blocks for the Y lead screw, the Y stepper mount and the Y lead screw nut coupler. These components, plus the linear rails, lead screw, lead screw nut, various bits of hardware and a TinyG resulted in a very accurate and fast half of Y axis. 

Here's a short video showing the half Y axis test: http://www.youtube.com/watch?v=LVPcsuYy4oA

This is running at 250mm per second and in a later test I measured it's repeatability to < 0.001" with a dial test indicator.

This weekend's goals are:

* Build the base frame out of 80/20.
* Get both rails that will make up Y mounted and parallel.
* Figure out a way to couple a hunk of 80/20 to the rails to make a gantry and test if single sided drive will work. If not, order a second Y screw and cut out two more baring blocks.
* Design the X axis in SketchUp and, if there is time, start cutting the parts for it.

By the end of the weekend I'd really like to have the Y axis fully built and running if possible and at least have a start on X.
