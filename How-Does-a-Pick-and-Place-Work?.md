# Work In Progress

A pick and place machine is a very complicated piece of hardware made up of several systems and lots of components. Having an understanding of those system and components, and how they fit together, is critical if you want to build a machine yourself. This page will attempt to describe those systems in medium detail to help spur an understanding of how a pick and place machine works.

If you can help add detail to this page, please click the Edit button!

# Rough Outline

- Frame and Bed
  - Work Holding
- Motion Control
  - Motors
    - Steppers
    - Servos
  - Motor Drivers
  - Rotational to Linear Motion
  - Motion Controllers
- Vacuum and Air
  - Vacuum Pumps
  - Solenoid Valves
  - Vacuum Sensors
  - Blow Off Systems
- Vision Systems
  - Cameras
    - USB
    - IP
  - Lenses
  - Lighting
- Feeders
- Conveyors
- Software

# Overview

At a high level, a pick and place machine consists of the following systems:

* Frame and Bed: The frame and bed make up the physical elements of the machine that everything else attaches to.
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

# Frame and Bed

The frame of the machine is simply what you attach everything else to. Frames come in all shapes and sizes, and everyone has their own idea of the perfect layout. They can be 3D printed, made out of wood or metal, welded or screwed together, and on professional machines they are even made out of multi-ton cast iron moldings! The most common frame material for DIY pick and place machines is t-slot aluminum extrusion.

Mass plays an important role in frame design. The more mass your frame has, the less likely it is to move around as the motors do their work. Stiffness, or rigidity, of a frame is important too. If your frame flexes as the machine does it's work it will be less precise.

# Motion Control

**Note to self: This 4D space stuff is bad terminology. Go back to 3D and refer to 4 axes.**

Motion Control is a big topic, and arguably the most important. The ultimate goal of a motion control system is to be able to move an end effector - a pick and place nozzle in our case - to a specific position in 4D space. There's a lot of ways to do it.

Before we continue - let's talk about 4D space. Why 4D? If you have experience with CNC machines you might be used to 3D being X, Y, and Z. X is left and right, Y is front and back, and Z is up and down. Pick and place adds a 4th dimension which is rotation. To place a part perfectly on a board you need to move to the right X, Y, Z, and rotation to match where the board designer expected the part to go.

This means that our motion control system has to have a minimum of four axes. In the simplest designs this means 4 motors, one for each axis.

OpenPnP speaks in real world units. When OpenPnP wants to place a part, it tells [[Motion Controllers]] to move to a real world coordinate, like 10mm in X, 20mm in Y, -10mm in Z and 90 degrees in rotation. A motion controller is a piece of hardware responsible for converting between those real world coordinates and electricity to move motors.

## Motion Control

The motion controller is, in many ways, the most important part of a pick and place. The motion controller is usually a PCB with a microprocessor and a number of outputs, but it can also be a piece of software running on a PC. It's job is to convert real world coordinates into motion.

Some motion controllers, like Smoothie, can drive motors directly, while other motion controllers like LinuxCNC leave the motor driving to separate electronics.

The most common type of motion controller is a Gcode step and direction stepper motor controller. Let's break that down:

- Gcode: [Gcode](https://en.wikipedia.org/wiki/G-code) is a text based language for telling machines how and where to move. OpenPnP can send Gcode commands to a motion controller to tell it where to move the head of the machine. Gcode can be incredibly complex, but OpenPnP only uses a very small subset of it. We'll touch on this more later.
- Step and direction: Step and direction or step/dir is the most common method of driving a variety of motors commonly used for CNC. It is a simple interface with two pins: step and direction. You set the direction pin high or low based on whether you want the motor to turn clockwise or anti-clockwise and then you pulse the step pin once for each "step" you want the motor to take. We'll talk more about this in the motors section below.
- Stepper motor: Stepper motors are the most common type of motor used in DIY CNC machines. They are commonly found in 3D printers. A stepper motor differs from a "normal" motor like you'd find in a drill or washing machine in that the stepper motor moves in small, defined steps instead of continuously rotating.

So, putting that all back together, a *Gcode step and direction stepper motor controller* reads Gcode commands and converts them into step and direction pulses to control a stepper motor.

# Motors

There are a lot of different ways to move a machine around. In pick and place machines, and in other CNC machines, the most common way is to use electric motors that can be told to move to a specific position. The two most common types of motors that can do this are stepper motors, and servos.

## Stepper Motors

We touched on stepper motors above, but it's worth expanding on it. Most stepper motors in use today are 200 steps per revolution, which means that the motor can stop at any one of 200 positions per turn. Since a turn is 360 degrees, this means the motor turns 1.8 degrees per step. You'll find these common numbers popping up all over the place when learning about stepper motors.

Most common stepper motors are bipolar, four wire motors. Bipolar means there are two sets of magnetic windings that are used to control the position of the motor. Each set of windings has two wires. By applying power to the two sets of windings in various configurations you can cause the motor to move one step in either direction.

Some stepper motor drivers also support something called microstepping. Microstepping allows the driver to position the motor in between steps. If you have a driver that supports "16x microstepping" this means it can (theoretically) position the motor at 16 microsteps per step. In other words, instead of the motor having 200 steps per rev, it has 200 * 16 = 3200 steps per rev!

Microstepping can be used to increase the resolution of your axes, but in reality it doesn't increase resolution much beyond 4x. It's still useful past 4x, though, as it helps smooth the motion out.

Finally, it's important to note that most stepper motors use **open loop control**. Open loop control means that the controller will tell the motor to move to a position, but it has no way to know that it did. If something was blocking the motor from moving, for instance, it might not be able to reach the position, and the controller will never know. This results in what is called "lost steps", and it can be a big problem if you try to drive your motor too fast. The opposite of open loop control is closed loop control, and we'll explore that below.

## Servos

The other most common type of motor used in CNC is a servo. You may have run into a servo motor before in radio controlled planes and cars, and the concept is the same. A servo is a motor that can control it's position with feedback from a sensor.

In an RC hobby servo, the sensor is often a potentiometer connected to the shaft of the motor. As the motor turns, the potentiometer's resistance changes and a controller reads the resistance. The controller maps the resistance to a position and it keeps the motor turning until it's reached the right position and then stops.

In CNC servos the sensor is usually referred to as an "encoder". A potentiometer is a type of encoder, in that it it encodes a position into an output, but CNC encoders are usually optical or magnetic. These types of encoders can be extremely precise, measuring 10,000 positions per rev or more!

So, with a servo you have a motor that can turn in either direction, and a sensor that tells you something about the position of the shaft of the motor. Reading this sensor and moving the shaft until the position is correct is called "closed loop control".

Servos come in a variety of shapes, sizes, and configurations. They can be AC or DC and brushed or brushless.

## Motor Drivers

Motor drivers take a variety of low voltage, low current inputs, usually digital, and convert them to (sometimes) high voltage, high current outputs to drive the motor. We talked about step and direction a bit above, and that is the most common type of motor driver used for CNC. Motor drivers can be incredibly complex, incorporating PID control, encoder reading, tuning, and switching of massive voltages and currents. 