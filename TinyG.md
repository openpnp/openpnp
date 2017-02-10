# GcodeDriver (Recommended)

OpenPnP now includes a generic Gcode driver that is far more flexible than the original TinyG specific driver. We recommend switching to the GcodeDriver. You can learn more about configuring the GcodeDriver at [[GcodeDriver]]. See below for an example configuration that will help.

## machine.xml Driver Section
```
<driver class="org.openpnp.machine.reference.driver.GcodeDriver" port-name="COM3" baud="115200" flow-control="Off" data-bits="Eight" stop-bits="One" parity="None" set-dtr="false" set-rts="false" units="Millimeters" max-feed-rate="1000" timeout-milliseconds="5000" connect-wait-time-milliseconds="1000">
   <command type="MOVE_TO_COMMAND">
      <text><![CDATA[G0 {X:X%.4f} {Y:Y%.4f} {Z:Z%.4f} {Rotation:A%.4f} F{FeedRate:%.0f}]]></text>
   </command>
   <command type="COMMAND_CONFIRM_REGEX">
      <text><![CDATA[.*ok>.*]]></text>
   </command>
   <command type="MOVE_TO_COMPLETE_REGEX">
      <text><![CDATA[.*stat:3.*]]></text>
   </command>
   <command type="HOME_COMMAND">
      <text><![CDATA[G28.2 X0 Y0 Z0 A0 ]]></text>
      <text><![CDATA[G92 X0 Y0 Z0]]></text>
   </command>
   <command type="DISABLE_COMMAND">
      <text><![CDATA[M9 M5]]></text>
   </command>
   <command type="CONNECT_COMMAND">
      <text><![CDATA[$SV=2]]></text>
      <text><![CDATA[$ME ]]></text>
      <text><![CDATA[$1pm=1]]></text>
      <text><![CDATA[$2pm=1]]></text>
      <text><![CDATA[$3pm=1]]></text>
      <text><![CDATA[$4pm=1]]></text>
      <text><![CDATA[$mt=1000000000]]></text>
      <text><![CDATA[G21 G90 G92 X0 Y0 Z0 A0 M8 M5]]></text>
   </command>
   <command type="ENABLE_COMMAND">
      <text><![CDATA[G21 M9]]></text>
   </command>
   <command type="PUMP_ON_COMMAND">
      <text><![CDATA[M4]]></text>
   </command>
   <command type="PUMP_OFF_COMMAND">
      <text><![CDATA[M5]]></text>
   </command>
   <command type="PLACE_COMMAND">
      <text><![CDATA[M9]]></text>
   </command>
   <command type="PICK_COMMAND">
      <text><![CDATA[M8]]></text>
   </command>
   <sub-drivers class="java.util.ArrayList"/>
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
         </head-mountable-ids>
      </axis>
      <axis name="rotation" type="Rotation" home-coordinate="0.0">
         <head-mountable-ids class="java.util.HashSet">
            <string>*</string>
         </head-mountable-ids>
      </axis>
   </axes>
</driver>
```

