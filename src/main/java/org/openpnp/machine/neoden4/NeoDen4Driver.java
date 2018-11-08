package org.openpnp.machine.neoden4;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.ReferencePasteDispenser;
import org.openpnp.machine.reference.driver.AbstractReferenceDriver;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Named;
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

    @Attribute(required = false)
    protected LengthUnit units = LengthUnit.Millimeters;

    @Attribute(required = false)
    protected int timeoutMilliseconds = 5000;

    @Attribute(required = false)
    protected int connectWaitTimeMilliseconds = 3000;
    
    @Attribute(required = false)
    protected String name = "NeoDen4Driver";

    private boolean connected;
    private Set<Nozzle> pickedNozzles = new HashSet<>();
    
    double x = 0, y = 0, z = 0, rotation = 0;
    
    public synchronized void connect() throws Exception {
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

    @Override
    public void dispense(ReferencePasteDispenser dispenser,Location startLocation,Location endLocation,long dispenseTimeMilliseconds) throws Exception {
    }
    
    int read() throws Exception {
        while (true) {
            try {
                int d = getCommunications().read();
                Logger.trace(String.format("< %02x", d));
                return d;
            }
            catch (TimeoutException e) {
                continue;
            }
        }
    }
    
    void write(int d) throws Exception {
        d = d & 0xff;
        Logger.trace(String.format("> %02x", d));
        getCommunications().write(d);
    }
    
    void write(byte[] b) throws Exception {
        for (int i = 0; i < b.length; i++) {
            write(b[i]);
        }
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

    void putInt32(int d, byte[] buffer, int position) throws Exception {
        buffer[position + 0] = (byte) ((d >> 0) & 0xff);
        buffer[position + 1] = (byte) ((d >> 8) & 0xff);
        buffer[position + 2] = (byte) ((d >> 16) & 0xff);
        buffer[position + 3] = (byte) ((d >> 24) & 0xff);
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
//        47 -> 0b
//        c7 -> 03
//        0100000000000000XX
//        07 -> 07
//        07 -> 43        
        
        write(0x47);
        expect(0x0b);
        
        write(0xc7);
        expect(0x03);

        byte[] b = new byte[8];
        putInt32(0x01, b, 0);
        putInt32(0x00, b, 4);
        write(b);
        write(checksum(b));
        
        pollFor(0x07, 0x43);
        
        this.x = -437.;
        this.y = 437.;
        ReferenceMachine machine = ((ReferenceMachine) Configuration.get().getMachine());
        machine.fireMachineHeadActivity(head);
    }

    @Override
    public Location getLocation(ReferenceHeadMountable hm) {
        return new Location(units, x, y, z, rotation).add(hm.getHeadOffsets());
    }

    @Override
    public void moveTo(ReferenceHeadMountable hm, Location location, double speed)
            throws Exception {
        location = location.convertToUnits(units);
        location = location.subtract(hm.getHeadOffsets());

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        double rotation = location.getRotation();

        // Handle NaNs, which means don't move this axis for this move. We just copy the existing
        // coordinate.
        if (Double.isNaN(x)) {
            x = this.x;
        }
        if (Double.isNaN(y)) {
            y = this.y;
        }
        if (Double.isNaN(z)) {
            z = this.z;
        }
        if (Double.isNaN(rotation)) {
            rotation = this.rotation;
        }
        
        if (x != this.x || y != this.y) {
//            48 -> 05
//            c8 -> 0d
//            x1x2x3x4y1y2y3y4XX
//            08 -> 09
//            08 -> 4d            
            
            write(0x48);
            expect(0x05);
            
            write(0xc8);
            expect(0x0d);

            byte[] b = new byte[8];
            putInt32((int) (x * 100), b, 0);
            putInt32((int) (y * 100), b, 4);
            write(b);
            write(checksum(b));
            
            pollFor(0x08, 0x4d);
            
            this.x = x;
            this.y = y;
        }

        // TODO STOPSHIP
        if (hm.getName().equals("N1")) {
            if (z < 0) {
                
            }
        }
        else if (hm.getName().equals("N2")) {
            
        }
    }

    @Override
    public void pick(ReferenceNozzle nozzle) throws Exception {
        pickedNozzles.add(nozzle);
        if (pickedNozzles.size() > 0) {
            // TODO STOPSHIP turn on pump
        }
        // TODO STOPSHIP send pick
    }

    @Override
    public void place(ReferenceNozzle nozzle) throws Exception {
        // TODO STOPSHIP send place
        
        pickedNozzles.remove(nozzle);
        if (pickedNozzles.size() < 1) {
            // TODO STOPSHIP turn off pump
        }
    }

    @Override
    public void actuate(ReferenceActuator actuator, boolean on) throws Exception {
        // TODO STOPSHIP actuate
    }

    @Override
    public void actuate(ReferenceActuator actuator, double value) throws Exception {
        // TODO STOPSHIP actuate
    }
    
    @Override
    public String actuatorRead(ReferenceActuator actuator) throws Exception {
        // TODO STOPSHIP actuate
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
                new PropertySheetWizardAdapter(super.getConfigurationWizard(), "Communications")
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
}
