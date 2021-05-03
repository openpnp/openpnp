package org.openpnp.machine.index;

import com.google.common.io.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Machine;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class IndexPropertiesTest {
    private Machine machine;
    private IndexProperties indexProperties;

    @BeforeEach
    public void setUp() throws Exception {
        File workingDirectory = Files.createTempDir();
        workingDirectory = new File(workingDirectory, ".openpnp");
        System.out.println("Configuration directory: " + workingDirectory);
        Configuration.initialize(workingDirectory);
        Configuration.get().load();

        machine = Configuration.get().getMachine();
        indexProperties = new IndexProperties(machine);
    }

    @Test
    public void getFeederSlotsCausesFeederSlotsToBeSetOnMachine() {
        assertNull(machine.getProperty(IndexProperties.FEEDER_SLOTS_PROPERTY));

        IndexFeederSlots feederSlots = indexProperties.getFeederSlots();

        assertNotNull(feederSlots);
        assertNotNull(machine.getProperty(IndexProperties.FEEDER_SLOTS_PROPERTY));
        assertSame(feederSlots, machine.getProperty(IndexProperties.FEEDER_SLOTS_PROPERTY));
    }
}
