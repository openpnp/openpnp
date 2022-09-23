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
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openpnp.model.Abstract2DLocatable.Side;
import org.openpnp.model.Placement.Type;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;

/**
 * A Job specifies a list of one or more PanelLocations and/or BoardLocations.
 */
@Root(name = "openpnp-job")
public class Job extends AbstractModelObject implements PropertyChangeListener {
    private static final Double LATEST_VERSION = 2.0;
    
    @Attribute(required = false)
    protected Double version = null;
    
    @Deprecated
    @ElementList(required = false)
    protected ArrayList<Panel> panels = new ArrayList<>();

    @Deprecated
    @ElementList(required = false)
    private ArrayList<BoardLocation> boardLocations = new ArrayList<>();

    @Element(required = false)
    private Panel rootPanel = new Panel("root");
    
    @ElementMap(required = false)
    private Map<String, Boolean> placed = new HashMap<>();

    @ElementMap(required = false)
    private Map<String, Boolean> enabled = new HashMap<>();

    @ElementMap(required = false)
    private Map<String, Boolean> checkFiducials = new HashMap<>();

    @ElementMap(required = false)
    private Map<String, Placement.ErrorHandling> errorHandling = new HashMap<>();

    
    private transient File file;
    private transient boolean dirty;
    private transient final PanelLocation rootPanelLocation;
    
    public Job() {
        rootPanelLocation = new PanelLocation(rootPanel);
        rootPanelLocation.setLocalToParentTransform(new AffineTransform());
        rootPanelLocation.setCheckFiducials(false);
        Logger.trace(String.format("Created new Job Panel @%08x, defined by @%08x", rootPanelLocation.getPanel().hashCode(), rootPanelLocation.getPanel().getDefinition().hashCode()));
        addPropertyChangeListener(this);
    }

    @Commit
    private void commit() {
         if (panels != null && !panels.isEmpty()) {
            //Convert deprecated list of Panels to list of PanelLocations
             
            //We need to create a new panel, populate it with the boards in the job, add the new 
            //panel to the configuration, and then add a panelLocation to the job's rootPanel that 
            //references it
            
            //First we need the root board location for the panel, this is the one that originally 
            //set the origin of the panel
            BoardLocation rootBoardLocation = boardLocations.get(0);
            
            //Now create a file for the new panel, we'll just use the root board's file name except 
            //change it to end with ".panel.xml"
            String boardFileName = rootBoardLocation.getFileName();
            String panelFileName = boardFileName.substring(0, boardFileName.indexOf(".board.xml")) + ".panel.xml";
            File panelFile = new File(panelFileName);
            
            Panel panel = panels.get(0);
            panel.setFile(panelFile);
            panel.setName(panelFile.getName());
            panel.setDefinition(panel);
            panel.setDimensions(Location.origin);
            
            Configuration configuration = Configuration.get();
            try {
                configuration.resolveBoard(this, rootBoardLocation);
            }
            catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            Location rootDims = rootBoardLocation.getBoard().getDimensions().
                    convertToUnits(configuration.getSystemUnits());
            
            panel.setDimensions(Location.origin.deriveLengths(
                    rootDims.getLengthX().add(panel.xGap).multiply(panel.columns).subtract(panel.xGap),
                    rootDims.getLengthY().add(panel.yGap).multiply(panel.rows).subtract(panel.yGap),
                    null, null));
            
            double pcbStepX = rootDims.getLengthX().add(panel.xGap).getValue();
            double pcbStepY = rootDims.getLengthY().add(panel.yGap).getValue();
            
            for (int j = 0; j < panel.rows; j++) {
                for (int i = 0; i < panel.columns; i++) {
                    // deep copy the existing rootPcb
                    BoardLocation newPcb = new BoardLocation(rootBoardLocation);
                    newPcb.setParent(null);
                    newPcb.setDefinition(newPcb);
                    newPcb.setGlobalSide(Side.Top);
                    newPcb.getPlaced().clear();
                    
                    // Offset the sub PCB
                    newPcb.setLocation(new Location(configuration.getSystemUnits(),
                                    pcbStepX * i,
                                    pcbStepY * j, 0, 0));
                    
                    panel.addChild(newPcb);
                    
                    int boardNum = j*panel.columns + (rootBoardLocation.getGlobalSide() == Side.Top ? i : panel.columns - 1 - i);
                    BoardLocation subBoard = boardLocations.get(boardNum);
                    
                    String keyRoot = PanelLocation.ID_PREFIX + "1" + PlacementsHolderLocation.ID_DELIMITTER + newPcb.getUniqueId();
                    Map<String, Boolean> subBoardPlaced = subBoard.getPlaced();
                    for (String key : subBoardPlaced.keySet()) {
                        placed.put(keyRoot + key, subBoardPlaced.get(key));
                    }
                }
            }
            boolean savedCheckFids = panel.isCheckFiducials();
            
            try {
                configuration.savePanel(panel);
            }
            catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            configuration.addPanel(panel);
            
            PanelLocation panelLocation = new PanelLocation();
            panelLocation.setFileName(panelFileName);
            panelLocation.setGlobalLocation(rootBoardLocation.getGlobalLocation());
            panelLocation.setGlobalSide(rootBoardLocation.getGlobalSide());
            panelLocation.setCheckFiducials(savedCheckFids);
            rootPanel.addChild(panelLocation);
            
            dirty = true;
            panels = null;
            boardLocations = null;
        }
        else if (boardLocations != null){
            //Add board locations to the root panel
            for (BoardLocation boardLocation : boardLocations) {
                rootPanel.addChild(boardLocation);
                
                //Move the deprecated placement status from the boardLocation to the job
                Map<String, Boolean> temp = boardLocation.getPlaced();
                if (temp != null) {
                    for (String placementId : temp.keySet()) {
                        setPlaced(boardLocation, placementId, temp.get(placementId));
                    }
                }
            }
            boardLocations = null;
            dirty = true;
        }
        
        rootPanelLocation.setPlacementsHolder(rootPanel);
    }
    
