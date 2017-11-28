# Discard Location
The Discard Location is where OpenPnP will go to drop parts that it fails to place. This can be as simple as a cup taped to your machine. The machine will discard parts when commanded to, and when a pick or bottom vision fails.

## Set Discard Location
![screen shot 2016-06-18 at 12 29 50 pm](https://cloud.githubusercontent.com/assets/1182323/16173259/60369a20-3550-11e6-958a-af61e601b540.png)

1. Open the Machine Setup tab and select the root node. This is usually called ReferenceMachine.
2. Find the Locations section in the setup screen on the right and locate the Discard Location fields.
3. Jog the nozzle so that it is centered over the area you want to discard parts to.
4. Lower the nozzle down as far as you'd like it to go before dropping a part. You can leave this at zero if your discard bin is high enough, or lower it to make sure the parts will definitely fall in the bin.
5. Click the capture nozzle location button ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/capture-nozzle.svg) to set the X, Y, Z and Rotation fields.
6. Click Apply to save your changes.

## Test Discard Location
![screen shot 2016-06-18 at 12 34 25 pm](https://cloud.githubusercontent.com/assets/1182323/16173276/021ea7a6-3551-11e6-917d-e3ed680d4d46.png)

With the discard location set, we can test that it works properly. We haven't setup a feeder yet, so we won't actually discard a part but by clicking the Discard button in the Special Commands tab we can make sure the nozzle moves to the right place. You should also hear your air solenoids click, if you have them.

***

| Previous Step                 | Jump To                 | Next Step                                   |
| ----------------------------- | ----------------------- | ------------------------------------------- |
| [Park Location](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Park-Location) | [Table of Contents](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration) | [Feeders](https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Feeders) |