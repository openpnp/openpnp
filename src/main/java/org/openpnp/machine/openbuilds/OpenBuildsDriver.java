package org.openpnp.machine.openbuilds;

import java.util.Locale;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.driver.MarlinDriver;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Nozzle;
import org.simpleframework.xml.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.corba.se.spi.legacy.connection.GetEndPointInfoAgainException;

public class OpenBuildsDriver extends MarlinDriver {
    private static final Logger logger = LoggerFactory.getLogger(OpenBuildsDriver.class);

    @Attribute
    private double zCamRadius = 26;
    
    @Attribute
    private double zCamWheelRadius = 9.5;
    
    @Attribute
    private double zGap = 2;
    
    @Attribute(required=false)
    private int neoPixelRed = 0;
    
    @Attribute(required=false)
    private int neoPixelGreen = 0;
    
    @Attribute(required=false)
    private int neoPixelBlue = 0;
    
    private boolean enabled;
    
    @Override
    public void setEnabled(boolean enabled) throws Exception {
        // TODO Auto-generated method stub
        super.setEnabled(enabled);
        if (enabled) {
            sendCommand(String.format("M420 R%d E%d B%d", neoPixelRed, neoPixelGreen, neoPixelBlue));
        }
        else {
            sendCommand("M420 R0 E0 B0");
            sendCommand("M42 P10 S0");
            sendCommand("M107");
        }
        this.enabled = enabled;
    }
    
    @Override
    public void home(ReferenceHead head) throws Exception {
        super.home(head);
        // After homing completes the Z axis is at the home switch location,
        // which is not 0. The home switch location has been set in the firmware
        // so the firmware's position is correct. We just need to move to zero
        // and update the position.
        sendCommand("G0Z0");
        dwell();
        getCurrentPosition();
    }

    @Override
    public Location getLocation(ReferenceHeadMountable hm) {
        if (hm instanceof ReferenceNozzle) {
            double z = Math.sin(Math.toRadians(this.z)) * zCamRadius;
            if (((ReferenceNozzle) hm).getName().equals("N2")) {
                z = -z;
            }
            z += zCamWheelRadius + zGap;                
            return new Location(LengthUnit.Millimeters, x, y, z, c).add(hm
                    .getHeadOffsets());
        }
        else {
            return new Location(LengthUnit.Millimeters, x, y, z, c).add(hm
                    .getHeadOffsets());
        }
    }

    @Override
    public void moveTo(ReferenceHeadMountable hm, Location location, double speed)
            throws Exception {
        location = location.subtract(hm.getHeadOffsets());

        location = location.convertToUnits(LengthUnit.Millimeters);
        
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        double c = location.getRotation();
        
        /*
         * Only move C and Z if it's a Nozzle.
         */
        if (!(hm instanceof Nozzle)) {
            c = Double.NaN;
            z = Double.NaN;
        }
        
        StringBuffer sb = new StringBuffer();
        if (!Double.isNaN(x) && x != this.x) {
            sb.append(String.format(Locale.US, "X%2.2f ", x));
            this.x = x;
        }
        if (!Double.isNaN(y) && y != this.y) {
            sb.append(String.format(Locale.US, "Y%2.2f ", y));
            this.y = y;
        }
        if (!Double.isNaN(z) && z != this.z) {
            double a = Math.toDegrees(Math.asin((z - zCamWheelRadius - zGap) / zCamRadius));
            logger.debug("nozzle {} {} {}", new Object[] { z, zCamRadius, a });
            if (hm instanceof ReferenceNozzle) {
                ReferenceNozzle nozzle = (ReferenceNozzle) hm;
                if (nozzle.getName().equals("N2")) {
                    a = -a;
                }
            }
            sb.append(String.format(Locale.US, "Z%2.2f ", a));
            this.z = a;
        }
        if (!Double.isNaN(c) && c != this.c) {
            sb.append(String.format(Locale.US, "E%2.2f ", c));
            this.c = c;
        }
        if (sb.length() > 0) {
            sb.append(String.format(Locale.US, "F%2.2f", feedRateMmPerMinute));
            sendCommand("G0 " + sb.toString());
            dwell();
        }
    }
    
    @Override
    public void pick(ReferenceNozzle nozzle) throws Exception {
        if (((ReferenceNozzle) nozzle).getName().equals("N1")) {
            sendCommand("M42 P10 S255");
        }
        else {
            sendCommand("M106");
        }
        dwell();
    }

    @Override
    public void place(ReferenceNozzle nozzle) throws Exception {
        if (((ReferenceNozzle) nozzle).getName().equals("N1")) {
            sendCommand("M42 P10 S0");
        }
        else {
            sendCommand("M107");
        }
        dwell();
    }
    
    @Override
    public Wizard getConfigurationWizard() {
        return new OpenBuildsDriverWizard(this);
    }

    public int getNeoPixelRed() {
        return neoPixelRed;
    }

    public void setNeoPixelRed(int neoPixelRed) throws Exception {
        this.neoPixelRed = neoPixelRed;
        if (enabled) {
            sendCommand(String.format("M420 R%d E%d B%d", neoPixelRed, neoPixelGreen, neoPixelBlue));
        }
    }

    public int getNeoPixelGreen() {
        return neoPixelGreen;
    }

    public void setNeoPixelGreen(int neoPixelGreen) throws Exception  {
        this.neoPixelGreen = neoPixelGreen;
        if (enabled) {
            sendCommand(String.format("M420 R%d E%d B%d", neoPixelRed, neoPixelGreen, neoPixelBlue));
        }
    }

    public int getNeoPixelBlue() {
        return neoPixelBlue;
    }

    public void setNeoPixelBlue(int neoPixelBlue) throws Exception  {
        this.neoPixelBlue = neoPixelBlue;
        if (enabled) {
            sendCommand(String.format("M420 R%d E%d B%d", neoPixelRed, neoPixelGreen, neoPixelBlue));
        }
    }
}
