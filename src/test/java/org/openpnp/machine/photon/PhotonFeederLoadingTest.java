package org.openpnp.machine.photon;

import com.google.common.io.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.driver.GcodeDriver;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Driver;
import org.openpnp.spi.Machine;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This has to be a separate test from PhotonFeederTest because in there we load the machine in the setup which precludes
 * us from testing if the photon properties are correctly loaded based on the machine.
 *
 * @see PhotonFeederTest
 */
public class PhotonFeederLoadingTest {

    private PhotonFeeder feeder;

    @BeforeEach
    public void setUp() throws Exception {
        File workingDirectory = Files.createTempDir();
        workingDirectory = new File(workingDirectory, ".openpnp");
        System.out.println("Configuration directory: " + workingDirectory);
        Configuration.initialize(workingDirectory);

        feeder = new PhotonFeeder();
    }

    @Test
    public void loadingOfPhotonProperties() throws Exception {
        assertNull(feeder.photonProperties);

        Configuration.get().load();

        assertSame(Configuration.get().getMachine(), feeder.photonProperties.machine);
    }

    @Test
    public void loadingOfDataActuator() throws Exception {
        Configuration.get().load();
        Machine machine = Configuration.get().getMachine();

        Actuator actuator = machine.getActuatorByName(PhotonFeeder.ACTUATOR_DATA_NAME);
        assertNotNull(actuator);
        assertTrue(actuator instanceof ReferenceActuator);
        assertEquals(PhotonFeeder.ACTUATOR_DATA_NAME, actuator.getName());
    }

    @Test
    public void loadingOfDataActuatorFillsInGcodeForGcodeDrivers() throws Exception {
        Configuration.get().load();
        Machine machine = Configuration.get().getMachine();

        // First remove the Reference Actuator that was made when we loaded the configuration
        machine.removeActuator(machine.getActuatorByName(PhotonFeeder.ACTUATOR_DATA_NAME));

        GcodeDriver gcodeDriver = new GcodeDriver();
        machine.addDriver(gcodeDriver);

        feeder = new PhotonFeeder();  // New feeder forces Actuator creation

        Actuator actuator = machine.getActuatorByName(PhotonFeeder.ACTUATOR_DATA_NAME);
        assertEquals("M485 {value}", gcodeDriver.getCommand(actuator, GcodeDriver.CommandType.ACTUATOR_READ_COMMAND));
        assertEquals("rs485-reply: (?<Value>.*)", gcodeDriver.getCommand(actuator, GcodeDriver.CommandType.ACTUATOR_READ_REGEX));
    }
}
