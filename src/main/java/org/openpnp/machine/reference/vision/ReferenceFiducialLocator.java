package org.openpnp.machine.reference.vision;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Action;
import javax.swing.Icon;

import org.apache.commons.io.IOUtils;
import org.opencv.core.KeyPoint;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.vision.wizards.ReferenceFiducialLocatorConfigurationWizard;
import org.openpnp.machine.reference.vision.wizards.ReferenceFiducialLocatorPartConfigurationWizard;
import org.openpnp.model.Board;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Footprint;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Panel;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.model.Placement.Type;
import org.openpnp.model.Point;
import org.openpnp.model.Board.Side;
import org.openpnp.spi.Camera;
import org.openpnp.spi.FiducialLocator;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.IdentifiableList;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.QuickHull;
import org.openpnp.util.Utils2D;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Root;

import com.google.common.collect.Sets;

/**
 * Implements an algorithm for finding a set of fiducials on a board and returning the correct
 * orientation for the board.
 */
@Root
public class ReferenceFiducialLocator implements FiducialLocator {
    @Element(required = false)
    protected CvPipeline pipeline = createDefaultPipeline();

    @ElementMap(required = false)
    protected Map<String, PartSettings> partSettingsByPartId = new HashMap<>();

    @Attribute(required = false)
    protected boolean enabledAveraging = false;
    
    @Attribute(required = false)
    protected int repeatFiducialRecognition = 3;
    
    public Location locateBoard(BoardLocation boardLocation) throws Exception {
        return locateBoard(boardLocation, false);
    }
    
    public Location locateBoard(BoardLocation boardLocation, boolean checkPanel) throws Exception {
        List<Placement> fiducials;

        if (checkPanel) {
            Panel panel = MainFrame.get().getJobTab().getJob().getPanels()
                    .get(boardLocation.getPanelId());
            fiducials = panel.getFiducials();
        }
        else {
            fiducials = getFiducials(boardLocation);
        }

        if (fiducials.size() < 2) {
            throw new Exception(String.format(
                    "The board side contains only %d placements marked as fiducials, but at least 2 are required.",
                    fiducials.size()));
        }

        // Get the best two or three fiducials from the total list of fiducials. If we can find
        // three good ones we can calculate translation, rotation, scale, and shear. If we only
        // find two we can calculate translation and rotation only.
        fiducials = getBestFiducials(fiducials);

        // Clear the current transform so it doesn't potentially send us to the wrong spot
        // to find the fiducials.
        boardLocation.setPlacementTransform(null);

        // Sort the fiducials by distance so that we don't make unoptimized moves while finding
        // them.
        fiducials.sort(new Comparator<Placement>() {
            @Override
            public int compare(Placement o1, Placement o2) {
                Location base = new Location(LengthUnit.Millimeters);
                double delta = base.getLinearDistanceTo(o1.getLocation()) - base.getLinearDistanceTo(o2.getLocation());
                return (int) Math.signum(delta);
            }
        });
        
        // Find each fiducial and store it's location
        Map<Placement, Location> locations = new HashMap<>();
        for (Placement fiducial : fiducials) {
            Location location = getFiducialLocation(boardLocation, fiducial);
            if (location == null) {
                throw new Exception("Unable to locate " + fiducial.getId());
            }
            locations.put(fiducial, location);
            Logger.debug("Found {} at {}", fiducial, location);
        }
        
        // Convert everything to mm.
        List<Location> sourceLocations = new ArrayList<>();
        List<Location> destLocations = new ArrayList<>();
        for (Placement placement : locations.keySet()) {
            sourceLocations.add(placement.getLocation().convertToUnits(LengthUnit.Millimeters));
            destLocations.add(locations.get(placement).convertToUnits(LengthUnit.Millimeters));
        }
        
        // Calculate the transform.
        AffineTransform tx = null;
        if (destLocations.size() == 2) {
            Location source0 = sourceLocations.get(0);
            Location source1 = sourceLocations.get(1);
            Location dest0 = destLocations.get(0);
            Location dest1 = destLocations.get(1);
            if (boardLocation.getSide() == Side.Bottom) {
                tx = Utils2D.deriveAffineTransform(
                        -source0.getX(), source0.getY(), 
                        -source1.getX(), source1.getY(), 
                        dest0.getX(), dest0.getY(),
                        dest1.getX(), dest1.getY());
            }
            else {
                tx = Utils2D.deriveAffineTransform(
                        source0.getX(), source0.getY(), 
                        source1.getX(), source1.getY(), 
                        dest0.getX(), dest0.getY(),
                        dest1.getX(), dest1.getY());
            }
        }
        else if (destLocations.size() == 3) {
            Location source0 = sourceLocations.get(0);
            Location source1 = sourceLocations.get(1);
            Location source2 = sourceLocations.get(2);
            Location dest0 = destLocations.get(0);
            Location dest1 = destLocations.get(1);
            Location dest2 = destLocations.get(2);
            if (boardLocation.getSide() == Side.Bottom) {
                tx = Utils2D.deriveAffineTransform(
                        -source0.getX(), source0.getY(), 
                        -source1.getX(), source1.getY(), 
                        -source2.getX(), source2.getY(),
                        dest0.getX(), dest0.getY(),
                        dest1.getX(), dest1.getY(),
                        dest2.getX(), dest2.getY());
            }
            else {
                tx = Utils2D.deriveAffineTransform(
                        source0.getX(), source0.getY(), 
                        source1.getX(), source1.getY(), 
                        source2.getX(), source2.getY(),
                        dest0.getX(), dest0.getY(),
                        dest1.getX(), dest1.getY(),
                        dest2.getX(), dest2.getY());
            }
        }
        else {
            throw new Exception(String.format("Expected 2 or 3 fiducial results, not %d. This is a programmer error. Please tell a programmer.",
                    destLocations.size()));
        }
        
        // Set the transform.
        boardLocation.setPlacementTransform(tx);
        Logger.info("Fiducial results: scale ({}, {}), translate ({}, {}), shear ({}, {})",
                tx.getScaleX(), tx.getScaleY(),
                tx.getTranslateX(), tx.getTranslateY(),
                tx.getShearX(), tx.getShearY());
        
        // TODO STOPSHIP Check if the results make sense and throw an error if they don't.
        // Probably need to let the user specify some limits.
        
        // Return the compensated board location
        Location result = Utils2D.calculateBoardPlacementLocation(boardLocation, new Location(LengthUnit.Millimeters));
        result = result.convertToUnits(boardLocation.getLocation().getUnits());
        result = result.derive(null, null, boardLocation.getLocation().getZ(), null);
        return result;
    }
    
