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
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.feeder.wizards.BlindsFeederConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Point;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.Utils2D;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
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

    @Override
    public Location getPickLocation() throws Exception {
        int feedCount = this.feedCount;

        Location l = new Location(LengthUnit.Millimeters,
                feedCount*this.pocketPitch.getValue()*feedCount, this.pocketCenterline.getValue(), 0, 0);

        return l;
    }


    public void feed(Nozzle nozzle) throws Exception {
        setFeedCount(getFeedCount() + 1);
    }

    /*private Location findClosestHole(Camera camera) throws Exception {
        try (CvPipeline pipeline = getPipeline()) {
            Integer pxMinDistance = (int) VisionUtils.toPixels(getHolePitchMin(), camera);
            Integer pxMinDiameter = (int) VisionUtils.toPixels(getHoleDiameterMin(), camera);
            Integer pxMaxDiameter = (int) VisionUtils.toPixels(getHoleDiameterMax(), camera);

            // Process the pipeline to clean up the image and detect the tape holes
            pipeline.setProperty("camera", camera);
            pipeline.setProperty("feeder", this);
            pipeline.setProperty("DetectFixedCirclesHough.minDistance", pxMinDistance);
            pipeline.setProperty("DetectFixedCirclesHough.minDiameter", pxMinDiameter);
            pipeline.setProperty("DetectFixedCirclesHough.maxDiameter", pxMaxDiameter);
            pipeline.process();

            try {
                MainFrame.get().getCameraViews().getCameraView(camera)
                        .showFilteredImage(OpenCvUtils.toBufferedImage(pipeline.getWorkingImage()), 250);
            }
            catch (Exception e) {
                // if we aren't running in the UI this will fail, and that's okay
            }

            // Grab the results
            Object result = null;
            List<CvStage.Result.Circle> results = null;
            try {
                result = pipeline.getResult(VisionUtils.PIPELINE_RESULTS_NAME).model;            
                results = (List<CvStage.Result.Circle>) result;
            }
            catch (ClassCastException e) {
                throw new Exception("Unrecognized result type (should be Circles): " + result);
            }
            if (results.isEmpty()) {
                throw new Exception("Feeder " + getName() + ": No tape holes found.");
            }

            // Find the closest result
            results.sort((a, b) -> {
                Double da = VisionUtils.getPixelLocation(camera, a.x, a.y)
                        .getLinearDistanceTo(camera.getLocation());
                Double db = VisionUtils.getPixelLocation(camera, b.x, b.y)
                        .getLinearDistanceTo(camera.getLocation());
                return da.compareTo(db);
            });

            CvStage.Result.Circle closestResult = results.get(0);
            Location holeLocation = VisionUtils.getPixelLocation(camera, closestResult.x, closestResult.y);
            return holeLocation;
        }
    }
     */

    private AffineTransform tx;
    private AffineTransform txInverse;

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
        return true;
    }

    AffineTransform getFeederToMachineTransform() {
        // Create the transformation from the fiducials. 
        if (tx == null) {
            updateFeederToMachineTransform();
        }
        return tx;
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
        feederLocation = feederLocation.convertToUnits(fiducial1Location.getUnits()); 
        Point2D.Double ptDst = new Point2D.Double();
        tx.transform(new Point2D.Double(feederLocation.getX(), feederLocation.getY()), ptDst);
        return new Location(fiducial1Location.getUnits(), ptDst.getX(), ptDst.getY(), 0, 0);
    }

    public Location transformMachineToFeederLocation(Location machineLocation) {
        AffineTransform tx = getMachineToFeederTransform();
        machineLocation = machineLocation.convertToUnits(fiducial1Location.getUnits()); 
        Point2D.Double ptDst = new Point2D.Double();
        tx.transform(new Point2D.Double(machineLocation.getX(), machineLocation.getY()), ptDst);
        return new Location(fiducial1Location.getUnits(), ptDst.getX(), ptDst.getY(), 0, 0);
    }

    public double getFeederToMachineAngle() {
        // Reconstruct the angle at which the feeder lays on the machine. When "normalize" is off, it is only
        // an approximation valid for lines parallel to the tapes (as is most useful). It ignores shear in the
        // transform.
        AffineTransform tx = getFeederToMachineTransform();
        Point2D.Double ptOrigin = new Point2D.Double();
        Point2D.Double ptRadius = new Point2D.Double();
        tx.transform(new Point2D.Double(0, 0), ptOrigin);
        tx.transform(new Point2D.Double(1, 0), ptRadius);
        double norm = ptRadius.distance(ptOrigin);
        double angle = Math.acos((ptRadius.getX() - ptOrigin.getX())/norm);
        if ((ptRadius.getY() - ptOrigin.getY()) < 0) {
            angle += Math.PI;
        }
        // All angles in OpenPNP seem to be in degrees.
        return angle*180/Math.PI;
    }
    public double transformFeederToMachineAngle(double angle) {
        return angle+getFeederToMachineAngle();
    }
    public double transformMachineToFeederAngle(double angle) {
        // We don't need th reverse transform, a simple sign reversion will do.
        return angle-getFeederToMachineAngle();
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
                return new Double(feeder1.getPocketCenterline().convertToUnits(LengthUnit.Millimeters).getValue())
                        .compareTo(feeder2.getPocketCenterline().convertToUnits(LengthUnit.Millimeters).getValue());
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

    public Location getFiducial1Location() {
        return fiducial1Location;
    }


    public void setFiducial1Location(Location fiducial1Location) {
        Location oldValue = this.fiducial1Location;
        this.fiducial1Location = fiducial1Location;
        if (! oldValue.equals(fiducial1Location)) {
            this.updateConnectedFeedersFromThis(oldValue, true);
            firePropertyChange("fiducial1Location", oldValue, fiducial1Location);
            if ((! oldValue.equals(nullLocation))
                    && (! fiducial1Location.equals(nullLocation))
                    && oldValue.convertToUnits(LengthUnit.Millimeters).getLinearDistanceTo(fiducial1Location) > 4) {
                // Large change in fiducial 1 location - move fiducials 2 and 3 as well (i.e. move the whole feeder).
                // NOTE the setters will in turn notify and update all the other feeders in the same holder.
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

