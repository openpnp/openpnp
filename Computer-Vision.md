# What is it?

Computer Vision is used throughout OpenPnP to detect exactly where things are in relation to the cameras, sometimes to just determine _if_ they are there, and sometimes to read text (OCR) or codes (QR-Codes). Computer vision mostly uses a facility called Pipeline to perform the detection. The Pipeline is structured in so-called Stages that the user can configure in a special Pipeline Editor. 

This page links to detailed instructions for some stages. 

# Editing Pipelines

The usage of the Pipeline Editor UI in general is explained on the [[CvPipeline]] page.

# Stages

* [[DetectCircularSymmetry]] searched the image for Circular Symmetry. It can be used to detect round things, regardless of their color, brightness, contrast, even if sharp edges are absent. The stage is very robust and self-tuning. 



___


| Previous Step                 | Jump To                 | Next Step                                   |
| ----------------------------- | ----------------------- | ------------------------------------------- |
| [Camera Lighting](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Camera-Lighting) | [Table of Contents](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration) | [[Issues and Solutions]] |