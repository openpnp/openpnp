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

import org.openpnp.model.Board.Side;
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
    
    @Element(required=false)
    private String defaultFiducialPartId;
    
    @Element
    private boolean checkFids;

    /**
     * @deprecated Use FiducialLocatable.placements instead
     */
    @Deprecated
    @ElementList(required = false)
    protected IdentifiableList<Placement> fiducials;
    
    @ElementList(required = false)
    protected ArrayList<String> pseudoPlacementIds = new ArrayList<>();
    
    @ElementList(required = false)
    protected IdentifiableList<PlacementsHolderLocation<?>> children = new IdentifiableList<>();
    
    @Commit
    private void commit() {
        //Converted deprecated elements
        if (fiducials != null) {
            defaultFiducialPartId = partId;
            placements.addAll(fiducials);
        }
        id = null;
        
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
        this.version = panel.version;
        this.defaultFiducialPartId = panel.defaultFiducialPartId;
        this.checkFids = panel.checkFids;
        for (PlacementsHolderLocation<?> child : panel.getChildren()) {
            if (child instanceof PanelLocation) {
                this.addChild(new PanelLocation((PanelLocation) child));
            }
            else if (child instanceof BoardLocation) {
                this.addChild(new BoardLocation((BoardLocation) child));
            }
        }
        for (String pseudoPlacementId : panel.pseudoPlacementIds) {
            this.pseudoPlacementIds.add(pseudoPlacementId);
        }
    }
    
    public Panel(String id) {
        super();
        this.version = LATEST_VERSION;
    }

    public List<String> getPseudoPlacementIds() {
        return pseudoPlacementIds;
    }

    @Override
    public void dispose() {
        for (PlacementsHolderLocation<?> child : getChildren()) {
            child.dispose();
        }
        super.dispose();
    }
    
    public void setPseudoPlacementIds(ArrayList<String> pseudoPlacementIds) {
        Object oldValue = new ArrayList<>(this.pseudoPlacementIds);
        this.pseudoPlacementIds = pseudoPlacementIds;
        firePropertyChange("pseudoPlacementIds", oldValue, pseudoPlacementIds);
    }
    
    public List<Placement> getPseudoPlacements() {
        IdentifiableList<Placement> pseudoPlacements = new IdentifiableList<>();
        for (String pseudoPlacementId : getPseudoPlacementIds()) {
            Pair<PlacementsHolderLocation<?>, Placement> pair = definedBy.getDescendantPlacement(pseudoPlacementId);
            Placement pseudoPlacement = new Placement(pair.second);
            pseudoPlacement.setDefinedBy(pseudoPlacement);
            pseudoPlacement.setEnabled(true);
            pseudoPlacement.setLocation(Utils2D.calculateBoardPlacementLocation(pair.first, pair.second).derive(null, null, 0.0, null));
            pseudoPlacement.setId(pseudoPlacementId);
            pseudoPlacement.setSide(pair.second.getSide().flip(pair.first.getGlobalSide() == Side.Bottom));
            pseudoPlacement.setComments("For panel alignment only");
            pseudoPlacements.add(pseudoPlacement);
        }
        return pseudoPlacements;
    }
    
    @Override
    public IdentifiableList<Placement> getPlacements() {
        IdentifiableList<Placement> allPlacements = super.getPlacements();
        allPlacements.addAll(getPseudoPlacements());
        return allPlacements;
    }

    public String getDescendantPlacementUniqueId(PlacementsHolderLocation<?> placementsParentLocation, Placement placement) {
        return placementsParentLocation.getUniqueId() + placement.getId();
    }
    
    public Pair<PlacementsHolderLocation<?>, Placement> getDescendantPlacement(String placementUniqueId) {
        for (PlacementsHolderLocation<?> child : children) {
            if (placementUniqueId.startsWith(child.getId())) {
                String remainderId = placementUniqueId.substring(child.getId().length());
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
    
    public List<PlacementsHolderLocation<?>> getChildren() {
        return Collections.unmodifiableList(children);
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

//    public void setChildren(IdentifiableList<FiducialLocatableLocation> children) {
//        Object oldValue = new IdentifiableList<>(this.children);
//        this.children = children;
//        firePropertyChange("children", oldValue, children);
//    }
//
//    public void setChildren(int index, FiducialLocatableLocation child) {
//        children.add(index, child);
//    }

    public void addChild(PlacementsHolderLocation<?> child) {
        String childId = child.getId();
        if (childId == null || children.get(childId) != null) {
            if (child instanceof BoardLocation) {
                childId = children.createId("Brd");
            }
            else if (child instanceof PanelLocation) {
                childId = children.createId("Pnl");
            }
        }
        if (child != null) {
            child.setId(childId);
            children.add(child);
            fireIndexedPropertyChange("children", children.indexOf(child), null, child);
            child.addPropertyChangeListener(this);
            child.getDefinedBy().addPropertyChangeListener(child);
        }
        
    }
    
    public void removeChild(PlacementsHolderLocation<?> child) {
        if (child != null) {
            int index = children.indexOf(child);
            if (index >= 0) {
                children.remove(child);
                fireIndexedPropertyChange("children", index, child, null);
                child.removePropertyChangeListener(this);
                child.getDefinedBy().removePropertyChangeListener(child);
            }
        }
    }
    
    public void removeChild(int index) {
        if (index >= 0 && index < children.size()) {
            PlacementsHolderLocation<?> child = children.get(index);
            children.remove(index);
            fireIndexedPropertyChange("children", index, child, null);
            child.removePropertyChangeListener(this);
            child.getDefinedBy().removePropertyChangeListener(child);
        }
    }
    
    public void removeAllChildren() {
        Object oldValue = children;
        for (PlacementsHolderLocation<?> child : children) {
            child.removePropertyChangeListener(this);
            child.getDefinedBy().removePropertyChangeListener(child);
        }
        children = new IdentifiableList<>(children);
        firePropertyChange("children", oldValue, children);
    }
    
    public String getDefaultFiducialPartId() {
        return this.defaultFiducialPartId;
    }

    public void setDefaultFiducialPartId(String defaultFiducialPartId) {
        Object oldValue = this.defaultFiducialPartId;
        this.defaultFiducialPartId = defaultFiducialPartId;
        firePropertyChange("defaultFiducialPartId", oldValue, defaultFiducialPartId);
    }
    
    public Part getDefaultFiducialPart() {
        if (defaultFiducialPartId == null) {
            return null;
        }
        return Configuration.get().getPart(defaultFiducialPartId);
    }
    
    public void setDefaultFiducialPart(Part fiducialPart) {
        if (fiducialPart == null) {
            setDefaultFiducialPartId(null);
        }
        setDefaultFiducialPartId(fiducialPart.getId());
    }

    public boolean isCheckFiducials() {
        return this.checkFids;
    }

    public void setCheckFiducials(boolean checkFids) {
        Object oldValue = this.checkFids;
        this.checkFids = checkFids;
        firePropertyChange("checkFids", oldValue, checkFids);
    }

    public Double getVersion() {
        return version;
    }
    
    @Override
    public String toString() {
        return String.format("Panel @%08x defined by @%08x: file %s, dims: %sx%s, fiducial count: %d, children: %d", hashCode(), definedBy.hashCode(), file, dimensions.getLengthX(), dimensions.getLengthY(), placements.size(), children.size());
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
//        Logger.trace(String.format("PropertyChangeEvent handled by Panel @%08x = %s", this.hashCode(), evt));
        if (evt.getSource() != Panel.this && evt.getSource() == definedBy && evt.getPropertyName().equals("children") && evt instanceof IndexedPropertyChangeEvent) {
            Logger.trace(String.format("Attempting to set %s @%08x property %s = %s", this.getClass().getSimpleName(), this.hashCode(), evt.getPropertyName(), evt.getNewValue()));
            int index = ((IndexedPropertyChangeEvent) evt).getIndex();
            if (evt.getNewValue() instanceof BoardLocation) {
                addChild(new BoardLocation((BoardLocation) evt.getNewValue()));
            }
            else if (evt.getNewValue() instanceof PanelLocation) {
                addChild(new PanelLocation((PanelLocation) evt.getNewValue()));
            }
            else if (evt.getOldValue() != null) {
                removeChild(index);
            }
        }
        super.propertyChange(evt);
    }

}
