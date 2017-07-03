# **THIS PAGE IS UNDER CONSTRUCTION**

## Loose part feeder: Why it is different

Most types of SMT feeders are designed to make use of parts' packing configurations, which in turn are designed to facilitate transport and storage of the parts, and also picking and placing of the parts. Paper or plastic carrier tapes are the most common packing configurations for SMT parts, while tubes, trays et.c. are also used for larger parts.  Packing specifications describe how parts are placed into their carriers so that a part's location and orientation is known in advance, before placing: 

![stripfeeder_mockup1_cropped](https://user-images.githubusercontent.com/1109829/27771876-54a8df70-5f60-11e7-9776-888ef5c0ba3b.png)

_8mm tape strips and holder._

A loose part feeder is different: It works with loose parts scattered on its surface in no particular order. A loose part feeder can be populated with parts that fell off their carrier, ejected by the machine, used parts et.c.:

![loosepartfeeder_mockup_all](https://user-images.githubusercontent.com/1109829/27770342-b4201920-5f45-11e7-8e83-6974acbd075c.png)

_A 2x3 loose part holder_

A loose part feeder's job is to detect the position and orientation of the parts, so that they can be picked and placed correctly on the PCB board. The task of detecting parts is assumed by the vision system, which uses the down looking camera (AKA the [Top Camera](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration:-Top-Camera-Setup)) to take images of the parts for image processing and detection.

## ReferenceLoosePartFeeder and AdvancedLoosePartFeeder

OpenPnP, at present, provides two loose part feeders, namely the _ReferenceLoosePartFeeder_ and the _AdvancedLoosePartFeeder_. Though they both feed loose parts, their methods for detecting parts are different:
- _ReferenceLoosePartFeeder_ offers simple detection for non-polarized parts with a +-45° orientation range. Only the shape outline of the part is considered for detection while surface features, marks, assymmetries et.c. are ignored.
- _AdvahcedLoosePartFeeder_ offers template matching detection for both polarized and non-polarized parts with a 360° orientation range. Surface features, marks, asymmetries et.c. are matched to determine true orientation.

## Adding a loose part feeder
Loose part feeders [can be added to OpenPnP](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration:-Feeders#adding-a-feeder) through the `Create feeder` button in the _Feeders_ tab:

![add_feeder](https://user-images.githubusercontent.com/1109829/27770481-162114ce-5f48-11e7-8cff-4c894f731137.png)

The two feeders are listed in the popup dialog:

![screenshot](https://user-images.githubusercontent.com/1109829/27770426-403790b8-5f47-11e7-90f7-ce7db6064a3a.png)

Both of these feeders use the OpenCV based [CvPipeline](https://github.com/openpnp/openpnp/wiki/CvPipeline) to do image processing. The CvPipeline works by separating the image pixels belonging to parts from the background using image processing techniques and deliver a model of the parts located in the coordinate space of the PnP machine.

## How a loose part feeder works

(To be continued)