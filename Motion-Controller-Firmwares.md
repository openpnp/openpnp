## Smoothieware

A special Smoothieware firmware for OpenPnP is available. It contains some bug-fixes and features that are crucial for use with OpenPnP but are not present/accepted in the official Smoothieware firmware. The firmware and more details are available here:

* [makr.zone: "Smoothieware: New Firmware for PnP"](https://makr.zone/smoothieware-new-firmware-for-pnp/500/)

Refer to the Smoothieware Wiki on how to upgrade:

* [Flashing Smoothie Firmware](http://smoothieware.org/flashing-smoothie-firmware)

## Duet3D

The work to make [Duet3D 2/3 controllers](https://www.duet3d.com/index.php?route=common/home#products) ideal for OpenPnP is ongoing. More info will be made available here.

Refer to the Duet3D Wiki on how to upgrade:

* [Duet 2 Installing and Updating Firmware](https://duet3d.dozuki.com/Wiki/Installing_and_Updating_Firmware)

* [Duet 3 Installing and Updating Firmware](https://duet3d.dozuki.com/Wiki/Getting_Started_With_Duet_3#Section_Updating_Duet_3_main_board_firmware)

## Marlin 2.0

OpenPnP user Bill made a Marlin 2.0 port to Teensy 4.1 with advanced axis support. More information there:

* [Bill's Marlin 2.0 fork](https://github.com/bilsef/Marlin/tree/Teensy4.1_PnP_6axis)

## TinyG 

[TinyG](https://synthetos.com/project/tinyg) is often used for Pick & Place because it comes with the Liteplacer kit. Some features have been added to TinyG to make it more like other controllers and to optimize its use with OpenPnP. The firmware and more details are available here:

* [makr.zone: TinyG: New G-code commands for OpenPnP use](https://makr.zone/tinyg-new-g-code-commands-for-openpnp-use/577/).
* See the [proposed changes (Pull Request)](https://github.com/synthetos/TinyG/pull/258).
* Follow [the discussion](https://groups.google.com/d/msg/openpnp/veyVAwqS0do/Zsn73noGBQAJ).


___

## Advanced Motion Control Topics

### Motion Control
- [[Advanced Motion Control]]
- [[GcodeAsyncDriver]]
- [[Motion Planner]]

### Machine Axes
- [[Machine Axes]]
- [[Backlash-Compensation]]
- [[Transformed Axes]]
- [[Linear Transformed Axes]]
- [[Mapping Axes]] 