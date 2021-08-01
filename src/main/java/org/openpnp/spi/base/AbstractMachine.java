package org.openpnp.spi.base;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.Icon;

import org.openpnp.machine.reference.axis.ReferenceLinearTransformAxis;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Solutions;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Driver;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.openpnp.spi.MotionPlanner.CompletionType;
import org.openpnp.spi.NozzleTip;
import org.openpnp.spi.PartAlignment;
import org.openpnp.spi.Signaler;
import org.openpnp.util.IdentifiableList;
import org.pmw.tinylog.Logger;
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

    volatile protected Thread taskThread;

    protected AbstractMachine() {}

    @SuppressWarnings("unused")
    @Commit
    protected void commit() {
        for (Head head : heads) {
            head.setMachine(this);
        }
    }

    public void addHead(Head head) {
        head.setMachine(this);
        heads.add(head);
    }

    @Override
    public List<Axis> getAxes() {
        return Collections.unmodifiableList(axes);
    }

    @Override
    public Axis getAxis(String id) {
        return axes.get(id);
    }

    /**
     * Find a default machine axis by type. This is just an educated guess that is good as a default assignment
     * to be reviewed by the user.
     * 
     * @param type
     * @return
     */
    public AbstractAxis getDefaultAxis(Axis.Type type) {
        // Look for a controller axis.
        AbstractAxis defaultAxis = null;
        for (Axis axis : getAxes()) {
            if (axis.getType() == type && axis instanceof AbstractControllerAxis) {
                defaultAxis = (AbstractAxis) axis;
                break;
            }
        }
        if (defaultAxis != null) {
            if (type != Axis.Type.Z) {
                // Unless it's Z, we look for transforms on top.
                for (Axis axis : getAxes()) {
                    if (axis instanceof AbstractSingleTransformedAxis 
                            && ((AbstractSingleTransformedAxis)axis).getInputAxis() == defaultAxis) {
                        defaultAxis = (AbstractAxis) axis;
                        break;
                    }
                }
            }
            // Look for linear transforms on top-
            for (Axis axis : getAxes()) {
                if (axis instanceof ReferenceLinearTransformAxis 
                        && ((ReferenceLinearTransformAxis)axis).getPrimaryInputAxis() == defaultAxis) {
                    defaultAxis = (AbstractAxis) axis;
                    break;
                }
            }
        }
        return defaultAxis;
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
    public Head getHeadByName(String name) {
        for (Head head : heads) {
            if (head.getName().equals(name)) {
                return head;
            }
        }
        return null;
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
    public List<Camera> getAllCameras() {
        Stream<Camera> stream = Stream.of();
        for (Head head : getHeads()) {
            stream = Stream.concat(stream, head.getCameras().stream());
        }
        stream = Stream.concat(stream, getCameras().stream());
        return stream.collect(Collectors.toList());
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
    public List<Actuator> getAllActuators() {
        Stream<Actuator> stream = Stream.of();
        for (Head head : getHeads()) {
            stream = Stream.concat(stream, head.getActuators().stream());
        }
        stream = Stream.concat(stream, getActuators().stream());
        return stream.collect(Collectors.toList());
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
        for (NozzleTip nt : getNozzleTips()) {
            nt.home();
        }
    }

    @Override
    public void addListener(MachineListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
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
            // Purge it out of Head-Mountables.
            for (Head head : getHeads()) {
                for (HeadMountable hm : head.getHeadMountables()) {
                    if (hm.getAxis(axis.getType()) == axis) {
                        ((AbstractHeadMountable)hm).setAxis(null, axis.getType());
                    }
                }
            }
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
    public void permutateCamera(Camera camera, int direction) {
        int index0 = cameras.indexOf(camera);
        int index1 = direction > 0 ? index0+1 : index0-1;
        if (0 <= index1 && cameras.size() > index1) {
            cameras.remove(camera);
            cameras.add(index1, camera);
            fireIndexedPropertyChange("cameras", index0, camera, cameras.get(index0));
            fireIndexedPropertyChange("cameras", index1, cameras.get(index0), camera);
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

    public void fireMachineTargetedUserAction(HeadMountable hm) {
        for (MachineListener listener : listeners) {
            listener.machineTargetedUserAction(this, hm);
        }
    }

    public void fireMachineActuatorActivity(Actuator actuator) {
        for (MachineListener listener : listeners) {
            listener.machineActuatorActivity(this, actuator);
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

    public void fireMachineAboutToBeDisabled(String reason) {
        for (MachineListener listener : listeners) {
            listener.machineAboutToBeDisabled(this, reason);
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

    public void fireMachineHomed(boolean isHomed) {
        for (MachineListener listener : listeners) {
            listener.machineHomed(this, isHomed);
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
                try {
                    boolean isBusy = isBusy();
                    // TODO: this should also lock drivers
                    setTaskThread(Thread.currentThread());

                    if (!isBusy) {
                        // Notify listeners that the machine is now busy
                        fireMachineBusy(true);
                    }

                    // Call the task, storing the result and exception if any
                    T result = null;
                    Exception exception = null;
                    try {
                        if (!ignoreEnabled && !isEnabled()) {
                            throw new Exception("Machine has not been started.");
                        }
                        result = callable.call();
                        // Make sure all pending motion commands are planned and sent to the controllers. 
                        // This does not necessarily wait for the motion to be complete physically, as this would 
                        // be undesirable for continuous Jog commands.  
                        getMotionPlanner().waitForCompletion(null, CompletionType.CommandJog);
                    }
                    catch (Exception e) {
                        exception = e;
                    }

                    if (exception != null) {
                        try {
                            // If there was an exception, we still need to execute the moves that were already in the queue
                            // when it happened. When full asynchronous operation is configured (location confirmation flow control)
                            // OpenPnP will not necessarily know which commands were executed successfully. We therefore issue a 
                            // WaitForStillstand, which will result in a position report and sync OpenPnP's internal position 
                            // with that of the machine.  
                            Logger.trace(exception, "Exception caught, executing pending motion");
                            getMotionPlanner().waitForCompletion(null, CompletionType.WaitForStillstand);
                        }
                        catch (Exception e) {
                            // If there is a second exception, there is likely something fundamentally wrong with the driver or communications. 
                            // We rely on user diagnosis to re-home the machine when necessary, there is really nothing we can do, except log this 
                            // secondary exception and hope the first one is conclusive for the user. 
                            Logger.error(e, "Secondary exception when executing pending motion planner commands");
                        }
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

                    // Finally, fulfill the Future by either throwing the
                    // exception or returning the result.
                    if (exception != null) {
                        throw exception;
                    }
                    return result;
                }
                finally {
                    // If no more tasks are scheduled notify listeners that
                    // the machine is no longer busy
                    if (executor.getQueue().isEmpty()) {
                        // TODO: this should also unlock drivers
                        fireMachineBusy(false);
                        setTaskThread(null);
                    }
                }
            }
        };

        return executor.submit(wrapper);
    }

    @Override
    public <T> T execute(final Callable<T> callable, final boolean onlyIfEnabled, final long timeout) 
            throws Exception {
        if (onlyIfEnabled && !isEnabled()) {
            // Ignore the task if the machine is not enabled.
            Logger.trace("Machine not enabled, task ignored.");
            return null;
        }
        if (isTask(Thread.currentThread())) {
            // We are already on the machine task, just execute this.
            return callable.call();
        }
        else {
            // Otherwise, submit a machine task and wait for its completion.
            try {
                long t1 = System.currentTimeMillis() + timeout;
                while (isBusy()) {
                    if (System.currentTimeMillis() >= t1) {
                        throw new TimeoutException("Machine still busy after timeout expired, task rejected.");
                    }
                    Thread.yield();
                }
                Future<T> future = submit(callable, null, false);
                return future.get();
            }
            catch (ExecutionException e) {
                if (e.getCause() instanceof Exception) {
                    throw (Exception)e.getCause();
                }
                throw e;
            }
        }
    }

    protected Thread getTaskThread() {
        return taskThread;
    }

    protected void setTaskThread(Thread taskThread) {
        this.taskThread = taskThread;
    }

    @Override
    public boolean isTask(Thread thread) {
        if (taskThread == null || thread == null) {
            return false;
        }
        return taskThread.getId() == thread.getId();
    }

    @Override
    public boolean isBusy() {
        return taskThread != null;
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

    @Override
    public void findIssues(Solutions solutions) {
        // MotionPlanner.
        getMotionPlanner().findIssues(solutions);
        // Recurse into axes.
        for (Axis axis : getAxes()) {
            axis.findIssues(solutions);
        }
        // Recurse into heads
        for (Head head : getHeads()) {
            head.findIssues(solutions);
        }
        // Recurse into machine cameras.  
        for (Camera camera : getCameras()) {
            camera.findIssues(solutions);
        }
        // Recurse into machine actuators.  
        for (Actuator actuator : getActuators()) {
            actuator.findIssues(solutions);
        }
        // Recurse into drivers.  
        for (Driver driver : getDrivers()) {
            driver.findIssues(solutions);
        }
        // Recurse into feeders.  
        for (Feeder feeder : getFeeders()) {
            feeder.findIssues(solutions);
        }
    }
}
