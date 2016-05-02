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

package org.openpnp.machine.reference;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Job;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.FiducialLocator;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.spi.PnpJobProcessor;
import org.openpnp.util.FiniteStateMachine;
import org.openpnp.util.MovableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferencePnpJobProcessor implements PnpJobProcessor {
    private static final Logger logger = LoggerFactory.getLogger(ReferencePnpJobProcessor.class);

    enum State {
        Uninitialized,
        Ready,
        PreFlight,
        FiducialCheck,
        Plan,
        ChangeNozzleTip,
        Feed,
        Pick,
        Align,
        Place,
        Complete,
        Aborted,
        Cleanup,
        Stopped
    }

    enum Message {
        Initialize,
        Next,
        Complete,
        Abort,
        Reset
    }

    private FiniteStateMachine<State, Message, Message> fsm =
            new FiniteStateMachine<>(State.Uninitialized);

    protected Message nextMessage;

    protected Job job;

    protected List<JobPlacement> jobPlacements;

    protected Machine machine;

    protected Head head;

    protected Map<Nozzle, JobPlacement> plannedPlacements = new HashMap<>();

    public ReferencePnpJobProcessor() {
        fsm.add(State.Uninitialized, Message.Initialize, State.Ready, this::doReady);

        fsm.add(State.Ready, Message.Next, State.PreFlight, this::doPreFlight);
        fsm.add(State.Ready, Message.Abort, State.Aborted, this::doAbort);

        fsm.add(State.PreFlight, Message.Next, State.FiducialCheck, this::doFiducialCheck);
        fsm.add(State.PreFlight, Message.Abort, State.Aborted, this::doAbort);

        fsm.add(State.FiducialCheck, Message.Next, State.Plan, this::doPlan);
        fsm.add(State.FiducialCheck, Message.Abort, State.Aborted, this::doAbort);

        fsm.add(State.Plan, Message.Next, State.ChangeNozzleTip, this::doChangeNozzleTip);
        fsm.add(State.Plan, Message.Abort, State.Aborted, this::doAbort);
        fsm.add(State.Plan, Message.Complete, State.Complete, this::doComplete);

        fsm.add(State.ChangeNozzleTip, Message.Next, State.Feed, this::doFeed);
        fsm.add(State.ChangeNozzleTip, Message.Abort, State.Aborted, this::doAbort);

        fsm.add(State.Feed, Message.Next, State.Pick, this::doPick);
        fsm.add(State.Feed, Message.Abort, State.Aborted, this::doAbort);

        fsm.add(State.Pick, Message.Next, State.Align, this::doAlign);
        fsm.add(State.Pick, Message.Abort, State.Aborted, this::doAbort);

        fsm.add(State.Align, Message.Next, State.Place, this::doPlace);
        fsm.add(State.Align, Message.Abort, State.Aborted, this::doAbort);

        fsm.add(State.Place, Message.Next, State.Plan, this::doPlan);
        fsm.add(State.Place, Message.Abort, State.Aborted, this::doAbort);

        fsm.add(State.Complete, Message.Next, State.Cleanup, this::doCleanup);

        fsm.add(State.Aborted, Message.Next, State.Cleanup, this::doCleanup);

        fsm.add(State.Cleanup, Message.Next, State.Stopped, this::doStop);

        fsm.add(State.Stopped, Message.Reset, State.Uninitialized, this::doReset);
    }

    public void initialize(Job job) throws Exception {
        this.job = job;
        nextMessage = fsm.send(Message.Initialize);
    }

    public boolean next() throws Exception {
        // If we've reached the Stopped state the process is complete. We reset the FSM and
        // return false to indicate that we're finished.
        if (fsm.getState() == State.Stopped) {
            nextMessage = fsm.send(Message.Reset);
            return false;
        }
        if (nextMessage == null) {
            nextMessage = Message.Next;
        }
        nextMessage = fsm.send(nextMessage);
        return true;
    }

    public void abort() throws Exception {
        nextMessage = fsm.send(Message.Abort);
        // Run to completion, handles cleanup.
        while (next());
    }

    protected Message doReady() throws Exception {
        if (job == null) {
            throw new Exception("Can't start a null Job.");
        }
        return null;
    }

    protected Message doPreFlight() throws Exception {
        this.machine = Configuration.get().getMachine();
        this.head = this.machine.getDefaultHead();
        this.jobPlacements = new ArrayList<>();
        for (BoardLocation boardLocation : job.getBoardLocations()) {
            for (Placement placement : boardLocation.getBoard().getPlacements()) {
                JobPlacement jp = new JobPlacement(boardLocation, placement);

                if (placement.getPart() == null) {
                    throw new Exception(String.format("Part not found for board %s, placement %s",
                            boardLocation.getBoard().getName(), placement.getId()));
                }

                findNozzleTip(placement.getPart());

                findFeeder(placement.getPart());

                // TODO: Optionally (based on config) validate part heights must be > 0

                jobPlacements.add(jp);
            }
        }
        return null;
    }

    protected Message doFiducialCheck() throws Exception {
        FiducialLocator locator = Configuration.get().getMachine().getFiducialLocator();
        for (BoardLocation boardLocation : job.getBoardLocations()) {
            if (!boardLocation.isEnabled()) {
                continue;
            }
            if (!boardLocation.isCheckFiducials()) {
                continue;
            }
            Location location = locator.locateBoard(boardLocation);
            boardLocation.setLocation(location);
        }
        return null;
    }

    protected Message doPlan() throws Exception {
        plannedPlacements.clear();

        // Sort the placements by part height and get only the ones with
        // the same height as the first.
        List<JobPlacement> jobPlacements = this.jobPlacements.stream()
                .sorted(Comparator.comparing(JobPlacement::getPartHeight))
                .collect(Collectors.toList());

        if (jobPlacements.isEmpty()) {
            return Message.Complete;
        }

        // Collect only the placements that have the same height as the first one. This
        // guarantees that we never place a taller part before a shorter one.
        double firstHeight = jobPlacements.get(0).getPartHeight();
        jobPlacements = jobPlacements.stream().filter(jobPlacement -> {
            return jobPlacement.getPartHeight() == firstHeight;
        }).collect(Collectors.toList());

        // Get the list of nozzles we have available
        List<Nozzle> nozzles = head.getNozzles();

        // And determine the best order to process the placements in to utilize the nozzle
        // tips we currently have loaded.
        // TODO: just faked for now
        for (Nozzle nozzle : nozzles) {
            if (jobPlacements.isEmpty()) {
                break;
            }
            plannedPlacements.put(nozzle, jobPlacements.remove(0));
        }
        return null;
    }

    protected Message doChangeNozzleTip() throws Exception {
        return null;
    }

    protected Message doFeed() throws Exception {
        return null;
    }

    protected Message doPick() throws Exception {
        return null;
    }

    protected Message doAlign() throws Exception {
        return null;
    }

    protected Message doPlace() throws Exception {
        return null;
    }

    protected Message doComplete() throws Exception {
        return null;
    }

    protected Message doAbort() throws Exception {
        return null;
    }

    protected Message doCleanup() throws Exception {
        // Safe Z the machine
        head.moveToSafeZ();
        // Discard any currently picked parts
        for (Nozzle nozzle : head.getNozzles()) {
            if (nozzle.getPart() != null) {
                discard(nozzle);
            }
        }
        // Home the machine
        // TODO: Move to park position instead.
        // https://github.com/openpnp/openpnp/issues/76
        machine.home();
        return null;
    }

    protected Message doStop() throws Exception {
        return null;
    }

    protected Message doReset() throws Exception {
        this.job = null;
        return null;
    }

    protected void discard(Nozzle nozzle) throws Exception {
        // move to the discard location
        MovableUtils.moveToLocationAtSafeZ(nozzle,
                Configuration.get().getMachine().getDiscardLocation());
        // discard the part
        nozzle.place();
        nozzle.moveToSafeZ();
    }

    protected NozzleTip findNozzleTip(Part part) throws Exception {
        for (Nozzle nozzle : head.getNozzles()) {
            for (NozzleTip nozzleTip : nozzle.getNozzleTips()) {
                if (nozzleTip.canHandle(part)) {
                    return nozzleTip;
                }
            }
        }
        throw new Exception("No compatible nozzle tip found for part " + part.getId());
    }

    protected Feeder findFeeder(Part part) throws Exception {
        for (Feeder feeder : machine.getFeeders()) {
            if (feeder.getPart() == part && feeder.isEnabled()) {
                return feeder;
            }
        }
        throw new Exception("No compatible, enabled feeder found for part " + part.getId());
    }

    public class JobPlacement {
        public final BoardLocation boardLocation;
        public final Placement placement;

        public JobPlacement(BoardLocation boardLocation, Placement placement) {
            this.boardLocation = boardLocation;
            this.placement = placement;
        }

        public double getPartHeight() {
            return placement.getPart().getHeight().convertToUnits(LengthUnit.Millimeters)
                    .getValue();
        }
    }

    public static void main(String[] args) throws Exception {
        ReferencePnpJobProcessor jp = new ReferencePnpJobProcessor();

        System.out.println(jp.fsm.toGraphviz());

        System.out.println(">>> cannot call next() without first initializing");
        try {
            jp.next();
            throw new Error();
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }

        System.out.println();
        System.out.println(">>> a job runs to completion");
        jp.initialize(new Job());
        while (jp.next()) {
        }

        System.out.println();
        System.out.println(">>> can reinitialize and run a job to completion");
        jp.initialize(new Job());
        while (jp.next()) {
        }

        // test abort
        System.out.println();
        System.out.println(">>> abort followed by next should fail");
        jp.initialize(new Job());
        for (int i = 0; i < 4; i++) {
            jp.next();
        }
        jp.abort();
        try {
            // should fail because the processor has not been reinitialized.
            jp.next();
            throw new Error();
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }

        System.out.println();
        System.out.println(">>> can reinitialize and run after an abort");
        jp.initialize(new Job());
        while (jp.next()) {
        }

        System.out.println();
        System.out.println(">>> reinitializing mid-job should fail");
        jp.initialize(new Job());
        for (int i = 0; i < 4; i++) {
            jp.next();
        }
        try {
            jp.initialize(new Job());
            throw new Error();
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
