package org.openpnp.machine.index.protocol;

public class IndexCommands {
    public static String getFeederId(int address) {
        return Packet.command(address, 0x01).toByteString();
    }

    public static String initializeFeeder(int address, String uuid) {
        return Packet.command(address, 0x02)
                .putUuid(uuid)
                .toByteString();
    }

    public static String getVersion(int address) {
        return Packet.command(address, 0x03).toByteString();
    }

    public static String moveFeedForward(int address, int distance) {
        return Packet.command(address, 0x04)
                .putByte(distance)
                .toByteString();
    }

    public static String moveFeedBackward(int address, int distance) {
        return Packet.command(address, 0x05)
                .putByte(distance)
                .toByteString();
    }
    public static String getFeederAddress(String uuid) {
        return Packet.command(0xFF, 0x01)
                .putUuid(uuid)
                .toByteString();
    }
}
