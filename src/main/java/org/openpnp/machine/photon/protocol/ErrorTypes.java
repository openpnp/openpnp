package org.openpnp.machine.photon.protocol;

public enum ErrorTypes {
    NONE(0x00),
    WRONG_FEEDER_UUID(0x01),
    COULD_NOT_REACH(0x02),
    UNINITIALIZED_FEEDER(0x03),
    FEEDING_IN_PROGRESS(0x04),
    // TIMEOUT(0xFE),
    UNKNOWN(0xFF);

    private final int id;

    ErrorTypes(int id) {
        this.id = id;
    }

    public static ErrorTypes fromId(int id) {
        for (ErrorTypes error : ErrorTypes.values()) {
            if (error.id == id) {
                return error;
            }
        }

        return UNKNOWN;
    }

    public int getId() {
        return id;
    }
}
