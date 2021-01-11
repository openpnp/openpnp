package org.openpnp.machine.reference;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.openpnp.CameraListener;
import org.openpnp.ConfigurationListener;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.openpnp.spi.base.AbstractCamera;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.core.Commit;

/**
 * AbstractPreviewCamera handles the preview capture broadcasting aspects of a Camera. 
 *
 */
public abstract class AbstractBroadcastingCamera extends AbstractCamera implements Runnable {
    // Calling notifyAll on this object will wake the stream thread for one loop to broadcast
    // a new image.
    protected Object captureNotifier = new Object();

    @Attribute(required = false)
    protected double fps = 3;

    @Attribute(required = false)
    protected boolean suspendPreviewInTasks = false;

    private Thread thread;

    /**
     * The lastTransformedImage is produced by transformImage() and consumed by the Camera thread.
     */
    private AtomicReference<BufferedImage> lastTransformedImage = new AtomicReference<>();

    AbstractBroadcastingCamera() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationComplete(Configuration configuration) throws Exception {
                Configuration.get().getMachine().addListener(new MachineListener.Adapter() {
                    @Override
                    public void machineHeadActivity(Machine machine, Head head) {
                        if (!isPreviewSuspended()) {
                            notifyCapture();
                        }
                    }

                    @Override 
                    public void machineEnabled(Machine machine) {
                        notifyCapture();
                    }
                });
            }
        });
    }

    public double getPreviewFps() {
        return fps;
    }

    public void setPreviewFps(double fps) throws Exception {
        Object oldValue = this.fps;
        this.fps = fps;
        firePropertyChange("previewFps", oldValue, fps);
    }

    public boolean isSuspendPreviewInTasks() {
        return suspendPreviewInTasks;
    }

    public void setSuspendPreviewInTasks(boolean suspendPreviewInTasks) {
        Object oldValue = this.suspendPreviewInTasks;
        this.suspendPreviewInTasks = suspendPreviewInTasks;
        firePropertyChange("suspendPreviewInTasks", oldValue, suspendPreviewInTasks);
    }

    protected Thread getThread() {
        return thread;
    }

    protected BufferedImage getLastTransformedImage() {
        return lastTransformedImage.get();
    }

    protected void setLastTransformedImage(BufferedImage lastTransformedImage) {
        this.lastTransformedImage.set(lastTransformedImage);
        notifyCapture();
    }

    protected void notifyCapture() {
        synchronized(captureNotifier) {
            captureNotifier.notifyAll();
        }
    }

    @Override
    public synchronized void startContinuousCapture(CameraListener listener) {
        ensureOpen();
        super.startContinuousCapture(listener);
    }

    @Override
    public synchronized void stopContinuousCapture(CameraListener listener) {
        super.stopContinuousCapture(listener);
        if (listeners.size() == 0) {
            stop();
        }
    }

    /**
     * New settings applied to the Camera are activated by reopening the camera.   
     * 
     * @throws Exception
     */
    public synchronized void reinitialize() throws Exception {
        stop();
        ensureOpen();
    }

    protected synchronized void stop() {
        if (isOpen()) {
            thread.interrupt();
            try {
                thread.join(3000);
            }
            catch (Exception e) {

            }
        }
        thread = null;
    }

    protected synchronized boolean isOpen() {
        return (thread != null && thread.isAlive());
    }

    protected synchronized boolean ensureOpen() {
        if (Configuration.get().getMachine() == null) {
            // For some reason, in some test units, but only when run trough mvn test but not with the debugger, 
            // we get calls to internalCapture() with an uninitialized Machine. 
            return false;
        }
        if (!isOpen()) {
            try {
                open();
            }
            catch (Exception e) {
                Logger.error(e);
                return false;
            }
        }
        return true;
    }

    public synchronized void open() throws Exception {
        stop();
        start();
    }

    @Override
    public void close() throws IOException {
        stop();
    }

    protected synchronized void start() {
        if (!isOpen()) {
            thread = new Thread(this);
            thread.setDaemon(true);
            thread.start();
        }
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {

            try {
                // The camera should reuse images recently captures by on-going computer vision as 
                // every call to captureTransformed() may consume the frame and make it unavailable 
                // to computer vision.  
                // Note, by using the atomic getAndSet() we make sure not to miss the last image.
                BufferedImage img = lastTransformedImage.getAndSet(null);
                if (img == null && !isPreviewSuspended()) {
                    if (hasNewFrame()){
                        // None available, try capture a new frame.
                        captureTransformed();
                        // Void the last image, so a new one will be triggered next time.
                        img = lastTransformedImage.getAndSet(null);
                    }
                }
                if (img != null) {
                    broadcastCapture(img);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            try {
                synchronized(captureNotifier) {
                    if (fps == 0) {
                        captureNotifier.wait();
                    }
                    else {
                        captureNotifier.wait((long) (1000. / fps));
                    }
                }
            }
            catch (InterruptedException e) {
                break;
            }
        }
    }

    public boolean isPreviewSuspended() {
        return (suspendPreviewInTasks && Configuration.get().getMachine().isBusy());
    }
}
