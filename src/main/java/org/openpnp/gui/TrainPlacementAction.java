package org.openpnp.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.imgproc.Imgproc;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.CameraViewFilter;
import org.openpnp.gui.support.Icons;
import org.openpnp.machine.reference.camera.SimulatedUpCamera;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.spi.PartAlignment;
import org.openpnp.spi.PartAlignment.PartAlignmentOffset;
import org.openpnp.util.ImageUtils;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.util.Utils2D;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.FluentCv;

public class TrainPlacementAction extends AbstractAction {
    public TrainPlacementAction() {
        putValue(SMALL_ICON, Icons.partAlign);
        putValue(NAME, "Train Placement");
        putValue(SHORT_DESCRIPTION, "Use a bottom vision overlay to train the placement.");
    }
    
    Nozzle findNozzle(Part part) throws Exception {
        org.openpnp.model.Package packag = part.getPackage();
        
        for (Nozzle nozzle : Configuration.get().getMachine().getDefaultHead().getNozzles()) {
            if (nozzle.getNozzleTip() == null) {
                continue;
            }
            if (packag.getCompatibleNozzleTips().contains(nozzle.getNozzleTip())) {
                return nozzle;
            }
        }
        
        for (Nozzle nozzle : Configuration.get().getMachine().getDefaultHead().getNozzles()) {
            for (NozzleTip nozzleTip : nozzle.getCompatibleNozzleTips()) {
                if (packag.getCompatibleNozzleTips().contains(nozzleTip)) {
                    nozzle.loadNozzleTip(nozzleTip);
                    return nozzle;
                }
            }
        }
        
        throw new Exception("No compatible nozzle and nozzle tip found for " + part.getName());
    }
    
    Feeder findFeeder(Part part) throws Exception {
        for (Feeder feeder : Configuration.get().getMachine().getFeeders()) {
            if (!feeder.isEnabled()) {
                continue;
            }
            if (feeder.getPart() == part) {
                return feeder;
            }
        }
        throw new Exception("No enabled feeder found for " + part.getName());
    }
    
    void pick(Part part, Feeder feeder, Nozzle nozzle) throws Exception {
        feeder.feed(nozzle);
        Location pickLocation = feeder.getPickLocation();
        MovableUtils.moveToLocationAtSafeZ(nozzle, pickLocation);
        nozzle.pick(part);
        nozzle.moveToSafeZ();
        if (nozzle.getPart() != part) {
            throw new Exception("Picked part does not match expected part. How?");
        }
    }
    
    PartAlignmentOffset align(Nozzle nozzle) throws Exception {
        ((SimulatedUpCamera) VisionUtils.getBottomVisionCamera())
            .setErrorOffsets(
                    new Location(LengthUnit.Millimeters, Math.random(), Math.random(), 0, Math.random() * 10));
        
        for (PartAlignment alignment : Configuration.get().getMachine().getPartAlignments()) {
            if (!alignment.canHandle(nozzle.getPart())) {
                continue;
            }
            return VisionUtils.findPartAlignmentOffsets(
                    alignment, nozzle.getPart(), null, new Location(LengthUnit.Millimeters), nozzle);
        }

        ((SimulatedUpCamera) VisionUtils.getBottomVisionCamera())
        .setErrorOffsets(
                new Location(LengthUnit.Millimeters));
        
        throw new Exception("No compatible part alignment found for " + nozzle.getPart().getName());
    }
    
    Location transformCameraLocation(Part part, Location cameraLocation,
            PartAlignmentOffset alignmentOffsets) {
        if (alignmentOffsets.getPreRotated()) {
            cameraLocation =
                    cameraLocation.subtractWithRotation(alignmentOffsets.getLocation());
        }
        else {
            Location alignmentOffsetsLocation = alignmentOffsets.getLocation();
            Location location = new Location(LengthUnit.Millimeters).rotateXyCenterPoint(
                    alignmentOffsetsLocation,
                    cameraLocation.getRotation() - alignmentOffsetsLocation.getRotation());
            location = location.derive(null, null, null,
                    cameraLocation.getRotation() - alignmentOffsetsLocation.getRotation());
            location = location.add(cameraLocation);
            location = location.subtract(alignmentOffsetsLocation);
            cameraLocation = location;
        }
        cameraLocation = cameraLocation.add(
                new Location(part.getHeight().getUnits(), 0, 0, part.getHeight().getValue(), 0));

        return cameraLocation;
    }

