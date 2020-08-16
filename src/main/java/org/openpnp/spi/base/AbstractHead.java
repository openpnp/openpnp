package org.openpnp.spi.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Icon;

import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.IdentifiableList;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.core.Commit;

public abstract class AbstractHead extends AbstractModelObject implements Head {
    @Attribute
    protected String id;

    @Attribute(required = false)
    protected String name;

    @ElementList(required = false)
    protected IdentifiableList<Nozzle> nozzles = new IdentifiableList<>();

    @ElementList(required = false)
    protected IdentifiableList<Actuator> actuators = new IdentifiableList<>();

    @ElementList(required = false)
    protected IdentifiableList<Camera> cameras = new IdentifiableList<>();

    @Element(required = false)
    protected Location parkLocation = new Location(LengthUnit.Millimeters);
    
    @Deprecated
    @Element(required=false)
    protected boolean softLimitsEnabled = false;

    @Deprecated
    @Element(required = false)
    protected Location minLocation = null;

    @Deprecated
    @Element(required = false)
    protected Location maxLocation = null;
    
    @Element(required = false)
    protected String zProbeActuatorName;

    @Element(required = false)
    protected String pumpActuatorName;

    /**
     * Choice of Visual Homing Method.
     * 
     * Previous Visual Homing reset the controller to home coordinates not to the fiducial coordinates as one
     * might expect. As a consequence the fiducial location may shift its meaning before/after homing i.e. it cannot be captured. 
     * This behavior has been called a bug by Jason. But we absolutely need to migrate this behavior in order not to 
     * break all the captured coordinates on a machine!
     *
     * As a consequence the method is now a choice. Users with new machines can select the more natural  
     * ResetToFiducialLocation method. This also applies to all Users that had the fiducial location == homing location, 
     * including those that used extra after-homing G0 X Y to make it so (like myself). 
     *
     */
    public enum VisualHomingMethod {
        None,
        ResetToFiducialLocation,
        ResetToHomeLocation
    }
    
    @Attribute(required = false)
    private VisualHomingMethod visualHomingMethod = VisualHomingMethod.None;

    @Element(required = false)
    protected Location homingFiducialLocation = new Location(LengthUnit.Millimeters);


    protected Machine machine;

    public AbstractHead() {
        this.id = Configuration.createId("HED");
        this.name = getClass().getSimpleName();
    }

