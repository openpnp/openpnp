The motion controller is the interface between OpenPnP and your hardware. It translates OpenPnP's movements commands into electrical signals that control your motors.

# What is a Motion Controller?

The term motion controller can be a little confusing, and it means a few different things. People often use the phrase "motion controller" to mean the actual board that you install in your machine, or it can mean the software (or firmware) that runs on the board. It can even mean a completely computer based system like LinuxCNC.

When we say motion controller we just mean "whatever OpenPnP sends commands to to make your hardware move."

# Choosing a Motion Controller

Choosing a motion controller for your machine is a very important decision. The motion controller will determine how fast the machine moves, how many motors it can control, how many outputs are available for things like solenoids and pumps, etc.

In general, the most important decision is to pick a motion controller that supports as many motors as you need on your machine. A basic pick and place machine has 4 motors, or axes. They are X, Y, Z and C, which is also called Rotation. X and Y move the head around, Z moves the nozzle up and down and C rotates the nozzle. More complex machines may have multiple Z axes and multiple C axes. These are referred to as Z2, Z3, C2, etc.

Another important consideration is making sure that the motion controller has enough outputs to control your various peripherals. The most basic PnP will have 1 output; usually a nozzle vacuum solenoid. More complex machines may have additional solenoids for exhaust and blow off, switches for pumps and lights, solenoids for feeders, etc.

There are a *lot* of motion controllers available, from Open Source software that runs on an Arduino and a shield, to all in one systems like Smoothie, all the way up to closed loop, high power servo controllers. Most people find that something in the middle works well.

# Smoothie

Based on years of evidence, and lots and lots of different builds, we recommend a [Smoothie](http://smoothieware.org/) based board for most machines. Smoothie is an Open Source motion controller firmware that runs on a variety of affordable, all in one boards. It's easy to configure, well documented and works great with OpenPnP.

Some Smoothie based boards that are known to work with OpenPnP, and which you can buy online are:
* **Smoothieboard**: http://smoothieware.org/getting-smoothieboard
    The original. Buying this board helps support the creators of Smoothie. Available with up to 5 stepper drivers and 6 MOSFET outputs.
* **Cohesion3D Remix**: http://cohesion3d.com/cohesion3d-remix/
    Created by an OpenPnP forum member, this board is designed with PnP in mind and has up to 6 stepper drivers and 6 MOSFET outputs. This board is great for larger, more complex machines.
* **MKS SBASE**: A popular Smoothie clone. Cheap, but not very well supported.

## Peter's Head

There is a common style of pick and place often referred to as "Peter's Head". This style of head has one Z axis motor which uses belts or gears to drive two nozzles. Due to the complex homing operation required for this type of head, you may need to use the modified Smoothie firmware found [here](https://github.com/openpnp/Smoothieware) if you have this style of head.

# Other Options

In general, any motion controller that can accept GCode commands for movement and output control will work with OpenPnP. You can even use other protocols than GCode if you are feeling adventurous.

## TinyG

[TinyG](http://synthetos.myshopify.com/products/tinyg) is another great Open Source motion control platform. It supports up to 6 axes and is one of the only ones to support S-curve acceleration. This makes it's motion very smooth and can allow for faster accelerations without losing steps. The TinyG board only has 4 stepper drivers, but if that's all you need than it's an excellent choice.

## Grbl

[Grbl](https://github.com/gnea/grbl) is an Open Source motion control system for the Arduino platform. Grbl is very easy to get up and running and can be considered the cheapest option, but it only supports 3 axes by default. This makes it not ideal for pick and place since it leaves you without an option to rotate the nozzle. There is a modification of Grbl available [here](https://github.com/openpnp/grbl) but it is out of date, unsupported and somewhat buggy. You can use it in a pinch if it's all you have, but it's not recommended.

## Marlin and Other 3D Printer Firmwares

Every 3D printer is by definition at least a 4 axis machine and this makes 3D printer firmware tempting for pick and place motion control. The most popular of the bunch is [Marlin](https://github.com/MarlinFirmware/Marlin). Marlin can be used with OpenPnP but it has some inherent limitations based on it's focus on 3D printing. It can be difficult to get acceleration and maximum velocity set up correctly since these are often tied together on a 3D printer. In addition, configuration is complex because you have to remove a lot of the 3D printing functionality. 

It's not recommended to use 3D printer firmware with OpenPnP, but if you work hard enough it can be made to work.

# Drivers

In OpenPnP, the driver is the piece of the program that talks to the motion controller. The driver converts OpenPnP command to motion controller commands. In all of the cases above you should use the [[GcodeDriver]]. It is a flexible, well supported and well documented driver that can be used with almost any motion controller.

OpenPnP also contains a number of motion controller specific drivers such as GrblDriver, OpenBuildsDriver, MarlinDriver, etc. These should only be used if you are using a machine specifically designed for these drivers. In almost every case the GcodeDriver will be a better fit.

There is more information about setting up your driver in [[Setup and Calibration: Driver Setup]].