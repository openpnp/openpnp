/*
 * Copyright (C) 2019 <mark@makr.zone>
 * based on the ReferenceStripFeeder 
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



import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.Action;

import org.apache.commons.io.IOUtils;
import org.opencv.core.RotatedRect;
import org.openpnp.ConfigurationListener;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.feeder.BlindsFeeder.FindFeatures;
import org.openpnp.machine.reference.feeder.wizards.BlindsFeederConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Point;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.Utils2D;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.Ransac.Line;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;


/**
 * Implementation of Feeder that indexes through an array of cut tape strips held
 * by a 3D printed base. Each tape lane has a blinds style cover that can open/close by 
 * shifting half the pocket pitch. This only works for tapes where the pockets use up 
 * less than half the pocket pitch as is usually the case with punched paper carrier tapes 
 * but also for some plastic/embossed carrier tapes.  
 */


public class BlindsFeeder extends ReferenceFeeder {

    static public final Location nullLocation = new Location(LengthUnit.Millimeters);

    @Element(required = false)
    private Location fiducial1Location = new Location(LengthUnit.Millimeters);

    @Element(required = false)
    private Location fiducial2Location = new Location(LengthUnit.Millimeters);

    @Element(required = false)
    private Location fiducial3Location = new Location(LengthUnit.Millimeters);

    @Attribute(required = false)
    private boolean normalize = true;

    @Element(required = false)
    private Length tapeLength = new Length(120, LengthUnit.Millimeters);

    @Element(required = false)
    private Length feederExtent = new Length(40, LengthUnit.Millimeters);

    @Element(required = false)
    private Length pocketCenterline = new Length(10, LengthUnit.Millimeters);

    @Element(required = false)
    private Length pocketPitch = new Length(4, LengthUnit.Millimeters);

    @Element(required = false)
    private Length pocketSize = new Length(3, LengthUnit.Millimeters);

    @Element(required = false)
    private CvPipeline pipeline = createDefaultPipeline();

    @Attribute(required = false)
    private boolean visionEnabled = true;

    @Attribute
    private int feedCount = 0;

    @Attribute(required = false)
    private int feederNo = 0;

    @Attribute(required = false)
    private int feedersTotal = 0;

    // These internal setting are not on the GUI but can be changed in the XML.
    @Attribute(required = false)
    private int fidLocMaxPasses = 3;

    @Attribute(required = false)
    private double fidLocToleranceMillimeter = 0.5;

    private boolean calibrating = false;
    private boolean calibrated = false;

    private void checkHomedState(Machine machine) {
        if (!machine.isHomed()) {
            this.setCalibrated(false);
        }
    }

