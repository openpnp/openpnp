package org.openpnp.machine.photon.protocol;

public class Packet {
    public int toAddress;
    public int fromAddress = 0;
    public int packetId = 0;
    public int payloadLength = 0;
    public int crc = 0;
    public int[] payload = new int[0];

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
