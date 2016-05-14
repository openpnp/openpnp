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
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.openpnp.model.Location;

import com.google.common.util.concurrent.FutureCallback;



/**
 * Machine represents the pick and place machine itself. It provides the information and interface
 * needed to cause the machine to do work. A Machine has one or more Heads. Unless otherwise noted,
 * the methods in this class block while performing their operations.
 */
public interface Machine extends WizardConfigurable, PropertySheetHolder, Closeable {
    /**
     * Gets all active heads on the machine.
     * 
     * @return
     */
    public List<Head> getHeads();

    public Head getHead(String id);

    /**
     * Gets a List of Feeders attached to the Machine.
     * 
     * @return
     */
    public List<Feeder> getFeeders();

    public Feeder getFeeder(String id);


    /**
     * Gets a List of Cameras attached to the Machine that are not attached to Heads.
     * 
     * @return
     */
    public List<Camera> getCameras();

    public Camera getCamera(String id);

    /**
     * Get a list of Actuators that are attached to this Machine and not to a Head.
     * 
     * @return
     */
    public List<Actuator> getActuators();

    /**
     * Get the Actuator attached to this Machine and not to a Head that has the specified id.
     * 
     * @param id
     * @return
     */
    public Actuator getActuator(String id);

    public Actuator getActuatorByName(String name);

    /**
     * Commands all Heads to move to their home positions and reset their current positions to
     * 0,0,0,0. Depending on the head configuration of the machine the home positions may not all be
     * the same but the end result should be that any head commanded to move to a certain position
     * will end up in the same position.
     */
    public void home() throws Exception;

    /**
     * Returns whether the Machine is currently ready for commands.
     */
    public boolean isEnabled();

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

    public void addListener(MachineListener listener);

    public void removeListener(MachineListener listener);

    public List<Class<? extends Feeder>> getCompatibleFeederClasses();

    public List<Class<? extends Camera>> getCompatibleCameraClasses();

    public void addFeeder(Feeder feeder) throws Exception;

    public void removeFeeder(Feeder feeder);

    public void addCamera(Camera camera) throws Exception;

    public void removeCamera(Camera camera);

    public Map<JobProcessor.Type, JobProcessor> getJobProcessors();

    public Future<Object> submit(Runnable runnable);

    public <T> Future<T> submit(Callable<T> callable);

    public <T> Future<T> submit(final Callable<T> callable, final FutureCallback<T> callback);

    /**
     * Submit a task to be run with access to the Machine. This is the primary entry point into
     * executing any blocking operation on the Machine. If you are doing anything that results in
     * the Machine doing something it should happen here.
     * 
     * Tasks can be cancelled and interrupted via the returned Future. Tasks which operate in a loop
     * should check Thread.currentThread().isInterrupted().
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

    public Head getDefaultHead() throws Exception;
    
    public PartAlignment getPartAlignment();
    
    public FiducialLocator getFiducialLocator();
    
    public Location getDiscardLocation();
    
    public void setSpeed(double speed);
    
    public double getSpeed();
}
