package org.openpnp.machine.reference.vision;

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
import org.openpnp.ConfigurationListener;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.vision.wizards.FiducialVisionSettingsConfigurationWizard;
import org.openpnp.machine.reference.vision.wizards.ReferenceFiducialLocatorConfigurationWizard;
import org.openpnp.model.AbstractVisionSettings;
import org.openpnp.model.Board;
import org.openpnp.model.Board.Side;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.FiducialVisionSettings;
import org.openpnp.model.Footprint;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Panel;
import org.openpnp.model.Part;
import org.openpnp.model.PartSettingsHolder;
import org.openpnp.model.PartSettingsRoot;
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
public class ReferenceFiducialLocator extends AbstractPartSettingsHolder implements PartSettingsRoot, FiducialLocator {
    @Deprecated
    @Element(required = false)
    protected CvPipeline pipeline;

    @Deprecated
    @ElementMap(required = false)
    protected Map<String, PartSettings> partSettingsByPartId;

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

    public ReferenceFiducialLocator() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationComplete(Configuration configuration) throws Exception {
                migratePartSettings(configuration);
                if (fiducialVisionSettings == null) {
                    // Recovery mode, take any setting.
                    for (AbstractVisionSettings settings : configuration.getVisionSettings()) {
                        if (settings instanceof FiducialVisionSettings) {
                            fiducialVisionSettings = (FiducialVisionSettings) settings;
                            break;
                        }
                    }
                }
            }
        });
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
                throw new Exception("Unable to locate " + fiducial.getId());
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
            throw new Exception("Fiducial locator results are invalid because: " + errString + ".  Potential remidies include " +
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
        // Because the homing fiducial must by definition be at default Z, we supply it here,
        // so the 3D units per pixel scaling will be correct. 
        Camera camera = getVisionCamera();
        location = location.deriveLengths(null, null, camera.getDefaultZ(), null);
        return getFiducialLocation(location, part);
    }

    /**
     * @return the Camera used for fiducial location.
     * @throws Exception
     */
    public Camera getVisionCamera() throws Exception {
        return Configuration.get().getMachine().getDefaultHead().getDefaultCamera();
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

    public CvPipeline getFiducialPipeline(Camera camera, PartSettingsHolder partSettingsHolder) throws Exception {
        org.openpnp.model.Package pkg = null;
        if (partSettingsHolder instanceof Part) {
            pkg = ((Part) partSettingsHolder).getPackage();
        }
        else if (partSettingsHolder instanceof org.openpnp.model.Package) {
            pkg = (org.openpnp.model.Package) partSettingsHolder;
        }
        if (pkg == null) {
            throw new Exception(
                    String.format("%s %s does not have a valid package assigned.", partSettingsHolder.getClass().getSimpleName(), partSettingsHolder.getShortName()));
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

        FiducialVisionSettings visionSettings = getInheritedVisionSettings(partSettingsHolder);
        if (!visionSettings.isEnabled()) {
            throw new  Exception(String.format(
                    "Package %s fidcuial vision settings %s are disabled.",
                    pkg.getId(), visionSettings.getName()));
        }
        CvPipeline pipeline = visionSettings.getPipeline(); 

        pipeline.setProperty("camera", camera);
        pipeline.setProperty("part", partSettingsHolder);
        pipeline.setProperty("package", pkg);
        pipeline.setProperty("footprint", footprint);
        Rectangle2D bounds = footprint.getPadsShape().getBounds2D();
        Length diameter = new Length(Math.max(bounds.getWidth(), bounds.getHeight()), footprint.getUnits());
        pipeline.setProperty("fiducial.diameter", diameter);
        pipeline.setProperty("fiducial.maxDistance", getMaxDistance());
        return pipeline;
    }

    public Location getFiducialLocation(Location nominalLocation, PartSettingsHolder partSettingsHolder) throws Exception {
        Location location = nominalLocation;
        Camera camera = getVisionCamera();

        int repeatFiducialRecognition = 3;
        if ( this.repeatFiducialRecognition > 3 ) {
            repeatFiducialRecognition = this.repeatFiducialRecognition;
        }

        Logger.debug("Looking for {} at {}", partSettingsHolder.getShortName(), location);
        MovableUtils.moveToLocationAtSafeZ(camera, location);

        List<Location> matchedLocations = new ArrayList<Location>();

        try(CvPipeline pipeline = getFiducialPipeline(camera, partSettingsHolder)) {
            for (int i = 0; i < repeatFiducialRecognition; i++) {
                // Perform vision operation
                pipeline.process();

                // Get the results
                List<KeyPoint> keypoints = pipeline.getExpectedResult(VisionUtils.PIPELINE_RESULTS_NAME)
                        .getExpectedListModel(KeyPoint.class, 
                                new Exception(partSettingsHolder.getId()+" no matches found."));

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

                Logger.debug("{} located at {}", partSettingsHolder.getId(), location);
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

            Logger.debug("{} averaged location is at {}", partSettingsHolder.getId(), location);

            camera.moveTo(location);
        }
        if (location.convertToUnits(maxDistance.getUnits()).getLinearDistanceTo(nominalLocation) > maxDistance.getValue()) {
            throw new Exception("Fiducial "+partSettingsHolder.getShortName()+" detected too far away.");
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

    @Override
    public String getId() {
        return null;
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
    
    public static CvPipeline createStockPipeline() {
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
                new PropertySheetWizardAdapter(new ReferenceFiducialLocatorConfigurationWizard(this)),
                new PropertySheetWizardAdapter(new FiducialVisionSettingsConfigurationWizard(getFiducialVisionSettings(), this))};
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return null;
    }

    @Override
    public Icon getPropertySheetHolderIcon() {
        return null;
    }

    @Override
    public PartSettingsHolder getParentHolder(PartSettingsHolder partSettingsHolder) {
        if (partSettingsHolder instanceof Part) {
            return ((Part) partSettingsHolder).getPackage();
        }
        else if (partSettingsHolder instanceof org.openpnp.model.Package) {
            return this;
        }
        else {
            return null;
        }
    }

    @Override 
    public FiducialVisionSettings getInheritedVisionSettings(PartSettingsHolder partSettingsHolder) {
        while (partSettingsHolder != null) {
            FiducialVisionSettings visionSettings = partSettingsHolder.getFiducialVisionSettings();
            if (visionSettings != null) {
                return visionSettings;
            }
            partSettingsHolder = getParentHolder(partSettingsHolder);
        }
        return null;
    }


    @Override
    public FiducialVisionSettings getVisionSettings(PartSettingsHolder partSettingsHolder) {
        return partSettingsHolder.getFiducialVisionSettings();
    }

    public Map<String, PartSettings> getPartSettingsByPartId() {
        return partSettingsByPartId;
    }

    @Override
    public Wizard getPartConfigurationWizard(PartSettingsHolder partSettingsHolder) {
        FiducialVisionSettings visionSettings = getInheritedVisionSettings(partSettingsHolder);
        try {
            visionSettings.getPipeline().setProperty("camera", getVisionCamera());
        }
        catch (Exception e) {
        }
        return new FiducialVisionSettingsConfigurationWizard(visionSettings, partSettingsHolder);
    }

    @Root
    @Deprecated
    public static class PartSettings {
        @Deprecated
        @Attribute
        protected boolean enabled = true;

        @Deprecated
        @Element
        protected CvPipeline pipeline;

        @Deprecated
        public PartSettings() {
        }

        @Deprecated
        public boolean isEnabled() {
            return enabled;
        }

        @Deprecated
        public CvPipeline getPipeline() {
            return pipeline;
        }

        @Deprecated
        public void setPipeline(CvPipeline pipeline) {
            this.pipeline = pipeline;
        }
    }  

    @Override
    public String getShortName() {
        return getPropertySheetHolderTitle();
    }

    public static ReferenceFiducialLocator getDefault() { 
        return (ReferenceFiducialLocator) Configuration.get().getMachine().getFiducialLocator();
    }

    protected void migratePartSettings(Configuration configuration) {
        if (partSettingsByPartId == null) {
            if (configuration.getVisionSettings(AbstractVisionSettings.STOCK_FIDUCIAL_ID) == null) {
                // Fresh configuration: need to migrate the stock and default settings, even if no partSettingsById are present.  
                partSettingsByPartId = new HashMap<>();
            }
            else { 
                return;
            }
        }

        HashMap<String, FiducialVisionSettings> fiducialVisionSettingsHashMap = new HashMap<>();
        // Create the factory stock settings.
        FiducialVisionSettings stockFiducialVisionSettings = createStockFiducialVisionSettings();
        configuration.addVisionSettings(stockFiducialVisionSettings);
        PartSettings equivalentPartSettings = new PartSettings();
        equivalentPartSettings.setPipeline(stockFiducialVisionSettings.getPipeline());
        fiducialVisionSettingsHashMap.put(AbstractVisionSettings.createSettingsFingerprint(equivalentPartSettings), stockFiducialVisionSettings);
        // Migrate the default settings.
        FiducialVisionSettings defaultFiducialVisionSettings = new FiducialVisionSettings(AbstractVisionSettings.DEFAULT_FIDUCIAL_ID);
        defaultFiducialVisionSettings.setName("- Default Machine Fiducial Locator -");
        defaultFiducialVisionSettings.setEnabled(true);
        configuration.addVisionSettings(defaultFiducialVisionSettings);
        if(pipeline != null) {
            defaultFiducialVisionSettings.setPipeline(pipeline);
            pipeline = null;
        }
        else {
            defaultFiducialVisionSettings.setPipeline(stockFiducialVisionSettings.getPipeline());
        }
        setFiducialVisionSettings(defaultFiducialVisionSettings);
        equivalentPartSettings.setPipeline(defaultFiducialVisionSettings.getPipeline());
        fiducialVisionSettingsHashMap.put(AbstractVisionSettings.createSettingsFingerprint(equivalentPartSettings), defaultFiducialVisionSettings);
        for (Part part: configuration.getParts()) {
            part.setFiducialVisionSettings(null);
        }
        for (org.openpnp.model.Package pkg : configuration.getPackages()) {
            pkg.setFiducialVisionSettings(null);
        }
        partSettingsByPartId.forEach((partId, partSettings) -> {
            if (partSettings == null) {
                return;
            }

            try {
                Part part = configuration.getPart(partId);
                if (part != null) { 
                    String serializedHash = AbstractVisionSettings.createSettingsFingerprint(partSettings);
                    FiducialVisionSettings fiducialVisionSettings = fiducialVisionSettingsHashMap.get(serializedHash);
                    if (fiducialVisionSettings == null) {
                        fiducialVisionSettings = new FiducialVisionSettings(partSettings);
                        fiducialVisionSettings.setName("");
                        fiducialVisionSettingsHashMap.put(serializedHash, fiducialVisionSettings);

                        configuration.addVisionSettings(fiducialVisionSettings);
                    }

                    part.setFiducialVisionSettings((fiducialVisionSettings != defaultFiducialVisionSettings) ? fiducialVisionSettings : null);
                    Logger.info("Part "+partId+" FiducialVisionSettings migrated.");
                } else {
                    Logger.warn("Part "+partId+" FiducialVisionSettings with no part.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        partSettingsByPartId = null;

        optimizeVisionSettings(configuration);
    }

    private FiducialVisionSettings createStockFiducialVisionSettings() {
        FiducialVisionSettings fiducialVisionSettings;
        try {
            fiducialVisionSettings = new FiducialVisionSettings(AbstractVisionSettings.STOCK_FIDUCIAL_ID);
            fiducialVisionSettings.setName("- Stock Fiducial Vision Settings -");
            fiducialVisionSettings.setEnabled(true);
            fiducialVisionSettings.setPipeline(createStockPipeline());
            return fiducialVisionSettings;
        }
        catch (Exception e) {
            throw new Error(e);
        }
    }

    public void optimizeVisionSettings(Configuration configuration) {
        // Remove any duplicate settings.
        HashMap<String, AbstractVisionSettings> visionSettingsHashMap = new HashMap<>();
        FiducialVisionSettings defaultVisionSettings = getFiducialVisionSettings();
        // Make it dominant in case it is identical to stock.
        visionSettingsHashMap.put(AbstractVisionSettings.createSettingsFingerprint(defaultVisionSettings), defaultVisionSettings);
        for (AbstractVisionSettings visionSettings : configuration.getVisionSettings()) {
            if (visionSettings instanceof FiducialVisionSettings) {
                String serializedHash = AbstractVisionSettings.createSettingsFingerprint(visionSettings);
                AbstractVisionSettings firstVisionSettings = visionSettingsHashMap.get(serializedHash);
                if (firstVisionSettings == null) {
                    visionSettingsHashMap.put(serializedHash, visionSettings);
                }
                else if (visionSettings != defaultVisionSettings
                        && !visionSettings.isStockSetting()) {
                    // Duplicate, remove any references.
                    for (PartSettingsHolder holder : visionSettings.getUsedFiducialVisionIn()) {
                        holder.setFiducialVisionSettings((FiducialVisionSettings) firstVisionSettings);
                    }
                    if (visionSettings.getUsedFiducialVisionIn().size() == 0) {
                        if (firstVisionSettings != defaultVisionSettings  
                                && !firstVisionSettings.isStockSetting()) {
                            firstVisionSettings.setName(firstVisionSettings.getName()+" + "+visionSettings.getName());
                        }
                        configuration.removeVisionSettings(visionSettings);
                    }
                }
            }
        }

        // Per package, search the most common settings on parts, and make them inherited package setting.
        for (org.openpnp.model.Package pkg : configuration.getPackages()) {
            HashMap<String, Integer> histogram = new HashMap<>();
            FiducialVisionSettings mostFrequentVisionSettings = null;
            int highestFrequency = 0;
            FiducialVisionSettings packageVisionSettings = getInheritedVisionSettings(pkg);
            for (Part part: configuration.getParts()) {
                if (part.getPackage() == pkg) {
                    FiducialVisionSettings visionSettings = getInheritedVisionSettings(part);
                    String id = visionSettings != null ? visionSettings.getId() : "";
                    Integer frequency = histogram.get(id);
                    frequency = (frequency != null ? frequency + 1 : 1);
                    histogram.put(id, frequency);
                    if (highestFrequency < frequency) {
                        highestFrequency = frequency;
                        mostFrequentVisionSettings = visionSettings;
                    }
                }
            }
            if (mostFrequentVisionSettings != null) {
                if (mostFrequentVisionSettings == defaultVisionSettings) {
                    pkg.setFiducialVisionSettings(null);
                }
                else {
                    pkg.setFiducialVisionSettings(mostFrequentVisionSettings);
                }
                for (Part part: configuration.getParts()) {
                    if (part.getPackage() == pkg) {
                        if (part.getFiducialVisionSettings() == mostFrequentVisionSettings) {
                            // Parts inherit from package now.
                            part.setFiducialVisionSettings(null);
                        }
                        else if (part.getFiducialVisionSettings() == null 
                                && packageVisionSettings != mostFrequentVisionSettings){
                            // Former package settings were inherited, now we must freeze them. 
                            part.setFiducialVisionSettings(packageVisionSettings);
                        }
                    }
                }
                if (mostFrequentVisionSettings != defaultVisionSettings
                        && !mostFrequentVisionSettings.isStockSetting()
                        && !mostFrequentVisionSettings.getName().isEmpty() 
                        && mostFrequentVisionSettings.getUsedFiducialVisionIn().size() == 1) {
                    // If these part settings are now unique to the package, name them so. 
                    mostFrequentVisionSettings.setName(pkg.getShortName());
                }
            }
        }

        // Set missing names by usage.
        AbstractVisionSettings.ListConverter listConverter = new AbstractVisionSettings.ListConverter(false);
        int various = 0;
        for (AbstractVisionSettings visionSettings : configuration.getVisionSettings()) {
            if (visionSettings instanceof FiducialVisionSettings) {
                List<PartSettingsHolder> usedIn = visionSettings.getUsedFiducialVisionIn();
                if (!visionSettings.isStockSetting()
                        && visionSettings != defaultVisionSettings
                        && usedIn.isEmpty()) {
                    configuration.removeVisionSettings(visionSettings);
                }
                else if (visionSettings.getName().isEmpty()) {
                    if (usedIn.size() <= 3) {
                        visionSettings.setName(listConverter.convertForward(usedIn));
                    }
                    else {
                        various++;
                        visionSettings.setName("Migrated "+various);
                    }
                }
            }
        }
    }

}
