package org.openpnp.machine.reference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.IOUtils;
import org.opencv.core.KeyPoint;
import org.openpnp.gui.MainFrame;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage.Result;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root
public class ReferenceNozzleTipCalibration extends AbstractModelObject {
    public static interface RunoutCompensation {

        Location getOffset(double angle);
        
        Location getAxisOffset();
        
        @Override
        String toString();
    }
    
    public static class TableBasedRunoutCompensation implements ReferenceNozzleTipCalibration.RunoutCompensation {
        List<Location> nozzleTipMeasuredLocations;


        public TableBasedRunoutCompensation(List<Location> nozzleTipMeasuredLocations) {
            //store data for later usage
            this.nozzleTipMeasuredLocations = nozzleTipMeasuredLocations;
        }

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
        
        /**
         * Find the two closest offsets to the angle being requested. The offsets start at first measurement at angleStart
         * and go to angleStop
         */
        private List<Location> getOffsetPairForAngle(double angle) {
            Location a = null, b = null;
            
            // angle asked for is the last in the table?
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
    	List<Location> nozzleTipMeasuredLocations;
    	
        double centerX = 0;
        double centerY = 0;
        double radius = 0;
        
        double phaseShift;

		public ModelBasedRunoutCompensation(List<Location> nozzleTipMeasuredLocations) {
			//store data for possible later usage
			this.nozzleTipMeasuredLocations = nozzleTipMeasuredLocations;
			
			// first calculate the circle fit and store the values to centerXY and radius
			// the measured offsets describe a circle with the rotational axis as the center, the runout is the circle radius
			this.calcCircleFitKasa(nozzleTipMeasuredLocations);
			
			// afterwards calc the phaseShift angle mapping
			this.calcPhaseShift(nozzleTipMeasuredLocations);
        }
		
		/* function to calc the model based offset in cartesian coordinates */
		public Location getOffset(double angle) {

            //add phase shift
            angle = angle - this.phaseShift;
            
            angle = Math.toRadians(angle);
            
            // convert from polar coords to xy cartesian offset values
            double offsetX = this.centerX + (this.radius * Math.cos(angle));
            double offsetY = this.centerY + (this.radius * Math.sin(angle));

            return new Location(LengthUnit.Millimeters, offsetX, offsetY, 0, 0);
		}
		
        private void calcCircleFitKasa(List<Location> nozzleTipMeasuredLocations) {
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
            
            Logger.debug("[nozzleTipCalibration]calculated nozzleEccentricity: {}", this.toString());
        }

        private void calcPhaseShift(List<Location> nozzleTipMeasuredLocations) {
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
        	double differenceAngleMean=0;
        	
            Iterator<Location> nozzleTipMeasuredLocationsIterator = nozzleTipMeasuredLocations.iterator();
    		while (nozzleTipMeasuredLocationsIterator.hasNext()) {
    			Location measuredLocation = nozzleTipMeasuredLocationsIterator.next();
    			
    			// get the measurement rotation
        		angle = measuredLocation.getRotation();		// the angle at which the measurement was made was stored to the nozzleTipMeasuredLocation into the rotation attribute
        		
        		// move the offset-location by the centerY/centerY. by this all offset-locations are wrt. the 0/0 origin
        		Location centeredLocation = measuredLocation.subtract(new Location(LengthUnit.Millimeters,this.centerX,this.centerY,0.,0.));
        		
        		// calculate the angle, the nozzle tip is located at
        		measuredAngle=Math.toDegrees(Math.atan2(centeredLocation.getY(), centeredLocation.getX()));
        		
        		// the difference is the phaseShift
        		double differenceAngle = angle-measuredAngle;
        		
        		// atan2 outputs angles from -PI to +PI. If one wants positive values, one needs to add +PI to negative values
        		if(differenceAngle < -180) {
        			differenceAngle += 360;
        		}
        		if(differenceAngle > 180) {
        			// since calculating the difference angle in some circumstances the angle can be bigger than 360 -> subtract
        			differenceAngle -= 360;
        		}
        		
        		Logger.trace("[nozzleTipCalibration]differenceAngle: {}", differenceAngle);
        		
        		// sum up all differenceAngles to build the average later
        		differenceAngleMean += differenceAngle;
    		}
        	
    		// calc the average
        	phaseShift = differenceAngleMean / nozzleTipMeasuredLocations.size();
            
        	this.phaseShift = phaseShift;
        	
        	Logger.debug("[nozzleTipCalibration]calculated phaseShift: {}", this.phaseShift);
        }
        
        
        @Override
        public String toString() {
            return String.format(Locale.US, "Center %f, %f, Runout %f", centerX, centerY, radius);
        }

        @Override
        public Location getAxisOffset() {
            return new Location(LengthUnit.Millimeters,centerX,centerY,0.,0.);
        }
    }
    
