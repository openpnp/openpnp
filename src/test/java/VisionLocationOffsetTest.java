import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.io.Files;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;
import javax.swing.Icon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpnp.CameraListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.pandaplacer.BambooFeederAutoVision;
import org.openpnp.machine.reference.feeder.ReferencePushPullFeeder;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.FocusProvider;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.VisionProvider;
import org.openpnp.spi.base.AbstractHeadMountable;
import org.openpnp.util.FeederVisionHelper;
import org.openpnp.util.FeederVisionHelper.FeederVisionHelperParams;
import org.openpnp.util.FeederVisionHelper.FindFeaturesMode;
import org.openpnp.util.FeederVisionHelper.PipelineType;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;

public class VisionLocationOffsetTest {
  private static final double DELTA = 1e-9;

  @BeforeEach
  public void before() throws Exception {
    File workingDirectory = Files.createTempDir();
    workingDirectory = new File(workingDirectory, ".openpnp");
    Configuration.initialize(workingDirectory);
    Configuration.get().load();
  }

  @Test
  public void testParamsDefaultOffsetIsZero() {
    FeederVisionHelperParams params = new FeederVisionHelperParams();
    assertNotNull(params.visionLocationOffset);
    assertEquals(0.0, params.visionLocationOffset.getX(), DELTA);
    assertEquals(0.0, params.visionLocationOffset.getY(), DELTA);
  }

  @Test
  public void testParamsConstructorStoresOffset() {
    Location offset = new Location(LengthUnit.Millimeters, 1.5, -2.0, 0, 0);
    FeederVisionHelperParams params =
        new FeederVisionHelperParams(
            null,
            PipelineType.CircularSymmetry,
            null,
            2000,
            true,
            false,
            new Length(4, LengthUnit.Millimeters),
            new Length(4, LengthUnit.Millimeters),
            1,
            new Location(LengthUnit.Millimeters),
            new Location(LengthUnit.Millimeters),
            new Location(LengthUnit.Millimeters),
            1.95,
            0.6,
            offset);
    assertSame(offset, params.visionLocationOffset);
    assertEquals(1.5, params.visionLocationOffset.getX(), DELTA);
    assertEquals(-2.0, params.visionLocationOffset.getY(), DELTA);
  }

  @Test
  public void testParamsConstructorNullOffsetStaysNull() {
    FeederVisionHelperParams params =
        new FeederVisionHelperParams(
            null,
            PipelineType.CircularSymmetry,
            null,
            2000,
            true,
            false,
            new Length(4, LengthUnit.Millimeters),
            new Length(4, LengthUnit.Millimeters),
            1,
            new Location(LengthUnit.Millimeters),
            new Location(LengthUnit.Millimeters),
            new Location(LengthUnit.Millimeters),
            1.95,
            0.6,
            null);
    assertNull(params.visionLocationOffset);
  }

  @Test
  public void testMidpointWithPositiveOffset() {
    Location hole1 = new Location(LengthUnit.Millimeters, 10, 20, 5, 0);
    Location hole2 = new Location(LengthUnit.Millimeters, 30, 20, 5, 0);
    Location offset = new Location(LengthUnit.Millimeters, 2.0, 3.0, 99.0, 45.0);

    Location result = hole1.add(hole2).multiply(0.5).add(offset.derive(null, null, 0.0, 0.0));

    assertEquals(22.0, result.getX(), DELTA);
    assertEquals(23.0, result.getY(), DELTA);
    assertEquals(5.0, result.getZ(), DELTA);
    assertEquals(0.0, result.getRotation(), DELTA);
  }

  @Test
  public void testCaptureOffsetRoundTrip() {
    Location hole1 = new Location(LengthUnit.Millimeters, 10, 20, 5, 0);
    Location hole2 = new Location(LengthUnit.Millimeters, 30, 20, 5, 0);
    Location midPoint = hole1.add(hole2).multiply(0.5);
    Location cameraLocation = new Location(LengthUnit.Millimeters, 23, 18, 3, 90);

    Location capturedOffset = cameraLocation.subtract(midPoint).derive(null, null, 0.0, 0.0);
    Location applied = midPoint.add(capturedOffset.derive(null, null, 0.0, 0.0));

    assertEquals(3.0, capturedOffset.getX(), DELTA);
    assertEquals(-2.0, capturedOffset.getY(), DELTA);
    assertEquals(0.0, capturedOffset.getZ(), DELTA);
    assertEquals(0.0, capturedOffset.getRotation(), DELTA);
    assertEquals(cameraLocation.getX(), applied.getX(), DELTA);
    assertEquals(cameraLocation.getY(), applied.getY(), DELTA);
    assertEquals(midPoint.getZ(), applied.getZ(), DELTA);
    assertEquals(midPoint.getRotation(), applied.getRotation(), DELTA);
  }

