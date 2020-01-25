# Introduction

## What is it?

A step up from the [[ReferenceStripFeeder]], this light 3D-printed feeder can hold and reload cut pieces of SMT tape and protect the parts using a sliding cover. The cover can be opened/closed automatically by the nozzle tip. Feeders are printed in whole arrays. Uses computer vision and fiducials for setup and operation. 

For prototyping and very small runs only.

![BlindsFeeder on Wood](https://user-images.githubusercontent.com/9963310/72673056-398ef700-3a65-11ea-92c5-51b565d0a16a.jpg)

![BlindsFeeder-Open-Close](https://user-images.githubusercontent.com/9963310/72674123-85489d00-3a73-11ea-8f32-46258859b0a4.gif)

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

## Modelling the Feeder

The feeder is created using the cool Open Source program [OpenSCAD](https://www.openscad.org/downloads.html). Download and install it to proceed.  

![grafik](https://user-images.githubusercontent.com/9963310/73119976-213d4180-3f69-11ea-8557-40611f907be6.png)

To get the feeder's OpenSCAD model files, it's easiest to extract them from OpenPNP, so we need to add the first BlindsFeeder right now: 

![grafik](https://user-images.githubusercontent.com/9963310/73119226-0c5bb080-3f5f-11ea-95f8-8cfcd125d57f.png)

Then press the Extract 3D-Printing Files button. OpenPNP will ask you where to save and then open the two files.

![grafik](https://user-images.githubusercontent.com/9963310/73119779-1386bc80-3f67-11ea-8eeb-6df80b2cdd79.png)

There are two files, the `BlindsFeeder-3DPrinting.scad` and the `BlindsFeeder-Library.scad`. Go to the first one, which is used to customize the actual feeder 3D-print. Just press the Preview button to see the standard example:


![grafik](https://user-images.githubusercontent.com/9963310/73120747-19ce6600-3f72-11ea-9494-131af8711339.png)

Rotate the preview for the Y axis to point downwards to get the standard view (why, will be explained later). 

### TapeDefinition

In the source code window, you see several `TapeDefinition`s. You can use those to define the geometrical properties of your SMT tapes and assign names to them. You can have as many as you like, and there is no need to delete any, as you can later decide which to print and which not. The idea is that you will build up a repository of proven `TapeDefinition`s over time. 

![grafik](https://user-images.githubusercontent.com/9963310/73120954-f2c56380-3f74-11ea-836c-487b2edcae4e.png)

The parameters (and there are more available) are all documented in the `BlindsFeeder-Library.scad` file. We'll only explain the most important here.

![grafik](https://user-images.githubusercontent.com/9963310/73120618-0e2e6f80-3f71-11ea-9f4f-9a11b8229e85.png)

Measure your tapes or look at datasheets for parts/tapes that you don't have yet. There are [generic datasheets](https://www.analog.com/media/en/technical-documentation/white-papers/tape-and-reel-packaging.pdf) that may also help.

Use the _nominal_ `tape_width`, this must be 8mm, 12mm, 16mm etc. (increments of 4mm). Any inaccuracy in the real tape width must **not** be entered here. Instead, we will adjust these later, using `tape_play`. 

Again, use the _nominal_ `pocket_pitch`, this must be 2mm, 4mm, 8mm, 12mm etc. (increments of 4mm).
 
The `tape_thickness`, `pocket_portrusion `you must get from the datasheet or measure from the physical tape. Probably need a caliper. But don't worry too much, the printer can only resolve this in layers of e.g. 0.2mm and that's good enough.

`pocket_width` is the width the physical pocket across the tape. You can make it a bit larger, as long as there is space. The model will make the blinds opening slightly larger.

`tape_play`, `cover_play` (negative or positive) are empirical. Start from the examples. There is more about those later.

### Building Up The Feeder Array

Once you've defined your `TapeDefinition`s, you can multiply and mix them to build up a feeder array. This is done at the end of the file:

![grafik](https://user-images.githubusercontent.com/9963310/73120868-66ff0780-3f73-11ea-90d3-940594bee1ff.png)

The `tape_length`, should again be specified in multiples of 4mm. 

The `arrayed_tape_lanes` and the `arrayed_tapes` contain the number and definitions of tapes to be arrayed, respectively. Just set the lanes to `0` to not print a tape definition at all (no need to delete definitions from the array).

You can set `debug=true` to see how the feeder will look when the covers are mounted. 

### Adjusting Play Values

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
