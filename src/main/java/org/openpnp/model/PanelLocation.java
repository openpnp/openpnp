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
import java.beans.PropertyChangeEvent;
import java.util.List;

import org.openpnp.util.Utils2D;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.core.Commit;

public class PanelLocation extends FiducialLocatableLocation {

    public PanelLocation() {
        super();
    }

    // Copy constructor needed for deep copy of object.
    public PanelLocation(PanelLocation panelLocation) {
        super(panelLocation);
        if (panelLocation.getPanel() != null) {
//            setPanel(new Panel(panelLocation.getPanel()));
            setPanel(panelLocation.getPanel());
            for (FiducialLocatableLocation child : getChildren()) {
                child.setParent(this);
            }
        }
    }

    public PanelLocation(Panel panel) {
        this();
        setPanel(panel);
    }

    @Commit
    protected void commit() {
        super.commit();
    }
    
    public Panel getPanel() {
        FiducialLocatable panel = getFiducialLocatable();
        if (panel instanceof Panel) {
            return (Panel) panel;
        }
        return null;
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

    public void addChild(FiducialLocatableLocation child) {
        child.setParent(this);
        getPanel().addChild(child);
    }
    
    public void removeChild(FiducialLocatableLocation child) {
        getPanel().removeChild(child);
    }
    
    public List<FiducialLocatableLocation> getChildren() {
        return getPanel().getChildren();
    }
    
    public void flipSide() {
        super.flipSide();
        for (FiducialLocatableLocation child : getChildren()) {
            child.flipSide();
        }
    }
    
    public void setLocalToParentTransform(AffineTransform localToParentTransform) {
        super.setLocalToParentTransform(localToParentTransform);
        for (FiducialLocatableLocation child : getChildren()) {
            child.setLocalToParentTransform(null);
        }
    }
    
    /**
     * Checks to see if a FiducialLocatableLocation is a decendant of this PanelLocation
     * @param potentialDecendant - the FiducialLocatableLocation to check
     * @return the direct parent of potentialDecendant or null if potentialDecendant is not a 
     * decendant of this PanelLocation
     */
    public PanelLocation getParentOfDecendant(FiducialLocatableLocation potentialDecendant) {
        for (FiducialLocatableLocation child : getChildren()) {
            if (child == potentialDecendant) {
                return this;
            }
            if (child instanceof PanelLocation) {
                PanelLocation parent = getParentOfDecendant(child);
                if (parent != null) {
                    return parent;
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("PanelLocation @%08x defined by @%08x: (%s), location (%s), side (%s)", hashCode(), definedBy.hashCode(), fileName, getLocation(), side);
    }

    public void dump(String leader) {
        PanelLocation parentPanelLocation = getParent();
        int parentHashCode = 0;
        if (parentPanelLocation != null) {
            parentHashCode = parentPanelLocation.hashCode();
        }
        Logger.trace(String.format("%sPanelLocation:@%08x defined by @%08x child of @%08x, %s, location=%s , globalLocation=%s side=%s (%s)", leader,  this.hashCode(), this.definedBy.hashCode(), parentHashCode, fileName, getLocation(), getGlobalLocation(), side, getPanel() == null ? "Null" : getPanel().toString()));
        if (getPanel() != null) {
            leader = leader + "    ";
            for (FiducialLocatableLocation child : getPanel().getChildren()) {
                if (child instanceof PanelLocation) {
                    ((PanelLocation) child).dump(leader);
                }
                if (child instanceof BoardLocation) {
                    ((BoardLocation) child).dump(leader);
                }
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        Logger.trace(String.format("PropertyChangeEvent handled by PanelLocation @%08x = %s", this.hashCode(), evt));
        super.propertyChange(evt);
    }
}
