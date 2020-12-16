package org.openpnp.machine.index.protocol;

public enum ErrorTypes {
    WRONG_FEEDER_UUID(0x01),
    MOTOR_FAULT(0x02),
    UNINITIALIZED_FEEDER(0x03);

    private final int id;

    ErrorTypes(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
