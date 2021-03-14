package org.openpnp.spi.base;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.*;

import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.openpnp.CameraListener;
import org.openpnp.ConfigurationListener;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Icons;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Issue;
import org.openpnp.model.Solutions.Severity;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.MotionPlanner.CompletionType;
import org.openpnp.spi.VisionProvider;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.SimpleGraph;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;

public abstract class AbstractCamera extends AbstractHeadMountable implements Camera {
    @Attribute
    protected String id;

    @Attribute(required = false)
    protected String name;

    @Attribute
    protected Looking looking = Looking.Down;

    @Attribute(required = false)
    protected boolean autoVisible = false;

    @Attribute(required = false)
    protected boolean shownInMultiCameraView = true;

    @Attribute(required = false)
    protected boolean beforeCaptureLightOn = true;

    @Attribute(required = false)
    protected boolean userActionLightOn = true;

    @Attribute(required = false)
    protected boolean afterCaptureLightOff = false;

    @Attribute(required = false)
    protected boolean antiGlareLightOff = false;

    /**
     * The primary units per pixel for this camera. This is used to convert the apparent size (in
     * pixels) of an object's image to an estimate of its physical size (in units). Note that this
     * conversion is only valid for objects at the same distance from the camera at which the
     * calibration of the units per pixel was performed. The units per pixel z-coordinate contains
     * the height at which the measurement was made. In combination with {@link #cameraPrimaryZ} a
     * camera relative Z distance can be computed (necessary for Z-movable cameras). 
     * 
     * Also see {@link #unitsPerPixelSecondary}.
     */
    @Element
    protected Location unitsPerPixel = new Location(LengthUnit.Millimeters);

    /**
     * The secondary units per pixel for this camera. This is typically calibrated at a different
     * distance from the camera than the primary {@link #unitsPerPixel} so that the two together
     * can be used compute an object's true size (in units) assuming its actual z coordinate is known. 
     */
    @Element(required = false)
    protected Location unitsPerPixelSecondary = null;

    /**
     * The Z coordinate of camera at the primary units per pixel measurement for this camera. 
     */
    @Element(required = false)
    protected Length cameraPrimaryZ = null;

    /**
     * The Z coordinate of camera at the secondary units per pixel measurement for this camera. 
     */
    @Element(required = false)
    protected Length cameraSecondaryZ = null;

    /**
     * The Z coordinate at which objects are assumed to be if their true height is unknown. 
     */
    @Element(required = false)
    protected Length defaultZ = null;

    @Attribute(required = false)
    boolean enableUnitsPerPixel3D = false;

    /**
     * Automatically set the CameraView viewing plane Z according to taregeted user action.  
     */
    @Attribute(required = false)
    boolean autoViewPlaneZ = false;

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

