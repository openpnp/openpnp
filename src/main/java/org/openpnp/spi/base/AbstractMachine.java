package org.openpnp.spi.base;

import java.io.Closeable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.Icon;

import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.MappedAxes;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Driver;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.openpnp.spi.NozzleTip;
import org.openpnp.spi.Signaler;
import org.openpnp.spi.PartAlignment;
import org.openpnp.util.IdentifiableList;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.core.Commit;

import com.google.common.util.concurrent.FutureCallback;

public abstract class AbstractMachine extends AbstractModelObject implements Machine {
    /**
     * History:
     * 
     * Note: Can't actually use the @Version annotation because of a bug in SimpleXML. See
     * http://sourceforge.net/p/simple/mailman/message/27887562/
     * 
     * 1.0: Initial revision. 1.1: Added jobProcessors Map and deprecated JobProcesor and
     * JobPlanner.
     */

    public enum State {
        ERROR
    }

    @ElementList(required = false)
    protected IdentifiableList<Axis> axes = new IdentifiableList<>();

    @ElementList
    protected IdentifiableList<Head> heads = new IdentifiableList<>();

    @ElementList(required = false)
    protected IdentifiableList<Signaler> signalers = new IdentifiableList<>();

    @ElementList(required = false)
    protected IdentifiableList<Feeder> feeders = new IdentifiableList<>();

    @ElementList(required = false)
    protected IdentifiableList<Camera> cameras = new IdentifiableList<>();

    @ElementList(required = false)
    protected IdentifiableList<Actuator> actuators = new IdentifiableList<>();

    @ElementList(required = false)
    protected IdentifiableList<PartAlignment> partAlignments = new IdentifiableList<>();

    @ElementList(required = false)
    protected IdentifiableList<Driver> drivers = new IdentifiableList<>();

    @Element(required = false)
    protected Location discardLocation = new Location(LengthUnit.Millimeters);

    @Attribute(required = false)
    protected double speed = 1.0D;
    
    @ElementMap(required = false)
    protected HashMap<String, Object> properties = new HashMap<>();
    
    @ElementList(required = false)
    protected IdentifiableList<NozzleTip> nozzleTips = new IdentifiableList<>();

    protected Set<MachineListener> listeners = Collections.synchronizedSet(new HashSet<>());

    protected ThreadPoolExecutor executor;

    protected AbstractMachine() {}

    @SuppressWarnings("unused")
    @Commit
    protected void commit() {
        for (Head head : heads) {
            head.setMachine(this);
        }
    }

    @Override
    public List<Axis> getAxes() {
        return Collections.unmodifiableList(axes);
    }

