package org.openpnp.machine.photon.protocol;

import java.util.Optional;

public interface PhotonBusInterface {
    Optional<Packet> send(Packet commandPacket) throws Exception;
}
