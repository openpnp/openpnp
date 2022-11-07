package org.openpnp.machine.photon.protocol;

public class PhotonCommands {
    public static String getFeederId(int address) {
        return PacketBuilder.command(address, 0x01).toByteString();
    }

    public static String initializeFeeder(int address, String uuid) {
        return PacketBuilder.command(address, 0x02)
                .putUuid(uuid)
                .toByteString();
    }

    public static String getVersion(int address) {
        return PacketBuilder.command(address, 0x03).toByteString();
    }

    public static String moveFeedForward(int address, int distance) {
        return PacketBuilder.command(address, 0x04)
                .putByte(distance)
                .toByteString();
    }

    public static String moveFeedBackward(int address, int distance) {
        return PacketBuilder.command(address, 0x05)
                .putByte(distance)
                .toByteString();
    }

    public static String getFeederAddress(String uuid) {
        return PacketBuilder.command(0xFF, 0xC0)
                .putUuid(uuid)
                .toByteString();
    }
}
