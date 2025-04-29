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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.openpnp.Translations;
import org.openpnp.gui.JobPanel;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.vision.AbstractPartAlignment;
import org.openpnp.machine.reference.wizards.ReferencePnpJobProcessorConfigurationWizard;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Job;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Motion.MotionOption;
import org.openpnp.model.PanelLocation;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.model.PlacementsHolderLocation;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.FiducialLocator;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Locatable.LocationOption;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.spi.PartAlignment;
import org.openpnp.spi.PnpJobPlanner;
import org.openpnp.spi.PnpJobPlanner.PlannedPlacement;
import org.openpnp.spi.PnpJobProcessor.JobPlacement.Status;
import org.openpnp.spi.base.AbstractJobProcessor;
import org.openpnp.spi.base.AbstractPnpJobProcessor;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.TravellingSalesman;
import org.openpnp.util.UiUtils;
import org.openpnp.util.Utils2D;
import org.openpnp.util.VisionUtils;
import org.openpnp.util.MotionUtils;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root
public class ReferencePnpJobProcessor extends AbstractPnpJobProcessor {
    interface Step {
        public Step step() throws JobProcessorException;
    }
    
    public enum JobOrderHint {
        Part, PartHeight,           // keep this values for backward compatibility
        PartBoard, HeightPartBoard, // sort as default, but use the board id as final sorting order
        BoardPart,                  // sort by board id first, then part id
        PickLocation,               // take the shortest route between all pick locations
        PickPlaceLocation,          // optimize all place locations feeder wise for shortest route
        NozzleTips,                 // group placements by compatible nozzle tips and optimize each group using PickPlaceLocation
        Unsorted;                   // keep the placements unsorted - for hand-optimized jobs

        // provide a dedicated toSting() method (with translation) to convert the enum values into
        // user friendly strings for the UI
        @Override
        public String toString() {
            return Translations.getString("MachineSetup.JobProcessors.ReferencePnpJobProcessor.JobOrder." + this.name());
        }
    }

    @Attribute(required = false)
    protected JobOrderHint jobOrder = JobOrderHint.NozzleTips;

    @Attribute(required = false)
    protected int maxVisionRetries = 3;
    
    @Attribute(required = false)
    boolean steppingToNextMotion = true;

    @Attribute(required = false)
    boolean optimizeMultipleNozzles = true;

    @Attribute(required = false)
    boolean allowImmediateNozzleTipCalibration = false;

    /**
     * Number of ficudial nesting level to check separately before checking the remaining all at once.
     * Default is 1 to check root-level panels/boards separately avoiding missdetections and/or extra
     * camera movements while checking fiducials on other layers while still preserving some benefit
     * of an optimized route.
     */
    @Attribute(required = false)
    int fiducialLevel = 1;

    /**
     * This flag - if enabled - pre-rotates all nozzles on way to the first feed or pick location,
     * the bottom camera and the first place location. Assuming that this move takes longer then 
     * moving the next nozzle to its pick, align or place location, an overall speed enhancement
     * combined with a lower risk of slipping parts on nozzles tips is expected.
     */
    @Attribute(required = false)
    boolean preRotateAllNozzles = true;

    @Element(required = false)
    public PnpJobPlanner planner = new SimplePnpJobPlanner();

    protected Job job;

    protected Machine machine;

    protected Head head;

    protected static Locator pickLocator;
    protected static Locator alignLocator;
    protected static Locator placeLocator;
    
    protected List<JobPlacement> jobPlacements = new ArrayList<>();

    private Step currentStep = null;
    
    long startTime;
    int totalPartsPlaced;
    
    public ReferencePnpJobProcessor() {
    }
    
    public synchronized void initialize(Job job) throws Exception {
        if (job == null) {
            throw new Exception("Can't initialize with a null Job.");
        }
        this.job = job;
        currentStep = new PreFlight();
        this.fireJobState(Configuration.get().getMachine().getSignalers(), AbstractJobProcessor.State.STOPPED);
    }

    @Override
    public synchronized boolean next() throws JobProcessorException {
        this.fireJobState(Configuration.get().getMachine().getSignalers(), AbstractJobProcessor.State.RUNNING);
        try {
            currentStep = currentStep.step();
        }
        catch (Exception e) {
            this.fireJobState(Configuration.get().getMachine().getSignalers(), AbstractJobProcessor.State.ERROR);
            throw e;
        }
        if (currentStep == null) {
            this.fireJobState(Configuration.get().getMachine().getSignalers(), AbstractJobProcessor.State.FINISHED);
        }
        return currentStep != null;
    }

