# Recipe

A machine coordinate system can be reconstructed from three or better more well-known locations, that are sufficiently far away from each other in both X and Y. Before modifying a machine, securely mount and capture multiple fiducials on PCB Z height. These will help reconstruct a machine coordinate system after the machine underwent modification.

The following is a simple recipe using OpenPnP's board fiducial system to help you do the math:


1. **You must do this _before_ you modify the machine!!!**
1. Create a fake (huge) Board in OpenPnP.
   
   ![grafik](https://user-images.githubusercontent.com/9963310/178228834-b2a8f903-a57c-4889-ae87-a19c8895e59a.png)

1. Add the fiducials that you added to your table as Placements.
   
   ![grafik](https://user-images.githubusercontent.com/9963310/178229037-968a46cf-a4ea-48be-9fe3-42b35c84505d.png)

1. Capture their location as good as you can using the eye.
   
   ![grafik](https://user-images.githubusercontent.com/9963310/178231861-169aaefd-7a51-4d1f-83e1-140fb0e15551.png)

1. Run the fiducial check:
   
   ![grafik](https://user-images.githubusercontent.com/9963310/178229166-6e3850de-4f75-40aa-a091-5c9e8070f4fc.png)

1. Now position to each fiducial. It should now be the precision location determined by vision, i.e. the AffineTransform is applied:
   
   ![grafik](https://user-images.githubusercontent.com/9963310/178229248-fa6060f5-3034-41d9-be84-c5188b5c52b0.png)

1. Now copy what you see in your DRO into the fiducials location, manually:
    
   ![grafik](https://user-images.githubusercontent.com/9963310/178229360-ff3cf821-f95a-42ad-b9b2-7a03ab2e9f6f.png)

   **Note**, you can't use the capture button, because it would reverse-apply the AffineTransform, i.e. change nothing.
1. Repeat for each fiducial.
   
1. Once you have these precision fiducial coordinates, make sure to screenshot them, just to be sure to have a technically independent copy:
   
   ![grafik](https://user-images.githubusercontent.com/9963310/178229562-8df9067f-f0c3-41e7-98bf-92f07b8c83f6.png)

1. And save the Job:
   
   ![grafik](https://user-images.githubusercontent.com/9963310/178229599-e9babaf1-b031-418e-b68c-528d15c23ad9.png)

1. **This `.job.xml` file and more importantly the accompanying `.board.xml` file is now the very important record of your OpenPnP machine coordinate system. Go check them out where you saved them.**

1. Now you can safely modify the machine. 
   
1. Afterwards, try to make the coordinates as similar as possible on the controller side (set your homing coordinates, so the coordinates match up roughly, i.e. ±1mm).
1. Load the Job.
1. Perform another fiducial check:
    
   ![grafik](https://user-images.githubusercontent.com/9963310/178229166-6e3850de-4f75-40aa-a091-5c9e8070f4fc.png)
1. Now look in the log for lines like this:
   
   ![grafik](https://user-images.githubusercontent.com/9963310/178230327-e6394140-0106-4e07-9dcb-0de682d9b810.png)

1. Enter the coefficients into your X and Y `ReferenceLinearAxis`: 
   
   ![grafik](https://user-images.githubusercontent.com/9963310/178230526-7f80bfbd-e889-4986-ac8d-2f5eca7dd433.png)
   See also here: 
   https://github.com/openpnp/openpnp/wiki/Linear-Transformed-Axes#rotate-the-machine-table
   
