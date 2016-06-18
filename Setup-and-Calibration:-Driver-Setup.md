# Driver Setup

In OpenPnP, the Driver is the part of the software that interfaces between OpenPnP and a particular type of machine. Typically this is just a small piece of code that translates OpenPnP commands into commands for a particular motion controller such as Smoothie, TinyG, Marlin, etc.

## Choosing a Driver
![screen shot 2016-06-18 at 10 48 23 am](https://cloud.githubusercontent.com/assets/1182323/16172727/d84709b8-3542-11e6-89a3-6890e2f0492e.png)

1. Go to the Machine Setup tab and select the root node of the tree. On most setups it's called "ReferenceMachine". A setup panel will appear on the right.
2. In the setup panel, select the driver that most closely matches you motion controller or machine. Most machines will probably use the [[GcodeDriver]]. Click apply.
  
  **Note: For more information about specific drivers see the Driver Specific Setup section below.**.
3. OpenPnP will prompt you to restart the program, so do that.

## Set Serial Port and Baud rate

Most of the drivers in OpenPnP communicate using the serial port. Before you can connect, you need to set the serial port and baud rate for your controller.

![screen shot 2016-06-18 at 12 28 31 pm](https://cloud.githubusercontent.com/assets/1182323/16173252/30d0f546-3550-11e6-90e0-facf96c4240f.png)

1. After restarting OpenPnP go back to the Machine Setup tab and find the Driver you selected in the tree. It should be near the bottom, under the Driver branch. Select it and a setup panel will appear.
2. Select the serial port and baud rate for your controller and press the Apply button.

## Driver Specific Setup

See the pages below for additional information on setting up specific drivers within OpenPnP.

* Gcode (Recommended): https://github.com/openpnp/openpnp/wiki/GcodeDriver
* TinyG: https://github.com/openpnp/openpnp/wiki/TinyG
* Grbl: https://github.com/openpnp/openpnp/wiki/Grbl

## Connect

Now that the driver is configured, press the green power button <img src="https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/power_button_on.svg" height="18"> to connect to your controller. If all goes well the button will turn red and the rest of the controls will become enabled. If there is an issue OpenPnP will give you an error message.

## Homing

If your machine connected successfully it's time to home it. If you don't have home switches installed you can skip this step. To home the machine click the home button ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/ic_home_black_18px.svg).

## Jogging the Machine
![screen shot 2016-06-18 at 10 33 18 am](https://cloud.githubusercontent.com/assets/1182323/16172512/1cf472b0-3540-11e6-987a-fff822524944.png)

With the machine homed, you can now try jogging the machine to make sure everything is working well. Set the Distance slider to 1mm and click the jog buttons ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/ic_arrow_back_black_18px.svg) ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/ic_arrow_downward_black_18px.svg) ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/ic_arrow_forward_black_18px.svg) ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/ic_arrow_upward_black_18px.svg) ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/ic_rotate_clockwise_black_18px.svg) ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/ic_rotate_counterclockwise_black_18px.svg) to move the head around. Make sure that the machine moves in the directions specified by the buttons. If it doesn't, check your controller configuration.

***

| Previous Step                 | Jump To                 | Next Step                                   |
| ----------------------------- | ----------------------- | ------------------------------------------- |
| [[Before You Start|Setup and Calibration: Before You Start]] | [[Table of Contents|Setup and Calibration]] | [[Top Camera Setup|Setup and Calibration: Top Camera Setup]] |