## What is it?

Unknown part heights can automatically be detected by autofocussing on its underside. The nozzle will servo up and down to find the optimum. 

![Auto Focus](https://user-images.githubusercontent.com/9963310/113595736-b4780980-9639-11eb-8e13-1b9a3c58f65d.gif)

## Nozzle Tip Configuration

As a pre-requisite, the **Part Dimensions** must be defined on each nozzle tip (Configuration tab). Both the **Max. Part Diameter** and **Max. Part Height** are used to determine the autofocus range and sensitive area. 

![Part Dimensions](https://user-images.githubusercontent.com/9963310/113574803-1d9d5400-961d-11eb-860e-bd391d797f16.png)

## Camera Configuration

To enable the auto focus on the camera, select the **Focus Sensing Method**. It is only available on up-looking cameras: 

![Enable Auto-Focus](https://user-images.githubusercontent.com/9963310/113592481-84c70280-9635-11eb-9b4d-c3dcef2d8c33.png)

Once you press **Apply**, a new **Auto Focus** tab appears:

![grafik](https://user-images.githubusercontent.com/9963310/113593256-7f1dec80-9636-11eb-840c-18f01ab0ff54.png)

**Focal Resolution** determines the maximum step size for the Auto Focus servo. 

**Averages Frames** lets you run the autofocus for multiple frames and average the results. This will improve the accuracy by effectively averaging out image noise etc. 

**Focus Speed** sets the relative motion speed for the focus stepping. 
 
**Show Diagnostics?** switches on auto focus edge highlighting and status info in the camera view.  

**Last Focus Distance** indicates the last live or test focus distance measured. If there is a part on the nozzle, this is the part height. 

Press the ![red dot](https://user-images.githubusercontent.com/9963310/113594994-b42b3e80-9638-11eb-870d-f32a728bd8e8.png) button to test the auto focus.  

### Calibrating the Up-looking Camera Z

The auto focus can help you calibrate the precise up-looking camera Z position. This will make sure your bottom vision happens at optimum distance. 

___
**CAUTION**: this procedure will change the subject to camera distance, which will change the scale of subjects. You will need to readjust the **Units per Pixel** after performing it. 
___

1. Use a Z calibrated nozzle tip with no part on. 
2. Auto focus it, using the ![red dot](https://user-images.githubusercontent.com/9963310/113594994-b42b3e80-9638-11eb-870d-f32a728bd8e8.png) button in the **Auto Focus** camera tab. . 
3. Press the **Adjust Camera Z** button to set the camera Z Position to the detected distance. 
4. Go to the **General Configuration** tab to adjust **Units per Pixel**.


### Part Height Auto-Learning

Whenever Alignment (a.k.a. Bottom Vision) encounters a part with unknown part height, it will employ the auto focus. The detected part height is then stored on the part. This will take some additional time (but hardly longer than manually measuring and entering the data). All subsequent vision and placement operations will reuse the height directly, with no further speed penalty incurred. 

The job processor knows when a camera has auto focus capability and bottom vision is enabled for the part. It will then allow starting a Job with unknown part heights.  As an alternative, part heights can also be auto-learned using a ContactProbeNozzle.  

![Part height unknown](https://user-images.githubusercontent.com/9963310/113597986-c313f000-963c-11eb-84e9-b0bedb797185.png)