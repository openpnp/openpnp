## Controllers and associated Firmware for Advanced Motion Control

This page lists Controllers and associated Firmware that are known to be compatible with [[GcodeAsyncDriver]] and [[Advanced Motion Control]] features. Other controllers may also support the needed features, if you think your controller provides the needed features, please [contact us in the user group](https://groups.google.com/forum/#!forum/openpnp). 


### Key Features

The following key features need to be supported (_in addition_ to basic homing and motion commands like [`G1`](https://www.reprap.org/wiki/G-code#G0_.26_G1:_Move) etc. of course):

1. The controller provides a [`M115`](https://www.reprap.org/wiki/G-code#M115:_Get_Firmware_Version_and_Capabilities) firmware report command. This is used to automatically detect the type of the firmware in OpenPnP. Some automated setup can then be provided by the [[Issues and Solutions]] system. Even for a non-motion controller (e.g. a feeder control board) it is recommended to implement this as a minimum, to easily identify a controller on a port and to allow for [[automatic Issues & Solutions auto-setup|Issues-and-Solutions]]. 

The following requirements apply for controllers with axes attached:

2. The controller can manage extra axes (`A`, `B`, `C` etc.) as true axes, with simultaneous motion. Controllers/firmwares that only support multiplexing "extruder" `E` axes with `T` [tool select commands](https://www.reprap.org/wiki/G-code#T:_Select_Tool) are not valid. Any mixing of axes must be correctly supported, including all aspects of feed rate and acceleration limiting according to the [NIST RS274/NGC Interpreter – Version 3 standard](https://www.nist.gov/publications/nist-rs274ngc-interpreter-version-3), more specifically section "2.1.2.5 Feed Rate", or better. 

3. The controller can report axes positions, including extra axes (`A`, `B`, `C` etc.), typically with the [`M114`](https://www.reprap.org/wiki/G-code#M114:_Get_Current_Position) command.

4. The controller can reset axes positions, including extra axes (`A`, `B`, `C` etc.), typically with the [`G92`](https://www.reprap.org/wiki/G-code#G92:_Set_Position) command. 

5. `G92` must work correctly when motion is still pending. Either by implicitly waiting for still-stand or (better!) by allowing on-the-fly offsetting. 

6. The controller must be able to wait for motion completion, typically with the [`M400`](https://www.reprap.org/wiki/G-code#M400:_Wait_for_current_moves_to_finish) command. Any further commands sent after the `M400` must be suspended until motion completion. The controller must only acknowledge the command, when motion is complete i.e. the "ok" (`COMMAND_CONFIRM_REGEX`) response must be suspended until then, providing blocking synchronization to OpenPnP. 

7. The controller must support dynamic acceleration and/or jerk limits, typically by the [`M204`](https://www.reprap.org/wiki/G-code#M204:_Set_default_acceleration) command for acceleration or the [`M201.3`](https://makr.zone/tinyg-new-g-code-commands-for-openpnp-use/577/) command for jerk.

To adapt a new firmware it is sometimes best to play with the `GcodeServer` controller simulator built into OpenPnP. You can then send commands and observe the expected response and behavior. You can activate the built-in `GcodeServer` by switching the GcodeDriver to **tcp** and setting the **IP Address** to exactly `GcodeServer` (case sensitive).

## Duet3D

[Duet3D 2/3 controllers](https://www.duet3d.com/index.php?route=common/home#products) firmwares have been perfected for use with OpenPnP. Many thanks to Duet3D for providing a free Duet 3 board and to [dc42](https://github.com/dc42) for implementing substantial improvements, and accepting a crucial [pull request](https://github.com/Duet3D/RepRapFirmware/pull/471). For advanced OpenPnP use, Duet firmware has been improved in...

* USB serial speed
* Fixes for compressed G-code parsing
* Fixes and configurable option for correct feed rate calculations according to section 2.1.2.5 of the NIST G-Code standard.
* Configurable grace period for proper look-ahead planning

Firmware [version 3.3beta](https://github.com/Duet3D/RepRapFirmware/wiki/Changelog-RRF-3.x-Beta-&-RC#reprapfirmware-33beta1) or newer must be used to support most OpenPnP Advanced Motion Control features. Use the [[Issues and Solutions]] system to help you detect the correct version and configuration and to setup proper G-code.

Refer to the Duet3D Wiki on how to upgrade:

* [Duet 2 Installing and Updating Firmware](https://duet3d.dozuki.com/Wiki/Installing_and_Updating_Firmware)

* [Duet 3 Installing and Updating Firmware](https://duet3d.dozuki.com/Wiki/Getting_Started_With_Duet_3#Section_Updating_Duet_3_main_board_firmware)

### Example config.g File
The following is basic working config.g file for the 6HC. This config allows Openpnp to fully drive/calibrate a machine with the following;
* 5 Axis machine; linear XYZ, rotational AB
* Single head, CAM driven dual nozzle with -180+180 rotation
* XYZ Limit switches
* x2 Analog inputs for nozzle pressure
* x1 binary input for drag pin state detection
* x7 Outputs PWM 20k to drive; x2 LED array, x2pumps, x2 pneumatic valves and x1 drag pin solenoid.
```
if {network.interfaces[0].type = "ethernet"}
	M552 P192.168.1.14 S1
else
	M552 S1
```


## Smoothieware

**⚠ WARNING ⚠** do not buy the illegitimate clones of the Smoothieboard that are typically offered in Chinese online-shops. These are known to violate Open Source licenses and brand names, they use inferior/sub-spec and counterfeit components, inadequate copper layers etc. They are known to fail with OpenPnP. **We will not provide support for these boards.** See the discussions [here](https://groups.google.com/g/openpnp/c/rdAXltRoSdc/m/lPNkWLX4BQAJ) and [here](https://groups.google.com/g/openpnp/c/4LswIzPOfpU/m/gopdUoiPAAAJ).   

___

A special Smoothieware firmware for OpenPnP is available. It contains some bug-fixes and features that are crucial for use with OpenPnP but are not present/accepted in the official Smoothieware firmware. The firmware and more details are available here:

* [makr.zone: "Smoothieware: New Firmware for PnP"](https://makr.zone/smoothieware-new-firmware-for-pnp/500/)
* Note: for CHMT machines you require a different build, see [[CharmHigh CHMT36VA]]

Refer to the Smoothieware Wiki on how to upgrade:

* [Flashing Smoothie Firmware](http://smoothieware.org/flashing-smoothie-firmware)

### Axes vs. Extruder Configuration

Please make sure your Smoothieware is configured to use true axes, i.e. `A` `B` `C`, not extruders (we're not 3D printing!). If your config.txt contains something like this:
  
```
# Extruder module configuration
# See http://smoothieware.org/extruder
extruder.hotend.enable                          true          # Whether to activate the extruder module at all. All configuration is ignored if false
extruder.hotend.steps_per_mm                    8.8888      # Steps per mm for extruder stepper
```

Then you must remove the `extruder` parts and instead use the `delta`, `epsilon` and `zeta` definitions as described in the [Smoothieware 6axis page](https://smoothieware.org/6axis).

If you skip this, you will get a complaint by [[Issues and Solutions]] saying "The driver does not report axes in the expected X Y Z A B C order".

![driver reported](https://user-images.githubusercontent.com/9963310/109156343-02633d00-7771-11eb-8f22-73a0af0ef0a7.png)

## Marlin 2.0

Marlin must be configured for modern 6- or 9-axis support, i.e. using axes `A` `B` `C` etc. OpenPnP does not work (reasonably) with the old multiplexed extruder `E0` `E1` etc. modes. 

* 6-axis support seems to be included in the official [Marlin bugfix-2.0.x](https://github.com/MarlinFirmware/Marlin/tree/bugfix-2.0.x) branch.
* 9-axis support was reported to be included in the [DerAndere1 9axis_pull](https://github.com/DerAndere1/Marlin/tree/9axis_pull).
* See also the [Teensy 4.1 controller base PCB](https://github.com/bilsef/teensy4_pnp_controller) by OpenPnP user Bill.

HELP WANTED: Configuration was discussed [here](https://github.com/openpnp/openpnp/issues/1240#issuecomment-893778594), but more concise instructions should be contributed as a separate [[Marlin Configuration]] page by Marlin users.

## TinyG 

[TinyG](https://synthetos.com/project/tinyg) is often used for Pick & Place because it comes with the Liteplacer kit. Some features have been added to TinyG to make it more like other controllers and to optimize its use with OpenPnP. The firmware and more details are available here:

* [makr.zone: TinyG: New G-code commands for OpenPnP use](https://makr.zone/tinyg-new-g-code-commands-for-openpnp-use/577/).
* See the [proposed changes (Pull Request)](https://github.com/synthetos/TinyG/pull/258).
* Follow [the discussion](https://groups.google.com/d/msg/openpnp/veyVAwqS0do/Zsn73noGBQAJ).

The changes have now officially been adopted by the TinyG project. But please still use the [makr.zone link](https://makr.zone/tinyg-new-g-code-commands-for-openpnp-use/577/), as the official firmware downloads are not yet updated and additional guidance is needed. 

___

## Advanced Motion Control Topics

### Motion Control
- [[Advanced Motion Control]]
- [[GcodeAsyncDriver]]
- [[Motion Planner]]
- [[Visual Homing]]
- [[Motion Controller Firmwares]]

### Machine Axes
- [[Machine Axes]]
- [[Backlash-Compensation]]
- [[Transformed Axes]]
- [[Linear Transformed Axes]]
- [[Mapping Axes]] 
- [[Axis Interlock Actuator]]

### General
- [[Issues and Solutions]]
