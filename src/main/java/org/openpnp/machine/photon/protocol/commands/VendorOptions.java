package org.openpnp.machine.photon.protocol.commands;

import java.util.Arrays;

import org.openpnp.machine.photon.protocol.Command;
import org.openpnp.machine.photon.protocol.ErrorTypes;
import org.openpnp.machine.photon.protocol.Packet;
import org.openpnp.machine.photon.protocol.PacketBuilder;

public class VendorOptions extends Command<VendorOptions.Response> {
    public static final int COMMAND_ID = 0xBF;
    private final int toAddress;
    private final int[] options;

    public VendorOptions(int toAddress, int[] options) {
        this.toAddress = toAddress;
        this.options = options;
    }

    @Override
    public Packet toPacket() {
        return PacketBuilder.command(COMMAND_ID, toAddress)
                .putArray(options)
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
        public final int[] data;
        public final ErrorTypes error;

        public Response(Packet packet) {
            toAddress = packet.toAddress;
            fromAddress = packet.fromAddress;

            if (packet.payloadLength > 1) {
                data = Arrays.copyOfRange(packet.payload, 1, packet.payloadLength - 1);
            } else {
                data = null;
                if(packet.payloadLength < 1) {
                    valid = false;
                    error = null;
                    return;
                }
            }

            valid = true;

            error = ErrorTypes.fromId(packet.payload[0]);
        }
    }
}
