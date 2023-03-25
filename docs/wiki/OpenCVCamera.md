# OpenCVCamera

Supports most USB cameras.

- Pro: Low latency and high framerates.
- Con: "USB Device Index" prone to changing if you're not careful about how the cameras are disconnected and connected.

## Configuration

Under the "Camera Specific" tab, "General" section:
- "USB Device Index" - Each camera connected to your computer will have a unique device index starting at index 0. The order you connect these in will determine their index, so be aware of this when disconnecting/reconnecting cameras (including USB hubs that they're connected to). You must do so in the same order, or you will have to change their "USB Device Index".
- "FPS" - Rate at which to grab a new frame from the camera and do any transformations such as applying lens calibration. Set this to the lowest acceptable refresh rate, as excessively high rates will just waste CPU processing time for no reason.
- "Preferred Width" and "Preferred Height" - Size of the frame to request. Set to 0 to use the default resolution for your camera.
