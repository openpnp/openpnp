## What is it?

The GcodeAsyncDriver can improve the performance of sending commands to the controller. All commands are prepared and then only put into an outgoing queue. The queued commands are sent to the controller using a separate writer thread. OpenPnP no longer waits for a command to be acknowledged by the controller. Instead, the two sides can fully work in parallel. This also means that _multiple_ GcodeAsyncDrivers now can fully work in parallel with each other _and_ with OpenPnP. Only when there is a real functional need, are the participants waiting for each other. 

Only the GcodeAsyncDriver allows you to use the **Simulated3rdOrderControl** mode (see [below](#gcodedriver-new-settings)), where a high volume of commands must be sent to the controller at great speed. 

## Using the GcodeAsyncDriver instead of the GcodeDriver
___
**Caution**: Be aware that using asynchronous drivers changes the behavior of the machine. There is no longer a strict ping-pong between OpenPnP and the controller(s). Things will now happen in parallel, even between multiple controllers. Be careful when testing the machine for the first few times, including the homing cycle. For initial migration, it is recommended to switch off **Home after Enable** in the Machine:

![Home after enable off](https://user-images.githubusercontent.com/9963310/96225353-3f345180-0f91-11eb-9d10-db23fc4418f5.png)

Some of the advanced Features require upgraded controller firmware. Please refer to the [[Motion Controller Firmwares]] page. 
___

### For new Drivers

If you are adding a new driver, just press the **+** button and select the GcodeAsyncDriver:

![Create GcodeAsyncDriver](https://user-images.githubusercontent.com/9963310/96038934-2f6d1e00-0e68-11eb-8736-12018f01a8fd.png)

### For existing GcodeDrivers 

Use the [[Issues and Solutions]] system. It will suggest the replacement of the driver for you. You need to progress to the **Advanced** Milestone, for Issues & Solutions to suggest the conversion. If you accept, all the work is done, i.e. you won't lose any existing G-code, or Axis and Actuator assignments.

If for some reason you still need to do it manually, and you are migrating an existing `machine.xml`, and you want to keep the settings and the G-code setup of your existing GcodeDriver, you need to change its class _in place_. 

1. Close OpenPnP. 
2. Open the `machine.xml` found [here](https://github.com/openpnp/openpnp/wiki/FAQ#where-are-configuration-and-log-files-located).
3. Search and replace "GcodeDriver" with "GcodeAsyncDriver". Save.
4. Restart OpenPnP.

## GcodeDriver new Settings 
___
**Note**: the following GcodeDriver specific settings can be intergrated into the [[GcodeDriver]] page, once this is all in the regular (`develop`) OpenPnP Version.
___

![GcodeDriver new Settings](https://user-images.githubusercontent.com/9963310/96040839-347f9c80-0e6b-11eb-915b-4da9fa11f39d.png)

### Motion Control Type

**Motion Control Type** determines how the OpenPnP MotionPlanner will plan the motion and how it will talk 
to the controller:
* **ToolpathFeedRate:**

  Apply the nominal driver feed-rate limit multiplied by the speed factor to the tool-path. No acceleration control is applied. Behaves like earlier versions of OpenPnP.

  Note: The driver feed-rate must be specified. 

* **EuclideanAxisLimits:**

  Apply axis feed-rate, acceleration and jerk limits multiplied by the proper speed factors.

  The Euclidean Metric is calculated to allow the machine to run faster in a diagonal, as long as the driver's **Max Feed Rate** does not limit it. Remove the driver's limit (set to 0) for best speed. 

  OpenPnP only sets the maximum limits. It is left to the controller to find the maximum rates it can reach for the length of the move. 

* **ConstantAcceleration:**

   Apply motion planning assuming a controller with constant acceleration motion control. The maximum feed-rate is calculated and set.

* **ModeratedConstantAcceleration:**

   Apply motion planning assuming a controller with constant acceleration motion control but moderate the acceleration and velocity to resemble those of 3rd order control, resulting in a move that takes the same amount of time and has similar average acceleration. 

   This will reduce vibrations to a degree.

* **SimpleSCurve:**

   Apply motion planning assuming a controller with simplified S-Curve motion control. 

   Simplified S-Curves have no constant acceleration phase, only jerk phases. Examples are TinyG and Marlin (if enabled). Slow for longer moves.

* **Simulated3rdOrderControl:**

   Apply motion planning assuming a controller with constant acceleration motion control but simulating 3rd order control with time step interpolation. [Watch a video](https://youtu.be/cH0SF2D6FhM) to see the effect of **Simulated3rdOrderControl** on oscillations/vibrations. 

* **Full3rdOrderControl:**

    Apply motion planning assuming a controller with full 3rd order motion control. No such controller is currently known. 

**Letter Variables** changes the Gcode variable names (the `{var ...}` markers) from the stock 4-axis `X`, `Y`, `Z`, `Rotation` to the actual controller Axis Letters, i.e. `X`, `Y`, `Z`, `A`, `B`, `C` etc. simplifying commands. Allows defining commands for all the axes of the controller at once. Different `MOVE_TO_COMMAND`s for different Head Mountables are no longer needed. The motion planner can now move all the axes at once (not just 4), which is needed for some "motion blending" applications. 

**Allow Pre-Move Commands?** must obviously be switched off for all-axis **Letter Variables**. Switching it off hides the pre-move command fields on the [[controller axes|Machine-Axes#referencecontrolleraxis]] and allows some of the more advanced motion control features. 

**Remove Comments?** removes all Gcode comments from the command strings sent to the controller. Safes bandwidth, which is relevant for **Simulated3rdOrderControl** mode, where the motion path interpolation creates a high volume of commands per time. 

**Compress Gcode?** removes all unnecessary characters from the Gcode command, such as all whitespace, trailing floating point-zeros etc., again to safe bandwidth.

## Control of Speed Factors

Among other things, the **Motion Control Type** determines how OpenPnP controls speeds. The **ToolpathFeedRate** (same behavior as previous versions of OpenPnP) simply scales the maximum driver feed-rate with the Speed [%] or speed factor. If a move is short, it will never reach the set feed-rate limit, so the speed factor is completely ineffective ([[using Motion Planner Diagnostics for illustration|Motion-Planner#motion-planner-diagnostics]]):

![Ineffective-Feedrate-Limit](https://user-images.githubusercontent.com/9963310/96266933-8b9a8400-0fc7-11eb-88c0-737ddaa0e0ee.gif)

However, with the other **Motion Control Type**s, the velocity that is effectively reached is properly scaled with the Speed [%] or speed factor. The move duration is scaled inverse-proportionally (1/factor), e.g. a 50% speed move takes exactly _twice_ as long:

![Effective-Feedrate-Limit](https://user-images.githubusercontent.com/9963310/96274469-cead2500-0fd0-11eb-8626-ea42324f8680.gif)

This is done by also controlling acceleration (and possibly jerk) limits. The acceleration limit is scaled with the speed factor to the power of 2, the jerk limit (if applicable) with the factor to the power of 3. So a 50% speed move has only 25% of the accleration and a mere 12.5% of the jerk. Even a moderate reduction of speed results in a much smoother/gentler motion, due to strong attenuation of accelerations ("g-forces" acting on nozzle, part etc.) and vibrations. 

Therefore, if you migrate an existing machine setup and then change the **Motion Control Type**, you will need to revisit the various speed factors. Because slowed moves are now much smoother/gentler, you can get away with higher speed factors. 

These are the most important configurations:

* [[Nozzle tip tool changer|Setup-and-Calibration_Nozzle-Setup#nozzle-tip-changer]] speed factors. 
* [[Parts|User-Manual#parts]] speed factors.
* [[ReferencePushPullFeeder|ReferencePushPullFeeder#push-pull-motion]] and ReferenceLeverFeeder feeder actuation speed factors.
* [[BlindsFeeder]] cover opening/closing speed factors.
* [[Backlash-Compensation]] speed factor (one-sided methods).

## GcodeAsyncDriver specific Settings

### Advanced Settings

The GcodeAsyncDriver adds the new **Advanced Settings** tab:

![GcodeAsyncDrivre Settings](https://user-images.githubusercontent.com/9963310/97790888-fa352f80-1bcc-11eb-8798-846b5df13e22.png)

### Settings

**Confirmation Flow Control** forces OpenPnP to wait for an "ok" by the controller, before sending the next command. This only concerns the communications writer thread, i.e. unlike the GcodeDriver (without the "Async") it does not block any of the other activities and threads of OpenPnP. More G-code commands can still be prepared and queued in parallel. This is particularly advantageous with multiple drivers/controllers.

**Location Confirmation** enables a position report from the controller whenever motion has completed. If the position has changed behind the back of OpenPnP, the reported location will be updated. This may happen after the homing cycle, with the [[Contact Probing Nozzle]] where the Z was probed, through custom Gcode in Actuators or Scripts etc. 

One of the two options must always be enabled, otherwise OpenPnP wouldn't know when the controller is finished with its commands. Full asynchronous/parallelized operation is only achieved with **Location Confirmation** _enabled_ and **Confirmation Flow Control** _disabled_. The position report response will be used as a signal for OpenPnP to know when the machine has reached a position and is now standing still. 

If you switch **Confirmation Flow Control** off, make sure you have flow-control in the communications. For serial port communications you need to select one of the active **Flow Control** choices:

![Serial Port Flow Control](https://user-images.githubusercontent.com/9963310/96151994-51b87780-0f0c-11eb-8bf6-771492d04862.png)

**RtsCts** has been confirmed to work for Smoothieboard and Duet 3 over USB, but not for TinyG. See also the [[Motion Controller Firmwares]] page. 

### Interpolation 

The interpolation settings determine how the **Simulated3rdOrderControl** is approximated (see the [Motion Control Type](#gcodedriver-new-settings)). 

**Maximum Number of Steps** limits the number of interpolation steps per move. If this number is exceeded, the interpolation will fail, and the **Motion Control Type** will fall-back to **ModeratedConstantAcceleration**. A debug-message will be logged.

This number must correspond to queue depth of your controller and best leave some steps free for look-ahead into subsequent moves. Smoothieware can be tuned in the `config.txt`. I was able to increase it from 32 to 48: 

`    planner_queue_size      48 # DO NOT CHANGE THIS UNLESS YOU KNOW EXACTLY WHAT YOUR ARE DOING`

Duet can set it using the [`M595` command](https://duet3d.dozuki.com/Wiki/Gcode#Section_M595_Set_movement_queue_length) with the `P` word. Also use the `R` word to set a grace period (please ask other Duet users for their proven values, this is still being optimized). 

Other controllers have a fixed queue size that you should try to find out. 

**Maximum Number of Jerk Steps** limits the number of interpolation steps used to ramp up and down acceleration. 

True 3rd order Motion Control (a.k.a. Jerk Control) would ramp up acceleration smoothly. On a constant acceleration controller this is simulated by a step-wise ramp. The [[Motion Planner Diagnostics|Motion-Planner#motion-planner-diagnostics]] illustrate how this is done. Stronger lines indicate the planned jerk controlled motion, lighter lines indicate the interpolated constant acceleration motion: 

![Simulated Jerk Control](https://user-images.githubusercontent.com/9963310/96153482-0dc67200-0f0e-11eb-8d6e-fe7ac8a249eb.png)

Note: the maximum number of steps is only used if the maximum acceleration limit is reached. Sometimes an extra step is added when the feed-rate limit is reached. [Watch a video](https://youtu.be/cH0SF2D6FhM) to see the effect of **Simulated3rdOrderControl** on oscillations/vibrations. 

**Minimum Time Step** determines the smallest possible interpolation interval. Generated interpolation steps will be multiples of this. Be aware that setting a very small interval will increase computation time. 

**Minimum Axis Resolution Ticks** effectively sets the minimum distance of one step. There is a practical limit when the steps start to collapse into very few micro-steps (or analog) of the axis motors (see the axis [[Resolution|Machine-Axes#controller-settings]]). Because axes can have dramatically different scalar resolutions (especially between linear and rotary axes) this limit is given as a number of resolution ticks. At least one axis must move so many ticks per interpolation step. 

**Maximum Junction Deviation** is a somewhat artificial maximum allowed elastic deviation of the machine given as a length (_s_). 

For [[Motion Blending|Motion-Planner#motion-blending]], we are generating curved trajectories. Interpolation uses a polygon to approximate the curve. But this means that in the corners (junctions) of the polygon, there is a "jolt" where the machine instantly changes its direction slightly. As it is physically impossible to change the direction of a moving mass instantly (against inertia), the machine will simply _have to_ react elastically. The moving mass will overshoot and then be drawn back on course by elastic forces. We just assume (hope) this happens mostly in the electromagnetic forces of the motors. For a stepper motor, the maximum allowed deviation is in the order of one full-step, before steps are lost, therefore we need to limit the junction deviation accordingly. 

The following image illustrates the effects (exagerated):

![junction-deviation](https://user-images.githubusercontent.com/9963310/96157974-44eb5200-0f13-11eb-9e97-371bd5dfb17d.png) 

OpenPnP takes each maximum axis acceleration into consideration to see how far the green trajectory deviates (the acceleration drawing the axis back on course). It can then deduce a per-axis maximum allowed instant velocity change. Interpolation steps are generated, before this threshold is exceeded. 

All the above parameters are subject to the speed factor applied to the move. The idea is to generate the same interpolation regardless of speed, so we can examine it in "slow motion". 

## Next Step

Configure the [[Motion Planner]].

___

## Advanced Motion Control Topics

### Motion Control
- [[Advanced Motion Control]]
- [[GcodeAsyncDriver]]
- [[Motion Planner]]
- [[Visual Homing]]
- [[Motion Controller Firmwares]]

### Machine Axes
- [[Machine Axes]]
- [[Backlash-Compensation]]
- [[Transformed Axes]]
- [[Linear Transformed Axes]]
- [[Mapping Axes]] 
- [[Axis Interlock Actuator]]

### General
- [[Issues and Solutions]]
