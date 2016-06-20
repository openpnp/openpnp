/**
 * Move the default nozzle in a 10mm square.
 * Demonstrates safely submitting machine tasks and using OpenPnP
 * Java types in scripts.
 */
var imports = new JavaImporter(org.openpnp.model, org.openpnp.util);
with (imports) {
	UiUtils['submitUiMachineTask(Thrunnable)'](function() {
		var nozzle = machine.defaultHead.defaultNozzle;
		var location = nozzle.getLocation();

		// Move 10mm right
		location = location.add(new Location(LengthUnit.Millimeters, 10, 0, 0, 0));
		nozzle.moveTo(location);

		// Move 10mm up
		location = location.add(new Location(LengthUnit.Millimeters, 0, 10, 0, 0));
		nozzle.moveTo(location);

		// Move 10mm left
		location = location.add(new Location(LengthUnit.Millimeters, -10, 0, 0, 0));
		nozzle.moveTo(location);

		// Move 10mm down
		location = location.add(new Location(LengthUnit.Millimeters, 0, -10, 0, 0));
		nozzle.moveTo(location);
	});
}
