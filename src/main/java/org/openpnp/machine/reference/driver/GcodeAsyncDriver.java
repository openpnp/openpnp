/*
 * Copyright (C) 2020 <mark@makr.zone>
 * inspired and based on work
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

package org.openpnp.machine.reference.driver;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.driver.wizards.GcodeAsyncDriverSettings;
import org.openpnp.machine.reference.driver.wizards.GcodeDriverSettings;
import org.openpnp.model.AxesLocation;
import org.openpnp.spi.MotionPlanner.CompletionType;
import org.openpnp.spi.PropertySheetHolder.PropertySheet;
import org.openpnp.util.Collect;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;

/**
 * The GcodeAsyncDriver extends the GcodeDriver for asynchronous communication with the controller. 
 * The goal is to increase the command throughput/decrease latency to allow for small time step motion 
 * path generation/interpolation. 
 *  
 * GcodeAsyncDriver creates a writer thread sending commands in the background while freeing the calling thread 
 * up to continue with the job. While the GcodeDriver performs hand-shaking for every command by waiting for a 
 * mandatory reply, the GcodeAsyncDriver will just blindly send commands, utilizing intermediate buffering
 * and pipelining in the communications chain, including the command buffers and look-ahead motion planning
 * in the controller itself.  
 * 
 * While the GcodeDriver (through its on-by-one hand-shaking) knows when each and every command is 
 * acknowledged by the controller, the GcodeAsyncDriver does not. The responses (mostly a stream of 
 * "ok"s) are too generic to reliably detect how many and which commands have been acknowledged. Sometimes 
 * controllers will also output additional unsolicitated messages. GcodeAsyncDriver must therefore find a new 
 * way to implement hand-shaking when (and only when) it is really needed. Most importantly this is the case 
 * when OpenPnP wants to wait for the machine to physically have completed a motion sequence. GcodeAsyncDriver 
 * will therefore issue specific reporting commands where needed, making the responses uniquely recognizable, 
 * and marking the position in the response stream. 
 * 
 * FUTURE WORK:
 * 
 * To optimize the asynchronous operation, Actuator reads should also be handled differently. Often the 
 * commands to elicit sensor reading reports are shared by multiple Actuators. Therefore the responses are not 
 * distinguishable, when they arrive in the response stream. Furthermore, these commands are executed 
 * asynchronously on the controller, i.e. they create an immediate response with the readings, in parallel 
 * with any on-going motion i.e. not waiting for its completion first (which is of course a good thing). The 
 * textbook use case is 3D printing, where temperature readings must be monitored in parallel with the motion, 
 * therefore it is also the assumption that all relevant Open Source controllers provide this feature. 
 * 
 * All this prompts us to create a new way of Actuator reading. Actuators can be switched to monitoring mode 
 * and the (minimum) period of readings can be configured. GcodeAsyncDriver will then periodically insert the   
 * ACTUATOR_READ_COMMAND into the command stream. Whenever a response arrives, it is matched against all the 
 * Actuators' consolidated ACTUATOR_READ_REGEXes. Where they match, the parsed values are immediately
 * stored on the Actuators. If the Actuator is read, it will immediately return the latest value to the caller
 * speeding up the calling thread.
 * 
 * On monitoring Actuators, alarm limits can be (temporarily) set. If the readings violate the limits, an 
 * alarm status is stored. Task such as PartOn/PartOff vacuum sensing can therefore completely be done in 
 * the background, fully parallel to continuous motion, the alarm status can be checked in the next 
 * JobProcessor step. 
 * 
 */
public class GcodeAsyncDriver extends GcodeDriver {

    @Attribute(required=false)
    private long writerPollingInterval = 100;

    @Attribute(required=false)
    private long writerQueueTimeout = 60000;

    @Attribute(required=false)
    private int maxCommandsQueued = 1000;

    @Attribute(required=false)
    private boolean confirmationFlowControl = false;

    @Attribute(required = false)
    private int interpolationMaxSteps = 32;

    @Attribute(required = false)
    private double interpolationTimeStep = 0.01;

    @Attribute(required = false)
    private int interpolationDistStep = 8;


    private WriterThread writerThread;

    static public class CommandLine extends Line {
        final long timeout;

        public CommandLine(String line, long timeout) {
            super(line);
            this.timeout = timeout;
        }

        public long getTimeout() {
            return timeout;
        }
    }
    protected LinkedBlockingQueue<CommandLine> commandQueue;

    public boolean isConfirmationFlowControl() {
        return confirmationFlowControl;
    }

    public void setConfirmationFlowControl(boolean confirmationFlowControl) {
        this.confirmationFlowControl = confirmationFlowControl;
    }

