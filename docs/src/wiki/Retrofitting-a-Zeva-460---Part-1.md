This page it's on his very early stages. Please come back on some time. Also please bare in mind that English it's not my mother tongue - positive criticism it's welcome.  

First of all I would like to give a big thank you to Jason and guys from group that helped me. Without their help I would not have been reached this point. I would like to thank you as well to Christophe from TinyG forum that has given me for free a pcb of his CncBooster - a Tinyg board driverless (with this board I am running now).  

You will see from description that retrofit that I have realized it's on his early stages but functional. Will follow some other parts that will enhance machine functionality.
I will try to describe it as general as possible to make this page a guideline for similar machine.  


![pm460machine1](https://cloud.githubusercontent.com/assets/670552/13706382/8a84b9ec-e7ad-11e5-8ede-192188d5c24e.jpg)  

### 1. Get documentation on your machine  
For this I have subscribed to  https://ca.groups.yahoo.com/neo/groups/zevatech/info . In section files you will find documentation for Zeva PM460 (and also other Zeva machines).   


### 2. Identify motion control system and transmission type/raport 
Zeva uses 5 phase stepper motors for X and Y Axis. Those motors have 0,72 degrees per step. Since original drives where supposed to require as input a quadrature signal , following suggestion from a fellow that does the same retrofitting I have changed the original driver with newer ones RKD514L-C. The biggest advantages for new stepper drivers are : documentation is available , are accepting step and dir input signals ,  happens to have lower micro-stepping ,  require supply to 220v (so I could convert all machine to 220v mains).  
After a little more research on datasheet of new drivers I have realized that older ones may accept as driving input step & dir as well (it's just a micro-switch setting) so if someone would like to make a fast start (or cheaper), you may leave this as they are.     
On both axis are same number of teeth at pulleys (just on Y axis pulleys are wider but same no of teeth). There is a mechanical reduction on motor and this drives the transmission belt. Small pulley that is on motor shaft has 15 teeth. Big pulley has 36 teeth  and the pulley that drives the effective transmission belt has 20 teeth.   
Making a simple calculation results an equivalent stepper motor with 0.3 degrees per step.
Machine uses transmission belt with a pitch of 3mm HTD + 0.3 degrees per step results a 0.05 mm / step.
Drivers that I have now support large micro-stepping but I have used only 1/8 so results the smaller step of 0.00625
mm.   


### 3. Decide on your motion control  
I have chosen TinyG. There are few shortcomings with it. There are a very limited number of ports output or input. Also a member from Openpnp group told me that tinyg does not report back when finises a movement instruction and therefore dwell commands are not that efficient.  
I have modified a bit the code for TinyG in order to have separate commands for spindle M4-M5, spindle_dir M10-M11 and coolant M8-M9 and I have changed the definition of #define INDICATOR_LED to SPINDLE_PWM_LED - tinyg on start and error toggles that led and in my case was connected to Z axis . Changes are here : [https://github.com/dandumit/TinyG](https://github.com/dandumit/TinyG)  
I still need some outputs for "Tape Knock", component 90 degrees rotation and mechanical centering...   
I plan for next release to move on Smoothie. In the past I have made some pcb's for this and I have developed easy modules for it.  Main advantage of smoothie would be the fact that allows a large number of inputs and outputs.  
For this retrofit are needed a lot of outputs (Example nozzle change , mechanical centering etc).  


### 4. Add vision camera  
First of all you must be aware that lighting (in image recognition domain) it's EXTREMELY important. Please document yourself. HAve a look for example on cognex : [http://www.cognex.com/ExploreLearn/UsefulTools/LightingAdvisor](http://www.cognex.com/ExploreLearn/UsefulTools/LightingAdvisor) and on products examples where you will see that only changing lighting images differ incredible. Choose pc board and see difference between simple standard ring and low angle ring.  
First of all , market endoscopes have leds oriented on viewing direction and (because of this orientation and due to glossy/reflecting soldermask) will generate a lot reflections on generated image - this makes impossible most of image recognition tasks. (to do add configuration for image debugging).  
In this moment OpenPnP support only uplooking camera. Following some advices from liteplacer forum finally I have bought an Andonstar endoscope camera.  Generally has a good quality image and has a nice focus feature , however I am fully satisfied about this : if you move the cable that gets in, image will rotate and get out of focus.  
The same , Andonstar endoscope has a ring-led around objective : in order to avoid a bit reflections, I have placed in front of them a milky plastic. Situation has improved a bit but still recommended a low angle ring.   

Good thing it's that camera comes as a simple replacement for machine existent "laser pointer".
![endoscope and mounting adaptor](https://cloud.githubusercontent.com/assets/670552/13505265/1d5e9446-e181-11e5-8d32-328604c4e129.jpg)   


![nozzle and camera impl](https://cloud.githubusercontent.com/assets/670552/13505269/2492ec58-e181-11e5-980e-b4803dbe5795.jpg)  
 
### 5. Integrate motion control with existing machine electronics  
I am talking here about movement signals, endstop optocouplers and drive signals for penumatic valves.
All pneumatic valves are at 24v. I have used for driving those an ULN2807 connected over board (to do add pictures).
For connecting machine's optocouplers I have connected to existing board with some 1n4148 diodes to avoid to inject 24v in tinyg board.   

### 6. Modified a bit OpenPnp TinyGDriver to support pneumatic movement  
It's a simple new class called TinyGDriverZPneumatic that instead of Z movement sends M10 - M11 to actionate the pneumatic Z.   
Note about Pneumatic Z : Initially I was very sorry that my machine does not have stepper motor on Z. Now I kind of like it.   

Please be aware that even it's a small actuator there are some drawbacks : 
+ hits so hard the surface than all components are shaken. (in my case I have started with a loosen stripfeeder and this actuator was shaken so hard that components where jumping of their sits).
+ pneumatic push period should be considered - there is a thread.sleep instruction that maybe has to be tuned from case to case.
   

To do next : 
* implement original feeders. This seems to be pretty easy. I just have to trigger a pneumatic ventil for "Tape Knock" just that I don't have that output port available.
* implement mechanical centering while movement (this has to be synchronized with movement)
* add a stepper motor for rotation. This is needed since machine originally was supporting only rotation by 90 degrees. I need to rotate at 45 degrees for one of my boards.
* make a new electronic to support a large number of outputs and inputs. I am considering Smoothie since I have some experience on it or Ting2 due to his S curve ramping motors driving.  
* support multiple nozzles/meaning to drive nozzle changer - this requires a new electronic
* add stepper on Z - I am not convinced anymore about this
* add uplooking camera - when OpenPnP will have it
* implement a flying vision? - too futuristic for now - I will try to use mechanical or uplooking camera ..