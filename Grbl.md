Grbl is a 3 axis CNC controller that can run on an Arduino. In it's default state it is not well suited for pick and place machines because a typical machine has at least 4 axes, but an effort has been made to convert Grbl to 4 axes and several people have had good luck with it.

Modified Grbl, with 4 axes is at https://github.com/openpnp/grbl.
This fork is working but has not been maintained in some time. Volunteers to maintain and pull requests are welcome!
The fork came from the work of Bob Beattie originally. For more information about the fork, see [this thread](https://groups.google.com/forum/?utm_medium=email&utm_source=footer#!msg/openpnp/TytZRlD2_Gw/ruG_9M4u538J).

Original Grbl, with 3 axes is at https://github.com/grbl/grbl.

In OpenPnP you will need to select the `org.openpnp.machine.reference.driver.GrblDriver` driver.

