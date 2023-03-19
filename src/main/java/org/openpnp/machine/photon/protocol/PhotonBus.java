package org.openpnp.machine.photon.protocol;

import org.openpnp.spi.Actuator;

import java.util.Optional;

class PhotonBus implements PhotonBusInterface{
    private final int fromAddress;
    private final Actuator photonActuator;
    private int packetId;

    public PhotonBus(int fromAddress, Actuator photonActuator) {
        this.fromAddress = fromAddress;
        this.photonActuator = photonActuator;
        this.packetId = 0;
    }

    private int nextPacketId() {
        int currentPacketId = packetId;
        packetId = (packetId + 1) % 256;

        return currentPacketId;
    }

    public Optional<Packet> send(Packet commandPacket) throws Exception {
        commandPacket.fromAddress = this.fromAddress;
        commandPacket.packetId = nextPacketId();

        // Send the packet
        String responseString = photonActuator.read(commandPacket.toByteString());

        Optional<Packet> optionalPacket = Packet.decode(responseString);

        if(! optionalPacket.isPresent()) {
            return Optional.empty();
        }

        Packet receivedPacket = optionalPacket.get();

        // Is this our packet?
        if(receivedPacket.packetId != commandPacket.packetId) {
            return Optional.empty();
        }

        return optionalPacket;
    }
}

