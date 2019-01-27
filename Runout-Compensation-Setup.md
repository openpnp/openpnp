# What is Runout and Runout Compensation?
See the animations below. Left side is without compensation, right side is compensation enabled. The compensation algorithm removes the eccentricity of the nozzle tip to gain better placement accuracy. The following is about how to setup that feature.
![nozzle tip without compensation](https://user-images.githubusercontent.com/3868450/51180932-110c7400-18ca-11e9-8518-aff180ec30d5.gif)
![runout compensated](https://user-images.githubusercontent.com/3868450/51181050-5df04a80-18ca-11e9-887b-b25f2942505b.gif)

# Setup Panel
You can set the compensation feature per nozzle tip. This allows you to tune the pipeline well to the different nozzles' diameter sizes.
1. Enable the feature for nozzle tips you have large runout.
2. Choose the algorithm to compensate for runout. The default is modelBased and should work for the most machines well. For details on the algorithms see the following section.
3. The compensation algorithm needs measurements of the nozzles at different angles to calculate the runout. For model-based algorithms you may set the divider as low as 3, making 3 measurements at -180°, -60° and 60°. For little higher accuracy you may want to choose 6. A reasonable minimum to gain sufficient accuracy with the table-based algorithm is 8 to measure at every 45° (360°/8=45°).
4. Now move the nozzle tip to the bottom camera using the button next to calibrate.
5. Edit the pipeline. Best is a pipeline that returns only exactly one result. You can use DetectHoughCircles and SimpleBlobDetector to find the nozzle tip.
6. Click apply.
7. Click calibrate.
8. When process is finished you may want to check that the nozzle is well centered over the bottom camera. Click bottom cam for that and rotate the nozzle.
![calibration panel](https://user-images.githubusercontent.com/3868450/51443853-7065ec00-1cef-11e9-8786-3b100840480a.PNG)

## Algorithm
There are two algorithms available. Normally you would choose the new model based algorithm, it's set default. The graphic shows the working priciple of both implemented algorithms:
![algorithm-graphic](https://user-images.githubusercontent.com/3868450/51412842-3b319080-1b6d-11e9-9642-f20b5d05f8a4.png)

|  | model-based algorithm | table-based algorithm |
| ------------- | ------------- | -------------- |
| working principle | Fitting a circle to the measured xy-offset data which then describes the nozzle runout at every arbitrary angle | Simple table lookup, interpolation between measured points |
| accuracy | same at every angle | exact only at measured angles due to linear interpolation between |
| needed minimum measurements | 3, 6 recommended | 8 |
|first implementation|01/2019|2016, fixed 01/2019|

The table-based algorithm is still available since it can be used if the mechanics of the machine can't turn the nozzle full 360°. In this case circle fitting may suffer accuracy. Since this is very rare, the model-based algorithm is the default and recommended.

## Pipeline
The pipeline should detect the nozzle tip in a very stable way. Further it should return only exactly ONE result, not many points. Here are suggestions how to adapt the default pipeline for your needs. Its not every aspect of the pipelines described here. Just parts that might be helpful for nozzle detection.

### Juki-Nozzles

  * Threshold

Adapt the Threshold just as high, that the nozzle tip shows as circle and doesn't bleed out.
![Threshold](https://user-images.githubusercontent.com/3868450/51399985-8c2e8e00-1b47-11e9-9cf0-c20e6b3cf8ad.PNG)

  * DetectCirclesHough

INFO INFO. 
Most relevant parameters: threshold value, mask circle diameter, houghcircle diameter max/min
![DetectCirclesHough](https://user-images.githubusercontent.com/3868450/51399987-8c2e8e00-1b47-11e9-8c37-0f9d9148300d.PNG)

  * If you were successful, it should look like this in the recall

![pipeline3](https://user-images.githubusercontent.com/3868450/51399984-8c2e8e00-1b47-11e9-92df-b6ac2bddb79b.PNG)


### Samsung CP40
ANYBODY?

