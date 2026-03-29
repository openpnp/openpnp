package org.openpnp.machine.neoden4;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.Arrays;

import org.openpnp.gui.support.PropertySheetWizardAdapter;
// import org.openpnp.logging.CalibrationLogger;
import org.openpnp.machine.neoden4.wizards.Neoden4DriverConfigurationWizard;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.driver.AbstractReferenceDriver;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Motion.MoveToCommand;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Axis;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MotionPlanner.CompletionType;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

import org.openpnp.util.Utils2D;

@Root
public class NeoDen4Driver extends AbstractReferenceDriver {
    // So, turns out it's just CRC16-CCITT
    // https://www.embeddedrelated.com/showthread/msp430/29689-1.php
    static short checksumLookupTable[] = {0, (short) 0x1021, (short) 0x2042, (short) 0x3063, (short) 0x4084,
            (short) 0x50A5, (short) 0x60C6, (short) 0x70E7, (short) 0x8108, (short) 0x9129,
            (short) 0x0A14A, (short) 0x0B16B, (short) 0x0C18C, (short) 0x0D1AD, (short) 0x0E1CE,
            (short) 0x0F1EF, (short) 0x1231, (short) 0x210, (short) 0x3273, (short) 0x2252,
            (short) 0x52B5, (short) 0x4294, (short) 0x72F7, (short) 0x62D6, (short) 0x9339,
            (short) 0x8318, (short) 0x0B37B, (short) 0x0A35A, (short) 0x0D3BD, (short) 0x0C39C,
            (short) 0x0F3FF, (short) 0x0E3DE, (short) 0x2462, (short) 0x3443, (short) 0x420,
            (short) 0x1401, (short) 0x64E6, (short) 0x74C7, (short) 0x44A4, (short) 0x5485,
            (short) 0x0A56A, (short) 0x0B54B, (short) 0x8528, (short) 0x9509, (short) 0x0E5EE,
            (short) 0x0F5CF, (short) 0x0C5AC, (short) 0x0D58D, (short) 0x3653, (short) 0x2672,
            (short) 0x1611, (short) 0x630, (short) 0x76D7, (short) 0x66F6, (short) 0x5695,
            (short) 0x46B4, (short) 0x0B75B, (short) 0x0A77A, (short) 0x9719, (short) 0x8738,
            (short) 0x0F7DF, (short) 0x0E7FE, (short) 0x0D79D, (short) 0x0C7BC, (short) 0x48C4,
            (short) 0x58E5, (short) 0x6886, (short) 0x78A7, (short) 0x840, (short) 0x1861,
            (short) 0x2802, (short) 0x3823, (short) 0x0C9CC, (short) 0x0D9ED, (short) 0x0E98E,
            (short) 0x0F9AF, (short) 0x8948, (short) 0x9969, (short) 0x0A90A, (short) 0x0B92B,
            (short) 0x5AF5, (short) 0x4AD4, (short) 0x7AB7, (short) 0x6A96, (short) 0x1A71,
            (short) 0x0A50, (short) 0x3A33, (short) 0x2A12, (short) 0x0DBFD, (short) 0x0CBDC,
            (short) 0x0FBBF, (short) 0x0EB9E, (short) 0x9B79, (short) 0x8B58, (short) 0x0BB3B,
            (short) 0x0AB1A, (short) 0x6CA6, (short) 0x7C87, (short) 0x4CE4, (short) 0x5CC5,
            (short) 0x2C22, (short) 0x3C03, (short) 0x0C60, (short) 0x1C41, (short) 0x0EDAE,
            (short) 0x0FD8F, (short) 0x0CDEC, (short) 0x0DDCD, (short) 0x0AD2A, (short) 0x0BD0B,
            (short) 0x8D68, (short) 0x9D49, (short) 0x7E97, (short) 0x6EB6, (short) 0x5ED5,
            (short) 0x4EF4, (short) 0x3E13, (short) 0x2E32, (short) 0x1E51, (short) 0x0E70,
            (short) 0x0FF9F, (short) 0x0EFBE, (short) 0x0DFDD, (short) 0x0CFFC, (short) 0x0BF1B,
            (short) 0x0AF3A, (short) 0x9F59, (short) 0x8F78, (short) 0x9188, (short) 0x81A9,
            (short) 0x0B1CA, (short) 0x0A1EB, (short) 0x0D10C, (short) 0x0C12D, (short) 0x0F14E,
            (short) 0x0E16F, (short) 0x1080, (short) 0x0A1, (short) 0x30C2, (short) 0x20E3,
            (short) 0x5004, (short) 0x4025, (short) 0x7046, (short) 0x6067, (short) 0x83B9,
            (short) 0x9398, (short) 0x0A3FB, (short) 0x0B3DA, (short) 0x0C33D, (short) 0x0D31C,
            (short) 0x0E37F, (short) 0x0F35E, (short) 0x2B1, (short) 0x1290, (short) 0x22F3,
            (short) 0x32D2, (short) 0x4235, (short) 0x5214, (short) 0x6277, (short) 0x7256,
            (short) 0x0B5EA, (short) 0x0A5CB, (short) 0x95A8, (short) 0x8589, (short) 0x0F56E,
            (short) 0x0E54F, (short) 0x0D52C, (short) 0x0C50D, (short) 0x34E2, (short) 0x24C3,
            (short) 0x14A0, (short) 0x481, (short) 0x7466, (short) 0x6447, (short) 0x5424,
            (short) 0x4405, (short) 0x0A7DB, (short) 0x0B7FA, (short) 0x8799, (short) 0x97B8,
            (short) 0x0E75F, (short) 0x0F77E, (short) 0x0C71D, (short) 0x0D73C, (short) 0x26D3,
            (short) 0x36F2, (short) 0x691, (short) 0x16B0, (short) 0x6657, (short) 0x7676,
            (short) 0x4615, (short) 0x5634, (short) 0x0D94C, (short) 0x0C96D, (short) 0x0F90E,
            (short) 0x0E92F, (short) 0x99C8, (short) 0x89E9, (short) 0x0B98A, (short) 0x0A9AB,
            (short) 0x5844, (short) 0x4865, (short) 0x7806, (short) 0x6827, (short) 0x18C0,
            (short) 0x8E1, (short) 0x3882, (short) 0x28A3, (short) 0x0CB7D, (short) 0x0DB5C,
            (short) 0x0EB3F, (short) 0x0FB1E, (short) 0x8BF9, (short) 0x9BD8, (short) 0x0ABBB,
            (short) 0x0BB9A, (short) 0x4A75, (short) 0x5A54, (short) 0x6A37, (short) 0x7A16,
            (short) 0x0AF1, (short) 0x1AD0, (short) 0x2AB3, (short) 0x3A92, (short) 0x0FD2E,
            (short) 0x0ED0F, (short) 0x0DD6C, (short) 0x0CD4D, (short) 0x0BDAA, (short) 0x0AD8B,
            (short) 0x9DE8, (short) 0x8DC9, (short) 0x7C26, (short) 0x6C07, (short) 0x5C64,
            (short) 0x4C45, (short) 0x3CA2, (short) 0x2C83, (short) 0x1CE0, (short) 0x0CC1,
            (short) 0x0EF1F, (short) 0x0FF3E, (short) 0x0CF5D, (short) 0x0DF7C, (short) 0x0AF9B,
            (short) 0x0BFBA, (short) 0x8FD9, (short) 0x9FF8, (short) 0x6E17, (short) 0x7E36,
            (short) 0x4E55, (short) 0x5E74, (short) 0x2E93, (short) 0x3EB2, (short) 0x0ED1,
            (short) 0x1EF0};

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

