package org.openpnp.machine.index;

import com.google.common.io.Files;
import org.junit.Before;
import org.junit.Test;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Machine;

import java.io.File;

import static org.junit.Assert.*;

public class IndexFeederSlotsTest {
    private IndexFeederSlots feederSlots;

    @Before
    public void setUp() throws Exception {
        File workingDirectory = Files.createTempDir();
        workingDirectory = new File(workingDirectory, ".openpnp");
        System.out.println("Configuration directory: " + workingDirectory);
        Configuration.initialize(workingDirectory);
        Configuration.get().load();

        Machine machine = Configuration.get().getMachine();
        feederSlots = new IndexProperties(machine).getFeederSlots();
    }
    @Test
    public void byDefaultAnUnknownSlotHasNoLocationConfigured() {
        int address = 5;
        IndexFeederSlots.Slot slot = feederSlots.getSlot(address);

        assertEquals(address, slot.getAddress());
        assertNull(slot.getLocation());
    }
}
