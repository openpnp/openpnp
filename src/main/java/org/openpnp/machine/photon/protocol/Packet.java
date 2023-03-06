package org.openpnp.machine.photon.protocol;

public class Packet {
    public int toAddress = 0;
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

    public void calculateCRC() {
        payloadLength = payload.length;

        CRC8_107 crc8 = new CRC8_107();

        crc8.add(toAddress);
        crc8.add(fromAddress);
        crc8.add(packetId);
        crc8.add(payloadLength);

        for (int dataByte : payload) {
            crc8.add(dataByte);
        }

        crc = crc8.getCRC();
    }

    public String uuid(int startingAtIndex) {
        return hexString(startingAtIndex, 12);
    }

    private String hexString(int startingAtIndex, int length) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < length; i++) {
            builder.append(String.format("%02X", payload[startingAtIndex + i]));
        }

        return builder.toString();
    }
}
