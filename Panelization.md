Panelizing is the process of taking a single board design, and replicating it in X and/or Y directions. Assembly houses prefer panelized designs because it lets you take a small board with a few parts and turn it into a large panel with lots of parts, thus minimizing the time related to board changeover. 

## Panelization Limitations
In the current release, it is expected that there is a single board loaded in the job. It's not possible to have several PCB loaded in the job, and panelize just one of the PCB or all of the PCB. You must have a job loaded with a single PCB loaded.

## Panelization Attributes

The image below shows a panelized design. There are a few key features readily visible on this panel. First, the panel is a 3x3 configuration--there are 3 rows of the design and 3 columns of the design. Second, note that lower left left and upper right have fiducials. As with fiducials on a single PCB, fiducials here help the PNP machine orient the board and determine if any rotation is present. In the image below, it's clear that the image is rotated. This was done for testing.

![](https://cloud.githubusercontent.com/assets/24760857/24583526/f4908b70-1701-11e7-8f6c-890540e15435.png)

Note that each board in the panel also has fiducials. If you have fiducials on your panel, in many cases you don't need them on each board. OpenPNP will work with just board fiducials, just panel fiducials, or board and panel fiducials. The final choice will be related to the overall accuracy of your machine. A very accurate machine could get by with just panel fiducials. 

In the panel image above, notice that in the X direction the boards are spaced line-to-line. That is, the right edge of the 0,0 board is butted directly against the left edge of the 1,0 board. This is an X spacing of 0. In the Y direction, there is a small gap because this design uses castellated vias. The Y spacing in this case is 1 mm. 

Below is a drawing highlighting some of the key concepts. 
![](https://user-images.githubusercontent.com/24760857/28502812-766aba98-6faf-11e7-8d72-569b414988d8.PNG)

## Coordinate Systems
There are two coordinate systems to keep mind when panelizing a board. You can think of these as global coordinates and PCB coordinates. This is not a big shift from what is done today. Normally in OpenPNP, you specify each PCB location in global coordinates, and then you specify each part on the PCB in PCB coordinates. Part coordinates are relative to the PCB XY location.

Panelization builds on that. Each of the PCB created by panelizing has a new global coordinate, the the parts for each PCB are located identically. That is C1 and PCB 0, 0 is in the same place as C1 on PCB 3,2.

Where it can get confusing is that panel fiducials are specified relative to the PCB 0,0 lower-left location. 

## Setting the Design Origin 
The lower left corner of PCB 0,0 (the lower-left PCB) is the design origin--**which is NOT NECESSARILY THE LOWER LEFT OF THE panel**. This means the lower left panel fiducial will have a negative Y. We'll enter that information below. This has important implications for your PCB export. **You need to export your PCB such that the lower-left corner of the lower-left PCB is 0,0.** If your design has rails, then some of the those rails (and rail fiducials) will have negative values. 

## Panelizing the Design
To panelize a design, create a job as you normally would, specifying the expected width and height. With the board selected, you'll see the panelize icon become active.

![](https://cloud.githubusercontent.com/assets/24760857/24589463/76e48fa8-178f-11e7-9668-61875589a153.png)

Clicking the panelize design opens a dialog, allowing you specify several parameters. 

![](https://cloud.githubusercontent.com/assets/24760857/24589462/76e110e4-178f-11e7-9085-8078d84d2ee7.png)

In the above image, we've specified the panelized design is a 3x3 array, the X and Y spacings discussed above have been entered, and the panel fiducial locations have been entered. We've also specified a part for the fiducial--in this case the fiducial was 1mm dot. When we select OK, the job list is auto-populated with derived boards. The math at this point is straightforward: We can see the first board location of 0,0 and the derived boards are spaced as expected give the X and Y spacing we entered in the dialog. 

![](https://cloud.githubusercontent.com/assets/24760857/24589460/76d4d0d6-178f-11e7-8112-c5066a2eba87.png)

Note that at this point, the job window acts differently than if we entered all these boards separately. Specifically, you can make changes to the first board in the list in terms of size, XY location, etc. But the other boards in the list cannot be edited: Their size and locations are all derived from the first board. 

Note we could unpanelize the design by clicking on the panelize button again and setting the row and columns both to "1". That would restore the normal behavior of the Job panel.

There is an icon provided in the Job panel to perform a fiducial check on a board. An icon also exists to perform a fiducial check on a panel, shown below.

![](https://cloud.githubusercontent.com/assets/24760857/24589461/76d76ef4-178f-11e7-8756-42ddbb2a25a1.png)

## Fiducials
it is expected that the panel will have two fiducials that are specified in the Panelization dialog. This could be unique fiducials, or they could be any fiducials that are on individual PCBs (located as far apart as possible, ideally). If no fiducials are on the panel or board, then you could leave the fiducial values as zeros and un-tick the "Check Panel Fiducials" checkbox. Doing this would jump straight to the normal board processing.

Very Important: When you specify fiducial locations in the Panelization Settings dialog, you must specify them in panel coordinates. 

If the fiducial information was correctly specified in the panel fiducial dialog shown above, then OpenPNP will calculate the location of all the boards in the panel and update them in the Job window. Below, we can see the Job window _after_ the panel fiducial check was run. As expected, the rotation for all boards in the panel is the same, and the X and Y locations are updated with some rather cryptic values that takes this rotation (and other parameters) into account. 

![](https://cloud.githubusercontent.com/assets/24760857/24589459/76d47ce4-178f-11e7-9e84-810764bc1c1b.png)

## X-Outs
When you have a panelized PCB manufactured for production, the fabrication house will ask you if you can accept "X out" boards. This is their way of asking if you can help them out in terms of improving yield (which, in turn, will mean lower cost for you). As a PCB panel increases in size, the chances of a defect occurring increases. Boards for production go through electrical testing, and thus a PCB manufacturer will usually know that a single board in a larger panel has a problem. For example, too much copper was etched away in a certain area resulting in an open. Or not enough copper was etched away in a certain area resulting in a short. In these cases, the manufacturer will mark the board in the panel with a big "X", which indicates to the operator that board should not be placed due to a fault in that particular board

Since X-outs are rare, most panels will have all boards in the panel populated. But if an X-out is encountered, the location can be specified in OpenPNP using the Panel X-out button

![](https://cloud.githubusercontent.com/assets/24760857/24589458/76d4705a-178f-11e7-8648-3e26e4fc7d99.png)

The button will bring up a dialog allowing you to specify the row and column of the x-out board(s). And upon clicking OK, the corresponding board in the Job list will be disabled. Note that each time you click the Panel X-out button, you start fresh with nothing selected. What you selected last time isn't remembered because each board is different. 

![](https://cloud.githubusercontent.com/assets/24760857/24589457/76d2ae0a-178f-11e7-867d-434e5a1769cb.png)

Upon clicking "OK", the specified board will be disabled for this job run. Note that the X-Out choices and corresponding Enable(s) are not reset after a job run. If you run a panel with an X-out, and the next panel doesn't have an X-out, you will need to specify that the subsequent panel has no X-outs. 

![](https://cloud.githubusercontent.com/assets/24760857/24589456/76d15712-178f-11e7-9214-fd2068b85fef.png)
