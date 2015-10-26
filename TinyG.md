OpenPnP installs with default motor control set for demo mode. To use TinyG as your motion controller, follow the instructions at https://github.com/openpnp/openpnp/wiki/User-Manual#the-driver and set your driver to `org.openpnp.machine.reference.driver.TinygDriver`.

If you'd prefer to set the driver manually the machine.xml file will need to be edited. The machine.xml needs to be created if it does not exist, launch OpenPnP and then exit, If OpenPnP cannot find the machine.xml file, it will create the file with defaults. The following line should replace the motor control demo line in the machine.xml file:

`<driver class="org.openpnp.machine.reference.driver.TinygDriver" port-name="COM10" baud="115200" feed-rate-mm-per-minute="5000.0">`

port-name will vary, Windows will assign the next available com port, windows control panel can verify which port was added. Linux will appear similar to "port-name=/dev/ttyS0".

TinyG Default settings should be checked, before using with OpenPnP. In Windows Hyperterminal or Coolterm can be used to verify settings. OpenPnP requires 115200, 8N1, Flow control off. After connecting TinyG to the USB port, communications software settings should be set to 115200, 8N1, CTS. This should allow the PC to send and receive from the TinyG pcb. The first settings to check are flow control, "$ex" will return 0, 1 or 2, OpenPnP requires flow control off, "$ex 0" will set flow control off. TinyG firmware version can be checked using "$fv", current version is 0.970 . 

TinyG has built in test routines, sending "$test=3" will move the X and Y stepper in an attempt to create a square. It is recommended for initial testing that TinyG be connected to steppers and tested for operation before connecting to actual PnP hardware, bench testing is simpler to debug motor issues, and this will prevent damage in case limit switches aren't connected properly.

The TinyG controller should be configured for use with OpenPnP now. Close any software that has been used to test the TinyG so that the comm port will be released. Launch OpenPnP, open the Jog controls, using the X or Y axis buttons the stepper motors should operate for a short time for each jog command issued.



