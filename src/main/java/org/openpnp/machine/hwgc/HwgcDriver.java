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

import static org.openpnp.machine.hwgc.HwgcCommand.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.machine.hwgc.wizards.HwgcDriverConfigurationWizard;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.ReferenceNozzleTip;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.machine.reference.axis.ReferenceVirtualAxis;
import org.openpnp.machine.reference.driver.AbstractReferenceDriver;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Motion.MoveToCommand;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Camera;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MotionPlanner.CompletionType;
import org.openpnp.spi.NozzleTip;
import org.openpnp.spi.base.AbstractHeadMountable;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

/**
 * OpenPnP driver for HWGC (Beijing Huawei Guochang) SMT pick-and-place machines.
 * Supports SMT550 (4 heads), SMT660 (6 heads), SMT880 (8 heads) family.
 *
 * Communicates with the PC SMT controller board via a 7-byte binary serial protocol
 * over a USB-UART bridge (Exar XR21V1410) at 460800 baud, 8N1.
 *
 * Protocol details: https://github.com/mcix/smt550-openpnp/blob/main/PROTOCOL.md
 */
@Root
public class HwgcDriver extends AbstractReferenceDriver {

    // Actuator name constants
    public static final String ACT_N1_VACUUM = "N1-Vacuum";
    public static final String ACT_N2_VACUUM = "N2-Vacuum";
    public static final String ACT_N3_VACUUM = "N3-Vacuum";
    public static final String ACT_N4_VACUUM = "N4-Vacuum";
    public static final String ACT_N1_BLOW = "N1-Blow";
    public static final String ACT_N2_BLOW = "N2-Blow";
    public static final String ACT_N3_BLOW = "N3-Blow";
    public static final String ACT_N4_BLOW = "N4-Blow";

    @Attribute(required = false)
    protected LengthUnit units = LengthUnit.Millimeters;

    @Attribute(required = false)
    protected int timeoutMilliseconds = 5000;

    @Attribute(required = false)
    protected int connectWaitTimeMilliseconds = 3000;

    /** Hardware generation: 0=GEN0, 2=GEN2. Affects XY coordinate encoding. */
    @Attribute(required = false)
    protected int comType = 0;

    /** Maximum X travel in machine units (from device calibration). */
    @Attribute(required = false)
    protected int maxX = 44300;

    /** Maximum Y travel in machine units (from device calibration). */
    @Attribute(required = false)
    protected int maxY = 57200;

    /** Maximum Z travel in machine units (from device calibration). */
    @Attribute(required = false)
    protected int maxZ = 11520;

    /** Number of nozzles on this machine (4 for SMT550). */
    @Attribute(required = false)
    protected int nozzleCount = 4;

    /**
     * Scale factor: machine units per millimeter.
     * This converts between OpenPnP's mm coordinates and the HWGC machine units.
     * Must be calibrated per machine.
     */
    @Attribute(required = false)
    protected double scaleX = 100.0;

    @Attribute(required = false)
    protected double scaleY = 100.0;

    @Attribute(required = false)
    protected double scaleZ = 100.0;

    /**
     * Scale factor for rotation: machine units per degree.
     * Negated so that a positive OpenPnP angle rotates the physical nozzle
     * in the direction bottom-vision expects for Test Alignment.
     */
    @Attribute(required = false)
    protected double scaleA = -25600.0 / 360.0;  // -71.111 — 200-step motor × 128 microsteps = 25600 steps/rev

    /** Break vacuum air blow duration in seconds. */
    @Attribute(required = false)
    protected double blowDurationSec = 0.3;

    /** Motion completion tolerance in machine units. */
    @Attribute(required = false)
    protected int motionTolerance = 10;

    /** Polling interval in ms for motion completion. */
    @Attribute(required = false)
    protected int motionPollIntervalMs = 50;

    private boolean connected;
    private boolean homed;
    private boolean motionPending;

    private AxesLocation homingOffsets = new AxesLocation();

    // Cached machine positions (in machine units)
    private int posX;
    private int posY;
    private final int[] posZ = new int[4];
    private final int[] posA = new int[4];

    // Target positions for motion completion detection
    private int targetX;
    private int targetY;
    private final int[] targetZ = new int[4];
    private final int[] targetA = new int[4];
    private boolean xyMoved;
    private final boolean[] zMoved = new boolean[4];
    private final boolean[] aMoved = new boolean[4];

    // ──────────────────────────────────────────
    //  Machine object setup
    // ──────────────────────────────────────────

    private ReferenceActuator getOrCreateActuatorInHead(ReferenceHead head, String name)
            throws Exception {
        ReferenceActuator a = (ReferenceActuator) head.getActuatorByName(name);
        if (a == null) {
            a = new ReferenceActuator();
            a.setName(name);
            head.addActuator(a);
        }
        return a;
    }

    private ReferenceActuator getOrCreateMachineActuator(ReferenceMachine machine, String name)
            throws Exception {
        ReferenceActuator a = (ReferenceActuator) machine.getActuatorByName(name);
        if (a == null) {
            a = new ReferenceActuator();
            a.setName(name);
            machine.addActuator(a);
        }
        return a;
    }

    public void createMachineObjects() throws Exception {
        ReferenceMachine machine = (ReferenceMachine) Configuration.get().getMachine();
        ReferenceHead head = (ReferenceHead) machine.getDefaultHead();

        // Default serial port for HWGC controller (Exar XR21V1410 USB-UART)
        if (getPortName() == null || getPortName().isEmpty()) {
            setPortName("COM23");
            setBaud(460800);
        }

        for (int i = 1; i <= nozzleCount; i++) {
            String nid = "N" + i;
            ReferenceNozzle n = (ReferenceNozzle) head.getNozzle(nid);
            if (n == null) {
                n = new ReferenceNozzle(nid);
                n.setName(nid);
                head.addNozzle(n);
            }
            n.setVacuumActuator(getOrCreateActuatorInHead(head, "N" + i + "-Vacuum"));
            n.setBlowOffActuator(getOrCreateActuatorInHead(head, "N" + i + "-Blow"));
            n.setAligningRotationMode(true);
        }

        // Pump actuator (no-op, suppresses warning — HWGC has per-nozzle vacuum control)
        if (head.getPumpActuator() == null) {
            ReferenceActuator pump = getOrCreateActuatorInHead(head, "Pump");
            head.setPumpActuator(pump);
        }

        // Machine-level actuators
        for (String actName : new String[] {
                "Lights-Down", "Lights-Up", "Mark-LED", "Mark-LED-Ill",
                "Fast-Cam-1-Light", "Fast-Cam-2-Light", "Fast-Cam-3-Light", "Fast-Cam-4-Light",
                "Buzzer", "InBoard", "OutBoard", "Clamp", "Unclamp",
                "Track-Wider", "Track-Narrower"}) {
            getOrCreateMachineActuator(machine, actName);
        }

        // Cameras
        createMarkCamera(machine, head);
        createFastCameras(machine);

        // Nozzle tips: N1=503, N2=503, N3=504, N4=504
        createNozzleTips(machine, head);

        // Head park position: X=100, Y=472, Z=0, C=0
        head.setParkLocation(new Location(LengthUnit.Millimeters, 100, 472, 0, 0));

        // Homing and calibration fiducial locations
        Location fidLoc = new Location(LengthUnit.Millimeters, 274.480, 385.700, 0, 0);
        Location homingFid = head.getHomingFiducialLocation();
        if (homingFid == null || (homingFid.getX() == 0 && homingFid.getY() == 0)) {
            head.setHomingFiducialLocation(fidLoc);
        }
        Location calPrimary = head.getCalibrationPrimaryFiducialLocation();
        if (calPrimary == null || (calPrimary.getX() == 0 && calPrimary.getY() == 0)) {
            head.setCalibrationPrimaryFiducialLocation(fidLoc);
        }
        // Secondary calibration fiducial — 25mm higher than primary
        Location calSecondary = head.getCalibrationSecondaryFiducialLocation();
        if (calSecondary == null || (calSecondary.getX() == 0 && calSecondary.getY() == 0)) {
            head.setCalibrationSecondaryFiducialLocation(
                    new Location(LengthUnit.Millimeters, 155.080, 385.500, 0, 0));
        }

        // Wire axes to nozzles and cameras (only if axes exist and aren't yet assigned)
        assignAxesToHeadMountables(machine, head);
    }