    BufferedImage captureImage(Nozzle nozzle, PartAlignmentOffset offsets) throws Exception {
        ((SimulatedUpCamera) VisionUtils.getBottomVisionCamera())
            .setDrawNozzle(false);
        Camera camera = VisionUtils.getBottomVisionCamera();
        Location location = camera.getLocation();
        location = transformCameraLocation(nozzle.getPart(), location, offsets);
        MovableUtils.moveToLocationAtSafeZ(nozzle, location);
        BufferedImage image = camera.settleAndCapture();
        ((SimulatedUpCamera) VisionUtils.getBottomVisionCamera())
            .setDrawNozzle(true);
        return image;
    }
    
    void discard(Nozzle nozzle) throws Exception {
        Location discardLocation = Configuration.get().getMachine().getDiscardLocation();
        MovableUtils.moveToLocationAtSafeZ(nozzle, discardLocation);
        nozzle.place();
        nozzle.moveToSafeZ();
    }
    
    static BufferedImage filterImage(BufferedImage image) {
        // Convert to gray and threshold using Otsu's method
        Mat mat = new FluentCv().toMat(image).toGray().threshold(0).mat();
        
        // Convert to RGBA so we have an alpha channel
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_GRAY2BGRA);
        
        // Turn all black pixels transparent and change channel order to match
        // the BufferedImage we'll create below.
        byte[] pixel = new byte[4];
        for (int y = 0; y < mat.cols(); y++) {
            for (int x = 0; x < mat.rows(); x++) {
                mat.get(y, x, pixel);
                if (pixel[0] == 0 && pixel[1] == 0 && pixel[2] == 0) {
                    pixel[3] = 0;
                }
                byte b = pixel[0];
                byte g = pixel[1];
                byte r = pixel[2];
                byte a = pixel[3];
                pixel[0] = a;
                pixel[1] = b;
                pixel[2] = g;
                pixel[3] = r;
                mat.put(y, x, pixel);
            }
        }

        // Convert the Mat back to BufferedImage
        image = new BufferedImage(mat.cols(), mat.rows(), BufferedImage.TYPE_4BYTE_ABGR);
        mat.get(0, 0, ((DataBufferByte) image.getRaster().getDataBuffer()).getData());
        mat.release();
        
