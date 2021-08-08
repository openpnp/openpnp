## Operating Principle

The [[Computer Vision]] pipeline stage **DetectCircularSymmetry** searches the image for [Circular Symmetry](https://en.wikipedia.org/wiki/Circular_symmetry) i.e. for center points where the pixels of individual surrounding concentric rings show little change (variance) when compared to the overall variance in the same area. 

As an example for Circular Symmetry, consider an archery target:

![Archery target](https://user-images.githubusercontent.com/9963310/115282503-94247080-a14a-11eb-85ad-6f28ccc7ba6c.png) 

Source:  [Wikipedia about Circular Symmetry](https://en.wikipedia.org/wiki/Circular_symmetry)

The stage can be used to detect concentric things, like fiducials, sprocket holes, nozzle tips in runout calibration etc. 

As an input, it takes the expected diameter (minimum and maximum) of the feature to be detected. This is often a property that is well known or can easily be measured (using a caliper) without special knowledge about computer vision.

Unlike other stages like **DetectCirclesHough**, or the **Countours** family of stages, it does not require sharp edges, thresholds, filtering, footprint colors etc. internally or as a prerequisite. Instead, it simply **compares the image to itself**, so **it is entirely self-tuning**, and it can robustly work with very weak contrasts, very soft images and changing ambient lights, varried/inverted colors.  

## Example Images and Use Cases
Here is a series of images before/after detection. All these are with the same **standard pipeline**, only adjusted for nozzle tip/fiducial diameter, **no tuning**:

Runout calibration with dark Samsung CP40 nozzle tips:

![circular-symmetry-nozzle-tip](https://user-images.githubusercontent.com/9963310/115287362-7b1ebe00-a150-11eb-8452-99e132ef689b.gif)

Same pipeline with a very low contrast, very soft/out of focus nozzle tip:

![circular-symmetry-blur](https://user-images.githubusercontent.com/9963310/115287413-8a057080-a150-11eb-97e0-a4d29e204b7a.gif)

Off-center view for camera position/rotation calibration: The nozzle tip is seen slightly from the side which means the various features of the nozzle are no longer concentric. The air bore is also slightly asymmetric. By simply selecting the wanted min/max diameter range it automatically snaps to the right ring. Observe how the blue center point jumps:

![circular-symmetry-diameter-selection](https://user-images.githubusercontent.com/9963310/115287591-c042f000-a150-11eb-9730-4cb51530eade.gif)

The same exact pipeline used for a fiducial:

![circular-symmetry-fiducial](https://user-images.githubusercontent.com/9963310/115287645-cf29a280-a150-11eb-9cf6-c0d503b5d395.gif)

## Diagnostics

For diagnostic purposes you can enable a heat map, indicating the degree of symmetry:

![circular-symmetry-diagnostics](https://user-images.githubusercontent.com/9963310/115288880-457ad480-a152-11eb-9d2e-85d5abe70aeb.gif)

# Editing the Stage in the Pipeline Editor

The standard pipeline is very simple (nozzle tip calibration example):

![Pipeline Editor](https://user-images.githubusercontent.com/9963310/115291350-ed919d00-a154-11eb-97ff-167f423240b3.png)

The **DetectCircularSymmetry** stage is configured as follows:

## General Properties

**diagnostics**: Switches on an overlaid heat map of [Circular Symmetry](https://en.wikipedia.org/wiki/Circular_symmetry). 

**minSymmetry**: Minimum relative circular symmetry. Defaults to `1.2`. Any match below this ratio is considered invalid. The score value is calculated as the ratio of overall area pixel variance divided by the sum of pixel ring variances. These are variances weighed by area i.e. by pixel count. Because the image is compared against itself, this ratio is very stable, i.e. it should not be necessary to tune this.  

**subSampling**: To improve performance, only one pixel out of a block of subSampling × subSampling pixels is sampled. The preliminary winner is then locally searched using iteration with smaller and smaller subSampling size. The subSampling value will automatically be reduced for small diameters. Use BlurGaussian before this stage if subSampling suffers from interference/moiré effects. 

**superSampling**: Can be used to achieve sub-pixel final precision: 1 means no super-sampling, 2 means half sub-pixel precision etc. Practical up to ~8.

**maxTargetCount**: Maximum number of targets (e.g. sprocket holes) to be found. The targets with best symmetry score are returned. Overlap is automatically eliminated. 

**corrSymmetry**: Correlated minimum circular symmetry for multiple matches, i.e. other matches must have at least this relative symmetry. Note that the symmetry score is very dynamic, so setting a robust correlation is difficult. 

## Properties used when controlled by Vision Operations

**propertyName**: Sets the property name, as set by vision operations using this pipeline. Currently, this is `nozzleTip` for the nozzle tip calibration and `fiducial` for the fiducial locator. As soon as the named property is set by the vision operation, the stage is dynamically controlled and no longer uses the fixed properties in the stage itself. Proper camera Units Per Pixel scaling makes sure the dynamic properties work across machines/cameras/resolutions. 

**innerMargin**: The inner margin, relative to the diameter. Only used when the **propertyName** is set i.e. when the nominal diameter is supplied by vision operations. The **minDiameter** is then computed as `diameter*(1 - innerMargin)`.

**outerMargin**: The outer margin, relative to the diameter. Only used when the **propertyName** is set i.e. when the nominal diameter is supplied by vision operations. The **maxDiameter** is then computed as `diameter*(1 + outerMargin)`.

## Properties used when controlled by the Stage 

**minDiameter**: Minimum diameter, in pixels, of the wanted circular feature. This should always be a bit smaller than the actual diameter, because the symmetry analysis needs some margin area to detect circular image gradients. 

**maxDiameter**: Maximum diameter, in pixels, of the wanted circular feature. This should always be a bit larger than the actual diameter, because the symmetry analysis needs some margin area to detect radial image gradients. 

**maxDistance**: Maximum search distance (radius), in pixels, from the nominal location of the feature (usually from the center of the camera view). The search distance will be cropped to the image margin. 

**searchWidth**: Maximum search width, in pixels, across the nominal location of the feature. If 0 is given, 2 × **maxDistance** is taken i.e. there is no limit applied.

**searchHeight**: Maximum search height, in pixels, across the nominal location of the feature. If 0 is given, 2 × **maxDistance** is taken i.e. there is no limit applied.

## Vision Operations Control

To supply the wanted diameter and maximum search distance from the vision operations, some new GUI properties have been added and other properties parametrized to the stage. 

## Nozzle Tip Calibration 
![Nozzle Tip Calibration](https://user-images.githubusercontent.com/9963310/115760425-a51aef00-a3a1-11eb-867e-81e44b260686.png)

**Vision Diameter**: Must be set to the physical diameter of the feature you want to detect. Typically, a feature at the very point of the nozzle tip. Either the outer diameter or the air bore. 

The **maxDistance** property is automatically derived from the already present **Offset Threshold**.

Standard pipeline (Edit the pipeline and paste this using the ![Paste](https://user-images.githubusercontent.com/9963310/115295345-f0db5780-a159-11eb-826e-4fcabc1917e6.png) button):

```
<cv-pipeline>
   <stages>
      <cv-stage class="org.openpnp.vision.pipeline.stages.ImageCapture" name="0" enabled="true" default-light="false" settle-first="true" count="1">
         <light class="java.lang.Double">128.0</light>
      </cv-stage>
      <cv-stage class="org.openpnp.vision.pipeline.stages.ImageWriteDebug" name="1" enabled="false" prefix="runoutCalibration_source" suffix=".png"/>
      <cv-stage class="org.openpnp.vision.pipeline.stages.BlurGaussian" name="13" enabled="false" kernel-size="7"/>
      <cv-stage class="org.openpnp.vision.pipeline.stages.DetectCircularSymmetry" name="results" enabled="true" min-diameter="30" max-diameter="50" max-distance="200" max-target-count="1" min-symmetry="1.2" corr-symmetry="0.0" property-name="nozzleTip" outer-margin="0.2" inner-margin="0.4" sub-sampling="8" super-sampling="1" diagnostics="false"/>
      <cv-stage class="org.openpnp.vision.pipeline.stages.DrawCircles" name="6" enabled="true" circles-stage-name="results" thickness="2">
         <color r="255" g="0" b="51" a="255"/>
         <center-color r="0" g="204" b="255" a="255"/>
      </cv-stage>
      <cv-stage class="org.openpnp.vision.pipeline.stages.ImageWriteDebug" name="7" enabled="false" prefix="runoutCalibration" suffix=".png"/>
   </stages>
</cv-pipeline>
```

## Fiducial Locator
![Fiducial Locator](https://user-images.githubusercontent.com/9963310/115297291-5597b180-a15c-11eb-940f-47a3cd2fde15.png)

The **diameter** of the fiducial is automatically derived from the footprint. Therefore, the same underlying data can be used both for the classic `MatchPartTemplate` based pipeline and the new pipeline (obviously this only works if the ficucial is actually round). 

**Max. Distance**: Maximum allowed distance between nominal fiducial location and detected fiducial location. It controls the search distance in the stage. However, this is also active with other pipelines/stages i.e. it also acts as an added sanity check: If this distance is exceeded, the locator will throw an exception. 

Standard pipeline (Edit the pipeline and paste this using the ![Paste](https://user-images.githubusercontent.com/9963310/115295345-f0db5780-a159-11eb-826e-4fcabc1917e6.png) button):

```
<cv-pipeline>
   <stages>
      <cv-stage class="org.openpnp.vision.pipeline.stages.ImageCapture" name="image" enabled="true" default-light="true" settle-first="true" count="1"/>
      <cv-stage class="org.openpnp.vision.pipeline.stages.BlurGaussian" name="blur" enabled="false" kernel-size="3"/>
      <cv-stage class="org.openpnp.vision.pipeline.stages.DetectCircularSymmetry" name="circular" enabled="true" min-diameter="10" max-diameter="100" max-distance="200" min-symmetry="1.2" property-name="fiducial" outer-margin="0.2" inner-margin="0.4" sub-sampling="8" diagnostics="false"/>
      <cv-stage class="org.openpnp.vision.pipeline.stages.ConvertModelToKeyPoints" name="results" enabled="true" model-stage-name="circular"/>
      <cv-stage class="org.openpnp.vision.pipeline.stages.DrawCircles" name="draw" enabled="true" circles-stage-name="circular" thickness="1">
         <color r="255" g="255" b="0" a="255"/>
         <center-color r="255" g="153" b="0" a="255"/>
      </cv-stage>
      <cv-stage class="org.openpnp.vision.pipeline.stages.ImageWriteDebug" name="debug_results" enabled="false" prefix="fidloc_results_" suffix=".png"/>
   </stages>
</cv-pipeline>
```

## ReferenceStripFeeder 

The ReferenceStripFeeder sets the **diameter** to be detected, i.e. the sprocket hole diameter according to the EIA 481 standard. A relatively accurate camera units per pixel setting is required and the tape surface needs to be near the camera focal plane in Z. 

The ReferenceStripFeeder also controls the **maxDistance** search range: full camera scope for Auto Setup and an optimized minimal search range for routine detection of the nearest hole.


Edit the pipeline and paste this using the ![Paste](https://user-images.githubusercontent.com/9963310/122116892-74e25080-ce26-11eb-9f49-3a50c4359d7b.png) button:

```
<cv-pipeline>
   <stages>
      <cv-stage class="org.openpnp.vision.pipeline.stages.ImageCapture" name="original" enabled="true" default-light="true" settle-first="true" count="1"/>
      <cv-stage class="org.openpnp.vision.pipeline.stages.ImageWriteDebug" name="deb0" enabled="false" prefix="strip_" suffix=".png"/>
      <cv-stage class="org.openpnp.vision.pipeline.stages.BlurGaussian" name="predetect-1" enabled="false" kernel-size="5"/>
      <cv-stage class="org.openpnp.vision.pipeline.stages.DetectCircularSymmetry" name="results" enabled="true" min-diameter="10" max-diameter="100" max-distance="100" max-target-count="20" min-symmetry="1.2" corr-symmetry="0.25" property-name="sprocketHole" outer-margin="0.3" inner-margin="0.1" sub-sampling="8" super-sampling="2" diagnostics="false"/>
      <cv-stage class="org.openpnp.vision.pipeline.stages.ImageRecall" name="recalled" enabled="false" image-stage-name="original"/>
      <cv-stage class="org.openpnp.vision.pipeline.stages.DrawCircles" name="display" enabled="true" circles-stage-name="results" thickness="1">
         <color r="255" g="0" b="0" a="255"/>
      </cv-stage>
      <cv-stage class="org.openpnp.vision.pipeline.stages.ImageWriteDebug" name="deb1" enabled="false" prefix="strip_result_" suffix=".png"/>
   </stages>
</cv-pipeline>

```

This was tested with deliberately difficult contrast and reflective background. The inner margin is set to only 0.05, forcing the stage to detect the outer edge, i.e. perspective and shadow bias is effectively minimized: 

![Strip Feeder](https://user-images.githubusercontent.com/9963310/122117060-afe48400-ce26-11eb-8317-e5020655427b.png)

A deliberately bent strip was perfectly followed, zero mis-detects:

![Bent Strip](https://user-images.githubusercontent.com/9963310/122117195-daced800-ce26-11eb-9a58-c42cd848de2e.jpg)


## ReferencePushPullFeeder

The ReferencePushPullFeeder sets the **diameter** to be detected, i.e. the sprocket hole diameter according to the EIA 481 standard. A relatively accurate camera units per pixel setting is required and the tape surface needs to be near the camera focal plane in Z. 

The ReferencePushPullFeeder also controls the **maxDistance** search range: full camera scope for Auto Setup and an optimized minimal search range for routine detection of the two reference holes.

Edit the pipeline and paste this using the ![Paste](https://user-images.githubusercontent.com/9963310/122116892-74e25080-ce26-11eb-9f49-3a50c4359d7b.png) button:

```
<cv-pipeline>
   <stages>
      <cv-stage class="org.openpnp.vision.pipeline.stages.ImageCapture" name="0" enabled="true" default-light="true" settle-first="true" count="1"/>
      <cv-stage class="org.openpnp.vision.pipeline.stages.BlurGaussian" name="1" enabled="true" kernel-size="5"/>
      <cv-stage class="org.openpnp.vision.pipeline.stages.AffineWarp" name="11" enabled="true" length-unit="Millimeters" x-0="0.0" y-0="0.0" x-1="0.0" y-1="0.0" x-2="0.0" y-2="0.0" scale="1.0" rectify="true" region-of-interest-property="regionOfInterest"/>
      <cv-stage class="org.openpnp.vision.pipeline.stages.ConvertColor" name="12" enabled="true" conversion="Bgr2Gray"/>
      <cv-stage class="org.openpnp.vision.pipeline.stages.SimpleOcr" name="OCR" enabled="true" alphabet="0123456789.-+_RCLDQYXJIVAFH%GMKkmuµnp" font-name="Liberation Mono" font-size-pt="7.0" font-max-pixel-size="20" auto-detect-size="false" threshold="0.75" draw-style="OverOriginalImage" debug="false"/>
      <cv-stage class="org.openpnp.vision.pipeline.stages.ImageRecall" name="20" enabled="true" image-stage-name="0"/>
      <cv-stage class="org.openpnp.vision.pipeline.stages.DetectCircularSymmetry" name="results" enabled="true" min-diameter="10" max-diameter="100" max-distance="100" max-target-count="10" min-symmetry="1.2" corr-symmetry="0.2" property-name="sprocketHole" outer-margin="0.2" inner-margin="0.2" sub-sampling="8" super-sampling="1" diagnostics="false"/>
      <cv-stage class="org.openpnp.vision.pipeline.stages.ImageRecall" name="30" enabled="false" image-stage-name="0"/>
      <cv-stage class="org.openpnp.vision.pipeline.stages.DrawCircles" name="2" enabled="false" circles-stage-name="results" thickness="1">
         <color r="255" g="0" b="0" a="255"/>
      </cv-stage>
   </stages>
</cv-pipeline>
```

Tested with difficult transparent tape, as always zero tuning required:

![Push Pull Feeder](https://user-images.githubusercontent.com/9963310/122117329-08b41c80-ce27-11eb-8398-73b6b5d39b19.png)

The exact same pipeline also works with a paper tape:

![Paper tape](https://user-images.githubusercontent.com/9963310/122117831-a0b20600-ce27-11eb-826d-f80f2365714b.png)
 
Note, the pipeline no longer requires a saturated "green-screen" color, it just uses whatever contrast/color difference there is. But it _does_ account for all color channels in the symmetry computation. 
