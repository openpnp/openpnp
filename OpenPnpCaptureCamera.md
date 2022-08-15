OpenPnpCaptureCamera is an OpenPnP camera driver based on the [openpnp-capture](https://github.com/openpnp/openpnp-capture) library by Niels Moseley. It is the most advanced OpenPnP camera driver available, making it possible to use multiple USB cameras on a single host, and allowing you to specify configuration parameters such as exposure, brightness, focus, etc.

# Multiple Cameras
When using multiple USB cameras on the same USB host, there are some limitations to consider. Primarily, it is important to use a compressed format when selecting the Format in OpenPnP. Compressed formats have a number of names. The most common are 1bmd, dmb1, mjpg, and mjpeg. Uncompressed formats such as yuv, vuy, yuv2, etc. cannot be used when using multiple cameras on the same host.

For more information on the limitations of multiple USB cameras, see the [[USB Camera Troubleshooting FAQ]].

To configure the format for a camera:
1. Go to Machine Setup -> Cameras or Machine Setup -> Heads -> Cameras and select the camera to configure.
2. Select the Camera Specific tab.
3. Scroll down to the General section and click the Format dropdown.
    ![screen shot 2018-09-10 at 9 03 09 pm](https://user-images.githubusercontent.com/1182323/45333843-b76ff380-b53d-11e8-9e70-82352408ce90.png)
4. In the list of Formats, find a **compressed** format that meets you resolution and FPS needs. If there are no compressed formats in the dropdown you will either need to change to a different camera (see [What Should I Build](https://github.com/openpnp/openpnp/wiki/FAQ#what-should-i-build) for recommendations) or put each camera on it's own USB host port.

# Camera Properties

Depending on the make and model of your camera, different properties will be available/enabled. [[Issues and Solutions]] will suggest some advice setting correct properties. 

Generally, **Auto** settings should be avoided, as they lead to unstable conditions for computer vision. We need to be able to set stable thresholds, e.g. for brightness, color hue, saturation etc. which is not possible if they fluctuate between camera subjects. If your camera does not support switching off automatic exposure, consider replacing it. Seriously. 

Unlike humans, computer vision does not care about the aesthetics of an image. Avoid all settings that aim at "pleasing" images, these can be detrimental to the accuracy of computer vision. The raw, unmanipulated image at the right exposure is what we want, so computer vision gets the original _real world_ information from the sensor, even if it looks not so crispy to you. Therefore, minimize **Sharpness**, set **Contrast**, **Gamma** etc. to their respective **Default** values indicated on the right side. 

The optimal image is optimized for the brightest scene that is encountered in operation. In other scenes it might appear too dark to humans, however computer vision can still get enough information from even a fraction of the dynamic range, it is much more important that levels remain stable between scenes. 

If in doubt, set to the **Default** value indicated on the right side. 

![Camera Properties](https://user-images.githubusercontent.com/9963310/184723700-9d25ca1f-5c94-4a33-9579-e3ce2f9aba3c.png)

# Troubleshooting

Note that not all cameras behave as well as others. If you want to be sure of reliable operation of two cameras on a single USB host it is recommended that you use the [ELP Model USB100W03M, 720p USB Cameras](http://www.elpcctv.com/hd-720p-usb-cameras-c-85_87.html). See the [[Build FAQ]] for more camera recommendations.
