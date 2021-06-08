**NOTE: THIS FEEDER HAS NOT BEEN RELEASED YET. ETA IS EARLY 2021**

### What is it?

Rapid Feeder is a new type of automatic feeder developed by and soon available at [Deltaprintr.com](deltaprintr.com). It's small, compact and very precise, capable of handling up to 0402 components and supports all tape widths up to 2mm pitch. Each feeder has a UUID address that's also readable on the QR sticker on the top and the side of the feeder. As a result, each feeder has a unique address that's pre-set by Deltaprintr. It was designed specifically so you don't have to mechanically index your feeders with a slot built into the machine. The feeder system is designed so you can save space by grouping many different width feeders close together in any order you'd like. No more feeder banks or empty gaps between feeders! The smallest width tape feeders (8mm tape) are only 10mm wide!

![](https://i.imgur.com/w4hB2dW.png)

### Features
1. Can detect if tape's cover takes too long to pull and will send an error to OpenPNP and turn on a red light to identify feeder. Useful if tape's cover has come loose.
2. Forward/reverse buttons on the feeder allow easy loading and unloading of tape.
3. LED indicators on feeder for error (red), forward (green), reverse (white) and actively pulling cover (yellow).
4. UUID address for each feeder. Can also be used for user's own data logging system to keep track of parts inventory by scanning the QR.
5. Supports all types of tapes, up to 0402 and 2mm pitch.
6. Reset button on feeder or optionally undock and re-dock the feeder.
7. Requires a feeder motherboard, also provided by Deltaprintr.

### Mounting the feeder

*To Do*

### Loading Tapes
*To Do*

### Adding and configuring feeders in OpenPNP

Youtube Video for setting up in OpenPNP: [https://youtu.be/VlpsAToBvS0](https://youtu.be/VlpsAToBvS0)

After creating an actuator, create a new sub-driver under GcodeDriver for the Rapid Feeder system. Under ACTUATE_STRING_COMMAND add M603 `{StringValue}` and hit apply. M603 is to advance the tape forward and M602 is reverse.

To perform an initial scan for feeders, go to the Feeders tab and create a new RapidFeeder. You should name it something that describes the bank of feeders that it will scan for, such as RapidFeeder-West.

In the settings for the feeder, locate the Scanning section and define the Scan Start, Scan End and Scan Increment values:

Scan Start: When scanning for QR codes, the camera will be moved to this position to start scanning. Move the camera to the location of the first possible QR code and save the location.
Scan End: When scanning for QR codes, the camera will finish the scan at this location. Move the camera to the location of the last possible QR code and save the location.
Scan Increment: When scanning for QR codes, the camera will move by this distance between each image capture. This value should be approximately half the size of the camera image frame. This ensures that the scan will never advance past a QR code and miss it.
When the above values are set, press the Scan button. The machine will move the camera along the line formed by the start and end points and capture QR codes. When it finishes it will create or update feeders in the feeder tab for each QR code it finds.

### Usage

Once scanning is complete, pressing the feed button for any of the new feeders will send the FEED command to the RAPIDFEEDER actuator in the form of `{M603 {ADDRESS} {PITCH}}`.