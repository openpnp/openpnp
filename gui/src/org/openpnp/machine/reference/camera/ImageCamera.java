package org.openpnp.machine.reference.camera;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.openpnp.model.Location;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.core.Commit;


/**
 * An implementation of Camera that returns a viewport into a field of
 * located images. This allows you to create a virtual camera that
 * uses multiple existing images to lay out an entire table view. 
 */
public class ImageCamera extends AbstractCamera implements Runnable {
	@ElementList
	private List<ImageLocation> imageLocations = new ArrayList<ImageLocation>();
	
	private BufferedImage image;
	
	@Commit
	private void commit() {
//		try {
//			image = ImageIO.read(new File(imageFile));
//		}
//		catch (Exception e) {
//			e.printStackTrace();
//		}
//		new Thread(this).start();
	}
	
	public void run() {
		while (true) {
			if (listeners.size() > 0) {
				broadcastCapture(image);
			}
			try {
				Thread.sleep(1000 / 10);
			}
			catch (Exception e) {
				
			}
		}
	}
	
	@Override
	public BufferedImage capture() {
		return image;
	}
	
	public static class ImageLocation {
		@Attribute
		private String file;
		@Element
		private Location location;
	}
}
