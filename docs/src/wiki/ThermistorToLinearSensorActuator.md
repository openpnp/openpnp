ThermistorToLinearSensorActuator is used to linearize readings from sensors connected to thermistor ports, as is common with vacuum sensors and Smoothie.

The Actuator performs a set of inverse transforms to convert the "temperature" reading to the original voltage. It also allows you to specify a linear offset and scale to furthur refine the output.

![Screen Shot 2021-02-04 at 12 28 43 AM](https://user-images.githubusercontent.com/1182323/106854080-5605cd80-6680-11eb-9556-8cd351312214.png)

# Configuration

The Actuator performs four transforms:

## Inverse Steinhart-Hart

Most thermistor controllers perform the Steinhart-Hart function to a value read from an ADC. Steinhart-Hart converts a linear resistance into a non-linear temperature that is based on the curve of a thermistor's temperature response. This Actuator performs the inverse of this to convert back to a linear resistance.

Parameters for the inverse Steinhart-Hart are A, B, and C. These are standard thermistor parameters. If you are using Smoothie you can find these for your selected configuration in [predefined_thermistors.h](https://github.com/Smoothieware/Smoothieware/blob/edge/src/modules/tools/temperaturecontrol/predefined_thermistors.h#L30). c1, c2, and c3 are equivalent to a, b, and c respectively.

## Inverse Voltage Divider

Thermistor circuits often include a voltage divider to put the thermistor output in the range of the ADC. Enter r2 to invert this. r2 can also be found at [predefined_thermistors.h](https://github.com/Smoothieware/Smoothieware/blob/edge/src/modules/tools/temperaturecontrol/predefined_thermistors.h#L30).

## Inverse ADC Conversion

The above two transforms result in a resistance value in ohms. The ADC parameters Maximum Value and Voltage Reference can be used to convert back from resistance to voltage. Maximum Value is the based on the bit width of the ADC. Use the formula `Maximum Value = (2 ^ adc_bit_width - 1)`. Voltage Reference is an attribute of the controller board. Check the schematic.

## Linear Transform

Finally, the Linear Transform can be used to scale and offset the voltage, generally to convert the voltage into the units of the sensor.

# Smoothie Configuration Example

```
temperature_control.zprobe.enable           true
temperature_control.zprobe.thermistor_pin   2.0
temperature_control.zprobe.heater_pin       nc
temperature_control.zprobe.thermistor       EPCOS100K
temperature_control.zprobe.designator       ZP

```

The Actuator defaults to parameters for the EPCOS100K thermistor. If you use that in your Smoothie configuration there is no need to configure Thermistor or ADC in OpenPnP.