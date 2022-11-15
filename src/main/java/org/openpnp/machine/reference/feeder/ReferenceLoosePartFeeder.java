/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.machine.reference.feeder;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.Action;
import javax.swing.SwingUtilities;

import org.apache.commons.io.IOUtils;
import org.opencv.core.RotatedRect;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.FeedersPanel;
import org.openpnp.gui.JobPanel;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.index.exceptions.FeedFailureException;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.feeder.wizards.ReferenceLoosePartFeederConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.simpleframework.xml.core.Commit;

public class ReferenceLoosePartFeeder extends ReferenceFeeder {
    private enum WhoCalledMe {
    	None,
    	Job,
    	Feed,	// From Feeders Tab
    	Pick,	// From Feeders Tab
    	Suspense
    }
	
    private Location visionLocation;
    private boolean humanVision = false;
    private WhoCalledMe whoCalledMe = WhoCalledMe.None;

    @Override
    public Location getPickLocation() throws Exception {
        return visionLocation == null ? location : visionLocation;
    }

    @Override
    public void feed(Nozzle nozzle) throws Exception {
    	if (!humanVision) {
	    	Camera camera = nozzle.getHead()
	    			.getDefaultCamera();
	    	// Move to the feeder pick location
	    	MovableUtils.moveToLocationAtSafeZ(camera, location);
	    	try (CvPipeline pipeline = getPipeline()) {
	    		for (int i = 0; i < 3; i++) {
	    			visionLocation = getPickLocationWithCv(pipeline, camera, nozzle);
	    			camera.moveTo(visionLocation.derive(null, null, null, 0.0));
	    		}
	    		MainFrame.get()
	    		.getCameraViews()
	    		.getCameraView(camera)
	    		.showFilteredImage(OpenCvUtils.toBufferedImage(pipeline.getWorkingImage()),
	    				1500);
	    	}
	    	catch (Exception e) {
	    		if (whoCalledMe == WhoCalledMe.None) {
		    		StackTraceElement[]  stacktrace = Thread.currentThread().getStackTrace();
	    			
		    		if (stacktrace[3].getMethodName() == "pickFeeder") {
		    			whoCalledMe = WhoCalledMe.Pick;
		    		}
		    		else if (stacktrace[2].getMethodName() == "feedFeeder") {
		    			whoCalledMe = WhoCalledMe.Feed;
		    		}
		    		else {
		    			whoCalledMe = WhoCalledMe.Job;		    			
		    		}
	    		}
	    		else {										// monkey testing
	        	    SwingUtilities.invokeLater(() -> {
		        		MainFrame.get().hideInstructions();
	        	    });	        		
	        		humanVision = false;
	        		whoCalledMe = WhoCalledMe.None;  		// fall back to default
	        		throw new Exception ("Human Vision Aborted! Resuming normal flow");
	    		}
	    			
    			setupForJogging(camera);
	    		switch (whoCalledMe) {
		    		case Job:
			    		humanVision = true;
			    		throw new FeedFailureException(VisionUtils.HUMAN_VISION_FALLBACK);
		    		case Feed:
			    		throw new Exception (VisionUtils.HUMAN_VISION_FALLBACK);
		    		case Pick:
			    		humanVision = true;
			    		throw new Exception (VisionUtils.HUMAN_VISION_FALLBACK);
		    		default:
		    			break;
	    		}
	    	}
    	}
    	else {
    		humanVision = false;
    	}
    }

    @Override
    public boolean isPartHeightAbovePickLocation() {
        return true;
    }

    private Location getPickLocationWithCv(CvPipeline pipeline, Camera camera, Nozzle nozzle)
            throws Exception {
        // Process the pipeline to extract RotatedRect results
        pipeline.setProperty("camera", camera);
        pipeline.setProperty("nozzle", nozzle);
        pipeline.setProperty("feeder", this);
        pipeline.process();
        // Grab the results
        List<RotatedRect> results = pipeline.getExpectedResult(VisionUtils.PIPELINE_RESULTS_NAME)
                .getExpectedListModel(RotatedRect.class, 
                        new Exception("Feeder " + getName() + ": No parts found."));

        // Find the closest result
        results.sort((a, b) -> {
            Double da = VisionUtils.getPixelLocation(camera, a.center.x, a.center.y)
                                   .getLinearDistanceTo(camera.getLocation());
            Double db = VisionUtils.getPixelLocation(camera, b.center.x, b.center.y)
                                   .getLinearDistanceTo(camera.getLocation());
            return da.compareTo(db);
        });
        RotatedRect result = results.get(0);
        // Get the result's Location
        Location location = VisionUtils.getPixelLocation(camera, result.center.x, result.center.y);
        // Update the location's rotation with the result's angle
        location = location.derive(null, null, null, result.angle + this.location.getRotation());
        // Update the location with the correct Z, which is the configured Location's Z.
        double z = this.location.convertToUnits(location.getUnits()).getZ(); 
        location = location.derive(null, null, z, null);
        return location;
    }

