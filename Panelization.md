Panelizing is the process of taking a single board design, and replicating in X and/or Y directions. Assembly houses prefer panelized designs because it lets you take a small board with a few parts and turn it into a large panel with lots of parts, thus minimizing the time related to board changeover. 

The image below shows a panelized design. There are a few key features readily visible on this panel. First, the panel is a 3x3 configuration--there are 3 rows of the design and 3 columns of the design. Second, note that lower left left and upper right have fiducials. As with fiducials on a single PCB, fiducials here help the PNP machine orient the board and determine if any rotation is present. In the image below, it's clear that the image is rotated. This was done for testing.

![](https://cloud.githubusercontent.com/assets/24760857/24583526/f4908b70-1701-11e7-8f6c-890540e15435.png)

Note that each board in the panel also have fiducials. If you have fiducials on your panel, in many cases you don't need them on each board. OpenPNP will work with just board fiducials, just panel fiducials, or board and panel fiducials. The final choice will be related to the overall accuracy of your machine. A very accurate machine could get by with just panel fiducials. 

In the panel image above, notice that in the X direction the boards are spaced line-to-line. That is, the right edge of the 0,0 board is butted directly against the left edge of the 1,0 board. This is an X spacing of 0. In the Y direction, there is a small gap because this design uses castellated vias. The Y spacing in this case is 1 mm. 

In this design, the lower left corner of PCB 0,0 is the design origin. This means the lower left panel fiducial will have a negative Y. We'll enter that information below. 

To panelize a design, create a job as you normally would, specifying the expected width and height. With the board selected, you'll see the panelize icon active.

[link to WikiPanelize1.png]

Clicking the panelize design opens a dialog, allowing you specify several parameters. 

[link to WikiPanelize2.png]

In the above image, we've specified the panelized design is a 3x3 matrix, the X and Y spacings discussed above have been entered, and the panel fiducial locations have been entered. We've also specified a part for the fiducial--in this case the fiducial was 1mm dot. When we select OK, the job list is auto-populated with derived boards. The math at this point is straightforward: We can see the first board location of 0,0 and the derived boards are spaced as expected give the X and Y spacing we entered in the dialog. 

[link to WikiPanelize3.png]

Note that at this point, the job window acts differently than if we entered all these boards separately. Specifically, you can make changes to the first board in the list in terms of size, XY location, etc. But the other boards in the list cannot be edited: Their size and locations are all derived from the first board. 

At this point, we could unpanelize the design by clicking on the panelize button again and setting the row and columns both to "1". That would restore the normal behavior of the Job panel.

There is an icon provided in the Job panel to perform a fiducial check on a board. An icon also exists to perform a fiducial check on a panel, shown below.

[link to WikiPanelize4.png]

If the fiducial information was correctly specified in the panel fiducial dialog shown above, then OpenPNP will calculate the location of all the boards in the panel and update them in the Job window. Below, we can see the Job window _after_ the panel fiducial check was run. As expected, the rotation for all panels is the same, and the X and Y locations are updated with some rather cryptic values that takes this rotation (and other parameters) into account. 

[link to WikiPanelize5.png]

When you have a panelized PCB manufactured for production, the fabrication house will ask you if you can accept "X out" boards. This is there way of asking if you can help them out in terms of improving yield. As a PCB panel increases in size, the chances of a defect occurring increases. Boards for production go through electrical testing, and thus a PCB manufacturer will usually know that a single board in a larger panel has a problem. For example, too much copper was etched away in a certain area resulting in an open. Or not enough copper was etched away in a certain area resulting in a short. In these cases, the manufacturer will mark the board in the panel with a big "X", which indicates to the operator that board should not be placed. 

Since X outs are rare, most panels will have all boards in the panel populated. But if an xout is encountered, the location can be specified in OpenPNP using the Panel X-out button

[link to WikiPanelize6.png]
