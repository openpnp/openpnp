package org.openpnp.machine.reference.feeder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpnp.gui.support.PickOffsetCorrectionPropertySheet;
import org.openpnp.machine.reference.feeder.ReferencePushPullFeeder.CalibrationTrigger;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.PickOffsetCorrection;

import com.google.common.io.Files;

public class ReferencePushPullFeederTest {
    private ReferencePushPullFeeder feeder;

    @BeforeEach
    public void setUp() throws Exception {
        File workingDirectory = Files.createTempDir();
        workingDirectory = new File(workingDirectory, ".openpnp");
        Configuration.initialize(workingDirectory);
        Configuration.get().load();

        feeder = new ReferencePushPullFeeder();
        feeder.setCalibrationTrigger(CalibrationTrigger.None);
        feeder.setHole1Location(new Location(LengthUnit.Millimeters, 10, 13.5, 0, 0));
        feeder.setHole2Location(new Location(LengthUnit.Millimeters, 14, 13.5, 0, 0));
        feeder.setLocation(new Location(LengthUnit.Millimeters, 12, 10, -1, 0));
        Configuration.get().getMachine().addFeeder(feeder);
    }

    private static void assertXyEquals(Location expected, Location actual) {
        expected = expected.convertToUnits(LengthUnit.Millimeters);
        actual = actual.convertToUnits(LengthUnit.Millimeters);
        assertEquals(expected.getX(), actual.getX(), 1e-6);
        assertEquals(expected.getY(), actual.getY(), 1e-6);
    }

    private static void assertNoCorrection(PickOffsetCorrection correction) {
        assertEquals(0, correction.getOffset().getLinearDistanceTo(0, 0), 1e-9);
    }

    @Test
    public void correctionShiftsPickLocation() throws Exception {
        Location uncorrected = feeder.getPickLocation();
        Location offset = new Location(LengthUnit.Millimeters, 0.2, -0.3, 0, 0);
        feeder.getPickOffsetCorrection().addOffset(offset);

        assertXyEquals(uncorrected.add(offset), feeder.getPickLocation());
    }

    @Test
    public void bottomVisionResultIsSettledWithITerm() throws Exception {
        Location uncorrected = feeder.getPickLocation();
        feeder.deferredBottomVisionResult(new Location(LengthUnit.Millimeters, 0.4, 0.2, 0, 0));

        double iTerm = feeder.getPickOffsetCorrection().getITerm();
        assertXyEquals(uncorrected.add(new Location(LengthUnit.Millimeters, 0.4 * iTerm, 0.2 * iTerm, 0, 0)),
                feeder.getPickLocation());
    }

    @Test
    public void bakeKeepsPickLocationAndClearsCorrection() throws Exception {
        feeder.getPickOffsetCorrection().addOffset(new Location(LengthUnit.Millimeters, 0.2, -0.3, 0, 0));
        Location corrected = feeder.getPickLocation();

        feeder.bakePickOffsetCorrectionIntoPosition();

        assertNoCorrection(feeder.getPickOffsetCorrection());
        assertXyEquals(corrected, feeder.getPickLocation());
    }

    @Test
    public void geometryChangesResetCorrection() {
        Location offset = new Location(LengthUnit.Millimeters, 0.2, -0.3, 0, 0);
        PickOffsetCorrection correction = feeder.getPickOffsetCorrection();

        correction.addOffset(offset);
        feeder.setLocation(feeder.getLocation());
        assertNoCorrection(correction);

        correction.addOffset(offset);
        feeder.setHole1Location(feeder.getHole1Location());
        assertNoCorrection(correction);

        correction.addOffset(offset);
        feeder.setPartPitch(new Length(2, LengthUnit.Millimeters));
        assertNoCorrection(correction);
    }

    @Test
    public void multiPartCycleSharesOffset() throws Exception {
        feeder.setPartPitch(new Length(2, LengthUnit.Millimeters));
        feeder.setFeedPitch(new Length(4, LengthUnit.Millimeters));
        assertEquals(2, feeder.getPartsPerFeedCycle());

        Location offset = new Location(LengthUnit.Millimeters, 0.2, -0.3, 0, 0);
        feeder.getPickOffsetCorrection().addOffset(offset);

        for (long feedCount = 1; feedCount <= 2; feedCount++) {
            feeder.setFeedCount(feedCount);
            long partInCycle = ((feedCount + 1) % 2) + 1;
            Location uncorrected = feeder.getPickLocation(partInCycle, null);
            assertXyEquals(uncorrected.add(offset), feeder.getPickLocation());
        }
    }

    @Test
    public void hasPickOffsetCorrectionPropertySheet() {
        assertTrue(Arrays.stream(feeder.getPropertySheets())
                .anyMatch(s -> s instanceof PickOffsetCorrectionPropertySheet));
    }
}
