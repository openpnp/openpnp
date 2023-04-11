/*
 * Copyright (C) 2023 Jason von Nieda <jason@vonnieda.org>, Tony Luken <tonyluken62+openpnp@gmail.com>
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

import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openpnp.gui.MainFrame;
import org.openpnp.util.IdentifiableList;
import org.openpnp.util.Pair;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;

/**
 * A Panel is a PlacementsHolder whose main purpose is to hold multiple Board and/or Panel Locations
 */
@Root(name = "openpnp-panel")
public class Panel extends PlacementsHolder<Panel> implements PropertyChangeListener {
    public static final Double LATEST_VERSION = 2.0;
    
    /**
     * The version number of the Panel
     */
    @Attribute(required = false)
    private Double version = null;
    
    /**
     * @deprecated The Id is now carried by the {@link PanelLocation}
     */
    @Deprecated
    @Element(required = false)
    public String id;
    
    /**
     * @deprecated Panels are no longer define by rows and columns but rather by the location of
     * their children
     */
    @Deprecated
    @Element(required = false)
    public Integer columns = 1;
    
    /**
     * @deprecated Panels are no longer define by rows and columns but rather by the location of
     * their children
     */
    @Deprecated
    @Element(required = false)
    public Integer rows = 1;
    
    /**
     * @deprecated Panels are no longer define by rows and columns but rather by the location of
     * their children
     */
    @Deprecated
    @Element(required = false)
    public Length xGap;
    
    /**
     * @deprecated Panels are no longer define by rows and columns but rather by the location of
     * their children
     */
    @Deprecated
    @Element(required = false)
    public Length yGap;
    
    /**
     * @deprecated Panels can now have an unlimited number of Placements (fiducials) that each have
     * their own Part 
     */
    @Deprecated
    @Element(required=false)
    private String partId;

    /**
     * @deprecated the check fiducials flag is now carried by the {@link PanelLocation}
     */
    @Deprecated
    @Element(required=false)
    private Boolean checkFids;

    /**
     * @deprecated Fiducials are now held in the placements List inherited from {@link PlacementsHolder}
     */
    @Deprecated
    @ElementList(required = false)
    protected IdentifiableList<Placement> fiducials;
    
    /**
     * The list of children contained by this Panel
     */
    @ElementList(required = false)
    protected IdentifiableList<PlacementsHolderLocation<?>> children = new IdentifiableList<>();
    
    /**
     * A list of Ids that point to placements (fiducials) on the Panel's children that are to be 
     * used for panel alignment 
     */
    @ElementList(required = false)
    protected ArrayList<String> pseudoPlacementIds = new ArrayList<>();
    
    /**
     * A list of Placements corresponding to those identified in {@link pseudoPlacementIds}. These
     * placements are copies of those on the Panel's children and are only used for panel alignment, i.e, 
     * they will never actually be placed.
     */
    protected transient IdentifiableList<Placement> pseudoPlacements = new IdentifiableList<>();
    
    /**
     * Runs just after de-serialization
     */
    @Commit
    protected void commit() {
        //Convert deprecated elements
        if (fiducials != null) {
            placements.addAll(fiducials);
        }
        id = null;
        
        super.commit();
        for (PlacementsHolderLocation<?> child : children) {
            child.addPropertyChangeListener(this);
        }
        
    }
    
    /**
     * Runs just before serialization
     */
    @Persist
    private void persist() {
        version = LATEST_VERSION;
        
        //Remove deprecated elements
        columns = null;
        rows = null;
        xGap = null;
        yGap = null;
        fiducials = null;
        partId = null;
        checkFids = null;
        
        //Refresh the list of pseudoPlacementIds
        pseudoPlacementIds.clear();
        for (Placement pseudoPlacement : pseudoPlacements) {
            pseudoPlacementIds.add(pseudoPlacement.getId());
        }
    }
    
    /**
     * Default constructor
     */
    public Panel() {
        super();
    }

    /**
     * Constructs a new Panel 
     * @param file - the file where this Panel will be stored
     */
    public Panel(File file) {
        this();
        setFile(file);
    }

    /**
     * Constructs a deep copy of the specified panel
     * @param panel - the Panel to copy
     */
    public Panel(Panel panel) {
        super(panel);
        version = panel.version;
        checkFids = panel.checkFids;
        for (PlacementsHolderLocation<?> child : panel.getChildren()) {
            if (child instanceof PanelLocation) {
                PanelLocation newPanelLocation = new PanelLocation((PanelLocation) child);
                addChild(newPanelLocation);
            }
            else if (child instanceof BoardLocation) {
                BoardLocation newboardLocation = new BoardLocation((BoardLocation) child);
                addChild(newboardLocation);
            }
        }
        for (Placement pseudoPlacement : panel.pseudoPlacements) {
            Placement newPseudoPlacement = new Placement(pseudoPlacement);
            pseudoPlacements.add(newPseudoPlacement);
            newPseudoPlacement.addPropertyChangeListener(this);
        }
    }
    
