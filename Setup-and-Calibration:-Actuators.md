# Actuators
Actuators are generic devices you can attach to your machine to do additional tasks not covered by Nozzles and Cameras. For instance, you can use a solenoid with a pin attached to the head to drag a tape forward to feed it. You can also use Actuators to read values from sensors attached to the machine.

Some of the things OpenPnP uses Actuators for are:

* [[Nozzle valve and vacuum pump control.|Setup and Calibration: Vacuum Setup]]
* [Auto feeder triggers](https://github.com/openpnp/openpnp/wiki/ReferenceAutoFeeder).
* [[Nozzle vacuum pressure sensing|Setup and Calibration: Vacuum Sensing]].
* Drag feeder solenoid pins.
* [Camera lighting control](https://github.com/openpnp/openpnp/wiki/Camera-Lighting-Control).

Using the [[Scripting]] system you can use Actuators to extend OpenPnP to control a wide array of devices and sensors.

## Adding Actuators
1. Open the Machine Setup tab.
2. 
    * If you are adding a head mounted actuator, find the head in the tree on the left. Under the head look for Actuators and select it. Head mounted actuators are often attached to devices such as drag feed solenoids and nozzle change tools.
    * If you are adding a machine mounted actuator, find Actuators under the root of the tree and select it. Machine mounted actuators can be used for things like conveyors and lighting.
3. Add an actuator by pressing the green plus button ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/general-add.svg).  
2. Select an actuator driver from the provided list and press the "Accept" button. The newly added actuator will show up in the actuators list.
3. Click on the name of the new actuator to open its properties.

## Actuator Settings

### Driver Assignment

This only applies to the ReferenceActuator.

**Driver** selects which driver i.e which connected controller is executing the commands needed to actuate or read the Actuator.

### Actuator Value Type

Each Actuator has a **Value Type** field. 

![Actuator value type](https://user-images.githubusercontent.com/9963310/103421042-9d3d0200-4b9a-11eb-8e88-cdb458fb441e.png)

![Actuator value type help](https://user-images.githubusercontent.com/9963310/103423683-3a526780-4ba8-11eb-8d21-732e9c450866.png)  

The **Value Type** mostly controls how values are edited in the GUI. It does not force the actuator function to actually use that value type. If existing functional actuator assignments in the Machine Setup, or Scripts or G-code use different or even mixed actuator value types, it should still work as before.  The **Value Type** will automatically be proposed when an actuator is first used in a functional assignment. 

For the **Double** or **String** value type, a pair of ON and OFF default values appear:

![Default ON OFF values](https://user-images.githubusercontent.com/9963310/103423861-7a661a00-4ba9-11eb-8c84-e8d01fc2d377.png)

These will be actuated, when Boolean semantics is used to control an actuator e.g. for an actuator that can be set to a specific scalar value (e.g. a LED light intensity), but also can be switched ON/OFF to specific preset ON/OFF values.

### Actuator Machine Coordination

Theses settings determine how actuator actuation/reading is coordinated with machine motion. The options are best explained _in the context of their application_ on the [Motion Planner](https://github.com/openpnp/openpnp/wiki/Motion-Planner#actuator-machine-coordination) page. 

### Actuator Machine States

![Actuator Machine States](https://user-images.githubusercontent.com/9963310/104474778-a37da600-55be-11eb-96ce-22970ce711e7.png)

The **Enabled**, **Homed**, **Disabled** machine state actuation settings can be used to **assume** or **actuate** specific ON/OFF values when these machine states are entered. 

* **LeaveAsIs** leaves the actuator state at its previous state (maybe the unknown state).
* **AssumeUnknown** sets the actuator state to unknown. OpenPnP will not know the state until it has first actuated the actuator.
* **AssumeActuatedOn** or **AssumeActuatedOff** assumes that the machine state has changed the actuator state to ON / OFF respectively (as a definitive side effect of the machine state change or perhaps via custom G-code). 
* **ActuateON** or **ActuateOFF** bring the actuator to a desired defined ON / OFF state when the machine state is entered. 

**NOTE**: **Enabled** and **Homed** actuation happens **after** the new machine state is reached i.e. after all the enabling or homing commands have beed executed. However, the **Disabled** actuation happens **before** the new machine state is reached, i.e. before any disabling commands are executed. 

### Actuator with Profiles

The **Profile** actuator value type is typically used when you want to control multiple other actuators in concert. As an example consider a camera light that can control the Red, Green, Blue channel intensities separately. A set of predefined profiles can be used to control such a multi-channel actuator in a consisent way across the application e.g. in many Pipelines (see the [usage in computer vision i.e. the ImageCapture Pipeline stage](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration:-Camera-Lighting#use-in-the-imagecapture-stage)).  

Once you have selected the **Profile** actuator value type, press **Apply**. A new "Profiles" Wizard tab will appear: 

![Profiles Wizard tabl](https://user-images.githubusercontent.com/9963310/103424296-9ec2f600-4bab-11eb-9253-46b51f9d3648.png)

You can assign up to 6 target actuators to be controlled by the profile. Again press **Apply** for new target actuators to appear as columns in the table. 

New profiles can be created using the ![Plus](https://user-images.githubusercontent.com/9963310/103424193-3ecc4f80-4bab-11eb-9c03-9a3b1b0eea14.png) button, or deleted using the ![Delete](https://user-images.githubusercontent.com/9963310/103424218-57d50080-4bab-11eb-979a-f08fe7dce499.png) button.

A Profile will become active once you assign a **Name**. 

The **Default ON** and **Default OFF** checkboxes control which profile will be selected when the actuator is driven using Boolean semantics. Obviously, the checks are (mutually) exclusive. 

**NOTES:** 

If the target Actuator is itself a **Profile** type actuator, it will be driven as a **String** value (to prevent endless recursion). Using the actuator itself as a target actuator is therefore explicitly supported. Using this technique, you can create self-contained multiple choice actuators. One application example would be a scalar actuator e.g. a light actuator with intensity control (perhaps by PWM). Using a limited set of profile entries you can drive it to specific step values, a bit like the f-stops in photography. Usage becomes more reproducible and values can be centrally maintained. 

![Stepped Profile Actuator](https://user-images.githubusercontent.com/9963310/103426053-e733e100-4bb6-11eb-924b-50ae12f00e6a.png)

Even though a profile target actuator is driven as a String value, you can still effectively drive Double values as well. Just enter them as Strings. It won't make a difference in effective GcodeDriver or HttpActuator encoding, and in ScriptActuators you can convert the values inside the script. 

## Actuator Control Panel

Open the Actuator Control Panel on the Machine Controls/Actuators tab:

![Actuators Tab](https://user-images.githubusercontent.com/9963310/137530471-1f27138a-5fa1-4f8d-b924-b69fbeb63406.png)

The Actuator Control Panels will now display **Value Type** specific GUI controls:

![Double Actuator Dialog](https://user-images.githubusercontent.com/9963310/103423809-088dd080-4ba9-11eb-9f71-bd0f0193ca2c.png) 

![Boolean Actuator Dialog](https://user-images.githubusercontent.com/9963310/103423814-193e4680-4ba9-11eb-9dac-b1d696235c4b.png)  

![Profile Actuator Dialog](https://user-images.githubusercontent.com/9963310/103424395-48a28280-4bac-11eb-8c1e-127aba9e77e8.png)

**NOTE**: if the **Default ON** and **Default OFF** values are defined as explained above, the **On** and **Off** buttons will appear in addition to the typed value field. 

## ReferenceActuator

This applies to the ReferenceActuator, for the other types please see the corresponding sections below.

### Assigning Commands

Once you've created an Actuator, you will generally need to assign commands to it. The most common case is using GcodeDriver/GcodeAsyncDriver and boolean actuators to control something like a switch, a solenoid, a pump, a valve, etc. 

In newer versions of OpenPnP you get support by [[Issues and Solutions]] to interactively set the Gcode commands: 

![Actuator G-code Issues](https://user-images.githubusercontent.com/9963310/149199024-8161894e-b27b-485b-9ec0-4a1faee1a36d.png)

Placeholders ❓ stand for G-code command numbers and regexes that cannot be fully suggested, because they are typically highly machine/controller/configuration specific:

![Placeholder](https://user-images.githubusercontent.com/9963310/149199538-103b2c27-d14e-41a6-903e-82fabc585cb5.png)

Issues & Solutions G-code command and regex support is enabled for Vacuum Valve, Blow-Off, Vacuum Sense, Pump, Z-Probe, Camera Light actuators. 

### Assigning Commands Manually

For manual setup or other actuators, follow the following procedures. 

To set the Gcode for a Boolean Actuator:
1. Go to Machine Setup -> Driver -> GcodeDriver/GcodeAsyncDriver -> Gcode. 
2. Select the Actuator from the dropdown menu.
3. Select the [ACTUATE_BOOLEAN](https://github.com/openpnp/openpnp/wiki/GcodeDriver%3A-Command-Reference#actuate_boolean_command)  command.
4. Enter the Gcode fragment. An example would be `{True:M801}{False:M800}`. This will send M801 when the Actuator is turned on, and M800 when it is turned off. The text after `True:` or `False:` is what will actually be sent.

Here is an example for the vacuum valve:

![Example for cmd assignment](https://user-images.githubusercontent.com/14028021/132058636-7f2a5e4d-8481-466c-8bac-e49522da4bc7.png)

To set the Gcode for a Double Actuator:
1. Go to Machine Setup -> Driver -> GcodeDriver/GcodeAsyncDriver -> Gcode. 
2. Select the Actuator from the dropdown menu.
3. Select the [ACTUATE_DOUBLE](https://github.com/openpnp/openpnp/wiki/GcodeDriver%3A-Command-Reference#actuate_double_command) command.
4. Enter the Gcode fragment. An example would be `M104 {DoubleValue}`. 

### Reading Sensors

This applies to the ReferenceActuator, for the other types please see the corresponding sections below.

Another common use of Actuators is to read a sensor, with [vacuum level sensors](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Vacuum-Sensing) being the most common. To read a sensor using the GcodeDriver/GcodeAsyncDriver the steps are a little different than the above.

To set the Gcode for an Actuator that reads a sensor:
1. Go to Machine Setup -> Driver -> GcodeDriver -> Gcode. 
2. Select the Actuator from the dropdown menu.
3. Select the [ACTUATOR_READ_COMMAND](https://github.com/openpnp/openpnp/wiki/GcodeDriver%3A-Command-Reference#actuator_read_command) command.
4. Enter the Gcode fragment. This should be whatever command you need to send to your controller to cause it to respond with the sensor value.
5. Select the [ACTUATOR_READ_REGEX](https://github.com/openpnp/openpnp/wiki/GcodeDriver#actuator_read_regex).
6. Enter a [regex](https://github.com/openpnp/openpnp/wiki/GcodeDriver#regular-expressions-receiving-responses) that matches the response that will come from the controller.

### GcodeDriver
(and GcodeAsyncDriver)

See [actuate-boolean-command](https://github.com/openpnp/openpnp/wiki/GcodeDriver%3A-Command-Reference#actuate_boolean_command)

See [actuate-double-command](https://github.com/openpnp/openpnp/wiki/GcodeDriver%3A-Command-Reference#actuate_double_command)

See [actuator-read-command](https://github.com/openpnp/openpnp/wiki/GcodeDriver%3A-Command-Reference#actuator_read_command)

See [actuator-read-regex](https://github.com/openpnp/openpnp/wiki/GcodeDriver#actuator_read_regex)

## HttpActuator 

The HttpActuator can control actuators by sending/reading HTTP requests.

If the Actuator has **Boolean** value type, the **On URL** and **Off URL** can be separately defined:

![grafik](https://user-images.githubusercontent.com/9963310/103424708-2f023a80-4bae-11eb-9254-2d409ff4087a.png)

If the Actuator has **Double** or **String** value type, it has a **Parametric URL** field with a placeholder `{val}` that can encode the value into the URL. You can use formatting in the form of e.g. `{val:%.4f}` (like in the GcodeDriver): 

![HttpActuator](https://user-images.githubusercontent.com/9963310/103424584-70461a80-4bad-11eb-8751-5e26b589b8b1.png)

**NOTE:** no URI-escaping is performed on String values, you can therefore enclose complex URI fragments such as multiple parameters at once. 

## Script Actuator

The ScriptActuator can execute a script with the given value as a parameter. The parameter is made available as `actuateBoolean`, `actuateDouble` or `actuateString` global, according to the **Value Type**. 

![ScriptActuator](https://user-images.githubusercontent.com/9963310/113142898-c1f15600-922b-11eb-875c-db80605e860f.png)

For more info on ScriptActuators and an example click [here](https://github.com/openpnp/openpnp/wiki/Script-Actuators)

## Head Offsets
See [Setting Head Offsets](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Nozzle-Setup#head-offsets) for the general process. It is basically the same for Actuators.

***

| Previous Step                 | Jump To                 | Next Step                                   |
| ----------------------------- | ----------------------- | ------------------------------------------- |
| [Nozzle Setup](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Nozzle-Setup)  | [Table of Contents](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration) | [Vacuum Setup](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Vacuum-Setup) |