package org.openpnp.machine.photon.protocol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PhotonCommandsTest {
    protected static String uuid1 = "00112233445566778899AABB";
    protected static String uuid2 = "FFEEDDCCBBAA998877665544";

    protected PhotonCommands commands;

    @BeforeEach
    public void setUp() {
        commands = new PhotonCommands(0);
    }

    @Test
    public void testGetFeederID() {
        assertEquals("010000017001", commands.getFeederId(1));
        assertEquals("02000101BD01", commands.getFeederId(2));
        assertEquals("030002016201", commands.getFeederId(3));
        assertEquals("44000301E801", commands.getFeederId(68));
    }

    @Test
    public void testInitializeFeeder() {
        assertEquals(
                "0100000D7C02" + uuid1,
                commands.initializeFeeder(1, uuid1)
        );
        assertEquals(
                "FE00010D5902" + uuid2,
                commands.initializeFeeder(0xFE, uuid2)
        );
    }

    @Test
    public void testGetVersion() {
        assertEquals("010000017E03", commands.getVersion(1));
        assertEquals("FE0001012C03", commands.getVersion(254));
    }

    @Test
    public void testMoveFeedForward() {
        assertEquals("01000002D1047F", commands.moveFeedForward(1, 127));
        assertEquals("050001022F040A", commands.moveFeedForward(5, 10));
    }

    @Test
    public void testMoveFeedBackward() {
        assertEquals("01000002C4057F", commands.moveFeedBackward(1, 127));
        assertEquals("050001023A050A", commands.moveFeedBackward(5, 10));
    }

    @Test
    public void testGetFeederAddress() {
        assertEquals("FF00000DB6C0" + uuid1, commands.getFeederAddress(uuid1));
        assertEquals("FF00010D72C0" + uuid2, commands.getFeederAddress(uuid2));
    }
}
