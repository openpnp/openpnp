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

    public String getFeederId(int address) {
        Packet packet = PacketBuilder.command(address, fromAddress, nextPacketId(), 0x01).toPacket();

        return packet.toByteString();
    }

    public String initializeFeeder(int address, String uuid) {
        Packet packet = PacketBuilder.command(address, fromAddress, nextPacketId(), 0x02)
                .putUuid(uuid)
                .toPacket();

        return packet.toByteString();
    }

    public String getVersion(int address) {
        Packet packet = PacketBuilder.command(address, fromAddress, nextPacketId(), 0x03).toPacket();

        return packet.toByteString();
    }

    public String moveFeedForward(int address, int distance) {
        Packet packet = PacketBuilder.command(address, fromAddress, nextPacketId(), 0x04)
                .putByte(distance)
                .toPacket();

        return packet.toByteString();
    }

    public String moveFeedBackward(int address, int distance) {
        Packet packet = PacketBuilder.command(address, fromAddress, nextPacketId(), 0x05)
                .putByte(distance)
                .toPacket();

        return packet.toByteString();
    }

    public String getFeederAddress(String uuid) {
        Packet packet = PacketBuilder.command(0xFF, fromAddress, nextPacketId(), 0xC0)
                .putUuid(uuid)
                .toPacket();

        return packet.toByteString();
    }
}
