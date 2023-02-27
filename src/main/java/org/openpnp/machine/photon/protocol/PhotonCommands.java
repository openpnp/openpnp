package org.openpnp.machine.photon.protocol;

public class PhotonCommands {
    int packetId = 0;
    int fromAddress;

    public PhotonCommands(int fromAddress) {
        this.fromAddress = fromAddress;
    }

    public void setFromAddress(int fromAddress) {
        this.fromAddress = fromAddress;
    }

    private int nextPacketId() {
        int currentPacketId = packetId;
        packetId = (packetId + 1) % 256;

        return currentPacketId;
    }

    public Packet getFeederId(int address) {
        return PacketBuilder.command(address, fromAddress, nextPacketId(), 0x01).toPacket();
    }

    public Packet initializeFeeder(int address, String uuid) {
        return PacketBuilder.command(address, fromAddress, nextPacketId(), 0x02)
                .putUuid(uuid)
                .toPacket();
    }

    public Packet getVersion(int address) {
        return PacketBuilder.command(address, fromAddress, nextPacketId(), 0x03).toPacket();
    }

    public Packet moveFeedForward(int address, int distance) {
        return PacketBuilder.command(address, fromAddress, nextPacketId(), 0x04)
                .putByte(distance)
                .toPacket();
    }

    public Packet moveFeedBackward(int address, int distance) {
        return PacketBuilder.command(address, fromAddress, nextPacketId(), 0x05)
                .putByte(distance)
                .toPacket();
    }

    public Packet getFeederAddress(String uuid) {
        return PacketBuilder.command(0xFF, fromAddress, nextPacketId(), 0xC0)
                .putUuid(uuid)
                .toPacket();
    }
}
