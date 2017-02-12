Vacuum sensing allows OpenPnP to determine if a pick operation or a place operation has succeeded by checking the vacuum level through a pressure sensor.

OpenPnP currently has rudimentary support for vacuum sensing in the GcodeDriver. This feature will soon be expanded to be available for all drivers. If you are using GcodeDriver you can use it now. If you want to use this feature with other drivers, please follow the issue at https://github.com/openpnp/openpnp/issues/447.

To set up vacuum sensing:

1. Attach a pressure sensor to your machine between the nozzle and the vacuum pump.
2. Configure your machine controller to be able to read the sensor using a command such as an Mcode or Gcode. How to do this is beyond the scope of this document, but you can ask for help on the mailing list.
3. Set a VACUUM_REQUEST_COMMAND in your GcodeDriver config. This command will be sent to your controller when OpenPnP needs to read the vacuum level. See https://github.com/openpnp/openpnp/wiki/GcodeDriver:-Command-Reference#vacuum_request_command for more information.
4. Set a VACUUM_REPORT_REGEX in your GcodeDriver config. This regex will be used to read the response to the VACUUM_REQUEST_COMMAND. See https://github.com/openpnp/openpnp/wiki/GcodeDriver#vacuum_report_regex for more information.
5. Open Machine Setup and select the NozzleTip you are currently using. Near the bottom of the configuration panel you will find Part On Nozzle Vacuum Value and Part Off Nozzle Vacuum Value.

    These are the values that are compared to the vacuum pressure level during pick and place operations. Part On Nozzle Vacuum Value is the minimum vacuum value that OpenPnP will expect to see after a part has been picked. It will read the vacuum value using GcodeDriver and then compare the value. If the value that is read is less than the value you specify an error will be thrown. The Part Off value is the maximum value that is expected to be seen after a place, so after a place the read value is expected to be below this value.

    These two values will be specific to your machine and to each NozzleTip. It may require a bit of experimentation to find values that work for you. Set the two values and press Apply. 
6. With everything fully configured, try performing a Pick operation. You can do this in the Feeders tab. If the pick is good then you should see no error message. If the pick fails you should see an error message. This same error message will appear during a job when there is a pick failure and it will cause the job to pause so you can fix the error.

***

| Previous Step                 | Jump To                 | Next Step                                   |
| ----------------------------- | ----------------------- | ------------------------------------------- |
| [[Feeders|Setup and Calibration: Bottom Vision]] | [[Table of Contents|Setup and Calibration]] | [[Next Steps|Setup and Calibration: Next Steps]] |