# Machine Squaring 

## What is it?

Unless you are the Übermechanic your machine's X and Y axes will not be perfectly squared, i.e. the angle between them will not be a perfect 90°. OpenPnP has a software solution to compensate for that, but before you start calibration the machine you have the (last) chance to try and eliminate (or minimize) the non-squareness mechanically. 

## Step-by-Step Guide

This guide covers mechanical squaring of a machine that has a double belt driven Y axis with coupling drive shaft (e.g. the Liteplacer):
![Liteplacer Frame](https://www.liteplacer.com/wp-content/uploads/2014/02/Liteplacer_v1_2_frame.png)
[(www.liteplacer.com)](https://liteplacer.com/the-machine/assembly-instructions/frame-step-17-finished-frame/)

1. Make sure to remove the non-squareness factor in OpenPNP if you had one.
2. Move the machine to the left X limit, somewhere in the middle of Y.
3. Put a millimeter paper under the camera and align the camera center (crosshairs enabled) to a chosen vertical line precisely (might mark it on the paper).
4. Try to rotate the paper carefully so that the vertical line stays in the camera center precisely while jogging up and down Y in 100mm steps.
5. Fixate the millimeter paper to the desk (e.g. with magnets). 
6. Align the camera center to a chosen horizontal line precisely (might mark it on the paper).
7. Jog as far right as your machine and paper allow.
8. Unless your machine is perfect from scratch, you will now have an offset from the chosen horizontal line. 
9. Keep your motors powered (for holding torque).
10. Make sure your down camera view can be seen from where you stand, while you can access the rear right side Y drive pulley of the machine.
11. Loosen the drive pulley on the drive shaft, while holding everything in place (not easy). 
12. Look at the down camera while slowly adjusting the squareness by gently moving the belt (in the Liteplacer you can nicely roll it over the idler pulley that is mounted behind and above the drive pulley).
13. Be shocked, how easy it is to skew the machine axes! 
14. Align the chosen horizontal line to the camera center as precisely as possible.
15. Tighten the pulley on the drive shaft. 
16. Be careful with the impossibly tiny set screws (Tip: use a high quality key, it matters). 
17. Check everything by jogging a 100mm x 100mm square.
18. Don’t overdo it. You can never be 100% precise. A slight imprecision is better that wearing out the delicate set screws, leaving you with a broken machine, if you don't have a replacement. You can [adjust any remaining non-squareness in OpenPNP](https://github.com/openpnp/openpnp/wiki/GcodeDriver#non-squareness-compensation).  
19. Note how the squareness of the machine depends on these tiny set screws gripping on the polished drive shaft. You might add a screw locker fluid between pulley and shaft (one that can be broken if needed). 

