# Introduction

Bottom Vision is a feature in OpenPnP that makes it possible to place components more accurately. Simply put, by using an up looking camera OpenPnP can identify if a part was picked with any offset or rotational error, determine what that error is and then apply a correction before placement. Bottom Vision can also be used to determine if a pick failure occurred.

# Operating Theory

The basic idea for bottom vision is:

1. Pick a part from a feeder.
2. Center the nozzle over an Up looking camera and take an image.
3. Using a [[CvPipeline]], determine the part's offset from center and whether or not it is rotated.
4. Provide the resulting X, Y and Rotation coordinates to the JobProcessor so that the error can be corrected during placement.

Core to the working of Bottom Vision is the [[CvPipeline]]. The pipeline describes a series of computer vision operations that will take place to convert the input image to a `RotatedRect`. `RotatedRect` is computer vision speak for a rectangle (width, height, X, Y) with a rotation component. Using this `RotatedRect` we calculate the error and correct it.

# Hardware Setup

* Connect and configure an Up looking camera. It should not be attached to a head.

* The lens should be set up such that when the nozzle is centered over the camera the largest part you intend to use fits within the frame. If you are using a fisheye lens or a lens with significant distortion, consider [[Camera Lens Calibration]].

* Set the camera's location (in it's setup panel) such that the nozzle is centered and focused over the camera.

# Global Configuration

To configure Bottom Vision visit Machine Setup -> Vision -> Bottom Vision.


