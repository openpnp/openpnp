SwitcherCamera lets you have multiple virtual cameras sharing the same physical capture hardware. This is common in commercial desktop pick and place machines where a single USB capture card captures images from multiple analog cameras. A serial command is used to switch between the two streams.

* [Video Demonstration](https://www.youtube.com/watch?v=gFXqbMJM2wM) 
   
   Note: the above video is outdated. In newer OpenPnP versions, each switched camera view will always only show images that come from the right perspective, i.e. if the camera is currently not the switched-to camera, the view is frozen on the last frame before the switch. 
* [Related Issue](https://github.com/openpnp/openpnp/issues/851)


## Configuration

The most common configuration for SwitcherCamera will be a single capture device using the OpenPnpCaptureCamera as a source, and two SwitcherCameras - one for Top and one for Bottom. The Top and Bottom SwitcherCameras will send a command to switch back and forth.

Please see the [Video Demonstration](https://www.youtube.com/watch?v=gFXqbMJM2wM) for details on a standard configuration. The basic steps to set everything up are:

![Screen Shot 2019-06-12 at 10 58 51 PM](https://user-images.githubusercontent.com/1182323/59403014-83449000-8d66-11e9-841e-40ee3717dc00.png)

1. Create the [[OpenPnpCaptureCamera]] as a normal camera and set it up to capture images from your capture hardware.
2. Create an Actuator that will switch between the Top and Bottom camera. Note down the commands you'll need to send to switch. Commonly this will be two M or G-Codes.
3. Create the Top SwitcherCamera. Under Device Settings, choose the source camera and the the actuator.
4. Fill in the actuator value with the numeric code that you will send to enable that camera. For instance, if the command M819 enables your Top camera, fill in 819.
5. Create the Bottom SwitcherCamera similarly to the Top. Make sure to set the correct numeric code for the Bottom camera. It should be different than the one for Top.
6. In GcodeDriver settings, configure the ACTUATE_DOUBLE_COMMAND for the actuator. Generally it will be something like `M{IntegerValue}` which will send M followed by the numeric code you entered.

To switch between cameras double click on the camera in the camera view. Note that this will capture a screenshot in your OpenPnP configuration directory. This process will be improved soon.

OpenPnP will automatically switch between the two cameras for vision operations without capturing a screenshot.

# Hide the Capturing Device camera
Cameras can be shown/not shown in multi camera view panels (**Show All Horizontal**/**Show All Vertical**). This is typically used to hide the capture card in a SwitcherCamera setup. The Camera can still be selected as a single CameraView.

![Show All](https://user-images.githubusercontent.com/9963310/106962435-6761df00-673f-11eb-8d8e-4098cbacb094.png)

Disable the **Show in multi camera view?** option if you want to hide the capture device camera:

![Show option](https://user-images.githubusercontent.com/9963310/106962570-9aa46e00-673f-11eb-99ae-a0c88732dd14.png)