    @Persist
    private void persist() {
        version = LATEST_VERSION;
        panels = null;
    }
    
    public List<BoardLocation> getBoardLocations() {
        return rootPanelLocation.getPanel().getDescendantBoardLocations();
    }

    public List<PanelLocation> getPanelLocations() {
        List<PanelLocation> retList = new ArrayList<>();
        retList.add(rootPanelLocation);
        retList.addAll(rootPanelLocation.getPanel().getDescendantPanelLocations());
        return Collections.unmodifiableList(retList);
    }

    public List<PlacementsHolderLocation<?>> getBoardAndPanelLocations() {
        List<PlacementsHolderLocation<?>> retList = new ArrayList<>();
        retList.add(rootPanelLocation);
        retList.addAll(rootPanelLocation.getPanel().getDescendants());
        return Collections.unmodifiableList(retList);
    }
    
    public void addBoardOrPanelLocation(PlacementsHolderLocation<?> boardOrPanelLocation) {
        rootPanelLocation.addChild(boardOrPanelLocation);
        boardOrPanelLocation.addPropertyChangeListener(this);
        firePropertyChange("rootPanelLocation", null, rootPanelLocation);
    }

    public void removeBoardOrPanelLocation(PlacementsHolderLocation<?> boardOrPanelLocation) {
        rootPanelLocation.removeChild(boardOrPanelLocation);
        boardOrPanelLocation.removePropertyChangeListener(this);
        firePropertyChange("rootPanelLocation", null, rootPanelLocation);
    }

