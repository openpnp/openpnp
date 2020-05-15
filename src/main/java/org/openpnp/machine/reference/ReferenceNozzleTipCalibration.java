package org.openpnp.machine.reference;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.opencv.core.KeyPoint;
import org.opencv.core.RotatedRect;
import org.openpnp.gui.MainFrame;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Point;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.Utils2D;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage.Result;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Commit;

@Root
public class ReferenceNozzleTipCalibration extends AbstractModelObject {
    public static interface RunoutCompensation {

        Location getOffset(double angle);

        Location getCameraOffset();

        Location getAxisOffset();

        @Override
        String toString();
    }

    public static class TableBasedRunoutCompensation implements ReferenceNozzleTipCalibration.RunoutCompensation {
        @Element(required = false)
        List<Location> nozzleTipMeasuredLocations;

        public TableBasedRunoutCompensation() {
        }
        public TableBasedRunoutCompensation(List<Location> nozzleTipMeasuredLocations) {
            //store data for later usage
            this.nozzleTipMeasuredLocations = nozzleTipMeasuredLocations;
        }

        @Override
        public Location getOffset(double angle) {

            // find the two angles in the measurements, that angle is between
            List<Location> offsets = getOffsetPairForAngle(angle);

            // the xy-offset is available via getX/getY. the angle is available via getRotation() - it's stored this way because then a simple Location type is sufficient
            Location offsetA = offsets.get(0);
            Location offsetB = offsets.get(1).convertToUnits(offsetA.getUnits());    // could this conversion be omitted?

            double ratio = 1.0;     // TODO Better solution than the workaround seems to be recommended.
            if ( (offsetB.getRotation() - offsetA.getRotation()) != 0 ) {
                ratio = (angle - offsetA.getRotation()) / (offsetB.getRotation() - offsetA.getRotation());
            }
            double deltaX = offsetB.getX() - offsetA.getX();
            double deltaY = offsetB.getY() - offsetA.getY();
            double offsetX = offsetA.getX() + (deltaX * ratio);
            double offsetY = offsetA.getY() + (deltaY * ratio);

            return new Location(offsetA.getUnits(), offsetX, offsetY, 0, 0);
        }

        @Override
        public Location getCameraOffset() {
            return new Location(nozzleTipMeasuredLocations.get(0).getUnits());
        }

        /**
         * Find the two closest offsets to the angle being requested. The offsets start at first measurement at angleStart
         * and go to angleStop
         */
        private List<Location> getOffsetPairForAngle(double angle) {
            Location a = null, b = null;

            // angle asked for is the last in the table?

            // Make sure the angle is between -180 and 180 - angles can get larger/smaller as +-180 if limitation to 180 degrees is disabled
            while (angle < -180) {
                angle += 360;
            }
            while (angle > 180) {
                angle -= 360;
            }

            if (angle >= nozzleTipMeasuredLocations.get(nozzleTipMeasuredLocations.size() - 1).getRotation()) {
                return Arrays.asList(nozzleTipMeasuredLocations.get(nozzleTipMeasuredLocations.size() - 1), nozzleTipMeasuredLocations.get(0));
            }

            for (int i = 0; i < nozzleTipMeasuredLocations.size(); i++) {
                if (angle < nozzleTipMeasuredLocations.get(i + 1).getRotation()) {
                    a = nozzleTipMeasuredLocations.get(i);
                    b = nozzleTipMeasuredLocations.get(i + 1);
                    break;
                }
            }
            return Arrays.asList(a, b);
        }

        @Override
        public String toString() {
            return String.format(Locale.US, "%d°-offset x=%f, y=%f", (int)nozzleTipMeasuredLocations.get(0).getRotation(), nozzleTipMeasuredLocations.get(0).getX(), nozzleTipMeasuredLocations.get(0).getY());
        }

        @Override
        public Location getAxisOffset() {
            // axis offset is not available with this algorithm
            return null;
        }
    }

    public static class ModelBasedRunoutCompensation implements ReferenceNozzleTipCalibration.RunoutCompensation {
        protected List<Location> nozzleTipMeasuredLocations;

        @Attribute(required = false)
        protected double centerX = 0;
        @Attribute(required = false)
        protected double centerY = 0;
        @Attribute(required = false)
        protected double radius = 0;
        @Attribute(required = false)
        protected double phaseShift;
        @Attribute(required = false)
        protected LengthUnit units = LengthUnit.Millimeters;
        @Attribute(required = false)
        protected Double peakError;
        @Attribute(required = false)
        protected Double rmsError;

        public ModelBasedRunoutCompensation() {
        }
        public ModelBasedRunoutCompensation(List<Location> nozzleTipMeasuredLocations) {
            //store data for possible later usage
            this.nozzleTipMeasuredLocations = nozzleTipMeasuredLocations;
            // save the units as the model is persisted without the locations 
            this.units = nozzleTipMeasuredLocations.size() > 0 ? 
                    nozzleTipMeasuredLocations.get(0).getUnits() : LengthUnit.Millimeters;

            // first calculate the circle fit and store the values to centerXY and radius
            // the measured offsets describe a circle with the rotational axis as the center, the runout is the circle radius
            this.calcCircleFitKasa(nozzleTipMeasuredLocations);

            // afterwards calc the phaseShift angle mapping
            this.calcPhaseShift(nozzleTipMeasuredLocations);
            
            estimateModelError(nozzleTipMeasuredLocations);

            Logger.debug("[nozzleTipCalibration]calculated nozzleEccentricity: {}", this.toString());
        }

