# What is it?

Computer Vision is used throughout OpenPnP to detect exactly where things are in relation to the cameras, sometimes to just determine _if_ they are there or not, and sometimes to read text (OCR) or codes (QR-Codes). Computer vision mostly uses a facility called Pipeline to perform the detection. The Pipeline is structured in so-called Stages that the user can configure in a special Pipeline Editor. 

This page is dedicated to explain some of the stages in more detail. The usage of the Pipeline Editor UI however is explained on the [[CvPipeline]] page.

# Stages

## DetectCircularSymmetry

### Operation Principle

DetectCircularSymmetry searches the image for [Circular Symmetry](https://en.wikipedia.org/wiki/Circular_symmetry) i.e. for center points where the pixels of individual surrounding concentric rings show little change (variance) when compared to the overall variance in the same area. 

As an example for Circular Symmetry, consider an archery target:

![Archery target](https://user-images.githubusercontent.com/9963310/115282503-94247080-a14a-11eb-85ad-6f28ccc7ba6c.png) 

Source:  [Wikipedia about Circular Symmetry](https://en.wikipedia.org/wiki/Circular_symmetry)

The stage can be used to detect concentric things, like fiducials, sprocket holes, nozzle tips in runout calibration etc. 

As an input it takes the expected diameter (minimum and maximum) of the feature to be detected. This is often a property that is well known or can easily be measured (using a caliper) without special knowledge about computer vision.

Unlike other stages like **DetectCirclesHough**, or the **Countours** family of stages, it does not require sharp edges, thresholds, filtering, footprint colors etc. internally or as a prerequisite. Instead, it simply **compares the image to itself**, so **it is entirely self-tuning**, and it can robustly work with very weak contrasts, very soft images and changing ambient lights, varried/inverted colors.  

### Example images
Here is a series of images before/after detection. All these are with the same **standard pipeline**, only adjusted for nozzle tip/fiducial diameter, **no tuning**:

Runout calibration with dark Samsung CP40 nozzle tips:

![circular-symmetry-nozzle-tip](https://user-images.githubusercontent.com/9963310/115287362-7b1ebe00-a150-11eb-8452-99e132ef689b.gif)

Same pipeline with a very low contrast, very soft/out of focus nozzle tip:

![circular-symmetry-blur](https://user-images.githubusercontent.com/9963310/115287413-8a057080-a150-11eb-97e0-a4d29e204b7a.gif)

Off-center view for camera position/rotation calibration: The nozzle tip is seen slightly from the side which means the various features of the nozzle are no longer concentric. The air bore is also slightly asymmetric. By simply selecting the wanted min/max diameter range it automatically snaps to the right ring. Observe how the blue center point jumps:

![circular-symmetry-diameter-selection](https://user-images.githubusercontent.com/9963310/115287591-c042f000-a150-11eb-9730-4cb51530eade.gif)

The same exact pipeline used for a fiducial:

![circular-symmetry-fiducial](https://user-images.githubusercontent.com/9963310/115287645-cf29a280-a150-11eb-9cf6-c0d503b5d395.gif)

### Diagnostics

For diagnostic purposes you can enable a heat map, indicating the degree of symmetry:

![circular-symmetry-diagnostics](https://user-images.githubusercontent.com/9963310/115288880-457ad480-a152-11eb-9d2e-85d5abe70aeb.gif)

The standard pipeline is very simple (nozzle tip calibration example):

![Pipeline Editor](https://user-images.githubusercontent.com/9963310/115291350-ed919d00-a154-11eb-97ff-167f423240b3.png)

The **DetectCircularSymmetry** stage is configured as follows:

### General Properties

**diagnostics**: Switches on an overlaid heat map of [Circular Symmetry](https://en.wikipedia.org/wiki/Circular_symmetry). 

**minSymmetry**: Minimum relative circular symmetry. Defaults to `1.2`. Any match below this ratio is considered invalid. The score value is calculated as the ratio of overall area pixel variance divided by the sum of pixel ring variances. These are variances weighed by area i.e. by pixel count. Because the image is compared against itself, this ratio is very stable, i.e. it should not be necessary to tune this.  

**subSampling**: To improve performance, only one pixel out of a block of subSampling × subSampling pixels is sampled. The preliminary winner is then locally searched using iteration with smaller and smaller subSampling size. The subSampling value will automatically be reduced for small diameters. Use BlurGaussian before this stage if subSampling suffers from interference/moiré effects. 

### Properties used when controlled by Vision Operations

**propertyName**: Sets the property name, as set by vision operations using this pipeline. Currently, this is `nozzleTip` for the nozzle tip calibration and `fiducial` for the fiducial locator. As soon as the named property is set by the vision operation, the stage is dynamically controlled and no longer uses the fixed properties in the stage itself. Proper camera Units Per Pixel scaling makes sure the dynamic properties work across machines/cameras/resolutions. 

**innerMargin**: The inner margin, relative to the diameter. Only used when the **propertyName** is set i.e. when the nominal diameter is supplied by vision operations. The **minDiameter** is then computed as `diameter*(1 - innerMargin)`.

**outerMargin**: The outer margin, relative to the diameter. Only used when the **propertyName** is set i.e. when the nominal diameter is supplied by vision operations. The **maxDiameter** is then computed as `diameter*(1 + outerMargin)`.

### Properties used when controlled by the Stage 

**minDiameter**: Minimum diameter, in pixels, of the wanted circular feature. This should always be a bit smaller than the actual diameter, because the symmetry analysis needs some margin area to detect circular image gradients. 

**maxDiameter**: Maximum diameter, in pixels, of the wanted circular feature. This should always be a bit larger than the actual diameter, because the symmetry analysis needs some margin area to detect radial image gradients. 

**maxDistance**: Maximum search distance, in pixels, from the nominal location of the feature (usually from the center of the camera view). 

## Vision Operations Control

To supply the wanted diameter and maximum search distance from the vision operations, some new GUI properties have been added: 

### Nozzle Tip Calibration 
![Nozzle Tip Calibration](https://user-images.githubusercontent.com/9963310/115760425-a51aef00-a3a1-11eb-867e-81e44b260686.png)

**Vision Diameter**: Must be set to the physical diameter of the feature you want to detect. Typically, a feature at the very point of the nozzle tip. Either the outer diameter or the air bore. In the future, this could also be used to control Hough circle diameters or circle masks etc.

The **maxDistance** property is automatically derived from the already present **Offset Threshold**.

Standard pipeline (Edit the pipeline and paste this using the ![Paste](https://user-images.githubusercontent.com/9963310/115295345-f0db5780-a159-11eb-826e-4fcabc1917e6.png) button):

```
<cv-pipeline>
   <stages>
      <cv-stage class="org.openpnp.vision.pipeline.stages.ImageCapture" name="0" enabled="true" default-light="false" settle-first="true" count="1">
         <light class="java.lang.Double">128.0</light>
      </cv-stage>
      <cv-stage class="org.openpnp.vision.pipeline.stages.BlurGaussian" name="13" enabled="false" kernel-size="7"/>
      <cv-stage class="org.openpnp.vision.pipeline.stages.DetectCircularSymmetry" name="results" enabled="true" min-diameter="30" max-diameter="50" max-distance="200" min-symmetry="1.2" property-name="nozzleTip" outer-margin="0.2" inner-margin="0.4" sub-sampling="8" diagnostics="false"/>
      <cv-stage class="org.openpnp.vision.pipeline.stages.DrawCircles" name="6" enabled="true" circles-stage-name="results" thickness="2">
         <color r="255" g="0" b="51" a="255"/>
         <center-color r="0" g="204" b="255" a="255"/>
      </cv-stage>
      <cv-stage class="org.openpnp.vision.pipeline.stages.ImageWriteDebug" name="7" enabled="false" prefix="runoutCalibration" suffix=".png"/>
   </stages>
</cv-pipeline>
```

### Fiducial Locator
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
 


| Previous Step                 | Jump To                 | Next Step                                   |
| ----------------------------- | ----------------------- | ------------------------------------------- |
| [Camera Lighting](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Camera-Lighting) | [Table of Contents](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration) | [Next Steps](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Next-Steps) |