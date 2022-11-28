/*
 * Copyright (C) 2022 Jason von Nieda <jason@vonnieda.org>, Tony Luken <tonyluken62+openpnp@gmail.com>
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

import java.beans.IndexedPropertyChangeEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openpnp.model.Abstract2DLocatable.Side;
import org.openpnp.util.IdentifiableList;
import org.openpnp.util.Pair;
import org.openpnp.util.Utils2D;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;

/**
 * A Panel is a PlacementsHolder that can also hold multiple Board and/or Panel Locations
 */
@Root(name = "openpnp-panel")
public class Panel extends PlacementsHolder<Panel> implements PropertyChangeListener {
    public static final Double LATEST_VERSION = 2.0;
    
    @Attribute(required = false)
    private Double version = null;
    
    @Deprecated
    @Element(required = false)
    public String id;
    @Deprecated
    @Element(required = false)
    public Integer columns = 1;
    @Deprecated
    @Element(required = false)
    public Integer rows = 1;
    @Deprecated
    @Element(required = false)
    public Length xGap;
    @Deprecated
    @Element(required = false)
    public Length yGap;
    @Deprecated
    @Element(required=false)
    private String partId;
    @Deprecated
    @Element(required=false)
    private Boolean checkFids;

    /**
     * @deprecated Use PlacementsHolder.placements instead
     */
    @Deprecated
    @ElementList(required = false)
    protected IdentifiableList<Placement> fiducials;
    
    @ElementList(required = false)
    protected IdentifiableList<PlacementsHolderLocation<?>> children = new IdentifiableList<>();
    
    @ElementList(required = false)
    protected ArrayList<String> pseudoPlacementIds = new ArrayList<>();
    
    protected transient IdentifiableList<Placement> pseudoPlacements = new IdentifiableList<>();
    
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
    
    public Panel() {
        super();
    }

    public Panel(File file) {
        super();
        this.version = LATEST_VERSION;
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
                newPanelLocation.addPropertyChangeListener(this);
            }
            else if (child instanceof BoardLocation) {
                BoardLocation newboardLocation = new BoardLocation((BoardLocation) child);
                addChild(newboardLocation);
                newboardLocation.addPropertyChangeListener(this);
            }
        }
        for (Placement pseudoPlacement : panel.pseudoPlacements) {
            Placement newPseudoPlacement = new Placement(pseudoPlacement);
            pseudoPlacements.add(newPseudoPlacement);
            newPseudoPlacement.addPropertyChangeListener("id", this);
        }
    }
    
    public Panel(String id) {
        super();
        this.version = LATEST_VERSION;
    }

    @Override
    public void dispose() {
        for (PlacementsHolderLocation<?> child : getChildren()) {
            child.removePropertyChangeListener(this);
            child.dispose();
        }
        for (Placement pseudoPlacement : pseudoPlacements) {
            pseudoPlacement.removePropertyChangeListener(this);
            pseudoPlacement.dispose();
        }
        super.dispose();
    }
    
    public List<PlacementsHolderLocation<?>> getChildren() {
        return children;
    }
    
    public void setChildren(IdentifiableList<PlacementsHolderLocation<?>> children) {
        Object oldValue = this.children;
        this.children = children;
        firePropertyChange("children", oldValue, children);
    }
    
    public PlacementsHolderLocation<?> getChild(int index) {
        return children.get(index);
    }
    
    public void setChild(int index, PlacementsHolderLocation<?> child) {
        if (this == this.definition) {
            throw new UnsupportedOperationException("Can not use setChild method to add/remove children from a Panel definition, use addChild or removeChild instead.");
        }
        if (child != null) {
            if (child instanceof BoardLocation) {
                child = new BoardLocation((BoardLocation) child.getDefinition());
            }
            else if (child instanceof PanelLocation) {
                child = new PanelLocation((PanelLocation) child.getDefinition());
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
    
    public void addChild(PlacementsHolderLocation<?> child) {
//        if (this != this.definition) {
//            throw new UnsupportedOperationException("This method should only be called on a Panel definition.");
//        }
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
//                        child.removePropertyChangeListener(pseudoPlacement);
                    }
                }
                children.remove(index);
                fireIndexedPropertyChange("child", index, child, null);
                child.removePropertyChangeListener(this);
                child.dispose();
            }
        }
    }
    
