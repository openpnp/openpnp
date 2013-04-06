package org.openpnp.spi.base;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.openpnp.util.IdentifiableList;
import org.simpleframework.xml.ElementList;

public abstract class AbstractMachine implements Machine {
    @ElementList
    protected IdentifiableList<Head> heads = new IdentifiableList<Head>();
    @ElementList(required=false)
    protected IdentifiableList<Feeder> feeders = new IdentifiableList<Feeder>();
    @ElementList(required=false)
    protected IdentifiableList<Camera> cameras = new IdentifiableList<Camera>();
    
    protected Set<MachineListener> listeners = Collections.synchronizedSet(new HashSet<MachineListener>());
    
    protected AbstractMachine() {
    }
    
    @Override
    public List<Head> getHeads() {
        return Collections.unmodifiableList(heads);
    }

    @Override
    public Head getHead(String id) {
        return heads.get(id);
    }

    @Override
    public List<Feeder> getFeeders() {
        return Collections.unmodifiableList(feeders);
    }

    @Override
    public Feeder getFeeder(String id) {
        return feeders.get(id);
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
    public void addFeeder(Feeder feeder) throws Exception {
        feeders.add(feeder);
    }

    @Override
    public void removeFeeder(Feeder feeder) {
        feeders.remove(feeder);
    }

    @Override
    public void addCamera(Camera camera) throws Exception {
        cameras.add(camera);
    }

    @Override
    public void removeCamera(Camera camera) {
        cameras.remove(camera);
    }

    public void fireMachineHeadActivity(Head head) {
        for (MachineListener listener : listeners) {
            listener.machineHeadActivity(this, head);
        }
    }
    
    public void fireMachineEnabled() {
        for (MachineListener listener : listeners) {
            listener.machineEnabled(this);
        }
    }
    
    public void fireMachineEnableFailed(String reason) {
        for (MachineListener listener : listeners) {
            listener.machineEnableFailed(this, reason);
        }
    }
    
    public void fireMachineDisabled(String reason) {
        for (MachineListener listener : listeners) {
            listener.machineDisabled(this, reason);
        }
    }
    
    public void fireMachineDisableFailed(String reason) {
        for (MachineListener listener : listeners) {
            listener.machineDisableFailed(this, reason);
        }
    }
}