        /**
         * Constructor that uses an affine transform to initialize the model
         * @param nozzleTipMeasuredLocations - list of measured nozzle tip locations
         * @param nozzleTipExpectedLocations - list of expected nozzle tip locations
         */
        public ModelBasedRunoutCompensation(List<Location> nozzleTipMeasuredLocations, List<Location> nozzleTipExpectedLocations) {
            // save the units as the model is persisted without the locations 
            this.units = nozzleTipMeasuredLocations.size() > 0 ? 
                    nozzleTipMeasuredLocations.get(0).getUnits() : LengthUnit.Millimeters;

            //Compute the best fit affine transform that takes the expected locations to the measured locations
            AffineTransform at = Utils2D.deriveAffineTransform(nozzleTipExpectedLocations, nozzleTipMeasuredLocations);
            Utils2D.AffineInfo ai = Utils2D.affineInfo(at);
            Logger.trace("[nozzleTipCalibration]nozzle tip affine transform = " + ai);
            
            //The expected locations were generated with a deliberate 1 mm runout so the affine scale is a direct 
            //measure of the true runout in millimeters.  However, since the affine transform gives both an x and y
            //scaling, their geometric mean is used to compute the radius.  Note that if measurement noise
            //dominates the actual runout, it is possible for the scale factors to become negative.  In
            //that case, the radius will be set to zero.
            this.radius = new Length( Math.sqrt(Math.max(0, ai.xScale) * Math.max(0, ai.yScale)),
                    LengthUnit.Millimeters).convertToUnits(this.units).getValue();
            
            //The phase shift is just the rotation of the affine transform (negated because of the subtraction in getRunout)
            this.phaseShift = -ai.rotationAngleDeg;
            
            //The center is just the translation part of the affine transform
            this.centerX = new Length( ai.xTranslation, LengthUnit.Millimeters).convertToUnits(this.units).getValue();
            this.centerY = new Length( ai.yTranslation, LengthUnit.Millimeters).convertToUnits(this.units).getValue();
            
            estimateModelError(nozzleTipMeasuredLocations);

            Logger.debug("[nozzleTipCalibration]calculated nozzleEccentricity: {}", this.toString());
        }

        /**
         * Estimates the model error based on the distance between the nozzle
         * tip measured locations and the locations computed by the model
         * @param nozzleTipMeasuredLocations - list of measured nozzle tip locations
         */
        private void estimateModelError(List<Location> nozzleTipMeasuredLocations) {
            Location peakErrorLocation = new Location(this.units);
            double sumError2 = 0;
            peakError = 0.0;
            for (Location l : nozzleTipMeasuredLocations) {
                //can't use getOffset here because it may be overridden
                Location m = getRunout(l.getRotation()).add(new Location(this.units, this.centerX, this.centerY, 0, 0));
                Logger.trace("[nozzleTipCalibration]compare measured = {}, modeled = {}", l, m);
                double error = l.convertToUnits(this.units).getLinearDistanceTo(m);
                sumError2 += error*error;
                if (error > peakError) {
                    peakError = error;
                    peakErrorLocation = l;
                }
            }
            rmsError = Math.sqrt(sumError2/nozzleTipMeasuredLocations.size());
            Logger.trace("[nozzleTipCalibration]peak error location = {}, error = {}", peakErrorLocation, peakError);
        }
        
        /* function to calc the model based runout in cartesian coordinates */
        public Location getRunout(double angle) {
            //add phase shift
            angle = angle - this.phaseShift;

            angle = Math.toRadians(angle);

            // convert from polar coords to xy cartesian offset values
            double offsetX = (this.radius * Math.cos(angle));
            double offsetY = (this.radius * Math.sin(angle));

            return new Location(this.units, offsetX, offsetY, 0, 0);
        }

        /* function to calc the model based offset in cartesian coordinates */
        @Override
        public Location getOffset(double angle) {

            Location location = getRunout(angle);

            return location.add(new Location(this.units, this.centerX, this.centerY, 0, 0));
        }

        @Override
        public Location getCameraOffset() {
            return new Location(this.units);
        }

