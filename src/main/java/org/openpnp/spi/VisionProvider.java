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

package org.openpnp.spi;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.List;

import org.openpnp.gui.support.Wizard;
import org.openpnp.model.Location;
import org.openpnp.model.Part;

/**
 * Provides an interface for implementors of vision systems to implement. A VisionProvider is
 * attached to a Camera in configuration and can be commanded by the system to perform a variety of
 * vision tasks.
 */
public interface VisionProvider {
    /**
     * Sets the Camera that the VisionProvider should use for image capture. This is called during
     * setup and will only be called once.
     * 
     * @param camera
     */
    public void setCamera(Camera camera);

    public Wizard getConfigurationWizard();

    public List<TemplateMatch> getTemplateMatches(BufferedImage template);

    /**
     * @deprecated This function's interface will change in the near future to return real units
     *             instead of pixels.
     * @param roiX
     * @param roiY
     * @param roiWidth
     * @param roiHeight
     * @param coiX
     * @param coiY
     * @param templateImage
     * @return
     * @throws Exception
     */
    public Point[] locateTemplateMatches(int roiX, int roiY, int roiWidth, int roiHeight, int coiX,
            int coiY, BufferedImage templateImage) throws Exception;

    public static class TemplateMatch {
        public Location location;
        public double score;

        @Override
        public String toString() {
            return location.toString() + " " + score;
        }
    }
}
