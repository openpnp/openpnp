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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.wizards.ReferencePnpJobProcessorConfigurationWizard;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Job;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Panel;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.FiducialLocator;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.spi.PartAlignment;
import org.openpnp.spi.PnpJobProcessor.JobPlacement.Status;
import org.openpnp.spi.base.AbstractJobProcessor;
import org.openpnp.spi.base.AbstractPnpJobProcessor;
import org.openpnp.util.Collect;
import org.openpnp.util.FiniteStateMachine;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.Utils2D;
import org.openpnp.util.VisionUtils;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@Root
public class ReferencePnpJobProcessor extends AbstractPnpJobProcessor {
    enum State {
        Uninitialized,
        PreFlight,
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

    public static class PlannedPlacement {
        public final JobPlacement jobPlacement;
        public final Nozzle nozzle;
        public Feeder feeder;
        public PartAlignment.PartAlignmentOffset alignmentOffsets;
        public boolean fed;
        public boolean stepComplete;

        public PlannedPlacement(Nozzle nozzle, JobPlacement jobPlacement) {
            this.nozzle = nozzle;
            this.jobPlacement = jobPlacement;
        }

        @Override
        public String toString() {
            return nozzle + " -> " + jobPlacement.toString();
        }
    }



    @Attribute(required = false)
    protected boolean parkWhenComplete = false;

    private FiniteStateMachine<State, Message> fsm = new FiniteStateMachine<>(State.Uninitialized);

    protected Job job;

    protected Machine machine;

    protected Head head;

    protected List<JobPlacement> jobPlacements = new ArrayList<>();

    protected List<PlannedPlacement> plannedPlacements = new ArrayList<>();

    protected Map<BoardLocation, Location> boardLocationFiducialOverrides = new HashMap<>();
    
    long startTime;
    int totalPartsPlaced;

    public ReferencePnpJobProcessor() {
        fsm.add(State.Uninitialized, Message.Initialize, State.PreFlight, this::doInitialize);

        fsm.add(State.PreFlight, Message.Next, State.FiducialCheck, this::doPreFlight,
                Message.Next);
        fsm.add(State.PreFlight, Message.Abort, State.Cleanup, Message.Next);

        fsm.add(State.FiducialCheck, Message.Next, State.Plan, this::doFiducialCheck, Message.Next);
        fsm.add(State.FiducialCheck, Message.Skip, State.Plan, Message.Next);
        fsm.add(State.FiducialCheck, Message.Abort, State.Cleanup, Message.Next);

        fsm.add(State.Plan, Message.Next, State.ChangeNozzleTip, this::doPlan, Message.Next);
        fsm.add(State.Plan, Message.Abort, State.Cleanup, Message.Next);
        fsm.add(State.Plan, Message.Complete, State.Cleanup, Message.Next);

        fsm.add(State.ChangeNozzleTip, Message.Next, State.Feed, this::doChangeNozzleTip,
                Message.Next);
        fsm.add(State.ChangeNozzleTip, Message.Skip, State.ChangeNozzleTip, this::doSkip,
                Message.Next);
        fsm.add(State.ChangeNozzleTip, Message.Abort, State.Cleanup, Message.Next);

        fsm.add(State.Feed, Message.Next, State.Align, this::doFeedAndPick, Message.Next);
        fsm.add(State.Feed, Message.Skip, State.Feed, this::doSkip, Message.Next);
        fsm.add(State.Feed, Message.Abort, State.Cleanup, Message.Next);

        // TODO: See notes on doFeedAndPick()
        // fsm.add(State.Feed, Message.Next, State.Pick, this::doFeed, Message.Next);
        // fsm.add(State.Feed, Message.Skip, State.Feed, this::doSkip, Message.Next);
        // fsm.add(State.Feed, Message.Abort, State.Cleanup, Message.Next);
        //
        // fsm.add(State.Pick, Message.Next, State.Align, this::doPick, Message.Next);
        // fsm.add(State.Pick, Message.Skip, State.Pick, this::doSkip, Message.Next);
        // fsm.add(State.Pick, Message.Abort, State.Cleanup, Message.Next);

        fsm.add(State.Align, Message.Next, State.Place, this::doAlign, Message.Next);
        fsm.add(State.Align, Message.Skip, State.Align, this::doSkip, Message.Next);
        fsm.add(State.Align, Message.Abort, State.Cleanup, Message.Next);

        fsm.add(State.Place, Message.Next, State.Plan, this::doPlace);
        fsm.add(State.Place, Message.Skip, State.Place, this::doSkip, Message.Next);
        fsm.add(State.Place, Message.Abort, State.Cleanup, Message.Next);

        fsm.add(State.Cleanup, Message.Next, State.Stopped, this::doCleanup, Message.Reset);

        fsm.add(State.Stopped, Message.Reset, State.Uninitialized, this::doReset);
    }