  @Test
  public void testOffsetWithDifferentUnits() {
    Location hole1 = new Location(LengthUnit.Millimeters, 10, 20, 0, 0);
    Location hole2 = new Location(LengthUnit.Millimeters, 30, 20, 0, 0);
    Location offset = new Location(LengthUnit.Inches, 0.1, -0.1, 0, 0);

    Location result = hole1.add(hole2).multiply(0.5).add(offset.derive(null, null, 0.0, 0.0));

    assertEquals(22.54, result.getX(), DELTA);
    assertEquals(17.46, result.getY(), DELTA);
  }

  @Test
  public void testReferencePushPullFeederOffsetProperty() {
    ReferencePushPullFeeder feeder = new ReferencePushPullFeeder();

    assertNotNull(feeder.getVisionLocationOffset());
    assertEquals(0.0, feeder.getVisionLocationOffset().getX(), DELTA);
    assertEquals(0.0, feeder.getVisionLocationOffset().getY(), DELTA);

    Location offset = new Location(LengthUnit.Millimeters, 3.0, -2.0, 0, 0);
    feeder.setVisionLocationOffset(offset);
    assertSame(offset, feeder.getVisionLocationOffset());
    assertEquals(3.0, feeder.getVisionLocationOffset().getX(), DELTA);
    assertEquals(-2.0, feeder.getVisionLocationOffset().getY(), DELTA);
  }

  @Test
  public void testReferencePushPullFeederOffsetPropertyChange() {
    ReferencePushPullFeeder feeder = new ReferencePushPullFeeder();

    final boolean[] fired = {false};
    final String[] propName = {null};
    feeder.addPropertyChangeListener(
        evt -> {
          if ("visionLocationOffset".equals(evt.getPropertyName())) {
            fired[0] = true;
            propName[0] = evt.getPropertyName();
          }
        });

    feeder.setVisionLocationOffset(new Location(LengthUnit.Millimeters, 1, 2, 0, 0));

    assertTrue(fired[0], "PropertyChangeEvent should have fired for visionLocationOffset");
    assertEquals("visionLocationOffset", propName[0]);
  }

  @Test
  public void testReferencePushPullFeederParamsIncludeOffset() throws Exception {
    TestReferencePushPullFeeder feeder = new TestReferencePushPullFeeder(new StubCamera());
    Location offset = new Location(LengthUnit.Millimeters, 3.25, -1.75, 5, 90);
    feeder.setVisionLocationOffset(offset);

    FeederVisionHelperParams params =
        invokeVisionHelperParams(feeder, ReferencePushPullFeeder.class);

    assertSame(offset, params.visionLocationOffset);
  }

  @Test
  public void testReferencePushPullFeederNominalVisionLocationAppliesOffset() throws Exception {
    TestReferencePushPullFeeder feeder =
        new TestReferencePushPullFeeder(
            new StubCamera(new Location(LengthUnit.Millimeters, 100, 200, 300, 45)));
    feeder.setLocation(new Location(LengthUnit.Millimeters, 40, 50, 6, 12));
    feeder.setRotationInFeeder(7.0);
    feeder.setHole1Location(new Location(LengthUnit.Millimeters, 10, 20, 1, 2));
    feeder.setHole2Location(new Location(LengthUnit.Millimeters, 30, 20, 9, 8));
    feeder.setVisionLocationOffset(new Location(LengthUnit.Millimeters, 2.0, -3.0, 99.0, 45.0));

    Location nominal = feeder.getNominalVisionLocation();

    assertEquals(22.0, nominal.getX(), DELTA);
    assertEquals(17.0, nominal.getY(), DELTA);
    assertEquals(6.0, nominal.getZ(), DELTA);
    assertEquals(19.0, nominal.getRotation(), DELTA);
  }

  @Test
  public void testBambooFeederOffsetProperty() {
    BambooFeederAutoVision feeder = new BambooFeederAutoVision();

    assertNotNull(feeder.getVisionLocationOffset());
    assertEquals(0.0, feeder.getVisionLocationOffset().getX(), DELTA);
    assertEquals(0.0, feeder.getVisionLocationOffset().getY(), DELTA);

    Location offset = new Location(LengthUnit.Millimeters, 5.0, -3.0, 0, 0);
    feeder.setVisionLocationOffset(offset);
    assertSame(offset, feeder.getVisionLocationOffset());
  }

