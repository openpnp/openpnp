# Park Location
The Park Location is the location that your head will move to when commanded to park, and at the end of a job. This should be a safe spot that is out of the way so that when a job is complete you can access the machine to remove your finished board.

# Set Park Location
![screen shot 2016-06-18 at 1 44 57 pm](https://cloud.githubusercontent.com/assets/1182323/16173591/eaf428a8-355a-11e6-9055-e94538de511a.png)

1. Open the Machine Setup tab and find your Head in the tree. It should be called ReferenceHead.
2. Select the head and look for the settings panel on the right.
3. Jog the nozzle to the location you want the machine to park when it's finished a job.
4. Click the capture nozzle location button ![](https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/capture-nozzle.svg) to capture the location.
5. Press Apply to save your changes.

# Test Park Location
1. Jog the nozzle away from the location it was left at in the previous step.
2. Click the X/Y Park button <img src="https://rawgit.com/openpnp/openpnp/develop/src/main/resources/icons/park.svg" height="18"> in the Jog Controls and see that the nozzle moves to the Park location.

# Enable Park When Job Completes
If you'd like the machine to park itself when a job completes, you have to turn that option on.
1. Open Machine Setup -> JobProcessors -> ReferencePnpJobProcessor.
2. Toggle the Park When Complete checkbox.
3. Press Apply.

![screen shot 2017-05-19 at 9 06 03 am](https://cloud.githubusercontent.com/assets/1182323/26251290/6a2b21ac-3c72-11e7-82aa-8b17693537c5.png)


***

| Previous Step                 | Jump To                 | Next Step                                   |
| ----------------------------- | ----------------------- | ------------------------------------------- |
| [[Bottom Camera Setup|Setup and Calibration: Bottom Camera Setup]] | [[Table of Contents|Setup and Calibration]] | [[Discard Location|Setup and Calibration: Discard Location]] |