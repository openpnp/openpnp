package org.openpnp.machine.photon.protocol.helpers;

import org.openpnp.machine.photon.protocol.ErrorTypes;
import org.openpnp.machine.photon.protocol.Packet;
import org.openpnp.machine.photon.protocol.PacketBuilder;

/**
 * This class is for response generation that we don't need in any of our actual code, but that we do use in our tests.
 * Because we use it in our tests, we also verify that the responses are valid in PhotonResponsesTest.
 */
public class ResponsesHelper {
    private final int toAddress;
    public final Errors errors = new Errors();
    public final GetFeederId getFeederId = new GetFeederId();
    public final InitializeFeeder initializeFeeder = new InitializeFeeder();
    public final GetVersion getVersion = new GetVersion();
    public final MoveFeedForward moveFeedForward = new MoveFeedForward();
    public final MoveFeedBackward moveFeedBackward = new MoveFeedBackward();
    public final MoveFeedStatus moveFeedStatus = new MoveFeedStatus();
    public final GetFeederAddress getFeederAddress = new GetFeederAddress();
    public final IdentifyFeeder identifyFeeder = new IdentifyFeeder();
    public final ProgramFeederFloor programFeederFloor = new ProgramFeederFloor();

    public ResponsesHelper(int toAddress) {
        this.toAddress = toAddress;
    }

    public class Errors {
        public Packet wrongFeederUUID(int feederAddress, String uuid) {
            return PacketBuilder.response(toAddress, feederAddress)
                    .putError(ErrorTypes.WRONG_FEEDER_UUID)
                    .putUuid(uuid)
                    .toPacket();
        }

        public Packet motorFault(int feederAddress) {
            return PacketBuilder.response(toAddress, feederAddress)
                    .putError(ErrorTypes.COULD_NOT_REACH)
                    .toPacket();
        }

        public Packet uninitializedFeeder(int feederAddress, String uuid) {
            return PacketBuilder.response(toAddress, feederAddress)
                    .putError(ErrorTypes.UNINITIALIZED_FEEDER)
                    .putUuid(uuid)
                    .toPacket();
        }

        public String timeout() {
            return "TIMEOUT";
        }
    }

    public class GetFeederId {
        public Packet ok(int feederAddress, String uuid) {
            return PacketBuilder.response(toAddress, feederAddress)
                    .putOk()
                    .putUuid(uuid)
                    .toPacket();
        }
    }

    public class InitializeFeeder {
        public Packet ok(int feederAddress, String uuid) {
            return PacketBuilder.response(toAddress, feederAddress)
                    .putOk()
                    .putUuid(uuid)
                    .toPacket();
        }
    }

    public class GetVersion {
        public Packet ok(int feederAddress, int version) {
            return PacketBuilder.response(toAddress, feederAddress)
                    .putOk()
                    .putByte(version)
                    .toPacket();
        }
    }

    public class MoveFeedForward {
        public Packet ok(int feederAddress, int expectedTimeToFeed) {
            return PacketBuilder.response(toAddress, feederAddress)
                    .putOk()
                    .putUint16(expectedTimeToFeed)
                    .toPacket();
        }
    }

    public class MoveFeedBackward {
        public Packet ok(int feederAddress, int expectedTimeToFeed) {
            return PacketBuilder.response(toAddress, feederAddress)
                    .putOk()
                    .putUint16(expectedTimeToFeed)
                    .toPacket();
        }
    }

    public class MoveFeedStatus {
        public Packet ok(int feederAddress) {
            return PacketBuilder.response(toAddress, feederAddress)
                    .putOk()
                    .toPacket();
        }
    }

    public class GetFeederAddress {
        public Packet ok(int feederAddress) {
            return PacketBuilder.response(toAddress, feederAddress)
                    .putOk()
                    .toPacket();
        }
    }

    public class IdentifyFeeder {
        public Packet ok(int feederAddress) {
            return PacketBuilder.response(toAddress, feederAddress)
                    .putOk()
                    .toPacket();
        }
    }

    public class ProgramFeederFloor {
        public Packet ok(int feederAddress) {
            return PacketBuilder.response(toAddress, feederAddress)
                    .putOk()
                    .toPacket();
        }
    }
}
