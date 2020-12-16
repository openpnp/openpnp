package org.openpnp.machine.index.protocol;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class PacketDecoderTest {
    private void assertInvalidResponse(PacketResponse response) {
        assertFalse(response.isValid());
        assertFalse(response.isOk());
        assertNull(response.getError());
        assertNull(response.getUuid());
        assertEquals(0, response.getTargetAddress());
        assertEquals(0, response.getFeederAddress());
    }

    @Test
    public void decodingEmptyString() {
        PacketResponse response = PacketDecoder.decode("").response();

        assertInvalidResponse(response);
    }

    @Test
    public void decodingOddLengthString() {
        PacketResponse response = PacketDecoder.decode("01234").response();

        assertInvalidResponse(response);
    }

    @Test
    public void decodingPacketWithBadChecksum() {
        // Real checksum is 0x84E1, modified here to 0xE080
        PacketResponse response = PacketDecoder.decode("0002FE00E080").response();

        assertInvalidResponse(response);
    }

    @Test
    public void decodingPacketWithBadLength() {
        // Real checksum is 0x8411, but length should be 0x02 instead of 0x01
        PacketResponse response = PacketDecoder.decode("0001FE001184").response();

        assertInvalidResponse(response);
    }
}
