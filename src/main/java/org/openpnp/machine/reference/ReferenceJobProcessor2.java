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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openpnp.model.BoardLocation;
import org.openpnp.model.Job;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferenceJobProcessor2 {
    private static final Logger logger = LoggerFactory.getLogger(ReferenceJobProcessor2.class);

    enum State {
        Ready,
        Preflight,
        FiducialCheck,
        Plan,
        Feed,
        Pick,
        Align,
        Place,
        Complete
    }

    private static final Map<State, State[]> transitions = new HashMap<>();

    static {
        transitions.put(State.Ready, new State[] {State.Preflight});
        transitions.put(State.Preflight, new State[] {State.FiducialCheck, State.Ready});
        transitions.put(State.FiducialCheck, new State[] {State.Plan, State.Ready});
        transitions.put(State.Plan, new State[] {State.Feed, State.Complete, State.Ready});
        transitions.put(State.Feed, new State[] {State.Pick, State.Ready});
        transitions.put(State.Pick, new State[] {State.Align, State.Ready});
        transitions.put(State.Align, new State[] {State.Place, State.Ready});
        transitions.put(State.Place, new State[] {State.Plan, State.Ready});
        transitions.put(State.Complete, new State[] {State.Complete, State.Ready});
    }

    protected State state = State.Ready;

    protected Job job;

    protected List<JobPlacement> jobPlacements;

    protected Machine machine;

    protected Head head;

    protected Map<Nozzle, JobPlacement> plannedPlacements = new HashMap<>();

    public ReferenceJobProcessor2() {}

    // TODO: the base FSM stuff should be in a base class
    // TODO: just store the ops with the transitions
    public boolean next() throws Exception {
        Op op = null;
        switch (this.state) {
            case Ready:
                op = this::ready;
                break;
            case Preflight:
                op = this::preFlight;
                break;
            case FiducialCheck:
                op = this::fiducialCheck;
                break;
            case Plan:
                op = this::plan;
                break;
            case Feed:
                op = this::feed;
                break;
            case Pick:
                op = this::pick;
                break;
            case Align:
                op = this::align;
                break;
            case Place:
                op = this::place;
                break;
            case Complete:
                op = this::complete;
                break;
        }
        State state = op.op();
        if (state == null) {
            return false;
        }
        checkTransition(state);
        this.state = state;
        return true;
    }

    public boolean next(State state) throws Exception {
        checkTransition(state);
        this.state = state;
        return next();
    }

    protected void checkTransition(State state) throws Exception {
        State[] states = transitions.get(this.state);
        if (!Arrays.stream(states).anyMatch(state::equals)) {
            throw new Exception("Invalid state transition from " + this.state + " to " + state);
        }
    }

    protected State ready() throws Exception {
        System.out.println("ready()");
        return State.Preflight;
    }

    /**
     * Setup the JobProcessor's internal state. List and sort the Placements. Check the Job for any
     * issues.
     * 
     * @throws Exception
     */
    protected State preFlight() throws Exception {
        // this.machine = Configuration.get().getMachine();
        // this.head = this.machine.getDefaultHead();
        // this.jobPlacements = new ArrayList<>();
        // for (BoardLocation boardLocation : job.getBoardLocations()) {
        // for (Placement placement : boardLocation.getBoard().getPlacements()) {
        // JobPlacement jp = new JobPlacement(boardLocation, placement);
        //
        // if (placement.getPart() == null) {
        // throw new Exception(String.format("Part not found for board %s, placement %s",
        // boardLocation.getBoard().getName(), placement.getId()));
        // }
        //
        // findNozzleTip(placement.getPart());
        //
        // findFeeder(placement.getPart());
        //
        // // TODO: Optionally (based on config) validate part heights must be > 0
        //
        // jobPlacements.add(jp);
        // }
        // }
        System.out.println("preFlight()");
        return State.FiducialCheck;
    }

    protected State fiducialCheck() throws Exception {
        // FiducialLocator locator = Configuration.get().getMachine().getFiducialLocator();
        // for (BoardLocation boardLocation : job.getBoardLocations()) {
        // if (!boardLocation.isEnabled()) {
        // continue;
        // }
        // if (!boardLocation.isCheckFiducials()) {
        // continue;
        // }
        // Location location = locator.locateBoard(boardLocation);
        // boardLocation.setLocation(location);
        // }
        System.out.println("fiducialCheck()");
        return State.Plan;
    }

    static int i = 0;

    protected State plan() throws Exception {
        // plannedPlacements.clear();
        //
        // // Sort the placements by part height and get only the ones with
        // // the same height as the first.
        // List<JobPlacement> jobPlacements = this.jobPlacements.stream()
        // .sorted(Comparator.comparing(JobPlacement::getPartHeight))
        // .collect(Collectors.toList());
        //
        // if (jobPlacements.isEmpty()) {
        // // TODO: transition to complete somehow but include a return of control first.
        // }
        //
        // // Collect only the placements that have the same height as the first one. This
        // guarantees
        // // that we never place a taller part before a shorter one.
        // double firstHeight = jobPlacements.get(0).getPartHeight();
        // jobPlacements = jobPlacements.stream()
        // .filter(jobPlacement -> {
        // return jobPlacement.getPartHeight() == firstHeight;
        // })
        // .collect(Collectors.toList());
        //
        // // Get the list of nozzles we have available
        // List<Nozzle> nozzles = head.getNozzles();
        //
        // // And determine the best order to process the placements in to utilize the nozzle
        // // tips we currently have loaded.
        // // TODO: just faked for now
        // for (Nozzle nozzle : nozzles) {
        // if (jobPlacements.isEmpty()) {
        // break;
        // }
        // plannedPlacements.put(nozzle, jobPlacements.remove(0));
        // }

        System.out.println("plan()");
        i++;
        if (i == 5) {
            return State.Complete;
        }
        return State.Feed;
    }

    protected State feed() throws Exception {
        System.out.println("feed()");
        return State.Pick;
    }

    protected State pick() throws Exception {
        System.out.println("pick()");
        return State.Align;
    }

    protected State align() throws Exception {
        System.out.println("align()");
        return State.Place;
    }

    protected State place() throws Exception {
        System.out.println("place()");
        return State.Plan;
    }

    protected State complete() throws Exception {
        System.out.println("complete()");
        return null;
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

    public void setJob(Job job) {
        this.job = job;
    }

    public Job getJob() {
        return job;
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

    public interface Op {
        public State op() throws Exception;
    }

    public static void main(String[] args) throws Exception {
        ReferenceJobProcessor2 jp = new ReferenceJobProcessor2();
        while (jp.next());
    }
}
