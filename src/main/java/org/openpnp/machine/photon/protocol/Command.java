package org.openpnp.machine.photon.protocol;

public abstract class Command<Response> {
    public Response send(PhotonBus bus) {
        Packet responsePacket = bus.send(this.toPacket());

        if(responsePacket != null) {
            return decodePacket(responsePacket);
        }

        return null;
    }

    protected abstract Packet toPacket();

    protected abstract Response decodePacket(Packet responsePacket);
}