    /**
     * Gets the best fiducials from the given list. If there are at least three fiducials that are
     * non-colinear, the three that are most distant from one another will be returned.
     * Otherwise the two most distant ones will be returned. If there are less than three
     * fiducials in the list altogether then the list is returned unchanged.
     * 
     * Note that given the above rules, the returned list can contain 0, 1, 2, or 3 fiducials. No
     * other number of results will be returned.
     * 
     * @param fiducials
     * @return
     */
    public static List<Placement> getBestFiducials(List<Placement> fiducials) {
        // If there are less than three fiducials there's nothing we can do.
        if (fiducials.size() < 3) {
            return fiducials;
        }

        // Get the convex hull set, which represents the outer bounds of all
        // the points. This is primarily an optimization to cut down on the number
        // of checks we need to perform below.
        try {
            // quickHull requires Points, and we have Placements, so we need to convert the
            // Placements to Points and we also need to be able to map the results back
            // to Placements when it's finished. So, we create a map, pass the keys
            // and then unmap it when it's done.
            Map<Point, Placement> pointsToPlacements = new HashMap<>();
            for (Placement placement : fiducials) {
                Point point = placement.getLocation()
                                       .convertToUnits(LengthUnit.Millimeters)
                                       .getXyPoint();
                pointsToPlacements.put(point, placement);
            }
            List<Point> points = QuickHull.quickHull(new ArrayList<>(pointsToPlacements.keySet()));
            fiducials = new ArrayList<>();
            for (Point point : points) {
                fiducials.add(pointsToPlacements.get(point));
            }
        }
        catch (Exception e) {
            // Quick Hull will fail if all of the points share an X coordinate. Knowing this,
            // we don't bother to check ahead of time and just handle it here. In this case
            // there is no point continuing, so we just return the two most distant points.
            return Utils2D.mostDistantPair(fiducials);
        }

        // Now, for each set of 3 unique points in the list of points, calculate the area of
        // the triangle. The largest is our answer.
        Placement[] bestPoints = null;
        double bestArea = 0;
        for (Set<Placement> tri : Sets.powerSet(Sets.newHashSet(fiducials))) {
            if (tri.size() != 3) {
                continue;
            }
            Placement[] triPoints = tri.toArray(new Placement[] {});
            double a = Utils2D.triangleArea(triPoints[0], triPoints[1], triPoints[2]);
            if (bestPoints == null || a > bestArea) {
                bestPoints = triPoints;
                bestArea = a;
            }
        }

        // If the best area is 0 then all the triangles were degenerate / collinear. In this case
        // the three points are not useful and we just return the two most distant.
        if (bestArea == 0) {
            return Utils2D.mostDistantPair(fiducials);
        }

        return Arrays.asList(bestPoints);
    }
    
