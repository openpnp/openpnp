package org.openpnp.machine.photon.protocol;

import java.nio.IntBuffer;

public class PacketBuilder {
    private final IntBuffer payloadBuffer;
    private final Packet packet;

    private PacketBuilder() {
        packet = new Packet();
        payloadBuffer = IntBuffer.allocate(32);
    }

    public static PacketBuilder command(int commandId, int toAddress) {
        return command(toAddress, 0, 0, commandId);
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

    // This should only be used in tests, but it didn't make sense to separate it from PacketBuilder
    public static PacketBuilder response(int toAddress, int fromAddress) {
        PacketBuilder builder = new PacketBuilder();
        Packet packet = builder.packet;
        packet.toAddress = toAddress;
        packet.fromAddress = fromAddress;
        packet.packetId = 0;

        return builder;
    }

    public PacketBuilder putByte(int data) {
        payloadBuffer.put(data & 0xFF);
        return this;
    }

    public PacketBuilder putUint16(int data) {
        payloadBuffer.put((data >> 8) & 0xFF);
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
        packet.payload = new int[payloadBuffer.remaining()];
        payloadBuffer.get(packet.payload);

        packet.calculateCRC();

        return packet;
    }
}