    public synchronized void abort() throws JobProcessorException {
        try {
            new Cleanup().step();
        }
        catch (Exception e) {
            // We swallow the error here because if we can't cleanup there's not really much
            // we can do. We have to end the job.
            Logger.error(e);
        }
        this.fireJobState(Configuration.get().getMachine().getSignalers(), AbstractJobProcessor.State.STOPPED);
        currentStep = null;
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
    protected class PreFlight implements Step {
        public Step step() throws JobProcessorException {
            startTime = System.currentTimeMillis();
            totalPartsPlaced = 0;
            
            jobPlacements.clear();

            // Create some shortcuts for things that won't change during the run
            machine = Configuration.get().getMachine();
            try {
                head = machine.getDefaultHead();
            }
            catch (Exception e) {
                throw new JobProcessorException(machine, e);
            }
            pickLocator  = new PickLocator();
            alignLocator = new AlignLocator();
            placeLocator = new PlaceLocator();
            
            checkSetupErrors();
            
            prepMachine();
            
            prepFeeders();
            
            scriptJobStarting();
            
            planner.restart();

            return new FiducialCheck();
        }
        
        private void checkSetupErrors() throws JobProcessorException {
            fireTextStatus("Checking job for setup errors.");

            for (BoardLocation boardLocation : job.getBoardLocations()) {
                // Only check enabled boards
                if (!boardLocation.isEnabled()) {
                    continue;
                }
                
                checkDuplicateRefs(boardLocation);
                
                for (Placement placement : boardLocation.getBoard().getPlacements()) {
                    // Ignore placements that aren't placements
                    if (placement.getType() != Placement.Type.Placement) {
                        continue;
                    }
                    
                    if (!placement.isEnabled()) {
                        continue;
                    }
                    
                    // Ignore placements that are placed already
                    if (job.retrievePlacedStatus(boardLocation, placement.getId())) {
                        continue;
                    }
                    
                    // Ignore placements that aren't on the side of the board we're processing.
                    if (placement.getSide() != boardLocation.getGlobalSide()) {
                        continue;
                    }

                    JobPlacement jobPlacement = new JobPlacement(boardLocation, placement);

                    checkJobPlacement(jobPlacement);

                    jobPlacements.add(jobPlacement);
                }
            }
        }
        
        private void checkJobPlacement(JobPlacement jobPlacement) throws JobProcessorException {
            BoardLocation boardLocation = jobPlacement.getBoardLocation();
            Placement placement = jobPlacement.getPlacement();
            
            // Make sure the part is not null
            if (placement.getPart() == null) {
                throw new JobProcessorException(placement, String.format("Part not found for board %s, placement %s.",
                        boardLocation.getBoard().getName(), placement.getId()));
            }
            
            Part part = placement.getPart();
            
            // Make sure the part has a package assigned
            if (part.getPackage() == null) {
                throw new JobProcessorException(part, String.format("No package set for part %s.",
                        part.getId()));                
            }

            // Make sure there is at least one compatible nozzle tip available
            validatePartNozzleTip(head, part);

            // Make sure there is at least one compatible and enabled feeder available
            findFeeder(machine, part);
        }
        
        private void validatePartNozzleTip(Head head, Part part) throws JobProcessorException {
            /**
             * 1. Make a list of NozzleTips that can be loaded into at least one Nozzle.
             * 2. Filter that list down to NozzleTips that can handle the part.
             * 3. Return !list.isEmpty()
             */
            
            Set<NozzleTip> compatibleNozzleTips = head
                    .getNozzles()
                    .stream()
                    .flatMap(nozzle -> {
                        return nozzle.getCompatibleNozzleTips(part).stream();
                    })
                    .collect(Collectors.toSet());
            
            if (compatibleNozzleTips.isEmpty()) {
                if (part.isPartHeightUnknown()) {
                    throw new JobProcessorException(part, String.format("No part height sensing method found for part %s. "
                            + "Check camera, contact probe nozzle and compatible, loadable nozzle tips for height sensing "
                            + "settings or set part height manually.",
                            part.getId()));
                }
                else {
                    throw new JobProcessorException(part, String.format("No compatible, loadable nozzle tip found for part %s.",
                            part.getId()));
                }
            }
        }
        
        private void scriptJobStarting() throws JobProcessorException {
            HashMap<String, Object> params = new HashMap<>();
            params.put("job", job);
            params.put("jobProcessor", this);
            try {
                Configuration.get().getScripting().on("Job.Starting", params);
            }
            catch (Exception e) {
                throw new JobProcessorException(null, e);
            }
        }
        
        private void prepMachine() throws JobProcessorException {
            // Everything looks good, so prepare the machine.
            fireTextStatus("Preparing machine.");

            // Safe Z the machine
            try {
                head.moveToSafeZ();
            }
            catch (Exception e) {
                throw new JobProcessorException(head, e);
            }
            // Discard any currently picked parts
            discardAll(head);
        }

        private void prepFeeders() throws JobProcessorException {
            // Everything still looks good, so prepare the feeders.
            fireTextStatus("Preparing feeders.");
            Machine machine = Configuration.get().getMachine();
            List<Feeder> feederVisitList = new ArrayList<>();
            List<Feeder> feederNoVisitList = new ArrayList<>();
            // Get all the feeders that are used in the pending placements.
            for (Feeder feeder : machine.getFeeders()) {
                if (feeder.isEnabled() && feeder.getPart() != null) {
                    for (JobPlacement placement : getPendingJobPlacements()) {
                        if (placement.getPartId().equals(feeder.getPart().getId())) {
                            if (feeder.getJobPreparationLocation() != null) {
                                // only feeders with location added to the visit list
                                // only add feeders once
                                if (!feederVisitList.contains(feeder)) {
                                    feederVisitList.add(feeder);
                                }
                            }
                            // always also add them to the general (second pass) prep list
                            feederNoVisitList.add(feeder);
                        }
                    }
                }
            }
            
            Location startLocation = null;
            try {
                startLocation = head.getDefaultCamera().getLocation();
            }
            catch (Exception e) {
                Logger.error(e);
            }                

            // Use a Travelling Salesman algorithm to optimize the path to actuate all the feeder covers.
            TravellingSalesman<Feeder> tsm = new TravellingSalesman<>(
                    feederVisitList, 
                    new TravellingSalesman.Locator<Feeder>() { 
                        @Override
                        public Location getLocation(Feeder locatable) {
                            return locatable.getJobPreparationLocation();
                        }
                    }, 
                    // start from current location
                    startLocation, 
                    // no particular end location
                    null);

            // Solve it using the default heuristics.
            tsm.solve();

            // Prepare feeders along the visit travel path.
            for (Feeder feeder : tsm.getTravel()) {
                try {
                    feeder.prepareForJob(true);
                }
                catch (Exception e) {
                    throw new JobProcessorException(feeder, e);
                }
            }
            // Prepare feeders in general (second pass for visited feeders).
            for (Feeder feeder : feederNoVisitList) {
                try {
                    feeder.prepareForJob(false);
                }
                catch (Exception e) {
                    throw new JobProcessorException(feeder, e);
                }
            }
        }

        private void checkDuplicateRefs(BoardLocation boardLocation) throws JobProcessorException {
            // Check for ID duplicates - throw error if any are found
            HashSet<String> idlist = new HashSet<String>();
            for (Placement placement : boardLocation.getBoard().getPlacements()) {
                if (idlist.contains(placement.getId())) {
                    throw new JobProcessorException(boardLocation, 
                            String.format("This board contains at least one duplicate ID entry: %s ",
                            placement.getId()));
                } 
                else {
                    idlist.add(placement.getId());
                }
            }       
        }
    }
    
    protected class FiducialCheck implements Step {
        protected Set<PlacementsHolderLocation<?>> completed = new HashSet<>();
        protected int level = 0; // counter used to process some nesting levels of fiducials separately

        public Step step() throws JobProcessorException {
            FiducialLocator locator = Configuration.get().getMachine().getFiducialLocator();
            
            // collect all board and panel fiducial locations
            List<ExtendedPlacementsHolderLocation> locations = collectAllBoardLocations(job.getRootPanelLocation(), 0);

            // filter placements holder locations that are not yet completed
            locations = locations.stream()
                    .filter(l -> { return !completed.contains(l.getPlacementsHolderLocation()); })
                    .collect(Collectors.toList());

            // if all locations have been processed, continue with next tep
            if (locations.isEmpty()) {
                return new Plan();
            }

            // on request, only process the top layer of fiducials
            if (level < fiducialLevel) {
                // filter the top layer fiducials
                fireTextStatus("Checking fiducials at level " + level + ".");
                int nestingLevel = locations.get(0).getNestingLevel();
                locations = locations.stream()
                        .filter(l -> { return l.getNestingLevel() == nestingLevel; })
                        .collect(Collectors.toList()); 
            }
            else {
                if (fiducialLevel > 0) {
                    fireTextStatus("Checking remaining fiducials.");
                }
                else {
                    fireTextStatus("Checking all fiducials.");
                }
            }
            
            // get all placementHolderLocations without nesting level
            List<PlacementsHolderLocation<?>> locationsToProcess = locations.stream()
                    .map(l -> {return l.getPlacementsHolderLocation(); })
                    .collect(Collectors.toList());
            
            try {
                locator.locateAllPlacementsHolder(locationsToProcess, null);
            }
            catch (Exception e) {
                throw new JobProcessorException(locationsToProcess, e);
            }

            // increment pass to process next layer on next pass and add processed to completed
            level++;
            completed.addAll(locationsToProcess);
            
            // return to process remaining fiducials (if any)
            return this;
        }

        // collect all board locations of all panels recursively
        private List <ExtendedPlacementsHolderLocation> collectAllBoardLocations(PlacementsHolderLocation<?> rootLocation, int nestingLevel) {
            List<ExtendedPlacementsHolderLocation> locations = new ArrayList<>();

            if (rootLocation instanceof BoardLocation) {
                BoardLocation boardLocation = (BoardLocation)rootLocation;
                if (boardLocation.isEnabled() && boardLocation.isCheckFiducials()) {
                    locations.add(new ExtendedPlacementsHolderLocation(boardLocation, nestingLevel));
                }
            }
            else if (rootLocation instanceof PanelLocation) {
                PanelLocation panelLocation = (PanelLocation)rootLocation;

                // only continue if the panel is enabled
                if (panelLocation.isEnabled()) {
                    // add the panel itself if enabled
                    if (panelLocation.isCheckFiducials()) {
                        locations.add(new ExtendedPlacementsHolderLocation(panelLocation, nestingLevel));
                    }
                    
                    // get all children of the panel
                    List<PlacementsHolderLocation<?>> children = panelLocation.getPanel().getChildren();

                    // loop over all children and collect their descendants
                    int nextNestingLevel = nestingLevel +1;
                    for (PlacementsHolderLocation<?> child : children) {
                       locations.addAll(collectAllBoardLocations(child, nextNestingLevel));
                    }
                }
            }
            
            // return the complete list
            return locations;
        }

        // this class holds BoardLocations together with their nesting level to optimize them respecting the nesting level
        private class ExtendedPlacementsHolderLocation {
            private final PlacementsHolderLocation<?> placementsHolderLocation;
            private final int nestingLevel;
            ExtendedPlacementsHolderLocation(PlacementsHolderLocation<?> placementsHolderLocation, int nestingLevel) {
                super();
                this.placementsHolderLocation = placementsHolderLocation;
                this.nestingLevel = nestingLevel;
            }
            PlacementsHolderLocation<?> getPlacementsHolderLocation() {
                return placementsHolderLocation;
            }
            int getNestingLevel() {
                return nestingLevel;
            }
            
            @Override
            public String toString() {
                return "@" + nestingLevel + ": " + placementsHolderLocation;
            }
        }
    }

    protected class Plan implements Step {
        public Step step() throws JobProcessorException {
            fireTextStatus("Planning placements.");

            ReturnJobPlacementsAndNozzleTips jobPlacementsAndNozzleTips;

            // sort/plan all pending job placements
            jobPlacementsAndNozzleTips = planJobPlacements(getPendingJobPlacements());
            
            List<JobPlacement> plannedJobPlacements = jobPlacementsAndNozzleTips.getJobPlacements();

            if (plannedJobPlacements == null || plannedJobPlacements.isEmpty()) {
                return new Finish();
            }

            long t = System.currentTimeMillis();
            List<PlannedPlacement> plannedPlacements = planner.plan(head, plannedJobPlacements, jobPlacementsAndNozzleTips.getNozzleTips());
            Logger.debug("Planner complete in {}ms: {}", (System.currentTimeMillis() - t), plannedPlacements);

            if (plannedPlacements.isEmpty()) {
                throw new JobProcessorException(planner, "Planner failed to plan any placements. Please contact support.");
            }

            for (PlannedPlacement plannedPlacement : plannedPlacements) {
                plannedPlacement.jobPlacement.setStatus(Status.Processing);
            }
            
            Logger.debug("Planned placements {}", plannedPlacements);
            
            return new ChangeNozzleTips(plannedPlacements);
        }

        /**
         * sort, optimize and plan all job placements
         * the result is stored for reuse if nothing has changed.
         * 
         * @return
         */
        private class ReturnJobPlacementsAndNozzleTips {
            final List<JobPlacement> jobPlacements;
            final List<NozzleTip> nozzleTips;
            
            public ReturnJobPlacementsAndNozzleTips(List<JobPlacement> jobPlacements, List<NozzleTip> nozzleTips) {
                this.jobPlacements = jobPlacements;
                this.nozzleTips = nozzleTips;
            }
            
            public List<JobPlacement> getJobPlacements() {
                return jobPlacements;
            }
            
            public List<NozzleTip> getNozzleTips() {
                return nozzleTips;
            }
        }
        private ReturnJobPlacementsAndNozzleTips planJobPlacements(List<JobPlacement> jobPlacements) {
            long t = System.currentTimeMillis();
            List<JobPlacement> plannedJobPlacements = null;
            ReturnJobPlacementsAndNozzleTips plannedJobPlacementsAndNozzleTips = null;
    
            // provide a shortcut to not execute all the sorting
            if (!jobPlacements.isEmpty()) {
                switch (jobOrder) {
                // this options are the default: all parts are groups but indistinguishable across the entire job
                case Part:
                    // Get the list of unfinished placements and sort them by part.
                    plannedJobPlacements = jobPlacements.stream()
                            .sorted(Comparator
                                    .comparing(JobPlacement::getPartId))
                            .collect(Collectors.toList());
                    break;
        
                case PartHeight:
                    // Get the list of unfinished placements and sort them by part height.
                    plannedJobPlacements = jobPlacements.stream()
                            .sorted(Comparator
                                    .comparing(JobPlacement::getPartHeight)
                                    .thenComparing(JobPlacement::getPartId))
                            .collect(Collectors.toList());
                    break;
        
                // this options are more specific and result in sorted lists even for panels of identical boards
                // placements will be still indistinguishable across individual boards.
                case PartBoard:
                    plannedJobPlacements = jobPlacements.stream()
                            .sorted(Comparator
                                    .comparing(JobPlacement::getPartId)
                                    .thenComparing(JobPlacement::getBoardId))
                            .collect(Collectors.toList());
                    break;
                    
                case HeightPartBoard:
                    plannedJobPlacements = jobPlacements.stream()
                            .sorted(Comparator
                                    .comparing(JobPlacement::getPartHeight)
                                    .thenComparing(JobPlacement::getPartId)
                                    .thenComparing(JobPlacement::getBoardId))
                            .collect(Collectors.toList());
                    break;
                    
                case BoardPart:
                    plannedJobPlacements = jobPlacements.stream()
                            .sorted(Comparator
                                    .comparing(JobPlacement::getBoardId)
                                    .thenComparing(JobPlacement::getPartId))
                            .collect(Collectors.toList());
                    break;
                    
                case PickLocation:
                    plannedJobPlacements = planJobPlacementsByPickLocation(jobPlacements);
                    break;
                    
                case PickPlaceLocation:
                    plannedJobPlacements = planJobPlacementsByPickPlaceLocation(jobPlacements);
                    break;
                    
                case NozzleTips:
                    plannedJobPlacementsAndNozzleTips = planJobPlacementsByNozzleTips(jobPlacements);
                    break;
                    
                // FIXME: generating a error if not all enum values are handled would be more error resistant
                default:
                    Logger.warn("JobProcessor:Plan(): unhandled jobOrder " + jobOrder);
        
                    // !! fall through to get the same result as unsorted and always well defined jobPlacements
        
                case Unsorted:
                    // use job placements unsorted - for "hand-crafted" jobs
                    plannedJobPlacements = jobPlacements;
                }
                
                Logger.debug("Placements sorting using {} completed in {}ms", jobOrder.name(), (System.currentTimeMillis() - t));
            }
                
            // some strategies return lists of nozzle tips to be used in planned order as well. If not available
            // return NULL. The JobPlanner will then later setup a list of all available nozzle tips
            if (plannedJobPlacementsAndNozzleTips == null) {
                plannedJobPlacementsAndNozzleTips = new ReturnJobPlacementsAndNozzleTips(plannedJobPlacements, null);
            }
            
            return plannedJobPlacementsAndNozzleTips;
        }

        /**
         * Plan placements by pick location: optimize the path between all feeders using
         * a traveling salesman.
         * 
         * @param input list to optimize
         * @return optimized list
         */
        private class ReturnListAndLocation {
            final List<JobPlacement> jobPlacements;
            final Location location;
            
            public ReturnListAndLocation(List<JobPlacement> jobPlacements, Location location) {
                this.jobPlacements = jobPlacements;
                this.location = location;
            }
            
            public List<JobPlacement> getJobPlacements() {
                return jobPlacements;
            }

            public Location getLocation() {
                return location;
            }
        }
        private ReturnListAndLocation planJobPlacementsByPickLocation(List<JobPlacement> input, Location startLocation) {
            List<JobPlacement> output;
            Location endLocation;

            // calculate 
            endLocation = updateFeederIndex(input, startLocation);

            // sort placements by feeder index
            output = input.stream()
                    .sorted(Comparator
                            .comparing(JobPlacement::getFeederIndex)
                            .thenComparing(JobPlacement::getPartId)
                            .thenComparing(JobPlacement::getBoardId))
                    .collect(Collectors.toList());

            return new ReturnListAndLocation(output, endLocation);
        }
        private List<JobPlacement> planJobPlacementsByPickLocation(List<JobPlacement> input) {
            ReturnListAndLocation data = planJobPlacementsByPickLocation(input, null);
            return data.getJobPlacements();
        }
        
        /**
         * Update feederIndex in jobPlacements using any available feeders pick locations
         * taking the shortest route between all feeders. Later this index can be used to
         * sort/optimize the jobPlacements. 
         * 
         * @param jobPlacements
         * @param startLocation location to use as start location for the traveling salesman
         * @return end location after optimization
         */
        private Location updateFeederIndex(List<JobPlacement> jobPlacements, Location startLocation) {
            List<Feeder> feeders = new ArrayList<>();
            
            // start by sorting the list for partIDs: as each partID has its own feeder,
            // we can then easily collect a list of all required feeders
            List<JobPlacement> local = jobPlacements.stream()
                    .sorted(Comparator
                            .comparing(JobPlacement::getPartId))
                    .collect(Collectors.toList());

            // loop over all placements and collect required feeders
            feeders.clear();
            for (JobPlacement p : local) {
                // get feeder and add it to the list
                try {
                    final Feeder feeder = findFeeder(machine, p.getPlacement().getPart());
                    if (!feeders.contains(feeder)) {
                        feeders.add(feeder);
                    }
                }
                catch (Exception e) {
                }
            }
            
            // route pick locations of all feeders through travelling salesman
            TravellingSalesman<Feeder> tsm = new TravellingSalesman<>(
                    feeders, 
                    new TravellingSalesman.Locator<Feeder>() { 
                        @Override
                        public Location getLocation(Feeder locatable) {
                            return getPickLocation(locatable);
                        }
                    }, 
                    startLocation,
                    null);
            
            // Solve it using the default heuristics.
            tsm.solve();
            
            // get the optimized list of feeders
            feeders = tsm.getTravel();
            
            // feed feeder locations back into jobPlacements as feederIndex
            for (JobPlacement p : local) {
                // find feeder for this placement
                final Part part = p.getPlacement().getPart();
                for (int i = 0; i < feeders.size(); ++i) {
                    if (feeders.get(i).getPart().equals(part)) {
                        p.setFeederIndex(i);
                        break;
                    }
                        
                }
            }
            
            return getPickLocation(feeders.get(feeders.size() -1));
        }

        private Location getPickLocation(Feeder f) {
            Location location = null;

            try {
                location = f.getPickLocation();
            }
            catch (Exception e) {
                location = null;
            }

            return location;
        }
        
        /**
         * Plan placements by pick and place location: optimize the path between all feeders
         * and the path between all place locations of placements from the same feeder using
         * a traveling salesman.
         * 
         * @param input list to optimize
         * @return optimized list
         */
        private ReturnListAndLocation planJobPlacementsByPickPlaceLocation(List<JobPlacement> input, Location startLocation) {
            ReturnListAndLocation tmp;
            List<JobPlacement> tmp2;
            ReturnListAndLocation output;
            
            tmp = planJobPlacementsByPickLocation(input, startLocation);

            tmp2 = optimizePlaceLocations(tmp.getJobPlacements());

            output = new ReturnListAndLocation(tmp2, tmp.getLocation());
            return output;
        }
        private List<JobPlacement> planJobPlacementsByPickPlaceLocation(List<JobPlacement> input) {
            ReturnListAndLocation d = planJobPlacementsByPickPlaceLocation(input, null);
            return d.getJobPlacements();
        }

        /**
         * Optimize a list of JobPlacements feeder wise (using feederIndex) for efficient
         * order of place locations.
         * 
         * @param input list of jobPlacements (with feederIndex) to optimize
         * @return optimized list of jobPlacements
         */
        private List<JobPlacement> optimizePlaceLocations(List<JobPlacement> input) {
            List<JobPlacement> output = new ArrayList<>();
            Location startLocation = null;
            
            while (!input.isEmpty()) {
                // get all placements with the same (first) feeder index
                final int feederIndex = input.get(0).getFeederIndex();
                List<JobPlacement> tmp = input.stream()
                        .filter(jobPlacement -> { return jobPlacement.getFeederIndex() == feederIndex; })
                        .collect(Collectors.toList());

                // remove all placements now in tmp from input
                input.removeAll(tmp);
                
                // optimize the path between place location of all placements in tmp
                TravellingSalesman<JobPlacement> tsm = new TravellingSalesman<>(
                        tmp, 
                        new TravellingSalesman.Locator<JobPlacement>() { 
                            @Override
                            public Location getLocation(JobPlacement locatable) {
                                return locatable.getPlacement().getLocation();
                            }
                        }, 
                        startLocation,
                        null);
                
                // Solve it using the default heuristics.
                tsm.solve();
                
                // add the optimized list of jobPlacements to the output list
                output.addAll(tsm.getTravel());
                
                // update startLocation to the end location of the last placement
                startLocation = output.get(output.size() -1).getPlacement().getLocation();
            }
            
            return output;
        }
        
        /**
         * Plan placements by selecting the most efficient use of the available nozzle tips.
         * Initially all placements are grouped by their compatible nozzle tips. This groups
         * and then sorted by size in descending order and all but the first instance of each
         * placement removed. Finally each group is optimized using PickPlaceLocation, which
         * optimizes all pick locations and all place locations using a traveling salesman.
         * 
         * @param input
         * @return
         */
        private ReturnJobPlacementsAndNozzleTips planJobPlacementsByNozzleTips(List<JobPlacement> input) {
            /**
             * Group nozzleTip and JobPlacements into one class to collect jobPlacements
             * per nozzleTip for further sorting, filtering and processing.
             */
            class JobPlacementNozzleTip extends ArrayList<JobPlacement> {
                private static final long serialVersionUID = 1L;
                private final NozzleTip nozzleTip;
                
                JobPlacementNozzleTip(NozzleTip nozzleTip) {
                    this.nozzleTip = nozzleTip;
                }

                NozzleTip getNozzleTip() {
                    return nozzleTip;
                }
            }
            
            List<JobPlacement> output;
            
            // get all nozzle tips
            List<NozzleTip> nozzleTips = new ArrayList<>(head.getMachine().getNozzleTips());
            List<Nozzle> nozzles = new ArrayList<>(head.getNozzles());
            
            // filter nozzleTips by compatibility with any nozzle
            nozzleTips = nozzleTips
                    .stream()
                    .filter(nozzleTip -> {
                        for (Nozzle nozzle : nozzles) {
                            if (nozzle.getCompatibleNozzleTips().contains(nozzleTip)) {
                                return true;
                            }
                        }
                        return false;
                    })
                    .collect(Collectors.toList());
            
            // this list contains one entry per NozzleTip. Each entry contains in addition a list
            // of all jobPlacments that are compatible with that nozzleTip.
            List<JobPlacementNozzleTip> perNozzleTipJobPlacements = new ArrayList<>();

            // build lists with placements compatible with each nozzleTip
            for (int i = 0; i < nozzleTips.size(); ++i) {
                NozzleTip nozzleTip = nozzleTips.get(i);
                JobPlacementNozzleTip jobPlacementNozzleTip = new JobPlacementNozzleTip(nozzleTip);
                
                // add all job placements that are compatible with that nozzle tip
                jobPlacementNozzleTip.addAll(input.stream().filter(jobPlacement -> {
                    Placement placement = jobPlacement.getPlacement();
                    Part part = placement.getPart();
                    org.openpnp.model.Package pkg = part.getPackage();
                    return pkg.getCompatibleNozzleTips().contains(nozzleTip);
                } ).collect(Collectors.toList()));

                if (!jobPlacementNozzleTip.isEmpty()) {
                    // add the list of placements and nozzleTip
                    perNozzleTipJobPlacements.add(jobPlacementNozzleTip);
                }
            }
            
            // sort lists per nozzleTips by their size such that the nozzle tip that can handle
            // the most jobPlacments is first. As second sorting criteria the nozzleTip's name is used.
            // For mulit-nozzle machines with different nozzleTips for each nozzle that are compatible
            // with the same jobPlacments this always results in groups of identical amounts of
            // jobPlacments. Taking the name into account makes the sorting unique again.
            perNozzleTipJobPlacements.sort(Comparator.comparing(JobPlacementNozzleTip::size).reversed()
                    .thenComparing(j -> j.getNozzleTip().getName()));
            
            // build a list of just all nozzleTips after sorting. This list is to be used by the
            // JobPlanner to select the next nozzleTip to use.
            // This list contains _all_ nozzle tips, that are compatible with all jobPlacements. It is
            // not limited to a set of tips, that is required to place all part. For single nozzle
            // machines it would be possible to derive such a list, but for multi nozzle machines it is
            // unknown at this stage, which nozzle tip might serve well in parallel with with others on
            // the other nozzles. Keeping all possible tips sorted by amount of placements they can
            // handle will likely provide a good starting point in selecting the next best one.
            List<NozzleTip> plannedNozzleTips = perNozzleTipJobPlacements.stream().map(j -> j.getNozzleTip()).collect(Collectors.toList());
            
            // Remove duplicate placements keeping only the first occurrence
            // Skip this step if there is only one nozzle tip left.
            if (perNozzleTipJobPlacements.size() > 1) {
                // loop over all nozzleTips but the last
                for (int i = 0; i < perNozzleTipJobPlacements.size() -1; ++i) {
                    final JobPlacementNozzleTip dominantPlacements = perNozzleTipJobPlacements.get(i);
                    
                    // loop over all other nozzleTip
                    for (int j = i + 1; j < perNozzleTipJobPlacements.size(); ++j) {
                        // remove all placements that are in the dominant group
                        JobPlacementNozzleTip recessivePlacements = perNozzleTipJobPlacements.get(j);
                        recessivePlacements.removeAll(dominantPlacements);
                        perNozzleTipJobPlacements.set(j, recessivePlacements);
                    }
                }
                
                // remove empty nozzle groups
                perNozzleTipJobPlacements = perNozzleTipJobPlacements.stream().filter(i -> !i.isEmpty()).collect(Collectors.toList());
            }
            
            // optimize each nozzle tip group using PickPlaceLocation
            output = new ArrayList<JobPlacement>();
            // This variable is the return value of planJobPlacmentsByPickPlaceLocation
            // The second argument is the end location of the optimization. It is here
            // used as start location for the next group.
            // FIXME: would it be a good idea to use the current head location as start location for the optimization?
            ReturnListAndLocation data = new ReturnListAndLocation(null, null);
            String traceMessage = "Selected nozzle tips:";
            for (JobPlacementNozzleTip jobPlacementNozzleTip : perNozzleTipJobPlacements) {
                // plan optimized feeder and pick locations
                data = planJobPlacementsByPickPlaceLocation(jobPlacementNozzleTip, data.getLocation());
                
                // and add the result to the output list
                output.addAll(data.getJobPlacements());

                // build a debug message to report about the selected nozzle tips and the amount of components each takes
                traceMessage += " " + jobPlacementNozzleTip.getNozzleTip().getName() + " (" + data.getJobPlacements().size() + ")";
            }
            
            Logger.trace(traceMessage);
            
            return new ReturnJobPlacementsAndNozzleTips(output, plannedNozzleTips);
        }
    }
    
    protected class ChangeNozzleTips extends PlannedPlacementStep {
        public ChangeNozzleTips(List<PlannedPlacement> plannedPlacements) {
            super(plannedPlacements);
        }
        
        @Override
        public Step stepImpl(PlannedPlacement plannedPlacement) throws JobProcessorException {
            if (plannedPlacement == null) {
                return new CalibrateNozzleTips(plannedPlacements);
            }
            
            final Nozzle nozzle = plannedPlacement.nozzle;
            final NozzleTip nozzleTip = plannedPlacement.nozzleTip;

            // If the Nozzle already has the correct NozzleTip loaded we're good.
            if (nozzle.getNozzleTip() == nozzleTip) {
                Logger.debug("No nozzle tip change needed for nozzle {}", nozzle.getName());
                return this;
            }
            
            fireTextStatus("Change nozzle tip on nozzle %s to %s.", 
                    nozzle.getName(), 
                    nozzleTip.getName());
            try {
                nozzle.loadNozzleTip(nozzleTip, allowImmediateNozzleTipCalibration);
            }
            catch (Exception e) {
                if (e instanceof ReferenceNozzle.ManualLoadException) {
                    throw new JobProcessorException(nozzleTip, 
                            new UiUtils.ExceptionWithContinuation(e, () -> { resumeJob(); }));
                } else {
                    throw new JobProcessorException(nozzleTip, e);
                }
            }
            
            return this;
        }
        
        public void resumeJob() {
            Logger.debug("Restarting the job now.");
            // change the job state from within the UI thread (we are currently in a machine thread)
            SwingUtilities.invokeLater(() -> { 
                JobPanel j = MainFrame.get().getJobTab();
                j.jobResume(); }
            );
        }
    }
    
    protected class CalibrateNozzleTips extends PlannedPlacementStep {
        public CalibrateNozzleTips(List<PlannedPlacement> plannedPlacements) {
            super(plannedPlacements);
        }
        
        @Override
        public Step stepImpl(PlannedPlacement plannedPlacement) throws JobProcessorException {
            if (plannedPlacement == null) {
                return new OptimizeNozzlesForPick(plannedPlacements);
            }
            
            final Nozzle nozzle = plannedPlacement.nozzle;
            final NozzleTip nozzleTip = nozzle.getNozzleTip();
            
            if (nozzleTip == null) {
                return this;
            }
            
            if (nozzle.isCalibrated()) {
                return this;
            }
            
            fireTextStatus("Calibrate nozzle tip %s on nozzle %s", nozzleTip.getName(), nozzle.getName());
            try {
                nozzle.calibrate();
            }
            catch (Exception e) {
                throw new JobProcessorException(nozzleTip, e);
            }
            
            return this;
        }
    }

    /**
     * Optimize nozzles for best pick performance
     */
    protected class OptimizeNozzlesForPick extends AbstractOptimizationNozzlesStep {
        public OptimizeNozzlesForPick(List<PlannedPlacement> plannedPlacements) {
            super(plannedPlacements);
        }

        public Step step() throws JobProcessorException {
            
            // sort plannedPlacements for picking with alignment as next/end location using TSM
            List<PlannedPlacement> optimizedPlannedPlacements = optimizePlacements(pickLocator, alignLocator);
            
            return new PrerotateAllNozzlesForPick(optimizedPlannedPlacements);
        }
    }
        
    /**
     * Prerotate all nozzles while moving to the first pick location.
     */
    protected class PrerotateAllNozzlesForPick extends PrerotateAllNozzlesStep {
        public PrerotateAllNozzlesForPick(List<PlannedPlacement> plannedPlacements) {
            super(plannedPlacements);
        }

        public Step step() throws JobProcessorException {
            
            // In order for the pick-prerotation to return the correct rotation, the 
            // nozzle rotation mode has to be applied. If not, the pickLocation may
            // return the wrong angle.
            for (PlannedPlacement p : plannedPlacements) {
                JobPlacement jobPlacement = p.jobPlacement;
                Placement placement = jobPlacement.getPlacement();
                Part part = placement.getPart();
                Nozzle nozzle = p.nozzle;
                Location pickLocation;

                Feeder feeder = findFeeder(machine, part);
                
                try {
                    pickLocation = feeder.getPickLocation();
                }
                catch (Exception e) {
                    throw new JobProcessorException(feeder, e);
                }
                
                Location placementLocation = Utils2D.calculateBoardPlacementLocation(jobPlacement.getBoardLocation(), placement.getLocation());
                
                try {
                    nozzle.prepareForPickAndPlaceArticulation(pickLocation, placementLocation);
                }
                catch (Exception e) {
                    throw new JobProcessorException(nozzle, e);
                }
            }
            
            // now request pre-rotation of all nozzles
            prerotateAllNozzles(pickLocator);
            
            return new Pick(plannedPlacements);
        }
    }

    /**
     * Pick step - pick parts using all nozzles
     */
    protected class Pick extends PlannedPlacementStep {
        HashMap<PlannedPlacement, Integer> retries = new HashMap<>();
        
        public Pick(List<PlannedPlacement> plannedPlacements) {
            super(plannedPlacements);
        }
        
        @Override
        public Step stepImpl(PlannedPlacement plannedPlacement) throws JobProcessorException {
            if (plannedPlacement == null) {
                return new OptimizeNozzlesForAlign(plannedPlacements);
            }
            
            final Nozzle nozzle = plannedPlacement.nozzle;
            final JobPlacement jobPlacement = plannedPlacement.jobPlacement;
            final Placement placement = jobPlacement.getPlacement();
            final Part part = placement.getPart();
            final BoardLocation boardLocation = jobPlacement.getBoardLocation();
            
            /**
             * If anything goes wrong that causes us to fail all the retries, this is the error
             * that will get thrown. 
             */
            JobProcessorException lastException = null;
            for (int partPickTry = 0; partPickTry < 1 + part.getPickRetryCount(); partPickTry++) {
                /**
                 * Find an available feeder. If one cannot be found this will throw. There's nothing
                 * else we can do with this part.
                 */
                final Feeder feeder = findFeeder(machine, part);
                
                /**
                 * Run the placement starting script. An error here will throw. That's the user's
                 * problem.
                 */
                try {
                    HashMap<String, Object> params = new HashMap<>();
                    params.put("job", job);
                    params.put("jobProcessor", this);
                    params.put("part", part);
                    params.put("nozzle", nozzle);
                    params.put("placement", placement);
                    params.put("boardLocation", boardLocation);
                    params.put("feeder", feeder);
                    Configuration.get()
                                 .getScripting()
                                 .on("Job.Placement.Starting", params);
                }
                catch (Exception e) {
                    throw new JobProcessorException(null, e);
                }
                
                /**
                 * Feed the feeder, retrying up to feedRetryCount times. That happens within the
                 * feed method. It will either succeed or throw after the retries. We catch the
                 * Exception so that we can continue the loop.
                 */
                try {
                    feed(feeder, nozzle);
                }
                catch (JobProcessorException jpe) {
                    lastException = jpe;
                    continue;
                }

                /**
                 * Currently this will throw and abort the placement if it fails. Probably it should
                 * discard and retry, and really it should probably be done before we attempt to
                 * feed. I *think* this has been debated as to whether or not it's useful
                 * and should maybe be done at the end of the cycle, rather than here. Maybe it just
                 * gets removed completely.
                 */
                checkPartOff(nozzle, part);

                try {
                    feederPickRetry(nozzle, feeder, jobPlacement, part);
                }
                catch (JobProcessorException jpe) {
                    lastException = jpe;
                    discard(nozzle);
                    continue;
                }
                
                /**
                 * If we get here with no problems then we are done.
                 */
                return this;
            }
            
            /**
             * If we didn't return in the loop above then we didn't succeed, so throw
             * the recorded error.
             */
            throw lastException;
        }
        
        private void feed(Feeder feeder, Nozzle nozzle) throws JobProcessorException {
            Exception lastException = null;

            Map<String, Object> globals = new HashMap<>();
            globals.put("nozzle", nozzle);
            globals.put("feeder", feeder);
            globals.put("part", feeder.getPart());

            for (int i = 0; i < 1 + feeder.getFeedRetryCount(); i++) {
                try {
                    fireTextStatus("Feed %s on %s.", feeder.getName(), feeder.getPart().getId());
                    
                    Configuration.get().getScripting().on("Feeder.BeforeFeed", globals);
                    feeder.feed(nozzle);
                    Configuration.get().getScripting().on("Feeder.AfterFeed", globals);
                    return;
                }
                catch (Exception e) {
                    lastException = e;
                }
            }
            feeder.setEnabled(false);
            throw new JobProcessorException(feeder, lastException);
        }
        
        private void checkPartOff(Nozzle nozzle, Part part) throws JobProcessorException {
            if (!nozzle.isPartOffEnabled(Nozzle.PartOffStep.BeforePick)) {
                return;
            }
            try {
                // Part-off check can only be done at safe Z. An explicit move to safe Z is needed, because some feeder classes 
                // may move the nozzle to (near) the pick location i.e. down in Z in feed().
                nozzle.moveToSafeZ();
                if (!nozzle.isPartOff()) {
                    throw new JobProcessorException(nozzle, part, "Part vacuum-detected on nozzle before pick.");
                }
            }
            catch (JobProcessorException e) {
                throw e;
            }
            catch (Exception e) {
                throw new JobProcessorException(nozzle, part, e);
            }
        }
        
        private void feederPickRetry(Nozzle nozzle, Feeder feeder, JobPlacement jobPlacement, Part part) throws JobProcessorException {
            Exception lastException = null;
            for (int i = 0; i < 1 + feeder.getPickRetryCount(); i++) {
                try {
                    pick(nozzle, feeder, jobPlacement, part);
                    postPick(feeder, nozzle);
                    checkPartOn(nozzle);
                    return;
                }
                catch (Exception e) {
                    lastException = e;
                }
            }
            throw new JobProcessorException(feeder, nozzle, lastException);
        }
        
        private void pick(Nozzle nozzle, Feeder feeder, JobPlacement jobPlacement, Part part) throws JobProcessorException {
            try {
                fireTextStatus("Pick %s from %s for %s using nozzle %s.", part.getId(), feeder.getName(),
                        jobPlacement.getPlacement().getId(), nozzle.getName());

                // Prepare the Nozzle for pick-to-place articulation.
                Location placementLocation = Utils2D.calculateBoardPlacementLocation(jobPlacement.getBoardLocation(), jobPlacement.getPlacement().getLocation());
                nozzle.prepareForPickAndPlaceArticulation(feeder.getPickLocation(), placementLocation);

                // Move to pick location.
                nozzle.moveToPickLocation(feeder);

                // Pick
                nozzle.pick(part);

                // Retract
                nozzle.moveToSafeZ();
            }
            catch (Exception e) {
                throw new JobProcessorException(nozzle, e);
            }
        }
        
        private void postPick(Feeder feeder, Nozzle nozzle) throws JobProcessorException {
            try {
                feeder.postPick(nozzle);
            }
            catch (Exception e) {
                throw new JobProcessorException(feeder, nozzle, e);
            }
        }
        
        private void checkPartOn(Nozzle nozzle) throws JobProcessorException {
            if (!nozzle.isPartOnEnabled(Nozzle.PartOnStep.AfterPick)) {
                return;
            }
            try {
                if(!nozzle.isPartOn()) {
                    throw new JobProcessorException(nozzle, "No part vacuum-detected after pick.");
                }
            }
            catch (JobProcessorException e) {
                throw e;
            }
            catch (Exception e) {
                throw new JobProcessorException(nozzle, e);
            }
        }
    }

    /**
     * Optimize nozzles for best alignment performance
     */
    protected class OptimizeNozzlesForAlign extends AbstractOptimizationNozzlesStep {
        public OptimizeNozzlesForAlign(List<PlannedPlacement> plannedPlacements) {
            super(plannedPlacements);
        }

        public Step step() throws JobProcessorException {

            // sort plannedPlacements for alignment with place as next/end location using TSM
            List<PlannedPlacement> optimizedPlannedPlacements = optimizePlacements(alignLocator, placeLocator);
            
            // continue with alignment
            return new PrerotateAllNozzlesForAlign(optimizedPlannedPlacements);
        }
    }

    /**
     * Prerotate all nozzles while moving to the first alignement location.
     */
    protected class PrerotateAllNozzlesForAlign extends PrerotateAllNozzlesStep {
        public PrerotateAllNozzlesForAlign(List<PlannedPlacement> plannedPlacements) {
            super(plannedPlacements);
        }

        public Step step() throws JobProcessorException {
            
            prerotateAllNozzles(alignLocator);
            
            return new Align(plannedPlacements);
        }
    }

    /**
     * Alignment step - align all parts on all nozzles
     */
    protected class Align extends PlannedPlacementStep {
        public Align(List<PlannedPlacement> plannedPlacements) {
            super(plannedPlacements);
        }
        
        @Override
        public Step stepImpl(PlannedPlacement plannedPlacement) throws JobProcessorException {
            if (plannedPlacement == null) {
                return new OptimizeNozzlesForPlace(plannedPlacements);
            }
            
            final Nozzle nozzle = plannedPlacement.nozzle;
            final JobPlacement jobPlacement = plannedPlacement.jobPlacement;
            final Placement placement = jobPlacement.getPlacement();
            final Part part = placement.getPart();

            final PartAlignment partAlignment = AbstractPartAlignment.getPartAlignment(part);
            
            if (partAlignment == null) {
                plannedPlacement.alignmentOffsets = null;
                Logger.debug("Not aligning {} as no compatible enabled aligners defined", part);
                return this;
            }

            align(plannedPlacement, partAlignment);
            
            checkPartOn(nozzle);

            return this;
        }
        
        private void align(PlannedPlacement plannedPlacement, PartAlignment partAlignment) throws JobProcessorException {
            final Nozzle nozzle = plannedPlacement.nozzle;
            final JobPlacement jobPlacement = plannedPlacement.jobPlacement;
            final Placement placement = jobPlacement.getPlacement();
            final BoardLocation boardLocation = jobPlacement.getBoardLocation();
            final Part part = placement.getPart();

            Exception lastException = null;
            for (int i = 0; i < ReferencePnpJobProcessor.this.getMaxVisionRetries(); i++) {
                fireTextStatus("Aligning %s for %s using nozzle %s.", part.getId(), placement.getId(), nozzle.getName());
                try {
                    plannedPlacement.alignmentOffsets = VisionUtils.findPartAlignmentOffsets(
                            partAlignment,
                            part,
                            boardLocation,
                            placement, nozzle);
                    Logger.debug("Align {} with {}, offsets {}", part, nozzle, plannedPlacement.alignmentOffsets);
                    return;
                }
                catch (Exception e) {
                    lastException = e;
                }
            }
            throw new JobProcessorException(part, nozzle, lastException);
        }
        
        private void checkPartOn(Nozzle nozzle) throws JobProcessorException {
            if (!nozzle.isPartOnEnabled(Nozzle.PartOnStep.Align)) {
                return;
            }
            try {
                if(!nozzle.isPartOn()) {
                    throw new JobProcessorException(nozzle, "No part vacuum-detected after alignment. Part may have been lost in transit.");
                }
            }
            catch (JobProcessorException e) {
                throw e;
            }
            catch (Exception e) {
                throw new JobProcessorException(nozzle, e);
            }
        }
    }

    /**
     * Optimize nozzles for best place performance
     */
    protected class OptimizeNozzlesForPlace extends AbstractOptimizationNozzlesStep {
        public OptimizeNozzlesForPlace(List<PlannedPlacement> plannedPlacements) {
            super(plannedPlacements);
        }
        
        public Step step() throws JobProcessorException {
            
            // sort plannedPlacements for place using TSM
            // FIXME: if the planner would provide a look-ahead feature, we could use it for further optimization here
            List<PlannedPlacement> optimizedPlannedPlacements = optimizePlacements(placeLocator, null);
            
            return new PrerotateAllNozzlesForPlace(optimizedPlannedPlacements);
        }
    }

    /**
     * Prerotate all nozzles while moving to the first placement location.
     */
    protected class PrerotateAllNozzlesForPlace extends PrerotateAllNozzlesStep {
        public PrerotateAllNozzlesForPlace(List<PlannedPlacement> plannedPlacements) {
            super(plannedPlacements);
        }

        public Step step() throws JobProcessorException {
            
            prerotateAllNozzles(placeLocator);
            
            return new Place(plannedPlacements);
        }
    }

    /**
     * Placement step - place all parts on all nozzles on the board
     */
    protected class Place extends PlannedPlacementStep {
        public Place(List<PlannedPlacement> plannedPlacements) {
            super(plannedPlacements);
        }

        @Override
        public Step stepImpl(PlannedPlacement plannedPlacement) throws JobProcessorException {
            if (plannedPlacement == null) {
                return new FinishCycle();
            }
            
            final Nozzle nozzle = plannedPlacement.nozzle;
            final JobPlacement jobPlacement = plannedPlacement.jobPlacement;
            final Placement placement = jobPlacement.getPlacement();
            final Part part = placement.getPart();
            final BoardLocation boardLocation = plannedPlacement.jobPlacement.getBoardLocation();

            scriptBeforeAssembly(plannedPlacement);
            
            // Calculate this after running the script, because the script might have fine-tuned the location data.
            // Such scripts can be used as a crude alternative to a "local fiducial" feature.
            Location placementLocation = getPlacementLocation(plannedPlacement);

            checkPartOn(nozzle, part);
            
            place(nozzle, part, placement, placementLocation);
            
            checkPartOff(nozzle, part);
            
            // Mark the placement as finished
            jobPlacement.setStatus(Status.Complete);
            
            // Mark the placement as "placed"
//            boardLocation.setPlaced(jobPlacement.getPlacement().getId(), true);
            job.storePlacedStatus(boardLocation, jobPlacement.getPlacement().getId(), true);
            
            totalPartsPlaced++;
            
            scriptComplete(plannedPlacement, placementLocation);

            // update placements within the UI thread (we are currently in a machine thread)
            SwingUtilities.invokeLater(() -> {
                MainFrame f = MainFrame.get();
                if (f != null) {
                    f.getJobTab().getJobPlacementsPanel().updateActivePlacements();
                }
            });
            
            return this;
        }
        
        private void place(Nozzle nozzle, Part part, Placement placement, Location placementLocation) throws JobProcessorException {
            fireTextStatus("Placing %s for %s using nozzle %s.", part.getId(), placement.getId(), nozzle.getName());
            
            try {
                // Move to the placement location
               nozzle.moveToPlacementLocation(placementLocation, part);

                // Place the part
                nozzle.place();

                // Retract
                nozzle.moveToSafeZ();
            }
            catch (Exception e) {
                throw new JobProcessorException(nozzle, e);
            }
        }
        
        private void checkPartOn(Nozzle nozzle, Part part) throws JobProcessorException {
            if (part == null || nozzle.getPart() == null) {
                throw new JobProcessorException(part, nozzle, "No part on nozzle before place.");
            }
            if (part != nozzle.getPart()) {
                throw new JobProcessorException(part, nozzle, "Part mismatch with part on nozzle before place.");
            }

            if (nozzle.isPartOnEnabled(Nozzle.PartOnStep.BeforePlace)) {
                try {
                    if (!nozzle.isPartOn()) {
                        throw new JobProcessorException(nozzle, "No part vacuum-detected on nozzle before place.");
                    }
                }
                catch (JobProcessorException e) {
                    throw e;
                }
                catch (Exception e) {
                    throw new JobProcessorException(nozzle, e);
                }
            }
        }
        
        private void checkPartOff(Nozzle nozzle, Part part) throws JobProcessorException {
            if (nozzle.isPartOffEnabled(Nozzle.PartOffStep.AfterPlace)) {
                try {
                    // Note, we 're already at safe Z, see place().
                    if (!nozzle.isPartOff()) {
                        throw new JobProcessorException(nozzle, "Part vacuum-detected on nozzle after place.");
                    }
                }
                catch (JobProcessorException e) {
                    throw e;
                }
                catch (Exception e) {
                    throw new JobProcessorException(nozzle, e);
                }
            }
        }
        
        private void scriptBeforeAssembly(PlannedPlacement plannedPlacement) throws JobProcessorException {
            String eventName = "Job.Placement.BeforeAssembly";
            if(Configuration.get().getScripting().hasNoScript(eventName)) {
                // We know for certain that there are no scripts for this event, so
                // we can skip the parameter setup as an optimisation.
                return;
            }

            final Nozzle nozzle = plannedPlacement.nozzle;
            final JobPlacement jobPlacement = plannedPlacement.jobPlacement;
            final Placement placement = jobPlacement.getPlacement();
            final Part part = placement.getPart();
            final BoardLocation boardLocation = plannedPlacement.jobPlacement.getBoardLocation();
            Length partHeight = part.getHeight();
            // Calculate the placement location here, so that the script knows the intended location.
            // It will be calculated a second time by our caller, in case the script fine-tuned this placement
            // location data as part of a "local fiducial" feature.
            Location placementLocation = getPlacementLocation(plannedPlacement);
            Location placementLocationPart = placementLocation.add(new Location(partHeight.getUnits(), 0, 0, partHeight.getValue(), 0));
            try {
                HashMap<String, Object> params = new HashMap<>();
                params.put("job", job);
                params.put("jobProcessor", this);
                params.put("part", part);
                params.put("nozzle", nozzle);
                params.put("placement", placement);
                params.put("boardLocation", boardLocation);
                params.put("placementLocationBase", placementLocation);
                params.put("placementLocation", placementLocationPart);
                params.put("alignmentOffsets", plannedPlacement.alignmentOffsets);
                Configuration.get().getScripting().on(eventName, params);
            }
            catch (Exception e) {
                throw new JobProcessorException(null, e);
            }
        }
        
        private void scriptComplete(PlannedPlacement plannedPlacement, Location placementLocation) throws JobProcessorException {
            final Nozzle nozzle = plannedPlacement.nozzle;
            final JobPlacement jobPlacement = plannedPlacement.jobPlacement;
            final Placement placement = jobPlacement.getPlacement();
            final Part part = placement.getPart();
            final BoardLocation boardLocation = plannedPlacement.jobPlacement.getBoardLocation();
            Length partHeight = part.getHeight();
            Location placementLocationPart = placementLocation.add(new Location(partHeight.getUnits(), 0, 0, partHeight.getValue(), 0));
            try {
                HashMap<String, Object> params = new HashMap<>();
                params.put("job", job);
                params.put("jobProcessor", this);
                params.put("part", part);
                params.put("nozzle", nozzle);
                params.put("placement", placement);
                params.put("boardLocation", boardLocation);
                params.put("placementLocationBase", placementLocation);
                params.put("placementLocation", placementLocationPart);
                Configuration.get().getScripting().on("Job.Placement.Complete", params);
            }
            catch (Exception e) {
                throw new JobProcessorException(null, e);
            }
        }
        
    }
    
    protected class FinishCycle implements Step {
        public Step step() throws JobProcessorException {
            discardAll(head);
            return new Plan();
        }
    }

    protected class Cleanup implements Step {
        public Step step() throws JobProcessorException {
            fireTextStatus("Cleaning up.");
            
            try {
                // Safe Z the machine
                head.moveToSafeZ();
                
                // Discard any currently picked parts
                discardAll(head);

                // Safe Z the machine
                head.moveToSafeZ();
            }
            catch (Exception e) {
                throw new JobProcessorException(head, e);
            }
            
            fireTextStatus("Park head.");
            try {
                MovableUtils.park(head);
            }
            catch (Exception e) {
                throw new JobProcessorException(head, e);
            }
            
            return null;
        }
    }
    
    protected class Finish implements Step {
        public Step step() throws JobProcessorException {
            new Cleanup().step();
          
            double dtSec = (System.currentTimeMillis() - startTime) / 1000.0;
            DecimalFormat df = new DecimalFormat("###,###.0");
            
            // Collect the errored placements
            List<JobPlacement> erroredPlacements = jobPlacements
                    .stream()
                    .filter(jp -> {
                        return jp.getStatus() == JobPlacement.Status.Errored;
                    })
                    .collect(Collectors.toList());

            Logger.info("Job finished {} parts in {} sec. This is {} CPH", totalPartsPlaced,
                    df.format(dtSec), df.format(totalPartsPlaced / (dtSec / 3600.0)));

            try {
                HashMap<String, Object> params = new HashMap<>();
                params.put("job", job);
                params.put("jobProcessor", this);
                Configuration.get()
                             .getScripting()
                             .on("Job.Finished", params);
            }
            catch (Exception e) {
                throw new JobProcessorException(null, e);
            }

            if (!erroredPlacements.isEmpty()) {
                fireTextStatus("Job finished with %d errors, placed %s parts in %s sec. (%s CPH)",
                        erroredPlacements.size(),
                        totalPartsPlaced,
                        df.format(dtSec), 
                        df.format(totalPartsPlaced / (dtSec / 3600.0)));

                Logger.info("Errored Placements: "+erroredPlacements.size());
                for (JobPlacement jobPlacement : erroredPlacements) {
                    Logger.info("{}: {}", jobPlacement, jobPlacement.getError().getMessage());
                }
            }
            else {
                fireTextStatus("Job finished without error, placed %s parts in %s sec. (%s CPH)", 
                        totalPartsPlaced,
                        df.format(dtSec), 
                        df.format(totalPartsPlaced / (dtSec / 3600.0)));
            }

            return null;
        }
    }
    
    protected class Abort implements Step {
        public Step step() throws JobProcessorException {
            new Cleanup().step();
            
            fireTextStatus("Aborted.");
            
            return null;
        }
    }
     
    private Location getPlacementLocation(PlannedPlacement plannedPlacement) {
        final JobPlacement jobPlacement = plannedPlacement.jobPlacement;
        final Placement placement = jobPlacement.getPlacement();
        final BoardLocation boardLocation = plannedPlacement.jobPlacement.getBoardLocation();

        // Check if there is a fiducial override for the board location and if so, use it.
        Location placementLocation =
                Utils2D.calculateBoardPlacementLocation(boardLocation, placement.getLocation());

        // If there are alignment offsets update the placement location with them
        if (plannedPlacement.alignmentOffsets != null) {
            /*
             * preRotated means during alignment we have already rotated the component - this is
             * useful for say an external rotating stage that the component is placed on,
             * rotated to correct placement angle, and then picked up again.
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

        // Note, do not add the part's height to the placement location, this will be done later to allow
        // for on-the-fly part height probing.  
        return placementLocation;
    }

    protected List<JobPlacement> getPendingJobPlacements() {
        return this.jobPlacements.stream().filter((jobPlacement) -> {
            return jobPlacement.getStatus() == Status.Pending;
        }).collect(Collectors.toList());
    }

    protected boolean isJobComplete() {
        return getPendingJobPlacements().isEmpty();
    }
    
    @Override
    public Wizard getConfigurationWizard() {
        return new ReferencePnpJobProcessorConfigurationWizard(this);
    }
    
    public JobOrderHint getJobOrder() {
        return jobOrder;
    }
    
    public void setJobOrder(JobOrderHint newJobOrder) {
        this.jobOrder = newJobOrder;
    }    

    public int getMaxVisionRetries() {
        return maxVisionRetries;
    }

    public void setMaxVisionRetries(int maxVisionRetries) {
        this.maxVisionRetries = maxVisionRetries;
    }

    @Override
    public boolean isSteppingToNextMotion() {
        return steppingToNextMotion;
    }

    public void setSteppingToNextMotion(boolean steppingToNextMotion) {
        this.steppingToNextMotion = steppingToNextMotion;
    }

    public boolean isOptimizeMultipleNozzles() {
        return optimizeMultipleNozzles;
    }

    public void setOptimizeMultipleNozzles(boolean optimizeMultipleNozzles) {
        this.optimizeMultipleNozzles = optimizeMultipleNozzles;
    }

    public boolean isPreRotateAllNozzles() {
        return preRotateAllNozzles;
    }

    public void setPreRotateAllNozzles(boolean preRotateAllNozzles) {
        this.preRotateAllNozzles = preRotateAllNozzles;
    }

    /**
     * This class groups a step for step for multi-nozzle optimization
     */
    protected abstract class AbstractOptimizationNozzlesStep implements Step {
        private final List<PlannedPlacement> plannedPlacements;
        protected AbstractOptimizationNozzlesStep(List<PlannedPlacement> plannedPlacements) {
            // sort placements into their default order to provide logging referencing the unoptimized state
            this.plannedPlacements = planner.sort(plannedPlacements);
        }

        /**
         * Sort the list of planned placements for better performance
         * this is done by first collecting the locations where the head will move
         * to when executing this step and then using a traveling salesman to 
         * optimize the list.
         * 
         * @param sortLocator An interface that shall return the location of a PlannedPlacement used to optimize for
         * @param endLocator An interface that shall return the location of a PlannedPlacement to be considered
         * the end location. The center between all this end location of all PlannedPlacements will be used
         * as endLocation for the optimization.
         * @return
         */
        protected List<PlannedPlacement> optimizePlacements(Locator sortLocator, Locator endLocator) {
            List<PlannedPlacement> optimizedPlannedPlacements;
            long t = System.currentTimeMillis();
            Location start; // start location of traveling salesman, current location of the head
        
            // if multi-nozzle optimization has been disabled, stop here
            if (!optimizeMultipleNozzles) {
                return plannedPlacements;
            }
            
            // no optimization can take place if there are not enough placements
            if (plannedPlacements.size() <= 1) {
                return plannedPlacements;
            }

            // if any sort locations are now empty, skip the optimization
            if (plannedPlacements.stream().filter(p -> {return sortLocator.getLocation(p) == null;}).count() != 0) {
                Logger.debug("Optimization skipped because not all placements provide locations");
                return plannedPlacements;
            }
            
            // a) get the heads current location as starting point
            // all nozzles are expected to be mounted on the same head so using
            // any nozzle as reference shall provide the same head location.
            Nozzle nozzle = plannedPlacements.get(0).nozzle;
            start = sortLocator.convertToHeadLocation(nozzle, nozzle.getLocation());
            
            // b) calculate end location as center between all locations of the next step
            Location endLocation = calcCenterLocation(plannedPlacements, endLocator);
            
            // c) sort PlanndPlacements according to sortLocation
            // Use a traveling salesman algorithm to optimize the path to visit the placements
            // FIXME: use a more realistic metric then just the distance between points to
            //        rate possible solutions. On a physical machine one axis is usually stronger
            //        and faster then the other. That means that the optimal solution might be
            //        a longer path on one axis compared to the other.
            TravellingSalesman<PlannedPlacement> tsm = new TravellingSalesman<>(
                    plannedPlacements, 
                    sortLocator,
                    start,
                    endLocation);
            
            // read distance before optimization
            double distance_ref = tsm.getTravellingDistance();
            
            // Solve it using the default heuristics.
            tsm.solve();
            
            double distance_optimized = tsm.getTravellingDistance();
            
            // set new order of placements
            optimizedPlannedPlacements = tsm.getTravel();
            
            double optimization_advantage = Math.max(100 * (1 - distance_optimized / distance_ref), 0);
            final DecimalFormat df = new DecimalFormat("0.0");
            
            Logger.debug("Optimization for {} completed in {}ms: {}, {}% gain", sortLocator.toString(), (System.currentTimeMillis() - t), optimizedPlannedPlacements, df.format(optimization_advantage));
            
            return optimizedPlannedPlacements;
        }
    }

    /**
     * Calculate the center location between a given set of PlannedPlacements using the given locator.
     * 
     * @param plannedPlacements
     * @param locator
     * @return
     */
    private static Location calcCenterLocation(List<PlannedPlacement> plannedPlacements, Locator locator) {
        Location centerLocation = null;
        if (locator != null) {
            centerLocation = new Location(LengthUnit.Millimeters);
            int cnt = 0;
            for (PlannedPlacement p : plannedPlacements) {
                Location l = locator.getLocation(p);
                if (l != null) {
                    centerLocation = centerLocation.add(l);
                    cnt++;
                }
            }
            
            if (cnt > 0) {
                centerLocation = centerLocation.multiply(1.0 / cnt);
            } else {
                centerLocation = null;
            }
        }
        
        return centerLocation;
    }
    
    /**
     * This class groups a step to prerotate all nozzles
     */
    protected abstract class PrerotateAllNozzlesStep implements Step {
        final List<PlannedPlacement> plannedPlacements;
        protected PrerotateAllNozzlesStep(List<PlannedPlacement> plannedPlacements) {
            this.plannedPlacements = plannedPlacements;
        }
        
        // prerorate all nozzles using a subordinate movement using the given locator
        protected void prerotateAllNozzles(TravellingSalesman.Locator<PlannedPlacement> locator) {
            // if pre-rotation is disabled, do nothing
            if (!preRotateAllNozzles) {
                return;
            }

            for (PlannedPlacement p : plannedPlacements) {
                Location l = locator.getLocation(p);
                if (l != null) {
                    double speed = p.nozzle.getHead().getMachine().getSpeed();
                    try {
                        // this movement sequence is of type subordinate and the location itself
                        // will be defined by the next movement segments not of subordinate type.
                        // the rotation of nozzles unrelated to that segment will survive and
                        // result in rotating this nozzles to the requested angle.
                        head.moveTo(p.nozzle, l, speed, MotionOption.SubordinateRotation);
                    } catch (Exception e) {
                        // ignore any errors
                    }
                }
            }
        
            Logger.debug("All nozzle pre-rotation for {} requested", locator.toString());
        }
    }
        
    /**
     * Create a class to group all pick, align an placement locator functions and to get rid of
     * the lengthy "TravellingSalesman.Locator<PlannedPlacement>"
     * 
     * This class uses the nozzles toHeadLocation() method to transform a location
     * into the reference space of the head and apply angular offsets of the nozzle,
     * which depend on the configured nozzle rotation mode. The location (x, y, z) 
     * is always correct, but the angle only if the nozzle rotation mode was applied
     * before.
     */
    protected abstract class Locator implements TravellingSalesman.Locator<PlannedPlacement> {
        /**
         * This method is called to query the approximate location of the head for a given placement.
         * The location is in the reference system of the head in order to compare it with others
         * or to apply math like calculating the center between them.
         */
        abstract public Location getLocation(JobPlacement jobPlacement, Nozzle nozzle);
        public Location getLocation(PlannedPlacement plannedPlacement) {
            final Nozzle nozzle = plannedPlacement.nozzle;
            final JobPlacement jobPlacement = plannedPlacement.jobPlacement;
            return getLocation(jobPlacement, nozzle);
        }
        
        /**
         * toString is used in log messages to generate meaningful messages where it locator as been used.
         */
        abstract public String toString();
        
        /**
         * Return the location of the head when the headmountable hm is at location ref.
         * This method is used to convert locations, calculated for eg. a nozzle to a head
         * location to return it via getLocation() above.
         */
        protected Location convertToHeadLocation(HeadMountable hm, Location ref) {
            Location location;
        
            if (ref == null) {
                location = null;
            }
            else {
                try {
                    location = hm.toHeadLocation(ref, LocationOption.Quiet);
                } catch (Exception e) {
                    location = null;
                }
            }
            
            return location;
        }
    }
    
    private class PickLocator extends Locator {
        public Location getLocation(JobPlacement jobPlacement, Nozzle nozzle) {
            Location location;
            final Placement placement = jobPlacement.getPlacement();
            final Part part = placement.getPart();

            // try to get the location where the alignment will take place
            try {
                final Feeder feeder = findFeeder(machine, part);

                location = feeder.getPickLocation();
            } catch (Exception e) {
                // ignore exceptions
                location = null;
            }
            
            return convertToHeadLocation(nozzle, location);
        }
        
        public String toString() {
            return "pick";
        }
    }
    
    private class AlignLocator extends Locator {
        public Location getLocation(JobPlacement jobPlacement, Nozzle nozzle) {
            Location location;
            final Placement placement = jobPlacement.getPlacement();
            final BoardLocation boardLocation = jobPlacement.getBoardLocation();
            final Part part = placement.getPart();

            final PartAlignment partAlignment = AbstractPartAlignment.getPartAlignment(part);
            
            if (partAlignment == null) {
                location = null;
            } else {
                // try to get the location where the alignment will take place
                try {
                    location = VisionUtils.getPartAlignmentLocation(
                            partAlignment,
                            part,
                            boardLocation,
                            placement, nozzle);
                }
                catch (Exception e) {
                    // ignore exceptions
                    location = null;
                }
            }

            return convertToHeadLocation(nozzle, location);
        }

        public String toString() {
            return "alignment";
        }
    }
    
    private class PlaceLocator extends Locator {
        // this version returns an approximative location without bottom vision - used for N+1 nozzle optimization
        public Location getLocation(JobPlacement jobPlacement, Nozzle nozzle) {
            final Placement placement = jobPlacement.getPlacement();
            final BoardLocation boardLocation = jobPlacement.getBoardLocation();
        
            Location location = Utils2D.calculateBoardPlacementLocation(boardLocation,
                    placement.getLocation());
        
            // convert location to where the head will move to to place the part
            return convertToHeadLocation(nozzle, location);
        }

        // this version takes possible bottom vision results into account - used for eg. nozzle pre-rotation
        public Location getLocation(PlannedPlacement p) {
            final Nozzle nozzle = p.nozzle;
        
            // get the place location using the same method as the place step of the job planner
            Location location = getPlacementLocation(p);
        
            // convert location to where the head will move to to place the part
            return convertToHeadLocation(nozzle, location);
        }
        
        public String toString() {
            return "place";
        }
    }
    
    protected abstract class PlannedPlacementStep implements Step {
        protected List<PlannedPlacement> plannedPlacements;
        private Set<PlannedPlacement> completed = new HashSet<>();
        
        protected PlannedPlacementStep(List<PlannedPlacement> plannedPlacements) {
            this.plannedPlacements = plannedPlacements;
        }
        
        /**
         * Process the step for the given planned placement. The method should perform everything
         * that needs to be done with that planned placement before returning. If there is an
         * error that must be handled by the user in real time the method may throw
         * PnpJobProcessorException. If there is an unrecoverable error with a placement
         * the method should setError() on the JobPlacement. 
         * 
         * @param plannedPlacement The plannedPlacement to process, or null if there are no more
         * to process. Null is a special case which means "Return the next step."
         * @return
         * @throws JobProcessorException
         */
        protected abstract Step stepImpl(PlannedPlacement plannedPlacement) throws JobProcessorException;

        /**
         * Find the next uncompleted, non-errored PlannedPlacement and pass it to stepImpl. If stepImpl
         * completes without error the PlannedPlacement is marked complete and control is returned
         * to the caller.  
         */
        public Step step() throws JobProcessorException {
            /**
             * Get the first planned placement from the list that is still in processing status
             * and that is not marked completed.
             */
            PlannedPlacement plannedPlacement = plannedPlacements
                    .stream()
                    .filter(p -> {
                        return p.jobPlacement.getStatus() == Status.Processing;
                    })
                    .filter(p -> {
                        return !completed.contains(p);
                    })
                    .findFirst()
                    .orElse(null);
            try {
                Step result = stepImpl(plannedPlacement);
                completed.add(plannedPlacement);
                return result;
            }
            catch (JobProcessorException e) {
                switch (plannedPlacement.jobPlacement.getPlacement().getEffectiveErrorHandling(job)) {
                    case Alert:
                        throw e;
                    case Defer:
                        if (e.isInterrupting()) {
                            throw e;
                        }
                        plannedPlacement.jobPlacement.setError(e);
                        return this;
                    default:
                        throw new Error("Unhandled Error Handling case " + plannedPlacement.jobPlacement.getPlacement().getErrorHandling());
                }
            }
        }
    }
    
    /**
     * A very simple planner that processes the job placements in the other they are specified
     * and does not support nozzle tip changes. The planner will return placements that work
     * with the loaded nozzle tips until none are left, and then the job will end.
     */
    @Root
    public static class TrivialPnpJobPlanner implements PnpJobPlanner {
        // this methods are no used here and have to be present because they are required at interface level
        public Strategy getStrategy() {
            return Strategy.Minimize;
        }
        public void setStrategy(Strategy strategy) {
        }
        public void restart() {
        }
        
        @Override
        public List<PlannedPlacement> plan(Head head, List<JobPlacement> jobPlacements, List<NozzleTip> nozzleTips) {
            /**
             * Create a List<PlannedPlacement> that we will fill up and then return.
             */
            List<PlannedPlacement> plannedPlacements = new ArrayList<>();
            
            /**
             * Loop over each nozzle in the head and assign a placement to it.
             */
            for (Nozzle nozzle : head.getNozzles()) {
                /**
                 * If the nozzle does not have a nozzle tip attached then we won't process it. We
                 * could choose to specify a nozzle tip change, but for the purpose of this simple
                 * example we assume the user only wants to process using the currently loaded
                 * nozzle tips.
                 */
                if (nozzle.getNozzleTip() == null) {
                    continue;
                }
                
                /**
                 * If there are no more placements to process then we're done, so exit the loop.
                 */
                if (jobPlacements.isEmpty()) {
                    break;
                }
                
                /**
                 * Loop through the remaining job placements and find the first one that is
                 * compatible with the nozzle and nozzle tip. Note that we use an Iterator here,
                 * instead of the normal for each loop. The reason is that we need to remove
                 * the job placement later in the loop, and Java does not support removing an
                 * item from a list while it's being stepped through. The iterator has a special
                 * method of Iterator.remove() which allows this.
                 */
                for (Iterator<JobPlacement> iterator = jobPlacements.iterator(); iterator.hasNext(); ) {
                    /**
                     * Get the next JobPlacement from the Iterator.
                     */
                    JobPlacement jobPlacement = iterator.next();
                    
                    /**
                     * Assign some local temporary variables to make the code below easier to read. 
                     */
                    Placement placement = jobPlacement.getPlacement();
                    Part part = placement.getPart();
                    org.openpnp.model.Package packag = part.getPackage();
                    NozzleTip nozzleTip = nozzle.getNozzleTip();
                    
                    /**
                     * Check if the job placemen't package is compatible with the nozzle tip
                     * attached to this nozzle.
                     */
                    if (packag.getCompatibleNozzleTips().contains(nozzleTip)) {
                        /**
                         * It's compatible, so create a PlannedPlacement which is a holder for a 
                         * nozzle, nozzle tip and a job placement.
                         */
                        PlannedPlacement plannedPlacement = new PlannedPlacement(nozzle, nozzle.getNozzleTip(), jobPlacement);
                        
                        /**
                         * Store it in the results.
                         */
                        plannedPlacements.add(plannedPlacement);
                        
                        /**
                         * And remove the job placement from the list. This ensures we don't process
                         * the same one again later.
                         */
                        iterator.remove();
                        
                        /**
                         * And exit the loop, because we are done with this nozzle.
                         */
                        break;
                    }
                }
            }
            
            /**
             * Return the results
             */
            return plannedPlacements;
        }

        @Override
        public List<PlannedPlacement> sort(List<PlannedPlacement> plannedPlacements) {
            return plannedPlacements;
        }
    }    
    
    /**
     * A simple two-pass planner which tries to fill each nozzle with a placement on
     * each cycle while minimizing nozzle tip changes.
     * 
     * The first pass tries to find a placement for each nozzle which will not require a
     * nozzle tip change.
     * 
     * The second pass allows nozzle tip changes while respecting any already used nozzle
     * tips for the cycle.
     */
    @Root
    public static class SimplePnpJobPlanner implements PnpJobPlanner {
        @Attribute(required = false)
        protected Strategy strategy = Strategy.Minimize;
        
        private boolean restart;
        
        @Override
        public Strategy getStrategy() {
            return strategy;
        }
        
        @Override
        public void setStrategy(Strategy strategy) {
            this.strategy = strategy;
        }

        @Override
        public void restart() {
            this.restart = true;
        }
        
        @Override
        public List<PlannedPlacement> plan(Head head, List<JobPlacement> jobPlacements, List<NozzleTip> nozzleTips) {
            /**
             * Create an empty List<PlannedPlacement> which will hold the results.
             */
            List<PlannedPlacement> plannedPlacements = new ArrayList<>();
            
            /**
             * Get a list of all the nozzles. We make a copy of the list so that we can modify
             * it within this function without modifying the machine. This makes the logic below
             * easier. As we plan a nozzle we'll remove it from the list until none are left.
             */
            List<Nozzle> nozzles = new ArrayList<>(head.getNozzles());
            
            /**
             * Same as above, except for NozzleTips.
             * Only fill the list with all available nozzle tips, if the input is empty
             */
            if (nozzleTips == null || nozzleTips.isEmpty()) {
                nozzleTips = new ArrayList<>(head.getMachine().getNozzleTips());
            }
            
            if (    strategy == Strategy.Minimize
                || (strategy == Strategy.StartAsPlanned && !restart)) {
                /**
                 * First we plan any placements that can be done without a nozzle change. For each
                 * nozzle we see if there is a placement that we can handle without doing a nozzletip
                 * change. If there is, we remove the nozzle, nozzle tip and job placement from their
                 * respective lists so that we don't plan the same one again.
                 */
                for (Nozzle nozzle : new ArrayList<>(nozzles)) {
                    PlannedPlacement plannedPlacement = planWithoutNozzleTipChange(nozzle, 
                            nozzle.getNozzleTip(), jobPlacements, plannedPlacements);
                    if (plannedPlacement != null) {
                        plannedPlacements.add(plannedPlacement);
                        jobPlacements.remove(plannedPlacement.jobPlacement);
                        nozzles.remove(plannedPlacement.nozzle);
                        nozzleTips.remove(plannedPlacement.nozzleTip);
                    }
                }
            }
            restart = false;
            
            /**
             * Now we'll try to plan any nozzles that didn't get planned on the first pass by
             * seeing if a nozzle tip change helps. This is nearly the same as above, except this
             * time we allow a nozzle tip change to happen.
             */
            for (Nozzle nozzle : new ArrayList<>(nozzles)) {
                PlannedPlacement plannedPlacement = planWithNozzleTipChange(nozzle, jobPlacements, 
                        nozzleTips, plannedPlacements);
                if (plannedPlacement != null) {
                    plannedPlacements.add(plannedPlacement);
                    jobPlacements.remove(plannedPlacement.jobPlacement);
                    nozzles.remove(plannedPlacement.nozzle);
                    nozzleTips.remove(plannedPlacement.nozzleTip);
                }
            }

            /**
             * Finally, we sort any planned placements by the nozzle name so that they are
             * performed in the order of nozzle name. This is not really necessary but some users
             * prefer it that way and it does no harm
             */
            plannedPlacements = sort(plannedPlacements);

            return plannedPlacements;
        }
        
        /**
         * Try to find a planning solution for the given nozzle that does not require
         * a nozzle tip change. This essentially just checks if there are any job placements
         * remaining that are compatible with the currently loaded nozzle tip.
         * @param nozzle
         * @param jobPlacements
         * @return
         */
        protected PlannedPlacement planWithoutNozzleTipChange(Nozzle nozzle, NozzleTip nozzleTip,
                List<JobPlacement> jobPlacements, List<PlannedPlacement>plannedPlacements) {
            if (nozzleTip == null) {
                return null;
            }
            // collect all placements that are compatible with the loaded nozzle tip
            List <JobPlacement> compatibleJobPlacements = jobPlacements
                    .stream()
                    .filter(jobPlacement -> {
                        Placement placement = jobPlacement.getPlacement();
                        Part part = placement.getPart();
                        org.openpnp.model.Package pkg = part.getPackage();
                        return pkg.getCompatibleNozzleTips().contains(nozzleTip);
                    })
                    .collect(Collectors.toList());

            // if there are no compatible job placements, leave now
            if (compatibleJobPlacements.isEmpty()) {
                return null;
            }
            
            // if strategy is not Minimize (do best optimization) only consider placements
            // that use the same feeder as the first placement.
            if (strategy != Strategy.Minimize) {
                Machine machine = Configuration.get().getMachine();
                Feeder referenceFeeder = findFeederWithoutException(machine, compatibleJobPlacements.get(0).getPlacement().getPart());

                // if the first/reference placement has no feeder, return just that placement to avoid any unwonted optimization
                if (referenceFeeder == null) {
                    compatibleJobPlacements.subList(1, compatibleJobPlacements.size()).clear();
                }
                else {
                    // now filter compatible job placements for same feeder as reference
                    compatibleJobPlacements = compatibleJobPlacements
                            .stream()
                            .filter(jobPlacement -> {
                                Feeder feeder = findFeederWithoutException(machine, jobPlacement.getPlacement().getPart());
                                return feeder != null && feeder.equals(referenceFeeder);
                            })
                            .collect(Collectors.toList());
                }
            }
            
            // if strategy is not FullyAsPlanned (no optimization at all) and if other placements 
            // have been planned, sort compatible placements by distance to pick and place location
            JobPlacement bestPlacement = null;
            if (strategy != Strategy.FullyAsPlanned
                && plannedPlacements != null && !plannedPlacements.isEmpty()
                && compatibleJobPlacements.size() > 1) {
                Location averagePickLocation  = calcCenterLocation(plannedPlacements, pickLocator);
                Location averagePlaceLocation = calcCenterLocation(plannedPlacements, placeLocator);
                
                // find the placement with the lowest cost to averagePickLocation and averagePlaceLocation.
                double bestCost = Double.MAX_VALUE;
                for (JobPlacement p : compatibleJobPlacements) {
                    double cost = MotionUtils.getMotionCost(pickLocator.getLocation(p, nozzle).subtract(averagePickLocation))
                                + MotionUtils.getMotionCost(placeLocator.getLocation(p, nozzle).subtract(averagePlaceLocation));

                    // if this placement is closest with respect to its pick and place
                    if (bestCost > cost) {
                        bestCost = cost;
                        bestPlacement = p;
                    }
                }
            }
            else {
                // no further optimization possible or requested, just choose the first of the list
                bestPlacement = compatibleJobPlacements.get(0);
            }
            
            // return the first of the list
            return new PlannedPlacement(nozzle, nozzleTip, bestPlacement);
        }

        /**
         * Variant of findFeeder() that consumes exceptions by returning NULL
         * @param part
         * @return
         */
        protected Feeder findFeederWithoutException(Machine machine, Part part) {
            Feeder feeder;
            try {
                feeder = findFeeder(machine, part);
            }
            catch (Exception e) {
                feeder = null;
            }
            
            return feeder;
        }
        
        /**
         * Try to find a planning solution that allows for a nozzle tip change. This is very
         * similar to planWithoutNozzleTipChange() except that it considers all available nozzle
         * tips on the machine that are compatible with both the nozzle and the placement, 
         * instead of just the one that is loaded.
         * @param nozzle
         * @param jobPlacements
         * @param nozzleTips
         * @return
         */
        protected PlannedPlacement planWithNozzleTipChange(Nozzle nozzle, 
                List<JobPlacement> jobPlacements,
                List<NozzleTip> nozzleTips, List<PlannedPlacement> plannedPlacements) {
            for (JobPlacement jobPlacement : jobPlacements) {
                Placement placement = jobPlacement.getPlacement();
                Part part = placement.getPart();
                org.openpnp.model.Package pkg = part.getPackage();
                // Get the intersection of nozzle tips that are not yet used, are compatible with
                // the package, and are compatible with the nozzle.
                List<NozzleTip> goodNozzleTips = nozzleTips
                        .stream()
                        .filter(nozzleTip -> {
                            return pkg.getCompatibleNozzleTips().contains(nozzleTip);
                        })
                        .filter(nozzleTip -> {
                            return nozzle.getCompatibleNozzleTips().contains(nozzleTip);
                        })
                        .collect(Collectors.toList());
                if (!goodNozzleTips.isEmpty()) {
                    // plan again with the selected nozzle tip using the same strategy as without nozzle tip change
                    return planWithoutNozzleTipChange(nozzle, goodNozzleTips.get(0), jobPlacements, plannedPlacements);
                }
            }
            return null;
        }
        
        @Override
        public List<PlannedPlacement> sort(List<PlannedPlacement> plannedPlacements) {
            /**
             * Sort any planned placements by the nozzle name so that they are
             * performed in the order of nozzle name. This is not really necessary but some users
             * prefer it that way and it does no harm
             */
            plannedPlacements.sort(Comparator.comparing(plannedPlacement -> {
                return plannedPlacement.nozzle.getName();
            }));
            return plannedPlacements;
        }
    }
}
