## What is it?

Background Calibration analyzes the empty nozzle tip and background in the bottom camera. The obtained baseline information is later used to remove the background in [[Bottom Vision]]. The Background Calibration is automatically (and repeatedly) performed during the [[Nozzle Tip Calibration|Nozzle-Tip-Calibration-Setup]]. 

## Calibration Operating Principle

Background Calibration reuses the images that are already captured by the [[Nozzle Tip Calibration|Nozzle-Tip-Calibration-Setup]], so no extra machine time and motion is needed to obtain them. 

For Juki (or similar) nozzle tips, it analyzes the background images for the **Key Color** (Chroma Key). Any dominant, vivid color is automatically detected and a likely bounding box computed in the [HSV Color Model](https://en.wikipedia.org/wiki/HSL_and_HSV). This step is optional.

For all nozzle types, a cutoff brightness is determined where the non-color-keyed parts of the background must all be darker. When we later detect the bright contacts of parts in bottom vision, they must all be significantly brighter. This gives us the _minimal_ brightness cutoff threshold (typically applied in a `Threshold` stage). 

The background calibration automatically ignores a round area in the calibration image, where the nozzle tip was detected. This will eliminate any shiny elements that are typically present on the tip (metallic needle, worn-off tip, etc.). OpenPnP assumes this center area to be always covered by the picked part, i.e. there is no need to include its color in the background calibration. Furthermore, any shiny spots in this area are not treated as a problem (see the [Trouble Shooting](#trouble-shooting) section). The blotted-out area has a size of **Min. Part Diameter** - 2 x **Max. Pick Tolerance** (see the [Nozzle Tip Configuration for bottom vision](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration:-Nozzle-Setup#nozzle-tip-configuration)).

The resulting calibration is also validated, and if the detected background quality is not good, diagnostic guidance and visual feed-back is provided for users to remedy the situation (see section [Trouble Shooting](#trouble-shooting) below).

## Calibration Pipeline Control

If the Background Calibration is enabled, the following is controlled in the bottom vision pipeline:

1. The `BlurGaussian` `kernelSize` property is controlled by a new **Minimum Detail Size** field, where the user can enter a length (in system units) that indicates the smallest sensible size of a feature on a part (such as the dimension of a pad, pin, ball of a package). The GaussianBlur is configured accordingly (to half of that size), to suppress image noise, dirt, texture etc. that is not relevant for detection. This is also applied to the Background Calibration itself, to get the same `MaskHSV` input data as the pipeline.
2. The `MaskCircle` `diameter` property is always controlled by the already existing **Max. Part Diameter** field on the Nozzle tip (this is also true, when the background calibration is not enabled). Peripheral background content that might disrupt the vision operation is masked out. This is also applied to the Background Calibration itself, to get the same `MaskHSV` input data as the pipeline.
3. The `MaskHSV` `hueMin`, `hueMax`, `saturationMin`,  `saturationMax`, `valueMin` and `valueMax` properties are all controlled by the Background Calibration bounding box.  

# Instructions for Use

## Configuration
First make sure your [[Nozzle Tip Calibration|Nozzle-Tip-Calibration-Setup]] is enabled and working. **You can't use the background calibration without the nozzle tip calibration.**

There is a new Background Calibration section on the Calibration tab: 

![Screenshot_20220219_154325](https://user-images.githubusercontent.com/9963310/154806475-db1c38d9-7be9-44f7-8bd4-86e06fbdcba4.png)

### Selecting the Method

**Method** controls the Background Calibration:

![Screenshot_20220219_160944](https://user-images.githubusercontent.com/9963310/154806671-75d40a06-bd88-42d1-b309-517f283cc75a.png)

- **None** switches Background Calibration off. The pipeline is no longer controlled, i.e. the user can edit the properties in the Pipeline Editor. 
- **Brightness** only determines the cutoff brightness of the nozzle tip and background, no key color is assumed to be present. 
- **BrightnessAndKeyColor** determines both the nozzle tip key color, and the remaining background cutoff brightness. 


### Other Settings

**Minimum Detail Size** (in system units) configures the smallest valid detail of a part to be detected in bottom vision, such as the smallest dimension of a pad, pin, ball of a package. All smaller image details like image noise, textures, dirt etc. are considered irrelevant for detection. The **Minimum Detail Size** controls a blurring filter to suppress these artifacts.

**Minimum** and **Maximum** columns in the **Hue** (base color), **Saturation**, **Value** (Brightness) [HSV color model](https://en.wikipedia.org/wiki/HSL_and_HSV) indicate the calibrated bounding box of the key color (if enabled). These values will be computed by the calibration.

The HSV color model can be illustrated like this ([Wikipedia](https://en.wikipedia.org/wiki/HSL_and_HSV)):

![image](https://user-images.githubusercontent.com/9963310/154807032-f8de9cd1-daf9-4de3-8fb2-02884266c4e7.png)

**Tolerance** can be used for each channel, to expand the detected bounding box. This provides robustness against changing ambient lighting, shadows cast by the picked parts, etc. Note: practical tolerance values are yet to be determined in testing. The tolerance is applied to both sides of the channel bounds, except for Saturation Maximum, and Value Minimum which are both  left unlimited in the bottom vision `MaskHSV` stage. OpenPnP assumes both darker and more vivid (greener) pixels are always part of the background, even if they were not detected, or cut off in the calibration. 

Note, the Tolerance is (currently) not shown in the graphical HSV color indicator. 

### Performing the Background Calibration

Once you have selected **Method** and **Minimum Detail Size**, you can test the calibration by pressing the **Calibrate** button. In production use the Background Calibration will be re-triggered together with the Nozzle Tip Calibration, as described in the [Calibration Operating Principle](#calibration-operating-principle) section. 

## Application

Calibrated background removal does only work, if your pipeline actually has a `MaskHsv` stage. This is the case with the OpenPnP stock pipeline. However, perhaps you modified the pipeline and removed it, so you may need to [activate the stock pipeline](/openpnp/openpnp/wiki/Computer-Vision#using-new-stock-pipelines) to get the functionality.

The Background Calibration data is then used to automatically parametrize the bottom vision operation. If the Background Calibration **Method** is set (not **None**), the relevant stage properties in the pipeline are fully controlled, as described in the [Calibration Operating Principle](#calibration-operating-principle) section. 

If the background was successfully calibrated (i.e. no problems indicated), the knockout should now be perfect:

![nozzle-tip-background-knockout](https://user-images.githubusercontent.com/9963310/154805842-e565ec69-a890-4757-963c-2ffe603cdf3f.gif)

Otherwise see the [Trouble Shooting section](#trouble-shooting).

As the Nozzle Tip Calibration is typically renewed at least whenever the machine was homed, or even with each nozzle tip change, the calibration data should be adaptive to changing ambient light and other conditions. A manual recalibration can also always be triggered, if needed.

Furthermore, any (color) differences between nozzle tips are also implicitly handled. This is important, when packages are compatible with multiple such nozzle tips. The pipeline (with is part/package dependent, but not nozzle tip dependent), can therefore dynamically react to these differences, through the calibration control. 

## Trouble Shooting

The calibration provides diagnostic feed-back regarding the quality of the background. A text describes the problem(s) and provides rough guidance as to how the problem can be remedied. Furthermore, the button **Show Problems** can be pressed to display the problematic image portions in the camera preview:

![nozzle-tip-background-diagnostics](https://user-images.githubusercontent.com/9963310/154807616-560c3710-6de2-4c44-ae6e-52625f0b4041.gif)

Credits: Nozzle tip images by Jan, with thanks!