        protected double computeDifference(Mat mat0, Mat mat1, double settleContrastEnhance, Mat mask) { 
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
    protected TreeMap<Double, BufferedImage> heatMappedImages = null;
    protected Double recordedImagePlayed = null;
    private SimpleGraph settleGraph = null;
    private int recordedMaskDiameter;

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

    public void setId(String id) {
        this.id = id;
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
        if (this.head != head && this.headSet) {
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
            return super.getLocation().subtract(tool.getCameraToolCalibratedOffset(this));
        }

        return super.getLocation();
    }

    /**
     * Gets the relative Z distance above the physical camera of the specified z coordinate
     * 
     * @param zCoordinate
     * @return 
     */
    public Length getCameraRelativeZ(Length zCoordinate) {
        Location cameraLocation = getCameraPhysicalLocation();
        return zCoordinate.subtract(cameraLocation.getLengthZ());
    }

    /**
     * Gets the absolute camera z coordinate from the given relative Z.
     * 
     * @param cameraRelativeZ
     * @return 
     */
    protected Length getCameraAbsoluteZ(Length cameraRelativeZ) {
        Location cameraLocation = getCameraPhysicalLocation();
        return cameraRelativeZ.add(cameraLocation.getLengthZ());
    }

    /**
     * Get the physical location of the camera i.e. do not take virtual axes into consideration.
     * 
     * @return 
     */
    public Location getCameraPhysicalLocation() {
        Location cameraLocation = getLocation();
        try {
            //Replace virtual axis coordinates, if any, with the head offset
            cameraLocation = getApproximativeLocation(cameraLocation, cameraLocation, LocationOption.ReplaceVirtual);
        }
        catch (Exception e1) {
            e1.printStackTrace();
        }
        return cameraLocation;
    }

    @Override
    public boolean isShownInMultiCameraView() {
        return shownInMultiCameraView;
    }

    public void setShownInMultiCameraView(boolean shownInMultiCameraView) {
        Object oldValue = this.shownInMultiCameraView;
        this.shownInMultiCameraView = shownInMultiCameraView;
        firePropertyChange("shownInMultiCameraView", oldValue, shownInMultiCameraView);
    }

    public boolean isEnableUnitsPerPixel3D() {
        return enableUnitsPerPixel3D;
    }

    public void setEnableUnitsPerPixel3D(boolean enableUnitsPerPixel3D) {
        this.enableUnitsPerPixel3D = enableUnitsPerPixel3D;
    }

    public boolean isAutoViewPlaneZ() {
        return autoViewPlaneZ;
    }

    public void setAutoViewPlaneZ(boolean autoViewPlaneZ) {
        this.autoViewPlaneZ = autoViewPlaneZ;
    }

    @Override
    public Location getUnitsPerPixel() {
        return getUnitsPerPixel(defaultZ);
    }

    @Override
    public Location getUnitsPerPixel(Length viewingPlaneZ) {
        if (!isSecondaryUnitsPerPixelCalibrated()) {
            return unitsPerPixel;
        }
        if (viewingPlaneZ == null) {
            viewingPlaneZ = defaultZ;
        }
        LengthUnit units = unitsPerPixel.getUnits();
        Location uppCal1 = unitsPerPixel;
        Location uppCal2 = unitsPerPixelSecondary.convertToUnits(units);
        double cameraRelZ1 = uppCal1.getLengthZ().subtract(cameraPrimaryZ).getValue();
        double cameraRelZ2 = uppCal2.getLengthZ().subtract(cameraSecondaryZ).getValue();
        if (cameraRelZ1 == cameraRelZ2) {
            // Calibration wasn't performed at two different Z / camera Z
            // return the primary units per pixels
            return unitsPerPixel;
        }

        double cameraRelZ = getCameraRelativeZ(viewingPlaneZ).convertToUnits(units).getValue();

        // Linearly interpolate between the two calibration points
        double k = (cameraRelZ - cameraRelZ2) / (cameraRelZ1 - cameraRelZ2);
        return new Location(units, k * (uppCal1.getX() - uppCal2.getX()) + uppCal2.getX(),
                k * (uppCal1.getY() - uppCal2.getY()) + uppCal2.getY(), cameraRelZ, 0.0);
    }

    public boolean isSecondaryUnitsPerPixelCalibrated() {
        return (enableUnitsPerPixel3D
                && unitsPerPixelSecondary != null 
                && unitsPerPixelSecondary.getX() != 0 
                && unitsPerPixelSecondary.getY() != 0 
                && cameraPrimaryZ != null
                && cameraSecondaryZ != null
                && defaultZ != null);
    }

    @Override
    public void setUnitsPerPixel(Location unitsPerPixel) {
        this.unitsPerPixel = unitsPerPixel;
    }

    /**
     * Gets the primary units per pixel (direct access getter)
     * 
     * @return a location whose x and y coordinates are the measured pixels per unit for those axis
     *         respectively and the z coordinate is the height at which the measurements were made.
     */
    public Location getUnitsPerPixelPrimary() {
        return unitsPerPixel;
    }

    /**
     * Sets the primary units per pixel (direct access setter)
     * 
     * @param unitsPerPixelPrimary - a location whose x and y coordinates are the measured pixels
     * per unit for those axis respectively and the z coordinate is the height at which the measurements 
     * were made.
     */
    public void setUnitsPerPixelPrimary(Location unitsPerPixelPrimary) {
        this.unitsPerPixel = unitsPerPixelPrimary;
    }

    /**
     * Gets the secondary units per pixel
     * 
     * @return a location whose x and y coordinates are the measured pixels per unit for those axis
     *         respectively and the z coordinate is the height at which the measurements were made.
     */
    public Location getUnitsPerPixelSecondary() {
        return unitsPerPixelSecondary;
    }

    /**
     * Sets the secondary units per pixel
     * 
     * @param unitsPerPixelSecondary - a location whose x and y coordinates are the measured pixels
     * per unit for those axis respectively and the z coordinate is the height at which the 
     * measurements were made.
     */
    public void setUnitsPerPixelSecondary(Location unitsPerPixelSecondary) {
        this.unitsPerPixelSecondary = unitsPerPixelSecondary;
    }

    @Override
    public Length getDefaultZ() {
        return defaultZ;
    }

    public void setDefaultZ(Length defaultZ) {
        this.defaultZ = defaultZ;
    }

    /**
     * @return Get the z coordinate of camera where the primary units per pixel measurement was made. 
     */
    public Length getCameraPrimaryZ() {
        return cameraPrimaryZ;
    }

    public void setCameraPrimaryZ(Length cameraPrimaryZ) {
        this.cameraPrimaryZ = cameraPrimaryZ;
    }

    /**
     * @return Get the z coordinate of camera where the secondary units per pixel measurement was made. 
     */
    public Length getCameraSecondaryZ() {
        return cameraSecondaryZ;
    }

    public void setCameraSecondaryZ(Length cameraSecondaryZ) {
        this.cameraSecondaryZ = cameraSecondaryZ;
    }

    /**
     * Estimates the Z height of an object based upon the observed units per pixel for the
     * object. This is typically found by capturing images of a feature of the object from two
     * different camera positions. The observed units per pixel is then computed by dividing the
     * actual change in camera position (in machine units) by the apparent change in position of the
     * feature (in pixels) between the two images.
     *
     * @param observedUnitsPerPixel - the observed units per pixel for the object
     * @return - the estimated Z height of the object
     */
    public Length estimateZCoordinateOfObject(Location observedUnitsPerPixel) throws Exception {
        if (!isSecondaryUnitsPerPixelCalibrated()) {
            throw new Exception("Secondary Camera Units Per Pixel have not been calibrated.");
        }
        LengthUnit units = observedUnitsPerPixel.getUnits();
        double uppX = Math.abs(observedUnitsPerPixel.getX());
        double uppY = Math.abs(observedUnitsPerPixel.getY());

        Location uppCal1 = unitsPerPixel.convertToUnits(units);
        Location uppCal2 = unitsPerPixelSecondary.convertToUnits(units);
        double cameraRelZ1 = uppCal1.getLengthZ().subtract(cameraPrimaryZ).getValue();
        double cameraRelZ2 = uppCal2.getLengthZ().subtract(cameraSecondaryZ).getValue();
        if (cameraRelZ1 == cameraRelZ2) {
            throw new Exception("Camera Units Per Pixel has not been calibrated at two different " +
                    "camera relative Z.");
        }

        if (!Double.isFinite(uppX) && !Double.isFinite(uppY)) {
            throw new Exception("Apparent change in position or apparent size of object feature " +
                    "is too small to estimate object Z coordinate.");
        }

        if (uppX == 0 && uppY == 0) {
            throw new Exception("Actual change in camera position or actual feature size too " +
                    "small to estimate object Z coordinate.");
        }

        // Compute the ratio of where the measurement falls between the two cal points using
        // whichever measurement is larger for better accuracy
        double k;
        if (!Double.isFinite(uppY) || (uppX > uppY)) {
            k = (uppX - uppCal2.getX()) / (uppCal1.getX() - uppCal2.getX());
        }
        else {
            k = (uppY - uppCal2.getY()) / (uppCal1.getY() - uppCal2.getY());
        }

        // Compute the Z offset relative to the camera
        double cameraRelZ = k * (cameraRelZ1 - cameraRelZ2) + cameraRelZ2;

        return getCameraAbsoluteZ(new Length(cameraRelZ, units));
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
    public boolean isAutoVisible() {
        return autoVisible;
    }

    public void setAutoVisible(boolean autoVisible) {
        Object oldValue = this.autoVisible;
        this.autoVisible = autoVisible;
        firePropertyChange("autoVisible", oldValue, autoVisible);
    }

    public boolean isBeforeCaptureLightOn() {
        return beforeCaptureLightOn;
    }

    public void setBeforeCaptureLightOn(boolean beforeCaptureLightOn) {
        this.beforeCaptureLightOn = beforeCaptureLightOn;
    }

    public boolean isUserActionLightOn() {
        return userActionLightOn;
    }

    public void setUserActionLightOn(boolean userActionLightOn) {
        this.userActionLightOn = userActionLightOn;
    }

    public boolean isAfterCaptureLightOff() {
        return afterCaptureLightOff;
    }

    public void setAfterCaptureLightOff(boolean afterCaptureLightOff) {
        this.afterCaptureLightOff = afterCaptureLightOff;
    }

    public boolean isAntiGlareLightOff() {
        return antiGlareLightOff;
    }

    public void setAntiGlareLightOff(boolean antiGlareLightOff) {
        this.antiGlareLightOff = antiGlareLightOff;
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
            Color gridColor = UIManager.getColor ( "PasswordField.capsLockIconColor" );
            if (gridColor == null) {
                gridColor = new Color(0, 0, 0, 64);
            } else {
                gridColor = new Color(gridColor.getRed(), gridColor.getGreen(), gridColor.getBlue(), 64);
            }
            // Diagnostics wanted, create the simple graph.
            SimpleGraph settleGraph = new SimpleGraph();
            settleGraph.setRelativePaddingLeft(0.05);
            // init difference scale
            SimpleGraph.DataScale settleScale =  settleGraph.getScale(DIFFERENCE);
            settleScale.setRelativePaddingBottom(0.3);
            settleScale.setColor(gridColor);
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
            .setColor(new Color(00, 0x5B, 0xD9)); // the OpenPNP blue
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
            TreeMap<Double, BufferedImage> settleImages = null;
            if (settleGraph != null) {
                settleImages = new TreeMap<>();
            }
            while(true) {
                // Capture an image. 
                if (settleGraph != null) {
                    // Record begin of capture.
                    settleGraph.getRow(BOOLEAN, CAPTURE).recordDataPoint(settleGraph.getT(), 0);
                    settleGraph.getRow(BOOLEAN, CAPTURE).recordDataPoint(settleGraph.getT(), 1);
                }

                // The actual capture.
                BufferedImage image = capture();

                double tCapture = 0.0; 
                if (settleGraph != null) {
                    tCapture = settleGraph.getT();
                    // Record end of capture.
                    settleGraph.getRow(BOOLEAN, CAPTURE).recordDataPoint(tCapture, 1);
                    settleGraph.getRow(BOOLEAN, CAPTURE).recordDataPoint(settleGraph.getT(), 0);
                }

                // Convert to Mat and if not full color, convert to gray.
                Mat mat = OpenCvUtils.toMat(image);
                if (!settleFullColor) {
                    Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
                }

                // Gaussian blur is the most expensive operation, so if it is large, we rescale the image instead.
                // This is effectively a box blur followed (later) by a Gaussian blur, i.e. still reasonable quality.
                // Rescaling will also make all subsequent steps significantly faster.
                // Do these calculations up front.
                final int resizeToMaxGaussianKernelSize = 5;
                int gaussianBlurEff = settleGaussianBlur;
                @SuppressWarnings("unused")
                int divisor = (resizeToMaxGaussianKernelSize > resizeToMaxGaussianKernelSize) ? 
                        (settleGaussianBlur+resizeToMaxGaussianKernelSize/2)/resizeToMaxGaussianKernelSize
                        : 1;

                int maskDiameter = 0;
                if (settleMaskCircle > 0.0) {
                    // Crop the image to the mask dimension. 
                    int imageDimension = Math.min(mat.rows(), mat.cols());
                    maskDiameter = Math.max(1, (int)(settleMaskCircle*imageDimension));
                    int maskedWidth= Math.min(mat.cols(), maskDiameter);
                    int maskedHeight= Math.min(mat.rows(), maskDiameter);
                    // Make it multiples of the rescale divisor*2.
                    maskDiameter = (int)Math.floor(maskDiameter/divisor/2)*divisor*2;
                    maskedWidth = (int)Math.floor(maskedWidth/divisor/2)*divisor*2;
                    maskedHeight = (int)Math.floor(maskedHeight/divisor/2)*divisor*2;
                    Rect rectCrop = new Rect(
                            (mat.cols() - maskedWidth)/2, (mat.rows() - maskedHeight)/2,
                            maskedWidth, maskedHeight);
                    Mat cropMat = mat.submat(rectCrop);
                    mat.release();
                    mat = cropMat;
                    if (maskFullsize == null) {
                        // This must be the first frame, also create the mask circle.
                        maskFullsize = createMask(mat, maskDiameter);
                        if (divisor == 1) {
                            // also valid as the rescaled mask
                            mask = maskFullsize;
                        }
                    }
                }

                if (settleContrastEnhance > 0.0) {
                    // Enhance the contrast. Note we need to do this before scaling the image down, so mixed
                    // colors can be created in the full dynamic range. 
                    mat = enhanceContrast(mat, maskFullsize);
                }

                if (divisor > 1) {
                    // Scale the image down, see the calculations further up.  
                    gaussianBlurEff = ((settleGaussianBlur)/divisor)|1;
                    Mat resizeMat = new Mat();
                    Imgproc.resize(mat, resizeMat, new Size(mat.cols()/divisor, mat.rows()/divisor), 1.0/divisor, 1.0/divisor);
                    mat.release();
                    mat = resizeMat;
                    maskDiameter /= divisor;
                }

                if (maskDiameter > 0 && mask == null) {
                    // This must be the first frame, also create the mask circle after rescale.
                    mask = createMask(mat, maskDiameter);
                }

                if (gaussianBlurEff > 1) {
                    // Apply the Gaussian blur, make the kernel size an odd number. 
                    Imgproc.GaussianBlur(mat, mat, new Size(gaussianBlurEff|1, gaussianBlurEff|1), 0);
                }

                if (settleGradients) {
                    // Apply Laplacian transform.
                    Mat gradientMat = new Mat();
                    Imgproc.Laplacian(mat, gradientMat, CvType.CV_16S, 3, 1, 0, Core.BORDER_REPLICATE );
                    Core.convertScaleAbs(gradientMat, gradientMat);
                    mat.release();
                    mat = gradientMat;
                }

                // Record the image with the capture time.
                if (settleGraph != null) {
                    BufferedImage img;
                    img = OpenCvUtils.toBufferedImage(mat);
                    settleImages.put(tCapture, img);
                    
                }

                // If this is the first time through the loop then assign the new image to
                // the lastSettleMat and loop again. We need at least two images to check.
                if (lastSettleMat == null) {
                    lastSettleMat = mat;
                    continue;
                }

                // Compute the differences of the two images according to the method.
                double result = settleMethod.computeDifference(lastSettleMat, mat, settleContrastEnhance, mask);
                if (settleGraph != null) {
                    settleGraph.getRow(DIFFERENCE, DATA).recordDataPoint(settleGraph.getT(), result);
                }

                // Release the lastSettleMat and store the new image as the lastSettleMat.
                lastSettleMat.release();
                lastSettleMat = mat;

                long t = System.currentTimeMillis();
                Logger.trace("autoSettleAndCapture t="+(t-t0)+" auto settle score: " + result);

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
                    // Timeout or debounced settleThreshold reached.
                    // Cleanup.
                    lastSettleMat.release();
                    lastSettleMat = null;
                    if (settleGraph != null) {
                        // Record last points in the graph. 
                        double tEnd = settleGraph.getT()+1;
                        settleGraph.getRow(BOOLEAN, CAPTURE).recordDataPoint(tEnd, 0);
                        settleGraph.getRow(DIFFERENCE, THRESHOLD).recordDataPoint(0.0, settleThreshold);
                        settleGraph.getRow(DIFFERENCE, THRESHOLD).recordDataPoint(tEnd, settleThreshold);
                        settleGraph.getRow(DIFFERENCE, DATA).recordDataPoint(tEnd, result);
                        // Set the graph to the camera.
                        setSettleGraph(settleGraph);
                        // Set recorded images along with the graph.
                        setRecordedImages(settleImages);
                        recordedMaskDiameter = maskDiameter;
                    }
                    Logger.debug("autoSettleAndCapture in {} ms", System.currentTimeMillis() - t0);
                    return image;
                }
            }
        }
        finally {
            // Whatever happens, always release these looping mats.
            if (maskFullsize != null) {
                maskFullsize.release();
                if (mask == maskFullsize) {
                    mask = null;
                }
            }
            if (mask != null) {
                mask.release();
            }
            if (lastSettleMat != null) {
                lastSettleMat.release();
            }
        }
    }

