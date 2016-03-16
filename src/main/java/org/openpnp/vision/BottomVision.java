package org.openpnp.vision;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opencv.core.MatOfPoint;
import org.opencv.core.RotatedRect;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.model.Configuration;
import org.openpnp.model.Job;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.VisionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BottomVision {
    private static final Logger logger = LoggerFactory.getLogger(BottomVision.class);

    private Map<Length, BufferedImage> backgroundImages = new HashMap<>();

    // TODO: Note that we're using the default nozzle here, but that won't work
    // in a multiple nozzle machine. We probably need to capture images for all
    // nozzles and then use the right one for the background.
    public void preProcess(Job job) throws Exception {
        backgroundImages.clear();

        Camera camera = getBottomVisionCamera();
        for (Head head : Configuration.get().getMachine().getHeads()) {
            head.moveToSafeZ(1.0);
            for (Nozzle nozzle : head.getNozzles()) {
                MovableUtils.moveToLocationAtSafeZ(nozzle, camera.getLocation(), 1.0);
                for (int i = 0; i < 360; i += 30) {
                    Location location = camera.getLocation()
                            .addWithRotation(new Location(LengthUnit.Millimeters, 0, 0, 0, i));
                    nozzle.moveTo(location, 1.0);
                    BufferedImage image = camera.settleAndCapture();
                }
                nozzle.moveToSafeZ(1.0);
            }
        }

        // Nozzle nozzle = Configuration.get().getMachine().getDefaultHead().getDefaultNozzle();
        // Camera camera = getBottomVisionCamera();
        //
        // // figure out all the unique part heights we need to deal with
        // Set<Length> heights = new HashSet<Length>();
        // for (BoardLocation boardLocation : job.getBoardLocations()) {
        // Board board = boardLocation.getBoard();
        // for (Placement placement : board.getPlacements()) {
        // if (placement.getType() != Placement.Type.Place || placement.getSide() !=
        // boardLocation.getSide()) {
        // continue;
        // }
        // Part part = placement.getPart();
        // heights.add(part.getHeight());
        // }
        // }
        //
        // // and get a list of nozzles that we'll need to image
        // Set<Nozzle> nozzles = new HashSet<Nozzle>();
        //
        // logger.debug("Capturing backgrounds for heights: {}", heights);
        // MovableUtils.moveToLocationAtSafeZ(nozzle, camera.getLocation(), 1.0);
        // for (Length height : heights) {
        // Location heightLocation = new Location(height.getUnits(), 0, 0, height.getValue(), 0);
        // Location location = camera.getLocation().add(heightLocation);
        // nozzle.moveTo(location, 1.0);
        // backgroundImages.put(height, camera.settleAndCapture());
        // }
        // nozzle.moveToSafeZ(1.0);
    }

    public Location findOffsets(Part part, Nozzle nozzle) throws Exception {
        Camera camera = getBottomVisionCamera();

        // Create a location that is the Camera's X, Y, it's Z + part height
        // and a rotation of 0.
        Location startLocation = camera.getLocation();
        Length partHeight = part.getHeight();
        Location partHeightLocation =
                new Location(partHeight.getUnits(), 0, 0, partHeight.getValue(), 0);
        startLocation = startLocation.add(partHeightLocation).derive(null, null, null, 0d);

        MovableUtils.moveToLocationAtSafeZ(nozzle, startLocation, 1.0);

        BufferedImage backgroundImage = backgroundImages.get(part.getHeight());

        File debugDir = new File("/Users/jason/Desktop/debug/" + System.currentTimeMillis());
        debugDir.mkdirs();

        for (int i = 0; i < 6; i++) {
            File backgroundFile = new File(debugDir, i + "_background.png");
            File foregroundFile = new File(debugDir, i + "_foreground.png");
            File absDiffFile = new File(debugDir, i + "_asdiff.png");
            File processedFile = new File(debugDir, i + "_processed.png");

            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            List<RotatedRect> rects = new ArrayList<RotatedRect>();
            BufferedImage filteredImage = new FluentCv().setCamera(camera)

                    .toMat(backgroundImage).write(backgroundFile).toGray()
                    .blurGaussian(3, "background")

                    .settleAndCapture("original").write(foregroundFile).toGray().blurGaussian(3)

                    .absDiff("background").write(absDiffFile)

                    .blurGaussian(13).findEdgesRobertsCross().threshold(30).findContours(contours)
                    .recall("original").drawContours(contours, null, 1)
                    .getContourMaxRects(contours, rects).drawRects(rects, null, 2)
                    .write(processedFile).toBufferedImage();

            CameraView cameraView = MainFrame.mainFrame.cameraPanel.getCameraView(camera);
            cameraView.showFilteredImage(filteredImage, 3000);

            RotatedRect rect = rects.get(0);
            System.out.println(rect);

            // Create the offsets object. This is the physical distance from
            // the center of the camera to the located part.
            Location offsets =
                    VisionUtils.getPixelCenterOffsets(camera, rect.center.x, rect.center.y);

            // We assume that the part is never picked more than 45º rotated
            // so if OpenCV tells us it's rotated more than 45º we correct
            // it. This seems to happen quite a bit when the angle of rotation
            // is close to 0.
            double angle = rect.angle;
            if (Math.abs(angle) > 45) {
                if (angle < 0) {
                    angle += 90;
                }
                else {
                    angle -= 90;
                }
            }
            // Set the angle on the offsets.
            offsets = offsets.derive(null, null, null, angle);
            System.out.println("offsets " + offsets);

            // Move the nozzle so that the part is oriented correctly over the
            // camera.
            Location location = nozzle.getLocation().subtractWithRotation(offsets);
            nozzle.moveTo(location, 1.0);
        }

        nozzle.moveToSafeZ(1.0);
        return new Location(LengthUnit.Millimeters, 0, 0, 0, 0);
    }

    private Camera getBottomVisionCamera() throws Exception {
        for (Camera camera : Configuration.get().getMachine().getCameras()) {
            if (camera.getLooking() == Camera.Looking.Up) {
                return camera;
            }
        }
        throw new Exception("No up-looking camera found on the machine to use for bottom vision.");
    }
}