    // ──────────────────────────────────────────
    //  Auto-create machine objects
    // ──────────────────────────────────────────

    private void createMarkCamera(ReferenceMachine machine, ReferenceHead head) throws Exception {
        String camName = "Mark-Cam";
        HwgcDvrCamera existing = null;
        for (Camera c : head.getCameras()) {
            if (camName.equals(c.getName())) {
                existing = (HwgcDvrCamera) c;
                break;
            }
        }
        if (existing == null) {
            existing = new HwgcDvrCamera();
            existing.setName(camName);
            existing.setChannel(4);
            existing.setLooking(Camera.Looking.Down);
            head.addCamera(existing);
            Logger.info("HWGC: created head camera {} (DVR ch4, down-looking)", camName);
        }
        // Assign Mark-LED-Ill as camera light actuator
        if (existing.getLightActuator() == null) {
            Actuator ledIll = machine.getActuatorByName("Mark-LED-Ill");
            if (ledIll != null) {
                existing.setLightActuator(ledIll);
            }
        }
        existing.setSuspendPreviewInTasks(true);
        existing.setAutoVisible(true);
    }

    private void createFastCameras(ReferenceMachine machine) throws Exception {
        for (int i = 0; i < nozzleCount; i++) {
            String camName = "Fast-Cam-" + (i + 1);
            HwgcDvrCamera cam = null;
            for (Camera c : machine.getCameras()) {
                if (camName.equals(c.getName())) {
                    cam = (HwgcDvrCamera) c;
                    break;
                }
            }
            if (cam == null) {
                cam = new HwgcDvrCamera();
                cam.setName(camName);
                cam.setChannel(i);
                cam.setLooking(Camera.Looking.Up);
                machine.addCamera(cam);
                Logger.info("HWGC: created machine camera {} (DVR ch{}, up-looking)", camName, i);
            }
            // Assign per-camera light actuator (LED channels 0-3)
            if (cam.getLightActuator() == null) {
                Actuator light = machine.getActuatorByName("Fast-Cam-" + (i + 1) + "-Light");
                if (light != null) {
                    cam.setLightActuator(light);
                }
            }
            cam.setSuspendPreviewInTasks(true);
            cam.setAutoVisible(true);
        }
    }

    /** Nozzle tip assignments: N1=503, N2=503, N3=504, N4=504 */
    private static final String[] NOZZLE_TIP_NAMES = {"503", "503", "504", "504"};

