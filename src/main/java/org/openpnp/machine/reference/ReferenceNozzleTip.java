package org.openpnp.machine.reference;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import org.apache.commons.io.IOUtils;
import org.opencv.core.KeyPoint;
import org.openpnp.ConfigurationListener;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.wizards.ReferenceNozzleTipCalibrationWizard;
import org.openpnp.machine.reference.wizards.ReferenceNozzleTipConfigurationWizard;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.AbstractNozzleTip;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage.Result;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Commit;

public class ReferenceNozzleTip extends AbstractNozzleTip {
    @ElementList(required = false, entry = "id")
    private Set<String> compatiblePackageIds = new HashSet<>();

    @Attribute(required = false)
    private boolean allowIncompatiblePackages;
    
    @Attribute(required = false)
    private int pickDwellMilliseconds;

    @Attribute(required = false)
    private int placeDwellMilliseconds;

    @Element(required = false)
    private Location changerStartLocation = new Location(LengthUnit.Millimeters);

    @Element(required = false)
    private double changerStartToMidSpeed = 1D;
    
    @Element(required = false)
    private Location changerMidLocation = new Location(LengthUnit.Millimeters);
    
    @Element(required = false)
    private double changerMidToMid2Speed = 1D;
    
    @Element(required = false)
    private Location changerMidLocation2;
    
    @Element(required = false)
    private double changerMid2ToEndSpeed = 1D;
    
    @Element(required = false)
    private Location changerEndLocation = new Location(LengthUnit.Millimeters);
    
    
    @Element(required = false)
    private Calibration calibration = new Calibration();

    @Element(required = false)
    private double vacuumLevelPartOnLow;
    
    @Element(required = false)
    private double vacuumLevelPartOnHigh;
    
    @Element(required = false)
    private double vacuumLevelPartOffLow;
    
    @Element(required = false)
    private double vacuumLevelPartOffHigh;

    @Deprecated
    @Element(required = false)
    private Double vacuumLevelPartOn;
    
    @Deprecated
    @Element(required = false)
    private Double vacuumLevelPartOff;
    
    private Set<org.openpnp.model.Package> compatiblePackages = new HashSet<>();