        protected void calcCircleFitKasa(List<Location> nozzleTipMeasuredLocations) {
            /* 
             * this function fits a circle my means of the Kasa Method to the given List<Location>.
             * this is a java port of http://people.cas.uab.edu/~mosya/cl/CPPcircle.html 
             * The Kasa method should work well for this purpose since the measured locations are captured along a full circle
             */
            int n;

            double kasaXi,kasaYi,kasaZi;
            double kasaMxy,kasaMxx,kasaMyy,kasaMxz,kasaMyz;
            double kasaB,kasaC,kasaG11,kasaG12,kasaG22,kasaD1,kasaD2;
            double kasaMeanX=0.0, kasaMeanY=0.0;

            n = nozzleTipMeasuredLocations.size();

            Iterator<Location> nozzleTipMeasuredLocationsIterator = nozzleTipMeasuredLocations.iterator();
            while (nozzleTipMeasuredLocationsIterator.hasNext()) {
                Location measuredLocation = nozzleTipMeasuredLocationsIterator.next();
                kasaMeanX += measuredLocation.getX();
                kasaMeanY += measuredLocation.getY();
            }
            kasaMeanX = kasaMeanX / (double)nozzleTipMeasuredLocations.size();
            kasaMeanY = kasaMeanY / (double)nozzleTipMeasuredLocations.size();

            kasaMxx=kasaMyy=kasaMxy=kasaMxz=kasaMyz=0.;

            for (int i = 0; i < n; i++) {
                kasaXi = nozzleTipMeasuredLocations.get(i).getX() - kasaMeanX;   //  centered x-coordinates
                kasaYi = nozzleTipMeasuredLocations.get(i).getY() - kasaMeanY;   //  centered y-coordinates
                kasaZi = kasaXi*kasaXi + kasaYi*kasaYi;

                kasaMxx += kasaXi*kasaXi;
                kasaMyy += kasaYi*kasaYi;
                kasaMxy += kasaXi*kasaYi;
                kasaMxz += kasaXi*kasaZi;
                kasaMyz += kasaYi*kasaZi;
            }
            kasaMxx /= n;
            kasaMyy /= n;
            kasaMxy /= n;
            kasaMxz /= n;
            kasaMyz /= n;

            // solving system of equations by Cholesky factorization
            kasaG11 = Math.sqrt(kasaMxx);
            kasaG12 = kasaMxy / kasaG11;
            kasaG22 = Math.sqrt(kasaMyy - kasaG12 * kasaG12);

            kasaD1 = kasaMxz / kasaG11;
            kasaD2 = (kasaMyz - kasaD1*kasaG12)/kasaG22;

            // computing parameters of the fitting circle
            kasaC = kasaD2/kasaG22/2.0;
            kasaB = (kasaD1 - kasaG12*kasaC)/kasaG11/2.0;

            // assembling the output
            Double centerX = kasaB + kasaMeanX;
            Double centerY = kasaC + kasaMeanY;
            Double radius = Math.sqrt(kasaB*kasaB + kasaC*kasaC + kasaMxx + kasaMyy);

            // saving output if valid
            // the values are NaN if all given nozzleTipMeasuredLocations are the same (this is the case probably only on a simulated machine with nulldriver)
            if ( !centerX.isNaN() && !centerY.isNaN() && !radius.isNaN() ) {
                // values valid
                this.centerX = centerX;
                this.centerY = centerY;
                this.radius = radius;
            } else {
                // nozzletip has zero runout and constant offset to bottom camera
                this.centerX = nozzleTipMeasuredLocations.get(0).getX();
                this.centerY = nozzleTipMeasuredLocations.get(0).getY();
                this.radius = 0;
            }
        }

        protected void calcPhaseShift(List<Location> nozzleTipMeasuredLocations) {
            /*
             * The phaseShift is calculated to map the angle the nozzle is located mechanically at
             * (that is what openpnp shows in the DRO) to the angle, the nozzle tip is located wrt. to the
             * centered circle runout path.
             * With the phaseShift available, the calibration offset can be calculated analytically for every
             * location/rotation even if not captured while measured (stepped by angleIncrement)
             * 
             */
            double phaseShift = 0;

            double angle=0;
            double measuredAngle=0;
            double priorDifferenceAngle = 0;
            double differenceAngleMean=0;

            Iterator<Location> nozzleTipMeasuredLocationsIterator = nozzleTipMeasuredLocations.iterator();
            while (nozzleTipMeasuredLocationsIterator.hasNext()) {
                Location measuredLocation = nozzleTipMeasuredLocationsIterator.next();

                // get the measurement rotation
                angle = measuredLocation.getRotation();		// the angle at which the measurement was made was stored to the nozzleTipMeasuredLocation into the rotation attribute

                // move the offset-location by the centerY/centerY. by this all offset-locations are wrt. the 0/0 origin
                Location centeredLocation = measuredLocation.subtract(new Location(this.units,this.centerX,this.centerY,0.,0.));

                // calculate the angle, the nozzle tip is located at
                measuredAngle=Math.toDegrees(Math.atan2(centeredLocation.getY(), centeredLocation.getX()));

                // the difference is the phaseShift
                double differenceAngle = angle-measuredAngle;

                //Correct for a possible phase wrap past +/-180 degrees
                while ((priorDifferenceAngle-differenceAngle) > 180) {
                        differenceAngle += 360;
                }
                while ((priorDifferenceAngle-differenceAngle) < -180) {
                        differenceAngle -= 360;
                }
                priorDifferenceAngle = differenceAngle;
                
                Logger.trace("[nozzleTipCalibration]differenceAngle: {}", differenceAngle);

                // sum up all differenceAngles to build the average later
                differenceAngleMean += differenceAngle;
            }

            // calc the average and normalize it to +/-180 degrees
            phaseShift = Utils2D.normalizeAngle180(differenceAngleMean / nozzleTipMeasuredLocations.size());

            this.phaseShift = phaseShift;
        }


        @Override
        public String toString() {
            return String.format(Locale.US, "Center %.3f, %.3f, Runout %.3f, Phase %.3f, Peak err %.3f, RMS err %.3f %s", centerX, centerY, radius, phaseShift, peakError, rmsError, units.getShortName());
        }

        @Override
        public Location getAxisOffset() {
            return new Location(this.units,centerX,centerY,0.,0.);
        }


        public double getPhaseShift() {
            return phaseShift;
        }
        
        /**
         * @return The peak error (in this.units) of the measured nozzle tip
         * locations relative to the locations computed by the model
         */
        public Double getPeakError() {
            return peakError;
        }
        
