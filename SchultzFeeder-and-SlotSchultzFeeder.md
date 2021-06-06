Before starting with the SlotSchultzFeeder, read the [ReferenceSlotAutofeeder page](https://github.com/openpnp/openpnp/wiki/ReferenceSlotAutoFeeder) to understand the core concepts.

You will need a controller board that takes Gcode from OpenPnP to control the feeders.  The details for a low cost controller can be found [here](https://github.com/bilsef/SchultzController).  It costs the same to build 1 board or 5 boards, so check on the OpenPnP Google Group to see if people have extras to share.

![Controller Board](https://user-images.githubusercontent.com/2394996/77212003-e49e4a00-6ac2-11ea-8f16-81dd4ba5883c.JPG)

## Actuators

The SlotSchultzFeeder is an extension of the SchultzFeeder, and both use the same actuators. You will need to define the following set of actuators:
1. GetID   - Reads the feeder ID from the EEPROM.
2. Pre-Pick  - Opens the shutter.
3. Post-Pick  - Closes shutter and advances the tape.
4. AdvanceIgnoreError  - Normally the tape will not advance if the feeder reports an error.  This will ignore the error and advance anyway.
5. GetCount  - Reports current feed count.
6. ClearCount  - Resets the feed count to 0.
7. GetPitch  - Reports the current pitch setting.
8. TogglePitch  - Toggles between 2mm and 4mm pitch.
9. GetStatus  - Reports the feeder status.

If you are using more than one controller, you will need a set of actuators for each one.  They require unique names, so add a prefix or suffix to identify which controller they will be used with (for example East, West).

Example actuators in machine.xml

```
         <actuator class="org.openpnp.machine.reference.ReferenceActuator" id="actSchultzGetID" name="SchultzGetID" value-type="Double" value-type-confirmed="true" default-on-double="0.0" default-on-string="" default-off-double="0.0" default-off-string="" interlock-actuator="false" driver-id="DRV1685dcff7c76eec8" coordinated-before-actuate="false" coordinated-after-actuate="false" coordinated-before-read="false" enabled-actuation="LeaveAsIs" homed-actuation="LeaveAsIs" disabled-actuation="LeaveAsIs" index="0">
            <head-offsets units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
         </actuator>
         <actuator class="org.openpnp.machine.reference.ReferenceActuator" id="actSchultzPrePick" name="SchultzPrePick" value-type="Double" value-type-confirmed="true" default-on-double="0.0" default-on-string="" default-off-double="0.0" default-off-string="" interlock-actuator="false" driver-id="DRV1685dcff7c76eec8" coordinated-before-actuate="false" coordinated-after-actuate="false" coordinated-before-read="false" enabled-actuation="LeaveAsIs" homed-actuation="LeaveAsIs" disabled-actuation="LeaveAsIs" index="0">
            <head-offsets units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
         </actuator>
         <actuator class="org.openpnp.machine.reference.ReferenceActuator" id="actSchultzPostPick" name="SchultzPostPick" value-type="Double" value-type-confirmed="true" default-on-double="0.0" default-on-string="" default-off-double="0.0" default-off-string="" interlock-actuator="false" driver-id="DRV1685dcff7c76eec8" coordinated-before-actuate="false" coordinated-after-actuate="false" coordinated-before-read="false" enabled-actuation="LeaveAsIs" homed-actuation="LeaveAsIs" disabled-actuation="LeaveAsIs" index="0">
            <head-offsets units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
         </actuator>
         <actuator class="org.openpnp.machine.reference.ReferenceActuator" id="actSchultzAdvIgnorErr" name="SchultzAdvIgnoreErr" value-type="Double" value-type-confirmed="true" default-on-double="0.0" default-on-string="" default-off-double="0.0" default-off-string="" interlock-actuator="false" driver-id="DRV1685dcff7c76eec8" coordinated-before-actuate="false" coordinated-after-actuate="false" coordinated-before-read="false" enabled-actuation="LeaveAsIs" homed-actuation="LeaveAsIs" disabled-actuation="LeaveAsIs" index="0">
            <head-offsets units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
         </actuator>
         <actuator class="org.openpnp.machine.reference.ReferenceActuator" id="actSchultzGetCount" name="SchultzGetCount" value-type="Double" value-type-confirmed="true" default-on-double="0.0" default-on-string="" default-off-double="0.0" default-off-string="" interlock-actuator="false" driver-id="DRV1685dcff7c76eec8" coordinated-before-actuate="false" coordinated-after-actuate="false" coordinated-before-read="false" enabled-actuation="LeaveAsIs" homed-actuation="LeaveAsIs" disabled-actuation="LeaveAsIs" index="0">
            <head-offsets units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
         </actuator>
         <actuator class="org.openpnp.machine.reference.ReferenceActuator" id="actSchultzClearCount" name="SchultzClearCount" value-type="Double" value-type-confirmed="true" default-on-double="0.0" default-on-string="" default-off-double="0.0" default-off-string="" interlock-actuator="false" driver-id="DRV1685dcff7c76eec8" coordinated-before-actuate="false" coordinated-after-actuate="false" coordinated-before-read="false" enabled-actuation="LeaveAsIs" homed-actuation="LeaveAsIs" disabled-actuation="LeaveAsIs" index="0">
            <head-offsets units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
         </actuator>
         <actuator class="org.openpnp.machine.reference.ReferenceActuator" id="actSchultzGetPitch" name="SchultzGetPitch" value-type="Double" value-type-confirmed="true" default-on-double="0.0" default-on-string="" default-off-double="0.0" default-off-string="" interlock-actuator="false" driver-id="DRV1685dcff7c76eec8" coordinated-before-actuate="false" coordinated-after-actuate="false" coordinated-before-read="false" enabled-actuation="LeaveAsIs" homed-actuation="LeaveAsIs" disabled-actuation="LeaveAsIs" index="0">
            <head-offsets units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
         </actuator>
         <actuator class="org.openpnp.machine.reference.ReferenceActuator" id="actSchultzTogglePitch" name="SchultzTogglePitch" value-type="Double" value-type-confirmed="true" default-on-double="0.0" default-on-string="" default-off-double="0.0" default-off-string="" interlock-actuator="false" driver-id="DRV1685dcff7c76eec8" coordinated-before-actuate="false" coordinated-after-actuate="false" coordinated-before-read="false" enabled-actuation="LeaveAsIs" homed-actuation="LeaveAsIs" disabled-actuation="LeaveAsIs" index="0">
            <head-offsets units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
         </actuator>
         <actuator class="org.openpnp.machine.reference.ReferenceActuator" id="actSchultzGetStatus" name="SchultzGetStatus" value-type="Double" value-type-confirmed="true" default-on-double="0.0" default-on-string="" default-off-double="0.0" default-off-string="" interlock-actuator="false" driver-id="DRV1685dcff7c76eec8" coordinated-before-actuate="false" coordinated-after-actuate="false" coordinated-before-read="false" enabled-actuation="LeaveAsIs" homed-actuation="LeaveAsIs" disabled-actuation="LeaveAsIs" index="0">
            <head-offsets units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
         </actuator>
      </actuators>
```

## Sub-drivers

Add a Gcode sub-driver for each controller.  In the sub-driver, you define the Gcode that is sent and the response regex for each actuator.  Here is a sample for the above actuators.

```
         <driver class="org.openpnp.machine.reference.driver.GcodeDriver" id="DRV1685dcff7c76eec8" name="Schultz" motion-control-type="ToolpathFeedRate" communications="serial" connection-keep-alive="false" units="Millimeters" max-feed-rate="1000" backlash-offset-x="-1.0" backlash-offset-y="-1.0" backlash-offset-z="0.0" backlash-offset-r="0.0" non-squareness-factor="0.0" backlash-feed-rate-factor="0.1" timeout-milliseconds="5000" connect-wait-time-milliseconds="3000" visual-homing-enabled="true" backslash-escaped-characters-enabled="false" remove-comments="true" compress-gcode="false" logging-gcode="false" supporting-pre-move="false" using-letter-variables="true" infinity-timeout-milliseconds="60000">
            <serial line-ending-type="LF" port-name="COM3" baud="115200" flow-control="RtsCts" data-bits="Eight" stop-bits="One" parity="None" set-dtr="false" set-rts="false" name="SerialPortCommunications"/>
            <tcp line-ending-type="LF" ip-address="127.0.0.1" port="23" name="TcpCommunications"/>
            <simulated line-ending-type="LF"/>
            <homing-fiducial-location units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
            <detected-firmware><![CDATA[FIRMWARE_NAME: Schultz Feeder Controller, FIRMWARE_VERSION: 2.0]]></detected-firmware>
            <command type="COMMAND_CONFIRM_REGEX">
               <text><![CDATA[^ok.*]]></text>
            </command>
            <command type="COMMAND_ERROR_REGEX">
               <text><![CDATA[^error.*]]></text>
            </command>
            <command head-mountable-id="actSchultzGetID" type="ACTUATOR_READ_COMMAND">
               <text><![CDATA[M610N{IntegerValue}]]></text>
            </command>
            <command head-mountable-id="actSchultzGetID" type="ACTUATOR_READ_REGEX">
               <text><![CDATA[^ok.*ID: (?<Value>.+)]]></text>
            </command>
            <command head-mountable-id="actSchultzPrePick" type="ACTUATOR_READ_COMMAND">
               <text><![CDATA[M600N{IntegerValue}]]></text>
            </command>
            <command head-mountable-id="actSchultzPostPick" type="ACTUATOR_READ_COMMAND">
               <text><![CDATA[M601N{IntegerValue}]]></text>
            </command>
            <command head-mountable-id="actSchultzAdvIgnorErr" type="ACTUATOR_READ_COMMAND">
               <text><![CDATA[M601N{IntegerValue}X1]]></text>
            </command>
            <command head-mountable-id="actSchultzGetCount" type="ACTUATOR_READ_COMMAND">
               <text><![CDATA[M603N{IntegerValue}]]></text>
            </command>
            <command head-mountable-id="actSchultzGetCount" type="ACTUATOR_READ_REGEX">
               <text><![CDATA[^ok.*count: (?<Value>\d+).*]]></text>
            </command>
            <command head-mountable-id="actSchultzClearCount" type="ACTUATOR_READ_COMMAND">
               <text><![CDATA[M623N{IntegerValue}]]></text>
            </command>
            <command head-mountable-id="actSchultzGetPitch" type="ACTUATOR_READ_COMMAND">
               <text><![CDATA[M608N{IntegerValue}]]></text>
            </command>
            <command head-mountable-id="actSchultzGetPitch" type="ACTUATOR_READ_REGEX">
               <text><![CDATA[^ok.(?<Value>.+)]]></text>
            </command>
            <command head-mountable-id="actSchultzTogglePitch" type="ACTUATOR_READ_COMMAND">
               <text><![CDATA[M628N{IntegerValue}]]></text>
            </command>
            <command head-mountable-id="actSchultzGetStatus" type="ACTUATOR_READ_REGEX">
               <text><![CDATA[^ok.*Status: (?<Value>.+)]]></text>
            </command>
            <command head-mountable-id="actSchultzGetStatus" type="ACTUATOR_READ_COMMAND">
               <text><![CDATA[M602N{IntegerValue}]]></text>
            </command>
         </driver>
```

# Setup

![AddFeeder](https://user-images.githubusercontent.com/2394996/77239413-9d35bd80-6b97-11ea-9b1b-fa7b82e74a6a.png)

1. Add a SlotSchultzFeeder by using the New Feeder button in the Feeders panel. The feeder will have a default bank assigned. If you want to change the name, click New and overwrite the automatically assigned name.
2. Set the SlotSchultzFeeder's name by double clicking it in the table. Use whatever name you use to identify the slot on the machine.
3. Physically mount the feeder you wish to use in this slot.
4. Assign the Feeder Number from 0 to 40.
5. Select the actuators using the selection box for each actuator.
6. Test the acutators using the buttons to the right of the actuator name.
7. When the Get ID actuator is working, you should see the ID.  Click the New button next to the feeder line in the Slot panel.  A new feeder will be crated with the feeder ID.
8. For the feeder Location, use the fiducial dot of the feeder.  After assigning a Fiducial Part and adjusting the pipeline to recognize it, you can click the Fiducial icon to locate the feeder precicely.  This is also useful when swapping feeders in the slot, since the position can change slightly.
9. Choose the part that is installed in the feeder. Typically you will keep this value set until you replace the reel in the feeder.
10. Set the pick location of the feeder by centering the camera over the exposed part in the feeder and click the camera capture button in the Feeder Offsets section.  The offsets will be set to the feeder location minus the current location.  Adjust the rotation of the part in the feeder if required.
11. Click the Feed and Pick button in the toolbar and test it out!

There are some options for the PrePick and PostPick actuators, depending on how you will use the feeder.  In some cases, you may not use the shutter.  In that case you would use the feed (PostPick or AdvanceIgnoreError) for the PrePick actuator and leave the PostPick actuator blank.

For initial testing, you might use AdvanceIgnoreError for the PostPick actuator, but once you have a reel loaded and the cover tape tension is correct, change it to the regular PostPick actuator.

# Usage

Once a slot is configured you should not have to change it's location, bank or actuators. In daily use, all you will need to do is change the feeder that is installed in the slot.

## Moving a Feeder to a New Slot

1. Select the slot in the Feeders tab.
2. In the configuration panel below, click the Feeder dropdown and select the new feeder to install in the slot. If the feeder was previously installed in another slot it will be removed from that slot and installed in the selected one.
3. Press Apply to save your changes.

## Changing a Part or Reel

To change the part installed in a particular feeder:

1. Select the slot in the Feeders tab that holds the feeder you want to change.
2. In the configuration panel below, click the Part dropdown and select the new part for this feeder.
3. Press Apply to save your changes.
4. If you would like to keep track of how many parts are used, you can clear the count.

# Scripts

There are a number of scripts available to aid in the use of these feeders.  The scripts are located here: [SchultzController Scripts](https://github.com/bilsef/SchultzController/tree/master/Scripts)

## CreateShultzFeeders.js

This script automates adding feeder slots to OpenPnP.  [Instructions for using the script.](https://github.com/bilsef/SchultzController/wiki/4.-Create-feeder-slots-in-OpenPnP)

## AlignFeeders.js

This script moves the head mounted camera to each SlotSchultzFeeder location and adjust the location based on the fiducial.  Useful to run after the above script executes, and after feeders are swapped out between jobs.

## LoadFeederSlots.js

Checks each SlotSchultzFeeder location to see what feeder is installed and updates the slot.  Run after feeders are swapped out.  Feeders must already be in the database (see step 7 in [[Setup]]).

## UnloadAllSlots.js

Clears all SlotSchultzFeeder slots.