    public ReferenceNozzleTip() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationLoaded(Configuration configuration) throws Exception {
                for (String id : compatiblePackageIds) {
                    org.openpnp.model.Package pkg = configuration.getPackage(id);
                    if (pkg == null) {
                        continue;
                    }
                    compatiblePackages.add(pkg);
                }
                /*
                 * Backwards compatibility. Since this field is being added after the fact, if
                 * the field is not specified in the config then we just make a copy of the
                 * other mid location. The result is that if a user already has a changer
                 * configured they will not suddenly have a move to 0,0,0,0 which would break
                 * everything.
                 */
                if (changerMidLocation2 == null) {
                    changerMidLocation2 = changerMidLocation.derive(null, null, null, null);
                }
            }
        });
    }
    
    @Commit
    public void commit() {
        vacuumLevelPartOn = null;
        vacuumLevelPartOff = null;
    }

    @Override
    public boolean canHandle(Part part) {
        boolean result =
                allowIncompatiblePackages || compatiblePackages.contains(part.getPackage());
        // Logger.debug("{}.canHandle({}) => {}", getName(), part.getId(), result);
        return result;
    }

    public Set<org.openpnp.model.Package> getCompatiblePackages() {
        return new HashSet<>(compatiblePackages);
    }

    public void setCompatiblePackages(Set<org.openpnp.model.Package> compatiblePackages) {
        this.compatiblePackages.clear();
        this.compatiblePackages.addAll(compatiblePackages);
        compatiblePackageIds.clear();
        for (org.openpnp.model.Package pkg : compatiblePackages) {
            compatiblePackageIds.add(pkg.getId());
        }
    }

    @Override
    public String toString() {
        return getName() + " " + getId();
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceNozzleTipConfigurationWizard(this);
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return null;
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return new Action[] {unloadAction, loadAction, deleteAction};
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
                new PropertySheetWizardAdapter(getConfigurationWizard()),
                new PropertySheetWizardAdapter(new ReferenceNozzleTipCalibrationWizard(this), "Calibration")
                };
    }

    public boolean isAllowIncompatiblePackages() {
        return allowIncompatiblePackages;
    }

    public void setAllowIncompatiblePackages(boolean allowIncompatiblePackages) {
        this.allowIncompatiblePackages = allowIncompatiblePackages;
    }
    
    public int getPickDwellMilliseconds() {
        return pickDwellMilliseconds;
    }

    public void setPickDwellMilliseconds(int pickDwellMilliseconds) {
        this.pickDwellMilliseconds = pickDwellMilliseconds;
    }

    public int getPlaceDwellMilliseconds() {
        return placeDwellMilliseconds;
    }

    public void setPlaceDwellMilliseconds(int placeDwellMilliseconds) {
        this.placeDwellMilliseconds = placeDwellMilliseconds;
    }

    public Location getChangerStartLocation() {
        return changerStartLocation;
    }

    public void setChangerStartLocation(Location changerStartLocation) {
        this.changerStartLocation = changerStartLocation;
    }

    public Location getChangerMidLocation() {
        return changerMidLocation;
    }

    public void setChangerMidLocation(Location changerMidLocation) {
        this.changerMidLocation = changerMidLocation;
    }

    public Location getChangerMidLocation2() {
        return changerMidLocation2;
    }

    public void setChangerMidLocation2(Location changerMidLocation2) {
        this.changerMidLocation2 = changerMidLocation2;
    }

    public Location getChangerEndLocation() {
        return changerEndLocation;
    }

    public void setChangerEndLocation(Location changerEndLocation) {
        this.changerEndLocation = changerEndLocation;
    }
    
    public double getChangerStartToMidSpeed() {
        return changerStartToMidSpeed;
    }

    public void setChangerStartToMidSpeed(double changerStartToMidSpeed) {
        this.changerStartToMidSpeed = changerStartToMidSpeed;
    }

    public double getChangerMidToMid2Speed() {
        return changerMidToMid2Speed;
    }

    public void setChangerMidToMid2Speed(double changerMidToMid2Speed) {
        this.changerMidToMid2Speed = changerMidToMid2Speed;
    }

    public double getChangerMid2ToEndSpeed() {
        return changerMid2ToEndSpeed;
    }

    public void setChangerMid2ToEndSpeed(double changerMid2ToEndSpeed) {
        this.changerMid2ToEndSpeed = changerMid2ToEndSpeed;
    }

    public Nozzle getParentNozzle() {
        for (Head head : Configuration.get().getMachine().getHeads()) {
            for (Nozzle nozzle : head.getNozzles()) {
                for (NozzleTip nozzleTip : nozzle.getNozzleTips()) {
                    if (nozzleTip == this) {
                        return nozzle;
                    }
                }
            }
        }
        return null;
    }

    public double getVacuumLevelPartOnLow() {
        return vacuumLevelPartOnLow;
    }

    public void setVacuumLevelPartOnLow(double vacuumLevelPartOnLow) {
        this.vacuumLevelPartOnLow = vacuumLevelPartOnLow;
    }

    public double getVacuumLevelPartOnHigh() {
        return vacuumLevelPartOnHigh;
    }

    public void setVacuumLevelPartOnHigh(double vacuumLevelPartOnHigh) {
        this.vacuumLevelPartOnHigh = vacuumLevelPartOnHigh;
    }
    
    public double getVacuumLevelPartOffLow() {
        return vacuumLevelPartOffLow;
    }

    public void setVacuumLevelPartOffLow(double vacuumLevelPartOffLow) {
        this.vacuumLevelPartOffLow = vacuumLevelPartOffLow;
    }

    public double getVacuumLevelPartOffHigh() {
        return vacuumLevelPartOffHigh;
    }

    public void setVacuumLevelPartOffHigh(double vacuumLevelPartOffHigh) {
        this.vacuumLevelPartOffHigh = vacuumLevelPartOffHigh;
    }

    public Calibration getCalibration() {
        return calibration;
    }
    
    public void calibrate() throws Exception {
        getCalibration().calibrate(this);
    }
    
    public boolean isCalibrated() {
        return getCalibration().isCalibrated();
    }

    public Action loadAction = new AbstractAction("Load") {
        {
            putValue(SMALL_ICON, Icons.nozzleTipLoad);
            putValue(NAME, "Load");
            putValue(SHORT_DESCRIPTION, "Load the currently selected nozzle tip.");
        }

        @Override
        public void actionPerformed(final ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                getParentNozzle().loadNozzleTip(ReferenceNozzleTip.this);
            });
        }
    };

    public Action unloadAction = new AbstractAction("Unload") {
        {
            putValue(SMALL_ICON, Icons.nozzleTipUnload);
            putValue(NAME, "Unload");
            putValue(SHORT_DESCRIPTION, "Unload the currently loaded nozzle tip.");
        }

        @Override
        public void actionPerformed(final ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                getParentNozzle().unloadNozzleTip();
            });
        }
    };
    
    public Action deleteAction = new AbstractAction("Delete Nozzle Tip") {
        {
            putValue(SMALL_ICON, Icons.nozzleTipRemove);
            putValue(NAME, "Delete Nozzle Tip");
            putValue(SHORT_DESCRIPTION, "Delete the currently selected nozzle tip.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            int ret = JOptionPane.showConfirmDialog(MainFrame.get(),
                    "Are you sure you want to delete " + getName() + "?",
                    "Delete " + getName() + "?", JOptionPane.YES_NO_OPTION);
            if (ret == JOptionPane.YES_OPTION) {
                getParentNozzle().removeNozzleTip(ReferenceNozzleTip.this);
            }
        }
    };

    @Root
    public static class Calibration extends AbstractModelObject {
        public static interface RunoutCompensation {

            Location getOffset(double angle);
            
            Location getAxisOffset();
            
            @Override
            String toString();
        }
        
        public static class TableBasedRunoutCompensation implements RunoutCompensation {
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

        public static class ModelBasedRunoutCompensation implements RunoutCompensation {
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
        
        private RunoutCompensation runoutCompensation = null;
        
        public enum RunoutCompensationAlgorithm {
            Model, Table
        }
        
        @Attribute(required = false)
        private RunoutCompensationAlgorithm runoutCompensationAlgorithm = RunoutCompensationAlgorithm.Model;      // modelBased or tableBased? Two implementations are available

        /**
         * TODO Left for backward compatibility. Unused. Can be removed after Feb 7, 2020.
         */
        @Deprecated
        @Attribute(required=false)
        private Double angleIncrement = null;
        
        @Commit
        public void commit() {
            angleIncrement = null;
        }
        
        // Max allowed linear distance w.r.t. bottom camera for an offset measurement - measurements above threshold are removed from pipelines results 
        @Attribute(required = false)
        private Double offsetThreshold = 0.5;

        public RunoutCompensationAlgorithm getRunoutCompensationAlgorithm() {
            return this.runoutCompensationAlgorithm;
        }

        public void setRunoutCompensationAlgorithm(RunoutCompensationAlgorithm runoutCompensationAlgorithm) {
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
            
            Nozzle nozzle = nozzleTip.getParentNozzle();
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
}