        /**
         * @return The rms error (in this.units) of the measured nozzle tip
         * locations relative to the locations computed by the model
         */
       public Double getRmsError() {
            return rmsError;
        }
    }

    public static class ModelBasedRunoutNoOffsetCompensation extends ReferenceNozzleTipCalibration.ModelBasedRunoutCompensation {
        public ModelBasedRunoutNoOffsetCompensation() {
            super();
        }
        public ModelBasedRunoutNoOffsetCompensation(List<Location> nozzleTipMeasuredLocations) {
            super(nozzleTipMeasuredLocations);
        }
        public ModelBasedRunoutNoOffsetCompensation(List<Location> nozzleTipMeasuredLocations, List<Location> nozzleTipExpectedLocations) {
            super(nozzleTipMeasuredLocations, nozzleTipExpectedLocations);
        }

        @Override
        public String toString() {
            return String.format(Locale.US, "Camera position error %.3f, %.3f, Runout %.3f, Phase %.3f, Peak err %.3f, RMS err %.3f %s", centerX, centerY, radius, phaseShift, peakError, rmsError, units.getShortName());
        }

        @Override 
        public Location getOffset(double angle) {
            // Just return the runout, do not add the axis offset.
            return getRunout(angle);
        }

    }
    public static class ModelBasedRunoutCameraOffsetCompensation extends ReferenceNozzleTipCalibration.ModelBasedRunoutNoOffsetCompensation {
        public ModelBasedRunoutCameraOffsetCompensation() {
            super();
        }
        public ModelBasedRunoutCameraOffsetCompensation(List<Location> nozzleTipMeasuredLocations) {
            super(nozzleTipMeasuredLocations);
        }
        public ModelBasedRunoutCameraOffsetCompensation(List<Location> nozzleTipMeasuredLocations, List<Location> nozzleTipExpectedLocations) {
            super(nozzleTipMeasuredLocations, nozzleTipExpectedLocations);
        }
        
        @Override
        public String toString() {
            return String.format(Locale.US, "Camera position offset %.3f, %.3f, Runout %.3f, Phase %.3f, Peak err %.3f, RMS err %.3f %s", centerX, centerY, radius, phaseShift, peakError, rmsError, units.getShortName());
        }

        @Override
        public Location getCameraOffset() {
            // Return the axis offset as the camera tool specific calibration offset.
            Logger.debug("[nozzleTipCalibration] getCameraOffset() returns: {}, {}", this.centerX, this.centerY);
            return new Location(this.units, this.centerX, this.centerY, 0., 0.);
        }
    }


    @Element(required = false)
    private CvPipeline pipeline = createDefaultPipeline();

    @Attribute(required = false)
    private int angleSubdivisions = 6;
    @Attribute(required = false)
    private int allowMisdetections = 0;
    @Attribute(required = false)
    private double angleStart = -180;
    @Attribute(required = false)
    private double angleStop = 180;
    // The excenter radius as a ratio of the camera minimum dimension.  
    @Attribute(required = false)
    private double excenterRatio = 0.25;

    @Attribute(required = false)
    private boolean enabled;

    private boolean calibrating;

    @Deprecated
    @Element(required = false)
    private RunoutCompensation runoutCompensation = null;
    @ElementMap(required = false)
    private Map<String, RunoutCompensation> runoutCompensationLookup = new HashMap<>();

    public enum RunoutCompensationAlgorithm {
        Model, ModelAffine, ModelNoOffset, ModelNoOffsetAffine, ModelCameraOffset, ModelCameraOffsetAffine, Table
    }

    public enum RecalibrationTrigger {
        NozzleTipChange, NozzleTipChangeInJob, MachineHome,  Manual
    }

    @Attribute(required = false)
    private ReferenceNozzleTipCalibration.RunoutCompensationAlgorithm runoutCompensationAlgorithm =
        RunoutCompensationAlgorithm.ModelCameraOffsetAffine;
    
    @Attribute(required = false)
    private double version = 1.0;

    @Attribute(required = false)
    private RecalibrationTrigger recalibrationTrigger = RecalibrationTrigger.NozzleTipChangeInJob;

    /**
     * TODO Left for backward compatibility. Unused. Can be removed after Feb 7, 2020.
     */
    @Deprecated
    @Attribute(required=false)
    private Double angleIncrement = null;

    @Commit
    public void commit() {
        angleIncrement = null;
        
        // OpenPNP Version update
        if (version < 2) {
            version = 2.0;
            // Force ModelCameraOffset calibration system.
            runoutCompensationAlgorithm = RunoutCompensationAlgorithm.ModelCameraOffset;
        }
        
        //Update from KASA to Affine transform technique
        if (version < 2.1) {
            version = 2.1; //bump version number so this update is only done once
            if (runoutCompensationAlgorithm == RunoutCompensationAlgorithm.Model) {
                runoutCompensationAlgorithm = RunoutCompensationAlgorithm.ModelAffine;
            } else if (runoutCompensationAlgorithm == RunoutCompensationAlgorithm.ModelNoOffset) {
                runoutCompensationAlgorithm = RunoutCompensationAlgorithm.ModelNoOffsetAffine;
            } else if (runoutCompensationAlgorithm == RunoutCompensationAlgorithm.ModelCameraOffset) {
                runoutCompensationAlgorithm = RunoutCompensationAlgorithm.ModelCameraOffsetAffine;
            }
        }
    }

