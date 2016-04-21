package org.openpnp.machine.reference.vision;

import javax.swing.Action;
import javax.swing.Icon;

import org.opencv.core.RotatedRect;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.machine.reference.vision.wizards.ReferenceBottomVisionConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PartAlignment;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage.Result;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: Add the Part as context to the pipeline and use the height to scale the circle mask
// to account for the smaller circle as the nozzle raises.
public class ReferenceBottomVision implements PartAlignment {
    private static final Logger logger = LoggerFactory.getLogger(ReferenceBottomVision.class);

    @Element(required = false)
    protected CvPipeline pipeline = new CvPipeline("<pipeline>" + "            <stages>"
            + "               <cv-stage class=\"org.openpnp.vision.pipeline.stages.ImageCapture\" name=\"0\" enabled=\"true\" settle-first=\"true\"/>"
            + "               <cv-stage class=\"org.openpnp.vision.pipeline.stages.ImageWriteDebug\" name=\"13\" enabled=\"true\" prefix=\"bv_source_\" suffix=\".png\"/>"
            + "               <cv-stage class=\"org.openpnp.vision.pipeline.stages.BlurGaussian\" name=\"10\" enabled=\"true\" kernel-size=\"9\"/>"
            + "               <cv-stage class=\"org.openpnp.vision.pipeline.stages.MaskCircle\" name=\"4\" enabled=\"true\" diameter=\"525\"/>"
            + "               <cv-stage class=\"org.openpnp.vision.pipeline.stages.ConvertColor\" name=\"1\" enabled=\"true\" conversion=\"Bgr2HsvFull\"/>"
            + "               <cv-stage class=\"org.openpnp.vision.pipeline.stages.MaskHsv\" name=\"2\" enabled=\"true\" hue-min=\"60\" hue-max=\"130\" saturation-min=\"0\" saturation-max=\"255\" value-min=\"0\" value-max=\"255\"/>"
            + "               <cv-stage class=\"org.openpnp.vision.pipeline.stages.ConvertColor\" name=\"3\" enabled=\"true\" conversion=\"Hsv2BgrFull\"/>"
            + "               <cv-stage class=\"org.openpnp.vision.pipeline.stages.ConvertColor\" name=\"6\" enabled=\"true\" conversion=\"Bgr2Gray\"/>"
            + "               <cv-stage class=\"org.openpnp.vision.pipeline.stages.Threshold\" name=\"12\" enabled=\"true\" threshold=\"100\" auto=\"false\" invert=\"false\"/>"
            + "               <cv-stage class=\"org.openpnp.vision.pipeline.stages.FindContours\" name=\"5\" enabled=\"true\" retrieval-mode=\"List\" approximation-method=\"None\"/>"
            + "               <cv-stage class=\"org.openpnp.vision.pipeline.stages.FilterContours\" name=\"9\" enabled=\"true\" contours-stage-name=\"5\" min-area=\"50.0\" max-area=\"900000.0\"/>"
            + "               <cv-stage class=\"org.openpnp.vision.pipeline.stages.MaskCircle\" name=\"11\" enabled=\"true\" diameter=\"0\"/>"
            + "               <cv-stage class=\"org.openpnp.vision.pipeline.stages.DrawContours\" name=\"7\" enabled=\"true\" contours-stage-name=\"9\" thickness=\"2\" index=\"-1\">"
            + "                  <color r=\"255\" g=\"255\" b=\"255\" a=\"255\"/>"
            + "               </cv-stage>"
            + "               <cv-stage class=\"org.openpnp.vision.pipeline.stages.MinAreaRect\" name=\"result\" enabled=\"true\" threshold-min=\"100\" threshold-max=\"255\"/>"
            + "               <cv-stage class=\"org.openpnp.vision.pipeline.stages.ImageRecall\" name=\"14\" enabled=\"true\" image-stage-name=\"0\"/>"
            + "               <cv-stage class=\"org.openpnp.vision.pipeline.stages.DrawRotatedRects\" name=\"8\" enabled=\"true\" rotated-rects-stage-name=\"result\" thickness=\"2\"/>"
            + "               <cv-stage class=\"org.openpnp.vision.pipeline.stages.ImageWriteDebug\" name=\"15\" enabled=\"true\" prefix=\"bv_result_\" suffix=\".png\"/>"
            + "            </stages>" + "         </pipeline>");



    @Attribute(required = false)
    protected boolean enabled = false;

    @Override
    public Location findOffsets(Part part, Nozzle nozzle) throws Exception {
        if (!enabled) {
            return new Location(LengthUnit.Millimeters);
        }

        Camera camera = getBottomVisionCamera();

        // Create a location that is the Camera's X, Y, it's Z + part height
        // and a rotation of 0.
        Location startLocation = camera.getLocation();
        Length partHeight = part.getHeight();
        Location partHeightLocation =
                new Location(partHeight.getUnits(), 0, 0, partHeight.getValue(), 0);
        startLocation = startLocation.add(partHeightLocation).derive(null, null, null, 0d);

        MovableUtils.moveToLocationAtSafeZ(nozzle, startLocation, part.getSpeed());

        pipeline.setCamera(camera);
        pipeline.process();

        CameraView cameraView = MainFrame.mainFrame.cameraPanel.getCameraView(camera);
        cameraView.showFilteredImage(OpenCvUtils.toBufferedImage(pipeline.getWorkingImage()), 1500);

        Result result = pipeline.getResult("result");
        RotatedRect rect = (RotatedRect) result.model;
        logger.debug("Result rect {}", rect);

        // Create the offsets object. This is the physical distance from
        // the center of the camera to the located part.
        Location offsets = VisionUtils.getPixelCenterOffsets(camera, rect.center.x, rect.center.y);

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
        offsets = offsets.derive(null, null, null, -angle);
        logger.debug("Final offsets {}", offsets);

        return offsets;
    }

    private Camera getBottomVisionCamera() throws Exception {
        for (Camera camera : Configuration.get().getMachine().getCameras()) {
            if (camera.getLooking() == Camera.Looking.Up) {
                return camera;
            }
        }
        throw new Exception("No up-looking camera found on the machine to use for bottom vision.");
    }

    public CvPipeline getPipeline() {
        try {
            pipeline.setCamera(getBottomVisionCamera());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return pipeline;
    }

    public void setPipeline(CvPipeline pipeline) {
        this.pipeline = pipeline;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return "Bottom Vision";
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
                new PropertySheetWizardAdapter(new ReferenceBottomVisionConfigurationWizard(this))};
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
}
