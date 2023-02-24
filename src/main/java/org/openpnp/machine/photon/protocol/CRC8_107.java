package org.openpnp.machine.photon.protocol;

public class CRC8_107 {
    private int crc = 0;

    public void add(int data) {
        crc ^= (data << 8);
        for (int bit_n = 0; bit_n < 8; bit_n++) {
            if ((crc & 0x8000) != 0) {
                crc ^= (0x1070 << 3);
            }
            crc <<= 1;
        }
    }

    public int getCRC() {
        return (crc >> 8) & 0xFF;
    }
}
