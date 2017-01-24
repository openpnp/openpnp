Example configurations for the GcodeDriver with various controllers. If you have a known working config, please post it here so that others can use it as a starting point for their machine.

### Sub-Driver Example
In the video below there are two GcodeDrivers running. One is controlling the Smoothieboard running the machine and one is controlling an Arduino running a simple sketch. The video shows several feeders which use actuators to send commands to a second controller. This is intended to demonstrate how you can easily integrate additional hardware such as feeders.

Demonstration Video (Pay attention to the red LEDs): https://www.youtube.com/watch?v=0ntYOy0s_8Y

<details>
  <summary>machine.xml</summary>
  <p>
```
<openpnp-machine>
   <machine class="org.openpnp.machine.reference.ReferenceMachine" speed="1.0">
      <heads>
         <head class="org.openpnp.machine.reference.ReferenceHead" id="22964dce-252a-453e-8106-65db104a0763" name="H1">
            <nozzles>
               <nozzle class="org.openpnp.machine.reference.ReferenceNozzle" id="69edd567-df6c-495a-9b30-2fcbf5c9742f" name="N1" pick-dwell-milliseconds="500" place-dwell-milliseconds="500" current-nozzle-tip-id="e092921a-2eef-449b-b340-aa3f40d8d791" changer-enabled="false" limit-rotation="true">
                  <nozzle-tips>
                     <nozzle-tip class="org.openpnp.machine.reference.ReferenceNozzleTip" id="e092921a-2eef-449b-b340-aa3f40d8d791" name="NT1" allow-incompatible-packages="true">
                        <compatible-package-ids class="java.util.HashSet"/>
                        <changer-start-location units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
                        <changer-mid-location units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
                        <changer-mid-location-2 units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
                        <changer-end-location units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
                     </nozzle-tip>
                  </nozzle-tips>
                  <head-offsets units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
                  <safe-z value="0.0" units="Millimeters"/>
               </nozzle>
            </nozzles>
            <actuators/>
            <cameras>
               <camera class="org.openpnp.machine.reference.camera.ImageCamera" id="d66faf53-05e1-4629-baae-b614c5ed8320" name="Zoom" looking="Down" settle-time-ms="250" rotation="0.0" flip-x="false" flip-y="false" offset-x="0" offset-y="0" crop-width="0" crop-height="0" fps="24" width="640" height="480">
                  <units-per-pixel units="Millimeters" x="0.04233" y="0.04233" z="0.0" rotation="0.0"/>
                  <vision-provider class="org.openpnp.machine.reference.vision.OpenCvVisionProvider"/>
                  <head-offsets units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
                  <safe-z value="0.0" units="Millimeters"/>
                  <source-uri>classpath://samples/pnp-test/pnp-test.png</source-uri>
               </camera>
            </cameras>
            <paste-dispensers>
               <paste-dispenser class="org.openpnp.machine.reference.ReferencePasteDispenser" id="53050ccf-59a0-4d9f-a8d3-6216f5412e4e" name="D1">
                  <head-offsets units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
                  <safe-z value="0.0" units="Millimeters"/>
               </paste-dispenser>
            </paste-dispensers>
            <park-location units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
         </head>
      </heads>
      <signalers/>
      <feeders>
         <feeder class="org.openpnp.machine.reference.feeder.ReferenceAutoFeeder" id="ec54bc2c-b18d-4088-aca5-dab09f5bf3d0" name="ReferenceAutoFeeder" enabled="true" part-id="R0201-1K" retry-count="3" actuator-name="A1" actuator-value="0.0">
            <location units="Millimeters" x="100.0" y="-200.0" z="-20.0" rotation="0.0"/>
         </feeder>
         <feeder class="org.openpnp.machine.reference.feeder.ReferenceAutoFeeder" id="aed8f089-f65d-42e6-baa6-d65bebd567a6" name="ReferenceAutoFeeder" enabled="true" part-id="R0402-1K" retry-count="3" actuator-name="A2" actuator-value="0.0">
            <location units="Millimeters" x="110.0" y="-200.0" z="-20.0" rotation="0.0"/>
         </feeder>
         <feeder class="org.openpnp.machine.reference.feeder.ReferenceAutoFeeder" id="de8de97f-fef7-4c5d-a525-e2e4ce257ff8" name="ReferenceAutoFeeder" enabled="true" part-id="R0603-1K" retry-count="3" actuator-name="A3" actuator-value="0.0">
            <location units="Millimeters" x="120.0" y="-200.0" z="-20.0" rotation="0.0"/>
         </feeder>
         <feeder class="org.openpnp.machine.reference.feeder.ReferenceAutoFeeder" id="53a97089-1bf8-4632-b91b-3f7f3d46362c" name="ReferenceAutoFeeder" enabled="true" part-id="R0805-1K" retry-count="3" actuator-name="A4" actuator-value="0.0">
            <location units="Millimeters" x="130.0" y="-200.0" z="-20.0" rotation="0.0"/>
         </feeder>
      </feeders>
      <cameras/>
      <actuators>
         <actuator class="org.openpnp.machine.reference.ReferenceActuator" id="A1" name="A1" index="0">
            <head-offsets units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
            <safe-z value="0.0" units="Millimeters"/>
         </actuator>
         <actuator class="org.openpnp.machine.reference.ReferenceActuator" id="A2" name="A2" index="1">
            <head-offsets units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
            <safe-z value="0.0" units="Millimeters"/>
         </actuator>
         <actuator class="org.openpnp.machine.reference.ReferenceActuator" id="A3" name="A3" index="2">
            <head-offsets units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
            <safe-z value="0.0" units="Millimeters"/>
         </actuator>
         <actuator class="org.openpnp.machine.reference.ReferenceActuator" id="A4" name="A4" index="3">
            <head-offsets units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
            <safe-z value="0.0" units="Millimeters"/>
         </actuator>
      </actuators>
      <discard-location units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
      <driver class="org.openpnp.machine.reference.driver.GcodeDriver" port-name="/dev/tty.usbmodem1A1211" baud="115200" flow-control="Off" data-bits="Eight" stop-bits="One" parity="None" set-dtr="false" set-rts="false" units="Millimeters" max-feed-rate="15000" timeout-milliseconds="5000" connect-wait-time-milliseconds="0">
         <command type="COMMAND_CONFIRM_REGEX">
            <text><![CDATA[^ok.*]]></text>
         </command>
         <command type="CONNECT_COMMAND">
            <text><![CDATA[G21]]></text>
            <text><![CDATA[G90]]></text>
            <text><![CDATA[M82]]></text>
         </command>
         <command type="ENABLE_COMMAND">
            <text><![CDATA[M810]]></text>
         </command>
         <command type="DISABLE_COMMAND">
            <text><![CDATA[M84]]></text>
            <text><![CDATA[M811]]></text>
         </command>
         <command type="HOME_COMMAND">
            <text><![CDATA[M84]]></text>
            <text><![CDATA[G4P500]]></text>
            <text><![CDATA[G28 X0 Y0]]></text>
            <text><![CDATA[G92 X0 Y0 Z0 E0]]></text>
         </command>
         <command type="MOVE_TO_COMMAND">
            <text><![CDATA[G0 {X:X%.4f} {Y:Y%.4f} {Z:Z%.4f} {Rotation:E%.4f} F{FeedRate:%.0f}]]></text>
            <text><![CDATA[M400]]></text>
         </command>
         <command type="PICK_COMMAND">
            <text><![CDATA[M808]]></text>
            <text><![CDATA[M800]]></text>
         </command>
         <command type="PLACE_COMMAND">
            <text><![CDATA[M809]]></text>
            <text><![CDATA[M801]]></text>
            <text><![CDATA[M802]]></text>
            <text><![CDATA[G4P250]]></text>
            <text><![CDATA[M803]]></text>
         </command>
         <sub-drivers class="java.util.ArrayList">
            <reference-driver class="org.openpnp.machine.reference.driver.GcodeDriver" port-name="/dev/tty.usbmodem1A12421" baud="9600" flow-control="Off" data-bits="Eight" stop-bits="One" parity="None" set-dtr="false" set-rts="false" units="Millimeters" max-feed-rate="50000" timeout-milliseconds="5000" connect-wait-time-milliseconds="750">
               <command type="COMMAND_CONFIRM_REGEX">
                  <text><![CDATA[^ok.*]]></text>
               </command>
               <command type="ACTUATE_DOUBLE_COMMAND">
                  <text><![CDATA[{Index}]]></text>
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
                        <string>*</string>
                     </head-mountable-ids>
                  </axis>
                  <axis name="rotation" type="Rotation" home-coordinate="0.0">
                     <head-mountable-ids class="java.util.HashSet">
                        <string>*</string>
                     </head-mountable-ids>
                  </axis>
               </axes>
            </reference-driver>
         </sub-drivers>
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
                  <string>*</string>
               </head-mountable-ids>
            </axis>
            <axis name="rotation" type="Rotation" home-coordinate="0.0">
               <head-mountable-ids class="java.util.HashSet">
                  <string>*</string>
               </head-mountable-ids>
            </axis>
         </axes>
      </driver>
      <pnp-job-processor class="org.openpnp.machine.reference.ReferencePnpJobProcessor" park-when-complete="false"/>
      <paste-dispense-job-processor class="org.openpnp.machine.reference.ReferencePasteDispenseJobProcessor" park-when-complete="false"/>
      <glue-dispense-job-processor class="org.openpnp.machine.reference.ReferenceGlueDispenseJobProcessor" park-when-complete="false"/>
      <fiducial-locator class="org.openpnp.machine.reference.vision.ReferenceFiducialLocator"/>
   </machine>
</openpnp-machine>
```
  <p>
