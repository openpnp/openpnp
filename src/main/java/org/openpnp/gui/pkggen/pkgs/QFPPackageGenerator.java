package org.openpnp.gui.pkggen.pkgs;

import org.openpnp.gui.pkggen.PackageGenerator;
import org.openpnp.model.Footprint;
import org.openpnp.model.Footprint.Pad;
import org.openpnp.model.LengthUnit;

public class QFPPackageGenerator extends PackageGenerator {
    double numPinsSideD = 16; // D
    double numPinsSideE = 16; // E
    double pinPitch = 0.5; // e
    double leadSpanSideD = 12.9; // D
    double leadSpanSideE = 12.9; // E
    double terminalLength = 0.905; // L, note that this only includes the flat part that touches the board, not the whole terminal
    double terminalWidth = 0.25; // b
    double bodyLength = 10.0; // D1
    double bodyWidth = 10.0; // E1
    double bodyHeight = 0.6; // A
    double bodyOffset = 0.05; // A1
    double thermalPadLength = 7; // D2
    double thermalPadWidth = 7; // E2
        
    public QFPPackageGenerator() {
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
                "numPinsSideD", 
                "numPinsSideE", 
                "pinPitch", 
                "leadSpanSideD", 
                "leadSpanSideE", 
                "terminalLength", 
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
        footprint.setBodyWidth(bodyWidth);
        footprint.setBodyHeight(bodyLength);
        
        Pad pad;

        int padNum = 1;
        
        double pinLength = (leadSpanSideE - bodyWidth) / 2.;
        
        for (int i = 0; i < numPinsSideE; i++) {
            pad = new Pad();
            pad.setName("" + padNum++);
            pad.setWidth(terminalWidth);
            pad.setHeight(pinLength);
            pad.setX(-numPinsSideE / 2 * pinPitch + (i * pinPitch) + (pinPitch / 2));
            pad.setY(-bodyLength / 2 - pinLength / 2);
            footprint.addPad(pad);
        }
        
        for (int i = 0; i < numPinsSideD; i++) {
            pad = new Pad();
            pad.setName("" + padNum++);
            pad.setWidth(terminalWidth);
            pad.setHeight(pinLength);
            pad.setX(bodyLength / 2 + pinLength / 2);
            pad.setY(-numPinsSideD / 2 * pinPitch + (i * pinPitch) + (pinPitch / 2));
            pad.setRotation(90);
            footprint.addPad(pad);
        }
        
        for (int i = 0; i < numPinsSideE; i++) {
            pad = new Pad();
            pad.setName("" + padNum++);
            pad.setWidth(terminalWidth);
            pad.setHeight(pinLength);
            pad.setX(numPinsSideE / 2 * pinPitch - (i * pinPitch) - (pinPitch / 2));
            pad.setY(bodyLength / 2 + pinLength / 2);
            footprint.addPad(pad);
        }
        
        for (int i = 0; i < numPinsSideD; i++) {
            pad = new Pad();
            pad.setName("" + padNum++);
            pad.setWidth(terminalWidth);
            pad.setHeight(pinLength);
            pad.setX(-bodyLength / 2 - pinLength / 2);
            pad.setY(numPinsSideD / 2 * pinPitch - (i * pinPitch) - (pinPitch / 2));
            pad.setRotation(90);
            footprint.addPad(pad);
        }
        
        
        if (thermalPadLength > 0 && thermalPadWidth > 0) {
            pad = new Pad();
            pad.setName("Thermal");
            pad.setWidth(thermalPadWidth);
            pad.setHeight(thermalPadLength);
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

    public double getTerminalLength() {
        return terminalLength;
    }

    public void setTerminalLength(double terminalLength) {
        this.terminalLength = terminalLength;
        firePropertyChange("terminalLength", null, terminalLength);
    }

    public double getNumPinsSideD() {
        return numPinsSideD;
    }

    public void setNumPinsSideD(double numPinsSideD) {
        this.numPinsSideD = numPinsSideD;
        firePropertyChange("numPinsSideD", null, numPinsSideD);
    }

    public double getNumPinsSideE() {
        return numPinsSideE;
    }

    public void setNumPinsSideE(double numPinsSideE) {
        this.numPinsSideE = numPinsSideE;
        firePropertyChange("numPinsSideE", null, numPinsSideE);
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

    public double getLeadSpanSideD() {
        return leadSpanSideD;
    }

    public void setLeadSpanSideD(double leadSpanSideD) {
        this.leadSpanSideD = leadSpanSideD;
        firePropertyChange("leadSpanSideD", null, leadSpanSideD);
    }

    public double getLeadSpanSideE() {
        return leadSpanSideE;
    }

    public void setLeadSpanSideE(double leadSpanSideE) {
        this.leadSpanSideE = leadSpanSideE;
        firePropertyChange("leadSpanSideE", null, leadSpanSideE);
    }

    public double getBodyOffset() {
        return bodyOffset;
    }

    public void setBodyOffset(double bodyOffset) {
        this.bodyOffset = bodyOffset;
        firePropertyChange("bodyOffset", null, bodyOffset);
    }
}
