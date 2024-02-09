# Marlin 2 on RAMPS 1.4
For those that that are doing it cheap (or available)

[Branch of Marlin2 for OpenPnP on RAMPS by crashmatt](https://github.com/crashmatt/Marlin/tree/openpnp)

Unsupported. Not suitable for PR to upstream. For reference only.

### Notes
* Supporting rotation axis that has no endstop switch.
* Rotation moved from extruder to I_AXIS reporting as "A" axis in gcode.
* All fans and heaters are disabled so FET outputs can be used for pumps, valves, lights etc. with GPIO direct.
* Required modification of motion do_homing_move to enable the rotation stepper without checking for endstops.  This is planned to be fixed in Marlin 2 the long term.
* To enable the I_AXIS the endstops are assigned to spare X/Y axis enstop inputs.  Those inputs are then inverted to make it look like the endstop is never triggered.  I may be able to remove this hack.
* My configuration is homing is to the back right corner and the bed is offset to zero at the front left corner.  Not ideal.  Don't copy this.

