package org.openpnp.machine.photon.protocol.commands;

import org.openpnp.machine.photon.protocol.Command;
import org.openpnp.machine.photon.protocol.ErrorTypes;
import org.openpnp.machine.photon.protocol.Packet;
import org.openpnp.machine.photon.protocol.PacketBuilder;

public class UninitializedFeedersRespond extends Command<UninitializedFeedersRespond.Response> {
    public static final int COMMAND_ID = 0xC3;
    public static final int BROADCAST_ID = 0xff;

    public UninitializedFeedersRespond() {
    }

    @Override
    public Packet toPacket() {
        return PacketBuilder.command(COMMAND_ID, BROADCAST_ID)
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
        public final String uuid;

        public Response(Packet packet) {
            toAddress = packet.toAddress;
            fromAddress = packet.fromAddress;

            if(packet.payloadLength != 13) {
                valid = false;
                error = null;
                uuid = null;
                return;
            }

            valid = true;

            error = ErrorTypes.fromId(packet.payload[0]);
            uuid = packet.uuid(1);
        }
    }
}
