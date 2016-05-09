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

import javax.swing.Action;
import javax.swing.Icon;

import org.openpnp.machine.reference.ReferencePnpJobProcessor.JobPlacement.Status;
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
import org.openpnp.spi.PropertySheetHolder;
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
        JobCheck,
        FiducialCheck,
        Plan,
        ChangeNozzleTip,
        Feed,
        Pick,
        Align,
        Place,
        Cleanup,
        Stopped
    }

    enum Message {
        Initialize,
        Next,
        Complete,
        Abort,
        Skip,
        Reset
    }

    private FiniteStateMachine<State, Message> fsm = new FiniteStateMachine<>(State.Uninitialized);

    protected Job job;

    protected Machine machine;

    protected Head head;

    protected List<JobPlacement> jobPlacements = new ArrayList<>();

    protected List<PlannedJobPlacement> plannedJobPlacements = new ArrayList<>();

    protected Map<BoardLocation, Location> boardLocationOverrides = new HashMap<>();

    /**
     * Each JobPlacement gets a "complete" boolean and "skip" boolean.
     * 
     * We have a resetComplete() function that resets all complete states.
     * 
     * We call resetComplete at the end of each successful step.
     * 
     * Each step loops through each JobPlacement, does the action and sets complete or throws an
     * Exception.
     * 
     * If a JP is already complete we skip it when looping through.
     * 
     * A Skip input sets skip for the current (top of list) JP and loops to the next one.
     *
     * We always do the placements in the same order, since they are in a list. It should be nozzle
     * order.
     * 
     * We return control after each full step, which means we pick all parts, return, align all
     * parts, return, etc.
     * 
     * The only time we ever call into a step twice is if the previous call had an error.
     * 
     * Once all errors are resolved (either via a fix or a skip) we continue to the next state.
     * 
     */
    // TODO STOPSHIP somehow we need to handle failures on preflight. what does the user do?
    public ReferencePnpJobProcessor() {
        fsm.add(State.Uninitialized, Message.Initialize, State.JobCheck, this::doInitialize);

        fsm.add(State.JobCheck, Message.Next, State.FiducialCheck, this::doJobCheck);
        fsm.add(State.JobCheck, Message.Abort, State.Cleanup);

        fsm.add(State.FiducialCheck, Message.Next, State.Plan, this::doFiducialCheck);
        fsm.add(State.FiducialCheck, Message.Abort, State.Cleanup);

        fsm.add(State.Plan, Message.Next, State.ChangeNozzleTip, this::doPlan);
        fsm.add(State.Plan, Message.Abort, State.Cleanup);
        fsm.add(State.Plan, Message.Complete, State.Cleanup);

        fsm.add(State.ChangeNozzleTip, Message.Next, State.Feed, this::doChangeNozzleTip);
        fsm.add(State.ChangeNozzleTip, Message.Skip, State.ChangeNozzleTip, this::doSkip);
        fsm.add(State.ChangeNozzleTip, Message.Abort, State.Cleanup);

        fsm.add(State.Feed, Message.Next, State.Pick, this::doFeed);
        fsm.add(State.Feed, Message.Skip, State.Feed, this::doSkip);
        fsm.add(State.Feed, Message.Abort, State.Cleanup);

        // TODO STOPSHIP make sure Skips are doing discard where needed
        fsm.add(State.Pick, Message.Next, State.Align, this::doPick);
        fsm.add(State.Pick, Message.Skip, State.Pick, this::doSkip);
        fsm.add(State.Pick, Message.Abort, State.Cleanup);

        fsm.add(State.Align, Message.Next, State.Place, this::doAlign);
        fsm.add(State.Align, Message.Skip, State.Align, this::doSkip);
        fsm.add(State.Align, Message.Abort, State.Cleanup);

        fsm.add(State.Place, Message.Next, State.Plan, this::doPlace);
        fsm.add(State.Place, Message.Skip, State.Place, this::doSkip);
        fsm.add(State.Place, Message.Abort, State.Cleanup);

        fsm.add(State.Cleanup, Message.Next, State.Stopped, this::doCleanup);

        fsm.add(State.Stopped, Message.Reset, State.Uninitialized, this::doReset);
    }

    public synchronized void initialize(Job job) throws Exception {
        this.job = job;
        fsm.send(Message.Initialize);
    }

    public synchronized boolean next() throws Exception {
        fsm.send(Message.Next);

        if (fsm.getState() == State.Stopped) {
            /*
             * If we've reached the Stopped state the process is complete. We reset the FSM and
             * return false to indicate that we're finished.
             */
            fsm.send(Message.Reset);
            return false;
        }
        else if (fsm.getState() == State.Plan && isJobComplete()) {
            /*
             * If we've reached the Plan state and there are no more placements to work on the job
             * is complete. We send the Complete Message to start the cleanup process and loop until
             * it's done.
             */
            fsm.send(Message.Complete);
            while (next());
            return false;
        }

        return true;
    }

    public synchronized void abort() throws Exception {
        fsm.send(Message.Abort);

        // Run to completion, handles cleanup.
        while (next());
    }

    public synchronized void skip() throws Exception {
        fsm.send(Message.Skip);
    }

    protected void doInitialize() throws Exception {
        if (job == null) {
            throw new Exception("Can't initialize with a null Job.");
        }
    }

    protected void doJobCheck() throws Exception {
        // Create some shortcuts for things that won't change during the run
        this.machine = Configuration.get().getMachine();
        this.head = this.machine.getDefaultHead();
        this.jobPlacements.clear();
        this.boardLocationOverrides.clear();

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

        // Everything looks good, so prepare the machine.

        // Safe Z the machine
        head.moveToSafeZ();
        // Discard any currently picked parts
        for (Nozzle nozzle : head.getNozzles()) {
            if (nozzle.getPart() != null) {
                discard(nozzle);
            }
        }
    }

    protected void doFiducialCheck() throws Exception {
        FiducialLocator locator = Configuration.get().getMachine().getFiducialLocator();
        for (BoardLocation boardLocation : job.getBoardLocations()) {
            if (!boardLocation.isEnabled()) {
                continue;
            }
            if (!boardLocation.isCheckFiducials()) {
                continue;
            }
            Location location = locator.locateBoard(boardLocation);
            boardLocationOverrides.put(boardLocation, location);
            logger.info("Fiducial check for {}", boardLocation);
        }
    }

    protected void doPlan() throws Exception {
        plannedJobPlacements.clear();

        // Get the list of unfinished placements and sort them by part height.
        List<JobPlacement> jobPlacements = getPendingJobPlacements().stream()
                .sorted(Comparator.comparing(JobPlacement::getPartHeight))
                .collect(Collectors.toList());

        if (jobPlacements.isEmpty()) {
            return;
        }

        // Collect only the placements that have the same height as the first one. This
        // guarantees that we never place a taller part before a shorter one.
        // TODO STOPSHIP: Is it okay to place the last part of one height and the
        // first part of the next height in the same pass? If not, why?
        // Or more generally, is it okay to fill all the nozzles with the next
        // sets of parts as long as we never place a taller part before finishing all
        // the shorter parts?
        double firstHeight = jobPlacements.get(0).getPartHeight();
        jobPlacements = jobPlacements.stream().filter(jobPlacement -> {
            return jobPlacement.getPartHeight() == firstHeight;
        }).collect(Collectors.toList());
        
        // And determine the best order to process the placements in to utilize the nozzle
        // tips we currently have loaded.
        // TODO STOPSHIP: Just take the first ones for now. Implement the above later.
        for (Nozzle nozzle : head.getNozzles()) {
            if (jobPlacements.isEmpty()) {
                break;
            }
            JobPlacement jp = jobPlacements.remove(0);
            jp.status = Status.Processing;
            plannedJobPlacements.add(new PlannedJobPlacement(nozzle, jp));
        }

        logger.debug("Planned {} placements", plannedJobPlacements.size());
    }

    protected void doChangeNozzleTip() throws Exception {
        for (PlannedJobPlacement p : plannedJobPlacements) {
            if (p.stepComplete) {
                continue;
            }

            Nozzle nozzle = p.nozzle;
            JobPlacement jobPlacement = p.jobPlacement;
            Placement placement = jobPlacement.placement;
            Part part = placement.getPart();

            // If the currently loaded NozzleTip can handle the Part we're good.
            if (nozzle.getNozzleTip().canHandle(part)) {
                logger.debug("No nozzle change needed for nozzle {}", nozzle.getName());
                continue;
            }
            // Otherwise find a compatible tip and load it
            NozzleTip nozzleTip = findNozzleTip(part);
            nozzle.unloadNozzleTip();
            nozzle.loadNozzleTip(nozzleTip);

            // Mark this step as complete
            p.stepComplete = true;
        }
        clearStepComplete();
    }

    protected void doFeed() throws Exception {
        for (PlannedJobPlacement p : plannedJobPlacements) {
            if (p.stepComplete) {
                continue;
            }
            Nozzle nozzle = p.nozzle;
            JobPlacement jobPlacement = p.jobPlacement;
            Placement placement = jobPlacement.placement;
            Part part = placement.getPart();

            while (true) {
                // Find a compatible, enabled feeder
                Feeder feeder = findFeeder(part);
                jobPlacement.feeder = feeder;

                // Feed the part
                try {
                    // Try to feed the part. If it fails, retry the specified number of times before
                    // giving up.
                    retry(1 + feeder.getRetryCount(), () -> {
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
                    // TODO STOPSHIP: Instead of disabling the feeder, which can be confusing for
                    // users, create an internal list of feeders that have failed and check it
                    // when deciding which to use next. We also probably need a status flag on
                    // Feeder indicating that it has failed so that the user knows where to look
                    // to try to fix problems. "last feed failed"
                    feeder.setEnabled(false);
                }
            }
            p.stepComplete = true;
        }
        clearStepComplete();
    }

    protected void doPick() throws Exception {
        for (PlannedJobPlacement p : plannedJobPlacements) {
            if (p.stepComplete) {
                continue;
            }
            Nozzle nozzle = p.nozzle;
            JobPlacement jobPlacement = p.jobPlacement;
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

            p.stepComplete = true;
        }

        clearStepComplete();
    }

    protected void doAlign() throws Exception {
        for (PlannedJobPlacement p : plannedJobPlacements) {
            if (p.stepComplete) {
                continue;
            }
            Nozzle nozzle = p.nozzle;
            JobPlacement jobPlacement = p.jobPlacement;
            Placement placement = jobPlacement.placement;
            Part part = placement.getPart();

            Location alignmentOffsets = machine.getPartAlignment().findOffsets(part, nozzle);
            jobPlacement.alignmentOffsets = alignmentOffsets;

            logger.info("Align {} with {}", part.getId(), nozzle.getName());

            p.stepComplete = true;
        }

        clearStepComplete();
    }

    protected void doPlace() throws Exception {
        for (PlannedJobPlacement p : plannedJobPlacements) {
            if (p.stepComplete) {
                continue;
            }
            Nozzle nozzle = p.nozzle;
            JobPlacement jobPlacement = p.jobPlacement;
            Placement placement = jobPlacement.placement;
            Part part = placement.getPart();
            BoardLocation boardLocation = p.jobPlacement.boardLocation;

            // TODO STOPSHIP: this handles the fid override, but it's ugly and I don't like it.
            if (boardLocationOverrides.containsKey(boardLocation)) {
                BoardLocation boardLocation2 = new BoardLocation(boardLocation.getBoard());
                boardLocation2.setSide(boardLocation.getSide());
                boardLocation2.setLocation(boardLocationOverrides.get(boardLocation));
                boardLocation = boardLocation2;
            }
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
            jobPlacement.status = Status.Complete;
            
            p.stepComplete = true;

            logger.info("Place {} with {}", part.getId(), nozzle.getName());
        }
        clearStepComplete();
    }

    protected void doCleanup() throws Exception {
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
    }

    protected void doReset() throws Exception {
        this.job = null;
    }

    /**
     * Discard the picked part, if any. Remove the currently processing PlannedJobPlacement from
     * the list and mark the JobPlacement as Skipped.
     * @throws Exception
     */
    protected void doSkip() throws Exception {
        if (plannedJobPlacements.size() > 0) {
            PlannedJobPlacement plannedJobPlacement = plannedJobPlacements.remove(0);
            JobPlacement jobPlacement = plannedJobPlacement.jobPlacement;
            Nozzle nozzle = plannedJobPlacement.nozzle;
            discard(nozzle);
            jobPlacement.status = Status.Skipped;
            logger.debug("Skipped {}", jobPlacement.placement);
        }
    }

    /**
     * Discard the Part, if any, on the given Nozzle. the Nozzle is returned to Safe Z at the end of
     * the operation.
     * 
     * @param nozzle
     * @throws Exception
     */
    protected void discard(Nozzle nozzle) throws Exception {
        if (nozzle.getPart() == null) {
            return;
        }
        // move to the discard location
        MovableUtils.moveToLocationAtSafeZ(nozzle,
                Configuration.get().getMachine().getDiscardLocation());
        // discard the part
        nozzle.place();
        nozzle.moveToSafeZ();
    }

    /**
     * Find the first NozzleTip that is able to handle the given Part.
     * 
     * @param part
     * @return
     * @throws Exception If no compatible NozzleTip can be found.
     */
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

    /**
     * Find the first enabled Feeder is that is able to feed the given Part.
     * 
     * @param part
     * @return
     * @throws Exception If no Feeder is found that is both enabled and is serving the Part.
     */
    protected Feeder findFeeder(Part part) throws Exception {
        for (Feeder feeder : machine.getFeeders()) {
            if (feeder.getPart() == part && feeder.isEnabled()) {
                return feeder;
            }
        }
        throw new Exception("No compatible, enabled feeder found for part " + part.getId());
    }

    /**
     * Call the Retryable's action method until it either does not throw an Exception or it is
     * called maxTries number of times. If the method throws an Exception each time then this method
     * will throw the final Exception.
     * 
     * @param maxTries
     * @param r
     * @throws Exception
     */
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
    
    protected void clearStepComplete() {
        for (PlannedJobPlacement p : plannedJobPlacements) {
            p.stepComplete = false;
        }
    }

    protected List<JobPlacement> getPendingJobPlacements() {
        return this.jobPlacements.stream().filter((jobPlacement) -> {
            return jobPlacement.status == Status.Pending;
        }).collect(Collectors.toList());
    }

    protected boolean isJobComplete() {
        return getPendingJobPlacements().isEmpty();
    }
    
    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return null;
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return null;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return null;
    }

    @Override
    public Icon getPropertySheetHolderIcon() {
        return null;
    }

    public static class JobPlacement {
        public enum Status {
            Pending,
            Processing,
            Skipped,
            Complete
        }
        
        public final BoardLocation boardLocation;
        public final Placement placement;
        public Feeder feeder;
        public Location alignmentOffsets;
        public Status status = Status.Pending;

        public JobPlacement(BoardLocation boardLocation, Placement placement) {
            this.boardLocation = boardLocation;
            this.placement = placement;
        }

        public double getPartHeight() {
            return placement.getPart().getHeight().convertToUnits(LengthUnit.Millimeters)
                    .getValue();
        }
    }

    public static class PlannedJobPlacement {
        public final Nozzle nozzle;
        public final JobPlacement jobPlacement;
        public boolean stepComplete;

        public PlannedJobPlacement(Nozzle nozzle, JobPlacement jobPlacement) {
            this.nozzle = nozzle;
            this.jobPlacement = jobPlacement;
        }
    }

    public interface Retryable {
        void action() throws Exception;
    }
}
