package org.openpnp.machine.photon.protocol;

import java.util.Optional;

public abstract class Command<Response> {
    public Response send(PhotonBus bus) throws Exception {
        Optional<Packet> optionalPacket = bus.send(this.toPacket());

        if(optionalPacket.isPresent()) {
            return decodePacket(optionalPacket.get());
        }

        return null;
    }

    public abstract Packet toPacket();

    protected abstract Response decodePacket(Packet responsePacket);
}
