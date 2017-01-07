If things don't seem to be working right, follow this step by step guide to make sure your job and machine configuration is correct.

The purpose of this guide will be to make sure that your machine is configured correctly in general and that your job and board files are set up right. We'll focus on feeder, board and job coordinates.

Some of the steps in this guide will be easier if you have an up looking camera configured. For more information on that see [[Setup and Calibration: Bottom Camera Setup]]. If you don't have an up looking camera, it's okay.

Please follow the steps in the guide in order, and if the results from one step don't look right then don't continue until it's resolved. If you can't figure out what is wrong, [please ask for help](http://groups.google.com/group/openpnp) and mention which step you are stuck on.

Finally, and most important, follow each step exactly as it's written. It's tempting to skip a step if you've already checked it, but the most common source of errors in setup is changing something small somewhere and then forgetting that you changed it.

## Job Setup
1. If you are using imported data then start with a brand new Job file in OpenPnP. Go to the menu and select File -> New Job. Then use the import menu to re-import your data. We do this to make sure that the job was not changed during debugging.
2. Place a board on the machine table. By default OpenPnP will have the board's side set to Top. If you are working with the board's bottom then set the side to Bottom in the board table.
3. Use the camera to manually align to the board's origin. Typically this is the bottom left corner of the board, when looking at the top, but if you've used a different origin you'll need to find it and move the camera to it. If you are working with the board's bottom, see [[Understanding Board Locations]] to make sure you understand how to set this.
4. Use the capture camera button ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/capture-camera.svg) in the boards table to store the coordinates.
5. Jog away from the position you are in by a few mm. Now click the position camera button ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/position-camera.svg) right next to the capture camera button and make sure the camera goes back to the board origin again.

## Board Setup
1. Now select any placement from the placements table that is on the same side of the board you are looking at. The placement's Side field should match the board's Side field in both tables. If you find you have to change one of these fields now then something has gone wrong in a previous step.
2. In the placements table, click the position camera button ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/position-camera.svg) and the camera should move to the placement. It's okay if it's not perfectly centered, but it shouldn't be more than a millimeter or two off. This step is where things are most likely to go wrong, so here are some things that might happen:
    * If the machine went off in the completely wrong direction, make sure the board side is set correctly and that you have set the board origin correctly based on [[Understanding Board Locations]].
    * If the camera went to basically the right place but is off by several mm, make sure you set the board origin accurately and check [[Setup and Calibration: Steps Per Mm]]. Incorrect steps per mm is the #1 reason for errors with OpenPnP. The ruler check is the single most important test you can do to verify your machine is working right.
    * Make sure the crosshair in the camera view rotates to reflect the orientation of the placement. If the placement's rotation is 0 then the crosshair should be pointing north. If the placement's rotation is 90 then the crosshair should be pointing either east or west, depending on whether you are working with the top or bottom.

## Feeder Setup
1. In the Feeders tab, select a feeder.
2. Click the position camera button ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/position-camera.svg) in the feeders toolbar. The camera should center on a part in the feeder. The crosshair should be pointing in the direction you think of as "0 degrees" for that part.
    * In OpenPnP, part orientation is relative to how you choose to work with your data. It's best to think of it in relation to how you place parts on a board. If you placed a part on a board with 0 degrees of rotation, then a feeder with 0 degrees of rotation should show the part in the same way. 
3. Now click the pick button ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/nozzletip-load.svg) in the feeders toolbar. The machine should perform a feed operation and then pick the part. The part should be on the end of the nozzle.
4. In the jog controls, click the park button ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/park.svg) for the C axis. This should return the nozzle back to 0. Examine the part on the nozzle. It should be at 0 degrees, or close to it. If it's a little off, it's okay.


