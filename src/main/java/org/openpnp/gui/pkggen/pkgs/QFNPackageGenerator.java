package org.openpnp.gui.pkggen.pkgs;

import org.openpnp.gui.pkggen.PackageGenerator;
import org.openpnp.model.Footprint;
import org.openpnp.model.Footprint.Pad;
import org.openpnp.model.LengthUnit;

public class QFNPackageGenerator extends PackageGenerator {
    double numPinsSideD = 6; // D
    double numPinsSideE = 8; // E
    double pinPitch = 0.5; // e
    double terminalLength = 0.4; // L
    double terminalWidth = 0.25; // b
    double bodyLength = 4.0; // D
    double bodyWidth = 5.0; // E
    double bodyHeight = 0.5; // A
    double thermalPadLength = 2; // D2
    double thermalPadWidth = 3; // E2
        
    public org.openpnp.model.Package generate() {
        Footprint footprint = new Footprint();
        footprint.setUnits(LengthUnit.Millimeters);
        footprint.setBodyWidth(bodyWidth);
        footprint.setBodyHeight(bodyLength);
        
        Pad pad;

        int padNum = 1;
        
        for (int i = 0; i < numPinsSideE; i++) {
            pad = new Pad();
            pad.setName("" + padNum++);
            pad.setWidth(terminalWidth);
            pad.setHeight(terminalLength);
            pad.setX(-numPinsSideE / 2 * pinPitch + (i * pinPitch) + (pinPitch / 2));
            pad.setY(-bodyLength / 2 + terminalLength / 2);
            footprint.addPad(pad);
        }
        
        for (int i = 0; i < numPinsSideD; i++) {
            pad = new Pad();
            pad.setName("" + padNum++);
            pad.setWidth(terminalWidth);
            pad.setHeight(terminalLength);
            pad.setX(bodyWidth / 2 - terminalLength / 2);
            pad.setY(-numPinsSideD / 2 * pinPitch + (i * pinPitch) + (pinPitch / 2));
            pad.setRotation(90);
            footprint.addPad(pad);
        }
        
        for (int i = 0; i < numPinsSideE; i++) {
            pad = new Pad();
            pad.setName("" + padNum++);
            pad.setWidth(terminalWidth);
            pad.setHeight(terminalLength);
            pad.setX(-numPinsSideE / 2 * pinPitch + (i * pinPitch) + (pinPitch / 2));
            pad.setY(bodyLength / 2 - terminalLength / 2);
            footprint.addPad(pad);
        }
        
        for (int i = 0; i < numPinsSideD; i++) {
            pad = new Pad();
            pad.setName("" + padNum++);
            pad.setWidth(terminalWidth);
            pad.setHeight(terminalLength);
            pad.setX(-bodyWidth / 2 + terminalLength / 2);
            pad.setY(-numPinsSideD / 2 * pinPitch + (i * pinPitch) + (pinPitch / 2));
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
    
    public String[] getPropertyNames() {
        return new String[] { 
                "numPinsSideD", 
                "numPinsSideE", 
                "pinPitch", 
                "terminalLength", 
                "terminalWidth",
                "bodyLength",
                "bodyWidth",
                "bodyHeight",
                "thermalPadLength",
                "thermalPadWidth",
                };
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
}
