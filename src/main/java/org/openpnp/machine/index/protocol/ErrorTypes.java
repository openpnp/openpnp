package org.openpnp.machine.index.protocol;

public enum ErrorTypes {
    WRONG_FEEDER_UUID(0x01),
    MOTOR_FAULT(0x02),
    UNINITIALIZED_FEEDER(0x03),
    TIMEOUT(0xFE),
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
