package org.openpnp.vision;

import java.util.List;

import org.opencv.core.RotatedRect;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: Add the Part as context to the pipeline and use the height to scale the circle mask
// to account for the smaller circle as the nozzle raises.
public class BottomVision {
    private static final Logger logger = LoggerFactory.getLogger(BottomVision.class);

    private CvPipeline pipeline;

    public BottomVision(CvPipeline pipeline) {
        System.out.println(pipeline.getStages());
        this.pipeline = pipeline;
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
        
        pipeline.setCamera(camera);
        pipeline.process();
        
        CameraView cameraView = MainFrame.mainFrame.cameraPanel.getCameraView(camera);
        cameraView.showFilteredImage(OpenCvUtils.toBufferedImage(pipeline.getWorkingImage()), 3000);
        
        Result result = pipeline.getResult("result");
        RotatedRect rect = (RotatedRect) result.model;
        System.out.println("rect " + rect);

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
