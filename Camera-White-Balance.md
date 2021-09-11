# What is it?

Getting a white-balanced camera image is important for Computer Vision operations that use color based processing, such as color-keyed a.k.a. "green screen" image masking, typically using the `MaskHSV` stage. Examples are [[Bottom Vision]] with green Juki nozzle tips, [[BlindsFeeder]] vision and [[ReferencePushPullFeeder]] vision.

Camera White Balance can remove color casts from camera images. The camera itself might only exhibit a slight color cast, but surprisingly large color casts are typically present due to inadequate (cheap) LED lighting. Read about the [Color rendering index (CRI)](https://en.wikipedia.org/wiki/Color_rendering_index) for background. To replace bad LEDs with better (higher CRI) ones is certainly the better option, but if not possible, use the OpenPnP White Balance function.

![white-balance-tab](https://user-images.githubusercontent.com/9963310/132950118-3f46b57a-8800-4abf-b752-7f0d8c1763f4.gif)

# Problems with Device White Balance

Some camera devices have an automatic and/or manual "White Balance" setting (see the **Device Settings** tab in an OpenPnpCaptureCamera). 

![device setting](https://user-images.githubusercontent.com/9963310/132950872-f8e64fdd-c8ba-4cd5-a34f-bf26d229307e.png)

_So Why not use it?_ There are these problems:

1. The **Auto** setting is adaptive, it will generate inconsistent color images, depending on what colors are present in the image. For BlindsFeeder vision, where large areas are green, the green becomes "bleached-out", making it hard to choose consistent MaskHSV thresholds: 

    ![bleached-out](https://user-images.githubusercontent.com/9963310/132950521-0fc57ba4-0305-4545-ae14-b36e951c4d1e.png)

2. The **Auto** setting takes time to adapt. You'll need large settle times for good color vision, or some operations might fail. In the following clip, you see how the camera adapts rather slowly in the histogram:

    ![slow-auto-white-balance](https://user-images.githubusercontent.com/9963310/132950804-9b7da9f1-1b3f-4ecc-9bca-6a1b898ab6dd.gif)   

3. The manual setting is not actually a true White Balance. Being one-dimensional (with only one slider) it can only balance two color channels. On ELP cameras, it turns out to control blue-red balance only. Example: Due to the strong green cast on my machine, it is impossible to achieve good white-balance. With the greenish cast always present, none of the slider settings are usable for green-screening/MaskHSV: 

    ![manual-white-balance-no-good](https://user-images.githubusercontent.com/9963310/132951732-3f73b017-66ea-41e2-84c9-012f939a5672.gif)

That's why OpenPnP's own White Balance is needed.

# Instructions for Use

## Prepare the Device Settings

First go to the **Device Settings** tab and switch off the **Auto** checkbox to the left of the **White Balance** slider. Then enter the **Default** value, as indicated with the right-most number (4600 in the example). 

![device-wb-off](https://user-images.githubusercontent.com/9963310/132952110-8e6a29ad-b829-462b-a35c-c4470d16a584.png)

Equally switch off **Auto** on **Brightness**, **Contrast** and **Gamma** and set the values to the **Default**s. 

Then switch off **Auto** on **Exposure** and use the slider to set the proper camera exposure and wanted image brightness. There should be no overexposed (clipped) parts in the image (apart from tiny pinprick highlights, perhaps). A well exposed image might even look slightly dark to humans. 

## OpenPnP Auto White-Balance

Automatic White-Balance can be computed and fixed when the camera looks at neutral grayscales. Move the camera to a spot on the machine, where it sees brushed metal, shiny objects with reflections of the LEDs, white paper, but also darker areas, shadows etc. You can also put objects with these properties in front of the camera. 

Use the context menu (right mouse button) in the CameraView to switch on/off the **Image Info**, which includes a [histogram](https://en.wikipedia.org/wiki/Image_histogram):

![Image Info](https://user-images.githubusercontent.com/9963310/131744015-e92a9b8d-6f69-4182-b7b4-83c5449b6314.png)

The histogram will also be switched on automatically, as soon as you perform one of the **Auto White-Balance** buttons.

Use the **Overall** button to get a neutral look for the overall image. If you look at the Histogram in the camera view, the "mountains" of the color channels should mostly overlap nicely. 

Use the **Brightest** button to adapt to the brightest areas in the image. This will likely capture the lit areas and reflections of the LEDs better and can therefore avoid the influence of ambient light.

Use the **Mapped Roughly** button to get a White-Balance that is mapped to a few individual brightness level bands. This will roughly compensate casts that are more/less pronounced at certain levels. If certain brightness level bands are missing, OpenPnP will tell you so. You then need to add camera subjects with these brightness levels present.  

Use the **Mapped Finely** button to get a White-Balance that is mapped to many individual brightness level bands. This will finely compensate casts that are more/less pronounced at certain levels. If certain brightness level bands are missing, OpenPnP will tell you so. You then need to add camera subjects with these brightness levels present.

![white-balance-methods](https://user-images.githubusercontent.com/9963310/132952607-9439bded-17dc-4b1a-9631-b53caa26515e.gif)

### Notes

* The Auto buttons are **one-time** automatic, i.e. the calibrated balance will then be fixed for repeatable Computer Vision when using color operations such as MaskHSV in bottom vision with Juki nozzles, the BlindsFeeder, ReferencePushPullFeeder, etc. 

* To avoid image noise and posterization effects, the Auto buttons will never increase the whole image brightness, even when the view is dark. You should also not try to do this manually. For good quality, use the **Exposure** slider in the **Device Settings** tab to set the proper camera exposure for the wanted image brightness. 

Use the **Reset** button to set the color balance to the neutral position (which is also the default). The white-balance is then effectively switched off, the computation skipped. 

## Manual White Balance

Starting from Auto White-Balance or even from scratch, you can also set or change the balance manually. This is mostly for experts:

Set the **Red, Green, Blue Balance** sliders manually in a range of 0% to 200%. These control the linear gain of the color channels.

Set the **Red, Green, Blue Gamma** sliders manually in a range of 0% to 200%. These control the [Gamma](https://en.wikipedia.org/wiki/Gamma_correction) of the color channels.

