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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.driver.GcodeDriver.CommandType;
import org.openpnp.model.AxesLocation;
import org.openpnp.spi.MotionPlanner.CompletionType;
import org.pmw.tinylog.Logger;

/**
 * The GcodeAsyncDriver extends the GcodeDriver for asynchronous communication with the controller. 
 * The goal is to increase the command throughput to allow for fine-grained motion path generation. 
 *  
 * It creates a writer thread that does the writing in the background while freeing the calling thread up
 * to continue with the job. While the GcodeDriver performs hand-shaking for every command by waiting for a 
 * mandatory reply, the GcodeAsyncDriver will blindly send the commands, utilizing intermediate buffering
 * and pipelining in the communications chain, including the command buffering of the controller itself.  
 * 
 * While the GcodeDriver (through its on-by-one hand-shaking) knows when each and every command is 
 * acknowledged by the controller, the GcodeAsyncDriver does not. The responses (mostly a stream of 
 * "ok"s) are too generic to reliably detect how many and which commands have been acknowledged. Sometimes 
 * controllers will output additional informations. GcodeAsyncDriver must therefore find a new way to 
 * implement hand-shaking when (and only when) it is really needed. Most importantly this is the case 
 * when OpenPnP wants to wait for the machine to physically have completed a motion sequence. GcodeAsyncDriver 
 * will therefore issue specific reporting commands where needed, making the responses uniquely recognizable, 
 * and marking the position in the response stream. 
 * 
 * To optimize this asynchronous operation, Actuator reads should also be handled differently. Often the 
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
    private long writerPollingInterval = 100;
    private long writerQueueTimeout = 60000;
    private int maxCommandsQueued = 1000;

    private WriterThread writerThread;
    private final List<String> emptyResponses = Collections.unmodifiableList(new ArrayList<>());

    protected class Command {
        final String line;
        final long timeout;

        public Command(String line, long timeout) {
            super();
            this.line = line;
            this.timeout = timeout;
        }
    }
    protected LinkedBlockingQueue<Command> commandQueue;
    private long confirmations;
    private String errorResponse;

    @Override
    protected void connectThreads() throws Exception {
        super.connectThreads();
        commandQueue = new LinkedBlockingQueue<>(maxCommandsQueued);
        writerThread = new WriterThread();
        writerThread.setDaemon(true);
        writerThread.start();
        errorResponse = null;
        confirmations = 0;
    }

    @Override
    protected void disconnectThreads() {
        try {
            if (writerThread != null && writerThread.isAlive()) {
                if (commandQueue.isEmpty()) {
                    writerThread.interrupt();
                }
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
            while (!disconnectRequested) {
                Command command;
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
                    getCommunications().writeLine(command.line);
                }
                catch (IOException e) {
                    Logger.error("Write error", e);
                    return;
                }
                Logger.trace("[{}] >> {}", getCommunications().getConnectionName(), command);
            }
        }
    }

    /**
     * Note this Override will completely change the way commands are sent and hand-shaking is done.
     * So it MUST NOT call super.sendCommand()
     */
    @Override
    public List<String> sendCommand(String command, long timeout) throws Exception {

        if (errorResponse != null) {
            // In the GcodeAsyncDriver everything is asynchronous (really?) and therefore the error is 
            // only indicated when the next command is sent.
            String error = errorResponse; 
            errorResponse = null;
            throw new Exception("Error response from controller: " + error);
            
        }

        if (command != null) {
            Logger.debug("sendCommand({}, {})...", command, timeout);
            commandQueue.offer(new Command(command, timeout), writerQueueTimeout, TimeUnit.MILLISECONDS);
        }

        return emptyResponses;
    }

    @Override
    protected void processResponse(String line) {
        super.processResponse(line);

        String regex = getCommand(null, CommandType.COMMAND_CONFIRM_REGEX);
        if (regex != null && line.matches(regex)) {
            confirmations++;
            return;
        }
        regex = getCommand(null, CommandType.COMMAND_ERROR_REGEX);
        if (regex != null && line.matches(regex)) {
            errorResponse = line;
            return;
        }
    }

    @Override
    public void waitForCompletion(ReferenceHeadMountable hm, 
            CompletionType completionType) throws Exception {
        super.waitForCompletion(hm, completionType);
        if (completionType.isWaitingForDrivers()) {
            // Explicitly wait for the controller's acknowledgment here. 
            // This is signaled with a position report.
            AxesLocation location = getMomentaryLocation();
            // TODO: Compare to current executed driver location.
            
        }
    }
}
