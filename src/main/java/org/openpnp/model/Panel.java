package org.openpnp.model;


import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    public static final String LATEST_VERSION = "1.0";
    
    @Attribute(required = false)
    private String version = null;
    
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
    protected List<FiducialLocatableLocation> children = new ArrayList<>();
    
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
        this.children = new ArrayList<>();
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

    public void setChildren(List<FiducialLocatableLocation> children) {
        this.children = children;
    }

    public void addChild(FiducialLocatableLocation child) {
        Object oldValue = children;
        children = new ArrayList<>(children);
        children.add(child);
        firePropertyChange("children", oldValue, children);
        if (child != null) {
            child.addPropertyChangeListener(this);
        }
    }
    
    public void removeChild(FiducialLocatableLocation child) {
        Object oldValue = children;
        children = new ArrayList<>(children);
        children.remove(child);
        firePropertyChange("children", oldValue, children);
        if (child != null) {
            child.removePropertyChangeListener(this);
        }
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

    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        Object oldValue = this.version;
        this.version = version;
        firePropertyChange("version", oldValue, version);
    }
    
    @Override
    public String toString() {
        return String.format("Panel: file %s, dims: %sx%s, fiducial count: %d, children: %d", file, dimensions.getLengthX(), dimensions.getLengthY(), placements.size(), children.size());
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        super.propertyChange(evt);
    }

}
