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

package org.openpnp.model;

import java.awt.geom.AffineTransform;

import org.openpnp.util.Utils2D;
import org.simpleframework.xml.core.Commit;

public class PanelLocation extends FiducialLocatableLocation {

    PanelLocation() {
        setLocation(new Location(LengthUnit.Millimeters));
    }

    // Copy constructor needed for deep copy of object.
    public PanelLocation(PanelLocation panelLocation) {
        super(panelLocation);
    }

    public PanelLocation(Panel panel) {
        this();
        setPanel(panel);
    }

    @Commit
    private void commit() {
        setLocation(location);
        setFiducialLocatable(fiducialLocatable);
    }
    
    public FiducialLocatable getPanel() {
        return getFiducialLocatable();
    }

    public void setPanel(Panel panel) {
        setFiducialLocatable(panel);
    }

    public String getPanelFile() {
        return getFileName();
    }

    public void setPanelFile(String panelFile) {
        setFileName(panelFile);
    }

    public String getPanelId() {
        return getParentId();
    }

    public void setPanelId(String id) {
        setParentId(id);
    }

    public AffineTransform getLocalToRootTransform() {
        AffineTransform localToRootTransform = Utils2D.getDefaultBoardPlacementLocationTransform(this);
        PanelLocation parent = (PanelLocation) getParent();
        if (parent != null) {
            localToRootTransform.preConcatenate(parent.getLocalToRootTransform());
        }
        return localToRootTransform;
    }
    
    @Override
    public String toString() {
        return String.format("Panel (%s), location (%s), side (%s)", fileName, location, side);
    }

}
