package org.openpnp.machine.index.protocol;

public class IndexCommands {
    public static class GetFeederId {
        public static String encode(int address) {
            return Packet.command(address, 0x01);
        }

        public static String ok(int feeder_address, String uuid) {
            int[] data = new int[16];
            data[0] = 0x00;
            data[1] = 0x0E; // Length
            data[2] = feeder_address;
            data[3] = 0x00; // OK
            Packet.writeUuidAt(uuid, data, 4);

            return Packet.toByteString(data);
        }
    }

    public static class InitializeFeeder {
        public static String encode(int address, String uuid) {
            return Packet.command(address, 0x02, uuid);
        }

        public static String ok(int feeder_address) {
            int[] data = new int[] {0x00, 0x02, feeder_address, 0x00};

            return Packet.toByteString(data);
        }
    }

    public static class GetVersion {
        public static String encode(int address) {
            return Packet.command(address, 0x03);
        }

        public static String ok(int feeder_address, int version) {
            int[] data = new int[] {0x00, 0x03, feeder_address, 0x00, version};

            return Packet.toByteString(data);
        }
    }

    public static class MoveFeedForward {
        public static String encode(int address, int distance) {
            return Packet.command(address, 0x04, distance);
        }

        public static String ok(int feeder_address) {
            int[] data = new int[] {0x00, 0x02, feeder_address, 0x00};

            return Packet.toByteString(data);
        }
    }

    public static class MoveFeedBackward {
        public static String encode(int address, int distance) {
            return Packet.command(address, 0x05, distance);
        }

        public static String ok(int feeder_address) {
            int[] data = new int[] {0x00, 0x02, feeder_address, 0x00};

            return Packet.toByteString(data);
        }
    }

    public static class GetFeederAddress {
        public static String encode(String uuid) {
            return Packet.command(0xFF, 0x01, uuid);
        }

        public static String ok(int feeder_address, String uuid) {
            int[] data = new int[16];
            data[0] = 0x00;
            data[1] = 0x0E; // Length
            data[2] = feeder_address;
            data[3] = 0x00; // OK
            Packet.writeUuidAt(uuid, data, 4);

            return Packet.toByteString(data);
        }
    }
}
