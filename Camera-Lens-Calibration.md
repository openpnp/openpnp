## WARNING: This is a new very feature. If you run into problems, please file a bug report.

Camera lens calibration allows OpenPnP to learn about the parameters of the camera lens you are using and then apply a undistort algorithm to remove any distortion of the image caused by the lens. This is especially useful in fisheye lenses as it can make straight lines look straight when they would typically be curved by the lens.

The calibration process consists of showing a known pattern to the camera in a variety of positions and orientations. A set number of images are captured of the pattern and then OpenCV is used to calculate the lens parameters.

To calibrate the camera, first download and print a pattern from http://nerian.com/support/resources/patterns/. Make sure that the full pattern is visible on the page and there is some whitespace in the margins.

Mount the printed pattern to a piece of cardboard or stiff cardstock. It needs to stay relatively flat during the calibration process and you will be moving it around.

Start the calibration process and follow the on screen instructions. You will see the pattern being recognized by the camera as you move the card around. A sample will be taken every 1.5 seconds and the screen will flash.

Calibration works best when the pattern is captured in many difference orientations and positions. Make sure to move the card around: turn it, angle it towards and away from the camera, move it around within the view of the camera, etc.

You can see a video of the process being performed at: [TBD YOUTUBE LINK]

When the process is complete OpenPnP will enable the undistort function and if all went well your camera view should now appear undistorted. You can toggle the "Apply Calibration?" checkbox back and forth to see the before and after results.

References:
http://docs.opencv.org/2.4/doc/tutorials/calib3d/camera_calibration/camera_calibration.html
http://opencv-java-tutorials.readthedocs.org/en/latest/09-camera-calibration.html
https://github.com/openpnp/openpnp/issues/226