    protected boolean isAlreadyHomed = false;

    //    @Deprecated
    @Attribute(required = false)
    protected double homeCoordinateX = -437.;

    @Deprecated
    @Attribute(required = false)
    protected double homeCoordinateY = 437.; /* Maybe this needs to be 400. - needs more testing  */

    @Attribute(required = false)
    protected double scaleFactorX = 1.0501;   // slightly bigger, might be between +.0001 and +.0009

    @Attribute(required = false)
    protected double scaleFactorY = 1.04947526;

    private boolean connected;

    double backlashCompensation = 0.5;

    private AxesLocation homingOffsets = new AxesLocation();

    double[] z = {0,0,0,0,0};
    double[] c = {0,0,0,0,0};

    private int xMs = 0, yMs = 0;
    public int getXms() { return this.xMs; }
    public int getYms() { return this.yMs; }

    private boolean motionPending;

    private ReferenceActuator getOrCreateActuatorInHead(ReferenceHead head, String actuatorName) throws Exception {
        ReferenceActuator a = (ReferenceActuator) head.getActuatorByName(actuatorName);
        if (a == null) {
            a = new ReferenceActuator();
            a.setName(actuatorName);
            head.addActuator(a);
        }
        return a;
    }

    public void createMachineObjects() throws Exception {
        // Make sure required objects exist
        ReferenceMachine machine = ((ReferenceMachine) Configuration.get().getMachine());

        ReferenceNozzle n;
        ReferenceActuator a;
        ReferenceHead head = (ReferenceHead) machine.getDefaultHead();
        n = (ReferenceNozzle) head.getNozzle("N1");
        if (n == null) {
            n = new ReferenceNozzle("N1");
            n.setName("N1");
            head.addNozzle(n);
            n.setVacuumActuator(getOrCreateActuatorInHead(head, ACT_N1_VACUUM));
            n.setBlowOffActuator(getOrCreateActuatorInHead(head, ACT_N1_BLOW));
        }

        n = (ReferenceNozzle) head.getNozzle("N2");
        if (n == null) {
            n = new ReferenceNozzle("N2");
            n.setName("N2");
            head.addNozzle(n);
            n.setVacuumActuator(getOrCreateActuatorInHead(head, ACT_N2_VACUUM));
            n.setBlowOffActuator(getOrCreateActuatorInHead(head, ACT_N2_BLOW));
        }

        n = (ReferenceNozzle) head.getNozzle("N3");
        if (n == null) {
            n = new ReferenceNozzle("N3");
            n.setName("N3");
            head.addNozzle(n);
            n.setVacuumActuator(getOrCreateActuatorInHead(head, ACT_N3_VACUUM));
            n.setBlowOffActuator(getOrCreateActuatorInHead(head, ACT_N3_BLOW));
       }

        n = (ReferenceNozzle) head.getNozzle("N4");
        if (n == null) {
            n = new ReferenceNozzle("N4");
            n.setName("N4");
            head.addNozzle(n);
            n.setVacuumActuator(getOrCreateActuatorInHead(head, ACT_N4_VACUUM));
            n.setBlowOffActuator(getOrCreateActuatorInHead(head, ACT_N4_BLOW));
        }

        a = (ReferenceActuator) machine.getActuatorByName("Lights-Down");
        if (a == null) {
            a = new ReferenceActuator();
            a.setName("Lights-Down");
            machine.addActuator(a);
        }

        a = (ReferenceActuator) machine.getActuatorByName("Lights-Up");
        if (a == null) {
            a = new ReferenceActuator();
            a.setName("Lights-Up");
            machine.addActuator(a);
        }

        a = (ReferenceActuator) machine.getActuatorByName("Rails");
        if (a == null) {
            a = new ReferenceActuator();
            a.setName("Rails");
            machine.addActuator(a);
        }

        a = (ReferenceActuator) machine.getActuatorByName("ReleaseC");
        if (a == null) {
            a = new ReferenceActuator();
            a.setName("ReleaseC");
            machine.addActuator(a);
        }
        
    }

