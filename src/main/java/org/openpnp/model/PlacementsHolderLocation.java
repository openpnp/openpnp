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

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.io.IOException;

import org.openpnp.gui.MainFrame;
import org.openpnp.util.Utils2D;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.core.Commit;

/**
 * A PlacementsHolderLocation is an abstraction of a container for a subtype of a PlacementsHolder 
 * that specifies its position, orientation, and how its local coordinates transform with respect 
 * to its parent.
 */
public abstract class PlacementsHolderLocation<T extends PlacementsHolderLocation<T>> 
        extends Abstract2DLocatable<T> {

    public enum PlacementsTransformStatus {NotSet, GloballySet, LocallySet};
    
    public static final String ID_DELIMITTER = "\u21D2"; //unicode character ⇒
    
    @Attribute(required = false)
    protected String fileName;

    @Attribute(required = false)
    protected boolean checkFiducials = false;

    @Attribute(required = false)
    protected boolean locallyEnabled = true;

    protected PanelLocation parent;
    protected PlacementsHolder<? extends PlacementsHolder<?>> placementsHolder;
    
    /**
     * Important note: The transform is in Millimeters.
     */
    protected AffineTransform localToParentTransform;
    protected PlacementsTransformStatus placementsTransformStatus = PlacementsTransformStatus.NotSet;
    
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
        placementsTransformStatus = placementsHolderLocationToCopy.placementsTransformStatus;
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
     * Cleans-up property change listeners
     */
    @Override
    public void dispose() {
        if (isDescendantOfJob()) {
            Job job = MainFrame.get().getJobTab().getJob();
            removePropertyChangeListener(job);
            placementsHolder.removePropertyChangeListener(job);
        }
        placementsHolder.dispose();
        super.dispose();
    }
    
    /**
     * 
     * @return true if this PlacementsHolderLocation is a descendant of the job
     */
    public boolean isDescendantOfJob() {
        PanelLocation jobRoot = MainFrame.get().getJobTab().getJob().getRootPanelLocation();
        PanelLocation ancestor = parent;
        while (ancestor != null) {
            if (ancestor == jobRoot) {
                return true;
            }
            ancestor = ancestor.parent;
        }
        return false;
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
     * @param side - the side that is to be facing the top side of its most distant ancestor
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
    public PlacementsHolder<?> getPlacementsHolder() {
        return placementsHolder;
    }

    /**
     * Sets the PlacementsHolder that is associated with this PlacementsHolderLocation
     * @param placementsHolder - the PlacementsHolder to associate with this 
     * PlacementsHolderLocation
     */
    public void setPlacementsHolder(PlacementsHolder<? extends PlacementsHolder<?>> placementsHolder) {
        PlacementsHolder<?> oldValue = this.placementsHolder;
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
     * @param enabledStateMap - the state to set the flag 
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
        if (localToParentTransform == null) {
            setPlacementsTransformStatus(PlacementsTransformStatus.NotSet);
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
        setPlacementsTransformStatus(PlacementsTransformStatus.LocallySet);
    }
    
    /**
     * Sets the location of this PlacementsHolderLocation
     * @param location - the location to set
     */
    public void setLocation(Location location) {
        Location oldValue = getLocation();
        // If the location is changing, it is not possible for the transform to still be valid, so
        //clear it.
        if (!location.equals(oldValue)) {
            setLocalToParentTransform(null);
        }
        super.setLocation(location);
    }

    /**
     * 
     * @return the status of the placements transform. Either NotSet - meaning the transform has not
     * been set, or GloballySet - meaning the transform is inheriting its value from an ancestor, or 
     * LocallySet - meaning the transform has been directly set
     */
    public PlacementsTransformStatus getPlacementsTransformStatus() {
        if (placementsTransformStatus == PlacementsTransformStatus.LocallySet) {
            return placementsTransformStatus;
        }
        else if (parent != null && parent.getPlacementsTransformStatus() != PlacementsTransformStatus.NotSet) {
            return PlacementsTransformStatus.GloballySet;
        }
        return PlacementsTransformStatus.NotSet;
    }
    
    /**
     * Sets the placements transform status
     * @param placementsTransformStatus - the status to set. Either NotSet - meaning the transform
     * has not been set, or GloballySet - meaning the transform is inheriting its value from an 
     * ancestor, or LocallySet - meaning the transform has been directly set
     */
    public void setPlacementsTransformStatus(PlacementsTransformStatus placementsTransformStatus) {
        Object oldValue = this.placementsTransformStatus;
        this.placementsTransformStatus = placementsTransformStatus;
        firePropertyChange("placementsTransformStatus", oldValue, placementsTransformStatus);
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
            return parent.getUniqueId() + ID_DELIMITTER + getId();
        }
        else {
            return getId();
        }
    }
}
