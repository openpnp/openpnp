# What is it?

Camera Settling is used to let your camera adjust to what it's looking at before a picture can be taken for Computer Vision (CV). This usually takes into account any blur from the end of a movement, waiting for vibrations to abate, along with time needed to perform any auto exposure or focus operations, plus any lag in communication and processing.

![AdvancedCameraSettleSmall](https://user-images.githubusercontent.com/9963310/81856586-504fe100-9561-11ea-982a-a0b95bf4c0e8.gif)

# Simple Method: Fixed Time

The simple method is to just wait a moment. The default settle time is 250 milliseconds. If you find that things like fiducial checks are getting blurry images, you may need to increase the settle time.

1. With the camera selected in the Machine Setup tab, select the Vision tab.
2. Set the Settle Method to FixedTime and change the value of Settle Time (ms) to the number of milliseconds required. The easiest way to determine the value is to start high (2000ms) and then lower it until you stop getting good results and use a slightly higher value.

But because the settle scenarios can be quite different, it is hard to find a settle time that covers the worst case, while not slowing down the machine too much. Computer Vision is used in very different scenarios. Sometimes the machine races to a location then stops abruptly and wants to perform a CV operation (e.g. find a fiducial). On the other hand there are "drill-down" type CV operations where many pictures are taken in succession with only minor adjustment moves between them. Unless you have the stiffest machine and the coolest camera, these scenarios will require vastly different settle times due to vibrations and other effects. 

It is also very difficult to know and provoke the worst case scenario in advance. That is the reason for advanced Settle Methods and diagnostics.

![grafik](https://user-images.githubusercontent.com/9963310/81851285-7f625480-9559-11ea-8e89-f12c5e98e87a.png)


# Advanced Settle Methods and Diagnostics

## The Idea

The Idea is to look at the pictures taken and by comparing each frame with its predecesor, determine when the settling is concluded. So after a brutal machine stop, it might take longer, but "drill-down" micro-move adjustments are very fast. This becomes possible with the advanced Settle Methods (i.e. not Fixed Time).

You can still set a **Settle Timeout (ms)** value, that will end the settling no matter what. You should choose a very conservative time to really cover all the worst case scenarios. The timeout will make sure your Cameras never "hang" when the settle goal (Threshold) is never reached. This could be due to flickering ambient light or a misconfiguration etc.. 

## Enable Diagnostics

To see what we are doing, we need to enable Diagnostics. Choose one of the advanced Settle Methods (i.e. not Fixed Time) and check the Diagnostics checkbox. Then just press the Camera settle test button.

![grafik](https://user-images.githubusercontent.com/9963310/81853527-d289d680-955c-11ea-804d-4399103ea938.png)

## Checking the Camera Frame Rate

The first task is to see how well the camera performs. We need a steady stream of frames taken. 

The Diagnostics should have plotted a graph. In the blue curve you see how fast your camera delivers frames. When the blue curve is up, it is waiting for a frame, when it is down, it is analyzing the settling. The scale is Milliseconds. 

![grafik](https://user-images.githubusercontent.com/9963310/81853582-e2a1b600-955c-11ea-8812-ed2accaa9f63.png)

If you move the mouse over the graph, you see the recorded settle frames in the Camera View. There's an info text where you see the frame number and the time it took to take this number of frames. You can calculate the avg. frame rate that your camera delivers. Tip: go to the 10th frame, easy to divide :-).

![grafik](https://user-images.githubusercontent.com/9963310/81853889-6360b200-955d-11ea-946b-46a9d1eb19bd.png)

The time the blue curve is down shows how long the frame settling analysis takes to compute. This gives you a first indication of your CPU's vision processing power.

If you see an irregular or unexpectedly slow rate, please reduce your preview FPS on the Camera's Device Settings tab, because the Camera Preview steals its frames away from Computer Vision / Settling and makes it slower. I recommend an FPS of only 3 to 5.

Go back to the Camera's Vision tab and retry. Only the occasional irregularity should now appear in the blue curve.

## Basic Tuning

You can start tuning the settings with similar values as in the screenshot (below). But start with a Settle Threshold of 0.

Then perform one of the settle tests with the icon buttons. These will move the Nozzle in front of the Bottom Camera  - or the Camera itself, if it is the Down-looking Camera - and then immediately settle the camera. Read the tool tips on the buttons for more info about each one. 

![grafik](https://user-images.githubusercontent.com/9963310/81853946-75daeb80-955d-11ea-8256-9ea0d8512781.png)

For finer tuning provoke a real Vision operation, like Part Alignment aka Bottom Vision or Board Fiducials, Nozzle Calibration etc. and go back to the Camera's Vision tab.

![grafik](https://user-images.githubusercontent.com/9963310/81854071-aa4ea780-955d-11ea-9a48-05f94cf47db5.png)

You should now see the red curve descend and settle to some value above 0. Move with the mouse over the graph ("scratch") to see the settle frames. The relative motion is highlighted by overlaying a "heat map".

![grafik](https://user-images.githubusercontent.com/9963310/81854208-e1bd5400-955d-11ea-8931-a5d6be9b388e.png)

When you scratch over the graph you see a red horizontal line with indicated difference value. Go to where you think the frame is stable enough and set this value as your **Threshold**. The last settle frame is also the one taken for computer vision. The higher the threshold, the faster your frame settling is. For best speed you need to judge how much residual motion you want to tolerate. If your machine vibrates at all ;-)

You will need to repeat this for different motion and Computer Vision scenarios and then take the threshold that works for all of them.

## Advanced Tuning

If the red curve shows strong oscillation (vibration of the machine), choose a **Debounce Frames** value to make sure the settling is complete and oscillation has abated enough. Otherwise, a freak coincidence of frames might dip below the threshold and trigger a false settling. You can trade some threshold safety margin in for that, so it's not necessarily slower. 

The **Denoise (Pixel)** setting is an indicator of how precise your Computer Vision needs to be. It often doesn't make sense to use the full resolution of the camera and you also want to filter out camera compression artifacts, sensor noise, aliasing (Moiré), artificial sharpness buzz etc. (Btw. if you haven't set your Camera's Sharpness setting to the minimum, now is the time). The Denoise (Pixel) setting depends on both how high your camera resolution is (Pixels per Millimeter) and how precise your smallest part pitch requires part placements to be. Best judge it with part on the nozzle and look at the pins. You should see the motion edges you still want to matter highlighted in yellow, finer motion should show reddish. In magnification, it should look something like this:

![grafik](https://user-images.githubusercontent.com/9963310/81855037-0b2aaf80-955f-11ea-98ba-b5aebf240dde.png)

If you have varied brightness and limited contrast levels in your vision scenarios (like black Samsung CP40 nozzle tips in calibration, white paper tape strips on white double-sided tape/shiny metal desk), use the **Contrast Enhance** setting to equalize between the scenarios.

Use the **Center Mask** to remove unwanted image peripherals that might spoil Contrast Enhance (like a diffuser or shade partially in view). If set to 0.0, no mask is applied. Values larger than 0.0 are relative to the camera dimension, i.e. 1.0 means from edge to edge. You can set values larger than 1.0, the circle will then be partially cropped.

If some of your Vision subjects are low in contrasts but despite using Center Mask you still have some very dark or very bright blurred objects in the camera view, then Contrast Enhance alone might not work. You might then want to combine it with the **Edge Sensitive** mode. A correctly set Denoise (Pixels) value is important to define the relevant sharpness of the edges. Note that the Edge Sensitive mode will not score large motion higher than medium motion, but we only need to judge the small motions anyway. Edge Sensitive mode might be fooled by motion blur, so you may need to combine it with Debounce Frames to make sure there is no longer any motion blur (it depends on the latency of your camera).

Use **Color Sensitive** for exotic scenarios, where the Vision subject has a color pattern but hardly any black and white contrast. Or if you simply like to watch your settle images in full color. It's even cooler to watch when combined with Edge Sensitive and a colorful subject :-).

If you have a weak CPU and/or a very high FPS camera and it takes too long to analyze the frames (blue curve stays down too long), you might want to set and reduce the Center Mask to only analyze a small part of the frame. You can also increase the Denoise (Pixel) value as this partly scales down the image so it has then fewer pixels to analyze.

## Settle Methods

Different Settle Methods can be used to score how different two subsequent frames are i.e. whether the camera has settled enough. All these scores are normed to a 100% theoretical maximum, but don't interprete too much into the number. We only need to set the proper threshold on these. 

### Maximum

The easiest to understand method is Maximum (the one Jason had implemented before). It just takes the largest single pixel difference as the settle score. Use Denoise (Pixels) to make this resilient against camera artifacts like compression, sensor noise, faulty pixels, aliasing (Moiré) and overly eager sharpness filters in the camera that you can't switch off.

### Mean

Takes the mean (average) pixel difference over the whole image. This might be used together with Edge Sensitive and full Contrast Enhance. But not recommended.

### Euclidean

Takes the Euclidean Distance between the frames (the square root over all the squared differences). The largest pixel differences dominate the score but small differences can still contribute significantly, if large areas change. Therefore this will handle motion blur better than Maximum, where only the single larges pixel difference counts. Unlike with Maximum, it matters to a degree how large and how structured the subject is. So tune this with the smallest/most uniform Computer Vision subjects (e.g. use nozzle calibration of the finest tip and not Alignment of a pin monster MCU).

### Square

Like Euclidean this takes the squared differences over the whole image. It's the classical difference (error) indicator from science. Unlike Euclidean, no square root is taken, so very small numbers will result and will be needed for the threshold. For our purpose of setting a threshold it is equivalent to Euclidean. But more for the math purist.