    protected Mat createMask(Mat mat, int maskDiameter) {
        Mat mask;
        mask = new Mat(mat.rows(), mat.cols(), CvType.CV_8U, Scalar.all(0));
        Imgproc.circle(mask,
                new Point(mat.cols()/2, mat.rows()/2),
                maskDiameter/2,
                new Scalar(255, 255, 255), -1);
        return mask;
    }

    protected Mat enhanceContrast(Mat mat, Mat mask) {
        // It's weirdly difficult to extract the minimum level (black point) from an image.
        // Core.norm(... NORM_MINMAX) does not seem to work and minMaxLoc() takes only single channel images. 
        // So we need to work with the channels individually here. I must be missing something.
        double max = SettleMethod.minimumRange/255.0;
        double range = SettleMethod.minimumRange/255.0;
        int nChannels = mat.channels();
        if (nChannels > 1) {
            Mat workingMat = new Mat();
            for (int cn=0; cn < nChannels; cn++) {
                Core.extractChannel(mat, workingMat, cn);
                MinMaxLocResult res = Core.minMaxLoc(workingMat, mask); // Note, mask can for once be null
                workingMat.release();
                max = Math.max(max, res.maxVal)/255.0;
                range = Math.max(range, res.maxVal-res.minVal)/255.0;
            }
        }
        else {
            MinMaxLocResult res = Core.minMaxLoc(mat, mask); // Note, mask can for once be null
            max = Math.max(max, res.maxVal)/255.0;
            range = Math.max(range, res.maxVal-res.minVal)/255.0;
        }
        double scale = settleContrastEnhance/range + (1.0 - settleContrastEnhance);
        double offset = -(max-range)*settleContrastEnhance/range;
        Mat tmpMat = new Mat();  
        Core.convertScaleAbs(mat, tmpMat, scale, offset*255.0);
        mat.release();
        mat = tmpMat;
        return mat;
    }