    private void createNozzleTips(ReferenceMachine machine, ReferenceHead head) throws Exception {
        // Ensure nozzle tip types exist
        for (String tipName : new String[] {"503", "504"}) {
            if (machine.getNozzleTip(tipName) == null) {
                boolean exists = false;
                for (NozzleTip nt : machine.getNozzleTips()) {
                    if (tipName.equals(nt.getName())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    ReferenceNozzleTip nt = new ReferenceNozzleTip();
                    nt.setName(tipName);
                    machine.addNozzleTip(nt);
                    Logger.info("HWGC: created nozzle tip {}", tipName);
                }
            }
        }

        // Assign tips to nozzles and load them
        for (int i = 0; i < nozzleCount; i++) {
            String nid = "N" + (i + 1);
            ReferenceNozzle n = (ReferenceNozzle) head.getNozzle(nid);
            if (n == null) {
                continue;
            }

            String tipName = NOZZLE_TIP_NAMES[i];
            // Find the nozzle tip by name
            ReferenceNozzleTip tip = null;
            for (NozzleTip nt : machine.getNozzleTips()) {
                if (tipName.equals(nt.getName())) {
                    tip = (ReferenceNozzleTip) nt;
                    break;
                }
            }
            if (tip == null) {
                continue;
            }

            // Add to compatible list if not already there
            if (!n.getCompatibleNozzleTips().contains(tip)) {
                n.addCompatibleNozzleTip(tip);
            }
            // Load the tip onto the nozzle if none loaded
            if (n.getNozzleTip() == null) {
                n.setNozzleTip(tip);
                Logger.info("HWGC: assigned nozzle tip {} to {}", tipName, nid);
            }
        }
    }

    private static final String[] Z_LETTERS = {"Z", "U", "V", "W"};
    private static final String[] A_LETTERS = {"A", "B", "C", "D"};

    @Override
    protected void createAxisMappingDefaults(ReferenceMachine machine) throws Exception {
        if (machine.getAxes().size() != 0) {
            return;
        }

        // Shared X and Y
        ReferenceControllerAxis axisX = createControllerAxis(machine, Axis.Type.X, "X", "X");
        axisX.setFeedratePerSecond(new Length(500, LengthUnit.Millimeters));
        axisX.setAccelerationPerSecond2(new Length(2000, LengthUnit.Millimeters));
        axisX.setJerkPerSecond3(new Length(10000, LengthUnit.Millimeters));

        ReferenceControllerAxis axisY = createControllerAxis(machine, Axis.Type.Y, "Y", "Y");
        // Y is inverted: machine Y=0 is at home (top-left), OpenPnP Y=0 is at front.
        // After homing (machine Y=0), OpenPnP Y = maxY/scaleY.
        axisY.setHomeCoordinate(new Length(maxY / scaleY, LengthUnit.Millimeters));
        axisY.setFeedratePerSecond(new Length(500, LengthUnit.Millimeters));
        axisY.setAccelerationPerSecond2(new Length(2000, LengthUnit.Millimeters));
        axisY.setJerkPerSecond3(new Length(10000, LengthUnit.Millimeters));

        // Per-nozzle Z axes (Z, U, V, W) — home at 0 (retracted)
        for (int i = 0; i < nozzleCount; i++) {
            ReferenceControllerAxis axisZ = createControllerAxis(machine,
                    Axis.Type.Z, "z-N" + (i + 1), Z_LETTERS[i]);
            axisZ.setHomeCoordinate(new Length(0, LengthUnit.Millimeters));
            axisZ.setSafeZoneLowEnabled(true);
            axisZ.setSafeZoneLow(new Length(0, LengthUnit.Millimeters));
            axisZ.setSafeZoneHighEnabled(true);
            axisZ.setSafeZoneHigh(new Length(0, LengthUnit.Millimeters));
            axisZ.setFeedratePerSecond(new Length(200, LengthUnit.Millimeters));
            axisZ.setAccelerationPerSecond2(new Length(1000, LengthUnit.Millimeters));
            axisZ.setJerkPerSecond3(new Length(5000, LengthUnit.Millimeters));
        }

        // Per-nozzle Rotation axes (A, B, C, D) — home at 0 degrees
        for (int i = 0; i < nozzleCount; i++) {
            ReferenceControllerAxis axisRot = createControllerAxis(machine,
                    Axis.Type.Rotation, "rotation-N" + (i + 1), A_LETTERS[i]);
            axisRot.setHomeCoordinate(new Length(0, LengthUnit.Millimeters));
            axisRot.setLimitRotation(true);
            axisRot.setWrapAroundRotation(true);
            axisRot.setFeedratePerSecond(new Length(400, LengthUnit.Millimeters));
            axisRot.setAccelerationPerSecond2(new Length(2000, LengthUnit.Millimeters));
            axisRot.setJerkPerSecond3(new Length(10000, LengthUnit.Millimeters));
        }

        Logger.info("HWGC: created {} axes for SMT550 configuration", machine.getAxes().size());
    }

    private ReferenceControllerAxis createControllerAxis(ReferenceMachine machine,
            Axis.Type type, String name, String letter) throws Exception {
        ReferenceControllerAxis axis = new ReferenceControllerAxis();
        axis.setType(type);
        axis.setName(name);
        axis.setLetter(letter);
        axis.setDriver(this);
        machine.addAxis(axis);
        return axis;
    }

    private void assignAxesToHeadMountables(ReferenceMachine machine, ReferenceHead head)
            throws Exception {
        // Enforce safe zones on all Z axes every time (covers existing configs)
        for (int i = 0; i < nozzleCount; i++) {
            ReferenceControllerAxis axisZ = findAxisByLetter(machine, Z_LETTERS[i]);
            if (axisZ != null) {
                if (!axisZ.isSafeZoneLowEnabled()) {
                    axisZ.setSafeZoneLowEnabled(true);
                    axisZ.setSafeZoneLow(new Length(0, LengthUnit.Millimeters));
                }
                if (!axisZ.isSafeZoneHighEnabled()) {
                    axisZ.setSafeZoneHighEnabled(true);
                    axisZ.setSafeZoneHigh(new Length(0, LengthUnit.Millimeters));
                }
            }
        }

        ReferenceControllerAxis axisX = findAxisByLetter(machine, "X");
        ReferenceControllerAxis axisY = findAxisByLetter(machine, "Y");
        if (axisX == null || axisY == null) {
            return;
        }

        // Assign axes to nozzles
        for (int i = 0; i < nozzleCount; i++) {
            String nid = "N" + (i + 1);
            ReferenceNozzle n = (ReferenceNozzle) head.getNozzle(nid);
            if (n == null) {
                continue;
            }
            AbstractHeadMountable hm = (AbstractHeadMountable) n;
            if (hm.getAxisX() == null) {
                hm.setAxisX(axisX);
            }
            if (hm.getAxisY() == null) {
                hm.setAxisY(axisY);
            }
            if (hm.getAxisZ() == null) {
                ReferenceControllerAxis axisZ = findAxisByLetter(machine, Z_LETTERS[i]);
                if (axisZ != null) {
                    hm.setAxisZ(axisZ);
                }
            }
            if (hm.getAxisRotation() == null) {
                ReferenceControllerAxis axisRot = findAxisByLetter(machine, A_LETTERS[i]);
                if (axisRot != null) {
                    hm.setAxisRotation(axisRot);
                }
            }
        }

        // Head-mounted cameras (mark cam): shared X/Y + virtual Z/Rotation
        for (Camera cam : head.getCameras()) {
            AbstractHeadMountable hm = (AbstractHeadMountable) cam;
            if (hm.getAxisX() == null) {
                hm.setAxisX(axisX);
            }
            if (hm.getAxisY() == null) {
                hm.setAxisY(axisY);
            }
            assignCameraVirtualAxes(machine, cam);
        }

        // Machine-mounted cameras (fast cams): all-virtual axes
        for (Camera cam : machine.getCameras()) {
            if (cam instanceof HwgcDvrCamera) {
                assignBottomCameraVirtualAxes(machine, cam);
            }
        }
    }

    private void assignBottomCameraVirtualAxes(ReferenceMachine machine, Camera cam)
            throws Exception {
        AbstractHeadMountable hm = (AbstractHeadMountable) cam;
        if (hm.getAxisX() == null) {
            ReferenceVirtualAxis vx = new ReferenceVirtualAxis();
            vx.setType(Axis.Type.X);
            vx.setName("virt-x-" + cam.getName());
            machine.addAxis(vx);
            hm.setAxisX(vx);
        }
        if (hm.getAxisY() == null) {
            ReferenceVirtualAxis vy = new ReferenceVirtualAxis();
            vy.setType(Axis.Type.Y);
            vy.setName("virt-y-" + cam.getName());
            machine.addAxis(vy);
            hm.setAxisY(vy);
        }
        assignCameraVirtualAxes(machine, cam);
    }

    private ReferenceControllerAxis findAxisByLetter(ReferenceMachine machine, String letter) {
        for (Axis axis : machine.getAxes()) {
            if (axis instanceof ReferenceControllerAxis) {
                ReferenceControllerAxis ca = (ReferenceControllerAxis) axis;
                if (letter.equals(ca.getLetter()) && ca.getDriver() == this) {
                    return ca;
                }
            }
        }
        return null;
    }

    // ──────────────────────────────────────────
    //  Connection lifecycle
    // ──────────────────────────────────────────

    public synchronized void connect() throws Exception {
        createMachineObjects();

        getCommunications().setDriverName(getName());
        getCommunications().connect();

        connected = false;

        // Read firmware version to verify communication
        write(READ_VERSION);
        byte[] version = readResponse(timeoutMilliseconds, -1);
        if (version != null && version.length >= RTN_PACKET_LEN) {
            Logger.info("HWGC firmware version: {} {} {} {} {}",
                    String.format("%02X", version[0] & 0xFF),
                    String.format("%02X", version[1] & 0xFF),
                    String.format("%02X", version[2] & 0xFF),
                    String.format("%02X", version[3] & 0xFF),
                    String.format("%02X", version[4] & 0xFF));
        }

        // Set travel limits — required before coordinate moves work
        sendMaxXYDistance(maxX, maxY);
        sendMaxZDistance(maxZ);

        connected = true;
        Logger.info("HWGC driver connected");
    }

    @Override
    public void setEnabled(boolean enabled) throws Exception {
        if (enabled && !connected) {
            connect();
        }
    }

    public synchronized void disconnect() {
        connected = false;
        homed = false;
        try {
            getCommunications().disconnect();
        }
        catch (Exception e) {
            Logger.error("disconnect()", e);
        }
    }

    @Override
    public void close() throws IOException {
        disconnect();
    }

    // ──────────────────────────────────────────
    //  Low-level serial I/O
    // ──────────────────────────────────────────

    private void sendCommand(byte[] cmd) throws Exception {
        if (cmd.length != CMD_PACKET_LEN) {
            throw new IllegalArgumentException("Command must be " + CMD_PACKET_LEN + " bytes");
        }

        StringBuilder sb = new StringBuilder();
        for (byte b : cmd) {
            sb.append(String.format("%02X ", b & 0xFF));
        }
        Logger.trace("HWGC TX: {}", sb.toString().trim());

        for (byte b : cmd) {
            getCommunications().write(b & 0xFF);
        }
    }

    private void write(int cmdByte) throws Exception {
        byte[] cmd = new byte[CMD_PACKET_LEN];
        cmd[CMD_BYTE_INDEX] = (byte) cmdByte;
        sendCommand(cmd);
    }

    /**
     * Drain any stale bytes sitting in the serial receive buffer.
     */
    private void flushInput() {
        try {
            while (true) {
                getCommunications().read();
            }
        }
        catch (Exception e) {
            // Expected — buffer is empty
        }
    }

    private byte[] readResponse(int timeoutMs, int expectedEcho) throws Exception {
        byte[] buf = new byte[RTN_PACKET_LEN];
        long deadline = System.currentTimeMillis() + timeoutMs;
        int pos = 0;
        while (pos < RTN_PACKET_LEN) {
            if (System.currentTimeMillis() > deadline) {
                if (pos == 0) {
                    return null;
                }
                break;
            }
            try {
                int b = getCommunications().read();
                // If we're at byte 0, validate the echo byte matches the command.
                // If it doesn't match, this is a stale/misaligned byte — skip it.
                if (pos == 0 && expectedEcho >= 0 && (b & 0xFF) != (expectedEcho & 0xFF)) {
                    Logger.trace("HWGC RX: skipping stale byte {:02X} (expected {:02X})",
                            b & 0xFF, expectedEcho & 0xFF);
                    continue;
                }
                buf[pos++] = (byte) (b & 0xFF);
            }
            catch (TimeoutException e) {
                // Continue until deadline
            }
        }

        if (pos > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < pos; i++) {
                sb.append(String.format("%02X ", buf[i] & 0xFF));
            }
            Logger.trace("HWGC RX: {}", sb.toString().trim());
        }

        return pos >= RTN_PACKET_LEN ? buf : null;
    }

    private byte[] sendAndReceive(byte[] cmd, int timeoutMs) throws Exception {
        int expectedEcho = cmd[CMD_BYTE_INDEX] & 0xFF;
        for (int attempt = 0; attempt < 3; attempt++) {
            if (attempt > 0) {
                // Flush stale data before retry
                flushInput();
                Thread.sleep(100);
            }
            sendCommand(cmd);
            byte[] response = readResponse(timeoutMs, expectedEcho);
            if (response != null) {
                return response;
            }
            Logger.warn("HWGC: no response on attempt {}, retrying...", attempt + 1);
        }
        return null;
    }

    // ──────────────────────────────────────────
    //  Position query
    // ──────────────────────────────────────────