    public int instanceCount(PlacementsHolder<?> boardOrPanel) {
        return rootPanelLocation.getPanel().getInstanceCount(boardOrPanel);
    }
    
    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        Object oldValue = this.file;
        this.file = file;
        firePropertyChange("file", oldValue, file);
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        boolean oldValue = this.dirty;
        this.dirty = dirty;
        firePropertyChange("dirty", oldValue, dirty);
    }

    public PanelLocation getRootPanelLocation() {
        return rootPanelLocation;
    }

    public int getTotalActivePlacements(PlacementsHolderLocation<?> placementsHolderLocation) {
        if (placementsHolderLocation == null || placementsHolderLocation.getPlacementsHolder() == null ||
                !placementsHolderLocation.isEnabled()) {
            return 0;
        }
        
        int counter = 0;
        if (placementsHolderLocation instanceof BoardLocation) {
            for(Placement placement : placementsHolderLocation.getPlacementsHolder().getPlacements()) {
                if (placement.getSide() == placementsHolderLocation.getGlobalSide()
                        && placement.getType() == Type.Placement
                        && placement.isEnabled()) {
                        counter++;
                }
            }
        }
        else if (placementsHolderLocation instanceof PanelLocation) {
            for (PlacementsHolderLocation<?> child : ((PanelLocation) placementsHolderLocation).getPanel().getChildren()) {
                counter += getTotalActivePlacements(child);
            }
        }
        else {
            throw new UnsupportedOperationException("Instance type " + placementsHolderLocation.getClass() + " not supported.");
        }
        return counter;
    }
    
    public int getActivePlacements(PlacementsHolderLocation<?> placementsHolderLocation) {
        if (placementsHolderLocation == null || placementsHolderLocation.getPlacementsHolder() == null ||
                !placementsHolderLocation.isEnabled()) {
            return 0;
        }
        
        int counter = 0;
        if (placementsHolderLocation instanceof BoardLocation) {
            for(Placement placement : placementsHolderLocation.getPlacementsHolder().getPlacements()) {
                if (placement.getSide() == placementsHolderLocation.getGlobalSide()
                        && placement.getType() == Type.Placement
                        && placement.isEnabled()
                        && !getPlaced(placementsHolderLocation, placement.getId())) {
                        counter++;
                }
            }
        }
        else if (placementsHolderLocation instanceof PanelLocation) {
            for (PlacementsHolderLocation<?> child : ((PanelLocation) placementsHolderLocation).getPanel().getChildren()) {
                counter += getActivePlacements(child);
            }
        }
        else {
            throw new UnsupportedOperationException("Instance type " + placementsHolderLocation.getClass() + " not supported.");
        }
        return counter;
    }

    public void setPlaced(PlacementsHolderLocation<?> placementsHolderLocation, String placementId, boolean placed) {
        String key = placementsHolderLocation.getUniqueId() + PlacementsHolderLocation.ID_DELIMITTER + placementId;
        this.placed.put(key, placed);
        firePropertyChange("placed", null, this.placed);
    }

    public boolean getPlaced(PlacementsHolderLocation<?> placementsHolderLocation, String placementId) {
        String key = placementsHolderLocation.getUniqueId() + PlacementsHolderLocation.ID_DELIMITTER + placementId;
        if (placed.containsKey(key)) {
            return placed.get(key);
        } 
        else {
            return false;
        }
    }
    
    public void removePlaced(PlacementsHolderLocation<?> placementsHolderLocation, String placementId) {
        String key = placementsHolderLocation.getUniqueId() + PlacementsHolderLocation.ID_DELIMITTER + placementId;
        placed.remove(key);
        firePropertyChange("placed", null, placed);
    }
    
    public void clearAllPlaced() {
        placed.clear();
        firePropertyChange("placed", null, placed);
    }
    
    public void setEnabled(PlacementsHolderLocation<?> placementsHolderLocation, Placement placement, boolean enabled) {
        String key = placementsHolderLocation.getUniqueId();
        if (placement != null) {
            key += PlacementsHolderLocation.ID_DELIMITTER + placement.getId();
        }
        this.enabled.put(key, enabled);
        firePropertyChange("enabled", null, this.enabled);
    }

    public boolean getEnabled(PlacementsHolderLocation<?> placementsHolderLocation, Placement placement) {
        String key = placementsHolderLocation.getUniqueId();
        if (placement != null) {
            key += PlacementsHolderLocation.ID_DELIMITTER + placement.getId();
        }
        if (enabled.containsKey(key)) {
            return enabled.get(key);
        } 
        else {
            return placement != null ? placement.isEnabled() : placementsHolderLocation.isLocallyEnabled();
        }
    }
    
    public void removeEnabled(PlacementsHolderLocation<?> placementsHolderLocation, Placement placement) {
        String key = placementsHolderLocation.getUniqueId();
        if (placement != null) {
            key += PlacementsHolderLocation.ID_DELIMITTER + placement.getId();
        }
        enabled.remove(key);
        firePropertyChange("enabled", null, enabled);
    }
    
    public void removeAllEnabled() {
        enabled.clear();
        firePropertyChange("enabled", null, enabled);
    }
    
    public void setCheckFiducials(PlacementsHolderLocation<?> placementsHolderLocation, boolean enabled) {
        String key = placementsHolderLocation.getUniqueId();
        this.checkFiducials.put(key, enabled);
        firePropertyChange("checkFiducials", null, this.checkFiducials);
    }

    public boolean getCheckFiducials(PlacementsHolderLocation<?> placementsHolderLocation) {
        String key = placementsHolderLocation.getUniqueId();
        if (checkFiducials.containsKey(key)) {
            return checkFiducials.get(key);
        } 
        else {
            return placementsHolderLocation.isCheckFiducials();
        }
    }
    
    public void removeCheckFiducials(PlacementsHolderLocation<?> placementsHolderLocation) {
        String key = placementsHolderLocation.getUniqueId();
        checkFiducials.remove(key);
        firePropertyChange("checkFiducials", null, checkFiducials);
    }
    
    public void removeAllCheckFiducials() {
        checkFiducials.clear();
        firePropertyChange("checkFiducials", null, checkFiducials);
    }
    
    public void setErrorHandling(PlacementsHolderLocation<?> placementsHolderLocation, Placement placement, Placement.ErrorHandling errorHandling) {
        String key = placementsHolderLocation.getUniqueId() + PlacementsHolderLocation.ID_DELIMITTER + placement.getId();
        this.errorHandling.put(key, errorHandling);
        firePropertyChange("errorHandling", null, this.errorHandling);
    }

    public Placement.ErrorHandling getErrorHandling(PlacementsHolderLocation<?> placementsHolderLocation, Placement placement) {
        String key = placementsHolderLocation.getUniqueId() + PlacementsHolderLocation.ID_DELIMITTER + placement.getId();
        if (errorHandling.containsKey(key)) {
            return errorHandling.get(key);
        } 
        else {
            return placement.getErrorHandling();
        }
    }
    
    public void removeErrorHandling(PlacementsHolderLocation<?> placementsHolderLocation, Placement placement) {
        String key = placementsHolderLocation.getUniqueId() + PlacementsHolderLocation.ID_DELIMITTER + placement.getId();
        errorHandling.remove(key);
        firePropertyChange("errorHandling", null, this.errorHandling);
    }
    
    public void removeAllErrorHandling() {
        errorHandling.clear();
        firePropertyChange("errorHandling", null, errorHandling);
    }
    
    /**
     * @return the version
     */
    public Double getVersion() {
        return version;
    }

    public void propertyChange(PropertyChangeEvent evt) {
        Logger.trace("PropertyChangeEvent = " + evt);
        if (evt.getSource() != Job.this || !evt.getPropertyName().equals("dirty")) {
            setDirty(true);
        }
    }
}
