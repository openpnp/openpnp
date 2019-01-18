# What is Runout and Runout Compensation? (WORK IN PROGRESS)
See the animations below. Left side is without compensation, right side is compensation enabled. The compensation algorithm removes the eccentricity of the nozzle tip to gain better placement accuracy. The following is about how to setup that feature.
![nozzle tip without compensation](https://user-images.githubusercontent.com/3868450/51180932-110c7400-18ca-11e9-8518-aff180ec30d5.gif)
![runout compensated](https://user-images.githubusercontent.com/3868450/51181050-5df04a80-18ca-11e9-887b-b25f2942505b.gif)

# Setup
![calibration panel](https://user-images.githubusercontent.com/3868450/51401080-3ad3ce00-1b4a-11e9-8e42-7d062c46929a.PNG)

## Algorithm
There are two algorithms available. Normally you would choose the new model based algorithm, it's set default. The graphic shows the working priciple of both implemented algorithms:
![algorithm-graphic](https://user-images.githubusercontent.com/3868450/51412842-3b319080-1b6d-11e9-9642-f20b5d05f8a4.png)

### model based compensation
The model based compensation works by fitting a circle to the measured xy-offset data which then describes the nozzle runout at every arbitrary angle. The algorithm
  * needs only a few measured points (at least three at 0°, 120° and 240°) to gain sufficient accuracy
  * ...
### table based compensation
The table based compensation was implemented in OpenPnP a long time ago and was finished/fixed now.
The algorithm
  * interpolates the runout between measured points and has little lower accuracy at non-measured angles and
  * needs generally more measurements for sufficient accuracy.
It's still available since it can be used if the mechanics of the machine can't turn the nozzle full 360°. Since this is very rare, the model based algorithm is the default though.

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


# Move to Event-Page after PR merge
### NozzleCalibration.Starting

Called before nozzle is calibrated. The other camera-events (beforeSette, .beforeCapture and .afterCapture) are fired while processing, too.

| Name  | Type | Description |
| ------------- | ------------- | -------------- |
| nozzle  | [org.openpnp.spi.Nozzle](http://openpnp.github.io/openpnp/develop/org/openpnp/spi/Nozzle.html) | The Nozzle that is being calibrated. |
| camera  | [org.openpnp.spi.Camera](http://openpnp.github.io/openpnp/develop/org/openpnp/spi/Camera.html) | The Camera which will be used to capture an image. |

Example for adapting the camera exposure:  
.openpnp/scripts/events/NozzleCalibration.Starting.js
```js
// TODO -> get from @netzmark
```


### NozzleCalibration.Finished

Called after nozzle calibration finished.

| Name  | Type | Description |
| ------------- | ------------- | -------------- |
| nozzle  | [org.openpnp.spi.Nozzle](http://openpnp.github.io/openpnp/develop/org/openpnp/spi/Nozzle.html) | The Nozzle that was calibrated. |
| camera  | [org.openpnp.spi.Camera](http://openpnp.github.io/openpnp/develop/org/openpnp/spi/Camera.html) | The Camera which will be used to capture an image. |

Example for adapting the camera exposure:  
.openpnp/scripts/events/NozzleCalibration.Finished.js
```js
// TODO -> get from @netzmark
```