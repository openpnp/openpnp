## Soft MaskHsv

MaskHsv is prominently used for removing the background "green" nozzle tip in bottom vision, see the [[Nozzle Tip Background Calibration]].

___

TODO: describe the basic operation and properties. The following just documents the **new** properties.

___

Because of the probabilistic nature of some stages, like the [[DetectRectlinearSymmetry]] stage, we need to generate a "natural" image. However, background removal using MaskHsv is usually "hard", i.e. it sets all the pixels to deep black, which then makes these different from the rest of the not-so-black background. Furthermore, the Mask is also hard on the edges, i.e. it either masks a pixel, or not. Amplified by some properties of typical MJPEG color encoding, and compression, this may result in a jagged image and block artifacts: 

![Hard Mask](https://user-images.githubusercontent.com/9963310/156914994-06566148-20cf-4dd0-9b94-e37652c24000.png)

To address this, the MaskHsv stage has therefore been extended with two new properties:

**softEdge** adds a soft edge around the boundaries of the HSV model mask. Pixels can now be somewhere in between being masked and not. This naturally maps to focal blur and other effects where the key color to be masked is mixed with others. 
   
![hsv-soft-edge](https://user-images.githubusercontent.com/9963310/156918804-ce4bbba9-a44a-45ac-8e28-7ca69a6fea79.gif)

**softFactor** determines, how strongly the mask should be applied. At 1.0 fully masked pixels will become all black (default). At 0.5, only half the brightness is taken out. 

![Soft Mask](https://user-images.githubusercontent.com/9963310/156915321-eaf0ee82-7604-477e-a9b0-c2c961dad104.png)