## TinyG Configuration
```
[fb]  firmware build            440.20
[fv]  firmware version            0.97
[hp]  hardware platform           1.00
[hv]  hardware version            8.00
[id]  TinyG ID                    3X3566-YSD
[ja]  junction acceleration 2000000 mm
[ct]  chordal tolerance           0.0100 mm
[sl]  soft limit enable           0
[st]  switch type                 0 [0=NO,1=NC]
[mt]  motor idle timeout        300.00 Sec
[ej]  enable json mode            0 [0=text,1=JSON]
[jv]  json verbosity              2 [0=silent,1=footer,2=messages,3=configs,4=linenum,5=verbose]
[js]  json serialize style        1 [0=relaxed,1=strict]
[tv]  text verbosity              1 [0=silent,1=verbose]
[qv]  queue report verbosity      2 [0=off,1=single,2=triple]
[sv]  status report verbosity     2 [0=off,1=filtered,2=verbose]
[si]  status interval           200 ms
[ec]  expand LF to CRLF on TX     0 [0=off,1=on]
[ee]  enable echo                 1 [0=off,1=on]
[ex]  enable flow control         1 [0=off,1=XON/XOFF, 2=RTS/CTS]
[baud] USB baud rate              5 [1=9600,2=19200,3=38400,4=57600,5=115200,6=230400]
[net] network mode                0 [0=master]
[gpl] default gcode plane         0 [0=G17,1=G18,2=G19]
[gun] default gcode units mode    1 [0=G20,1=G21]
[gco] default gcode coord system  1 [1-6 (G54-G59)]
[gpa] default gcode path control  2 [0=G61,1=G61.1,2=G64]
[gdi] default gcode distance mode 0 [0=G90,1=G91]
[1ma] m1 map to axis              0 [0=X,1=Y,2=Z...]
[1sa] m1 step angle               0.900 deg
[1tr] m1 travel per revolution   40.0000 mm
[1mi] m1 microsteps               8 [1,2,4,8]
[1po] m1 polarity                 1 [0=normal,1=reverse]
[1pm] m1 power management         2 [0=disabled,1=always on,2=in cycle,3=when moving]
[2ma] m2 map to axis              1 [0=X,1=Y,2=Z...]
[2sa] m2 step angle               0.900 deg
[2tr] m2 travel per revolution   40.0000 mm
[2mi] m2 microsteps               8 [1,2,4,8]
[2po] m2 polarity                 0 [0=normal,1=reverse]
[2pm] m2 power management         2 [0=disabled,1=always on,2=in cycle,3=when moving]
[3ma] m3 map to axis              2 [0=X,1=Y,2=Z...]
[3sa] m3 step angle               1.800 deg
[3tr] m3 travel per revolution    8.0000 mm
[3mi] m3 microsteps               8 [1,2,4,8]
[3po] m3 polarity                 1 [0=normal,1=reverse]
[3pm] m3 power management         2 [0=disabled,1=always on,2=in cycle,3=when moving]
[4ma] m4 map to axis              3 [0=X,1=Y,2=Z...]
[4sa] m4 step angle               0.900 deg
[4tr] m4 travel per revolution  160.0000 mm
[4mi] m4 microsteps               8 [1,2,4,8]
[4po] m4 polarity                 0 [0=normal,1=reverse]
[4pm] m4 power management         2 [0=disabled,1=always on,2=in cycle,3=when moving]
[xam] x axis mode                 1 [standard]
[xvm] x velocity maximum      10000 mm/min
[xfr] x feedrate maximum      10000 mm/min
[xtn] x travel minimum            0.000 mm
[xtm] x travel maximum          600.000 mm
[xjm] x jerk maximum           1000 mm/min^3 * 1 million
[xjh] x jerk homing            2000 mm/min^3 * 1 million
[xjd] x junction deviation        0.0100 mm (larger is faster)
[xsn] x switch min                3 [0=off,1=homing,2=limit,3=limit+homing]
[xsx] x switch max                2 [0=off,1=homing,2=limit,3=limit+homing]
[xsv] x search velocity        2000 mm/min
[xlv] x latch velocity          100 mm/min
[xlb] x latch backoff             8.000 mm
[xzb] x zero backoff              2.000 mm
[yam] y axis mode                 1 [standard]
[yvm] y velocity maximum      10000 mm/min
[yfr] y feedrate maximum      10000 mm/min
[ytn] y travel minimum            0.000 mm
[ytm] y travel maximum          400.000 mm
[yjm] y jerk maximum           1000 mm/min^3 * 1 million
[yjh] y jerk homing            2000 mm/min^3 * 1 million
[yjd] y junction deviation        0.0100 mm (larger is faster)
[ysn] y switch min                3 [0=off,1=homing,2=limit,3=limit+homing]
[ysx] y switch max                2 [0=off,1=homing,2=limit,3=limit+homing]
[ysv] y search velocity        2000 mm/min
[ylv] y latch velocity          100 mm/min
[ylb] y latch backoff             8.000 mm
[yzb] y zero backoff              2.000 mm
[zam] z axis mode                 1 [standard]
[zvm] z velocity maximum       5000 mm/min
[zfr] z feedrate maximum       5000 mm/min
[ztn] z travel minimum         -120.000 mm
[ztm] z travel maximum           80.000 mm
[zjm] z jerk maximum            500 mm/min^3 * 1 million
[zjh] z jerk homing             500 mm/min^3 * 1 million
[zjd] z junction deviation        0.0100 mm (larger is faster)
[zsn] z switch min                0 [0=off,1=homing,2=limit,3=limit+homing]
[zsx] z switch max                3 [0=off,1=homing,2=limit,3=limit+homing]
[zsv] z search velocity        1000 mm/min
[zlv] z latch velocity          100 mm/min
[zlb] z latch backoff             4.000 mm
[zzb] z zero backoff              2.000 mm
[aam] a axis mode                 1 [standard]
[avm] a velocity maximum      50000 deg/min
[afr] a feedrate maximum     200000 deg/min
[atn] a travel minimum           -1.000 deg
[atm] a travel maximum          400.000 deg
[ajm] a jerk maximum           5000 deg/min^3 * 1 million
[ajh] a jerk homing            5000 deg/min^3 * 1 million
[ajd] a junction deviation        0.0100 deg (larger is faster)
[ara] a radius value              5.3052 deg
[asn] a switch min                0 [0=off,1=homing,2=limit,3=limit+homing]
[asx] a switch max                0 [0=off,1=homing,2=limit,3=limit+homing]
[asv] a search velocity        2000 deg/min
[alv] a latch velocity         2000 deg/min
[alb] a latch backoff             5.000 deg
[azb] a zero backoff              2.000 deg
[bam] b axis mode                 0 [disabled]
[bvm] b velocity maximum       3600 deg/min
[bfr] b feedrate maximum       3600 deg/min
[btn] b travel minimum           -1.000 deg
[btm] b travel maximum           -1.000 deg
[bjm] b jerk maximum             20 deg/min^3 * 1 million
[bjd] b junction deviation        0.0100 deg (larger is faster)
[bra] b radius value              1.0000 deg
[cam] c axis mode                 0 [disabled]
[cvm] c velocity maximum       3600 deg/min
[cfr] c feedrate maximum       3600 deg/min
[ctn] c travel minimum           -1.000 deg
[ctm] c travel maximum           -1.000 deg
[cjm] c jerk maximum             20 deg/min^3 * 1 million
[cjd] c junction deviation        0.0100 deg (larger is faster)
[cra] c radius value              1.0000 deg
[p1frq] pwm frequency               100 Hz
[p1csl] pwm cw speed lo            1000 RPM
[p1csh] pwm cw speed hi            2000 RPM
[p1cpl] pwm cw phase lo           0.125 [0..1]
[p1cph] pwm cw phase hi           0.200 [0..1]
[p1wsl] pwm ccw speed lo           1000 RPM
[p1wsh] pwm ccw speed hi           2000 RPM
[p1wpl] pwm ccw phase lo          0.125 [0..1]
[p1wph] pwm ccw phase hi          0.200 [0..1]
[p1pof] pwm phase off             0.100 [0..1]
[g54x] g54 x offset               0.000 mm
[g54y] g54 y offset               0.000 mm
[g54z] g54 z offset               0.000 mm
[g54a] g54 a offset               0.000 deg
[g54b] g54 b offset               0.000 deg
[g54c] g54 c offset               0.000 deg
[g55x] g55 x offset             145.000 mm
[g55y] g55 y offset             160.000 mm
[g55z] g55 z offset               0.000 mm
[g55a] g55 a offset               0.000 deg
[g55b] g55 b offset               0.000 deg
[g55c] g55 c offset               0.000 deg
[g56x] g56 x offset               0.000 mm
[g56y] g56 y offset               0.000 mm
[g56z] g56 z offset               0.000 mm
[g56a] g56 a offset               0.000 deg
[g56b] g56 b offset               0.000 deg
[g56c] g56 c offset               0.000 deg
[g57x] g57 x offset               0.000 mm
[g57y] g57 y offset               0.000 mm
[g57z] g57 z offset               0.000 mm
[g57a] g57 a offset               0.000 deg
[g57b] g57 b offset               0.000 deg
[g57c] g57 c offset               0.000 deg
[g58x] g58 x offset               0.000 mm
[g58y] g58 y offset               0.000 mm
[g58z] g58 z offset               0.000 mm
[g58a] g58 a offset               0.000 deg
[g58b] g58 b offset               0.000 deg
[g58c] g58 c offset               0.000 deg
[g59x] g59 x offset               0.000 mm
[g59y] g59 y offset               0.000 mm
[g59z] g59 z offset               0.000 mm
[g59a] g59 a offset               0.000 deg
[g59b] g59 b offset               0.000 deg
[g59c] g59 c offset               0.000 deg
[g92x] g92 x offset               0.000 mm
[g92y] g92 y offset               0.000 mm
[g92z] g92 z offset               0.000 mm
[g92a] g92 a offset               0.000 deg
[g92b] g92 b offset               0.000 deg
[g92c] g92 c offset               0.000 deg
[g28x] g28 x position             0.000 mm
[g28y] g28 y position             0.000 mm
[g28z] g28 z position             0.000 mm
[g28a] g28 a position             0.000 deg
[g28b] g28 b position             0.000 deg
[g28c] g28 c position             0.000 deg
[g30x] g30 x position             0.000 mm
[g30y] g30 y position             0.000 mm
[g30z] g30 z position             0.000 mm
[g30a] g30 a position             0.000 deg
[g30b] g30 b position             0.000 deg
[g30c] g30 c position             0.000 deg
```

