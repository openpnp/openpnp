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

import org.openpnp.model.Placement.Type;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Persist;

/**
 * A Job specifies a list of one or more PanelLocations and/or BoardLocations.
 */
@Root(name = "openpnp-job")
public class Job extends AbstractModelObject implements PropertyChangeListener {
    private static final Double LATEST_VERSION = 2.0;
    
    @Attribute(required = false)
    protected Double version = null;
    
    /**
     * @deprecated All Panels are now held as children of the job's rootPanel
     */
    @Deprecated
    @ElementList(required = false)
    protected ArrayList<Panel> panels = null;

    /**
     * @deprecated All Boards are now held as children of the job's rootPanel
     */
    @Deprecated
    @ElementList(required = false)
    protected ArrayList<BoardLocation> boardLocations = null;

    @Element(required = false)
    protected Panel rootPanel = new Panel();
    
    @ElementMap(required = false)
    protected Map<String, Boolean> placedStatusMap = new HashMap<>();

    @ElementMap(required = false)
    protected Map<String, Boolean> enabledStateMap = new HashMap<>();

    @ElementMap(required = false)
    protected Map<String, Boolean> checkFiducialsStateMap = new HashMap<>();

    @ElementMap(required = false)
    protected Map<String, Placement.ErrorHandling> errorHandlingStateMap = new HashMap<>();

    
    protected transient File file;
    protected transient boolean dirty;
    protected transient final PanelLocation rootPanelLocation;
    
    public Job() {
        rootPanelLocation = new PanelLocation(rootPanel);
        rootPanelLocation.setLocalToParentTransform(new AffineTransform());
        rootPanelLocation.setCheckFiducials(false);
        addPropertyChangeListener(this);
    }

    @Persist
    private void persist() {
        version = LATEST_VERSION;
        
        //Remove the deprecated items 
        panels = null;
        boardLocations = null;
    }
    
    /**
     *
     * @return a flattened list of all BoardLocations held by the job
     */
    public List<BoardLocation> getBoardLocations() {
        return Collections.unmodifiableList(rootPanelLocation.getPanel().getDescendantBoardLocations());
    }

    /**
    *
    * @return a flattened list of all PanleLocations held by the job
    */
    public List<PanelLocation> getPanelLocations() {
        List<PanelLocation> retList = new ArrayList<>();
        retList.add(rootPanelLocation);
        retList.addAll(rootPanelLocation.getPanel().getDescendantPanelLocations());
        return Collections.unmodifiableList(retList);
    }

    /**
    *
    * @return a flattened list of all PanelLocations and BoardLocations held by the job
    */
    public List<PlacementsHolderLocation<?>> getBoardAndPanelLocations() {
        List<PlacementsHolderLocation<?>> retList = new ArrayList<>();
        retList.add(rootPanelLocation);
        retList.addAll(rootPanelLocation.getPanel().getDescendants());
        return Collections.unmodifiableList(retList);
    }
    
    /**
     * Adds a BoardLocation or PanelLocation to the job
     * @param boardOrPanelLocation - the BoardLocation or PanelLocation
     */
    public void addBoardOrPanelLocation(PlacementsHolderLocation<?> boardOrPanelLocation) {
        rootPanelLocation.addChild(boardOrPanelLocation);
        boardOrPanelLocation.addPropertyChangeListener(this);
        firePropertyChange("rootPanelLocation", null, rootPanelLocation);
    }

    /**
     * Removes a BoardLocation or PanelLocation from the job
     * @param boardOrPanelLocation - the BoardLocation or PanelLocation
     */
    public void removeBoardOrPanelLocation(PlacementsHolderLocation<?> boardOrPanelLocation) {
        boardOrPanelLocation.removePropertyChangeListener(this);
        rootPanelLocation.removeChild(boardOrPanelLocation);
        firePropertyChange("rootPanelLocation", null, rootPanelLocation);
    }

    /**
     * Counts the number of instances of a Board or Panel that are held by the job
     * @param boardOrPanel - the Board or Panel whose instances are to be counted
     * @return - the count
     */
    public int instanceCount(PlacementsHolder<?> boardOrPanel) {
        return rootPanelLocation.getPanel().getInstanceCount(boardOrPanel);
    }
    
    /**
     * Gets the file where the job is stored in the file system
     * @return - the file
     */
    public File getFile() {
        return file;
    }

    /**
     * Sets the file where the job is stored in the file system
     * @param file - the file
     */
    public void setFile(File file) {
        Object oldValue = this.file;
        this.file = file;
        firePropertyChange("file", oldValue, file);
    }

    /**
     * Checks to see if the Job has been modified
     * @return - true if the Job has been modified
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Set the flag that indicates if the Job has been modified
     * @param dirty - the state to set the flag
     */
    public void setDirty(boolean dirty) {
        boolean oldValue = this.dirty;
        this.dirty = dirty;
        firePropertyChange("dirty", oldValue, dirty);
    }

