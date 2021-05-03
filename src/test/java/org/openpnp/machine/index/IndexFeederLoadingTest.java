package org.openpnp.machine.index;

import com.google.common.io.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Machine;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This has to be a separate test from IndexFeederTest because in there we load the machine in the setup which precludes
 * us from testing if the index properties are correctly loaded based on the machine.
 *
 * @see IndexFeederTest
 */
public class IndexFeederLoadingTest {

    private IndexFeeder feeder;

    @BeforeEach
    public void setUp() throws Exception {
        File workingDirectory = Files.createTempDir();
        workingDirectory = new File(workingDirectory, ".openpnp");
        System.out.println("Configuration directory: " + workingDirectory);
        Configuration.initialize(workingDirectory);

        feeder = new IndexFeeder();
    }

    @Test
    public void loadingOfIndexProperties() throws Exception {
        assertNull(feeder.indexProperties);

        Configuration.get().load();

        assertSame(Configuration.get().getMachine(), feeder.indexProperties.machine);
    }

    @Test
    public void loadingOfDataActuator() throws Exception {Configuration.get().load();
        Machine machine = Configuration.get().getMachine();

        Actuator actuator = machine.getActuatorByName(IndexFeeder.ACTUATOR_DATA_NAME);
        assertNotNull(actuator);
        assertTrue(actuator instanceof ReferenceActuator);
        assertEquals(IndexFeeder.ACTUATOR_DATA_NAME, actuator.getName());
    }
}
