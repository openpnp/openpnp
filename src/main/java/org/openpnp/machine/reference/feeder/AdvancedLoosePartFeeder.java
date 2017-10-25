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
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.simpleframework.xml.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdvancedLoosePartFeeder extends ReferenceFeeder {
    private final static Logger logger = LoggerFactory.getLogger(AdvancedLoosePartFeeder.class);

    @Element(required = false)
    private CvPipeline pipeline = createDefaultPipeline();

    @Element(required = false)
    private CvPipeline trainingPipeline = createDefaultTrainingPipeline();

    private Location pickLocation;

    @Override
    public Location getPickLocation() throws Exception {
        return pickLocation == null ? location : pickLocation;
    }

    @Override
    public void feed(Nozzle nozzle) throws Exception {
        Camera camera = nozzle.getHead().getDefaultCamera();
        // Move to the feeder pick location
        MovableUtils.moveToLocationAtSafeZ(camera, location);
        for (int i = 0; i < 3; i++) {
            pickLocation = getPickLocation(camera, nozzle);
            camera.moveTo(pickLocation);
        }
    }

    private Location getPickLocation(Camera camera, Nozzle nozzle) throws Exception {
        try (CvPipeline pipeline = getPipeline()) {
            // Process the pipeline to extract RotatedRect results
            pipeline.setProperty("camera", camera);
            pipeline.setProperty("nozzle", nozzle);
            pipeline.setProperty("feeder", this);
            pipeline.process();
            // Grab the results
            List<RotatedRect> results = (List<RotatedRect>) pipeline.getResult(VisionUtils.PIPELINE_RESULTS_NAME).model;
            if (results.isEmpty()) {
                throw new Exception("Feeder " + getName() + ": No parts found.");
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
            Location location = VisionUtils.getPixelLocation(camera, result.center.x, result.center.y);
            // Get the result's Location
            // Update the location with the result's rotation
            location = location.derive(null, null, null, -(result.angle + getLocation().getRotation()));
            // Update the location with the correct Z, which is the configured Location's Z
            // plus the part height.
            location =
                    location.derive(null, null,
                            this.location.convertToUnits(location.getUnits()).getZ()
                                    + part.getHeight().convertToUnits(location.getUnits()).getValue(),
                            null);
            MainFrame.get().getCameraViews().getCameraView(camera)
                    .showFilteredImage(OpenCvUtils.toBufferedImage(pipeline.getWorkingImage()), 250);
            return location;
        }
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
