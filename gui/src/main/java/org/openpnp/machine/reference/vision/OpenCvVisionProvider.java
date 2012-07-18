package org.openpnp.machine.reference.vision;

import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_32F;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvMinMaxLoc;
import static com.googlecode.javacv.cpp.opencv_core.cvRect;
import static com.googlecode.javacv.cpp.opencv_core.cvResetImageROI;
import static com.googlecode.javacv.cpp.opencv_core.cvSetImageROI;
import static com.googlecode.javacv.cpp.opencv_core.cvSize;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvMatchTemplate;

import java.awt.Point;
import java.awt.image.BufferedImage;

import org.openpnp.gui.support.Wizard;
import org.openpnp.spi.Camera;
import org.openpnp.spi.VisionProvider;
import org.simpleframework.xml.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_imgproc;

public class OpenCvVisionProvider implements VisionProvider {
	private final static Logger logger = LoggerFactory.getLogger(OpenCvVisionProvider.class);
	
	// SimpleXML requires at least one attribute or element on a class before
	// it will recognize it.
	@Attribute(required=false)
	private String dummy;
	
	private Camera camera;
	
//	private OpenCvVisionProviderDebugger debugger = new OpenCvVisionProviderDebugger();
	
	@Override
	public void setCamera(Camera camera) {
		this.camera = camera;
	}

	@Override
	public Wizard getConfigurationWizard() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Circle[] locateCircles(int roiX, int roiY, int roiWidth, int roiHeight,
			int coiX, int coiY, int minimumDiameter, int diameter,
			int maximumDiameter) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Point[] locateTemplateMatches(int roiX, int roiY, int roiWidth,
			int roiHeight, int coiX, int coiY, BufferedImage templateImage_)
			throws Exception {
    	double minVal[] = new double[1];
    	double maxVal[] = new double[1];
    	CvPoint minLoc = new CvPoint();
    	CvPoint maxLoc = new CvPoint();
        CvPoint resLoc;
        double resValue;
    	
        IplImage templateImage = IplImage.createFrom(templateImage_);
        IplImage image = IplImage.createFrom(camera.capture());
        IplImage result = cvCreateImage(
        		cvSize(roiWidth - templateImage.width() + 1, roiHeight - templateImage.height() + 1),
        		IPL_DEPTH_32F,
        		1);
        
        cvSetImageROI(image, cvRect(roiX, roiY, roiWidth, roiHeight));
        cvMatchTemplate(image, templateImage, result, opencv_imgproc.CV_TM_CCOEFF);
        cvResetImageROI(image);
        cvMinMaxLoc(result, minVal, maxVal, minLoc, maxLoc, null);
        
        resLoc = maxLoc;
        resValue = maxVal[0];
        
        // TODO: Figure out certainty and how to filter on it.
        
        logger.debug(String.format("locateTemplateMatches certainty %f at %d, %d", resValue, resLoc.x(), resLoc.y()));
        
        return new Point[] { new Point(resLoc.x() + roiX, resLoc.y() + roiY) };
	}

}
