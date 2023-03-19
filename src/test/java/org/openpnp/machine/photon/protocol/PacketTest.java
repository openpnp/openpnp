package org.openpnp.machine.photon.protocol;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class PacketTest {
    @Test
    public void callingToByteStringAutomaticallySetsPayloadLengthAndCrC() {
        Packet packet = new Packet();

        packet.toAddress = 0x2B;
        packet.fromAddress = 0x13;
        packet.packetId = 0x47;
        packet.payload = new int[]{ 0x03 };

        assertEquals(0, packet.payloadLength);
        assertEquals(0, packet.crc);

        String packetString = packet.toByteString();

        assertEquals("2B1347010A03", packetString);

        assertEquals(0x01, packet.payloadLength);
        assertEquals(0x0A, packet.crc);
    }

    @Test
    public void decodingValidPacket() {
        Optional<Packet> packetOptional = Packet.decode("2B1347010A03");

        assertTrue(packetOptional.isPresent());

        Packet packet = packetOptional.get();

        assertEquals(0x2B, packet.toAddress);
        assertEquals(0x13, packet.fromAddress);
        assertEquals(0x47, packet.packetId);
        assertEquals(0x01, packet.payloadLength);
        assertEquals(0x0A, packet.crc);
        assertArrayEquals(new int[]{ 0x03 }, packet.payload);
    }

    @Test
    public void decodingEmptyString() {
        Optional<Packet> packetOptional = Packet.decode("");

        assertFalse(packetOptional.isPresent());
    }

    @Test
    public void decodingOddLengthString() {
        Optional<Packet> packetOptional = Packet.decode("01234");

        assertFalse(packetOptional.isPresent());
    }

    @Test
    public void decodingPacketWithBadChecksum() {
        // Real checksum is 0xC7, modified here to 0xC9
        Optional<Packet> packetOptional = Packet.decode("2B000001C903");

        assertFalse(packetOptional.isPresent());
    }

    @Test
    public void decodingPacketWithTooShortLength() {
        // Length should be 0x01 instead of 0x00. Checksum is valid for incorrect length
        Optional<Packet> packetOptional = Packet.decode("2B000000D203");

        assertFalse(packetOptional.isPresent());
    }

    @Test
    public void decodingPacketWithTooLongLength() {
        // Length should be 0x01 instead of 0x02. Checksum is valid for incorrect length
        Optional<Packet> packetOptional = Packet.decode("2B000002F803");

        assertFalse(packetOptional.isPresent());
    }

    @Test
    public void decodingTimeout() {
        Optional<Packet> packetOptional = Packet.decode("TIMEOUT");

        assertFalse(packetOptional.isPresent());
    }
}