    public BlindsFeeder() {
        // Listen to the machine become unhomed. Invalidate feeder calibration in this case. 
        // Note that home()  first switches the machine isHomed() state off, then on again, 
        // so we als catch re-homing. 
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationComplete(Configuration configuration) throws Exception {
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

    @Override
    public Location getPickLocation() throws Exception {

        // Make sure the feeder locations are calibrated, if vision is enabled. 
        if (isVisionEnabled()) {
            if (!isCalibrated()) {
                calibrateFeederLocations();
            }
        }

        // Calculate the next pick location in local feeder coordinates. 
        Length feederX = this.pocketPitch.multiply(this.feedCount).convertToUnits(location.getUnits());
        Length feederY = this.pocketCenterline.convertToUnits(location.getUnits());

        /* from the 3D print OpenSCAD code:
         * 
         *  TODO:
         *  
            tape_net=tape_length-sprocket_grid;//2*min_width-sprocket_diameter0;
            sprocket_count=floor(tape_net/sprocket_grid);
                 //grid_count*grid/sprocket_grid;
            pocket_count=floor(tape_net/pocket_grid)
                //grid_count*grid/pocket_grid
                + (pocket_grid < sprocket_grid ? 1 : 0); // 2mm grid has one pocket more
            sprocket_d = sprocket_grid*0.5+
                tape_align*(tape_net - (sprocket_count)*sprocket_grid);
            pocket_d = sprocket_d 
                + (pocket_grid < sprocket_grid ? -pocket_grid*0.5 : 0)
                + (((pocket_grid/sprocket_grid) % 2 == 0) ? sprocket_grid*0.5 : 0); // even numbered sprockets per part need shift
         */

        Location feederLocation = new Location(location.getUnits(), feederX.getValue(), feederY.getValue(), 
                location.getZ(), location.getRotation());
        Location machineLocation = transformFeederToMachineLocation(feederLocation);
        return machineLocation;
    }


    public void feed(Nozzle nozzle) throws Exception {
        setFeedCount(getFeedCount() + 1);
    }


    public class FindFeatures {
        private Camera camera;
        private CvPipeline pipeline;

        private List<RotatedRect> blinds;
        private List<RotatedRect> fiducials;
        private List<Line> lines;

        public FindFeatures(Camera camera, CvPipeline pipeline) {
            this.camera = camera;
            this.pipeline = pipeline;
        }

        public List<RotatedRect> getBlinds() {
            return blinds;
        }
        public List<RotatedRect> getFiducials() {
            return fiducials;
        }
        public List<Line> getLines() {
            return lines;
        }

        private double angleNorm(double angle) {
            while (angle > 45) {
                angle -= 90;
            }
            while (angle < -45) {
                angle += 90;
            }
            return angle;
        }

        public FindFeatures invoke() throws Exception {
            List<RotatedRect> results = null;
            try {
                // Grab the results
                results = (List<RotatedRect>) pipeline.getResult(VisionUtils.PIPELINE_RESULTS_NAME).model;
                if (results == null /*???|| results.isEmpty()*/) {
                    throw new Exception("Feeder " + getName() + ": No features found.");
                }
                // in accordance with EIA-481 etc. we use millimeters.
                Location mmScale = camera.getUnitsPerPixel().convertToUnits(LengthUnit.Millimeters);
                // TODO: configurable?
                final double fidMin = 1.8;
                final double fidMax = 2.2;
                final double fidAspect = 1.2; 
                // TODO: configurable?
                double blindMin = getPocketPitch().convertToUnits(LengthUnit.Millimeters).getValue()*0.5;
                double blindMax = getPocketSize().convertToUnits(LengthUnit.Millimeters).getValue();
                double blindAspect = 1.2*blindMax/blindMin;
                if (blindMax == 0) {
                    blindMax = 22;
                    blindAspect = 2;
                }
                if (blindMin == 0) {
                    blindMin = 0.5;
                    blindAspect = 3;
                }
                double sizeTolerance = 0.5/mmScale.getX();
                double angleTolerance = 20;
                double cosinusTolerance = Math.cos(angleTolerance*Math.PI/180);

                // Convert camera center.
                Location cameraLocation = transformMachineToFeederLocation(camera.getLocation());
                // Try to make sense of it.
                boolean angleTolerant = 
                        BlindsFeeder.nullLocation.equals(getFiducial1Location())
                        ||BlindsFeeder.nullLocation.equals(getFiducial2Location());
                boolean positionTolerant = angleTolerant || getPocketCenterline().getValue() == 0;
                blinds = new ArrayList<>();
                fiducials = new ArrayList<>();
                //List<Point> ransacPoints = new ArrayList<>();
                for (RotatedRect result : results) {
                    org.opencv.core.Point points[] = new org.opencv.core.Point[4];
                    result.points(points);
                    boolean isAtMargin = false;
                    for (int i = 0; i < 4; i++) {
                        // Filter out rects sticking to the edges.
                        if (points[i].x <= 2 || points[i].x >= camera.getWidth() - 2
                                || points[i].y <= 2 || points[i].y >= camera.getHeight() - 2) {
                            isAtMargin = true;
                        }
                        // Next, please.
                        i++;
                    }
                    if (! isAtMargin) {
                        // Convert shape center.
                        Location location = transformMachineToFeederLocation(VisionUtils.getPixelLocation(camera, result.center.x, result.center.y));
                        double angle = transformPixelToFeederAngle(result.angle);
                        Location mmSize = mmScale.multiply(result.size.width, result.size.height, 0, 0);
                        if (positionTolerant || Math.abs(cameraLocation.convertToUnits(LengthUnit.Millimeters).getLinearDistanceTo(location)) < 5) {
                            if (angleTolerant || Math.abs(angleNorm(angle - 45)) < angleTolerance) {
                                if (mmSize.getX() > fidMin && mmSize.getX() < fidMax && mmSize.getY() > fidMin && mmSize.getY() < fidMax
                                        && mmSize.getX()/mmSize.getY() < fidAspect 
                                        && mmSize.getY()/mmSize.getX() < fidAspect) {
                                    fiducials.add(result);
                                }
                            }
                            if (angleTolerant || Math.abs(angleNorm(angle - 0)) < angleTolerance) {
                                if (mmSize.getX() > blindMin && mmSize.getX() < blindMax && mmSize.getY() > blindMin && mmSize.getY() < blindMax
                                        && mmSize.getX()/mmSize.getY() < blindAspect 
                                        && mmSize.getY()/mmSize.getX() < blindAspect) {
                                    // Fits the size requirements.
                                    blinds.add(result);
                                    /* // collect for ransac too
                                    for (int i = 0; i < results.size(); i++) {
                                        ransacPoints.add(result.center.clone());
                                    }*/
                                }
                            }
                        }
                    }
                }

                // Sort fiducials by distance from camera center.
                Collections.sort(fiducials, new Comparator<RotatedRect>() {
                    @Override
                    public int compare(RotatedRect o1, RotatedRect o2) {
                        double d1 = VisionUtils.getPixelLocation(camera, o1.center.x, o1.center.y).getLinearDistanceTo(camera.getLocation());
                        double d2 = VisionUtils.getPixelLocation(camera, o2.center.x, o2.center.y).getLinearDistanceTo(camera.getLocation());
                        return Double.compare(d1, d2);
                    }
                });

                // Get rid of outliers.
                List<RotatedRect> bestList = null;
                int bestSize = 0;
                Line bestLine = null;
                // For each two blinds speculate about a pitch and line.
                for (int i = 0; i < blinds.size(); i++) {
                    RotatedRect blind1 = blinds.get(i);
                    Location location1 = transformMachineToFeederLocation(VisionUtils.getPixelLocation(camera, blind1.center.x, blind1.center.y))
                            .convertToUnits(LengthUnit.Millimeters);
                    double angle1 = transformPixelToFeederAngle(blind1.angle);
                    for (int j = i+1; j < blinds.size(); j++) {
                        RotatedRect blind2 = blinds.get(j);
                        Location location2 = transformMachineToFeederLocation(VisionUtils.getPixelLocation(camera, blind2.center.x, blind2.center.y))
                                .convertToUnits(LengthUnit.Millimeters);
                        double angle2 = transformPixelToFeederAngle(blind2.angle);
                        if (Math.abs(angleNorm(angle1 - angle2)) < angleTolerance
                                && Math.abs(blind1.size.width - blind2.size.width) < sizeTolerance
                                && Math.abs(blind1.size.height - blind2.size.height) < sizeTolerance) {
                            // Same angle and dimension.
                            double distance2 = location2.getLinearDistanceTo(location1);
                            int pitch = (int) Math.round(distance2/2)*2;
                            if (pitch == 2 || (pitch % 4) == 0) {
                                // Valid pitch.
                                //double presetPitch = feeder.getPocketPitch().convertToUnits(LengthUnit.Millimeters).getValue();
                                Location axis = location2.subtract(location1).multiply(1/distance2, 1/distance2, 0, 0);
                                List<RotatedRect> candList = new ArrayList<>();
                                candList.add(blind1);
                                candList.add(blind2);
                                Line line = new Line(blind1.center, blind2.center);
                                double minCos = 0;
                                double maxCos = 1;
                                for (int k = 0; k < blinds.size(); k++) {
                                    if (k != i && k != j) {
                                        RotatedRect blind3 = blinds.get(k);
                                        Location location3 = transformMachineToFeederLocation(VisionUtils.getPixelLocation(camera, blind3.center.x, blind3.center.y))
                                                .convertToUnits(LengthUnit.Millimeters);
                                        double angle3 = transformPixelToFeederAngle(blind3.angle);
                                        if (Math.abs(angleNorm(angle1 - angle3)) < angleTolerance
                                                && Math.abs(blind1.size.width - blind3.size.width) < sizeTolerance
                                                && Math.abs(blind1.size.height - blind3.size.height) < sizeTolerance) {
                                            // Same angle and dimension.
                                            double distance3 = location3.getLinearDistanceTo(location1);
                                            double pitchFactor = Math.round(distance3/pitch);
                                            double pitchErr = Math.abs(pitch*pitchFactor - distance3); 
                                            if (pitchErr < 1) {
                                                // Matches pitch
                                                Location vector = location3.subtract(location1);
                                                double cosinus = (axis.getX()*vector.getX() + axis.getY()*vector.getY())/distance3;
                                                if (Math.abs(cosinus) > cosinusTolerance) {
                                                    // Right, we got one
                                                    candList.add(blind3);
                                                    if (cosinus < minCos) {
                                                        line.a = blind3.center;
                                                        minCos = cosinus;
                                                    }
                                                    else if (cosinus > maxCos) {
                                                        line.b = blind3.center;
                                                        maxCos = cosinus;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                if (bestList == null || candList.size() > bestSize) {
                                    bestList = candList;
                                    bestSize = candList.size();
                                    bestLine = line;
                                }
                            }
                        }
                    }
                }
                lines = new ArrayList<>();
                if (bestLine != null && bestSize >= 3) {
                    lines.add(bestLine);
                }
            }
            catch (ClassCastException e) {
                throw new Exception("Unrecognized result type (should be RotatedRect): " + results);
            }

            return this;
        }
    }

    private Location locateFiducial(Camera camera, Location location) throws Exception {
        if (location.equals(nullLocation)) {
            throw new Exception("Feeder " + getName() + ": Fiducial location not set.");
        }

        location = location.derive(camera.getLocation(), false, false, true, false);
        try (CvPipeline pipeline = getCvPipeline(camera, true)) {

            for (int i = 0; i < fidLocMaxPasses; i++) {
                // Move to the location
                MovableUtils.moveToLocationAtSafeZ(camera, location);

                // Process vision
                pipeline.process();

                // Interpret the results
                BlindsFeeder.FindFeatures findFeaturesResults = new FindFeatures(camera, pipeline).invoke();
                List<RotatedRect> fiducials = findFeaturesResults.getFiducials();
                if (fiducials.isEmpty()) {
                    throw new Exception("Feeder " + getName() + ": Fiducial not found.");
                }
                // Convert location.
                RotatedRect bestFiducial = fiducials.get(0);
                Location bestFiducialLocation = VisionUtils.getPixelLocation(camera, bestFiducial.center.x, bestFiducial.center.y);
                double mmDistance = bestFiducialLocation.getLinearLengthTo(location).convertToUnits(LengthUnit.Millimeters).getValue();

                // update location
                location = bestFiducialLocation;

                if (mmDistance < fidLocToleranceMillimeter) {
                    // Already a good enough fix - skip further passes
                    break;
                }
            }
            // Return the final result.
            return location;
        }
    }

    public void calibrateFeederLocations() throws Exception {
        if (!isCalibrating()) {

            if ( !Configuration.get().getMachine().isHomed() ) {
                throw new Exception("Feeder " + getName() + ": Machine not yet homed.");
            }

            setCalibrating(true);
            try {
                Camera camera = Configuration.get()
                        .getMachine()
                        .getDefaultHead()
                        .getDefaultCamera();
                setFiducial1Location(locateFiducial(camera, getFiducial1Location()));
                setFiducial2Location(locateFiducial(camera, getFiducial2Location()));
                setFiducial3Location(locateFiducial(camera, getFiducial3Location()));
                setCalibrated(true);
            }
            finally {
                setCalibrating(false);
            }
        }
    }


    /**
     * The Machine to Feeder transformation and inverse.
     */
    private AffineTransform tx;
    private AffineTransform txInverse;
    private double txRotation;

    
    public void invalidateFeederTransformation() {
        tx = null;
        txInverse = null;
    }
    
    /**
     * Generates the transformation from feeder local coordinate system to 
     * machine coordinates as defined by the fiducials.
     * @return
     */
    private boolean updateFeederToMachineTransform() {
        // Make sure the inverse will be regenerated.
        txInverse = null;
        // Get some basics. 
        Location origin = fiducial1Location;
        double mm = new Length(1, LengthUnit.Millimeters).convertToUnits(origin.getUnits()).getValue();
        double distance = origin.getLinearDistanceTo(fiducial2Location);
        // Check sanity.
        if (nullLocation.equals(fiducial1Location) 
                || nullLocation.equals(fiducial2Location) 
                || distance < 1*mm) {
            // Some fiducials not set yet or invalid - just take the unity transform for now.  
            tx = new AffineTransform();
            // Translate for fiducial 1 (if set).
            tx.translate(origin.getX(), origin.getY());
            txRotation = 0.;
            return false;
        }
        if (!normalize) {
            // We know the fiducial distance should be a multiple of 2mm as it is aligned with the 
            // sprockets in the 3D printed model. 
            distance = Math.round(distance/2/mm)*2*mm;
        }
        // Update the tape length to rounded distance.
        setTapeLength(new Length(Math.round(distance/2/mm)*2*mm, origin.getUnits()));

        Location axisX;
        Location axisY; 
        axisX = fiducial2Location.subtract(origin).multiply(1/distance, 1/distance, 0, 0);

        // Fiducial 3 may be the one across from fiducial 1 or 2 - the nearer one is it. 
        Location ref = fiducial3Location.getLinearDistanceTo(fiducial2Location) < fiducial3Location.getLinearDistanceTo(fiducial1Location) ?
                fiducial2Location : fiducial1Location;
        distance = fiducial3Location.equals(nullLocation) ? 0 : fiducial3Location.getLinearDistanceTo(ref);
        if (normalize || distance < 1*mm) {
            // We want to normalize or fiducial 3 is not set or has no distance. 
            // Take the cross product of the X axis to form the Y axis (i.e. the fiducial 3 is ignored for the axis).
            axisY = new Location(axisX.getUnits(), -axisX.getY(), axisX.getY(), 0, 0);
            // Fiducial 3 can still serve to get the extent of the overall feeder.
            setFeederExtent(new Length(Math.round(distance/mm)*mm, origin.getUnits()));
        }
        else {
            // We know the fiducial distance should be a multiple of 1mm as this is enforced in the 3D printed model. 
            distance = Math.round(distance/mm)*mm;
            axisY = fiducial3Location.subtract(ref).multiply(1/distance, 1/distance, 0, 0);
            // Fiducial 3 also serves to get the extent of the overall feeder.
            setFeederExtent(new Length(distance, origin.getUnits()));
        }
        // Finally create the transformation.  
        tx = new AffineTransform(
                axisX.getX(), axisX.getY(), 
                axisY.getX(), axisY.getY(), 
                origin.getX(), origin.getY());
        // Reconstruct the angle at which the feeder lays on the machine. When "normalize" is off, it is only
        // an approximation valid for lines parallel to the tapes (as is most useful). It ignores shear in the
        // transform.
        txRotation = Math.toDegrees(Math.atan2(axisX.getX(), axisX.getY()));
        return true;
    }

    AffineTransform getFeederToMachineTransform() {
        if (tx == null) {
            updateFeederToMachineTransform();
        }
        return tx;
    }

    double getFeederToMachineRotation() {
        if (tx == null) {
            updateFeederToMachineTransform();
        }
        return txRotation;
    }

    double getMachineToFeederRotation() {
        if (tx == null) {
            updateFeederToMachineTransform();
        }
        return -txRotation;
    }

    private AffineTransform getMachineToFeederTransform() {
        if (txInverse == null) {
            txInverse = new AffineTransform(getFeederToMachineTransform());
            try {
                txInverse.invert();
            }
            catch (NoninvertibleTransformException e) {
                // Should really never happen, as getFeederToMachineTransform() falls back to benign transforms.   
                // Just take the unity transform for now. 
                txInverse = new AffineTransform();
                // Translate from fiducial 1 (if set).
                txInverse.translate(-fiducial1Location.getX(), -fiducial1Location.getY());
            }
        }
        return txInverse;
    }

    public Location transformFeederToMachineLocation(Location feederLocation) {
        AffineTransform tx = getFeederToMachineTransform();
        double rotation = getFeederToMachineRotation();
        feederLocation = feederLocation.convertToUnits(fiducial1Location.getUnits()); 
        Point2D.Double ptDst = new Point2D.Double();
        tx.transform(new Point2D.Double(feederLocation.getX(), feederLocation.getY()), ptDst);
        return new Location(fiducial1Location.getUnits(), ptDst.getX(), ptDst.getY(), 
                feederLocation.getZ(), feederLocation.getRotation()+rotation);
    }

    public Location transformMachineToFeederLocation(Location machineLocation) {
        AffineTransform tx = getMachineToFeederTransform();
        double rotation = getMachineToFeederRotation();
        machineLocation = machineLocation.convertToUnits(fiducial1Location.getUnits()); 
        Point2D.Double ptDst = new Point2D.Double();
        tx.transform(new Point2D.Double(machineLocation.getX(), machineLocation.getY()), ptDst);
        return new Location(fiducial1Location.getUnits(), ptDst.getX(), ptDst.getY(), 
                machineLocation.getZ(), machineLocation.getRotation()+rotation);
    }

    public double transformFeederToMachineAngle(double angle) {
        return angle+getFeederToMachineRotation();
    }
    public double transformMachineToFeederAngle(double angle) {
        // We don't need th reverse transform, a simple sign reversion will do.
        return angle+getMachineToFeederRotation();
    }
    public double transformPixelToFeederAngle(double angle) {
        // Pixel angles are left-handed, coming from the OpenCV coordinate system, where 
        // Z points away from the viewer whereas in OpenPNP the opposite is true. 
        return -transformMachineToFeederAngle(angle);
    }

    public boolean isLocationInFeeder(Location location, boolean fiducial1MatchOnly) {
        // First check if it is a fiducial 1 match.  
        if (nullLocation.equals(fiducial1Location)) {
            // Never match a uninizialized fiducial.
            return false;
        }
        if (fiducial1Location.convertToUnits(LengthUnit.Millimeters).getLinearDistanceTo(location) < 1) {
            // Direct fiducial 1 match with tolerance.
            return true;
        }
        else if (fiducial1MatchOnly) {
            // No fiducial 1 match but one is required.
            return false;
        }
        // No match on the fiducial 1, so check whole feeder holder area.	
        Location feederLocation = transformMachineToFeederLocation(location);
        double mm = new Length(1, LengthUnit.Millimeters).convertToUnits(feederLocation.getUnits()).getValue();
        if (feederLocation.getX() >= -1*mm && feederLocation.getX() <= tapeLength.convertToUnits(feederLocation.getUnits()).getValue() + 1*mm) {
            if (feederLocation.getY() >= -1*mm && feederLocation.getY() <= feederExtent.getValue() + 1*mm) {
                return true;
            }
        }
        return false;
    }

    public static List<BlindsFeeder> getConnectedFeedersByLocation(Location location, boolean fiducial1MatchOnly) {
        // Get all the feeders with connected by location.
        List<BlindsFeeder> list = new ArrayList<>();
        for (Feeder feeder : Configuration.get().getMachine().getFeeders()) {
            if (feeder instanceof BlindsFeeder) {
                BlindsFeeder blindsFeeder = (BlindsFeeder) feeder;
                if (blindsFeeder.isLocationInFeeder(location, fiducial1MatchOnly)) {
                    list.add(blindsFeeder);
                }
            }
        }
        // Sort by feeder tape centerline.
        Collections.sort(list, new Comparator<BlindsFeeder>() {
            @Override
            public int compare(BlindsFeeder feeder1, BlindsFeeder feeder2)  {
                return new Double(feeder1.getPocketCenterline().getValue())
                        .compareTo(feeder2.getPocketCenterline().convertToUnits(feeder1.getPocketCenterline().getUnits()).getValue());
            }
        });
        return list;
    }

    public List<BlindsFeeder> getConnectedFeeders() {
        // Get all the feeders with the same fiducial 1 location.
        return getConnectedFeedersByLocation(fiducial1Location, true);
    }

    private boolean isUpdating = false;

    public void updateFromConnectedFeeder(BlindsFeeder feeder) {
        if (this != feeder && ! isUpdating) {
            isUpdating = true;
            setFiducial1Location(feeder.fiducial1Location);
            setFiducial2Location(feeder.fiducial2Location);
            setFiducial3Location(feeder.fiducial3Location);
            setTapeLength(feeder.tapeLength);
            setFeederExtent(feeder.feederExtent);
            setNormalize(feeder.normalize);
            setCalibrated(feeder.calibrated);
            isUpdating = false;
        }
    }

    private void updateTapeNumbering()    {
        // Renumber the feeder tape lanes.
        List<BlindsFeeder> list = getConnectedFeedersByLocation(fiducial1Location, true);
        int feedersTotal = list.size();
        int feederNo = 0;
        for (BlindsFeeder feeder : list) {
            feeder.setFeedersTotal(feedersTotal);
            feeder.setFeederNo(++feederNo);
        }
    }

    public boolean updateFromConnectedFeeder(Location location, boolean fiducial1MatchOnly) {
        boolean hasMatch = false;
        for (BlindsFeeder feeder : getConnectedFeedersByLocation(location, fiducial1MatchOnly)) {
            if (feeder != this) {
                updateFromConnectedFeeder(feeder);
                hasMatch = true;
                break;
            }
        }
        if (! nullLocation.equals(fiducial1Location)) {
            // Now that we have the (partial) coordinate system we can calculate the tape pocket centerline.
            Location feederLocation = transformMachineToFeederLocation(location);
            double mm = new Length(1, LengthUnit.Millimeters).convertToUnits(feederLocation.getUnits()).getValue();
            // Take the nearest integer Millimeter value. 
            this.setPocketCenterline(new Length(Math.round(feederLocation.getY()/mm)*mm, feederLocation.getUnits()));
        }
        updateTapeNumbering();
        return hasMatch;
    }

    public void updateConnectedFeedersFromThis(Location location, boolean fiducial1MatchOnly) {
        if (! isUpdating) {
            isUpdating = true;
            // Transform might have changed.
            updateFeederToMachineTransform();
            // Update all the feeders on the same 3D printed holder from this.
            for (BlindsFeeder feeder : getConnectedFeedersByLocation(location, fiducial1MatchOnly)) {
                if (feeder != this) {
                    feeder.updateFromConnectedFeeder(this);
                }
            }
            isUpdating = false;
        }
    }
    public void updateConnectedFeedersFromThis() {
        updateConnectedFeedersFromThis(fiducial1Location, true);
    }

    public CvPipeline getPipeline() {
        return pipeline;
    }

    public void resetPipeline() {
        pipeline = createDefaultPipeline();
    }

    public CvPipeline getCvPipeline(Camera camera, boolean clone) {
        try {
            CvPipeline pipeline = getPipeline();;
            if (clone) {
                pipeline = pipeline.clone();
            }
            pipeline.setProperty("camera", camera);
            pipeline.setProperty("feeder", this);
            /* this won't work, properties must be hardwired in the stages
             * 
             * // Provide pixel min/max area to pipeline.
            // We restrict this to 24mm tape carrier having a 20.1mm max pocket size.
            // See ANSI/EIA-48 1 -C p. 11.
            double mm = VisionUtils.toPixels(new Length(1, LengthUnit.Millimeters), camera);
            Integer minArea = (int) (0.8*mm*1*mm); // 2mm pitch punched paper carrier tape (0402). 
            Integer maxArea = (int) (21*mm*21*mm); // 24mm tape carrier.
            // feeder specific
            double nominalArea = VisionUtils.toPixels(feeder.getPocketPitch(), camera)
             *VisionUtils.toPixels(feeder.getPocketSize(), camera);
            Integer minArea = (int) (0.75*nominalArea); 
            Integer maxArea = (int) (1.25*nominalArea); 
            pipeline.setProperty("FilterContours.minArea", minArea);
            pipeline.setProperty("FilterContours.maxArea", maxArea);
             */
            return pipeline;
        }
        catch (CloneNotSupportedException e) {
            throw new Error(e);
        }
    }



    public boolean isCalibrating() {
        return calibrating;
    }

    private void setCalibrating(boolean calibrating) {
        this.calibrating = calibrating;
    }

    public boolean isCalibrated() {
        return calibrated;
    }

    private void setCalibrated(boolean calibrated) {
        boolean oldValue = this.calibrated;
        this.calibrated = calibrated;
        if (oldValue != calibrated) {
            this.updateConnectedFeedersFromThis();
            firePropertyChange("calibrated", oldValue, calibrated);
        }
    }

    public Location getFiducial1Location() {
        return fiducial1Location;
    }

    public void setFiducial1Location(Location fiducial1Location) {
        Location oldValue = this.fiducial1Location;
        this.fiducial1Location = fiducial1Location;
        if (! oldValue.equals(fiducial1Location)) {
            this.invalidateFeederTransformation();
            this.updateConnectedFeedersFromThis(oldValue, true);
            firePropertyChange("fiducial1Location", oldValue, fiducial1Location);
            if (oldValue.equals(nullLocation) 
                    && fiducial2Location.equals(nullLocation)
                    && fiducial3Location.equals(nullLocation)) {
                // That's an initial fix. Try to clone from another feeder.
                updateFromConnectedFeeder(fiducial1Location, true);
            }
            else if ((! oldValue.equals(nullLocation))
                    && oldValue.convertToUnits(LengthUnit.Millimeters).getLinearDistanceTo(fiducial1Location) > 4) {
                // Large change in fiducial 1 location - move fiducials 2 and 3 as well (i.e. move the whole feeder).
                if (! fiducial2Location.equals(nullLocation)) {
                    setFiducial2Location(fiducial2Location.add(fiducial1Location.subtract(oldValue)));
                }
                if (! fiducial3Location.equals(nullLocation)) {
                    setFiducial3Location(fiducial3Location.add(fiducial1Location.subtract(oldValue)));
                }
            }
        }
    }


    public Location getFiducial2Location() {
        return fiducial2Location;
    }


    public void setFiducial2Location(Location fiducial2Location) {
        Location oldValue = this.fiducial2Location;
        this.fiducial2Location = fiducial2Location;
        if (! oldValue.equals(fiducial2Location)) {
            this.invalidateFeederTransformation();
            this.updateConnectedFeedersFromThis();
            firePropertyChange("fiducial2Location", oldValue, fiducial2Location);
        }
    }


    public Location getFiducial3Location() {
        return fiducial3Location;
    }


    public void setFiducial3Location(Location fiducial3Location) {
        Location oldValue = this.fiducial3Location;
        this.fiducial3Location = fiducial3Location;
        if (! oldValue.equals(fiducial3Location)) {
            this.invalidateFeederTransformation();
            this.updateConnectedFeedersFromThis();
            firePropertyChange("fiducial3Location", oldValue, fiducial3Location);
        }
    }


    public boolean isNormalize() {
        return normalize;
    }


    public void setNormalize(boolean normalize) {
        boolean oldValue = this.normalize;
        this.normalize = normalize;
        if (oldValue != normalize) {
            this.invalidateFeederTransformation();
            this.updateConnectedFeedersFromThis();
            firePropertyChange("normalize", oldValue, normalize);
        }
    }


    public Length getTapeLength() {
        return tapeLength;
    }


    public void setTapeLength(Length tapeLength) {
        Length oldValue = this.tapeLength;
        this.tapeLength = tapeLength;
        if (! oldValue.equals(tapeLength)) {
            this.updateConnectedFeedersFromThis();
            firePropertyChange("tapeLength", oldValue, tapeLength);
        }
    }


    public Length getFeederExtent() {
        return feederExtent;
    }


    public void setFeederExtent(Length feederExtent) {
        Length oldValue = this.feederExtent;
        this.feederExtent = feederExtent;
        if (! oldValue.equals(feederExtent)) {
            this.updateConnectedFeedersFromThis();
            firePropertyChange("feederExtent", oldValue, feederExtent);
        }
    }


    public Length getPocketCenterline() {
        return pocketCenterline;
    }


    public void setPocketCenterline(Length pocketCenterline) {
        Length oldValue = this.pocketCenterline;
        this.pocketCenterline = pocketCenterline;
        firePropertyChange("pocketCenterline", oldValue, pocketCenterline);
        updateTapeNumbering();
    }


    public Length getPocketPitch() {
        return pocketPitch;
    }


    public void setPocketPitch(Length pocketPitch) {
        Length oldValue = this.pocketPitch;
        this.pocketPitch = pocketPitch;
        firePropertyChange("pocketPitch", oldValue, pocketPitch);
    }


    public Length getPocketSize() {
        return pocketSize;
    }


    public void setPocketSize(Length pocketSize) {
        Length oldValue = this.pocketSize;
        this.pocketSize = pocketSize;
        firePropertyChange("pocketSize", oldValue, pocketSize);
    }

    public int getFeedCount() {
        return feedCount;
    }

    public void setFeedCount(int feedCount) {
        int oldValue = this.feedCount;
        this.feedCount = feedCount;
        //this.visionOffsets = null;
        firePropertyChange("feedCount", oldValue, feedCount);
    }

    public boolean isVisionEnabled() {
        return visionEnabled;
    }

    public void setVisionEnabled(boolean visionEnabled) {
        boolean oldValue = this.visionEnabled;
        this.visionEnabled = visionEnabled;
        firePropertyChange("visionEnabled", oldValue, visionEnabled);
    }


    public int getFeederNo() {
        return feederNo;
    }


    public void setFeederNo(int feederNo) {
        int oldValue = this.feederNo;
        this.feederNo = feederNo;
        firePropertyChange("feederNo", oldValue, feederNo);
    }


    public int getFeedersTotal() {
        return feedersTotal;
    }


    public void setFeedersTotal(int feedersTotal) {
        int oldValue = this.feedersTotal;
        this.feedersTotal = feedersTotal;
        firePropertyChange("feedersTotal", oldValue, feedersTotal);
    }


    @Override
    public String toString() {
        return getName();
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new BlindsFeederConfigurationWizard(this);
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
        return null;
    }

    private static CvPipeline createDefaultPipeline() {
        try {
            String xml = IOUtils.toString(BlindsFeeder.class
                    .getResource("BlindsFeeder-DefaultPipeline.xml"));
            return new CvPipeline(xml);
        }
        catch (Exception e) {
            throw new Error(e);
        }
    }
}

