package org.openpnp.machine.photon.protocol.helpers;

import org.openpnp.machine.photon.protocol.Packet;
import org.openpnp.machine.photon.protocol.PhotonBusInterface;

import java.util.ArrayList;
import java.util.Optional;

public class TestBus implements PhotonBusInterface {
    private final ArrayList<TestBusReply> replies;

    public TestBus() {
        replies = new ArrayList<>();
    }

    @Override
    public Optional<Packet> send(Packet commandPacket) throws Exception {
        for (TestBusReply testBusReply : replies) {
            if(! testBusReply.isCommand(commandPacket)) {
                continue;
            }

            Optional<Packet> optionalPacket = testBusReply.getPacket();
            if(optionalPacket.isPresent()) {
                Packet packet = optionalPacket.get();
                packet.packetId = commandPacket.packetId;
                return Optional.of(packet);
            } else {
                return Optional.empty();
            }
        }

        throw new NoPacketMocking("Command packet did not match any requested mock.");
    }

    public TestBusReply when(Packet command) {
        TestBusReply testBusReply = new TestBusReply(command);

        replies.add(testBusReply);

        return testBusReply;
    }

    public static class TestBusReply {
        private final Packet packet;
        private Packet response;
        private boolean packetSpecified = false;

        protected TestBusReply(Packet packet) {
            this.packet = packet;
            this.response = null;
        }

        public void reply(Packet responsePacket) {
            response = responsePacket;
            packetSpecified = true;
        }

        public void timeout() {
            response = null;
            packetSpecified = true;
        }

        boolean isCommand(Packet testPacket) {
            if(packet.toAddress != testPacket.toAddress ||
                packet.fromAddress != testPacket.fromAddress ||
                packet.payloadLength != testPacket.payloadLength ||
                packet.payload.length != testPacket.payload.length) {
                return false;
            }

            for (int i = 0; i < packet.payload.length; i++) {
                if(packet.payload[i] != testPacket.payload[i]) {
                    return false;
                }
            }

            return true;
        }

        Optional<Packet> getPacket() throws NoPacketMocking {
            if(! packetSpecified) {
                throw new NoPacketMocking("Packet sent was specified but no reply given.");
            } else if(response == null) {
                return Optional.empty();
            } else {
                return Optional.of(response);
            }
        }
    }

    public static class NoPacketMocking extends Exception {
        public NoPacketMocking(String message) {
            super(message);
        }
    }
}