    // Max allowed linear distance w.r.t. bottom camera for an offset measurement - measurements above threshold are removed from pipelines results 
    @Attribute(required = false)
    @Deprecated
    private Double offsetThreshold = 0.0;
    @Element(required = false)
    private Length offsetThresholdLength = new Length(0.5, LengthUnit.Millimeters);
    @Element(required = false)
    private Length calibrationZOffset = new Length(0.0, LengthUnit.Millimeters);

    public ReferenceNozzleTipCalibration.RunoutCompensationAlgorithm getRunoutCompensationAlgorithm() {
        return this.runoutCompensationAlgorithm;
    }

    public void setRunoutCompensationAlgorithm(ReferenceNozzleTipCalibration.RunoutCompensationAlgorithm runoutCompensationAlgorithm) {
        this.runoutCompensationAlgorithm = runoutCompensationAlgorithm;
    }

    public void calibrate(ReferenceNozzle nozzle, boolean homing, boolean calibrateCamera) throws Exception {
        if ( !isEnabled() ) {
            return;
        }

        if (!(homing || Configuration.get().getMachine().isHomed())) {
            throw new Exception("Machine not yet homed, nozzle tip calibration request aborted");
        }

        if (nozzle == null) {
            throw new Exception("Nozzle to nozzle tip mismatch.");
        }
        Camera camera = VisionUtils.getBottomVisionCamera();
        ReferenceCamera referenceCamera = null;
        if (camera instanceof ReferenceCamera) {
            referenceCamera = (ReferenceCamera)camera;
        }

        // This is our baseline location. Note: we do not apply the tool specific calibration offset here
        // as this would defy the very purpose of finding a new one here.  
        Location cameraLocation = camera.getLocation();
        Location measureBaseLocation = cameraLocation.derive(null, null, null, 0d)
                .add(new Location(this.calibrationZOffset.getUnits(), 0, 0, this.calibrationZOffset.getValue(), 0));

        try {
            calibrating = true;
            Location excenter = new Location(measureBaseLocation.getUnits());
            if (! calibrateCamera) {
                reset(nozzle);
            }
            else {
                if (! isCalibrated(nozzle)) {
                    throw new Exception("Calibrate the nozzle tip first."); 
                }
                if (referenceCamera == null) {
                    throw new Exception("For calibration the bottom vision camera must be a ReferenceCamera."); 
                }
                excenter = VisionUtils.getPixelCenterOffsets(camera, 
                        camera.getWidth()/2 + Math.min(camera.getWidth(), camera.getHeight())*excenterRatio, 
                        camera.getHeight()/2);
            }

            HashMap<String, Object> params = new HashMap<>();
            params.put("nozzle", nozzle);
            params.put("camera", camera);
            Configuration.get().getScripting().on("NozzleCalibration.Starting", params);

            // move nozzle to the camera location at the start angle - the nozzle must not necessarily be at the center
            MovableUtils.moveToLocationAtSafeZ(nozzle, measureBaseLocation.derive(null, null, null, angleStart));

            // determine the resulting angleIncrements
            double angleIncrement = ( angleStop - angleStart ) / this.angleSubdivisions;

            // determine the number of measurements to be made
            int angleSubdivisions = this.angleSubdivisions;
            if(Math.abs(angleStart + 360 - angleStop) < 0.1) {
                // we're measuring a full circle, the last measurement can be omitted
                angleSubdivisions--;
            }

            Logger.debug("[nozzleTipCalibration]starting measurement; angleStart: {}, angleStop: {}, angleIncrement: {}, angleSubdivisions: {}", 
                    angleStart, angleStop, angleIncrement, angleSubdivisions);

            // Capture nozzle tip positions and add them to a list. For these calcs the camera location is considered to be 0/0
            List<Location> nozzleTipMeasuredLocations = new ArrayList<>();
            List<Location> nozzleTipExpectedLocations = new ArrayList<>();
            int misdetects = 0;
            for (int i = 0; i <= angleSubdivisions; i++) {
                // calc the current measurement-angle
                double measureAngle = angleStart + (i * angleIncrement); 

                Logger.debug("[nozzleTipCalibration]i: {}, measureAngle: {}", i, measureAngle);

                // rotate nozzle to measurement angle
                Location measureLocation = measureBaseLocation
                        .derive(null, null, null, measureAngle)
                        .add(excenter.rotateXy(measureAngle));
                nozzle.moveTo(measureLocation);
                
                Location expectedLocation;
                if (!calibrateCamera) {
                    //For nozzle tip calibration, we artificially create an expected run-out of 1 mm and
                    //compare the actuals to these to compute the true run-out
                    expectedLocation = new Location(LengthUnit.Millimeters, 1.0, 0, 0, measureAngle).rotateXy(measureAngle);
                } else {
                    //For camera calibration, the expected location is just the nozzle location
                    expectedLocation = measureLocation;
                }

                // detect the nozzle tip
                Location offset = findCircle(measureLocation);
                if (offset != null) {
                    // for later usage in the algorithm, the measureAngle is stored to the offset location in millimeter unit 
                    offset = offset.derive(null, null, null, measureAngle);		

                    // add offset to array
                    nozzleTipMeasuredLocations.add(offset);
                    nozzleTipExpectedLocations.add(expectedLocation);

                    Logger.trace("[nozzleTipCalibration]measured offset: {}", offset);
                } else {
                    misdetects++;
                    if (misdetects > this.allowMisdetections) {
                        throw new Exception("Too many vision misdetects. Check pipeline and threshold.");
                    }
                }
            }

            if (nozzleTipMeasuredLocations.size() < Math.max(3, angleSubdivisions + 1 - this.allowMisdetections)) {
                throw new Exception("Not enough results from vision. Check pipeline and threshold."); 
            }

            Configuration.get().getScripting().on("NozzleCalibration.Finished", params);

            if (!calibrateCamera) {
                if (this.runoutCompensationAlgorithm == RunoutCompensationAlgorithm.Model) {
                    this.setRunoutCompensation(nozzle, new ModelBasedRunoutCompensation(nozzleTipMeasuredLocations));
                } else if (this.runoutCompensationAlgorithm == RunoutCompensationAlgorithm.ModelAffine) {
                    this.setRunoutCompensation(nozzle, new ModelBasedRunoutCompensation(nozzleTipMeasuredLocations, nozzleTipExpectedLocations));
                } else if (this.runoutCompensationAlgorithm == RunoutCompensationAlgorithm.ModelNoOffset) {
                    this.setRunoutCompensation(nozzle, new ModelBasedRunoutNoOffsetCompensation(nozzleTipMeasuredLocations));
                } else if (this.runoutCompensationAlgorithm == RunoutCompensationAlgorithm.ModelNoOffsetAffine) {
                    this.setRunoutCompensation(nozzle, new ModelBasedRunoutNoOffsetCompensation(nozzleTipMeasuredLocations, nozzleTipExpectedLocations));
                } else if (this.runoutCompensationAlgorithm == RunoutCompensationAlgorithm.ModelCameraOffset) {
                    this.setRunoutCompensation(nozzle, new ModelBasedRunoutCameraOffsetCompensation(nozzleTipMeasuredLocations));
                } else if (this.runoutCompensationAlgorithm == RunoutCompensationAlgorithm.ModelCameraOffsetAffine) {
                    this.setRunoutCompensation(nozzle, new ModelBasedRunoutCameraOffsetCompensation(nozzleTipMeasuredLocations, nozzleTipExpectedLocations));
                } else {
                    this.setRunoutCompensation(nozzle, new TableBasedRunoutCompensation(nozzleTipMeasuredLocations));
                }
            }
            else {
                if ((this.runoutCompensationAlgorithm == RunoutCompensationAlgorithm.ModelAffine) ||
                    (this.runoutCompensationAlgorithm == RunoutCompensationAlgorithm.ModelNoOffsetAffine) ||
                    (this.runoutCompensationAlgorithm == RunoutCompensationAlgorithm.ModelCameraOffsetAffine)) {
                    //This camera alignment stuff should be moved out of nozzle tip calibration
                    //and placed with the rest of the camera setup stuff
                    AffineTransform at = Utils2D.deriveAffineTransform(nozzleTipMeasuredLocations, nozzleTipExpectedLocations);
                    Utils2D.AffineInfo ai = Utils2D.affineInfo(at);
                    Logger.debug("[nozzleTipCalibration]bottom camera affine transform: " + ai);
                    Location newCameraPosition = new Location(LengthUnit.Millimeters, ai.xTranslation, ai.yTranslation, 0, 0);
                    newCameraPosition = referenceCamera.getHeadOffsets().derive(newCameraPosition, true, true, false, false);
                    Logger.debug("[nozzleTipCalibration]applying axis offset to bottom camera position: {} - {} = {}", 
                            referenceCamera.getHeadOffsets(),
                            referenceCamera.getHeadOffsets().subtract(newCameraPosition),
                            newCameraPosition);
                    referenceCamera.setHeadOffsets(newCameraPosition);
                    double newCameraAngle = referenceCamera.getRotation() - ai.rotationAngleDeg;
                    Logger.debug("[nozzleTipCalibration]applying angle offset to bottom camera rotation: {} - {} = {}", 
                            referenceCamera.getRotation(),
                            ai.rotationAngleDeg,
                            newCameraAngle);
                    referenceCamera.setRotation(newCameraAngle);
                } else {
                    ModelBasedRunoutCompensation cameraCompensation = new ModelBasedRunoutCompensation(nozzleTipMeasuredLocations);
                    Location newCameraPosition = referenceCamera.getHeadOffsets()
                            .subtract(cameraCompensation.getAxisOffset());
                    Logger.debug("[nozzleTipCalibration]applying axis offset to bottom camera position: {} - {} = {}", 
                            referenceCamera.getHeadOffsets(),
                            cameraCompensation.getAxisOffset(),
                            newCameraPosition);
                    referenceCamera.setHeadOffsets(newCameraPosition);
                    // Calculate and apply the new angle
                    double newCameraAngle = referenceCamera.getRotation() - cameraCompensation.getPhaseShift();
                    Logger.debug("[nozzleTipCalibration]applying angle offset to bottom camera rotation: {} - {} = {}", 
                            referenceCamera.getRotation(),
                            cameraCompensation.getPhaseShift(),
                            newCameraAngle);
                    referenceCamera.setRotation(newCameraAngle);
                }
            }
        }
        finally {
            // go to camera position (now offset-corrected). prevents the user from being irritated if it's not exactly centered
            nozzle.moveTo(camera.getLocation(nozzle).derive(null, null, measureBaseLocation.getZ(), angleStop));

            // after processing the nozzle returns to safe-z
            nozzle.moveToSafeZ();

            // setting to false in the very end to prevent endless calibration repetitions if calibration was not successful (pipeline not well or similar) and the nozzle is commanded afterwards somewhere else (where the calibration is asked for again ...)
            calibrating = false;
        }
    }

