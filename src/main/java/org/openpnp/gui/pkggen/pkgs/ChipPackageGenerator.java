package org.openpnp.gui.pkggen.pkgs;

import org.openpnp.gui.pkggen.PackageGenerator;
import org.openpnp.model.Footprint;
import org.openpnp.model.Footprint.Pad;
import org.openpnp.model.LengthUnit;

public class ChipPackageGenerator extends PackageGenerator {
    double bodyLength = 3.2; // D
    double bodyWidth = 1.6; // E
    double bodyHeight = 0.35; // A
    double terminalLength = 0.5; // L
    double oddTerminalLength = 0.5; // L1
        
    public org.openpnp.model.Package generate() {
        Footprint footprint = new Footprint();
        footprint.setUnits(LengthUnit.Millimeters);
        footprint.setBodyWidth(bodyLength);
        footprint.setBodyHeight(bodyWidth);
        
        Pad pad;

        pad = new Pad();
        pad.setName("1");
        pad.setWidth(terminalLength);
        pad.setHeight(bodyWidth);
        pad.setX((-bodyLength / 2) + (terminalLength / 2));
        footprint.addPad(pad);
        
        pad = new Pad();
        pad.setName("2");
        pad.setWidth(oddTerminalLength);
        pad.setHeight(bodyWidth);
        pad.setX((bodyLength / 2) - (oddTerminalLength / 2));
        footprint.addPad(pad);
        
        org.openpnp.model.Package pkg = new org.openpnp.model.Package("NEW");
        pkg.setFootprint(footprint);
        
        return pkg;
    }
    
    public String[] getPropertyNames() {
        return new String[] { "bodyLength", "bodyWidth", "bodyHeight", "terminalLength", "oddTerminalLength" };
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

    public double getOddTerminalLength() {
        return oddTerminalLength;
    }

    public void setOddTerminalLength(double oddTerminalLength) {
        this.oddTerminalLength = oddTerminalLength;
        firePropertyChange("oddTerminalLength", null, oddTerminalLength);
    }
}
