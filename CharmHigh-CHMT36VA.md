This page will serve as a repository of information on the CharmHigh CHMT36VA and the process of developing an OpenPnP driver for it.

For more information about how we got here, please see: https://www.gofundme.com/help-openpnp-grow

For more information about the CHMT36VA in general, and SparkFun's experiences with it, see: https://github.com/sparkfunX/Desktop-PickAndPlace-CHMT36VA

# Project Status and Updates

See the GoFundMe page for project updates: https://www.gofundme.com/help-openpnp-grow (Click Updates)

# Source Code

See https://github.com/openpnp/openpnp/tree/feature/chmt36va/src/main/java/org/openpnp/machine/chmt36va for the latest source code.

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