    /**
     * Query current XY position from the controller.
     * Updates cached posX/posY and returns int[]{x, y}.
     */
    private int[] readCurrentXY() throws Exception {
        byte[] cmd = new byte[CMD_PACKET_LEN];
        cmd[CMD_BYTE_INDEX] = (byte) READ_XY_COORD;
        byte[] resp = sendAndReceive(cmd, timeoutMilliseconds);
        if (resp == null) {
            throw new IOException("HWGC: no response to XY position query");
        }
        int[] xy = decodeXY(resp);
        // Sanity check: decoded values must be within machine limits
        if (xy[0] < 0 || xy[0] > maxX * 2 || xy[1] < 0 || xy[1] > maxY * 2) {
            Logger.warn("HWGC: bogus XY position {},{} (limits {},{}) — using cached {},{} ",
                    xy[0], xy[1], maxX, maxY, posX, posY);
            return new int[] {posX, posY};
        }
        posX = xy[0];
        posY = xy[1];
        return xy;
    }

    /**
     * Query current Z position for a nozzle pair (0 = noz1+2, 1 = noz3+4).
     * Updates cached posZ and returns the value.
     * <p>
     * NOTE: READ_Z_COORD (0xE1) does NOT generate a response from the controller.
     * Instead, Z positions are accessed via READ_A_COORD (0xE2) with a combined index:
     *   index 0 = Z pair 0, index 1 = Z pair 1,
     *   index 2 = A nozzle 0, index 3 = A nozzle 1, ...
     */
    private int readCurrentZ(int nozzlePair) throws Exception {
        byte[] cmd = new byte[CMD_PACKET_LEN];
        cmd[CMD_BYTE_INDEX] = (byte) READ_A_COORD;  // E2, not E1!
        cmd[4] = (byte) nozzlePair;  // index 0 or 1 = Z pair
        byte[] resp = sendAndReceive(cmd, timeoutMilliseconds);
        if (resp == null) {
            throw new IOException("HWGC: no response to Z position query");
        }
        int z = decode32LE(resp);
        // Sanity check: Z must be within ±2× machine limit
        if (z < -maxZ * 2 || z > maxZ * 2) {
            Logger.warn("HWGC: bogus Z position {} for pair {} (limit ±{}) — using cached {}",
                    z, nozzlePair, maxZ * 2, posZ[nozzlePair * 2]);
            return posZ[nozzlePair * 2];
        }
        posZ[nozzlePair * 2] = z;
        posZ[nozzlePair * 2 + 1] = z;
        return z;
    }

    /**
     * Query current rotation position for a nozzle (0-based).
     * Updates cached posA and returns the value.
     * <p>
     * The READ_A_COORD (0xE2) combined index maps:
     *   0,1 = Z pairs; 2,3,4,5 = A nozzles 0-3.
     * So nozzle N reads at index N+2.
     */
    private int readCurrentA(int nozzle) throws Exception {
        byte[] cmd = new byte[CMD_PACKET_LEN];
        cmd[CMD_BYTE_INDEX] = (byte) READ_A_COORD;
        cmd[4] = (byte) (nozzle + 2);  // offset by 2: indices 0-1 are Z pairs
        byte[] resp = sendAndReceive(cmd, timeoutMilliseconds);
        if (resp == null) {
            throw new IOException("HWGC: no response to A position query");
        }
        int a = decode32LE(resp);
        // Sanity check: rotation range is roughly ±360° × |scaleA|. Use
        // abs() because scaleA may be negative to flip rotation direction —
        // without this, every read trips the bogus check and the motion
        // completion loop spins on stale cached positions until timeout.
        int maxA = (int) (360 * Math.abs(scaleA) * 2);
        if (a < -maxA || a > maxA) {
            Logger.warn("HWGC: bogus A position {} for nozzle {} (limit ±{}) — using cached {}",
                    a, nozzle, maxA, posA[nozzle]);
            return posA[nozzle];
        }
        posA[nozzle] = a;
        return a;
    }

    // ──────────────────────────────────────────
    //  Protocol commands
    // ──────────────────────────────────────────

    private void sendMaxXYDistance(int mX, int mY) throws Exception {
        byte[] cmd = new byte[CMD_PACKET_LEN];
        cmd[CMD_BYTE_INDEX] = (byte) SET_MAX_XY;
        encodeXY(cmd, mX, mY);
        cmd[5] = (byte) comType;
        sendCommand(cmd);
    }

    private void sendMaxZDistance(int mZ) throws Exception {
        byte[] cmd = new byte[CMD_PACKET_LEN];
        cmd[CMD_BYTE_INDEX] = (byte) SET_MAX_Z;
        cmd[0] = (byte) mZ;
        cmd[1] = (byte) (mZ >> 8);
        cmd[2] = (byte) (mZ >> 16);
        cmd[3] = (byte) (mZ >> 24);
        sendCommand(cmd);
    }

    private void moveXyMachineUnits(int x, int y, int speed) throws Exception {
        byte[] cmd = new byte[CMD_PACKET_LEN];
        cmd[CMD_BYTE_INDEX] = (byte) XY_COORDINATE;
        encodeXY(cmd, x, y);
        cmd[5] = encodeSpeed(speed);
        sendCommand(cmd);
        posX = x;
        posY = y;
    }

    private void moveZMachineUnits(int nozzle, int z, int speed) throws Exception {
        // Skip if motor pair is already at the target — prevents duplicate
        // commands to the same physical motor (firmware locks up otherwise).
        if (posZ[nozzle] == z) {
            return;
        }
        byte[] cmd = new byte[CMD_PACKET_LEN];
        cmd[CMD_BYTE_INDEX] = (byte) Z_COORDINATE;
        cmd[0] = (byte) z;
        cmd[1] = (byte) (z >> 8);
        cmd[2] = (byte) (z >> 16);
        cmd[3] = (byte) (z >> 24);
        cmd[4] = (byte) (nozzle / 2);
        cmd[5] = encodeSpeed(speed);
        sendCommand(cmd);
        posZ[nozzle] = z;
        // Shared motor: update partner nozzle's tracked position (same raw motor value)
        int partner = (nozzle % 2 == 0) ? nozzle + 1 : nozzle - 1;
        if (partner >= 0 && partner < nozzleCount) {
            posZ[partner] = z;
        }
    }

    private void moveAMachineUnits(int nozzle, int a, int speed) throws Exception {
        byte[] cmd = new byte[CMD_PACKET_LEN];
        cmd[CMD_BYTE_INDEX] = (byte) A_COORDINATE;
        cmd[0] = (byte) a;
        cmd[1] = (byte) (a >> 8);
        cmd[2] = (byte) (a >> 16);
        cmd[3] = (byte) (a >> 24);
        cmd[4] = (byte) nozzle;
        cmd[5] = encodeSpeed(speed);
        sendCommand(cmd);
        posA[nozzle] = a;
    }

    private void sendVacuum(int nozzle, boolean on) throws Exception {
        byte[] cmd = new byte[CMD_PACKET_LEN];
        cmd[CMD_BYTE_INDEX] = (byte) VACUUM_SWITCH;
        cmd[4] = (byte) nozzle;
        cmd[5] = (byte) (on ? 1 : 0);
        sendCommand(cmd);
    }

    private void sendBreakVacuum(int nozzle) throws Exception {
        byte[] cmd = new byte[CMD_PACKET_LEN];
        cmd[CMD_BYTE_INDEX] = (byte) BREAK_VACUUM;
        int delayInt = (int) (blowDurationSec * 10.0);
        cmd[2] = (byte) (delayInt & 0xFF);
        cmd[3] = (byte) ((delayInt >> 8) & 0xFF);
        cmd[4] = (byte) nozzle;
        sendCommand(cmd);
    }

    private void sendLedLevel(int ledNo, int level) throws Exception {
        byte[] cmd = new byte[CMD_PACKET_LEN];
        cmd[CMD_BYTE_INDEX] = (byte) LEVEL_LED;
        cmd[0] = (byte) level;
        cmd[1] = (byte) (level >> 8);
        cmd[2] = (byte) (level >> 16);
        cmd[3] = (byte) (level >> 24);
        cmd[4] = (byte) ledNo;
        sendCommand(cmd);
    }

