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

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.vision.AbstractPartAlignment;
import org.openpnp.machine.reference.wizards.ReferencePnpJobProcessorConfigurationWizard;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Job;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.PanelLocation;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.FiducialLocator;
import org.openpnp.spi.Head;
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
import org.openpnp.util.Utils2D;
import org.openpnp.util.VisionUtils;
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
        PartHeight,
        Part
    }

    @Attribute(required = false)
    protected JobOrderHint jobOrder = JobOrderHint.PartHeight;

    @Attribute(required = false)
    protected int maxVisionRetries = 3;
    
    @Attribute(required = false)
    boolean steppingToNextMotion = true;

    @Attribute(required = false)
    boolean optimizeMultipleNozzles = true;

    @Element(required = false)
    public PnpJobPlanner planner = new SimplePnpJobPlanner();

    protected Job job;

    protected Machine machine;

    protected Head head;

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
            
            checkSetupErrors();
            
            prepMachine();
            
            prepFeeders();
            
            scriptJobStarting();

            return new PanelFiducialCheck();
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
            validatePartNozzleTip(head, placement.getPart());

            // Make sure there is at least one compatible and enabled feeder available
            findFeeder(machine, placement.getPart());
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
                                feederVisitList.add(feeder);
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
    
    protected class PanelFiducialCheck implements Step {
        public Step step() throws JobProcessorException {
            FiducialLocator locator = Configuration.get().getMachine().getFiducialLocator();
            
            for (PanelLocation panelLocation : job.getPanelLocations()) {
                if (!panelLocation.isEnabled()) {
                    continue;
                }
                if (!panelLocation.isCheckFiducials()) {
                    continue;
                }
                fireTextStatus("Panel fiducial check on %s", panelLocation);
                try {
                    locator.locatePlacementsHolder(panelLocation);
                }
                catch (Exception e) {
                    throw new JobProcessorException(panelLocation, e);
                }
            }
            return new BoardLocationFiducialCheck();
        }
    }
    
    protected class BoardLocationFiducialCheck implements Step {
        protected Set<BoardLocation> completed = new HashSet<>();
        
        public Step step() throws JobProcessorException {
            FiducialLocator locator = Configuration.get().getMachine().getFiducialLocator();

            for (BoardLocation boardLocation : job.getBoardLocations()) {
                if (!boardLocation.isEnabled()) {
                    continue;
                }
                if (!boardLocation.isCheckFiducials()) {
                    continue;
                }
                if (completed.contains(boardLocation)) {
                    continue;
                }
                
                fireTextStatus("Fiducial check for %s", boardLocation);
                try {
                    locator.locatePlacementsHolder(boardLocation);
                }
                catch (Exception e) {
                    throw new JobProcessorException(boardLocation, e);
                }
                
                completed.add(boardLocation);
                return this;
            }
            
            return new Plan();
        }
    }

    protected class Plan implements Step {
        public Step step() throws JobProcessorException {
            fireTextStatus("Planning placements.");

            List<JobPlacement> jobPlacements;

            if (jobOrder.equals(JobOrderHint.Part)) {
                // Get the list of unfinished placements and sort them by part.
                    jobPlacements = getPendingJobPlacements().stream()
                            .sorted(Comparator.comparing(JobPlacement::getPartId))
                            .collect(Collectors.toList());
            } 
            else {
                // Get the list of unfinished placements and sort them by part height.
                    jobPlacements = getPendingJobPlacements().stream()
                            .sorted(Comparator
                                .comparing(JobPlacement::getPartHeight)
                                .thenComparing(JobPlacement::getPartId))
                            .collect(Collectors.toList());
            }

            if (jobPlacements.isEmpty()) {
                return new Finish();
            }

            long t = System.currentTimeMillis();
            List<PlannedPlacement> plannedPlacements = planner.plan(head, jobPlacements);
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
                Logger.debug("No nozzle tip change needed for nozzle {}", nozzle);
                return this;
            }
            
            fireTextStatus("Change nozzle tip on nozzle %s to %s.", 
                    nozzle.getName(), 
                    nozzleTip.getName());
            try {
                nozzle.unloadNozzleTip();
                nozzle.loadNozzleTip(nozzleTip);
            }
            catch (Exception e) {
                throw new JobProcessorException(nozzleTip,  e);
            }
            
            return this;
        }
    }
    
    protected class CalibrateNozzleTips extends PlannedPlacementStep {
        public CalibrateNozzleTips(List<PlannedPlacement> plannedPlacements) {
            super(plannedPlacements);
        }
        
        @Override
        public Step stepImpl(PlannedPlacement plannedPlacement) throws JobProcessorException {
            if (plannedPlacement == null) {
                return new Pick(plannedPlacements);
            }
            
            final NozzleTip nozzleTip = plannedPlacement.nozzle.getNozzleTip();
            
            if (nozzleTip == null) {
                return this;
            }
            
            if (plannedPlacement.nozzle.isCalibrated()) {
                return this;
            }
            
            fireTextStatus("Calibrate nozzle tip %s", nozzleTip);
            try {
                plannedPlacement.nozzle.calibrate();
            }
            catch (Exception e) {
                throw new JobProcessorException(nozzleTip, e);
            }
            
            return this;
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
        
        // provide a location to allow nozzle path optimization
        @Override
        public Location getLocation(PlannedPlacement plannedPlacement) {
            // FIXME: optimizing the pick step is more difficult then other steps:
            //        the location the action is start/take place depends on the
            //        type of feeder and there is a path between feed and pick, that
            //        is not considered here. For now, we'll just use the pick location.
            return getPickLocation(plannedPlacement);
        }

        // provide a next location: use the location of the first part on the board
        @Override
        public Location getNextLocation(List<PlannedPlacement>plannedPlacements) {
            return getAlignLocation(plannedPlacements.get(0));
        }

        @Override
        public Step stepImpl(PlannedPlacement plannedPlacement) throws JobProcessorException {
            if (plannedPlacement == null) {
                return new Align(plannedPlacements);
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
            for (int i = 0; i < 1 + feeder.getFeedRetryCount(); i++) {
                try {
                    fireTextStatus("Feed %s on %s.", feeder.getName(), feeder.getPart().getId());
                    
                    feeder.feed(nozzle);
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
                    throw new JobProcessorException(nozzle, "Part vacuum-detected on nozzle before pick.");
                }
            }
            catch (JobProcessorException e) {
                throw e;
            }
            catch (Exception e) {
                throw new JobProcessorException(nozzle, e);
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
            throw new JobProcessorException(feeder, lastException);
        }
        
        private void pick(Nozzle nozzle, Feeder feeder, JobPlacement jobPlacement, Part part) throws JobProcessorException {
            try {
                fireTextStatus("Pick %s from %s for %s.", part.getId(), feeder.getName(),
                        jobPlacement.getPlacement().getId());

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
                throw new JobProcessorException(feeder, e);
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
     * Alignment step - align all parts on all nozzles
     */
    protected class Align extends PlannedPlacementStep {
        public Align(List<PlannedPlacement> plannedPlacements) {
            super(plannedPlacements);
        }

        // provide a location to allow nozzle path optimization
        @Override
        public Location getLocation(PlannedPlacement plannedPlacement) {
            return getAlignLocation(plannedPlacement);
        }
        
        // provide a next location: use the location of the first part on the board
        @Override
        public Location getNextLocation(List<PlannedPlacement>plannedPlacements) {
            return getRawPlaceLocation(plannedPlacements.get(0));
        }
        
        @Override
        public Step stepImpl(PlannedPlacement plannedPlacement) throws JobProcessorException {
            if (plannedPlacement == null) {
                return new Place(plannedPlacements);
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
                fireTextStatus("Aligning %s for %s.", part.getId(), placement.getId());
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
            throw new JobProcessorException(part, lastException);
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
     * Placement step - place all parts on all nozzles on the board
     */
    protected class Place extends PlannedPlacementStep {
        public Place(List<PlannedPlacement> plannedPlacements) {
            super(plannedPlacements);
        }

        // provide a location to allow nozzle path optimization
        @Override
        public Location getLocation(PlannedPlacement plannedPlacement) {
            return getRawPlaceLocation(plannedPlacement);
        }

        // provide a next location: use the location of the first part on the board
        @Override
        public Location getNextLocation(List<PlannedPlacement>plannedPlacements) {
            // get the next planned placements
            List<PlannedPlacement>plannedNextPlacements = planner.planNext(head, jobPlacements);
            
            // if the list of planned next placements is empty, return the park location
            if (plannedNextPlacements.isEmpty()) {
                return head.getParkLocation();
            }
            
            // else use the pick location of the first as next location after this step
            return getPickLocation(plannedNextPlacements.get(0));
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

            Location placementLocation = getPlacementLocation(plannedPlacement);
            
            scriptBeforeAssembly(plannedPlacement, placementLocation);

            checkPartOn(nozzle);
            
            place(nozzle, part, placement, placementLocation);
            
            checkPartOff(nozzle, part);
            
            // Mark the placement as finished
            jobPlacement.setStatus(Status.Complete);
            
            // Mark the placement as "placed"
//            boardLocation.setPlaced(jobPlacement.getPlacement().getId(), true);
            job.storePlacedStatus(boardLocation, jobPlacement.getPlacement().getId(), true);
            
            totalPartsPlaced++;
            
            scriptComplete(plannedPlacement, placementLocation);
            
            return this;
        }
        
        private void place(Nozzle nozzle, Part part, Placement placement, Location placementLocation) throws JobProcessorException {
            fireTextStatus("Placing %s for %s.", part.getId(), placement.getId());
            
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
        
        private void checkPartOn(Nozzle nozzle) throws JobProcessorException {
            if (!nozzle.isPartOnEnabled(Nozzle.PartOnStep.BeforePlace)) {
                return;
            }
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
        
        private void checkPartOff(Nozzle nozzle, Part part) throws JobProcessorException {
            if (!nozzle.isPartOffEnabled(Nozzle.PartOffStep.AfterPlace)) {
                return;
            }
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
        
        private void scriptBeforeAssembly(PlannedPlacement plannedPlacement, Location placementLocation) throws JobProcessorException {
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
                params.put("alignmentOffsets", plannedPlacement.alignmentOffsets);
                Configuration.get().getScripting().on("Job.Placement.BeforeAssembly", params);
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
        
        private Location getPlacementLocation(PlannedPlacement plannedPlacement) {
            final JobPlacement jobPlacement = plannedPlacement.jobPlacement;
            final Placement placement = jobPlacement.getPlacement();
            final Part part = placement.getPart();
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

    protected abstract class PlannedPlacementStep implements Step {
        protected final List<PlannedPlacement> plannedPlacements;
        private Set<PlannedPlacement> completed = new HashSet<>();
        
        protected PlannedPlacementStep(List<PlannedPlacement> plannedPlacements) {
            // sort placements order for better performance
            if (plannedPlacements.size() > 1 && optimizeMultipleNozzles) {
                plannedPlacements = sortPlacements(plannedPlacements);
            }
        
            this.plannedPlacements = plannedPlacements;
        }

        /**
         * Sort the list of planned placements for better performance
         * this is done by first collecting the locations where the head will move
         * to when executing this step and then using a traveling salesman to 
         * optimize the list.
         * 
         * Steps that support optimization, have to implement getLocation() to
         * allow the optimizer to read the location the head will move to to
         * perform the step for the placement.
         * 
         * In addition, steps my implement getNextLocation() to allow the
         * optimizer to get a location where the head will move to after this
         * step has been executed for all placements.
         * 
         * @param plannedPlacements
         * @return
         */
        private List<PlannedPlacement> sortPlacements(List<PlannedPlacement> plannedPlacements) {
            long t = System.currentTimeMillis();
            Location start; // start location of traveling salesman, current location of the head
            Location end;   // end location of traveling salesman, location of the next step
        
            // a) collect data: sortLocation, start and end point
            for (PlannedPlacement plannedPlacement : plannedPlacements) {
                plannedPlacement.sortLocation = getLocation(plannedPlacement);
            }
        
            // if all sort locations are now empty, revert the list to its original order
            if (plannedPlacements.stream().filter(p -> {return p.sortLocation != null;}).count() == 0) {
                plannedPlacements.sort(Comparator.comparing(plannedPlacement -> {
                    return plannedPlacement.nozzle.getName();
                }));
                
            } else {
                
                // b) get the heads current location as starting point
                // all nozzles are expected to be mounted on the same head so using
                // any nozzle as reference shall provide the same head location.
                start = getHeadLocation(plannedPlacements.get(0).nozzle);
                    
                // c) get the location the next step will take place as end point
                end = getNextLocation(plannedPlacements);
                
                // c) sort PlanndPlacements according to sortLocation
                // Use a traveling salesman algorithm to optimize the path to visit the placements
                // FIXME: use a more realistic metric then just the distance between points to
                //        rate possible solutions. On a physical machine one axis is usually stronger
                //        and faster then the other. That means that the optimal solution might be
                //        a longer path on one axis compared to the other.
                TravellingSalesman<PlannedPlacement> tsm = new TravellingSalesman<>(
                        plannedPlacements, 
                        new TravellingSalesman.Locator<PlannedPlacement>() { 
                            @Override
                            public Location getLocation(PlannedPlacement locatable) {
                                return locatable.sortLocation;
                            }
                        }, 
                        start,
                        end);
                
                // Solve it using the default heuristics.
                tsm.solve();
                
                // set new order of placements
                plannedPlacements = tsm.getTravel();
                
                Logger.debug("Optimization completed in {}ms: {}", (System.currentTimeMillis() - t), plannedPlacements);
            }
            
            return plannedPlacements;
        }
	    
        /**
         * Return the location the head will move to when executing this step
         * for this placement.
         * The location is used to optimize the head movement.
         * 
         * @param plannedPlacement
         * @return
         */
        protected Location getLocation(PlannedPlacement plannedPlacement) {
            return null;
        }
        
        /**
         * Return the location the head will move to after this step has been
         * executed for all placements. The location my be approximative assuming
         * the distance between nozzles is small compared to the distance to that
         * location.
         * The location is used to optimize the head movement.
         * 
         * @param plannedPlacement
         * @return
         */
        protected Location getNextLocation(List<PlannedPlacement> plannedPlacements) {
            return null;
        }
        
        /**
         * Get the location of the head the nozzle is on
         */
        public Location getHeadLocation(Nozzle nozzle)
        {
            Location location;
            
            try {
                // this translates the nozzles current location into the location of the head
                location = nozzle.toHeadLocation(nozzle.getLocation());
            } catch (Exception e) {
                location = null;
            }
            
            return location;
        }
        
        /**
         * Get the location the the part for this placement will be picked from.
         * This is used to generate locations for the optimizer.
         */
        public Location getPickLocation(PlannedPlacement plannedPlacement) {
            Location location;
            final Nozzle nozzle = plannedPlacement.nozzle;
            final JobPlacement jobPlacement = plannedPlacement.jobPlacement;
            final Placement placement = jobPlacement.getPlacement();
            final Part part = placement.getPart();

            // try to get the location where the alignment will take place
            try {
                final Feeder feeder = findFeeder(machine, part);

                location = nozzle.toHeadLocation(feeder.getPickLocation());
            } catch (Exception e) {
                // ignore exceptions
                location = null;
            }
            
            return location;
        }
        
        /**
         * Get the location where the alignment will take place.
         * This is used to generate locations for the optimizer.
         */
        public Location getAlignLocation(PlannedPlacement plannedPlacement) {
            Location location;
            final Camera camera;
            final Nozzle nozzle = plannedPlacement.nozzle;

            // try to get the location where the alignment will take place
            try {
                camera = VisionUtils.getBottomVisionCamera();
                location = nozzle.toHeadLocation(camera.getLocation());
            } catch (Exception e) {
                // ignore exceptions
                location = null;
            }
            
            return location;
        }
        
        /**
         * Get the raw/uncorrected location of the placement on the board.
         * This is used to generate locations for the optimizer.
         */
        public Location getRawPlaceLocation(PlannedPlacement plannedPlacement) {
            final JobPlacement jobPlacement = plannedPlacement.jobPlacement;
            final Placement placement = jobPlacement.getPlacement();
            final BoardLocation boardLocation = plannedPlacement.jobPlacement.getBoardLocation();
        
            Location location = Utils2D.calculateBoardPlacementLocation(boardLocation,
                    placement.getLocation());
            
            return location;
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
                switch (plannedPlacement.jobPlacement.getPlacement().getErrorHandling()) {
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
        @Override
        public List<PlannedPlacement> plan(Head head, List<JobPlacement> jobPlacements) {
            return planDo(head, jobPlacements, true);
        }

        @Override
        public List<PlannedPlacement> planNext(Head head, List<JobPlacement> jobPlacements) {
            return planDo(head, jobPlacements, false);
        }

        private List<PlannedPlacement> planDo(Head head, List<JobPlacement> jobPlacements, boolean removeFromList) {
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
                        
                        if (removeFromList) {
                            /**
                             * And remove the job placement from the list. This ensures we don't process
                             * the same one again later.
                             */
                            iterator.remove();
                        }
                        
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
        @Override
        public List<PlannedPlacement> plan(Head head, List<JobPlacement> jobPlacements) {
            return planDo(head, jobPlacements, true);
        }
        
        /**
         * Plan the next placement by looking ahead. This is used to optimize the head movement
         * in the place step by using the pick location of the placement as end point.
         * 
         * The code is identical to plan(), except that the planned placements are not removed.
         * 
         * @param head
         * @param jobPlacements
         * @return
         */
        @Override
        public List<PlannedPlacement> planNext(Head head, List<JobPlacement> jobPlacements) {
            return planDo(head, jobPlacements, false);
        }

        /**
         * Execute the placement planning. This method is used my plan() to really plan placements and
         * planNext() to look ahead what will be the next placement.
         *
         * @param head
         * @param jobPlacements
         * @return
         */
        private List<PlannedPlacement> planDo(Head head, List<JobPlacement> jobPlacements, boolean removeFromLists) {
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
             */
            List<NozzleTip> nozzleTips = new ArrayList<>(head.getMachine().getNozzleTips());
            
            /**
             * First we plan any placements that can be done without a nozzle change. For each
             * nozzle we see if there is a placement that we can handle without doing a nozzletip
             * change. If there is, we remove the nozzle, nozzle tip and job placement from their
             * respective lists so that we don't plan the same one again.
             */
            for (Nozzle nozzle : new ArrayList<>(nozzles)) {
                PlannedPlacement plannedPlacement = planWithoutNozzleTipChange(nozzle, jobPlacements);
                if (plannedPlacement != null) {
                    plannedPlacements.add(plannedPlacement);
                    if (removeFromLists) {
                        jobPlacements.remove(plannedPlacement.jobPlacement);
                        nozzles.remove(plannedPlacement.nozzle);
                        nozzleTips.remove(plannedPlacement.nozzleTip);
                    }
                }
            }
            
            /**
             * Now we'll try to plan any nozzles that didn't get planned on the first pass by
             * seeing if a nozzle change helps. This is nearly the same as above, except this
             * time we allow a nozzle tip change to happen.
             */
            for (Nozzle nozzle : new ArrayList<>(nozzles)) {
                PlannedPlacement plannedPlacement = planWithNozzleTipChange(nozzle, jobPlacements, nozzleTips);
                if (plannedPlacement != null) {
                    plannedPlacements.add(plannedPlacement);
                    if (removeFromLists) {
                        jobPlacements.remove(plannedPlacement.jobPlacement);
                        nozzles.remove(plannedPlacement.nozzle);
                        nozzleTips.remove(plannedPlacement.nozzleTip);
                    }
                }
            }

            /**
             * Finally, we sort any planned placements by the nozzle name so that they are
             * performed in the order of nozzle name. This is not really necessary but some users
             * prefer it that way and it does no harm
             */
            plannedPlacements.sort(Comparator.comparing(plannedPlacement -> {
                return plannedPlacement.nozzle.getName();
            }));

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
        protected PlannedPlacement planWithoutNozzleTipChange(Nozzle nozzle, 
                List<JobPlacement> jobPlacements) {
            if (nozzle.getNozzleTip() == null) {
                return null;
            }
            for (JobPlacement jobPlacement : jobPlacements) {
                Placement placement = jobPlacement.getPlacement();
                Part part = placement.getPart();
                org.openpnp.model.Package pkg = part.getPackage();
                NozzleTip nozzleTip = nozzle.getNozzleTip();
                if (pkg.getCompatibleNozzleTips().contains(nozzleTip)) {
                    return new PlannedPlacement(nozzle, nozzleTip, jobPlacement);
                }
            }
            return null;
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
                List<NozzleTip> nozzleTips) {
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
                    return new PlannedPlacement(nozzle, goodNozzleTips.get(0), jobPlacement);
                }
            }
            return null;
        }
    }
}
