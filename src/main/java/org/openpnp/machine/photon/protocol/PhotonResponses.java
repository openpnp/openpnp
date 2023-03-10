package org.openpnp.machine.photon.protocol;

public class PhotonResponses {
    public static class GetFeederId {
        public static PacketResponse decode(String message) {
            return PacketDecoder.decode(message)
                    .matchUuid()
                    .response();
        }
    }

    public static class InitializeFeeder {
        public static PacketResponse decode(String message) {
            return PacketDecoder.decode(message).response();
        }
    }

    public static class GetVersion {
        public static final String VERSION_FIELD = "version";

        public static String ok(int feederAddress, int version) {
            return PacketBuilder.response(feederAddress, 0)
                    .putOk()
                    .putByte(version)
                    .toPacket()
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
            return PacketBuilder.response(feederAddress, 0).putOk()
                    .toPacket().toByteString();
        }

        public static PacketResponse decode(String message) {
            return PacketDecoder.decode(message).response();
        }
    }

    public static class MoveFeedBackward {
        public static String ok(int feederAddress) {
            return PacketBuilder.response(feederAddress, 0).putOk()
                    .toPacket().toByteString();
        }

        public static PacketResponse decode(String message) {
            return PacketDecoder.decode(message).response();
        }
    }

    public static class GetFeederAddress {
        public static String ok(int feederAddress, String uuid) {
            return PacketBuilder.response(feederAddress, 0)
                    .putOk()
                    .putUuid(uuid)
                    .toPacket()
                    .toByteString();
        }

        public static PacketResponse decode(String message) {
            return PacketDecoder.decode(message)
                    .matchUuid()
                    .response();
        }
    }
}
