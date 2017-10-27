package org.openpnp.machine.reference.vision;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.Icon;

import org.apache.commons.io.IOUtils;
import org.opencv.features2d.KeyPoint;
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
import org.openpnp.spi.Camera;
import org.openpnp.spi.FiducialLocator;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.VisionProvider.TemplateMatch;
import org.openpnp.util.IdentifiableList;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.Utils2D;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Root;

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
        IdentifiableList<Placement> fiducials;

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

        // Find the two that are most distant from each other
        List<Placement> mostDistant = getMostDistantPlacements(fiducials);

        Placement placementA = mostDistant.get(0);
        Placement placementB = mostDistant.get(1);

        Logger.debug("Chose {} and {}", placementA.getId(), placementB.getId());

        // Run the fiducial check on each and get their actual locations
        Location actualLocationA = getFiducialLocation(boardLocation, placementA);
        if (actualLocationA == null) {
            throw new Exception("Unable to locate first fiducial.");
        }
        Location actualLocationB = getFiducialLocation(boardLocation, placementB);
        if (actualLocationB == null) {
            throw new Exception("Unable to locate second fiducial.");
        }

        // Calculate the linear distance between the ideal points and the
        // located points. If they differ by more than a few percent we
        // probably made a mistake.
        double fidDistance =
                Math.abs(placementA.getLocation().getLinearDistanceTo(placementB.getLocation()));
        double visionDistance = Math.abs(actualLocationA.getLinearDistanceTo(actualLocationB));
        if (Math.abs(fidDistance - visionDistance) > fidDistance * 0.01) {
            throw new Exception("Located fiducials are more than 1% away from expected.");
        }

        Location location = Utils2D.calculateBoardLocation(boardLocation, placementA, placementB,
                actualLocationA, actualLocationB);

        location = location.derive(null, null,
                boardLocation.getLocation().convertToUnits(location.getUnits()).getZ(), null);

        return location;
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
    
    /**
     * Given a List of Placements, find the two that are the most distant from each other.
     * 
     * @param fiducials
     * @return
     */
    private static List<Placement> getMostDistantPlacements(List<Placement> fiducials) {
        if (fiducials.size() < 2) {
            return null;
        }
        Placement maxA = null, maxB = null;
        double max = 0;
        for (Placement a : fiducials) {
            for (Placement b : fiducials) {
                if (a == b) {
                    continue;
                }
                double d = Math.abs(a.getLocation().getLinearDistanceTo(b.getLocation()));
                if (d > max) {
                    maxA = a;
                    maxB = b;
                    max = d;
                }
            }
        }
        ArrayList<Placement> results = new ArrayList<>();
        results.add(maxA);
        results.add(maxB);
        return results;
    }

    private static IdentifiableList<Placement> getFiducials(BoardLocation boardLocation) {
        Board board = boardLocation.getBoard();
        IdentifiableList<Placement> fiducials = new IdentifiableList<>();
        for (Placement placement : board.getPlacements()) {
            if (placement.getType() == Type.Fiducial
                    && placement.getSide() == boardLocation.getSide()) {
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
