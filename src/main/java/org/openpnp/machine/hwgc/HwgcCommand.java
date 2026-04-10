/*
 * Copyright (C) 2026 Contributed by Arnoud @ DeltaProto <arnoud@deltaproto.com>
 *
 * This file is part of OpenPnP.
 *
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.machine.hwgc;

/**
 * Command byte constants for the HWGC serial protocol.
 * Reverse-engineered from ClassLibrary1.DLL (QIGN_CONNECTION.QnConnectionClass).
 *
 * Packet format: 7 bytes total, command byte at index 6 (last byte).
 * Response format: 6 bytes total, echo byte at index 0.
 */
public final class HwgcCommand {

    private HwgcCommand() {}

    // Motion commands
    public static final int XY_COORDINATE     = 0x60;
    public static final int Z_COORDINATE      = 0x62;
    public static final int A_COORDINATE      = 0x16;  // Rotation axis
    public static final int STOP_MOVE         = 0x1F;

    // Constant speed (jog) commands
    public static final int XY_CONSTANT_SPEED = 0x18;
    public static final int Z_CONSTANT_SPEED  = 0x19;
    public static final int A_CONSTANT_SPEED  = 0x1A;

    // Travel limit commands
    public static final int SET_MAX_XY        = 0x10;
    public static final int SET_MAX_Z         = 0x11;

    // I/O commands
    public static final int VACUUM_SWITCH     = 0x41;
    public static final int BREAK_VACUUM      = 0x45;
    public static final int FEEDER_SWITCH     = 0x40;
    public static final int MARK_LED          = 0x23;
    public static final int MARK_LED_ILL      = 0x22;
    public static final int LEVEL_LED         = 0x20;
    public static final int HS_LED            = 0x21;
    public static final int BUZZER            = 0x42;
    public static final int ALARM_LAMP        = 0x43;
    public static final int SMT_READY         = 0x39;
    public static final int FNOFB             = 0x4F;

    // Board handling
    public static final int IN_BOARD          = 0x30;
    public static final int OUT_BOARD         = 0x31;
    public static final int TRACK_CONST_SPEED = 0x32;
    public static final int TRACK_STOP_MOVE   = 0x33;
    public static final int TRACK_COORDINATE  = 0x34;
    public static final int EXECUTE_PLYWOOD   = 0x35;
    public static final int TAB_ONLY          = 0x37;
    public static final int TRACK_SPEED       = 0x3A;
    public static final int TRACK_DELAY       = 0x3B;
    public static final int INBOARD_MODE      = 0x3D;
    public static final int INBOARD_RESET     = 0x3E;
    public static final int TRACK_DINGBAN     = 0x3F;

    // Drop detection
    public static final int LOUKONG_ENABLE    = 0x50;
    public static final int LOUKONG_CLEAR_S3  = 0x51;
    public static final int LOUKONG_SET_STATE = 0x52;

    // Transfer
    public static final int TRANSFER_TASK_RUN   = 0x6A;
    public static final int TRANSFER_SMT_DONE   = 0x6B;
    public static final int TRANSFER_PLATE_EMPTY = 0x6C;

    // System commands
    public static final int RESET             = 0x7F;
    public static final int READ_VERSION      = 0x80;

    // RESET sub-commands
    public static final int RESET_ALL         = 0;
    public static final int RESET_ZA          = 1;
    public static final int RESET_XY          = 2;

    // Status queries
    public static final int READ_XY_COORD     = 0xE0;
    public static final int READ_Z_COORD      = 0xE1;
    public static final int READ_A_COORD      = 0xE2;
    public static final int READ_PLYWOOD_STATE = 0xB9;
    public static final int READ_OPEN_SHAPE   = 0xD0;
    public static final int READ_FEEDER_GOODBYE = 0xD1;

    // Protocol constants
    public static final int CMD_PACKET_LEN    = 7;
    public static final int RTN_PACKET_LEN    = 6;
    public static final int CMD_BYTE_INDEX    = 6;

    // Speed
    public static final int SPEED_MIN         = 1;
    public static final int SPEED_MAX         = 64;

    // Machine limits
    public static final int MAX_NOZZLES       = 8;

    /** Encode speed for wire: inverted so higher = slower. */
    public static byte encodeSpeed(int speed) {
        if (speed < SPEED_MIN) {
            speed = SPEED_MIN;
        }
        if (speed > SPEED_MAX) {
            speed = SPEED_MAX;
        }
        return (byte) (SPEED_MAX - speed + 1);
    }

    /** Decode speed from wire format. */
    public static int decodeSpeed(int wireSpeed) {
        return SPEED_MAX - (wireSpeed & 0xFF) + 1;
    }

    /**
     * Encode XY position into a 7-byte command packet (GEN0 20-bit nibble-packed).
     * Bytes 0-1: X low 16 bits, Bytes 2-3: Y low 16 bits,
     * Byte 4 high nibble: X bits 16-19, Byte 4 low nibble: Y bits 16-19.
     */
    public static void encodeXY(byte[] cmd, int x, int y) {
        cmd[0] = (byte) x;
        cmd[1] = (byte) (x >> 8);
        cmd[2] = (byte) y;
        cmd[3] = (byte) (y >> 8);
        cmd[4] = (byte) (((x >> 12) & 0xF0) | ((y >> 16) & 0x0F));
    }

    /**
     * Decode XY position from a 6-byte response packet.
     * Response format: [echo, status, X_lo, X_hi, Y_lo, Y_hi]
     * XY is simple 16-bit LE (NOT 20-bit nibble-packed like commands).
     * Returns int[2] = {x, y}.
     */
    public static int[] decodeXY(byte[] resp) {
        int x = (resp[2] & 0xFF) | ((resp[3] & 0xFF) << 8);
        int y = (resp[4] & 0xFF) | ((resp[5] & 0xFF) << 8);
        return new int[] {x, y};
    }

    /**
     * Decode a 32-bit little-endian value from response bytes [2..5].
     * Response format: [echo, nozzle/status, data0, data1, data2, data3]
     * Used for Z and A position responses.
     */
    public static int decode32LE(byte[] resp) {
        return (resp[2] & 0xFF) | ((resp[3] & 0xFF) << 8)
                | ((resp[4] & 0xFF) << 16) | ((resp[5] & 0xFF) << 24);
    }
}