</details>

<details>
  <summary>Arduino Sketch</summary>
  <p>
```
String inData;

void setup() {
  for (int i = 0; i < 4; i++) {
    pinMode(2 + i, OUTPUT);
    digitalWrite(2 + i, LOW);
  }
  
  Serial.begin(9600);
  Serial.println("Feeduino");
  Serial.println("ok");
}

void loop() {
    while (Serial.available() > 0) {
      char recieved = Serial.read();
      inData += recieved; 

      if (recieved == '\n') {
        processCommand();
        inData = "";
      }
  }
}

void processCommand() {
  int pin = inData.toInt();
  digitalWrite(2 + pin, HIGH);
  delay(500);
  digitalWrite(2 + pin, LOW);
  Serial.println("ok");
}
```
  </p>
</details>

### Alex's MKS Sbase 1.3 Configuration

This is a configuration from mailing list user Александр Зендриков. It uses an MKS Sbase controller and has two nozzles in a cam configuration.

The files can be found at https://gist.github.com/vonnieda/7a72c5d7b459f8da00e7124e1067f9e7.

The discussion is at https://groups.google.com/d/msgid/openpnp/f487734a-4af2-4d6a-b8d2-ef4fbe267e49%40googlegroups.com?utm_medium=email&utm_source=footer.