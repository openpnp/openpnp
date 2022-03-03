## Example Duet3D config.g File for Openpnp
The following is basic working config.g file for the 6HC or Mini 5+. This config allows Openpnp to fully drive calibrate a machine with the following;
* 5 Axis machine; linear XYZ, rotational AB
* Single head, CAM driven dual nozzle with -180+180 rotation
* XYZ Limit switches
* x2 Analog inputs for nozzle pressure
* x1 Digital input for drag pin state detection
* x7 PWM Outputs @20k to drive; x2 LED array, x2pumps, x2 pneumatic valves and x1 drag pin solenoid.

Following the config.g is a practical wiring example using this config on a 6HC to replace the main control board of a Charmhigh 36VA machine.
```
;***Display initial welcome message
;-M291: Display message and optionally wait for response \ https://duet3d.dozuki.com/Wiki/M291
;M291 P"Please go to <a href=""https://www.duet3d.com/StartHere"" target=""_blank"">this</a> page for further instructions on how to set it up." R"Welcome to your new Duet 3!" S1 T0

;***Network Settings Ethernet Version only
;-M552: Set IP address, enable/disable network interface \ https://duet3d.dozuki.com/Wiki/M552
if {network.interfaces[0].type = "ethernet"}
	M552 P192.168.1.14 S1
else
	M552 S1

;***Serial Setup
;-M575: Set serial comms parameters \ https://duet3d.dozuki.com/Wiki/M575
;M575 P1 B57600 S1

;***Name Controller
;-M550: Set Name \ https://duet3d.dozuki.com/Wiki/M550
M550 P"BBE PnP 36V" 	; Set machine name

;***General preferences
;-G90: Set to Absolute Positioning \ https://duet3d.dozuki.com/Wiki/G90
G90		; absolute coordinates
;-M83: Set extruder to relative mode \ https://duet3d.dozuki.com/Wiki/M83
M83		; relative extruder moves
;-G4: Dwell \ https://duet3d.dozuki.com/Wiki/G4
G4 S2		; wait for expansion boards to start

;***Axis/Drives
;-M569: Set motor driver direction, enable polarity, mode and step pulse timing \ https://duet3d.dozuki.com/Wiki/M569
;XY driven by off board CAN expanders, begininng CAN ID 40
M569 P40.0 S1 R1 	; change enable polarity, active = disable drive
M569 P41.0 S1 R1 	; change enable polarity, active = disable drive
;ZUV (ZAB) driven on board
M569 P0 S1      	; Z 
M569 P1 S0	        ; U
M569 P3 S0      	; V P2 skipped 2nd "Winding Ground Fault" reported on that driver for this specific board
;-M584: Set drive mapping \ https://duet3d.dozuki.com/Wiki/M584
M584 X40.0 R0	; X LIN R0 = LINEAR, R1 = ROTATION
M584 Y41.0 R0	; Y LIN R0 = LINEAR, R1 = ROTATION
M584 Z0 R0	; Z LIN
M584 U1 R1 	; A ROT
M584 V3 R1	; B ROT

;***Motor Settings
;-M350: Set microstepping mode \ https://duet3d.dozuki.com/Wiki/M350
M350 Z16 U16 V16 I1					; Configure microstepping with interpolation
;-M92: Set axis steps per unit \ https://duet3d.dozuki.com/Wiki/M92
M92 X40.00 Y40.00 Z8.8880 U8.8888 V8.8888		; Set steps per mm 50mm/rev 
;-M566: Set allowable instantaneous speed change \ https://duet3d.dozuki.com/Wiki/M566
M566 X900.00 Y900.00 Z900.00 U100.0 V100.0		; Set maximum instantaneous speed changes (mm/min)
;-M203: Set maximum feedrate \ https://duet3d.dozuki.com/Wiki/M203
M203 X126000.00 Y126000.00 Z24000.00 U24000.0 V24000.0	; Set maximum speeds (mm/min)
;-M201: Set max acceleration \ https://duet3d.dozuki.com/Wiki/M201
M201 X50000.00 Y50000.00 Z500.00 U5000.0 V5000.0	; Set accelerations (mm/s^2)
;- M906: Set motor currents \ https://duet3d.dozuki.com/Wiki/M906
M906 Z600.00 U500.0 V500.0 I30				; Set motor currents (mA) and motor idle factor in per cent
;-M564: Limit axes \ https://duet3d.dozuki.com/Wiki/M564
M564 H0							; Sets homing, H0 allows mvmnt wo homing
;-M84: Stop idle hold \ https://duet3d.dozuki.com/Wiki/M84
M84 S30							; Set idle timeout

;***Axis Limits
;-M208: Set axis max travel \ https://duet3d.dozuki.com/Wiki/M208
;-Parameters
;-Snnn 0 = set axis maximum (default), 1 = set axis minimum
;-Xnnn X axis limit
;-Ynnn Y axis limit
;-Znnn Z axis limit
M208 X0 Y0 Z-50 U-180 V-180 S1	; Set axis minima
M208 X450 Y500 Z50 U180 V180 S0	; Set axis maxima

;***Endstops
;-M574: Set endstop configuration \ https://duet3d.dozuki.com/Wiki/M574
;-Parameters
;-Xnnn Position of X endstop: 0 = none, 1 = low end, 2 = high end.
;-Ynnn Position of Y endstop: 0 = none, 1 = low end, 2 = high end.
;-Znnn Position of Z endstop: 0 = none, 1 = low end, 2 = high end.
;-E Select extruder endstops to define active high or low (RepRapFirmware 1.16 and earlier only)
;-Snnn Endstop type: 0 = active low endstop input, 1 = active high endstop input, 2 = Z probe, 3 = motor load detection
M574 X1 S1 P"!io0.in"	;X LIM mech NO                    
M574 Y1 S1 P"!io1.in"	;Y LIM mech NO                         
M574 Z1 S1 P"!io2.in"	;Z Opto

;***IO
;-M950: Create heater, fan, spindle or GPIO/servo pin \ https://duet3d.dozuki.com/Wiki/M950
;-Parameters:
;-Hnn Heater number
;-Fnn Fan number
;-Jnn Input pin number (RRF 3.01RC2 and later only)
;-Pnn or Snn Output/servo pin number. Servo pins are just GpOut pins with a different default PWM frequency.
;-Rnn Spindle number (RRF 3.3beta2 and later only)
;-Dn (Duet 3 MB6HC running RRF 3.4 or later only) SD slot number (the only value supported is 1)
;-C"name" Pin name(s) and optional inversion status, see Pin Names. Pin name "nil" frees up the pin. A leading '!' character inverts the input or output. A leading '^' character enables the pullup resistor1. The '^' and '!' characters may be placed in either order.
;-Qnn (optional) PWM frequency in Hz. Valid range: 0-65535, default: 500 for GpOut pins, 250 for fans and heaters
;-T Temperature sensor number, required only when creating a heater. See M308.
;-Lbbbor Laaa:bbb RPM values that are achieved at zero PWM and at maximum RPM. (optional and for spindles only - RRF 3.3beta2 and later)
;***Inputs
M950 J0 C"io3.in"	;Drag Pin Sensor
;***Outputs
M950 P0 C"out0" Q20000	;LED UP Actuator
M950 P1 C"out1" Q20000	;Pump (-) Actuator
M950 P2 C"out2" Q20000	;Pump (+) Actuator
M950 P3 C"out3" Q20000	;Drag pin Actuator
M950 P4 C"out4" Q20000	;LED DN Actuator
M950 P5 C"out5" Q20000	;Valve 1 Actuator
M950 P6 C"out6" Q20000	;Valve 2 Actuator

;***Analog Sensors for Nozzle pressure
;-Common Parameters
;-Sn Sensor number
;-P"pin_name" The name of the control board pin that this sensor uses. For thermistors it is the thermistor input pin name, see Pin Names. For sensors connected to the SPI bus it is the name of the output pin used as the chip select.
;-Y"sensor_type" The sensor and interface type, one of: "thermistor", "pt1000", "rtd-max31865", "thermocouple-max31855", "thermocouple-max31856", "linear-analog", "dht21", "dht22", "dhthumidity", "current-loop-pyro", "drivers", "mcu-temp" (see note below regarding "mcu-temp" support on Duet 3 Mini 5+). Duet WiFi/Ethernet with an attached DueX2 or DueX5 also support "drivers-duex". Firmware 3.2 and earlier also supports "dht11" but this support is likely to be removed in future firmware versions.
;-A"name" Sensor name (optional), displayed in the web interface
M308 S0 P"temp0" Y"linear-analog" A"Pressure_1" F0 B0 C4095
M308 S1 P"temp1" Y"linear-analog" A"Pressure_2" F0 B0 C4095

;***End of config.g

```
## Example Schematic for wiring a 6HC to a CHMT-36VA

