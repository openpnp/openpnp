# **THIS PAGE IS UNDER CONSTRUCTION**

## Loose part feeder: Why it is different

Most types of SMT feeders are designed to make use of parts' packing configurations, which in turn are designed to facilitate transport and storage of the parts, and also picking and placing of the parts. Paper or plastic carrier tapes are the most common packing configurations for SMT parts, while tubes, trays et.c. are also used for larger parts.  Packing specifications describe how parts are placed into their carriers so that a part's location and orientation is known in advance, before placing: 

![stripfeeder_mockup1_cropped](https://user-images.githubusercontent.com/1109829/27771876-54a8df70-5f60-11e7-9776-888ef5c0ba3b.png)

_8mm tape strips and holder._

A loose part feeder is different: It works with loose parts scattered on its surface in no particular order. A loose part feeder can be populated with parts that fell off their carrier, ejected by the machine, used parts et.c.:

![loosepartfeeder_mockup_all](https://user-images.githubusercontent.com/1109829/27770342-b4201920-5f45-11e7-8e83-6974acbd075c.png)

_A 2x3 loose part holder_

A loose part feeder's job is to detect the position and orientation of the parts, so that they can be picked and placed correctly on the PCB board. The task of detecting parts is assumed by the vision system, which uses the down looking camera (AKA the [Top Camera](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration:-Top-Camera-Setup)) to take images of the parts for image processing and model detection.

## The AdvancedLoosePartFeeder
### What is needed
OpenPnP job processor expects a list of rotated rectangles, properly oriented, as the output of a loose part feeder.

Achieving proper orientation is not trivial. Simply modeling parts with rotated rectangles is not sufficient for proper part detection, given the inherent uncertainty in the rotation of a RotatedRect model. The following images show the limitations of such a simple model:

![referenceloosepartfeeder1](https://user-images.githubusercontent.com/1109829/27795792-210cd6b6-6010-11e7-87b3-73e9aba35199.png) ![referenceloosepartfeeder3](https://user-images.githubusercontent.com/1109829/27795793-210ffa3a-6010-11e7-9f20-41543d5d6532.png)
_Detection of SOT223-3 and C1206 packages using simple `RotatedRect` models. Notice the orientation lines of the parts pointing to different/wrong directions._

Evidently, detecting polarized parts require more than fitting rectangles on part outlines, e.g. taking into consideration surface features, text marking, pin1 marks, asymmetries et.c. Additionally, there should be a notion of what the software considers "proper" orientation in respect with the conventions used by the CAD program that designed the PCB and put the parts on it. The following images show the desired result:

![referenceloosepartfeeder2](https://user-images.githubusercontent.com/1109829/27795795-2125676c-6010-11e7-8065-1e638a06726d.png) ![referenceloosepartfeeder4](https://user-images.githubusercontent.com/1109829/27795794-2118bbac-6010-11e7-95e2-78836f99b46e.png) ![referenceloosepartfeeder5](https://user-images.githubusercontent.com/1109829/27795796-215df37a-6010-11e7-9c33-a02555105c4a.png)


As mentioned above, proper orientation when detecting a part requires knowledge in advance by the feeder of what is considered to be "proper"; therefore, there has to be some kind of training of the feeder to recognize and match "proper" orientation. Even in the case of non-polarized parts, when trying to orient a part in a "square", horizontal or vertical, position, the feeder needs to know whether "portrait" or "landscape" mode is desired.
 
## Adding an AdvancedLoosePartFeeder

Loose part feeders [can be added to OpenPnP](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration:-Feeders#adding-a-feeder) through the `Create feeder` button in the _Feeders_ tab:

![add_feeder](https://user-images.githubusercontent.com/1109829/27770481-162114ce-5f48-11e7-8cff-4c894f731137.png)

The two feeders are listed in the popup dialog:

![screenshot](https://user-images.githubusercontent.com/1109829/27770426-403790b8-5f47-11e7-90f7-ce7db6064a3a.png)

Both of these feeders use the OpenCV based [CvPipeline](https://github.com/openpnp/openpnp/wiki/CvPipeline) to do image processing. The CvPipeline works by separating the image pixels belonging to parts from the background using image processing techniques and deliver a model of the parts located in the coordinate space of the PnP machine.

## How a loose part feeder works

(To be continued)