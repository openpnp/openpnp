package org.openpnp.machine.reference.vision;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.Action;
import javax.swing.Icon;

import org.apache.commons.io.IOUtils;
import org.opencv.core.CvException;
import org.opencv.features2d.KeyPoint;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.vision.wizards.ReferenceBottomVisionConfigurationWizard;
import org.openpnp.machine.reference.vision.wizards.ReferenceFiducialLocatorConfigurationWizard;
import org.openpnp.model.Board;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Footprint;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.model.Placement.Type;
import org.openpnp.spi.Camera;
import org.openpnp.spi.FiducialLocator;
import org.openpnp.spi.PartAlignment;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.PropertySheetHolder.PropertySheet;
import org.openpnp.util.IdentifiableList;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.Utils2D;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
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

    @Element(required = true)
    protected CvPipeline defaultFiducialPipeline = createPartPipeline(null);

    @ElementMap(required = false)
    protected Map<String, PartFiducialPipeline> fiducialPipelineByPartId = new HashMap<>();

    @Override
    public Wizard getPartConfigurationWizard(Part part) {
        return new ReferenceFiducialLocatorConfigurationWizard(this, part);
    }

    public PartFiducialPipeline getFiducialSettings(Part part) {
        PartFiducialPipeline fidSettings = this.fiducialPipelineByPartId.get(part.getId());

        if (fidSettings == null) {
            // was not in XML/table, create new settings record
            fidSettings = new PartFiducialPipeline(false, null);
            this.fiducialPipelineByPartId.put(part.getId(), fidSettings);
        }
        else {
            // got it, set camera if null
            try {
                if (fidSettings.getPipeline() != null
                        && fidSettings.getPipeline().getCamera() == null) {
                    fidSettings.getPipeline().setCamera(
                            Configuration.get().getMachine().getDefaultHead().getDefaultCamera());
                }
            }
            catch (Exception ignored) {
            }
        }
        return fidSettings;
    }

    public CvPipeline getDefaultPipeline() {
        return defaultFiducialPipeline;
    }

    public Location locateBoard(BoardLocation boardLocation) throws Exception {
        // Find the fids in the board
        IdentifiableList<Placement> fiducials = getFiducials(boardLocation);

        if (fiducials.size() < 2) {
            throw new Exception(String.format(
                    "The board side contains %d placements marked as a fiducial, at least 2 are required.",
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
            throw new Exception("Located fiducials are more than 1% away from expected, board location not set.");
        }

        // eh voila
        Location location = Utils2D.calculateBoardLocation(boardLocation, placementA, placementB,
                actualLocationA, actualLocationB);

        // convert Z-value units
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

        // Move to where we expect to find the fid, if user has not specified then we treat 0,0,0,0
        // as the place for this to be
        if (location != null) {
            MovableUtils.moveToLocationAtSafeZ(camera, location);
        }

        for (int i = 0; i < 3; i++) {
            // Wait for camera to settle
            Thread.sleep(camera.getSettleTimeMs());
            // Perform vision operation
            location = findFiducial(camera, part);
            if (location == null) {
                Logger.debug("No matches found!");
                continue;
            }
            Logger.debug("home fid. located at {}", location);
            // Move to where we actually found the fid
            camera.moveTo(location);
        }

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
     * @param fid
     * @return
     * @throws Exception
     */
    public Location getFiducialLocation(BoardLocation boardLocation, Placement fid)
            throws Exception {
        Camera camera = Configuration.get().getMachine().getDefaultHead().getDefaultCamera();

        Logger.debug("Locating {}", fid.getId());

        Part part = fid.getPart();
        if (part == null) {
            throw new Exception(
                    String.format("Fiducial %s does not have a valid part assigned.", fid.getId()));
        }

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

        // Move to where we expect to find the fid
        Location expectedlocation =
                Utils2D.calculateBoardPlacementLocation(boardLocation, fid.getLocation());
        Logger.debug("Looking for {} at {}", fid.getId(), expectedlocation);
        MovableUtils.moveToLocationAtSafeZ(camera, expectedlocation);

        Location location = expectedlocation;
        for (int i = 0; i < 3; i++) { // and the number of the counting shall be three
            // Wait for camera to settle
            Thread.sleep(camera.getSettleTimeMs());
            // Perform vision operation
            location = findFiducial(camera, part);
            if (location == null) {
                Logger.debug("No matches found!");
                continue;
            }
            location = location.add(expectedlocation);
            Logger.debug("{} located at {}", fid.getId(), location);
            // Move to where we actually found the fid
            MovableUtils.moveToLocationAtSafeZ(camera, location);
            break;
        }

        return location;
    }

    private Location findFiducial(final Camera camera, Part part) throws Exception {
        Object result = null;
        CvPipeline pipeline = createPartPipeline(getFiducialSettings(part));
        try {
            pipeline.setCamera(camera);
            pipeline.process();
            result = pipeline.getResult("results");
            // ack, if pipeline is bad... silently repair it, or error out here
            if (result == null) {
                pipeline = createPartPipeline(null);
                pipeline.setCamera(camera);
                pipeline.process();
                result = pipeline.getResult("results");
            }
            if (result == null)
                throw new CvException(
                        "Cannot find 'results' from fiducial locator vision processing");

            result = ((CvStage.Result) result).model;
        }
        catch (Throwable t) {
            throw new CvException("Error in fiducial locator vision processing");
        }
        List<Location> fidLocations = new ArrayList<>();
        if (result instanceof List) {
            if (((List) result).size() > 0) {
                if (((List) result).get(0) instanceof KeyPoint) {
                    List<KeyPoint> kps = (List<KeyPoint>) result;
                    fidLocations.addAll(kps.stream().map(keyPoint -> {
                        return VisionUtils.getPixelCenterOffsets(camera, keyPoint.pt.x,
                                keyPoint.pt.y);
                    }).sorted((a, b) -> {
                        double a1 = a.getLinearDistanceTo(
                                new Location(LengthUnit.Millimeters, 0, 0, 0, 0));
                        double b1 = b.getLinearDistanceTo(
                                new Location(LengthUnit.Millimeters, 0, 0, 0, 0));
                        return Double.compare(a1, b1);
                    }).collect(Collectors.toList()));
                }
            }
        }

        try {
            MainFrame.get().getCameraViews().getCameraView(camera).showFilteredImage(
                    OpenCvUtils.toBufferedImage(pipeline.getWorkingImage()), 500);
        }
        catch (Exception e) {
            // if we aren't running in the UI this will fail, and that's okay
        }

        if (fidLocations.isEmpty()) {
            return null;
        }

        return fidLocations.get(0);
    }

    /**
     * Create a template image based on a Placement's footprint. The image will be scaled to match
     * the dimensions of the current camera.
     * 
     * @param unitsPerPixel, footprint
     * @return
     */
    private static BufferedImage createTemplate(Location unitsPerPixel, Footprint footprint)
            throws Exception {
        Shape shape = footprint.getShape();

        if (shape == null) {
            throw new Exception(
                    "Invalid footprint found, unable to create template for fiducial match. See https://github.com/openpnp/openpnp/wiki/Fiducials.");
        }

        // Determine the scaling factor to go from Outline units to
        // Camera units.
        Length l = new Length(1, footprint.getUnits());
        l = l.convertToUnits(unitsPerPixel.getUnits());
        double unitScale = l.getValue();

        // Create a transform to scale the Shape by
        AffineTransform tx = new AffineTransform();

        // First we scale by units to convert the units and then we scale
        // by the camera X and Y units per pixels to get pixel locations.
        tx.scale(unitScale, unitScale);
        tx.scale(1.0 / unitsPerPixel.getX(), 1.0 / unitsPerPixel.getY());

        // Transform the Shape and draw it out.
        shape = tx.createTransformedShape(shape);

        Rectangle2D bounds = shape.getBounds2D();

        if (bounds.getWidth() == 0 || bounds.getHeight() == 0) {
            throw new Exception(
                    "Invalid footprint found, unable to create template for fiducial match. Width and height of pads must be greater than 0. See https://github.com/openpnp/openpnp/wiki/Fiducials.");
        }

        // Make the image 50% bigger than the shape. This gives better
        // recognition performance because it allows some border around the edges.
        double width = bounds.getWidth() * 1.5;
        double height = bounds.getHeight() * 1.5;
        BufferedImage template =
                new BufferedImage((int) width, (int) height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = (Graphics2D) template.getGraphics();

        g2d.setStroke(new BasicStroke(1f));
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.white);
        // center the drawing
        g2d.translate(width / 2, height / 2);
        g2d.fill(shape);

        g2d.dispose();

        return template;
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

    @Override
    public String getPropertySheetHolderTitle() {
        return "Fiducial Locator";
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {new PropertySheetWizardAdapter(
                new ReferenceFiducialLocatorConfigurationWizard(this, null))};
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Icon getPropertySheetHolderIcon() {
        // TODO Auto-generated method stub
        return null;
    }

    // Get the fiducial pipeline from the config
    public CvPipeline createPartPipeline(PartFiducialPipeline settings) {
        CvPipeline cvp = null;
        try {
            if (settings != null) {
                // try and get per-part pipeline
                cvp = settings.getPipeline();
                if (cvp == null) {
                    // we tried to get it, but not there. Use default.
                    try {
                        cvp = defaultFiducialPipeline.clone();
                    }
                    catch (Exception e) {
                    }
                }
            }
            else {
                // use default XML canned resource file
                String xml = IOUtils.toString(ReferenceFiducialLocator.class
                        .getResource("ReferenceFiducialLocator-DefaultPipeline.xml"));
                cvp = new CvPipeline(xml);
            }
        }
        catch (Exception e) {
            throw new Error(e);
        }
        // try and set camera. If not in the GUI, this will fail and that's okay
        try {
            cvp.setCamera(Configuration.get().getMachine().getDefaultHead().getDefaultCamera());
        }
        catch (Exception e) {
        }
        return cvp;
    }

    @Root
    public static class PartFiducialPipeline {
        @Attribute(required = true)
        protected boolean enabled;

        @Element(required = false)
        protected CvPipeline pipeline;

        public PartFiducialPipeline() {
            setEnabled(false);
            setPipeline(null);
        }

        public PartFiducialPipeline(boolean enabled, CvPipeline pipeline) {
            setEnabled(enabled);
            setPipeline(pipeline);
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public CvPipeline getPipeline() {
            return pipeline;
        }

        public void setPipeline(CvPipeline pipeline) {
            this.pipeline = pipeline;
        }
    }
}
