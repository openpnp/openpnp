## Pick-and-Place vs 3D Printers and CNC Machines
The hardware design considerations for a pick-and-place (PnP) machine are different from those for a 3D printer of CNC machine. Though you can adapt other designs to work as a PnP if all you need is a bare-bones machine, it's nearly always necessary to diverge significantly from 3D printer and CNC designs to the point that you should not expect to use one as a starting point for your design/build.

### Compared to both 3D printers and CNC machines
* The X and Y axes of PnP machines need accuracy and precision only in final movement coordinates; how they get there is almost irrelevant.
* The Z axis has very limited travel.
* Often there'll be 2, 4, or more Z-axes. Most people find 2 is sufficient (even if they've built a machine with 4 heads!).
* The Z-axis accuracy and precision can be very low (and the axis is often spring-loaded to add more wiggle room).

### Compared to a 3D printer
* The head in a PnP machine is much heavier, which drastically changes the X and Y axis requirements.
* PnP machines can operate much faster as they don't have to wait for molten plastic to flow and harden.
* You *may* not be able to use 3D printed parts for structural or mechanical components of a PnP.

### Compared to a CNC machine
* PnP machines have no cutting forces, allowing for lighter-duty components.
* PnP machines can operate much faster as they don't have to consider chip load on cutters, cutting forces, etc.

### Unique aspects of a PnP machine
* Rapid movements of a heavy head will make the machine move across tables and the floor. Industrial machines have huge masses to prevent this.
* Relatively complicated electronic, mechanical, and pneumatic design. Complexity multiplies (electronic\*mechanical\*pneumatic = complexity cubed!).
* The Z axis has to rotate. For large fine-pitch components, it needs to do so with high accuracy and precision.
* Computer vision is used extensively. This can get around some mechanical issues with machines, but should not be used as a crutch.
* Great open source software. ;)