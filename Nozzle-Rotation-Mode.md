## What is it?

The first thing to realize, is that _only_ the nozzle rotation between the moment it picks a part and the moment it places it, matters. The absolute angle of the nozzle at the time of the pick _is irrelevant_, _only_ the _relative_ rotation _while it carries a part_ matters. 

The **Rotation Mode** can now exploit these facts and optimize the way a nozzle rotates between the pick and place. This can serve the following purposes:

1. Support machines that have nozzles with restricted angular articulation, i.e. that cannot articulate the full 360°. Machines with a bit more than 180° are supported by exploiting the available _relative_ rotation between pick and placement. 

    ![rotation-mode](https://user-images.githubusercontent.com/9963310/137022424-6ec3ce82-5983-4ef8-b0e7-890bb6c00898.png)

    In _practice_, we need a bit more than 180° (say 200°) minimum articulation, as bottom vision needs some extra leeway on top of the _theoretical_ maximum relative rotation, to be able to align parts properly.

2. Even for machines with >= 360° articulation, it can make sense to limit the articulation to a minimum, e.g. for nozzles with tubing that is not free-running. This will reduce the stress on the tubing and also somewhat optimize rotation times.

3. Optimize the performance of shared rotation (`C`) axes machines: Parts are picked at angular offsets that make them pre-rotated for placement, i.e. all the nozzles are subsequently pre-rotated for alignment (bottom vision) and placement at their shared 0° position. No large rotations are therefore needed for alignment and placement (only the small _and fast_ adjustments from bottom vision). This means that the more precise pre-rotate bottom vision is available to these machines without performance penalty. 

4. For regular machines (with free-running tubing, i.e. unlimited articulation), and for very repetitive applications (e.g. in LED panel assembly), there is yet another optimization available that simply picks the part with whatever rotation the nozzle happens to have at the time of the pick. Alignment and placement are then rotated _relative_ to that. This will speed up operation if multiple nozzles pick from the same feeder, or from feeders that are very close together, i.e. where the move time before and between picks is not enough to rotate the nozzle fast enough. 

## Setting up the Nozzle Rotation Axis

### Rotational Axis Limits

In the [ReferenceControllerAxis](https://github.com/openpnp/openpnp/wiki/Machine-Axes#referencecontrolleraxis) configuration, as soon as **Limit to Range** is enabled, the **Soft Limit Low** and **Soft Limit High** are displayed:

![ReferenceControllerAxis](https://user-images.githubusercontent.com/9963310/136701242-6cbf8e36-637c-4ee2-8eda-6e1b535f0e88.png)

The limits can be captured etc. as with linear axes. If no soft limits are defined, the axis defaults to the -180° ... +180° range.

Rotational limits can be used to address these use cases:

1. Define the specific angular range for machines with limited articulation (< 360°). See the intro section above. 

2. For machines that can reach 360° and more buts still have restricted rotation (e.g. tubing that is not free-running), a more relaxed custom range can be defined, e.g. -200° ... 200°, avoiding "pirouetting" (rotating the nozzle all the way around), if the placement angle happens to be near the ±180° default wrap-around limit. A shifted custom range, like -160° ... 200° can at least avoid "pirouetting" at the common 180° placement angle, i.e. pirouetting would only occur at the rare 200° angle. 

3. Define the range for axes that are transformed, i.e. where moving 360 axis units does not result in a 360° rotation. See next section.

**Note:** if the articulation is effectively limited to less than 360°, the **Wrap-around** option is not allowed. An error will be reported, when the motion planner attempts to wrap-around into the unreachable range. Issues & Solution will also report this misconfiguration and suggest to switch it off.

### Rotational Axis Transformations

You can use [[Transformed Axes]] for rotation, the example shown here establishes a 1:10 scaling between the controller axis and the transformed axis: 

![Mapped](https://user-images.githubusercontent.com/9963310/136701340-0c24bedc-fb9e-4723-9af3-b2b505e794d5.png)

The transformed axis can then be assigned to the Nozzle:

![Nozzle axis mapping](https://user-images.githubusercontent.com/9963310/136701429-4c9e7155-92b0-4450-ab91-a4e64a4db755.png)

## Setting up the Nozzle

### Setting the Rotational Mode on the Nozzle

The **Rotation Mode** is configured on the Nozzle:

![Rotation Mode](https://user-images.githubusercontent.com/9963310/136701468-7a676042-54d2-44ed-8030-269cd15382e4.png)

In the axis configuration example (that was explained in the previous sections), we have a controller axis that rotates from -1 ... 19, which is 1:10 transformed to a -10° ... 190° angular range. This is a 200° range to allow for the needed ±180° minmum articulation plus ±20° bottom vision part alignment offsets. The range is less than 360°, therefore the **LimitedArticulation** mode must be configured on the nozzle.

### Rotation Modes

The **Rotation Mode** entries work as follows:

- **AbsolutePartAngle**: the part is picked at its absolute angle, i.e. if the nozzle is at 0°, the part is rotated as drawn in the E-CAD library. This is the default and legacy mode, no angular offset is applied. In this mode, the nozzle must be able to articulate 360° or more. 

- **PlacementAngle**: the part is picked with the placement angle compensated, i.e. if the nozzle is at 0°, the part is rotated as it will be aligned and placed. You must enable the **Pre-Rotate** option in bottom vision, for this to make sense. This is optimal for shared C axes. In this mode, the nozzle must be able to articulate 360° or more. 

- **MinimalRotation**: the part is picked at whatever angle the nozzle happens to be at, i.e. no rotation is needed prior to the pick. Alignment and placement are done at _relative_ angles. This might speed up operation if multiple nozzles pick from the same feeder many, many times (e.g. in LED panel assembly), or from feeders that are close together, i.e. where the move time before and between picks is not enough to rotate nozzles fast enough. In this mode, the nozzle must be able to articulate 360° or more. 

- **LimitedArticulation**: the articulation of the pick-to-placement rotation is centered around the midpoint of whatever angular range is available (taking into account a margin for alignment offsets). For the pick, the nozzle is automatically rotated towards either the minimum or maximum range limit, to allow for positive or negative relative rotation, as needed. 

   ![rotation-mode](https://user-images.githubusercontent.com/9963310/137022424-6ec3ce82-5983-4ef8-b0e7-890bb6c00898.png)

  Once the part is picked, only the placement angle is guaranteed to be reachable. This means you _must_ enable the **Pre-Rotate** option [in bottom vision](https://github.com/openpnp/openpnp/wiki/Bottom-Vision#global-configuration). In this mode, the nozzle must be able to articulate 180° plus an allowance for part alignment offsets, e.g. 200° or more. This mode can also make sense for nozzles that can reach 360°, but you still want to limit the articulation to a minimum, to ease the stress on tubes etc. and to avoid "pirouetting", that you might get with the default **AbsolutePartAngle** method. 

### Align Nozzle Rotation with Part

Regardles of the **Rotation Mode**, it is recommended to enable the **Align with Part?** option on the nozzles:

![Align with part switch](https://user-images.githubusercontent.com/9963310/178480834-200e20d4-8640-46c4-8143-14903fdadc3d.png)

It offsets the nozzle rotation coordinate to match that of the part, once the part is aligned in bottom vision. This means that the cross-hairs and the DRO are precisely aligned with the part. 

## Working with Limited Articulation

This section describes working with a nozzle that has a limited articulation, i.e. less than 360°, and the **LimitedArticulation** rotation mode set.

Obviously you can no longer rotate the nozzle all the way. Without a part picked, the nozzle will articulate in its native range. If you try to jog beyond the range, an error is displayed. This does not apply to the camera virtual axis, so you should still be able to capture any rotation, e.g. from feeders. However, you might get errors when switching from camera to nozzle. 

![Limited Rotation Error](https://user-images.githubusercontent.com/9963310/136702778-f7300cdb-c1bc-4b61-99ad-a63d2d9038ed.png)

Once a part is picked, the angular offset needed for placement articulation is taken into account (see the **LimitedArticulation** mode description). The effective _part_ angle is displayed as expected in the DRO and through the cross-hairs of the bottom camera view, i.e. the rotation offset is hidden from the user (use the log to see at what absolute angles the controller axis is driven). 

### OpenPnP Support for Limited Articulation

- [[Issues and Solutions]] supports the proper setup of limited articulation Axes and Nozzles by pointing out illegal configurations and suggesting the right solution:

    ![Issues and Solutions and the Rotation Mode](https://user-images.githubusercontent.com/9963310/137872316-95dfd12d-4426-48eb-87a4-a131dcd6b497.png)

- Inside a Job, the pick-to-place articulation is automatically controlled. 

- ![Pick Button](https://user-images.githubusercontent.com/9963310/136702931-477b112a-bfdf-4ee1-a5da-ff68cd6614bd.png) The **Pick** action in the Parts and Feeders panels takes the **Test Alignment Angle** on the part alignment settings into account, to predict the needed articulation for alignment. Again, you _must_ enable the **Pre-Rotate** option [in bottom vision](https://github.com/openpnp/openpnp/wiki/Bottom-Vision#global-configuration). 

   ![Pick Action](https://user-images.githubusercontent.com/9963310/136702905-0b975517-62ef-4893-bd86-9ee5e86d1141.png)
 
- The **Discard** cycle will not try to rotate the part for discard.
 
- [Issues & Solutions precision nozzle-to-head offset calibration](https://github.com/openpnp/openpnp/wiki/Calibration-Solutions#calibrating-precision-camera-to-nozzle-offsets) properly prepares the angular articulation.
 
- The rotation `P` button on the jog controls will now move to the 90° step angle nearest to the mid-position of a limited articulation nozzle (Note: this angle can vary, when a part is on the nozzle, i.e. when a `RotationMode` offset is applied).

- Nozzle tip calibration will respect rotation axis limits, i.e. it will perform the runout calibration using whatever angular range is available (e.g. only half-sided). Thanks to @tonyluken's superb Affine Transformation method, this seems not to be a problem. 

**NOTE:** There might be other functions where limited articulation is not yet respected, especially when a rotation is applied to the nozzle were it does not actually matter. Please help with the improvement of OpenPnP and report such functions to the [discussion group](http://groups.google.com/group/openpnp). 
