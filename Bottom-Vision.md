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

To enable Bottom Vision visit Machine Setup -> Vision -> Bottom Vision, check the `Enabled?` checkbox and press Apply.

In the same window, click the Pipeline `Edit` button to view and edit the default pipeline that will be used to locate parts. The default pipeline is used when no part specific pipeline has been configured. The built in pipeline is described below.

You will probably need to customize the pipeline a bit for your machine. Since this is a very new feature there is likely to be a lot of discussion and learning happening on [the mailing list](http://groups.google.com/group/openpnp). That should be your first stop for help.

# Part Configuration

Each Part in your Parts library can have it's own custom pipeline. In most cases the default pipeline will work but this allows you tweak the pipeline for troublesome parts or create entirely new pipelines when the default won't work.

You can also enable or disable bottom vision on for each part.

To access the bottom vision part settings go to the Parts tab in OpenPnP, select a part and look for the Alignment tab on the right.

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

# FAQ

## How do I see debug images?

Enable DEBUG or TRACE level logging. See [this FAQ](https://github.com/openpnp/openpnp/wiki/FAQ#how-do-i-turn-on-debug-logging) for more information. Bottom vision will now produce a pair of debug images in your `.openpnp/org.openpnp.vision.pipeline.stages.ImageWriteDebug` directory.
