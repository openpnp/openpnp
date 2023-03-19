package org.openpnp.machine.photon.protocol;

import java.util.Optional;

public class Packet implements Cloneable {
    public int toAddress = 0;
    public int fromAddress = 0;
    public int packetId = 0;
    public int payloadLength = 0;
    public int crc = 0;
    public int[] payload = new int[0];

    @Override
    public Packet clone() throws CloneNotSupportedException{
        Packet clone = (Packet) super.clone();

        clone.payload = new int[this.payloadLength];
        System.arraycopy(this.payload, 0, clone.payload, 0, this.payloadLength);

        return clone;
    }

    public String toByteString() {
        calculateCRC();

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

    public int uint16(int startingAtIndex) {
        return 256 * payload[startingAtIndex] + payload[startingAtIndex + 1];
    }

    public static Optional<Packet> decode(String packetString) {
        if(packetString.equals("TIMEOUT")) {
            return Optional.empty();
        }

        if(packetString.length() % 2 != 0) {
            return Optional.empty();
        }

        // Minimum 5 bytes: <to address> <from address> <packet id> <length> <crc8>
        if(packetString.length() < 10) {
            return Optional.empty();
        }

        int[] data = new int[packetString.length() / 2];
        for (int i = 0; i < data.length; i++) {
            data[i] = getByteAtPhoton(packetString, i);
        }

        Packet packet = new Packet();
        packet.toAddress = data[0];
        packet.fromAddress = data[1];
        packet.packetId = data[2];
        packet.payloadLength = data[3];
        int expectedCrC = data[4];
        int realPacketLength = data.length - 5;  // Header is 5 bytes

        if(packet.payloadLength != realPacketLength) {
            return Optional.empty();
        }

        packet.payload = new int[packet.payloadLength];

        // Payload starts after 5 byte header
        System.arraycopy(data, 5, packet.payload, 0, packet.payloadLength);

        packet.calculateCRC();
        if(expectedCrC != packet.crc) {
            return Optional.empty();
        }

        return Optional.of(packet);
    }

    protected static int getByteAtPhoton(String s, int index) {
        return (Character.digit(s.charAt(2 * index), 16) << 4) +
                Character.digit(s.charAt(2 * index + 1), 16);
    }
}