    /**
     * Given a placement containing a fiducial, attempt to find the fiducial using the vision
     * system. The function first moves the camera to the ideal location of the fiducial based on
     * the board location. It then performs a template match against a template generated from the
     * fiducial's footprint. These steps are performed thrice to "home in" on the fiducial. Finally,
     * the location is returned. If the fiducial was not able to be located with any degree of
     * certainty the function returns null.
     *
     * @param location, part
     * @return
     * @throws Exception
     */
    public Location getHomeFiducialLocation(Location location, Part part) throws Exception {
        return getFiducialLocation(location, part);
    }

    /**
     * Given a placement containing a fiducial, attempt to find the fiducial using the vision
     * system. The function first moves the camera to the ideal location of the fiducial based on
     * the board location. It then performs a template match against a template generated from the
     * fiducial's footprint. These steps are performed thrice to "home in" on the fiducial. Finally,
     * the location is returned. If the fiducial was not able to be located with any degree of
     * certainty the function returns null.
     * 
     * @param fid
     * @return
     * @throws Exception
     */
    private Location getFiducialLocation(BoardLocation boardLocation, Placement fid)
            throws Exception {
        Logger.debug("Locating {}", fid.getId());

        Part part = fid.getPart();
        if (part == null) {
            throw new Exception(
                    String.format("Fiducial %s does not have a valid part assigned.", fid.getId()));
        }

        Location location =
                Utils2D.calculateBoardPlacementLocation(boardLocation, fid.getLocation());

        return getFiducialLocation(location, part);
    }
    
    private Location getFiducialLocation(Location location, Part part) throws Exception {
        Camera camera = Configuration.get().getMachine().getDefaultHead().getDefaultCamera();

        org.openpnp.model.Package pkg = part.getPackage();
        if (pkg == null) {
            throw new Exception(
                    String.format("Part %s does not have a valid package assigned.", part.getId()));
        }

        Footprint footprint = pkg.getFootprint();
        if (footprint == null) {
            throw new Exception(String.format(
                    "Package %s does not have a valid footprint. See https://github.com/openpnp/openpnp/wiki/Fiducials.",
                    pkg.getId()));
        }

        if (footprint.getShape() == null) {
            throw new Exception(String.format(
                    "Package %s has an invalid or empty footprint.  See https://github.com/openpnp/openpnp/wiki/Fiducials.",
                    pkg.getId()));
        }
        
        int repeatFiducialRecognition = 3;
        if ( this.repeatFiducialRecognition > 3 ) {
        	repeatFiducialRecognition = this.repeatFiducialRecognition;
        }

        Logger.debug("Looking for {} at {}", part.getId(), location);
        MovableUtils.moveToLocationAtSafeZ(camera, location);

        PartSettings partSettings = getPartSettings(part);
        List<Location> matchedLocations = new ArrayList<Location>();
        
        try (CvPipeline pipeline = partSettings.getPipeline()) {
            MovableUtils.moveToLocationAtSafeZ(camera, location);

            pipeline.setProperty("camera", camera);
            pipeline.setProperty("part", part);
            pipeline.setProperty("package", pkg);
            pipeline.setProperty("footprint", footprint);
            
            for (int i = 0; i < repeatFiducialRecognition; i++) {
                List<KeyPoint> keypoints;
                try {
                    // Perform vision operation
                    pipeline.process();
                    
                    // Get the results
                    keypoints = (List<KeyPoint>) pipeline.getResult(VisionUtils.PIPELINE_RESULTS_NAME).getModel();
                }
                catch (Exception e) {
                    Logger.debug(e);
                    return null;
                }
                
                if (keypoints == null || keypoints.isEmpty()) {
                    Logger.debug("No matches found!");
                    return null;
                }
                
                // Convert to Locations
                List<Location> locations = new ArrayList<Location>();
                for (KeyPoint keypoint : keypoints) {
                    locations.add(VisionUtils.getPixelLocation(camera, keypoint.pt.x, keypoint.pt.y));
                }
                
                // Sort by distance from center.
                Collections.sort(locations, new Comparator<Location>() {
                    @Override
                    public int compare(Location o1, Location o2) {
                        double d1 = o1.getLinearDistanceTo(camera.getLocation());
                        double d2 = o2.getLinearDistanceTo(camera.getLocation());
                        return Double.compare(d1, d2);
                    }
                });
                
                // And use the closest result
                location = locations.get(0);
                
                Logger.debug("{} located at {}", part.getId(), location);
                // Move to where we actually found the fid
                camera.moveTo(location);
    
                if (i > 0) {
                	//to average, keep a list of all matches except the first, since its probably most off
                	matchedLocations.add(location);
                }
            
                Logger.debug("{} located at {}", part.getId(), location);
                // Move to where we actually found the fid
                camera.moveTo(location);
            }
        }
        
        if (this.enabledAveraging && matchedLocations.size() >= 2) {
            // the arithmetic average is calculated if user wishes to do so and there were at least
            // 2 matches
            double sumX = 0;
            double sumY = 0;

            for (Location matchedLocation : matchedLocations) {
                sumX += matchedLocation.getX();
                sumY += matchedLocation.getY();
            }

            // update the location to the arithmetic average
            location = location.derive(sumX / matchedLocations.size(),
                    sumY / matchedLocations.size(), null, null);

            Logger.debug("{} averaged location is at {}", part.getId(), location);

            camera.moveTo(location);
        }
        
        return location;
    }
    
