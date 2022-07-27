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
import java.awt.geom.NoninvertibleTransformException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openpnp.model.Board.Side;
import org.openpnp.model.Placement.Type;
import org.openpnp.util.IdentifiableList;
import org.openpnp.util.ResourceUtils;
import org.openpnp.util.Utils2D;
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
//    @ElementList(required = false)
//    protected IdentifiableList<PanelLocation> panelLocations = new IdentifiableList<>();

    private static final Double LATEST_VERSION = 2.0;
    
    @Attribute(required = false)
    protected Double version = null;
    
    @Deprecated
    @ElementList(required = false)
    protected ArrayList<Panel> panels = new ArrayList<>();

    @Deprecated
    @ElementList(required = false)
    private ArrayList<BoardLocation> boardLocations = new ArrayList<>();

    @ElementMap(required = false)
    private Map<String, Boolean> placed = new HashMap<>();

    @Element(required = false)
    private Panel rootPanel = new Panel("root");
    
    private transient File file;
    private transient boolean dirty;
    private transient final PanelLocation rootPanelLocation;
    
    public Job() {
        rootPanelLocation = new PanelLocation(rootPanel);
        rootPanelLocation.setLocalToParentTransform(new AffineTransform());
        Logger.trace(String.format("Created new Job Panel @%08x, defined by @%08x", rootPanelLocation.getPanel().hashCode(), rootPanelLocation.getPanel().getDefinedBy().hashCode()));
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
            panel.setDefinedBy(panel);
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
                    newPcb.setDefinedBy(newPcb);
                    newPcb.setSide(Side.Top);
//                    newPcb.setLocation(newPcb.getLocation().derive(0.0, 0.0, 0.0, null));
                    newPcb.getPlaced().clear();
                    
                    // Offset the sub PCB
                    newPcb.setLocation(new Location(configuration.getSystemUnits(),
                                    pcbStepX * i,
                                    pcbStepY * j, 0, 0));
                    
                    panel.addChild(newPcb);
                    
                    int boardNum = j*panel.columns + (rootBoardLocation.getSide() == Side.Top ? i : panel.columns - 1 - i);
                    BoardLocation subBoard = boardLocations.get(boardNum);
                    
                    String keyRoot = "Pnl1" + FiducialLocatableLocation.ID_SEPARATOR + newPcb.getUniqueId() + FiducialLocatableLocation.ID_SEPARATOR;
                    Map<String, Boolean> subBoardPlaced = subBoard.getPlaced();
                    for (String key : subBoardPlaced.keySet()) {
                        placed.put(keyRoot + key, subBoardPlaced.get(key));
//                        setPlaced(newPcb, key, subBoardPlaced.get(key));
                    }
                }
            }

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
            panelLocation.setLocation(rootBoardLocation.getLocation());
            panelLocation.setSide(rootBoardLocation.getSide());
//            this.addPanelLocation(panelLocation);
            rootPanel.addChild(panelLocation);
            
            dirty = true;
            panels = null;
            boardLocations = null;
        }
        else if (boardLocations != null){
            //Add board locations to the root panel
            for (BoardLocation boardLocation : boardLocations) {
//                rootPanelLocation.addChild(boardLocation);
                rootPanel.addChild(boardLocation);
//                boardLocation.addPropertyChangeListener(this);
                
                //Move the deprecated placement status from the boardLocation to the job
                Map<String, Boolean> temp = boardLocation.getPlaced();
                if (temp != null) {
//                    String prefix = boardLocation.getUniqueId() + FiducialLocatableLocation.ID_SEPARATOR;
                    for (String placementId : temp.keySet()) {
                        setPlaced(boardLocation, placementId, temp.get(placementId));
                    }
                }
            }
            boardLocations = null;
            dirty = true;
        }
        
        rootPanelLocation.setFiducialLocatable(rootPanel);
        rootPanelLocation.addPropertyChangeListener(this);
    }
    
    @Persist
    private void persist() {
        version = LATEST_VERSION;
//        //Remove deprecated items
//        panels = null;
//        
//        //Refresh the panel and board location lists with the children of the root panel
//        panelLocations.clear();
//        boardLocations.clear();
//        for (FiducialLocatableLocation child : rootPanelLocation.getChildren()) {
//            if (child instanceof PanelLocation) {
//                panelLocations.add((PanelLocation) child);
//            }
//            else if (child instanceof BoardLocation) {
//                boardLocations.add((BoardLocation) child);
//            }
//            else {
//                throw new UnsupportedOperationException("Instance type " + child.getClass() + " not supported.");
//            }
//        }
    }
    
    public List<BoardLocation> getBoardLocations() {
        return Collections.unmodifiableList(boardLocations);
    }

    public void addBoardLocation(BoardLocation boardLocation) {
//        Object oldValue = boardLocations;
//        boardLocations = new IdentifiableList<>(boardLocations);
//        boardLocation.setId(boardLocations.createId("Brd"));
//        boardLocations.add(boardLocation);
//        firePropertyChange("boardLocations", oldValue, boardLocations);
//        boardLocation.addPropertyChangeListener(this);
        rootPanelLocation.addChild(boardLocation);
    }

    public void removeBoardLocation(BoardLocation boardLocation) {
//        Object oldValue = boardLocations;
//        boardLocations = new IdentifiableList<>(boardLocations);
//        boardLocations.remove(boardLocation);
//        firePropertyChange("boardLocations", oldValue, boardLocations);
//        boardLocation.removePropertyChangeListener(this);
//        ((AbstractModelObject) boardLocation.getDefinedBy()).removePropertyChangeListener(boardLocation);
        rootPanelLocation.removeChild(boardLocation);
    }

