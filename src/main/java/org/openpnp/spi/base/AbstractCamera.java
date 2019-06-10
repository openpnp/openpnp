package org.openpnp.spi.base;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.Icon;

import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.openpnp.CameraListener;
import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.Icons;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.VisionProvider;
import org.openpnp.util.OpenCvUtils;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public abstract class AbstractCamera extends AbstractModelObject implements Camera {
    @Attribute
    protected String id;

    @Attribute(required = false)
    protected String name;

    @Attribute
    protected Looking looking = Looking.Down;

    @Element
    protected Location unitsPerPixel = new Location(LengthUnit.Millimeters);

    @Element(required = false)
    protected VisionProvider visionProvider;

    @Attribute(required = false)
    protected long settleTimeMs = 250;

    protected Set<ListenerEntry> listeners = Collections.synchronizedSet(new HashSet<>());

    protected Head head;

    protected Integer width;

    protected Integer height;
    
    private boolean headSet = false;
    
    private Mat lastSettleMat = null;

    public AbstractCamera() {
        this.id = Configuration.createId("CAM");
        this.name = getClass().getSimpleName();
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationLoaded(Configuration configuration) throws Exception {
                if (visionProvider != null) {
                    visionProvider.setCamera(AbstractCamera.this);
                }
            }
        });
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
        firePropertyChange("name", null, name);
    }

    @Override
    public Head getHead() {
        return head;
    }

    @Override
    public void setHead(Head head) {
        if (this.headSet) {
            throw new Error("Can't change head on camera " + this);
        }
        this.head = head;
        this.headSet = true;
    }

    @Override
    public Location getCameraToolCalibratedOffset(Camera camera) {
        return new Location(camera.getUnitsPerPixel().getUnits());
    }

    @Override
    public Location getLocation(HeadMountable tool) {
        if (tool != null) {
            return getLocation().subtract(tool.getCameraToolCalibratedOffset(this));
        }

        return getLocation();
    }

    @Override
    public Location getUnitsPerPixel() {
        return unitsPerPixel;
    }

    @Override
    public void setUnitsPerPixel(Location unitsPerPixel) {
        this.unitsPerPixel = unitsPerPixel;
    }

    @Override
    public void setLooking(Looking looking) {
        this.looking = looking;
        firePropertyChange("looking", null, looking);
    }

    @Override
    public Looking getLooking() {
        return looking;
    }

    @Override
    public void startContinuousCapture(CameraListener listener) {
        listeners.add(new ListenerEntry(listener));
    }

    @Override
    public void stopContinuousCapture(CameraListener listener) {
        listeners.remove(new ListenerEntry(listener));
    }

    @Override
    public void setVisionProvider(VisionProvider visionProvider) {
        this.visionProvider = visionProvider;
        visionProvider.setCamera(this);
    }

    @Override
    public VisionProvider getVisionProvider() {
        return visionProvider;
    }
    
    private BufferedImage autoSettleAndCapture() {
        long t = System.currentTimeMillis();
        while (true) {
            // Capture an image, convert to Mat and convert to gray.
            BufferedImage image = capture();
            Mat mat = OpenCvUtils.toMat(image);
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
            
            // If this is the first time through the loop then assign the new image to
            // the lastSettleMat and loop again. We need at least two images to check.
            // to lastSettleMat and 
            if (lastSettleMat == null) {
                lastSettleMat = mat;
                continue;
            }
            
            // Take the absdiff of the two images and get the max changed pixel.
            Mat diff = new Mat();
            Core.absdiff(lastSettleMat, mat, diff);
            MinMaxLocResult result = Core.minMaxLoc(diff);
            Logger.debug("autoSettleAndCapture auto settle score: " + result.maxVal);
            diff.release();

            // Release the lastSettleMat and store the new image as the lastSettleMat.
            lastSettleMat.release();
            lastSettleMat = mat;

            // If the image changed at least a bit (due to noise) and and less than our
            // threshold, we have a winner. The check for > 0 is to ensure that we're not just
            // receiving a duplicate frame from the camera. Every camera has at least a little
            // noise so we're just checking that at least one pixel changed by 1 bit.
            if (result.maxVal > 0 && result.maxVal < Math.abs(getSettleTimeMs())) {
                lastSettleMat.release();
                lastSettleMat = null;
                Logger.debug("autoSettleAndCapture in {} ms", System.currentTimeMillis() - t);
                return image;
            }
        }
    }

    public BufferedImage settleAndCapture() {
        try {
            Map<String, Object> globals = new HashMap<>();
            globals.put("camera", this);
            Configuration.get().getScripting().on("Camera.BeforeSettle", globals);
        }
        catch (Exception e) {
            Logger.warn(e);
        }
        
    	
        if (getSettleTimeMs() >= 0) {
            try {
                Thread.sleep(getSettleTimeMs());
            }
            catch (Exception e) {

            }
            return capture();
        }
        else {
            return autoSettleAndCapture();
        }
    }

    protected void broadcastCapture(BufferedImage img) {
        for (ListenerEntry listener : new ArrayList<>(listeners)) {
            listener.listener.frameReceived(img);
        }
    }

    public long getSettleTimeMs() {
        return settleTimeMs;
    }

    public void setSettleTimeMs(long settleTimeMs) {
        this.settleTimeMs = settleTimeMs;
    }

    @Override
    public Icon getPropertySheetHolderIcon() {
        return Icons.captureCamera;
    }
    
    @Override
    public void moveTo(Location location) throws Exception {
        moveTo(location, getHead().getMachine().getSpeed());
    }

    @Override
    public void moveToSafeZ() throws Exception {
        moveToSafeZ(getHead().getMachine().getSpeed());
    }
    
    @Override
    public String toString() {
        return getName();
    }
    
    protected class ListenerEntry {
        public CameraListener listener;
        public long lastFrameSent;

        public ListenerEntry(CameraListener listener) {
            this.listener = listener;
        }

        @Override
        public int hashCode() {
            return listener.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj.equals(listener);
        }
    }
}
