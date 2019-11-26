# Vacuum Setup

Each nozzle in OpenPnP will usually have an attached solenoid valve and vacuum pump to control vacuum to the nozzle tip. In some configurations with more than one nozzle there is one pump and multiple valves, and in other configurations there is a pump and valve per nozzle. Some configurations turn the pump on and off, and others leave it on all the time. All these configurations are supported.

## Nozzle Valve Setup

Each nozzle will have a valve that controls the vacuum to the nozzle tip. To configure the vacuum valve for a nozzle:

1. [[Create a head mounted actuator|Setup and Calibration: Actuators]] for each nozzle. It should be named something like N1_VAC, N2_VAC, etc.
2. Set the boolean command for the actuator so that it turns the valve on and off when the actuator is triggered.
3. In Machine Setup -> Nozzles, select a nozzle and then select the Vacuum tab.
4. In the Vacuum tab, select the Actuator you created for this nozzle's valve.
5. Repeat steps 2 and 3 for all nozzles.

## Pump Control Setup

OpenPnP can turn the pump on and off whenever it is needed by any nozzle on the head. As long as at least one nozzle is calling for vacuum, the pump will run. To use this functionality:

1. [[Create a head mounted actuator|Setup and Calibration: Actuators]] and set the boolean command to turn the pump on and off.
2. Go to Machine Setup -> Heads and select your Head, then find the Vacuum Pump Actuator field and select the actuator you created for your pump.

***

| Previous Step                 | Jump To                 | Next Step                                   |
| ----------------------------- | ----------------------- | ------------------------------------------- |
| [Actuators](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Actuators) | [Table of Contents](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration) | [Bottom Camera Setup](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Bottom-Camera-Setup) |