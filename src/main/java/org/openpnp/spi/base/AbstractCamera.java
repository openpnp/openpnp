package org.openpnp.spi.base;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.openpnp.CameraListener;
import org.openpnp.ConfigurationListener;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.VisionProvider;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public abstract class AbstractCamera implements Camera {
    @Attribute
    protected String id;
    
    @Attribute
    protected Looking looking = Looking.Down;
    
    @Element
    protected Location unitsPerPixel = new Location(LengthUnit.Millimeters);
    
    @Element(required=false)
    protected VisionProvider visionProvider;
    
    @Attribute(required=false)
    protected double rotation = 0;
    
    protected Set<ListenerEntry> listeners = Collections.synchronizedSet(new HashSet<ListenerEntry>());
    
    protected Head head;
    
    public AbstractCamera() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationLoaded(Configuration configuration)
                    throws Exception {
                if (visionProvider != null) {
                    visionProvider.setCamera(AbstractCamera.this);
                }
            }
        });
    }
    
    @Override
    public void setId(String id) {
        if (this.id != null) {
            throw new Error("Can't set id once it has been assigned");
        }
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
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
    }

    @Override
    public Looking getLooking() {
        return looking;
    }
    
    @Override
    public void startContinuousCapture(CameraListener listener, int maximumFps) {
        listeners.add(new ListenerEntry(listener, maximumFps));
    }

    @Override
    public void stopContinuousCapture(CameraListener listener) {
        listeners.remove(new ListenerEntry(listener, 0));
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

    protected void broadcastCapture(BufferedImage img) {
        for (ListenerEntry listener : listeners) {
            if (listener.lastFrameSent < (System.currentTimeMillis() - (1000 / listener.maximumFps))) {
                listener.listener.frameReceived(img);
                listener.lastFrameSent = System.currentTimeMillis();
            }
        }
    }
    
    protected BufferedImage applyRotation(BufferedImage image) {
        if (rotation == 0) {
            return image;
        }
        // Create a rotation transform to determine how big the resulting
        // rotated image should be.
        AffineTransform xform = new AffineTransform();
        xform.rotate(Math.toRadians(-rotation));
        Rectangle2D r2d = xform.createTransformedShape(new Rectangle2D.Double(0, 0, image.getWidth(), image.getHeight())).getBounds2D();
        int width = (int) r2d.getWidth();
        int height = (int) r2d.getHeight();
        BufferedImage out = new BufferedImage(width, height, image.getType());
        Graphics2D g2d = out.createGraphics();
        // Create the transform we'll actually use to rotate the image.
        xform = new AffineTransform();
        // Translate the source to the center of the output.
        xform.translate(out.getWidth() / 2, out.getHeight() / 2);
        // Rotate the image.
        xform.rotate(Math.toRadians(-rotation));
        // Translate the image to it's center so the rotation happens about
        // the centerpoint.
        xform.translate(-image.getWidth() / 2, -image.getHeight() / 2);
        g2d.drawImage(image, xform, null);
        g2d.dispose();
        return out;
    }

    protected class ListenerEntry {
        public CameraListener listener;
        public int maximumFps;
        public long lastFrameSent;

        public ListenerEntry(CameraListener listener, int maximumFps) {
            this.listener = listener;
            this.maximumFps = maximumFps;
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