    public synchronized void initialize(Job job) throws Exception {
        this.job = job;
        fsm.send(Message.Initialize);
    }

    public synchronized boolean next() throws Exception {

        try{
            fsm.send(Message.Next);
        } catch (Exception e) {
            this.fireJobState(this.machine.getSignalers(), AbstractJobProcessor.State.ERROR);
            throw(e);
        }


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
             * is complete. We send the Complete Message to start the cleanup process.
             */
            fsm.send(Message.Complete);
            this.fireJobState(this.machine.getSignalers(), AbstractJobProcessor.State.FINISHED);
            return false;
        }

        return true;
    }

    public synchronized void abort() throws Exception {
        fsm.send(Message.Abort);
    }

    public synchronized void skip() throws Exception {
        fsm.send(Message.Skip);
    }

    /*
     * TODO Due to the Align Skip issue I think we'd be better off replacing this API with
     * something like List<Message> getOptions(). This would return a list of options that the
     * caller can take at a given step. Need to figure out a way to make this generic enough
     * that other JP implementations can use it, thus it's probably not appropriate to just
     * use Message, but instead maybe a PnpJobProcessor specific enum.
     * Options would be things like:
     *      * Skip Placement
     *      * Try Later
     *      * Retry Action
     *      * Continue (Next)
     *      
     * Really just need to think about the way a user will want to respond to various error
     * conditions that arise in each step and see if these can be generalized in a meaningful
     * way.
     */
    public boolean canSkip() {
        return fsm.canSend(Message.Skip);
    }

    /**
     * Validate that there is a job set before allowing it to start.
     * 
     * @throws Exception
     */
    protected void doInitialize() throws Exception {
        if (job == null) {
            throw new Exception("Can't initialize with a null Job.");
        }
    }

