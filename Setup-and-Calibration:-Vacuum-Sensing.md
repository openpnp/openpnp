Vacuum sensing allows OpenPnP to determine if a pick operation or a place operation has succeeded by checking the vacuum level through a pressure sensor.

There is a video that shows how to migrate settings from the old system to this system at https://www.youtube.com/watch?v=FsZ5dy7n1Ag. This video also serves as a useful tutorial for the instructions below.

To set up vacuum sensing:

1. Attach a pressure sensor to your machine between the nozzle and the vacuum pump.
2. Configure your machine controller to be able to read the sensor using a command such as an Mcode or Gcode. How to do this is beyond the scope of this document, but you can ask for help on the mailing list.
3. [Create an actuator](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Actuators) on the head on the nozzle you want to sense vacuum for. Give it a logical name like "H1VAC".
4. Set a ACTUATOR_READ_COMMAND in your GcodeDriver config on the new Actuator. This command will be sent to your controller when OpenPnP needs to read the vacuum level. See [actuator-read-command](https://github.com/openpnp/openpnp/wiki/GcodeDriver%3A-Command-Reference#actuator_read_command) for more information.
5. Set a ACTUATOR_READ_REGEX in your GcodeDriver config on the new Actuator. This regex will be used to read the response to the ACTUATOR_READ_COMMAND. See [actuator-read-regex](https://github.com/openpnp/openpnp/wiki/GcodeDriver#actuator_read_regex) for more information.
6. You can test that the actuator read is working by opening the Actuators tab in Machine Controls, clicking the button for the new actuator and clicking the Read button. You should see the value that was read in the text field.
7. Open Machine Setup and select the Nozzle you want to sense vacuum for. Near the bottom of the configuration panel find the Vaccuum Sense Actuator Name and set it to the name of the actuator you created.
8. Open Machine Setup and select the NozzleTip you are currently using. Near the bottom of the configuration panel you will find Part On Nozzle Vacuum Value and Part Off Nozzle Vacuum Value.

    These are the values that are compared to the vacuum pressure level during pick and place operations. Part On Nozzle Vacuum Value is the minimum vacuum value that OpenPnP will expect to see after a part has been picked. It will read the vacuum value using GcodeDriver and then compare the value. If the value that is read is less than the value you specify an error will be thrown. The Part Off value is the maximum value that is expected to be seen after a place, so after a place the read value is expected to be below this value.

    These two values will be specific to your machine and to each NozzleTip. It may require a bit of experimentation to find values that work for you. Set the two values and press Apply. 
6. With everything fully configured, try performing a Pick operation. You can do this in the Feeders tab. If the pick is good then you should see no error message. If the pick fails you should see an error message. This same error message will appear during a job when there is a pick failure and it will cause the job to pause so you can fix the error.

***

| Previous Step                 | Jump To                 | Next Step                                   |
| ----------------------------- | ----------------------- | ------------------------------------------- |
| [Bottom Vision](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Bottom-Vision) | [Table of Contents](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration) | [Camera Lighting](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Camera-Lighting) |