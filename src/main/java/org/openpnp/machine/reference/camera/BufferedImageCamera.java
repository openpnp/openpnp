/*
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

package org.openpnp.machine.reference.camera;

import java.awt.image.BufferedImage;
import java.beans.PropertyChangeSupport;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.spi.Camera;
import org.openpnp.spi.PropertySheetHolder;

public class BufferedImageCamera extends ReferenceCamera {
    private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private Camera originalCamera;
    private BufferedImage source;

    public BufferedImageCamera(Camera _originalCamera, BufferedImage _source) {
        originalCamera = _originalCamera;
        source = _source;

        setUnitsPerPixel(originalCamera.getUnitsPerPixel());
    }

    @Override
    public synchronized BufferedImage internalCapture() {
        // Don't do transformImage() like regular cameras - expect the image in this camera to have already
        // been transformed by the original camera.
        return source;
    }

    @Override
    public Wizard getConfigurationWizard() {
        return null;
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return null;
    }
}
