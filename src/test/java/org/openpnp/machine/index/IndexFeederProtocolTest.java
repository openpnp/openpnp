package org.openpnp.machine.index;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.openpnp.machine.index.IndexFeederProtocol.*;

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

    @Test
    public void testMoveFeedForward() {
        assertEquals("0102047FE338", moveFeedForward(1, 127));
        assertEquals("0502040A23EF", moveFeedForward(5, 10));
    }

    @Test
    public void testMoveFeedBackward() {
        assertEquals("0102057FE2A8", moveFeedBackward(1, 127));
        assertEquals("0502050A227F", moveFeedBackward(5, 10));
    }

    @Test
    public void testGetFeederAddress() {
        assertEquals(
                "FF0D0100112233445566778899AABB7989",
                getFeederAddress("00112233445566778899AABB")
        );
        assertEquals(
                "FF0D01FFEEDDCCBBAA9988776655442C08",
                getFeederAddress("FFEEDDCCBBAA998877665544")
        );
    }
}
