# What is it?

Computer Vision is used throughout OpenPnP to detect exactly where things are in relation to the cameras, sometimes to just determine _if_ they are there, and sometimes to read text (OCR) or codes (QR-Codes). Computer vision mostly uses a facility called Pipeline to perform the detection. The Pipeline is structured in so-called Stages that the user can configure in a special Pipeline Editor. 

This page links to detailed instructions for some tasks around Computer Vision. 

## Using New Stock Pipelines

OpenPnP ships with stock pipelines that you can use or adapt to your liking. When you update OpenPnP the newest stock pipeline is made available, but it is not automatically replacing your pipelines, as this would likely break your adaptations, of course. Therefore, if you want to upgrade to the newest stock pipeline, and replace yours, it takes a few steps. This is explained for the Bottom Vision pipeline here, but it is similar with other vision settings. 

1. If you're sure you want the new stock pipeline, just jump to step **7.**, otherwise we'll test it first, as follows:
1. Go to the **Vision** tab and press the **Copy** button on the Stock settings.
  
   ![Press Copy](https://user-images.githubusercontent.com/9963310/155680959-790b3784-137a-4f60-bb10-b89668c0ae81.png)

1. Then press the **Paste** button.
1. Give it a telling name.
   
   ![Name new vision settings](https://user-images.githubusercontent.com/9963310/155681016-e6d26551-8807-42fc-b6ab-7701b6cefb93.png)

1. Then go to a test Part or Package and assign it in the **BottomVision** column of the list:

   ![Assign to part](https://user-images.githubusercontent.com/9963310/155681065-aaf69d58-5d74-40d5-a6f6-b244b414116f.png)

1. Then test the new settings by using **Test Align** etc. 
1. You can also _directly_ set the new Stock pipeline on the "Default Machine Bottom Vision" settings by pressing the **Reset** button:

   ![Set Default to Stock](https://user-images.githubusercontent.com/9963310/155681138-2087b511-46fa-48c6-9fe3-61ba0be88aa4.png)
    


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