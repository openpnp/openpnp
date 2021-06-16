# Introduction

## What is it?

A step up from the [[ReferenceStripFeeder]], this light 3D-printed feeder can hold and reload cut pieces of SMT tape and protect the parts using a sliding cover. The cover can be opened/closed automatically by the nozzle tip. Feeders are printed in whole arrays. Uses computer vision and fiducials for setup and operation. Intended for prototyping and very small runs.

![BlindsFeeder on Wood](https://user-images.githubusercontent.com/9963310/72673056-398ef700-3a65-11ea-92c5-51b565d0a16a.jpg)

![BlindsFeeder-Open-Close](https://user-images.githubusercontent.com/9963310/72674123-85489d00-3a73-11ea-8f32-46258859b0a4.gif)

Watch the Quick Demo Video:
https://youtu.be/dGde59Iv6eY

## Features and Use Cases

1.	Holds cut tape strips. Indexed to sprocket holes.
2.	Can be reloaded/reused.
3.	For taping/gluing down on a sturdy surface.
4.	Fast 3D print using an OpenSCAD parametric model. 
5.	No additional parts or tools needed (except tweezers, double-sided tape). 
6.	Multiple feeders can be printed in one array, even mixed in type.
7.	Covers prevent parts from falling out: allows off-machine preparation and storage. 
8.	Two cover types: blinds cover and push-away cover.
9.	Blinds cover for tapes where part pockets are spaced out (e.g. most passives on paper carrier tape). The cover, having openings at the same pitch, can be shifted by half pitch to close up the parts. Uses almost no extra space on the machine table. 
10.	The push-away cover can be used for tapes with tightly spaced parts (e.g. larger parts in embossed plastic carrier tapes). The cover will be pushed away from the parts as they are used up. Uses extra space for the pushed-away cover.
11.	Both covers are simply opened/closed using the nozzle tip. 
12.	Nozzle tips pushing can be allowed/disallowed.
13.	All covers can be opened at Job start i.e. a special (sturdy) nozzle tip can be used to open all the covers. ([Travelling salesman solver](https://en.wikipedia.org/wiki/Travelling_salesman_problem) implemented to optimize the machine motion path).
14.	If nozzle tip pushing is not wanted, the feeder supports manual cover operation with vision-based “is open” check. 
15.	Resilient "Green Screen" vision system (that's why the feeders must be green).
16.	Fiducials to accurately vision-calibrate the feeder local coordinate system (all the feeders in the array at once). Lazy evaluation, once per homing automatic calibration. 
17.	Vision based “one-click” tape setup for the blinds cover: pocket centerline, pocket size, pocket pitch (for the push-away cover, the pocket pitch must be entered manually). 
18.	Vision based calibration of the blinds cover push edges. 
19.	Tolerates mechanical imprecision of all kinds, including attaching feeders to the machine by simple means (+/-2mm tolerance). Supports full affine transformation (position, rotation, optionally shear). 
20.	High accuracy and vision robustness by using knowledge of the EIA-481-C standard for carrier tapes and the 3D printing model, i.e. all the important local coordinates are made to be integral 1mm, 2mm or even 4mm multiples. 
21.	Shared properties of the arrayed feeders (such as fiducial locations etc.) are automatically synced.  
22.	Can later move/rotate a whole feeder array on the machine table, by recapturing one fiducial (move) or two (move and rotate). 
23.	Z height capture.

# Setting up a Feeder

## Modelling the Feeder

The feeder is created using the cool Open Source program [OpenSCAD](https://www.openscad.org/downloads.html). Download and install it to proceed.  

![grafik](https://user-images.githubusercontent.com/9963310/73119976-213d4180-3f69-11ea-8557-40611f907be6.png)

To get the feeder's OpenSCAD model files, it's easiest to extract them from OpenPNP, so we need to add the first BlindsFeeder right now: 

![grafik](https://user-images.githubusercontent.com/9963310/73119226-0c5bb080-3f5f-11ea-95f8-8cfcd125d57f.png)

Then press the Extract 3D-Printing Files button. OpenPNP will ask you where to save and then open the two files.

![grafik](https://user-images.githubusercontent.com/9963310/73119779-1386bc80-3f67-11ea-8eeb-6df80b2cdd79.png)

You can also get the newest ones from GitHub (i.e. regardless of the version of OpenPNP you have).

* https://raw.githubusercontent.com/openpnp/openpnp/develop/src/main/resources/org/openpnp/machine/reference/feeder/BlindsFeeder-Library.scad
* https://raw.githubusercontent.com/openpnp/openpnp/develop/src/main/resources/org/openpnp/machine/reference/feeder/BlindsFeeder-3DPrinting.scad

There are two files, the `BlindsFeeder-3DPrinting.scad` and the `BlindsFeeder-Library.scad`. Go to the first one, which is used to customize the actual feeder 3D-print. Just press the Preview button to see the standard example:


![grafik](https://user-images.githubusercontent.com/9963310/73120747-19ce6600-3f72-11ea-9494-131af8711339.png)

Rotate the preview for the Y axis to point downwards to get the standard view (why, will be explained later). 

### TapeDefinition

In the source code window, you see several `TapeDefinition`s. You can use those to define the geometrical properties of your SMT tapes and assign names to them. You can have as many as you like, and there is no need to delete any, as you can later decide which to print and which not. The idea is that you will build up a repository of proven `TapeDefinition`s over time. 

![grafik](https://user-images.githubusercontent.com/9963310/73120954-f2c56380-3f74-11ea-836c-487b2edcae4e.png)

The parameters (and there are more available) are all documented in the `BlindsFeeder-Library.scad` file. We'll only explain the most important ones here.

![grafik](https://user-images.githubusercontent.com/9963310/73120618-0e2e6f80-3f71-11ea-9f4f-9a11b8229e85.png)

Measure your tapes or look at datasheets for parts/tapes that you don't have yet. There are [generic datasheets](https://www.analog.com/media/en/technical-documentation/white-papers/tape-and-reel-packaging.pdf) that may also help.

Use the _nominal_ `tape_width`: must be 8mm, 12mm, 16mm etc. (increments of 4mm). Any inaccuracy in the real tape width must **not** be entered here. Instead, we will adjust these later, using `tape_play`. 

Use the _nominal_ `pocket_pitch`: must be 2mm, 4mm, 8mm, 12mm etc. (increments of 4mm).
 
Get the `tape_thickness`, `pocket_portrusion` from the datasheets or measure from the physical tape. You probably need a caliper. But don't worry too much, the printer can only resolve this in layers of e.g. 0.2mm and that's good enough.

The `pocket_width` is the width the physical pocket across the tape. You can make it a bit larger, as long as there is space. The model will make the blinds opening in the cover slightly larger than that.

`tape_play`, `cover_play` (negative or positive) are empirical. Start from the examples. There is more about those in the [Advanced](#Advanced) section.

### Building up the Feeder Array

Once you've defined your `TapeDefinition`s, you can multiply and mix them to build up a feeder array i.e. print many feeders in one piece (at end of the `BlindsFeeder-3DPrinting.scad` file):

![grafik](https://user-images.githubusercontent.com/9963310/73120868-66ff0780-3f73-11ea-90d3-940594bee1ff.png)

The `tape_length`, should again be specified in multiples of 4mm. 

The `arrayed_tape_lanes` and the `arrayed_tapes` contain the number and definitions of tapes to be arrayed. Just set the lanes entry to `0` to not print a tape definition at all (no need to delete slots from the arrays).

You can set `debug=true` to see how the feeder will look when the covers are mounted. 

## Creating the STL File

Once you're ready, press the Render button. This will now calculate a true solids model. Takes a while. 

![grafik](https://user-images.githubusercontent.com/9963310/73120998-8860f300-3f75-11ea-9bf5-1226204737c2.png)

Then press Export as STL:

![grafik](https://user-images.githubusercontent.com/9963310/73121034-f9080f80-3f75-11ea-8472-a3293d5004d6.png)

## 3D Printing the Feeder

Import the STL into your slicer. PrusaSlicer for a PRUSA i3 MK3 is the example here:

![grafik](https://user-images.githubusercontent.com/9963310/73121124-01147f00-3f77-11ea-821b-2c1633de0b52.png)

### Print Settings

The Print Settings (derived from 0.2mm SPEED MK3) have only been changed minimally (indicated by the orange Unlocked Lock symbols):

![grafik](https://user-images.githubusercontent.com/9963310/73121167-59e41780-3f77-11ea-8c89-90a19fe6cd79.png)

The essential one is "Detect thin walls", that allows the slicer to create walls that are only one extrusion thin. 

Reducing the Solid layers settings speeds up the print (only for embossed plastic tapes). 

The extrusion widths have all been unified for printing speed. With these, the slicer will not disrupt the fast rectlinear filling pattern across multiple tape lanes of varying height. 

![grafik](https://user-images.githubusercontent.com/9963310/73121202-1342ed00-3f78-11ea-982d-c4fb639171e4.png)

### Filament Settings

You need a vivid green filament to allow for the "green screen" computer vision effect that the BlindsFeeder uses extensively. Other vivid colors will work too, but you will have to more profoundly tweak the OpenPNP vision pipeline. I recommend PETG because it is easy to print, doesn't smell and is surprisingly tough and elastic. 

Measure your filament. This is important for the precision we need (and for repeatability):

![grafik](https://user-images.githubusercontent.com/9963310/73121357-9add2b80-3f79-11ea-84af-1d3d5685e195.png)

Enter the Diameter and also make sure to set the correct temperatures given by the filament manufacturer. The [Extrudr PETG signal green](https://www.extrudr.com/en/products/catalogue/petg-signalgrun_1767/) I used, surprisingly needed 20°C less than the generic PETG profile (and it mattered).

![grafik](https://user-images.githubusercontent.com/9963310/73121306-07a3f600-3f79-11ea-9f8e-70b98b53d184.png)

### Slice and Export The G-Code

Use the slicer and then press Export G-code to write the print to the SD card.

![grafik](https://user-images.githubusercontent.com/9963310/73121514-0378d800-3f7b-11ea-8a71-36d390341d39.png)

**Side Note**: Don't try printing using USB. The filigrane structures of the BlindsFeeder create so many G-codes per time unit that the serial transmission and/or the G-code interpreter/motion planner are overwhelmed. The Printer slows down to a crawl, especially on the sprocket thorns. The quality of the 3D print is affected by this, the sprocket thorns come out differently. It still does that to a degree even from the SD card (guess that's the 8bit controller finally showing its limits). 

### 3D Print

Getting the first layer right, is probably the key element for success. 

Be sure to prepare the print bed with an adhesive. Having single extrusion structures on the first layer, it simply won't work without. I use a [3D-printing adhesive spray](https://www.3dlac.com/) with very reliable results. Use plenty, until the platter looks slightly wet for a moment. 

Also make sure your first layer calibration is right. Follow the [manual of your printer](https://help.prusa3d.com/article/ZhBlGFD9Ah-live-adjust-z).

Then go for it:

![grafik](https://user-images.githubusercontent.com/9963310/73121741-a894b000-3f7d-11ea-8aa3-e11e69d8ce6b.png)

After the print, you might want to add an anti-static coating and let it dry (for ESD sensitive parts, and for all 0402 packages, because those will fly out of pockets just by static forces).

## Mounting the Feeder 

Using double-sided tape or even glue, mount the feeder on a flat sturdy surface. The mechanical stability comes from the surface, not the 3D print. The flat surface is also essential to give the feeders the right tension against their covers (otherwise it will bend backwards and the covers will just fall out). 

![grafik](https://user-images.githubusercontent.com/9963310/73122117-5c4b6f00-3f81-11ea-8de8-12502589ef25.png)

You can directly attach the feeder to the machine table or use a detachable "slate" design. The BlindsFeeder nicely supports the later, because it can work with mounting tolerances of up to +/-2mm through the use of fiducials.

## Loading Tapes

Align with the sprocket thorns and wedge the tape in, sprocket side first. I use the smooth back of the tweezers to press both tape edges down. Cut to length. 

![grafik](https://user-images.githubusercontent.com/9963310/73123053-b9e4b900-3f8b-11ea-9744-7b18afa21b8f.png)

Make sure your feeder is now rock stable. If your "slate" is too light to damp vibrations, clamp it down on a table. 

Now remove the cover foil, carefully. It is usually no problem with 0603 and up, but for 0402 it can be tricky. 

## Closing the Cover

For 0603 and up: Align the cover with the blinds openings _between_ the parts (closed position) and press-snap it down. 

For 0402: Better to _very carefully_ slide the cover in from the side. Take your time. 

## Positioning and orienting the Feeder on the Machine Table 

The feeder position and orientation is defined by the four fiducials (the diamond shaped holes in the corners). The first fiducial is the one with the square besides it. Note the fiducial numbers from this graphic for later. 

![grafik](https://user-images.githubusercontent.com/9963310/73122287-72f2c580-3f83-11ea-8beb-7f3da3e8f459.png)

Feeding starts from the right edge, this side usually points towards the center of the machine table. 

Obviously the nozzle needs to be able to reach _all_ of the feeder area (and a bit beyond). 

Ideally the camera can reach all four fiducials. However the feeder will also work if it can reach the right half of the feeder (highlighted in yellow here). This way you can exploit more of the space on your machine table. The BlindsFeeder will automatically do the right thing, if you have defined and enabled your machine [Soft Limits](https://github.com/openpnp/openpnp/wiki/Machine-Axes#kinematic-settings--axis-limits). 

## Feeder Setup in OpenPNP

Once the feeder is mounted on the machine and loaded with tape (computer vision won't work without), we can go back to OpenPNP and the BlindsFeeder that we already added earlier (or just add a new one). 

You can give the feeder a name (use F2 on the list entry). A structured naming scheme, numbering the array and then individual feeder _on_ the array, is recommended. Something like "001_01 Resistors 1%". OpenPNP can sort feeders by name, so they will be properly grouped this way.  

You might want to enable the feeder.

### Pin down the Fiducials

To define the feeder array position and rotation, we need to capture three out of the four fiducials. You must use number 1 and 2 (see the graphic above), but you can freely choose 3a or 3b as the third. Usually 3b is the better choice (faster to reach). 

Capture them using the camera ![grafik](https://user-images.githubusercontent.com/9963310/73122674-c6ffa900-3f87-11ea-8e25-9397dbeae895.png) or the nozzle tip, if not reachable by the camera ![grafik](https://user-images.githubusercontent.com/9963310/73122686-f0203980-3f87-11ea-8b15-d4b0c1783caf.png). 

It is best to always press Apply after each capture, as there are some automatisms at play internally (that will come handy later).  

![grafik](https://user-images.githubusercontent.com/9963310/73122938-91a88a80-3f8a-11ea-9680-eb607389211b.png)

Once you pinned down the first feeder in the whole array, it becomes real easy.

### Auto Setup of the Tape

Roughly move the camera onto the parts in the tape and press Auto Setup. Using computer vision i.e. the "green screen" effect, OpenPNP will automatically recognize the blinds in the cover and determine the tape centerline, the pocket pitch and the pocket size. 

![grafik](https://user-images.githubusercontent.com/9963310/73123368-a0de0700-3f8f-11ea-98d1-f7c6fd46f0a0.png)

The BlindsFeeder implements the EIA-481-C standard and its own geometry makes sure, all the relevant specs are integral millimeter values. Therefore computer vision does not need to be super precise. 

### Setting the Z 

Like on other feeders, set the Part Z. For the BlindsFeeder this the surface of the tape. With paper tape, best capture it between the pockets. 

![grafik](https://user-images.githubusercontent.com/9963310/73123737-70986780-3f93-11ea-88df-8e1646aad3c9.png)

Lower the nozzle tip down until it barely touches the tape, then press the Part Z capture button.

![grafik](https://user-images.githubusercontent.com/9963310/73123684-b30d7480-3f92-11ea-9299-0d01613977f6.png)

### Allowing the Nozzle Tip to Push the Cover

Before the BlindsFeeder can automatically open and close a cover, you need to set up the Nozzle Tip's Push and Drag Usage:

![grafik](https://user-images.githubusercontent.com/9963310/73123775-fddbbc00-3f93-11ea-8263-ba4d73fc692f.png)

### Open and Close the Cover Edges

You can now press the Calibrate Cover Edges button to find the best pushing offsets for opening and closing the cover. OpenPNP uses computer vision to optimize this automatically.

Then test ![grafik](https://user-images.githubusercontent.com/9963310/73123845-befa3600-3f94-11ea-9910-c986cb925341.png) and ![grafik](https://user-images.githubusercontent.com/9963310/73123851-d6392380-3f94-11ea-8fdc-692476ae26a4.png).

Hint: use the Pick Location button to have a look with the camera:

![grafik](https://user-images.githubusercontent.com/9963310/73123886-36c86080-3f95-11ea-9c46-394041f340b5.png)

![BlindsFeeder-Open-Close](https://user-images.githubusercontent.com/9963310/72674123-85489d00-3a73-11ea-8f32-46258859b0a4.gif)

### Choosing when the Cover will be opened

You can choose when the cover will automatically be opened. 

![grafik](https://user-images.githubusercontent.com/9963310/73123956-d685ee80-3f95-11ea-8ef8-5341e6c447e3.png)

* **Manual**: The user is responsible to open the cover manually. OpenPNP will just go ahead and pick.
* **CheckOpen**: The user must still open the cover manually, but OpenPNP will check if the cover is open using computer vision. If not, the operation is stopped. The check is only performed before the first pick after machine homing, or after pressing the feed count Reset button. 
* **OpenOnFirstUse**: The cover is opened before the first pick after machine homing, or after pressing the feed count Reset button.
* **OpenOnJobStart**: The cover is opened when the job starts. This happens collectively for all the BlindsFeeders with this setting. OpenPNP solves the [Travelling Salesman Problem](https://en.wikipedia.org/wiki/Travelling_salesman_problem) using [Simulated Annealing](https://en.wikipedia.org/wiki/Simulated_annealing) to optimize the path among the feeders. If the pick happens outside a job, or if the feeder wasn't opened on job start (feeder enabled later, reloaded tape and reset, etc.), it behaves like **OpenOnFirstUse**. 
 
You can also use the Open / Close all Covers buttons.

### More Feeders on the same Array

For the next Feeder on the same array, you can go directly to the Auto Setup ("one click setup"). OpenPNP knows that the camera position is on the area already pinned down by the fiducials of the first feeder. It can simply copy the fiducials (and other information) over. 

The feeders are now linkend and all the common information is constantly synced, if you change it on any one of them. 

## Advanced 

TODO: other cover types etc. 

### Adjusting Play Values

(preliminary, copied from group discussion)

Be sure to set your filament diameter precisely in the slicer, then print small sample feeders. TIP: you can print a series of almost identical `TapeDefinitions` where you vary the `tape_play`, `cover_play` values systematically.

These play values can only be tested with the tape loaded, because the cover is designed with a combination of sideways and downwards clamping forces in mind. Using double-sided tape on the full underside, tack the feeder on a flat hard surface (important, so it can't bend against the covers' spreading forces), load the tapes, peel off the cover foil, and then test how easy it is to move the cover. The cover must move quite easily but there should be no skidding after a fast push.

WARNING: if the cover really sticks, don't try on the machine. No point in ruining your nozzle tips. Forget lubricants, they might have side effects for soldering and it really should work without.

Note that PETG takes a day or so to really settle. The cover will slide much more easily after that. Also be aware that the longer the feeder is, the more friction the cover will get, so don't jump up from printing a tiny sample to filling up your print volume in one step. Also print multiple lanes to get a representative side-by-side tension.

These tape_play, cover_play are forgiving for 0603 and up. I hope my examples will mostly just work. For your 0805 "bricks", I expect you to get it right on the very first try.
Actually, everything is very easy for 0603 and up. You can just snap in the cover on top of the parts without danger of parts jumping out. Just ease in the cover edge with the blinds first, align the "closed" position and then press the whole cover in. Don't be afraid, you can twist and pinch a PETG cover a lot before it gives.

Having said that, things can be quite frustrating to get right for 0402 (so don't start there). The 0402 is so hard mostly because the cover must not only just "be there", but also press down just right on top of the tape to keep these incredibly tiny parts in their very shallow pockets. If this is not just right, the parts will be "rolled" into the air gap between cover and tape. It is quite a challenge to get the balance between this pressing-down and too much friction right. Expect to print a few :-)

My hope is that once people get it right for their printer model, they can reproduce 0402 feeders reliably, anytime. It works for me and my PRUSA i3 MK3. And I hope that the community will share back these tape_play, cover_play values and other tips so others with the same printer model can just get it right on the first try.
Everybody: If you have a printer other than PRUSA i3 MK3, please report back your findings, thanks!

If parts keep jumping out, I found that an anti-static coating on the feeder helps. This anti-static coating is also a must for ESD sensitive parts, I guess.  
