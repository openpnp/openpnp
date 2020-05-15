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

import java.util.Arrays;
import java.util.List;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.axis.wizards.ReferenceCamCounterClockwiseAxisConfigurationWizard;
import org.openpnp.machine.reference.axis.wizards.ReferenceLinearTransformAxisConfigurationWizard;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.MappedAxes;
import org.openpnp.spi.Axis;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Movable.LocationOption;
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
    @Attribute(required = false)
    private boolean compensation = false;

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
    public MappedAxes getControllerAxes(Machine machine) {
        MappedAxes mappedAxes = new MappedAxes(machine,
                AbstractAxis.getControllerAxes(inputAxisX, machine),
                AbstractAxis.getControllerAxes(inputAxisY, machine),
                AbstractAxis.getControllerAxes(inputAxisZ, machine),
                AbstractAxis.getControllerAxes(inputAxisRotation, machine));
        return mappedAxes;
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

    public boolean isCompensation() {
        return compensation;
    }

    public void setCompensation(boolean compensation) {
        this.compensation = compensation;
    }

    public double[] getLinearTransform() throws Exception {
        if (factorX != 0.0 && inputAxisX == null) {
            throw new Exception(getName()+" has X factor but no input axis."); 
        }
        if (factorY != 0.0 && inputAxisY == null) {
            throw new Exception(getName()+" has Y factor but no input axis."); 
        }
        if (factorZ != 0.0 && inputAxisZ == null) {
            throw new Exception(getName()+" has Z factor but no input axis."); 
        }
        if (factorRotation != 0.0 && inputAxisRotation == null) {
            throw new Exception(getName()+" has Rotation factor but no input axis."); 
        }
        return new double[] { 
                inputAxisX != null ? factorX : 0.0,
                        inputAxisY != null ? factorY : 0.0,
                                inputAxisZ != null ? factorZ : 0.0,
                                        inputAxisRotation != null ? factorRotation : 0.0,
                                                offset.convertToUnits(AxesLocation.getUnits()).getValue(),
        };
    }


    private static double [] getLinearTransform(ReferenceLinearTransformAxis axis, double [] unit, LocationOption... options) throws Exception {
        if (axis != null) { 
            if (axis.compensation == false || !Arrays.asList(options).contains(LocationOption.SuppressCompensation)) {
                return axis.getLinearTransform();
            }
            else {
                return unit;
            }
        }
        return unit;
    }

    private static double getLinearCoordinate(AxesLocation location,
            ReferenceLinearTransformAxis linearAxis, AbstractAxis inputAxis) {
        if (linearAxis != null) {
            return location.getCoordinate(linearAxis);
        }
        else {
            return location.getCoordinate(inputAxis);
        }
    }


    @Override
    public AxesLocation toTransformed(AxesLocation location, LocationOption... options) {
        location = AbstractTransformedAxis.toTransformed(inputAxisX, location, options);
        location = AbstractTransformedAxis.toTransformed(inputAxisY, location, options);
        location = AbstractTransformedAxis.toTransformed(inputAxisZ, location, options);
        location = AbstractTransformedAxis.toTransformed(inputAxisRotation, location, options);
        double x = location.getCoordinate(inputAxisX);
        double y = location.getCoordinate(inputAxisY);
        double z = location.getCoordinate(inputAxisZ);
        double rotation = location.getCoordinate(inputAxisRotation);
        if (compensation == false || !Arrays.asList(options).contains(LocationOption.SuppressCompensation)) {
            double offset = this.offset.convertToUnits(AxesLocation.getUnits()).getValue();
            return location.put(new AxesLocation(this, 
                    x * factorX
                    + y * factorY
                    + z * factorZ
                    + rotation * factorRotation
                    + offset));
        }
        else {
            // Compensation suppressed, just return the typed axis (unit transform).
            double coordinate;
            switch (getType()) {
                case X:
                    coordinate = x; 
                    break;
                case Y:
                    coordinate = y; 
                    break;
                case Z:
                    coordinate = z;
                    break;
                case Rotation:
                    coordinate = rotation; 
                    break;
                default:
                    coordinate = 0.0;
                    break;
            }
            return location.put(new AxesLocation(this, coordinate));
        }
    }

    @Override
    public AxesLocation toRaw(AxesLocation location, LocationOption... options) throws Exception {
        // Check if the transformation was already done by another axis.
        if (location.contains(inputAxisX)
                && location.contains(inputAxisY)
                && location.contains(inputAxisZ)
                && location.contains(inputAxisRotation)) {
            // Already done. 
            return location;
        }
        // Get all the ReferenceLinearTransformAxis from the transformed location.
        ReferenceLinearTransformAxis [] linearAxes = new ReferenceLinearTransformAxis [4]; 
        AbstractAxis [] inputAxes = new AbstractAxis [4];  
        for (Axis axis : location.getAxes()) {
            if (axis instanceof ReferenceLinearTransformAxis) {
                int i = axis.getType().ordinal();
                if (linearAxes[i] != null) {
                    throw new Exception("Axes "+linearAxes[i].getName()+" and "+axis.getName()
                    +" both have the same type in a linear transformation.");
                }
                linearAxes[i] = (ReferenceLinearTransformAxis) axis;
                consolidateInputAxes(axis, linearAxes[i].inputAxisX, Axis.Type.X, inputAxes);
                consolidateInputAxes(axis, linearAxes[i].inputAxisY, Axis.Type.Y, inputAxes);
                consolidateInputAxes(axis, linearAxes[i].inputAxisZ, Axis.Type.Z, inputAxes);
                consolidateInputAxes(axis, linearAxes[i].inputAxisRotation, Axis.Type.Rotation, inputAxes);
            }
        }
        // Query each axis for its Affine Transform vector.
        double  [][] affineTransform = new double [][] {
            ReferenceLinearTransformAxis.getLinearTransform(linearAxes[0], new double [] { 1, 0, 0, 0, 0}, options),
            ReferenceLinearTransformAxis.getLinearTransform(linearAxes[1], new double [] { 0, 1, 0, 0, 0}, options),
            ReferenceLinearTransformAxis.getLinearTransform(linearAxes[2], new double [] { 0, 0, 1, 0, 0}, options),
            ReferenceLinearTransformAxis.getLinearTransform(linearAxes[3], new double [] { 0, 0, 0, 1, 0}, options),
            { 0, 0, 0, 0, 1 }
        };

        // Compute the inverse.
        double [][] invertedAffineTransform = Matrix.inverse(affineTransform);
        // Get the transformed vector by querying the linear axes' coordinates.
        double [][] transformedVector = new double [][] {
            { ReferenceLinearTransformAxis.getLinearCoordinate(location, linearAxes[0], inputAxes[0]) },
            { ReferenceLinearTransformAxis.getLinearCoordinate(location, linearAxes[1], inputAxes[1]) },
            { ReferenceLinearTransformAxis.getLinearCoordinate(location, linearAxes[2], inputAxes[2]) },
            { ReferenceLinearTransformAxis.getLinearCoordinate(location, linearAxes[3], inputAxes[3]) },
            { 1 }
        };
        // Calculate the raw vector by applying the inverdet Affine Transform.
        double [][] rawVector = Matrix.multiply(invertedAffineTransform, transformedVector);
        // Place the consolidated result in the location
        location = location.put(new AxesLocation(inputAxes[0], rawVector[0][0]));
        location = location.put(new AxesLocation(inputAxes[1], rawVector[1][0]));
        location = location.put(new AxesLocation(inputAxes[2], rawVector[2][0]));
        location = location.put(new AxesLocation(inputAxes[3], rawVector[3][0]));
        // Recurse input axes to raw.
        location = AbstractTransformedAxis.toRaw(inputAxes[0], location, options);
        location = AbstractTransformedAxis.toRaw(inputAxes[1], location, options);
        location = AbstractTransformedAxis.toRaw(inputAxes[2], location, options);
        location = AbstractTransformedAxis.toRaw(inputAxes[3], location, options);
        return location;
    }
    protected void consolidateInputAxes(Axis axis, AbstractAxis inputAxis, Axis.Type inputType,
            AbstractAxis[] inputAxes) throws Exception {
        int j = inputType.ordinal();
        if (inputAxis != null) {
            if (inputAxes[j] != null && inputAxes[j] != inputAxis) {
                throw new Exception("Axes "+axis.getName()+" has a different input "+inputType
                        +" axis than other linear transformation axes.");
            }
            inputAxes[j] = inputAxis;
        }
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceLinearTransformAxisConfigurationWizard((AbstractMachine)Configuration.get().getMachine(), this);
    }
}
