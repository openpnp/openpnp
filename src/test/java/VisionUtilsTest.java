import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.Action;
import javax.swing.Icon;

import org.junit.Assert;
import org.junit.Test;
import org.openpnp.CameraListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.VisionProvider;
import org.openpnp.util.VisionUtils;


public class VisionUtilsTest {
    @Test
    public void testOffsets() {
        Camera camera = new TestCamera();
        Location location = camera.getLocation();
        Assert.assertEquals(location, new Location(LengthUnit.Millimeters, 0, 0, 0, 0));
        Assert.assertEquals(camera.getWidth(), 640);
        Assert.assertEquals(camera.getHeight(), 480);
        Location pixelOffsets = VisionUtils.getPixelCenterOffsets(camera, 100, 100);
        Assert.assertEquals(pixelOffsets, new Location(LengthUnit.Millimeters, -220, 140, 0, 0));
        Location pixelLocation = VisionUtils.getPixelLocation(camera, 100, 100);
        Assert.assertEquals(pixelLocation, new Location(LengthUnit.Millimeters, -220, 140, 0, 0));
    }

    static class TestCamera implements Camera {
        @Override
        public String getId() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Head getHead() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setHead(Head head) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void moveTo(Location location, double speed) throws Exception {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void moveToSafeZ(double speed) throws Exception {
            // TODO Auto-generated method stub
            
        }

        @Override
        public Location getLocation() {
            return new Location(LengthUnit.Millimeters, 0, 0, 0, 0);
        }

        @Override
        public Wizard getConfigurationWizard() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getPropertySheetHolderTitle() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public PropertySheetHolder[] getChildPropertySheetHolders() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public PropertySheet[] getPropertySheets() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Action[] getPropertySheetHolderActions() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getName() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setName(String name) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public Looking getLooking() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setLooking(Looking looking) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public Location getUnitsPerPixel() {
            return new Location(LengthUnit.Millimeters, 1, 1, 0, 0);
        }

        @Override
        public void setUnitsPerPixel(Location unitsPerPixel) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public BufferedImage capture() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void startContinuousCapture(CameraListener listener,
                int maximumFps) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void stopContinuousCapture(CameraListener listener) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setVisionProvider(VisionProvider visionProvider) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public VisionProvider getVisionProvider() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public int getWidth() {
            return 640;
        }

        @Override
        public int getHeight() {
            return 480;
        }

        @Override
        public Icon getPropertySheetHolderIcon() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void close() throws IOException {
            // TODO Auto-generated method stub
            
        }

		@Override
		public BufferedImage settleAndCapture() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getSettleTimeMs() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void setSettleTimeMs(long settleTimeMs) {
			// TODO Auto-generated method stub
			
		}
    }
}
