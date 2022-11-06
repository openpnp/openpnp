# Vacuum Setup

Each nozzle in OpenPnP will usually have an attached solenoid valve and vacuum pump to control vacuum to the nozzle tip. In some configurations with more than one nozzle there is one pump and multiple valves, and in other configurations there is a pump and valve per nozzle. Some configurations turn the pump on and off, and others leave it on all the time. All these configurations are supported.

## Nozzle Valve Setup

Each nozzle has a valve that controls the vacuum to the nozzle tip. When you use the [Issues & Solutions nozzle solution](https://github.com/openpnp/openpnp/wiki/Issues-and-Solutions#welcome-milestone), the required vacuum actuators will be configured for you (along with nozzles, axes etc.). So you should normally already have the required valve actuators ready. [Issues & Solutions](https://github.com/openpnp/openpnp/wiki/Issues-and-Solutions) will also guide you through the required G-code setup. 

To manually configure the vacuum valve for a nozzle:

1. [[Create a head mounted actuator|Setup and Calibration: Actuators]] for each nozzle. It should be named something like N1_VAC, N2_VAC, etc.
2. [Set the boolean command for the actuator](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Actuators#assigning-commands) so that it turns the valve on and off when the actuator is triggered.
3. In Machine Setup -> Nozzles, select a nozzle and then select the Vacuum tab.
4. In the Vacuum tab, select the Actuator you created for this nozzle's valve.
5. Repeat steps 2 and 3 for all nozzles.

## Pump Control Setup

OpenPnP can turn the pump on and off whenever it is needed by any nozzle on the head. You should already have the PUMP actuator on the machine head. If not, create one as follows:

1. [[Create a head mounted actuator|Setup and Calibration: Actuators]]. 
2. [Set the boolean command](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Actuators#assigning-commands) so that it turns the pump on and off when the actuator is triggered.
3. Go to Machine Setup -> Heads and select your Head, then find the Vacuum Pump Actuator field and select the actuator you created for your pump.

![Vacuum Pump](https://user-images.githubusercontent.com/9963310/181920168-facf1355-300d-405f-8bff-2f2d359a993b.png)

**Pump Control** determines how the pump on/off state is controlled:

![Control Methods](https://user-images.githubusercontent.com/9963310/181920613-9a05ac18-bd80-4afe-b01f-da8d0448002c.png)

- **None** the pump is controlled manually or outside of OpenPnP. Use this for a controller-side hysteresis control, for instance.
- **PartOn**: the pump is switched on when a part is about to be picked, it is switched off as soon as no part is on any nozzle anymore.
- **TaskDuration**: the pump is switched on when a part is about to be picked, it is only switched off when queued tasks (e.g. the running job) is finished, given no part is on any nozzle anymore.
- **KeepRunning**: the pump is switched on when a part is about to be picked, it is kept running until explicitly switched off, or until the machine is being disabled.

**Pump On Wait [ms]** sets the wait time used to give the pump a chance to establish the proper vacuum level.


***

| Previous Step                 | Jump To                 | Next Step                                   |
| ----------------------------- | ----------------------- | ------------------------------------------- |
| [Actuators](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Actuators) | [Table of Contents](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration) | [Bottom Camera Setup](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Bottom-Camera-Setup) |