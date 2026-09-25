package org.openpnp.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;

import org.junit.jupiter.api.Test;
import org.openpnp.model.Configuration;
import org.simpleframework.xml.Serializer;

public class PickOffsetCorrectionTest {
    /**
     * Container so the nested Nudging is serialized as an @Element field, exactly the way
     * {@link org.openpnp.machine.photon.PhotonFeeder} persists it.
     */
    @org.simpleframework.xml.Root(name = "holder")
    public static class Holder {
        @org.simpleframework.xml.Element(required = false)
        public PickOffsetCorrection.Nudging correction = new PickOffsetCorrection.Nudging();
    }

    private static Holder roundTrip(Holder holder) throws Exception {
        Serializer serializer = Configuration.createSerializer();
        StringWriter writer = new StringWriter();
        serializer.write(holder, writer);
        return serializer.read(Holder.class, writer.toString());
    }

    @Test
    public void unsetFeedPlaneRoundTripsAsUnset() throws Exception {
        Holder result = roundTrip(new Holder());
        assertFalse(result.correction.isFeedPlaneSet());
    }

    @Test
    public void overriddenFeedPlaneIsPersisted() throws Exception {
        Holder holder = new Holder();
        holder.correction.setFeedPlane(new Location(LengthUnit.Millimeters, -1, 0, 0, 0));

        Holder result = roundTrip(holder);

        assertTrue(result.correction.isFeedPlaneSet());
        Location plane = result.correction.getFeedPlane().convertToUnits(LengthUnit.Millimeters);
        assertEquals(-1, plane.getX(), 1e-9);
        assertEquals(0, plane.getY(), 1e-9);
    }

    @Test
    public void settingsRoundTrip() throws Exception {
        Holder holder = new Holder();
        holder.correction.setEnabled(false);
        holder.correction.setITerm(0.25);
        holder.correction.setCorrectionLimit(new Length(2.5, LengthUnit.Millimeters));
        holder.correction.setNudgingEnabled(false);

        Holder result = roundTrip(holder);

        assertFalse(result.correction.isEnabled());
        assertEquals(0.25, result.correction.getITerm(), 1e-9);
        assertEquals(2.5, result.correction.getCorrectionLimit().convertToUnits(LengthUnit.Millimeters).getValue(), 1e-9);
        assertFalse(result.correction.isNudgingEnabled());
    }

    @Test
    public void disabledLearnsAndAppliesNothing() {
        PickOffsetCorrection correction = new PickOffsetCorrection();
        correction.setEnabled(false);

        correction.recordVision(new Location(LengthUnit.Millimeters, 1, 1, 0, 0));
        correction.settle();

        assertEquals(0, correction.getVisionsSinceLastFeed());
        assertEquals(0, correction.getOffset().getLinearDistanceTo(0, 0), 1e-9);
    }

    @Test
    public void nudgeStatsTrackLastAndTotals() {
        PickOffsetCorrection.Nudging n = new PickOffsetCorrection.Nudging();
        n.setFeedPlane(new Location(LengthUnit.Millimeters, 0, 1, 0, 0));
        Length quantum = new Length(4, LengthUnit.Millimeters);

        // Drive +Y drift so a nudge winds the tape back by whole ticks.
        n.addOffset(new Location(LengthUnit.Millimeters, 0, 9, 0, 0));
        assertEquals(-2, n.nudge(quantum)); // -8 mm
        assertEquals(-8, n.getLastNudge().convertToUnits(LengthUnit.Millimeters).getValue(), 1e-9);
        assertEquals(-8, n.getNudgeSum().convertToUnits(LengthUnit.Millimeters).getValue(), 1e-9);
        assertEquals(8, n.getNudgeAbsSum().convertToUnits(LengthUnit.Millimeters).getValue(), 1e-9);

        // A -Y drift nudges the other way: net sum shrinks, absolute total grows.
        n.addOffset(new Location(LengthUnit.Millimeters, 0, -13, 0, 0));
        assertEquals(3, n.nudge(quantum)); // +12 mm
        assertEquals(12, n.getLastNudge().convertToUnits(LengthUnit.Millimeters).getValue(), 1e-9);
        assertEquals(4, n.getNudgeSum().convertToUnits(LengthUnit.Millimeters).getValue(), 1e-9);
        assertEquals(20, n.getNudgeAbsSum().convertToUnits(LengthUnit.Millimeters).getValue(), 1e-9);

        // Reverting the last nudge rolls it out of the totals and clears "last".
        n.revertNudge();
        assertEquals(0, n.getLastNudge().convertToUnits(LengthUnit.Millimeters).getValue(), 1e-9);
        assertEquals(-8, n.getNudgeSum().convertToUnits(LengthUnit.Millimeters).getValue(), 1e-9);
        assertEquals(8, n.getNudgeAbsSum().convertToUnits(LengthUnit.Millimeters).getValue(), 1e-9);

        // Reset clears everything.
        n.reset();
        assertEquals(0, n.getNudgeSum().convertToUnits(LengthUnit.Millimeters).getValue(), 1e-9);
        assertEquals(0, n.getNudgeAbsSum().convertToUnits(LengthUnit.Millimeters).getValue(), 1e-9);
    }

    @Test
    public void iTermAndLimitDriveSettle() {
        PickOffsetCorrection correction = new PickOffsetCorrection();
        correction.setITerm(0.5);
        correction.setCorrectionLimit(new Length(5.0, LengthUnit.Millimeters));

        correction.recordVision(new Location(LengthUnit.Millimeters, 2, 0, 0, 0));
        correction.settle();
        // One sample averaged with I term 0.5 -> half the residual.
        assertEquals(1.0, correction.getOffset().convertToUnits(LengthUnit.Millimeters).getX(), 1e-9);

        // A huge residual saturates to the configured safety limit.
        correction.setCorrectionLimit(new Length(3.0, LengthUnit.Millimeters));
        correction.recordVision(new Location(LengthUnit.Millimeters, 100, 0, 0, 0));
        correction.settle();
        assertEquals(3.0, correction.getOffset().getLinearDistanceTo(0, 0), 1e-9);
    }
}
