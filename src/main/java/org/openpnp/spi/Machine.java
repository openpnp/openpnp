/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.spi;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.model.Solutions;

import com.google.common.util.concurrent.FutureCallback;

/**
 * Machine represents the pick and place machine itself. It provides the information and interface
 * needed to cause the machine to do work. A Machine has one or more Heads. Unless otherwise noted,
 * the methods in this class block while performing their operations.
 */
public interface Machine extends WizardConfigurable, PropertySheetHolder, Closeable, Solutions.Subject {
    /**
     * Gets a List of Axes attached to the Machine.
     * 
     * @return
     */
    public List<Axis> getAxes();

    public Axis getAxis(String id);

    /**
     * Gets all active heads on the machine.
     * 
     * @return
     */
    public List<Head> getHeads();

    public Head getHead(String id);

    public Head getHeadByName(String name);
    
    /**
     * Gets a List of Signalers attached to the Machine.
     *
     * @return
     */
    public List<Signaler> getSignalers();

    public Signaler getSignaler(String id);

    public Signaler getSignalerByName(String name);

    /**
     * Gets a List of Feeders attached to the Machine.
     * 
     * @return
     */
    public List<Feeder> getFeeders();

    public Feeder getFeeder(String id);

    public Feeder getFeederByName(String name);

    /**
     * Gets a List of Cameras attached to the Machine that are not attached to Heads.
     * 
     * @return
     */
    public List<Camera> getCameras();

    /**
     * Gets a list of all Cameras attached to the Machine and to all the Heads.
     * @return
     */
    public List<Camera> getAllCameras();

    public Camera getCamera(String id);

    /**
     * Get a list of Actuators that are attached to this Machine and not to a Head.
     * 
     * @return
     */
    public List<Actuator> getActuators();

    /**
     * Gets a list of all Actuator attached to the Machine and to all the Heads.
     * @return
     */
    public List<Actuator> getAllActuators();

    /**
     * Get the Actuator attached to this Machine and not to a Head that has the specified id.
     * 
     * @param id
     * @return
     */
    public Actuator getActuator(String id);

    public Actuator getActuatorByName(String name);

    /**
     * Gets a List of Drivers attached to the Machine.
     * 
     * @return
     */
    public List<Driver> getDrivers();

    public Driver getDriver(String id);

    public MotionPlanner getMotionPlanner();

    /**
     * Commands all Heads to perform visual homing if available. Depending on the head configuration of the machine 
     * the home positions may not all be the same but the end result should be that any head commanded to move to a certain position
     * will end up in the same position.
     */
    public void home() throws Exception;

    /**
     * Returns whether the Machine is currently ready for commands.
     */
    public boolean isEnabled();

    /**
     * Returns whether the Machine is homed
     */
    public boolean isHomed();

    /**
     * Attempts to bring the Machine to a ready state or attempts to immediately stop it depending
     * on the value of enabled.
     * 
     * If true, this would include turning on motor drivers, turning on compressors, resetting
     * solenoids, etc. If the Machine is unable to become ready for any reason it should throw an
     * Exception explaining the reason. This method should block until the Machine is ready.
     * 
     * After this method is called successfully, isEnabled() should return true unless the Machine
     * encounters some error.
     * 
     * If false, stops the machine and disables it as soon as possible. This may include turning off
     * power to motors and stopping compressors. It is expected that the machine may need to be
     * re-homed after this is called.
     * 
     * If the Machine cannot be stopped for any reason, this method may throw an Exception
     * explaining the reason but this should probably only happen in very extreme cases. This method
     * should effectively be considered a software emergency stop. After this method returns,
     * isEnabled() should return false until setEnabled(true) is successfully called again.
     */
    public void setEnabled(boolean enabled) throws Exception;
    
    public void setHomed(boolean homed);

    public void addListener(MachineListener listener);

    public void removeListener(MachineListener listener);

    public List<Class<? extends Axis>> getCompatibleAxisClasses();

    public List<Class<? extends Feeder>> getCompatibleFeederClasses();

    public List<Class<? extends Camera>> getCompatibleCameraClasses();

    public List<Class<? extends Nozzle>> getCompatibleNozzleClasses();

    public List<Class<? extends Actuator>> getCompatibleActuatorClasses();

    public List<Class<? extends Signaler>> getCompatibleSignalerClasses();

    public List<Class<? extends Driver>> getCompatibleDriverClasses();

    public List<Class<? extends MotionPlanner>> getCompatibleMotionPlannerClasses();

    public void addAxis(Axis axis) throws Exception;

    public void removeAxis(Axis axis);

    public void permutateAxis(Axis axis, int direction);

    public void addDriver(Driver driver) throws Exception;

    public void removeDriver(Driver driver);

    public void permutateDriver(Driver driver, int direction);

    public void addFeeder(Feeder feeder) throws Exception;

    public void removeFeeder(Feeder feeder);

    public void addSignaler(Signaler signaler) throws Exception;

    public void removeSignaler(Signaler signaler);

    public void addCamera(Camera camera) throws Exception;

    public void removeCamera(Camera camera);

    public void permutateCamera(Camera driver, int direction);

    public void addActuator(Actuator actuator) throws Exception;

    public void removeActuator(Actuator actuator);

    public void permutateActuator(Actuator actuator, int direction);

    public PnpJobProcessor getPnpJobProcessor();
    
    public Future<Object> submit(Runnable runnable);

