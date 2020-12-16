This is a collection of frequently asked questions specific to designing, building, and assembling a machine. For questions more specific to the OpenPnP software or running the machine, see the main [[FAQ]].

See also:
* [Pick-and-Place Design Comparison](https://github.com/openpnp/openpnp/wiki/Pick-and-Place-Design-Comparison)
* [Mechanical Issues (aka Debugging)](https://github.com/openpnp/openpnp/wiki/Mechanical-Issues-(aka-Debugging))

## 101

### Questions Before You Start
1. What is your budget?
    * Don't expect to build something for as cheap as a 3D printer from Amazon.
1. What materials will you be using?
1. What fabrication methods do you have access to?
1. What are the machine's required capabilities?
    * Be very specific about the types of PCBs that you want the machine to assemble. PCB/panel dimensions (even the thickness) and mounting methods, number of different types of components, and component sizes.
    * Be realistic about component sizes.
        * Do you really need to place 144-TQFP (large, high Z-rotation accuracy and precision requirements) or is a 48-TQFP or 32-QFN realistically enough?
        * What about large connectors, is hand-placing them OK?
        * 1x1 9-BGA? 0603? 0402? hint: 0201 and below are unlikely to be possible unless you're an expert and putting a lot of work into the machine - in which case why are you reading this?!
1. How fast does the machine have to operate?
    * Is speed even a factor to you - is it OK if it takes all night to populate a PCB?
    * Due to the weight of the head on PnP machines, momentum can be an issue at higher speeds.
    * The faster the speed, the more rigid and heavy the machine needs to be. It's an exponential relationship; doubling the speed will quadruple the rigidity requirements of the entire machine.

### Gotcha's To Be Aware Of
1. All Z heights that the pickup nozzle needs to pick from or place on, must be exactly the same. Specifically, this means that the top surface of all components in feeders must be equal, and the top of the PCB has to be very close also.
    * Within a millimeter isn't good enough here - the Z heights have to be very close to making a single plane across the entire machine. This may sound easy, but across a large machine with a lot of removable feeders of different types, this can be difficult to achieve.
    * This goes for PCBs of different thicknesses too (even 1.6mm vs 1.0mm may be too much depending on your camera and lenses depth of field), though computer vision can help somewhat here.
    * That means you're going to want to design in a way to precisely adjust the Z-height of different parts of your PnP
1. The pickup nozzle at the end of the Z-axis ultimately has to be the most accurate and precise part of the machine as it's the 'business end'. It's also the part at the end of the mechanical chain (Y-axis -> X-axis -> head -> Z-axis -> pickup nozzle), so will be impacted by issues further up the chain.
1. There will be a lot of cable management to do. Plan for how you're going to get all the wires and pneumatics to the machines head, especially the USB cable(s) (atleast one for the top-vision camera, and potentially a controller) as they are not designed for repeated flexing.
1. Lighting is incredibly important for computer vision, and it's very hard to get good lighting. It's more important than you think, and most likely more important than you're thinking right now reading this. A ring light around the camera is no where near good enough - it's usually unusably bad actually, and a diffuser is rarely enough to save it (but you shouldn't expect to try for something this simple).
1. You will likely be buying some of the more complex machine components (like the entire machine head), and that's not cheap. The OpenPnP community have somewhat settled on a few common designs that various people and companies have made available, but if you have the time and capabilities you're more than welcome to design and build your own too.

### What kind of PC will I need?

OpenPnP has been used on all manner of computing hardware. OpenPnP is written in Java and makes use of OpenCV. Hardware that runs Java and OpenCV well will generally run OpenPnP well. An i5 wwith 8GB RAM is a good minimum requirement, but OpenCV performance is not critical at all as it is not a bottleneck in the operation of OpenPnP. You can run OpenPnP on other things but this is a sensible recommendation for an easy machine to start with.

The one hardware constraint worth mentioning has to do with USB Cameras. In order to use more than one USB camera (which is required for all but the most basic and least-capable of pick-and-place machines), you need a machine with more than one USB bus/root hub. On many machines, and especially laptops, all USB ports will share a single USB bus and this does not provide sufficient bandwidth for multiple simultaneous cameras to operate. Please read the documentation on the [[OpenPnpCaptureCamera]] driver for more details.

## Design vs Build

### Design
Even if you choose the path of building an existing design, you will need to understand [accuracy and precision](https://en.wikipedia.org/wiki/Accuracy_and_precision) and how they combine to determine the repeatability of the final machine. It should be considered a pre-requisite of any build.

A few aspects that affect a machines accuracy and precision
* Rigidity
* Squareness
* Backlash, or 'slop'
* Temperature(!)
* Beam deflection
* Machine dimensions
* Mass
    * Both of the machine and its components (i.e. head, X-axis which includes the head, Y-axis which includes the X and head, machine base)

### Build (Existing Designs)
See http://openpnp.org/hardware/ for a number of options. 

The current recommendation for a build guideline is:
* [Stephen Hawes's Index](https://github.com/sphawes/index/). The design and build of every aspect has been extensively documented in a great, high-quality YouTube series.
* [DIY Pick and Place](https://hackaday.io/project/9319-diy-pick-and-place) for frame and X/Y.
* [ICEpick Direct Drive Head](https://github.com/BETZtechnik/ICEpick--Direct-drive-pick-and-place-head/wiki) for the head.
* [Quick Change Juki Nozzle Holders](http://www.betztechnik.ca/store/p32/Quick_change_Juki_nozzle_holders-_NEMA_8_5mm_OD_hollow_shaft-_STOCK.html) for nozzle holders and nozzles.
* [Smoothieboard](http://smoothieware.org/getting-smoothieboard) or [Cohesion3D](http://cohesion3d.com/cohesion3d-remix/) for motion control. The [[Motion Controllers]] page also has additional information about this important component.
* [ELP Model USB100W03M, 720p USB Cameras](http://www.elpcctv.com/hd-720p-usb-cameras-c-85_87.html) which can be purchased on Amazon, AliExpress and eBay. Lenses are standard M12 mount and can be replaced to customize for your machine's geometry. See below for lens and positioning recommendations.
* Yamaha CL Feeders (available on Aliexpress and [Robotdigg](https://www.robotdigg.com/product/829/CL82-or-CL84-Feeder-4-OpenPnP)) if you want auto feeders. Make sure to get a [mounting block](https://www.robotdigg.com/product/1190/Pick-and-place-machine-Feeder-mounting-block), too. 

## Mechanical

// TODO

belts vs leadscrews  
different types of each  
feeders. Should be broken out into its own page as there's so much to cover.

### Mounting Vacuum Hose to NEMA-8 Motors
Most home built systems use NEMA-8 stepper motors for nozzle rotation. To get vacuum to the nozzle on the motor shaft, there are three common options:

1. Slide the vacuum hose over the back shaft directly: This works fine, and is how many Chinese machines work. The downside to it is that you do not get 360 degree rotation and it puts a little extra load on the motor shaft. Since these motors don't have much torque to begin with, this may cause extra current and heating. But again, it works.
1. Use a rotary fitting coupler: Places like RobotDigg sell a rotary fitting that will mount to the back of the shaft and the hose. The coupler is airtight and puts very little strain on the motor.
1. Use a "cap": The OpenBuilds reference design includes a [3D printed cap](https://github.com/openpnp/openpnp-openbuilds/blob/1.0/Mechanical/3D%20Printed/STL/OpenPnP-OpenBuilds-C-Mount.stl) for the back of the motor. The cap accepts the vacuum hose and using an o-ring and a little silicone grease creates a very low friction, airtight fitting.
    > A common question for the cap style is whether the bearings of the motors will leak vacuum, and the answer is: No, or not enough to matter. Additionally, you don't have to do anything fancy to the 3D print to make it airtight. Just print and go.

## Vision
A normal machine designed for OpenPnP will have 2 USB cameras, one on the placement head looking down and one in the machine bed looking up at the placement nozzles. The top camera is used for looking at components in feeders, and fiducials on PCBs and the machine bed. The bottom camera is used for determining the orientation (and presence!) of picked components so the orientation can be corrected prior to placement.

The resolution of the camera (e.g. 1280x720), its sensor size (e.g. 1/4"), distance from the object it's imaging, and lens all affect the maximum area that the camera will image, and the resolution (pixels per millimeter) of that image. The area will affect the maximum size of components that can be imaged, and the resolution will affect the minimum detail size (e.g. pads on fine-pitch components). Ideally you want at least 10 pixels to cover the minimum feature size of a component.

### Lighting
Great lighting is critical to good computer vision results. You shouldn't aim for good-enough lighting, as it's been our experience that few people's good enough in this area is anywhere close to good enough in reality. Expect to do significant research and design in this area, as it's very important and hard to get right.

### Backgrounds (nozzles, feeders)
You want to have the background behind what's being imaged to be easily identifiable so it either doesn't impact the computer vision or can be removed from the image by OpenPnP's vision pipeline. Practically, this means that it's best if all your feeders are the same colour (so don't use random filament colours if you're 3D printing them).

### How Far Should Cameras Be Mounted From Objects
This is a hard to answer question since it's specific to the machine, the camera, the lens, the size of parts you want to place, etc. A good guideline is:
* Top Camera: 8mm lens, 100mm distance from board.
* Bottom Camera: 3.2mm or 3.6mm lens, 30-40mm distance from nozzle tip.

M12 lenses are cheap, so we recommend picking up a couple sizes to experiment with. 

The [Vision Doctor optics basics page](https://www.vision-doctor.com/en/optical-basics.html) and [lens calculators](https://www.vision-doctor.com/en/optical-calculations.html) can help with this decision.

## Pneumatics

// TODO
