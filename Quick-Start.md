Introduction
============

This Quick Start guide will guide you through installing OpenPnP, seeing the major components of the interface and running a sample job in the OpenPnP simulator. This will allow you to quickly understand how OpenPnP works and give you a foundation to begin hooking it up to your own machine.

Installation
============

Download
--------

Visit http://openpnp.org/downloads to find out how to download the latest snapshot or release of OpenPnP.

Install and Run
---------------

If you are using one of the binary installers from the website, just run the installer and follow the instructions. After installation you can run OpenPnP from your operating system's applications list, i.e. Start Menu, Applications folder, etc.

If you are using an archive version of OpenPnP, unzip the software into a directory of your choosing. Typically this would be the same place you keep your other applications. Inside the folder you unzipped OpenPnP to there is an `openpnp.sh` and `openpnp.bat` script. These should work for Windows, Mac and Linux. For Mac and Linux, run `openpnp.sh` and for Windows run `openpnp.bat`. After a short wait you should see the OpenPnP Main Window.

A Quick Tour
============

OpenPnP comes set up so that you can use it right away to see how it works. You don't even a need a machine connected.

When you start OpenPnP for the first time you will see a simulated pick and place table in the camera view. Try following along with the items below to get a feel for how OpenPnP works:

1. Press the big green START button to start the virtual "machine".
2. Press Ctrl-Tab to open the jog controls window. You can use the buttons in the window to move the camera around. You can change the distance each click moves by changing the value of the slider above it.
3. Visit each of the tabs along the bottom of the window to see how Jobs, Parts, Packages, Feeders and the Machine is configured. Right now it's best not to change anything.

Your First Job
==============

Now that you've seen the user interface a bit, it's time to try running a pick and place job. Follow along with the instructions below:

1. Select the Job tab at the bottom of the main OpenPnP window.
2. From the File menu, select Open Job.
3. Using your computer's file dialog, find the `samples` directory that came with OpenPnP. It should be in the same directory you installed OpenPnP into.
4. In the `samples` directory, find the `pnp-test` directory and open the `pnp-test.job.xml` file inside it.
5. You'll see the job has loaded and there are now boards and placements listed. You can browse the boards and placements to see what the job will be doing.
6. If you haven't already, press the big green START button to start the machine.
6. Press the ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/control-start.svg) button to start the job and the camera will start moving.

OpenPnP will now simulate a full pick and place job. It will use computer vision to align the boards using fiducials, find parts in virtual feeders and place the parts on virtual boards. You can follow along by watching the camera view.

When the job is complete, congratulations! You've run a job in OpenPnP! The next step is to dive into the [[User Manual]] and start learning how to hook OpenPnP up to a real machine.

