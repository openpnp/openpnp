Panelizing is the process of taking a single board design, and replicating in X and/or Y directions. Assembly houses prefer panelized designs because it lets you take a small board with a few parts and turn it into a large panel with lots of parts, thus minimizing the time related to board changeover. 

The image below shows a panelized design. There are a few key features readily visible on this panel. First, the panel is a 3x3 configuration--there are 3 rows of the design and 3 columns of the design. Second, note that lower left left and upper right have fiducials. As with fiducials on a single PCB, fiducials here help the PNP machine orient the board and determine if any rotation is present. In the image below, it's clear that the image is rotated. This was done for testing.

![](https://cloud.githubusercontent.com/assets/24760857/24583526/f4908b70-1701-11e7-8f6c-890540e15435.png)

Note that each board in the panel also have fiducials. If you have fiducials on your panel, in many cases you don't need them on each board. OpenPNP will work with just board fiducials, just panel fiducials, or board and panel fiducials. The final choice will be related to the overall accuracy of your machine. A very accurate machine could get by with just panel fiducials. 

In the panel image above, notice that in the X direction the boards are spaced line-to-line. That is, the right edge of the 0,0 board is butted directly against the left edge of the 1,0 board. This is a X spacing of 0. In the Y direction, there is a small gap because this design uses castellated vias. The Y spacing in this case is 1 mm. 

