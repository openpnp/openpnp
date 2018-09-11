# USB Camera Troubleshooting FAQ 

If you are having trouble using multiple USB cameras with OpenPnP, make sure that you are using the [[OpenPnpCaptureCamera]], and not OpenCvCamera. [[OpenPnpCaptureCamera]] overcomes the issues described below. For more information about correctly configuring [[OpenPnpCaptureCamera]] for multiple cameras, visit [[OpenPnpCaptureCamera]]. If you must use OpenCvCamera, continue reading for more information about why you might run into issues.

## I just want it to work!
Put each camera on it's own USB host controller. This normally means that each camera needs it's own dedicated USB port but even that doesn't always solve the problem. If you look in your computer's USB device tree and all of your ports go back to one USB host controller you will need a different computer.

## What about USB hubs?
Think of a USB bus like a hose connected to the faucet. You can only get as much water out as will flow through the faucet. Even if you hook a bunch of splitters to the end of the hose you aren't getting any more water. The faucet is the host controller. The hub is the splitter. 

Most systems will be able to use a hub to run one camera and some other low bandwidth devices. For instance, running a Smoothieboard + one camera on a hub is known to work if you hook your second camera directly to the computer on another USB port.

## What's the problem in technical terms?
Most USB cameras on the market today default to uncompressed YUV video mode, which is very high bandwidth. When the camera enumerates on the USB bus it tells the bus it needs something like 90% of the bandwidth, leaving no bandwidth for other cameras or devices. When you try to connect a second camera it will fail to enumerate or it will enumerate but then when you try to open it it will fail. This usually results in locking up OpenPnP.

## What about USB 3?
If you have USB 3 specific cameras these should work. USB 2 cameras on a USB 3 port, or a USB 3 hub, will not work. USB 2 devices on a USB 3 bus share a USB 2 bus over the USB 3 cable, so they have the same limitation.

## Why is it a software limitation?
Most cameras on the market also support MJPEG compressed video. This is **much** lower bandwidth and you can easily have 2 or more cameras on a port. Unfortunately the library we use for camera capture (OpenCV) does not have a way to tell the camera to switch to MJPEG mode so we are stuck with YUV. There is an effort happening to fix this in 

## What are some other options?
ONVIF IP cameras have none of these limitations. If you use these you should be able to use as many as your network will support. **Note: Links to some good ONVIF cameras here would be appreciated.**