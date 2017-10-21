Marlin is a 5 axis controller that can run on an Arduino and intended for 3d printer. In it's default state it is not well suited for pick and place machines because several functions useful for 3d printers interfere if using without heated bed, extruders, thermistors, etc. Nevertheless if configured properly, the firmware can be a good and favorable choice in combination with a ramps 1.4 board + A4988 stepper drivers.

Source files of a configured marlin version running OpenPnP are located at [github](https://github.com/mgrl/MarlinOnRamps4OpenPnP). That fork can be used as a template to be adapted for a specific machine as the fork itself was.

In OpenPnP you will need to select the org.openpnp.machine.reference.driver.GcodeDriver driver.

# Hardware
The proposed firmware was configured for a kit including an Arduino Mega, Ramps 1.4, A4988 stepper driver and a RepRap Full Graphics Display for about 40€. Such kits are widely available at [Amazon](https://www.amazon.de/gp/product/B06XPST1SY/) or Aliexpress.

By default Ramps 1.4 is only rated for 12V, but it can easily be modded that it can handle 24V which is what most OpenPnP users use. Remove the diode D1 (it passes power to the arduino which can handle only 12v max) and the polyfuse which is rated for 16V. A detailed explanation can be found [here](http://www.3d-druck-community.de/thread-4761.html) or with google.

[[https://github.com/mgrl/MarlinOnRamps4OpenPnP/blob/openPnPoptimized/_images/ramps-wiring.png|alt=octocat]]

# Firmware Configuration
The [marlin-firmware](https://github.com/mgrl/MarlinOnRamps4OpenPnP) will be specific to your machine and has to be adjusted. Most of the changes needed to work well with OpenPnP summarizes [this commit](https://github.com/mgrl/MarlinOnRamps4OpenPnP/commit/4350a064b687e50f57dfbf211109dcf3d361b661). Changes were made to configuration.h, configuration_adv.h and pins_RAMPS.h. Usually digital out D8, D9, D10 would be reserved and controlled by marlin to heat extruders or the bed of a 3d printer. These pins were remapped to the unused pin 70 so that the functionality is taken off of the pins. The mosfets can now be used to turn on/off a vacuum pump, solenoids, or lights by issuing an M42 command.

Output of M503 of a running machine:
```
> M503
echo:  G21    ; Units in mm
echo:  M149 C ; Units in Celsius
echo:Filament settings: Disabled
echo:  M200 D3.00
echo:  M200 T1 D3.00
echo:  M200 D0
echo:Steps per unit:
echo:  M92 X80.00 Y80.00 Z106.10 E8.89
echo:Maximum feedrates (units/s):
echo:  M203 X800.00 Y800.00 Z100.00 E500.00
echo:Maximum Acceleration (units/s2):
echo:  M201 X3000 Y3000 Z350 E3000
echo:Acceleration (units/s2): P<print_accel> R<retract_accel> T<travel_accel>
echo:  M204 P1500.00 R1500.00 T1500.00
echo:Advanced: S<min_feedrate> T<min_travel_feedrate> B<min_segment_time_ms> X<max_xy_jerk> Z<max_z_jerk> E<max_e_jerk>
echo:  M205 S0.00 T0.00 B0 X20.00 Y20.00 Z0.40 E5.00
echo:Home offset:
echo:  M206 X0.00 Y0.00 Z0.00
echo:Hotend offsets:
echo:  M218 T1 X0.00 Y0.00
echo:Material heatup parameters:
echo:  M145 S0 H180 B70 F0
M145 S1 H240 B110 F0
echo:Z-Probe Offset (mm):
echo:  M851 Z0.34
ok
```


# machine.xml Driver Section
```xml
  <driver class="org.openpnp.machine.reference.driver.GcodeDriver" port-name="COM19" baud="250000" flow-control="Off" data-bits="Eight" stop-bits="One" parity="None" set-dtr="false" set-rts="false" units="Millimeters" max-feed-rate="30000" backlash-offset-x="0.0" backlash-offset-y="0.0" non-squareness-factor="0.0" backlash-feed-rate-factor="0.1" timeout-milliseconds="10000" connect-wait-time-milliseconds="1000">
	 <homing-fiducial-location units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
	 <command type="COMMAND_CONFIRM_REGEX">
		<text><![CDATA[^ok.*]]></text>
	 </command>
	 <command type="MOVE_TO_COMMAND">
		<text><![CDATA[G1 {BacklashOffsetX:X%.4f} {BacklashOffsetY:Y%.4f} {Z:Z%.4f} {Rotation:E%.4f} F{FeedRate:%.0f}]]></text>
		<text><![CDATA[M400; wait before returning]]></text>
	 </command>
	 <command type="DISABLE_COMMAND">
		<text><![CDATA[M42 P9 S0 ; turn off topcamera leds]]></text>
		<text><![CDATA[M42 P10 S0 ; turn off bottomcamera leds]]></text>
		<text><![CDATA[M84; steppers off]]></text>
	 </command>
	 <command type="HOME_COMMAND">
		<text><![CDATA[G28]]></text>
		<text><![CDATA[G0 X0 Y0 Z0]]></text>
		<text><![CDATA[T0 S1; choose tool 0 (left nozzle rotation)]]></text>
		<text><![CDATA[G92 E0 ; reset coords]]></text>
		<text><![CDATA[T1 S1; choose tool 1 (right nozzle rotation)]]></text>
		<text><![CDATA[G92 E0 ; reset coordinates]]></text>
	 </command>
	 <command type="ENABLE_COMMAND">
		<text><![CDATA[G21 ; Set millimeters mode]]></text>
		<text><![CDATA[G90 ; Set absolute positioning mode]]></text>
		<text><![CDATA[M82 ; extruder in absolute mode]]></text>
		<text><![CDATA[G92 X0 Y0 Z0 E0; openpnp is at 0, grbl must not neccessarily]]></text>
		<text><![CDATA[M42 P9 S255 ; turn on topcamera leds]]></text>
	 </command>
	 <command type="COMMAND_ERROR_REGEX">
		<text><![CDATA[^error.*]]></text>
	 </command>
	 <command type="POST_VISION_HOME_COMMAND">
		<text><![CDATA[G92 X0 Y0]]></text>
	 </command>
	 <command head-mountable-id="ACT1507482667850" type="ACTUATE_BOOLEAN_COMMAND">
		<text><![CDATA[{True:M42 P9 S255}{False:M42 P9 S0}]]></text>
	 </command>
	 <command head-mountable-id="ACT1507482692948" type="ACTUATE_BOOLEAN_COMMAND">
		<text><![CDATA[{True:M42 P10 S255}{False:M42 P10 S0}]]></text>
	 </command>
	 <axes class="java.util.ArrayList">
		<axis name="x" type="X" home-coordinate="0.0">
		   <head-mountable-ids class="java.util.HashSet">
			  <string>*</string>
		   </head-mountable-ids>
		</axis>
		<axis name="y" type="Y" home-coordinate="0.0">
		   <head-mountable-ids class="java.util.HashSet">
			  <string>*</string>
		   </head-mountable-ids>
		</axis>
		<axis name="z" type="Z" home-coordinate="0.0">
		   <head-mountable-ids class="java.util.HashSet">
			  <string>N1</string>
			  <string>N2</string>
		   </head-mountable-ids>
		   <transform class="org.openpnp.machine.reference.driver.GcodeDriver$NegatingTransform">
			  <negated-head-mountable-id>N2</negated-head-mountable-id>
		   </transform>
		</axis>
		<axis name="c1" type="Rotation" home-coordinate="0.0">
		   <head-mountable-ids class="java.util.HashSet">
			  <string>N1</string>
		   </head-mountable-ids>
		   <pre-move-command><![CDATA[
		   T0S1
		   G92E{Coordinate:%.4f}
		   ]]></pre-move-command>
		</axis>
		<axis name="c2" type="Rotation" home-coordinate="0.0">
		   <head-mountable-ids class="java.util.HashSet">
			  <string>N2</string>
		   </head-mountable-ids>
		   <pre-move-command><![CDATA[
		   T1S1
		   G92E{Coordinate:%.4f}
		   ]]></pre-move-command>
		</axis>
	 </axes>
  </driver>
```


# Useful Commands While Setting Up OpenPnP
| Command | Description |
| :------------- | ------------- |
| `M42 Pxx Syyy` | Switch digital IOs P: Pin, S: 0...255 (0 off, 255 full on) |
| `M500` | Store current settings in EEPROM for the next startup or M501.
| `M501` | Read all parameters from EEPROM. (Or, undo changes.)|
| `M502` | Reset current settings to defaults, as set in configuration.h. (Follow with M500 to reset the EEPROM too.)|
| `M503` | Display current settings (stepps/mm, accel., velocity,...) |
| `M851` | Set offset of z-endstop limit switch. |
