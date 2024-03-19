package org.openpnp.machine.reference.feeder;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.openpnp.ConfigurationListener;
import org.openpnp.gui.MainFrame;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.Utils2D;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.FluentCv;
import org.openpnp.vision.Ransac;
import org.openpnp.vision.Ransac.Line;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.CvStage.Result;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public abstract class ReferenceVisionFeeder extends ReferenceFeeder {



    // Rotation of the part within the feeder (i.e. within the tape)
    // This is compatible with tonyluken's pending PR #943 or a similar solution.
    // Conversely, feeder.location.rotation contains the orientation of the feeder itself
    // and it defines the local feeder coordinate system. The rotationInFeeder here can be removed
    // once it is inherited.
    @Attribute(required=false)
    protected Double rotationInFeeder = new Double(0.0);

    @Attribute(required = false)
    protected boolean normalizePickLocation = true;

    @Attribute(required = false)
    protected boolean snapToAxis = false;

    @Element(required = false)
    protected Location hole1Location = new Location(LengthUnit.Millimeters);
    @Element(required = false)
    protected Location hole2Location = new Location(LengthUnit.Millimeters);

    @Element(required = false)
    private Length partPitch = new Length(4, LengthUnit.Millimeters);
    @Element(required = false)
    private Length feedPitch = new Length(4, LengthUnit.Millimeters);


    @Attribute(required = false)
    private long feedCount = 0;

    public enum PipelineType {
        ColorKeyed("Default"),
        CircularSymmetry("CircularSymmetry");

        public String tag;

        PipelineType(String tag) {
            this.tag = tag;
        }
    }

    @Element(required = false)
    private CvPipeline pipeline = createDefaultPipeline(PipelineType.ColorKeyed);
    @Attribute(required = false)
    protected PipelineType pipelineType = PipelineType.ColorKeyed;

    @Element(required = false)
    private Length precisionWanted = new Length(0.1, LengthUnit.Millimeters);

    @Attribute(required = false)
    private int calibrationCount = 0;
    @Element(required = false)
    private Length sumOfErrors = new Length(0, LengthUnit.Millimeters);
    @Element(required = false)
    private Length sumOfErrorSquares = new Length(0, LengthUnit.Millimeters);


    // These are not on the GUI but can be tweaked in the machine.xml /////////////////

    // initial calibration tolerance, i.e. how much the feeder can be shifted physically
    @Attribute(required = false)
    protected double calibrationToleranceMm = 1.95;
    // vision and comparison sprocket hole tolerance (in size, position)
    @Attribute(required = false)
    protected double sprocketHoleToleranceMm = 0.6;
    // for rows of feeders, the tolerance in X, Y
    @Attribute(required = false)
    protected double rowLocationToleranceMm = 4.0;
    // for rows of feeders, the tolerance in Z
    @Attribute(required = false)
    protected double rowZLocationToleranceMm = 1.0;

    @Attribute(required = false)
    protected int calibrateMaxPasses = 3;
    // how close the camera has to be to prevent one more pass
    @Attribute(required = false)
    protected double calibrateToleranceMm = 0.3;
    @Attribute(required = false)
    protected int calibrateMinStatistic = 2;

    // Some EIA 481 standard constants.
    static final double sprocketHoleDiameterMm = 1.5;
    static final double sprocketHolePitchMm = 4;
    static final double minSprocketHolesDistanceMm = 3.5;

    /*
     * visionOffset contains the difference between where the part was expected to be and where it
     * is. Subtracting these offsets from the pickLocation produces the correct pick location.
     */
    protected Location visionOffset;

    public enum CalibrationTrigger {
        None,
        OnFirstUse,
        UntilConfident,
        OnEachTapeFeed
    }

    @Attribute(required = false)
    protected CalibrationTrigger calibrationTrigger = CalibrationTrigger.UntilConfident;

    public static final Location nullLocation = new Location(LengthUnit.Millimeters);

    public void checkHomedState(Machine machine) {
        if (!machine.isHomed()) {
            this.resetCalibration();
        }
    }

    public ReferenceVisionFeeder() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationComplete(Configuration configuration) throws Exception {
                // Listen to the machine become unhomed to invalidate feeder calibration.
                // Note that home()  first switches the machine isHomed() state off, then on again,
                // so we also catch re-homing.
                Configuration.get().getMachine().addListener(new MachineListener.Adapter() {

                    @Override
                    public void machineHeadActivity(Machine machine, Head head) {
                        checkHomedState(machine);
                    }

                    @Override
                    public void machineEnabled(Machine machine) {
                        checkHomedState(machine);
                    }
                });
            }
        });
    }

    public Camera getCamera() throws Exception {
        return  Configuration.get()
                .getMachine()
                .getDefaultHead()
                .getDefaultCamera();
    }

    public void assertCalibrated(boolean tapeFeed) throws Exception {
        if (getHole1Location().convertToUnits(LengthUnit.Millimeters).getLinearDistanceTo(getHole2Location()) < 3) {
            throw new Exception("Feeder "+getName()+" sprocket hole locations undefined/too close together.");
        }
        if ((visionOffset == null && calibrationTrigger != CalibrationTrigger.None)
                || (tapeFeed && calibrationTrigger == CalibrationTrigger.UntilConfident && !isPrecisionSufficient())
                || (tapeFeed && calibrationTrigger == CalibrationTrigger.OnEachTapeFeed)) {
            // not yet calibrated (enough)
            obtainCalibratedVisionOffset();
            if (visionOffset == null) {
                // no lock obtained
                throw new Exception(String.format("Vision failed on feeder %s.", getName()));
            }
        }
    }

    public boolean isPrecisionSufficient() {
        if (calibrationCount < calibrateMinStatistic) {
            return false;
        }
        else if (getPrecisionConfidenceLimit().divide(getPrecisionWanted()) > 1.0) {
            return false;
        }
        return true;
    }

    public boolean isVisionEnabled() {
        return calibrationTrigger != CalibrationTrigger.None;
    }

    @Override
    public Location getPickLocation() throws Exception {
        // Numbers are 1-based (a feed is needed before the very first part can be picked),
        // therefore the modulo calculation is a bit gnarly.
        // The 1-based approach has the benefit, that at feed count 0 (reset) the part closest to the reel
        // is the pick location which is the last part in a multi-part feed cycle, which is the one we want for setup.
        long partInCycle = ((getFeedCount()+getPartsPerFeedCycle()-1) % getPartsPerFeedCycle())+1;
        return getPickLocation(partInCycle, visionOffset);
    }

