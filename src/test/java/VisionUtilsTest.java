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
        protected Head head;

        @Override
        public String getId() {
            return null;
        }

        @Override
        public Head getHead() {
            return head;
        }

        @Override
        public void setHead(Head head) {
            this.head = head;
        }

        @Override
        public void moveTo(Location location, double speed) throws Exception {

        }

        @Override
        public void moveToSafeZ(double speed) throws Exception {

        }

        @Override
        public Location getLocation() {
            return new Location(LengthUnit.Millimeters, 0, 0, 0, 0);
        }

        @Override
        public Wizard getConfigurationWizard() {
            return null;
        }

        @Override
        public String getPropertySheetHolderTitle() {
            return null;
        }

        @Override
        public PropertySheetHolder[] getChildPropertySheetHolders() {
            return null;
        }

        @Override
        public PropertySheet[] getPropertySheets() {
            return null;
        }

        @Override
        public Action[] getPropertySheetHolderActions() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public void setName(String name) {

        }

        @Override
        public Looking getLooking() {
            return null;
        }

        @Override
        public void setLooking(Looking looking) {

        }

        @Override
        public Location getUnitsPerPixel() {
            return new Location(LengthUnit.Millimeters, 1, 1, 0, 0);
        }

        @Override
        public void setUnitsPerPixel(Location unitsPerPixel) {

        }

        @Override
        public BufferedImage capture() {
            return null;
        }

        @Override
        public void startContinuousCapture(CameraListener listener, int maximumFps) {

        }

        @Override
        public void stopContinuousCapture(CameraListener listener) {

        }

        @Override
        public void setVisionProvider(VisionProvider visionProvider) {

        }

        @Override
        public VisionProvider getVisionProvider() {
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
            return null;
        }

        @Override
        public void close() throws IOException {

        }

        @Override
        public BufferedImage settleAndCapture() {
            return null;
        }

        @Override
        public long getSettleTimeMs() {
            return 0;
        }

        @Override
        public void setSettleTimeMs(long settleTimeMs) {

        }

        @Override
        public void moveTo(Location location) throws Exception {
            moveTo(location, getHead().getMachine().getSpeed());
        }

        @Override
        public void moveToSafeZ() throws Exception {
            moveToSafeZ(getHead().getMachine().getSpeed());
        }
    }
}