    @Element(required = false)
    private CvPipeline pipeline = createDefaultPipeline();

    @Attribute(required = false)
    private int angleSubdivisions = 6;
    @Attribute(required = false)
    private double angleStart = -180;
    @Attribute(required = false)
    private double angleStop = 180;
    
    @Attribute(required = false)
    private boolean enabled;
    
    private boolean calibrating;
    
    private ReferenceNozzleTipCalibration.RunoutCompensation runoutCompensation = null;
    
    public enum RunoutCompensationAlgorithm {
        Model, Table
    }
    
    @Attribute(required = false)
    private ReferenceNozzleTipCalibration.RunoutCompensationAlgorithm runoutCompensationAlgorithm = RunoutCompensationAlgorithm.Model;      // modelBased or tableBased? Two implementations are available

    // Max allowed linear distance w.r.t. bottom camera for an offset measurement - measurements above threshold are removed from pipelines results 
    @Attribute(required = false)
    private Double offsetThreshold = 0.5;

    public ReferenceNozzleTipCalibration.RunoutCompensationAlgorithm getRunoutCompensationAlgorithm() {
        return this.runoutCompensationAlgorithm;
    }

    public void setRunoutCompensationAlgorithm(ReferenceNozzleTipCalibration.RunoutCompensationAlgorithm runoutCompensationAlgorithm) {
        this.runoutCompensationAlgorithm = runoutCompensationAlgorithm;
    }

    public String getRunoutCompensationInformation() {
        if(isCalibrated()) {
            return runoutCompensation.toString();
        } else {
            return "Uncalibrated";
        }
    }
    
    public void calibrate(ReferenceNozzleTip nozzleTip) throws Exception {
        if ( !isEnabled() ) {
            return;
        }
        
        if ( !Configuration.get().getMachine().isHomed() ) {
            Logger.trace("[nozzleTipCalibration]Machine not yet homed, nozzle tip calibration request aborted");
            return;
        }
        
        // TODO STOPSHIP refactor calibration to nozzle, instead of nozzletip
        if (true) {
            throw new Exception("Calibration is broken in this version. Please downgrade if you require calibration.");
        }
//        Nozzle nozzle = nozzleTip.getParentNozzle();
        Nozzle nozzle = null;
        Camera camera = VisionUtils.getBottomVisionCamera();
        
        try {
            calibrating = true;
            
        	reset();

            // Move to the camera with an angle of 0.
            Location cameraLocation = camera.getLocation();
            // This is our baseline location
            Location measureBaseLocation = cameraLocation.derive(null, null, null, 0d);

            HashMap<String, Object> params = new HashMap<>();
            params.put("nozzle", nozzle);
            params.put("camera", camera);
            Configuration.get().getScripting().on("NozzleCalibration.Starting", params);
            
            // move nozzle to the camera location at zero degree - the nozzle must not necessarily be at the center
            MovableUtils.moveToLocationAtSafeZ(nozzle, measureBaseLocation);

            // determine the resulting angleIncrements
            double angleIncrement = ( angleStop - angleStart ) / this.angleSubdivisions;
            
            // determine the number of measurements to be made
            int angleSubdivisions = this.angleSubdivisions;
            if(angleStart == -180 && angleStop == 180) {
                // on a normal machine start is at 0°, stop would be at 360°. since the nozzle tip is at the same position then, the last measurement can be omitted
                angleSubdivisions--;
            }
            
            Logger.debug("[nozzleTipCalibration]starting measurement; angleStart: {}, angleStop: {}, angleIncrement: {}, angleSubdivisions: {}", angleStart, angleStop, angleIncrement, angleSubdivisions);
            
            // Capture nozzle tip positions and add them to a list. For these calcs the camera location is considered to be 0/0
            List<Location> nozzleTipMeasuredLocations = new ArrayList<>();
            for (int i = 0; i <= angleSubdivisions; i++) {
                // calc the current measurement-angle
                double measureAngle = angleStart + (i * angleIncrement); 
                
                Logger.debug("[nozzleTipCalibration]i: {}, measureAngle: {}", i, measureAngle);
                
            	// rotate nozzle to measurement angle
                Location measureLocation = measureBaseLocation.derive(null, null, null, measureAngle);
                nozzle.moveTo(measureLocation);
                
                // detect the nozzle tip
                Location offset = findCircle();
                offset = offset.derive(null, null, null, measureAngle);		// for later usage in the algorithm, the measureAngle is stored to the offset location 
                
                // add offset to array
                nozzleTipMeasuredLocations.add(offset);
                
                Logger.trace("[nozzleTipCalibration]measured offset: {}", offset);
                
            }
            
            Configuration.get().getScripting().on("NozzleCalibration.Finished", params);
            
        	if (this.runoutCompensationAlgorithm == RunoutCompensationAlgorithm.Model) {
        	    this.runoutCompensation = new ModelBasedRunoutCompensation(nozzleTipMeasuredLocations);
        	} else {
        	    this.runoutCompensation = new TableBasedRunoutCompensation(nozzleTipMeasuredLocations);
        	}
            
        }
        finally {
            // go to camera position (now offset-corrected). prevents the user from being irritated if it's not exactly centered
            nozzle.moveTo(camera.getLocation().derive(null, null, null, 0d));
            
            // after processing the nozzle returns to safe-z
            nozzle.moveToSafeZ();
            
            // setting to false in the very end to prevent endless calibration repetitions if calibration was not successful (pipeline not well or similar) and the nozzle is commanded afterwards somewhere else (where the calibration is asked for again ...)
            calibrating = false;
            
            // inform UI about new information available
            firePropertyChange("runoutCompensationInformation", null, null);
        }
    }
    
