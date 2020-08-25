# RapidFeeder

This is an experimental implementation RapidFeeder meant to provide a skeleton for full
implementation of the feeder class.

# Description

The feeder is a simple auto feeder with two configurable values: address and pitch. Address
sets the bus address of the feeder, and pitch is an arbitrary number that will be passed
to feed operations to tell the feeder how far to advance.

The feeder implementation also includes QR code scanning for finding feeders on the system,
along with automatic registration of located feeders.

# Requirements

The RapidFeeder class requires an actuator with the name "RAPIDFEEDER" to be created on
the machine. The feeder will send actuate(String) commands to this actuator to perform
bus actions.

# Setup and Scanning

After creating an actuator, create a new sub-driver under GcodeDriver for the Rapid Feeder system. Under ACTUATE_STRING_COMMAND add `M603 {StringValue}` and hit apply. M603 is to advance the tape forward and M602 is reverse.

To perform an initial scan for feeders, go to the Feeders tab and create a new RapidFeeder. You
should name it something that describes the bank of feeders that it will scan for, such as
RapidFeeder-West.

In the settings for the feeder, locate the Scanning section and define the Scan Start, Scan End and
Scan Increment values:

- Scan Start: When scanning for QR codes, the camera will be moved to this position to start
  scanning. Move the camera to the location of the first possible QR code and save the location.
- Scan End: When scanning for QR codes, the camera will finish the scan at this location. Move
  the camera to the location of the last possible QR code and save the location.
- Scan Increment: When scanning for QR codes, the camera will move by this distance between
  each image capture. This value should be approximately half the size of the camera image
  frame. This ensures that the scan will never advance past a QR code and miss it.
  
When the above values are set, press the Scan button. The machine will move the camera along the
line formed by the start and end points and capture QR codes. When it finishes it will create
or update feeders in the feeder tab for each QR code it finds.

# Usage

Once scanning is complete, pressing the feed button for any of the new feeders will send
the FEED command to the RAPIDFEEDER actuator in the form of `{M603 {ADDRESS} {PITCH}}`.
       
