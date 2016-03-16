//package org.openpnp.vision.pipeline.stages;
//
//import java.awt.image.BufferedImage;
//
//import org.openpnp.spi.Camera;
//import org.openpnp.util.OpenCvUtils;
//import org.openpnp.vision.pipeline.CvPipeline;
//import org.openpnp.vision.pipeline.CvStage;
//import org.simpleframework.xml.Attribute;
//
///**
// * Generate an image from a variety of native types. Uses reflection to determine the type of
// * the input and attempts to generate an image from it based on the provided parameters. 
// */
//public class GenerateImage extends CvStage {
//    @Attribute
//    private String modelStageName;
//    
//    @Override
//    public Result process(CvPipeline pipeline) throws Exception {
//        if (modelStageName == null) {
//            throw new Exception("modelStageName is required.");
//        }
//        
//        
//        
//        Camera camera = pipeline.getCamera();
//        if (camera == null) {
//            throw new Exception("No Camera set on pipeline.");
//        }
////        BufferedImage image;
////        if (settleFirst) {
////            image = camera.settleAndCapture();
////        }
////        else {
////            image = camera.capture();
////        }
//        return new Result(OpenCvUtils.toMat(image));
//    }
//}
