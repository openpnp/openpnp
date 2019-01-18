# What is Runout and Runout Compensation? (WORK IN PROGRESS)
See the animations below. Left side is without compensation, right side is compensation enabled. The compensation algorithm removes the eccentricity of the nozzle tip to gain better placement accuracy. The following is about how to setup that feature.
![nozzle tip without compensation](https://user-images.githubusercontent.com/3868450/51180932-110c7400-18ca-11e9-8518-aff180ec30d5.gif)
![runout compensated](https://user-images.githubusercontent.com/3868450/51181050-5df04a80-18ca-11e9-887b-b25f2942505b.gif)

# Setup
![calibration panel](https://user-images.githubusercontent.com/3868450/51401080-3ad3ce00-1b4a-11e9-8e42-7d062c46929a.PNG)

## Algorithm
There are two algorithms available. Normally you would choose the new model based algorithm, it's set default.
### model based compensation
  * list of differences, features, limitations 
### table based compensation
  * list of differences, features, limitations, TODO

## Pipeline
The pipeline should detect the nozzle tip in a very stable way. Further it should return only exactly ONE result, not many points. Here is a suggestion how to adapt the default pipeline for your needs:
![pipeline1](https://user-images.githubusercontent.com/3868450/51399985-8c2e8e00-1b47-11e9-9cf0-c20e6b3cf8ad.PNG)
![pipeline2](https://user-images.githubusercontent.com/3868450/51399987-8c2e8e00-1b47-11e9-8c37-0f9d9148300d.PNG)
![pipeline3](https://user-images.githubusercontent.com/3868450/51399984-8c2e8e00-1b47-11e9-92df-b6ac2bddb79b.PNG)

Most relevant parameters: threshold value, mask circle diameter, houghcircle diameter max/min


NOTE: include the new events Calibration.before and after in the eventpage and add examples (with exposure...)