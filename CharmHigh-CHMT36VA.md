# Project Status
___
**This Wiki is outdated.** There are newer/better versions of Smoothieware firmware, Smoothieware configurations, and OpenPnP configuration around. This Wiki pages should be update by someone who owns such a machine and is on top of things. 

**HELP WANTED!**

Some pointers (unconfirmed):
- Discussion and OpenPnP Config:
  https://groups.google.com/g/desktop-pick-and-place/c/bkAQFhvyEE4/m/yJ9yCGLtAAAJ
- Smoothieware firmware and config:
  https://github.com/c-riegel/Smoothieware-CHMT/
  with these important patches:
  https://github.com/c-riegel/Smoothieware-CHMT/commits/chmt

_Mark (2021-12-04)
___


The stock firmware on the CHMT36VA turned out to be a dead end. There does not appear to be any command that can be used to rotate the nozzle. The best way forward seems to be to use [Matt Baker's Smoothie port](https://github.com/mattthebaker/Smoothieware-CHMT) which can be flashed on the stock main board.

Using the Smoothie firmware is an irreversible operation. Once you flash it, there does not appear to be a way to go back to the stock firmware, so consider carefully if you want to do that. If you'd prefer to buy a second controller board to use for OpenPnP instead of using your stock one, contact Kimi. I was quoted $280 including shipping to the US.

Using Smoothie, OpenPnP can use all functions of the CHMT36VA. It does not reach the same level of performance as the original software, but there are many new features and functions in OpenPnP that are not available in the OEM software.

A common question is whether or not this port is ready for daily production use. The answer is yes, but it will take much longer to initially configure the system and there is a steep learning curve. Be prepared to have the machine down for a week or more while you get everything configured and become familiar with OpenPnP.

# Setup Guide

The following can be used as a rough guide to getting OpenPnP working on your CHMT36VA. Reminder: **Installing Smoothie on the main board is a irreversible operation. Once you install Smoothie you will not be able to flash the stock firmware, and you will not be able to use the OEM software.**

If you want to proceed, follow these steps:

1. Install [Matt Baker's Smoothie port](https://github.com/mattthebaker/Smoothieware-CHMT) on your CHMT36VA main board. This requires some familiarity with the STM32 tools, and requires a flash tool. See [[Charmhigh Modifications for OpenPnP]] and the [Smoothie on STM32/CHMT Thread](https://groups.google.com/d/msg/desktop-pick-and-place/C-n9dksqhDQ/xZdmKPh3CAAJ) for more information on this.
2. Either Install [Matt Baker's example OpenPnP configuration](https://github.com/mattthebaker/openpnp-config-chmt) to get started or configure from scratch using the [[Setup and Calibration]] Guide. It is recommended to step through the guide even if you use the example config and check that each setting applies to your machine and works correctly.
3. If you are using the stock cameras and camera board, see the [SwitcherCamera Documentation](https://github.com/openpnp/openpnp/wiki/SwitcherCamera) for how to set up the multiplexer.
4. Please consider joining the [OpenPnP Discussion Group](http://groups.google.com/group/openpnp) and the [SparkFun Desktop Pick and Place Discussion Group](https://groups.google.com/forum/#!forum/desktop-pick-and-place) to post questions and information about your setup.

This documentation is incomplete and can use your help! Anyone with a GitHub account can edit this page and improve it. Please consider adding your experiences and findings here.

# Helpful Resources

* [The GoFundMe Page](https://www.gofundme.com/help-openpnp-grow)
* [SparkFun's CHMT36VA Repo](https://github.com/sparkfunX/Desktop-PickAndPlace-CHMT36VA) contains translations, documentation, software, and utilities for working with the machine.
* [Matt Baker's Smoothie port](https://github.com/mattthebaker/Smoothieware-CHMT) which can be used to flash Smoothie to your CHMT36VA for use with OpenPnP.
* [Matt Baker's example OpenPnP configuration](https://github.com/mattthebaker/openpnp-config-chmt).
* [OpenPnP + CHMT Thread](https://groups.google.com/forum/#!msg/desktop-pick-and-place/qaoGrnM7pPw/-2k-5FBHCAAJ) contains tons of good information on making the CHMT work with OpenPnP.
* [Smoothie on STM32/CHMT Thread](https://groups.google.com/d/msg/desktop-pick-and-place/C-n9dksqhDQ/xZdmKPh3CAAJ)
* [SwitcherCamera Documentation](https://github.com/openpnp/openpnp/wiki/SwitcherCamera): The SwitcherCamera lets OpenPnP switch the camera board on the CHMT between the top and bottom camera.
* [CharmHigh Conversion](https://github.com/openpnp/openpnp/wiki/Charmhigh-modifications-for-OpenPnP): how to convert the CharmHigh machine to use Smoothieware and OpenPnP.

---

# Native Driver (Obsolete)

The information below pertains to the development of a native driver, which has turned out to be impossible. The information is obsolete but left here for reference.

# Source Code

See https://github.com/openpnp/openpnp/tree/feature/chmt36va/src/main/java/org/openpnp/machine/chmt36va for the latest source code.

# Testing

**WARNING** The code is currently in alpha testing. **IT MAY BREAK YOUR MACHINE**. Running the current code is only recommended for people with experience with OpenPnP and who are willing to potentially destroy your machine.

To be clear: **This code might permanently damage your machine, and your warranty is likely to be voided. I accept no responsibility for any damage you cause. Proceed at your own risk!**

To run the CHMT36VA driver in OpenPnP, follow these basic instructions:

1. Connect your CHMT36VA to your OpenPnP computer using the supplied USB cables. There are two - one for the camera, and one for the serial port.
2. Download and install the test branch of OpenPnP from http://openpnp.org/test-downloads/.
3. Start OpenPnP once, and then exit. This creates your .openpnp configuration directory. See https://github.com/openpnp/openpnp/wiki/FAQ#where-are-configuration-and-log-files-located to find the location. You'll need it for the next step.
4. Copy your CharmHigh supplied `LICENSE.smt` file to your configuration directory. Note, make sure to COPY and not move the file. Always keep a backup!
5. Start OpenPnP again and begin to work through the [[Setup and Calibration]] Guide. When you reach the point where you need to select a driver, choose the `CHMT36VADriver`. You will be prompted to restart.
6. Go to Machine Setup -> Driver -> CHMT36VADriver -> Communications.
7. Select your Port, Baud: 115200, Parity: None, Data Bits: Eight, Stop Bits: One and pretty Apply.
8. Click the green OpenPnP power button. The machine should reset and beep.
9. Click the Home button to home the machine.
10. Read the Warnings and Things to Try sections before doing anything else.

## Warnings

* Z and Rotation are not working yet.
* No machine extents or limits are enforced, you can easily crash the head of the machine and cause permanent damage. Be careful and cautious!
* Don't allow the drag pin to be extended for more than a few seconds at a time or it may overheat and be damaged.

## Things to Try

* Jog the machine in X/Y using the jog buttons.
* Add a [Top Camera](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Top-Camera-Setup) and perform camera calibration. Try camera jogging.
* Visit the Actuators panel and try the various Actuators. You can select different cameras, turn lights on and off, extend the drag pin, turn the pump on and off, turn on nozzle 1 and nozzle 2 vacuum, etc.

# Machine Overview

The CHMT36VA is a desktop pick and place machine. It has an internal controller but requires an external PC to run.

For feeders, the machine has 29 lanes of drag feeders: 22 8mm lanes, 4 12mm lanes, 2 16mm lanes, and 1 24mm lane, and a clutch driven cover tape peeler. There are also 14 single chip trays, and the software supports trays placed randomly around the machine.

There are two nozzles in the shared Z see-saw configuration that is now common on almost all of these types of machines. The nozzles each have a NEMA8 hollow shaft stepper and a permanently mounted nozzle tip holder. The nozzle tip holder takes standard Juki style nozzles, and they are retained by 4 ball bearings and a rubber band.

The vision system has an up looking and a down looking camera, both analog, at 640x480. As far as I can tell, the down looking camera is only used for manual positioning, it does not appear to be used for any type of vision operations. The up looking camera is used for the standard "bottom vision" operation.

The OEM software is Windows based, written with Qt, and is designed for a small touch screen interface. There are other CharmHigh desktop pick and places that use an on board tablet interface, and I suspect the same software runs on the tablet.

For vacuum and air, the machine has an internal vacuum pump and a small internal air compressor. The vacuum pump is used to pick components up, and the air compressor is used to evacuate the vacuum lines when the machine places a part.

## Axes

The X and Y axes are belt driven, with linear bearings on smooth rod. Power comes from NEMA23 steppers with optical encoders and the motion is closed loop. The steppers are powered at 36v and are quite fast. A drive shaft spreads the load of the Y axis to dual belts on either side, while the X axis has a single belt and is directly driven.

Homing is via two mechanical homing switches with rollers.

The Z axis uses a see-saw configuration with spring return. The motor is a NEMA11 (or 14?) and has an optical homing disk with a small slot in it. The disk interrupts the optical beam and the slot allows it through. The machine checks this whenever Z returns to center to ensure it's safe to move.

The C axes (nozzle rotation) are via NEMA 8 hollow shaft steppers with vacuum tubing connected directly to the back shaft. 

## Head

The head includes the two nozzles, a drag pin solenoid, vacuum sensors, vacuum solenoids, and a couple PCBs. The drag solenoid also has an optical interruptor style gate to ensure it's in the up position before moving. It appears to use a spring return made of a piece of rubber tubing, but I haven't been able to inspect it closely to be sure.

## Lighting

The gantry has a white LED strip across it's entire length that the software refers to as the "Work Light". This does a pretty good job of lighting the work area without creating intense reflections.

The up facing camera has a white LED ring light with 3mm LEDs bent at an angle.

## Cameras

There are two cameras, on up facing and one down facing. They are analog cameras and both feed into an off the shelf single capture card with a strange switching PCB soldered to the top of it. The result is that only one camera can send a signal to the host at a time. In general, the software keeps the up looking camera activated and only activates the down looking during user targeting.

The up looking camera appears to be a Sony Effio SN700, which is a cheap and common security camera. I have not yet determined what the down looking one is.

The capture card seems to be a clone of an EZCAP and is identified as "USB2.0 PC CAMERA" with VID 18EC, PID 5850.

# Communications

The machine has an RS-232 9 pin connector, and a USB B connector on the back. The RS-232 is used by the host to send and receive commands and status, and the USB B connector routes directly to the capture card.

## Protocol

The protocol is little endian binary over RS-232 serial. Communication is at 115200 baud, N81, no flow control. Most commands from the host result in a response from the machine, and the machine also sends some asynchronous responses.

The packet format is as follows:

Example packet: ebfbebfb110400ca0032002a0001012c00cbdbcbdb

| Offset     | Data       | Description                                               |
| ---------- | ---------- | --------------------------------------------------------- |
| 00         | ebfbebfb   | header                                                    |
| 04         | 11         | I call this packetType, but not completely sure on it yet |
| 05         | 0400       | payload size, 16 bit unsigned little endian               |
| 07         | ca         | encryption key / subkey info                              |
| 08         | 00         | unknown                                                   |
| 09         | 3200       | tableId, 16 bit unsigned little endian                    |
| 11         | 2a00 01 01 | payload (paramId, data type, field value)                 |
| length - 6 | 2c00       | crc16, 16 bit unsigned little endian                      |
| length - 4 | cbdbcbdb   | footer                                                    |

- All packets are encrypted using keys from the included LICENSE.smt file.
  - In LICENSE.smt, there is a 12 byte key at 1024, and a 1024 byte key at 4096.
  - The 12 byte key is "decrypted" by XOR 0xAA.
  - The 1024 byte key is encrypted by XORing with the decrypted 12 byte key with the index mod 12.
  - For details of the encryption see https://github.com/openpnp/openpnp/blob/feature/chmt36va/src/main/java/org/openpnp/machine/chmt36va/Protocol.java

- Commands are somewhat generic, and can be found in the software's smt.db sqlite3 database. See https://docs.google.com/spreadsheets/d/1mVowA6ZmbxwnvEy32Ap_YYBsWuUACl5wKZB5wjBqljY
