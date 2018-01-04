package org.openpnp.spi.base;

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
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PasteDispenser;
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

    @ElementList(required = false)
    protected IdentifiableList<PasteDispenser> pasteDispensers = new IdentifiableList<>();
    
    @Element(required = false)
    protected Location parkLocation = new Location(LengthUnit.Millimeters);

    protected Machine machine;

    public AbstractHead() {
        this.id = Configuration.createId("HED");
        this.name = getClass().getSimpleName();
    }

    @SuppressWarnings("unused")
    @Commit
    private void commit() {
        for (Nozzle nozzle : nozzles) {
            nozzle.setHead(this);
        }
        for (Camera camera : cameras) {
            camera.setHead(this);
        }
        for (Actuator actuator : actuators) {
            actuator.setHead(this);
        }
        for (PasteDispenser pasteDispenser : pasteDispensers) {
            pasteDispenser.setHead(this);
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
    public void moveToSafeZ(double speed) throws Exception {
        for (Nozzle nozzle : nozzles) {
            nozzle.moveToSafeZ(speed);
        }
        for (Camera camera : cameras) {
            camera.moveToSafeZ(speed);
        }
        for (Actuator actuator : actuators) {
            actuator.moveToSafeZ(speed);
        }
        for (PasteDispenser dispenser : pasteDispensers) {
            dispenser.moveToSafeZ(speed);
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
    public List<PasteDispenser> getPasteDispensers() {
        return Collections.unmodifiableList(pasteDispensers);
    }

    @Override
    public PasteDispenser getPasteDispenser(String id) {
        return pasteDispensers.get(id);
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
    public PasteDispenser getDefaultPasteDispenser() throws Exception {
        List<PasteDispenser> dispensers = getPasteDispensers();
        if (dispensers == null || dispensers.isEmpty()) {
            throw new Exception("No default paste dispenser available on head " + getName());
        }
        return dispensers.get(0);
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
}