## Troubleshooting

* If you are using TinyG G2 you may need to set DTR and RTS. You can do this from the driver configuration UI in Machine Setup.
* If your pick and place commands fire before moves are complete, make sure you are using `move-to-complete-regex` as shown in the configuration above.

## Quirks

TinyG has some quirks that make it hard to use for pick and place. I want to look more into these later, so I am putting these references here so I can come back to it:

* G4P0 does not want for end of movement: https://github.com/synthetos/g2/issues/138
* Movement status not always sent. I think this happens for small moves, mostly: https://github.com/synthetos/g2/issues/139
* Different Gcode required for small moves: https://groups.google.com/forum/#!msg/openpnp/j-TAyyZ9XQ0/NIl9ZUNnBwAJ
* Also related to small moves: https://www.synthetos.com/topics/g0-minimum-movement/

# TinygDriver (Outdated, Not Recommended)

## Driver Source

The current source for the TinygDriver can be found at https://github.com/openpnp/openpnp/blob/develop/src/main/java/org/openpnp/machine/reference/driver/TinygDriver.java.
It's worth taking a look through the source code when doing an integration with TinyG. The source is not complex and it is the best reference for how things work.

## Usage Notes

OpenPnP installs with default motor control set for demo mode. To use TinyG as your motion controller, follow the instructions at https://github.com/openpnp/openpnp/wiki/User-Manual#the-driver and set your driver to `org.openpnp.machine.reference.driver.TinygDriver`.

