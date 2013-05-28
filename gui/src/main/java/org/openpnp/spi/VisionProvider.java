/*
 	Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 	
 	This file is part of OpenPnP.
 	
	OpenPnP is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenPnP is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenPnP.  If not, see <http://www.gnu.org/licenses/>.
 	
 	For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.spi;

import java.awt.Point;
import java.awt.image.BufferedImage;

import org.openpnp.gui.support.Wizard;

/**
 * Provides an interface for implementors of vision systems to implement. A
 * VisionProvider is attached to a Camera in configuration and can be commanded
 * by the system to perform a variety of vision tasks.
 */
public interface VisionProvider {
    /**
     * Sets the Camera that the VisionProvider should use for image capture.
     * This is called during setup and will only be called once.
     * 
     * @param camera
     */
    public void setCamera(Camera camera);

    public Wizard getConfigurationWizard();

    // TODO: decide if results are measured from top or bottom left and
    // standardize on it
    public Circle[] locateCircles(int roiX, int roiY, int roiWidth,
            int roiHeight, int coiX, int coiY, int minimumDiameter,
            int diameter, int maximumDiameter) throws Exception;

    public Point[] locateTemplateMatches(int roiX, int roiY, int roiWidth,
            int roiHeight, int coiX, int coiY, BufferedImage templateImage)
            throws Exception;

    public class Circle {
        private double x;
        private double y;
        private double diameter;

        public Circle(double x, double y, double diameter) {
            this.x = x;
            this.y = y;
            this.diameter = diameter;
        }

        public double getX() {
            return x;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getY() {
            return y;
        }

        public void setY(double y) {
            this.y = y;
        }

        public double getDiameter() {
            return diameter;
        }

        public void setDiameter(double diameter) {
            this.diameter = diameter;
        }
    }
}
