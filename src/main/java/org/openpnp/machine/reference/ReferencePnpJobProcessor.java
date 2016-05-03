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
import java.util.Map.Entry;
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
import org.openpnp.util.Utils2D;
import org.simpleframework.xml.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Root
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

    protected Machine machine;

    protected Head head;

    protected List<JobPlacement> jobPlacements;

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
        // TODO STOPSHIP: This does the full cycle stuff. Do we want it?
        do {
            if (nextMessage == null) {
                nextMessage = Message.Next;
            }
            nextMessage = fsm.send(nextMessage);
        } while (fsm.getState() != State.Plan);
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
        // Create some shortcuts for things that won't change during the run
        this.machine = Configuration.get().getMachine();
        this.head = this.machine.getDefaultHead();
        this.jobPlacements = new ArrayList<>();

        for (BoardLocation boardLocation : job.getBoardLocations()) {
            // Only check enabled boards
            if (!boardLocation.isEnabled()) {
                continue;
            }
            for (Placement placement : boardLocation.getBoard().getPlacements()) {
                // Ignore placements that aren't set to be placed
                if (placement.getType() != Placement.Type.Place) {
                    continue;
                }

                // Ignore placements that aren't on the side of the board we're processing.
                if (placement.getSide() != boardLocation.getSide()) {
                    continue;
                }

                JobPlacement jobPlacement = new JobPlacement(boardLocation, placement);

                // Make sure the part is not null
                if (placement.getPart() == null) {
                    throw new Exception(String.format("Part not found for board %s, placement %s.",
                            boardLocation.getBoard().getName(), placement.getId()));
                }

                // Verify that the part height is greater than zero. Catches a common configuration
                // error.
                if (placement.getPart().getHeight().getValue() <= 0D) {
                    throw new Exception(String.format("Part height for %s must be greater than 0.",
                            placement.getPart().getId()));
                }

                // Make sure there is at least one compatible nozzle tip available
                findNozzleTip(placement.getPart());

                // Make sure there is at least one compatible and enabled feeder available
                findFeeder(placement.getPart());

                jobPlacements.add(jobPlacement);
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
            logger.info("Fiducial check for {}", boardLocation);
        }
        return null;
    }

    protected Message doPlan() throws Exception {
        plannedPlacements.clear();

        // Sort the placements by part height and filter out any that are already finished.
        List<JobPlacement> jobPlacements = this.jobPlacements.stream().filter((jobPlacement) -> {
            return !jobPlacement.finished;
        }).sorted(Comparator.comparing(JobPlacement::getPartHeight)).collect(Collectors.toList());

        if (jobPlacements.isEmpty()) {
            return Message.Complete;
        }

        // Collect only the placements that have the same height as the first one. This
        // guarantees that we never place a taller part before a shorter one.
        double firstHeight = jobPlacements.get(0).getPartHeight();
        jobPlacements = jobPlacements.stream().filter(jobPlacement -> {
            return jobPlacement.getPartHeight() == firstHeight;
        }).collect(Collectors.toList());

        // And determine the best order to process the placements in to utilize the nozzle
        // tips we currently have loaded.
        // TODO STOPSHIP: just faked for now
        for (Nozzle nozzle : head.getNozzles()) {
            if (jobPlacements.isEmpty()) {
                break;
            }
            plannedPlacements.put(nozzle, jobPlacements.remove(0));
        }
        return null;
    }

    protected Message doChangeNozzleTip() throws Exception {
        for (Entry<Nozzle, JobPlacement> entry : plannedPlacements.entrySet()) {
            Nozzle nozzle = entry.getKey();
            JobPlacement jobPlacement = entry.getValue();
            Placement placement = jobPlacement.placement;
            Part part = placement.getPart();

            // If the currently loaded NozzleTip can handle the Part we're good.
            if (nozzle.getNozzleTip().canHandle(part)) {
                continue;
            }
            // Otherwise find a compatible tip and load it
            NozzleTip nozzleTip = findNozzleTip(part);
            nozzle.unloadNozzleTip();
            nozzle.loadNozzleTip(nozzleTip);
        }
        return null;
    }

    protected Message doFeed() throws Exception {
        for (Entry<Nozzle, JobPlacement> entry : plannedPlacements.entrySet()) {
            Nozzle nozzle = entry.getKey();
            JobPlacement jobPlacement = entry.getValue();
            Placement placement = jobPlacement.placement;
            Part part = placement.getPart();

            while (true) {
                // Find a compatible, enabled feeder
                Feeder feeder = findFeeder(part);
                jobPlacement.feeder = feeder;

                // Feed the part
                try {
                    // Try to feed the part. If it fails, retry a couple times.
                    retry(3, () -> {
                        logger.info("Attempt Feed {} from {} with {}.",
                                new Object[] {part.getId(), feeder.getName(), nozzle.getName()});

                        feeder.feed(nozzle);

                        logger.info("Fed {} from {} with {}.",
                                new Object[] {part.getId(), feeder.getName(), nozzle.getName()});
                    });

                    break;
                }
                catch (Exception e) {
                    logger.info("Feed {} from {} with {} failed!",
                            new Object[] {part.getId(), feeder.getName(), nozzle.getName()});
                    // If the feed fails, disable the feeder and continue. If there are no
                    // more valid feeders the findFeeder() call above will throw and exit the
                    // loop.
                    feeder.setEnabled(false);
                }

            }
        }
        return null;
    }

    protected Message doPick() throws Exception {
        for (Entry<Nozzle, JobPlacement> entry : plannedPlacements.entrySet()) {
            Nozzle nozzle = entry.getKey();
            JobPlacement jobPlacement = entry.getValue();
            Placement placement = jobPlacement.placement;
            Part part = placement.getPart();

            // Get the feeder that was used to feed
            Feeder feeder = jobPlacement.feeder;

            // Move to the pick location
            MovableUtils.moveToLocationAtSafeZ(nozzle, feeder.getPickLocation());

            // Pick
            nozzle.pick(part);

            // Retract
            nozzle.moveToSafeZ();

            logger.info("Pick {} from {} with {}",
                    new Object[] {part.getId(), feeder.getName(), nozzle.getName()});
        }
        return null;
    }

    protected Message doAlign() throws Exception {
        for (Entry<Nozzle, JobPlacement> entry : plannedPlacements.entrySet()) {
            Nozzle nozzle = entry.getKey();
            JobPlacement jobPlacement = entry.getValue();
            Placement placement = jobPlacement.placement;
            Part part = placement.getPart();

            Location alignmentOffsets = machine.getPartAlignment().findOffsets(part, nozzle);
            jobPlacement.alignmentOffsets = alignmentOffsets;

            logger.info("Align {} with {}", part.getId(), nozzle.getName());
        }

        return null;
    }

    protected Message doPlace() throws Exception {
        for (Entry<Nozzle, JobPlacement> entry : plannedPlacements.entrySet()) {
            Nozzle nozzle = entry.getKey();
            JobPlacement jobPlacement = entry.getValue();
            BoardLocation boardLocation = jobPlacement.boardLocation;
            Placement placement = jobPlacement.placement;
            Part part = placement.getPart();

            Location placementLocation =
                    Utils2D.calculateBoardPlacementLocation(boardLocation, placement.getLocation());

            // If there are alignment offsets update the placement location with them
            if (jobPlacement.alignmentOffsets != null) {
                Location alignmentOffsets = jobPlacement.alignmentOffsets;
                // Rotate the point 0,0 using the alignment offsets as a center point by the angle
                // that is
                // the difference between the alignment angle and the calculated global
                // placement angle.
                Location location =
                        new Location(LengthUnit.Millimeters).rotateXyCenterPoint(alignmentOffsets,
                                placementLocation.getRotation() - alignmentOffsets.getRotation());

                // Set the angle to the difference mentioned above, aligning the part to the
                // same angle as
                // the placement.
                location = location.derive(null, null, null,
                        placementLocation.getRotation() - alignmentOffsets.getRotation());

                // Add the placement final location to move our local coordinate into global
                // space
                location = location.add(placementLocation);

                // Subtract the alignment offsets to move the part to the final location,
                // instead of
                // the nozzle.
                location = location.subtract(alignmentOffsets);

                placementLocation = location;
            }

            // Add the part's height to the placement location
            placementLocation = placementLocation.add(new Location(part.getHeight().getUnits(), 0,
                    0, part.getHeight().getValue(), 0));

            // Move to the placement location
            MovableUtils.moveToLocationAtSafeZ(nozzle, placementLocation);

            // Place the part
            nozzle.place();

            // Retract
            nozzle.moveToSafeZ();

            // Mark the placement as finished
            jobPlacement.finished = true;

            logger.info("Place {} with {}", part.getId(), nozzle.getName());
        }

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

    protected void retry(int maxTries, Retryable r) throws Exception {
        for (int i = 0; i < maxTries; i++) {
            try {
                r.action();
                break;
            }
            catch (Exception e) {
                if (i == maxTries - 1) {
                    throw e;
                }
            }
        }
    }

    public class JobPlacement {
        public final BoardLocation boardLocation;
        public final Placement placement;
        public Feeder feeder;
        public Location alignmentOffsets;
        public boolean finished;

        public JobPlacement(BoardLocation boardLocation, Placement placement) {
            this.boardLocation = boardLocation;
            this.placement = placement;
        }

        public double getPartHeight() {
            return placement.getPart().getHeight().convertToUnits(LengthUnit.Millimeters)
                    .getValue();
        }
    }

    public interface Retryable {
        void action() throws Exception;
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
