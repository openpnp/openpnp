package org.openpnp.spi.base;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.swing.Icon;

import org.openpnp.CameraListener;
import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.Icons;
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
    
    @Attribute(required=false)
    protected String name;
    
    @Attribute
    protected Looking looking = Looking.Down;
    
    @Element
    protected Location unitsPerPixel = new Location(LengthUnit.Millimeters);
    
    @Element(required=false)
    protected VisionProvider visionProvider;
    
    @Attribute(required=false)
    protected long settleTimeMs = 250;
    
    protected Set<ListenerEntry> listeners = Collections.synchronizedSet(new HashSet<>());
    
    protected Head head;
    
    protected Integer width;
    
    protected Integer height;
    
    public AbstractCamera() {
        this.id = Configuration.createId();
        this.name = getClass().getSimpleName();
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
    
    public BufferedImage settleAndCapture() {
    	try {
    		Thread.sleep(getSettleTimeMs());
    	}
    	catch (Exception e) {
    		
    	}
    	return capture();
    }
    
    protected void broadcastCapture(BufferedImage img) {
        for (ListenerEntry listener : new ArrayList<>(listeners)) {
            if (listener.lastFrameSent < (System.currentTimeMillis() - (1000 / listener.maximumFps))) {
                listener.listener.frameReceived(img);
                listener.lastFrameSent = System.currentTimeMillis();
            }
        }
    }
    
    @Override
    public int getWidth() {
        if (width == null) {
            BufferedImage image = capture();
            width = image.getWidth();
            height = image.getHeight();
        }
        return width;
    }

    @Override
    public int getHeight() {
        if (width == null) {
            BufferedImage image = capture();
            width = image.getWidth();
            height = image.getHeight();
        }
        return height;
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
