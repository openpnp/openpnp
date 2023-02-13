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


import java.beans.IndexedPropertyChangeEvent;
import java.beans.PropertyChangeEvent;
import java.util.List;

import org.openpnp.util.Pair;
import org.openpnp.util.Utils2D;
import org.pmw.tinylog.Logger;

/**
 * A PseudoPlacement is a copy of a placement or fiducial located on one of a panel's descendants 
 * that is only used for panel alignment.
 */
public class PseudoPlacement extends Placement {

    protected Panel panel;
    protected List<PlacementsHolderLocation<?>> branches;
    protected PlacementsHolderLocation<?> tip;
    protected Placement leaf;
    
    /**
     * Constructs a pseudo-placement for the specified Panel with the specified Id
     * @param panel - the Panel for which the pseudo-placement is to be used for alignment
     * @param pseudoPlacementId - the unique id of the actual Placement that is to be used
     * @throws Exception 
     */
    public PseudoPlacement(Panel panel, String pseudoPlacementId) throws Exception {
        super(pseudoPlacementId);
        removePropertyChangeListener(this);
        setEnabled(true);
        this.panel = panel;
        Pair<List<PlacementsHolderLocation<?>>, Placement> pair = panel.getDescendantPlacement(pseudoPlacementId);
        if (pair == null || pair.second == null) {
            throw new Exception(String.format("Invalid pseudoPlacementId \"%s\" for %s", pseudoPlacementId, panel.toString()));
        }
        branches = pair.first;
        leaf = pair.second;
        tip = pair.first.get(pair.first.size()-1);
        Location location = Utils2D.calculateBoardPlacementLocation(tip, leaf).derive(null, null, 0.0, null);
        Side side = leaf.getSide().flip(tip.getGlobalSide() == Side.Bottom);
        if (panel.children.get(0).getParent() != null) {
            if (panel.children.get(0).getParent().getGlobalSide() == Side.Bottom) {
                location = location.invert(false, false, false, true);
            }
        }
        else if (side == Side.Bottom) {
            location = location.invert(false, false, false, true);
        }
        setPart(leaf.getPart());
        setType(leaf.getType());
        setLocation(location);
        setSide(side);
        if (leaf.getType() == Placement.Type.Placement) {
            setComments("Pseudo-placement, for panel alignment only");
        }
        else {
            setComments("Pseudo-fiducial, for panel alignment only");
        }
        
        //Listen for changes to the real placement that may affect this pseudo-placement
        leaf.definition.addPropertyChangeListener("location", this);
        leaf.definition.addPropertyChangeListener("side", this);
        leaf.definition.addPropertyChangeListener("id", this);
        leaf.definition.addPropertyChangeListener("part", this);
        leaf.definition.addPropertyChangeListener("type", this);
        
        for (PlacementsHolderLocation<?> phl : branches) {
            //Listen for changes to each of the PlacementsHolderLocations that are part of the 
            //branch that leads to the real placement
            phl.definition.addPropertyChangeListener("placementsHolder", this);
            phl.definition.addPropertyChangeListener("location", this);
            phl.definition.addPropertyChangeListener("side", this);
            phl.definition.addPropertyChangeListener("id", this);
            if (phl == tip) {
                //At the tip of the branch, we need to listen for changes to the placements list. 
                //Changes here will be due to an IndexedPropertyChangeEvent
                phl.definition.placementsHolder.definition.addPropertyChangeListener("placement", this);
            }
            else {
                //For all other parts of the branch, we need to listen for changes to the children 
                //list. Again, changes here will be due to an IndexedPropertyChangeEvent
                phl.definition.placementsHolder.definition.addPropertyChangeListener("child", this);
            }
        }
        addPropertyChangeListener(this);
    }
    
    /**
     * Clean-up all the property change listeners
     */
    @Override
    public void dispose() {
        leaf.definition.removePropertyChangeListener("location", this);
        leaf.definition.removePropertyChangeListener("side", this);
        leaf.definition.removePropertyChangeListener("id", this);
        leaf.definition.removePropertyChangeListener("part", this);
        leaf.definition.removePropertyChangeListener("type", this);
        
        for (PlacementsHolderLocation<?> phl : branches) {
            phl.definition.removePropertyChangeListener("placementsHolder", this);
            phl.definition.removePropertyChangeListener("location", this);
            phl.definition.removePropertyChangeListener("side", this);
            phl.definition.removePropertyChangeListener("id", this);
            if (phl == tip) {
                if (phl.definition.placementsHolder.definition != null) {
                    phl.definition.placementsHolder.definition.removePropertyChangeListener("placement", this);
                }
                else {
                    phl.definition.placementsHolder.removePropertyChangeListener("placement", this);
                }
            }
            else {
                phl.definition.placementsHolder.definition.removePropertyChangeListener("child", this);
            }
        }
        super.dispose();
    }
    
    /**
     * Tests to see if the specified PlacementsHolderLocation is somewhere in the branch containing
     * the real placement associated with this pseudo-placement.
     */
    protected boolean isInBranches(PlacementsHolderLocation<?> placementsHolderLocation) {
        for (PlacementsHolderLocation<?> phl : branches) {
            if ((phl == placementsHolderLocation) || (phl.definition == placementsHolderLocation) ||
                    (phl == placementsHolderLocation.definition) || (phl.definition == placementsHolderLocation.definition)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() == this) {
            super.propertyChange(evt);
            return;
        }
        if (evt instanceof IndexedPropertyChangeEvent) {
            IndexedPropertyChangeEvent iPCE = (IndexedPropertyChangeEvent) evt;
            if (iPCE.getNewValue() == null && 
                    ((iPCE.getPropertyName().equals("placement") && leaf.definition.equals(((Placement) iPCE.getOldValue()).definition)) || 
                    (iPCE.getPropertyName().equals("child") && isInBranches((PlacementsHolderLocation<?>) iPCE.getOldValue())))) {
                panel.removePseudoPlacement(this);
            }
        }
        else {
            if (evt.getPropertyName().equals("id")) {
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
                        Pair<List<PlacementsHolderLocation<?>>, Placement> testPair = panel.definition.getDescendantPlacement(newId);
                        if (testPair != null && testPair.second != null) {
                            //found it!
                            setId(newId);
                            return;
                        }
                    }
                }
                Logger.warn("Unable to find matching id!"); //Probably should throw an exception here
            }
            else if (evt.getPropertyName().equals("part")) {
                setPart((Part) evt.getNewValue());
            }
            else if (evt.getPropertyName().equals("type")) {
                setType((Placement.Type) evt.getNewValue());
                if (((Placement.Type) evt.getNewValue()) == Placement.Type.Placement) {
                    setComments("Pseudo-placement, for panel alignment only");
                }
                else {
                    setComments("Pseudo-fiducial, for panel alignment only");
                }
            }
            else if ((evt.getPropertyName().equals("location") || evt.getPropertyName().equals("side")) && evt.getSource() != this){
                Location location = Utils2D.calculateBoardPlacementLocation(tip, leaf).derive(null, null, 0.0, null);
                Side side = leaf.getSide().flip(tip.getGlobalSide() == Side.Bottom);
                if (panel.children.get(0).getParent() != null) {
                    if (panel.children.get(0).getParent().getGlobalSide() == Side.Bottom) {
                        location = location.invert(false, false, false, true);
                    }
                }
                else if (side == Side.Bottom) {
                    location = location.invert(false, false, false, true);
                }
                setLocation(location);
                setSide(side);
            }
            else {
                super.propertyChange(evt);
            }
        }
    }
    
}
