# What is it?

Kinematic Solutions are provided by [[Issues and Solutions]] when targeting the **Kinematic** milestone. This page as a whole provides the background information for it. Individual sections are linked from the corresponding solutions.  

The Kinematic Solutions are part of an overall holistic (whole-machine) approach to calibrate both the cameras and various mechanics of the machine by playing the two against each other. On one hand, the known metrics of machine motion are used to calibrate the cameras. On the other hand, the camera is used to make the motion more precise. The inherent chicken and egg situation between the two is circumvented by exploiting various forms of symmetry and by approximation through iteration, things that would be hard or tedious to do by hand. 

To get some impressions, [watch a video of these and other calibration solutions](https://youtu.be/md68n_J7uto). The video is not complete, so come back to this page.

# Capture Soft Limits

For each axis, capture the lower and higher soft limit:

![Capture Soft Limit](https://user-images.githubusercontent.com/9963310/129459436-55511299-6a66-4d49-91e5-2a7d56d2c294.png)

These captures coordinates will set the Soft Limits as described on the [Machine Axes page](https://github.com/openpnp/openpnp/wiki/Machine-Axes#kinematic-settings--axis-limits).

# Dynamic Safe Z
Choose whether you want **Dynamic Safe Z** handling or not: 

![Dynamic Safe Z](https://user-images.githubusercontent.com/9963310/129459347-17eab60a-6ca3-423e-bf26-699f0c0097a8.png)

With **Dynamic Safe Z**, the nozzle Safe Z motion is optimized to be as minimal and fast as possible, by exploiting the fact that smaller parts need to be lifted up less to be at Safe Z, i.e. to clear all obstacles on the machine table. The Safe Z coordinate is adjusted to the **underside** of the part. 

With **Fixed Safe Z**, the nozzle Safe Z motion is always the same, i.e. the nozzle is always conservatively lifted so high that even the tallest part will clear all obstacles on the machine table. Use this setting to make the nozzles **balanced** at Safe Z, if you prefer this style. This is usually a slower but simpler method. Note: For machines with only "binary" nozzle Z positions (e.g. pneumatic) **Fixed Safe Z** _must _be selected!

# Capture Safe Z

According to the choice of [Dynamic Safe Z](#dynamic-safe-z), these solutions let you capture Safe Z for all the nozzles from the machine position. Note, the instructions are different for **Dynamic Safe Z** (shown here) and **Fixed Safe Z**. Follow the instructions carefully.

![Capture Safe Z](https://user-images.githubusercontent.com/9963310/129459402-b644dfd6-2af6-4cb8-a114-9353ae372772.png)

These captured coordinates will set the Safe Z zone as described on the [Machine Axes page](https://github.com/openpnp/openpnp/wiki/Machine-Axes#kinematic-settings--axis-limits).

# Dynamic Safe Z Zone

If [Dynamic Safe Z](#dynamic-safe-z) is enabled, the nozzle needs headroom inside the Safe Z Zone in order to dynamically adjust the Z higher, if a taller part is on the nozzle tip. Issues & Solutions can detect if the headroom i.e. the Safe Z Zone is too small. You must then either increase the **Safe Z Zone** (if possible) or reduce the **Max. Part Height** on the nozzle tip. 

![Safe Z Zone Isssue](https://user-images.githubusercontent.com/9963310/162012365-124e356a-ee21-4c82-8f48-dd528169d811.png)

See either the [Machine Axes page](https://github.com/openpnp/openpnp/wiki/Machine-Axes#kinematic-settings--axis-limits) or [this section about maximum part dimensions, configured on the Nozzle Tip](https://github.com/openpnp/openpnp/wiki/Contact-Probing-Nozzle#part-dimensions).


