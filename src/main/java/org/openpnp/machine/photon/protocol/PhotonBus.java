package org.openpnp.machine.photon.protocol;

class PhotonBus {
    private final int fromAddress;
    private int packetId;

    public PhotonBus(int fromAddress) {
        this.fromAddress = fromAddress;
        this.packetId = 0;
    }

    private int nextPacketId() {
        int currentPacketId = packetId;
        packetId = (packetId + 1) % 256;

        return currentPacketId;
    }

    public Packet send(Packet commandPacket) {
        commandPacket.fromAddress = this.fromAddress;
        commandPacket.packetId = nextPacketId();

        // Send the packet

        Packet receivedPacket = null;

        // Is this our packet?
        // Is this a valid packet?

        return receivedPacket;
    }
}