    private void setupForJogging(Camera camera) {
	    SwingUtilities.invokeLater(() -> {
	    	MainFrame mf = MainFrame.get();
	        String title = String.format("Part Detection Failed with CV:");
	        String instructions= String.format("Manually jog the camera to the part and click Accept");
	        mf.showInstructions(title, instructions, true, true,
	               "Accept" , cancelActionListener, proceedActionListener);
	    });
	    
		MovableUtils.fireTargetedUserAction(camera);
	    
    }
    
    private final ActionListener proceedActionListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
        	doProceed();
       }
    };
    
    private void doProceed() {
        Camera camera;
		try {
			camera = Configuration.get().getMachine().getDefaultHead().getDefaultCamera();
	        Location l = camera.getLocation();
	        
	        // Update the location with the correct Z, which is the configured Location's Z.
	        double z = this.location.convertToUnits(l.getUnits()).getZ(); 
	        visionLocation = l.derive(null, null, z, null);

	        
	        if (whoCalledMe == WhoCalledMe.Job) {							// resubmit
		    	JobPanel jobTab = MainFrame.get().getJobTab();
	    		if ( JobPanel.State.Paused == jobTab.getJobState()) {
	    			jobTab.resumeJob();
	    		}
	        }
	        else if (whoCalledMe == WhoCalledMe.Pick) {						// resubmit pick
		    	FeedersPanel fedTab = MainFrame.get().getFeedersTab();
		    	fedTab.pickFeederAction
		    		.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null)); // restart pick
	        }
	        
	    	MainFrame.get().hideInstructions();
	    	whoCalledMe = WhoCalledMe.None;
	    }
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    private final ActionListener cancelActionListener = new ActionListener() {
    	public void actionPerformed(ActionEvent e) {
    		humanVision = false;
    		MainFrame.get().hideInstructions();
	    	whoCalledMe = WhoCalledMe.None;
    	}
    };    
    
    /**
     * Returns if the feeder can take back a part.
     * Makes the assumption, that after each feed a pick followed,
     * so the area around the visionLocation is now empty.
     */
    @Override
    public boolean canTakeBackPart() {
        if (visionLocation != null ) {  
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void takeBackPart(Nozzle nozzle) throws Exception {
        // first check if we can and want to take back this part (should be always be checked before calling, but to be sure)
        if (nozzle.getPart() == null) {
            throw new UnsupportedOperationException("No part loaded that could be taken back.");
        }
        if (!nozzle.getPart().equals(getPart())) {
            throw new UnsupportedOperationException("Feeder: " + getName() + " - Can not take back " + nozzle.getPart().getName() + " this feeder only supports " + getPart().getName());
        }
        if (!canTakeBackPart()) {
            throw new UnsupportedOperationException("Feeder: " + getName() + " - Currently no known free space. Can not take back the part.");
        }

        // ok, now put the part back on the location of the last pick
        nozzle.moveToPickLocation(this);
        nozzle.place();
        nozzle.moveToSafeZ();
        if (nozzle.isPartOffEnabled(Nozzle.PartOffStep.AfterPlace) && !nozzle.isPartOff()) {
            throw new Exception("Feeder: " + getName() + " - Putting part back failed, check nozzle tip");
        }
        // set visionLocation to null, avoid putting a second part on the same location
        visionLocation = null;
    }
    
    public CvPipeline getPipeline() {
    	if (part != null) {
    		return part.getFiducialVisionSettings().getPipeline(); 
    	}
    	else {
    		return null;
    	}	
    }

    public void resetPipeline() {
    	CvPipeline pipeline = createDefaultPipeline();
    	if (part != null) {
    		part.getFiducialVisionSettings().setPipeline(pipeline); 
    	}
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceLoosePartFeederConfigurationWizard(this);
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
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {new PropertySheetWizardAdapter(getConfigurationWizard())};
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return null;
    }

    public static CvPipeline createDefaultPipeline() {
        try {
            String xml = IOUtils.toString(ReferenceLoosePartFeeder.class.getResource(
                    "ReferenceLoosePartFeeder-DefaultPipeline.xml"));
            return new CvPipeline(xml);
        }
        catch (Exception e) {
            throw new Error(e);
        }
    }

    @Commit
    void commit() {
    }

	  public void setPart(Part part) { 
		  super.setPart(part); 
	  }    
}