    private void sendMarkLed(boolean on) throws Exception {
        byte[] cmd = new byte[CMD_PACKET_LEN];
        cmd[CMD_BYTE_INDEX] = (byte) MARK_LED;
        cmd[5] = (byte) (on ? 1 : 0);
        sendCommand(cmd);
    }

    private void sendMarkLedIll(boolean on) throws Exception {
        byte[] cmd = new byte[CMD_PACKET_LEN];
        cmd[CMD_BYTE_INDEX] = (byte) MARK_LED_ILL;
        cmd[5] = (byte) (on ? 1 : 0);
        sendCommand(cmd);
    }

    private void sendBuzzer(boolean on) throws Exception {
        byte[] cmd = new byte[CMD_PACKET_LEN];
        cmd[CMD_BYTE_INDEX] = (byte) BUZZER;
        cmd[5] = (byte) (on ? 1 : 0);
        sendCommand(cmd);
    }

    /** Send feeder activate/deactivate. feederNo is 0-based. */
    public void sendFeeder(int feederNo, boolean on) throws Exception {
        byte[] cmd = new byte[CMD_PACKET_LEN];
        cmd[CMD_BYTE_INDEX] = (byte) FEEDER_SWITCH;
        cmd[4] = (byte) feederNo;
        cmd[5] = (byte) (on ? 1 : 0);
        sendCommand(cmd);
    }

    private void sendStopMove() throws Exception {
        byte[] cmd = new byte[CMD_PACKET_LEN];
        cmd[0] = 1;
        cmd[CMD_BYTE_INDEX] = (byte) STOP_MOVE;
        sendCommand(cmd);
    }

    // Board handling

    /**
     * Load and clamp the board via the IN_BOARD (0x30) opcode. The firmware
     * state machine drives the conveyor and automatically engages the clamp
     * solenoid when the board reaches the middle sensor — there is no
     * standalone clamp command on HW_4SG_50.
     */
    public void sendInBoard() throws Exception {
        sendInBoard(2);
    }

    /**
     * Load a board onto the conveyor via IN_BOARD (0x30).
     * @param velocity conveyor speed (1-9, default in SmtProgram is 7)
     */
    public void sendInBoard(int velocity) throws Exception {
        byte[] cmd = new byte[CMD_PACKET_LEN];
        cmd[CMD_BYTE_INDEX] = (byte) IN_BOARD;
        cmd[3] = (byte) velocity;
        sendCommand(cmd);
    }

    /**
     * Release the clamp and unload the board via the OUT_BOARD (0x31)
     * opcode.
     */
    public void sendOutBoard() throws Exception {
        sendOutBoard(2);
    }

    /**
     * Unload a board from the conveyor via OUT_BOARD (0x31).
     * @param velocity conveyor speed (1-9, default in SmtProgram is 7)
     */
    public void sendOutBoard(int velocity) throws Exception {
        byte[] cmd = new byte[CMD_PACKET_LEN];
        cmd[CMD_BYTE_INDEX] = (byte) OUT_BOARD;
        cmd[3] = (byte) velocity;
        sendCommand(cmd);
    }

    private void sendBoardClamp(boolean clamp) throws Exception {
        byte[] cmd = new byte[CMD_PACKET_LEN];
        cmd[CMD_BYTE_INDEX] = (byte) TRACK_DINGBAN;
        cmd[5] = (byte) (clamp ? 1 : 0);
        sendCommand(cmd);
    }

    /**
     * Clamp or unclamp the PCB using opcode 0x35 (Execute Plywood).
     * Decompiled from QIGN_COMMON.exe ClassLibrary1.DLL sendexetueplywood().
     * byte[4]=boardNo, byte[5]=1 clamp / 0 unclamp.
     */
    public void sendExecutePlywood(int boardNo, boolean clamp) throws Exception {
        byte[] cmd = new byte[CMD_PACKET_LEN];
        cmd[CMD_BYTE_INDEX] = (byte) EXECUTE_PLYWOOD;
        cmd[4] = (byte) boardNo;
        cmd[5] = (byte) (clamp ? 1 : 0);
        sendCommand(cmd);
    }

    /**
     * Jog conveyor track at constant speed (0x32) to widen or narrow rails.
     * On HW_4SG_50: direction 0 = widen (Track+), 1 = narrow (Track-).
     * Call {@link #sendTrackStopMove()} to stop.
     */
    public void sendTrackConstantSpeed(int direction, int speed) throws Exception {
        byte[] cmd = new byte[CMD_PACKET_LEN];
        cmd[CMD_BYTE_INDEX] = (byte) TRACK_CONST_SPEED;
        cmd[4] = (byte) direction;
        cmd[5] = (byte) (64 - speed + 1);
        sendCommand(cmd);
    }

    /** Stop conveyor track movement (0x33). */
    public void sendTrackStopMove() throws Exception {
        byte[] cmd = new byte[CMD_PACKET_LEN];
        cmd[CMD_BYTE_INDEX] = (byte) TRACK_STOP_MOVE;
        sendCommand(cmd);
    }

    // ──────────────────────────────────────────
    //  Driver interface implementation
    // ──────────────────────────────────────────

    @Override
    public void home(Machine machine) throws Exception {
        homingOffsets = new AxesLocation();

        // Retract all nozzles first
        for (int i = 0; i < nozzleCount; i++) {
            moveZMachineUnits(i, 0, SPEED_MAX);
        }
        Thread.sleep(500);

        // Send home command (all axes)
        byte[] cmd = new byte[CMD_PACKET_LEN];
        cmd[CMD_BYTE_INDEX] = (byte) RESET;
        cmd[5] = (byte) RESET_ALL;
        sendCommand(cmd);

        // The HWGC controller firmware handles the home sequence internally.
        // Poll position until it stabilizes near zero.
        Logger.info("HWGC: homing started, waiting for completion...");
        waitForHomingComplete();

        // Re-send travel limits after homing
        sendMaxXYDistance(maxX, maxY);
        sendMaxZDistance(maxZ);

        posX = 0;
        posY = 0;
        for (int i = 0; i < 4; i++) {
            posZ[i] = 0;
            posA[i] = 0;
        }

        // Store home coordinates to axes
        AxesLocation homeLocation = new AxesLocation(machine, this,
                (axis) -> (axis.getHomeCoordinate()));
        homeLocation.setToDriverCoordinates(this);

        homed = true;
        Logger.info("HWGC homing complete");
    }

    /**
     * Wait for homing to complete by polling XY position until stable near zero.
     * Homing typically takes 10-20 seconds.
     */
    private void waitForHomingComplete() throws Exception {
        long deadline = System.currentTimeMillis() + 30000;
        int stableCount = 0;
        int lastX = -1;
        int lastY = -1;

        // Initial delay to let homing start
        Thread.sleep(2000);

        while (System.currentTimeMillis() < deadline) {
            try {
                int[] xy = readCurrentXY();
                if (xy[0] == lastX && xy[1] == lastY) {
                    stableCount++;
                    if (stableCount >= 3) {
                        Logger.debug("HWGC: homing position stable at X={} Y={}", xy[0], xy[1]);
                        return;
                    }
                } else {
                    stableCount = 0;
                }
                lastX = xy[0];
                lastY = xy[1];
            }
            catch (Exception e) {
                Logger.trace("HWGC: position query during homing failed (normal): {}", e.getMessage());
            }
            Thread.sleep(500);
        }
        Logger.warn("HWGC: homing timeout after 30s, continuing anyway");
    }

    @Override
    public void setGlobalOffsets(Machine machine, AxesLocation location) throws Exception {
        AxesLocation newDriverLocation = location.drivenBy(this);
        AxesLocation oldDriverLocation = new AxesLocation(newDriverLocation.getAxes(this),
                (axis) -> (axis.getDriverLengthCoordinate()));
        Logger.debug("setGlobalOffsets({} -> {})", oldDriverLocation, newDriverLocation);
        homingOffsets = newDriverLocation.subtract(oldDriverLocation).add(homingOffsets);
        newDriverLocation.setToDriverCoordinates(this);
    }

