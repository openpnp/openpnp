## What is it?

The Nozzle Tip Changer allows OpenPnP to automatically choose the best nozzle tip for a given part. It can optionally Z calibrate the nozzle tip.

The following shows a nozzle tip load and Z probe motion sequence.

![Nozzle Tip Change and Z Calibrate](https://user-images.githubusercontent.com/9963310/113582499-c00f0480-9628-11eb-8e7d-db51b25813cd.gif)


## Four Positions

OpenPnP supports the so-called "Four Positions" changer style. You can configure four motions that load a nozzle tip. To unload a nozzle tip, the four position motions are executed in reverse. You can set the relative speed between the way-points. Furthermore, an actuator can be actuated in between.

![Four Positions](https://user-images.githubusercontent.com/9963310/118364459-c59d2a00-b598-11eb-95bc-a8f77163e620.png)

Note that the _First Location_ is approached from Safe Z above it and the last move is returning from _Last Location_ to Safe Z. 

## Nozzle Tip Z Probing

See the [Nozzle Tip Probing Configuration](https://github.com/openpnp/openpnp/wiki/Contact-Probing-Nozzle#nozzle-tip-probing-configuration) documentation, if you have a ContactProbeNozzle. 

![Z Calibration](https://user-images.githubusercontent.com/9963310/118365257-3b56c500-b59c-11eb-8287-34e7eb78c7dc.png)

## Vision Calibration

Using computer vision Nozzle Tip Changer locations can be calibrated in X and Y. The process uses two template images, of the empty and occupied changer slot respectively. It can therefore also detect if the slot is empty/occupied as expected.

**Vision Location** determines the changer location at which the template images are taken. Chose a location where the nozzle tip is visible when it occupies the slot. 
 
![Vision Location](https://user-images.githubusercontent.com/9963310/113585531-a2dc3500-962c-11eb-8395-f8fe1db1b30b.png)

**Calibration Trigger** determines when the vision calibration takes places:

![Calibration Trigger](https://user-images.githubusercontent.com/9963310/113585861-11b98e00-962d-11eb-9d97-9356a55fbd4f.png)

* **Manual** allows only manual calibration. The calibration will be stored in the configuration (machine.xml)
* **Machine Home** invalidates the calibration when the machine is homed. As soon as nozzle tip load/unload is requested, the slot will first be vision calibrated. 
* **NozzleTipChange** recalibrates the the locations on each nozzle tip change. 

![Vision Calibration](https://user-images.githubusercontent.com/9963310/113588193-1b90c080-9630-11eb-9564-2a81e315c90a.png)

**Template Width** and **Template Height** determine the dimensions of the template image. Use a crop that includes relevant horizontal and vertical edges of the changer slot surrounded by an extra millimeter or so. The nozzle tip must be visible, when it occupies the slot. You can use the **Capture** buttons to optimize.  

**Tolerance** determines by how far from the nominal location the vision detected location may be. 

**Wanted Precision** determines at what distance the obtained template image match location is close enough. Otherwise, the camera is centered to the detected location and another vision pass is made (drill down).

**Max. passes** limits the maximum number of vision passes. 

**Minimum Score** determines how similar the camera image must be in comparison with the template image (0.0 ... 1.0). If the score is not reached, the calibration fails. 

**Last Score** shows the template match score obtained from last live or test calibration. 

Use the **Test** button to test the calibration. 

Use the **Capture** button to capture the respective **Template Empty** or **Template Occupied** image. The camera will automatically move to the selected **Vision Location**. You can hover the mouse over the captured template thumbnail to see it enlarged in the camera view. 

Use the **Reset** button to remove templates images. 

The two images are both compared, and the better match tells OpenPnP if the slot in question is empty or occupied. If this does not correspond to the state internally stored by OpenPnP, it will abort the calibration and therefore the nozzle tip load/unload operation. Nasty collisions can be safely prevented. 

![Found occupied](https://user-images.githubusercontent.com/9963310/113589665-f00ed580-9631-11eb-9522-272dbf86ee64.png)

## Cloning Settings

![Cloning Settings](https://user-images.githubusercontent.com/9963310/113582934-49bed200-9629-11eb-9340-fa6b391f77dd.png)

All the settings can be cloned to/from other nozzle tips, so you need to configure them only once. The various **Locations** are automatically translated according to the **First Location**, so you need only set one location for additional nozzle tip slots. 

* Marking a nozzle tip changer slot as the **Template** makes it the template that others can be cloned from. Use the **Clone Tool Changer Settings to all Nozzle Tips** button to distribute the settings to all the other nozzle tip changer slots. 

* Marking a nozzle tip changer slot as **Clones from Template** lets you clone it from the template. Use the **Clone Tool Changer Settings from Template** button to clone the settings to this nozzle tip changer slots. 

* Marking a nozzle tip changer slot as **Locked** prevents any cloning to/from it. Use it if this slot is different from others. This must be used for the "unloaded" nozzle tip stand-in.