    public <T> Future<T> submit(Callable<T> callable);

    public <T> Future<T> submit(final Callable<T> callable, final FutureCallback<T> callback);

    public boolean getHomeAfterEnabled();

    /**
     * Submit a task to be run with access to the Machine. The submit() and execute() methods are 
     * the primary entry points into executing any blocking operation on the Machine. If you are 
     * doing anything that results in the Machine doing something it should happen here.
     * 
     * With the submit() method, tasks can be cancelled and interrupted via the returned Future. 
     * 
     * Tasks which operate in a loop should check Thread.currentThread().isInterrupted().
     * 
     * When a task begins the MachineListeners are notified with machineBusy(true). When the task
     * ends, if there are no more tasks to run then machineBusy(false) is called.
     * 
     * TODO: When any task is running the driver for the machine is locked and any calls to the
     * driver outside of the task will throw an Exception.
     * 
     * If any tasks throws an Exception then all queued future tasks are cancelled.
     * 
     * If a task includes a callback the callback is executed before the next task begins.
     * 
     * TODO: By supplying a tag you can guarantee that there is only one of a certain type of task
     * queued. Attempting to queue another task with the same tag will return null and the task will
     * not be queued.
     * 
     * @param callable
     * @param callback
     * @param ignoreEnabled True if the task should execute even if the machine is not enabled. This
     *        is specifically for enabling the machine and should not typically be used elsewhere.
     */
    public <T> Future<T> submit(final Callable<T> callable, final FutureCallback<T> callback,
            boolean ignoreEnabled);

    /**
     * Execute a task to be run with access to the Machine. The submit() and execute() methods are 
     * the primary entry points into executing any blocking operation on the Machine. If you are 
     * doing anything that results in the Machine doing something it should happen here.
     * 
     * With the execute() method, tasks are executed and waited for, rather than queued. 
     * If called from inside a machine task the execution is immediate. If called from a different 
     * thread, the task is submitted and then the calling thread will wait for completion.  
     *   
     * Return value and exceptions are always handled as if called directly. 
     * 
     * @param <T>
     * @param callable
     * @param onlyIfEnabled True if the task must only be executed if the machine is enabled.
     * @param busyTimeout If the machine is busy executing other submitted task, the execution 
     * will be rejected when the timeout (in milliseconds) expires, throwing a TimeoutException. 
     * This will typically happen, when a long-running operation like a Job is pending.  
     * @return
     * @throws Exception
     */
    public <T> T execute(Callable<T> callable, boolean onlyIfEnabled, long busyTimeout) throws Exception;

    public long DEFAULT_TASK_BUSY_TIMEOUT_MS = 1000;

    /**
     * Calls {@link #execute(Callable, boolean, long)} with default busy timeout. 
     * 
     * @param <T>
     * @param callable
     * @return
     * @throws Exception
     */
    public default <T> T execute(Callable<T> callable) throws Exception {
        return execute(callable, false, DEFAULT_TASK_BUSY_TIMEOUT_MS);
    }

    /**
     * Same as execute() but the task is only executed if the Machine is enabled. 
     * 
     * @param <T>
     * @param callable
     * @return
     * @throws Exception
     */
    public default <T> T executeIfEnabled(Callable<T> callable) throws Exception {
        return execute(callable, true, DEFAULT_TASK_BUSY_TIMEOUT_MS);
    }

    /**
     * Determines whether the given thread is a task thread currently executed by the machine. 
     * 
     * @param thread
     * @return
     */
    public boolean isTask(Thread thread);

    /**
     * @return True if a machine task is currently running/pending.
     */
    public boolean isBusy();

    public Head getDefaultHead() throws Exception;

    public List<PartAlignment> getPartAlignments();

    public FiducialLocator getFiducialLocator();

    public Location getDiscardLocation();

    public void setSpeed(double speed);

    public double getSpeed();
    
    public Object getProperty(String name);
    
    public void setProperty(String name, Object value);

    /**
     * Get a list of the NozzleTips currently attached to the Nozzle.
     * 
     * @return
     */
    public List<NozzleTip> getNozzleTips();

    public void addNozzleTip(NozzleTip nozzleTip) throws Exception;
    
    public void removeNozzleTip(NozzleTip nozzleTip);
    
    public NozzleTip getNozzleTip(String id);
    
    public NozzleTip getNozzleTipByName(String name);

    /**
     * @return True if the tool in machine controls should be auto-selected based on targeted user action.
     */
    public boolean isAutoToolSelect();

    /**
     * @return True if the Z Park button should move all other HeadMountables to Safe Z. 
     */
    public boolean isSafeZPark();
    
    /**
     * @return True if the machine heads should be parked after the machine was homed.
     */
    public boolean isParkAfterHomed();
    
    /**
     * 
     * Virtual Z axes (typically on cameras) are invisible, therefore it can easily be overlooked 
     * by users that it is at unsafe Z. When they later press the Move tool to camera location button, 
     * an unexpected Z down-move will result, potentially crashing the tool. 
     * The maximum allowable roaming distance at unsafe Z therefore limits the jogging area 
     * within which an unsafe virtual Z is kept. It should be enough to fine-adjust a captured
     * location, but jogging further away will automatically move the virtual axis to Safe Z.
     * 
     * @return Maximum allowable roaming distance at unsafe Z. 
     */
    public Length getUnsafeZRoamingDistance();
}