    public static void resetAllNozzleTips() {
        // Reset all nozzle tip calibrations, as they have become invalid due to some machine configuration change.
        for (NozzleTip nt: Configuration.get().getMachine().getNozzleTips()) {
            if (nt instanceof ReferenceNozzleTip) {
                ((ReferenceNozzleTip)nt).getCalibration().resetAll();
            }
        }
    }

    public void calibrate(ReferenceNozzle nozzle) throws Exception {
        calibrate(nozzle, false, false);
    }

    public void calibrateCamera(ReferenceNozzle nozzle) throws Exception {
        calibrate(nozzle, false, true);
    }

    /*
     * While calibrating the nozzle a circle was fitted to the runout path of the tip.
     * here the offset is reconstructed in XY-cartesian coordinates to be applied in moveTo commands.
     */
    public Location getCalibratedOffset(ReferenceNozzle nozzle, double angle) {
        if (!isEnabled() || !isCalibrated(nozzle)) {
            return new Location(LengthUnit.Millimeters, 0, 0, 0, 0);
        }

        return this.getRunoutCompensation(nozzle).getOffset(angle);

    }

    /*
     * The axis offset determined in runout calibration can be applied as a tool specific camera offset.
     */
    public Location getCalibratedCameraOffset(ReferenceNozzle nozzle, Camera camera) {
        try {
            if (camera == VisionUtils.getBottomVisionCamera()) {
                if (isEnabled() && isCalibrated(nozzle)) {
                    return this.getRunoutCompensation(nozzle).getCameraOffset();
                }
            } 
        }
        catch (Exception e) {
            // There is no bottom vision camera, that's fine.
        }

        return new Location(LengthUnit.Millimeters, 0, 0, 0, 0);
    }

