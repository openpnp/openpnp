package org.openpnp.gui.pkggen.pkgs;

import org.openpnp.gui.pkggen.PackageGenerator;
import org.openpnp.model.Footprint;
import org.openpnp.model.Footprint.Pad;
import org.openpnp.model.LengthUnit;

public class SOICPackageGenerator extends PackageGenerator {
    /**
     * TODO STOPSHIP
     * In these packages that use spans, some of the properties are overkill. We don't
     * need the terminal length because the span includes it. Ideally, I'd rather know the
     * pin length but I think maybe the spec doesn't support that because of the shape of
     * the pin.
     */
    
    double numPins = 20;
    double pinPitch = 1.27; // e
    double leadSpan = 10.325; // E
    double terminalWidth = 0.385; // b
    double bodyLength = 12.8; // D
    double bodyWidth = 7.5; // E1
    double bodyHeight = 1.325; // A
    double bodyOffset = 0.25; // A1
    double thermalPadLength = 8.61; // D2
    double thermalPadWidth = 4.8; // E2
        
    public SOICPackageGenerator() {
        // Ensures that if any property is updated we broadcast a change on the
        // package property.
        addPropertyChangeListener(e -> {
            if (e.getPropertyName().equals("package")) {
                return;
            }
            firePropertyChange("package", null, getPackage());
        });
    }

    public String[] getPropertyNames() {
        return new String[] { 
                "numPins", 
                "pinPitch", 
                "leadSpan", 
                "terminalWidth",
                "bodyLength",
                "bodyWidth",
                "bodyHeight",
                "bodyOffset",
                "thermalPadLength",
                "thermalPadWidth",
                };
    }

    public org.openpnp.model.Package getPackage() {
        Footprint footprint = new Footprint();
        footprint.setUnits(LengthUnit.Millimeters);
        footprint.setBodyWidth(bodyLength);
        footprint.setBodyHeight(bodyWidth);
        
        Pad pad;

        int padNum = 1;
        
        double pinLength = (leadSpan - bodyWidth) / 2.;
        
        for (int i = 0; i < numPins / 2; i++) {
            pad = new Pad();
            pad.setName("" + padNum++);
            pad.setWidth(terminalWidth);
            pad.setHeight(pinLength);
            pad.setX(-numPins / 4 * pinPitch + (i * pinPitch) + (pinPitch / 2));
            pad.setY(-bodyWidth / 2 - pinLength / 2);
            footprint.addPad(pad);
        }
        
        for (int i = 0; i < numPins / 2; i++) {
            pad = new Pad();
            pad.setName("" + padNum++);
            pad.setWidth(terminalWidth);
            pad.setHeight(pinLength);
            pad.setX(numPins / 4 * pinPitch - (i * pinPitch) - (pinPitch / 2));
            pad.setY(bodyWidth / 2 + pinLength / 2);
            footprint.addPad(pad);
        }
        
        if (thermalPadLength > 0 && thermalPadWidth > 0) {
            pad = new Pad();
            pad.setName("Thermal");
            pad.setWidth(thermalPadLength);
            pad.setHeight(thermalPadWidth);
            footprint.addPad(pad);
        }
        
        org.openpnp.model.Package pkg = new org.openpnp.model.Package("NEW");
        pkg.setFootprint(footprint);
        
        return pkg;
    }
    
    public double getBodyLength() {
        return bodyLength;
    }

    public void setBodyLength(double bodyLength) {
        this.bodyLength = bodyLength;
        firePropertyChange("bodyLength", null, bodyLength);
    }

    public double getBodyWidth() {
        return bodyWidth;
    }

    public void setBodyWidth(double bodyWidth) {
        this.bodyWidth = bodyWidth;
        firePropertyChange("bodyWidth", null, bodyWidth);
    }

    public double getBodyHeight() {
        return bodyHeight;
    }

    public void setBodyHeight(double bodyHeight) {
        this.bodyHeight = bodyHeight;
        firePropertyChange("bodyHeight", null, bodyHeight);
    }

    public double getPinPitch() {
        return pinPitch;
    }

    public void setPinPitch(double pinPitch) {
        this.pinPitch = pinPitch;
        firePropertyChange("pinPitch", null, pinPitch);
    }

    public double getTerminalWidth() {
        return terminalWidth;
    }

    public void setTerminalWidth(double terminalWidth) {
        this.terminalWidth = terminalWidth;
        firePropertyChange("terminalWidth", null, terminalWidth);
    }

    public double getThermalPadLength() {
        return thermalPadLength;
    }

    public void setThermalPadLength(double thermalPadLength) {
        this.thermalPadLength = thermalPadLength;
        firePropertyChange("thermalPadLength", null, thermalPadLength);
    }

    public double getThermalPadWidth() {
        return thermalPadWidth;
    }

    public void setThermalPadWidth(double thermalPadWidth) {
        this.thermalPadWidth = thermalPadWidth;
        firePropertyChange("thermalPadWidth", null, thermalPadWidth);
    }

    public double getBodyOffset() {
        return bodyOffset;
    }

    public void setBodyOffset(double bodyOffset) {
        this.bodyOffset = bodyOffset;
        firePropertyChange("bodyOffset", null, bodyOffset);
    }

    public double getNumPins() {
        return numPins;
    }

    public void setNumPins(double numPins) {
        this.numPins = numPins;
        firePropertyChange("numPins", null, numPins);
    }

    public double getLeadSpan() {
        return leadSpan;
    }

    public void setLeadSpan(double leadSpan) {
        this.leadSpan = leadSpan;
        firePropertyChange("leadSpan", null, leadSpan);
    }
}
