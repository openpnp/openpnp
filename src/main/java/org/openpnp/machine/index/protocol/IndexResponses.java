package org.openpnp.machine.index.protocol;

public class IndexResponses {
    public static class Errors {
        public static String wrongFeederUUID(int feederAddress, String uuid) {
            return PacketBuilder.response(feederAddress)
                    .putError(ErrorTypes.WRONG_FEEDER_UUID)
                    .putUuid(uuid)
                    .toByteString();
        }

        public static String motorFault(int feederAddress) {
            return PacketBuilder.response(feederAddress)
                    .putError(ErrorTypes.MOTOR_FAULT)
                    .toByteString();
        }

        public static String uninitializedFeeder(int feederAddress) {
            return PacketBuilder.response(feederAddress)
                    .putError(ErrorTypes.UNINITIALIZED_FEEDER)
                    .toByteString();
        }
    }

    public static class GetFeederId {
        public static String ok(int feederAddress, String uuid) {
            return PacketBuilder.response(feederAddress)
                    .putOk()
                    .putUuid(uuid)
                    .toByteString();
        }

        public static PacketResponse decode(String message) {
            return PacketDecoder.decode(message)
                    .matchUuid()
                    .response();
        }
    }

    public static class InitializeFeeder {
        public static String ok(int feederAddress) {
            return PacketBuilder.response(feederAddress).putOk().toByteString();
        }

        public static PacketResponse decode(String message) {
            return PacketDecoder.decode(message).response();
        }
    }

    public static class GetVersion {
        public static final String VERSION_FIELD = "version";

        public static String ok(int feederAddress, int version) {
            return PacketBuilder.response(feederAddress)
                    .putOk()
                    .putByte(version)
                    .toByteString();
        }

        public static PacketResponse decode(String message) {
            return PacketDecoder.decode(message)
                    .matchByte(VERSION_FIELD)
                    .response();
        }
    }

    public static class MoveFeedForward {
        public static String ok(int feederAddress) {
            return PacketBuilder.response(feederAddress).putOk().toByteString();
        }

        public static PacketResponse decode(String message) {
            return PacketDecoder.decode(message).response();
        }
    }

    public static class MoveFeedBackward {
        public static String ok(int feederAddress) {
            return PacketBuilder.response(feederAddress).putOk().toByteString();
        }

        public static PacketResponse decode(String message) {
            return PacketDecoder.decode(message).response();
        }
    }

    public static class GetFeederAddress {
        public static String ok(int feederAddress, String uuid) {
            return PacketBuilder.response(feederAddress)
                    .putOk()
                    .putUuid(uuid)
                    .toByteString();
        }

        public static PacketResponse decode(String message) {
            return PacketDecoder.decode(message)
                    .matchUuid()
                    .response();
        }
    }
}
