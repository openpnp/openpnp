package org.openpnp.machine.reference;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.openpnp.gui.MainFrame;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Point;
import org.openpnp.spi.Camera;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.NanosecondTime;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.Utils2D;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.FluentCv;
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
    @Attribute(required = false)
    private boolean failHoming = true;

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

    public enum BackgroundCalibrationMethod {
        None, Brightness, BrightnessAndKeyColor
    }

    @Attribute(required = false)
    private BackgroundCalibrationMethod backgroundCalibrationMethod = BackgroundCalibrationMethod.None;

    @Element(required = false)
    private Length minimumDetailSize = new Length(0.1, LengthUnit.Millimeters);

    @Attribute(required = false)
    private int backgroundMinHue; 

    @Attribute(required = false)
    private int backgroundMaxHue; 

    @Attribute(required = false)
    private int backgroundTolHue = 8; 

    @Attribute(required = false)
    private int backgroundMinSaturation; 

    @Attribute(required = false)
    private int backgroundMaxSaturation; 

    @Attribute(required = false)
    private int backgroundTolSaturation = 8; 

    @Attribute(required = false)
    private int backgroundMinValue; 

    @Attribute(required = false)
    private int backgroundMaxValue; 

    @Attribute(required = false)
    private int backgroundTolValue = 8; 

    @Attribute(required = false)
    private String backgroundDiagnostics; 


    /**
     * Minimum brightness (value, range 0..255) at which to consider hue masking.
     */
    @Attribute(required = false)
    private int minBackgroundMaskValue = 32;  

    /**
     * Maximum brightness (value, range 0..255) at which to consider hue masking.
     */
    @Attribute(required = false)
    private int maxBackgroundMaskValue = 128;

    /**
     * A key color should always be quite vivid, the worst saturation is set here.
     */
    @Attribute(required = false)
    private int backgroundWorstSaturation = 255/4;

    /**
     * A key color should be quite consistent, the worst hue span (max - min) is set here. 
     */
    @Attribute(required = false)
    private int backgroundWorstHueSpan = 255/6; // The hue span should not be larger than 60°.

    /**
     * A background (minus the key color) should be quite dark. 
     */
    @Attribute(required = false)
    private int backgroundWorstValue = 255/2; 

    private List<byte []> backgroundImages;

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
    /**
     * Vision detection search distance margin, relative to the threshold: we want to detect nozzle tips outside the threshold and
     * get positive falses rather than false positives.  
     */
    @Attribute(required = false)
    private double detectionThresholdMargin = 0.4;
    @Element(required = false)
    private Length calibrationZOffset = new Length(0.0, LengthUnit.Millimeters);
    @Element(required = false)
    private Length calibrationTipDiameter = new Length(0.0, LengthUnit.Millimeters);

    private List<BufferedImage> backgroundCalibrationImages;

    private int backgroundImageRows;

    private int backgroundImageCols;

    private int backgroundImageChannels;

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

        if (nozzle.getPart()!= null) {
            throw new Exception("Cannot calibrate nozzle tip with part on nozzle "+nozzle.getName()+".");
        }
        // Make sure to set start and end rotation to the limits.
        double [] rotationModeLimits = nozzle.getRotationModeLimits();
        angleStart = rotationModeLimits[0];
        angleStop = rotationModeLimits[1];
        // Make sure no rotation mode offset is currently applied.
        nozzle.setRotationModeOffset(null);

        Camera camera = VisionUtils.getBottomVisionCamera();
        ReferenceCamera referenceCamera = null;
        if (camera instanceof ReferenceCamera) {
            referenceCamera = (ReferenceCamera)camera;
        }

        // Note: we do not apply the tool specific calibration offset here
        // as this would defy the very purpose of finding a new one here. Pass null.  
        Location measureBaseLocation = getCalibrationLocation(camera, null);

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
                Location offset = findCircle(nozzle, measureLocation, calibrateCamera);
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
                        throw new Exception(
                                "Nozzle tip calibration: too many vision misdetects. Check pipeline and threshold.");
                    }
                }
            }

            if (nozzleTipMeasuredLocations.size() < Math.max(3, angleSubdivisions + 1 - this.allowMisdetections)) {
                throw new Exception(
                        "Nozzle tip calibration: not enough results from vision. Check pipeline and threshold.");
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

            // Finish the background calibration, if images were successfully collected.  
            finishBackgroundCalibration(referenceCamera, nozzle);

            // after processing the nozzle returns to safe-z
            nozzle.moveToSafeZ();
            MovableUtils.fireTargetedUserAction(nozzle);

            // setting to false in the very end to prevent endless calibration repetitions if calibration was not successful (pipeline not well or similar) and the nozzle is commanded afterwards somewhere else (where the calibration is asked for again ...)
            calibrating = false;
        }
    }

    public Location getCalibrationLocation(Camera camera, HeadMountable nozzle) {
        // This is our baseline location. 
        Location cameraLocation = camera.getLocation(nozzle);
        Location measureBaseLocation = cameraLocation.derive(null, null, null, 0d)
                .add(new Location(this.calibrationZOffset.getUnits(), 0, 0, this.calibrationZOffset.getValue(), 0));
        return measureBaseLocation;
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

    private Location findCircle(ReferenceNozzle nozzle, Location measureLocation, boolean calibrateCamera) throws Exception {
        Camera camera = VisionUtils.getBottomVisionCamera();
        try (CvPipeline pipeline = getPipeline(camera, nozzle, measureLocation)) {
            
            pipeline.process();
            List<Location> locations = new ArrayList<>();

            String stageName = VisionUtils.PIPELINE_RESULTS_NAME;
            List results = pipeline.getExpectedResult(stageName).getExpectedModel(List.class);

            //show result from pipeline in camera view, but only if GUI is present (not so in UnitTests).
            MainFrame mainFrame = MainFrame.get();
            if (mainFrame != null) {
                mainFrame.getCameraViews().getCameraView(camera).showFilteredImage(
                        OpenCvUtils.toBufferedImage(pipeline.getWorkingImage()), 1000);
            }

            // add all results from pipeline to a Location-list post processing
            // are there any results from the pipeline?
            if (0==results.size()) {
                // Don't throw new Exception("No results from vision. Check pipeline.");      
                // Instead the number of obtained fixes is evaluated later.
                return null;
            }
            for (Object result : results) {
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
                    Logger.error("[nozzleTipCalibration] Unrecognized result " + result);
                    throw new Exception("Unrecognized result " + result);
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

            if (!calibrateCamera) {
                addBackgroundImage(camera, nozzle, pipeline.getLastCapturedImage(), locations.get(0));
            }

            // finally return the location at index (0) which is either a) the only one or b) the one best matching the nozzle tip
            return locations.get(0);
        }
        finally {
            pipeline.setProperty("MaskCircle.center", null);
        }
    }

    private void addBackgroundImage(Camera camera, ReferenceNozzle nozzle, BufferedImage bufferedImage, Location location) {
        if (bufferedImage != null
                && getBackgroundCalibrationMethod() != BackgroundCalibrationMethod.None) {
            Mat image = OpenCvUtils.toMat(bufferedImage);
            ReferenceNozzleTip nozzleTip = nozzle.getCalibrationNozzleTip();
            if (nozzleTip != null) {
                // Blot out the nozzle tip center part.
                Point center = VisionUtils.getLocationPixels(camera, location.add(camera.getLocation()));
                org.opencv.core.Point center2 = new org.opencv.core.Point(center.x, center.y);
                Length minPartDiameter = nozzleTip.getMinPartDiameterWithTolerance();
                if (minPartDiameter.compareTo(getCalibrationTipDiameter()) < 0) {
                    minPartDiameter = getCalibrationTipDiameter();
                }
                int radius = (int)Math.ceil(minPartDiameter
                        .divide(camera.getUnitsPerPixel().getLengthX())
                        *0.5);
                Imgproc.circle(image, center2, 
                        radius, 
                        FluentCv.colorToScalar(new Color(0, 0, 0)),
                        Imgproc.FILLED, 8, 0);
            }
            // Blur.
            int kernelSize = ((int)getMinimumDetailSize()
                    .divide(camera.getUnitsPerPixel().getLengthX()))|1;
            Imgproc.GaussianBlur(image, image, new Size(kernelSize, kernelSize), 0);
            if (getBackgroundCalibrationMethod() == BackgroundCalibrationMethod.BrightnessAndKeyColor) {
                Imgproc.cvtColor(image, image, FluentCv.ColorCode.Bgr2HsvFull.getCode());
            }
            else {
                Imgproc.cvtColor(image, image, FluentCv.ColorCode.Bgr2Gray.getCode());
            }
            backgroundImageRows = image.rows();
            backgroundImageCols = image.cols();
            backgroundImageChannels = image.channels();
            byte data[] = new byte[backgroundImageRows*backgroundImageCols*backgroundImageChannels];
            image.get(0, 0, data);
            image.release();
            backgroundImages.add(data);
        }
    }

    protected synchronized void finishBackgroundCalibration(Camera camera, ReferenceNozzle nozzle) throws Exception {
        try{
            backgroundCalibrationImages = new ArrayList<BufferedImage>();
            ReferenceNozzleTip nozzleTip = nozzle.getCalibrationNozzleTip();
            if (nozzleTip != null 
                    && backgroundImages.size() > 3) {
                double t0 = NanosecondTime.getRuntimeSeconds();
                double maskDiameterPixels = nozzleTip.getMaxPartDiameterWithTolerance()
                        .divide(camera.getUnitsPerPixel().getLengthX())*0.5;
                int maskSq = (int)Math.pow(maskDiameterPixels, 2);
                int rows = backgroundImageRows, cols = backgroundImageCols, ch = backgroundImageChannels;
                if (getBackgroundCalibrationMethod() == BackgroundCalibrationMethod.BrightnessAndKeyColor) {
                    int histogramHueValue[] = new int[256*256];
                    int histogramMonochrome[] = new int[256];
                    int minSaturationAtValue[] = new int[256];
                    int maxSaturationAtValue[] = new int[256];
                    int maxValue = 0;
                    Arrays.fill(minSaturationAtValue, 255);
                    for (byte data[] : backgroundImages) {
                        for (int y = 0; y < rows; y++) {
                            for (int x = 0, idx = y*cols*ch; x < cols; x++, idx += ch) {
                                int dx = x - cols/2;
                                int dy = y - rows/2;
                                int rSq = dx*dx + dy*dy;
                                if (rSq < maskSq) {
                                    int hue = Byte.toUnsignedInt(data[idx+0]);
                                    int saturation = Byte.toUnsignedInt(data[idx+1]);
                                    int value = Byte.toUnsignedInt(data[idx+2]);
                                    if (saturation >= backgroundWorstSaturation) { 
                                        histogramHueValue[hue*256 + value]++;
                                        minSaturationAtValue[value] = Math.min(minSaturationAtValue[value], saturation);
                                        maxSaturationAtValue[value] = Math.max(maxSaturationAtValue[value], saturation);
                                    }
                                    else {
                                        histogramMonochrome[value]++;
                                    }
                                }
                            }
                        }
                    }
                    // Sum histogram at value and larger.
                    for (int hue = 0; hue < 256; hue++) {
                        for (int value = 1; value < 256; value++) {
                            int count = histogramHueValue[hue*256 + value]; 
                            if (count > 0) {
                                for (int valueSum = 0; valueSum < value; valueSum++) {
                                    histogramHueValue[hue*256 + valueSum] += count; 
                                    minSaturationAtValue[valueSum] = Math.min(minSaturationAtValue[valueSum], minSaturationAtValue[value]);
                                    maxSaturationAtValue[valueSum] = Math.max(maxSaturationAtValue[valueSum], maxSaturationAtValue[value]);
                                }
                                maxValue = Math.max(maxValue, value);
                            }
                        }
                    }
                    // Sum min, max saturation at value and larger.
                    for (int value = 1; value < 256; value++) {
                        for (int valueSum = 0; valueSum < value; valueSum++) {
                            minSaturationAtValue[valueSum] = Math.min(minSaturationAtValue[valueSum], minSaturationAtValue[value]);
                            maxSaturationAtValue[valueSum] = Math.max(maxSaturationAtValue[valueSum], maxSaturationAtValue[value]);
                            histogramMonochrome[valueSum] += histogramMonochrome[value];
                        }
                    }

                    // Find the best monochrome cutoff + key color box.
                    int bestMasked = Integer.MAX_VALUE;
                    int bestMinHue = 0;
                    int bestMaxHue = 0;
                    int bestMinSaturation = 0;
                    int bestMaxSaturation = 0;
                    int bestMaxValue = 0;
                    int bestMinValue = 0;
                    for (int minValue = minBackgroundMaskValue; 
                            minValue <= maxBackgroundMaskValue; 
                            minValue++) {
                        if (histogramMonochrome[minValue] == 0 || minValue == maxBackgroundMaskValue) {
                            // Find the largest gap in hue data (it wraps around).
                            int largestGap = 0;
                            int minHue = 0;
                            int maxHue = 0;
                            for (int hue = 0; hue < 256; hue++) {
                                int gap = 0;
                                for (; histogramHueValue[((hue+gap)&0xFF)*256 + minValue] == 0 && gap < 255; gap++) {}
                                if (gap > largestGap) {
                                    largestGap = gap;
                                    maxHue = (hue - 1)&0xFF;
                                    minHue = (hue + gap)&0xFF;
                                }
                            }
                            int minSaturation = minSaturationAtValue[minValue];
                            int maxSaturation = maxSaturationAtValue[minValue];
                            // Calculate the masked color space.
                            int brightnessMasked = minValue
                                    *(256-backgroundWorstSaturation)
                                    *(backgroundWorstHueSpan+16);
                            int hsvMasked = 
                                    ((maxHue + 1 - minHue)&0xFF)
                                    *(maxValue + 1 - minValue)
                                    *(maxSaturation + 1 - minSaturation);
                            int totalMasked = brightnessMasked + hsvMasked;
                            if (totalMasked < bestMasked) {
                                bestMasked = totalMasked;
                                bestMinValue = minValue;
                                bestMinHue = minHue;
                                bestMaxHue = maxHue;
                                bestMinSaturation = minSaturation;
                                bestMaxSaturation = maxSaturation;
                                bestMaxValue = maxValue;
                            }
                        }
                    }
                    int hueSpan = (bestMaxHue-bestMinHue)&0xFF;
                    if (hueSpan > backgroundWorstHueSpan) {
                        // Inconsistent color, take best span.
                        long bestHueSpanSum = 0;
                        int radius = backgroundWorstHueSpan/2;
                        for (int hue = 0; hue < 256; hue++) {
                            long sum = 0;
                            for (int span = 0; span <= backgroundWorstHueSpan; span++) {
                                int d = span - radius;
                                int weight = radius*radius - d*d;
                                sum += weight*histogramHueValue[((hue+span)&0xFF)*256 + bestMinValue];
                            }
                            if (sum > bestHueSpanSum) {
                                bestHueSpanSum = sum;
                                bestMinHue = hue;
                                bestMaxHue = (hue+backgroundWorstHueSpan)&0xFF;
                            }
                        }
                    }
                    // Set the result.
                    setBackgroundMinHue(bestMinHue);
                    setBackgroundMaxHue(bestMaxHue);
                    setBackgroundMinSaturation(bestMinSaturation);
                    setBackgroundMaxSaturation(bestMaxSaturation);
                    setBackgroundMinValue(bestMinValue);
                    setBackgroundMaxValue(bestMaxValue);

                    // Create the diagnostic images.
                    int avgHue = (bestMinHue > bestMaxHue ? (bestMinHue + bestMaxHue)/2 : ((255 + bestMinHue + bestMaxHue)/2) & 0xFF);
                    int signalColor = Color.getHSBColor(avgHue/255.0f, 1.0f, 1.0f).getRGB();
                    for (byte data[] : backgroundImages) {
                        int imagePixels[] = new int[cols*rows];
                        int problemPixels[] = new int[cols*rows];
                        boolean hasProblems = false;
                        for (int y = 0; y < rows; y++) {
                            for (int x = 0, idx = y*cols*ch; x < cols; x++, idx += ch) {
                                int dx = x - cols/2;
                                int dy = y - rows/2;
                                int rSq = dx*dx + dy*dy;
                                if (rSq < maskSq) {
                                    int hue = Byte.toUnsignedInt(data[idx+0]);
                                    int saturation = Byte.toUnsignedInt(data[idx+1]);
                                    int value = Byte.toUnsignedInt(data[idx+2]);
                                    Color color = Color.getHSBColor(hue/255.0f, saturation/255.0f, value/255.0f);
                                    int rgb = color.getRGB();
                                    if (value >= bestMinValue
                                            && (value > bestMaxValue
                                            || saturation < bestMinSaturation
                                            || saturation > bestMaxSaturation
                                            || (bestMinHue < bestMaxHue ? 
                                                    hue > bestMaxHue || hue < bestMinHue :
                                                        hue > bestMaxHue && hue < bestMinHue))) {
                                        // outside mask
                                        imagePixels[y*cols + x] = rgb;
                                        problemPixels[y*cols + x] = signalColor;
                                        hasProblems = true;
                                    }
                                    else {
                                        imagePixels[y*cols + x] = rgb;
                                        problemPixels[y*cols + x] = rgb;
                                    }
                                }
                            }
                        }
                        if (hasProblems) {
                            BufferedImage backgroundCalibrationImage = new BufferedImage(cols, rows, BufferedImage.TYPE_INT_ARGB);
                            backgroundCalibrationImage.setRGB(0, 0, cols, rows, imagePixels, 0, cols);
                            backgroundCalibrationImages.add(backgroundCalibrationImage);
                            backgroundCalibrationImage = new BufferedImage(cols, rows, BufferedImage.TYPE_INT_ARGB);
                            backgroundCalibrationImage.setRGB(0, 0, cols, rows, problemPixels, 0, cols);
                            backgroundCalibrationImages.add(backgroundCalibrationImage);
                        }
                    }

                    // Diagnostics.
                    StringBuilder report = new StringBuilder();
                    report.append("<html>");
                    report.append("Non-color-keyed background elements are ");
                    if (bestMinValue > backgroundWorstValue) {
                        report.append("too bright.<br/>"
                                + "Try to eliminate highlights and reflections.<br/>"
                                + "Use a shade behind the nozzle.<br/>"
                                + "Renew the blackening of dark parts of the nozzle tip.<br/>"
                                + "Clean the nozzle tip. If it is shiny, make it dull.<br/>"
                                + "Eliminate light sources that reflect on the nozzle tip.");
                    }
                    else if (bestMinValue > backgroundWorstValue/2) {
                        report.append("sufficiently dark. <br/>");
                    }
                    else if (bestMinValue <= minBackgroundMaskValue) {
                        report.append("possibly too dark. <br/>"
                                + "Check camera exposure.<br/>");
                    }
                    else {
                        report.append("quite dark. Perfect!<br/>");
                    }
                    if (bestMinValue <= backgroundWorstValue) {
                        report.append("<hr/>");
                        report.append("The key color is ");
                        if (hueSpan > backgroundWorstHueSpan) {
                            report.append("not consistent enough.<br/>"
                                    + (bestMinValue <= backgroundWorstValue ? 
                                            "Check camera white balance (see the Wiki).<br/>" : "")
                                    + "Clean the nozzle tip. If it is shiny, make it dull.<br/>"
                                    + "Eliminate light sources that reflect on the nozzle tip..<br/>");
                        }
                        else if (hueSpan > backgroundWorstHueSpan/2) {
                            report.append("sufficiently consistent.<br/>");
                        }
                        else {
                            report.append("very consistent. Perfect!<br/>");
                        }
                        if (hueSpan <= backgroundWorstHueSpan) {
                            report.append("<hr/>");
                            report.append("The key color is ");
                            if (bestMinSaturation < backgroundWorstSaturation) {
                                report.append("not vivid enough.<br/>"
                                        + (bestMinValue <= backgroundWorstValue ? 
                                                "Check camera white balance (see the Wiki).<br/>" : "")
                                        + "Clean the nozzle tip. If it is shiny, make it dull.<br/>"
                                        + "Eliminate light sources that reflect on the nozzle tip.<br/>");
                            }
                            else if (bestMinSaturation < (255+backgroundWorstSaturation)/2) {
                                report.append("sufficiently saturated.<br/>");
                            }
                            else {
                                report.append("very vivid. Perfect!<br/>");
                            }
                        }
                    }
                    report.append("</html>");
                    setBackgroundDiagnostics(report.toString());
                }
                else if (getBackgroundCalibrationMethod() == BackgroundCalibrationMethod.Brightness) {
                    // Simple grayscale.
                    int bestMinValue = 255; 
                    int bestMaxValue = 0;
                    int signalColor = new Color(255, 0, 255).getRGB();
                    for (byte data[] : backgroundImages) {
                        int imagePixels[] = new int[cols*rows];
                        int problemPixels[] = new int[cols*rows];
                        boolean hasProblems = false;
                        for (int y = 0; y < rows; y++) {
                            for (int x = 0, idx = y*cols*ch; x < cols; x++, idx += ch) {
                                int dx = x - cols/2;
                                int dy = y - rows/2;
                                int rSq = dx*dx + dy*dy;
                                if (rSq < maskSq) {
                                    int value = Byte.toUnsignedInt(data[idx]);
                                    if (value > bestMaxValue) {
                                        bestMaxValue = value;
                                    }
                                    if (value < bestMinValue && value > 0) {
                                        // Exclude 0 from black point as it could be an undefined pixel 
                                        // (from image transforms or advanced camera calibration). 
                                        bestMinValue = value;
                                    }
                                    int rgb = new Color(value, value, value).getRGB();
                                    if (value > backgroundWorstValue) {
                                        // outside mask
                                        imagePixels[y*cols + x] = rgb;
                                        problemPixels[y*cols + x] = signalColor;
                                        hasProblems = true;
                                    }
                                    else {
                                        imagePixels[y*cols + x] = rgb;
                                        problemPixels[y*cols + x] = rgb;
                                    }
                                }
                            }
                        }
                        if (hasProblems) {
                            BufferedImage backgroundCalibrationImage = new BufferedImage(cols, rows, BufferedImage.TYPE_INT_ARGB);
                            backgroundCalibrationImage.setRGB(0, 0, cols, rows, imagePixels, 0, cols);
                            backgroundCalibrationImages.add(backgroundCalibrationImage);
                            backgroundCalibrationImage = new BufferedImage(cols, rows, BufferedImage.TYPE_INT_ARGB);
                            backgroundCalibrationImage.setRGB(0, 0, cols, rows, problemPixels, 0, cols);
                            backgroundCalibrationImages.add(backgroundCalibrationImage);
                        }
                    }
                    if (bestMinValue == 1) {
                        // black point goes all the way down.
                        bestMinValue = 0;
                    }
                    // set the results
                    setBackgroundMinHue(0);
                    setBackgroundMaxHue(255);
                    setBackgroundMinSaturation(0);
                    setBackgroundMaxSaturation(255);
                    setBackgroundMinValue(bestMinValue);
                    setBackgroundMaxValue(bestMaxValue);
                    // Judge the result.
                    // Diagnostics.
                    StringBuilder report = new StringBuilder();
                    report.append("<html>");
                    report.append("Background elements are ");
                    if (bestMaxValue > backgroundWorstValue) {
                        report.append("too bright.<br/>"
                                + "Try to eliminate highlights and reflections.<br/>"
                                + "Use a shade behind the nozzle.<br/>"
                                + "Renew the blackening of dark parts of the nozzle tip.<br/>"
                                + "Clean the nozzle tip. If it is shiny, make it dull.<br/>"
                                + "Eliminate light sources that reflect on the nozzle tip.");
                    }
                    else if (bestMaxValue > backgroundWorstValue/2) {
                        report.append("sufficiently dark. <br/>");
                    }
                    else if (bestMaxValue <= minBackgroundMaskValue) {
                        report.append("possibly too dark. <br/>"
                                + "Check camera exposure.<br/>");
                    }
                    else {
                        report.append("quite dark. Perfect!<br/>");
                    }
                    report.append("</html>");
                    setBackgroundDiagnostics(report.toString());
                }
                Logger.debug("Background calibration computation time: "+(NanosecondTime.getRuntimeSeconds() - t0)+"s");
            }
        }
        finally {
            backgroundImages = null;
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
        // Start a new set of background images.
        backgroundImages = new ArrayList<>();
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

    public Length getCalibrationTipDiameter() {
        return calibrationTipDiameter;
    }

    public void setCalibrationTipDiameter(Length calibrationTipDiameter) {
        this.calibrationTipDiameter = calibrationTipDiameter;
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

    public boolean isFailHoming() {
        return failHoming;
    }

    public void setFailHoming(boolean failHoming) {
        this.failHoming = failHoming;
    }

    public BackgroundCalibrationMethod getBackgroundCalibrationMethod() {
        return backgroundCalibrationMethod;
    }

    public void setBackgroundCalibrationMethod(
            BackgroundCalibrationMethod backgroundCalibrationMethod) {
        this.backgroundCalibrationMethod = backgroundCalibrationMethod;
    }

    public Length getMinimumDetailSize() {
        return minimumDetailSize;
    }

    public void setMinimumDetailSize(Length minimumDetailSize) {
        Object oldValue = this.minimumDetailSize;
        this.minimumDetailSize = minimumDetailSize;
        firePropertyChange("minimumDetailSize", oldValue, minimumDetailSize);
    }

    public int getBackgroundMinHue() {
        return backgroundMinHue;
    }

    public void setBackgroundMinHue(int backgroundMinHue) {
        Object oldValue = this.backgroundMinHue;
        this.backgroundMinHue = backgroundMinHue;
        firePropertyChange("backgroundMinHue", oldValue, backgroundMinHue);
    }

    public int getBackgroundMaxHue() {
        return backgroundMaxHue;
    }

    public void setBackgroundMaxHue(int backgroundMaxHue) {
        Object oldValue = this.backgroundMaxHue;
        this.backgroundMaxHue = backgroundMaxHue;
        firePropertyChange("backgroundMaxHue", oldValue, backgroundMaxHue);
    }

    public int getBackgroundTolHue() {
        return backgroundTolHue;
    }

    public void setBackgroundTolHue(int backgroundTolHue) {
        Object oldValue = this.backgroundTolHue;
        this.backgroundTolHue = backgroundTolHue;
        firePropertyChange("backgroundTolHue", oldValue, backgroundTolHue);
    }

    public int getBackgroundMinSaturation() {
        return backgroundMinSaturation;
    }

    public void setBackgroundMinSaturation(int backgroundMinSaturation) {
        Object oldValue = this.backgroundMinSaturation;
        this.backgroundMinSaturation = backgroundMinSaturation;
        firePropertyChange("backgroundMinSaturation", oldValue, backgroundMinSaturation);
    }

    public int getBackgroundMaxSaturation() {
        return backgroundMaxSaturation;
    }

    public void setBackgroundMaxSaturation(int backgroundMaxSaturation) {
        Object oldValue = this.backgroundMaxSaturation;
        this.backgroundMaxSaturation = backgroundMaxSaturation;
        firePropertyChange("backgroundMaxSaturation", oldValue, backgroundMaxSaturation);
    }


    public int getBackgroundTolSaturation() {
        return backgroundTolSaturation;
    }

    public void setBackgroundTolSaturation(int backgroundTolSaturation) {
        Object oldValue = this.backgroundTolSaturation;
        this.backgroundTolSaturation = backgroundTolSaturation;
        firePropertyChange("backgroundTolSaturation", oldValue, backgroundTolSaturation);
    }

    public int getBackgroundMinValue() {
        return backgroundMinValue;
    }

    public void setBackgroundMinValue(int backgroundMinValue) {
        Object oldValue = this.backgroundMinValue;
        this.backgroundMinValue = backgroundMinValue;
        firePropertyChange("backgroundMinValue", oldValue, backgroundMinValue);
    }

    public int getBackgroundMaxValue() {
        return backgroundMaxValue;
    }

    public void setBackgroundMaxValue(int backgroundMaxValue) {
        Object oldValue = this.backgroundMaxValue;
        this.backgroundMaxValue = backgroundMaxValue;
        firePropertyChange("backgroundMaxValue", oldValue, backgroundMaxValue);
    }

    public int getBackgroundTolValue() {
        return backgroundTolValue;
    }

    public void setBackgroundTolValue(int backgroundTolValue) {
        Object oldValue = this.backgroundTolValue;
        this.backgroundTolValue = backgroundTolValue;
        firePropertyChange("backgroundTolValue", oldValue, backgroundTolValue);
    }

    public String getBackgroundDiagnostics() {
        return backgroundDiagnostics;
    }

    public void setBackgroundDiagnostics(String backgroundDiagnostics) {
        Object oldValue = this.backgroundDiagnostics;
        this.backgroundDiagnostics = backgroundDiagnostics;
        firePropertyChange("backgroundDiagnostics", oldValue, backgroundDiagnostics);
    }

    public CvPipeline getPipeline(Camera camera, Nozzle nozzle, Location measureLocation) throws Exception {
        pipeline.setProperty("camera", camera);
        pipeline.setProperty("nozzleTip.diameter", getCalibrationTipDiameter());
        // Set the search tolerance to be somewhat larger than the threshold.
        pipeline.setProperty("nozzleTip.maxDistance", getOffsetThresholdLength().multiply(1 + detectionThresholdMargin));
        pipeline.setProperty("nozzleTip.center", measureLocation);
        Point maskCenter = VisionUtils.getLocationPixels(camera, measureLocation);
        pipeline.setProperty("MaskCircle.center", new org.opencv.core.Point(maskCenter.getX(), maskCenter.getY()));
        return pipeline;
    }

    public void setPipeline(CvPipeline calibrationPipeline) {
        this.pipeline = calibrationPipeline;
    }

    public synchronized List<BufferedImage> getBackgroundCalibrationImages() {
        return backgroundCalibrationImages;
    }
}