    @Override
    public AxesLocation getReportedLocation(long timeout) throws Exception {
        int[] xy = readCurrentXY();
        // Read Z for each nozzle pair
        for (int pair = 0; pair < (nozzleCount + 1) / 2; pair++) {
            readCurrentZ(pair);
        }
        // Read A for each nozzle
        for (int n = 0; n < nozzleCount; n++) {
            readCurrentA(n);
        }

        // Build AxesLocation from reported positions
        ReferenceMachine machine = (ReferenceMachine) Configuration.get().getMachine();
        return new AxesLocation(machine, this, (axis) -> {
            if (axis.getType() == Axis.Type.X) {
                return new Length(posX / scaleX, units);
            } else if (axis.getType() == Axis.Type.Y) {
                // Y is inverted: machine Y=0 is at top-left (home), OpenPnP Y=0 is at front
                return new Length((maxY - posY) / scaleY, units);
            } else if (axis.getType() == Axis.Type.Z) {
                int idx = "ZUVW".indexOf(axis.getLetter());
                if (idx >= 0 && idx < nozzleCount) {
                    // Even nozzles: motor sign matches OpenPnP sign (negative = down)
                    // Odd nozzles: motor sign is inverted (positive motor = down)
                    double openpnpZ = (idx % 2 == 0) ? posZ[idx] : -posZ[idx];
                    return new Length(openpnpZ / scaleZ, units);
                }
            } else if (axis.getType() == Axis.Type.Rotation) {
                int idx = "ABCD".indexOf(axis.getLetter());
                if (idx >= 0 && idx < nozzleCount) {
                    return new Length(posA[idx] / scaleA, units);
                }
            }
            return new Length(0, units);
        });
    }

