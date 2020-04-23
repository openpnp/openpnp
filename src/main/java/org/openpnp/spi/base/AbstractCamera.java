package org.openpnp.spi.base;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.SwingUtilities;

import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.openpnp.CameraListener;
import org.openpnp.ConfigurationListener;
import org.openpnp.gui.MainFrame;
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
import org.openpnp.util.SimpleGraph;
import org.openpnp.vision.FluentCv;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;

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

    public enum SettleMethod {
        FixedTime,
        Maximum,
        Mean,
        Euclidean,
        Square;

        int getNorm() {
            switch(this) {
                case Maximum:
                    return Core.NORM_INF;
                case Mean:
                    return Core.NORM_L1;
                case Euclidean:
                    return Core.NORM_L2;
                case Square:
                    return Core.NORM_L2;
            }
            return -1;
        }
        double normedResult(double result, Mat mat) {
            // all results are scaled to 100 (percent)
            switch(this) {
                case Maximum:
                    return result/(mat.channels()*2.55);
                case Mean:
                    return result/(mat.cols()*mat.rows()*mat.channels()*2.55);
                case Euclidean:
                    return result/(Math.sqrt(mat.cols()*mat.rows()*mat.channels())*2.55);
                case Square:
                    return result/(mat.cols()*mat.rows()*mat.channels()*255*2.55);
            }
            return result*100.0;
        }
    }

    @Attribute(required = false)
    protected SettleMethod settleMethod = null;

    @Attribute(required = false)
    protected long settleTimeMs = 250;

    @Attribute(required = false)
    protected long settleTimeoutMs = 500;

    @Attribute(required = false)
    protected double settleThreshold = 0.0;

    @Attribute(required = false)
    protected boolean settleFullColor = false;

    @Attribute(required = false)
    protected int settleGaussianBlur = 0;

    @Attribute(required = false)
    protected boolean settleGradients = false;

    @Attribute(required = false)
    protected double settleMaskCircle = 0.0;

    @Attribute(required = false)
    protected double settleContrastAdapt = 0.0;

    @Attribute(required = false)
    protected boolean settleDiagnostics = false;


    @Commit
    protected void commit() throws Exception {
        if (settleMethod == null) {
            if (settleTimeMs < 0) {
                settleMethod = SettleMethod.Maximum;
                // migrate the old threshold, coded as a negative number
                settleThreshold = Math.abs(settleTimeMs)/2.55;
                settleTimeMs = 250;
            }
            else {
                settleMethod = SettleMethod.FixedTime;
            }
        }
    }


    protected Set<ListenerEntry> listeners = Collections.synchronizedSet(new HashSet<>());

    protected Head head;

    protected Integer width;

    protected Integer height;
    
    private boolean headSet = false;
    
    private Mat lastSettleMat = null;
    private SimpleGraph settleGraph = null;

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

    public static final String DIFFERENCE = "D"; 
    public static final String BOOLEAN = "B"; 
    
    protected TreeMap<Double, BufferedImage> settleRecordedImages = null;

    private void startDiagnostics() {
        if (settleDiagnostics) {
            SimpleGraph settleGraph = new SimpleGraph();
            settleGraph.setOffsetMode(true);
            settleGraph.setRelativePaddingLeft(0.05);
            // init difference scale
            SimpleGraph.DataScale settleScale =  settleGraph.getScale(DIFFERENCE);
            settleScale.setRelativePaddingBottom(0.3);
            settleScale.setColor(new Color(0, 0, 0, 64));
            SimpleGraph.DataScale captureScale =  settleGraph.getScale(BOOLEAN);
            captureScale.setRelativePaddingTop(0.8);
            captureScale.setRelativePaddingBottom(0.1);
            // init the difference data
            settleGraph.getRow(DIFFERENCE, "D")
                .setColor(new Color(255, 0, 0));
            // setpoint
            settleGraph.getRow(DIFFERENCE, "S")
            .setColor(new Color(0, 200, 0));
            // init the capture data
            settleGraph.getRow(BOOLEAN, "C")
                .setColor(new Color(00, 0x5B, 0xD9)); // the OpenPNP color
            // set to trigger property change
            setSettleGraph(settleGraph);
            // a place to store images
            settleRecordedImages = new TreeMap<>();
        }
        else {
            setSettleGraph(null);
            settleRecordedImages = null;
        }
    }

    private BufferedImage autoSettleAndCapture() {
        Mat mask = null;
        long t0 = System.currentTimeMillis();
        long timeout = t0 + settleTimeoutMs;
        startDiagnostics();
        while(true) {
            // Capture an image. 
            long t = System.currentTimeMillis();
            if (settleGraph != null) {
                // record capture begins
                settleGraph.getRow(BOOLEAN, "C").recordDataPoint(t-1, 0);
                settleGraph.getRow(BOOLEAN, "C").recordDataPoint(t, 1);
            }
            BufferedImage image = capture();
            t = System.currentTimeMillis();
            if (settleGraph != null) {
                // record capture ends
                settleGraph.getRow(BOOLEAN, "C").recordDataPoint(t-1, 1);
                settleGraph.getRow(BOOLEAN, "C").recordDataPoint(t, 0);
            }
            // convert to Mat and convert to gray.
            Mat mat = OpenCvUtils.toMat(image);
            if (!settleFullColor) {
                Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
            }

            if (settleMaskCircle > 0.0) {
                // Crop the image to the mask dimension. 
                int imageDimension = Math.min(mat.rows(), mat.cols());
                int maskDiameter = Math.max(1,  Math.min(imageDimension, (int)(settleMaskCircle*imageDimension)));
                Rect rectCrop = new Rect(
                        (mat.cols() - maskDiameter)/2, (mat.rows() - maskDiameter)/2,
                        maskDiameter, maskDiameter);
                Mat matCrop = mat.submat(rectCrop);
                mat.release();
                mat = matCrop;

                if (mask == null) {
                    // This must be the first frame, also create the mask circle
                    mask = new Mat(mat.rows(), mat.cols(), CvType.CV_8U, Scalar.all(0));
                    Imgproc.circle(mask,
                            new Point( mat.rows() / 2, mat.cols() / 2 ),
                            maskDiameter/2,
                            new Scalar(255, 255, 255), -1);
                }
            }
            if (settleContrastAdapt > 0.0 && !settleGradients) {
                double max = Core.norm(mat, Core.NORM_INF);
                double range = Core.norm(mat, Core.NORM_INF | Core.NORM_MINMAX);
                double scale = settleContrastAdapt*255.999/range + (1-settleContrastAdapt);
                double offset = (255.999-max*scale)*0.5;
                Core.convertScaleAbs(mat, mat, scale, offset);
            }
            if (settleGaussianBlur > 1) {
                // Apply the Gaussian blur, make the kernel size an odd number 
                Imgproc.GaussianBlur(mat, mat, new Size(settleGaussianBlur|1, settleGaussianBlur|1), 0);
            }
            if (settleGradients) {
                Mat gradient = new Mat();
                // apply Laplacian transform
                Imgproc.Laplacian(mat, gradient, CvType.CV_16S, 3, 1, 0, Core.BORDER_DEFAULT);
                if (settleContrastAdapt > 0.0) {
                    double max = Core.norm(mat, Core.NORM_INF);
                    double range = Core.norm(mat, Core.NORM_INF | Core.NORM_MINMAX);
                    double scale = settleContrastAdapt*255.999/range + (1-settleContrastAdapt);
                    double offset = (255.999-max*scale)*0.5;
                    Core.convertScaleAbs(gradient, gradient, scale, offset);
                }
                else {
                    Core.convertScaleAbs(gradient, gradient);
                }
                mat.release();
                mat = gradient;
            }
            t = System.currentTimeMillis();
            if (settleDiagnostics) {
                // Write the image to disk.
                try {
                    BufferedImage img;
                    Mat resultMat; 
                    Mat masked = null;
                    if (mask != null) {
                        masked = new Mat(mat.rows(), mat.cols(), mat.type(), Scalar.all(0));
                        mat.copyTo(masked, mask);
                        resultMat = masked;
                    }
                    else {
                        resultMat = mat;
                    }
                    img = OpenCvUtils.toBufferedImage(resultMat);
                    settleRecordedImages.put((double)t, img);
                    if (Logger.getLevel() == org.pmw.tinylog.Level.DEBUG || Logger.getLevel() == org.pmw.tinylog.Level.TRACE) {
                        File file = Configuration.get().createResourceFile(getClass(), "settle", ".png");
                        Imgcodecs.imwrite(file.getAbsolutePath(), resultMat);
                    }
                    if (masked != null) {
                        masked.release();
                    }
                }
                catch (IOException e) {
                    Logger.error(e);
                }
            }
            // If this is the first time through the loop then assign the new image to
            // the lastSettleMat and loop again. We need at least two images to check.
            if (lastSettleMat == null) {
                lastSettleMat = mat;
                continue;
            }

            // Take the norm of the differences of the two images.
            double result; 
            if (mask != null) { 
                // masked by the circle
                result = settleMethod.normedResult(Core.norm(lastSettleMat, mat, settleMethod.getNorm(), mask), mat);
            }
            else {
                // the whole image
                result = settleMethod.normedResult(Core.norm(lastSettleMat, mat, settleMethod.getNorm()), mat);
            }
            if (settleGraph != null) {
                settleGraph.getRow(DIFFERENCE, "D").recordDataPoint(t, result);
            }
            Logger.debug("autoSettleAndCapture t="+(t-t0)+" auto settle score: " + result);

            // Release the lastSettleMat and store the new image as the lastSettleMat.
            lastSettleMat.release();
            lastSettleMat = mat;

            // If the image changed at least a bit (due to noise) and and less than our
            // threshold, we have a winner. The check for > 0 is to ensure that we're not just
            // receiving a duplicate frame from the camera. Every camera has at least a little
            // noise so we're just checking that at least one pixel changed by 1 bit.
            if ((t > timeout)
                    || (result > 0.0 && result < settleThreshold)) {
                lastSettleMat.release();
                lastSettleMat = null;
                if (mask != null) {
                    mask.release();
                    mask = null;
                }
                if (settleGraph != null) {
                   // record last points in graph 
                   settleGraph.getRow(BOOLEAN, "C").recordDataPoint(t, 0);
                   settleGraph.getRow(DIFFERENCE, "S").recordDataPoint(t0, settleThreshold);
                   settleGraph.getRow(DIFFERENCE, "S").recordDataPoint(t+10, settleThreshold);
                   settleGraph.getRow(DIFFERENCE, "D").recordDataPoint(t+10, result);
                    // set to trigger the property change
                    setSettleGraph(settleGraph);
                }
                Logger.debug("autoSettleAndCapture in {} ms", System.currentTimeMillis() - t0);
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
        
    	
        if (settleMethod == SettleMethod.FixedTime) {
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

    public SettleMethod getSettleMethod() {
        return settleMethod;
    }

    public void setSettleMethod(SettleMethod settleMethod) {
        this.settleMethod = settleMethod;
    }

    public long getSettleTimeMs() {
        return settleTimeMs;
    }

    public void setSettleTimeMs(long settleTimeMs) {
        this.settleTimeMs = settleTimeMs;
    }

    public long getSettleTimeoutMs() {
        return settleTimeoutMs;
    }

    public void setSettleTimeoutMs(long settleTimeoutMs) {
        this.settleTimeoutMs = settleTimeoutMs;
    }

    public double getSettleThreshold() {
        return settleThreshold;
    }

    public void setSettleThreshold(double settleThreshold) {
        this.settleThreshold = settleThreshold;
    }

    public boolean isSettleGradients() {
        return settleGradients;
    }

    public void setSettleGradients(boolean settleGradients) {
        this.settleGradients = settleGradients;
    }

    public boolean isSettleFullColor() {
        return settleFullColor;
    }

    public void setSettleFullColor(boolean settleFullColor) {
        this.settleFullColor = settleFullColor;
    }

    public int getSettleGaussianBlur() {
        return settleGaussianBlur;
    }

    public void setSettleGaussianBlur(int settleGaussianBlur) {
        Object oldValue = this.settleGaussianBlur;
        this.settleGaussianBlur = settleGaussianBlur <= 1 ? 0 : settleGaussianBlur|1; // make it odd
        firePropertyChange("settleGaussianBlur", oldValue, settleGaussianBlur);
    }

    public double getSettleMaskCircle() {
        return settleMaskCircle;
    }

    public void setSettleMaskCircle(double settleMaskCircle) {
        this.settleMaskCircle = settleMaskCircle;
    }

    public double getSettleContrastAdapt() {
        return settleContrastAdapt;
    }

    public void setSettleContrastAdapt(double settleContrastAdapt) {
        this.settleContrastAdapt = settleContrastAdapt;
    }

    public boolean isSettleDiagnostics() {
        return settleDiagnostics;
    }

    public void setSettleDiagnostics(boolean settleDiagnostics) {
        this.settleDiagnostics = settleDiagnostics;
    }

    public SimpleGraph getSettleGraph() {
        return settleGraph;
    }

    public BufferedImage getRecordedImage(double t) {
        Map.Entry<Double, BufferedImage> entry = settleRecordedImages.floorEntry(t);
        if (entry != null) {
            return entry.getValue();
        }
        return null;
    }
    public void playRecordedImage(double t) {
        Map.Entry<Double, BufferedImage> entry = settleRecordedImages.floorEntry(t);
        if (entry != null) {
            // I'm sure there's a better way to count the preceding images :-(
            int n = 0;
            for (Double t0 : settleRecordedImages.keySet()) {
                if (t0 > t) {
                    break;
                }
                ++n;
            }
            double t0 = (settleGraph != null ? settleGraph.getOffset() : 0.0);
            String message = "Camera settling, frame number "+n+", t=+"+(entry.getKey()-t0)+"ms";
            MainFrame.get().getCameraViews().getCameraView(this)
            .showFilteredImage(entry.getValue(), message, 1000);
        }
    }

    public void setSettleGraph(SimpleGraph settleGraph) {
        Object oldValue = this.settleGraph;
        this.settleGraph = settleGraph;
        firePropertyChange("settleGraph", oldValue, settleGraph);
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