![Schematic_36VA_2022-02-14](https://user-images.githubusercontent.com/72060223/153997525-25ea2149-ba17-4b97-9615-c6fbbb237899.png)

## Interfacing The above configured 6HC to Openpnp via GCode
Openpnp is very flexible in that most if not all communication between itself as the motion planner and the hardware driver such the Duet control board is user defined. Like most modern machine control, Duet/RRF uses GCode type command syntax.

The first step is to define the individual axis's and their parameters via the 'Machine Setup' tab.

Second step is to add and configure the "Actuators" within Openpnp. Actuators are essentially the inputs and outputs already configured on the 6HC within the config.g file. These are added as the axis's were via the 'Machine Setup' tab. Once added, actuators behavior is configured via GCode Settings tab.

### Example Actuator Gcode driver example
As an example we will look at the Actuator, 'N1VAC'. N1VAC is a user defined Openpn actuator comprised of two elements, A binary controlled valve to control vacuum to nozzle 1 and vacuum sensor the read the actual vacuum of nozzle 1. Referring to the Duet config.g file we are using P5 for the valve and S0 for the sensor.

```
//Within our config.g file
M950 P5 C"out5" Q20000		;Valve 1
M308 S0 P"temp0" Y"linear-analog" A"Pressure_1" F0 B0 C4095
```
In order for Openpnp to trigger the valve as needed we need to set the GCode ACTUATE_BOOLEAN_COMMAND setting of P5 in syntax the Duet/RRF understands as;
```
M42 P5 S{True:1}{False:0}
```
![image](https://user-images.githubusercontent.com/72060223/156489615-21bc98ec-89e2-4a98-896d-cfdae0af4c91.png)

Now when Openpnp needs to actuate or deactivate the N1VAC valve it will send; 

```
M42 P5 S1 ; for true
M42 P5 S0 ; for false
```
That was for the valve, now we need to setup the Gcode ACTUATOR_READ_COMMAND so Openpnp can send a sensor reading request. Again, the sensor we are addressing is S0 in Duet/RRF so we send the following.
```
M118 P1 S{sensors\u002Eanalog[0]\u002ElastReading} 
//NOTE \u002 is used to Prevent Openpnp from eliminating '.' when using Gcode Compression.
//If Gcode Compression is not set the correct command would be
//M118 P1 S{sensors.analog[0].lastReading} 
```
![image](https://user-images.githubusercontent.com/72060223/156490976-5b8b0e89-85ea-426f-9030-afcf57fa04a9.png)

Now in order for Openpn to understand the expected response we need to set up the correct ACTUATOR_READ_REGEX. Since we are expecting a decimal value the Regex is as;
```
(?<Value>-?\d+\.\d+).*
```
As seen via the Openpnp GUI;

![image](https://user-images.githubusercontent.com/72060223/156491597-f00091c5-fa59-4a2c-b799-e554fa051c2a.png)

This can now be tested within Openpnp Machine Controls tab and accessing the correct actuator button as in the following

![image](https://user-images.githubusercontent.com/72060223/156492072-a353dacb-18c9-4896-818f-e635e49ed83b.png)

If for some reason the valve or sensor does not respond in a predicted manner. Use Openpnp's trace log to determine exactly what the communication issue potentially is as; 

```
2022-03-02 19:48:46.793 GcodeAsyncDriver DEBUG: serial://COM13 commandQueue.offer(M118 P1 S{sensors\u002Eanalog[0]\u002ElastReading}, 5000)...
2022-03-02 19:48:46.794 GcodeAsyncDriver$WriterThread TRACE: [serial://COM13] >> M118P1S{sensors.analog[0].lastReading}
2022-03-02 19:48:46.795 GcodeDriver$ReaderThread TRACE: [serial://COM13] << 2007.8
2022-03-02 19:48:46.795 GcodeDriver$ReaderThread TRACE: [serial://COM13] << ok
2022-03-02 19:48:46.795 GcodeDriver TRACE: actuatorRead response: 2007.8
2022-03-02 19:48:46.795 ReferenceActuator DEBUG: N1VAC.read(): 2007.8
2022-03-02 19:48:46.796 GcodeAsyncDriver DEBUG: serial://COM13 commandQueue.offer(M400 ; Wait for moves to complete before returning, 7143)...
2022-03-02 19:48:46.796 GcodeAsyncDriver DEBUG: serial://COM13 commandQueue.offer(M114 ; get position, -1)...
2022-03-02 19:48:46.796 GcodeAsyncDriver$WriterThread TRACE: [serial://COM13] >> M400; Wait for moves to complete before returning
2022-03-02 19:48:46.796 GcodeAsyncDriver$WriterThread TRACE: [serial://COM13] >> M114; get position
2022-03-02 19:48:46.797 GcodeDriver$ReaderThread TRACE: [serial://COM13] << ok
2022-03-02 19:48:46.798 GcodeDriver$ReaderThread TRACE: [serial://COM13] << X:394.358 Y:54.861 Z:0.000 U:-0.009 V:0.000 D:30.000 E:0.000 Count 15774 2194 0 0 0 2078 Machine 394.358 54.861 0.000 -0.009 0.000 30.000 Bed comp 0.000
2022-03-02 19:48:46.798 GcodeDriver TRACE: Position report: X:394.358 Y:54.861 Z:0.000 U:-0.009 V:0.000 D:30.000 E:0.000 Count 15774 2194 0 0 0 2078 Machine 394.358 54.861 0.000 -0.009 0.000 30.000 Bed comp 0.000
2022-03-02 19:48:46.798 GcodeDriver$ReaderThread TRACE: [serial://COM13] << ok
2022-03-02 19:48:46.798 GcodeDriver TRACE: GcodeAsyncDriver got lastReportedLocation (X:394.358000, Y:54.861000, Z:0.000000, A:-0.009000, B:0.000000)
2022-03-02 19:48:46.798 GcodeAsyncDriver TRACE: GcodeAsyncDriver confirmation complete.

```

Once your first actuator is congfigured within Duet/RRF and added/set and working correctly within Openpnp apply this to the rest of your actuators. Once all Axis's and Actuators are set follow Openpnp's Issues and Solutions guide to finish configuring and tuning your machine.

