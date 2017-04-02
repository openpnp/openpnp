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
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.IdentifiableList;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.Utils2D;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.CvStage.Result;
import org.openpnp.vision.pipeline.CvStage.Result.Circle;
import org.openpnp.vision.pipeline.CvStage.Result.TemplateMatch;
import org.openpnp.vision.pipeline.stages.ImageInput;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Root;

/**
 * Implements an algorithm for finding a set of fiducials on a board and returning the correct
 * orientation for the board.
 */
@Root
public class ReferenceFiducialLocator implements FiducialLocator {

    // this is the default pipeline, either a copy of the resource XML or 
    //  pipeline which has been edited by the user
    @Element(required = true)
    protected CvPipeline defaultFiducialPipeline = createPartPipeline(null);

    // map of default and custom pipelines for parts
    @ElementMap(required = false)
    protected Map<String, PartFiducialPipeline> fiducialPipelineByPartId = new HashMap<>();

    @Override
    public Wizard getPartConfigurationWizard(Part part) {
        return new ReferenceFiducialLocatorConfigurationWizard(this, part);
    }

    // get settings for this part
    //  created if not previously in existance
    public PartFiducialPipeline getFiducialSettings(Part part) {
        if (part == null) {
            return null;
        }
        
        PartFiducialPipeline fidSettings = this.fiducialPipelineByPartId.get(part.getId());

        if (fidSettings == null) {
            // was not in XML/table, create new settings record
            fidSettings = new PartFiducialPipeline();
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

    public void setDefaultPipeline(CvPipeline cvPipeline) {
        this.defaultFiducialPipeline = cvPipeline;
    }

    // find board fiducials, use them to calculate origin of board
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
            location = findFiducial(camera, part, 0.0D);
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
            location = findFiducial(camera, part, boardLocation.getLocation().getRotation() + fid.getLocation().getRotation());
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

    /*
     * use default or custom pipeline for part to pinpoint fiducial location
     * return the first(closest) identified point
     */
    private Location findFiducial(final Camera camera, Part part, double rotation) throws Exception {

        PartFiducialPipeline settings = getFiducialSettings(part);
        CvPipeline pipeline = createPartPipeline(settings);
        
        insertTemplateIntoPipeline(pipeline, rotation,
                camera.getUnitsPerPixel(), part, settings);

        Object result = getPipelineProcessingResults(camera, pipeline);
        
        List<Location> fidLocations = processResultsIntoLocationList(camera, result);        

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

        // return location closest to expected
        return fidLocations.get(0);
    }
    
    /*
     * Given a part and it's orientation, create a template
     * to that rotation and insert it into the pipeline 
     * if it contains a receiving stage class (ImageInput).
     * 
     * Cause no errors, return null on fail
     */
    public void insertTemplateIntoPipeline(CvPipeline pipeline,
            Double rotation, Location unitsPerPixel, 
            Part part, PartFiducialPipeline settings) {

        BufferedImage template = null;
        
        // here we either need a footprint or a previously created template
        Footprint fp = null;
        if (rotation == settings.getTemplateRotation())
            template = settings.getTemplate();
        if (template == null) {
            // no template
            try {
                fp = part.getPackage().getFootprint();
            }
            catch (Exception e) {
                // no feet either
                fp = null;
                Logger.warn("Could not get \"" + part.getId() + "\" footprint, will not use template matching.");
            }
        }
        // got something we can use
        if (template != null || fp != null) {
            // look for an ImageInput stage named 'template'
            CvStage templateStage = pipeline.getStage("template");
            // if the named stage is of the correct type
            if ((templateStage != null) && (templateStage instanceof ImageInput)) {
                ImageInput imgIn = (ImageInput) templateStage;
                // make template if we haven't done so yet
                if (template == null)
                {
                    try {
                        template = createTemplate(unitsPerPixel, fp, rotation);
                        Logger.debug("Template created for \"" + part.getId() + "\" fiducial");
                    }
                    catch (Exception e){
                        template = null;
                    }
                }
                // save template image in settings (nullable)
                settings.setTemplate(template);
                // insert template image into this stage
                if (template != null) {
                    imgIn.setInputImage(template);
                    imgIn.setEnabled(true);
                }
            }
        }
    }

    /**
     * Create a template image based on a Placement's footprint. The image will be scaled to match
     * the dimensions of the current camera.
     * 
     * @param unitsPerPixel, footprint
     * @return
     */
    public static BufferedImage createTemplate(Location unitsPerPixel, Footprint footprint, double rotation)
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
        tx.rotate(Math.toRadians(-rotation));

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

    /*
     * process the pipeline with the camera and return the "results"
     * throw error if not found
     */
    public Object getPipelineProcessingResults(Camera camera, CvPipeline pipeline) {
        Object result = null;
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
            // if no result, we really can't process this, so error out
            if (result == null)
                throw new CvException(
                        "Cannot find \"results\" from fiducial locator vision processing");

            result = ((Result) result).model;
        }
        catch (Throwable t) {
            throw new CvException("Error in fiducial locator vision processing");
        }
        
        return result;
    }
    
    /*
     * return a list of locations from a pipeline processing result
     * result may be keypoints, circles, or template matches 
     * -> watch the lambda functions, Oracle 8 doesn't like them sometimes
     */
    private List<Location> processResultsIntoLocationList(Camera camera, Object result) {

        List<Location> fidLocations = new ArrayList<Location>();

        if (result instanceof List) {
            if (((List) result).size() > 0) {
                
                // process keypoints
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
                
                // process template matches
                if (((List) result).get(0) instanceof TemplateMatch) {
                    List<TemplateMatch> tm = (List<TemplateMatch>) result;
                    fidLocations.addAll(tm.stream().map(templateMatch -> {
                        return VisionUtils.getPixelCenterOffsets(camera,
                                templateMatch.x + templateMatch.width/2.0,
                                templateMatch.y + templateMatch.height/2.0);
                    }).sorted((a, b) -> {
                        double a1 = a.getLinearDistanceTo(
                                new Location(LengthUnit.Millimeters, 0, 0, 0, 0));
                        double b1 = b.getLinearDistanceTo(
                                new Location(LengthUnit.Millimeters, 0, 0, 0, 0));
                        return Double.compare(a1, b1);
                    }).collect(Collectors.toList()));
                }
                
                // process circle matches
                if (((List) result).get(0) instanceof Circle) {
                    List<Circle> tm = (List<Circle>) result;
                    fidLocations.addAll(tm.stream().map(circle -> {
                        return VisionUtils.getPixelCenterOffsets(camera,
                                circle.x,
                                circle.y);
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
        
        return fidLocations;
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
        @Element(required = false)
        protected CvPipeline pipeline;
        
        protected BufferedImage template;
        protected double templateRotation;

        public PartFiducialPipeline() {
            pipeline = null;
            template = null;
            templateRotation = 99999.9D;
        }

        public PartFiducialPipeline(CvPipeline pipeline, boolean useCustomPipeline, boolean useTemplateMatch) {
            this.pipeline = pipeline;
            this.template = null;
            templateRotation = 99999.9D;
        }

        public CvPipeline getPipeline() {
            return pipeline;
        }

        public void setPipeline(CvPipeline pipeline) {
            this.pipeline = pipeline;
        }

        public BufferedImage getTemplate() {
            return template;
        }

        public void setTemplate(BufferedImage template) {
            this.template = template;
        }

        public double getTemplateRotation() {
            return templateRotation;
        }

        public void setTemplateRotation(double templateRotation) {
            this.templateRotation = templateRotation;
        }
    }
}
