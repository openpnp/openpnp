# OnvifIPCamera

Under the "Camera Specific" tab, "General" section:
- "Camera IP" - Set as &lt;IP address&gt;:&lt;port&gt;. e.g. 192.168.1.193:8899
- "Username" and "Password" - Set to the username and password for your camera. The password is optional.
- "FPS" - Rate at which to grab a new frame from the camera and do any transformations such as applying lens calibration. Set this to the lowest acceptable refresh rate, as excessively high rates will just waste CPU processing time for no reason.
- "Resolution" - Size of the frame to request. Set to &lt;blank&gt; to use the default resolution for your camera. See important note below for HI3516/HI3518 chipset based IP cameras.
- "Target Width" and "Target Height" - Resize the captured image to correct for any aspect ratio issues, such as a 16:9 (widescreen) frame being squashed into a 4:3 (NTSC/PAL) image. Set either to 0 to not resize that dimension.

## IP Camera Chipsets

### HI3516 / HI3518 (and variations)

Used in the majority of budget IP cameras from AliExpress/eBay/etc, or resellers of them.
- "Resolution" - Does not support configuring the resolution of captured frames.

### TI Davinci

Please add any limitations or other notes here.