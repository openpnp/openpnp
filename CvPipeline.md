# Warning: Preliminary Documentation

# Warning: This feature has just been released and is considered Alpha. Please report issues [here](https://github.com/openpnp/openpnp/issues).

# Introduction

CvPipeline stands for Computer Vision Pipeline and is a suite of classes and utilities in OpenPnP for creating complex computer vision tasks out of small, reusable components. CvPipeline is used primarily in [[Bottom Vision]] but will be used more and more throughout the system for computer visions tasks.

A short video showing some of the capabilities of CvPipeline can be found here: https://www.youtube.com/watch?v=iUEP0bILAU0

# Components

The CvPipeline suite includes the CvPipeline and CvStage classes, the CvPipelineEditor GUI component and a number of CvStage implementations that do the actual computer vision work. There is also a StandaloneEditor class that can be run directly to work with a new CvPipeline.

# Operation

A CvPipeline is a list of operations to perform on an image. The operations consist of both basic and advanced computer vision operations. They are performed in order and the image is generally updated with the results of each one. These operations are called "Stages" and a list of "Stages" makes a "Pipeline".

Stages may also produce non-image data as output. This data can be used by later stages or by the caller of the pipeline.

# UI

CvPipeline includes a basic UI for editing the pipeline and viewing the results of each stage. At each stage you can view the resulting image and any additional data that was produced.

# FAQ
