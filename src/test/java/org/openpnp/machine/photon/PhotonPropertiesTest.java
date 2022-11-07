package org.openpnp.machine.photon;

import com.google.common.io.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Machine;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class PhotonPropertiesTest {
    private Machine machine;
    private PhotonProperties photonProperties;

    @BeforeEach
    public void setUp() throws Exception {
        File workingDirectory = Files.createTempDir();
        workingDirectory = new File(workingDirectory, ".openpnp");
        System.out.println("Configuration directory: " + workingDirectory);
        Configuration.initialize(workingDirectory);
        Configuration.get().load();

        machine = Configuration.get().getMachine();
        photonProperties = new PhotonProperties(machine);
    }

    @Test
    public void getFeederSlotsCausesFeederSlotsToBeSetOnMachine() {
        assertNull(machine.getProperty(PhotonProperties.FEEDER_SLOTS_PROPERTY));

        PhotonFeederSlots feederSlots = photonProperties.getFeederSlots();

        assertNotNull(feederSlots);
        assertNotNull(machine.getProperty(PhotonProperties.FEEDER_SLOTS_PROPERTY));
        assertSame(feederSlots, machine.getProperty(PhotonProperties.FEEDER_SLOTS_PROPERTY));
    }
}
