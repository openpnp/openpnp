package org.openpnp.machine.index.protocol;

import java.util.HashMap;

public class PacketResponse {
    private boolean valid;
    private ErrorTypes error;
    private int targetAddress;
    private int feederAddress;
    private String uuid;
    private final HashMap<String, Integer> fields;

    public PacketResponse() {
        fields = new HashMap<>();
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public boolean isOk() {
        return isValid() && error == null;
    }

    public ErrorTypes getError() {
        return error;
    }

    public void setError(ErrorTypes error) {
        this.error = error;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getTargetAddress() {
        return targetAddress;
    }

    public void setTargetAddress(int address) {
        this.targetAddress = address;
    }

    public int getFeederAddress() {
        return feederAddress;
    }

    public void setFeederAddress(int address) {
        this.feederAddress = address;
    }

    public int getField(String field) {
        return fields.get(field);
    }

    public void setField(String fieldName, int datum) {
        fields.put(fieldName, datum);
    }
}
