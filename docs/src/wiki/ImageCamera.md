ImageCamera is a virtual camera that renders it's frames from a static image, rather than from a video stream. It is used in the demo configuration that comes with OpenPnP.

You can change the image that is displayed by following these steps:

1. Export an image of the board from your PCB CAD. .png is best, but most formats will work. Export at high resolution - I think I used 600 DPI.
2. In OpenPnP, go to Machine Settings -> Heads -> Cameras -> ImageCamera -> Device Settings and click Browse to locate the image you exported.
3. Go to Machine Settings -> Heads -> Cameras -> ImageCamera -> General Configuration and update the Units per Pixel X and Y to the DPI of the image you exported using this formula: XY = 1 / DPI * 25.4.
4. Restart OpenPnP.
