/*
 * Copyright (C) 2020 <mark@makr.zone>
 * inspired and based on work
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.machine.reference.axis;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.axis.wizards.ReferenceCamCounterClockwiseAxisConfigurationWizard;
import org.openpnp.machine.reference.axis.wizards.ReferenceLinearTransformAxisConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Axis;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.base.AbstractAxis;
import org.openpnp.spi.base.AbstractMachine;
import org.openpnp.spi.base.AbstractTransformedAxis;
import org.openpnp.util.Matrix;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public class ReferenceLinearTransformAxis extends AbstractTransformedAxis {
    // The input axes of the transformation. Any of these can be null. 
    private AbstractAxis inputAxisX;
    private AbstractAxis inputAxisY;
    private AbstractAxis inputAxisZ;
    private AbstractAxis inputAxisRotation;

    @Attribute(required = false)
    private String inputAxisXId;
    @Attribute(required = false)
    private String inputAxisYId;
    @Attribute(required = false)
    private String inputAxisZId;
    @Attribute(required = false)
    private String inputAxisRotationId;

    @Attribute(required = false)
    private double factorX = 0.0;
    @Attribute(required = false)
    private double factorY = 0.0;
    @Attribute(required = false)
    private double factorZ = 0.0;
    @Attribute(required = false)
    private double factorRotation = 0.0;
    @Element(required = false)
    private Length offset = new Length(0.0, LengthUnit.Millimeters);

    public ReferenceLinearTransformAxis() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {

            @Override
            public void configurationLoaded(Configuration configuration) throws Exception {
                inputAxisX = (AbstractAxis) configuration.getMachine().getAxis(inputAxisXId);
                inputAxisY = (AbstractAxis) configuration.getMachine().getAxis(inputAxisYId);
                inputAxisZ = (AbstractAxis) configuration.getMachine().getAxis(inputAxisZId);
                inputAxisRotation = (AbstractAxis) configuration.getMachine().getAxis(inputAxisRotationId);
            }
        });
    }

    public AbstractAxis getPrimaryInputAxis() {
        switch (type) {
            case X:
                return inputAxisX;
            case Y:
                return inputAxisY;
            case Z:
                return inputAxisZ;
            case Rotation:
                return inputAxisRotation;
            default:
                return null;
        }
    }

    @Override
    public ControllerAxis getControllerAxis() {
        AbstractAxis inputAxis = getPrimaryInputAxis();
        if (inputAxis != null) {
            return inputAxis.getControllerAxis();
        }
        return null;
    }

    @Override
    public double[] getLinearTransform(LengthUnit lengthUnit) {
        return new double[] { 
                inputAxisX != null ? factorX : 0.0,
                        inputAxisY != null ? factorY : 0.0,
                                inputAxisZ != null ? factorZ : 0.0,
                                        inputAxisRotation != null ? factorRotation : 0.0,
                                                offset.convertToUnits(lengthUnit).getValue(),
        };
    }

    public AbstractAxis getInputAxisX() {
        return inputAxisX;
    }
    public void setInputAxisX(AbstractAxis inputAxisX) {
        Object oldValue = this.inputAxisX;
        this.inputAxisX = inputAxisX;
        this.inputAxisXId = (inputAxisX == null) ? null : inputAxisX.getId();
        firePropertyChange("inputAxisX", oldValue, inputAxisX);
    }
    public AbstractAxis getInputAxisY() {
        return inputAxisY;
    }
    public void setInputAxisY(AbstractAxis inputAxisY) {
        Object oldValue = this.inputAxisY;
        this.inputAxisY = inputAxisY;
        this.inputAxisYId = (inputAxisY == null) ? null : inputAxisY.getId();
        firePropertyChange("inputAxisY", oldValue, inputAxisY);
    }
    public AbstractAxis getInputAxisZ() {
        return inputAxisZ;
    }
    public void setInputAxisZ(AbstractAxis inputAxisZ) {
        Object oldValue = this.inputAxisZ;
        this.inputAxisZ = inputAxisZ;
        this.inputAxisZId = (inputAxisZ == null) ? null : inputAxisZ.getId();
        firePropertyChange("inputAxisZ", oldValue, inputAxisZ);
    }
    public AbstractAxis getInputAxisRotation() {
        return inputAxisRotation;
    }
    public void setInputAxisRotation(AbstractAxis inputAxisRotation) {
        Object oldValue = this.inputAxisRotation;
        this.inputAxisRotation = inputAxisRotation;
        this.inputAxisRotationId = (inputAxisRotation == null) ? null : inputAxisRotation.getId();
        firePropertyChange("inputAxisRotation", oldValue, inputAxisRotation);
    }

    public double getFactorX() {
        return factorX;
    }

    public void setFactorX(double factorX) {
        this.factorX = factorX;
    }

    public double getFactorY() {
        return factorY;
    }

    public void setFactorY(double factorY) {
        this.factorY = factorY;
    }

    public double getFactorZ() {
        return factorZ;
    }

    public void setFactorZ(double factorZ) {
        this.factorZ = factorZ;
    }

    public double getFactorRotation() {
        return factorRotation;
    }

    public void setFactorRotation(double factorRotation) {
        this.factorRotation = factorRotation;
    }

    public Length getOffset() {
        return offset;
    }

    public void setOffset(Length offset) {
        this.offset = offset;
    }

    @Override
    public double toRaw(Location location, double [][] invertedAffineTransform) {
        double [][] transformedVector = new double [][] {
            { AbstractTransformedAxis.toTransformed(inputAxisX, location) },
            { AbstractTransformedAxis.toTransformed(inputAxisY, location) },
            { AbstractTransformedAxis.toTransformed(inputAxisZ, location) },
            { AbstractTransformedAxis.toTransformed(inputAxisRotation, location) },
        };
        double [][] rawVector = Matrix.multiply(invertedAffineTransform, transformedVector);
        return rawVector[type.ordinal()][0];
    }

    @Override
    public double toTransformed(Location location) {
        double x = AbstractTransformedAxis.toTransformed(inputAxisX, location);
        double y = AbstractTransformedAxis.toTransformed(inputAxisY, location);
        double z = AbstractTransformedAxis.toTransformed(inputAxisZ, location);
        double rotation = AbstractTransformedAxis.toTransformed(inputAxisRotation, location);
        return x * factorX
                + y * factorY
                + z * factorZ
                + rotation * factorRotation
                + offset.convertToUnits(location.getUnits()).getValue();
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceLinearTransformAxisConfigurationWizard((AbstractMachine)Configuration.get().getMachine(), this);
    }
}
