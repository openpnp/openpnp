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
import java.util.ArrayList;
import java.util.List;

import org.pmw.tinylog.Logger;
import org.simpleframework.xml.core.Commit;

public class PanelLocation extends PlacementsHolderLocation<PanelLocation> {
    
    public static final String ID_PREFIX = "Pnl";
    
    public PanelLocation() {
        super();
    }

    // Copy constructor needed for deep copy of object.
    public PanelLocation(PanelLocation panelLocation) {
        super(panelLocation);
        if (panelLocation.getPanel() != null) {
//            setPanel(panelLocation.getPanel());
            for (PlacementsHolderLocation<?> child : getChildren()) {
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
        return (Panel) getPlacementsHolder();
    }

    public void setPanel(Panel panel) {
        setPlacementsHolder(panel);
    }

    public String getPanelFile() {
        return getFileName();
    }

    public void setPanelFile(String panelFile) {
        setFileName(panelFile);
    }

    public void addChild(PlacementsHolderLocation<?> child) {
        child.setParent(this);
        Panel panel = getPanel();
        panel.addChild(child);
    }
    
    public void removeChild(PlacementsHolderLocation<?> child) {
        getPanel().removeChild(child);
    }
    
    public List<PlacementsHolderLocation<?>> getChildren() {
        if (getPanel() == null) {
            return new ArrayList<>();
        }
        return getPanel().getChildren();
    }
    
    public void flipSide() {
        super.flipSide();
        for (PlacementsHolderLocation<?> child : getChildren()) {
            child.flipSide();
        }
    }
    
    public void setLocalToParentTransform(AffineTransform localToParentTransform) {
        super.setLocalToParentTransform(localToParentTransform);
        for (PlacementsHolderLocation<?> child : getChildren()) {
            child.setLocalToParentTransform(null);
        }
    }
    
    public static void setParentsOfAllDescendants(PanelLocation panelLocation) {
        for (PlacementsHolderLocation<?> child : panelLocation.getChildren() ) {
            child.setParent(panelLocation);
            if (child instanceof PanelLocation) {
                PanelLocation.setParentsOfAllDescendants((PanelLocation) child);
            }
        }
    }
    
    /**
     * Checks to see if a FiducialLocatableLocation is a decendant of this PanelLocation
     * @param potentialDecendant - the FiducialLocatableLocation to check
     * @return the direct parent of potentialDecendant or null if potentialDecendant is not a 
     * decendant of this PanelLocation
     */
    public PanelLocation getParentOfDecendant(PlacementsHolderLocation<?> potentialDecendant) {
        for (PlacementsHolderLocation<?> child : getChildren()) {
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
        return String.format("PanelLocation @%08x defined by @%08x: (%s), location (%s), side (%s)", hashCode(), definition.hashCode(), fileName, getLocation(), side);
    }

    public void dump(String leader) {
        PanelLocation parentPanelLocation = getParent();
        int parentHashCode = 0;
        if (parentPanelLocation != null) {
            parentHashCode = parentPanelLocation.hashCode();
        }
        Logger.trace(String.format("%s (%s) PanelLocation:@%08x defined by @%08x child of @%08x, %s, location=%s , globalLocation=%s side=%s (%s)", leader,  this.id, this.hashCode(), this.definition.hashCode(), parentHashCode, fileName, getLocation(), getGlobalLocation(), side, getPanel() == null ? "Null" : getPanel().toString()));
        if (getPanel() != null) {
            if (leader.isEmpty()) {
                leader = "  +--";
            }
            else {
                leader = "    " + leader;
            }
            for (PlacementsHolderLocation<?> child : getPanel().getChildren()) {
                if (child instanceof PanelLocation) {
                    ((PanelLocation) child).dump(leader);
                }
                if (child instanceof BoardLocation) {
                    ((BoardLocation) child).dump(leader);
                }
            }
        }
    }
}
