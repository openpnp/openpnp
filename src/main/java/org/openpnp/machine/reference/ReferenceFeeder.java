package org.openpnp.machine.reference;

import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.base.AbstractFeeder;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;

public abstract class ReferenceFeeder extends AbstractFeeder {
    public static Location ROOT_FEEDER_LOCATION = new Location(LengthUnit.Millimeters,0,0,0,0);
    
    // This now holds the location and rotation of the feeder relative to its parent's location  
    @Element
    private Location location = new Location(LengthUnit.Millimeters);

    // This now holds the rotation of the part within the feeder
    @Attribute(required=false)
    protected Double rotationInFeeder = 0.0;
    
    @Override
    @Commit
    public void commit() {
        super.commit();
        if (rotationInFeeder == null) {
            /*
             * Originally the rotation of the part was stored in the feeder's location field
             * and there really was no concept of the feeder's rotation.  Now, with the introduction
             * of feeder groups, the rotation of the part in the feeder is separated from the
             * rotation of the feeder.  The feeder's location field now contains the location and
             * rotation of the feeder relative to its parent and the rotationInFeeder field contains
             * the rotation of the part relative to the feeder.  The below conversion from old to new
             * style assumes all multiple of 90 degree rotations in the old location field are due to
             * part rotation in the feeder and the remainder is due to actual rotation of the feeder.
             * While this may not necessarily be true, it doesn't hurt anything since for most feeders,
             * the pick rotation will just be the sum of the two rotations.  And for those feeders
             * where the pick rotation is not the sum of the two (namely loose part feeders), the
             * rotation of the part in the feeder is what is needed to compute the pick rotation (the
             * feeder's rotation is irrelevant).
             */
            double r = getLocation().getRotation();
            int n = (int) Math.round(r/90.0);
            rotationInFeeder = n*90.0;
            setLocation(getLocation().derive(null, null, null, r-rotationInFeeder));
        }
    }
    
    public void setRotationInFeeder(Double rotationInFeeder) {
        this.rotationInFeeder = rotationInFeeder;
    }
    
    public Double getRotationInFeeder() {
        return rotationInFeeder;
    }
        
    @Override
    public void setParentId(String parentId) {
        Location globalLocation = getLocation();
        super.setParentId(parentId);
        setLocation(globalLocation);
    }

    private Location getLocalLocation() {
        return location;
    }

    private void setLocalLocation(Location localLocation) {
        Object oldValue = this.location;
        this.location = localLocation;
        firePropertyChange("location", oldValue, localLocation);
    }
    
    public Location getLocation() {
        Location parentLocation;
        if (parentId.equals(ROOT_FEEDER_ID)) {
            parentLocation = ROOT_FEEDER_LOCATION;
        } else {
            parentLocation = ((ReferenceFeeder) Configuration.get().getMachine().getFeeder(parentId)).getLocation();
        }
        return convertToGlobalLocation(parentLocation, getLocalLocation());
    }

    public void setLocation(Location globalLocation) {
        Location parentLocation;
        if (parentId.equals(ROOT_FEEDER_ID)) {
            parentLocation = ROOT_FEEDER_LOCATION;
        } else {
            parentLocation = ((ReferenceFeeder) Configuration.get().getMachine().getFeeder(parentId)).getLocation();
        }
        setLocalLocation(convertToLocalLocation(parentLocation, globalLocation));
    }
    
    public Location convertToGlobalLocation(Location localLocation) {
        Location originLocation = getLocation();
        return convertToGlobalLocation(originLocation, localLocation);
    }
    
    public Location convertToLocalLocation(Location globalLocation) {
        Location originLocation = getLocation();
        return convertToLocalLocation(originLocation, globalLocation);
    }

    public Location convertToGlobalDeltaLocation(Location localDeltaLocation) {
        Location originLocation = getLocation();
        return convertToGlobalDeltaLocation(originLocation, localDeltaLocation);
    }
    
    public Location convertToLocalDeltaLocation(Location globalDeltaLocation) {
        Location originLocation = getLocation();
        return convertToLocalDeltaLocation(originLocation, globalDeltaLocation);
    }

    static public Location convertToGlobalLocation(Location origin, Location localLocation) {
        if (localLocation == null) {
            return null;
        }
        return localLocation.vectorAdd(origin);
    }
    
    static public Location convertToLocalLocation(Location origin, Location globalLocation) {
        if (globalLocation == null) {
            return null;
        }
        return globalLocation.vectorSubtract(origin);
    }
    
    static public Location convertToGlobalDeltaLocation(Location origin, Location localDeltaLocation) {
        if (localDeltaLocation == null) {
            return null;
        }
        return convertToGlobalLocation(origin, localDeltaLocation).subtract(origin);
    }

    static public Location convertToLocalDeltaLocation(Location origin, Location globalDeltaLocation) {
        if (globalDeltaLocation == null) {
            return null;
        }
        return convertToLocalLocation(origin, globalDeltaLocation.add(origin));
    }

}
