package org.openpnp.util;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.openpnp.machine.reference.vision.ReferenceBottomVision;
import org.openpnp.machine.reference.vision.ReferenceFiducialLocator;
import org.openpnp.model.Area;
import org.openpnp.model.AreaUnit;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.FiducialVisionSettings;
import org.openpnp.model.Footprint;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.model.Package;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.model.Point;
import org.openpnp.model.Footprint.Pad;
import org.openpnp.spi.Camera;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PartAlignment;
import org.openpnp.spi.PartAlignment.PartAlignmentOffset;
import org.openpnp.vision.pipeline.CvPipeline;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

public class VisionUtils {
    final public static String PIPELINE_RESULTS_NAME = "results";
    final public static String PIPELINE_CONTROL_PROPERTY_NAME = "propertyName";

    /**
     * Given pixel coordinates within the frame of the Camera's image, get the offsets from Camera
     * center to the coordinates in Camera space and units. The resulting value is the distance the
     * Camera can be moved to be centered over the pixel coordinates.
     * 
     * Example: If the x, y coordinates describe a position above and to the left of the center of
     * the camera the offsets will be -,+.
     * 
     * If the coordinates position are below and to the right of center the offsets will be +, -.
     * 
     * Calling camera.getLocation().add(getPixelCenterOffsets(...) will give you the location of x,
     * y with respect to the center of the camera.
     * 
     * @param camera
     * @param x
     * @param y
     * @return
     */
    public static Location getPixelCenterOffsets(Camera camera, double x, double y) {
        double imageWidth = camera.getWidth();
        double imageHeight = camera.getHeight();

        // Calculate the difference between the center of the image to the
        // center of the match.
        double offsetX = x - (imageWidth / 2);
        double offsetY = y - (imageHeight / 2);

        return getPixelOffsets(camera, offsetX, offsetY);
    }

    /**
     * Given pixel offset coordinates, get the same offsets in Camera space and units.
     * 
     * @param camera
     * @param offsetX
     * @param offsetY
     * @return
     */
    public static Location getPixelOffsets(Camera camera, double offsetX, double offsetY) {
        // Convert pixels to units
        Location unitsPerPixel = camera.getUnitsPerPixelAtZ();
        offsetX *= unitsPerPixel.getX();
        offsetY *= unitsPerPixel.getY();
        // Convert to right-handed.
        return new Location(unitsPerPixel.getUnits(), offsetX, -offsetY, 0, 0);
    }

    /**
     * Get the Location of a set of pixel coordinates referenced to the center of the given camera.
     * This is a helper method that simply adds the offsets from
     * {@link VisionUtils#getPixelCenterOffsets(Camera, double, double)} to the Camera's current
     * location.
     * 
     * @param camera
     * @param x
     * @param y
     * @return
     */
    public static Location getPixelLocation(Camera camera, double x, double y) {
        return camera.getLocation().add(getPixelCenterOffsets(camera, x, y));
    }

    /**
     * Same as getPixelLocation() but including the tool specific calibration offset.
     *  
     * @param camera
     * @param tool
     * @param x
     * @param y
     * @return
     */
    public static Location getPixelLocation(Camera camera, HeadMountable tool, double x, double y) {
        return camera.getLocation(tool).add(getPixelCenterOffsets(camera, x, y));
    }

    /**
     * Get an angle in the OpenPNP coordinate system from an angle in the camera pixel  
     * coordinate system. 
     * The angle needs to be sign reversed to reflect the fact that the Z and Y axis are sign reversed.
     * OpenPNP uses a coordinate system with Z pointing towards the viewer, Y pointing up. OpenCV
     * however uses one with Z pointing away from the viewer, Y pointing downwards. Right-handed
     * rotation must be sign-reversed.   
     * See {@link VisionUtils#getPixelCenterOffsets(Camera, double, double)}.
     * 
     * @param camera
     * @param angle
     * @return
     */
    public static double getPixelAngle(Camera camera, double angle) {
        return -angle;
    }
    
    public static List<Location> sortLocationsByDistance(final Location origin,
            List<Location> locations) {
        // sort the results by distance from center ascending
        Collections.sort(locations, new Comparator<Location>() {
            public int compare(Location o1, Location o2) {
                Double o1d = origin.getLinearDistanceTo(o1);
                Double o2d = origin.getLinearDistanceTo(o2);
                return o1d.compareTo(o2d);
            }
        });
        return locations;
    }
    
