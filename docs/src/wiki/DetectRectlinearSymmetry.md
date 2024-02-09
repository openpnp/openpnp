## What is it?

The most important OpenPnP computer vision task is Alignment, or "Bottom Vision" to find the precise angle and position of parts. 

By default this is done by isolating the brightest elements of the part (the shiny contacts) and then finding the minimum area rectangle that bounds these. This works well, but it requires setting thresholds etc. to isolate the brightest elements, which can vary between packages, machines, cameras etc. 

This stage adds a new way to detect the parts that does not rely on specific thresholds etc. It exploits the fact, that in typical projects, an _overwhelming majority_ of the parts/packages have a large degree of mirror symmetry (or [reflection symmetry](https://en.wikipedia.org/wiki/Reflection_symmetry)), i.e. their left side is a mirror image of the right side, and/or their upper side is a mirror image of the lower side. 

Symmetry has the nice property that it is self-contained, i.e. we only need to compare the image to itself, no outside information (thresholds, template images, etc.) is needed and fewer assumptions (that contacts always appear bright, that the overall outline is well defined by a bounding rectangle etc.) are required to be true.

The mirror symmetry is present left-right and upper-lower, i.e. in the two 90° dimensions of the part, hence we call it Rectlinear Symmetry. 

## Operation Principle

The new stage basically works in two phases:

1. Firstly, it determines the angle of the subject, by analyzing 90° cross-sections of the image at varied angles (in reality this happens iteratively), and then taking the one with the largest contrast. It is a bit like shooting x-rays across the subject, and looking for the crispiest result. The following animation shows how this works. Cross-sections are indicated in blue, the yellow graph indicates the recorded contrast at various angles:

    ![find-maximum-angular-contrast-small-ic](https://user-images.githubusercontent.com/9963310/156884621-110f2ef0-f49b-4e97-bda0-171060e6d1ed.gif)

2. Secondly, the resulting cross-sections (at the best angle) are analyzed for symmetry. The stage will select a mid-point for each 90° cross-section that shows the best-matching left-right and upper-lower mirror image. 

For truly symmetric parts, the stage is completely tuning free. Assuming there are no disruptive patterns in the background (and we took care of that in [[Nozzle Tip Background Calibration]]) it will be very robust in determining the alignment. 

The stage is robust against irregularities, because it works in a _probabilistic_ way. There are no hard edges and thresholds involved where the image is broken down into either black or white. Instead, the _most likely_ match is detected from the _integral_ image information. For this reason the bent pin in the following example will not disrupt the bounding box, as it would in the default OpenPnP pipeline. These irregularities, and their robust suppression can make a crucial difference in placement accuracy: 

![Irregular pin](https://user-images.githubusercontent.com/9963310/156891569-e7299dd2-e312-447c-a969-bc1f8da9a863.png)

Rect-linear symmetry does not imply a rectangular contact arrangement, parts could well be cross-shaped, for instance. Furthermore, a certain tolerance is also implied, parts with fewer/smaller pins on one side (e.g. SOT23-5) are not a problem. The cross-section symmetry is not perfect, but still the best match: 

![Semi-asymmetric](https://user-images.githubusercontent.com/9963310/156913390-152f11be-314e-4dbf-9b40-a6397744441e.png)


## Nozzle Tip Configuration

As a prerequisite, you need to configure (new) settings on each nozzle tip: 

![Max. Pick Tolerance](https://user-images.githubusercontent.com/9963310/156925117-1c2fbd35-3003-4354-96bd-6481b9920342.png)

**Min. Part Diameter** sets the minimum part dimension assumed to be picked with this nozzle tip. This diameter is used to calculate an _inner_ diameter of the nozzle tip that will always be covered by the part (**Min. Part Diameter** minus two times the **Maximum Pick Tolerance**). The pixels in the _inner_ diameter are then disregarded/blotted-out  by the [Background Calibration](/openpnp/openpnp/wiki/Nozzle-Tip-Background-Calibration).

**Max. Part Diameter** sets the maximum part dimensions (diagonal) assumed to be picked with this nozzle tip. This diameter is used to limit the image area that is taken into consideration. This is also shown as a black mask using MaskCircle. Note: this diameter is also important to limit the computation time of the stage. You should really reduce it as far as possible. 

**Maximum Pick Tolerance** sets the maximum allowed distance of the detected part, from its expected location. If it is larger than the given distance, an error will be thrown. This also controls the pipeline, by limiting the maximum search distance and lessening the computational cost. 

## Getting the new Rectlinear Pipeline

Go to the Vision tab, select the Rectlinear Symmetry entry and copy the pipeline:

![Copy](https://user-images.githubusercontent.com/9963310/156918197-942066ff-f217-45c3-a522-b3d00fc07aa6.png)

Then go to the Part, Package where you want to try this, and press **Specialize for ...**. Or create a new Vision Setting for it.

Then paste the pipeline:

![Paste](https://user-images.githubusercontent.com/9963310/156918295-473d96c6-5e18-40c6-b9b3-297c916f7404.png)

## Using with Symmetric Parts

For symmetric parts, assigning the pipeline should be all that is needed. The part detection should just work! 

OK, I know, this is a "bold promise 😁", please report any problem to the [OpenPnP discussion group](http://groups.google.com/group/openpnp). 

## Using with Asymmetric Parts

There seems to be a trend towards more symmetry, as parts get miniaturized and large current pins are implemented by just using _many_ regular pins/pads/balls. So if you have an asymmetric part, maybe checking for new packages could be a sensible first step.

Unfortunately, some parts just aren't symmetric and never will be. For those, the stage can be made to work with some extra tweaking. Because there is no symmetry, we lose the self-tuning property, i.e., we must adjust the threshold and optionally the minimum detail size, as shown in the animation below:

![Asymmetric-rectlinear](https://user-images.githubusercontent.com/9963310/156898693-71453ea2-43ad-4bd6-b102-64453e6c22e8.gif)

1. Specialize the asymmetric part or package or create a new Vision Setting to handle all these together.
2. Switch the left-right and/or upper/lower symmetry off. 
3. Adjust the **Threshold** to capture the span of the asymmetric dimension.
4. Optionally adjust the **Min. Detail Size** to suppress artifacts that are not relevant.

# Advanced Use and Authoring

## Automatic Bottom Vision Pipeline Control

The Bottom Vision pipeline is parametrized by OpenPnP, i.e. some stage properties are controlled from the calling function:

- Background removal is controlled as described on the [[Nozzle Tip Background Calibration]] page.
- The location where the DetectRectlinearSymmetry stage searches for the part is controlled through the "DetectRectlinearSymmetry.center" pipeline property. Typically just the center of the camera. 
- The **expectedAngle** property of the DetectRectlinearSymmetry stage is controlled through the "DetectRectlinearSymmetry.expectedAngle" pipeline property. This is set to the nominal alignment angle.
- The **searchDistance** property of the DetectRectlinearSymmetry stage is controlled through the "DetectRectlinearSymmetry.searchDistance" pipeline property. This is taken from the Nozzle Tip Configuration **Max. Pick Tolerance**, [see above](#nozzle-tip-configuration).
-  The **maxWidth** and **maxHeight** properties of the DetectRectlinearSymmetry stage are controlled through the "DetectRectlinearSymmetry.maxWidth" and "DetectRectlinearSymmetry.maxHeight" pipeline properties, respectively. If the **Part size check** is enabled on the Vision Settings, the effective part size is taken (with the **Size tolerance** added). If no part size info is present, the **Max. Part Diameter** from the nozzle tip is taken.
- The **subSampling** property of the DetectRectlinearSymmetry stage is controlled through the "DetectRectlinearSymmetry.subSampling" pipeline property. This is set to half the **Minimum Detail Size**, as configured on the [[Nozzle Tip Background Calibration]].

In addition, the visible parameters are obviously controlled too. Please consult the [Pipeline Parameters](https://github.com/openpnp/openpnp/wiki/Exposed-Pipeline-Parameters) inside the pipeline to see how.

## Configuring the DetectRectlinearSymmetry Stage

![DetectRectlinearSymmetry Stage](https://user-images.githubusercontent.com/9963310/156915932-cadb0291-4c2d-4d7e-a908-71acd5997132.png)

**expectedAngle** tells the stage the expected angle of subject to be detected. 

**searchDistance** sets the search distance around the center.

**searchAngle** sets the search angle (two-sided) around the expected angle.

**maxWidth** sets the maximum cross-section width of the subject to be detected.

**maxHeight** sets the maximum cross-section height of the subject to be detected.

**symmetricLeftRight** tells the stage whether the subject is left/right-symmetric (in the sense as seen at 0° subject rotation). According to this switch, the stage either takes the **symmetricFunction**, or the **asymmetricFunction**.

**symmetricUpperLower** tells the stage whether the subject is upper/lower-symmetric (in the sense as as seen at 0° subject rotation). According to this switch, the stage either takes the **symmetricFunction**, or the **asymmetricFunction**.

**symmetricFunction** determines how the cross-section is evaluated for **symmetric** subjects. 

- **FullSymmetry** looks for full inner and outline symmetry. Use for truly symmetric subjects and best precision.
- **EdgeSymmetry** looks for full inner and outline symmetry of edges. Use for partially symmetric subjects, where some, but not all features are present on both sides, or where shades differ.
- **OutlineSymmetry** looks for outline symmetry only. Used for subjects that are symmetric on their outline, but not on the inside.
- **OutlineEdgeSymmetry** looks for outline symmetry of edges only. Used for subjects that are symmetric on their outline, but not on the inside, and where some, but not all features are present on both sides, or where shades differ.
- **OutlineSymmetryMasked** looks for outline mask symmetry only. Use for quite asymmetric subjects. Requires setting a mask threshold.

**asymmetricFunction** determines how the cross-section is evaluated for **asymmetric** subjects. The choice is the same as for **symmetricFunction**.

**minSymmetry** minimum relative symmetry. Values larger than 1.0 indicate symmetry. The value is highly image-dependent and can only be used for very individual settings.

**subSampling** is used to speed things up: Only one pixel out of a square of **subSampling** × **subSampling** pixels is sampled. The best region is then locally searched using iteration with smaller and smaller **subSampling** size. The **subSampling** value will automatically be reduced when other search properties require it. Use **BlurGaussian** before this stage if **subSampling** suffers from moiré effects. 

**superSampling** can be used to achieve sub-pixel final precision: 1 means no super-sampling, 2 means half-pixel precision etc. Negative values can be used to stop refining **subSampling**, -2 means it will stop at a 2-pixel resolution.

**smoothing** is applied to the sampled cross-sections. Given as the Gaussian kernel size. This is needed to eliminate interferences, when angular sampling coincides with the pixel raster or its diagonals.

**gamma** determines the [Gamma correction](https://en.wikipedia.org/wiki/Gamma_correction) to be applied to the image. The input signal is raised to the power of **gamma**. With **gamma** > 1.0 the bright image parts are emphasized.

**threshold** is only used when the **OutlineSymmetryMasked** function is active. Only pixels with luminance greater than the **threshold** are considered when determining the outline of the part.

**minFeatureSize** is only used, when the **OutlineSymmetryMasked** function is active. Pixels are masked by the **threshold** property, and a cross-section pixel count over these masked pixels is then determined. A cross-section bin only counts as "detected" when the pixel count is larger than **minFeatureSize**. This is used to remove masking imperfections, i.e. image specks and impurities up to a certain size and frequency.

**propertyName** determines the pipeline property name under which this stage is controlled by the vision operation. If set, these will override some of the properties configured here. Defaults to "alignment" for Bottom Vision.

**diagnostics**, if enabled, indicates the detection with cross-hairs and bounds.

**diagnosticsMap** , if enabled, overlays a diagnostic map indicating the angular reclinear contrast and rectlinear cross-section.

