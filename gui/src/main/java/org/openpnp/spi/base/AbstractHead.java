package org.openpnp.spi.base;

import java.util.List;

import org.openpnp.gui.support.Wizard;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.IdentifiableList;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.core.Commit;

public abstract class AbstractHead implements Head {
    protected Machine machine;
    @Attribute
    protected String id;
    @ElementList(required=false)
    protected IdentifiableList<Nozzle> nozzles = new IdentifiableList<Nozzle>();
    @ElementList(required=false)
    protected IdentifiableList<Actuator> actuators = new IdentifiableList<Actuator>();
    @ElementList(required=false)
    protected IdentifiableList<Camera> cameras = new IdentifiableList<Camera>();
    
    @Commit
    public void commit() {
        for (Nozzle nozzle : nozzles) {
            nozzle.setHead(this);
        }
        for (Camera camera : cameras) {
            camera.setHead(this);
        }
        for (Actuator actuator : actuators) {
            actuator.setHead(this);
        }
    }
    
    @Override
    public String getId() {
        return id;
    }

    @Override
    public List<Nozzle> getNozzles() {
        return nozzles;
    }

    @Override
    public Nozzle getNozzleById(String id) {
        return nozzles.get(id);
    }

    @Override
    public List<Actuator> getActuators() {
        return actuators;
    }

    @Override
    public Actuator getActuatorById(String id) {
        return actuators.get(id);
    }

    @Override
    public List<Camera> getCameras() {
        return cameras;
    }

    @Override
    public Camera getCameraById(String id) {
        return cameras.get(id);
    }

    @Override
    public Wizard getConfigurationWizard() {
        return null;
    }
}
