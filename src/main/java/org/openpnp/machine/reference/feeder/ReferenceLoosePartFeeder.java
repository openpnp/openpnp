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
import org.openpnp.machine.reference.feeder.wizards.ReferenceLoosePartFeederConfigurationWizard;
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

/*
 * TODO: We need to be able to determine "normal" angle for the part, which probably means width and
 * height, which is going to be tough when the body is square. Might need to make it more related to
 * bottom vision so that it can determine the correct angle for what it locates.
 * 
 * TODO: Remember the idea of using contours or whatever to find the part and then using template
 * matching to get it's orientation. That would solve the above, and make it work for polarized
 * parts too.
 * 
 * And related to that, this might be a good time to play with something like SIFT.
 */
public class ReferenceLoosePartFeeder extends ReferenceFeeder {
    private final static Logger logger = LoggerFactory.getLogger(ReferenceLoosePartFeeder.class);

    @Element(required = false)
    private CvPipeline pipeline = createDefaultPipeline();

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
            pickLocation = getPickLocation(camera);
            camera.moveTo(pickLocation);
        }
    }

    private Location getPickLocation(Camera camera) throws Exception {
        // Process the pipeline to extract RotatedRect results
        pipeline.setCamera(camera);
        pipeline.setFeeder(this);
        pipeline.process();
        // Grab the results
        List<RotatedRect> results = (List<RotatedRect>) pipeline.getResult("results").model;
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
        location = location.derive(null, null, null, result.angle);
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

    public CvPipeline getPipeline() {
        return pipeline;
    }

    public void resetPipeline() {
        pipeline = createDefaultPipeline();
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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {new PropertySheetWizardAdapter(getConfigurationWizard())};
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        // TODO Auto-generated method stub
        return null;
    }

    public static CvPipeline createDefaultPipeline() {
        try {
            String xml = IOUtils.toString(ReferenceLoosePartFeeder.class
                    .getResource("ReferenceLoosePartFeeder-DefaultPipeline.xml"));
            return new CvPipeline(xml);
        }
        catch (Exception e) {
            throw new Error(e);
        }
    }
}