    private Location findCircle(Location measureLocation) throws Exception {
        Camera camera = VisionUtils.getBottomVisionCamera();
        try (CvPipeline pipeline = getPipeline()) {
            pipeline.setProperty("camera", camera);
            Point maskCenter = VisionUtils.getLocationPixels(camera, measureLocation);
            pipeline.setProperty("MaskCircle.center", new org.opencv.core.Point(maskCenter.getX(), maskCenter.getY()));

            pipeline.process();
            List<Location> locations = new ArrayList<>();

            String stageName = VisionUtils.PIPELINE_RESULTS_NAME;
            Result pipelineResult = pipeline.getResult(stageName);
            if (pipelineResult == null) {
                throw new Exception(String.format("There should be a \"%s\" stage in the pipeline.", stageName));
            }

            Object results = pipelineResult.model;

            if (results instanceof Exception) {
                throw (Exception)results;
            }

            //show result from pipeline in camera view
            MainFrame.get().getCameraViews().getCameraView(camera).showFilteredImage(
                    OpenCvUtils.toBufferedImage(pipeline.getWorkingImage()), 1000);

            // add all results from pipeline to a Location-list post processing
            if (results instanceof List) {
                // are there any results from the pipeline?
                if (0==((List) results).size()) {
                    // Don't throw new Exception("No results from vision. Check pipeline.");      
                    // Instead the number of obtained fixes is evaluated later.
                    return null;
                }
                for (Object result : (List) results) {
                    if ((result) instanceof Result.Circle) {
                        Result.Circle circle = ((Result.Circle) result);
                        locations.add(VisionUtils.getPixelCenterOffsets(camera, circle.x, circle.y));
                    }
                    else if ((result) instanceof KeyPoint) {
                        KeyPoint keyPoint = ((KeyPoint) result);
                        locations.add(VisionUtils.getPixelCenterOffsets(camera, keyPoint.pt.x, keyPoint.pt.y));
                    }
                    else if ((result) instanceof RotatedRect) {
                        RotatedRect rect = ((RotatedRect) result);
                        locations.add(VisionUtils.getPixelCenterOffsets(camera, rect.center.x, rect.center.y));
                    }
                    else {
                        throw new Exception("Unrecognized result " + result);
                    }
                }
            }

            // remove all results that are above threshold
            Iterator<Location> locationsIterator = locations.iterator();
            while (locationsIterator.hasNext()) {
                Location location = locationsIterator.next();
                Location measureLocationRelative = measureLocation.convertToUnits(location.getUnits()).
                        subtract(camera.getLocation());
                double threshold = offsetThresholdLength.convertToUnits(location.getUnits()).getValue();
                if (location.getLinearDistanceTo(measureLocationRelative) > threshold) {
                    locationsIterator.remove();
                    Logger.trace("[nozzleTipCalibration]Removed offset location {} from results; measured distance {} exceeds offsetThresholdLength {}", location, location.getLinearDistanceTo(0., 0.), threshold); 
                }
            }

            // check for a valid resultset
            if (locations.size() == 0) {
                // Don't throw new Exception("No valid results from pipeline within threshold");
                // Instead the number of obtained fixes is evaluated later.
                return null;
            } else if (locations.size() > 1) {
                // Don't throw an exception here either. Since we've gotten more results than expected we can't be
                // sure which, if any, are the correct result so just discard them all and log an info message.
                Logger.info("[nozzleTipCalibration]Got more than one result from pipeline. For best performance tweak pipeline to return exactly one result only. Discarding all locations (since it is unknown which may be correct) from the following set: " + locations);
                return null;
            }

            // finally return the location at index (0) which is either a) the only one or b) the one best matching the nozzle tip
            return locations.get(0);
        }
        finally {
            pipeline.setProperty("MaskCircle.center", null);
        }
    }

