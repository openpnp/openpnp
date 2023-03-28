package org.openpnp.machine.photon.protocol.commands;


import org.openpnp.machine.photon.protocol.Command;
import org.openpnp.machine.photon.protocol.ErrorTypes;
import org.openpnp.machine.photon.protocol.Packet;
import org.openpnp.machine.photon.protocol.PacketBuilder;

public class GetFeederId extends Command<GetFeederId.Response> {
    public static final int COMMAND_ID = 0x01;
    private final int toAddress;

    public GetFeederId(int toAddress) {
        this.toAddress = toAddress;
    }

    @Override
    public Packet toPacket() {
        return PacketBuilder
                .command(COMMAND_ID, toAddress)
                .toPacket();
    }

    protected Response decodePacket(Packet packet) {
        return new Response(packet);
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
