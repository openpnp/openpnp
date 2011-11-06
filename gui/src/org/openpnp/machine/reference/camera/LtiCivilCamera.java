package org.openpnp.machine.reference.camera;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.openpnp.Configuration;
import org.w3c.dom.Node;

import com.lti.civil.CaptureDeviceInfo;
import com.lti.civil.CaptureException;
import com.lti.civil.CaptureObserver;
import com.lti.civil.CaptureStream;
import com.lti.civil.CaptureSystem;
import com.lti.civil.CaptureSystemFactory;
import com.lti.civil.DefaultCaptureSystemFactorySingleton;
import com.lti.civil.Image;
import com.lti.civil.VideoFormat;
import com.lti.civil.awt.AWTImageConverter;

/**
<pre>
{@code
<!-- 
	Specify deviceId="" to get a list of available devices on the console. 
-->
<Configuration deviceId="?" />
}
</pre>
 */
public class LtiCivilCamera extends AbstractCamera implements CaptureObserver {
	private CaptureSystemFactory captureSystemFactory;
	private CaptureSystem captureSystem;
	private CaptureStream captureStream;
	private VideoFormat videoFormat;

	private String deviceId;
	private boolean forceGrayscale;
	
	private int width, height;
	
	private BufferedImage grayImage;
	
	@Override
	public void configure(Node n) throws Exception {
		XPath xpath = XPathFactory.newInstance().newXPath();
		
		deviceId = Configuration.getAttribute(n, "deviceId");
		
		captureSystemFactory = DefaultCaptureSystemFactorySingleton.instance();
		captureSystem = captureSystemFactory.createCaptureSystem();
		
		if (deviceId == null || deviceId.trim().length() == 0) {
			// TODO make this a dialog
			System.out.println("No deviceId specified for LtiCivilCamera [" + getName() + "]. Available deviceIds are:");
			System.out.println();
			for (CaptureDeviceInfo captureDeviceInfo : (List<CaptureDeviceInfo>) captureSystem.getCaptureDeviceInfoList()) {
				System.out.println("\"" + captureDeviceInfo.getDeviceID() + "\"");
			}
			System.out.println();
			System.out.println("Please specify one of the available deviceIds in the deviceId attribute of the Configuration for this Camera.");
			System.exit(1);
		}
		
		forceGrayscale = Configuration.getBooleanAttribute(n, "forceGrayscale");
		
		captureStream = captureSystem.openCaptureDeviceStream(deviceId);
		videoFormat = captureStream.getVideoFormat();
		width = videoFormat.getWidth();
		height = videoFormat.getHeight();
		System.out.println("Camera " + getName() + " dimensions are " + width + ", " + height);
		captureStream.setObserver(this);
		captureStream.start();
	}
	
	@Override
	public void onError(CaptureStream captureStream, CaptureException captureException) {
	}

	@Override
	// TODO should probably turn off the stream if there are no listeners, to save CPU
	public void onNewImage(CaptureStream captureStream, Image newImage) {
		if (listeners.size() > 0) {
			BufferedImage bImage = AWTImageConverter.toBufferedImage(newImage);
			if (forceGrayscale) {
				if (grayImage == null) {
					grayImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
				}
				Graphics g = grayImage.getGraphics();
				g.drawImage(bImage, 0, 0, null);  
				g.dispose();
				broadcastCapture(grayImage);
			}
			else {
				broadcastCapture(bImage);
			}
		}
	}

	@Override
	public BufferedImage capture() {
		// TODO not implemented
		throw new Error("LtiCivilCamera.capture() not yet implemented.");
	}
}
