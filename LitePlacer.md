LitePlacer is "The Low Cost Prototype Builder’s Pick and Place Machine". It's an affordable, open source pick and place machine that you can buy right now in kit form from http://www.liteplacer.com/. The LitePlacer software is very good and is designed to work specifically with the machine. You can also run OpenPnP to control the LitePlacer. OpenPnP does not integrate with LitePlacer as well as the LitePlacer software, but it does offer some additional features such as bottom vision.

To use OpenPnP with LitePlacer you have to physically switch the zmin and zmax limit switch wires on the included TinyG and then you can use [[GcodeDriver]] to power the machine. A LitePlacer forum user named sebastian has provided instructions and a sample configuration file at https://wiki.apertus.org/index.php/Liteplacer_PnP_Machine_Notes#OpenPnP_Setup.

More information about LitePlacer and OpenPnP can be found in the links below:

* http://liteplacer.com/phpBB/viewtopic.php?f=13&t=308&p=2872&hilit=openpnp#p2872
* http://liteplacer.com/phpBB/viewtopic.php?f=10&t=308

For archival purposes, the driver section from sebastian's machine.xml is reproduced below. His Wiki is a better source for this information, but in case it is not available the XML below should serve as a good starting point.

```
<driver baud="115200" class="org.openpnp.machine.reference.driver.GcodeDriver" connect-wait-time-milliseconds="1000" data-bits="Eight" flow-control="Off" max-feed-rate="1000" parity="None" port-name="COM3" set-dtr="false" set-rts="false" stop-bits="One" timeout-milliseconds="5000" units="Millimeters">
   <homing-fiducial-location rotation="0.0" units="Millimeters" x="0.0" y="0.0" z="0.0"/>
   <command type="DISABLE_COMMAND">
      <text><![CDATA[M9 M5]]></text>
   </command>
   <command type="ENABLE_COMMAND">
      <text><![CDATA[G21 M9]]></text>
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
      <text><![CDATA[$xjm=2000]]></text>
      <text><![CDATA[$xvm=8000]]></text>
      <text><![CDATA[$xsv=2000]]></text>
      <text><![CDATA[$xsn=3]]></text>
      <text><![CDATA[$xjh=2000]]></text>
      <text><![CDATA[$xsx=2]]></text>
      <text><![CDATA[$1mi=8]]></text>
      <text><![CDATA[$1sa=0.900]]></text>
      <text><![CDATA[$1tr=39.983]]></text>
      <text><![CDATA[$yjm=2000]]></text>
      <text><![CDATA[$yvm=8000]]></text>
      <text><![CDATA[$ysn=3]]></text>
      <text><![CDATA[$ysx=2]]></text>
      <text><![CDATA[$yjh=2000]]></text>
      <text><![CDATA[$ysv=1800]]></text>
      <text><![CDATA[$2mi=8]]></text>
      <text><![CDATA[$2sa=0.900]]></text>
      <text><![CDATA[$2tr=39.9540]]></text>
      <text><![CDATA[$zjm=2300]]></text>
      <text><![CDATA[$zvm=3000]]></text>
      <text><![CDATA[$zjh=2000]]></text>
      <text><![CDATA[$zsv=1500]]></text>
      <text><![CDATA[$xfr=8000]]></text>
      <text><![CDATA[$yfr=8000]]></text>
      <text><![CDATA[$zfr=3000]]></text>
      <text><![CDATA[$afr=20000]]></text>
      <text><![CDATA[$3mi=8]]></text>
      <text><![CDATA[$3sa=1.800]]></text>
      <text><![CDATA[$3tr=8.0000]]></text>
      <text><![CDATA[$ajm=600]]></text>
      <text><![CDATA[$avm=20000]]></text>
      <text><![CDATA[$4mi=8]]></text>
      <text><![CDATA[$4sa=0.900]]></text>
      <text><![CDATA[$4tr=160.0000]]></text>
      <text><![CDATA[$mt=300.00]]></text>
      <text><![CDATA[$zzb=2]]></text>
      <text><![CDATA[$3po=1]]></text>
      <text><![CDATA[$zsx=3]]></text>
      <text><![CDATA[$zsn=0]]></text>
   </command>
   <command type="HOME_COMMAND">
      <text><![CDATA[G28.2 X0 Y0 Z0 A0 ]]></text>
      <text><![CDATA[G92 X0 Y0 Z0]]></text>
   </command>
   <command type="COMMAND_CONFIRM_REGEX">
      <text><![CDATA[.*ok&gt;.*]]></text>
   </command>
   <command type="MOVE_TO_COMPLETE_REGEX">
      <text><![CDATA[.*stat:3.*]]></text>
   </command>
   <command type="PICK_COMMAND">
      <text><![CDATA[M8]]></text>
      <text><![CDATA[G4 P0.5]]></text>
   </command>
   <command type="PLACE_COMMAND">
      <text><![CDATA[M9]]></text>
      <text><![CDATA[G4 P0.5]]></text>
   </command>
   <command type="PUMP_ON_COMMAND">
      <text><![CDATA[M4]]></text>
   </command>
   <command type="PUMP_OFF_COMMAND">
      <text><![CDATA[M5]]></text>
   </command>
   <command type="MOVE_TO_COMMAND">
      <text><![CDATA[G0 {X:X%.4f} {Y:Y%.4f} {Z:Z%.4f} {Rotation:A%.4f} F{FeedRate:%.0f}]]></text>
   </command>
   <sub-drivers class="java.util.ArrayList"/>
   <axes class="java.util.ArrayList">
      <axis home-coordinate="0.0" name="x" type="X">
         <head-mountable-ids class="java.util.HashSet">
            <string>*</string>
         </head-mountable-ids>
      </axis>
      <axis home-coordinate="0.0" name="y" type="Y">
         <head-mountable-ids class="java.util.HashSet">
            <string>*</string>
         </head-mountable-ids>
      </axis>
      <axis home-coordinate="0.0" name="z" type="Z">
         <head-mountable-ids class="java.util.HashSet">
            <string>N1</string>
         </head-mountable-ids>
      </axis>
      <axis home-coordinate="0.0" name="rotation" type="Rotation">
         <head-mountable-ids class="java.util.HashSet">
            <string>*</string>
         </head-mountable-ids>
      </axis>
   </axes>
</driver>
```