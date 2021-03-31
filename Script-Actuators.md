ScriptActuators are great when you need to ask the machine to make movements which are not part of the standard OpenPnP build.

Simply put, a ScriptActuator can run code whenever a 'normal' actuator is called - including turning on and off other actuators.  You can use one wherever a regular Actuator is able to be used and it will run the script instead of performing the Actuator action.

As an example, say you want to activate a solenoid or an air cylinder as part of your auto feeder pick process.  You will see that some feeders have drop down boxes for pre- and post- pick actuator actions. If you have set up an actuator to activate the solenoid, then all you need is to place the name of the relevant actuator here.

However, let's say your requirement is a bit more exotic.  You want to move the head in a certain direction, actuate the solenoid, wait some time, then deactivate the solenoid then move back over the pick area.  This can nicely be achieved with a script actuator in place of the regular one.

## Step 1 - Create a ScriptActuator

Use the green Add button under Machine Setup > Actuators to create a new ScriptActuator. Give it a meaningful name and under 'Script Name' enter the name of the actual script **including the extension** e.g. SouthFeeders.**js** 

## Step 2 - Write a Script

Write the script which will perform the action you require and put it in the top scripts directory.  This is the directory that opens when you select scripts > Open Scripts Directory (usually UserName>.openpnp2>scripts).  Do not put it in the 'events' sub-directory.

Here is an example script that determines which nozzle is in play, moves the head a certain amount, actuates the solenoid, waits a bit, then deactivates the solenoid and returns to the original position.


