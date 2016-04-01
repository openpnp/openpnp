# Pre-Release Warning: This feature is not yet released. The documentation is not complete.

GcodeDriver is a universal driver that makes it possible to configure complex machines and add-on hardware such as feeders without having to write any custom driver code. All configuration is done through commands in the configuration files.

Demonstration Video: https://www.youtube.com/watch?v=0ntYOy0s_8Y

Sample Configuration
```
<openpnp-machine>
   <machine class="org.openpnp.machine.reference.ReferenceMachine">
      <heads>
         <head class="org.openpnp.machine.reference.ReferenceHead" id="22964dce-252a-453e-8106-65db104a0763" name="H1">
            <nozzles>
               <nozzle class="org.openpnp.machine.reference.ReferenceNozzle" id="69edd567-df6c-495a-9b30-2fcbf5c9742f" name="N1" pick-dwell-milliseconds="500" place-dwell-milliseconds="500" current-nozzle-tip-id="e092921a-2eef-449b-b340-aa3f40d8d791" changer-enabled="false" limit-rotation="true">
                  <nozzle-tips>
                     <nozzle-tip class="org.openpnp.machine.reference.ReferenceNozzleTip" id="e092921a-2eef-449b-b340-aa3f40d8d791" name="NT1" allow-incompatible-packages="true">
                        <compatible-package-ids class="java.util.HashSet"/>
                        <changer-start-location units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
                        <changer-mid-location units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
                        <changer-end-location units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
                     </nozzle-tip>
                  </nozzle-tips>
                  <head-offsets units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
                  <safe-z value="0.0" units="Millimeters"/>
               </nozzle>
            </nozzles>
            <actuators/>
            <cameras>
               <camera class="org.openpnp.machine.reference.camera.ImageCamera" id="d66faf53-05e1-4629-baae-b614c5ed8320" name="Zoom" looking="Down" settle-time-ms="250" rotation="0.0" flip-x="false" flip-y="false" offset-x="0" offset-y="0" fps="24" width="640" height="480">
                  <units-per-pixel units="Millimeters" x="0.04233" y="0.04233" z="0.0" rotation="0.0"/>
                  <vision-provider class="org.openpnp.machine.reference.vision.OpenCvVisionProvider"/>
                  <head-offsets units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
                  <safe-z value="0.0" units="Millimeters"/>
                  <calibration enabled="false">
                     <camera-matrix length="9">6.08418777596376E-310, 7.63918484747E-313, 6.9520291754075E-310, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0</camera-matrix>
                     <distortion-coefficients length="5">6.95202917515477E-310, 0.0, 0.0, 0.0, 0.0</distortion-coefficients>
                  </calibration>
                  <source-uri>classpath://samples/pnp-test/pnp-test.png</source-uri>
               </camera>
            </cameras>
            <paste-dispensers>
               <paste-dispenser class="org.openpnp.machine.reference.ReferencePasteDispenser" id="53050ccf-59a0-4d9f-a8d3-6216f5412e4e" name="D1">
                  <head-offsets units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
                  <safe-z value="0.0" units="Millimeters"/>
               </paste-dispenser>
            </paste-dispensers>
         </head>
      </heads>
      <feeders>
         <feeder class="org.openpnp.machine.reference.feeder.ReferenceAutoFeeder" id="ec54bc2c-b18d-4088-aca5-dab09f5bf3d0" name="ReferenceAutoFeeder" enabled="true" part-id="R0201-1K" actuator-name="A1" actuator-value="0.0">
            <location units="Millimeters" x="100.0" y="-200.0" z="-20.0" rotation="0.0"/>
         </feeder>
         <feeder class="org.openpnp.machine.reference.feeder.ReferenceAutoFeeder" id="aed8f089-f65d-42e6-baa6-d65bebd567a6" name="ReferenceAutoFeeder" enabled="true" part-id="R0402-1K" actuator-name="A2" actuator-value="0.0">
            <location units="Millimeters" x="110.0" y="-200.0" z="-20.0" rotation="0.0"/>
         </feeder>
         <feeder class="org.openpnp.machine.reference.feeder.ReferenceAutoFeeder" id="de8de97f-fef7-4c5d-a525-e2e4ce257ff8" name="ReferenceAutoFeeder" enabled="true" part-id="R0603-1K" actuator-name="A3" actuator-value="0.0">
            <location units="Millimeters" x="120.0" y="-200.0" z="-20.0" rotation="0.0"/>
         </feeder>
         <feeder class="org.openpnp.machine.reference.feeder.ReferenceAutoFeeder" id="53a97089-1bf8-4632-b91b-3f7f3d46362c" name="ReferenceAutoFeeder" enabled="true" part-id="R0805-1K" actuator-name="A4" actuator-value="0.0">
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
      <job-processors class="java.util.HashMap">
         <job-processor type="PickAndPlace">
            <job-processor class="org.openpnp.machine.reference.ReferenceJobProcessor" demo-mode="false">
               <job-planner class="org.openpnp.planner.SimpleJobPlanner"/>
            </job-processor>
         </job-processor>
      </job-processors>
      <driver class="org.openpnp.machine.reference.driver.GcodeDriver" port-name="/dev/tty.usbmodem1A1211" baud="115200" units="Millimeters" max-feed-rate="15000" connect-wait-time-milliseconds="0">
         <home-location units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
         <command-confirm-regex>^ok.*</command-confirm-regex>
         <connect-command>G21
G90
M82</connect-command>
         <enable-command>M810</enable-command>
         <disable-command>M84
M811</disable-command>
         <home-command>M84
G4P500
G28 X0 Y0
G92 X0 Y0 Z0 E0</home-command>
         <move-to-command>G0 {X:X%.4f} {Y:Y%.4f} {Z:Z%.4f} {Rotation:E%.4f} F{FeedRate:%.0f}
M400</move-to-command>
         <pick-command>M808
M800</pick-command>
         <place-command>M809
M801
M802
G4P250
M803</place-command>
         <sub-drivers class="java.util.ArrayList">
            <reference-driver class="org.openpnp.machine.reference.driver.GcodeDriver" port-name="/dev/tty.usbmodem1A12421" baud="9600" units="Millimeters" max-feed-rate="50000" connect-wait-time-milliseconds="750">
               <home-location units="Millimeters" x="0.0" y="0.0" z="0.0" rotation="0.0"/>
               <command-confirm-regex>^ok.*</command-confirm-regex>
               <actuate-double-command>{Index}</actuate-double-command>
               <sub-drivers class="java.util.ArrayList"/>
            </reference-driver>
         </sub-drivers>
      </driver>
   </machine>
</openpnp-machine>
```

Arduino Sketch Shown in Video
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