    /**
     * Gets the root PanelLocation of the Job. This contains all the BoardLocations and/or
     * PanelLocations for the Job.
     * @return - the root PanelLocation
     */
    public PanelLocation getRootPanelLocation() {
        return rootPanelLocation;
    }

    /**
     * Gets the total number of Placements held by a PlacementsHolderLocation and it descendants
     * that are facing up on the machine and are enabled.
     * @param placementsHolderLocation - the PlacementsHolderLocation
     * @return the total number of active placements
     */
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
    
    /**
     * Gets the number of Placements held by a PlacementsHolderLocation and its descendants that are
     * facing up on the machine, are enabled, and have not yet been placed.
     * @param placementsHolderLocation - the PlacementsHolderLocation
     * @return the number of active placements
     */
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
                        && !retrievePlacedStatus(placementsHolderLocation, placement.getId())) {
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

    /**
     * Stores the placed status of a Placement in a way that is uniquely identifiable to it   
     * @param placementsHolderLocation - the PlacementsHolderLocation that contains the Placement
     * @param placementId - the id of the Placement
     * @param placed - the status to be stored
     */
    public void storePlacedStatus(PlacementsHolderLocation<?> placementsHolderLocation, String placementId, boolean placed) {
        String key = placementsHolderLocation.getUniqueId() + PlacementsHolderLocation.ID_DELIMITTER + placementId;
        this.placedStatusMap.put(key, placed);
        firePropertyChange("placed", null, this.placedStatusMap);
    }

    /**
     * Retrieves the stored placed status of the Placement
     * @param placementsHolderLocation - the PlacementsHolderLocation that contains the Placement
     * @param placementId - the id of the Placement
     * @return the placed status of the Placement if one has previously been stored, otherwise
     * returns false
     */
    public boolean retrievePlacedStatus(PlacementsHolderLocation<?> placementsHolderLocation, String placementId) {
        String key = placementsHolderLocation.getUniqueId() + PlacementsHolderLocation.ID_DELIMITTER + placementId;
        if (placedStatusMap.containsKey(key)) {
            return placedStatusMap.get(key);
        } 
        else {
            return false;
        }
    }
    
    /**
     * Removes the stored placed status of a Placement
     * @param placementsHolderLocation - the PlacementsHolderLocation that contains the Placement
     * @param placementId - the id of the Placement
     */
    public void removePlacedStatus(PlacementsHolderLocation<?> placementsHolderLocation, String placementId) {
        String key = placementsHolderLocation.getUniqueId() + PlacementsHolderLocation.ID_DELIMITTER + placementId;
        if (placedStatusMap.remove(key) != null) {
            firePropertyChange("placed", null, placedStatusMap);
        }
    }
    
    /**
     * Removes all stored placed status 
     */
    public void removeAllPlacedStatus() {
        placedStatusMap.clear();
        firePropertyChange("placed", null, placedStatusMap);
    }
    
    /**
     * Stores the enabled state of a Placement or PlacementsHolderLocation in a way that is 
     * uniquely identifiable to it
     * @param placementsHolderLocation - the PlacementsHolderLocation holding the Placement
     * @param placement - the Placement whose state is to be stored or null if the 
     * PlacementsHolderLocation state is to be stored
     * @param enabledStateMap - the enabled state to be stored
     */
    public void storeEnabledState(PlacementsHolderLocation<?> placementsHolderLocation, Placement placement, boolean enabled) {
        String key = placementsHolderLocation.getUniqueId();
        if (placement != null) {
            key += PlacementsHolderLocation.ID_DELIMITTER + placement.getId();
        }
        this.enabledStateMap.put(key, enabled);
        firePropertyChange("enabled", null, this.enabledStateMap);
    }

    /**
     * Retrieves the stored enabled state of a Placement or PlacementsHolderLocation 
     * @param placementsHolderLocation - the PlacementsHolderLocation holding the Placement
     * @param placement - the Placement whose state is to be retrieved or null if the 
     * PlacementsHolderLocation state is to be retrieved
     * @return the stored enabled state if one has been previously stored, otherwise returns the
     * current enabled state of the Placement or PlacementsHolderLocation
     */
    public boolean retrieveEnabledState(PlacementsHolderLocation<?> placementsHolderLocation, Placement placement) {
        String key = placementsHolderLocation.getUniqueId();
        if (placement != null) {
            key += PlacementsHolderLocation.ID_DELIMITTER + placement.getId();
        }
        if (enabledStateMap.containsKey(key)) {
            return enabledStateMap.get(key);
        } 
        else {
            return placement != null ? placement.isEnabled() : placementsHolderLocation.isLocallyEnabled();
        }
    }
    
    /**
     * Removes the stored enabled state of a PlacementsHolderLocation or Placement
     * @param placementsHolderLocation - the placementsHolderLocation that contains the placement
     * @param placement - the Placement whose state is to be removed or null if the 
     * PlacementsHolderLocation state is to be removed
     */
    public void removeEnabledState(PlacementsHolderLocation<?> placementsHolderLocation, Placement placement) {
        String key = placementsHolderLocation.getUniqueId();
        if (placement != null) {
            key += PlacementsHolderLocation.ID_DELIMITTER + placement.getId();
        }
        enabledStateMap.remove(key);
        firePropertyChange("enabled", null, enabledStateMap);
    }
    
    /**
     * Removes all stored enabled states
     */
    public void removeAllEnabledState() {
        enabledStateMap.clear();
        firePropertyChange("enabled", null, enabledStateMap);
    }
    
    /**
     * Stores the check fiducials state of a PlacementsHolderLocation in a way that is uniquely
     * identifiable to it
     * @param placementsHolderLocation - the PlacementsHolderLocation whose state is to be stored
     * @param enabledStateMap - the check fiducials state to be stored
     */
    public void storeCheckFiducialsState(PlacementsHolderLocation<?> placementsHolderLocation, boolean enabled) {
        String key = placementsHolderLocation.getUniqueId();
        this.checkFiducialsStateMap.put(key, enabled);
        firePropertyChange("checkFiducials", null, this.checkFiducialsStateMap);
    }

    /**
     * Retrieves the stored check fiducials state of a PlacementsHolderLocation
     * @param placementsHolderLocation - the PlacementsHolderLocation whose state is to be retrieved
     * @return the stored check fiducials state if one has been previously stored, otherwise returns
     * the current check fiducials state of the PlacementsHolderLocation
     */
    public boolean retrieveCheckFiducialsState(PlacementsHolderLocation<?> placementsHolderLocation) {
        String key = placementsHolderLocation.getUniqueId();
        if (checkFiducialsStateMap.containsKey(key)) {
            return checkFiducialsStateMap.get(key);
        } 
        else {
            return placementsHolderLocation.isCheckFiducials();
        }
    }
    
    /**
     * Removes the stored check fiducials state of a PlacementsHolderLocation
     * @param placementsHolderLocation - the PlacementsHolderLocation whose state is to be removed
     */
    public void removeCheckFiducialsState(PlacementsHolderLocation<?> placementsHolderLocation) {
        String key = placementsHolderLocation.getUniqueId();
        checkFiducialsStateMap.remove(key);
        firePropertyChange("checkFiducials", null, checkFiducialsStateMap);
    }
    
    /**
     * Removes all stored check fiducial states
     */
    public void removeAllCheckFiducialsState() {
        checkFiducialsStateMap.clear();
        firePropertyChange("checkFiducials", null, checkFiducialsStateMap);
    }
    
    /**
     * Stores the error handling state of a Placement in a way that is uniquely identifiable to it   
     * @param placementsHolderLocation - the PlacementsHolderLocation that contains the Placement
     * @param placement - the Placement
     * @param errorHandling - the state to be stored
     */
    public void storeErrorHandlingState(PlacementsHolderLocation<?> placementsHolderLocation, Placement placement, Placement.ErrorHandling errorHandling) {
        String key = placementsHolderLocation.getUniqueId() + PlacementsHolderLocation.ID_DELIMITTER + placement.getId();
        this.errorHandlingStateMap.put(key, errorHandling);
        firePropertyChange("errorHandling", null, this.errorHandlingStateMap);
    }

    /**
     * Retrieves the stored error handling state of a Placement   
     * @param placementsHolderLocation - the PlacementsHolderLocation that contains the Placement
     * @param placement - the Placements whose stored state is to be retrieved
     * @return - the stored error handling state if one was previously stored, otherwise returns
     * the current error handling state of the Placement
     */
    public Placement.ErrorHandling retrieveErrorHandlingState(PlacementsHolderLocation<?> placementsHolderLocation, Placement placement) {
        String key = placementsHolderLocation.getUniqueId() + PlacementsHolderLocation.ID_DELIMITTER + placement.getId();
        if (errorHandlingStateMap.containsKey(key)) {
            return errorHandlingStateMap.get(key);
        } 
        else {
            return placement.getErrorHandling();
        }
    }
    
    /**
     * Removes the stored error handling state of a Placement   
     * @param placementsHolderLocation - the PlacementsHolderLocation that contains the Placement
     * @param placement - the Placements whose stored state is to be removed
     */
    public void removeErrorHandlingState(PlacementsHolderLocation<?> placementsHolderLocation, Placement placement) {
        String key = placementsHolderLocation.getUniqueId() + PlacementsHolderLocation.ID_DELIMITTER + placement.getId();
        errorHandlingStateMap.remove(key);
        firePropertyChange("errorHandling", null, this.errorHandlingStateMap);
    }
    
    /**
     * Removes all stored error handling states
     */
    public void removeAllErrorHandlingState() {
        errorHandlingStateMap.clear();
        firePropertyChange("errorHandling", null, errorHandlingStateMap);
    }
    
    /**
     * @return the job's version
     */
    public Double getVersion() {
        return version;
    }

    /**
     * Processes property change events for the job to set the flag indicating the job has been 
     * modified
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() != Job.this || !evt.getPropertyName().equals("dirty")) {
            setDirty(true);
        }
    }
}