    /**
     * Create some internal shortcuts to various buried objects.
     * 
     * Check for obvious setup errors in the job: Feeders are available and enabled, Placements all
     * have valid parts, Parts all have height values set, Each part has at least one compatible
     * nozzle tip.
     * 
     * Populate the jobPlacements list with all the placements that we'll perform for the entire
     * job.
     * 
     * Safe-Z the machine, discard any currently picked parts.
     * 
     * @throws Exception
     */
    protected void doPreFlight() throws Exception {
        startTime = System.currentTimeMillis();
        totalPartsPlaced = 0;
        
        // Create some shortcuts for things that won't change during the run
        this.machine = Configuration.get().getMachine();
        this.head = this.machine.getDefaultHead();
        this.jobPlacements.clear();
        this.boardLocationFiducialOverrides.clear();

        fireTextStatus("Checking job for setup errors.");

        for (BoardLocation boardLocation : job.getBoardLocations()) {
            // Only check enabled boards
            if (!boardLocation.isEnabled()) {
                continue;
            }
            
            // Check for ID duplicates - throw error if any are found
            HashSet<String> idlist = new HashSet<String>();
            for (Placement placement : boardLocation.getBoard().getPlacements()) {
            	if (idlist.contains(placement.getId())) {
            		throw new Exception(String.format("This board contains at least one duplicate ID entry: %s ",
            				placement.getId()));
            	} else {
            		idlist.add(placement.getId());
            	}
            }		
            
            for (Placement placement : boardLocation.getBoard().getPlacements()) {
                // Ignore placements that aren't set to be placed
                if (placement.getType() != Placement.Type.Place) {
                    continue;
                }
                
                // Ignore placements that are placed already
                if (boardLocation.getPlaced(placement.getId())) {
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
                findNozzleTip(head, placement.getPart());

                // Make sure there is at least one compatible and enabled feeder available
                findFeeder(machine, placement.getPart());

                jobPlacements.add(jobPlacement);
            }
        }

        // Everything looks good, so prepare the machine.
        fireTextStatus("Preparing machine.");

        // Safe Z the machine
        head.moveToSafeZ();
        // Discard any currently picked parts
        discardAll(head);
        
        HashMap<String, Object> params = new HashMap<>();
        params.put("job", job);
        params.put("jobProcessor", this);
        Configuration.get().getScripting().on("Job.Starting", params);
    }

    protected void doFiducialCheck() throws Exception {
        fireTextStatus("Performing fiducial checks.");

        FiducialLocator locator = Configuration.get().getMachine().getFiducialLocator();
        
        if (job.isUsingPanel() && job.getPanels().get(0).isCheckFiducials()){
        	Panel p = job.getPanels().get(0);
        	
        	BoardLocation boardLocation = job.getBoardLocations().get(0);
        	
        	Location location = locator.locateBoard(boardLocation, p.isCheckFiducials());
        	boardLocationFiducialOverrides.put(boardLocation, location);
        	Logger.debug("Panel Fiducial check for {}", boardLocation);
        }
        
        for (BoardLocation boardLocation : job.getBoardLocations()) {
            if (!boardLocation.isEnabled()) {
                continue;
            }
            if (!boardLocation.isCheckFiducials()) {
                continue;
            }
            Location location = locator.locateBoard(boardLocation);
            boardLocationFiducialOverrides.put(boardLocation, location);
            Logger.debug("Fiducial check for {}", boardLocation);
        }
    }
    
    protected void doIndividualFiducialCheck(BoardLocation boardLocation) throws Exception {
        fireTextStatus("Performing individual fiducial check.");

        FiducialLocator locator = Configuration.get().getMachine().getFiducialLocator();
        
        Location location = locator.locateBoard(boardLocation);
        boardLocationFiducialOverrides.put(boardLocation, location);
        Logger.debug("Fiducial check for {}", boardLocation);
    }

    /**
     * Description of the planner:
     * 
     * 1. Create a List<List<JobPlacement>> where each List<JobPlacement> is a List of JobPlacements
     * that the corresponding (in order) Nozzle can handle in Nozzle order.
     * 
     * In addition, each List<JobPlacement> contains one instance of null which represents a
     * solution where that Nozzle does not perform a placement.
     * 
     * 2. Create the Cartesian product of all of the List<JobPlacement>. The resulting List<List
     * <JobPlacement>> represents possible solutions for a single cycle with each JobPlacement
     * corresponding to a Nozzle.
     * 
     * 3. Filter out any solutions where the same JobPlacement is represented more than once. We
     * don't want more than one Nozzle trying to place the same Placement.
     * 
     * 4. Sort the solutions by fewest nulls followed by fewest nozzle changes. The result is that
     * we prefer solutions that use more nozzles in a cycle and require fewer nozzle changes.
     * 
     * Note: TODO: Originally planned to have this sort by part height but that went out the window
     * during development. Need to think about how to best combine the height requirement with the
     * want to fill all nozzles and perform minimal nozzle changes. Based on IRC discussion, the
     * part height thing might be a red herring - most machines will have enough Z to place all
     * parts regardless of height order.
     */
    protected void doPlan() throws Exception {
        plannedPlacements.clear();

        fireTextStatus("Planning placements.");

        // Get the list of unfinished placements and sort them by part height.
        List<JobPlacement> jobPlacements = getPendingJobPlacements().stream()
                .sorted(Comparator.comparing(JobPlacement::getPartHeight))
                .collect(Collectors.toList());

        if (jobPlacements.isEmpty()) {
            return;
        }

        // Create a List of Lists of JobPlacements that each Nozzle can handle, including
        // one instance of null per Nozzle. The null indicates a possible "no solution"
        // for that Nozzle.
        List<List<JobPlacement>> solutions = head.getNozzles().stream().map(nozzle -> {
            return Stream.concat(jobPlacements.stream().filter(jobPlacement -> {
                return nozzleCanHandle(nozzle, jobPlacement.placement.getPart());
            }), Stream.of((JobPlacement) null)).collect(Collectors.toList());
        }).collect(Collectors.toList());

        // Get the cartesian product of those Lists
        List<JobPlacement> result = Collect.cartesianProduct(solutions).stream()
                // Filter out any results that contains the same JobPlacement more than once
                .filter(list -> {
                    // Note: A previous version of this code just dumped everything into a
                    // set and compared the size. This worked for two nozzles since there would
                    // never be more than two nulls, but for > 2 nozzles there will always be a
                    // solution that has > 2 nulls, which means the size will never match.
                    // This version of the code ignores the nulls (since they are valid
                    // solutions) and instead only checks for duplicate valid JobPlacements.
                    // There is probably a more clever way to do this, but it isn't coming
                    // to me at the moment.
                    HashSet<JobPlacement> set = new HashSet<>();
                    for (JobPlacement jp : list) {
                        if (jp == null) {
                            continue;
                        }
                        if (set.contains(jp)) {
                            return false;
                        }
                        set.add(jp);
                    }
                    return true;
                })
                // Sort by the solutions that contain the fewest nulls followed by the
                // solutions that require the fewest nozzle changes.
                .sorted(byFewestNulls.thenComparing(byFewestNozzleChanges))
                // And return the top result.
                .findFirst().orElse(null);

        // Now we have a solution, so apply it to the nozzles and plan the placements.
        for (Nozzle nozzle : head.getNozzles()) {
            // The solution is in Nozzle order, so grab the next one.
            JobPlacement jobPlacement = result.remove(0);
            if (jobPlacement == null) {
                continue;
            }
            jobPlacement.status = Status.Processing;
            plannedPlacements.add(new PlannedPlacement(nozzle, jobPlacement));
        }

        Logger.debug("Planned placements {}", plannedPlacements);
    }

    protected void doChangeNozzleTip() throws Exception {
        for (PlannedPlacement plannedPlacement : plannedPlacements) {
            if (plannedPlacement.stepComplete) {
                continue;
            }

            Nozzle nozzle = plannedPlacement.nozzle;
            JobPlacement jobPlacement = plannedPlacement.jobPlacement;
            Placement placement = jobPlacement.placement;
            Part part = placement.getPart();

            // If the currently loaded NozzleTip can handle the Part we're good.
            if (nozzle.getNozzleTip() != null && nozzle.getNozzleTip().canHandle(part)) {
                Logger.debug("No nozzle change needed for nozzle {}", nozzle);
                plannedPlacement.stepComplete = true;
                continue;
            }

            fireTextStatus("Changing nozzle tip on nozzle %s.", nozzle.getId());

            // Otherwise find a compatible tip and load it
            NozzleTip nozzleTip = findNozzleTip(nozzle, part);
            Logger.debug("Change nozzle tip on {} from {} to {}",
                    new Object[] {nozzle, nozzle.getNozzleTip(), nozzleTip});
            nozzle.unloadNozzleTip();
            nozzle.loadNozzleTip(nozzleTip);

            // Mark this step as complete
            plannedPlacement.stepComplete = true;
        }

        clearStepComplete();
    }

    /*
     * TODO: This method is a compromise due to time constraints. Below, there is doFeed and doPick,
     * which were intended to be used in sequence. I realized too late that I had made an error in
     * designing the FSM and for multiple nozzles it was doing feed, feed, pick, pick instead of
     * feed, pick, feed, pick. The latter is correct while the former is useless. Since I need to
     * release this feature before Maker Faire I've decided to just combine the methods to get this
     * done.
     * 
     * The whole FSM system needs to be reconsidered. There are two main things to consider: 1.
     * current FSM cannot handle transitions within action methods. If it could then we could have
     * doFeed process one PlannedPlacement, continue to Pick and then have Pick either loop back to
     * Feed if there are more PlannedPlacements or continue to Align if not. I don't love this idea
     * because it makes the FSM non-deterministic and thus harder to reason about.
     * 
     * 2. An ideal system would treat each step that required actions for multiple PlannedPlacements
     * as their own FSM, producing a hierarchy of FSMs. I've also seen this idea referred to as
     * "fork and join" FSMs and I have brainstormed this type of system a bit in the image at:
     * https://imgur.com/a/63Y1t
     */
    protected void doFeedAndPick() throws Exception {
        for (PlannedPlacement plannedPlacement : plannedPlacements) {
            if (plannedPlacement.stepComplete) {
                continue;
            }
            Nozzle nozzle = plannedPlacement.nozzle;
            JobPlacement jobPlacement = plannedPlacement.jobPlacement;
            Placement placement = jobPlacement.placement;
            Part part = placement.getPart();

            if (!plannedPlacement.fed) {
                Exception lastError = null;
                Feeder lastErrorFeeder = null;
                while (true) {
                    // Find a compatible, enabled feeder
                    Feeder feeder;
                    try {
                        feeder = findFeeder(machine, part);
                    }
                    catch (Exception e) {
                        if (lastError != null) {
                            throw new Exception(String.format("Unable to feed %s. Feeder %s: %s.", 
                                    part.getId(), 
                                    lastErrorFeeder.getName(), 
                                    lastError.getMessage()), 
                                lastError);
                        }
                        else {
                            throw new Exception(String.format("Unable to feed %s. No enabled feeder found.", part.getId()));
                        }
                    }
                    plannedPlacement.feeder = feeder;

                    // Feed the part
                    try {
                        // Try to feed the part. If it fails, retry the specified number of times
                        // before
                        // giving up.
                        retry(1 + feeder.getRetryCount(), () -> {
                            fireTextStatus("Feeding %s from %s for %s.", part.getId(),
                                    feeder.getName(), placement.getId());
                            Logger.debug("Attempt Feed {} from {} with {}.",
                                    new Object[] {part, feeder, nozzle});

                            feeder.feed(nozzle);

                            Logger.debug("Fed {} from {} with {}.",
                                    new Object[] {part, feeder, nozzle});
                        });

                        break;
                    }
                    catch (Exception e) {
                        Logger.debug("Feed {} from {} with {} failed!",
                                new Object[] {part, feeder, nozzle});
                        // If the feed fails, disable the feeder and continue. If there are no
                        // more valid feeders the findFeeder() call above will throw and exit the
                        // loop.
                        feeder.setEnabled(false);
                        lastErrorFeeder = feeder;
                        lastError = e;
                    }
                }
                plannedPlacement.fed = true;
            }

            // Get the feeder that was used to feed
            Feeder feeder = plannedPlacement.feeder;

            // Move to the pick location
            MovableUtils.moveToLocationAtSafeZ(nozzle, feeder.getPickLocation());

            fireTextStatus("Picking %s from %s for %s.", part.getId(), feeder.getName(),
                    placement.getId());
            
            ++totalPartsPlaced;

            // Pick
            nozzle.pick(part);

            // Retract
            nozzle.moveToSafeZ();

            Logger.debug("Pick {} from {} with {}", part, feeder, nozzle);

            if (feeder != null) {
                feeder.postPick(nozzle);
            }

            plannedPlacement.stepComplete = true;
        }

        clearStepComplete();
    }

    protected void doAlign() throws Exception {
        for (PlannedPlacement plannedPlacement : plannedPlacements) {
            if (plannedPlacement.stepComplete) {
                continue;
            }
            Nozzle nozzle = plannedPlacement.nozzle;
            JobPlacement jobPlacement = plannedPlacement.jobPlacement;
            Placement placement = jobPlacement.placement;
            Part part = placement.getPart();
            fireTextStatus("Aligning %s for %s.", part.getId(), placement.getId());

            PartAlignment partAlignment = findPartAligner(machine, part);

            // Check if there is a fiducial override for the board location and if so, use it.
            BoardLocation boardLocation = getFiducialCompensatedBoardLocation(jobPlacement.boardLocation);
            
            if(partAlignment!=null) {
                plannedPlacement.alignmentOffsets = VisionUtils.findPartAlignmentOffsets(
                        partAlignment,
                        part,
                        boardLocation,
                        placement.getLocation(), nozzle);
                Logger.debug("Align {} with {}", part, nozzle);
            }
            else
            {
                plannedPlacement.alignmentOffsets=null;
                Logger.debug("Not aligning {} as no compatible enabled aligners defined",part);
            }

            plannedPlacement.stepComplete = true;
        }

        clearStepComplete();
    }

    protected void doPlace() throws Exception {
        for (PlannedPlacement plannedPlacement : plannedPlacements) {
            if (plannedPlacement.stepComplete) {
                continue;
            }
            Nozzle nozzle = plannedPlacement.nozzle;
            JobPlacement jobPlacement = plannedPlacement.jobPlacement;
            Placement placement = jobPlacement.placement;
            Part part = placement.getPart();
            BoardLocation boardLocation = plannedPlacement.jobPlacement.boardLocation;
            //Check if the individual piece has a fiducial check and check to see if the board is enabled
            if(jobPlacement.placement.getCheckFids()&&jobPlacement.boardLocation.isEnabled()) {
                doIndividualFiducialCheck(jobPlacement.boardLocation);
            }

            // Check if there is a fiducial override for the board location and if so, use it.
            boardLocation = getFiducialCompensatedBoardLocation(boardLocation);

            Location placementLocation =
                    Utils2D.calculateBoardPlacementLocation(boardLocation, placement.getLocation());

            // If there are alignment offsets update the placement location with them
            if (plannedPlacement.alignmentOffsets != null) {

                /*
                        preRotated means during alignment we have already rotated the component
                        - this is useful for say an external rotating stage that the component is
                        placed on, rotated to correct placement angle, and then picked up again.
                 */
                if (plannedPlacement.alignmentOffsets.getPreRotated()) {
                    placementLocation = placementLocation.subtractWithRotation(
                            plannedPlacement.alignmentOffsets.getLocation());
                }
                else {
                    Location alignmentOffsets = plannedPlacement.alignmentOffsets.getLocation();
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
            }

            // Add the part's height to the placement location
            placementLocation = placementLocation.add(new Location(part.getHeight().getUnits(), 0,
                    0, part.getHeight().getValue(), 0));

            // Move to the placement location
            MovableUtils.moveToLocationAtSafeZ(nozzle, placementLocation);

            fireTextStatus("Placing %s for %s.", part.getId(), placement.getId());

            // Place the part
            nozzle.place();

            // Retract
            nozzle.moveToSafeZ();

            // Mark the placement as finished
            jobPlacement.status = Status.Complete;
            
            // Mark the placement as "placed"
            boardLocation.setPlaced(jobPlacement.placement.getId(), true);

            plannedPlacement.stepComplete = true;

            HashMap<String, Object> params = new HashMap<>();
            params.put("job", job);
            params.put("jobProcessor", this);
            params.put("part", part);
            params.put("nozzle", nozzle);
            params.put("placement", placement);
            params.put("boardLocation", boardLocation);
            params.put("placementLocation", placementLocation);
            Configuration.get().getScripting().on("Job.Placement.Complete", params);
            
            Logger.debug("Place {} with {}", part, nozzle.getName());
        }

        clearStepComplete();
    }

    protected void doCleanup() throws Exception {
        fireTextStatus("Cleaning up.");

        // Safe Z the machine
        head.moveToSafeZ();
        
        // Discard any currently picked parts
        discardAll(head);

        // Safe Z the machine
        head.moveToSafeZ();

        if (parkWhenComplete) {
            fireTextStatus("Park nozzle.");
            MovableUtils.moveToLocationAtSafeZ(head.getDefaultNozzle(), head.getParkLocation());
        }
        
        double dtSec = (System.currentTimeMillis() - startTime)/1000.0;
        DecimalFormat df = new DecimalFormat("###,###.0");
        
        Logger.info("Job finished {} parts in {} sec. This is {} pph", totalPartsPlaced, df.format(dtSec), df.format(totalPartsPlaced / (dtSec / 3600.0)));
        
        HashMap<String, Object> params = new HashMap<>();
        params.put("job", job);
        params.put("jobProcessor", this);
        Configuration.get().getScripting().on("Job.Finished", params);
        
        fireTextStatus("Job finished - placed %s parts in %s sec. (%s CPH)", totalPartsPlaced, df.format(dtSec), df.format(totalPartsPlaced / (dtSec / 3600.0)));
    }

    protected void doReset() throws Exception {
        this.job = null;
    }

    /**
     * Discard the picked part, if any. Remove the currently processing PlannedPlacement from the
     * list and mark the JobPlacement as Skipped.
     * 
     * @throws Exception
     */
    protected void doSkip() throws Exception {
        if (plannedPlacements.size() > 0) {
            PlannedPlacement plannedPlacement = plannedPlacements.remove(0);
            JobPlacement jobPlacement = plannedPlacement.jobPlacement;
            Nozzle nozzle = plannedPlacement.nozzle;
            discard(nozzle);
            jobPlacement.status = Status.Skipped;
            Logger.debug("Skipped {}", jobPlacement.placement);
        }
    }

    protected void clearStepComplete() {
        for (PlannedPlacement plannedPlacement : plannedPlacements) {
            plannedPlacement.stepComplete = false;
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
    public Wizard getConfigurationWizard() {
        return new ReferencePnpJobProcessorConfigurationWizard(this);
    }
    
    public boolean isParkWhenComplete() {
        return parkWhenComplete;
    }

    public void setParkWhenComplete(boolean parkWhenComplete) {
        this.parkWhenComplete = parkWhenComplete;
    }
    
    public List<JobPlacement> getJobPlacementsById(String id) { 
        return jobPlacements.stream().filter((jobPlacement) -> {
            return jobPlacement.toString() == id;
        }).collect(Collectors.toList()); 
    } 
    
    public List<JobPlacement> getJobPlacementsById(String id, Status status) {
        return jobPlacements.stream().filter((jobPlacement) -> {
            return jobPlacement.toString() == id && jobPlacement.status == status;
        }).collect(Collectors.toList());
    }

    // Sort a List<JobPlacement> by the number of nulls it contains in ascending order.
    Comparator<List<JobPlacement>> byFewestNulls = (a, b) -> {
        return Collections.frequency(a, null) - Collections.frequency(b, null);
    };

    // Sort a List<JobPlacement> by the number of nozzle changes it will require in
    // descending order.
    Comparator<List<JobPlacement>> byFewestNozzleChanges = (a, b) -> {
        int countA = 0, countB = 0;
        for (int i = 0; i < head.getNozzles().size(); i++) {
            Nozzle nozzle = head.getNozzles().get(i);
            JobPlacement jpA = a.get(i);
            JobPlacement jpB = b.get(i);
            if (nozzle.getNozzleTip() == null) {
                countA++;
                countB++;
                continue;
            }
            if (jpA != null && !nozzle.getNozzleTip().canHandle(jpA.placement.getPart())) {
                countA++;
            }
            if (jpB != null && !nozzle.getNozzleTip().canHandle(jpB.placement.getPart())) {
                countB++;
            }
        }
        return countA - countB;
    };
    
    BoardLocation getFiducialCompensatedBoardLocation(BoardLocation boardLocation) {
        // Check if there is a fiducial override for the board location and if so, use it.
        if (boardLocationFiducialOverrides.containsKey(boardLocation)) {
            BoardLocation boardLocation2 = new BoardLocation(boardLocation.getBoard());
            boardLocation2.setSide(boardLocation.getSide());
            boardLocation2.setLocation(boardLocationFiducialOverrides.get(boardLocation));
            return boardLocation2;
        }
        return boardLocation;
    }

}