//    public void removeAllBoards() {
//        ArrayList<BoardLocation> oldValue = boardLocations;
//        boardLocations = new IdentifiableList<>();
//
//        firePropertyChange("boardLocations", (Object) oldValue, boardLocations);
//
//        for (int i = 0; i < oldValue.size(); i++) {
//            oldValue.get(i).removePropertyChangeListener(this);
//            ((AbstractModelObject) oldValue.get(i).getDefinedBy()).removePropertyChangeListener(oldValue.get(i));
//        }
//    }
//
//    public void addPanel(Panel panel) {
//        panels.add(panel);
//    }
//
//    public void removeAllPanels() {
//        panels.clear();
//    }
//
    public List<Panel> getPanels() {
        return panels;
    }

    // In the first release of the Auto Panelize software, there is assumed to be a
    // single panel in use, even though the underlying plumbing supports a list of
    // panels. This function is intended to let the rest of OpenPNP know if the
    // autopanelize function is being used
//    public boolean isUsingPanel() {
//        if (panels == null) {
//            return false;
//        }
//
//        if (panels.size() >= 1) {
//            return true;
//        }
//
//        return false;
//    }

    public List<PanelLocation> getPanelLocations() {
        List<PanelLocation> ret = new ArrayList<>();
        for (FiducialLocatableLocation fll : getFiducialLocatableLocations()) {
            if (fll instanceof PanelLocation) {
                ret.add((PanelLocation) fll);
            }
        }
        return Collections.unmodifiableList(ret);
    }

    public void addPanelLocation(PanelLocation panelLocation) {
//        Object oldValue = panelLocations;
//        panelLocations = new IdentifiableList<>(panelLocations);
//        panelLocation.setId(panelLocations.createId("Pnl"));
//        panelLocations.add(panelLocation);
//        firePropertyChange("panelLocations", oldValue, panelLocations);
//        panelLocation.addPropertyChangeListener(this);
        rootPanelLocation.addChild(panelLocation);
    }

    public void removePanelLocation(PanelLocation panelLocation) {
//        Object oldValue = panelLocations;
//        panelLocations = new IdentifiableList<>(panelLocations);
//        panelLocations.remove(panelLocation);
//        firePropertyChange("boardLocations", oldValue, boardLocations);
//        panelLocation.removePropertyChangeListener(this);
        rootPanelLocation.removeChild(panelLocation);
    }

