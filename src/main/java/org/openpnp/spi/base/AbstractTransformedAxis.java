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

package org.openpnp.spi.base;

import org.openpnp.ConfigurationListener;
import org.openpnp.model.Configuration;
import org.openpnp.spi.TransformedAxis;
import org.simpleframework.xml.Attribute;

public abstract class AbstractTransformedAxis extends AbstractAxis implements TransformedAxis {
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

    protected AbstractTransformedAxis() {
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
}
