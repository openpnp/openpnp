ScriptActuators are great when you need to ask the machine to make movements which are not part of the standard OpenPnP build.

Simply put, a ScriptActuator can run code whenever a 'normal' actuator is called - including turning on and off other actuators.  You can use one wherever a regular Actuator is able to be used and it will run the script instead of performing the Actuator action.

As an example, say you want to activate a solenoid or an air cylinder as part of your auto feeder pick process.  You will see that some feeders have drop down boxes for pre- and post- pick actuator actions. If you have set up an actuator to activate the solenoid, then all you need is to place the name of the relevant actuator here.

However, let's say your requirement is a bit more exotic.  You want to move the head in a certain direction, actuate the solenoid, wait some time, then deactivate the solenoid then move back over the pick area.  This can nicely be achieved with a script actuator in place of the regular one.

