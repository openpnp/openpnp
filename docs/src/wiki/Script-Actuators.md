## What are ScriptActuators?

ScriptActuators are great when you need to ask the machine to make movements or other actions which are not part of the standard OpenPnP build, but tie them in with the overall motion of the machine e.g. whilst running a job.

Simply put, a ScriptActuator can run code whenever a 'normal' actuator is called - including turning on and off other actuators.  You can use one wherever a regular Actuator is able to be used and it will run the script instead of performing the Actuator action.

As an example, say you want to activate a solenoid or an air cylinder as part of your auto feeder pick process.  You will see that some feeders have drop down boxes for pre- and post- pick actuator actions. If you have set up an actuator to activate the solenoid, then all you need is to place the name of the relevant actuator here.

![](https://user-images.githubusercontent.com/1681591/113157840-249a2000-9233-11eb-9cf7-cc8b2f5803b3.png)

However, let's say your requirement is a bit more exotic.  You want to move the head in a certain direction, actuate the solenoid, wait some time, then deactivate the solenoid then move back over the pick area.  This can nicely be achieved with a script actuator in place of the regular one.

## Step 1 - Create a ScriptActuator

Use the green Add button under Machine Setup > Actuators to create a new ScriptActuator. 

![](https://user-images.githubusercontent.com/1681591/113158356-a12cfe80-9233-11eb-8298-346ca44e5a63.png)

Give it a meaningful name and under 'Script Name' enter the name of the actual script **including the extension** e.g. SouthFeeders.**js** like this 

![](https://user-images.githubusercontent.com/1681591/113158610-da656e80-9233-11eb-86dd-916788a522a7.png)


## Step 2 - Write a Script

Write the script which will perform the action you require and put it in the top scripts directory.  This is the directory that opens when you select scripts > Open Scripts Directory usually `YourUserName>.openpnp2>scripts`.  Do not put it in the `events` sub-directory.

Here is an example script that determines which nozzle is in play, moves the head a certain amount, actuates the solenoid, waits a bit, then deactivates the solenoid and returns to the original position.

```js
// Import some OpenPnP classes we'll use
var imports = new JavaImporter(org.openpnp.model, org.openpnp.util);
with (imports) {

// Moves a distance from the origin, actuates the tape knock, then returns to the origin
print('-------------Running South Feeder Script----------');
var knock = machine.getActuatorByName("Tape Knock");
var nozzle = gui.machineControls.selectedNozzle;
var location = nozzle.location;
print('Origin: ' + location + '  Nozzle: ' + nozzle + '   Actuator: ' + knock);

if (nozzle == 'N1 N1') {
		print('------------- Moving Nozzle 1 ----------');
		// Move to Tape Knock Position
		location = location.add(new Location(LengthUnit.Millimeters, 19, -37, 0, 0));
		nozzle.moveTo(location);

		// Activate tape knock
		knock.actuate(true);
		java.lang.Thread.sleep(600);
		knock.actuate(false);

		//Move back to Origin
		location = location.add(new Location(LengthUnit.Millimeters, -19, 37, 0, 0));
		nozzle.moveTo(location);

 } else if (nozzle == 'N2 N2') {
		print('------------- Moving Nozzle 2 ----------');
		// Move to Tape Knock Position
		location = location.add(new Location(LengthUnit.Millimeters, 47, -37, 0, 0));
		nozzle.moveTo(location);

		// Activate tape knock
		knock.actuate(true);
		java.lang.Thread.sleep(600);
		knock.actuate(false);

		//Move back to Origin
		location = location.add(new Location(LengthUnit.Millimeters, -47, 37, 0, 0));
		nozzle.moveTo(location);
 } else {
 print('Nozzle not recognized');
 }

// Wait a short time for the feeder to physically move forwards
java.lang.Thread.sleep(200);
//
print('-------------Running South Feeder Script Complete ----------');
};
```

## Step 3 - Test the Actuator

You will see that the new script will appear under the Scripts menu like this 

![image](https://user-images.githubusercontent.com/1681591/113160044-059c8d80-9235-11eb-84eb-b7c10f9a120c.png)

Clicking on this item will run the script - **CAREFUL!** _**if your script has errors, it may have unintended consequences**_. OpenPnP won't check SafeZ or anything else at this point, so it's recommended to temporarily remove nozzles if you can to save them getting bent until you are certain the script runs as intended.

If this checks out OK, you can go ahead and run the machine action that will use this actuator and verify that it works as you expect.


Note in the above script that you can easily write to the log from any script.  In addition, you can grab things like which nozzle is in use or the physical location of the nozzle / head and write them out to the log.   This is very helpful in debugging scripts in general and you can always comment these lines out afterwards if they become obtrusive.  