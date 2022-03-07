## ImageRead Stage

![image](https://user-images.githubusercontent.com/9963310/156917949-f338f79a-fbb9-4f62-bef8-828a4702abdd.png)

Reads an image from a file, as given in the **file** property.

The **colorSpace** indicates how the color information should be handled in the pipeline.

To facilitate testing with pre-captured images, potentially from other users, the ImageRead stage can emulate a camera-captured image: 

**handleAsCaptured**, if enabled, handles the loaded image as if captured by the camera. The image resolution and aspect ratio will be adapted, and if information is present, the image is scaled to camera Units per Pixel. Any pixel coordinates obtained from the image are therefore correctly interpreted, as they would from a camera captured image. The image is also registered as the pipeline captured image.

Images are scaled, cropped and/or extended with black borders, as needed. 

The Units per Pixel information is given in a `upp.txt` file which must reside side-by-side with the image file. The `upp.txt` simply contains two millimeter values, separated by a space. This is the convention, as adopted in the [openpnp-test-images](https://github.com/openpnp/openpnp-test-images) repository.