//    public void removeChild(int index) {
//        setChild(index, null);
//    }
    
    public void removeAllChildren() {
        List<PlacementsHolderLocation<?>> oldValue = children;
        for (PlacementsHolderLocation<?> child : oldValue) {
            removeChild(child);
        }
    }
    
    public List<String> getPseudoPlacementIds() {
        return pseudoPlacementIds;
    }

    public void setPseudoPlacementIds(ArrayList<String> pseudoPlacementIds) {
        Object oldValue = new ArrayList<>(this.pseudoPlacementIds);
        this.pseudoPlacementIds = pseudoPlacementIds;
        firePropertyChange("pseudoPlacementIds", oldValue, pseudoPlacementIds);
    }
    
    public List<Placement> getPseudoPlacements() {
        return pseudoPlacements;
    }
    
    public void setPseudoPlacements(IdentifiableList<Placement> pseudoPlacements) {
        Object oldValue = this.pseudoPlacements;
        this.pseudoPlacements = pseudoPlacements;
        firePropertyChange("pseudoPlacements", oldValue, pseudoPlacements);
    }
    
    public Placement getPseudoPlacement(int index) {
        if (index < 0 || index >= pseudoPlacements.size()) {
            Logger.trace("OhOh!");
        }
        return pseudoPlacements.get(index);
    }
    
    public void setPseudoPlacement(int index, Placement pseudoPlacement) {
        if (this == this.definition) {
            throw new UnsupportedOperationException("Use addPseudoPlacement to add a PseudoPlacement to a panel definition");
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
    
    public void addPseudoPlacement(Placement pseudoPlacement) {
        if (this != this.definition) {
            throw new UnsupportedOperationException("Can only add new pseudoPlacements to a panel definition");
        }
        if (pseudoPlacement != null) {
            Logger.trace(String.format("Adding pseudoPlacement to %s @%08x: %s @%08x", this.getClass().getSimpleName(), this.hashCode(), pseudoPlacement, pseudoPlacement.hashCode()));
            if (pseudoPlacements.get(pseudoPlacement.getId()) != null) {
                removePseudoPlacement(pseudoPlacements.get(pseudoPlacement.getId()));
            }
            pseudoPlacements.add(pseudoPlacement);
            pseudoPlacement.addPropertyChangeListener(this);
            fireIndexedPropertyChange("pseudoPlacement", pseudoPlacements.indexOf(pseudoPlacement), null, pseudoPlacement);
        }
    }

    public void removePseudoPlacement(Placement pseudoPlacement) {
        if (this != this.definition) {
            throw new UnsupportedOperationException("Can only remove pseudoPlacements from a panel definition");
        }
        String id = pseudoPlacement.getId();
        Pair<PlacementsHolderLocation<?>, Placement> pair = getDescendantPlacement(id);
        int index = pseudoPlacements.indexOf(pseudoPlacement);
        pseudoPlacements.remove(pseudoPlacement);
        fireIndexedPropertyChange("pseudoPlacement", index, pseudoPlacement, null);
        if (this == definition && pair != null && pair.second != null) {
            pair.second.definition.removePropertyChangeListener("location", pseudoPlacement);
            pair.second.definition.removePropertyChangeListener("side", pseudoPlacement);
            pair.second.definition.removePropertyChangeListener("id", pseudoPlacement);
            PlacementsHolderLocation<?> next = pair.first;
            while (next != null) {
                next.definition.removePropertyChangeListener("placementsHolder", pseudoPlacement);
                next.definition.removePropertyChangeListener("location", pseudoPlacement);
                next.definition.removePropertyChangeListener("side", pseudoPlacement);
                next.definition.removePropertyChangeListener("id", pseudoPlacement);
                if (next == pair.first) {
                    next.definition.placementsHolder.definition.removePropertyChangeListener("placement", pseudoPlacement);
                }
                else {
                    next.definition.placementsHolder.definition.removePropertyChangeListener("child", pseudoPlacement);
                }
                if (next.placementsHolder == this) {
                    break;
                }
                next = next.parent;
            }
        }
        pseudoPlacement.removePropertyChangeListener(this);
        pseudoPlacement.dispose();
    }
    
    public Placement createPseudoPlacement(String pseudoPlacementId) {
        if (this != this.definition) {
            throw new UnsupportedOperationException("Can only create pseudoPlacements for a panel definition");
        }
        Pair<PlacementsHolderLocation<?>, Placement> pair = getDescendantPlacement(pseudoPlacementId);
        Placement pseudoPlacement = new Placement(pair.second) {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt instanceof IndexedPropertyChangeEvent) {
                    Pair<PlacementsHolderLocation<?>, Placement> pair2 = getDescendantPlacement(this.id);
                    if (evt.getNewValue() == null && pair2 == null) {
                        Logger.trace("IndexedPropertyChangeEvent handled by PseudoPlacement " + this.id + " " + evt.getSource() + " " + evt.getPropertyName() + " " + evt.getNewValue());
                        removePseudoPlacement(this);
                    }
                }
                else {
                    Logger.trace("PropertyChangeEvent handled by PseudoPlacement " + this.id + " " + evt.getSource() + " " + evt.getPropertyName() + " " + evt.getNewValue());
                    if (evt.getPropertyName().equals("id") && evt.getSource() != this) {
                        String searchStr = (String) evt.getOldValue();
                        String[] idParts = this.id.split(PlacementsHolderLocation.ID_DELIMITTER);
                        for (int i=0; i<idParts.length; i++) {
                            if (searchStr.equals(idParts[i])) {
                                //have a possible match, need to construct a new Id and test it
                                String newId;
                                if (i == 0) {
                                    newId = (String) evt.getNewValue();
                                }
                                else {
                                    newId = idParts[0];
                                }
                                for (int j=1; j<idParts.length; j++) {
                                    newId = newId + PlacementsHolderLocation.ID_DELIMITTER;
                                    if (j != i) {
                                        newId = newId + idParts[j];
                                    }
                                    else {
                                        newId = newId + (String) evt.getNewValue();
                                    }
                                }
                                if (Panel.this.definition.getDescendantPlacement(newId) != null) {
                                    //found it!
                                    setId(newId);
                                    return;
                                }
                            }
                        }
                        Logger.warn("Unable to find matching id!!!!!!!!!!!!!!!!");
                    }
                    else {
                        if (evt.getPropertyName().equals("id")) {
                            Logger.trace("hu?");
                        }
                        Pair<PlacementsHolderLocation<?>, Placement> pair = Panel.this.definition.getDescendantPlacement(pseudoPlacementId);
                        Location location = Utils2D.calculateBoardPlacementLocation(pair.first, pair.second).derive(null, null, 0.0, null);
                        Side side = pair.second.getSide().flip(pair.first.getGlobalSide() == Side.Bottom);
                        if (children.get(0).getParent() != null) {
                            if (children.get(0).getParent().getGlobalSide() == Side.Bottom) {
                                location = location.invert(false, false, false, true);
                            }
                        }
                        else if (side == Side.Bottom) {
                            location = location.invert(false, false, false, true);
                        }
                        setLocation(location);
                        setSide(side);
                    }
                }
            }
        };
        
        pseudoPlacement.setDefinition(pseudoPlacement);
        pseudoPlacement.removePropertyChangeListener(pseudoPlacement);
        pseudoPlacement.setEnabled(true);
        Location location = Utils2D.calculateBoardPlacementLocation(pair.first, pair.second).derive(null, null, 0.0, null);
        Side side = pair.second.getSide().flip(pair.first.getGlobalSide() == Side.Bottom);
        if (children.get(0).getParent() != null) {
            if (children.get(0).getParent().getGlobalSide() == Side.Bottom) {
                location = location.invert(false, false, false, true);
            }
        }
        else if (side == Side.Bottom) {
            location = location.invert(false, false, false, true);
        }
        pseudoPlacement.setLocation(location);
        pseudoPlacement.setId(pseudoPlacementId);
        pseudoPlacement.setSide(side);
        pseudoPlacement.setComments("Pseudo-placement for panel alignment only");

        pair.second.definition.addPropertyChangeListener("location", pseudoPlacement);
        pair.second.definition.addPropertyChangeListener("side", pseudoPlacement);
        pair.second.definition.addPropertyChangeListener("id", pseudoPlacement);
        PlacementsHolderLocation<?> next = pair.first;
        while (next != null) {
            next.definition.addPropertyChangeListener("placementsHolder", pseudoPlacement);
            next.definition.addPropertyChangeListener("location", pseudoPlacement);
            next.definition.addPropertyChangeListener("side", pseudoPlacement);
            next.definition.addPropertyChangeListener("id", pseudoPlacement);
            Logger.trace(String.format("Added property change listener %s @%08x to %s @%08x", pseudoPlacement.id, pseudoPlacement.hashCode(), next.definition.getClass().getSimpleName(), next.definition.hashCode()));
            if (next == pair.first) {
                next.definition.placementsHolder.definition.addPropertyChangeListener("placement", pseudoPlacement);
            }
            else {
                next.definition.placementsHolder.definition.addPropertyChangeListener("child", pseudoPlacement);
            }
            if (next.placementsHolder == this) {
                break;
            }
            next = next.parent;
        }
        
        return pseudoPlacement;
    }
    
    public String getDescendantPlacementUniqueId(PlacementsHolderLocation<?> placementsParentLocation, Placement placement) {
        return placementsParentLocation.getUniqueId() + PlacementsHolderLocation.ID_DELIMITTER + placement.getId();
    }
    
    public Pair<PlacementsHolderLocation<?>, Placement> getDescendantPlacement(String placementUniqueId) {
        for (PlacementsHolderLocation<?> child : children) {
            if (placementUniqueId.startsWith(child.getId())) {
                String remainderId = placementUniqueId.substring(child.getId().length());
                if (remainderId.startsWith(PlacementsHolderLocation.ID_DELIMITTER)) {
                    remainderId = remainderId.substring(1);
                }
                Placement placement = child.getPlacementsHolder().getPlacements().get(remainderId);
                if (placement != null) {
                    return new Pair<PlacementsHolderLocation<?>, Placement>(child, placement);
                }
                if (child instanceof PanelLocation) {
                    Pair<PlacementsHolderLocation<?>, Placement> ret = ((PanelLocation) child).getPanel().getDescendantPlacement(remainderId);
                    if (ret != null) {
                        return ret;
                    }
                }
            }
        }
        return null;
    }
    
    public boolean isDescendant(PlacementsHolder<?> placementsHolder) {
        List<PlacementsHolderLocation<?>> descendants = getDescendants();
        for (PlacementsHolderLocation<?> descendantLocation : descendants) {
            if (descendantLocation.getPlacementsHolder() == placementsHolder) {
                return true;
            }
            if (descendantLocation.getPlacementsHolder() instanceof Panel) {
                if (((Panel) descendantLocation.getPlacementsHolder()).isDefinition(placementsHolder)) {
                    return true;
                }
            }
        }
        return false;
    }
    
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

    public boolean isCheckFiducials() {
        return this.checkFids;
    }

    public Double getVersion() {
        return version;
    }
    
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
    
    @Override
    public String toString() {
        return String.format("Panel @%08x defined by @%08x: file %s, dims: %sx%s, fiducial count: %d, children: %d", hashCode(), definition != null ? definition.hashCode() : 0, file, dimensions.getLengthX(), dimensions.getLengthY(), placements.size(), children.size());
    }
}
