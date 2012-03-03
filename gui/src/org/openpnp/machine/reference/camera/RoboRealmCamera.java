package org.openpnp.machine.reference.camera;

import java.awt.image.BufferedImage;

import org.openpnp.CameraListener;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.vision.roborealm.RR_API;
import org.openpnp.spi.Camera;
import org.simpleframework.xml.Attribute;

public class RoboRealmCamera extends AbstractCamera implements CameraListener {
	@Attribute
	private String host;
	@Attribute
	private int port;
	@Attribute
	private String sourceCameraName;
	
	private RR_API roboRealm = new RR_API();
	
	private Camera sourceCamera;

	@Override
	public void frameReceived(BufferedImage image) {
		try {
			int width = image.getWidth();
			int height = image.getHeight();
			int rgbInt[] = new int[width * height];
			image.getRGB(0, 0, width, height, rgbInt, 0, width);
			byte rgbByte[] = new byte[width * height * 3];
			int i, j;
			for (j = i = 0; i < width * height;) {
				int num = rgbInt[i++];
				rgbByte[j++] = (byte) (num & 255);
				rgbByte[j++] = (byte) ((num >> 8) & 255);
				rgbByte[j++] = (byte) ((num >> 16) & 255);
			}
			roboRealm.setImage(rgbByte, width, height);
			
			rgbInt = roboRealm.getImage("processed");
			image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
			image.setRGB(0, 0, width, height, rgbInt, 0, width);
			broadcastCapture(image);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setReferenceMachine(ReferenceMachine machine) throws Exception {
		super.setReferenceMachine(machine);
		for (Camera camera : machine.getCameras()) {
			if (camera.getName().equals(sourceCameraName)) {
				sourceCamera = camera;
			}
		}
		roboRealm.connect(host, port);
		sourceCamera.startContinuousCapture(this, 10);
	}

	@Override
	public BufferedImage capture() {
		return null;
	}
}
