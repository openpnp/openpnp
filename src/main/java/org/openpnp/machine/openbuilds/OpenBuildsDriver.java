package org.openpnp.machine.openbuilds;

import java.util.Locale;

import org.openpnp.gui.support.Wizard;
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
        }
        this.enabled = enabled;
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
        }
        if (!Double.isNaN(y) && y != this.y) {
            sb.append(String.format(Locale.US, "Y%2.2f ", y));
        }
        if (!Double.isNaN(z) && z != this.z) {
            // TODO: This fails with values larger than the radius because the
            // input to asin must be abs(0-1). 
            double degrees = Math.toDegrees(Math.asin(z / zCamRadius));
            logger.debug("nozzle {} {} {}", new Object[] { z, zCamRadius, degrees });
            if (hm instanceof ReferenceNozzle) {
                ReferenceNozzle nozzle = (ReferenceNozzle) hm;
                if (nozzle.getName().equals("N2")) {
                    degrees = -degrees;
                }
            }
            sb.append(String.format(Locale.US, "Z%2.2f ", degrees));
        }
        if (!Double.isNaN(c) && c != this.c) {
            sb.append(String.format(Locale.US, "E%2.2f ", c));
        }
        if (sb.length() > 0) {
            sb.append(String.format(Locale.US, "F%2.2f", feedRateMmPerMinute));
            sendCommand("G0 " + sb.toString());
            dwell();
        }
        if (!Double.isNaN(x)) {
            this.x = x;
        }
        if (!Double.isNaN(y)) {
            this.y = y;
        }
        if (!Double.isNaN(z)) {
            this.z = z;
        }
        if (!Double.isNaN(c)) {
            this.c = c;
        }
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
