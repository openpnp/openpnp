# Actuators and Other Head Objects
Actuators are generic devices you can attach to your machine to do additional tasks not covered by Nozzles and Cameras. For instance, you can use a solenoid with a pin attached to the head to drag a tape forward to feed it. Actuators are an advanced topic and typically will require some modifications to the software to do what you want.

## Adding Actuators
TODO

2016-09Sep-03: Notes from Jacob Christ: It looks like actuators can be added in two places in the machine.xml:
* In a list of actuators within the defined machine
* In a list of actuators within a defined head

The first type of actuator is independent of a head, maybe this could be used to advance a conveyor connected to your machine.

The second type of actuator is connected to a head and can move with the machine.  This may be fore a drag feeder that is attached to the head of your machine.

See [actuate-boolean-command](https://github.com/openpnp/openpnp/wiki/GcodeDriver#actuate-boolean-command)

See [actuate-double-command](https://github.com/openpnp/openpnp/wiki/GcodeDriver#actuate-double-command)

As far as I can tell there is no way to add an actuator within openpnp at this time, instead the machine.xml file must be edited.  Once an actuator has been added (for a head) the gcode to control the actuator can be adjusted in the gcode driver.  However, adding and adjusting machine actuators gcode commands do not seem editable from within openpnp at this time.

Adding actuators to machine.xml in either the head or machine portion of the file might look something like this:

            <actuators>
               <actuator class="org.openpnp.machine.reference.ReferenceActuator" id="5b8a8cbd-d6c1-4324-af8b-eba5f0444622" name="A1" index="0">
                  <head-offsets units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
                  <safe-z value="0.0" units="Millimeters"/>
               </actuator>
               <actuator class="org.openpnp.machine.reference.ReferenceActuator" id="5b8a8cbd-d6c1-4324-af8b-eba5f0444624" name="ResetSmoothie" index="1">
                  <head-offsets units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
                  <safe-z value="0.0" units="Millimeters"/>
               </actuator>
            </actuators>

Each one of these actuators has an id, I think they need to be unique, the ones shown above are not.

In your gcode driver something like the following would be added for a specific actuation:

         <command head-mountable-id="5b8a8cbd-d6c1-4324-af8b-eba5f0444624" type="ACTUATE_BOOLEAN_COMMAND">
            <text><![CDATA[{True:M999}]]></text>
            <text><![CDATA[M400]]></text>
         </command>

### The roll of the actuator class
TODO, eg: class="org.openpnp.machine.reference.ReferenceActuator"
### The roll of the actuator id
TODO, eg: id="5b8a8cbd-d6c1-4324-af8b-eba5f0444624"
### The roll of the actuator name
TODO, eg: name="ResetSmoothie"
### The roll of the actuator index
TODO, eg: index="1"

## Head Offsets
TODO: See [Setting Head Offsets](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Nozzle-Setup#head-offsets) for the general process. It is basically the same for Actuators.

***

| Previous Step                 | Jump To                 | Next Step                                   |
| ----------------------------- | ----------------------- | ------------------------------------------- |
| [[Nozzle Setup|Setup and Calibration: Nozzle Setup]] | [[Table of Contents|Setup and Calibration]] | [[Bottom Camera Setup|Setup and Calibration: Bottom Camera Setup]] |