  @Test
  public void testBambooFeederOffsetPropertyChange() {
    BambooFeederAutoVision feeder = new BambooFeederAutoVision();

    final boolean[] fired = {false};
    feeder.addPropertyChangeListener(
        evt -> {
          if ("visionLocationOffset".equals(evt.getPropertyName())) {
            fired[0] = true;
          }
        });

    feeder.setVisionLocationOffset(new Location(LengthUnit.Millimeters, 1, 2, 0, 0));
    assertTrue(fired[0], "PropertyChangeEvent should have fired for visionLocationOffset");
  }

  @Test
  public void testBambooFeederParamsIncludeOffset() throws Exception {
    TestBambooFeederAutoVision feeder = new TestBambooFeederAutoVision(new StubCamera());
    Location offset = new Location(LengthUnit.Millimeters, -4.5, 1.25, 6, 15);
    feeder.setVisionLocationOffset(offset);

    FeederVisionHelperParams params =
        invokeVisionHelperParams(
            feeder,
            Class.forName("org.openpnp.machine.pandaplacer.AbstractPandaplacerVisionFeeder"));

    assertSame(offset, params.visionLocationOffset);
  }

  @Test
  public void testBambooFeederNominalVisionLocationAppliesOffset() throws Exception {
    TestBambooFeederAutoVision feeder =
        new TestBambooFeederAutoVision(
            new StubCamera(new Location(LengthUnit.Millimeters, 100, 200, 300, 45)));
    feeder.setLocation(new Location(LengthUnit.Millimeters, 40, 50, 4, 18));
    feeder.setRotationInFeeder(-3.0);
    feeder.setHole1Location(new Location(LengthUnit.Millimeters, 10, 20, 1, 2));
    feeder.setHole2Location(new Location(LengthUnit.Millimeters, 30, 20, 9, 8));
    feeder.setVisionLocationOffset(new Location(LengthUnit.Millimeters, -1.0, 2.5, 99.0, 45.0));

    Location nominal = feeder.getNominalVisionLocation();

    assertEquals(19.0, nominal.getX(), DELTA);
    assertEquals(22.5, nominal.getY(), DELTA);
    assertEquals(4.0, nominal.getZ(), DELTA);
    assertEquals(15.0, nominal.getRotation(), DELTA);
  }

  @Test
  public void testSearchRangeNotExpandedByOffset() {
    // The search range should be the same regardless of offset because the search
    // center is shifted to the holes midpoint via sprocketHole.center, not the
    // camera position. The radius stays half_hole_distance + pitch.
    Location hole1 = new Location(LengthUnit.Millimeters, 10, 20, 0, 0);
    Location hole2 = new Location(LengthUnit.Millimeters, 30, 20, 0, 0);
    double sprocketHolePitchMm = 4.0;

    Length range = hole1.getLinearLengthTo(hole2)
            .multiply(0.5)
            .add(new Length(sprocketHolePitchMm, LengthUnit.Millimeters));

    // hole distance = 20mm, half = 10mm, + pitch 4mm = 14mm
    assertEquals(14.0, range.getValue(), DELTA);
  }

  @Test
  public void testSearchCenterIsHolesMidpointNotCameraPosition() {
    // The search center should be the midpoint of the holes, so the search area
    // is shifted to be symmetric around the holes regardless of the camera offset.
    Location hole1 = new Location(LengthUnit.Millimeters, 10, 20, 0, 0);
    Location hole2 = new Location(LengthUnit.Millimeters, 30, 20, 0, 0);
    Location offset = new Location(LengthUnit.Millimeters, 5.0, 0, 0, 0);

    Location searchCenter = hole1.add(hole2).multiply(0.5);
    Location cameraPos = searchCenter.add(offset.derive(null, null, 0.0, 0.0));

    // Search center is the midpoint (20, 20), not the camera position (25, 20)
    assertEquals(20.0, searchCenter.getX(), DELTA);
    assertEquals(20.0, searchCenter.getY(), DELTA);
    assertEquals(25.0, cameraPos.getX(), DELTA);

    // Both holes are equidistant from the search center
    double d1 = searchCenter.getLinearDistanceTo(hole1);
    double d2 = searchCenter.getLinearDistanceTo(hole2);
    assertEquals(d1, d2, DELTA);
    assertEquals(10.0, d1, DELTA);

    // Range of 14mm (half distance + pitch) covers both holes from the midpoint
    double range = 14.0;
    assertTrue(range > d1, "Range covers hole1 from midpoint");
    assertTrue(range > d2, "Range covers hole2 from midpoint");
  }

