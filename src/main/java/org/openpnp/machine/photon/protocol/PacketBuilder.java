package org.openpnp.machine.photon.protocol;

import java.nio.IntBuffer;

public class PacketBuilder {
    private final IntBuffer dataBuffer;

    private PacketBuilder() {
        dataBuffer = IntBuffer.allocate(32);
    }

    public static PacketBuilder command(int address, int command_id) {
        return new PacketBuilder()
                .putByte(address)
                .putByte(0x00) // For length, will be updated later
                .putByte(command_id);
    }

    public static PacketBuilder response(int feederAddress) {
        return new PacketBuilder()
                .putByte(0x00)
                .putByte(0x00) // For length, will be updated later
                .putByte(feederAddress);
    }

    public PacketBuilder putByte(int data) {
        dataBuffer.put(data & 0xFF);
        return this;
    }

    public PacketBuilder putUuid(String uuid) {
        for (int i = 0; i < 12; i++) {
            int data = PacketHelper.getByteAtPhoton(uuid, i);
            this.putByte(data);
        }

        return this;
    }

    public PacketBuilder putOk() {
        return this.putByte(0x00);
    }

    public PacketBuilder putError(ErrorTypes error) {
        return putByte(error.getId());
    }

    public String toByteString() {
        dataBuffer.put(1, dataBuffer.position() - 2); // Update length

        dataBuffer.flip();
        int[] dataBytes = new int[dataBuffer.remaining()];
        dataBuffer.get(dataBytes);

        int checksum = PacketHelper.crc16(dataBytes);
        StringBuilder result = new StringBuilder();

        for (int data : dataBytes) {
            result.append(String.format("%02X", data & 0xFF));
        }
        result.append(String.format("%02X", checksum & 0xFF));
        result.append(String.format("%02X", (checksum >> 8) & 0xFF));

        return result.toString();
    }
}
