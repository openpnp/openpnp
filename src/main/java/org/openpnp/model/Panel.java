package org.openpnp.model;


import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.beans.IndexedPropertyChangeEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;
import org.openpnp.model.Board.Side;
import org.openpnp.util.IdentifiableList;
import org.openpnp.util.Utils2D;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;

@Root(name = "openpnp-panel")
public class Panel extends FiducialLocatable implements PropertyChangeListener {
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
    protected IdentifiableList<FiducialLocatableLocation> children = new IdentifiableList<>();
    
    @Commit
    private void commit() {
        //Converted deprecated elements
        if (fiducials != null) {
            defaultFiducialPartId = partId;
            placements.addAll(fiducials);
        }
        id = null;
        
        for (FiducialLocatableLocation child : children) {
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
        addPropertyChangeListener(this);
    }

    public Panel(File file) {
        super();
        this.version = LATEST_VERSION;
        setFile(file);
        addPropertyChangeListener(this);
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
        for (FiducialLocatableLocation child : panel.getChildren()) {
            if (child instanceof PanelLocation) {
                this.addChild(new PanelLocation((PanelLocation) child));
            }
            else if (child instanceof BoardLocation) {
                this.addChild(new BoardLocation((BoardLocation) child));
            }
        }
        addPropertyChangeListener(this);
    }
    
    public Panel(String id) {
        super();
        this.version = LATEST_VERSION;
        addPropertyChangeListener(this);
    }

    public void setTo(Panel panel) {
        super.setTo(panel);
        this.version = panel.version;
        this.defaultFiducialPartId = panel.defaultFiducialPartId;
        this.checkFids = panel.checkFids;
        this.children = new IdentifiableList<>();
        for (FiducialLocatableLocation child : panel.getChildren()) {
            if (child instanceof PanelLocation) {
                this.addChild(new PanelLocation((PanelLocation) child));
            }
            else if (child instanceof BoardLocation) {
                this.addChild(new BoardLocation((BoardLocation) child));
            }
        }
    }
    
    public List<Placement> getFiducials() {
        return getPlacements();
    }
    
    public List<FiducialLocatableLocation> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public void setChildren(IdentifiableList<FiducialLocatableLocation> children) {
        this.children = children;
    }

    public void setChildren(int index, FiducialLocatableLocation child) {
        children.add(index, child);
    }

    public void addChild(FiducialLocatableLocation child) {
        String childId = null;
        if (child instanceof BoardLocation) {
            childId = children.createId("Brd");
        }
        else if (child instanceof PanelLocation) {
            childId = children.createId("Pnl");
        }
        if (child != null) {
            child.setId(childId);
            children.add(child);
            fireIndexedPropertyChange("children", children.indexOf(child), null, child);
            child.addPropertyChangeListener(this);
            child.getDefinedBy().addPropertyChangeListener(child);
        }
        
    }
    
    public void removeChild(FiducialLocatableLocation child) {
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
    
    public void removeAllChildren() {
        Object oldValue = children;
        for (FiducialLocatableLocation child : children) {
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
        Logger.trace(String.format("PropertyChangeEvent handled by Panel @%08x = %s", this.hashCode(), evt));
        if (evt.getSource() != this && evt.getSource() == definedBy && evt.getPropertyName().equals("children")) {
            dirty = true;
            int index = ((IndexedPropertyChangeEvent) evt).getIndex();
            if (evt.getNewValue() instanceof BoardLocation) {
                BoardLocation boardLocation = new BoardLocation((BoardLocation) evt.getNewValue());
                boardLocation.addPropertyChangeListener(this);
                boardLocation.getDefinedBy().addPropertyChangeListener(boardLocation);
                children.add(boardLocation);
            }
            else if (evt.getNewValue() instanceof PanelLocation) {
                PanelLocation panelLocation = new PanelLocation((PanelLocation) evt.getNewValue());
                panelLocation.addPropertyChangeListener(this);
                panelLocation.getDefinedBy().addPropertyChangeListener(panelLocation);
                children.add(panelLocation);
            }
            else if (evt.getOldValue() != null) {
                FiducialLocatableLocation fiducialLocatableLocation = children.get(index);
                fiducialLocatableLocation.removePropertyChangeListener(this);
                fiducialLocatableLocation.getDefinedBy().removePropertyChangeListener(fiducialLocatableLocation);
                children.remove(index);
            }
        }
        else {
            super.propertyChange(evt);
        }
    }

}
