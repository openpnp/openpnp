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

    /**
     * The number 50 was chosen because the initial shipper of hardware running the photon protocol was Opulo, and they
     * are shipping with a harness that can support 50 feeders. This is a tradeoff between scan time and being able to
     * grab all the feeders by default. This number can be increased within reason if need be to an ultimate max of 254.
     */
    @Test
    public void byDefaultTheMaxFeederAddressIs50() {
        assertEquals(50, photonProperties.getMaxFeederAddress());
    }
}