  @Test
  public void testCalibrationLoopMidpointIncludesOffset() {
    // Simulates the midpoint calculation in the calibration loop
    Location runningHole1 = new Location(LengthUnit.Millimeters, 10, 20, 0, 0);
    Location runningHole2 = new Location(LengthUnit.Millimeters, 30, 20, 0, 0);
    Location cameraLocation = new Location(LengthUnit.Millimeters, 50, 50, 5, 0);
    Location runningPickLocation = new Location(LengthUnit.Millimeters, 20, 15, 6, 12);
    double rotationInFeeder = 3.0;
    Location offset = new Location(LengthUnit.Millimeters, 2.0, -1.0, 99.0, 45.0);

    // This mirrors the calibration loop code:
    // midPoint = runningHole1.add(runningHole2).multiply(0.5, 0.5, 0, 0)
    //         .add(offset.derive(null, null, 0.0, 0.0))
    //         .derive(camera, false, false, true, false)
    //         .derive(null, null, null, rotation+rotationInFeeder);
    Location midPoint = runningHole1.add(runningHole2).multiply(0.5, 0.5, 0, 0)
            .add(offset.derive(null, null, 0.0, 0.0))
            .derive(cameraLocation, false, false, true, false)
            .derive(null, null, null, runningPickLocation.getRotation() + rotationInFeeder);

    // midpoint of holes = (20, 20), + offset (2, -1) = (22, 19)
    assertEquals(22.0, midPoint.getX(), DELTA);
    assertEquals(19.0, midPoint.getY(), DELTA);
    // Z from camera
    assertEquals(5.0, midPoint.getZ(), DELTA);
    // Rotation = pick rotation + rotationInFeeder = 12 + 3 = 15
    assertEquals(15.0, midPoint.getRotation(), DELTA);
  }

  @Test
  public void testCalibrationLoopMidpointWithZeroOffset() {
    Location runningHole1 = new Location(LengthUnit.Millimeters, 10, 20, 0, 0);
    Location runningHole2 = new Location(LengthUnit.Millimeters, 30, 20, 0, 0);
    Location cameraLocation = new Location(LengthUnit.Millimeters, 50, 50, 5, 0);
    Location runningPickLocation = new Location(LengthUnit.Millimeters, 20, 15, 6, 12);
    double rotationInFeeder = 3.0;
    Location zeroOffset = new Location(LengthUnit.Millimeters);

    Location midPoint = runningHole1.add(runningHole2).multiply(0.5, 0.5, 0, 0)
            .add(zeroOffset.derive(null, null, 0.0, 0.0))
            .derive(cameraLocation, false, false, true, false)
            .derive(null, null, null, runningPickLocation.getRotation() + rotationInFeeder);

    // Without offset, midpoint is just (20, 20)
    assertEquals(20.0, midPoint.getX(), DELTA);
    assertEquals(20.0, midPoint.getY(), DELTA);
  }

  @Test
  public void testCalibrateHolesReferenceLocationSubtractsOffset() {
    // In CalibrateHoles mode, the reference location for line-distance is:
    // camera.getLocation().subtract(visionLocationOffset)
    // This undoes the offset so the reference is at the true midpoint of holes.
    Location cameraLocation = new Location(LengthUnit.Millimeters, 22, 19, 5, 0);
    Location offset = new Location(LengthUnit.Millimeters, 2.0, -1.0, 0, 0);

    Location referenceLocation = cameraLocation.subtract(offset);

    // Camera is at midpoint+offset=(22,19), subtract offset gives midpoint=(20,20)
    assertEquals(20.0, referenceLocation.getX(), DELTA);
    assertEquals(20.0, referenceLocation.getY(), DELTA);
  }

  @Test
  public void testCalibrateHolesReferenceLocationUnchangedWithZeroOffset() {
    Location cameraLocation = new Location(LengthUnit.Millimeters, 20, 20, 5, 0);
    Location zeroOffset = new Location(LengthUnit.Millimeters);

    Location referenceLocation = cameraLocation.subtract(zeroOffset);

    assertEquals(20.0, referenceLocation.getX(), DELTA);
    assertEquals(20.0, referenceLocation.getY(), DELTA);
  }

