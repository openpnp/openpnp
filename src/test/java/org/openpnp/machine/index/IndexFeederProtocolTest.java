package org.openpnp.machine.index;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.openpnp.machine.index.IndexFeederProtocol.getFeederId;
import static org.openpnp.machine.index.IndexFeederProtocol.getVersion;
import static org.openpnp.machine.index.IndexFeederProtocol.initializeFeeder;

public class IndexFeederProtocolTest {
    @Test
    public void testGetFeederID() {
        assertEquals("010101E050", getFeederId(1));
        assertEquals("0201011050", getFeederId(2));
        assertEquals("0301014190", getFeederId(3));
    }
    // TODO Exceptions for <= 0 and >= 255

    @Test
    public void testInitializeFeeder() {
        assertEquals(
                "010D0200112233445566778899AABB020A",
                initializeFeeder(1, "00112233445566778899AABB")
        );
        assertEquals(
                "FE0D02FFEEDDCCBBAA998877665544E8CB",
                initializeFeeder(0xFE, "FFEEDDCCBBAA998877665544")
        );
    }
    // TODO Exceptions for anything that's not a 24 character string
    
    @Test
    public void testGetVersion() {
        assertEquals("0101036191", getVersion(1));
        assertEquals("FE010351A1", getVersion(0xFE));
    }
}