    @SuppressWarnings("unused")
    @Commit
    private void commit() {
        for (HeadMountable hm : getHeadMountables()) {
            hm.setHead(this);
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public List<Nozzle> getNozzles() {
        return Collections.unmodifiableList(nozzles);
    }

    @Override
    public Nozzle getNozzle(String id) {
        return nozzles.get(id);
    }

    @Override
    public List<Actuator> getActuators() {
        return Collections.unmodifiableList(actuators);
    }

    @Override
    public Actuator getActuator(String id) {
        return actuators.get(id);
    }

    @Override
    public Actuator getActuatorByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        for (Actuator actuator : actuators) {
            if (actuator.getName().equals(name)) {
                return actuator;
            }
        }
        return null;
    }

    @Override
    public List<Camera> getCameras() {
        return Collections.unmodifiableList(cameras);
    }

    @Override
    public Camera getCamera(String id) {
        return cameras.get(id);
    }

    @Override
    public void addCamera(Camera camera) throws Exception {
        camera.setHead(this);
        cameras.add(camera);
        fireIndexedPropertyChange("cameras", cameras.size() - 1, null, camera);
    }

    @Override
    public void removeCamera(Camera camera) {
        int index = cameras.indexOf(camera);
        if (cameras.remove(camera)) {
            fireIndexedPropertyChange("cameras", index, camera, null);
        }
    }

    @Override
    public void addActuator(Actuator actuator) throws Exception {
        actuator.setHead(this);
        actuators.add(actuator);
        fireIndexedPropertyChange("actuators", actuators.size() - 1, null, actuator);
    }

    @Override
    public void removeActuator(Actuator actuator) {
        int index = actuators.indexOf(actuator);
        if (actuators.remove(actuator)) {
            fireIndexedPropertyChange("actuators", index, actuator, null);
        }
    }

    @Override
    public void addNozzle(Nozzle nozzle) throws Exception {
        nozzle.setHead(this);
        nozzles.add(nozzle);
        fireIndexedPropertyChange("nozzles", nozzles.size() - 1, null, nozzle);
    }

    @Override
    public void removeNozzle(Nozzle nozzle) {
        int index = nozzles.indexOf(nozzle);
        if (nozzles.remove(nozzle)) {
            fireIndexedPropertyChange("nozzles", index, nozzle, null);
        }
    }

    @Override
    public List<HeadMountable> getHeadMountables() {
        List<HeadMountable> list = new ArrayList<>();
        list.addAll(nozzles);
        list.addAll(cameras);
        list.addAll(actuators);
        return list;
    }

    @Override
    public void moveToSafeZ(double speed) throws Exception {
        for (HeadMountable hm : getHeadMountables()) {
            hm.moveToSafeZ(speed);
        }
    }

    @Override
    public void home() throws Exception {
        for (HeadMountable hm : getHeadMountables()) {
            hm.home();
        }
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
    public Icon getPropertySheetHolderIcon() {
        return null;
    }

    @Override
    public Camera getDefaultCamera() throws Exception {
        List<Camera> cameras = getCameras();
        if (cameras == null || cameras.isEmpty()) {
            throw new Exception("No default camera available on head " + getName());
        }
        return cameras.get(0);
    }

    @Override
    public Nozzle getDefaultNozzle() throws Exception {
        List<Nozzle> nozzles = getNozzles();
        if (nozzles == null || nozzles.isEmpty()) {
            throw new Exception("No default nozzle available on head " + getName());
        }
        return nozzles.get(0);
    }

    @Override
    public HeadMountable getDefaultHeadMountable() throws Exception {
        // Camera takes precedence.
        List<Camera> cameras = getCameras();
        if (cameras != null && !cameras.isEmpty()) {
            return cameras.get(0);
        }
        // Fall back to any head mountable.
        List<HeadMountable> headMountables = getHeadMountables();
        if (headMountables == null || headMountables.isEmpty()) {
            throw new Exception("No default head mountable available on head " + getName());
        }
        return headMountables.get(0);
    }

    @Override
    public Machine getMachine() {
        return machine;
    }

    @Override
    public void setMachine(Machine machine) {
        this.machine = machine;
    }

    @Override
    public void moveToSafeZ() throws Exception {
        moveToSafeZ(getMachine().getSpeed());
    }

    public Location getParkLocation() {
        return parkLocation;
    }

    public void setParkLocation(Location parkLocation) {
        this.parkLocation = parkLocation;
    }

    public boolean isCarryingPart() {
        for (Nozzle nozzle : getNozzles()) {
            if (nozzle.getPart() != null) {
                return true;
            }
        }
        return false;
    }

    public double getMaxPartSpeed() {
        double speed = 1;

        for (Nozzle nozzle : getNozzles()) {
            if (nozzle.getPart() != null) {
                speed = Math.min(nozzle.getPart().getSpeed(), speed);
            }
        }

        return speed;
    }

    @Deprecated
    public Location getMinLocation() {
        return minLocation;
    }

    @Deprecated
    public void setMinLocation(Location minLocation) {
        this.minLocation = minLocation;
    }

    @Deprecated
    public Location getMaxLocation() {
        return maxLocation;
    }

    @Deprecated
    public void setMaxLocation(Location maxLocation) {
        this.maxLocation = maxLocation;
    }

    @Deprecated
    public boolean isSoftLimitsEnabled() {
        return softLimitsEnabled;
    }

    @Override
    public Actuator getZProbe() {
        return getActuatorByName(zProbeActuatorName); 
    }

    public String getzProbeActuatorName() {
        return zProbeActuatorName;
    }

    public void setzProbeActuatorName(String zProbeActuatorName) {
        this.zProbeActuatorName = zProbeActuatorName;
    }

    @Override
    public Actuator getPump() {
        return getActuatorByName(pumpActuatorName); 
    }

    public String getPumpActuatorName() {
        return pumpActuatorName;
    }

    public void setPumpActuatorName(String pumpActuatorName) {
        this.pumpActuatorName = pumpActuatorName;
    }

    public VisualHomingMethod getVisualHomingMethod() {
        return visualHomingMethod;
    }

    public void setVisualHomingMethod(VisualHomingMethod visualHomingMethod) {
        this.visualHomingMethod = visualHomingMethod;
    }

    public Location getHomingFiducialLocation() {
        return homingFiducialLocation;
    }

    public void setHomingFiducialLocation(Location homingFiducialLocation) {
        this.homingFiducialLocation = homingFiducialLocation;
    }
}