    protected BufferedImage createHeatMapDiagnosticImage(Mat mat0, Mat mat1) {
        // Record diagnostic images.
        Mat diagnosticMat = mat1;
        Mat mask = null;
        if (recordedMaskDiameter > 0) {
            // Also create the mask.
            mask = createMask(mat1, recordedMaskDiameter);
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
            // We have mat0, i.e. this is at least the second frame. 
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

            // Am I doing something wrong? This OpenCV arithmetic is awful to code. 
            Mat alphaMat = new Mat();
            // Square the normed diff, to emphasize large mevements for the alpha channel.
            Core.multiply(normMat, normMat, alphaMat, 1/255.0/255.0, CvType.CV_32F);
            // Create the complement for the alpha channel.
            Mat oneMat = new Mat(alphaMat.rows(), alphaMat.cols(), alphaMat.type(), Scalar.all(1.0));
            Mat invertedAlphaMat = new Mat();
            Core.subtract(oneMat, alphaMat, invertedAlphaMat);
            oneMat.release();
            // Use the alpha channel for the heat map.
            Mat mulMat = new Mat();
            Core.multiply(heatmapMat, alphaMat, mulMat, 1.0, CvType.CV_8U);
            heatmapMat.release();
            heatmapMat = mulMat;
            // Use the inverted alpha channel for the backgound image.
            mulMat = new Mat();
            Core.multiply(backgroundMat, invertedAlphaMat, mulMat, 1.0, CvType.CV_8U);
            backgroundMat.release();
            backgroundMat = mulMat;
            // Blend the two.
            Mat blendedMat = new Mat();
            Core.add(backgroundMat, heatmapMat, blendedMat);
            backgroundMat.release();
            heatmapMat.release();
            // Cleanup
            normMat.release();
            alphaMat.release();
            invertedAlphaMat.release();
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

        BufferedImage img = OpenCvUtils.toBufferedImage(diagnosticMat);
        // cleanup
        if (diagnosticMat != mat1) {
            diagnosticMat.release();
        }
        if (mask != null) {
            mask.release();
        }
        return img;
    }

    @Override
    public BufferedImage lightSettleAndCapture() throws Exception {
        actuateLightBeforeCapture();
        try {
            return settleAndCapture();
        }
        finally {
            actuateLightAfterCapture();
        }
    }

    @Override
    public BufferedImage settleAndCapture() throws Exception {
        try {
            Map<String, Object> globals = new HashMap<>();
            globals.put("camera", this);
            Configuration.get().getScripting().on("Camera.BeforeSettle", globals);
        }
        catch (Exception e) {
            Logger.warn(e);
        }

        try {
            // Make sure the camera (or its subject) stands still.
            waitForCompletion(CompletionType.WaitForStillstand);

            if (settleMethod == null) {
                // Method undetermined, probably created a new camera (no @Commit handler)
                settleMethod = SettleMethod.FixedTime;
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
        finally {

            try {
                Map<String, Object> globals = new HashMap<>();
                globals.put("camera", this);
                Configuration.get().getScripting().on("Camera.AfterSettle", globals);
            }
            catch (Exception e) {
                Logger.warn(e);
            }
        }
    }

    protected static void actuateLight(Actuator lightActuator, Object light) throws Exception {
        // Make sure it is actuated in a machine task, but only if the machine is enabled.
        Configuration.get().getMachine().executeIfEnabled(() -> {
            // Only actuate a light when the current state is unknown or different. 
            if (lightActuator.getLastActuationValue() == null 
                    || !lightActuator.getLastActuationValue().equals(light)) {
                lightActuator.actuate(light);
            }
            return null; 
        });
    }

    @Override
    public void actuateLightBeforeCapture(Object light) throws Exception {
        // Anti-glare: switch off opposite looking cameras.
        for (Camera camera : Configuration.get().getMachine().getAllCameras()) {
            if (camera != this
                    && (camera instanceof AbstractCamera)
                    && ((AbstractCamera) camera).isAntiGlareLightOff() 
                    && camera.getLooking() != this.getLooking()) {
                Actuator lightActuator = camera.getLightActuator();
                if (lightActuator != null 
                        && lightActuator.isActuated()) {
                    AbstractActuator.assertOnOffDefined(lightActuator);
                    actuateLight(lightActuator, lightActuator.getDefaultOffValue());
                }
            }
        }

        if (isBeforeCaptureLightOn()) {
            Actuator lightActuator = getLightActuator();
            if (lightActuator != null) {
                AbstractActuator.assertOnOffDefined(lightActuator);
                actuateLight(lightActuator, 
                        (light != null ? light : lightActuator.getDefaultOnValue()));
            }
        }
    }

    @Override
    public void actuateLightAfterCapture() throws Exception {
        if (isAfterCaptureLightOff()) {
            Actuator lightActuator = getLightActuator();
            if (lightActuator != null) {
                AbstractActuator.assertOnOffDefined(lightActuator);
                actuateLight(lightActuator, lightActuator.getDefaultOffValue());
            }
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

    protected TreeMap<Double, BufferedImage> getRecordedImages() {
        return recordedImages;
    }

    protected void setRecordedImages(TreeMap<Double, BufferedImage> recordedImages) {
        this.recordedImages = recordedImages;
        this.heatMappedImages = recordedImages == null ? null : new TreeMap<>();
    }

    public Double getRecordedImagePlayed() {
        return recordedImagePlayed;
    }

    public void setRecordedImagePlayed(Double recordedImagePlayed) {
        Object oldValue = this.recordedImagePlayed;
        this.recordedImagePlayed = recordedImagePlayed;
        firePropertyChange("recordedImagePlayed", oldValue, recordedImagePlayed);
        if (recordedImagePlayed != null && oldValue != recordedImagePlayed) {
            playRecordedImage(recordedImagePlayed);
        }
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

    public void playRecordedImage(double t) {
        if (recordedImages != null) {
            BufferedImage img = null;
            Map.Entry<Double, BufferedImage> entry1 = recordedImages.floorEntry(t);
            if (entry1 != null) {
                double tFrame = entry1.getKey();
                img = heatMappedImages.get(tFrame);
                if (img == null) {
                    // first time use, create the heat mapped image
                    Map.Entry<Double, BufferedImage> entry0 = recordedImages.lowerEntry(tFrame);
                    Mat mat0 = null;
                    if (entry0 != null) {
                        mat0 = OpenCvUtils.toMat(entry0.getValue());
                    }
                    Mat mat1 = OpenCvUtils.toMat(entry1.getValue());
                    img = createHeatMapDiagnosticImage(mat0, mat1);
                    if (mat0 != null) {
                        mat0.release();
                    }
                    mat1.release();
                    heatMappedImages.put(tFrame, img);
                }
                // I'm sure there's a better way to count the preceding images :-(
                int n = 0;
                for (Double t1 : recordedImages.keySet()) {
                    if (t1 > t) {
                        break;
                    }
                    ++n;
                }
                String message = "Camera settling, frame number "+n+", t=+"+String.format(Locale.US, "%.1f", tFrame)+"ms";
                MainFrame.get().getCameraViews().getCameraView(this)
                .showFilteredImage(img, message, 1500);
                ensureCameraVisible();
            }
        }
    }

    @Override
    public void ensureCameraVisible() {
        SwingUtilities.invokeLater(() -> {
            MainFrame.get().getCameraViews().ensureCameraVisible(this);
        });
    }

    @Override
    public Icon getPropertySheetHolderIcon() {
        return Icons.captureCamera;
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

    @Override
    public void findIssues(List<Issue> issues) {
        super.findIssues(issues);

        if ((unitsPerPixel.getX() == 0) || (unitsPerPixel.getY() == 0)) {
             issues.add(new Solutions.PlainIssue(
                    this, 
                    "Camera units per pixel has not been calibrated.", 
                    "Calibrate the camera's units per pixel.", 
                    Severity.Warning,
                    "https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-General-Camera-Setup#set-units-per-pixel"));
        }
        else if (!isSecondaryUnitsPerPixelCalibrated()) {
            issues.add(new Solutions.PlainIssue(
                    this, 
                    "Camera units per pixel can be calibrated for 3D scale estimation.", 
                    "Calibrate the camera's units per pixel at two different heights.", 
                    Severity.Suggestion,
                    "https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-General-Camera-Setup#set-units-per-pixel"));
        }
    }
}
