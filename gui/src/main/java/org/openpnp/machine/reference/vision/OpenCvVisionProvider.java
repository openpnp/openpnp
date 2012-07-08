package org.openpnp.machine.reference.vision;

import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_32F;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvMinMaxLoc;
import static com.googlecode.javacv.cpp.opencv_core.cvSize;
import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvMatchTemplate;

import java.awt.Point;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;

import org.openpnp.gui.support.Wizard;
import org.openpnp.spi.Camera;
import org.openpnp.spi.VisionProvider;
import org.simpleframework.xml.Attribute;

import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_imgproc;

public class OpenCvVisionProvider implements VisionProvider {
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
        cvMatchTemplate(image, templateImage, res, opencv_imgproc.CV_TM_CCOEFF);
        cvMinMaxLoc(res, minVal, maxVal, minLoc, maxLoc, null);
        CvPoint resLoc = maxLoc;
        
        cvLine(res, maxLoc, cvPoint(maxLoc.x() + templateImage.width(), maxLoc.y()), CvScalar.RED, 1, CV_AA, 0);
        cvLine(res, maxLoc, cvPoint(maxLoc.x(), maxLoc.y() + templateImage.height()), CvScalar.RED, 1, CV_AA, 0);
        
//        BufferedImage r = new BufferedImage(res.width(), res.height(), BufferedImage.TYPE_INT_ARGB);
//        res.copyTo(r);
//        debugger.getImage1().setIcon(new ImageIcon(templateImage.getBufferedImage()));
//        debugger.getImage2().setIcon(new ImageIcon(image.getBufferedImage()));
//        debugger.getImage3().setIcon(new ImageIcon(r));
        return new Point[] { new Point(resLoc.x(), resLoc.y()) };
	}

}
