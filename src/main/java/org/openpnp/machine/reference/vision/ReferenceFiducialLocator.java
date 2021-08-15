package org.openpnp.machine.reference.vision;
import org.I18n.I18n;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
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
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.vision.wizards.ReferenceFiducialLocatorConfigurationWizard;
import org.openpnp.machine.reference.vision.wizards.ReferenceFiducialLocatorPartConfigurationWizard;
import org.openpnp.model.Board;
import org.openpnp.model.Board.Side;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Footprint;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Panel;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.model.Placement.Type;
import org.openpnp.model.Point;
import org.openpnp.spi.Camera;
import org.openpnp.spi.FiducialLocator;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.IdentifiableList;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.QuickHull;
import org.openpnp.util.TravellingSalesman;
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
    
    @Element(required = false)
    protected Length maxDistance = new Length(4, LengthUnit.Millimeters);

    @Element(required = false)
    protected FiducialLocatorTolerances tolerances = new FiducialLocatorTolerances();
    
    public static class FiducialLocatorTolerances {
        protected double scalingTolerance = 0.05; //unitless
        protected double shearingTolerance = 0.05; //unitless
        protected Length boardLocationTolerance = new Length(5.0, LengthUnit.Millimeters);
    }
    
    public Location locateBoard(BoardLocation boardLocation) throws Exception {
        return locateBoard(boardLocation, false);
    }
    
    public Location locateBoard(BoardLocation boardLocation, boolean checkPanel) throws Exception {
        List<Placement> fiducials;

        Side boardSide = boardLocation.getSide();  // save for later
        Location savedBoardLocation = boardLocation.getLocation();
        AffineTransform savedPlacementTransform = boardLocation.getPlacementTransform();
       
        if (checkPanel) {
            Panel panel = MainFrame.get().getJobTab().getJob().getPanels()
                    .get(boardLocation.getPanelId());
            fiducials = panel.getFiducials();
            // If we are looking for panel fiducials, we need to treat the board as top side
            boardLocation.setSide(Side.Top);
        }
        else {
            fiducials = getFiducials(boardLocation);
        }

        if (fiducials.size() < 2) {
            throw new Exception(String.format(
                    "The board side contains only %d placements marked as fiducials, but at least 2 are required.",
                    fiducials.size()));
        }

        // Clear the current transform so it doesn't potentially send us to the wrong spot
        // to find the fiducials.
        boardLocation.setPlacementTransform(null);

        //Define where the fiducial trip will begin
        Location currentCameraLocation = new Location(LengthUnit.Millimeters);
        try {
            currentCameraLocation = MainFrame.get().getMachineControls().getSelectedTool().getHead().getDefaultCamera().getLocation();
        } catch (Exception e) {
            currentCameraLocation = boardLocation.getLocation();
        }
        
        // Use a traveling salesman algorithm to optimize the path to visit the fiducials
        TravellingSalesman<Placement> tsm = new TravellingSalesman<>(
                fiducials, 
                new TravellingSalesman.Locator<Placement>() { 
                    @Override
                    public Location getLocation(Placement locatable) {
                        return Utils2D.calculateBoardPlacementLocation(boardLocation, locatable.getLocation());
                    }
                }, 
                // start from current camera location
                currentCameraLocation,
                // and end at the board origin
                boardLocation.getLocation());

        // Solve it using the default heuristics.
        tsm.solve();

        // Visit each fiducial and store its expected and measured location
        List<Location> expectedLocations = new ArrayList<>();
        List<Location> measuredLocations = new ArrayList<>();
        for (Placement fiducial : tsm.getTravel()) {
            Location measuredLocation = getFiducialLocation(boardLocation, fiducial);
            if (measuredLocation == null) {
                throw new Exception(I18n.gettext("Unable to locate ") + fiducial.getId());
            }
            expectedLocations.add(fiducial.getLocation().invert(boardSide==Side.Bottom, false, false, false));
            measuredLocations.add(measuredLocation);
            
            Logger.debug("Found {} at {}", fiducial.getId(), measuredLocation);
        }
        
        // Calculate the transform.
        AffineTransform tx = Utils2D.deriveAffineTransform(expectedLocations, measuredLocations);
        
        // Set the transform.
        boardLocation.setPlacementTransform(tx);
        
        // Return the compensated board location
        Location origin = new Location(LengthUnit.Millimeters);
        if (boardLocation.getSide() == Side.Bottom) {
            origin = origin.add(boardLocation.getBoard().getDimensions().derive(null, 0., 0., 0.));
        }
        Location newBoardLocation = Utils2D.calculateBoardPlacementLocation(boardLocation, origin);
        newBoardLocation = newBoardLocation.convertToUnits(boardLocation.getLocation().getUnits());
        newBoardLocation = newBoardLocation.derive(null, null, boardLocation.getLocation().getZ(), null);

        if (checkPanel) {
            boardLocation.setSide(boardSide);	// restore side
        }
        
        Utils2D.AffineInfo ai = Utils2D.affineInfo(tx);
        Logger.info("Fiducial results: " + ai);
        
        double boardOffset = newBoardLocation.getLinearLengthTo(savedBoardLocation).convertToUnits(LengthUnit.Millimeters).getValue();
        Logger.info("Board origin offset distance: " + boardOffset + "mm");
        
        //Check for out-of-nominal conditions
        String errString = "";
        if (Math.abs(ai.xScale-1) > tolerances.scalingTolerance) {
            errString += "x scaling = " + String.format("%.5f", ai.xScale) + " which is outside the expected range of [" +
                    String.format("%.5f", 1-tolerances.scalingTolerance) + ", " + String.format("%.5f", 1+tolerances.scalingTolerance) + "], ";
        }
        if (Math.abs(ai.yScale-1) > tolerances.scalingTolerance) {
            errString += "the y scaling = " + String.format("%.5f", ai.yScale) + " which is outside the expected range of [" +
                    String.format("%.5f", 1-tolerances.scalingTolerance) + ", " + String.format("%.5f", 1+tolerances.scalingTolerance) + "], ";
        }
        if (Math.abs(ai.xShear) > tolerances.shearingTolerance) {
            errString += "the x shearing = " + String.format("%.5f", ai.xShear) + " which is outside the expected range of [" +
                    String.format("%.5f", -tolerances.shearingTolerance) + ", " + String.format("%.5f", tolerances.shearingTolerance) + "], ";
        }
        if (boardOffset > tolerances.boardLocationTolerance.convertToUnits(LengthUnit.Millimeters).getValue()) {
            errString += "the board origin moved " + String.format("%.4f", boardOffset) +
                    "mm which is greater than the allowed amount of " +
                    String.format("%.4f", tolerances.boardLocationTolerance.convertToUnits(LengthUnit.Millimeters).getValue()) + "mm, ";
        }
        if (errString.length() > 0) {
            errString = errString.substring(0, errString.length()-2); //strip off the last comma and space
            boardLocation.setPlacementTransform(savedPlacementTransform);
            throw new Exception("Fiducial locator results are invalid because: " + errString + I18n.gettext(".  Potential remidies include ") +
                    "setting the initial board X, Y, Z, and Rotation in the Boards panel; using a different set of fiducials; " +
                    "or changing the allowable tolerances in the <tolerances> section of the fiducial-locator section in machine.xml.");
        }

        return newBoardLocation;
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

    public CvPipeline getFiducialPipeline(Camera camera, Part part) throws Exception {
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

        PartSettings partSettings = getPartSettings(part);
        CvPipeline pipeline = partSettings.getPipeline(); 

        pipeline.setProperty("camera", camera);
        pipeline.setProperty("part", part);
        pipeline.setProperty("package", pkg);
        pipeline.setProperty("footprint", footprint);
        Rectangle2D bounds = footprint.getPadsShape().getBounds2D();
        Length diameter = new Length(Math.max(bounds.getWidth(), bounds.getHeight()), footprint.getUnits());
        pipeline.setProperty("fiducial.diameter", diameter);
        pipeline.setProperty("fiducial.maxDistance", getMaxDistance());
        return pipeline;
    }

    private Location getFiducialLocation(Location nominalLocation, Part part) throws Exception {
        Location location = nominalLocation;
        Camera camera = Configuration.get().getMachine().getDefaultHead().getDefaultCamera();

        int repeatFiducialRecognition = 3;
        if ( this.repeatFiducialRecognition > 3 ) {
            repeatFiducialRecognition = this.repeatFiducialRecognition;
        }

        Logger.debug("Looking for {} at {}", part.getId(), location);
        MovableUtils.moveToLocationAtSafeZ(camera, location);

        List<Location> matchedLocations = new ArrayList<Location>();

        try(CvPipeline pipeline = getFiducialPipeline(camera, part)) {
            for (int i = 0; i < repeatFiducialRecognition; i++) {
                // Perform vision operation
                pipeline.process();

                // Get the results
                List<KeyPoint> keypoints = pipeline.getExpectedResult(VisionUtils.PIPELINE_RESULTS_NAME)
                        .getExpectedListModel(KeyPoint.class, 
                                new Exception(part.getId()+" no matches found."));

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

                MainFrame frame = MainFrame.get(); 
                if (frame != null) {
                    CameraView cameraView = frame.getCameraViews().getCameraView(camera);
                    if (cameraView != null) {    
                        LengthConverter lengthConverter = new LengthConverter();
                        cameraView.showFilteredImage(OpenCvUtils.toBufferedImage(pipeline.getWorkingImage()), 
                                lengthConverter.convertForward(location.getLengthX())+", "
                                        +lengthConverter.convertForward(location.getLengthY())+" "
                                        +location.getUnits().getShortName(),
                                1500);
                    }
                }

                Logger.debug("{} located at {}", part.getId(), location);
                // Move to where we actually found the fid
                camera.moveTo(location);
    
                if (i > 0) {
                	//to average, keep a list of all matches except the first, since its probably most off
                	matchedLocations.add(location);
                }
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
        if (location.convertToUnits(maxDistance.getUnits()).getLinearDistanceTo(nominalLocation) > maxDistance.getValue()) {
            throw new Exception("Fiducial "+part.getName()+" detected too far away.");
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

    public Length getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(Length maxDistance) {
        this.maxDistance = maxDistance;
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
        return I18n.gettext("Fiducal Locator");
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