    public static Camera getBottomVisionCamera() throws Exception {
        for (Camera camera : Configuration.get().getMachine().getCameras()) {
            if (camera.getLooking() == Camera.Looking.Up) {
                return camera;
            }
        }
        throw new Exception("No up-looking camera found on the machine to use for bottom vision.");
    }
    
    public static double toPixels(Length length, Camera camera) {
        // convert inputs to the same units
        Location unitsPerPixel = camera.getUnitsPerPixelAtZ();
        length = length.convertToUnits(unitsPerPixel.getUnits());

        // we average the units per pixel because circles can't be ovals
        double avgUnitsPerPixel = (unitsPerPixel.getX() + unitsPerPixel.getY()) / 2;

        // convert it all to pixels
        return length.getValue() / avgUnitsPerPixel;
    }
    
    public static double toPixels(Area area, Camera camera) {
        // convert inputs to the same units
        Location unitsPerPixel = camera.getUnitsPerPixel();
        area = area.convertToUnits(AreaUnit.fromLengthUnit(unitsPerPixel.getUnits()));

        // convert it all to pixels
        return area.getValue() / (unitsPerPixel.getX() * unitsPerPixel.getY());
    }

    /**
     * Get a location in camera pixels. This is the reverse transformation of getPixelLocation().
     *  
     * @param location
     * @param camera
     * @return
     */
    public static Point getLocationPixels(Camera camera, Location location) {
        return getLocationPixels(camera, null, location);
    }

    /**
     * Get a location in camera pixels. This is the reverse transformation of getPixelLocation(tool).
     * This overload includes the tool specific calibration offset. 
     * 
     * @param camera
     * @param tool
     * @param location
     * @return
     */
    public static Point getLocationPixels(Camera camera, HeadMountable tool, Location location) {
        Point center = getLocationPixelCenterOffsets(camera, tool, location);
        return new Point(center.getX()+camera.getWidth()/2, center.getY()+camera.getHeight()/2);
    }

    /**
     * Get a location camera center offsets in pixels. 
     *  
     * @param location
     * @param camera
     * @return
     */
    public static Point getLocationPixelCenterOffsets(Camera camera, Location location) {
        return getLocationPixelCenterOffsets(camera, null, location);
    }

    /**
     * Get a location camera center offsets in pixels. 
     * This overload includes the tool specific calibration offset. 
     * 
     * @param camera
     * @param tool
     * @param location
     * @return
     */
    public static Point getLocationPixelCenterOffsets(Camera camera, HeadMountable tool, Location location) {
        // get the units per pixel scale 
        Location unitsPerPixel = camera.getUnitsPerPixelAtZ();
        // convert inputs to the same units, center on camera and scale
        location = location.convertToUnits(unitsPerPixel.getUnits())
                .subtract(camera.getLocation(tool))
                .multiply(1./unitsPerPixel.getX(), -1./unitsPerPixel.getY(), 0., 0.);
        // relative center of camera in pixels
        return new Point(location.getX(), location.getY());
    }

    /**
     * Using the given camera, try to find a QR code and return it's text. This is just a wrapper
     * for the generic scanBarcode(Camera) function. This one was added before the other and I don't
     * want to remove it in case people are using it, but it does the same thing. 
     * @param camera
     * @return
     * @throws Exception 
     */
    public static String readQrCode(Camera camera) throws Exception {
        return scanBarcode(camera);
    }
    
