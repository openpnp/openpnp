## The Machine Setup Tree

Most of the Machine Configuration is contained in the Machine Setup Tree.

![Machine Setup](https://user-images.githubusercontent.com/9963310/105196780-45137300-5b3c-11eb-82a0-bd118b236b3a.png)

## Configuration 

A few basic settings are on the Machine.

**Home after enabled?** automatically homes the machine, after you have enabled it. **CAUTION:** You should only enable this option, when your homing is all set up and working. See the [GcodeDriver `HOME_COMMAND`](https://github.com/openpnp/openpnp/wiki/GcodeDriver%3A-Command-Reference#home_command) and [[Visual Homing]].

**Auto tool select?** automatically selects the current tool in the Machine Controls according to specific user action. For instance when you press one of the ![Position Camera](https://user-images.githubusercontent.com/9963310/105197609-14800900-5b3d-11eb-8ea2-1975b38360c8.png) **Camera Position** buttons, the corresponding camera will be selected in Machine Controls. 

![Machine Controls](https://user-images.githubusercontent.com/9963310/105197874-5d37c200-5b3d-11eb-9dc0-6e69d6adb768.png)

**Motion Planning** is explained on the [[Motion Planner]] page. 

**Discard Location** specifies a location where parts will be discarded when the Job processor determines the part was not picked well (after [Vacuum Sensing](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Vacuum-Sensing) or [Bottom Vision](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Bottom-Vision) errors).


***

| Previous Step                 | Jump To                 | Next Step                                   |
| ----------------------------- | ----------------------- | ------------------------------------------- |
| [Before You Start](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Before-You-Start) | [Table of Contents](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration) | [Driver Setup](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Driver-Setup) |