        return image;
    }
    
    Camera moveCameraToPlacement(BoardLocation boardLocation, Placement placement) throws Exception {
        Part part = placement.getPart();
        Location placementLocation =
                Utils2D.calculateBoardPlacementLocation(boardLocation, placement.getLocation());
        placementLocation = placementLocation.add(new Location(part.getHeight().getUnits(), 0,
                0, part.getHeight().getValue(), 0));
        Camera camera = Configuration.get().getMachine().getDefaultHead().getDefaultCamera();
        MovableUtils.moveToLocationAtSafeZ(camera, placementLocation);
        return camera;
    }
    
    void render(Camera camera, BufferedImage image) {
        try {
            CameraView cameraView = MainFrame.get().getCameraViews().getCameraView(camera);
            cameraView.setCameraViewFilter(new CameraViewFilter() {
                @Override
                public BufferedImage filterCameraImage(Camera camera, BufferedImage cameraImage) {
                    BufferedImage overlay = image;
                    AffineTransform tx;
                    AffineTransformOp op;
                    // Rotate the image about it's center, to the camera's rotation
                    overlay = rotate(overlay, -camera.getLocation().getRotation());
                    
                    // Scale the overlay to the camera units per pixel
                    double scaleX = 1, scaleY = 1;
                    try {
                        Location bottomUpp = VisionUtils.getBottomVisionCamera().getUnitsPerPixel().convertToUnits(LengthUnit.Millimeters);
                        Location topUpp = camera.getUnitsPerPixel().convertToUnits(LengthUnit.Millimeters);
                        scaleX = bottomUpp.getX() / topUpp.getX();
                        scaleY = bottomUpp.getY() / topUpp.getY();
                    }
                    catch (Exception e) {
                        
                    }
                    tx = AffineTransform.getScaleInstance(scaleX, scaleY);
                    op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
                    overlay = op.filter(overlay, null);
                    
                    // Create the image that will be used as the final image
                    BufferedImage target = new BufferedImage(cameraImage.getWidth(), cameraImage.getHeight(), cameraImage.getType());
                    Graphics2D g = (Graphics2D) target.getGraphics();
                    
                    // Draw the camera image first
                    g.drawImage(cameraImage, 0, 0, null);

                    // And draw the overlay, centered
                    g.drawImage(overlay, 
                            target.getWidth() / 2 - overlay.getWidth() / 2, 
                            target.getHeight() / 2 - overlay.getHeight() / 2, 
                            null);                
                    g.dispose();
                    return target;
                }
            });
        }
        catch (Exception e) {
            // Throw away, just means we're running outside of the UI.
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent arg0) {
        Placement placement = MainFrame.get().getJobTab().getJobPlacementsPanel()
                   .getSelection();
        BoardLocation boardLocation = MainFrame.get().getJobTab().getSelection();
        Part part = placement.getPart();
        UiUtils.submitUiMachineTask(() -> {
            /**
             * Find a nozzle that can pick the part
             * Find a feeder that can feeder the part
             * Pick the part
             * Align the part
             * Position at bottom vision (using alignment offsets)
             * Settle and capture image
             * Discard or replace part
             * Move camera to placement
             * Render image over placement center at 20% opaque
             */
            
            Camera camera = Configuration.get().getMachine().getDefaultHead().getDefaultCamera();
            CameraView cameraView = MainFrame.get().getCameraViews().getCameraView(camera);
            if (cameraView.getCameraViewFilter() != null) {
                cameraView.setCameraViewFilter(null);
                return;
            }
            
            Nozzle nozzle = findNozzle(part);
            Feeder feeder = findFeeder(part);
            pick(part, feeder, nozzle);
            PartAlignmentOffset offsets = align(nozzle);
            BufferedImage image = captureImage(nozzle, offsets);
            discard(nozzle);
            image = filterImage(image);
            camera = moveCameraToPlacement(boardLocation, placement);
            render(camera, image);
        });
    }

    // https://stackoverflow.com/questions/2687926/how-can-i-rotate-an-image-using-java-swing-and-then-set-its-origin-to-0-0
    public static BufferedImage rotate(BufferedImage image, double _thetaInDegrees) {
        /*
         * Affline transform only works with perfect squares. The following code is used to take any
         * rectangle image and rotate it correctly. To do this it chooses a center point that is
         * half the greater length and tricks the library to think the image is a perfect square,
         * then it does the rotation and tells the library where to find the correct top left point.
         * The special cases in each orientation happen when the extra image that doesn't exist is
         * either on the left or on top of the image being rotated. In both cases the point is
         * adjusted by the difference in the longer side and the shorter side to get the point at
         * the correct top left corner of the image. NOTE: the x and y axes also rotate with the
         * image so where width > height the adjustments always happen on the y axis and where the
         * height > width the adjustments happen on the x axis.
         * 
         */
        AffineTransform xform = new AffineTransform();
        double _theta = Math.toRadians(_thetaInDegrees);

        if (image.getWidth() > image.getHeight()) {
            xform.setToTranslation(0.5 * image.getWidth(), 0.5 * image.getWidth());
            xform.rotate(_theta);
            xform.translate(-0.5 * image.getWidth(), -0.5 * image.getWidth());
        }
        else if (image.getHeight() > image.getWidth()) {
            xform.setToTranslation(0.5 * image.getHeight(), 0.5 * image.getHeight());
            xform.rotate(_theta);
            xform.translate(-0.5 * image.getHeight(), -0.5 * image.getHeight());
        }
        else {
            xform.setToTranslation(0.5 * image.getWidth(), 0.5 * image.getHeight());
            xform.rotate(_theta);
            xform.translate(-0.5 * image.getHeight(), -0.5 * image.getWidth());
        }

        AffineTransformOp op = new AffineTransformOp(xform, AffineTransformOp.TYPE_BILINEAR);

        BufferedImage newImage =
                new BufferedImage(image.getHeight(), image.getWidth(), image.getType());
        return op.filter(image, newImage);
    }
}
