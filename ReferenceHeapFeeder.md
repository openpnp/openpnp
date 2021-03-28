## Overview
The HeapFeeder is a high density feeder implementation.

![HeapFeeder_Overview](https://user-images.githubusercontent.com/6315907/112737020-8d457c00-8f57-11eb-80b0-64cab848818f.png)

The core concept is, that the parts are stored in heaps, a random number is picked using the nozzle and thrown in a drop box. There vision is used to determine which parts are upside up and the orientation. If no parts are upside up, parts are picked up and dropped again, until it is by chance upside up.

A vacuum sensor is required, blow of functionality (to avoid parts sticking to the nozzle) and good top vision system is highly recommended.

Since it heavily utilizes vision, makes multiple moves for each part it is a very slow feeder, but over 200 feeder can be placed in the space required for a single A4 paper sheet.

Example 3D printed parts can be found [here](https://github.com/doppelgrau/PnP/tree/master/HeapFeeder).

## Heap

Each heap is a simple „bin“, where the parts are stored in 3 dimensions in a chaotic order. From the OpenPnP side the Heap is defined by the top location and the depth. The size is assumed, that around the top location there is at least 1.25mm play in each direction, at any (valid) nozzle height.

![HeapFeeder_FeederConfig](https://user-images.githubusercontent.com/6315907/112737019-8cace580-8f57-11eb-95bd-44631c512cf2.png)

The other settings are:

- The used DropBox
- Three way-points used to move the parts from or to the heap. So it can be avoided that parts not safely attached to the nozzle drops into an other heap, by choosing a safe way (See below).
- FeedRetry Count like in any feeder, the number of feed retries.
- PickRetryCount like in any feeder, the number of feed retries.
- Max flip attempts says how many times a part is picked up and thrown away without finding any part upside up, before the content of the DropBox is purged and tried again with new parts. Total attempts is currently hard coded at 12 tries. After that there is a feed failed exception raised.
- LastFeedDepth the depth relative to the center top, where the last time parts were found. Usually the „searching“ for a part is limited to 3 times (to avoid going down forever if something is wrong with the vacuum), the part height, except the Last Feed Depth is zero. The next start for part searching is a bit higher, so all parts on the same level can be caught before the nozzle dives further down.
- Vacuum Difference the required difference in the vacuum sensor to assume a part sticking on the nozzle. Since parts are often only partly sticking to the nozzle, the „partOn“ from the nozzleTip can not be used.
- Part like in all feeders the part in that feeder. I would not recommend anything larger that 1206 or anything with many pins that could bend. Non polar parts like resistors and capacitors are the easiest, but with good top- and bottom vision also polar parts like diodes can be used.
- DetectionPipeline is the vision pipeline that is used to detect the parts, upright up. Should have a very low false positive rate. Used template matching. Reset gives a good default for DropBoxes with the color green, white or black.
- TemplatePipeline is used to create a template image. The template should show the part upside up in the desired 0° orientation. Reset gives a good default for the current selected drop box, with the color/name green, white or black. If a good template is generated, it has to be moved from the „templates/new“ folder inside the openpnp configuration in the „templates“ folder.
- Get Samples get some parts from that heap and drops them in the dropBox. Can be used for creating a template.

![HeapFeeder_Move](https://user-images.githubusercontent.com/6315907/112737022-8e76a900-8f57-11eb-9a19-2278c2a7c492.png)

## Drop Box

The drop box has two functions:

- Deliver a good background for the vision. So depending on the part color, choose a matching drop box.
- Contain the parts when dropped into the drop box, but at the same time increase the change of turning the part while dropping.

Drop boxes are shared between the heaps.

![HeapFeeder-DropBoxSettings](https://user-images.githubusercontent.com/6315907/112737018-8c144f00-8f57-11eb-9fe3-b235a878a4e2.png)

The settings for the drop boxes are way simpler:

- Center Bottom marks the center point of the drop box. Used to position the camera for the vision pipelines and to determine the pick height in combination with the part height.
- Drop Location determines where the parts are dropped. I use the sidewalls as drop location, to increase the chance that the part bounces a bit in the drop box, so it hopefully flips over.
- Part Pipeline is used to detect any part in the drop box. Used to make sure the drop box is empty and to find parts that can be thrown in again. Should have a very low false negative rate.
- Dummy Part is the part, if unknown what part is currently in the drop box. Usually the last used heap is known and that part is used, but in some circumstances (e.g. after start up) the part is not known.
- CleanDropBox removes the parts from the drop box. If the last heap is known, the parts are put back in the heap. Else the parts are discarded.