![Bottom Vision General Settings](https://user-images.githubusercontent.com/9963310/115859927-13f35900-a431-11eb-8a20-85638ebad74a.png)

**Enabled?**: Switches on bottom vision.

**Pipeline**: Press the `Edit` button to view and edit the default pipeline that will be used to locate parts. The default pipeline is used when no part specific pipeline has been configured. The built-in pipeline is described below. Press `Reset to Default` to reset the pipeline to the OpenPnP default. Press `Reset All Parts` to assign the current pipeline to all parts. 

**Rotate parts prior to vision?**: Will already pre-rotate the part on the nozzle to its final placement angle. This generally improves precision. This option also enables multi-pass vision. This mode must be set with a nozzle that has limited articulation (i.e. less than 360°); see [[Nozzle Rotation Mode]].

### Multi-pass Vision

The part will first be positioned and rotated as picked from the feeder, which means it might be slightly offset both in location and angle, due to play in the feeder etc. The vision operation then determines these offsets, but these may be inaccurate as the part is seen slightly from the side, which may create parallax errors and slight changes in light relection angles which might affect how beveled/angled surfaces are lighted. Furthermore, the scale in the camera view (units per pixel) might be slightly in error as the part or some of its features (angled pins etc.) might be slightly outside the focal plane. 

In order to further improve precision, the part is then centered in the camera view, according to the preliminary offsets and the process is repeated. By centering the part (multiple times), the mentioned errors can be cancelled out by symmetry. 

**Max vision passes**: Determines the maximum number of passes used to pinpoint the part (only applies when pre-rotate is active).

**Max linear offset**: As long as preliminary linear offsets are larger than this, further passes are made, up until **Max. vision passes** is reached. Linear offsets are center offsets and corner offsets, see the illustration below. (only applies when pre-rotate is active).

**Max angular offset**: As long as preliminary angular offsets are larger than this, further passes are made, up until **Max. vision passes** is reached (only applies when pre-rotate is active).

![Offsets Explained](https://user-images.githubusercontent.com/9963310/115862405-67b37180-a434-11eb-96ff-1f22ec1c9b0b.png)

You will probably need to customize the pipeline a bit for your machine. See the [Tips](#tips) section below. There is likely to be a lot of discussion and learning happening on [the mailing list](http://groups.google.com/group/openpnp). That should be your first stop for help.

# Part Configuration

Each Part in your Parts library can have it's own custom pipeline. In most cases the default pipeline will work but this allows you tweak the pipeline for troublesome parts or create entirely new pipelines when the default won't work.

You can also enable or disable bottom vision on for each part.

To access the bottom vision part settings go to the Parts tab in OpenPnP, select a part and look for the Alignment tab on the right.

![Part Bottom Vision Settings](https://user-images.githubusercontent.com/9963310/115862882-f9bb7a00-a434-11eb-92c6-b55d5710af25.png)

**Enabled?**: Switches on bottom vision for this part. This is checked in addition to the global **Enabled?** switch (see above).

**Pre-rotate**: This can either inherit the global setting or override it as **Always On** or **Always Off**. 

![Pre-Rotate Options](https://user-images.githubusercontent.com/9963310/115863124-4e5ef500-a435-11eb-8745-c0ffd324b28e.png)

**Test**: Press the `Test Alignment` button to perform an alignment of that part. It must already be picked and held on the nozzle. 

**Center After Test**: Centers the part after the vision alignment test. 

**Pipeline**: Press the `Edit` button to view and edit the part specific pipeline that will be used to locate parts. Press `Reset to Default` to reset the pipeline to the global default (see [Global Configuration](#global-configuration)). 

**Rotation**: Use the option **Adjust** for all standard pipelines. Only an adjustment of ±45° can be detected and applied, which is usually more than sufficient for parts picked from normal feeders. Use the option **Full** only for special pipelines that specifically support full 360° orientation detection (pin 1 detection). **CAUTION**: You may get very strange and intermittent errors, if you set **Full** and your pipeline is not specifically made for it.  

![Rotation Options](https://user-images.githubusercontent.com/9963310/115863659-10ae9c00-a436-11eb-8a86-1ecb4db357e4.png)

**Part Size Check**: You can add a part size check, where the vision result is compared against a known good part size. This can serve as an alternative to vacuum sensing tests, to detect whether a pick was successful. 

- Use **BodySize** if your pipeline looks for the body of the part (e.g. the black body of lead free packages). 
- Use **PadExtents** if your pipeline looks for the contacts (which the default pipeline does). Note, you need to define the footprint in the Package for this to work.

![Part Size Check Options](https://user-images.githubusercontent.com/9963310/115866469-fd9dcb00-a439-11eb-90b8-6b47ad4c49a6.png)

**Size tolerance (%)**: Determines by how much (relatively speaking) the detected part size may deviate from the footprint defined size. If the tolerance is exceeded, the aligment fails. Most likely the part was then not properly picked. 

# Usage

## Note: This section will be expanded soon.

See https://www.youtube.com/watch?v=pRYQaFKhsuw for a short demonstration of how to pick, test and discard a part for bottom vision.

When bottom vision is enabled in Machine Setup and for a specific part it will be used automatically during a job run. If the system is able to determine the offsets they will be applied. If the operation fails the placement will continue with no offset correction. This will be improved in the future to handle retry and discard.

# Default Pipeline

OpenPnP comes with a default pipeline. The pipeline was developed for one particular machine design but using the [[CvPipeline]] tools it is possible to customize the pipeline for any type of machine. In general, the changes should be minimal if certain rules are followed. If you have not read [[CvPipeline]] it's worth taking a moment to do so as it will help you understand the rest of this.

The default pipeline is described below:

1. ImageCapture: Waits for the camera to settle and captures an image.
2. ImageWriteDebug: Writes the input image to a file on disk to help with debugging.
3. BlurGaussian: Performs minor blurring on the input image. This is used to reduce noise in the image.
4. MaskCircle: Blacks out everything outside of a circle of a given diameter. On the development machine this circle represents a "safe" area in the image where nothing is visible except the nozzle.
5. ConvertColor: Convert from RGB color to HSV color, which is required for the next stage.
6. MaskHsv: Searches the image for any pixels that match a certain hue (the H in HSV) and turns them black. The purpose of this is to remove green and "greenish" pixels from the image. Green is the color of the nozzle holder. This is similar to the concept of "green screening".
7. ConvertColor: Convert back from HSV to RGB. This is required by the next stage.
8. ConvertColor: Convert from RGB to grayscale.
9. Threshold: Turns the image into a binary image - meaning that it has only two colors: white and black. Any gray pixels that are darker than the threshold value turn black and any lighter turn white.
10. FindContours: Find connected contours in the image. Contours are a way to describe simple features in an image such as lines and curves.
11. FilterContours: Removes any contours from the previous stage that are smaller than a specified value. This helps remove noise and features that don't pertain to the main contour around the part.
12. SetColor: Sets the entire image to black. This simply provides a blank canvas for the next stage to draw on.
13. DrawContours: Draw all the of the remaining contours in white on the black background. At this point we hope that we're simply drawing the shape of the part only.
14. MinAreaRect: This is where the magic happens! MinAreaRect creates a `RotatedRect` that fits around any non-black pixels in the image. Since we drew contours representing the part this now finds the bounds and rotation of the part.
15. ImageRecall: Recalls the original input image so that we can show the user the results of all this work.
16. DrawRotatedRects: Draw the `RotatedRect` in red overtop the recalled original image. If all went well we should now see the original input image with a red rectangle surrounding the part.
17. ImageWriteDebug: Writes the resulting image out to a file for help with debugging.

# Tips

* Much of the purpose of the vision pipeline is to filter the image so that the only thing that is visible is the part you are interested in. The various Mask stages and Thresholds can help with this.
* Switch off the camera's auto-exposure. Otherwise, this will never be stable and repeatable. If your camera does not allow that, chose a different model. Seriously.
* You should set your Camera's exposure, so you the [dynamic range](https://en.wikipedia.org/wiki/Dynamic_range) includes the brightest parts of the image without [clipping](https://en.wikipedia.org/wiki/Clipping_(photography)). We need to be able to distinguish the bright tones. Use the shiniest part to set this up. The image will be rather dark for humans, but it is fine for machine vision. 
* The following animation quickly shows you how to initially set or tune a threshold, using the mouse to probe image pixels. Look at the third channel (the `V`) of the `HSV(full)` indicator on the status line (this indicator for brightness will work both for color and grayscale images). Probe the pixels that should be _excluded_ and probe the pixels that should be _included_, then set the threshold to a value in between:

  _While holding down the Shift key, use the context menu to display the image separately. This will restart the animation from the beginning._

  ![BottomVisionThreshold](https://user-images.githubusercontent.com/9963310/96963951-e1ac8180-1509-11eb-87c5-630dec575931.gif)

* If you have green color Juki nozzle tips you can use the MaskHSV stage to mask the green parts reliably. Make yourself familiar with the HSV model ([from the Wikipedia](https://en.wikipedia.org/wiki/HSL_and_HSV)). 

  ![HSV Model](https://user-images.githubusercontent.com/9963310/96978029-335f0700-151e-11eb-9833-802249f8b7ef.png)

* The following animation quickly shows you how to initially set or tune an HSV mask, using the mouse to probe image pixels. First we probe green pixels and look at the first channel (the `H` as in "hue") of the `HSV(full)` indicator on the status line to _include_ what is green. Then we _exclude_ parts that are too bright (the pins) looking at the third value (the `V` as in "value" i.e. brightness) of the `HSV(full)` indicator on the status line. Finally, we _exclude_ the parts that are too dark (background stuff, the IC body), using the same method. 

  _While holding down the Shift key, use the context menu to display the image separately. This will restart the animation from the beginning._

  ![MaskHSVSetup](https://user-images.githubusercontent.com/9963310/96976803-8e8ffa00-151c-11eb-8db1-edc0626316c7.gif)
  
# FAQ

## How do I see debug images?

Enable DEBUG or TRACE level logging. See [this FAQ](https://github.com/openpnp/openpnp/wiki/FAQ#how-do-i-turn-on-debug-logging) for more information. Bottom vision will now produce a pair of debug images in your `.openpnp/org.openpnp.vision.pipeline.stages.ImageWriteDebug` directory.
