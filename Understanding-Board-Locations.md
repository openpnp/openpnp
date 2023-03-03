Moved to [[Board Locations]]

# The following is a work in progress...

In order for OpenPnP to successfully place a part on a board, it needs to "know" where the part should be positioned on the board, and, just as importantly, it also needs to "know" where the board is positioned on the machine. That first bit of knowledge comes from the CAD database for the board. OpenPnP provide importers for several popular CAD tools that translate the information from the tool to the proper format and conventions used by OpenPnP.

The figure below depicts a generalized board design one might create with a CAD tool. Note that the board has a coordinate system denoted by arrows pointing in the positive X-axis (red) direction and in the positive Y-axis (cyan) direction. When viewed from the top side of the board, the positive Y-axis is 90 degrees counterclockwise from the positive X-axis.
 
<img width="612" alt="generalizedBoardTopView" src="https://user-images.githubusercontent.com/50550971/222567235-3752310b-5e9e-4e52-b8ee-96fd2dc1fad0.png">
