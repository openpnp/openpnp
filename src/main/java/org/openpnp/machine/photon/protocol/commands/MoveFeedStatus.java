package org.openpnp.machine.photon.protocol.commands;

import org.openpnp.machine.photon.protocol.Command;
import org.openpnp.machine.photon.protocol.ErrorTypes;
import org.openpnp.machine.photon.protocol.Packet;
import org.openpnp.machine.photon.protocol.PacketBuilder;

public class MoveFeedStatus extends Command<MoveFeedStatus.Response> {
    public static final int COMMAND_ID = 0x06;
    private final int toAddress;

    public MoveFeedStatus(int toAddress) {
        this.toAddress = toAddress;
    }

    @Override
    public Packet toPacket() {
        return PacketBuilder.command(COMMAND_ID, toAddress)
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
