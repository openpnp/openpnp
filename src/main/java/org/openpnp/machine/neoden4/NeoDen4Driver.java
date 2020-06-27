package org.openpnp.machine.neoden4;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.machine.neoden4.wizards.Neoden4DriverConfigurationWizard;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.driver.AbstractReferenceDriver;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Named;
import org.openpnp.spi.Movable.MoveToOption;
import org.openpnp.spi.Nozzle;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@Root
public class NeoDen4Driver extends AbstractReferenceDriver implements Named {
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
    
    @Attribute(required = false)
    protected String name = "NeoDen4Driver";
    
    @Attribute(required = false)
    protected double homeCoordinateX = -437.;
    
    @Attribute(required = false)
    protected double homeCoordinateY = 437.; /* Maybe this needs to be 400. - needs more testing  */

    @Attribute(required = false)
    protected double scaleFactorX = 1.0501;   // slightly bigger, might be between +.0001 and +.0009
    
    @Attribute(required = false)
    protected double scaleFactorY = 1.04947526;

    private boolean connected;
    private Set<Nozzle> pickedNozzles = new HashSet<>();


    double x = 0, y = 0;
    double z1 = 0, z2 = 0, z3 = 0, z4 = 0;
    double c1 = 0, c2 = 0, c3 = 0, c4 = 0;
    

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
            getOrCreateActuatorInHead(head, ACT_N1_VACUUM);
            getOrCreateActuatorInHead(head, ACT_N1_BLOW);
            n.setBlowOffActuatorName(ACT_N1_BLOW);
            n.setVacuumActuatorName(ACT_N1_VACUUM);
        }
        
        n = (ReferenceNozzle) head.getNozzle("N2");
        if (n == null) {
            n = new ReferenceNozzle("N2");
            n.setName("N2");
            head.addNozzle(n);
            getOrCreateActuatorInHead(head, ACT_N2_VACUUM);
            getOrCreateActuatorInHead(head, ACT_N2_BLOW);
            n.setBlowOffActuatorName(ACT_N2_BLOW);
            n.setVacuumActuatorName(ACT_N2_VACUUM);
        }
        
        n = (ReferenceNozzle) head.getNozzle("N3");
        if (n == null) {
            n = new ReferenceNozzle("N3");
            n.setName("N3");
            head.addNozzle(n);
            getOrCreateActuatorInHead(head, ACT_N3_VACUUM);
            getOrCreateActuatorInHead(head, ACT_N3_BLOW);
            n.setBlowOffActuatorName(ACT_N3_BLOW);
            n.setVacuumActuatorName(ACT_N3_VACUUM);
       }
        
        n = (ReferenceNozzle) head.getNozzle("N4");
        if (n == null) {
            n = new ReferenceNozzle("N4");
            n.setName("N4");
            head.addNozzle(n);
            getOrCreateActuatorInHead(head, ACT_N4_VACUUM);
            getOrCreateActuatorInHead(head, ACT_N4_BLOW);
            n.setBlowOffActuatorName(ACT_N4_BLOW);
            n.setVacuumActuatorName(ACT_N4_VACUUM);
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
    }
    
    public synchronized void connect() throws Exception {
        createMachineObjects();
        
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
            try {
                int d = getCommunications().read();
                if (log) {
                    Logger.trace(String.format("< %02x", d & 0xff));
                }
                return d;
            }
            catch (TimeoutException e) {
                continue;
            }
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
    
    void writeWithChecksum(byte[] b) throws Exception {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            sb.append(String.format("%02x", b[i] & 0xff));
        }
        sb.append(String.format("%02x", checksum(b) & 0xff));
        Logger.trace("> " + sb.toString());
        for (int i = 0; i < b.length; i++) {
            write(b[i], false);
        }
        getCommunications().write(checksum(b) & 0xff);
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
        int received = read();
        if (received != expected) {
            throw new Exception(String.format("Expected %02x but received %02x.", expected, received));
        }
        return received;
    }
    
    int pollFor(int command, int response) throws Exception {
        do {
            write(command);
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
    public void home(ReferenceHead head) throws Exception {
        /* Make sure *all* nozzles are up before moving */ 
        moveZ(1, 0);
        moveZ(2, 0);
        moveZ(3, 0);
        moveZ(4, 0);

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
        this.x = this.homeCoordinateX;
        this.y = this.homeCoordinateY;

        ReferenceMachine machine = ((ReferenceMachine) Configuration.get().getMachine());
        machine.fireMachineHeadActivity(head);
    }

    @Override
    public Location getLocation(ReferenceHeadMountable hm) {
        switch (hm.getId()) {
            case "N1":
                return new Location(units, x, y, z1, c1).add(hm.getHeadOffsets());
            case "N2":
                return new Location(units, x, y, z2, c2).add(hm.getHeadOffsets());
            case "N3":
                return new Location(units, x, y, z3, c3).add(hm.getHeadOffsets());
            case "N4":
                return new Location(units, x, y, z4, c4).add(hm.getHeadOffsets());
        }
        return new Location(units, x, y, 0, 0).add(hm.getHeadOffsets());
    }
    
    private void moveXy(double x, double y) throws Exception {
        write(0x48);
        expect(0x05);
      
        write(0xc8);
        expect(0x0d);

        byte[] b = new byte[8];
        putInt32((int) (x*scaleFactorX * 100), b, 0);
        putInt32((int) (y*scaleFactorY * 100), b, 4);
        writeWithChecksum(b);
      
        pollFor(0x08, 0x4d);

        if (! waitForStatusReady(100, 30000)) {
            throw new Exception("moveXy timeout while waiting for status==ready");
        }
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
    
    private void moveC(int nozzle, double c) throws Exception {
        write(0x41);
        expect(0x0d);
        
        write(0xc1);
        expect(0x05);

        byte[] b = new byte[8];
        putInt16((int) (c * 10.), b, 0);
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

    @Override
    public void moveTo(ReferenceHeadMountable hm, Location location, double speed, MoveToOption...options)
            throws Exception {
        location = location.convertToUnits(units);
        location = location.subtract(hm.getHeadOffsets());

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        double c = location.getRotation();

        // Handle NaNs, which means don't move this axis for this move. We just copy the existing
        // coordinate.
        x = Double.isNaN(x) ? this.x : x;
        y = Double.isNaN(y) ? this.y : y;
        if (x != this.x || y != this.y) {
            setMoveSpeed(speed);
            moveXy(x, y);
            
            this.x = x;
            this.y = y;
        }

        double minZ = 0.;
        double maxZ = -13.;

        switch (hm.getId()) {
            case "N1":
                z = Double.isNaN(z) ? this.z1 : z;
                z = Math.min(z, minZ);
                z = Math.max(z, maxZ);
                if (z != this.z1) {
                    moveZ(1, z);
                    this.z1 = z;
                }
                
                c = Double.isNaN(c) ? this.c1 : c;
                c = Math.max(c, -180.);
                c = Math.min(c, 180.);                
                if (c != this.c1) {
                    moveC(1, c);
                    this.c1 = c;
                }
                break;
            case "N2":
                z = Double.isNaN(z) ? this.z2 : z;
                z = Math.min(z, minZ);
                z = Math.max(z, maxZ);
                if (z != this.z2) {
                    moveZ(2, z);
                    this.z2 = z;
                }
                
                c = Double.isNaN(c) ? this.c2 : c;
                c = Math.max(c, -180.);
                c = Math.min(c, 180.);                
                if (c != this.c2) {
                    moveC(2, c);
                    this.c2 = c;
                }
                break;
            case "N3":
                z = Double.isNaN(z) ? this.z3 : z;
                z = Math.min(z, minZ);
                z = Math.max(z, maxZ);
                if (z != this.z3) {
                    moveZ(3, z);
                    this.z3 = z;
                }
                
                c = Double.isNaN(c) ? this.c3 : c;
                c = Math.max(c, -180.);
                c = Math.min(c, 180.);                
                if (c != this.c3) {
                    moveC(3, c);
                    this.c3 = c;
                }
                break;
            case "N4":
                z = Double.isNaN(z) ? this.z4 : z;
                z = Math.min(z, minZ);
                z = Math.max(z, maxZ);
                if (z != this.z4) {
                    moveZ(4, z);
                    this.z4 = z;
                }
                
                c = Double.isNaN(c) ? this.c4 : c;
                c = Math.max(c, -180.);
                c = Math.min(c, 180.);                
                if (c != this.c4) {
                    moveC(4, c);
                    this.c4 = c;
                }
                break;
        }
    }

    @Override
    public void actuate(ReferenceActuator actuator, boolean on) throws Exception {
        switch (actuator.getName()) {
            case ACT_N1_VACUUM:
            case ACT_N2_VACUUM:
            case ACT_N3_VACUUM:
            case ACT_N4_VACUUM: {
                if (on) {
                    actuate(actuator, -128.0);
                } else {
                    actuate(actuator, 20.0);
                    actuate(actuator, 0.0);
                }
                break;
            }
            case ACT_N1_BLOW:
            case ACT_N2_BLOW:
            case ACT_N3_BLOW:
            case ACT_N4_BLOW: {
                if (on) {
                    actuate(actuator, 127.0);
                } else {
                    actuate(actuator, 0.0);
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
                    actuate(actuator, 2.0);
                } else {
                    actuate(actuator, 0.0);
                }
                break;
            }
            case "Rails": {
                if (on) {
                    actuate(actuator, 25.0);
                } else {
                    actuate(actuator, 0.0);
                }
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

    @Override
    public void actuate(ReferenceActuator actuator, double value) throws Exception {
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
        }
    }
    
    private int getNozzleAirValue(int nozzleNum) throws Exception {
        assert(nozzleNum >= 0);
        assert(nozzleNum <= 3);

        write(0x40);
        expect(0x0c);

        write(0x00);
        expect(0x11);

        write(0x80);
        expect(0x19);

        byte[] payload = readWithChecksum(8);
        return payload[nozzleNum];
    }

    @Override
    public String actuatorRead(ReferenceActuator actuator) throws Exception {
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
            new PropertySheetWizardAdapter(super.getConfigurationWizard(), "Communications"),
            new PropertySheetWizardAdapter(new Neoden4DriverConfigurationWizard(this), "Machine")
        };
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        firePropertyChange("name", null, getName());
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

    public double getHomeCoordinateX() {
        return this.homeCoordinateX;
    }

    public void setHomeCoordinateX(double homeX) {
        this.homeCoordinateX = homeX;
    }

    public double getHomeCoordinateY() {
        return this.homeCoordinateY;
    }

    public void setHomeCoordinateY(double homeY) {
        this.homeCoordinateY = homeY;
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
}