    private static IdentifiableList<Placement> getFiducials(BoardLocation boardLocation) {
        Board board = boardLocation.getBoard();
        IdentifiableList<Placement> fiducials = new IdentifiableList<>();
        for (Placement placement : board.getPlacements()) {
            if (placement.getType() == Type.Fiducial
                    && placement.getSide() == boardLocation.getSide()
                    && placement.isEnabled()) {
                fiducials.add(placement);
            }
        }
        return fiducials;
    }
    
    public boolean isEnabledAveraging() {
        return enabledAveraging;
    }

    public void setEnabledAveraging(boolean enabledAveraging) {
        this.enabledAveraging = enabledAveraging;
    }

    public int getRepeatFiducialRecognition() {
    	return this.repeatFiducialRecognition;
    }
    
    public void setRepeatFiducialRecognition(int repeatFiducialRecognition) {
        this.repeatFiducialRecognition = repeatFiducialRecognition;
    }
    
    public CvPipeline getPipeline() {
        return pipeline;
    }

    public void setPipeline(CvPipeline pipeline) {
        this.pipeline = pipeline;
    }
    
    public static CvPipeline createDefaultPipeline() {
        try {
            String xml = IOUtils.toString(ReferenceBottomVision.class
                    .getResource("ReferenceFiducialLocator-DefaultPipeline.xml"));
            return new CvPipeline(xml);
        }
        catch (Exception e) {
            throw new Error(e);
        }
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return "Fiducal Locator";
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return null;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
                new PropertySheetWizardAdapter(new ReferenceFiducialLocatorConfigurationWizard(this))};
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return null;
    }

    @Override
    public Icon getPropertySheetHolderIcon() {
        return null;
    }
    
    public PartSettings getPartSettings(Part part) {
        PartSettings partSettings = this.partSettingsByPartId.get(part.getId());
        if (partSettings == null) {
            partSettings = new PartSettings(this);
            this.partSettingsByPartId.put(part.getId(), partSettings);
        }
        return partSettings;
    }

    public Map<String, PartSettings> getPartSettingsByPartId() {
        return partSettingsByPartId;
    }

    @Override
    public Wizard getPartConfigurationWizard(Part part) {
        PartSettings partSettings = getPartSettings(part);
        try {
            partSettings.getPipeline().setProperty("camera", VisionUtils.getBottomVisionCamera());
        }
        catch (Exception e) {
        }
        return new ReferenceFiducialLocatorPartConfigurationWizard(this, part);
    }

    @Root
    public static class PartSettings {
        @Attribute
        protected boolean enabled;

        @Element
        protected CvPipeline pipeline;

        public PartSettings() {

        }

        public PartSettings(ReferenceFiducialLocator fiducialLocator) {
            try {
                setPipeline(fiducialLocator.getPipeline().clone());
            }
            catch (Exception e) {
                throw new Error(e);
            }
        }

        public CvPipeline getPipeline() {
            return pipeline;
        }

        public void setPipeline(CvPipeline pipeline) {
            this.pipeline = pipeline;
        }
    }  
}