    @Override 
    public Integer getInterpolationMaxSteps() {
        return interpolationMaxSteps;
    }

    public void setInterpolationMaxSteps(int interpolationMaxSteps) {
        this.interpolationMaxSteps = interpolationMaxSteps;
    }

    @Override
    public Double getInterpolationTimeStep() {
        return interpolationTimeStep;
    }

    public void setInterpolationTimeStep(double interpolationTimeStep) {
        this.interpolationTimeStep = interpolationTimeStep;
    }

    @Override
    public Integer getInterpolationMinStep() {
        return interpolationDistStep;
    }

    public void setInterpolationDistStep(int interpolationDistStep) {
        this.interpolationDistStep = interpolationDistStep;
    }

    @Override
    protected void connectThreads() throws Exception {
        super.connectThreads();
        commandQueue = new LinkedBlockingQueue<>(maxCommandsQueued);
        writerThread = new WriterThread();
        writerThread.setDaemon(true);
        writerThread.start();
    }

    @Override
    protected void disconnectThreads() {
        try {
            if (writerThread != null && writerThread.isAlive()) {
                writerThread.join(3000);
            }
            commandQueue = null;
        }
        catch (Exception e) {
            Logger.error("disconnect()", e);
        }

        super.disconnectThreads();
    }

    protected class WriterThread extends Thread {

        @Override
        public void run() {
            CommandLine lastCommand = null;
            // Get the copy that is valid for this thread. 
            LinkedBlockingQueue<CommandLine> commandQueue = GcodeAsyncDriver.this.commandQueue;
            
            long wantedConfirmations = 0;
            while (!disconnectRequested) {
                CommandLine command;
                try {
                    command = commandQueue.poll(writerPollingInterval,
                            TimeUnit.MILLISECONDS);
                }
                catch (InterruptedException e1) {
                    continue;
                }
                if (command == null) {
                    continue;
                }
                try {
                    if (confirmationFlowControl && lastCommand != null) {
                        // Before we can send the new command, make sure the wanted confirmation count of the last command was received.
                        waitForConfirmation(lastCommand.toString(), lastCommand.getTimeout(), wantedConfirmations);
                    }
                    // Set up the wanted confirmations for next time.
                    wantedConfirmations = receivedConfirmations.get() + 1;
                    lastCommand = command;
                    getCommunications().writeLine(command.line);
                    Logger.trace("[{}] >> {}", getCommunications().getConnectionName(), command);
                }
                catch (IOException e) {
                    Logger.error("Write error", e);
                    return;
                }
                catch (Exception e) {
                    // We probably got a timeout exception. We can't throw from the writer thread. Therefore, set 
                    // the exception as an error response, it will be reported when the driver wants to do the next step. 
                    errorResponse = new Line(e.getMessage());
                    //Logger.error("[{}] {}", getCommunications().getConnectionName(), e);
                }
            }
            Logger.trace("[{}] diconnectRequested, bye-bye.", getCommunications().getConnectionName());
        }
    }

    @Override
    protected void bailOnError() throws Exception {
        super.bailOnError();
        if (! writerThread.isAlive()) {
            throw new Exception("IO Error on writing to the controller.");
        }
    }
    /**
     * Note this Override will completely change the way commands are sent and hand-shaking is done.
     * So it MUST NOT call super.sendCommand()
     */
    @Override
    public void sendCommand(String command, long timeout) throws Exception {
        bailOnError();
        if (command == null) {
            return;
        }

        Logger.debug("{} commandQueue.offer({}, {})...", getCommunications().getConnectionName(), command, timeout);
        command = preProcessCommand(command);
        CommandLine commandLine = new CommandLine(command, timeout);
        commandQueue.offer(commandLine, writerQueueTimeout, TimeUnit.MILLISECONDS);
    }

    @Override
    public void waitForCompletion(ReferenceHeadMountable hm, 
            CompletionType completionType) throws Exception {
        if (!isMotionPending()) {
            return;
        }
        // Issue the M400 in the super class.
        super.waitForCompletion(hm, completionType);
        // Then make sure we get a uniquely recognizable confirmation. 
        if (completionType.isWaitingForDrivers()) {
            // Explicitly wait for the controller's acknowledgment here. 
            // This is signaled with a position report.
            AxesLocation location = getMomentaryLocation();
            // TODO: Compare to current executed driver location.
            
        }
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return Collect.concat(super.getPropertySheets(), new PropertySheet[] { 
                new PropertySheetWizardAdapter(new GcodeAsyncDriverSettings(this), "Advanced Settings")
        });
    }
}
