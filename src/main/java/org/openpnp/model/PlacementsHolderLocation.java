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

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.beans.PropertyChangeEvent;
import java.io.IOException;
import org.openpnp.model.Board.Side;
import org.openpnp.util.Utils2D;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.core.Commit;

/**
 * A PlacementsHolderLocation is an abstraction of a container for a subtype of a PlacementsHolder 
 * that specifies its position, orientation, and how its local coordinates transform with respect 
 * to its parent.
 */
public abstract class PlacementsHolderLocation<T extends PlacementsHolderLocation<T>> 
        extends AbstractLocatable<T> {

    /**
     * The side of the PlacementsHolder that faces the Top side of its parent
     */
    @Attribute
    protected Side side = Side.Top;

    @Attribute(required = false)
    protected String fileName;

    @Attribute(required = false)
    protected boolean checkFiducials = true;

    @Attribute(required = false)
    protected boolean locallyEnabled = true;

    protected PanelLocation parent;
    protected PlacementsHolder<? extends PlacementsHolder<?>> placementsHolder;

    /**
     * Important note: The transform is in Millimeters.
     */
    protected AffineTransform localToParentTransform;
    
    @Commit
    protected void commit() {
        setLocation(getLocation());
        setPlacementsHolder(placementsHolder);
    }
    
    /**
     * Creates a new PlacementsHolderLocation
     */
    public PlacementsHolderLocation() {
        super(new Location(LengthUnit.Millimeters));
    }
    
    /**
     * Creates a deep copy of the specified PlacementsHolderLocation
     * @param placementsHolderLocationToCopy
     */
    public PlacementsHolderLocation(T placementsHolderLocationToCopy) {
        super(placementsHolderLocationToCopy);
        this.side = placementsHolderLocationToCopy.side;
        this.fileName = placementsHolderLocationToCopy.fileName;
        this.checkFiducials = placementsHolderLocationToCopy.checkFiducials;
        this.locallyEnabled = placementsHolderLocationToCopy.locallyEnabled;
        this.parent = placementsHolderLocationToCopy.parent;
        if (placementsHolderLocationToCopy.placementsHolder instanceof Board) {
            this.placementsHolder = new Board((Board) placementsHolderLocationToCopy.placementsHolder);
        }
        else if (placementsHolderLocationToCopy.placementsHolder instanceof Panel) {
            this.placementsHolder = new Panel((Panel) placementsHolderLocationToCopy.placementsHolder);
        }
        if (placementsHolderLocationToCopy.localToParentTransform != null) {
            this.localToParentTransform = 
                    new AffineTransform(placementsHolderLocationToCopy.localToParentTransform);
        }
        else {
            this.localToParentTransform = null;
        }
        setDefinedBy(placementsHolderLocationToCopy.getDefinedBy());
    }
    
    /**
     * Creates a new PlacementsHolderLocation that contains the specified PlacementsHolder
     * @param placementsHolder
     * @throws IOException 
     */
    public PlacementsHolderLocation(PlacementsHolder<? extends PlacementsHolder<?>> placementsHolder) {
        this();
        setPlacementsHolder(placementsHolder);
    }

    /**
     * @return the side of the PlacementsHolder that is facing the Top side of its parent
     */
    public Side getSide() {
        return side;
    }
    
    /**
     * Sets the side of the PlacementsHolder that is facing the Top side of its parent
     * @param side - the side that is to be facing the Top side of its parent
     */
    public void setSide(Side side) {
        Object oldValue = this.side;
        this.side = side;
        firePropertyChange("side", oldValue, side);
    }
    
    /**
     * @return the side of the PlacementsHolder that is facing the top side of its most distant
     * ancestor (which is usually the machine)
     */
    public Side getGlobalSide() {
        if (parent != null && parent.getGlobalSide() == Side.Bottom) {
            return side.flip();
        }
        return side;
    }
    
    /**
     * Sets the side of the PlacementsHolder that is facing the top side of its most distant
     * ancestor (which is usually the machine)
     * @param side - the side that is to be facing top side of its most distant ancestor
     */
    public void setGlobalSide(Side side) {
        Side oldValue = this.side;
        if (parent != null && parent.getGlobalSide() == Side.Bottom) {
            this.side = side.flip();
        }
        else {
            this.side = side;
        }
        firePropertyChange("side", oldValue, side);
    }
    
    /**
     * Flips the PlacementsHolder over so that the side that was facing up is now facing 
     * down and the side that was facing down is now facing up
     */
    public void flipSide() {
        setGlobalSide(getGlobalSide().flip());
    }

    /**
     * 
     * @return the PlacementsHolder associated with this PlacementsHolderLocation
     */
    public PlacementsHolder<? extends PlacementsHolder<?>> getPlacementsHolder() {
        return placementsHolder;
    }

    /**
     * Sets the PlacementsHolder that is associated with this PlacementsHolderLocation
     * @param placementsHolder - the PlacementsHolder to associate with this 
     * PlacementsHolderLocation
     */
    public void setPlacementsHolder(PlacementsHolder<? extends PlacementsHolder<?>> placementsHolder) {
        PlacementsHolder<? extends PlacementsHolder<?>> oldValue = this.placementsHolder;
        this.placementsHolder = placementsHolder;
        if (placementsHolder != null && placementsHolder.getFile() != null) {
            try {
                setFileName(placementsHolder.getFile().getCanonicalPath());
            }
            catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        firePropertyChange("placementsHolder", oldValue, placementsHolder);
    }

    /**
     * @return the filename where the PlacementsHolder that is associated with this 
     * PlacementsHolderLocation is stored
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Sets the filename where the PlacementsHolder that is associated with this 
     * PlacementsHolderLocation is stored
     * @param fileName - the filename
     */
    public void setFileName(String fileName) {
        String oldValue = this.fileName;
        this.fileName = fileName;
        firePropertyChange("fileName", oldValue, fileName);
    }

    /**
     * 
     * @return the state of this PlacementsHolderLocation's checkFiducials flag
     */
    public boolean isCheckFiducials() {
        return checkFiducials;
    }

    /**
     * Sets the state of the checkFiducials flag
     * @param checkFiducials - the state to set the flag
     */
    public void setCheckFiducials(boolean checkFiducials) {
        boolean oldValue = this.checkFiducials;
        this.checkFiducials = checkFiducials;
        firePropertyChange("checkFiducials", oldValue, checkFiducials);
    }

    /**
     * @return true if all ancestors on the direct lineage path of this PlacementsHolderLocation 
     * all the way back to its most distant ancestor are enabled
     */
    public boolean isParentBranchEnabled() {
        if (parent != null) {
           return getParent().isEnabled();
        }
        return true;
    }
    
    /**
     * 
     * @return the state of this PlacementsHolderLocation's locallyEnabled flag
     */
    public boolean isLocallyEnabled() {
        return locallyEnabled;
    }

    /**
     * Sets the state of this PlacementsHolderLocation's locallyEnabled flag
     * @param enabled - the state to set the flag 
     */
    public void setLocallyEnabled(boolean enabled) {
        boolean oldValue = this.locallyEnabled;
        this.locallyEnabled = enabled;
        firePropertyChange("locallyEnabled", oldValue, enabled);
    }

    /**
     * 
     * @return true if this PlacementsHolderLocation is locally enabled and all of its ancestors on
     * its direct lineage path all the way back its most distant ancestor are enabled
     */
    public boolean isEnabled() {
        return locallyEnabled && isParentBranchEnabled();
    }

    /**
     * 
     * @return the parent PanelLocation of this PlacementsHolderLocation
     */
    public PanelLocation getParent() {
        return parent;
    }

    /**
     * Sets the parent PanelLocation of this PlacementsHolderLocation
     * @param parent - the parent to set
     */
    public void setParent(PanelLocation parent) {
        this.parent = parent;
    }

    /**
     * 
     * @return - the AffineTransform that transforms coordinates expressed in the 
     * PlacementsHolder's reference frame to those expressed in the parent's reference frame
     */
    public AffineTransform getLocalToParentTransform() {
        if (localToParentTransform == null && placementsHolder != null) {
            localToParentTransform = Utils2D.getDefaultBoardPlacementLocationTransform(this);
        }
        return localToParentTransform;
    }

    /**
     * Sets the AffineTransform that transforms coordinates expressed in the PlacementsHolder's 
     * reference frame to those expressed in the parent's reference frame
     * @param localToParentTransform - the AffineTransform to set
     */
    public void setLocalToParentTransform(AffineTransform localToParentTransform) {
        Object oldValue = this.localToParentTransform;
        this.localToParentTransform = localToParentTransform;
        firePropertyChange("localToParentTransform", oldValue, localToParentTransform);
    }

    /**
     * Sets the location of this PlacementsHolderLocation
     * @param location - the location to set
     */
    public void setLocation(Location location) {
        Location oldValue = getLocation();
        super.setLocation(location);
        // If the location is changing, it is not possible for the transform to still be valid, so
        //clear it.
        if (!getLocation().equals(oldValue)) {
            setLocalToParentTransform(null);
        }
    }

    /**
     * 
     * @return - the AffineTransform that transforms coordinates expressed in the 
     * PlacementsHolder's reference frame to those expressed in its most distant ancestor's
     * reference frame (which is often the machine's reference frame)
     */
    public AffineTransform getLocalToGlobalTransform() {
        AffineTransform at = new AffineTransform(getLocalToParentTransform());
        if (parent != null) {
            at.preConcatenate(parent.getLocalToGlobalTransform());
        }
        return at;
    }
    
    /**
     * Sets the AffineTransform that transforms coordinates expressed in the 
     * PlacementsHolder's reference frame to those expressed in its most distant ancestor's
     * reference frame (which is often the machine's reference frame)
     * @param localToGlobalTransform - the transform to set
     * @throws NoninvertibleTransformException
     */
    public void setLocalToGlobalTransform(AffineTransform localToGlobalTransform) throws 
            NoninvertibleTransformException {
        AffineTransform localToParentTransform = new AffineTransform(localToGlobalTransform);
        if (parent != null) {
            localToParentTransform.preConcatenate(parent.getLocalToGlobalTransform()
                    .createInverse());
        }
        setLocalToParentTransform(localToParentTransform);
    }
    
    /**
     * 
     * @return - the location of the PlacementsHolder with coordinates expressed in its 
     * most distant ancestor's reference frame (which is often the machine's reference frame)
     */
    public Location getGlobalLocation() {
        if (parent != null) {
            return Utils2D.calculateBoardPlacementLocation(parent, this);
        }
        else {
            return getLocation();
        }
    }
    
    /**
     * Sets the location of the PlacementsHolder
     * @param globalLocation - the location to set with coordinates expressed in the 
     * PlacementsHolder's most distant ancestor's reference frame (which is often the 
     * machine's reference frame)
     */
    public void setGlobalLocation(Location globalLocation) {
        if (parent != null) {
            setLocation(Utils2D.calculateBoardPlacementLocationInverse(parent, globalLocation));
        }
        else {
            setLocation(globalLocation);
        }
    }
    
    /**
     * Checks to see if the PlacementsHolder is a descendant of another
     * @param potentialAncestor - the potential ancestor to check against
     * @return true if the PlacementsHolder is a descendant of potentialAncestor
     */
    public boolean isDescendantOf(PlacementsHolderLocation<?> potentialAncestor) {
        PlacementsHolderLocation<?> ancestor = parent;
        while (ancestor != null && ancestor.getFileName() != null) {
            if (ancestor.getFileName().equals(potentialAncestor.getFileName())) {
                return true;
            }
            ancestor = ancestor.getParent();
        }
        return false;
    }
    
    /**
     * 
     * @return an id of this PlacementsHolderLocation that is different than the id of every other
     * of its relatives
     */
    public String getUniqueId() {
        if (parent != null && parent.getUniqueId() != null) {
            return parent.getUniqueId() + getId();
        }
        else {
            return getId();
        }
    }
    
//    @Override
//    public void propertyChange(PropertyChangeEvent evt) {
////        Logger.trace(String.format("PropertyChangeEvent handled by FiducialLocatable @%08x = %s", this.hashCode(), evt));
//        super.propertyChange(evt);
//    }

}
