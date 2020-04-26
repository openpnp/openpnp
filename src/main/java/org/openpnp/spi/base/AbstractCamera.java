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

        protected static final double minimumRange = 16;

        protected int getNorm() {
            switch(this) {
                case Maximum:
                    return Core.NORM_INF;
                case Mean:
                    return Core.NORM_L1;
                case Euclidean:
                    return Core.NORM_L2;
                case Square:
                    return Core.NORM_L2SQR;
                default:
                    return -1;
            }
        }

        protected double getScale(Mat mat) {
            switch(this) {
                case Maximum:
                    return 255.0;
                case Mean:
                    return 255.0*mat.cols()*mat.rows()*mat.channels();
                case Euclidean:
                    return 255.0*Math.sqrt(mat.cols()*mat.rows()*mat.channels());
                case Square:
                    return 255.0*255.0*mat.cols()*mat.rows()*mat.channels(); 
                default:
                    return -1;
            }
        }

        protected double computeDifferenceMetric(Mat mat0, Mat mat1, double settleContrastEnhance, Mat mask) { 
            // Compute the method difference norm
            double result, range = 1.0;
            if (mask != null) { 
                // masked by the circle
                result = Core.norm(mat0, mat1, getNorm(), mask)/getScale(mat1);
                if (settleContrastEnhance != 0.0) {
                    range = Core.norm(mat1, getNorm(), mask)/getScale(mat1);
                }
            }
            else {
                // the whole image
                result = Core.norm(mat0, mat1, getNorm())/getScale(mat1);
                if (settleContrastEnhance != 0.0) {
                    range = Core.norm(mat1, getNorm())/getScale(mat1);
                }
            }
            if (range != 0.0) {
                result = (settleContrastEnhance/range + (1.0 - settleContrastEnhance))*result;
            }
            // Make it percent
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
    protected int settleDebounce = 0;

    @Attribute(required = false)
    protected boolean settleFullColor = false;

    @Attribute(required = false)
    protected int settleGaussianBlur = 0;

    @Attribute(required = false)
    protected boolean settleGradients = false;

    @Attribute(required = false)
    protected double settleMaskCircle = 0.0;

    @Attribute(required = false)
    protected double settleContrastEnhance = 0.0;

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
    
    public static final String DIFFERENCE = "D"; 
    public static final String BOOLEAN = "B"; 
    public static final String CAPTURE = "C"; 
    public static final String THRESHOLD = "TH"; 
    public static final String DATA = "D"; 
    
    protected TreeMap<Double, BufferedImage> recordedImages = null;
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

    private SimpleGraph startDiagnostics() {
        if (settleDiagnostics) {
            // Diagnostics wanted, create the simple graph.
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
            settleGraph.getRow(DIFFERENCE, DATA)
            .setColor(new Color(255, 0, 0));
            // setpoint
            settleGraph.getRow(DIFFERENCE, THRESHOLD)
            .setColor(new Color(0, 180, 0));
            // init the capture data
            settleGraph.getRow(BOOLEAN, CAPTURE)
            .setColor(new Color(00, 0x5B, 0xD9)); // the OpenPNP color
            return settleGraph;
        }
        else {
            // No diagnostics wanted, also cleanup the previous one. 
            setSettleGraph(null);
            recordedImages = null;
            return null;
        }
    }

    private BufferedImage autoSettleAndCapture() {
        Mat mask = null;
        Mat maskFullsize = null;
        Mat lastSettleMat = null;

        try {
            long t0 = System.currentTimeMillis();
            long timeout = t0 + settleTimeoutMs;
            int debounceCount = 0;
            SimpleGraph settleGraph = startDiagnostics();
            TreeMap<Double, BufferedImage> settleImages = new TreeMap<>();
            while(true) {
                // Capture an image. 
                long t = System.currentTimeMillis();
                if (settleGraph != null) {
                    // record capture begins
                    settleGraph.getRow(BOOLEAN, CAPTURE).recordDataPoint(t-1, 0);
                    settleGraph.getRow(BOOLEAN, CAPTURE).recordDataPoint(t, 1);
                }

                // The actual capture.
                BufferedImage image = capture();

                long tC = System.currentTimeMillis();
                if (settleGraph != null) {
                    // record capture ends
                    settleGraph.getRow(BOOLEAN, CAPTURE).recordDataPoint(tC-1, 1);
                    settleGraph.getRow(BOOLEAN, CAPTURE).recordDataPoint(tC, 0);
                }

                // Convert to Mat and if not full color, convert to gray.
                Mat mat = OpenCvUtils.toMat(image);
                if (!settleFullColor) {
                    Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
                }

                if (settleContrastEnhance > 0.0) {
                    // Enhance the contrast. Note we need to do this before scaling the image down, so mixed
                    // colors can be created in the enhanced dynamic range. 

                    // Need to create a full size mask.
                    if (settleMaskCircle > 0.0 && maskFullsize == null) {
                        int imageDimension = Math.min(mat.rows(), mat.cols());
                        int maskDiameter = Math.max(1,  Math.min(imageDimension, (int)(settleMaskCircle*imageDimension)));
                        maskFullsize = new Mat(mat.rows(), mat.cols(), CvType.CV_8U, Scalar.all(0));
                        Imgproc.circle(maskFullsize,
                                new Point( mat.rows() / 2, mat.cols() / 2 ),
                                maskDiameter/2,
                                new Scalar(255, 255, 255), -1);
                    }

                    // It's really hard to extract the minimum level from an image, it seems.
                    // Core.norm(norm|NORM_MINMAX) does not seem to work and minMaxLoc() takes only
                    // single channel images. So we need to extract channels here. I must be missing 
                    // something.
                    double max = SettleMethod.minimumRange/255.0;
                    double range = SettleMethod.minimumRange/255.0;
                    int nChannels = mat.channels();
                    if (nChannels > 1) {
                        Mat workingMat = new Mat();
                        for (int cn=0; cn < nChannels; cn++) {
                            Core.extractChannel(mat, workingMat, cn);
                            MinMaxLocResult res = Core.minMaxLoc(workingMat, maskFullsize); // Note, mask can for once be null
                            workingMat.release();
                            max = Math.max(max, res.maxVal)/255.0;
                            range = Math.max(range, res.maxVal-res.minVal)/255.0;
                        }
                    }
                    else {
                        MinMaxLocResult res = Core.minMaxLoc(mat, maskFullsize); // Note, mask can for once be null
                        max = Math.max(max, res.maxVal)/255.0;
                        range = Math.max(range, res.maxVal-res.minVal)/255.0;
                    }
                    double scale = settleContrastEnhance/range + (1.0 - settleContrastEnhance);
                    double offset = -(max-range)*settleContrastEnhance/range;
                    Mat tmpMat = new Mat();  
                    Core.convertScaleAbs(mat, tmpMat, scale, offset*255.0);
                    mat.release();
                    mat = tmpMat;
                }

                // Gaussian blur is the most expensive operation, so if it is large, we rescale the image instead.
                // This is effectively a box blur followed (later) by a Gaussian blur, i.e. still reasonable quality.
                // Rescaling will make all subsequent steps significantly faster. 
                final int resizeToMaxGaussianKernelSize = 5;
                int gaussianBlurEff = settleGaussianBlur;
                int divisor = (settleGaussianBlur+resizeToMaxGaussianKernelSize/2)/resizeToMaxGaussianKernelSize;
                if (divisor > 1) {
                    gaussianBlurEff = ((settleGaussianBlur)/divisor)|1;
                    Mat resizeMat = new Mat();
                    // TODO: find a way to roll resize and crop into one.
                    Imgproc.resize(mat, resizeMat, new Size(mat.cols()/divisor, mat.rows()/divisor), 1.0/divisor, 1.0/divisor);
                    mat.release();
                    mat = resizeMat;
                }

                if (settleMaskCircle > 0.0) {
                    // Crop the image to the mask dimension. 
                    int imageDimension = Math.min(mat.rows(), mat.cols());
                    int maskDiameter = Math.max(1,  Math.min(imageDimension, (int)(settleMaskCircle*imageDimension)));
                    Rect rectCrop = new Rect(
                            (mat.cols() - maskDiameter)/2, (mat.rows() - maskDiameter)/2,
                            maskDiameter, maskDiameter);
                    Mat cropMat = mat.submat(rectCrop);
                    mat.release();
                    mat = cropMat;
                    if (mask == null) {
                        // This must be the first frame, also create the mask circle.
                        mask = new Mat(mat.rows(), mat.cols(), CvType.CV_8U, Scalar.all(0));
                        Imgproc.circle(mask,
                                new Point( mat.rows() / 2, mat.cols() / 2 ),
                                maskDiameter/2,
                                new Scalar(255, 255, 255), -1);
                    }
                }

                if (gaussianBlurEff > 1) {
                    // Apply the Gaussian blur, make the kernel size an odd number. 
                    Imgproc.GaussianBlur(mat, mat, new Size(gaussianBlurEff|1, gaussianBlurEff|1), 0);
                }

                if (settleGradients) {
                    Mat gradientMat = new Mat();
                    // apply Laplacian transform
                    Imgproc.Laplacian(mat, gradientMat, CvType.CV_16S, 3, 1, 0, Core.BORDER_REPLICATE );
                    Core.convertScaleAbs(gradientMat, gradientMat);
                    mat.release();
                    mat = gradientMat;
                }

                recordDiagnosticImage(tC, lastSettleMat, mat, mask, settleImages);
                t = System.currentTimeMillis();

                // If this is the first time through the loop then assign the new image to
                // the lastSettleMat and loop again. We need at least two images to check.
                if (lastSettleMat == null) {
                    lastSettleMat = mat;
                    continue;
                }

                // Take the norm of the differences of the two images.
                double result = settleMethod.computeDifferenceMetric(lastSettleMat, mat, settleContrastEnhance, mask);
                if (settleGraph != null) {
                    settleGraph.getRow(DIFFERENCE, DATA).recordDataPoint(t, result);
                }
                Logger.debug("autoSettleAndCapture t="+(t-t0)+" auto settle score: " + result);

                // Release the lastSettleMat and store the new image as the lastSettleMat.
                lastSettleMat.release();
                lastSettleMat = mat;

                // If the image changed at least a bit (due to noise) and less than our
                // threshold, we have a winner. The check for > 0 is to ensure that we're not just
                // receiving a duplicate frame from the camera. Every camera has at least a little
                // noise so we're just checking that at least one pixel changed by 1 bit.
                if (result > settleThreshold) {
                    // No good, reset the debounce count, as we crossed over the limit (again).
                    debounceCount = 0;
                }
                else if (result > 0.0) {
                    // Register one "bounce" under the limit.
                    debounceCount++;
                }
                if (t > timeout || debounceCount > settleDebounce) {
                    lastSettleMat.release();
                    lastSettleMat = null;
                    if (settleGraph != null) {
                        // record last points in graph 
                        settleGraph.getRow(BOOLEAN, CAPTURE).recordDataPoint(t, 0);
                        settleGraph.getRow(DIFFERENCE, THRESHOLD).recordDataPoint(t0, settleThreshold);
                        settleGraph.getRow(DIFFERENCE, THRESHOLD).recordDataPoint(t+1, settleThreshold);
                        settleGraph.getRow(DIFFERENCE, DATA).recordDataPoint(t+1, result);
                        // set to trigger the property change
                        setSettleGraph(settleGraph);
                        settleGraph = null;
                    }
                    if (settleImages != null) {
                        recordedImages = settleImages;
                    }
                    Logger.debug("autoSettleAndCapture in {} ms", System.currentTimeMillis() - t0);
                    return image;
                }
            }
        }
        finally {
            // whatever happens, always release these looping mats
            if (mask != null) {
                mask.release();
            }
            if (maskFullsize != null) {
                maskFullsize.release();
            }
            if (lastSettleMat != null) {
                lastSettleMat.release();
            }
        }
    }

    protected void recordDiagnosticImage(long t, Mat mat0, Mat mat1, Mat mask,
            TreeMap<Double, BufferedImage> settleImages) {
        if (settleImages != null) {
            // Record diagnostic images.
            Mat diagnosticMat = mat1;

            if (mask != null) {
                // Simulate the mask for the diagnostic image.
                Mat tmpMat = new Mat(diagnosticMat.rows(), diagnosticMat.cols(), diagnosticMat.type(), Scalar.all(0));
                diagnosticMat.copyTo(tmpMat, mask);
                if (diagnosticMat != mat1) {
                    diagnosticMat.release();
                }
                diagnosticMat = tmpMat;
            }

            // Create the difference heat-map
            if (mat0 != null) {
                Mat diffMat = new Mat();
                Core.absdiff(mat0, mat1, diffMat);
                if (settleFullColor) {
                    Imgproc.cvtColor(diffMat, diffMat, Imgproc.COLOR_BGR2GRAY);
                }
                Mat normMat = new Mat();
                if (mask != null) {
                    Core.normalize(diffMat, normMat, 255, 0, 
                            Core.NORM_INF, 
                            0, mask);
                }
                else {
                    Core.normalize(diffMat, normMat, 255, 0, 
                            Core.NORM_INF, 
                            0);
                }
                Imgproc.cvtColor(normMat, normMat, Imgproc.COLOR_GRAY2BGR);
                diffMat.release();
                Mat heatmapMat = new Mat();
                Imgproc.applyColorMap(normMat, heatmapMat, Imgproc.COLORMAP_HOT);
                Mat backgroundMat = new Mat();
                if (settleFullColor) {
                    diagnosticMat.copyTo(backgroundMat);
                }
                else {
                    Imgproc.cvtColor(diagnosticMat, backgroundMat, Imgproc.COLOR_GRAY2BGR);
                }

                // Am I doing something wrong? This arithmetic is awful to use. 
                Mat norm2Mat = new Mat();
                Core.multiply(normMat, normMat, norm2Mat, 1/255.0/255.0, CvType.CV_32F);
                Mat mulMat = new Mat();
                Core.multiply(heatmapMat, norm2Mat, mulMat, 1.0, CvType.CV_8U);
                heatmapMat.release();
                heatmapMat = mulMat;
                Mat allMat = new Mat(norm2Mat.rows(), norm2Mat.cols(), norm2Mat.type(), Scalar.all(1.0));
                Mat inverted2NormMat = new Mat();
                Core.subtract(allMat, norm2Mat, inverted2NormMat);
                allMat.release();
                mulMat = new Mat();
                Core.multiply(backgroundMat, inverted2NormMat, mulMat, 1.0, CvType.CV_8U);
                backgroundMat.release();
                backgroundMat = mulMat;
                Mat blendedMat = new Mat();
                Core.add(backgroundMat, heatmapMat, blendedMat);
                backgroundMat.release();
                heatmapMat.release();
                // cleanup
                normMat.release();
                norm2Mat.release();
                inverted2NormMat.release();
                // New result
                if (diagnosticMat != mat1) {
                    diagnosticMat.release();
                }
                diagnosticMat = blendedMat;
            }

            // Save file to disk.
            if (Logger.getLevel() == org.pmw.tinylog.Level.DEBUG || Logger.getLevel() == org.pmw.tinylog.Level.TRACE) {
                try {
                    File file = Configuration.get()
                            .createResourceFile(getClass(), "settle", ".png");
                    Imgcodecs.imwrite(file.getAbsolutePath(), diagnosticMat);
                }
                catch (Exception e) {
                    Logger.error(e);
                }
            }

            // Record this image to play later
            BufferedImage img;
            img = OpenCvUtils.toBufferedImage(diagnosticMat);
            settleImages.put((double)t, img);

            // cleanup
            if (diagnosticMat != mat1) {
                diagnosticMat.release();
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

    public int getSettleDebounce() {
        return settleDebounce;
    }

    public void setSettleDebounce(int settleDebounce) {
        this.settleDebounce = settleDebounce;
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

    public double getSettleContrastEnhance() {
        return settleContrastEnhance;
    }

    public void setSettleContrastEnhance(double settleContrastEnhance) {
        this.settleContrastEnhance = settleContrastEnhance;
    }

    public boolean isSettleDiagnostics() {
        return settleDiagnostics;
    }

    public void setSettleDiagnostics(boolean settleDiagnostics) {
        this.settleDiagnostics = settleDiagnostics;
        if (!settleDiagnostics) {
            // get rid of recordings
            setSettleGraph(null);
            setRecordedImages(null);
        }
    }

    public SimpleGraph getSettleGraph() {
        return settleGraph;
    }

    public void setSettleGraph(SimpleGraph settleGraph) {
        Object oldValue = this.settleGraph;
        this.settleGraph = settleGraph;
        firePropertyChange("settleGraph", oldValue, settleGraph);
    }

    public BufferedImage getRecordedImage(double t) {
        if (recordedImages != null) {
            Map.Entry<Double, BufferedImage> entry = recordedImages.floorEntry(t);
            if (entry != null) {
                return entry.getValue();
            }
        }
        return null;
    }
    
    
    protected TreeMap<Double, BufferedImage> getRecordedImages() {
        return recordedImages;
    }

    protected void setRecordedImages(TreeMap<Double, BufferedImage> recordedImages) {
        this.recordedImages = recordedImages;
    }

    public void playRecordedImage(double t) {
        if (recordedImages != null) {
            Map.Entry<Double, BufferedImage> entry = recordedImages.floorEntry(t);
            if (entry != null) {
                // I'm sure there's a better way to count the preceding images :-(
                int n = 0;
                for (Double t0 : recordedImages.keySet()) {
                    if (t0 > t) {
                        break;
                    }
                    ++n;
                }
                double t0 = (settleGraph != null ? settleGraph.getOffset() : 0.0);
                String message = "Camera settling, frame number "+n+", t=+"+(entry.getKey()-t0)+"ms";
                MainFrame.get().getCameraViews().getCameraView(this)
                .showFilteredImage(entry.getValue(), message, 1500);
                SwingUtilities.invokeLater(() -> {
                    MainFrame.get().getCameraViews().ensureCameraVisible(this);
                });
            }
        }
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
