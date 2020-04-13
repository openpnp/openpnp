## ContactProbeNozzle
If you have a machine with a nozzle tat can sense when it makes contact tith the nozzle tip (the Liteplacer has it), you can use the new ContactProbeNozzle.

Instead of using the `ReferenceNozzle`, create a `ContactProbeNozzle`. 
![grafik](https://user-images.githubusercontent.com/9963310/59982466-2fae2e00-9613-11e9-91d2-d7f8034ba55f.png)

If you already have one or more `ReferenceNozzle`s set up, close OpenPNP, edit your machine.xml and replace all 

`<nozzle class="org.openpnp.machine.reference.ReferenceNozzle" 
`
with  
`<nozzle class="org.openpnp.machine.reference.ContactProbeNozzle" 
`

Start OpenPNP again and on the Head add a new probing Actuator for each Nozzle. On the Nozzle, assign the probe:
![grafik](https://user-images.githubusercontent.com/9963310/69479673-33cbbb00-0e00-11ea-9401-dd438b180c86.png)

On your GCode driver choose the new contact probing Actuator and define the ACTUATE_BOOLEAN_COMMAND for probing. 

![grafik](https://user-images.githubusercontent.com/9963310/69479755-4f839100-0e01-11ea-89b7-201993ef22b7.png)

An example (for Smoothieware) could be (Note, we are using M400 followed by M114.2 instead of plain M114 because Smoothieware does not report the rotation axis with M114):
``` 
{True:G38.2 Z-4 F1200   ; Probe down max. 4mm for contact with picked/placed part }
{True:M400          ; Wait until machine has stopped }
{True:M114.2        ; Report current realtime position, as M114 does not report rotation }
{False:G91 G0 Z1 G90; Retract 1mm }
{False:M400         ; Wait until machine has stopped }
{False:M114.2       ; Report current realtime position, as M114 does not report rotation }
```
You need to define the POSITION_REPORT_REGEX to read back the probed Z i.e. to get OpenPNP back in sync with the machine position after probing. 

![grafik](https://user-images.githubusercontent.com/9963310/69497644-d149ec00-0edf-11ea-9af8-089f69e3eaac.png)

For Smoothieware this could be:

`^ok MCS: X:(?<x>-?\d+\.\d+) Y:(?<y>-?\d+\.\d+) Z:(?<z>-?\d+\.\d+) A:(?<rotation>-?\d+\.\d+).*`

To test the contact probing, move your nozzle tip on top of an object not quite touching it. Then go to the machine controls, switch to the Actuators tab and click on the Probe Actuator:
![grafik](https://user-images.githubusercontent.com/9963310/69497687-2dad0b80-0ee0-11ea-85ed-852254c53f4f.png)

You can now use the On and Off buttons to probe and retract. If you are too far above the contact surface, the machine will halt because it did not reach the contact within the 4mm given in the Gcode example above (adapt this value to your machine i.e. the contact probe travel/spring). This is another safety check halting the machine if something is not right (such as the feeder Z position not set right).

You should set your PCB Z and the feeder pick location Z so that the nozzle tip is not quite touching the part. The probing will then allow for variations within the probing distance, so you don't need super precision in your feeders etc. and your setup. 

Note the probed part Z height is not yet used within OpenPNP. A future PR could use the ContactProbingNozzle to calibrate part heights on the fly (first use) to improve precision in bottom vision (perfect focal plane) and in placement. 