//    public void removeAllPanelLocations() {
//        List<PanelLocation> oldValue = panelLocations;
//        panelLocations = new IdentifiableList<>();
//
//        firePropertyChange("panelLocations", (Object) oldValue, panelLocations);
//
//        for (int i = 0; i < oldValue.size(); i++) {
//            oldValue.get(i).removePropertyChangeListener(this);
//        }
//    }

    private void panelLocationToList(PanelLocation panelLocation, List<FiducialLocatableLocation> list) {
        list.add(panelLocation);
        for (FiducialLocatableLocation child : panelLocation.getPanel().getChildren()) {
            if (child instanceof PanelLocation) {
                panelLocationToList((PanelLocation) child, list);
            }
            else if (child instanceof BoardLocation) {
                list.add((BoardLocation) child);
            }
            else {
                throw new UnsupportedOperationException("Instance type " + child.getClass() + " not supported.");
            }
        }
    }
    
    public List<FiducialLocatableLocation> getFiducialLocatableLocations() {
        List<FiducialLocatableLocation> retList = new ArrayList<>();
        panelLocationToList(rootPanelLocation, retList);
        return Collections.unmodifiableList(retList);
    }
    
    public void addFiducialLocatableLocation(FiducialLocatableLocation fiducialLocatableLocation) {
        if (fiducialLocatableLocation instanceof PanelLocation) {
            addPanelLocation((PanelLocation) fiducialLocatableLocation);
        }
        else if (fiducialLocatableLocation instanceof BoardLocation) {
            addBoardLocation((BoardLocation) fiducialLocatableLocation);
        }
        else {
            throw new UnsupportedOperationException("Instance type " + fiducialLocatableLocation.getClass() + " not supported.");
        }
    }

    public void removeFiducialLocatableLocation(FiducialLocatableLocation fiducialLocatableLocation) {
        if (fiducialLocatableLocation instanceof PanelLocation) {
            removePanelLocation((PanelLocation) fiducialLocatableLocation);
        }
        else if (fiducialLocatableLocation instanceof BoardLocation) {
            removeBoardLocation((BoardLocation) fiducialLocatableLocation);
        }
        else {
            throw new UnsupportedOperationException("Instance type " + fiducialLocatableLocation.getClass() + " not supported.");
        }
    }

    public List<FiducialLocatable> getFiducialLocatableInstancesOf(File file) {
        List<FiducialLocatable> ret = new ArrayList<>();
        if (file == null) {
            return ret;
        }
        for (FiducialLocatableLocation fiducialLocatableLocation : getFiducialLocatableLocations()) {
            if (fiducialLocatableLocation.getFiducialLocatable() != null) {
                if (file.equals(fiducialLocatableLocation.getFiducialLocatable().getFile())) {
                    ret.add(fiducialLocatableLocation.getFiducialLocatable());
                }
            }
        }
        return ret;
    }
    
    public int getNumberOfFiducialLocatableInstancesOf(File file) {
        return getFiducialLocatableInstancesOf(file).size();
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

    public int getTotalActivePlacements(FiducialLocatableLocation fiducialLocatableLocation) {
        if (fiducialLocatableLocation == null || fiducialLocatableLocation.getFiducialLocatable() == null) {
            return 0;
        }
        
        int counter = 0;
        if (fiducialLocatableLocation instanceof BoardLocation) {
            for(Placement placement : fiducialLocatableLocation.getFiducialLocatable().getPlacements()) {
                if (placement.getSide() == fiducialLocatableLocation.getSide()
                        && placement.getType() == Type.Placement
                        && placement.isEnabled()) {
                        counter++;
                }
            }
        }
        else if (fiducialLocatableLocation instanceof PanelLocation) {
            for (FiducialLocatableLocation child : ((PanelLocation) fiducialLocatableLocation).getPanel().getChildren()) {
                counter += getTotalActivePlacements(child);
            }
        }
        else {
            
        }
        return counter;
    }
    
    public int getActivePlacements(FiducialLocatableLocation fiducialLocatableLocation) {
        if (fiducialLocatableLocation == null || fiducialLocatableLocation.getFiducialLocatable() == null) {
            return 0;
        }
        
        int counter = 0;
        if (fiducialLocatableLocation instanceof BoardLocation) {
            for(Placement placement : fiducialLocatableLocation.getFiducialLocatable().getPlacements()) {
                if (placement.getSide() == fiducialLocatableLocation.getSide()
                        && placement.getType() == Type.Placement
                        && placement.isEnabled()
                        && !getPlaced(fiducialLocatableLocation, placement.getId())) {
                        counter++;
                }
            }
        }
        else if (fiducialLocatableLocation instanceof PanelLocation) {
            for (FiducialLocatableLocation child : ((PanelLocation) fiducialLocatableLocation).getPanel().getChildren()) {
                counter += getActivePlacements(child);
            }
        }
        else {
            
        }
        return counter;
    }

    public void setPlaced(FiducialLocatableLocation fiducialLocatableLocation, String placementId, boolean placed) {
        String key = fiducialLocatableLocation.getUniqueId() + FiducialLocatableLocation.ID_SEPARATOR + placementId;
        this.placed.put(key, placed);
        firePropertyChange("placed", null, this.placed);
    }

    public boolean getPlaced(FiducialLocatableLocation fiducialLocatableLocation, String placementId) {
        String key = fiducialLocatableLocation.getUniqueId() + FiducialLocatableLocation.ID_SEPARATOR + placementId;
        if (placed.containsKey(key)) {
            return placed.get(key);
        } 
        else {
            return false;
        }
    }
    
    public void removePlaced(FiducialLocatableLocation fiducialLocatableLocation, String placementId) {
        String key = fiducialLocatableLocation.getUniqueId() + FiducialLocatableLocation.ID_SEPARATOR + placementId;
        placed.remove(key);
    }
    
    public void clearAllPlaced() {
        this.placed.clear();
        firePropertyChange("placed", null, this.placed);
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
