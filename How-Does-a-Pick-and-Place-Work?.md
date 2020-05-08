# Work In Progress

A pick and place machine is a very complicated piece of hardware made up of several systems and lots of components. Having an understanding of those system and components, and how they fit together, is critical if you want to build a machine yourself. This page will attempt to describe those systems in medium detail to help spur an understanding of how a pick and place machine works.

If you can help add detail to this page, please click the Edit button!

# Overview

At a high level, a pick and place machine consists of the following systems:

* Motion Control: Hardware, electronics, and firmware that controls the movement of the machine.
* Vacuum and Air: Vacuum pumps, plumbing, solenoid valves, vacuum sensors, "blow off" pumps, etc.
* Computer Vision: Cameras, lenses, and lighting which lets the machine "see" things.
* Feeders: Feeders provide parts for the machine to place. They can be simple or entire machines on their own.
* Software: OpenPnP is software you can use to run a pick and place machine and build boards!

Each of these topics will be explored in detail below.

# CNC

Before we get into details, it's important to touch on the concept of [CNC](https://en.wikipedia.org/wiki/Numerical_control). CNC stands for Computer Numerical Control, and is a somewhat quaint term that describes using a computer to move a machine tool.

Before CNC, machines like milling machines, lathes, plasma cutters, saws, pipe benders, and drills were moved manually to each position that needed to be worked on by a skilled operator. CNC added additional motors to these machines which let a computer move them automatically into position and modern manufacturing was born.

CNC is important in this discussion because a pick and place machine is, at it's heart, a CNC machine. You could certainly move the nozzle around by hand, and turn the vacuum on and off with a switch, but it's so much easier and fun to let your computer, and OpenPnP, do it for you!

In the discussion below, when you see CNC you can just think of it as a machine controlled by a computer.

# Motion Control

Motion Control is a big topic, and arguably the most important. The ultimate goal of a motion control system is to be able to move an end effector - a pick and place nozzle in our case - to a specific position in 4D space. There's a lot of ways to do it.

Before we continue - let's talk about 4D space. Why 4D? If you have experience with CNC machines you might be used to 3D being X, Y, and Z. X is left and right, Y is front and back, and Z is up and down. Pick and place adds a 4th dimension which is rotation. To place a part perfectly on a board you need to move to the right X, Y, Z, and rotation to match where the board designer expected the part to go.

This means that our motion control system has to have a minimum of four axes. In the simplest designs this means 4 motors, one for each axis.

OpenPnP speaks in real world units. When OpenPnP wants to place a part, it tells [[Motion Controllers]] to move to a real world coordinate, like 10mm in X, 20mm in Y, -10mm in Z and 90 degrees in rotation. A motion controller is a piece of hardware responsible for converting between those real world coordinates and electricity to move motors.

## The Motion Controller

The motion controller is, in many ways, the most important part of a pick and place. The motion controller is usually a PCB with a microprocessor and a number of outputs, but it can also be a piece of software running on a PC. It's job is to convert real world coordinates into motion.

Some motion controllers, like Smoothie, can drive motors directly, while other motion controllers like LinuxCNC leave the motor driving to separate electronics.

The most common type of motion controller is a Gcode step and direction controller. Let's break that down:

- Gcode: [Gcode](https://en.wikipedia.org/wiki/G-code) is a text based language for telling machines how and where to move. OpenPnP can send Gcode commands to a motion controller to tell it where to move the head of the machine. Gcode can be incredibly complex, but OpenPnP only uses a very small subset of it. We'll touch on this more later.
- Step and direction: Step and direction or step/dir is the most common method of driving 