package org.openpnp.machine.photon.protocol.commands;

import org.openpnp.machine.photon.protocol.Command;
import org.openpnp.machine.photon.protocol.ErrorTypes;
import org.openpnp.machine.photon.protocol.Packet;
import org.openpnp.machine.photon.protocol.PacketBuilder;

public class MoveFeedForward  extends Command<MoveFeedForward.Response> {
    public static final int COMMAND_ID = 0x04;
    private final int toAddress;
    private final int distance;

    /**
     * @param toAddress address to send this packet to
     * @param distance distance to move in 0.1 mm increments
     */
    public MoveFeedForward(int toAddress, int distance) {
        this.toAddress = toAddress;
        this.distance = distance;
    }

    @Override
    public Packet toPacket() {
        return PacketBuilder.command(COMMAND_ID, toAddress)
                .putByte(distance)
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
        public final int expectedTimeToFeed;

        public Response(Packet packet) {
            toAddress = packet.toAddress;
            fromAddress = packet.fromAddress;

            if(packet.payloadLength < 1) {  // Can't even get the error bytes
                valid = false;
                error = null;
                expectedTimeToFeed = 0;
                return;
            }

            error = ErrorTypes.fromId(packet.payload[0]);

            if(error == ErrorTypes.NONE) {
                expectedTimeToFeed = packet.uint16(1);
                valid = true;
            } else if(error == ErrorTypes.UNINITIALIZED_FEEDER) {
                expectedTimeToFeed = 0;
                valid = true;
            } else {
                expectedTimeToFeed = 0;
                valid = false;
            }
        }
    }
}
