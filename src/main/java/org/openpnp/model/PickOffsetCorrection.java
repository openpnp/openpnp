package org.openpnp.model;

import org.openpnp.ConfigurationListener;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.simpleframework.xml.Element;

/**
 * Reusable implementation of the Deferred Closed Loop Pick Offset I-Controller for 0 and 1 DOF feeders.
 */
public class PickOffsetCorrection extends AbstractModelObject {
    public static final Length DEFAULT_CORRECTION_LIMIT = new Length(5.0, LengthUnit.Millimeters);
    public static final double DEFAULT_I_TERM = 0.5;
    @Element(required = false)
    private boolean enabled = true;
    @Element(required = false)
    private double iTerm = DEFAULT_I_TERM;
    @Element(required = false)
    private Length correctionLimit = DEFAULT_CORRECTION_LIMIT;

    private Location offset = new Location(LengthUnit.Millimeters);
    private Location pendingSum = new Location(LengthUnit.Millimeters);
    private int visionsSinceLastFeed = 0;
    private Location lastVision = null;

    public PickOffsetCorrection() {
        // By openpnp convention homing resets all transient state including this.
        if (Configuration.isInstanceInitialized()) {
            Configuration.get().addListener(new ConfigurationListener.Adapter() {
                @Override
                public void configurationLoaded(Configuration configuration) {
                    configuration.getMachine().addListener(new MachineListener.Adapter() {
                        @Override
                        public void machineHomed(Machine machine, boolean isHomed) {
                            if (!isHomed) {
                                reset();
                            }
                        }
                    });
                }
            });
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        boolean oldValue = this.enabled;
        this.enabled = enabled;
        firePropertyChange("enabled", oldValue, enabled);
        firePropertyChange("offset", null, offset);
    }

    public double getITerm() {
        return iTerm;
    }

    public void setITerm(double iTerm) {
        double oldValue = this.iTerm;
        this.iTerm = iTerm;
        firePropertyChange("iTerm", oldValue, iTerm);
    }

    public Length getCorrectionLimit() {
        return correctionLimit;
    }

    public void setCorrectionLimit(Length correctionLimit) {
        Length oldValue = this.correctionLimit;
        this.correctionLimit = correctionLimit;
        firePropertyChange("correctionLimit", oldValue, correctionLimit);
    }

    public Location getOffset() {
        return enabled ? offset : new Location(LengthUnit.Millimeters);
    }

    public Location getLastVision() {
        return lastVision;
    }

    public int getVisionsSinceLastFeed() {
        return visionsSinceLastFeed;
    }

    public void recordVision(Location pickFrameOffset) {
        if (!enabled) {
            return;
        }
        pendingSum = pendingSum.add(pickFrameOffset);
        setLastVision(pickFrameOffset);
        setVisionsSinceLastFeed(visionsSinceLastFeed + 1);
    }

    /**
     * Compute the average of all recorded visions since the last settle and apply
     * the result to offset.
     */
    public void settle() {
        if (!enabled || visionsSinceLastFeed == 0) {
            return;
        }
        Location averageError = pendingSum.multiply(iTerm / (double) visionsSinceLastFeed);
        Location newOffset = offset.add(averageError);

        double distance = newOffset.convertToUnits(correctionLimit.getUnits()).getLinearDistanceTo(0, 0);
        if (distance > correctionLimit.getValue()) {
            // Avoid physical damage, saturate it back to the limit.
            newOffset = newOffset.multiply(correctionLimit.getValue() / distance);
        }

        setOffset(newOffset);
        pendingSum = new Location(LengthUnit.Millimeters);
        setVisionsSinceLastFeed(0);
    }

    /**
     * Adds a raw delta to the integrated offset without averaging, clamping or I term.
     */
    public void addOffset(Location delta) {
        setOffset(offset.add(delta));
    }

    /**
     * Subtracts a raw delta from the integrated offset without averaging, clamping or I term.
     */
    void subtractOffset(Location delta) {
        setOffset(offset.subtract(delta));
    }

    /**
     * Clears all state but leaves configuration unchanged.
     */
    public void reset() {
        setOffset(new Location(LengthUnit.Millimeters));
        pendingSum = new Location(LengthUnit.Millimeters);
        setLastVision(null);
        setVisionsSinceLastFeed(0);
    }

    private void setOffset(Location offset) {
        Location oldValue = this.offset;
        this.offset = offset;
        firePropertyChange("offset", oldValue, offset);
    }

    private void setLastVision(Location lastVision) {
        Location oldValue = this.lastVision;
        this.lastVision = lastVision;
        firePropertyChange("lastVision", oldValue, lastVision);
    }

    private void setVisionsSinceLastFeed(int visionsSinceLastFeed) {
        int oldValue = this.visionsSinceLastFeed;
        this.visionsSinceLastFeed = visionsSinceLastFeed;
        firePropertyChange("visionsSinceLastFeed", oldValue, visionsSinceLastFeed);
    }

    /**
     * An extension on top of {@link PickOffsetCorrection} for feeders
     * able to change the part pitch.
     * Allows to remove consistent long-term drift by nudging the part
     * pitch when the correction gets big enough.
     */
    public static class Nudging extends PickOffsetCorrection {
        private static final Location DEFAULT_FEED_PLANE = new Location(LengthUnit.Millimeters, 0, 1, 0, 0);

        // Unit vector, in the machine frame, of the direction a positive feed advances the part.
        @Element(required = false)
        private Location feedPlane = null;

        private Location lastNudge = new Location(LengthUnit.Millimeters);

        // Debug stats, runtime-only: the signed along-feed length (mm) the last nudge advanced the
        // tape, the running net signed total, and the running total of absolute travel.
        private Length dbgLastNudge = new Length(0, LengthUnit.Millimeters);
        private Length dbgNudgeSum = new Length(0, LengthUnit.Millimeters);
        private Length dbgNudgeAbsSum = new Length(0, LengthUnit.Millimeters);

        public Length getLastNudge() {
            return dbgLastNudge;
        }

        public Length getNudgeSum() {
            return dbgNudgeSum;
        }

        public Length getNudgeAbsSum() {
            return dbgNudgeAbsSum;
        }

        private void setNudgeStats(Length last, Length sum, Length absSum) {
            this.dbgLastNudge = last;
            this.dbgNudgeSum = sum;
            this.dbgNudgeAbsSum = absSum;
            // The panel refreshes wholesale on any change; fire one representative property.
            firePropertyChange("lastNudge", null, getLastNudge());
        }

        @Override
        public void reset() {
            super.reset();
            lastNudge = new Location(LengthUnit.Millimeters);
            Length zero = new Length(0, LengthUnit.Millimeters);
            setNudgeStats(zero, zero, zero);
        }

        /** Whether {@link #nudge(Length)} winds long-term drift back into the feed mechanism. */
        @Element(required = false)
        private boolean nudgingEnabled = true;

        public boolean isNudgingEnabled() {
            return nudgingEnabled;
        }

        public void setNudgingEnabled(boolean nudgingEnabled) {
            boolean oldValue = this.nudgingEnabled;
            this.nudgingEnabled = nudgingEnabled;
            firePropertyChange("nudgingEnabled", oldValue, nudgingEnabled);
        }

        /**
         * @return true once a feed plane has been set (derived or overridden), so it should no longer
         *         be derived from the feeder geometry.
         */
        public boolean isFeedPlaneSet() {
            return feedPlane != null;
        }

        /**
         * @return the feed-plane direction: a unit vector in the pick frame pointing the way a
         *         positive feed tick advances the part. Defaults to +Y until one is set.
         */
        public Location getFeedPlane() {
            return feedPlane != null ? feedPlane : DEFAULT_FEED_PLANE;
        }

        public void setFeedPlane(Location feedPlane) {
            Location oldValue = this.feedPlane;
            this.feedPlane = feedPlane;
            firePropertyChange("feedPlane", oldValue, feedPlane);
        }

        /**
         * Drains the correction's component parallel with {@link #getFeedPlane()}.
         * call {@link #revertNudge()} to undo the offset change.
         *
         * @param quanta the physical length of one feed tick
         * @return the signed number of quantum to add to the nominal feed distance
         */
        public int nudge(Length quanta) {
            if (!nudgingEnabled) {
                lastNudge = new Location(LengthUnit.Millimeters);
                return 0;
            }
            Location dir = getFeedPlane().convertToUnits(quanta.getUnits());
            Location off = getOffset().convertToUnits(quanta.getUnits());
            double errorAlongFeed = off.getX() * dir.getX() + off.getY() * dir.getY();

            Location nudgeOffset = new Location(LengthUnit.Millimeters);
            int ticks = 0;
            if (errorAlongFeed <= -quanta.getValue() || quanta.getValue() <= errorAlongFeed) {
                ticks = -(int) (errorAlongFeed / quanta.getValue());
                nudgeOffset = dir.multiply(ticks * quanta.getValue());
                addOffset(nudgeOffset);
            }
            lastNudge = nudgeOffset;
            Length nudgesLength = quanta.multiply((double)ticks);
            setNudgeStats(nudgesLength, dbgNudgeSum.add(nudgesLength), dbgNudgeAbsSum.add(nudgesLength.abs()));
            return ticks;
        }

        /**
         * Reverts the offset change made by the most recent {@link #nudge(Length)}.
         */
        public void revertNudge() {
            subtractOffset(lastNudge);
            lastNudge = new Location(LengthUnit.Millimeters);
            // Roll the reverted nudge back out of the debug stats too.
            setNudgeStats(new Length(0, LengthUnit.Millimeters), dbgNudgeSum.subtract(dbgLastNudge), dbgNudgeAbsSum.subtract(dbgLastNudge.abs()));
        }
    }
}