If you'd prefer to set the driver manually the machine.xml file will need to be edited. The machine.xml needs to be created if it does not exist, launch OpenPnP and then exit, If OpenPnP cannot find the machine.xml file, it will create the file with defaults. The following line should replace the motor control demo line in the machine.xml file:

`<driver class="org.openpnp.machine.reference.driver.TinygDriver" port-name="COM10" baud="115200" feed-rate-mm-per-minute="5000.0">`

port-name will vary, Windows will assign the next available com port, windows control panel can verify which port was added. Linux will appear similar to "port-name=/dev/ttyS0".

TinyG Default settings should be checked, before using with OpenPnP. In Windows Hyperterminal or Coolterm can be used to verify settings. OpenPnP requires 115200, 8N1, Flow control off. After connecting TinyG to the USB port, communications software settings should be set to 115200, 8N1, CTS. This should allow the PC to send and receive from the TinyG pcb. The first settings to check are flow control, "$ex" will return 0, 1 or 2, OpenPnP requires flow control off, "$ex 0" will set flow control off. TinyG firmware version can be checked using "$fv", current version is 0.970 . 

TinyG has built in test routines, sending "$test=3" will move the X and Y stepper in an attempt to create a square. It is recommended for initial testing that TinyG be connected to steppers and tested for operation before connecting to actual PnP hardware, bench testing is simpler to debug motor issues, and this will prevent damage in case limit switches aren't connected properly.

The TinyG controller should be configured for use with OpenPnP now. Close any software that has been used to test the TinyG so that the comm port will be released. Launch OpenPnP, open the Jog controls, using the X or Y axis buttons the stepper motors should operate for a short time for each jog command issued.

## Gcode Mapping

The following Gcodes are currently sent by the driver:

Function      | Gcode
------------- | -------------
Home          | G28.2 X0 Y0 Z0 A0
Zero Coords.  | G92 X Y Z A
Move          | G1 X Y Z A F
Pick          | M4
Place         | M5
Actuator On   | M8
Actuator Off  | M9
