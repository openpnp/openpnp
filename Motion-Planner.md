## What is it?
The Motion Planner is the central entity planning and coordinating motion across drivers. In many respects it acts like one black-box that behaves as if it were one single driver. 

## Choosing a Motion Planner
The Motion Planner type can be chosen on the machine. 

![Machine Motion Planner](https://user-images.githubusercontent.com/9963310/96167234-63efe100-0f1f-11eb-94d1-fa317a9c83a1.png)

**CAUTION**: when you select a different type and press **Apply**, the specific settings of the former planner will be lost!

## NullMotionPlanner

**NullMotionPlanner** is the minimal planner selected by default. It is almost completely equivalent to how OpenPnP worked before the Motion Planner was introduced.

The NullMotionPlanner will execute moves one by one. It will wait for each move to complete, before allowing OpenPnP to go on. The machine will always move from complete still-stand in a straight line to complete still-stand, again. This is a very simple and robust model that is equally simple to understand. It is therefore still a valid and attractive choice for those who want to **keep it simple**.

Plus you can still use _some_ of the features offered by the [[Advanced Motion Control]] package, like better axis centric speed control and [[new GcodeDriver settings|GcodeAsyncDriver#gcodedriver-new-settings]].

## ReferenceAdvancedMotionPlanner

**ReferenceAdvancedMotionPlanner** is the motion planner that can do the things explained on the [[Advanced Motion Control]] page.

### Motion Interpolation

The ReferenceAdvancedMotionPlanner can perform full 3rd order Motion Planning a.k.a. Jerk Control. However, most common controllers don't support this. Using time-stepped interpolation, the ReferenceAdvancedMotionPlanner can then still simulate Jerk Control on these constant acceleration controllers.

See the GcodeAsyncDriver's [[Interpolation settings|GcodeAsyncDriver#interpolation]].

![Simulated Jerk Control](https://user-images.githubusercontent.com/9963310/96153482-0dc67200-0f0e-11eb-8d6e-fe7ac8a249eb.png)

### Motion Path Planning

The ReferenceAdvancedMotionPlanner will first just record the motion that OpenPnP wants to do. Nothing is planned or executed yet. At some point it will be a functional necessity for OpenPnP to make sure the machine is at the target location. One example is wanting to do Computer Vision, obviously the camera needs to be at the target location before it can take a picture. OpenPnP will then ask the Motion Planner to wait for motion completion, which triggers motion planning, execution and finally truly waiting for its completion. 

In the meantime there may have been several moves recorded, creating a motion path. The controller will now get all the path commands at once, which will allow it to use look-ahead planning and doing subsequent steps as fast as possible without any communications ping-pong in between moves. This alone will increase speed for slow or laggy connections. 

But the real winner comes now.

### Motion Blending 
___
Experimental!
___

Having recorded a sequence of moves, the ReferenceAdvancedMotionPlanner is now free to optimize and plan its execution, rearranging and even modiyfing moves within the machine constraints. 

One thing the planner despises are corners. Corners (often 90° in OpenPnP) mean the machine must come to (almost) a complete still-stand and then start accelerating from scratch. In OpenPnP, the typical application is the Move-to-Location-at-Safe-Z pattern. The Nozzle is down for a pick or place, then goes up to Safe Z, moves over the target location and then plunges down in Z to pick or place again. 

The NullMotionPlanner would generate a rectangular path, like the first one shown below. The ReferenceAdvancedMotionPlanner optimizes that path to look more like the second one. Because of the rounded, overshooting curves, the machine does not need to slow down much at any time. Instead, it can speed through the whole path in one fluid motion!

![Move-to-Location-at-Safe-Z](https://user-images.githubusercontent.com/9963310/96170324-bc28e200-0f23-11eb-8099-d78cb9ffe1b9.png)

OpenPnP has the notion of a Safe Zone (see the [[Safe Zone axis limits|Machine-Axes#kinematic-settings--axis-limits]]) where no obstacles are allowed, so collisions can be excluded. The motion planners is therefore free to "play" anywhere in this Safe Zone. This is exploited to let the Z axis overshoot, the X/Y axis is already free to start moving, even before the Z axis has come to a stop. The same happens at re-entry, in reverse order. The planner can effectively overlap _or blend_ the deceleration phase of Z with the acceleration phase of X/Y (and vice versa), hence the name "Motion Blending".  

![AdvancedMotionAnimation](https://user-images.githubusercontent.com/9963310/95627544-ab3c2480-0a7c-11eb-8d36-d6921ecf7423.gif)

The actual result will be illustrated in the [Motion Planner Diagnostics](#motion-planner-diagnostics) section, below. 

## Motion Planner 

### Settings

If you have the ReferenceAdvancedMotionPlanner selected, the **Motion Planner** tab will be available.

![Motion Planner Settings](https://user-images.githubusercontent.com/9963310/96173504-41ae9100-0f28-11eb-8e0f-c5367da7dfe0.png) 

**Allow continuous motion?** will enable path recording and will defer planning and execution until there is functional requirement to wait for motion completion. **Caution**: When enabling this on an existing machine, be aware that this may change its behavior. There might be constellation when this requires changes in G-code. Be careful! 

**Allow uncoordinated?** will enable curved motion in the Save Zone. If you want Motion Blending, you must enable this.

**Interpolation Retiming?** the interpolation works using a **velocity over space** polygonal approximation. The straight lines of the polygon used to approximate the curve are acting like "shortcuts". This means that the resulting move is slightly faster than its planned counterpart. By enabling this switch, the interpolated move will be stretched in time, to match the planned time again. Conversely, this will slightly lower the peak velocity when compared to the planned peak velocity. 

### Test Motion 

You can define a test motion with 4 locations and speed factors to test the motion planner in a repeatable way. Press the **Test** button on the **Motion Planner Diagnostics** tab to do so.

## Motion Planner Diagnostics

If you have the ReferenceAdvancedMotionPlanner selected, the **Motion Planner Diagnostics** tab will be available. The graphical diagnostics plot the movement of all participating axes over time. You get Location, Velocity, Acceleration and Jerk both for the planned motion (strong lines) and their interpolation (light lines). Move the mouse over the graphs to read off values from the plots. 

The following example shows a move with Motion Blending. It is clearly visible, how the Z axis motion _blends_ with the X axis movement over time.

![Motion Planer Diagnostics](https://user-images.githubusercontent.com/9963310/96174107-1e381600-0f29-11eb-8e0a-9a4bd160963b.png)

**Enabled?** switches diagnostics on. This might add a slight CPU load penalty.  

The **Test** button plays the **Test Motion** defined on the previous tab. It does it forward/backward, alternating.

**Planned [s]** indicates how long the move would take as planned, in seconds. This is always available. 

**Actual** indicated the time the move actually took, as measured, including all the overhead. Note, this is only available for the **Test Motion**. In the example screenshot you see how the move took longer than planned. This is due to Smoothieware not offering a queue size that is large enough. Motion Blending has not been proven in practice yet (see the [pull-request for more information about this](https://github.com/openpnp/openpnp/pull/1061)).

