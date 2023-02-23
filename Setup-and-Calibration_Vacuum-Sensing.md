Vacuum sensing allows OpenPnP to determine if a pick operation or a place operation has succeeded by checking the vacuum level through a pressure sensor.

Instructions for OpenPnP 1.0 and 2.0 are different. See further down the page for 1.0 instructions.

# Vacuum sensing in OpenPnP 2.0

## Actuator Setup

Make sure to have [[configured the vacuum valve Actuator|Setup and Calibration_Vacuum Setup]]. The same Actuator can be reused to sense the vacuum, or you can create a second one (for instance if your actuator is on a different driver):

1. Set a ACTUATOR_READ_COMMAND in your GcodeDriver config on the new Actuator. This command will be sent to your controller when OpenPnP needs to read the vacuum level. See [actuator-read-command](https://github.com/openpnp/openpnp/wiki/GcodeDriver_Command-Reference#actuator_read_command) for more information.
2. Set a ACTUATOR_READ_REGEX in your GcodeDriver config on the new Actuator. This regex will be used to read the response to the ACTUATOR_READ_COMMAND. See [actuator-read-regex](https://github.com/openpnp/openpnp/wiki/GcodeDriver#actuator_read_regex) for more information.
3. You can test that the actuator read is working by opening the Actuators tab in Machine Controls, clicking the button for the actuator and clicking the Read button. You should see the value that was read in the text field. 
   ![Test Actuators](https://user-images.githubusercontent.com/9963310/114305655-6f405580-9ad9-11eb-855e-39d4607c3671.png)

## Nozzle Tip Setup

Now that the Actuator works, you need to set up the vacuum sensing Part-on and Part-off criteria. As nozzle tips have different characteristics, this needs to be done per nozzle tip. In the Machine Setup, navigate to the Nozzle Tip Part Detection tab.

### Measurement Method

Choose the Measurement Methods you want to employ:

![grafik](https://user-images.githubusercontent.com/9963310/82137198-283cd800-9816-11ea-93f4-2ad6f2a9b05b.png)

These are available for both the Part-on and the Part-off detection.

**None** switches off the detection altogether (any configuration for another Measurement Method is still kept around). 

**Absolute** measures the absolute vacuum level and compares it to a given range. 

**Difference** in addition to the absolute vacuum level, this method also looks at how the level _changes_. Use this method, if the absolute vacuum level is fluctuating too much and the difference in pressure Part-on vs. Part-off is too small to distinguish from fluctuations. This may be the case if the nozzle tip is very fine and your pump is too strong and/or has a reservoir and there are other influences like cross-talk between multiple nozzles (other parts with varying leak rate on other nozzles), or a hysteresis for the reservoir pressure. By comparing the difference over a short time, these fluctuations can be filtered out. The difference is computed in comparison too a baseline: For the Part-on this is the established vacuum level, before the nozzle is lifted up. For Part-off this is the vacuum level before switching on the valve for a quick probing pulse. You can still use the Absolute range as a an additional sanity check for the baseline. 

Depending on what method you choose, the Wizard shows the necessary fields. 

![grafik](https://user-images.githubusercontent.com/9963310/82137812-87e9b200-981b-11ea-9bce-f399833af402.png)


![grafik](https://user-images.githubusercontent.com/9963310/82138076-ed3ea280-981d-11ea-90a5-ec9f78bb0f05.png)

### Dwell Times & Establish Level

By default OpenPnP will patiently wait for the sum **Pick Dwell Time (ms)** / **Place Dwell Time (ms)** as set both on the Nozzle:

![grafik](https://user-images.githubusercontent.com/9963310/82137895-4c031c80-981c-11ea-8bce-b531fecbc188.png)

And on the Nozzle Tip Configuration tab:

![grafik](https://user-images.githubusercontent.com/9963310/82137900-532a2a80-981c-11ea-861f-94cda054508c.png)

When **Establish Level** (back on the Nozzle Tip Part Detection tab) is enabled, OpenPnP will continuously monitor the vacuum level until a value in the desired range is established. The Dwell times now only serve as timeouts, in case the level cannot be established. The machine will in general not have to wait for the full duration and be faster. In return, you can now choose more conservative timeouts to be on the safe side. 

### When to perform the Checks

Part-off and Part-on Vacuum Sensing have multiple checkbox options for when the check is performed. These should be self-explanatory.

The Before Pick option for the Part-off detection is a bit special in that is happens on the _next_ placement. If you chose this option, be aware that any error will be signaled for the _next_ placement and the _previous_ placement will still remain marked as successful.  

### Valve Open/Close Time

In addition to the common options, the Part-off detection also has the **Valve open/close time (ms)** settings. This creates the probing pulse where the valve quickly switches on and off again to create a vacuum spike in order to probe for a part stuck on the nozzle. The close time can be used to allow for a typical signal delay. Often thermistor inputs and routines on motions-controllers are used to measure the vacuum level. These are often optimized for slow signals both electrically and in software. 

## Diagnostics

As soon as you enable the **Differential** Method or choose the **Establish Level** option, the graphical diagnostics are enabled. OpenPnP now records and draws a graph of the last detection:

![grafik](https://user-images.githubusercontent.com/9963310/82138358-195b2300-9820-11ea-9d80-02796a9a201f.png)

Use your mouse to move over the graph and read off the values. These values as well as the **Last Reading** field can help you set up the proper ranges.


# Vacuum sensing in OpenPnP 1.0

There is a video that shows how to migrate settings from the old system to this system at https://www.youtube.com/watch?v=FsZ5dy7n1Ag. This video also serves as a useful tutorial for the instructions below.

1. Attach a pressure sensor to your machine between the nozzle and the vacuum pump.
2. Configure your machine controller to be able to read the sensor using a command such as an Mcode or Gcode. How to do this is beyond the scope of this document, but you can ask for help on the mailing list.
3. [Create an actuator](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration_Actuators) on the head on the nozzle you want to sense vacuum for. Give it a logical name like "H1VAC".
4. Set a ACTUATOR_READ_COMMAND in your GcodeDriver config on the new Actuator. This command will be sent to your controller when OpenPnP needs to read the vacuum level. See [actuator-read-command](https://github.com/openpnp/openpnp/wiki/GcodeDriver_Command-Reference#actuator_read_command) for more information.
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
| [Bottom Vision](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration_Bottom-Vision) | [Table of Contents](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration) | [Camera Lighting](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration_Camera-Lighting) |