    /**
     * Removes all the property change listeners associated with this panel. Should be called if
     * this panel instance is no longer needed.
     */
    @Override
    public void dispose() {
        for (Placement pseudoPlacement : pseudoPlacements) {
            pseudoPlacement.removePropertyChangeListener(this);
            pseudoPlacement.dispose();
        }
        for (PlacementsHolderLocation<?> child : getChildren()) {
            child.removePropertyChangeListener(this);
            child.dispose();
        }
        super.dispose();
    }
    
    /**
     * 
     * @return a list of all the panel's children
     */
    public List<PlacementsHolderLocation<?>> getChildren() {
        return children;
    }
    
    /**
     * Sets the children of this panel
     * @param children - a list of PlacementsHolderLocations that are the children of this panel
     */
    public void setChildren(IdentifiableList<PlacementsHolderLocation<?>> children) {
        Object oldValue = this.children;
        this.children = children;
        firePropertyChange("children", oldValue, children);
    }
    
    /**
     * Gets the specified child of this panel
     * @param index - the index of the child
     * @return the child
     */
    public PlacementsHolderLocation<?> getChild(int index) {
        return children.get(index);
    }
    
    /**
     * Sets the child at the specified index. This method should only be called from a panel 
     * definition's property change listener when an indexed property change event has been fired
     * that adds, modifies, or deletes a child 
     * @param index - the index of the child to set
     * @param child - the child to add or modify, set to null to delete the child
     * @throws UnsupportedOperationException if called on a panel definition or if the specified 
     * child is neither a BoardLocation nor a PanelLocation
     */
    public void setChild(int index, PlacementsHolderLocation<?> child) {
        if (this == this.definition) {
            throw new UnsupportedOperationException("Can not use setChild method to add/remove "
                    + "children from a Panel definition - use addChild or removeChild instead.");
        }
        if (child != null) {
            if (child instanceof BoardLocation) {
                child = new BoardLocation((BoardLocation) child.getDefinition());
                if (MainFrame.get().getJobTab().getJob().getRootPanelLocation().getPanel().isMember(this)) {
                    try {
                        Configuration.get().resolveBoard(MainFrame.get().getJobTab().getJob(), (BoardLocation) child);
                    }
                    catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            else if (child instanceof PanelLocation) {
                child = new PanelLocation((PanelLocation) child.getDefinition());
                if (MainFrame.get().getJobTab().getJob().getRootPanelLocation().getPanel().isMember(this)) {
                    try {
                        Configuration.get().resolvePanel(MainFrame.get().getJobTab().getJob(), (PanelLocation) child);
                    }
                    catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            else {
                throw new UnsupportedOperationException("Unable to create panel child of type " + child.getClass());
            }
            if (index >= children.size()) {
                children.add(child);
            }
            else {
                PlacementsHolderLocation<?> oldChild = children.get(index);
                oldChild.removePropertyChangeListener(this);
                oldChild.dispose();
                children.set(index, child);
            }
            fireIndexedPropertyChange("child", index, null, child);
            child.addPropertyChangeListener(this);
        }
        else {
            if (index >= 0 && index < children.size()) {
                child = children.get(index);
                if (child != null) {
                    children.remove(index);
                    fireIndexedPropertyChange("child", index, child, null);
                    child.removePropertyChangeListener(this);
                    child.dispose();
                }
            }
        }
    }
    
    /**
     * Adds a child to this panel
     * @param child - the child to add
     * @throws UnsupportedOperationException if the specified child is neither a BoardLocation nor 
     * a PanelLocation
     */
    public void addChild(PlacementsHolderLocation<?> child) {
        if (child != null) {
            String childId = child.getId();
            if (childId == null || children.get(childId) != null) {
                if (child instanceof BoardLocation) {
                    childId = children.createId("Brd");
                }
                else if (child instanceof PanelLocation) {
                    childId = children.createId("Pnl");
                }
                else {
                    throw new UnsupportedOperationException("Unable to create panel child id for type " + child.getClass());
                }
            }
            child.setId(childId);
            children.add(child);
            fireIndexedPropertyChange("child", children.indexOf(child), null, child);
            child.addPropertyChangeListener(this);
        }
    }
    
    /**
     * Removes a child from this panel. This method should only be called on panel definitions.
     * @param child - the child to remove
     * @throws UnsupportedOperationException if called on a non-panel definition
     */
    public void removeChild(PlacementsHolderLocation<?> child) {
        if (this != this.definition) {
            throw new UnsupportedOperationException("This method should only be called on a Panel definition.");
        }
        if (child != null) {
            int index = children.indexOf(child);
            if (index >= 0) {
                List<Placement> oldPseudoPlacements = new IdentifiableList<>(pseudoPlacements);
                for (Placement pseudoPlacement : oldPseudoPlacements) {
                    if (pseudoPlacement.getId().startsWith(child.getId() + PlacementsHolderLocation.ID_DELIMITTER)) {
                        removePseudoPlacement(pseudoPlacement);
                    }
                }
                children.remove(index);
                child.removePropertyChangeListener(this);
                fireIndexedPropertyChange("child", index, child, null);
                child.dispose();
            }
        }
    }
    
    /**
     * Removes all children from this panel
     */
    public void removeAllChildren() {
        List<PlacementsHolderLocation<?>> oldValue = new ArrayList<>(children);
        for (PlacementsHolderLocation<?> child : oldValue) {
            removeChild(child);
        }
    }
    
    /**
     * Gets the list of pseudo-placement ids for this panel. A pseudo-placement is a copy of a 
     * placement or fiducial located on one of the panel's descendants that is only used for panel
     * alignment.
     * @return - the list of pseudo-placement ids
     */
    public List<String> getPseudoPlacementIds() {
        return pseudoPlacementIds;
    }

    /**
     * Sets the list of pseudo-placement ids for this panel. A pseudo-placement is a copy of a 
     * placement or fiducial located on one of the panel's descendants that is only used for panel
     * alignment. 
     * @param pseudoPlacementIds - the list of pseudo-placement ids
     */
    public void setPseudoPlacementIds(ArrayList<String> pseudoPlacementIds) {
        Object oldValue = new ArrayList<>(this.pseudoPlacementIds);
        this.pseudoPlacementIds = pseudoPlacementIds;
        firePropertyChange("pseudoPlacementIds", oldValue, pseudoPlacementIds);
    }
    
    /**
     * Gets the list of pseudo-placements for this panel. A pseudo-placement is a copy of a 
     * placement or fiducial located on one of the panel's descendants that is only used for panel
     * alignment. 
     * @return - the list of pseudo-placements
     */
    public List<Placement> getPseudoPlacements() {
        return pseudoPlacements;
    }
    
    /**
     * Sets the list of pseudo-placements for this panel. A pseudo-placement is a copy of a 
     * placement or fiducial located on one of the panel's descendants that is only used for panel 
     * alignment.
     * @param pseudoPlacements - the list of pseudo-placements
     */
    public void setPseudoPlacements(IdentifiableList<Placement> pseudoPlacements) {
        Object oldValue = this.pseudoPlacements;
        this.pseudoPlacements = pseudoPlacements;
        firePropertyChange("pseudoPlacements", oldValue, pseudoPlacements);
    }
    
    /**
     * Gets the specified pseudo-placement. A pseudo-placement is a copy of a placement or fiducial
     * located on one of the panel's descendants that is only used for panel alignment.
     * @param index - specifies the pseudo-placement to get
     * @return the pseudo-placement
     */
    public Placement getPseudoPlacement(int index) {
        return pseudoPlacements.get(index);
    }
    
    /**
     * Sets the pseudo-placement at the specified index. A pseudo-placement is a copy of a placement
     * or fiducial located on one of the panel's descendants that is only used for panel alignment.
     * This method should only be called from a panel definition's property change listener when an
     * indexed property change event has been fired that adds, modifies, or deletes a 
     * pseudo-placement. 
     * @param index - the index of the pseudo-placement to set
     * @param pseudoPlacement - the pseudo-placement to add or modify, set to null to delete the 
     * pseudo-placement
     * @throws UnsupportedOperationException if called on a panel definition
     */
    public void setPseudoPlacement(int index, Placement pseudoPlacement) {
        if (this == this.definition) {
            throw new UnsupportedOperationException("Use addPseudoPlacement to add a "
                    + "PseudoPlacement to a panel definition");
        }
        if (pseudoPlacement != null) {
            pseudoPlacement = new Placement(pseudoPlacement);
            if (index >= pseudoPlacements.size()) {
                pseudoPlacements.add(pseudoPlacement);
            }
            else {
                pseudoPlacements.set(index, pseudoPlacement);
            }
            pseudoPlacement.addPropertyChangeListener(this);
            fireIndexedPropertyChange("pseudoPlacement", index, null, pseudoPlacement);
        }
        else {
            if (index >= 0 && index < pseudoPlacements.size()) {
                pseudoPlacement = pseudoPlacements.get(index);
                pseudoPlacements.remove(pseudoPlacement);
                pseudoPlacement.removePropertyChangeListener(this);
                pseudoPlacement.dispose();
                fireIndexedPropertyChange("pseudoPlacement", index, pseudoPlacement, null);
            }
        }
    }
    
    /**
     * Adds a pseudo-placement to the panel. A pseudo-placement is a copy of a placement or fiducial
     * located on one of the panel's descendants that is only used for panel alignment. This method
     * should only be called on panel definitions.
     * @param pseudoPlacement - the pseudo-placement to add
     * @throws UnsupportedOperationException if called on a non-panel definition
     */
    public void addPseudoPlacement(Placement pseudoPlacement) {
        if (this != this.definition) {
            throw new UnsupportedOperationException("Can only add new pseudoPlacements to a panel definition");
        }
        if (pseudoPlacement != null) {
            if (pseudoPlacements.get(pseudoPlacement.getId()) != null) {
                removePseudoPlacement(pseudoPlacements.get(pseudoPlacement.getId()));
            }
            pseudoPlacements.add(pseudoPlacement);
            pseudoPlacement.addPropertyChangeListener(this);
            fireIndexedPropertyChange("pseudoPlacement", pseudoPlacements.indexOf(pseudoPlacement), null, pseudoPlacement);
        }
    }

    /**
     * Removes a pseudo-placement from the panel. A pseudo-placement is a copy of a placement or 
     * fiducial located on one of the panel's descendants that is only used for panel alignment.
     * This method should only be called on panel definitions.
     * @param pseudoPlacement - the pseudo-placement to remove
     * @throws UnsupportedOperationException if called on a non-panel definition
     */
    public void removePseudoPlacement(Placement pseudoPlacement) {
        if (this != this.definition) {
            throw new UnsupportedOperationException("Can only remove pseudoPlacements from a panel definition");
        }
        int index = pseudoPlacements.indexOf(pseudoPlacement);
        pseudoPlacements.remove(pseudoPlacement);
        fireIndexedPropertyChange("pseudoPlacement", index, pseudoPlacement, null);
        pseudoPlacement.removePropertyChangeListener(this);
    }
    
    /**
     * Creates a pseudo-placement given the pseudo-placement's id. A pseudo-placement is a copy of a
     * placement or fiducial located on one of the panel's descendants that is only used for panel
     * alignment. This method should only be called on panel definitions.
     * @param pseudoPlacementId - the id of the pseudo-placement to create
     * @return - the pseudo-placement
     * @throws Exception if pseudoPlacementId is invalid
     * @throws UnsupportedOperationException if called on a non-panel definition
     */
    public Placement createPseudoPlacement(String pseudoPlacementId) throws Exception {
        if (this != this.definition) {
            throw new UnsupportedOperationException("Can only create pseudoPlacements for a panel definition");
        }
        return new PseudoPlacement(this, pseudoPlacementId);
    }
    
    /**
     * Gets a unique Id for a placement based on its location in the panel's family hierarchy
     * @param placementsParentLocation - the placement's parent PlacementsHolderLocation
     * @param placement - the placement
     * @return - the unique Id
     */
    public String getDescendantPlacementUniqueId(PlacementsHolderLocation<?> placementsParentLocation, Placement placement) {
        return placementsParentLocation.getUniqueId() + PlacementsHolderLocation.ID_DELIMITTER + placement.getId();
    }
    
    /**
     * Gets a placement's parent PlacementsHolderLocation and the placement itself given the 
     * placement's unique Id. Essentially the inverse of getDescendantPlacementUniqueId 
     * @param placementUniqueId - the placement's unique Id
     * @return - the placement's parent along with the actual placement
     */
    public Pair<List<PlacementsHolderLocation<?>>, Placement> getDescendantPlacement(String placementUniqueId) {
        for (PlacementsHolderLocation<?> child : children) {
            if (placementUniqueId.startsWith(child.getId())) {
                String remainderId = placementUniqueId.substring(child.getId().length());
                if (remainderId.startsWith(PlacementsHolderLocation.ID_DELIMITTER)) {
                    remainderId = remainderId.substring(1);
                }
                Placement placement = child.getPlacementsHolder().getPlacements().get(remainderId);
                if (placement != null) {
                    List<PlacementsHolderLocation<?>> ret = new ArrayList<>();
                    ret.add(child);
                    return new Pair<List<PlacementsHolderLocation<?>>, Placement>(ret, placement);
                }
                if (child instanceof PanelLocation) {
                    Pair<List<PlacementsHolderLocation<?>>, Placement> ret = ((PanelLocation) child).getPanel().getDescendantPlacement(remainderId);
                    if (ret != null) {
                        ret.first.add(0, child);
                        return ret;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Checks to see if the definition of a placementsHolder is the definition of this panel or or
     * any of its descendants
     * @param placementsHolder - the placementsHolder to check
     * @return - true if the definition of placementsHolder is the definition of this panel or or
     * any of its descendants
     */
    public boolean isDefinitionUsed(PlacementsHolder<?> placementsHolder) {
        if (this.getDefinition() == placementsHolder.getDefinition()) {
            return true;
        }
        List<PlacementsHolderLocation<?>> descendants = getDescendants();
        for (PlacementsHolderLocation<?> descendantLocation : descendants) {
            if (descendantLocation.getPlacementsHolder().getDefinition() == placementsHolder.getDefinition()) {
                return true;
            }
            if (descendantLocation.getPlacementsHolder() instanceof Panel) {
                if (((Panel) descendantLocation.getPlacementsHolder()).isDefinitionUsed(placementsHolder)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Returns a flattened list of all descendants of this panel
     * @return - the list of all descendants
     */
    public List<PlacementsHolderLocation<?>> getDescendants() {
        List<PlacementsHolderLocation<?>> retList = new ArrayList<>();
        for (PlacementsHolderLocation<?> child : children) {
            retList.add(child);
            if (child instanceof PanelLocation) {
                retList.addAll(((Panel) child.getPlacementsHolder()).getDescendants());
            }
        }
        return Collections.unmodifiableList(retList);
    }

    /**
     * Returns a flattened list of all BoardLocations that are descendants of this panel
     * @return - the list of all BoardLocations
     */
    public List<BoardLocation> getDescendantBoardLocations() {
        List<BoardLocation> retList = new ArrayList<>();
        for (PlacementsHolderLocation<?> child : children) {
            if (child instanceof BoardLocation) {
                retList.add((BoardLocation) child);
            }
            else if (child instanceof PanelLocation) {
                retList.addAll(((Panel) child.getPlacementsHolder()).getDescendantBoardLocations());
            }
        }
        return Collections.unmodifiableList(retList);
    }

    /**
     * Returns a flattened list of all PanelLocations that are descendants of this panel
     * @return - the list of all PanelLocations
     */
    public List<PanelLocation> getDescendantPanelLocations() {
        List<PanelLocation> retList = new ArrayList<>();
        for (PlacementsHolderLocation<?> child : children) {
            if (child instanceof PanelLocation) {
                retList.add((PanelLocation) child);
                retList.addAll(((Panel) child.getPlacementsHolder()).getDescendantPanelLocations());
            }
        }
        return Collections.unmodifiableList(retList);
    }

    /**
     * 
     * @return true if fiducials should be used to align this panel during a job
     */
    public boolean isCheckFiducials() {
        return this.checkFids;
    }

    /**
     * 
     * @return the version number of this panel
     */
    public Double getVersion() {
        return version;
    }
    
    /**
     * Counts the number of instances of a specified PlacementsHolder that occur within the panel's 
     * family hierarchy
     * @param placementsHolder - the PlacementsHolder to count
     * @return the count
     */
    public int getInstanceCount(PlacementsHolder<?> placementsHolder) {
        int instanceCount = 0;
        for (PlacementsHolderLocation<?> child : children) {
            if (child.getPlacementsHolder().isDefinition(placementsHolder.getDefinition())) {
                instanceCount++;
            }
            else if (child instanceof PanelLocation) {
                instanceCount += ((PanelLocation) child).getPanel().getInstanceCount(placementsHolder);
            }
        }
        return instanceCount;
    }
    
    /**
     * Checks whether the specified PlacementsHolder is a member of this Panel's family hierarchy
     * @param placementsHolder - the PlacementsHolder to check
     * @return true if it is a member
     */
    public boolean isMember(PlacementsHolder<?> placementsHolder) {
        for (PlacementsHolderLocation<?> child : children) {
            if (child.getPlacementsHolder() == placementsHolder) {
                return true;
            }
            else if (child instanceof PanelLocation && ((PanelLocation) child).getPanel().isMember(placementsHolder)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public String toString() {
        return String.format("Panel @%08x defined by @%08x: file %s, dims: %sx%s, fiducial count: %d, children: %d", hashCode(), definition != null ? definition.hashCode() : 0, file, dimensions.getLengthX(), dimensions.getLengthY(), placements.size(), children.size());
    }
}
