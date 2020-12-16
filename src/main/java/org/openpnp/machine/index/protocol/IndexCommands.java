package org.openpnp.machine.index.protocol;

public class IndexCommands {
    public static class GetFeederId {
        public static String encode(int address) {
            return Packet.command(address, 0x01).toByteString();
        }

        public static String ok(int feeder_address, String uuid) {
            return Packet.response(feeder_address)
                    .putOk()
                    .putUuid(uuid)
                    .toByteString();
        }
    }

    public static class InitializeFeeder {
        public static String encode(int address, String uuid) {
            return Packet.command(address, 0x02)
                    .putUuid(uuid)
                    .toByteString();
        }

        public static String ok(int feeder_address) {
            return Packet.response(feeder_address).putOk().toByteString();
        }
    }

    public static class GetVersion {
        public static String encode(int address) {
            return Packet.command(address, 0x03).toByteString();
        }

        public static String ok(int feeder_address, int version) {
            return Packet.response(feeder_address)
                    .putOk()
                    .putByte(version)
                    .toByteString();
        }
    }

    public static class MoveFeedForward {
        public static String encode(int address, int distance) {
            return Packet.command(address, 0x04)
                    .putByte(distance)
                    .toByteString();
        }

        public static String ok(int feeder_address) {
            return Packet.response(feeder_address).putOk().toByteString();
        }
    }

    public static class MoveFeedBackward {
        public static String encode(int address, int distance) {
            return Packet.command(address, 0x05)
                    .putByte(distance)
                    .toByteString();
        }

        public static String ok(int feeder_address) {
            return Packet.response(feeder_address).putOk().toByteString();
        }
    }

    public static class GetFeederAddress {
        public static String encode(String uuid) {
            return Packet.command(0xFF, 0x01)
                    .putUuid(uuid)
                    .toByteString();
        }

        public static String ok(int feeder_address, String uuid) {
            return Packet.response(feeder_address)
                    .putOk()
                    .putUuid(uuid)
                    .toByteString();
        }
    }
}
