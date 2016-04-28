package org.openpnp.spi.base;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.Icon;

import org.openpnp.machine.reference.vision.ReferenceBottomVision;
import org.openpnp.machine.reference.vision.ReferenceFiducialLocator;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.FiducialLocator;
import org.openpnp.spi.Head;
import org.openpnp.spi.JobPlanner;
import org.openpnp.spi.JobProcessor;
import org.openpnp.spi.JobProcessor.Type;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.openpnp.spi.PartAlignment;
import org.openpnp.util.IdentifiableList;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.core.Commit;

import com.google.common.util.concurrent.FutureCallback;

public abstract class AbstractMachine implements Machine {
    /**
     * History:
     * 
     * Note: Can't actually use the @Version annotation because of a bug in SimpleXML. See
     * http://sourceforge.net/p/simple/mailman/message/27887562/
     * 
     * 1.0: Initial revision. 1.1: Added jobProcessors Map and deprecated JobProcesor and
     * JobPlanner.
     */

    @ElementList
    protected IdentifiableList<Head> heads = new IdentifiableList<>();

    @ElementList(required = false)
    protected IdentifiableList<Feeder> feeders = new IdentifiableList<>();

    @ElementList(required = false)
    protected IdentifiableList<Camera> cameras = new IdentifiableList<>();

    @ElementList(required = false)
    protected IdentifiableList<Actuator> actuators = new IdentifiableList<>();

    @Deprecated
    @Element(required = false)
    protected JobPlanner jobPlanner;

    @Deprecated
    @Element(required = false)
    protected JobProcessor jobProcessor;

    @ElementMap(entry = "jobProcessor", key = "type", attribute = true, inline = false,
            required = false)
    protected Map<JobProcessor.Type, JobProcessor> jobProcessors = new HashMap<>();

    @Element(required = false)
    protected PartAlignment partAlignment = new ReferenceBottomVision();

    @Element(required = false)
    protected FiducialLocator fiducialLocator = new ReferenceFiducialLocator();

    @Element(required = false)
    protected Location discardLocation = new Location(LengthUnit.Millimeters);

    @Attribute(required = false)
    protected double speed = 1.0D;

    protected Set<MachineListener> listeners = Collections.synchronizedSet(new HashSet<>());

    protected ThreadPoolExecutor executor;

    protected AbstractMachine() {}

    @SuppressWarnings("unused")
    @Commit
    private void commit() {
        for (Head head : heads) {
            head.setMachine(this);
        }
        if (jobProcessors.isEmpty()) {
            jobProcessors.put(JobProcessor.Type.PickAndPlace, jobProcessor);
            jobProcessor = null;
            jobPlanner = null;
        }
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

    @Override
    public Map<Type, JobProcessor> getJobProcessors() {
        return Collections.unmodifiableMap(jobProcessors);
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
        // TODO Auto-generated method stub
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

                // If a callback was supplied, call it with the results
                if (callback != null) {
                    if (exception != null) {
                        callback.onFailure(exception);
                    }
                    else {
                        callback.onSuccess(result);
                    }
                }

                // If there was an error cancel all pending tasks.
                if (exception != null) {
                    executor.shutdownNow();
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

    public PartAlignment getPartAlignment() {
        return partAlignment;
    }

    public void setPartAlignment(PartAlignment partAlignment) {
        this.partAlignment = partAlignment;
    }

    public FiducialLocator getFiducialLocator() {
        return fiducialLocator;
    }

    public void setFiducialLocator(FiducialLocator fiducialLocator) {
        this.fiducialLocator = fiducialLocator;
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
}