    @Override
    public void moveTo(HeadMountable hm, MoveToCommand move) throws Exception {
        if (!homed) {
            throw new Exception("HWGC driver: machine must be homed before movement");
        }

        boolean success = false;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                moveToInternal(hm, move);
                success = true;
                break;
            }
            catch (Exception e) {
                Logger.warn("HWGC moveTo attempt {} failed: {}", attempt + 1, e.getMessage());
                Thread.sleep(500);
            }
        }

        if (!success) {
            throw new IOException("HWGC moveTo failed after 3 attempts");
        }
    }

    private void moveToInternal(HeadMountable hm, MoveToCommand move) throws Exception {
        AxesLocation location1 = move.getLocation1();
        AxesLocation location0 = move.getLocation0();
        AxesLocation displacement = location0.motionSegmentTo(location1).drivenBy(this);

        // Reset move tracking
        xyMoved = false;
        for (int i = 0; i < 4; i++) {
            zMoved[i] = false;
            aMoved[i] = false;
        }

        // Compute speed from feedrate: driver reports 250 mm/s at 100% speed slider,
        // so feedRate = 250 * nominalSpeed.  Map to firmware range 1-64.
        double feedRate = move.getFeedRatePerSecond();
        int speed = (int) Math.max(1, Math.min(SPEED_MAX, feedRate / 4.0));

        // Drive rotation axes first
        for (ControllerAxis axis : displacement.byType(Axis.Type.Rotation).getControllerAxes()) {
            int index = "ABCD".indexOf(axis.getLetter());
            if (index < 0 || index >= nozzleCount) {
                throw new Exception("Invalid rotation axis letter " + axis.getLetter()
                        + " for " + axis.getName());
            }
            double degrees = location1.getCoordinate(axis, units);
            int machineA = (int) (degrees * scaleA);
            moveAMachineUnits(index, machineA, speed);
            targetA[index] = machineA;
            aMoved[index] = true;
        }

        // Drive Z axes — exactly one command per physical motor pair.
        // Motor pair 0 = N1+N2 (axes Z,U), motor pair 1 = N3+N4 (axes V,W).
        // Collect targets from all Z axes in the displacement, then send
        // at most 2 hardware commands (one per motor).
        int numPairs = (nozzleCount + 1) / 2;
        int[] pairTarget = new int[numPairs];
        boolean[] pairNeeded = new boolean[numPairs];
        int[] pairSourceNozzle = new int[numPairs]; // which nozzle triggered the command

        for (ControllerAxis axis : displacement.byType(Axis.Type.Z).getControllerAxes()) {
            int index = "ZUVW".indexOf(axis.getLetter());
            if (index < 0 || index >= nozzleCount) {
                throw new Exception("Invalid Z axis letter " + axis.getLetter()
                        + " for " + axis.getName());
            }
            int pair = index / 2;
            double mm = location1.getCoordinate(axis, units);
            // N1/N3 (even): down = negative motor direction
            // N2/N4 (odd):  down = positive motor direction
            int machineZ = (index % 2 == 0)
                    ? (int) (mm * scaleZ)
                    : (int) (-mm * scaleZ);
            targetZ[index] = machineZ;
            zMoved[index] = true;

            // First nozzle in the pair to appear wins; both map to the same
            // physical motor so the value should be consistent.
            if (!pairNeeded[pair]) {
                pairTarget[pair] = machineZ;
                pairSourceNozzle[pair] = index;
                pairNeeded[pair] = true;
            }
        }

        // Send exactly one command per motor pair that needs to move
        for (int p = 0; p < numPairs; p++) {
            if (pairNeeded[p]) {
                moveZMachineUnits(pairSourceNozzle[p], pairTarget[p], speed);
            }
        }

        // Drive XY axes
        if (displacement.getAxis(Axis.Type.X) != null
                || displacement.getAxis(Axis.Type.Y) != null) {

            double x = location1.getCoordinate(
                    location1.getAxis(this, Axis.Type.X), units);
            double y = location1.getCoordinate(
                    location1.getAxis(this, Axis.Type.Y), units);

            x -= homingOffsets.getCoordinate(homingOffsets.getAxis(Axis.Type.X));
            y -= homingOffsets.getCoordinate(homingOffsets.getAxis(Axis.Type.Y));

            int machineX = (int) (x * scaleX);
            // Y is inverted: machine Y=0 is at top-left (home), OpenPnP Y=0 is at front
            int machineY = maxY - (int) (y * scaleY);

            // Clamp to valid range to prevent wrap-around
            machineX = Math.max(0, Math.min(maxX, machineX));
            machineY = Math.max(0, Math.min(maxY, machineY));

            Logger.debug("HWGC moveXY: {},{} mm -> {},{} units, speed {}",
                    String.format("%.3f", x), String.format("%.3f", y),
                    machineX, machineY, speed);

            moveXyMachineUnits(machineX, machineY, speed);
            targetX = machineX;
            targetY = machineY;
            xyMoved = true;
        }

        location1.setToDriverCoordinates(this);
        motionPending = true;
    }

    @Override
    public boolean isMotionPending() {
        return motionPending;
    }

    @Override
    public void waitForCompletion(HeadMountable hm, CompletionType completionType)
            throws Exception {
        if (!motionPending) {
            return;
        }
        try {
            waitForMotionComplete();
        }
        finally {
            motionPending = false;
        }
    }

    /**
     * Poll position until all moved axes are within tolerance of their targets.
     */
    private void waitForMotionComplete() throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMilliseconds;
        int stallCount = 0;
        int lastSnapX = -1;
        int lastSnapY = -1;

        while (System.currentTimeMillis() < deadline) {
            boolean allDone = true;

            // Check XY
            if (xyMoved) {
                int[] xy = readCurrentXY();
                if (Math.abs(xy[0] - targetX) > motionTolerance
                        || Math.abs(xy[1] - targetY) > motionTolerance) {
                    allDone = false;
                }
                // Stall detection
                if (xy[0] == lastSnapX && xy[1] == lastSnapY && !allDone) {
                    stallCount++;
                    if (stallCount >= 10) {
                        Logger.warn("HWGC: XY stall detected at {},{} (target {},{}) — continuing",
                                xy[0], xy[1], targetX, targetY);
                        break;
                    }
                } else {
                    stallCount = 0;
                }
                lastSnapX = xy[0];
                lastSnapY = xy[1];
            }

            // Check Z (only poll axes that moved)
            for (int i = 0; i < nozzleCount; i++) {
                if (zMoved[i]) {
                    int z = readCurrentZ(i / 2);
                    if (Math.abs(z - targetZ[i]) > motionTolerance) {
                        allDone = false;
                    }
                }
            }

            // Check A
            for (int i = 0; i < nozzleCount; i++) {
                if (aMoved[i]) {
                    int a = readCurrentA(i);
                    if (Math.abs(a - targetA[i]) > motionTolerance) {
                        allDone = false;
                    }
                }
            }

            if (allDone) {
                Logger.trace("HWGC: motion complete");
                return;
            }

            Thread.sleep(motionPollIntervalMs);
        }

        Logger.warn("HWGC: motion completion timeout after {}ms", timeoutMilliseconds);
    }

    // ──────────────────────────────────────────
    //  Actuators
    // ──────────────────────────────────────────

    @Override
    public void actuate(Actuator actuator, boolean on) throws Exception {
        String name = actuator.getName();
        switch (name) {
            case ACT_N1_VACUUM:
                sendVacuum(0, on);
                if (!on) {
                    sendBreakVacuum(0);
                }
                break;
            case ACT_N2_VACUUM:
                sendVacuum(1, on);
                if (!on) {
                    sendBreakVacuum(1);
                }
                break;
            case ACT_N3_VACUUM:
                sendVacuum(2, on);
                if (!on) {
                    sendBreakVacuum(2);
                }
                break;
            case ACT_N4_VACUUM:
                sendVacuum(3, on);
                if (!on) {
                    sendBreakVacuum(3);
                }
                break;
            case ACT_N1_BLOW:
                if (on) {
                    sendBreakVacuum(0);
                }
                break;
            case ACT_N2_BLOW:
                if (on) {
                    sendBreakVacuum(1);
                }
                break;
            case ACT_N3_BLOW:
                if (on) {
                    sendBreakVacuum(2);
                }
                break;
            case ACT_N4_BLOW:
                if (on) {
                    sendBreakVacuum(3);
                }
                break;
            case "Lights-Down":
                sendLedLevel(0, on ? 420 : 0);
                break;
            case "Lights-Up":
                sendLedLevel(7, on ? 420 : 0);
                break;
            case "Fast-Cam-1-Light":
                sendLedLevel(0, on ? 420 : 0);
                break;
            case "Fast-Cam-2-Light":
                sendLedLevel(1, on ? 420 : 0);
                break;
            case "Fast-Cam-3-Light":
                sendLedLevel(2, on ? 420 : 0);
                break;
            case "Fast-Cam-4-Light":
                sendLedLevel(3, on ? 420 : 0);
                break;
            case "Mark-LED":
                sendMarkLed(on);
                break;
            case "Mark-LED-Ill":
                sendMarkLedIll(on);
                break;
            case "Buzzer":
                sendBuzzer(on);
                break;
            case "InBoard":
                if (on) {
                    sendInBoard();
                }
                break;
            case "OutBoard":
                if (on) {
                    sendOutBoard();
                }
                break;
            case "Clamp":
                sendExecutePlywood(0, true);
                break;
            case "Unclamp":
                sendExecutePlywood(0, false);
                break;
            case "Track-Wider":
                if (on) {
                    sendTrackConstantSpeed(0, 7);
                } else {
                    sendTrackStopMove();
                }
                break;
            case "Track-Narrower":
                if (on) {
                    sendTrackConstantSpeed(1, 7);
                } else {
                    sendTrackStopMove();
                }
                break;
            case "Pump":
                // HWGC machines have per-nozzle vacuum solenoids rather than
                // a central pump. OpenPnP still toggles its configured pump
                // actuator at pick start/end; silently accept so the log
                // stays clean and pick/place aren't delayed.
                break;
            default:
                Logger.warn("HWGC: unknown boolean actuator '{}'", name);
                break;
        }
    }

    @Override
    public void actuate(Actuator actuator, double value) throws Exception {
        String name = actuator.getName();
        int intVal = (int) value;
        switch (name) {
            case ACT_N1_VACUUM:
            case ACT_N2_VACUUM:
            case ACT_N3_VACUUM:
            case ACT_N4_VACUUM: {
                int nozzle = name.charAt(1) - '1';
                sendVacuum(nozzle, intVal != 0);
                break;
            }
            case "Lights-Down":
                sendLedLevel(0, intVal);
                break;
            case "Lights-Up":
                sendLedLevel(7, intVal);
                break;
            case "Fast-Cam-1-Light":
                sendLedLevel(0, intVal);
                break;
            case "Fast-Cam-2-Light":
                sendLedLevel(1, intVal);
                break;
            case "Fast-Cam-3-Light":
                sendLedLevel(2, intVal);
                break;
            case "Fast-Cam-4-Light":
                sendLedLevel(3, intVal);
                break;
            default:
                Logger.warn("HWGC: unknown double actuator '{}' = {}", name, value);
                break;
        }
    }

    @Override
    public void actuate(Actuator actuator, String value) throws Exception {
        if (value != null && value.startsWith("FEED:")) {
            int feederNo = Integer.parseInt(value.substring(5));
            sendFeeder(feederNo, true);
            Thread.sleep(200);
            sendFeeder(feederNo, false);
        }
        else {
            Logger.debug("HWGC: string actuate '{}' = '{}'", actuator.getName(), value);
        }
    }

    @Override
    public String actuatorRead(Actuator actuator) throws Exception {
        return null;
    }

    // ──────────────────────────────────────────
    //  Continuous jog (constant speed) commands
    // ──────────────────────────────────────────
    //
    // The HWGC firmware supports two motion modes:
    //   1. Goto position (0x60/0x62/0x16): move to absolute coords, used by moveTo().
    //   2. Constant speed (0x18/0x19/0x1A): move in a direction until stopped, used for jogging.
    //
    // The methods below implement mode 2. They are NOT yet called by OpenPnP because
    // the JogControlsPanel uses click-to-step (each click → moveTo with increment).
    //
    // TO INTEGRATE INTO OPENPNP UI (JogControlsPanel.java):
    //
    // 1. The jog buttons currently use ActionListener which only fires on click/release.
    //    To support hold-to-jog, add a MouseListener to each jog JButton:
    //      - mousePressed:  start continuous jog (call jogXyConstantSpeed / jogZConstantSpeed / etc.)
    //      - mouseReleased: stop movement (call jogStop())
    //
    // 2. The JogControlsPanel needs a reference to the HwgcDriver. Options:
    //    a) Add an optional interface (e.g. ContinuousJogCapable) that HwgcDriver implements,
    //       then check: if (driver instanceof ContinuousJogCapable) { use hold-to-jog }
    //       else { fall back to existing click-to-step behavior }.
    //    b) Or add jogStart/jogStop methods to the Driver SPI as default no-op methods.
    //
    // 3. Speed: the jogSpeed parameter (1-64) should be derived from the existing speed
    //    slider (JogControlsPanel.getSpeed() returns 0.0-1.0):
    //      int jogSpeed = Math.max(1, (int)(slider * SPEED_MAX));
    //
    // 4. For XY, the firmware needs repeated sends every ~50ms (see startJogRepeatLoop).
    //    For Z and A, a single send is sufficient — firmware maintains constant speed
    //    until a STOP_MOVE (0x1F) command is received on release.
    //
    // 5. Direction mapping from OpenPnP jog buttons to firmware direction values:
    //    XY: use the XY_DIR_* constants below (combined X/Y nibble encoding).
    //    Z:  POSITIVE (1) = down, REVERSE (-1) = up. Even nozzles need inversion.
    //    A:  POSITIVE (1) = CW, REVERSE (-1) = CCW.
    //
    // See java-driver HwgcTestPanel.jogHoldBtn() / zHoldBtn() / aHoldBtn() for a
    // working reference implementation of hold-to-jog with MouseListener.

    // XY jog direction constants (combined X high-nibble / Y low-nibble)
    public static final int XY_DIR_STOP              = 0x00;
    public static final int XY_DIR_Y_POSITIVE        = 0x01;
    public static final int XY_DIR_Y_REVERSE         = 0x0F;
    public static final int XY_DIR_X_POSITIVE        = 0x10;
    public static final int XY_DIR_X_POSITIVE_Y_POS  = 0x11;
    public static final int XY_DIR_X_POSITIVE_Y_REV  = 0x1F;
    public static final int XY_DIR_X_REVERSE         = 0xF0;
    public static final int XY_DIR_X_REVERSE_Y_POS   = 0xF1;
    public static final int XY_DIR_X_REVERSE_Y_REV   = 0xFF;

    // Z/A jog direction constants
    public static final int JOG_DIR_STOP     = 0;
    public static final int JOG_DIR_POSITIVE = 1;
    public static final int JOG_DIR_REVERSE  = -1;

    /** Recommended repeat interval for XY jog commands (ms). */
    public static final int JOG_REPEAT_MS = 50;

    private volatile Thread jogThread;
    private volatile Runnable activeJogAction;

    /**
     * Jog XY at constant speed in the given direction.
     * Must be called repeatedly (~50ms) while the button is held.
     * Call jogXyStop() on release.
     *
     * @param xyDir  Combined direction (XY_DIR_* constant)
     * @param speed  Speed 1-64 (map from UI slider: max(1, slider * 64))
     */
    public void jogXyConstantSpeed(int xyDir, int speed) throws Exception {
        byte[] cmd = new byte[CMD_PACKET_LEN];
        cmd[CMD_BYTE_INDEX] = (byte) XY_CONSTANT_SPEED;
        int xDir = (xyDir & 0xF0) >> 4;
        int yDir = xyDir & 0x0F;
        // Convert 15 (0x0F) to -1 (0xFF) — firmware uses signed direction bytes
        if (xDir == 15) {
            xDir = -1;
        }
        if (yDir == 15) {
            yDir = -1;
        }
        cmd[2] = (byte) xDir;
        cmd[3] = (byte) yDir;
        cmd[5] = encodeSpeed(speed);
        sendCommand(cmd);
    }

    /**
     * Stop XY constant-speed jog by sending direction = 0.
     */
    public void jogXyStop(int speed) throws Exception {
        jogXyConstantSpeed(XY_DIR_STOP, speed);
    }

    /**
     * Jog Z at constant speed. Send once on press — firmware continues until jogStop().
     * Even nozzles (0, 2) need direction inversion because they share a motor with
     * odd nozzles but are mounted on the opposite side.
     *
     * @param nozzle  Nozzle index (0-based)
     * @param dir     JOG_DIR_POSITIVE (down) or JOG_DIR_REVERSE (up)
     * @param speed   Speed 1-64
     */
    public void jogZConstantSpeed(int nozzle, int dir, int speed) throws Exception {
        // Invert direction for even nozzles (opposite side of shared motor)
        int effectiveDir = dir;
        if (nozzle % 2 == 0) {
            if (dir == JOG_DIR_POSITIVE) {
                effectiveDir = JOG_DIR_REVERSE;
            }
            else if (dir == JOG_DIR_REVERSE) {
                effectiveDir = JOG_DIR_POSITIVE;
            }
        }
        byte[] cmd = new byte[CMD_PACKET_LEN];
        cmd[CMD_BYTE_INDEX] = (byte) Z_CONSTANT_SPEED;
        cmd[3] = (byte) effectiveDir;
        cmd[4] = (byte) (nozzle / 2);
        cmd[5] = encodeSpeed(speed);
        sendCommand(cmd);
    }

    /**
     * Jog rotation (A) axis at constant speed. Send once — firmware continues until jogStop().
     *
     * @param nozzle  Nozzle index (0-based)
     * @param dir     JOG_DIR_POSITIVE (CW) or JOG_DIR_REVERSE (CCW)
     * @param speed   Speed 1-64
     */
    public void jogAConstantSpeed(int nozzle, int dir, int speed) throws Exception {
        byte[] cmd = new byte[CMD_PACKET_LEN];
        cmd[CMD_BYTE_INDEX] = (byte) A_CONSTANT_SPEED;
        cmd[3] = (byte) dir;
        cmd[4] = (byte) nozzle;
        cmd[5] = encodeSpeed(speed);
        sendCommand(cmd);
    }

    /**
     * Emergency stop all axes — call on button release to halt jog motion.
     */
    public void jogStop() throws Exception {
        stopJogRepeatLoop();
        byte[] cmd = new byte[CMD_PACKET_LEN];
        cmd[0] = 1;
        cmd[CMD_BYTE_INDEX] = (byte) STOP_MOVE;
        sendCommand(cmd);
    }

    /**
     * Start a background thread that repeatedly sends an XY jog command every
     * {@link #JOG_REPEAT_MS} ms. XY constant speed requires repeated sends to
     * maintain motion; Z and A do not (firmware keeps moving until stop).
     *
     * @param action  The jog command to repeat (e.g. () -> jogXyConstantSpeed(dir, speed))
     */
    public void startJogRepeatLoop(Runnable action) {
        stopJogRepeatLoop();
        activeJogAction = action;
        jogThread = new Thread(() -> {
            while (activeJogAction == action) {
                try {
                    action.run();
                    Thread.sleep(JOG_REPEAT_MS);
                } catch (InterruptedException ex) {
                    break;
                } catch (Exception ex) {
                    Logger.warn("HWGC jog loop error: {}", ex.getMessage());
                    break;
                }
            }
        }, "hwgc-jog-loop");
        jogThread.setDaemon(true);
        jogThread.start();
    }

    /**
     * Stop the XY jog repeat loop.
     */
    public void stopJogRepeatLoop() {
        activeJogAction = null;
        if (jogThread != null) {
            jogThread.interrupt();
            jogThread = null;
        }
    }

    // ──────────────────────────────────────────
    //  Configuration / UI
    // ──────────────────────────────────────────

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
            new PropertySheetWizardAdapter(super.getConfigurationWizard()),
            new PropertySheetWizardAdapter(new HwgcDriverConfigurationWizard(this), "HWGC")
        };
    }

    @Override
    public Length getFeedRatePerSecond() {
        return new Length(250, getUnits());
    }

    @Override
    public LengthUnit getUnits() {
        return units;
    }

    public void setUnits(LengthUnit units) {
        this.units = units;
    }

    public AxesLocation getHomingOffsets() {
        return homingOffsets;
    }

    public void setHomingOffsets(AxesLocation homingOffsets) {
        this.homingOffsets = homingOffsets;
    }

    @Deprecated
    @Override
    public void migrateDriver(Machine machine) throws Exception {
        machine.addDriver(this);
        if (machine instanceof ReferenceMachine) {
            ReferenceMachine refMachine = (ReferenceMachine) machine;
            createMachineObjects();
            createAxisMappingDefaults(refMachine);
            ReferenceHead head = (ReferenceHead) refMachine.getDefaultHead();
            assignAxesToHeadMountables(refMachine, head);
        }
    }

    @Override
    public boolean isUsingLetterVariables() {
        return true;
    }

    // ──────────────────────────────────────────
    //  Getters/setters for XML serialization
    // ──────────────────────────────────────────

    public int getTimeoutMilliseconds() { return timeoutMilliseconds; }
    public void setTimeoutMilliseconds(int v) { this.timeoutMilliseconds = v; }

    public int getComType() { return comType; }
    public void setComType(int v) { this.comType = v; }

    public int getMaxX() { return maxX; }
    public void setMaxX(int v) { this.maxX = v; }

    public int getMaxY() { return maxY; }
    public void setMaxY(int v) { this.maxY = v; }

    public int getMaxZ() { return maxZ; }
    public void setMaxZ(int v) { this.maxZ = v; }

    public int getNozzleCount() { return nozzleCount; }
    public void setNozzleCount(int v) { this.nozzleCount = v; }

    public double getScaleX() { return scaleX; }
    public void setScaleX(double v) { this.scaleX = v; }

    public double getScaleY() { return scaleY; }
    public void setScaleY(double v) { this.scaleY = v; }

    public double getScaleZ() { return scaleZ; }
    public void setScaleZ(double v) { this.scaleZ = v; }

    public double getScaleA() { return scaleA; }
    public void setScaleA(double v) { this.scaleA = v; }

    public double getBlowDurationSec() { return blowDurationSec; }
    public void setBlowDurationSec(double v) { this.blowDurationSec = v; }

    public int getMotionTolerance() { return motionTolerance; }
    public void setMotionTolerance(int v) { this.motionTolerance = v; }

    public int getMotionPollIntervalMs() { return motionPollIntervalMs; }
    public void setMotionPollIntervalMs(int v) { this.motionPollIntervalMs = v; }
}
