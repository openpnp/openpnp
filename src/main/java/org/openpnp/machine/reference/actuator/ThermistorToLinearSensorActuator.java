package org.openpnp.machine.reference.actuator;

import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.actuator.wizards.ThermistorToLinearSensorActuatorTransforms;
import org.simpleframework.xml.Attribute;

/**
 * Performs inverse Steinhart-Hart and voltage divider calculations to convert
 * from a thermistor reading to a linear voltage, with optional scaling. This
 * is used to linearize sensor readings when used with controllers like
 * Smoothie where the ADC inputs all go through thermistor conversions. This
 * essentially reverses what happens inside Smoothie. 
 */
public class ThermistorToLinearSensorActuator extends ReferenceActuator {
    @Attribute(required=false)
    private double a = 0.000722378300319346;
    
    @Attribute(required=false)
    private double b = 0.000216301852054578;
    
    @Attribute(required=false)
    private double c = 9.2641025635702e-08;
    
    @Attribute(required=false)
    private double adcMax = 4095;
    
    @Attribute(required=false)
    private double vRef = 3.3;
    
    @Attribute(required=false)
    private double r1 = 0;
    
    @Attribute(required=false)
    private double r2 = 4700;
    
    @Attribute(required=false)
    private double scale = 1;
    
    @Attribute(required=false)
    private double offset = 0;

    private double temperatureToResistance(double t) {
        double tK, x, y, r;
        tK = t + 273.15;
        x = 1 / (2 * c) * (a - 1 / tK);
        y = Math.sqrt(Math.pow(b / (3 * c), 3) + Math.pow(x, 2));
        r = Math.exp(Math.pow(y - x, 1.0 / 3) - Math.pow(y + x, 1.0 / 3));
        return r;
    }

    private double resistanceToAdc(double r) {
        // r1 not yet implemented.
        // if (r1 > 0.0F) r = (r1 * r) / (r1 - r);
        return (adcMax * r) / (r + r2);
    }
    
    private double adcToVoltage(double adc) {
        return (adc / adcMax) * vRef;
    }

    @Override
    public String read() throws Exception {
        String s = super.read();
        double t = Double.valueOf(s);
        double r = temperatureToResistance(t);
        double adc = resistanceToAdc(r);
        double v = adcToVoltage(adc);
        double scaled = v * scale;
        double fin = scaled + offset;
        return Double.toString(fin);
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
                new PropertySheetWizardAdapter(super.getConfigurationWizard()),
                new PropertySheetWizardAdapter(new ThermistorToLinearSensorActuatorTransforms(this), "Transforms")
        };
    }

    public double getAdcMax() {
        return adcMax;
    }

    public void setAdcMax(double adcMax) {
        this.adcMax = adcMax;
        firePropertyChange("adcMax", null, adcMax);
    }

    public double getvRef() {
        return vRef;
    }

    public void setvRef(double vRef) {
        this.vRef = vRef;
        firePropertyChange("vRef", null, vRef);
    }

    public double getR1() {
        return r1;
    }

    public void setR1(double r1) {
        this.r1 = r1;
        firePropertyChange("r1", null, r1);
    }

    public double getR2() {
        return r2;
    }

    public void setR2(double r2) {
        this.r2 = r2;
        firePropertyChange("r2", null, r2);
    }

    public double getA() {
        return a;
    }

    public void setA(double a) {
        this.a = a;
        firePropertyChange("a", null, a);
    }

    public double getB() {
        return b;
    }

    public void setB(double b) {
        this.b = b;
        firePropertyChange("b", null, b);
    }

    public double getC() {
        return c;
    }

    public void setC(double c) {
        this.c = c;
        firePropertyChange("c", null, c);
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
        firePropertyChange("scale", null, scale);
    }

    public double getOffset() {
        return offset;
    }

    public void setOffset(double offset) {
        this.offset = offset;
        firePropertyChange("offset", null, offset);
    }
}
