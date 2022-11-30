SwitcherCamera lets you have multiple virtual cameras sharing the same physical capture hardware. This is common in commercial desktop pick and place machines where a single USB capture card captures images from multiple analog cameras. A serial command is used to switch between the two streams.

* [Video Demonstration](https://www.youtube.com/watch?v=gFXqbMJM2wM) 
   
   Note: the above video is outdated. In newer OpenPnP versions, each switched camera view will always only show images that come from the right perspective, i.e. if the camera is currently not the switched-to camera, the view is frozen on the last frame before the switch. 
* [Related Issue](https://github.com/openpnp/openpnp/issues/851)


## Configuration

The most common configuration for SwitcherCamera will be a single capture device using the OpenPnpCaptureCamera as a source, and two SwitcherCameras - one for Top and one for Bottom. The Top and Bottom SwitcherCameras will send a command to switch back and forth.

The basic steps to set everything up are:

1. Create an Actuator that will switch between the Top and Bottom camera. Set its **Value Type** to **Double**. 

   ![Switcher Actuator](https://user-images.githubusercontent.com/9963310/204848779-615e803c-6281-4e91-a138-dab82888963a.png)

1. Create the [[OpenPnpCaptureCamera]] as a normal camera and set it up to capture images from your capture hardware.

   ![Capture Device](https://user-images.githubusercontent.com/9963310/204850391-48b45cd0-402f-4f00-93d5-4abbec962522.png)

1. Make sure to set **Preview FPS** to 0. Disable **Show in multiple camera view?**.

1. Create the Bottom SwitcherCamera. Under Device Settings, choose the source camera and the the actuator. 

   ![Bottom Switcher](https://user-images.githubusercontent.com/9963310/204848021-1d12a6ce-8312-4371-b231-9acadd5718c9.png)

1. Fill in the actuator value with the numeric code that you will send to enable that camera. For instance, if the command M810 enables your Bottom camera, fill in 810.

1. Make sure to set **Preview FPS** to 0. Enable **Suspend During Tasks?**.

1. Create the Top SwitcherCamera similarly to the Top. 

1. Make sure to set the correct numeric code for the Bottom camera. It should be different from the Top.

1. Let [[Issues and Solutions]] help you create the G-Code for the Switcher actuator. Enter:

   `M{IntegerValue} ; actuate camera switcher`

   ![Actuator G-Code](https://user-images.githubusercontent.com/9963310/204856344-99e53fe2-b8ff-4495-a949-6fc36081f0e6.png)


To switch between cameras double click on the camera in the camera view. Note that this will capture a screenshot in your OpenPnP configuration directory. This process will be improved soon.

OpenPnP will automatically switch between the two cameras for vision operations without capturing a screenshot.

# Hide the Capturing Device camera
Cameras can be shown/not shown in multi camera view panels (**Show All Horizontal**/**Show All Vertical**). This is typically used to hide the capture card in a SwitcherCamera setup. The Camera can still be selected as a single CameraView.

![Show All](https://user-images.githubusercontent.com/9963310/106962435-6761df00-673f-11eb-8d8e-4098cbacb094.png)

Disable the **Show in multi camera view?** option if you want to hide the capture device camera:

![Show option](https://user-images.githubusercontent.com/9963310/106962570-9aa46e00-673f-11eb-99ae-a0c88732dd14.png)

