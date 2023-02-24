package org.openpnp.machine.photon.protocol;

public class Packet {
    int toAddress;
    int fromAddress = 0;
    int packetId = 0;
    int payloadLength = 0;
    int crc = 0;
    int[] payload = new int[0];

    public String toByteString() {
        StringBuilder result = new StringBuilder();
        // Header
        result.append(String.format(
                "%02X%02X%02X%02X%02X",
                toAddress,
                fromAddress,
                packetId,
                payloadLength,
                crc
        ));

        // Payload
        for (int data : payload) {
            result.append(String.format("%02X", data & 0xFF));
        }

        return result.toString();
    }
}
