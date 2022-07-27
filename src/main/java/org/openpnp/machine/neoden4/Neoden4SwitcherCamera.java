package org.openpnp.machine.neoden4;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.neoden4.wizards.Neoden4SwitcherCameraConfigurationWizard;
import org.openpnp.machine.reference.camera.ReferenceCamera;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Camera;
import org.openpnp.spi.PropertySheetHolder;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;

public class Neoden4SwitcherCamera extends ReferenceCamera {
    @Attribute(required=false)
    private int switcher = 0;
    
    @Attribute(required=false)
    private String cameraId;

    @Attribute(required = false)
    private int exposure = 25;
    @Attribute(required = false)
    private int gain = 8;
    
    private static Map<Integer, Camera> switchers = new HashMap<>();
    
    protected int getCaptureTryCount() {
        return 2;
    }

    @Override
	public synchronized BufferedImage internalCapture() {
		if (!ensureOpen()) {
			return null;
		}
		synchronized (switchers) {
			if (switchers.get(switcher) != this) {
				switchers.put(switcher, this);
			}
			
			Neoden4Camera neodenCam = null;
			for (Camera c : Configuration.get().getMachine().getAllCameras()) {
				if (c instanceof Neoden4Camera) {
					neodenCam = (Neoden4Camera) c;
					break;
				}
			}
			if (neodenCam == null) {
				Logger.error("Can't find Neoden4Camera!");
				return null;
			} 
			else {
				neodenCam.setCameraId(switcher);
				neodenCam.setCameraExposureAndGain(exposure, gain); 
			}
		}
        // Note, the target camera is actually a capture device with multiple analog cameras connected via multiplexer. 
        // Each analog camera can have a different lens attached and may be subject to different mounting imperfections, 
        // therefore each SwitcherCamera must have its own set of lens calibration and transforms. 
        // The target camera device however must not apply any calibration or transform, hence the raw capture.  
        return getCamera().captureRaw();
    }

    @Override
    public boolean hasNewFrame() {
        if (!isOpen()) {
            return false;
        }
        synchronized (switchers) {
            if (switchers.get(switcher) != this) {
                // Always assume an off-switched camera has a new frame.
                return true;
            }
        }
        return getCamera().hasNewFrame();
    }

    @Override
    protected synchronized boolean ensureOpen() {
        if (getCamera() == null) {
            return false;
        }
        return super.ensureOpen();
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return null;
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new Neoden4SwitcherCameraConfigurationWizard(this);
    }

    public String getCameraId() {
        return cameraId;
    }

    public void setCameraId(String cameraId) {
        this.cameraId = cameraId;
        firePropertyChange("cameraId", null, cameraId);
    }

    public int getSwitcher() {
        return switcher;
    }

    public void setSwitcher(int switcher) {
        this.switcher = switcher;
        firePropertyChange("switcher", null, switcher);
    }

    public Camera getCamera() {
        return Configuration.get().getMachine().getCamera(cameraId);
    }

    public void setCamera(Camera camera) {
        if (camera == null) {
            setCameraId(null);
        }
        else {
            setCameraId(camera.getId());
        }
        firePropertyChange("camera", null, camera);
    }

    public int getExposure() {
        return this.exposure;
    }

    public void setExposure(int exposure) {
        this.exposure = exposure;
    }

    public int getGain() {
        return this.gain;
    }

    public void setGain(int gain) {
        this.gain = gain;
    }
}
