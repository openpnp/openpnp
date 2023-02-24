package org.openpnp.machine.photon.protocol;

import java.nio.IntBuffer;

public class PacketBuilder {
    private final IntBuffer payloadBuffer;
    private final Packet packet;

    private PacketBuilder() {
        packet = new Packet();
        payloadBuffer = IntBuffer.allocate(32);
    }

    public static PacketBuilder command(int toAddress, int fromAddress, int packetId, int command_id) {
        PacketBuilder builder = new PacketBuilder();
        Packet packet = builder.packet;
        packet.toAddress = toAddress;
        packet.fromAddress = fromAddress;
        packet.packetId = packetId;

        builder.putByte(command_id);

        return builder;
    }

    public static PacketBuilder response(int feederAddress) {
        return new PacketBuilder()
                .putByte(0x00)
                .putByte(0x00) // For length, will be updated later
                .putByte(feederAddress);
    }

    public PacketBuilder putByte(int data) {
        payloadBuffer.put(data & 0xFF);
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

    public Packet toPacket() {
        // Flip resets the buffer so that we're at the first byte and can grab the new "remaining" bytes and put them
        // into our dataBytes.

        payloadBuffer.flip();
        int[] dataBytes = new int[payloadBuffer.remaining()];
        payloadBuffer.get(dataBytes);
        packet.payloadLength = dataBytes.length;
        packet.payload = dataBytes;

        CRC8_107 crc8 = new CRC8_107();

        crc8.add(packet.toAddress);
        crc8.add(packet.fromAddress);
        crc8.add(packet.packetId);
        crc8.add(packet.payloadLength);

        for (int dataByte : dataBytes) {
            crc8.add(dataByte);
        }

        packet.crc = crc8.getCRC();

        return packet;
    }
}