  @Test
  public void testNominalVisionLocationWithZeroOffset() throws Exception {
    // Both feeders should return the same result as before when offset is zero
    TestReferencePushPullFeeder feeder =
        new TestReferencePushPullFeeder(
            new StubCamera(new Location(LengthUnit.Millimeters, 100, 200, 300, 45)));
    feeder.setLocation(new Location(LengthUnit.Millimeters, 40, 50, 6, 12));
    feeder.setRotationInFeeder(7.0);
    feeder.setHole1Location(new Location(LengthUnit.Millimeters, 10, 20, 1, 2));
    feeder.setHole2Location(new Location(LengthUnit.Millimeters, 30, 20, 9, 8));
    // Leave offset as default zero

    Location nominal = feeder.getNominalVisionLocation();

    // midpoint = (20, 20), no offset, Z from pick location, rotation = 12 + 7
    assertEquals(20.0, nominal.getX(), DELTA);
    assertEquals(20.0, nominal.getY(), DELTA);
    assertEquals(6.0, nominal.getZ(), DELTA);
    assertEquals(19.0, nominal.getRotation(), DELTA);
  }

  @Test
  public void testBambooNominalVisionLocationWithZeroOffset() throws Exception {
    TestBambooFeederAutoVision feeder =
        new TestBambooFeederAutoVision(
            new StubCamera(new Location(LengthUnit.Millimeters, 100, 200, 300, 45)));
    feeder.setLocation(new Location(LengthUnit.Millimeters, 40, 50, 4, 18));
    feeder.setRotationInFeeder(-3.0);
    feeder.setHole1Location(new Location(LengthUnit.Millimeters, 10, 20, 1, 2));
    feeder.setHole2Location(new Location(LengthUnit.Millimeters, 30, 20, 9, 8));

    Location nominal = feeder.getNominalVisionLocation();

    // midpoint = (20, 20), no offset
    assertEquals(20.0, nominal.getX(), DELTA);
    assertEquals(20.0, nominal.getY(), DELTA);
    assertEquals(4.0, nominal.getZ(), DELTA);
    assertEquals(15.0, nominal.getRotation(), DELTA);
  }

  @Test
  public void testBothFeedersNominalVisionLocationEquivalent() throws Exception {
    // Given the same inputs, both feeders should produce the same nominal vision location
    Location cameraLoc = new Location(LengthUnit.Millimeters, 100, 200, 300, 45);
    Location pickLoc = new Location(LengthUnit.Millimeters, 40, 50, 6, 12);
    Location hole1 = new Location(LengthUnit.Millimeters, 10, 20, 1, 2);
    Location hole2 = new Location(LengthUnit.Millimeters, 30, 20, 9, 8);
    Location offset = new Location(LengthUnit.Millimeters, 2.5, -1.5, 99, 45);
    double rotInFeeder = 7.0;

    TestReferencePushPullFeeder rppf = new TestReferencePushPullFeeder(new StubCamera(cameraLoc));
    rppf.setLocation(pickLoc);
    rppf.setRotationInFeeder(rotInFeeder);
    rppf.setHole1Location(hole1);
    rppf.setHole2Location(hole2);
    rppf.setVisionLocationOffset(offset);

    TestBambooFeederAutoVision bamboo = new TestBambooFeederAutoVision(new StubCamera(cameraLoc));
    bamboo.setLocation(pickLoc);
    bamboo.setRotationInFeeder(rotInFeeder);
    bamboo.setHole1Location(hole1);
    bamboo.setHole2Location(hole2);
    bamboo.setVisionLocationOffset(offset);

    Location nominalRppf = rppf.getNominalVisionLocation();
    Location nominalBamboo = bamboo.getNominalVisionLocation();

    assertEquals(nominalBamboo.getX(), nominalRppf.getX(), DELTA);
    assertEquals(nominalBamboo.getY(), nominalRppf.getY(), DELTA);
    assertEquals(nominalBamboo.getZ(), nominalRppf.getZ(), DELTA);
    assertEquals(nominalBamboo.getRotation(), nominalRppf.getRotation(), DELTA);
  }

  // ---- End-to-end tests: run findFeatures with fake pipeline results ----