    public synchronized void connect() throws Exception {
        createMachineObjects();

        getCommunications().setDriverName(getName());
        getCommunications().connect();

        connected = false;

        // Disable the machine
        setEnabled(false);

        connected = true;
    }

    @Override
    public void setEnabled(boolean enabled) throws Exception {
        if (enabled && !connected) {
            connect();
        }
        if (connected) {
            if (enabled) {
                // TODO STOPSHIP enable
            }
            else {
                // TODO STOPSHIP disable
            }
        }
    }

    int read() throws Exception {
        return read(true);
    }
    
    int read(boolean log) throws Exception {
        while (true) {
//            try {
            int d = getCommunications().read();
            if (log) {
                Logger.trace(String.format("< %02x", d & 0xff));
            }
            return d;
//            }
//            catch (TimeoutException e) {
//                continue;
//            }
        }
    }

    void flushInput() throws Exception{
        try {
            while(true) {
                read();
            }
        }
        catch (TimeoutException e) {
        }
    }

    void write(int d) throws Exception {
        write(d, true);
    }

    void write(int d, boolean log) throws Exception {
        d = d & 0xff;
        if (log) {
            Logger.trace(String.format("> %02x", d));
        }
        getCommunications().write(d);
    }

    void writeBytes(byte[] b, boolean log) throws Exception {
        if (log) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < b.length; i++) {
                sb.append(String.format("%02x", b[i] & 0xff));
            }
            Logger.trace("> " + sb.toString());
        }

        getCommunications().writeBytes(b);
    }

    void writeWithChecksum(byte[] b) throws Exception {
        byte target[] = Arrays.copyOf(b, b.length+1);
        target[b.length] = (byte)(checksum(b) & 0xff);
        writeBytes(target, true);
    }

    byte[] readWithChecksum(int length) throws Exception {
        byte[] b = new byte[length];
        for (int i = 0; i < length; i++) {
            b[i] = (byte) (read(false) & 0xff);
        }
        int checksum = read(false);
        // TODO STOPSHIP verify checksum
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            sb.append(String.format("%02x", b[i] & 0xff));
        }
        sb.append(String.format("%02x", checksum & 0xff));
        Logger.trace("< " + sb.toString());
        return b;
    }

    int expect(int expected) throws Exception {
        int received = read() & 0xff;
        if (received != expected) {
            throw new Exception(String.format("Expected %02x but received %02x.", expected, received));
        }
        return received;
    }

    int pollFor(int command, int response) throws Exception {
    	return pollFor(command, response, 500);
    }

    /**
     * @param timeoutMs disabled if <=0 
     */
    int pollFor(int command, int response, int timeoutMs) throws Exception {
        long start = System.currentTimeMillis();
        do {
            write(command);
            
            if(System.currentTimeMillis() - start > timeoutMs && timeoutMs > 0) {
            	throw new TimeoutException("Pool for timeout");
            }
            if(command == response) {
            	throw new TimeoutException("Pool for error");
            }
            
        } while (read() != response);
        
        return response;
    }

    void putInt32(int value, byte[] buffer, int position) throws Exception {
        buffer[position + 0] = (byte) ((value >> 0) & 0xff);
        buffer[position + 1] = (byte) ((value >> 8) & 0xff);
        buffer[position + 2] = (byte) ((value >> 16) & 0xff);
        buffer[position + 3] = (byte) ((value >> 24) & 0xff);
    }

    void putInt16(int value, byte[] buffer, int position) throws Exception {
        buffer[position + 0] = (byte) ((value >> 0) & 0xff);
        buffer[position + 1] = (byte) ((value >> 8) & 0xff);
    }

    int checksum(byte[] b) {
        short result;

        if (b.length == 0) {
            return 0;
        }
        result = 0;
        for (int i = 0; i < b.length; i++) {
            short result_l = (short) (result << 8);
            short result_r = (short) (result >> 8);
            short tmp = result_l;
            tmp = (short) (checksumLookupTable[(b[i] ^ result_r) & 0xff] ^ result_l);
            result = tmp;
        }
        return result;
    }

    @Override
    public void home(Machine machine) throws Exception {
        homingOffsets = new AxesLocation();

        //comment this to force neoden home every time
        if(isAlreadyHomed) {
            return;
        }

        /* Make sure *all* nozzles are up before moving */ 
        for(int index = 1; index <= 4; index++)
        {
            moveZ(index, 0);
            moveC(index, 0);
        }

        releaseCMotors();

        /* Now, send home command */
        write(0x47);
        expect(0x0b);

        write(0xc7);
        expect(0x03);

        byte[] b = new byte[8];
        putInt32(0x01, b, 0);
        putInt32(0x00, b, 4);
        writeWithChecksum(b);
        pollFor(0x07, 0x43);

        if (! waitForStatusReady(100, 30000)) {
            throw new Exception("home timeout while waiting for status==ready");
        }

        /* Initialize coordinates correctly after home is completed */
        AxesLocation homeLocation = new AxesLocation(machine, this, (axis) -> (axis.getHomeCoordinate()));

        // Store the new location to the axes.
        homeLocation.setToDriverCoordinates(this);

        isAlreadyHomed = true;	
//        CalibrationLogger.addToLog("\n====================== HARD HOME ======================\n");    	
    }

    @Override
    public void setGlobalOffsets(Machine machine, AxesLocation location) throws Exception {
        // Take only this driver's axes.
        AxesLocation newDriverLocation = location.drivenBy(this);
        // Take the current driver location of the given axes.
        AxesLocation oldDriverLocation = new AxesLocation(newDriverLocation.getAxes(this), 
                (axis) -> (axis.getDriverLengthCoordinate()));
        Logger.debug("setGlobalOffsets("+oldDriverLocation+" -> "+newDriverLocation+")");
        // Calculate the new machine to working coordinate system offset. 
        homingOffsets = newDriverLocation.subtract(oldDriverLocation).add(homingOffsets);
        // Store to axes
        newDriverLocation.setToDriverCoordinates(this);
    }

    @Override
    public AxesLocation getReportedLocation(long timeout) throws Exception {
        // TODO: if the driver can do it, please implement. 
        throw new Exception("Not supported in this driver 2");
    }

    private double distance(double dX, double dY) {
    	Point2D.Double start = new Point2D.Double(0, 0);
    	Point2D.Double end = new Point2D.Double(dX,dY);
    	double result = Utils2D.distance(start,end);
    	return result;
    }

    private void retractNozzles() throws Exception {
        for(int index = 1; index <= 4; index++) {
            if(z[index] < 0)
            {
                moveZ(index, 0);
                this.z[index] = 0;
                Thread.sleep(300);
            }
        }
    }

    private void moveXy(double x, double y) throws Exception {
        moveStep((int) (x*scaleFactorX * 100), (int) (y*scaleFactorY * 100));
    }

    public void moveStep(int sx, int sy) throws Exception {
        write(0x48);
        expect(0x05);

        write(0xc8);
        expect(0x0d);

        byte[] b = new byte[8];
        Logger.trace(String.format("Neoden moveStep %d, %d", sx, sy));
        putInt32(sx, b, 0);
        putInt32(sy, b, 4);
        writeWithChecksum(b);
        pollFor(0x08, 0x4d);

        if (! waitForStatusReady(100, 30000)) {
            throw new Exception("moveXy timeout while waiting for status==ready");
        }

        this.xMs = sx;
        this.yMs = sy;
        Logger.trace(String.format("xMs, yMs changed to %d,%d", sx, sy));
    }

    private Boolean isStatusReady() throws Exception {
        pollFor(0x45, 0x09);
        pollFor(0x05, 0x14);

        write(0x85);
        expect(0x1c);
        byte[] b = readWithChecksum(8);

        int readyStatus = b[0];

        if (readyStatus == 0) {
            return true;
        }

        return false;
    }

    private Boolean waitForStatusReady(int sleepMilliS, int maxMilliS) throws Exception {
        int totalWaitMilliS = 0;
        do {
            Thread.sleep(sleepMilliS);
            totalWaitMilliS += sleepMilliS;

            if (totalWaitMilliS >= maxMilliS) {
                return false;
            }
        } while (! isStatusReady());
        return true;
    }

    private void moveZ(int nozzle, double z) throws Exception {
        // Neoden thinks 13.0 is max retracted into the head, 0 is max out.
        // In our world, 0 is max up and -13 is max down.

        Logger.debug("Move Z");

        z = Math.abs(z) * 1000.;

        write(0x42);
        expect(0x0e);

        write(0xc2);
        expect(0x06);

        byte[] b = new byte[8];
        putInt16((int) (z), b, 0);
        b[2] = 0x64;
        b[3] = (byte) nozzle;
        writeWithChecksum(b);
        pollFor(0x02, 0x46);
    }

    private void releaseCMotors()  throws Exception{
        moveC(0,0);

        //This will force nozzle to move on next MoveTo
        for(int index = 1; index <= 4; index++) {
            this.c[index] = 360;
        }
    }

    private void moveC(int nozzle, double c) throws Exception {
        Logger.debug("Move C");

        write(0x41);
        expect(0x0d);

        write(0xc1);
        expect(0x05);

        byte[] b = new byte[8];
        putInt16((int) (-c * 10.), b, 0);
        b[2] = 0x32;
        b[3] = (byte) nozzle;
        writeWithChecksum(b);
        pollFor(0x01, 0x45);
    }

    private void setMoveSpeed(double speed) throws Exception {
        write(0x46);
        expect(0x0a);

        write(0xc6);
        expect(0x02);

        byte[] b = new byte[8];
        // Speed is percentage of max speed.  Speed is really 10-130
        putInt16((int) ((120. * speed)+10), b, 0);
        b[2] = 0x09;
        b[4] = (byte) 0xc8;
        writeWithChecksum(b);
        pollFor(0x06, 0x42);
    }

    private void feedInternal(int id, int strength, int feedRate) throws Exception {
        write(0x3f);
        expect(0x0c);

        write(0x46+id);
        read();

        write(0xff);
        expect(0x00);

        write(0x46+id);
        read();

        byte[] b = new byte[8];
        // Speed is percentage of max speed.  Speed is really 10-130
        //putInt16((int) ((120. * speed)+10), b, 0);
        b[0] = (byte) strength;
        b[1] = (byte) feedRate;
        writeWithChecksum(b);

        write(0x3f);
        expect(0x0c);

        write(0x46+id);
        read();
        //pollFor(0x47, 0x42);
    }

    public void feed(int id, int strength, int feedRate) throws Exception {
        Logger.debug(String.format("Feed, id=%d, strength=%d, feedRate=%d", id, strength, feedRate));
        boolean success = false;
        for(int i=0; i<3; i++) {
            try {
                feedInternal(id, strength, feedRate);
                success = true;
                break;
            }
            catch (Exception e){
                Thread.sleep(1000);
                flushInput();
                Logger.warn("Recovered feed");
                Thread.sleep(1000);
            }
        }

        if(!success) {
            throw new IOException("Feed error.");
        }
    }

    private void changeFeederIdInternal(int oldId, int newId) throws Exception {
        write(0x3f);
        expect(0x0c);

        write(0x46+oldId);
        read();

        write(0xff);
        expect(0x00);

        write(0x46+oldId);
        read();

        byte[] b = new byte[8];

        b[0] = (byte) newId;
        b[7] = (byte) 0x01;
        writeWithChecksum(b);

        write(0x3f);
        expect(0x0c);

        write(0x46+oldId);
        read();
    }

    public void changeFeederId(int oldId, int newId) throws Exception {
        Logger.debug(String.format("changeFeederId, oldId=%d, newId=%d", oldId, newId));
        if((oldId < 0)||(oldId >= 100)) {
            throw new IOException("changeFeederId oldId must be between 0-99.");
        }
        else {
            if((newId < 0)||(newId >= 100)) {
                throw new IOException("changeFeederId newId must be between 0-99.");
            }
            else {
                boolean success = false;
                for(int i=0; i<3; i++) {
                    try {
                        changeFeederIdInternal(oldId, newId);
                        success = true;
                        break;
                    }
                    catch (Exception e){
                        Thread.sleep(1000);
                        flushInput();
                        Logger.warn("Recovered changeFeederId");
                        Thread.sleep(1000);
                    }
                }
                
                if(!success) {
                    throw new IOException("changeFeederId error.");
                }
            }
        }
    }
    private void peelInternal(int id, int strength, int feedRate) throws Exception {

        boolean isTopHalf = false;

        if(id >= 20) {
            isTopHalf = true;
        }
    
        if(!isTopHalf) {
            write(0x4c);
            expect(0x01);

            write(0xcc);
            expect(0x09);

            byte[] b = new byte[8];
            b[0] = (byte) id;
            b[1] = (byte) feedRate;
            b[2] = (byte) strength;
            writeWithChecksum(b);
            pollFor(0x0c, 0x49);
        }
        else {

            write(0x4e);
            expect(0x03);

            write(0xce);
            expect(0x0B);

            byte[] b = new byte[8];
            b[0] = (byte) (id-19);
            b[1] = (byte) feedRate;
            b[2] = (byte) strength;
            writeWithChecksum(b);
            pollFor(0x0E, 0x4B);
        }
    }

    public void peel(int id, int strength, int feedRate) throws Exception {
        Logger.debug(String.format("Peel, id=%d, strength=%d, feedRate=%d", id, strength, feedRate));
        boolean success = false;
        for(int i=0; i<3; i++) {
            try {
                peelInternal(id, strength, feedRate);
                success = true;
                break;
            }
            catch (Exception e){
                Thread.sleep(1000);
                flushInput();
                Logger.warn("Recovered peel");
                Thread.sleep(1000);
            }
        }

        if(!success) {
            throw new IOException("Peel error.");
        }
    }

    @Override
    public Length getFeedRatePerSecond() {
        // Default implementation for feeders that don't implement an extra feed-rate. 
        // The axes' fee-rate will be used.
        return new Length(250, getUnits());
    }

    private void moveToInternal(HeadMountable hm, MoveToCommand move) 
            throws Exception {
        boolean isDelayNeeded = false;

        AxesLocation location1 = move.getLocation1();
        AxesLocation location0 = move.getLocation0();

        // Take only the changed axes and only those driven by this driver
        AxesLocation displacement = location0.motionSegmentTo(location1)
                .drivenBy(this);

        // Drive rotation axes.
        for(ControllerAxis axis : displacement.byType(Axis.Type.Rotation)
                .getControllerAxes()){
            // map from axis letter to driver index.
            int index = 1+"ABCD".indexOf(axis.getLetter());
            if (index < 1 || index > 4) {
                throw new Exception("Invalid axis letter "+axis.getLetter()
                +" for nozzle rotation axis "+axis.getName());
            }            
            else {
                this.c[index] = location1.getCoordinate(axis, getUnits());
                moveC(index, location1.getCoordinate(axis, getUnits()));
                isDelayNeeded = true;
            }
        }

        if(isDelayNeeded) {
            Thread.sleep(100);
            isDelayNeeded = false;
        }

        // Drive Z axes
        for(ControllerAxis axis : displacement.byType(Axis.Type.Z)
                .getControllerAxes()){

            // map from axis letter to driver index.
            int index = 1+"ZUVW".indexOf(axis.getLetter());
            if (index < 1 || index > 4) {
                throw new Exception("Invalid axis letter "+axis.getLetter()
                +" for nozzle z axis "+axis.getName());
            }            
            else {
                this.z[index] = location1.getCoordinate(axis, getUnits());
                moveZ(index, location1.getCoordinate(axis, getUnits()));
                isDelayNeeded = true;
            }
        }

        if(isDelayNeeded) {
            Thread.sleep(100);
            isDelayNeeded = false;
        }

        // Drive XY axes
        double feedRate = move.getFeedRatePerSecond();
        // Reconstruct speed factor from "virtual" feed-rate assuming the default 
        // 250mm/s axes feedrate.
        // TODO: better solution than just assuming 250. 
        double speed = Math.max(0.0, Math.min(1.0, feedRate/250.0));

        double x = location1.getCoordinate(location1.getAxis(this, Axis.Type.X), units);
        double y = location1.getCoordinate(location1.getAxis(this, Axis.Type.Y), units);

        if(displacement.getAxis(Axis.Type.X) != null || displacement.getAxis(Axis.Type.Y) != null)
        {
           setMoveSpeed(speed);

           x -= homingOffsets.getCoordinate(homingOffsets.getAxis(Axis.Type.X));
           y -= homingOffsets.getCoordinate(homingOffsets.getAxis(Axis.Type.Y));

           Logger.debug(String.format("Neoden move to to %.3f,%.3f", x, y));
           moveXy(x,y);

           isDelayNeeded = true;
        }

        if(isDelayNeeded) {
           Thread.sleep(100);
        }

        // Store the new location to the axes.
        location1.setToDriverCoordinates(this);
        motionPending = true;
    }

    @Override
    public void moveTo(HeadMountable hm, MoveToCommand move)
            throws Exception {

        // Disable movement when not homed
        if(!isAlreadyHomed) {
            throw new Exception("NeoDen4Driver moveTo: Machine must be homed before movement");
        }
        
        boolean success = false;
        for(int i=0; i<3; i++) {
            try {
                moveToInternal(hm, move);
                success = true;
                break;
            }
            catch (Exception e){
                Thread.sleep(1000);
                flushInput();
                Thread.sleep(1000);
                Logger.warn("Recovered moveTo");
            }
        }
        
        if(!success) {
            throw new IOException("MoveTo error.");
        }
    }

    @Override
    public boolean isMotionPending() {
        return motionPending;
    }

    @Override
    public void waitForCompletion(HeadMountable hm, 
            CompletionType completionType) throws Exception {
        // TODO implement
        motionPending = false;
    }

    @Override
    public void actuate(Actuator actuator, boolean on) throws Exception {
        switch (actuator.getName()) {
            case ACT_N1_VACUUM:
            case ACT_N2_VACUUM:
            case ACT_N3_VACUUM:
            case ACT_N4_VACUUM: {
                if (on) {
                    actuate(actuator, -128.0);
                } else {
                    actuate(actuator, 20.0);
                    Thread.sleep(100);
                    actuate(actuator, 0.0);
                }
                break;
            }
            case ACT_N1_BLOW:
            case ACT_N2_BLOW:
            case ACT_N3_BLOW:
            case ACT_N4_BLOW: {
                if (on) {
//                    actuate(actuator, 127.0);
//                    Thread.sleep(400);
//                    actuate(actuator, 0.0);
                } else {
                }
                break;
            }
            case "Lights-Down": {
                if (on) {
                    actuate(actuator, 3.0);
                } else {
                    actuate(actuator, 0.0);
                }
              break;
            }
            case "Lights-Up": {
                if (on) {
                    actuate(actuator, 1.0);
                } else {
                    actuate(actuator, 0.0);
                }
                break;
            }
            case "Rails": {
                if (on) {
                    actuate(actuator, 25.0);
                }else{
                    actuate(actuator, 0.0);
                }
                break;
            }
            case "ReleaseC": {
                actuate(actuator, 0.0);
            break;
            }
        }
    }

    private void stopRail() throws Exception {
        byte[] b = new byte[8];
        write(0x47);
        expect(0x0b);
        write(0xc7);
        expect(0x03);

        b[0] = (byte)0x00;
        b[1] = (byte)0x02;
        b[2] = (byte)0x00;
        b[3] = (byte)0x00;
        b[4] = (byte)0x00;
        b[5] = (byte)0x00;
        b[6] = (byte)0x00;
        b[7] = (byte)0x00;

        writeWithChecksum(b);
        pollFor(0x07,  0x43);
    }

    private void forwardRail() throws Exception {
        byte[] b = new byte[8];
        write(0x49);
        expect(0x04);
        write(0xc9);
        expect(0x0c);

        b[0] = (byte)0x00;
        b[1] = (byte)0x00;
        b[2] = (byte)0x00;
        b[3] = (byte)0x00;
        b[4] = (byte)0xc9;
        b[5] = (byte)0x03;
        b[6] = (byte)0x0c;
        b[7] = (byte)0x00;
        writeWithChecksum(b);
        pollFor(0x09,  0x4c);
    }

    private void reverseRail()  throws Exception {
        byte[] b = new byte[8];
        write(0x49);
        expect(0x04);
        write(0xc9);
        expect(0x0c);

        b[0] = (byte)0x00;
        b[1] = (byte)0x00;
        b[2] = (byte)0x00;
        b[3] = (byte)0x00;
        b[4] = (byte)0x37;
        b[5] = (byte)0xfc;
        b[6] = (byte)0xf3;
        b[7] = (byte)0xff;
        writeWithChecksum(b);
        pollFor(0x09,  0x4c);
    }

    private void setRailSpeed(byte speed)  throws Exception {
        speed = (byte)Math.abs(speed);
        if (speed < 20) {
            speed = (byte)20;
        }
        else if (speed > 200) {
            speed = (byte)200;
        }

        byte[] b = new byte[8];
        write(0x46);
        expect(0x0a);
        write(0xc6);
        expect(0x02);

        b[0] = (byte)0x32;
        b[1] = (byte)0x09;
        b[2] = (byte)0x00;
        b[3] = speed;
        b[4] = (byte)0x00;
        b[5] = (byte)0x00;
        b[6] = (byte)0x00;
        b[7] = (byte)0x00;
        writeWithChecksum(b);
        pollFor(0x06,  0x42);
    }

    private void setAirParameters(int nozzleNum, double value) throws Exception {
        write(0x43);
        expect(0x0f);

        write(0xc3);
        expect(0x07);

        byte[] b = new byte[8];
        b[0] = (byte) value;
        b[1] = (byte) nozzleNum;
        writeWithChecksum(b);
        pollFor(0x03,  0x47);
    }

    public void setBuzzer(boolean state) throws Exception {
        Logger.trace(String.format("Neoden setBuzzer %b", state));
        write(0x47);
        expect(0x0b);

        write(0xc7);
        expect(0x03);

        byte[] b = new byte[8];  
        b[0] = (byte) 0x00;
        b[1] = (byte) 0x00;
        b[2] = (byte) 0x00;
        b[3] = (byte) 0x00;
        b[4] = (byte) 0x00;
        b[5] = state ? (byte) 0x01 : (byte) 0x00;
        b[6] = (byte) 0x00;
        b[7] = (byte) 0x00;

        writeWithChecksum(b);
        pollFor(0x07,  0x43);
    }

    private void actuateInternal(Actuator actuator, double value) throws Exception {
        switch (actuator.getName()) {
        case ACT_N1_BLOW:
        case ACT_N1_VACUUM: {
            setAirParameters(1, value);
            break;
        }
        case ACT_N2_BLOW:
        case ACT_N2_VACUUM: {
            setAirParameters(2, value);
            break;
        }
        case ACT_N3_BLOW:
        case ACT_N3_VACUUM: {
            setAirParameters(3, value);
            break;
        }
        case ACT_N4_BLOW:
        case ACT_N4_VACUUM: {
            setAirParameters(4, value);
            break;
        }
        case "Lights-Down": {
            write(0x44);
            expect(0x08);
            
            write(0xc4);
            expect(0x00);
            
            byte[] b = new byte[8];
            b[0] = (byte) value;
            writeWithChecksum(b);
            
            pollFor(0x04,  0x40);
          break;
        }
        case "Lights-Up": {
            write(0x47);
            expect(0x0b);
            
            write(0xc7);
            expect(0x03);
            
            byte[] b = new byte[8];
            b[4] = (byte) value;
            writeWithChecksum(b);
            
            pollFor(0x07,  0x43);
            break;
        }
        case "Rails": {
            if ((int)value == 0) {
              stopRail();
            } else if (value > 0) {
                stopRail();
                setRailSpeed((byte)value);
                forwardRail();
            }
            else {
                stopRail();
                setRailSpeed((byte)value);
                reverseRail();
            }
            
          break;
        }
        case "ReleaseC": {
        	releaseCMotors();
        	break;
        }
        }
    }

    @Override
    public void actuate(Actuator actuator, double value) throws Exception {
        Logger.trace(String.format("Neoden actuate %s, %f", actuator.getName(), value));
    	boolean success = false;
    	for(int i=0; i<3; i++) {
        	try {
        		actuateInternal(actuator, value);
        		success = true;
        		break;
        	}
        	catch (Exception e){
        		Thread.sleep(1000);
        		flushInput();
        		Logger.warn(String.format("actuate: try %d, exception %s, [%s]", i, e.toString(), actuator.toString()));
        		Thread.sleep(1000);
        	}
    	}
    	
    	if(!success) {
    		throw new IOException("Actuate error.");
    	}
    }
    
    private int getNozzleAirValue(int nozzleNum) throws Exception {
        Logger.trace(String.format("Neoden getNozzleAirValue %d", nozzleNum));
        assert (nozzleNum >= 0);
        assert (nozzleNum <= 3);

        byte[] payload = { 0, 0, 0, 0 };
        boolean success = false;

        for (int i = 0; i < 5; i++) {
            try {
                write(0x40);
                expect(0x0c);

                write(0x00);
                expect(0x11);

                write(0x80);
                expect(0x19);

                payload = readWithChecksum(8);
                success = true;
                break;
            }catch (Exception e) {
                Thread.sleep(1000);
                flushInput();
                Logger.warn("Recovered getNozzleAirValue");
                Thread.sleep(1000);
            }
        }

        if (!success) {
            throw new IOException("getNozzleAirValue error.");
        } 
        else {
            int airValue = (int) payload[nozzleNum];

            if (airValue > 110) {
                Logger.trace(String.format("Error in getNozzleAirValue! Value<-128 (%d)", airValue));
                // HACK
                // sometimes when usign small nozzletip 
                // neoden returns values smaller than -128
                // and thus the variable jumps for example from -128 to 127 
                // Let's change the variable range
                // from (-128, 127) to (-145, 110)
                airValue = -128 - (128-airValue);
            }
            return airValue;
        }
    }

    @Override
    public String actuatorRead(Actuator actuator) throws Exception {
        switch (actuator.getName()) {
            case ACT_N1_BLOW:
            case ACT_N1_VACUUM: {
                return Integer.toString(getNozzleAirValue(0));
            }
            case ACT_N2_BLOW:
            case ACT_N2_VACUUM:  {
                return Integer.toString(getNozzleAirValue(1));
            }
            case ACT_N3_BLOW:
            case ACT_N3_VACUUM:  {
                return Integer.toString(getNozzleAirValue(2));
            }
            case ACT_N4_BLOW:
            case ACT_N4_VACUUM:  {
                return Integer.toString(getNozzleAirValue(3));
            }
        }
        return null;
    }

    public synchronized void disconnect() {
        connected = false;

        try {
            getCommunications().disconnect();
        }
        catch (Exception e) {
            Logger.error("disconnect()", e);
        }
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
            new PropertySheetWizardAdapter(super.getConfigurationWizard()),
            new PropertySheetWizardAdapter(new Neoden4DriverConfigurationWizard(this), "Machine")
        };
    }

    public AxesLocation getHomingOffsets() {
        return homingOffsets;
    }

    public void setHomingOffsets(AxesLocation homingOffsets) {
        this.homingOffsets = homingOffsets;
    }

    public LengthUnit getUnits() {
        return units;
    }

    public void setUnits(LengthUnit units) {
        this.units = units;
    }

    public int getTimeoutMilliseconds() {
        return timeoutMilliseconds;
    }

    public void setTimeoutMilliseconds(int timeoutMilliseconds) {
        this.timeoutMilliseconds = timeoutMilliseconds;
    }

    public int getConnectWaitTimeMilliseconds() {
        return connectWaitTimeMilliseconds;
    }

    public void setConnectWaitTimeMilliseconds(int connectWaitTimeMilliseconds) {
        this.connectWaitTimeMilliseconds = connectWaitTimeMilliseconds;
    }

    public double getScaleFactorX() {
        return this.scaleFactorX;
    }

    public void setScaleFactorX(double scaleFactorX) {
        this.scaleFactorX = scaleFactorX;
    }

    public double getScaleFactorY() {
        return this.scaleFactorY;
    }

    public void setScaleFactorY(double scaleFactorY) {
        this.scaleFactorY = scaleFactorY;
    }

    public double getHomeCoordinateX() {
        return this.homeCoordinateX;
    }

    public void setHomeCoordinateX(double homeCoordinateX) {
        this.homeCoordinateX = homeCoordinateX;
    }

    public double getHomeCoordinateY() {
        return this.homeCoordinateY;
    }

    public void setHomeCoordinateY(double homeCoordinateY) {
        this.homeCoordinateY = homeCoordinateY;
    }    

    @Deprecated
    @Override
    public void migrateDriver(Machine machine) throws Exception {
        machine.addDriver(this);
        if (machine instanceof ReferenceMachine) {
            createAxisMappingDefaults((ReferenceMachine) machine);
            AxesLocation homeLocation = new AxesLocation(machine, this, (axis) -> ( axis.getHomeCoordinate() ));
            for (ControllerAxis axis : homeLocation.getAxes(this)) {
                if (axis.getType() == Axis.Type.X) {
                    ((ControllerAxis) axis).setHomeCoordinate(new Length(homeCoordinateX, getUnits()));
                }
                else if (axis.getType() == Axis.Type.Y) {
                    ((ControllerAxis) axis).setHomeCoordinate(new Length(homeCoordinateY, getUnits()));
                }
            }
        }
    }

    @Override
    public boolean isUsingLetterVariables() {
        return true;
    }
}
