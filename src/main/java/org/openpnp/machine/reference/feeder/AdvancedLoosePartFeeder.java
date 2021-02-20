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

import java.util.List;

import javax.swing.Action;

import org.apache.commons.io.IOUtils;
import org.opencv.core.RotatedRect;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.feeder.wizards.AdvancedLoosePartFeederConfigurationWizard;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.MotionPlanner.CompletionType;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.simpleframework.xml.Element;

public class AdvancedLoosePartFeeder extends ReferenceFeeder {

    @Element(required = false)
    private CvPipeline pipeline = createDefaultPipeline();

    @Element(required = false)
    private CvPipeline trainingPipeline = createDefaultTrainingPipeline();

    private Location pickLocation;

    @Override
    public Location getPickLocation() throws Exception {
        if (pickLocation == null) {
            throw new Exception("Feeder " + getName() + ": No parts found.");
        }
        return pickLocation;
    }

    @Override
    public void feed(Nozzle nozzle) throws Exception {
        // no part found => no pick location
        pickLocation = location.derive(null, null, Double.NaN, 0.0);

        // if there is a part, get a precise location
        for (int i = 0; i < 3 && pickLocation != null; i++) {
            pickLocation = locateFeederPart(nozzle, pickLocation);
        }
    }

    /**
     * Executes the vision pipeline to locate a part.
     * @param nozzle used nozzle
     * @return location or null
     * @throws Exception something went wrong
     */
    private Location locateFeederPart(Nozzle nozzle, Location startPoint) throws Exception {
        Camera camera = nozzle.getHead().getDefaultCamera();
        MovableUtils.moveToLocationAtSafeZ(camera, startPoint.derive(null, null, Double.NaN, 0d));
        camera.waitForCompletion(CompletionType.WaitForStillstand);
        try (CvPipeline pipeline = getPipeline()) {
            // Process the pipeline to extract RotatedRect results
            pipeline.setProperty("camera", camera);
            pipeline.setProperty("nozzle", nozzle);
            pipeline.setProperty("feeder", this);
            pipeline.process();
            // Grab the results
            List<RotatedRect> results = (List<RotatedRect>) pipeline.getResult(VisionUtils.PIPELINE_RESULTS_NAME).model;
            if ((results == null) || results.isEmpty()) {
                //nothing found
                return null;
            }
            // Find the closest result
            results.sort((a, b) -> {
                Double da = VisionUtils.getPixelLocation(camera, a.center.x, a.center.y)
                        .getLinearDistanceTo(camera.getLocation());
                Double db = VisionUtils.getPixelLocation(camera, b.center.x, b.center.y)
                        .getLinearDistanceTo(camera.getLocation());
                return da.compareTo(db);
            });
            RotatedRect result = results.get(0);
            Location partLocation = VisionUtils.getPixelLocation(camera, result.center.x, result.center.y);
            // Get the result's Location
            // Update the location with the result's rotation
            partLocation = partLocation.derive(null, null, null, -(result.angle + getLocation().getRotation()));
            // Update the location with the correct Z, which is the configured Location's Z
            // plus the part height.
            partLocation =
                    partLocation.derive(null, null,
                            this.location.convertToUnits(partLocation.getUnits()).getZ()
                            + part.getHeight().convertToUnits(partLocation.getUnits()).getValue(),
                            null);
            MainFrame.get().getCameraViews().getCameraView(camera)
            .showFilteredImage(OpenCvUtils.toBufferedImage(pipeline.getWorkingImage()), 250);
            
            return checkIfInInitialView(camera, partLocation);
        }
    }

    /**
     * Checks if the testLocation is inside the camera view starting on the feeder location.
     * Avoids to run outside the initial area if a bad pipeline repeated detects the parts
     * on one edge of the field of view, even after moving the camera to the location.
     * @param camera the used camera
     * @param testLocation the location to test
     * @return the testLocation, or null if outside the initial field of view
     */
    private Location checkIfInInitialView(Camera camera, Location testLocation) {
        // just make sure, the vision did not "run away" => outside of the initial camera range
        // should never happen, but with badly dialed in pipelines ...
        double distanceX = Math.abs(this.location.convertToUnits(LengthUnit.Millimeters).getX() - testLocation.convertToUnits(LengthUnit.Millimeters).getX());
        double distanceY = Math.abs(this.location.convertToUnits(LengthUnit.Millimeters).getY() - testLocation.convertToUnits(LengthUnit.Millimeters).getY());
        
        // if moved more than the half of the camera picture size => something went wrong => return no result
        if (distanceX > camera.getUnitsPerPixel().convertToUnits(LengthUnit.Millimeters).getX() * camera.getWidth() / 2
                || distanceY > camera.getUnitsPerPixel().convertToUnits(LengthUnit.Millimeters).getY() * camera.getHeight() / 2) {
            System.err.println("Vision outside of the initial area");
            return null;
        }        
        return testLocation;
    }

    public CvPipeline getPipeline() {
        return pipeline;
    }

    public void resetPipeline() {
        pipeline = createDefaultPipeline();
    }

    public CvPipeline getTrainingPipeline() {
        return trainingPipeline;
    }

    public void resetTrainingPipeline() {
        trainingPipeline = createDefaultTrainingPipeline();
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new AdvancedLoosePartFeederConfigurationWizard(this);
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
            String xml = IOUtils.toString(AdvancedLoosePartFeeder.class
                    .getResource("AdvancedLoosePartFeeder-DefaultPipeline.xml"));
            return new CvPipeline(xml);
        }
        catch (Exception e) {
            throw new Error(e);
        }
    }
    
    public static CvPipeline createDefaultTrainingPipeline() {
        try {
            String xml = IOUtils.toString(AdvancedLoosePartFeeder.class
                    .getResource("AdvancedLoosePartFeeder-DefaultTrainingPipeline.xml"));
            return new CvPipeline(xml);
        }
        catch (Exception e) {
            throw new Error(e);
        }
    }
}