  /**
   * Helper: build a FeederVisionHelperParams with a pipeline that returns a line of
   * sprocket-hole circles at 4mm pitch along the X axis at the given Y coordinate.
   * The StubCamera has UPP=1mm/px, 640x480, so pixel = world with simple transform.
   *
   * Holes are placed from worldHole1.x to worldHole2.x at 4mm intervals, all at
   * worldHole1.y (same Y). hole1/hole2 in params are the two endpoint holes.
   */
  private static FeederVisionHelper runFindFeatures(
      Location cameraLocation, Location pickLocation,
      Location hole1, Location hole2, Location offset,
      Location worldHole1, Location worldHole2,
      FindFeaturesMode mode) throws Exception {

    StubCamera camera = new StubCamera(cameraLocation);

    double sprocketDiameterPx = 1.5; // 1.5mm at 1mm/px
    double pitchMm = 4.0;

    // Place a line of holes from worldHole1.x to worldHole2.x at 4mm pitch, all at same Y.
    List<CvStage.Result.Circle> circles = new ArrayList<>();
    double startX = Math.min(worldHole1.getX(), worldHole2.getX());
    double endX = Math.max(worldHole1.getX(), worldHole2.getX());
    double holeY = worldHole1.getY();
    for (double wx = startX; wx <= endX + 0.01; wx += pitchMm) {
      double px = 320 + (wx - cameraLocation.getX());
      double py = 240 - (holeY - cameraLocation.getY());
      circles.add(new CvStage.Result.Circle(px, py, sprocketDiameterPx));
    }

    CvPipeline pipeline = new CvPipeline();
    pipeline.add("results", new FakeResultsStage(circles));
    pipeline.setProperty("camera", camera);

    FeederVisionHelperParams params = new FeederVisionHelperParams(
        camera, PipelineType.CircularSymmetry, pipeline, 0,
        true, false,
        new Length(4, LengthUnit.Millimeters),  // partPitch
        new Length(4, LengthUnit.Millimeters),  // feedPitch
        1,                                       // feedMultiplier
        pickLocation, hole1, hole2,
        1.95, 0.6, offset);

    FeederVisionHelper helper = new FeederVisionHelper(params);
    helper.findFeatures(mode);
    return helper;
  }

  @Test
  public void testFindFeaturesCalibrationWithOffset() throws Exception {
    // Holes at world (10,20) and (30,20). Midpoint = (20,20).
    // Offset = (2,-1). Camera at midpoint+offset = (22,19).
    // CalibrateHoles mode: the reference location should be the midpoint (20,20)
    // after subtracting the offset — so the line is accepted.
    Location hole1 = new Location(LengthUnit.Millimeters, 10, 20, 0, 0);
    Location hole2 = new Location(LengthUnit.Millimeters, 30, 20, 0, 0);
    Location offset = new Location(LengthUnit.Millimeters, 2, -1, 0, 0);
    Location cameraAt = new Location(LengthUnit.Millimeters, 22, 19, 5, 0);
    Location pickLoc = new Location(LengthUnit.Millimeters, 20, 16, 5, 0);

    FeederVisionHelper result = assertDoesNotThrow(() ->
        runFindFeatures(cameraAt, pickLoc, hole1, hole2, offset,
            hole1, hole2, FindFeaturesMode.CalibrateHoles));

    assertNotNull(result.getCalibratedHole1Location());
    assertNotNull(result.getCalibratedHole2Location());
  }

  @Test
  public void testFindFeaturesCalibrationWithZeroOffset() throws Exception {
    // Same setup but no offset — camera is at the midpoint.
    Location hole1 = new Location(LengthUnit.Millimeters, 10, 20, 0, 0);
    Location hole2 = new Location(LengthUnit.Millimeters, 30, 20, 0, 0);
    Location zeroOffset = new Location(LengthUnit.Millimeters);
    Location cameraAt = new Location(LengthUnit.Millimeters, 20, 20, 5, 0);
    Location pickLoc = new Location(LengthUnit.Millimeters, 20, 16, 5, 0);

    FeederVisionHelper result = assertDoesNotThrow(() ->
        runFindFeatures(cameraAt, pickLoc, hole1, hole2, zeroOffset,
            hole1, hole2, FindFeaturesMode.CalibrateHoles));

    assertNotNull(result.getCalibratedHole1Location());
    assertNotNull(result.getCalibratedHole2Location());
  }

  @Test
  public void testFindFeaturesCalibrationWithLargeOffset() throws Exception {
    // Large offset — makes sure even big offsets work when reference is corrected.
    Location hole1 = new Location(LengthUnit.Millimeters, 10, 20, 0, 0);
    Location hole2 = new Location(LengthUnit.Millimeters, 30, 20, 0, 0);
    Location offset = new Location(LengthUnit.Millimeters, 8, -5, 0, 0);
    Location cameraAt = new Location(LengthUnit.Millimeters, 28, 15, 5, 0);
    Location pickLoc = new Location(LengthUnit.Millimeters, 20, 16, 5, 0);

    FeederVisionHelper result = assertDoesNotThrow(() ->
        runFindFeatures(cameraAt, pickLoc, hole1, hole2, offset,
            hole1, hole2, FindFeaturesMode.CalibrateHoles));

    assertNotNull(result.getCalibratedHole1Location());
    assertNotNull(result.getCalibratedHole2Location());
  }

