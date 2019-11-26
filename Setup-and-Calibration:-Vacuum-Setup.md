# Vacuum Setup

Each nozzle in OpenPnP will usually have an attached solenoid valve and vacuum pump to control vacuum to the nozzle tip. In some configurations with more than one nozzle there is one pump and multiple valves, and in other configurations there is a pump and valve per nozzle. Some configurations turn the pump on and off, and others leave it on all the time. All these configurations are supported.

## Nozzle Valve Setup

Each nozzle will have a valve that controls the vacuum to the nozzle tip. To configure the vacuum valve for a nozzle:

1. [[Create an Actuator|Setup and Configuration: Actuators]] for each nozzle, and one additional Actuator if you want to control your pump.
2. In Machine Setup -> Nozzles, select a nozzle and then select the Vacuum tab.
3. In the Vacuum tab, select the Actuator you created for this nozzle's valve.
4. Repeat steps 2 and 3 for all nozzles.
5. If you want your pump to turn on and off when needed, go to Machine Setup -> Heads and select your Head, then find the Vacuum Pump Actuator field and select the actuator you created for your pump.







| Previous Step                 | Jump To                 | Next Step                                   |
| ----------------------------- | ----------------------- | ------------------------------------------- |
| [Actuators](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Actuators) | [Table of Contents](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration) | [Bottom Camera Setup](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Bottom-Camera-Setup) |