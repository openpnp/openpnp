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
import java.util.WeakHashMap;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.spi.Camera;
import org.openpnp.spi.PropertySheetHolder;

public class BufferedImageCamera extends ReferenceCamera {
    private static WeakHashMap<Camera, BufferedImageCamera> bufferedCameras = new WeakHashMap<>(); 

    private Camera originalCamera;
    private BufferedImage image;

    public BufferedImageCamera(Camera originalCamera) {
        this.originalCamera = originalCamera;

        setUnitsPerPixel(originalCamera.getUnitsPerPixel());
    }

    protected void setImage(BufferedImage source) {
        this.image = source;
    }

    @Override 
    protected boolean isBroadcasting() {
        // Switch off any Broadcasting for this one. 
        return false;
    }

    @Override
    protected synchronized boolean ensureOpen() {
        // Never really open this one.
        return true;
    }

    @Override
    public synchronized BufferedImage internalCapture() {
        return image;
    }

    @Override
    protected BufferedImage transformImage(BufferedImage image) {
        return image;
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

    public static synchronized BufferedImageCamera get(Camera camera, BufferedImage image) {
        BufferedImageCamera bufferedCamera = bufferedCameras.get(camera);
        if (bufferedCamera == null) {
            bufferedCamera = new BufferedImageCamera(camera); 
            bufferedCameras.put(camera, bufferedCamera);
        }
        bufferedCamera.setImage(image);
        return bufferedCamera;
    }
}