    /**
     * Using the given camera, try to find any supported barcode and return it's text. 
     * @param camera
     * @return
     * @throws Exception 
     */
    public static String scanBarcode(Camera camera) throws Exception {
        BufferedImage image = camera.lightSettleAndCapture();
        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(
                new BufferedImageLuminanceSource(image)));
        try {
            Result qrCodeResult = new MultiFormatReader().decode(binaryBitmap);
            return qrCodeResult.getText();    
        }
        catch (Exception e) {
            return null;
        }
    }
    
    public static PartAlignment.PartAlignmentOffset findPartAlignmentOffsets(PartAlignment p, Part part, BoardLocation boardLocation, Placement placement, Nozzle nozzle) throws Exception {
        Map<String, Object> globals = new HashMap<>();
        globals.put("part", part);
        globals.put("nozzle", nozzle);
        Configuration.get().getScripting().on("Vision.PartAlignment.Before", globals);

        PartAlignmentOffset offsets = null;
        try {
            offsets = p.findOffsets(part, boardLocation, placement, nozzle);
            return offsets;
        }
        finally {
            globals.put("offsets", offsets);
            Configuration.get().getScripting().on("Vision.PartAlignment.After", globals);
        }
    }

    /**
     * Compute an RGB histogram over the provided image.
     * 
     * @param image
     * @return the histogram as long[channel][value] with channel 0=Red 1=Green 2=Blue and value 0...255.
     */
    public static long[][] computeImageHistogram(BufferedImage image) {
        long[][] histogram = new long[3][256];
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = (rgb >> 0) & 0xff;
                histogram[0][r]++;
                histogram[1][g]++;
                histogram[2][b]++;
            }
        }
        return histogram;
    }

    /**
     * Compute an HSV histogram over the provided image.
     * 
     * @param image
     * @return the histogram as long[channel][value] with channel 0=Hue 1=Saturation 2=Value and value 0...255.
     */
    public static long[][] computeImageHistogramHsv(BufferedImage image) {
        long[][] histogram = new long[3][256];
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = (rgb >> 0) & 0xff;
                float[] hsb = Color.RGBtoHSB(r, g, b, null);
                int h = (int)(hsb[0]*255.999);
                int s = (int)(hsb[1]*255.999);
                int v = (int)(hsb[2]*255.999);
                histogram[0][h]++;
                histogram[1][s]++;
                histogram[2][v]++;
            }
        }
        return histogram;
    }

    /**
     * Ready the FIDUCIAL-HOME, either by returning the existing part or creating a new
     * part with the given circular fiducialDiameter.
     * 
     * @param fiducialDiameter 
     * @param overwrite If the FIDUCIAL-HOME already exists, overwrite it and make sure it has the given fiducialDiameter
     * and circular shape.
     * @return The FIDUCIAL-HOME part.
     * @throws Exception 
     */
    static public Part readyHomingFiducialWithDiameter(Length fiducialDiameter, boolean overwrite) throws Exception {
        Configuration configuration = Configuration.get();
        Part part = configuration.getPart("FIDUCIAL-HOME");
        if (part == null) {
            part = new Part("FIDUCIAL-HOME");
            configuration.addPart(part);
        }
        else if (!overwrite) {
            return part;
        }
        org.openpnp.model.Package pkg = configuration.getPackage("FIDUCIAL-HOME");
        if (pkg == null) {
            pkg = new org.openpnp.model.Package("FIDUCIAL-HOME");
            configuration.addPackage(pkg);
        }
        part.setPackage(pkg);
        Footprint footprint = new Footprint();
        footprint.setUnits(fiducialDiameter.getUnits());
        Pad pad = new Pad();
        pad.setName("FID");
        pad.setWidth(fiducialDiameter.getValue());
        pad.setHeight(fiducialDiameter.getValue());
        pad.setRoundness(100.0);
        footprint.addPad(pad);
        pkg.setFootprint(footprint);
        ReferenceFiducialLocator fiducialLocator = ReferenceFiducialLocator.getDefault();
        String xmlPipeline = IOUtils.toString(ReferenceBottomVision.class
                .getResource("ReferenceFiducialLocator-DefaultPipeline.xml"));
        CvPipeline pipeline = new CvPipeline(xmlPipeline);
        FiducialVisionSettings visionSettings = fiducialLocator.getInheritedVisionSettings(part);
        if (pipeline.equals(visionSettings.getPipeline())) {
            // Already the right vision settings.
        }
        else {
            if (visionSettings.getUsedFiducialVisionIn().size() == 1 
                    && visionSettings.getUsedFiducialVisionIn().get(0) == part) {
                // Already a special setting on the part. Modify it.
            }
            else {
                // Needs new settings (likely means the default was changed by the user).
                FiducialVisionSettings newSettings = new FiducialVisionSettings();
                newSettings.setValues(visionSettings);
                newSettings.setName(part.getShortName());
                part.setFiducialVisionSettings(newSettings);
                Configuration.get().addVisionSettings(newSettings);
                visionSettings = newSettings;
            }
            visionSettings.setPipeline(pipeline);
        }
        return part;
    }
}
