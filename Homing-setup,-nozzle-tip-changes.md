Hi all,

Iam using TinyG and trying to calibrate the machine with Y-coordinates is offset by 1mm/100mm so I tried put in the non-squareness factor of -.01 but it didn't help. Don't know what else to do. 
I followed the manual and even if I put in -0.01 or 0.01 in non-squareness factor but it didn't help much. Is there away to fix it?


Also, I tried to do lights control on the camera using the step provided. However, it didn't work I put in True M110 False M111. Not too sure if they are the right M... to turn the lights on or off. Also is there a way to adjust brightness of the lights down at all as they are quite bright.

For the Homing setting and configuration. I did not understand what I really have to do as the intrucstion was rather describing it than telling users to set it up. The TinyG command says 
M84 ; Disable steppers, resetting the Z axis G4P500 ; Wait half a second for the Z axis to settle G28 X0 Y0 ; Home X and Y G92 X0 Y0 Z0 E0  what should I do for it. 
Also https://github.com/synthetos/TinyG/wiki/Homing-and-Limits-Setup-and-Troubleshooting#soft-limits says differently and I got trouble following it.

With nozzle sensing, does every machine have nozzle sensor(s) that's required setting up. So in githublink https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Nozzle-Setup it suggested to use ContactProbeNozzle. Just wondering if I already setup the ReferenceNozzle then only need to change <nozzle class="org.openpnp.machine.reference.ReferenceNozzle" with
<nozzle class="org.openpnp.machine.reference.ContactProbeNozzle" so that I don't have to redo it right? Also will the  ACTUATE_BOOLEAN_COMMAND  need to be changed as well? Can you please provide the link for it as only  the code for Soothieware is provided.

I am really struggling to follow homing setup instruction. The manual describes the process rather showing what to do. Can you please tell me the steps for setting it up please?


Cheers, Jack 