    public static CvPipeline createDefaultPipeline() {
        try {
            String xml = IOUtils.toString(ReferenceNozzleTip.class
                    .getResource("ReferenceNozzleTip-Calibration-DefaultPipeline.xml"));
            return new CvPipeline(xml);
        }
        catch (Exception e) {
            throw new Error(e);
        }
    }

    public void resetPipeline() {
        pipeline = createDefaultPipeline();
    }

    public void reset(ReferenceNozzle nozzle) {
        // reset the combined nozzle tip + nozzle runout compensation for the nozzle we are currently attached to 
        setRunoutCompensation(nozzle, null);
        // deprecated
        runoutCompensation = null;
    }

    public void resetAll() {
        // reset the nozzle tip + nozzle runout for all the nozzles this tip was attached to
        // i.e. just wipe the whole lookup table
        runoutCompensationLookup.clear();
        // inform UI about changed information
        firePropertyChange("calibrationInformation", null, null);
        // deprecated
        runoutCompensation = null;
    }

    private RunoutCompensation getRunoutCompensation(ReferenceNozzle nozzle) {
        // get the combined nozzle tip + nozzle runout compensation for the nozzle we are currently attached to 
        if (nozzle != null) {
            return runoutCompensationLookup.get(nozzle.getId());
        }
        return null;
    }

    private void setRunoutCompensation(ReferenceNozzle nozzle, RunoutCompensation runoutCompensation) {
        // set the combined nozzle tip + nozzle runout compensation for the nozzle we are currently attached to 
        if (nozzle != null) {
            if (runoutCompensation == null) {
                runoutCompensationLookup.remove(nozzle.getId());
            }
            else {
                runoutCompensationLookup.put(nozzle.getId(), runoutCompensation);
            }
                
            // inform UI about changed information
            firePropertyChange("calibrationInformation", null, null);
        }
        // deprecated
        runoutCompensation = null;
    }

    public String getCalibrationInformation(ReferenceNozzle nozzle) { 
        return getRunoutCompensation(nozzle).toString();
    }

    public boolean isCalibrated(ReferenceNozzle nozzle) {
        return getRunoutCompensation(nozzle) != null;
    }

    public boolean isCalibrating() {
        return calibrating;
    }

    public int getAngleSubdivisions() {
        return angleSubdivisions;
    }

    public void setAngleSubdivisions(int angleSubdivisions) {
        this.angleSubdivisions = angleSubdivisions;
    }

    public int getAllowMisdetections() {
        return allowMisdetections;
    }

    public void setAllowMisdetections(int allowMisdetections) {
        this.allowMisdetections = allowMisdetections;
    }

    @Deprecated
    public double getOffsetThreshold() {
        return getOffsetThresholdLength().convertToUnits(LengthUnit.Millimeters).getValue();
    }

    @Deprecated
    public void setOffsetThreshold(double offsetThreshold) {
        this.setOffsetThresholdLength(new Length(offsetThreshold, LengthUnit.Millimeters));
    }

    public Length getOffsetThresholdLength() {
        // Migrate old unit-less setting.
        if (this.offsetThreshold > 0.) {
            offsetThresholdLength = new Length(this.offsetThreshold, LengthUnit.Millimeters);
            this.offsetThreshold = 0.;
        }
        return offsetThresholdLength;
    }

    public void setOffsetThresholdLength(Length offsetThresholdLength) {
        Length oldValue = this.offsetThresholdLength;
        this.offsetThresholdLength = offsetThresholdLength;
        firePropertyChange("offsetThresholdLength", oldValue, offsetThresholdLength);
    }

    public Length getCalibrationZOffset() {
        return calibrationZOffset;
    }

    public void setCalibrationZOffset(Length calibrationZOffset) {
        this.calibrationZOffset = calibrationZOffset;
    }

    public RecalibrationTrigger getRecalibrationTrigger() {
        return recalibrationTrigger;
    }

    public void setRecalibrationTrigger(RecalibrationTrigger recalibrationTrigger) {
        this.recalibrationTrigger = recalibrationTrigger;
    }

    public boolean isRecalibrateOnNozzleTipChangeInJobNeeded(ReferenceNozzle nozzle) {
        return recalibrationTrigger == RecalibrationTrigger.NozzleTipChangeInJob;
    }

    public boolean isRecalibrateOnNozzleTipChangeNeeded(ReferenceNozzle nozzle) {
        return (recalibrationTrigger == RecalibrationTrigger.NozzleTipChange)
                || (recalibrationTrigger == RecalibrationTrigger.MachineHome && !isCalibrated(nozzle));
    }

    public boolean isRecalibrateOnHomeNeeded(ReferenceNozzle nozzle) {
        return recalibrationTrigger == RecalibrationTrigger.NozzleTipChange
                ||  recalibrationTrigger == RecalibrationTrigger.MachineHome;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public CvPipeline getPipeline() throws Exception {
        pipeline.setProperty("camera", VisionUtils.getBottomVisionCamera());
        return pipeline;
    }

    public void setPipeline(CvPipeline calibrationPipeline) {
        this.pipeline = calibrationPipeline;
    }
}