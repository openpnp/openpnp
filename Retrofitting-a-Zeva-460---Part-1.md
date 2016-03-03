This page it's on his very early stages. Please come back on some time.
You will see from description that retrofit that I have realized it's on his early stages but functional. Will follow some other parts that will enhance machine functionality.
I will try to describe it as general as possible to make this page a guideline for similar machine.

1. Get documentation on your machine  
For this I have subscribed to  https://ca.groups.yahoo.com/neo/groups/zevatech/info . In section files you will find documentation for Zeva PM460.

2. Identify motion control system and transmission type/raport  
Zeva uses 5 phase stepper motors for X and Y Axis. Those have 0,72 degrees per step.  
Machine uses transmission belt with a pitch of 3mm HTD.
I have changed the original driver with newer ones that happens to have lower micro-stepping and require supply to 220v.

3. Decide on your motion control  
I have chosen TinyG. There are few shortcomings with it. There are a very limited number of ports output or input. Also a member from Openpnp group told me that tinyg does not report back when finises a movement instruction and therefore dwell commands are not that efficient.

4. Integrate motion control with existing machine electronics  
I am talking here about movement signals, endstop optocouplers and drive signals for penumatic valves.