# Introduction

CvPipeline stands for Computer Vision Pipeline and is a suite of classes and utilities in OpenPnP for creating complex computer vision tasks out of small, reusable components. CvPipeline is used primarily in [[Bottom Vision]] but will be used more and more throughout the system for computer visions tasks.

A short video showing some of the capabilities of CvPipeline can be found here: https://www.youtube.com/watch?v=iUEP0bILAU0

This work is primarily inspired by FireSight by Karl Lew and Šimon Fojtů: https://github.com/firepick1/FireSight.

# Components

The CvPipeline suite includes the CvPipeline and CvStage classes, the CvPipelineEditor GUI component and a number of CvStage implementations that do the actual computer vision work. There is also a StandaloneEditor class that can be run directly to work with a new CvPipeline.

# Operation

A CvPipeline is a list of operations to perform on an image. The operations consist of both basic and advanced computer vision operations. They are performed in order and the image is generally updated with the results of each one. These operations are called "Stages" and a list of "Stages" makes a "Pipeline".

Stages may also produce non-image data as output. This data can be used by later stages or by the caller of the pipeline.

# UI

The pipeline editor is split into two halves, with the pipeline displayed on the left and the results of the selected stage displayed on the right.

![screen shot 2017-04-30 at 11 54 21 am](https://cloud.githubusercontent.com/assets/1182323/25566302/d7566d44-2d9b-11e7-97e6-07328db96843.png)

## Pipeline

The left side of the pipeline editor is further split into top (pipeline stages) and bottom (properties for the selected pipeline stage) halves. You can add, remove, and re-order the pipeline stages in the top half, and edit the properties for the selected pipeline stage in the bottom half.

Each pipeline stage has a Name which should be unique for each CvPipeline, which allows subsequent pipeline stages to use the results of earlier pipeline stages.

## Results

You can view the resulting image and any additional data that was produced on the right side of the pipeline editor. Mousing over the image will display the pixel coordinates of the mouse cursor and the pixel colors in RGB and HSB.

# Running the Editor

You can use the pipeline editor within OpenPnP wherever you see an "Edit Pipeline" button. You can also launch it standalone for creating pipelines outside of OpenPnP. To do that you need Java installed.

1. Go to the OpenPnP install directory on the command line.
2. Run `java -cp openpnp-gui-0.0.1-alpha-SNAPSHOT.jar org.openpnp.vision.pipeline.ui.StandaloneEditor`

The standalone pipeline editor has one additional leftmost panel for quickly choosing input images for an `imageRead` stage, with just a single mouse click or an `up` or `down` keypress.

# Stage Documentation

## Note to Contributors
**Instead of adding basic documentation here, please add annotations to the source code and submit a pull request. This allows the documentation to be viewed in the editor. For an example, see [BlurGaussian](https://github.com/openpnp/openpnp/blob/develop/src/main/java/org/openpnp/vision/pipeline/stages/BlurGaussian.java) and the [Stage](https://github.com/openpnp/openpnp/blob/develop/src/main/java/org/openpnp/vision/pipeline/Stage.java) and [Property](https://github.com/openpnp/openpnp/blob/develop/src/main/java/org/openpnp/vision/pipeline/Property.java) annotations. Existing documentation that was here has been moved to the stages.**

## Normalize
On color images it does RGB Max algorithm, removes shadow and looses color information.
```
R = R/(R+G+B)
G = G/(R+G+B)
B = B/(R+G+B)
```

## SimpleBlobDetector
Actually it detects black circles, anything other don't work.

## ScriptRun
Example:
```
var Result = Packages.org.openpnp.vision.pipeline.CvStage.Result;
var result = new Result(pipeline.getWorkingImage(), "I am a model object");
result;
```

# FAQ