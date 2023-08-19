package org.openpnp.spi.base;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.SwingUtilities;

import org.openpnp.CameraListener;
import org.openpnp.ConfigurationListener;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Icons;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.VisionProvider;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

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

    @Deprecated
    @Attribute(required = false)
    boolean autoViewPlaneZ = false;

    @Element(required = false)
    protected VisionProvider visionProvider;

    @Element(required = false)
    protected Length roamingRadius = new Length(0, LengthUnit.Millimeters);

    protected Set<ListenerEntry> listeners = Collections.synchronizedSet(new HashSet<>());

    protected Head head;

    protected Integer width;

    protected Integer height;

    private boolean headSet = false;


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
        Object oldValue = this.id;
        this.id = id;
        firePropertyChange("id", oldValue, id);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        Object oldValue = this.name;
        this.name = name;
        firePropertyChange("name", oldValue, name);
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
        Object oldValue = this.head;
        this.head = head;
        this.headSet = true;
        firePropertyChange("head", oldValue, head);
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
        Object oldValue = this.enableUnitsPerPixel3D;
        this.enableUnitsPerPixel3D = enableUnitsPerPixel3D;
        firePropertyChange("enableUnitsPerPixel3D", oldValue, enableUnitsPerPixel3D);
    }

    @Override
    public Location getUnitsPerPixel() {
        return getUnitsPerPixel(defaultZ);
    }

    @Override
    public Location getUnitsPerPixel(Length viewingPlaneZ) {
        if (!isUnitsPerPixelAtZCalibrated()) {
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

    @Override
    public Location getUnitsPerPixelAtZ() {
        if (getAxisZ() != null) {
            // Camera has an axis (virtual or physical), so it may set the viewing plane. Note, this automatically 
            // also excludes the bottom camera.
            Length viewingPlaneZ = getLocation().getLengthZ();
            if (viewingPlaneZ.compareTo(getSafeZ()) < 0 ) {
                // Z is below Safe Z, so this is a purposefully set viewingPlaneZ.
                return getUnitsPerPixel(viewingPlaneZ);
            }
        }
        return Camera.super.getUnitsPerPixelAtZ();
    }

    @Override
    public boolean isUnitsPerPixelAtZCalibrated() {
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
        Object oldValue = this.unitsPerPixel;
        this.unitsPerPixel = unitsPerPixel;
        firePropertyChange("unitsPerPixel", oldValue, unitsPerPixel);
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
        Object oldValue = this.unitsPerPixel;
        this.unitsPerPixel = unitsPerPixelPrimary;
        firePropertyChange("unitsPerPixelPrimary", oldValue, unitsPerPixelPrimary);
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
        Object oldValue = this.unitsPerPixelSecondary;
        this.unitsPerPixelSecondary = unitsPerPixelSecondary;
        firePropertyChange("unitsPerPixelSecondary", oldValue, unitsPerPixelSecondary);
    }

    @Override
    public Length getDefaultZ() {
        return defaultZ;
    }

    public void setDefaultZ(Length defaultZ) {
        Object oldValue = this.defaultZ;
        this.defaultZ = defaultZ;
        firePropertyChange("defaultZ", oldValue, defaultZ);
    }

    /**
     * @return Get the z coordinate of camera where the primary units per pixel measurement was made. 
     */
    public Length getCameraPrimaryZ() {
        return cameraPrimaryZ;
    }

    public void setCameraPrimaryZ(Length cameraPrimaryZ) {
        Object oldValue = this.cameraPrimaryZ;
        this.cameraPrimaryZ = cameraPrimaryZ;
        firePropertyChange("cameraPrimaryZ", oldValue, cameraPrimaryZ);
    }

    /**
     * @return Get the z coordinate of camera where the secondary units per pixel measurement was made. 
     */
    public Length getCameraSecondaryZ() {
        return cameraSecondaryZ;
    }

    public void setCameraSecondaryZ(Length cameraSecondaryZ) {
        Object oldValue = this.cameraSecondaryZ;
        this.cameraSecondaryZ = cameraSecondaryZ;
        firePropertyChange("cameraSecondaryZ", oldValue, cameraSecondaryZ);
    }

    @Override
    public Length getRoamingRadius() {
        return roamingRadius;
    }

    public void setRoamingRadius(Length roamingRadius) {
        Object oldValue = this.roamingRadius;
        this.roamingRadius = roamingRadius;
        firePropertyChange("roamingRadius", oldValue, roamingRadius);
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
        if (!isUnitsPerPixelAtZCalibrated()) {
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
        Object oldValue = this.looking;
        this.looking = looking;
        firePropertyChange("looking", oldValue, looking);
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
        Object oldValue = this.beforeCaptureLightOn;
        this.beforeCaptureLightOn = beforeCaptureLightOn;
        firePropertyChange("beforeCaptureLightOn", oldValue, beforeCaptureLightOn);
    }

    public boolean isUserActionLightOn() {
        return userActionLightOn;
    }

    public void setUserActionLightOn(boolean userActionLightOn) {
        Object oldValue = this.userActionLightOn;
        this.userActionLightOn = userActionLightOn;
        firePropertyChange("userActionLightOn", oldValue, userActionLightOn);
    }

    public boolean isAfterCaptureLightOff() {
        return afterCaptureLightOff;
    }

    public void setAfterCaptureLightOff(boolean afterCaptureLightOff) {
        Object oldValue = this.afterCaptureLightOff;
        this.afterCaptureLightOff = afterCaptureLightOff;
        firePropertyChange("afterCaptureLightOff", oldValue, afterCaptureLightOff);
    }

    public boolean isAntiGlareLightOff() {
        return antiGlareLightOff;
    }

    public void setAntiGlareLightOff(boolean antiGlareLightOff) {
        Object oldValue = this.antiGlareLightOff;
        this.antiGlareLightOff = antiGlareLightOff;
        firePropertyChange("antiGlareLightOff", oldValue, antiGlareLightOff);
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
        Object oldValue = this.visionProvider;
        this.visionProvider = visionProvider;
        visionProvider.setCamera(this);
        firePropertyChange("visionProvider", oldValue, visionProvider);
    }

    @Override
    public VisionProvider getVisionProvider() {
        return visionProvider;
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
                        && (lightActuator.isActuated() == null || lightActuator.isActuated())) {
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
}
