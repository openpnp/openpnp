package org.openpnp.machine.photon;

import com.google.common.io.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Machine;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class PhotonFeederSlotsTest {
    private PhotonFeederSlots feederSlots;

    @BeforeEach
    public void setUp() throws Exception {
        File workingDirectory = Files.createTempDir();
        workingDirectory = new File(workingDirectory, ".openpnp");
        System.out.println("Configuration directory: " + workingDirectory);
        Configuration.initialize(workingDirectory);
        Configuration.get().load();

        Machine machine = Configuration.get().getMachine();
        feederSlots = new PhotonProperties(machine).getFeederSlots();
    }
    @Test
    public void byDefaultAnUnknownSlotHasNoLocationConfigured() {
        int address = 5;
        PhotonFeederSlots.Slot slot = feederSlots.getSlot(address);

        assertEquals(address, slot.getAddress());
        assertNull(slot.getLocation());
    }
}
