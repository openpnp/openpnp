package org.openpnp.machine.photon.protocol.helpers;

import org.openpnp.machine.photon.protocol.Command;
import org.openpnp.machine.photon.protocol.Packet;
import org.openpnp.machine.photon.protocol.PhotonBusInterface;
import org.opentest4j.AssertionFailedError;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.fail;

public class TestBus implements PhotonBusInterface {
    private final List<TestBusReply> replies;
    private final List<Packet> calls;
    private final List<Packet> mockedPackets;
    private final ContinuedVerification continuedVerification;

    public TestBus() {
        replies = new ArrayList<>();
        calls = new ArrayList<>();
        mockedPackets = new ArrayList<>();
        continuedVerification = new ContinuedVerification();
    }

    public <T> Optional<Packet> send(Command<T> command) throws Exception {
        return send(command.toPacket());
    }

    @Override
    public Optional<Packet> send(Packet commandPacket) throws Exception {
        calls.add(commandPacket.clone());

        for (TestBusReply testBusReply : replies) {
            if (!testBusReply.isCommand(commandPacket)) {
                continue;
            }

            Optional<Packet> optionalPacket = testBusReply.getPacket();
            if (optionalPacket.isPresent()) {
                Packet packet = optionalPacket.get();
                packet.packetId = commandPacket.packetId;
                return Optional.of(packet);
            } else {
                return Optional.empty();
            }
        }

        throw new NoPacketMocking("Command packet did not match any requested mock.");
    }

    public TestBusReply when(Packet packet) throws Exception {
        packet = packet.clone();

        mockedPackets.add(packet);

        for (TestBusReply testBusReply : replies) {
            if (testBusReply.isCommand(packet)) {
                return testBusReply;
            }
        }

        TestBusReply testBusReply = new TestBusReply(packet);

        replies.add(testBusReply);

        return testBusReply;
    }

    public <T> TestBusReply when(Command<T> command) throws Exception {
        return when(command.toPacket());
    }

    public ContinuedVerification verify(Packet packet) {
        continuedVerification.then(packet);

        return continuedVerification;
    }

    public <T> ContinuedVerification verify(Command<T> command) {
        return verify(command.toPacket());
    }

    public void verifyNothingSent() {
        continuedVerification.nothingElseSent();
    }

    public void verifyInMockedOrder() {
        for (Packet packet : mockedPackets) {
            continuedVerification.then(packet);
        }

        continuedVerification.nothingElseSent();
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
            if (packet.toAddress != testPacket.toAddress ||
                    packet.fromAddress != testPacket.fromAddress ||
                    packet.payloadLength != testPacket.payloadLength ||
                    packet.payload.length != testPacket.payload.length) {
                return false;
            }

            for (int i = 0; i < packet.payload.length; i++) {
                if (packet.payload[i] != testPacket.payload[i]) {
                    return false;
                }
            }

            return true;
        }

        Optional<Packet> getPacket() throws NoPacketMocking {
            if (!packetSpecified) {
                throw new NoPacketMocking("Packet sent was specified but no reply given.");
            } else if (response == null) {
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

    public class ContinuedVerification {
        private int currentVerificationIndex;

        ContinuedVerification() {
            currentVerificationIndex = 0;
        }

        public <T> ContinuedVerification then(Command<T> command) {
            return then(command.toPacket());
        }

        public ContinuedVerification then(Packet expectedPacket) {
            if (currentVerificationIndex >= calls.size()) {
                fail("No more calls on this test bus, but wanted " + expectedPacket.toByteString());
            }

            Packet actualPacket = calls.get(currentVerificationIndex);

            if (!actualPacket.toByteString().equals(expectedPacket.toByteString())) {
                // fail method doesn't have the right arguments, throw the exception directly.
                throw new AssertionFailedError("Did not find expected packet", expectedPacket.toByteString(), actualPacket.toByteString());
            }

            currentVerificationIndex++;

            return this;
        }

        public void nothingElseSent() {
            if (calls.size() > currentVerificationIndex) {
                int howManyMore = calls.size() - currentVerificationIndex;

                StringBuilder builder = new StringBuilder()
                        .append("There ")
                        .append(howManyMore == 1 ? "is" : "are")
                        .append(" still ")
                        .append(howManyMore)
                        .append(" more call")
                        .append(howManyMore == 1 ? "" : "s")
                        .append(":\n");

                for (int i = 0; i < howManyMore; i++) {
                    Packet packet = calls.get(currentVerificationIndex + i);
                    builder.append("- ").append(packet.toByteString()).append("\n");
                }

                fail(builder.toString());
            }
        }
    }
}
