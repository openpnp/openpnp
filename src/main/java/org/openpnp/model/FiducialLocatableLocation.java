package org.openpnp.model;

import java.awt.geom.AffineTransform;

import org.openpnp.model.Board.Side;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.core.Commit;

public class FiducialLocatableLocation extends AbstractLocatable implements Identifiable {

    @Attribute(required = false)
    String id;
    
    @Attribute
    protected Side side = Side.Top;

    @Attribute(required = false)
    protected String fileName;

    @Attribute(required = false)
    protected String parentId = new String("Root");

    @Attribute(required = false)
    protected boolean checkFiducials;

    @Attribute(required = false)
    protected boolean enabled = true;

    protected FiducialLocatableLocation parent;
    protected FiducialLocatable fiducialLocatable;

    /**
     * Important note: The transform is in Millimeters.
     */
    protected AffineTransform localToParentTransform;
    
    @Commit
    private void commit() {
        setLocation(location);
        setFiducialLocatable(fiducialLocatable);
    }
    
    public FiducialLocatableLocation() {
        super(new Location(LengthUnit.Millimeters));
    }
    
    public FiducialLocatableLocation(FiducialLocatableLocation fiducialLocatableLocation) {
        super(fiducialLocatableLocation.getLocation());
        this.id = fiducialLocatableLocation.id;
        this.side = fiducialLocatableLocation.side;
        this.fileName = fiducialLocatableLocation.fileName;
        this.parentId = fiducialLocatableLocation.parentId;
        this.checkFiducials = fiducialLocatableLocation.checkFiducials;
        this.enabled = fiducialLocatableLocation.enabled;
        if (fiducialLocatableLocation.parent != null) {
            this.parent = new FiducialLocatableLocation(fiducialLocatableLocation.parent);
        }
        else {
            this.parent = null;
        }
        if (fiducialLocatableLocation.fiducialLocatable != null) {
            this.fiducialLocatable = new FiducialLocatable(fiducialLocatableLocation.fiducialLocatable);
        }
        else {
            this.fiducialLocatable = null;
        }
        if (fiducialLocatableLocation.localToParentTransform != null) {
            this.localToParentTransform = new AffineTransform(fiducialLocatableLocation.localToParentTransform);
        }
        else {
            this.localToParentTransform = null;
        }
        
    }
    
    public FiducialLocatableLocation(FiducialLocatable fiducialLocatable) {
        this();
        setFiducialLocatable(fiducialLocatable);
    }

    public Side getSide() {
        return side;
    }
    
    public void setSide(Side side) {
        Object oldValue = this.side;
        this.side = side;
        firePropertyChange("side", oldValue, side);
    }

    public FiducialLocatable getFiducialLocatable() {
        return fiducialLocatable;
    }

    public void setFiducialLocatable(FiducialLocatable fiducialLocatable) {
        FiducialLocatable oldValue = this.fiducialLocatable;
        this.fiducialLocatable = fiducialLocatable;
        firePropertyChange("fiducialLocatable", oldValue, fiducialLocatable);
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        String oldValue = this.fileName;
        this.fileName = fileName;
        firePropertyChange("fileName", oldValue, fileName);
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        String oldValue = this.parentId;
        this.parentId = parentId;
        firePropertyChange("parentId", oldValue, parentId);
    }

    public boolean isCheckFiducials() {
        return checkFiducials;
    }

    public void setCheckFiducials(boolean checkFiducials) {
        boolean oldValue = this.checkFiducials;
        this.checkFiducials = checkFiducials;
        firePropertyChange("checkFiducials", oldValue, checkFiducials);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        boolean oldValue = this.enabled;
        this.enabled = enabled;
        firePropertyChange("enabled", oldValue, enabled);
    }

    public FiducialLocatableLocation getParent() {
        return parent;
    }

    public void setParent(FiducialLocatableLocation parent) {
        this.parent = parent;
    }

    public AffineTransform getLocalToParentTransform() {
        return localToParentTransform;
    }

    public void setLocalToParentTransform(AffineTransform localToParentTransform) {
        Object oldValue = this.localToParentTransform;
        this.localToParentTransform = localToParentTransform;
        firePropertyChange("localToParentTransform", oldValue, localToParentTransform);
    }

    public void setLocation(Location location) {
        Location oldValue = this.location;
        super.setLocation(location);
        // If the location is changing, it is not possible for the transform to still be valid, so
        //clear it.
        if (!this.location.equals(oldValue)) {
            setLocalToParentTransform(null);
        }
    }

    public void setId(String id) {
        String oldValue = this.id;
        this.id = id;
        firePropertyChange("id", oldValue, id);
    }
    
    @Override
    public String getId() {
        return id;
    }
    
}
