# What is it?

Vision Compositing a.k.a. Multi-Shot Bottom Vision can take more than one vision shot to determine the alignment of a part. 

![multi-shot](https://user-images.githubusercontent.com/9963310/178464287-c1100a86-b81e-40f3-b11d-b736822f7e1c.gif)

The following functions are provided:

- Align packages that are larger than the camera view.
- Align packages that are not rectangular in their overall shape. 
- Improve accuracy by centering features in the camera view, avoiding errors from parallax, viewing & lighting side angles, and residual lens distortion. 
- Align packages based on pad corners facing inwards, i.e. parts that are asymmetric on the outside, or so large you can't capture their outside corners.
- Support the Pipeline Editor to cycle through multiple shots.

# Motivation

## Camera Design

Between tiny 0201/0402 passives and enormous LQFP-256 packages, it is quite hard to get the bottom camera view right. One has to trade resolution against view size. Attempting to escape this dilemma by using a higher resolution camera comes at a significant additional cost in processing power, more lighting required, more compression artifacts, or (worse) reduced frame rate (fps) and additional camera lag. 

Wanting a large camera view also adds constraints to the machine build. The ideal camera has a long focal length ("telephoto") lens to minimize parallax errors, which means it has to be far away from the subject. However, this usually means building a taller machine, especially for table-top designs. Consequently, designers often use (very) wide-angle lenses, which are detrimental to alignment accuracy (see the next section). 

Ironically, the very large parts are often quite rare in projects, just the _one_ MCU, for example. The overwhelming majority of parts are rather small. It hurts to make poor tradeoffs for the few exceptions.  

Vision Compositing helps with all these problems. 

Cameras can now be optimized to the brunt of the work with medium and small sized parts. You can use medium resolution cameras with high fps, longish focal length lenses and still get a reasonable compact table-top design. A narrower view means more accuracy, and faster convergence on multi-pass alignment. 

The few large parts can then still be aligned with multiple shots. Obviously, there is an extra cost in alignment time, which may or may not be compensated by avoided tradeoffs elsewehere. 

## Improving Accuracy

As an independent benefit, multi-shot alignment increases accuracy, particularly for very large parts. In some cases it might be the key to successful placement, regardless of the package actually being too large for the camera view. 

The following illustration (exaggerated) shows how a slightly tilted nozzle might result in large placements errors (red) due to large parallax errors in (very) wide angle lenses:

![illu](https://user-images.githubusercontent.com/9963310/178497746-51f0a470-8410-4cfd-b95e-0bb22a44c74a.png)

Detecting the same corners using two shots reduces these errors to nothing, as the parallax is negligible when looking straight up from the camera center, and even what little remains, is symmetric left and right, and cancels itself out.

Errors from viewing pins from the side, from having inconsistent light reflection across viewing angles, or from residual lens distortions, can also be reduced by always centering the relevant features in the camera view.

## Odd Shaped Packages

Multi-shot bottom vision also enables alignment of some non-rectangular (hull) parts (best see the video for examples). 

# Instructions for Use

For an easy overview, [please watch the video](https://youtu.be/P-ZudS7QQeE).

## Camera Roaming Radius

A **Camera Roaming Radius** of zero (default) effectively switches off Vision Compositing.

A **Camera Roaming Radius** of more than zero enables Vision Compositing. The nozzle is allowed to hold the part anywhere within this radius of the bottom camera location. 

![Camera Roaming Radius](https://user-images.githubusercontent.com/9963310/178479776-158df3f9-e5f1-45a0-8b17-c9bf554d4a57.png)

Within the **Camera Roaming Radius**, the nozzle is allowed to move freely in X, Y, i.e. without going to Safe Z first. This is also used to optimize in-camera drag-jogging. However, this is **not** a soft-limit, you still have to be careful not to bump your nozzle and/or part into any obstacles near your camera. 

The radius must accommodate both the nozzle distance from the camera center, and the part protruding from the nozzle. This is automatically observed during Vision Compositing, taking the the hull of the package footprint into consideration. Because the part is typically held in its center, and because the corners are the package's obvious multi-shot targets, this means that part sizes are more or less constrained to _those with diameters smaller than the **Camera Roaming Radius**_. 

The following illustrates an LQFP-144 package within a 30mm camera roaming radius: 

![Roaming as a limit](https://user-images.githubusercontent.com/9963310/178480317-770b6801-9625-42cf-b4f1-723c55e08caa.png)

**CAUTION**: Your camera "pit" must physically provide this freedom of movement, when the nozzle tip and part is at Camera Z (focal plane), and with the worst pick tolerances and other deviations to spare. 

## Other Machine Preparation

It is recommended to enable the **Align with Part?** option on the nozzles:

![Align with part switch](https://user-images.githubusercontent.com/9963310/178480834-200e20d4-8640-46c4-8143-14903fdadc3d.png)

It offsets the nozzle rotation coordinate to match that of the part, once the part is aligned. This means that the cross-hairs and the DRO are nicely aligned with the part. This is especially useful in the multi-shot scenario, where you often only see a fraction of the whole part, and the reticle (cross-hairs, grid etc.) provides valuable visual feedback. 

Furthermore, revisit the [Nozzle Tip Configuration](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration_Nozzle-Setup#nozzle-tip-configuration), **Max. Part Diameter** and **Max. Pick Tolerance** are important inputs for the multi-shot operation. 

## Package Footprint

In order for Multi-Shot Bottom Vision to work, the footprint must be defined. Most common footprints can easily be generated from very few datasheet parameters, right inside OpenPnP:

![Footprint generator](https://user-images.githubusercontent.com/9963310/178484759-bdcc34c4-feba-48d2-8b79-9dd87338d8ec.png)

Note: the Eagle E-CAD importer can also import the pads along with the packages. 

## Package Vision Compositing

Basic operation should be automatic. Given the **Camera Roaming Radius**, the multi-shot is enabled. It will automatically be employed with parts nearly as large as the camera view. 

The package **Vision Compositing** tab can give a quick preview of the footprint and its treatment by bottom vision:

![Vision-Compositing-Tab](https://user-images.githubusercontent.com/9963310/178488589-01a1725a-2af1-4182-b73d-b069dac64e2d.gif)

You can hover your mouse over the shots, indicated by corner marker and masking radius. It then indicates the camera view rectangle according to your camera size. The thick red/dashed circle around the package indicates the **Camera Roaming Radius**, i.e. where the nozzle and part can be.

You can press the mouse to see how the pads are fused together, where they are too close to isolate. Only the outside corners of these blocks can be considered as shot locations. But even these are subject to many more constraints.

### Compositing Settings

**Method** determines how the footprint is treated:

![Compositing Method Drop-Down](https://user-images.githubusercontent.com/9963310/178490264-70c62247-3998-4e9f-ad26-5e393393df3f.png)

- **None**: the part is aligned with one shot, even if its size or other properties of the footprint would suggest otherwise. This can be used to basically enforce the old behavior. It might be a way to tell OpenPnP to still align select packages with one shot, even if the footprint (plus tolerances) are actually overlapping the camera central circle. Or when pre-rotate is switched off and the part just happens to fit inside the rectangular camera view.
- **Restricted**: the part is always aligned with one shot if it fits inside the camera view (central circle), regardless of other properties of the footprint, like convex pad patterns. This is the default method, intended to provide continuity for the majority of packages. 
- **Automatic**: the part is aligned with one shot if it fits inside the camera view (central circle), and if its corners are understood to be symmetric and convex, to be capture as one bounding rectangle. If this is not the case, a multi-shot is automatically performed. It might combine pairs of corners into "bracket" shots.
- **SingleCorners**: every isolated corner is shot individually. This can be used for the best accuracy, for the following reasons: 
  - Only the camera center , i.e. a perpendicular "ray" up Z will provide positional information. Parallax as well as view and light side angle errors are minimized. 
  - More shots reduce random errors, staticstically.

**Extra Shots** determines how many extra shots are taken for this package. The minimum is automatically computed from the geometry of the pads. For example, a square package shape can be aligned with just two corners shots, but the opposing two corners are made available as extra shots, to improve accuracy.

**Max. Pick Tolerance**: determines by how much a package, or rather its corners, can deviate after the pick. When set to zero, the analog setting on the nozzle tip is taken. If non-zero, this value overrides the nozzle tip's. The larger this tolerance, the more the corners must be isolated from other, obstructing corners, or from the limits of the camera view. Finding a solution can therefore fail, if tolerance are too large. 

**Min. Angle Leverage**: determines how far away two corners have to be, in order to define the detected angle of the package, _relative_ to the dimension of the package (the lesser of width and height). A setting of 0.5 means that the corners must be half-way apart across the package. 

**Allow inside corner?**: determines if inside corners are allowed. For very large parts, where even the **Camera Roaming Radius** is not enough to position the part to its corners, the alignment might still be gleaned from the inside corners of large pads.

![Inside corners](https://user-images.githubusercontent.com/9963310/178497553-a355b84d-40ad-4462-9e3d-8402c38ca6c4.png)

## Using in the Pipeline Editor

If a Vision Compositing pipeline has multiple shots, you can cycle through them using the **Step** button:

![Pipeline Editor Shots](https://user-images.githubusercontent.com/9963310/178543041-1937df53-7fd1-4579-b2dc-411bf650256f.png)

Note, the pipeline is the same for all the shots, i.e. any modifications to stages and stage properties will affect all shots, and must work with all of them. It is only the input image (camera subject position) and some _externally_ controlled pipeline and stage properties that will change between shots. 