  @Test
  public void testFindFeaturesAutoSetupWithOffset() throws Exception {
    // FromPickLocationGetHoles mode uses partLocation as reference.
    // Camera is at pick location (not offset) for auto-setup.
    Location hole1 = new Location(LengthUnit.Millimeters, 10, 20, 0, 0);
    Location hole2 = new Location(LengthUnit.Millimeters, 30, 20, 0, 0);
    Location offset = new Location(LengthUnit.Millimeters, 2, -1, 0, 0);
    Location pickLoc = new Location(LengthUnit.Millimeters, 20, 16, 5, 0);
    // In auto-setup the camera is at the pick location
    Location cameraAt = new Location(LengthUnit.Millimeters, 20, 16, 5, 0);

    FeederVisionHelper result = assertDoesNotThrow(() ->
        runFindFeatures(cameraAt, pickLoc, hole1, hole2, offset,
            hole1, hole2, FindFeaturesMode.FromPickLocationGetHoles));

    assertNotNull(result.getCalibratedHole1Location());
    assertNotNull(result.getCalibratedHole2Location());
    assertNotNull(result.getCalibratedPickLocation());
  }

  @Test
  public void testFindFeaturesAutoSetupWithZeroOffset() throws Exception {
    Location hole1 = new Location(LengthUnit.Millimeters, 10, 20, 0, 0);
    Location hole2 = new Location(LengthUnit.Millimeters, 30, 20, 0, 0);
    Location zeroOffset = new Location(LengthUnit.Millimeters);
    Location pickLoc = new Location(LengthUnit.Millimeters, 20, 16, 5, 0);
    Location cameraAt = new Location(LengthUnit.Millimeters, 20, 16, 5, 0);

    FeederVisionHelper result = assertDoesNotThrow(() ->
        runFindFeatures(cameraAt, pickLoc, hole1, hole2, zeroOffset,
            hole1, hole2, FindFeaturesMode.FromPickLocationGetHoles));

    assertNotNull(result.getCalibratedHole1Location());
    assertNotNull(result.getCalibratedHole2Location());
    assertNotNull(result.getCalibratedPickLocation());
  }

  @Test
  public void testFindFeaturesCalibrationResultsAccurateWithOffset() throws Exception {
    // Verify the calibrated hole locations match the input world positions.
    Location hole1 = new Location(LengthUnit.Millimeters, 10, 20, 0, 0);
    Location hole2 = new Location(LengthUnit.Millimeters, 30, 20, 0, 0);
    Location offset = new Location(LengthUnit.Millimeters, 2, -1, 0, 0);
    Location cameraAt = new Location(LengthUnit.Millimeters, 22, 19, 5, 0);
    Location pickLoc = new Location(LengthUnit.Millimeters, 20, 16, 5, 0);

    FeederVisionHelper result = runFindFeatures(cameraAt, pickLoc, hole1, hole2, offset,
        hole1, hole2, FindFeaturesMode.CalibrateHoles);

    assertEquals(10.0, result.getCalibratedHole1Location().getX(), 0.5);
    assertEquals(20.0, result.getCalibratedHole1Location().getY(), 0.5);
    assertEquals(30.0, result.getCalibratedHole2Location().getX(), 0.5);
    assertEquals(20.0, result.getCalibratedHole2Location().getY(), 0.5);
  }

  @Test
  public void testFindFeaturesCalibrationResultsAccurateWithZeroOffset() throws Exception {
    Location hole1 = new Location(LengthUnit.Millimeters, 10, 20, 0, 0);
    Location hole2 = new Location(LengthUnit.Millimeters, 30, 20, 0, 0);
    Location zeroOffset = new Location(LengthUnit.Millimeters);
    Location cameraAt = new Location(LengthUnit.Millimeters, 20, 20, 5, 0);
    Location pickLoc = new Location(LengthUnit.Millimeters, 20, 16, 5, 0);

    FeederVisionHelper result = runFindFeatures(cameraAt, pickLoc, hole1, hole2, zeroOffset,
        hole1, hole2, FindFeaturesMode.CalibrateHoles);

    assertEquals(10.0, result.getCalibratedHole1Location().getX(), 0.5);
    assertEquals(20.0, result.getCalibratedHole1Location().getY(), 0.5);
    assertEquals(30.0, result.getCalibratedHole2Location().getX(), 0.5);
    assertEquals(20.0, result.getCalibratedHole2Location().getY(), 0.5);
  }

  // A CvStage that returns pre-defined circles as its result.
  private static class FakeResultsStage extends CvStage {
    private final List<Result.Circle> circles;

    FakeResultsStage(List<Result.Circle> circles) {
      this.circles = circles;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
      return new Result(null, new ArrayList<>(circles));
    }
  }

