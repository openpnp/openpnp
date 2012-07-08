package org.openpnp.machine.reference.vision;

import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_32F;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvMinMaxLoc;
import static com.googlecode.javacv.cpp.opencv_core.cvSize;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvMatchTemplate;

import java.awt.Point;
import java.awt.image.BufferedImage;

import org.openpnp.gui.support.Wizard;
import org.openpnp.spi.Camera;
import org.openpnp.spi.VisionProvider;
import org.simpleframework.xml.Attribute;

import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_imgproc;

public class OpenCvVisionProvider implements VisionProvider {
	@Attribute(required=false)
	private String dummy;
	
	private Camera camera;
	
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
	public Circle[] locateCircles(int roiX1, int roiY1, int roiX2, int roiY2,
			int poiX, int poiY, int minimumDiameter, int diameter,
			int maximumDiameter) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Point[] locateTemplateMatches(int roiX1, int roiY1, int roiX2,
			int roiY2, int poiX, int poiY, BufferedImage templateImage_)
			throws Exception {
    	double minVal[] = new double[1];
    	double maxVal[] = new double[1];
    	CvPoint minLoc = new CvPoint();
    	CvPoint maxLoc = new CvPoint();
    	
        IplImage templateImage = IplImage.createFrom(templateImage_);
        BufferedImage rawImage = camera.capture();
        IplImage image = IplImage.createFrom(rawImage);
        IplImage res = cvCreateImage(
        		cvSize(
        				image.width() - templateImage.width() + 1, 
        				image.height() - templateImage.height() + 1),
        		IPL_DEPTH_32F, 1);
        System.out.println(templateImage.origin());
        System.out.println(image.origin());
        System.out.println(res.origin());
        cvMatchTemplate(image, templateImage, res, opencv_imgproc.CV_TM_CCOEFF);
        cvMinMaxLoc( res, minVal, maxVal, minLoc, maxLoc, null );
        CvPoint resLoc = maxLoc;
        return new Point[] { new Point(resLoc.x(), resLoc.y()) };
	}

}
