/**
 * Places a part if its holding one at current location.
 */

// Load Examples/Utility.js functions into our scope. This will define the
// task function.
load(scripting.getScriptsDirectory().toString() + '../Examples/JavaScript/Utility.js');

// Import some OpenPnP classes we'll use
var imports = new JavaImporter(org.openpnp.model, org.openpnp.util);

// Using the imports from above, do some work.
with (imports)
{
	task(function()
	{
		if(machine.defaultHead.isCarryingPart())
		{
			machine.defaultHead.defaultNozzle.place();
		}
		else
		{
		    print("No part to place.");
		}
	});
}