  private static FeederVisionHelperParams invokeVisionHelperParams(
      Object feeder, Class<?> ownerClass) throws Exception {
    Method method =
        ownerClass.getDeclaredMethod(
            "getVisionHelperParams", Camera.class, org.openpnp.vision.pipeline.CvPipeline.class);
    method.setAccessible(true);
    return (FeederVisionHelperParams) method.invoke(feeder, null, null);
  }

  private static class TestReferencePushPullFeeder extends ReferencePushPullFeeder {
    private final Camera camera;

    private TestReferencePushPullFeeder(Camera camera) {
      this.camera = camera;
    }

    @Override
    public Camera getCamera() {
      return camera;
    }
  }

  private static class TestBambooFeederAutoVision extends BambooFeederAutoVision {
    private final Camera camera;

    private TestBambooFeederAutoVision(Camera camera) {
      this.camera = camera;
    }

    @Override
    public Camera getCamera() {
      return camera;
    }
  }

  private static class StubCamera extends AbstractHeadMountable implements Camera {
    private final Location location;

    private StubCamera() {
      this(new Location(LengthUnit.Millimeters));
    }

    private StubCamera(Location location) {
      this.location = location;
    }

    @Override
    public String getId() {
      return null;
    }

    @Override
    public Head getHead() {
      return null;
    }

    @Override
    public void setHead(Head head) {}

    @Override
    public Location getLocation() {
      return location;
    }

    @Override
    public Location getLocation(HeadMountable tool) {
      if (tool != null) {
        return getLocation().subtract(tool.getCameraToolCalibratedOffset(this));
      }
      return getLocation();
    }

    @Override
    public Location getCameraToolCalibratedOffset(Camera camera) {
      return new Location(camera.getUnitsPerPixel().getUnits());
    }

    @Override
    public Wizard getConfigurationWizard() {
      return null;
    }

    @Override
    public String getPropertySheetHolderTitle() {
      return null;
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
      return null;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
      return null;
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
      return null;
    }

    @Override
    public String getName() {
      return null;
    }

    @Override
    public void setName(String name) {}

    @Override
    public Looking getLooking() {
      return null;
    }

    @Override
    public void setLooking(Looking looking) {}

    @Override
    public Location getUnitsPerPixel() {
      return new Location(LengthUnit.Millimeters, 1, 1, 0, 0);
    }

    @Override
    public void setUnitsPerPixel(Location unitsPerPixel) {}

    @Override
    public BufferedImage capture() {
      return null;
    }

    @Override
    public BufferedImage captureTransformed() {
      return null;
    }

    @Override
    public BufferedImage captureRaw() {
      return null;
    }

    @Override
    public void startContinuousCapture(CameraListener listener) {}

    @Override
    public void stopContinuousCapture(CameraListener listener) {}

    @Override
    public void setVisionProvider(VisionProvider visionProvider) {}

    @Override
    public VisionProvider getVisionProvider() {
      return null;
    }

    @Override
    public int getWidth() {
      return 640;
    }

    @Override
    public int getHeight() {
      return 480;
    }

    @Override
    public Icon getPropertySheetHolderIcon() {
      return null;
    }

    @Override
    public void close() throws IOException {}

    @Override
    public BufferedImage settleAndCapture(SettleOption settleOption) throws Exception {
      return null;
    }

    @Override
    public BufferedImage lightSettleAndCapture() {
      return null;
    }

    @Override
    public void actuateLightBeforeCapture(Object light) throws Exception {}

    @Override
    public void actuateLightAfterCapture() throws Exception {}

    @Override
    public Length getSafeZ() {
      return null;
    }

    @Override
    public Location getHeadOffsets() {
      return null;
    }

    @Override
    public void setHeadOffsets(Location headOffsets) {}

    @Override
    public void home() throws Exception {}

    @Override
    public Actuator getLightActuator() {
      return null;
    }

    @Override
    public void ensureCameraVisible() {}

    @Override
    public boolean hasNewFrame() {
      return true;
    }

    @Override
    public Location getUnitsPerPixel(Length z) {
      return new Location(LengthUnit.Millimeters, 1, 1, 0, 0)
          .derive(null, null, z.getValue(), null);
    }

    @Override
    public Length getDefaultZ() {
      return new Length(0.0, LengthUnit.Millimeters);
    }

    @Override
    public boolean isShownInMultiCameraView() {
      return false;
    }

    @Override
    public FocusProvider getFocusProvider() {
      return null;
    }

    @Override
    public Length getRoamingRadius() {
      return new Length(10, LengthUnit.Millimeters);
    }
  }
}
