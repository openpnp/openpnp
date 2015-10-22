import java.awt.image.BufferedImage;

import org.junit.Test;
import org.openpnp.vision.FluentCv;

public class OpenCvTest {
	/**
	 * Just tests to make sure OpenCV is working. This is primarily to catch
	 * any issues on non-Mac platform builds.
	 */
	@Test
	public void openCvWorks() throws Exception {
		BufferedImage img = new BufferedImage(640, 480, BufferedImage.TYPE_3BYTE_BGR);
		new FluentCv().toMat(img).toGray();
	}
}
