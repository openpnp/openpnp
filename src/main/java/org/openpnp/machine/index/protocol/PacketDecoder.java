package org.openpnp.machine.index.protocol;

public class PacketDecoder {
    private final PacketResponse response;
    private String message;
    private int[] data;
    private int index;

    private PacketDecoder(String message) {
        response = new PacketResponse();

        if(message.length() % 2 != 0) {
            response.setValid(false);
            return;
        }

        // Minimum 6 bytes: <host address> <length> <feeder address> <status> <crc16>
        if(message.length() < 12) {
            response.setValid(false);
            return;
        }

        this.message = message;
        data = new int[(message.length() / 2) - 2];
        for (int i = 0; i < data.length; i++) {
            data[i] = PacketHelper.getByteAtIndex(message, i);
        }

        int expectedChecksum = (PacketHelper.getByteAtIndex(message, data.length + 1) << 8) +
                PacketHelper.getByteAtIndex(message, data.length);
        int actualChecksum = PacketHelper.crc16(data);

        if(expectedChecksum != actualChecksum) {
            response.setValid(false);
            return;
        }

        int packetLength = data[1];
        if(packetLength != data.length - 2) {
            response.setValid(false);
            return;
        }

        response.setValid(true);

        index = 4;
        response.setTargetAddress(data[0]);
        response.setFeederAddress(data[2]);

        if (data[3] != 0x00) {
            this.decodeError(ErrorTypes.fromId(data[3]));
        }
    }

    private void decodeError(ErrorTypes error) {
        switch (error) {
            case WRONG_FEEDER_UUID:
                this.matchUuid();
                break;
            case UNINITIALIZED_FEEDER:
                this.matchUuid();
                break;
            case MOTOR_FAULT:
                break;
        }

        /*
        Set the error at the end to close off the match functions. Externally, the caller
        of our API assumes the packet is not in error and they match based on that. In this
        function we might have to match fields for the error before disabling those match
        functions.
         */
        response.setError(error);
    }

    public static PacketDecoder decode(String message) {
        return new PacketDecoder(message);
    }

    public PacketDecoder matchUuid() {
        if (! shouldKeepMatching()) {
            return this;
        }

        if(index + 12 > data.length) {
            response.setValid(false);
            return this;
        }

        String uuid = message.substring(2 * index, 2 * (index + 12));
        response.setUuid(uuid);

        index += 12;

        return this;
    }

    public PacketDecoder matchByte(String fieldName) {
        if (! shouldKeepMatching()) {
            return this;
        }

        if(index + 1 > data.length) {
            response.setValid(false);
            return this;
        }

        response.setField(fieldName, data[index]);
        index += 1;
        return this;
    }

    public PacketResponse response() {
        return response;
    }

    private boolean shouldKeepMatching() {
        return response.isValid() && response.isOk();
    }
}
