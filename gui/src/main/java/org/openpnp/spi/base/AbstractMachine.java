package org.openpnp.spi.base;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openpnp.RequiresConfigurationResolution;
import org.openpnp.gui.support.Wizard;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.openpnp.util.IdentifiableList;
import org.simpleframework.xml.ElementList;

public abstract class AbstractMachine implements Machine, RequiresConfigurationResolution {
    @ElementList(required=false)
    protected IdentifiableList<Head> heads = new IdentifiableList<Head>();
    @ElementList(required=false)
    protected IdentifiableList<Feeder> feeders = new IdentifiableList<Feeder>();
    @ElementList(required=false)
    protected IdentifiableList<Camera> cameras = new IdentifiableList<Camera>();
    
    protected Set<MachineListener> listeners = Collections.synchronizedSet(new HashSet<MachineListener>());
    
    @Override
    public void resolve(Configuration configuration) throws Exception {
        for (Head head : heads) {
            configuration.resolve(head);
        }
        for (Feeder feeder : feeders) {
            configuration.resolve(feeder);
        }
        for (Camera camera : cameras) {
            configuration.resolve(camera);
        }
    }

    @Override
    public List<Head> getHeads() {
        return heads;
    }

    @Override
    public Head getHeadById(String id) {
        return heads.get(id);
    }

    @Override
    public List<Feeder> getFeeders() {
        return feeders;
    }

    @Override
    public Feeder getFeederById(String id) {
        return feeders.get(id);
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
    public void home() throws Exception {
        for (Head head : heads) {
            head.home();
        }
    }

    @Override
    public void addListener(MachineListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(MachineListener listener) {
        listeners.remove(listener);
    }
    
    @Override
    public Wizard getConfigurationWizard() {
        return null;
    }

    protected void fireMachineHeadActivity(Machine machine, Head head) {
        for (MachineListener listener : listeners) {
            listener.machineHeadActivity(machine, head);
        }
    }
    
    protected void fireMachineEnabled(Machine machine) {
        for (MachineListener listener : listeners) {
            listener.machineEnabled(machine);
        }
    }
    
    protected void fireMachineEnableFailed(Machine machine, String reason) {
        for (MachineListener listener : listeners) {
            listener.machineEnableFailed(machine, reason);
        }
    }
    
    protected void fireMachineDisabled(Machine machine, String reason) {
        for (MachineListener listener : listeners) {
            listener.machineDisabled(machine, reason);
        }
    }
    
    protected void fireMachineDisableFailed(Machine machine, String reason) {
        for (MachineListener listener : listeners) {
            listener.machineDisableFailed(machine, reason);
        }
    }
}
