package org.openpnp.machine.reference;

import org.simpleframework.xml.Attribute;

public abstract class FeederWithOptions extends ReferenceFeeder {

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

    @Attribute(required=false)
    protected FeedOptions feedOptions = FeedOptions.Normal;


    public FeedOptions getFeedOptions() {
        return feedOptions;
    }

    public void setFeedOptions(FeedOptions feedOptions) {
        Object oldValue = this.feedOptions;
        this.feedOptions = feedOptions;
        firePropertyChange("feedOptions", oldValue, feedOptions);
    }

}