    @Override
    public Axis getAxis(String id) {
        return axes.get(id);
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
    public List<Signaler> getSignalers() {
        return Collections.unmodifiableList(signalers);
    }

    @Override
    public Signaler getSignaler(String id) {
        return signalers.get(id);
    }

    @Override
    public Signaler getSignalerByName(String name) {
        for (Signaler signaler : signalers) {
            if (signaler.getName().equals(name)) {
                return signaler;
            }
        }
        return null;
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
    public List<Driver> getDrivers() {
        return Collections.unmodifiableList(drivers);
    }

    @Override
    public Driver getDriver(String id) {
        return drivers.get(id);
    }

    @Override
    public List<PartAlignment> getPartAlignments() {
        return Collections.unmodifiableList(partAlignments);
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
    public Feeder getFeederByName(String name) {
        for (Feeder feeder : feeders) {
            if (feeder.getName().equals(name)) {
                return feeder;
            }
        }
        return null;
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
    public void addAxis(Axis axis) throws Exception {
        axes.add(axis);
        fireIndexedPropertyChange("axes", axes.size() - 1, null, axis);
    }

    @Override
    public void removeAxis(Axis axis) {
        int index = axes.indexOf(axis);
        if (axes.remove(axis)) {
            fireIndexedPropertyChange("axes", index, axis, null);
        }
    }

    @Override 
    public void permutateAxis(Axis axis, int direction) {
        int index0 = axes.indexOf(axis);
        int index1 = direction > 0 ? index0+1 : index0-1;
        if (0 <= index1 && axes.size() > index1) {
            axes.remove(axis);
            axes.add(index1, axis);
            fireIndexedPropertyChange("axes", index0, axis, axes.get(index0));
            fireIndexedPropertyChange("axes", index1, axes.get(index0), axis);
        }
    }

    @Override
    public void addFeeder(Feeder feeder) throws Exception {
        feeders.add(feeder);
        fireIndexedPropertyChange("feeders", feeders.size() - 1, null, feeder);
    }

    @Override
    public void removeFeeder(Feeder feeder) {
        int index = feeders.indexOf(feeder);
        if (feeders.remove(feeder)) {
            fireIndexedPropertyChange("feeders", index, feeder, null);
        }
    }

    @Override
    public void addCamera(Camera camera) throws Exception {
        camera.setHead(null);
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
        actuator.setHead(null);
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
    public void addDriver(Driver driver) throws Exception {
        drivers.add(driver);
        fireIndexedPropertyChange("drivers", drivers.size() - 1, null, drivers);
    }

    @Override
    public void removeDriver(Driver driver) {
        int index = drivers.indexOf(driver);
        if (drivers.remove(driver)) {
            fireIndexedPropertyChange("drivers", index, driver, null);
        }
    }

    @Override 
    public void permutateDriver(Driver driver, int direction) {
        int index0 = drivers.indexOf(driver);
        int index1 = direction > 0 ? index0+1 : index0-1;
        if (0 <= index1 && drivers.size() > index1) {
            drivers.remove(driver);
            drivers.add(index1, driver);
            fireIndexedPropertyChange("drivers", index0, driver, drivers.get(index0));
            fireIndexedPropertyChange("drivers", index1, drivers.get(index0), driver);
        }
    }

    @Override
    public void addSignaler(Signaler signaler) throws Exception {
        signalers.add(signaler);
        fireIndexedPropertyChange("signalers", signalers.size() - 1, null, signalers);
    }

    @Override
    public void removeSignaler(Signaler signaler) {
        int index = signalers.indexOf(signaler);
        if (signalers.remove(signaler)) {
            fireIndexedPropertyChange("signalers", index, signaler, null);
        }
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

    public void fireMachineBusy(boolean busy) {
        for (MachineListener listener : listeners) {
            listener.machineBusy(this, busy);
        }
    }

    @Override
    public Icon getPropertySheetHolderIcon() {
        return null;
    }

    @Override
    public Future<Object> submit(Runnable runnable) {
        return submit(Executors.callable(runnable));
    }

    @Override
    public <T> Future<T> submit(Callable<T> callable) {
        return submit(callable, null);
    }

    @Override
    public <T> Future<T> submit(final Callable<T> callable, final FutureCallback<T> callback) {
        return submit(callable, callback, false);
    }

    @Override
    public <T> Future<T> submit(final Callable<T> callable, final FutureCallback<T> callback,
            final boolean ignoreEnabled) {
        synchronized (this) {
            if (executor == null || executor.isShutdown()) {
                executor = new ThreadPoolExecutor(1, 1, 1, TimeUnit.SECONDS,
                        new LinkedBlockingQueue<>());
            }
        }

        Callable<T> wrapper = new Callable<T>() {
            public T call() throws Exception {
                // TODO: lock driver

                // Notify listeners that the machine is now busy
                fireMachineBusy(true);

                // Call the task, storing the result and exception if any
                T result = null;
                Exception exception = null;
                try {
                    if (!ignoreEnabled && !isEnabled()) {
                        throw new Exception("Machine has not been started.");
                    }
                    result = callable.call();
                }
                catch (Exception e) {
                    exception = e;
                }

                // If there was an error cancel all pending tasks.
                if (exception != null) {
                    executor.shutdownNow();
                }

                // If a callback was supplied, call it with the results
                if (callback != null) {
                    if (exception != null) {
                        callback.onFailure(exception);
                    }
                    else {
                        callback.onSuccess(result);
                    }
                }

                // TODO: unlock driver

                // If no more tasks are scheduled notify listeners that
                // the machine is no longer busy
                if (executor.getQueue().isEmpty()) {
                    fireMachineBusy(false);
                }

                // Finally, fulfill the Future by either throwing the
                // exception or returning the result.
                if (exception != null) {
                    throw exception;
                }
                return result;
            }
        };

        return executor.submit(wrapper);
    }

    @Override
    public Head getDefaultHead() throws Exception {
        List<Head> heads = getHeads();
        if (heads == null || heads.isEmpty()) {
            throw new Exception("No default head available.");
        }
        return heads.get(0);
    }

    public Location getDiscardLocation() {
        return discardLocation;
    }

    public void setDiscardLocation(Location discardLocation) {
        this.discardLocation = discardLocation;
    }

    @Override
    public void setSpeed(double speed) {
        this.speed = speed;
    }

    @Override
    public double getSpeed() {
        return speed;
    }

    @Override
    public Object getProperty(String name) {
        return properties.get(name);
    }

    @Override
    public void setProperty(String name, Object value) {
        properties.put(name, value);
    }
    
    @Override
    public List<NozzleTip> getNozzleTips() {
        return Collections.unmodifiableList(nozzleTips);
    }

    @Override
    public void addNozzleTip(NozzleTip nozzleTip) throws Exception {
        nozzleTips.add(nozzleTip);
        fireIndexedPropertyChange("nozzleTips", nozzleTips.size() - 1, null, nozzleTip);
    }

    @Override
    public void removeNozzleTip(NozzleTip nozzleTip) {
        int index = nozzleTips.indexOf(nozzleTip);
        if (nozzleTips.remove(nozzleTip)) {
            fireIndexedPropertyChange("nozzleTips", index, nozzleTip, null);
        }
    }
    
    @Override
    public NozzleTip getNozzleTip(String id) {
        return nozzleTips.get(id);
    }

    @Override
    public NozzleTip getNozzleTipByName(String name) {
        for (NozzleTip nozzleTip : nozzleTips) {
            if (nozzleTip.getName().equals(name)) {
                return nozzleTip;
            }
        }
        return null;
    }
}
