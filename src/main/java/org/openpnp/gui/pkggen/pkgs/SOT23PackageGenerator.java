package org.openpnp.gui.pkggen.pkgs;

import org.openpnp.gui.pkggen.PackageGenerator;
import org.openpnp.model.Footprint;
import org.openpnp.model.Footprint.Pad;
import org.openpnp.model.LengthUnit;

public class SOT23PackageGenerator extends PackageGenerator {
    /**
     * TODO STOPSHIP
     * In these packages that use spans, some of the properties are overkill. We don't
     * need the terminal length because the span includes it. Ideally, I'd rather know the
     * pin length but I think maybe the spec doesn't support that because of the shape of
     * the pin.
     */
    
    double numPins = 3;
    double pinPitch = 1.27; // e
    double leadSpan = 10.325; // E
    double terminalWidth = 0.385; // b
    double bodyLength = 12.8; // D
    double bodyWidth = 7.5; // E1
    double bodyHeight = 1.325; // A
    double bodyOffset = 0.25; // A1
        
    public SOT23PackageGenerator() {
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
        
        /**
         * So, a SOT23 starts with 3 pins, and always has more pins on the bottom
         * then the top when the number of pins are odd.
         * Also, the pins start centered, it seems and instead of being a pitch apart
         * on the even side they are two pitches apart. So, it's almost like one of the
         * three is moving to the other side?
         * 
         * I think one way to think of it is:
         * If there's an even number of pins, the first pin is to the left, the next pin
         *    is on the other side at the same X, and so on.
         * If there's an odd number of pins, the first pin to the left, the next pin is
         *    on the other side at one pitch advanced, and so on.
         *    
         * Although SOT23-5 seems to break that.
         * 
         * Okay, so actually it's like it's always a 6 pin part, so we measure it like a 6
         * pin part but we just don't insert the pins we don't need.
         */
        
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
