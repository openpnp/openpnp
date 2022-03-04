package org.openpnp.model;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;

import org.openpnp.model.Board.Side;
import org.openpnp.util.Utils2D;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.core.Commit;

public class FiducialLocatableLocation extends AbstractLocatable {

    @Attribute
    protected Side side = Side.Top;

    @Attribute(required = false)
    protected String fileName;

    @Attribute(required = false)
    protected boolean checkFiducials;

    @Attribute(required = false)
    protected boolean locallyEnabled = true;

    protected PanelLocation parent;
    protected FiducialLocatable fiducialLocatable;

    /**
     * Important note: The transform is in Millimeters.
     */
    protected AffineTransform localToParentTransform;
    
    @Commit
    protected void commit() {
        setLocation(getLocation());
        setFiducialLocatable(fiducialLocatable);
    }
    
    public FiducialLocatableLocation() {
        super(new Location(LengthUnit.Millimeters));
    }
    
    public FiducialLocatableLocation(FiducialLocatableLocation fiducialLocatableLocation) {
        super(fiducialLocatableLocation.getLocation());
        this.side = fiducialLocatableLocation.side;
        this.fileName = fiducialLocatableLocation.fileName;
        this.checkFiducials = fiducialLocatableLocation.checkFiducials;
        this.locallyEnabled = fiducialLocatableLocation.locallyEnabled;
        this.parent = fiducialLocatableLocation.parent;
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

    public Side getLocalSide() {
        return side;
    }
    
    public void setLocalSide(Side side) {
        Object oldValue = this.side;
        this.side = side;
        firePropertyChange("side", oldValue, side);
    }
    
    public void flipSide() {
        side = side.flip();
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

    public boolean isCheckFiducials() {
        return checkFiducials;
    }

    public void setCheckFiducials(boolean checkFiducials) {
        boolean oldValue = this.checkFiducials;
        this.checkFiducials = checkFiducials;
        firePropertyChange("checkFiducials", oldValue, checkFiducials);
    }

    public boolean isParentBranchEnabled() {
        if (parent != null) {
           return getParent().isEnabled();
        }
        return true;
    }
    
    public boolean isLocallyEnabled() {
        return locallyEnabled;
    }

    public void setLocallyEnabled(boolean enabled) {
        boolean oldValue = this.locallyEnabled;
        this.locallyEnabled = enabled;
        firePropertyChange("locallyEnabled", oldValue, enabled);
    }

    public boolean isEnabled() {
        return locallyEnabled && isParentBranchEnabled();
    }

    public PanelLocation getParent() {
        return parent;
    }

    public void setParent(PanelLocation parent) {
        this.parent = parent;
    }

    public AffineTransform getLocalToParentTransform() {
        if (localToParentTransform == null && fiducialLocatable != null) {
            localToParentTransform = Utils2D.getDefaultBoardPlacementLocationTransform(this);
        }
        return localToParentTransform;
    }

    public void setLocalToParentTransform(AffineTransform localToParentTransform) {
        Object oldValue = this.localToParentTransform;
        this.localToParentTransform = localToParentTransform;
        firePropertyChange("localToParentTransform", oldValue, localToParentTransform);
    }

    public void setLocation(Location location) {
        Location oldValue = getLocation();
        super.setLocation(location);
        // If the location is changing, it is not possible for the transform to still be valid, so
        //clear it.
        if (!getLocation().equals(oldValue)) {
            setLocalToParentTransform(null);
        }
    }

    public AffineTransform getLocalToGlobalTransform() {
        AffineTransform at = new AffineTransform(getLocalToParentTransform());
        if (parent != null) {
            at.preConcatenate(parent.getLocalToGlobalTransform());
        }
        return at;
    }
    
    public void setLocalToGlobalTransform(AffineTransform localToGlobalTransform) throws NoninvertibleTransformException {
        AffineTransform ltg = new AffineTransform(localToGlobalTransform);
        if (parent != null) {
            ltg.preConcatenate(parent.getLocalToGlobalTransform().createInverse());
        }
        setLocalToParentTransform(ltg);
    }
    
    public Location getGlobalLocation() {
        if (parent != null) {
            return Utils2D.calculateBoardPlacementLocation(parent, this);
        }
        return getLocation();
    }
    
    public void setGlobalLocation(Location globalLocation) {
        if (parent != null) {
            setLocation(Utils2D.calculateBoardPlacementLocationInverse(parent, globalLocation));
        }
        else {
            setLocation(globalLocation);
        }
    }
    
    public Side getSide() {
        if (parent != null && parent.getSide() == Side.Bottom) {
            return side.flip();
        }
        return side;
    }
    
    public void setSide(Side side) {
        if (parent != null && parent.getSide() == Side.Bottom) {
            this.side = side.flip();
        }
        else {
            this.side = side;
        }
    }
    
    public boolean isDecendantOf(FiducialLocatableLocation potentialAncestor) {
        FiducialLocatableLocation ancestor = parent;
        while (ancestor != null) {
            if (ancestor.getFileName().equals(potentialAncestor.getFileName())) {
                return true;
            }
            ancestor = ancestor.getParent();
        }
        return false;
    }
}