//    // Inherited from ReferenceFeeder. Actual feeders will implement this
//    @Override
//    public void feed(Nozzle nozzle) throws Exception {
//    }

    public void ensureCameraZ(Camera camera, boolean setZ) throws Exception {
        if (camera.isUnitsPerPixelAtZCalibrated()
                && !getLocation().getLengthZ().isInitialized()) {
            throw new Exception("Feeder "+getName()+": Please set the Pick Location Z coordinate first, "
                    + "it is required to determine the true scale of the camera view for accurate computer vision.");
        }
        if (setZ && getLocation().getLengthZ().isInitialized()) {
            // If we already have the Feeder Z, move the camera there to get the right units per pixel.
            camera.moveTo(camera.getLocation().deriveLengths(null, null, getLocation().getLengthZ(), null));
        }
    }


    protected void obtainCalibratedVisionOffset() throws Exception {
        Camera camera = getCamera();
        try (CvPipeline pipeline = getCvPipeline(camera, true, false)) {
            performVisionOperations(camera, pipeline, false, false, true, null);
        }
    }

    @Override
    public String toString() {
        return String.format("%s id %s", getClass().getName(), id);
    }

    @Override
    public void setLocation(Location location) {
        super.setLocation(location);
        resetCalibration();
    }

    public Double getRotationInFeeder() {
        if (rotationInFeeder == null) {
            rotationInFeeder = new Double(0.0);
        }
        return rotationInFeeder;
    }

    public void setRotationInFeeder(Double rotationInFeeder) {
        Object oldValue = this.rotationInFeeder;
        this.rotationInFeeder = rotationInFeeder;
        firePropertyChange("rotationInFeeder", oldValue, rotationInFeeder);
    }

    public boolean isNormalizePickLocation() {
        return normalizePickLocation;
    }

    public void setNormalizePickLocation(boolean normalizePickLocation) {
        Object oldValue = this.normalizePickLocation;
        this.normalizePickLocation = normalizePickLocation;
        firePropertyChange("normalizePickLocation", oldValue, normalizePickLocation);
    }

    public boolean isSnapToAxis() {
        return snapToAxis;
    }

    public void setSnapToAxis(boolean snapToAxis) {
        Object oldValue = this.snapToAxis;
        this.snapToAxis = snapToAxis;
        firePropertyChange("snapToAxis", oldValue, snapToAxis);
    }

    public Location getHole1Location() {
        return hole1Location;
    }

    public void setHole1Location(Location hole1Location) {
        Object oldValue = this.hole1Location;
        this.hole1Location = hole1Location;
        firePropertyChange("hole1Location", oldValue, hole1Location);
        resetCalibration();
    }

    public Location getHole2Location() {
        return hole2Location;
    }

    public void setHole2Location(Location hole2Location) {
        Object oldValue = this.hole2Location;
        this.hole2Location = hole2Location;
        firePropertyChange("hole2Location", oldValue, hole2Location);
        resetCalibration();
    }

    public Length getPartPitch() {
        return partPitch;
    }

    public void setPartPitch(Length partPitch) {
        Object oldValue = this.partPitch;
        this.partPitch = partPitch;
        firePropertyChange("partPitch", oldValue, partPitch);
    }

    public Length getFeedPitch() {
        return feedPitch;
    }

    public void setFeedPitch(Length feedPitch) {
        Object oldValue = this.feedPitch;
        this.feedPitch = feedPitch;
        firePropertyChange("feedPitch", oldValue, feedPitch);
    }


    public long getFeedCount() {
        return feedCount;
    }

    public void setFeedCount(long feedCount) {
        long oldValue = this.feedCount;
        this.feedCount = feedCount;
        firePropertyChange("feedCount", oldValue, feedCount);
    }

    public CalibrationTrigger getCalibrationTrigger() {
        return calibrationTrigger;
    }

    public void setCalibrationTrigger(CalibrationTrigger calibrationTrigger) {
        Object oldValue = this.calibrationTrigger;
        this.calibrationTrigger = calibrationTrigger;
        firePropertyChange("calibrationTrigger", oldValue, calibrationTrigger);
    }


    public Length getPrecisionWanted() {
        return precisionWanted;
    }

    public void setPrecisionWanted(Length precisionWanted) {
        Object oldValue = this.precisionWanted;
        this.precisionWanted = precisionWanted;
        firePropertyChange("precisionWanted", oldValue, precisionWanted);
    }

    public int getCalibrationCount() {
        return calibrationCount;
    }

    public void setCalibrationCount(int calibrationCount) {
        int oldValue = this.calibrationCount;
        Length oldPrecision = getPrecisionAverage();
        Length oldConfidence = getPrecisionConfidenceLimit();
        this.calibrationCount = calibrationCount;
        firePropertyChange("calibrationCount", oldValue, calibrationCount);
        if (oldValue !=  calibrationCount) {
            // this also implicitly changes the stats
            firePropertyChange("precisionAverage", oldPrecision, getPrecisionAverage());
            firePropertyChange("precisionConfidenceLimit", oldConfidence, getPrecisionConfidenceLimit());
        }
    }

    public Length getSumOfErrors() {
        return sumOfErrors;
    }

    public void addCalibrationError(Length error) {
        sumOfErrors = sumOfErrors.add(error);
        // this is a bit dodgy as the true unit is actually the length unit squared, but we'll use the square root of that later, so it will be fine.
        error = error.convertToUnits(sumOfErrorSquares.getUnits());
        sumOfErrorSquares = sumOfErrorSquares.add(error.multiply(error.getValue()));
        setCalibrationCount(getCalibrationCount()+1); // will also fire average and confidence prop change
    }

    public Length getPrecisionAverage() {
        return calibrationCount > 0 ?
                sumOfErrors.multiply(1.0/calibrationCount)
                : new Length(0, LengthUnit.Millimeters);
    }

    public void setPrecisionAverage(Length precisionAverage) {
        // swallow this
    }

    public Length getPrecisionConfidenceLimit() {
        if (calibrationCount >= 2) {
            // Note, we don't take the average of the error, because the error is already a distance that is distributed
            // around the true sprocket holes center location i.e. distributed around zero i.e. zero is the mean
            // (this is limited math knowledge speaking).
            Length variance = sumOfErrorSquares.multiply(1.0/(calibrationCount-1));
            Length scatter = new Length(Math.sqrt(variance.getValue()/Math.sqrt(calibrationCount)), variance.getUnits());
            return scatter.multiply(1.64); // 95% confidence interval, normal distribution.
        }
        else {
            return new Length(0, LengthUnit.Millimeters);
        }
    }

    public void setPrecisionConfidenceLimit(Length precisionConfidenceLimit) {
        // swallow this
    }

    public Length getPartsToSprocketHoleDistance() {
        return new Length(getLocation().getLinearDistanceToLineSegment(getHole1Location(), getHole2Location()),
                getLocation().getUnits());
    }

    public long getPartsPerFeedCycle() {
        long feedsPerPart = (long)Math.ceil(getPartPitch().divide(getFeedPitch()));
        return Math.round(1*Math.ceil(feedsPerPart*getFeedPitch().divide(getPartPitch())));
    }

    public void resetCalibrationStatistics() {
        sumOfErrors = new Length(0, LengthUnit.Millimeters);
        sumOfErrorSquares = new Length(0, LengthUnit.Millimeters);
        setCalibrationCount(0);
        resetCalibration();
    }

    public static Location forwardTransform(Location location, Location transform) {
        return location.rotateXy(transform.getRotation()).addWithRotation(transform);
    }

    public static Location backwardTransform(Location location, Location transform) {
        return location.subtractWithRotation(transform).rotateXy(-transform.getRotation());
    }

    protected Location getTransform(Location visionOffset) {
        // Our local feeder coordinate system is relative to the EIA 481 standard tape orientation
        // i.e. with the sprocket holes on top and the tape advancing to the right, which is our +X
        // The pick location is on [0, 0] local, which corresponds to feeder.location global.
        // The feeder.location.rotation contains the orientation of the tape on the machine.

        // to make sure we get the right rotation, we update it from the sprocket holes
        // instead of trusting the location.rotation. This might happen when the user fiddles
        // with the locations manually.

        Location unitVector = getHole1Location().unitVectorTo(getHole2Location());
        if (!(Double.isFinite(unitVector.getX()) && Double.isFinite(unitVector.getY()))) {
            // Catch (yet) undefined hole locations.
            unitVector = new Location(getHole1Location().getUnits(), 0, 1, 0, 0);
        }
        double rotationTape = Math.atan2(unitVector.getY(), unitVector.getX())*180.0/Math.PI;
        Location transform = getLocation().derive(null, null, null, rotationTape);
        if (Math.abs(rotationTape - getLocation().getRotation()) > 0.1) {
            // HACK: something is not up-to-date -> refresh
            setLocation(transform);
        }

        if (visionOffset != null) {
            transform = transform.subtractWithRotation(visionOffset);
        }
        return transform;
    }

    protected Location transformFeederToMachineLocation(Location feederLocation, Location visionOffset) {
        return forwardTransform(feederLocation, getTransform(visionOffset));
    }

    protected Location transformMachineToFeederLocation(Location machineLocation, Location visionOffset) {
        return backwardTransform(machineLocation, getTransform(visionOffset));
    }

    public Location getPickLocation(long partInCycle, Location visionOffset)  {
        // If the feeder is advancing more than one part per feed cycle (e.g. with 2mm pitch tape or if a multiplier is
        // given), we need to cycle through multiple pick locations. partInCycle is 1-based and goes to getPartsPerFeedCycle().
        long offsetPitches = (getPartsPerFeedCycle() - partInCycle) % getPartsPerFeedCycle();
        Location feederLocation = new Location(partPitch.getUnits(), partPitch.multiply((double)offsetPitches).getValue(),
                0, 0, getRotationInFeeder());
        Location machineLocation = transformFeederToMachineLocation(feederLocation, visionOffset);
        return machineLocation;
    }


    public CvPipeline getPipeline() {
        return pipeline;
    }

    public void setPipeline(CvPipeline pipeline) {
        this.pipeline = pipeline;
    }

    public PipelineType getPipelineType() {
        return pipelineType;
    }

    public void setPipelineType(PipelineType pipelineType) {
        Object oldValue = this.pipelineType;
        this.pipelineType = pipelineType;
        firePropertyChange("pipelineType", oldValue, pipelineType);
    }

    public Location getVisionOffset() {
        if (isVisionEnabled() && visionOffset != null) {
            return visionOffset;
        }
        else {
            return Location.origin;
        }
    }

    public void setVisionOffset(Location visionOffset) {
        this.visionOffset = visionOffset;
    }

    public void resetCalibration() {
        setVisionOffset(null);
    }

    public void resetPipeline(PipelineType type) {
        pipeline = createDefaultPipeline(type);
        setPipelineType(type);
    }

    public Location getNominalVisionLocation() throws Exception {
        if (!(hole1Location.isInitialized() && hole2Location.isInitialized())) {
            // not yet initialized, just return the current camera location
            return getCamera().getLocation();
        }
        else {
            ensureCameraZ(getCamera(), false);
            return getHole1Location().add(getHole2Location()).multiply(0.5)
                    .deriveLengths(null, null, getLocation().getLengthZ(),
                            getLocation().getRotation()+getRotationInFeeder());
        }
    }


    public CvPipeline getCvPipeline(Camera camera, boolean clone, boolean autoSetup) {
        try {
            CvPipeline pipeline = getPipeline();
            if (clone) {
                pipeline = pipeline.clone();
            }
            pipeline.setProperty("camera", camera);
            pipeline.setProperty("feeder", this);
            pipeline.setProperty("sprocketHole.diameter", new Length(sprocketHoleDiameterMm, LengthUnit.Millimeters));
            Length range;
            if (autoSetup) {
                // Auto-Setup: search Range is half camera.
                Location upp = camera.getUnitsPerPixelAtZ();
                range = camera.getWidth() > camera.getHeight() ?
                        upp.getLengthY().multiply(camera.getHeight()/2)
                        : upp.getLengthX().multiply(camera.getWidth()/2);
            }
            else {
                // Normal mode: search range is half the distance between the holes plus one pitch.
                range = getHole1Location().getLinearLengthTo(getHole2Location())
                        .multiply(0.5)
                        .add(new Length(sprocketHolePitchMm, LengthUnit.Millimeters));
            }
            pipeline.setProperty("sprocketHole.maxDistance", range);

            return pipeline;
        }
        catch (CloneNotSupportedException e) {
            throw new Error(e);
        }
    }


    // Note: ReferencePushPullFeeder has different pipelines that include OCR.
    protected CvPipeline createDefaultPipeline(PipelineType type) {
        try {
            String xml = IOUtils.toString(BlindsFeeder.class
                    .getResource("ReferenceVisionFeeder-"+type.tag+"Pipeline.xml"));
            return new CvPipeline(xml);
        }
        catch (Exception e) {
            throw new Error(e);
        }
    }

    public enum FindFeaturesMode {
        FromPickLocationGetHoles,
        CalibrateHoles
    }

    public class FindFeatures {
        private Camera camera;
        private CvPipeline pipeline;
        private long showResultMilliseconds;
        private FindFeaturesMode autoSetupMode;
        private Location calibratedVisionOffset;
        private Location calibratedHole1Location;
        private Location calibratedHole2Location;
        private Location calibratedPickLocation;

        // recognized stuff
        private List<Result.Circle> holes;
        private List<Line> lines;

        public FindFeatures(Camera camera, CvPipeline pipeline, final long showResultMilliseconds, FindFeaturesMode autoSetupMode) {
            this.camera = camera;
            this.pipeline = pipeline;
            this.showResultMilliseconds = showResultMilliseconds;
            this.autoSetupMode = autoSetupMode;
        }

        public List<Result.Circle> getHoles() {
            return holes;
        }
        public List<Line> getLines() {
            return lines;
        }

        private void drawHoles(Mat mat, List<Result.Circle> features, Color color) {
            if (features == null || features.isEmpty()) {
                return;
            }
            for (Result.Circle circle : features) {
                org.opencv.core.Point c =  new org.opencv.core.Point(circle.x, circle.y);
                Imgproc.circle(mat, c, (int) (circle.diameter+0.5)/2, FluentCv.colorToScalar(color), 2, Imgproc.LINE_AA);
                Imgproc.circle(mat, c, 1, FluentCv.colorToScalar(color), 3, Imgproc.LINE_AA);
            }
        }

        private void drawLines(Mat mat, List<Line> lines, Color color) {
            if (lines == null || lines.isEmpty()) {
                return;
            }
            for (Line line : lines) {
                Imgproc.line(mat, line.a, line.b, FluentCv.colorToScalar(color), 2, Imgproc.LINE_AA);
            }
        }

        // number the parts in the pockets
        private void drawPartNumbers(Mat mat, Color color) {
            // make sure the numbers are not too dense
            int [] baseLine = null;
            double feederPocketPitchMm =  getPartPitch().convertToUnits(LengthUnit.Millimeters).getValue();
            if (feederPocketPitchMm < 1.) {
                // feeder not set up yet
                return;
            }

            // calculate the diagonal text size
            double fontScale = 1.0;
            Size size = Imgproc.getTextSize(String.valueOf(getPartsPerFeedCycle()),
                    Imgproc.FONT_HERSHEY_PLAIN, fontScale, 2, baseLine);
            Location textSizeMm = camera.getUnitsPerPixelAtZ().multiply(size.width, size.height, 0., 0.)
                    .convertToUnits(LengthUnit.Millimeters);
            if (textSizeMm.getY() < 0.0) {
                textSizeMm = textSizeMm.multiply(1.0, -1.0, 0.0, 0.0);
            }
            final double minFontSizeMm = 0.6;
            if (textSizeMm.getY() < minFontSizeMm) {
                fontScale = minFontSizeMm / textSizeMm.getY();
                textSizeMm = textSizeMm.multiply(fontScale, fontScale, 0.0, 0.0);
            }
            double textSizePitchCount = textSizeMm.getLinearDistanceTo(Location.origin)/feederPocketPitchMm;
            int step;
            if (textSizePitchCount < 0.75) {
                step = 1;
            }
            else if (textSizePitchCount < 1.5) {
                step = 2;
            }
            else if (textSizePitchCount < 4) {
                step = 5;
            }
            else {
                // something must be wrong - feeder probably not set up correctly (yet)
                return;
            }
            // go through all the parts, step-wise
            for (int i = step; i <= getPartsPerFeedCycle(); i += step) {
                String text = String.valueOf(i);
                Size textSize = Imgproc.getTextSize(text, Imgproc.FONT_HERSHEY_PLAIN, fontScale, 2, baseLine);

                Location partLocation = getPickLocation(i, calibratedVisionOffset)
                        .convertToUnits(LengthUnit.Millimeters);
                // TODO: go besides part
                Location textLocation = transformMachineToFeederLocation(partLocation, calibratedVisionOffset);
                textLocation = textLocation.add(new Location(LengthUnit.Millimeters, 0., -textSizeMm.getY()*0.25, 0., 0.));
                textLocation = transformFeederToMachineLocation(textLocation, calibratedVisionOffset)
                        .convertToUnits(LengthUnit.Millimeters);
                org.openpnp.model.Point p = VisionUtils.getLocationPixels(camera, textLocation);
                if (p.x > 0 && p.x < camera.getWidth() && p.y > 0 && p.y < camera.getHeight()) {
                    // roughly in the visible range - draw it
                    // determine the alignment based on where the text is located in relation to the pocket
                    double dx = textLocation.getX() - partLocation.getX();
                    double dy = textLocation.getY() - partLocation.getY();
                    // the alignment, in relation to the lower left corner of the text
                    double alignX, alignY;
                    if (Math.abs(dx) > Math.abs(dy)) {
                        // more horizontal displacement
                        if (dx < 0) {
                            // to the left
                            alignX = -textSize.width;
                            alignY = textSize.height/2;
                        }
                        else {
                            // to the right
                            alignX = 0.;
                            alignY = textSize.height/2;
                        }
                    }
                    else {
                        // more vertical displacement
                        if (dy > 0) {
                            // above
                            alignX = -textSize.width/2;
                            alignY = 0.0;
                        }
                        else {
                            // below
                            alignX = -textSize.width/2;
                            alignY = textSize.height;
                        }
                    }
                    Imgproc.putText(mat, text,
                            new org.opencv.core.Point(p.x + alignX, p.y + alignY),
                            Imgproc.FONT_HERSHEY_PLAIN,
                            fontScale,
                            FluentCv.colorToScalar(color), 2, 0, false);
                }
            }
        }


        public FindFeatures invoke() throws Exception {
            List resultsList = null;
            try {
                // in accordance with EIA-481 etc. we use all millimeters.
                Location mmScale = camera.getUnitsPerPixelAtZ()
                        .convertToUnits(LengthUnit.Millimeters);
                // reset the features
                holes = new ArrayList<>();
                lines = new ArrayList<>();

                if (calibrationTrigger == CalibrationTrigger.None) {
                    // No vision calibration wanted - just copy the pre-set locations
                    calibratedHole1Location = getHole1Location();
                    calibratedHole2Location = getHole2Location();
                    calibratedPickLocation  = getLocation();
                }
                else {
                    final double partPitchMinMm = 2;
                    final double sprocketHoleToPartMinMm = 3.5; // sprocket hole to part @ 8mm
                    final double sprocketHoleToPartGridMm = 2;  // +multiples of 2mm for wider tapes
                    final double sprocketHoleDiameterPx = sprocketHoleDiameterMm/mmScale.getX();
                    final double sprocketHolePitchPx = sprocketHolePitchMm/mmScale.getX();
                    final double sprocketHoleTolerancePx = sprocketHoleToleranceMm/mmScale.getX();
                    // Grab the results
                    resultsList = pipeline.getExpectedResult(VisionUtils.PIPELINE_RESULTS_NAME)
                            .getExpectedModel(List.class);

                    // Convert eligible results into circles
                    List<CvStage.Result.Circle> results = new ArrayList<>();;
                    for (Object result : resultsList) {
                        if ((result) instanceof Result.Circle) {
                            Result.Circle circle = ((Result.Circle) result);
                            if (Math.abs(circle.diameter*mmScale.getX() - sprocketHoleDiameterMm) < sprocketHoleToleranceMm) {
                                results.add(circle);
                            }
                            else {
                                Logger.debug("Dismissed Circle with non-compliant diameter "+(circle.diameter*mmScale.getX())+"mm, "
                                        + "allowed tolerance is ±"+sprocketHoleToleranceMm+"mm");
                            }
                        }
                        else if ((result) instanceof RotatedRect) {
                            RotatedRect rect = ((RotatedRect) result);
                            double diameter = (rect.size.width+rect.size.height)/2.0;
                            if (Math.abs(rect.size.width*mmScale.getX() - sprocketHoleDiameterMm) < sprocketHoleToleranceMm
                                    && Math.abs(rect.size.height*mmScale.getX() - sprocketHoleDiameterMm) < sprocketHoleToleranceMm) {
                                results.add(new Result.Circle(rect.center.x, rect.center.y, diameter));
                            }
                            else {
                                Logger.debug("Dismissed RotatedRect with non-compliant width or height "
                                        +(rect.size.width*mmScale.getX())+"mm x "+rect.size.height*mmScale.getX()+"mm, "
                                                + "allowed tolerance is ±"+sprocketHoleToleranceMm+"mm");
                            }
                        }
                        else if ((result) instanceof KeyPoint) {
                            KeyPoint keyPoint = ((KeyPoint) result);
                            results.add(new Result.Circle(keyPoint.pt.x, keyPoint.pt.y, sprocketHoleDiameterPx));
                        }
                    }

                    // collect the circles into a list of points
                    List<Point> points = new ArrayList<>();
                    for (Result.Circle circle : results) {
                        points.add(new Point(circle.x, circle.y));
                    }
                    List<Ransac.Line> ransacLines = Ransac.ransac(points, 100, sprocketHoleTolerancePx,
                            sprocketHolePitchPx, sprocketHoleTolerancePx, false);
                    if (ransacLines.isEmpty()) {
                        Logger.debug("Ransac algorithm has not found any lines of sprocket holes with "+sprocketHolePitchMm+"mm pitch, "
                                + "allowed pitch and line tolerance is ±"+sprocketHoleToleranceMm+"mm");
                    }
                    // Get the best line within the calibration tolerance
                    Ransac.Line bestLine = null;
                    Location bestUnitVector = null;
                    double bestDistanceMm = Double.MAX_VALUE;
                    for (Ransac.Line line : ransacLines) {
                        Point a = line.a;
                        Point b = line.b;

                        Location aLocation = VisionUtils.getPixelLocation(camera, a.x, a.y);
                        Location bLocation = VisionUtils.getPixelLocation(camera, b.x, b.y);

                        // Checks the distance to the line.
                        // In Auto-Setup/Preview mode we go from the pick location and there must be a minimum distance
                        // in order not to confuse pockets for sprocket holes. But then take the closest one, in order not
                        // to confuse with the neighboring tape's holes. We assume the pick location is always closer to our
                        // sprocket holes than to the neighboring tape's holes.
                        // In Calibration mode we are between the the sprocket holes, and there is no minimum distance
                        // and the line must simply be within calibration tolerance.
                        double distanceMm = camera.getLocation().convertToUnits(LengthUnit.Millimeters)
                                .getLinearDistanceToLineSegment(aLocation, bLocation);
                        double minDistanceMm = (autoSetupMode == FindFeaturesMode.CalibrateHoles ?
                                0 : minSprocketHolesDistanceMm)
                                - sprocketHoleToleranceMm;
                        double maxDistanceMm = (autoSetupMode == FindFeaturesMode.CalibrateHoles ?
                                calibrationToleranceMm : bestDistanceMm);

                        if (distanceMm >= minDistanceMm && distanceMm < maxDistanceMm) {
                            bestLine = line;
                            bestUnitVector = aLocation.unitVectorTo(bLocation);
                            bestDistanceMm = distanceMm;
                            lines.add(line);
                            if (autoSetupMode == FindFeaturesMode.CalibrateHoles) {
                                // Take the first line that is close enough, as the lines are ordered by length (descending).
                                break;
                            }
                            // Otherwise take the closest line, go on.
                        }
                        else if (autoSetupMode == null) {
                            lines.add(line);
                            Logger.debug("Dismissed line by distance, "+(distanceMm)+"mm, not within "+minDistanceMm+"mm .. "+maxDistanceMm+"mm");
                        }
                    }

                    if (autoSetupMode != null) {
                        if (bestLine == null) {
                            throw new Exception("No line of sprocket holes can be recognized");
                        }
                    }

                    if (bestLine != null) {
                        // Filter the circles by distance from the resulting line
                        for (Result.Circle circle : results) {
                            Point p = new Point(circle.x, circle.y);
                            if (FluentCv.pointToLineDistance(bestLine.a, bestLine.b, p) <= sprocketHoleTolerancePx) {
                                holes.add(circle);
                            }
                        }

                        // Sort holes by distance from camera center.
                        Collections.sort(holes, new Comparator<Result.Circle>() {
                            @Override
                            public int compare(Result.Circle o1, Result.Circle o2) {
                                double d1 = VisionUtils.getPixelLocation(camera, o1.x, o1.y).getLinearDistanceTo(camera.getLocation());
                                double d2 = VisionUtils.getPixelLocation(camera, o2.x, o2.y).getLinearDistanceTo(camera.getLocation());
                                return Double.compare(d1, d2);
                            }
                        });

                        if (autoSetupMode == FindFeaturesMode.FromPickLocationGetHoles
                                || (autoSetupMode == null && !(getHole1Location().isInitialized() && getHole2Location().isInitialized()))) {
                            // because we sorted the holes by distance, the first two are our holes 1 and 2
                            if (holes.size() < 2) {
                                throw new Exception("At least two sprocket holes need to be recognized");
                            }
                            calibratedHole1Location = VisionUtils.getPixelLocation(camera, holes.get(0).x, holes.get(0).y)
                                    .convertToUnits(LengthUnit.Millimeters);
                            calibratedHole2Location = VisionUtils.getPixelLocation(camera, holes.get(1).x, holes.get(1).y)
                                    .convertToUnits(LengthUnit.Millimeters);
                            Location partLocation = camera.getLocation().convertToUnits(LengthUnit.Millimeters);
                            double angle1 = Math.atan2(calibratedHole1Location.getY()-partLocation.getY(), calibratedHole1Location.getX()-partLocation.getX());
                            double angle2 = Math.atan2(calibratedHole2Location.getY()-partLocation.getY(), calibratedHole2Location.getX()-partLocation.getX());
                            double angleDiff = Utils2D.angleNorm(Math.toDegrees(angle2-angle1), 180);
                            if (angleDiff > 0) {
                                // The holes 1 and 2 must appear counter-clockwise from the part location, swap them!
                                Location swap = calibratedHole2Location;
                                calibratedHole2Location = calibratedHole1Location;
                                calibratedHole1Location = swap;
                            }
                            if (calibratedHole1Location.unitVectorTo(calibratedHole2Location)
                                    .dotProduct(bestUnitVector).getValue() < 0.0) {
                                // turn the unite vector around
                                bestUnitVector = bestUnitVector.multiply(-1.0, -1.0, 0, 0);
                            }
                            // determine the correct transformation
                            double angleTape = Math.atan2(bestUnitVector.getY(), bestUnitVector.getX())*180.0/Math.PI;
                            // preliminary pick location
                            calibratedPickLocation = camera.getLocation()
                                    .derive(getLocation(), false, false, true, false) // previous Z
                                    .derive(null,  null, null, angleTape); // preliminary feeeder orientation
                        }
                        else {
                            // find the two holes matching
                            for (Result.Circle hole : holes) {
                                Location l = VisionUtils.getPixelLocation(camera, hole.x, hole.y)
                                        .convertToUnits(LengthUnit.Millimeters);
                                double dist1Mm = l.getLinearDistanceTo(getHole1Location());
                                double dist2Mm = l.getLinearDistanceTo(getHole2Location());
                                if (dist1Mm < calibrationToleranceMm && dist1Mm < dist2Mm) {
                                    calibratedHole1Location = l;
                                }
                                else if (dist2Mm < calibrationToleranceMm && dist2Mm < dist1Mm) {
                                    calibratedHole2Location = l;
                                }
                            }
                            if (calibratedHole1Location == null || calibratedHole2Location == null) {
                                if (autoSetupMode  == FindFeaturesMode.CalibrateHoles) {
                                    throw new Exception("The two reference sprocket holes cannot be recognized");
                                }
                            }
                            else {
                                if (calibratedHole1Location.unitVectorTo(calibratedHole2Location)
                                        .dotProduct(bestUnitVector).getValue() < 0.0) {
                                    // turn the unit vector around
                                    bestUnitVector = bestUnitVector.multiply(-1.0, -1.0, 0, 0);
                                }
                                if (snapToAxis) {
                                    if (Math.abs(bestUnitVector.getX()) > Math.abs(bestUnitVector.getY())*5) {
                                        // close enough, snap to X
                                        bestUnitVector = new Location(LengthUnit.Millimeters, Math.signum(bestUnitVector.getX()), 0, 0, 0);
                                    }
                                    else if (Math.abs(bestUnitVector.getY()) > Math.abs(bestUnitVector.getX())*5) {
                                        // close enough, snap to Y
                                        bestUnitVector = new Location(LengthUnit.Millimeters, 0, Math.signum(bestUnitVector.getY()), 0, 0);
                                    }
                                }
                                // determine the correct transformation
                                double angleTape = Math.atan2(bestUnitVector.getY(), bestUnitVector.getX())*180.0/Math.PI;
                                // the new calibration target is really the mid-point
                                Location midPoint = calibratedHole1Location.add(calibratedHole2Location).multiply(0.5, 0.5, 0, 0);
                                // but let's project that back to the real hole positions with nominal pitch (undistorted by the camera lens and Z parallax)
                                double distanceHolesMm = Math.round(calibratedHole1Location.getLinearDistanceTo(calibratedHole2Location)
                                        /sprocketHolePitchMm)*sprocketHolePitchMm;
                                calibratedHole1Location = midPoint.subtract(bestUnitVector.multiply(distanceHolesMm*0.5, distanceHolesMm*0.5, 0, 0));
                                calibratedHole2Location = midPoint.add(bestUnitVector.multiply(distanceHolesMm*0.5, distanceHolesMm*0.5, 0, 0));
                                Logger.trace("[ReferenceVisionFeeder] calibrated hole locations are: " + calibratedHole1Location + ", " +calibratedHole2Location);
                                if (autoSetupMode  == FindFeaturesMode.CalibrateHoles) {
                                    // get the current pick location relative to hole 1
                                    Location pickLocation = getLocation().convertToUnits(LengthUnit.Millimeters);
                                    Location relativePickLocation = pickLocation
                                            .subtract(getHole1Location());
                                    // rotate from old angle
                                    relativePickLocation =  relativePickLocation.rotateXy(-pickLocation.getRotation())
                                            .derive(null, null, null, 0.0);
                                    // normalize to a nominal local pick location according to EIA 481
                                    if (normalizePickLocation) {
                                        relativePickLocation = new Location(LengthUnit.Millimeters,
                                                Math.round(relativePickLocation.getX()/partPitchMinMm)*partPitchMinMm,
                                                -sprocketHoleToPartMinMm+Math.round((relativePickLocation.getY()+sprocketHoleToPartMinMm)/sprocketHoleToPartGridMm)*sprocketHoleToPartGridMm,
                                                0, 0);
                                    }
                                    // calculate the new pick location with the new hole 1 location and tape angle
                                    calibratedPickLocation = calibratedHole1Location.add(relativePickLocation.rotateXy(angleTape))
                                            .derive(null, null, pickLocation.getZ(), angleTape);
                                }
                            }
                        }

                        if (calibratedHole1Location != null && calibratedPickLocation != null) {
                            // we have our calibrated locations
                            // Get the calibrated vision offset (with Z always 0)
                            calibratedVisionOffset = getLocation()
                                    .subtractWithRotation(calibratedPickLocation)
                                    .derive(null, null, 0.0, null);
                            Logger.debug("calibrated vision offset is: " + calibratedVisionOffset
                                    + ", length is: "+calibratedVisionOffset.getLinearLengthTo(Location.origin));

                            // Add tick marks for show
                            if (calibratedPickLocation != null) {
                                org.openpnp.model.Point a;
                                org.openpnp.model.Point b;
                                Location tick = new Location(LengthUnit.Millimeters, -bestUnitVector.getY(), bestUnitVector.getX(), 0, 0);
                                a = VisionUtils.getLocationPixels(camera, calibratedPickLocation.subtract(tick));
                                b = VisionUtils.getLocationPixels(camera, calibratedPickLocation.add(tick));
                                lines.add(new Ransac.Line(new Point(a.x, a.y), new Point(b.x, b.y)));
                                a = VisionUtils.getLocationPixels(camera, calibratedPickLocation.subtract(bestUnitVector));
                                b = VisionUtils.getLocationPixels(camera, calibratedPickLocation.add(bestUnitVector));
                                lines.add(new Ransac.Line(new Point(a.x, a.y), new Point(b.x, b.y)));
                                Logger.debug("calibrated pick location is: " + calibratedPickLocation);
                            }
                        }
                    }
                }

                if (showResultMilliseconds > 0) {
                    // Draw the result onto the pipeline image.
                    Mat resultMat = pipeline.getWorkingImage().clone();
                    drawHoles(resultMat, getHoles(), Color.green);
                    drawLines(resultMat, getLines(), new Color(0, 0, 255));
                    drawPartNumbers(resultMat, Color.orange);
                    if (getHoles().isEmpty()) {
                        Imgproc.line(resultMat, new Point(0, 0), new Point(resultMat.cols()-1, resultMat.rows()-1),
                                FluentCv.colorToScalar(Color.red), 2, Imgproc.LINE_AA);
                        Imgproc.line(resultMat, new Point(0, resultMat.rows()-1), new Point(resultMat.cols()-1, 0),
                                FluentCv.colorToScalar(Color.red), 2, Imgproc.LINE_AA);
                    }

                    if (Logger.getLevel() == org.pmw.tinylog.Level.DEBUG || Logger.getLevel() == org.pmw.tinylog.Level.TRACE) {
                        File file = Configuration.get().createResourceFile(getClass(), "push-pull-feeder", ".png");
                        Imgcodecs.imwrite(file.getAbsolutePath(), resultMat);
                    }
                    BufferedImage showResult = OpenCvUtils.toBufferedImage(resultMat);
                    resultMat.release();
                    MainFrame.get().getCameraViews().getCameraView(camera)
                    .showFilteredImage(showResult, showResultMilliseconds);
                }
            }
            catch (ClassCastException e) {
                throw new Exception("Unrecognized result type (should be Result.Circle, RotatedRect, KeyPoint): " + resultsList);
            }
            return this;
        }
    }

    public void showFeatures() throws Exception {
        Camera camera = getCamera();
        ensureCameraZ(camera, true);
        try (CvPipeline pipeline = getCvPipeline(camera, true, true)) {

            // Process vision and show feature without applying anything
            pipeline.process();
            new FindFeatures(camera, pipeline, 2000, null).invoke();
        }
    }

    public void autoSetup() throws Exception {
        Camera camera = getCamera();
        if (calibrationTrigger == CalibrationTrigger.None) {
            // Just assume the user wants it now
            setCalibrationTrigger(CalibrationTrigger.UntilConfident);
        }

        ensureCameraZ(camera, true);
        // Try with cloned pipeline.
        Exception e = autoSetupPipeline(camera, null);
        if (e != null) {
            Logger.debug(e, "Auto-Setup: exception");
            // Failed, try with pipeline type defaults.
            for (PipelineType type : PipelineType.values()) {
                Logger.debug("Auto-Setup: trying with stock pipeline type "+type);
                e = autoSetupPipeline(camera, type);
                if (e == null) {
                    // Success.
                    Logger.debug(e, "Auto-Setup: success");
                    return;
                }
                Logger.debug(e, "Auto-Setup: exception");
            }
            // Still no luck, throw.
            Logger.debug(e, "Auto-Setup: final exception");
            throw e;
        }
    }

    protected Exception autoSetupPipeline(Camera camera, PipelineType type) {
        if (type != null) {
            resetPipeline(type);
        }
        try (CvPipeline pipeline = getCvPipeline(camera, true, true)) {
            // Process vision and get some features
            pipeline.process();
            FindFeatures feature = new FindFeatures(camera, pipeline, 2000, FindFeaturesMode.FromPickLocationGetHoles)
                    .invoke();
            // Store the initial vision based results
            setLocation(feature.calibratedPickLocation);
            setHole1Location(feature.calibratedHole1Location);
            setHole2Location(feature.calibratedHole2Location);
            // As we've changed all this -> reset any stats
            resetCalibrationStatistics();
            try {
                // Now run a sprocket hole calibration, make sure to change the part (not swap it)
                performVisionOperations(camera, pipeline, true, true, false, null);
            }
            finally {
                // Move the camera back to the pick location, including when there is an exception.
                MovableUtils.moveToLocationAtSafeZ(camera, getLocation());
                MovableUtils.fireTargetedUserAction(camera);
            }
            return null;
        }
        catch (Exception e) {
            return e;
        }
    }

    public Length getTapeWidth() {
        // infer the tape width from EIA-481 where:
    	// 1) tape edge to hole middle = 1.75mm
    	// 2) hole middle to part middle is 3.5 / 5.5 / 7.5 / 11.5mm for tape of 8 / 12 / 16 / 24mm width
    	// 3) therefore
    	// 3) a) hole middle to part middle + 0.5mm = half of tape width
    	// 3) b) (holeLocation + 0.5mm) * 2 = tapeWidth
        Location hole1Location = transformMachineToFeederLocation(getHole1Location(), null)
                .convertToUnits(LengthUnit.Millimeters);
        final double partToSprocketHoleHalfTapeWidthDiffMm = 0.5; // deducted from EIA-481
        double tapeWidth = Math.round(hole1Location.getY()+partToSprocketHoleHalfTapeWidthDiffMm)*2;
        return new Length(tapeWidth, LengthUnit.Millimeters);
    }





    protected static Location relocatedLocation(Location location, Location oldTransform, Location newTransform) {
        if (!location.isInitialized()) {
            // a location with all zeroes is assumed as uninitialized
            return location;
        }
        else {
            Location feederLocalLocation = backwardTransform(location, oldTransform);
            location = forwardTransform(feederLocalLocation, newTransform);
            return location;
        }
    }
    protected static Location relocatedXyLocation(Location location, Location oldTransform, Location newTransform) {
        // disregard Z and Rotation
        return relocatedLocation(location.multiply(1.0, 1.0, 0.0, 0.0), oldTransform, newTransform);
    }
    protected static Location relocatedXyzLocation(Location location, Location oldTransform, Location newTransform) {
        // disregard Rotation
        return relocatedLocation(location.multiply(1.0, 1.0, 1.0, 0.0), oldTransform, newTransform);
    }





    @Override
    public Location getJobPreparationLocation() {
        if (visionOffset == null
            && (calibrationTrigger != CalibrationTrigger.None)) {
            return getPickLocation(0, null);
        }
        else {
            return null;
        }
    }

    @Override
    public void prepareForJob(boolean visit) throws Exception {
        super.prepareForJob(visit);
        if (visit && visionOffset == null) {
            if (calibrationTrigger != CalibrationTrigger.None) {
                // Calibrate the feeder.
            	performSprocketCalibration(null);
            }
            else {
                assertCalibrated(false);
            }
        }
    }


    public void performSprocketCalibration(StringBuilder report) throws Exception {
        Camera camera = getCamera();
        try (CvPipeline pipeline = getCvPipeline(camera, true, false)) {
            // run a sprocket hole calibration
            performVisionOperations(camera, pipeline, false, false, true, report);
        }
    }


    protected void performVisionOperations(Camera camera, CvPipeline pipeline,
            boolean storeHoles, boolean storePickLocation, boolean storeVisionOffset,
            StringBuilder report) throws Exception {
        Location runningHole1Location = getHole1Location();
        Location runningHole2Location = getHole2Location();
        Location runningPickLocation = getLocation();
        Location runningVisionOffset = getVisionOffset();
        ensureCameraZ(camera, true);

        FindFeatures feature = null;

        if (storeHoles || storePickLocation || storeVisionOffset) {
            // Calibrate the exact hole locations by obtaining a mid-point lock on them,
            // assuming that any camera lens and Z parallax distortion is symmetric.
            for (int i = 0; i < calibrateMaxPasses; i++) {
                // move the camera to the mid-point
                Location midPoint = runningHole1Location.add(runningHole2Location).multiply(0.5, 0.5, 0, 0)
                        .derive(camera.getLocation(), false, false, true, false)
                        .derive(null, null, null, runningPickLocation.getRotation()+getRotationInFeeder());
                Logger.debug("calibrating sprocket holes pass "+ i+ " midPoint is "+midPoint);
                MovableUtils.moveToLocationAtSafeZ(camera, midPoint);
                // take a new shot
                pipeline.process();
                feature = new FindFeatures(camera, pipeline, 2000, FindFeaturesMode.CalibrateHoles)
                        .invoke();
                runningHole1Location = feature.calibratedHole1Location;
                runningHole2Location = feature.calibratedHole2Location;
                runningPickLocation = feature.calibratedPickLocation;
                // calculate the worst pick location delta this gives, cycle part 1 is the worst as it is farthest away
                Location uncalibratedPick1Location = getPickLocation(1, runningVisionOffset);
                Location calibratedPick1Location = getPickLocation(1, feature.calibratedVisionOffset);
                Length error = calibratedPick1Location.getLinearLengthTo(uncalibratedPick1Location);
                Logger.trace("new vision offset "+feature.calibratedVisionOffset
                        +" vs. previous vision offset "+runningVisionOffset+" results in error "+error+" at the (farthest) pick location");
                // store data if requested
                if (storeHoles) {
                    setHole1Location(runningHole1Location);
                    setHole2Location(runningHole2Location);
                }
                if (storePickLocation) {
                    setLocation(runningPickLocation);
                }
                if (storeVisionOffset) {
                    // update the stats
                    if (visionOffset != null) {
                        // Only when a previous vision offset has been stored, should we store the error
                        // because the feeder might have been moved physically. The user's actions are
                        // not part of the calibration error. :-)
                        addCalibrationError(error);
                    }
                    setVisionOffset(feature.calibratedVisionOffset);
                }
                // is it good enough? Compare with running offset.
                if (error.convertToUnits(LengthUnit.Millimeters).getValue() < calibrateToleranceMm) {
                    break;
                }
                runningVisionOffset = feature.calibratedVisionOffset;
            }
        }
    }

}
