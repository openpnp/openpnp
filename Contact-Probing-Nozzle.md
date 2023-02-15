## What is it?
Some machines (e.g. the Liteplacer) have a nozzle that can sense when the nozzle tip touches down. The idea is to automatically stop Z down motion without having to know exactly how high a part, a feeder and/or a PCB is. To use this feature in OpenPNP, you can use the `ContactProbeNozzle`.

What is it used for?

- Nozzle/Nozzle Tip Z Calibration
- Part Height Auto-Learning
- Feeder Z Auto-Learning
- Placement Z Auto Learning

The use cases are explained further down.

## What is it _not_?

Contact Probing is not to be confused with [[Z Probing]], which is a completely separate concept, used to measure Z surfaces. [[Z Probing]] is always manual and more for setting up the machine and feeders, and may be able to measure delicate surfaces (like lose parts in feeder) without contact if you have a laser displacement sensor. 

Conversely, the Contact Probing Nozzle explained here, can be fully integrated into production, measurements can efficiently take place "on the go", with no extra machine moves, because we can probe with a part on the nozzle (sandwiching), to get the _real live_ part, solder paste, and board surface heights, or _real live_ feeder pick heights.

## Use Cases

### Nozzle/Nozzle Tip Z Calibration

Machines that home the Z axis with easily shifted Zmax switches ([e.g.](https://liteplacer.com/the-machine/assembly-instructions/pnp-head-step-11-attach-z-high-limit-switch/) Liteplacer), hard to balance dual-nozzle mid-axis sensors or even by just letting springs retract unpowered Z motors, may not provide a precise and repeatable Z reference.  

A ContactProbeNozzle can then be used to calibrate the Nozzle in Z, more specifically the point of the loaded Nozzle Tip. By probing a known reference Z touch surface, it can apply the obtained calibration offset to all future Z movements. 

For multi-nozzle machines, the nozzles can therefore be perfectly harmonized in Z. This can (optionally) be extended to individual nozzle tips if these have varying heights, or if the coupling to the nozzle is not very consistent. 

Having a calibrated Z not only establishes precision in movements, but also in capturing or probing other Z locations. 

### Part Height Auto-Learning

With a ContactProbeNozzle and enabled feeder and part height probing, OpenPnP can now also learn part heights automatically. Whenever a part with unknown height is picked or placed for the first time, the probed height will be stored on the part. 

* Part height auto-learning works for feeders that have the part height _above_ the pick location Z (ReferenceLoosePartFeeder and AdvancedLoosePartFeeeder). 
* Part height auto-learning also works in all placements, as obviously all parts are above the known board location Z. 

The job processor knows when a ContactProbeNozzle has this capability and will in this case allow starting a Job with unknown part heights. If only one nozzle in a multi-nozzle setup has probing capabilities, the planner will automatically restrict parts with unknown heights to this nozzle. As an alternative, part heights can also be auto-learned using [[Up looking Camera Auto Focus]], if bottom vision is enabled for the part. Note that if bottom vision is enabled for the part, auto focus part height auto-learning must be enabled as well, even though a ContactProbeNozzle is available. This is because bottom vision requires the component height to be known, and contact probing with the nozzle only happens after that, when the component is placed on the PCB.



![Part height unknown](https://user-images.githubusercontent.com/9963310/113597986-c313f000-963c-11eb-84e9-b0bedb797185.png)

### Feeder Z Auto-Learning

The ContactProbeNozzle can also auto-learn the pick Z location of feeders. On the first pick the height will be probed and then remembered. This does not (currently) change the Z location of the feeder per se, because there is no universal way to do this (the handling of Z coordinates is feeder class specific). Instead a Z pick _offset_ is maintained and stored. 

Note: some feeders like the [[BlindsFeeder]] still need a precise Z location configured (e.g. for cover actuatuation). 

### Placement Z Auto Learning

The ContactProbeNozzle can also auto-learn the placement Z location of parts, even if the part height is known. On the first placement the height will be probed and then remembered. This will equalize nozzle (tip) spring compression force, taking solder paste height into consideration. 

Note, Placement Z Auto Learning is part of the **Part Height Probing** settings (below).

## Enable the ContactProbeNozzle 
If you don't have a ContactProbeNozzle yet, let [[Issues and Solutions]] replace the existing ReferenceNozzle for you. **Note**, the solution will only be available in the **Advanced** milestone, it is not recommended to configure it earlier in the machine setup process:
 
![Replace nozzle](https://user-images.githubusercontent.com/9963310/118363173-6a1c6d80-b593-11eb-9c8f-dff2827e5996.png)

The solution automatically extends the Nozzle with a new tab **Contact Probe**. 

You can also create a new Nozzle manually on the head and choose the ContactProbeNozzle type. This is not recommended.

## Contact Probe Nozzle Configuration

### Method Selection

![Contact Probe Method](https://user-images.githubusercontent.com/9963310/113565832-f25e3900-960b-11eb-9146-8d72be48e0af.png)

**Method** lets you select the probing method. 

- **None** switches contact probing off. The nozzle behaves as if it were a regular ReferenceNozzle. 
- **Vacuum Sense** uses the Vacuum Sensing setup of the nozzle to perform "sniffle probing". More specifically, the [Part Off](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration_Vacuum-Sensing#measurement-method) detection method is used to sense if the nozzle tip has touched the surface. 
- **ContactSensingActuator** uses a separate Actuator to probe and sense for contact. This is a setup made popular by the Liteplacer kit. 

Notes: The Wizard will show different fields according to the selection. If at least one nozzle has an active **Method** selected, other parts of the OpenPnP GUI will also show additional elements for contact probing. 

### Vacuum Sense Method

![Vacuum Sense](https://user-images.githubusercontent.com/9963310/113567748-77971d00-960f-11eb-996f-8ccd6828b488.png)

**Start Offset** determines how high above the nominal probing location, the sensing should start. For the **Vacuum Sense** method, this must be high enough to get a clear "Part Off" result on the first sniffle.

**Probe Depth** determines how far the probing should go. 

**Sniffle Increment** determines the step in Z. 

**Sniffle Dwell Time [ms]** sets the waiting time between two sniffles. This is needed to clear out any under-pressure from the previous sniffle.

**Final Adjustment** after contact was sensed, add this adjustment to the Z position. Positive values retract the nozzle, negative values add spring loading. You should aim for a light touch with barely any spring loading.  This way the tip Z reference can also be used to push things sideways such as with the [[BlindsFeeder]] cover actuation. 

**Calibration Z Offset** shows the last calibration result. 

**Calibrate Now** performs the Z calibration. You must first setup a **Touch Location** on all the noozle tip's **Tool Changer** tab. 

### Contact Sense Method

![ContactSenseActuator](https://user-images.githubusercontent.com/9963310/113569966-ac0cd800-9613-11eb-9b02-da625610db6d.png)

**Contact Sense Actuator** sets the actuator to sense when contact is made. See further below [how to setup the Actuator](#setting-up-a-probe-actuator). 

**Start Offset** determines how high above the nominal probing location, the sensing should start. For the **Vacuum Sense** method, this must be high enough to get a clear "Part Off" result on the first sniffle.

**Probe Depth** determines how far the probing should go. In fact it may go further but if the probing result is beyond the given depth an exception is thrown. 

**Final Adjustment** after contact was sensed, add this adjustment to the Z position. Positive values retract the nozzle, negative values add spring loading. You should aim for a light touch with barely any spring loading. This way the tip Z reference can also be used to push things sideways such as with the [[BlindsFeeder]] cover actuation. 

**Feeder Height Probing** determines when feeders should be probed for pick location Z offsets. 

**Part Height Probing** determines when parts should be probed for their height, or if the height is known, for the placement location Z offsets (nozzle spring force/solder paste height). 

![Probing Triggers](https://user-images.githubusercontent.com/9963310/113590798-6102bd00-9633-11eb-83df-8fc04a234680.png)

* **Off** switches off probing and Z offset application for the feeders/parts.
* **Once** probes for a Z offset only once. The offset is stored in the configuration (`machine.xml`). 
    
    CAUTION: Z offsets from probing _once_ will subsequently be applied quietly, _for ever_. You might forget it is applied, and when you later modify the feeder/machine geometry etc. this might appear as _seemingly_ broken behavior. Look for applied offsets in the log at TRACE level (search for "applies feeder" or "applies part"). To get rid of any stored Z probing data, switch to **Off** and home the machine.
* **AfterHoming** probes for fresh Z offsets per feeder/per part once after having homed the machine.  
* **Each Time** probes for Z offsets in all picks/placements. This is the most tolerant setting, but also likely the slowest. It can even equalize feeders and/or a PCB that are not planar in Z (a bit like Z probing on a 3D printer).

**Calibration Z Offset** shows the last Z calibration result for this nozzle with the current nozzle tip attached. See also the Nozzle Tip Probing Configuration.

**Calibrate Now** performs the Z calibration. You must first setup the Nozzle Tip Probing Configuration.

#### Setting up a Probe Actuator

With the Contact Sense Method you need a sensor actuator.

Go to Head / Actuators, [add a new probing Actuator](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration_Actuators#adding-actuators). 

On the Nozzle, assign the probe:

![grafik](https://user-images.githubusercontent.com/9963310/69479673-33cbbb00-0e00-11ea-9401-dd438b180c86.png)

#### Setting up the G-Code

[[Issues and Solutions]] will automatically propose G-code for probing on some well known controllers/firmwares. If these solutions do not appear you need to do it manually.

On your GCodeDriver choose the new contact probing Actuator and define the ACTUATE_BOOLEAN_COMMAND for probing:

![grafik](https://user-images.githubusercontent.com/9963310/69479755-4f839100-0e01-11ea-89b7-201993ef22b7.png)

### Testing the Contact Probing

To test the contact probing, move your nozzle tip on top of an object not quite touching it. Then go to the machine controls, switch to the Actuators tab and click on the Probe Actuator:

![grafik](https://user-images.githubusercontent.com/9963310/69497687-2dad0b80-0ee0-11ea-85ed-852254c53f4f.png)

You can now use the On and Off buttons to probe and (if applicable) retract. 


## Nozzle Tip Probing Configuration

### Part Dimensions

The first pre-requisite for probing are the maximum **Part Dimensions**, defined in the **Configuration** tab:

![Part Dimensions](https://user-images.githubusercontent.com/9963310/113574803-1d9d5400-961d-11eb-860e-bd391d797f16.png)

**Max. Part Diameter** determines the diameter (or diagonal) of the largest part you ever expect to pick with this nozzle tip. Add a few millimeters on top of data sheet dimension for tolerances. This property will be used in present and future up-looking camera operations to restrict the region of interest (Auto-Focus, MaskCircle etc.). 

**Max. Part Height** determines the height of the tallest part you ever expect to pick with this nozzle tip. Add a few tenths of a millimeter on top of data sheet dimension for tolerances. This property will be used to start probing above a part with unknown height. Furthermore, it will be used for dynamic Safe Z, again as long as a part height is unknown. 

### Tool Changer 

As soon as at least one ContactProbeNozzle has an active probing method, the Tool Changer tab will show extra elements:

![Tool Changer](https://user-images.githubusercontent.com/9963310/113617535-b8feeb00-9656-11eb-977e-6b4b4c8840bc.png)

**Touch Location** lets you define a special Z probing touch location. It will be the reference for all other Z coordinates on your machine, therefore it should represent a fundamental and "eternal" reference surface for your machine.  At the same time it should be very close to the changer locations so the extra moves for Z calibration do not take much time.  

![Nozzle Tip Change and Z Calibrate](https://user-images.githubusercontent.com/9963310/113582499-c00f0480-9628-11eb-8e7d-db51b25813cd.gif)

After capturing the X, Y coordinates with the camera, you must initially probe the reference Z coordinate with the middle button:

![Probe](https://user-images.githubusercontent.com/9963310/113576642-3824fc80-9620-11eb-8fae-ad1f35913c60.png) 

**CAUTION**: do not use the probe button later. You will lose your "eternal" Z reference! 

### Tool Changer Z Calibration

**Auto Z Calibration** determines when the Z calibration happens. 

![Auto Z Calibration](https://user-images.githubusercontent.com/9963310/113577217-3576d700-9621-11eb-9971-94a9fd2eca94.png)

* **Manual** switches automatic Z calibration off. You can manually calibrate and the obtained Z offset will be stored in the configuration (machine.xml). Note: **Z calibration is per nozzle**, i.e. **Manual** calibration assumes all nozzle tips have equal length and will be loaded consistently. This calibration can handle uneven nozzles in a multi-nozzle machine.
* **MachineHome** performs automatic Z calibration if this nozzle tip is loaded when the machine is homed. Note: **Z calibration is per nozzle**, when other nozzle tips are later loaded to the same nozzle, no additional Z calibration is triggered, i.e. **MachineHome** calibration assumes all nozzle tips have equal length and will be loaded consistently. This calibration can handle uneven nozzles in a multi-nozzle machine, as well as inconsistent Z homing. 
* **NozzleTipChange** performs automatic Z calibration whenever this nozzle tip is loaded to a nozzle. In addition, Z calibration also happens if this nozzle tip is loaded when the machine is homed. Note: **Z calibration is now per nozzle tip**. This can handle everything, from uneven nozzles to inconsistent homing to uneven nozzle tips with different heights or inconsistent loading. 

**Fail Homing?** If enabled, aborts the homing cycle when Z Calibration fails as part of it. If disabled, continues the homing cycle and only displays an error message. 

**Z** (behind the **Auto Z Calibration**) indicates the last obtained Z calibration offset. 

**Reset** removes the current Z calibration offset. 

**Calibrate now** performs manual Z calibration. 

### Z Calibration and Safe Z

Z Calibration affects all the nozzle Z motion as well as the capturing of Z coordinates from the current nozzle position. But it does not affect Safe Z, as Safe Z is relative to the raw axis coordinate system, _not_ the calibrated nozzle Z coordinate system. To balance a multi-nozzle machine's nozzles in Safe Z, use the [Axis capture buttons](https://github.com/openpnp/openpnp/wiki/Machine-Axes#kinematic-settings--axis-limits) instead. 

### Z Calibration inside the Tool Changer

The following does only apply, if **Auto Z Calibration** is set to **NozzleTipChange**, i.e. when Z calibration happens **per nozzle tip** and not per nozzle. For machines with uneven nozzle tips.

Z Calibration is referencing the coordinate system Z to the point of the nozzle tip (the red dashed line in the illustration below). This is obviously the most important Z reference point for a pick & place machine. However, when loading and unloading a nozzle tip, the point of the tip is not actually relevant, other features of the nozzle tip are: There are flanges etc. on the tip that come into contact with the changer slot and allow it to pressed on, or pulled off, these might be at different Z, as measured from the nozzle tip point (case B in the illustration below). And there is the coupling of the nozzle with the nozzle tip, these might be at different Z, as measured from the flange (case C in the illustration below). 

![nozzle_tip_different_Z_calibration](https://user-images.githubusercontent.com/9963310/131672327-9b7e377f-2326-4a8f-9a45-e9e0d2312d96.png)

Case A and B may actually use the same changer slot Z coordinates, because the flange is at the same (uncalibrated) nozzle Z. Case B's nozzle tip is longer, so after Z calibration, B is lifted up a bit. 

Case C however, because the nozzle is inserted less when coupling, needs a special slot configuration with higher-up (uncalibrated) Z coordinates, otherwise it would likely collide with the slot when pushed down as much as case A. After Z calibration, case C is lifted up to compensate for the lesser coupling depth.

When you initially capture the changer slot locations, the nozzle is not yet Z calibrated (or it is neutrally calibrated for the "unloaded" state i.e. the bare nozzle). These captured Z coordinates must **not** be made subject to Z calibration, as you can easily see, when you mentally try to load or unload case B or C with _calibrated_ instead of _uncalibrated_ Z. Therefore, OpenPnP voids the Z calibration immediately before unloading a nozzle tip. 

### Z Calibration for the bare Nozzle

For machines with very inconsistent Z homing and/or very uneven multiple nozzles, there must be a way to calibrate Z even before we can load any nozzle tips. There is a simple trick in OpenPNP to do that: just create a nozzle tip that is named "unloaded". The calibration system will automatically use this stand-in whenever the bare nozzle needs to be calibrated and compensated. The "unloaded" stand-in can be configured for Z calibration, like regular nozzle tips. The Z calibration will then be used for unloading/loading any nozzle tip. **CAUTION**: On a machine with very inconsistent Z homing, you must unload all the nozzle tips before exiting OpenPnP, power-cycling or homing the machine. On the next startup the bare nozzle can be measured as the first thing. The "unloaded" stand-in is explained in the similar context of [nozzle tip runout calibration](https://github.com/openpnp/openpnp/wiki/Nozzle-Tip-Calibration-Setup#calibrating-the-bare-nozzle).

### Cloning Settings

![Cloning Settings](https://user-images.githubusercontent.com/9963310/113582934-49bed200-9629-11eb-9340-fa6b391f77dd.png)

Among the other settings, the **Z Calibration** settings can be cloned to/from other nozzle tips, so you need to configure them only once. The **Touch Location** is automatically translated according to the **First Location** (along with the other locations). 

Marking this nozzle tip as the **Template** makes the **Touch Location Z** the global machine reference Z. 

Use the **Clone Tool Changer Settings to all Nozzle Tips** button to distribute the Z calibration settings to all the other nozzles. 

Use the  **Callibrate all Touch Locations' Z to Template** to reference them to the global machine reference Z.

See also the [Nozzle Tip Changer page](https://github.com/openpnp/openpnp/wiki/Nozzle-Tip-Changer#cloning-settings).

