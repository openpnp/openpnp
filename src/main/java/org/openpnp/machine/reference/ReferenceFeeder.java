package org.openpnp.machine.reference;

import java.util.List;

import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.base.AbstractFeeder;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public abstract class ReferenceFeeder extends AbstractFeeder {
    @Element
    protected Location location = new Location(LengthUnit.Millimeters);

    @Attribute(required=false)
    protected FeedOptions feedOptions = FeedOptions.Normal;

    /**
     * Additional feed options which enable feeder implement skipping physical movement.
     * The implementation is on feeder itself as even movement is disabled then still
     * a vision may be requested
     */
    public enum FeedOptions {
        Normal("Normal feed"),
        SkipNext("Skip next feed"),
        Disable("Disable feed");

        private String name;

        FeedOptions(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        Object oldValue = this.location;
        this.location = location;
        firePropertyChange("location", oldValue, location);
    }

    /**
     * @return True if feeder supports FeedOptions
     */
    public boolean supportsFeedOptions() {
        return false;
    }

    @Override
    public Location getJobPreparationLocation()  {
        // the default RefrenceFeeder has no prep. location
        return null;
    }

    @Override
    public void prepareForJob(boolean visit) throws Exception {
        // the default RefrenceFeeder needs no prep.
    }
    public FeedOptions getFeedOptions() {
        return feedOptions;
    }

    public void setFeedOptions(FeedOptions feedOptions) {
        Object oldValue = this.feedOptions;
        this.feedOptions = feedOptions;
        firePropertyChange("feedOptions", oldValue, feedOptions);
    }

}
