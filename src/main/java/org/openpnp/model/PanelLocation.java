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

/**
 * A container for a Panel that gives the panel a physical Location relative to its parent. It 
 * also holds a coordinate transformation that is used to convert the panel's local coordinates to
 * its parent's coordinates.  In addition, it contains information on where the Panel's definition 
 * is stored in the file system.
 */
public class PanelLocation extends PlacementsHolderLocation<PanelLocation> {
    
    public static final String ID_PREFIX = "Pnl";
    
    /**
     * Default constructor
     */
    public PanelLocation() {
        super();
    }

    /**
     * Contructs a deep copy of the specified PanelLocation.
     * @param panelLocation - the PanelLocation to copy
     */
    public PanelLocation(PanelLocation panelLocation) {
        super(panelLocation);
        if (panelLocation.getPanel() != null) {
            for (PlacementsHolderLocation<?> child : getChildren()) {
                child.setParent(this);
            }
        }
    }

    /**
     * Constructs a new PanelLocation that contains the specified Panel
     * @param panel - the Panel
     */
    public PanelLocation(Panel panel) {
        this();
        setPanel(panel);
    }

    /**
     * 
     * @return the Panel associated with this PanelLocation
     */
    public Panel getPanel() {
        return (Panel) getPlacementsHolder();
    }

    /**
     * Sets the Panel associated with this PanelLocation
     * @param panel - the Panel
     */
    public void setPanel(Panel panel) {
        setPlacementsHolder(panel);
    }

    /**
     * 
     * @return the file name where the Panel associated with this PanelLocation is stored
     */
    public String getPanelFile() {
        return getFileName();
    }

    /**
     * Sets the file name where the Panel associated with this PanelLocation is stored
     * @param panelFile
     */
    public void setPanelFile(String panelFile) {
        setFileName(panelFile);
    }

    /**
     * Adds a child to the Panel associated with this PanelLocation
     * @param child - the child to add
     */
    public void addChild(PlacementsHolderLocation<?> child) {
        child.setParent(this);
        Panel panel = getPanel();
        panel.addChild(child);
    }
    
    /**
     * Removes a child from the Panel associated with this PanelLocation
     * @param child - the child to remove
     */
    public void removeChild(PlacementsHolderLocation<?> child) {
        getPanel().removeChild(child);
    }
    
    /**
     * 
     * @return a list of the children contained by the Panel associated with this PanelLocation
     */
    public List<PlacementsHolderLocation<?>> getChildren() {
        if (getPanel() == null) {
            return new ArrayList<>();
        }
        return getPanel().getChildren();
    }
    
    /**
     * Sets the AffineTransform that transforms coordinates expressed in the Panel's 
     * reference frame to those expressed in the parent's reference frame
     */
    public void setLocalToParentTransform(AffineTransform localToParentTransform) {
        super.setLocalToParentTransform(localToParentTransform);
        for (PlacementsHolderLocation<?> child : getChildren()) {
            child.setLocalToParentTransform(null);
        }
    }
    
    /**
     * Traverses the tree of descendants of the specified PanelLocation and sets the parent of each
     * descendant
     * @param panelLocation - the PanelLocation whose descendants should be set
     */
    public static void setParentsOfAllDescendants(PanelLocation panelLocation) {
        for (PlacementsHolderLocation<?> child : panelLocation.getChildren() ) {
            child.setParent(panelLocation);
            if (child instanceof PanelLocation) {
                PanelLocation.setParentsOfAllDescendants((PanelLocation) child);
            }
        }
    }
    
    /**
     * Checks to see if a PlacementsHolderLocation is a descendant of this PanelLocation
     * @param potentialDescendant - the PlacementsHolderLocation to check
     * @return the direct parent of potentialDescendant or null if potentialDescendant is not a 
     * descendant of this PanelLocation
     */
    public PanelLocation getParentOfDescendant(PlacementsHolderLocation<?> potentialDescendant) {
        for (PlacementsHolderLocation<?> child : getChildren()) {
            if (child == potentialDescendant) {
                return this;
            }
            if (child instanceof PanelLocation) {
                PanelLocation parent = getParentOfDescendant(child);
                if (parent != null) {
                    return parent;
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("PanelLocation %s @%08x defined by @%08x: (%s), location (%s), side (%s)", getId(), hashCode(), definition != null ? definition.hashCode() : 0, fileName, getLocation(), side);
    }
        

    /**
     * Creates a formated dump of the PanelLocation and all of its descendants
     * @param leader - prefix to indent the dump 
     */
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
