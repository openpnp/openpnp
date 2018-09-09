- [Introduction](#introduction)
- [The Basics](#the-basics)
- [The User Interface](#the-user-interface)
- [Job Setup](#job-setup)
- [Running a Job](#running-a-job)
- [Machine Setup](#machine-setup)
- [Frequently Asked Questions](#frequently-asked-questions)
- [Advanced Topics](#advanced-topics)
- [Getting Help](#getting-help)

# Introduction

OpenPnP is an Open Source SMT pick and place system designed and built with the hobbyist in mind but with the features and power to run commercial pick and place operations. Its goal is to bring pick and place to the desktop of anyone who needs to make more than a few of something.

OpenPnP is made up of three components. The hardware, the firmware and the software. This User Manual focuses only on the software. To get information about the hardware and firmware please visit http://openpnp.org.

The purpose of this manual is to help you get the software up and running, and to teach you how to configure and operate it.

## Getting Started

If you are brand new to OpenPnP have a look at the [[Quick Start]] guide to quickly get up and running and comfortable with the software. Once you are done there it will guide you back here to get into the details.

## Setup and Calibration

To setup OpenPnP with your own machine, see the [[Setup and Calibration]] guide. If you don't have a machine yet you can still work through this manual using OpenPnP in simulator mode. OpenPnP starts this way by default.

# The Basics

At it's core, OpenPnP is a Computer Numerical Control (CNC) controller. It reads a job file and then sends commands to a machine to execute. Unlike common CNC controllers for 3D printers, milling machines, and lathes, OpenPnP also uses cameras for feedback, and allows you to completely configure a job within it's user interface.

The definitions and explanations below will introduce you to the terminology used in OpenPnP, and in pick and place in general.

## Coordinate System

OpenPnP uses the right handed coordinate system which is also used in physics, math, 3D graphics and many CAD packages.

For a more formal definition of this coordinate system see the [Wikipedia Page](http://en.wikipedia.org/wiki/Right-hand_rule).

In this coordinate system we are standing above the machine, looking down at it.

The X axis moves right and left. Right is positive.
The Y axis moves forward and back. Forward is positive.
The Z axis moves up and down. Up is positive.
The C, or rotation, axis rotates clockwise and counter-clockwise. Counter-clockwise is positive.

![screen shot 2016-06-18 at 12 56 07 pm](https://cloud.githubusercontent.com/assets/1182323/16173361/0e54935c-3554-11e6-9cf6-caf13e6d4a65.png)

The units for the X, Y and Z axes are set in the GUI. The default is Millimeters.  The units for the C axis is degrees. OpenPnP measures rotation in degrees and treats them like Millimeters from the perspective of the controller.

## Important Definitions

- **Board**: A board is a physical version of a PCB. Every new version of a PCB is a new board. A board contains placements that tell OpenPnP where to place parts.
- **Fiducial**: A fidicual, or fiducial mark, is a small mark on a PCB that helps the machine locate the PCB automatically with great accuracy. Fiducials are typically small round pads with a large keepout area and no solder paste applied.
- **Part**: A part is a specific component for placement on a board. They are often synonymous with a manufacturer part number. Two parts with different values are different parts. For example, a 10k 0603 resistor is a different part than a 22k 0603 resistor. Every part is also assigned a package.
- **Package**: A package describes the part's physical attributes such as it's length and width, and it's footprint. Many parts have the same package. Some examples of packages are 0603 resistor, 0603 capacitor, SOIC-8, TQFP-32, etc.
- **Placement**: A placement is a location on the PCB where a part should be placed. These are usually the same as the X and Y coordinates where you placed parts when designing your PCB. Every placement has an X and Y coordinate relative to the board's origin, and a part assignment that tells OpenPnP which part goes on that placement.
- **Job**: A job is a a file that contains a list of boards for the machine to process in a single run. A job can contain any mixture of any number of boards, including multiples of the same board.
- **Footprint**: A footprint is a definition of the numbers and shapes of the pads on the part. Footprints are not currently used in OpenPnP.
- **Reticle**: The reticle is a crosshair, or other shape, overlayed on the camera window that helps you see the center of the image. Reticles can also display rulers, or arbitrary shapes in any physical size.
- **Driver**: The driver is the part of OpenPnP that converts OpenPnP's commands into commands the machine can understand. Many machines speak Gcode, and that is the most commonly used driver in OpenPnP. Using different drivers, OpenPnP can talk to a wide variety of different types of machines, whether the use Gcode, ASCII, or a proprietary protocol.
- **Bottom Vision**: Bottom vision refers to both an upwards facing camera and the process of using computer vision to automatically inspect parts before placing them. This inspection allows OpenPnP to place the part much more accurately than just going directly from feeder to placement.
- **Top Vision**: Top vision is the downward facing camera that is typically mounted to the head. It is used to identify fiducials and for computer vision on vision assisted feeders.
- **Feeder**: A feeder can be both a piece of hardware that can supply parts, and a feature in OpenPnP that allows it to pick parts from non-hardware sources. OpenPnP supports feeding from automatic feeders, cut tape strips, "drag" style feeders, tubes, trays, and bins of loose parts.
- **Head**: The head is the part of the machine that can move in both X and Y. It carries the nozzles, and often the top vision camera.
- **Nozzle**: The nozzle is the part of the machine that can lower down to pick up parts. The thing that actually touches the part to pick it up is a nozzle tip, and this is a common source of confusion. The nozzle carries the nozzle tip, and the nozzle tip touches the part. A machine can have any number of nozzles.
- **Nozzle Tip**: The nozzle tip is the part of the machine that actually touches a part to pick it up. A nozzle tip is mounted to a nozzle and there can be any number of nozzle tips assigned to a nozzle. Nozzle tips are generally chosen by size of the part they are designed to pick up. OpenPnP can also automatically change nozzle tips to pick up different parts.

# The User Interface

OpenPnP has a single window interface, broken up into multiple sections. Those sections are explained in detail below.

## The Main Window

OpenPnP's Main Window is laid out in sections. The main sections are the Machine Controls, Digital Read Outs (DROs), the Camera Panel, Tabbed Interface.

![screen shot 2017-04-14 at 6 15 55 pm](https://cloud.githubusercontent.com/assets/1182323/25058445/1c1ce4f8-213f-11e7-86f7-f30aea7d7425.png)

## Machine Controls

![screen shot 2017-01-09 at 7 59 34 pm](https://cloud.githubusercontent.com/assets/1182323/21791132/78884716-d6a6-11e6-8579-fe2021c927b5.png)

The Machine Controls are your interface to interacting with the machine. From here you can:

- Turn the machine on and off with the power button <img src="https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/power_button_off.svg" height="18">.
- Select the nozzle that you'd like to use for setup operations.
- Jog the currently selected nozzle with the jog buttons.
- Use the Distance slider to change how far the machine moves with each jog.
- Use the Speed slider to change how fast movements happen.
- Use the Special, Actuators and Dispense tabs to trigger more advanced actions.
- Press the Park buttons <img src="https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/park.svg" height="18"> to move either the X/Y axes, the Z axis or the C axis to the Park location defined on your head.
- Press the Home button <img src="https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/home.svg" height="18"> to perform a homing operation. You should do this each time you start your machine.

## Digital Read Outs (DROs)

![screen shot 2017-01-09 at 8 03 00 pm](https://cloud.githubusercontent.com/assets/1182323/21791175/b900e7da-d6a6-11e6-8979-17308c278d51.png)

The DROs show the current position of the selected nozzle in your preferred units. You can click the DROs to set them to relative mode which will zero them out and turn them blue. You can use this to measure distances; the DROs will show the distance from where you first clicked them. Click again to go back to normal mode.

You may notice that the coordinates shown are not the same as the coordinates sent to your controller. This is due to [Head Offsets](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Nozzle-Setup#head-offsets) which tell OpenPnP where the items on the head are in relation to each other. The coordinates shown in the DROs are the machine's coordinates plus the head offsets for the selected nozzle.

## The Camera Panel

![screen shot 2016-06-18 at 1 23 19 pm](https://cloud.githubusercontent.com/assets/1182323/16173504/dec01e3c-3557-11e6-9132-fa04ffcd0e4f.png)

The Camera Panel is where images from your cameras will show. You can select the currently visible camera from the dropdown, or select to show All Cameras or None. When multiple cameras are shown they are broken up into multiple Camera Views.

You can right click in each Camera View to set options specific to that camera. Most importantly, you can choose your Reticle here. The Reticle is an overlay on the camera view to help with targeting. You should start with the Crosshair Reticle and try out some of the other options to see what you prefer.

You can also hold down Shift and click the Left Mouse Button in a Camera View to move the nozzle to that location. This is called Camera Jogging. Camera jogging makes it easy to "drag" the camera to where you want it.

## The Tabs

![screen shot 2017-01-09 at 8 15 26 pm](https://cloud.githubusercontent.com/assets/1182323/21791442/6253b302-d6a8-11e6-806b-222fec908b91.png)

The tabs at the right of the window are where all job operations, job setup, and configuration take place. The tabs are covered in more detail in the sections below, but here is a brief overview:

- **Job**: Job setup and control.
- **Parts**: Create new parts, setup bottom vision, pick parts for testing.
- **Packages**: Create packages, setup footprints. Important for [[Fiducials]].
- **Feeders**: Setup feeders, specify parts to feed.
- **Machine Setup**: Configure every aspect of the machine's hardware and setup.
- **Log**: Shows log output from the system and lets you choose what level of detail to show.

## Location Buttons

![screen shot 2016-06-18 at 4 46 29 pm](https://cloud.githubusercontent.com/assets/1182323/16174378/69ab388a-3574-11e6-96b4-8419db5f3584.png)

The Location Buttons are used in many places throughout OpenPnP. It's good to get familiar with these buttons and their functions as you will see them everywhere.

From the left, the buttons are:

- Capture Camera Location ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/capture-camera.svg): Used to capture the location the camera is currently looking at. Will typically be used to fill in the X, Y, Z and Rotation fields of an associated value.
- Capture Nozzle Location ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/capture-nozzle.svg): Same the above, but captures the nozzle location.
- Move Camera to Location ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/position-camera.svg): Move the camera to a given location. Usually to the location identified by associated fields.
- Move Nozzle to Location ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/position-nozzle.svg): Save as above, but moves the nozzle.

You'll often see these buttons grouped together with a set of fields, like this:

![screen shot 2016-06-18 at 4 52 04 pm](https://cloud.githubusercontent.com/assets/1182323/16174397/0395145c-3575-11e6-8566-dcecb5754606.png)

The buttons control the fields and respond to the values in them. You'll also see the buttons used individually where applicable, like in the Job tab. Here the buttons are used in context with the selected board to capture the board location or move to it.

Finally, note the color and the icon of the buttons. These colors and icons are used throughout the system to mean the same thing. Red icons are a warning - clicking it may make something bad happen. In this case it will make the machine move, so make sure you are clear of it. Blue icons mean capture - blue is used to indicate that clicking this button will capture a value into a field.

## Keyboard Shortcuts

You can do everything in OpenPnP with the mouse, but knowing keyboard shortcuts can help speed things up. The most common ones are:

- **Ctrl+H**: Home the machine.
- **Ctrl+Arrow Key**: Jog the currently selected Nozzle in X and Y. Up and Down arrows jog in Y and Left and Right arrows jog in X.
- **Ctrl+/**, **Ctrl+'**: Jog the currently selected Nozzle down and up in Z.
- **Ctrl+<**, **Ctrl+>**: Rotate the currently selected Nozzle counter-clockwise and clockwise.
- **Ctrl+Plus**, **Ctrl+Minus**: Change the jog distance slider. This changes how far each jog key will move the Nozzle.
- **Shift+Left Mouse Click**: Hold Shift and left click the mouse anywhere in the camera view to move the camera to that position.
- **Ctrl-Shift-R**: Starts a job
- **Ctrl-Shift-S**: Steps through the job
- **Ctrl-Shift-A**: Stops the job
- **Ctrl-Shift-P**: Parks head (Z retract then XY park)
- **Ctrl-Shift-L**: Parks Z axis only
- **Ctrl-Shift-Z**: Moves head to safe Z
- **Ctrl-Shift-D**: Discard component
- **Ctrl-Shift-F1**: 0.01mm / 0.001" jog increment
- **Ctrl-Shift-F2**: 0.1mm / 0.01" jog increment
- **Ctrl-Shift-F3**: 1mm / 0.1" jog increment
- **Ctrl-Shift-F4**: 10mm / 1" jog increment
- **Ctrl-Shift-F5**: 100mm / 10" jog increment

The below shortcuts are bound to your system command modifier key. On Windows and Linux this is Ctrl, and on Mac it's Cmd:

- **Modifier+O**: Open a Job.
- **Modifier+N**: New Job.
- **Modifier+S**: Save Job.
- **Modifier+E**: Enable/Disable Machine (Power Button).
- **Modifier+`**: Home.

## Tooltips

OpenPnP makes extensive use of tooltips for it's help system. You can hover over most buttons to get a tooltip that explains what the button does. The tooltip will appear in a small yellow box.

![screen shot 2018-07-24 at 8 36 53 pm](https://user-images.githubusercontent.com/1182323/43174752-64e50524-8f81-11e8-8715-70bfdd965fe6.png)

# Job Setup

A job consists of one or more boards, along with locations and information about the boards. A job may contain any number of boards, and can include multiples of the same board or multiple different boards. 

Each board entry in a job tells OpenPnP where to find one particular board using machine coordinates. When you run the Job OpenPnP will process all of the placements for each board in the Job.

You can easily set up panels of PCBs using the [[Panelization]] feature without having to add each board to the job individually.

Jobs are stored in files with the extension `.job.xml`.

When setting up a job, you'll need to configure Boards, Placements, Parts, Packages, and Feeders. These are all introduced in the sections below.

## Boards

Boards tell OpenPnP which parts to place and where to place them. Boards are stored in files with the extension `.board.xml`. A board contains a list of placements. A placement tells OpenPnP which part to place at what coordinates and rotation.

Board files are independent from any user or machine. You can share board files for a given PCB design and use the file to build that particular PCB.

You will typically create a new board file by [importing data from your CAD software](https://github.com/openpnp/openpnp/wiki/Importing-Centroid-Data) such as Eagle or KiCAD. Once you've created a board file for a design there's no need to change it unless the design changes.

Board locations represent the 0, 0, 0 (X, Y, Z) origin of the top of the PCB. This tells the machine where to find 0, 0, 0 on the board and it performs the math needed to find the individual placements from there. Part height is added when placing a part so that the nozzle tip stops at the right height above the board.

![screen shot 2018-07-24 at 8 38 21 pm](https://user-images.githubusercontent.com/1182323/43174782-8c242f16-8f81-11e8-8551-7f0a6e9b9c94.png)



You can add a new or existing board to the job by clicking the ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/general-add.svg) button in the Boards section.

The columns in the table are:

- **Board**: The name of the board at this location.
- **Width**: The width of the bare PCB. This is optional, but it's helpful to set it if you will be placing the bottom of the board in addition to the top.
- **Length**: The length, or height, of the bare PCB. Not currently used in OpenPnP.
- **Side**: The side of the board you want to place in this job. Selecting the side tells OpenPnP which placements to place during the job run.
- **X, Y, Z, Rot**: The X, Y, Z, and Rotation coordinates of the board in relation to the origin of the machine. This can be a complex topic. See [[Understanding Board Locations]] for all the details.
- **Enabled?**: Only boards that are enabled will be processed in the job. You can uncheck a board to skip it for a job run.
- **Check Fids?**: If this is checked OpenPnP will run a fiducial check on the board before processing it, and it will use the coordinates it finds to better locate the board.

## Placements

A placement is a single position on the board where you want to place a part. Placements usually correspond to the reference designators you created when you designed your PCB. Placements will often have names like R1, C1, U1, etc.

Each placement has an X, and Y coordinate, a rotation in degrees, information about the part that should be placed and additional information specific to that placement.

![screen shot 2018-07-24 at 8 43 37 pm](https://user-images.githubusercontent.com/1182323/43174940-44b7c100-8f82-11e8-8ccc-ce26888b2dd2.png)



Placements are usually set up by importing a PCB CAD file, but you can also set them up manually by clicking the ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/general-add.svg) button to add one and then double clicking the fields in the table to edit them.

When you select a part for a placement, you are telling OpenPnP that you want that part to be placed at that position. OpenPnP will find a feeder to pick the part automatically.

The columns in the table are:

-  **ID**: The reference designator for the placement. These are usually the same as the reference designators you set when designing the PCB.
- **Part**: The part that should be placed at this location.
- **Side**: The side of the PCB the placement is on.
- **X, Y, Rot**: The  X, Y, and Rotation coordinates of the placement in relation to the board's origin. These will usually match the coordinates in your PCB software.
- **Type**: Sets the type of placement from one of Place, Ignore, or Fiducial. You can use Ignore if you don't ever want to place this placement for this board. This is often referred to DNU (Do Not Use) or DNI (Do Not Install). You can select Fiducial if the placement specifies a Fiducial mark, or Place if it's a normal placement.
- **Placed**: Indicates that the placement has already been placed. When a job finishes, all the enabled placements will be marked Placed. You can use this checkbox for fine grained control of which placements OpenPnP will place during a run.
- **Status**: Shows any errors associated with the placement to help you know of a job is ready to run. Common errors are things like a invalid part height, or a missing feeder.
- **Check Fids**: Placement Check Fids is an advanced feature that will cause OpenPnP to re-check fiducials before placing that placement. It can help improve accuracy if your machine tends to lose accuracy over the course of a job.

## Parts

Everything in OpenPnP eventually makes it way back to a part. A part is simply a record that tells OpenPnP about a unique part that you want to place on a board. In general, a part should refer to a specific manufacturer part number but not to a packaging type. For instance, a 10k 0603 5% resistor and a 10k 0603 1% resistor are two different parts, but a 10k 0603 5% resistor in cut tape, and a a 10k 0603 5% resistor in a reel are the same part. It's easiest to think of parts as the manufacturer part number you specify in your BOM.

Parts are picked from feeders, and placed on placements. The information in the parts tab is used to tell OpenPnP about the physical properties of the part, which OpenPnP uses to accurately place the part.

![screen shot 2018-07-24 at 9 07 24 pm](https://user-images.githubusercontent.com/1182323/43175613-9f18a012-8f85-11e8-931d-37dc21ea2589.png)



The fields in the table are:

- **ID**: A unique ID for the part. It's usually best to use the manufacturer part number here.
- **Description**: A user defined description of the part. You can use this field for informational purposes, or for your own identifiers.
- **Height**: The physical height of the part as measured with calipers, or found in a data sheet. Part height is important because it determines how high above the board OpenPnP will stop when placing the part.  You should only need to measure the part height for a specific part once, and then you can refer to the part in as many boards as you like.
- **Package**: The package type of the part. Common packages are things like R0603, C0402, TQFP-32, QFN-48, etc. By setting a package you tell OpenPnP more about the part and this information can be used for computer vision tasks. Packages do not have a height, since many parts share the same package but have different heights. Package is also used to tell OpenPnP the shape of a fiducial for fiducial recognition. See [[Fiducials]] for more information.
- **Speed %**: By default all parts are placed at 100% of the speed of the machine. You can lower that number here if you find that certain parts slip while being moved. In general, heavier parts may need slower speeds.

## Packages

Packages tell OpenPnP more information about a part's physical properties. You can specify how many and what shape of pads or pins the part has, and information about the size of it's body.

![screen shot 2018-07-24 at 9 13 38 pm](https://user-images.githubusercontent.com/1182323/43175863-c73f2592-8f86-11e8-9528-2ac46a83d059.png)

Packages are not currently used very much in OpenPnP, except for in fiducials. You can ignore them for the most part. For information about setting up fiducial packages see [[Fiducials]].

## Feeders

Feeders are where OpenPnP goes to find parts to pick. OpenPnP supports many different types of feeders, and each type can be configured in many different ways. 

Most machines will have one or two types of feeders installed, and may have a number of "virtual" feeders, which can be as simple a piece of cut strip taped to the bed of the machine. Some of the most common feeder types are:

- Drag Feeders: A simple feeder that holes SMT tape. The head of the machine is used to drag the tape forward to expost the next part.
- Strip Feeders: A "virtual" feeder that uses computer vision to find parts in a piece of cut tape mounted to the machine. This is the easiest feeder to get started with. All you need to do is use double sided tape to stick a strip of parts down.
- Tray Feeders: A "virtual" feeder that picks parts from a uniform array of parts. Many larger parts come in JEDEC matrix trays and OpenPnP can easily cycle through all the parts in the tray.
- Auto Feeders: An auto feeder is a mechanism that OpenPnP can command to feed a part. These types of feeders are typically used on commercial machines to feed parts very quickly and accurately.

You can see all the different types of feeders by clicking the ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/general-add.svg) button in the feeders tab.

![screen shot 2018-07-24 at 9 22 06 pm](https://user-images.githubusercontent.com/1182323/43176035-a3c2f264-8f87-11e8-9d07-afd02c805114.png)

Most feeders have a location that you configure. The location is where you want OpenPnP to go to pick the part. Feeder locations are simple locations.  In general no math is applied. The location you set is where the nozzle will go to pick.  The part height is not used here.

![screen shot 2018-07-24 at 9 23 09 pm](https://user-images.githubusercontent.com/1182323/43176099-e77cd0ce-8f87-11e8-9ad4-a3bf0e552891.png)



When you add a feeder you need to set the part that is installed in it. You can have multiple feeders feeding the same part. OpenPnP will use one until it's empty and then move on to the next one.

In general, you will need as many feeders as there are unique parts on your board. For instance, if your board has a 10k 0603 5% resistor, and an 0603 red LED you will need two feeders - one for each part. Note, though, that don't need a feeder for every placement. Even if your board have 100 0603 red LEDs, you just need to set up the feeder for that part once.

The fields in the feeder table are:

- **Name**: An informative name you assign to the feeder. For example, if you use drag feeders you might call one feeder "Drag Slot 1".
- **Type**: The type of the feeder which you select when you add it.
- **Part**: The part that this feeder feeds. You can easily select a new part if, for instance, you remove one reel from a feeder and install a different one.
- **Enabled**: Whether the feeder is enabled. A disabled feeder will not be considered when OpenPnP is looking for parts.

Feeders are a big topic all on their own. For more details, see the [Feeders section of the Setup and Calibration Guide](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Feeders).

## Preparing a Job

Now that we understand all the pieces of a job, let's put them all together. This is a basic outline of what you'll do when setting up a new job. Some of these points are things you'll do every time, but many will be the same across multiple runs of the same job, or even different jobs.

1. Select the Job tab and create a new Job by selecting Main Menu -> File -> New Job. The Boards and Pick and Place tables will clear giving you a clean slate to work on.

2. Click the Add Board ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/general-add.svg) button to add either a new board or an existing one. The most common option here is to add a new board and then import data for it.

3. Select Main Menu -> File -> Import Board and select the type of board definition you want to import. You can learn more about importing in [[Importing Centroid Data]].

4. OpenPnP will import the board, optionally creating part and package definitions for you. You can edit these definitions later if you need to specify more information.

5. Look in the placements table and check the status of the placements that were imported. Change the type of placements that are used for fiducials, or that should be ignored. The status field will tell you more information about what else you might need to setup  before you can run the job.

6. Go to the parts tab and set part heights for any parts that are missing their height value. You only need to do this once per part.

7. Configure feeders for each part in the job. An easy way to do this is to click a placement and then click the feeder edit button ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/feeder-edit.svg). OpenPnP will either open the feeder for you, or prompt you to create a new one.

8. Set the position of the board in the Job tab. You can use capture camera ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/capture-camera.svg) to align it to the corner, use fiducial locate ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/board-fiducial-locate.svg) to find it automatically or use the two placement manual process ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/board-two-placement-locate.svg).

   For additional information on using fiducials, see [[Fiducials]].

9. You'll need to set the Z position of the board, too. Touch the nozzle tip to the board and use capture nozzle ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/capture-nozzle.svg) to set it.

Those are the basic steps to setting up a job. Each person will find a workflow that works best for them. Some people prefer to import all their parts up front and then setup the job, and others will just import the job and touch up what is needed.

Once all your placements are showing Ready in the status field, it's time to start placing!

# Running a Job

You run jobs from the Job tab. The Start![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/control-start.svg), Pause ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/control-pause.svg), Stop ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/control-stop.svg), and Step ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/control-next.svg) buttons let you control the job as it progresses. Pressing Start will start the job and if there are no errors the machine will run the job to completion and then stop automatically.

## Monitoring

As the job progresses, you can see how it's going by looking at the status bar at the bottom of the main window. 

![screen shot 2018-07-24 at 10 07 59 pm](https://user-images.githubusercontent.com/1182323/43177466-0e186cec-8f8e-11e8-8ef1-f5e4a26341e8.png)

The status bar show you how many total placements there are in the whole job, and for the selected board, and how many have been placed. The progress bar gives you an indication of the total progress of the job. To the left a text field shows information about what OpenPnP is doing at any given moment, and will give information about any errors that come up.

## Handling Errors

When a job is running and something goes wrong, OpenPnP will try to recover from the problem if it can. In many cases, though, OpenPnP will stop and ask you how to proceed. You'll see an error message like this:

![screen shot 2018-07-24 at 10 11 20 pm](https://user-images.githubusercontent.com/1182323/43177594-a0c6f504-8f8e-11e8-8246-25cca0b7f977.png)

Based on the error you can decide how you want to proceed:

- **Pause Job**: The job pauses, the error message goes away, and you can make changes in OpenPnP to try to fix the problem.
- **Ignore and Continue**: Ignore the problem and try to go to the next step.
- **Skip**: Skip the rest of the processing for this placement and move on to the next one.
- **Try Again**: Try the operation that failed again.

If you choose to pause the job, once you've solved the problem you can hit Start again to continue from where you left off.

# Machine Setup

The Machine Setup tab is both the most complex part of OpenPnP, and the most simple. Here you configure all the components that make up the machine. This configuration is what makes OpenPnP work with your specific machine.

![screen shot 2018-07-24 at 9 40 30 pm](https://user-images.githubusercontent.com/1182323/43176687-6d9f73da-8f8a-11e8-99c6-3164c2206074.png)



The Machine Setup tab is a tree control with the root of the tree being the machine itself, and the branches and leaves of the tree being different things attached to the machine. You can setup cameras, feeders, nozzles, nozzle tips, job processing settings, and most importantly, the driver that lets OpenPnP talk to your machine controller.

As you click on different parts of the tree, the buttons under the tab will change to show options for that component. For instance, if you expand Heads and select Nozzles you can click the buttons above to add and remove nozzles. Clicking a nozzle let's you add nozzle tips to it, and so on.

The [[Setup and Calibration]] Guide is the best reference to the Machine Setup tab. It will guide you through setting up your own machine, but you should also consider clicking through the objects in the tree to get a feel for what options are present for each.

# Frequently Asked Questions

## Understanding Z

A common source of confusion is how OpenPnP uses the various Z values and Part heights throughout the system. In simplest terms:

- Parts are picked from feeders at the Z value specified on the feeder. Every feeder type has a Z value you can set, and this is where the nozzle will be lowered to before picking.
- Every board in a job has a location which includes a Z value. The Z value is the top of the board.
- Every part has an associated height value. The height value is added to the board's Z value when placing.

# Advanced Topics

## Scripting

See [[Scripting]] for information about OpenPnP's built in scripting engine, which can be used to add new functionality to OpenPnP without changing the source code.

## Configuration Files

Configuration files are located in your home directory, under a subdirectory called `.openpnp`.

- On Mac this will typically be `/Users/[username]/.openpnp`.
- On Windows 2000, XP and 2003 it will be `C:\Documents and Settings\[username]\.openpnp`.
- On Windows Vista and above it's `C:\Users\[username]\.openpnp`.

Configuration files are in XML format and can be edited by hand in a text editor. You should shutdown OpenPnP before editing files by hand as OpenPnP will rewrite the configuration files on exit.

There are three primary configuration files. They are:

1. `machine.xml`: Contains the primary configuration for the entire system, including information about the machine, cameras, feeders, nozzles, etc.
2. `parts.xml`: A portable parts database. As you define parts (components) in OpenPnP they are stored here.
3. `packages.xml`: A portable packages database. Component package information including shape and dimensions are stored here.

## Custom Implementations and Integration

If you are interested in having OpenPnP work with a machine that is not currently supported
you will need an OpenPnP driver that can talk to your hardware and you will need to
configure it in the `machine.xml`.

To get started, look at the list of drivers in the package below to see what drivers are
available and determine if one will meet your needs.

https://github.com/openpnp/openpnp/tree/develop/src/main/java/org/openpnp/machine/reference/driver

If none of those will work for your machine, you will need to write one. Once
you have a driver, you can specify it's classname and configuration parameters
in `machine.xml`.

See the Development section for more information if you decide you need to write code.

## Development

For more information about developing OpenPnP, especially regarding contributing, please see
https://github.com/openpnp/openpnp/wiki/Developers-Guide.

# Getting Help

## Discussion Group

There is an active discussion group at http://groups.google.com/group/openpnp. This will typically be the best place to get help.

## IRC

We also have an IRC channel on Freenode IRC at #openpnp. If you don't have an IRC client, you can use [this web based one](http://webchat.freenode.net/?channels=openpnp).