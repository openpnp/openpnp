You can use OpenPnP's [Actuator](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration:-Actuators-and-Other-Head-Objects) system along with [Scripting Events](https://github.com/openpnp/openpnp/wiki/Scripting#scripting-events) to turn lights on and off in response to camera capture events.

1. Add an actuator for the lights you want to control. Select Machine Setup -> ReferenceMachine -> Actuators and then click the green + button to add a new Actuator.
2. Expand the Actuators list and select the new Actuator.
3. Change the Actuator's name to "UpCamLights" and press Apply.
4. Go to Machine Setup -> ReferenceMachine -> Driver -> GcodeDriver -> Gcode.
5. In the Head Mountable dropdown, select UpCamLights.
6. In the Setting dropdown select ACTUATE_BOOLEAN_COMMAND.
7. In the text area, set the Gcode to the commands that will turn your lights on and off. For example, to send `M800` for on and `M801` for off, use: `{True:M800}{False:M801}`. You should change the two commands to whatever works for your machine.
8. Press the Apply button.
9. Restart OpenPnP.
10. Go to Main Window -> Machine Controls -> Actuators and click the UpCamLights button. Your lights should come on. Click it again and they should turn off. If this doesn't work, adjust your Gcode settings until it does, or ask for help.