    /*
     * While calibrating the nozzle a circle was fitted to the runout path of the tip.
     * here the offset is reconstructed in XY-cartesian coordinates to be applied in moveTo commands.
     */
    public Location getCalibratedOffset(double angle) {
        if (!isEnabled() || !isCalibrated()) {
            return new Location(LengthUnit.Millimeters, 0, 0, 0, 0);
        }

        // Make sure the angle is between -180 and 180 - angles can get larger/smaller as +-180 if limitation to 180 degrees is disabled
        while (angle < 180) {
            angle += 360;
        }
        while (angle > 180) {
            angle -= 360;
        }
        
        return this.runoutCompensation.getOffset(angle);
        
    }

    private Location findCircle() throws Exception {
        Camera camera = VisionUtils.getBottomVisionCamera();
        try (CvPipeline pipeline = getPipeline()) {
            pipeline.setProperty("camera", camera);
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
            MainFrame.get().get().getCameraViews().getCameraView(camera).showFilteredImage(
                    OpenCvUtils.toBufferedImage(pipeline.getWorkingImage()), 1000);

            // add all results from pipeline to a Location-list post processing
            if (results instanceof List) {
                // are there any results from the pipeline?
                if (0==((List) results).size()) {
                    throw new Exception("No results from vision. Check pipeline.");                    
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
                    else {
                        throw new Exception("Unrecognized result " + result);
                    }
                  }
            }
            
            // remove all results that are above threshold
            Iterator<Location> locationsIterator = locations.iterator();
            while (locationsIterator.hasNext()) {
                Location location = locationsIterator.next();
                if (location.getLinearDistanceTo(0., 0.) > offsetThreshold) {
                    locationsIterator.remove();
                    Logger.trace("[nozzleTipCalibration]Removed offset location {} from results; measured distance {} exceeds offsetThreshold {}", location, location.getLinearDistanceTo(0., 0.), offsetThreshold);
                }
            }
            
            // check for a valid resultset
            if (locations.size() == 0) {
                throw new Exception("No valid results from pipeline within threshold");
            } else if (locations.size() > 1) {
                // one could throw an exception here, but we just log an info for now since
                // - invalid measurements above threshold are removed from results already and
                // - we expect the best match delivered from pipeline to be the first on the list.
                Logger.info("[nozzleTipCalibration]Got more than one result from pipeline. For best performance tweak pipeline to return exactly one result only. First location from the following set is taken as valid: " + locations);
            }
            
            // finally return the location at index (0) which is either a) the only one or b) the one best matching the nozzle tip
            return locations.get(0);
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

    public void reset() {
    	runoutCompensation = null;
    	
    	// inform UI about removed information
        firePropertyChange("runoutCompensationInformation", null, null);
    }

    public boolean isCalibrated() {
        return runoutCompensation != null;
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

    public double getOffsetThreshold() {
        return offsetThreshold;
    }

    public void setOffsetThreshold(double offsetThreshold) {
        this.offsetThreshold = offsetThreshold;
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