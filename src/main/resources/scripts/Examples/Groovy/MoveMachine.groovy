package scripts.Examples.Groovy

import org.openpnp.model.LengthUnit
import org.openpnp.model.Location
import org.openpnp.spi.Machine

// submitUiMachineTask should be used for all code that interacts
// with the machine. It guarantees that operations happen in the
// correct order, and that the user is presented with a dialog
// if there is an error.
import static org.openpnp.util.UiUtils.submitUiMachineTask

def main() {
    submitUiMachineTask(this::moveNozzle)
}

def moveNozzle() {
    // Use explicit type casting from 'machine' to Machine to help with IDE code completion.
    def nozzle = ((Machine)machine).defaultHead.defaultNozzle
    def location = nozzle.location
    printLocation(location)

    // Move 10mm right
    location = location.add(new Location(LengthUnit.Millimeters, 10, 0, 0, 0))
    printLocation(location)
    nozzle.moveTo(location)

    // Move 10mm up
    location = location.add(new Location(LengthUnit.Millimeters, 0, 10, 0, 0))
    printLocation(location)
    nozzle.moveTo(location)

    // Move 10mm left
    location = location.add(new Location(LengthUnit.Millimeters, -10, 0, 0, 0))
    printLocation(location)
    nozzle.moveTo(location)

    // Move 10mm down
    location = location.add(new Location(LengthUnit.Millimeters, 0, -10, 0, 0))
    printLocation(location)
    nozzle.moveTo(location)
}

def printLocation(Location location) {
    println("Location: ${location}")
}

main()