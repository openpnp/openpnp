# What is it?

Computer Vision is used throughout OpenPnP to detect exactly where things are in relation to the cameras, sometimes to just determine _if_ they are there, and sometimes to read text (OCR) or codes (QR-Codes). Computer vision mostly uses a facility called Pipeline to perform the detection. The Pipeline is structured in so-called Stages that the user can configure in a special Pipeline Editor. 

This page links to detailed instructions for some tasks around Computer Vision. 

## Using New Stock Pipelines

OpenPnP ships with stock pipelines that you can use or adapt to your liking. When you update OpenPnP the newest stock pipeline is made available, but it is not automatically replacing your pipelines, as this would likely break your adaptations, of course. Therefore, if you want to upgrade to the newest stock pipeline, and replace yours, it takes a few steps. This is explained for the Bottom Vision pipeline here, but it is similar with other vision settings. 

1. Go to the Stock vision setting.
1. Press the **Copy** button on the pipeline panel:

   ![Stock pipeline copy](https://user-images.githubusercontent.com/9963310/155850071-018b769e-8f22-481b-8288-a6f0f50d51a0.png)

1. Go to the vision settings, where you want to try the new pipeline.
1. Press the **Paste** button on the pipeline panel.

1. Then test the new settings by using **Test Align** etc. 

   ![Paste](https://user-images.githubusercontent.com/9963310/155850406-c1472eb0-fc2f-4e40-a95d-86ece65547e5.png)

If you want to use the Stock pipeline as your new default:

1. You can also _directly_ set the new Stock pipeline on the "Default Machine Bottom Vision" settings by pressing the **Reset** button:

   ![Reset Default to Stock](https://user-images.githubusercontent.com/9963310/155850750-a665e245-c96a-4dd5-a4d7-4b8a0604ac4c.png)
    

# Tweaking Pipeline Parameters

Some essential tweaking parameters can be exposed from a pipeline. See the [[Exposed Pipeline Parameters]] page for how to use this, or for how to create your own parameters. 

# Editing Pipelines

The usage of the Pipeline Editor UI in general is explained on the [[CvPipeline]] page.

# Bottom Vision Background Removal

OpenPnP can automatically calibrate the background (with empty nozzle tip) for bottom vision background removal. See the [[Nozzle Tip Background Calibration]] page.

# Stages

* [[DetectCircularSymmetry]] searched the image for Circular Symmetry. It can be used to detect round things, regardless of their color, brightness, contrast, even if sharp edges are absent. The stage is very robust and self-tuning. 



___


| Previous Step                 | Jump To                 | Next Step                                   |
| ----------------------------- | ----------------------- | ------------------------------------------- |
| [Camera Lighting](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Camera-Lighting) | [Table of Contents](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration) | [[Issues and Solutions]] |