package org.openpnp.machine.photon.protocol.commands;

import org.openpnp.machine.photon.protocol.Command;
import org.openpnp.machine.photon.protocol.ErrorTypes;
import org.openpnp.machine.photon.protocol.Packet;
import org.openpnp.machine.photon.protocol.PacketBuilder;

public class ProgramFeederFloorAddress extends Command<ProgramFeederFloorAddress.Response> {
    public static final int COMMAND_ID = 0xC2;
    public static final int BROADCAST_ID = 0xff;
    private final String uuid;
    private final int floorAddress;

    public ProgramFeederFloorAddress(String uuid, int floorAddress) {
        this.uuid = uuid;
        this.floorAddress = floorAddress;
    }

    @Override
    public Packet toPacket() {
        return PacketBuilder.command(COMMAND_ID, BROADCAST_ID)
                .putUuid(uuid)
                .putByte(floorAddress)
                .toPacket();
    }

    @Override
    protected Response decodePacket(Packet responsePacket) {
        return new Response(responsePacket);
    }

    public static class Response {
        public final boolean valid;
        public final int toAddress;
        public final int fromAddress;
        public final ErrorTypes error;

        public Response(Packet packet) {
            toAddress = packet.toAddress;
            fromAddress = packet.fromAddress;

            if(packet.payloadLength != 1) {
                valid = false;
                error = null;
                return;
            }

            valid = true;

            error = ErrorTypes.fromId(packet.payload[0]);
        }
    }
}
