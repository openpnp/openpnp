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
import java.util.List;

import org.openpnp.util.IdentifiableList;
import org.openpnp.util.ResourceUtils;
import org.openpnp.util.Utils2D;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;

/**
 * A Job specifies a list of one or more BoardLocations.
 */
@Root(name = "openpnp-job")
public class Job extends AbstractModelObject implements PropertyChangeListener {
    @ElementList(required = false)
    protected IdentifiableList<PanelLocation> panelLocations = new IdentifiableList<>();

    @Deprecated
    @ElementList(required = false)
    protected IdentifiableList<Panel> panels = new IdentifiableList<>();

    @ElementList
    private ArrayList<BoardLocation> boardLocations = new ArrayList<>();

    private transient File file;
    private transient boolean dirty;

    public Job() {
        addPropertyChangeListener(this);
    }

    @SuppressWarnings("unused")
    @Commit
    private void commit() {
        for (BoardLocation boardLocation : boardLocations) {
            boardLocation.addPropertyChangeListener(this);
        }
        for (PanelLocation panelLocation : panelLocations) {
            panelLocation.addPropertyChangeListener(this);
        }
        
        //Convert deprecated list of Panels to list of PanelLocations
        if (panels != null && !panels.isEmpty()) {
            Panel panel = panels.get(0);
//            String boardFileName = boardLocations.get(0).getBoardFile();
//            String panelFileName = boardFileName.substring(0, boardFileName.indexOf(".board.xml")) + ".panel.xml";
            PanelLocation panelLocation = new PanelLocation(panel);
//            panelLocation.setPanelFile(panelFileName);
            panelLocation.setParent(null);
            panelLocation.setLocation(boardLocations.get(0).getLocation());
            panelLocation.setId(panel.getId());
//            AffineTransform panelLocalToRoot = panelLocation.getLocalToRootTransform();
//            for (BoardLocation boardLocation : boardLocations) {
//                
//                AffineTransform boardLocalToPanel = Utils2D.getDefaultBoardPlacementLocationTransform(boardLocation);
//                try {
//                    boardLocalToPanel.concatenate(panelLocalToRoot.createInverse());
//                }
//                catch (NoninvertibleTransformException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//                boardLocation.setLocalToParentTransform(boardLocalToPanel);
//                Location newBoardLocation = Utils2D.calculateBoardPlacementLocation(boardLocation, Location.origin);
//                newBoardLocation = newBoardLocation.convertToUnits(boardLocation.getLocation().getUnits());
//                newBoardLocation = newBoardLocation.derive(null, null, boardLocation.getLocation().getZ(), null);
//                boardLocation.setLocation(newBoardLocation);
//                panelLocation.getPanel().getChildren().add(boardLocation);
//            }
//            boardLocations.clear();
            panelLocation.addPropertyChangeListener(this);
            panelLocations.add(panelLocation);
            panels = null;
        }
    }
    
    public List<BoardLocation> getBoardLocations() {
        return Collections.unmodifiableList(boardLocations);
    }

    public void addBoardLocation(BoardLocation boardLocation) {
        Object oldValue = boardLocations;
        boardLocations = new ArrayList<>(boardLocations);
        boardLocations.add(boardLocation);
        firePropertyChange("boardLocations", oldValue, boardLocations);
        boardLocation.addPropertyChangeListener(this);
    }

    public void removeBoardLocation(BoardLocation boardLocation) {
        Object oldValue = boardLocations;
        boardLocations = new ArrayList<>(boardLocations);
        boardLocations.remove(boardLocation);
        firePropertyChange("boardLocations", oldValue, boardLocations);
        boardLocation.removePropertyChangeListener(this);
    }

    public void removeAllBoards() {
        ArrayList<BoardLocation> oldValue = boardLocations;
        boardLocations = new ArrayList<>();

        firePropertyChange("boardLocations", (Object) oldValue, boardLocations);

        for (int i = 0; i < oldValue.size(); i++) {
            oldValue.get(i).removePropertyChangeListener(this);
        }
    }

    public void addPanel(Panel panel) {
        panels.add(panel);
    }

    public void removeAllPanels() {
        panels.clear();
    }

    public IdentifiableList<Panel> getPanels() {
        return panels;
    }

    // In the first release of the Auto Panelize software, there is assumed to be a
    // single panel in use, even though the underlying plumbing supports a list of
    // panels. This function is intended to let the rest of OpenPNP know if the
    // autopanelize function is being used
    public boolean isUsingPanel() {
        if (panelLocations == null) {
            return false;
        }

        if (panelLocations.size() >= 1) {
            return true;
        }

        return false;
    }

    public IdentifiableList<PanelLocation> getPanelLocations() {
        return panelLocations;
    }

    public void setPanelLocations(IdentifiableList<PanelLocation> panelLocations) {
        Object oldValue = this.panelLocations;
        this.panelLocations = panelLocations;
        firePropertyChange("panelLocations", oldValue, panelLocations);
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

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() != Job.this || !evt.getPropertyName().equals("dirty")) {
            setDirty(true);
        }
    }
}
