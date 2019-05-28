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
import java.util.Set;
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
import org.openpnp.spi.PnpJobPlanner;
import org.openpnp.spi.PnpJobProcessor.JobPlacement.Status;
import org.openpnp.spi.base.AbstractJobProcessor;
import org.openpnp.spi.base.AbstractPnpJobProcessor;
import org.openpnp.util.Collect;
import org.openpnp.util.MovableUtils;
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

    public static class PlannedPlacement {
        public final JobPlacement jobPlacement;
        public final Nozzle nozzle;
        public Feeder feeder;
        public PartAlignment.PartAlignmentOffset alignmentOffsets;

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
    protected JobOrderHint jobOrder = JobOrderHint.PartHeight;

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
                    if (boardLocation.getPlaced(placement.getId())) {
                        continue;
                    }

                    // Ignore placements that aren't on the side of the board we're processing.
                    if (placement.getSide() != boardLocation.getSide()) {
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

            // Verify that the part height is greater than zero. Catches a common configuration
            // error.
            if (placement.getPart().getHeight().getValue() <= 0D) {
                throw new JobProcessorException(placement.getPart(), String.format("Part height for %s must be greater than 0.",
                        placement.getPart().getId()));
            }

            // Make sure there is at least one compatible nozzle tip available
            findNozzleTip(head, placement.getPart());

            // Make sure there is at least one compatible and enabled feeder available
            findFeeder(machine, placement.getPart());
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
            
            if (job.isUsingPanel() && job.getPanels().get(0).isCheckFiducials()){
                Panel p = job.getPanels().get(0);
                
                BoardLocation boardLocation = job.getBoardLocations().get(0);
                
                fireTextStatus("Panel fiducial check on %s", boardLocation);
                try {
                    locator.locateBoard(boardLocation, p.isCheckFiducials());
                }
                catch (Exception e) {
                    throw new JobProcessorException(boardLocation, e);
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
                    locator.locateBoard(boardLocation);
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
            List<PlannedPlacement> plannedPlacements = new ArrayList<>();
            
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
                            .sorted(Comparator.comparing(JobPlacement::getPartHeight))
                            .collect(Collectors.toList());
            }

            if (jobPlacements.isEmpty()) {
                return new Finish();
            }

            long t = System.currentTimeMillis();
            List<JobPlacement> result = planner.plan(head, jobPlacements);
            Logger.debug("Planner complete in {}ms: {}", (System.currentTimeMillis() - t), result);

            // Now we have a solution, so apply it to the nozzles and plan the placements.
            for (Nozzle nozzle : head.getNozzles()) {
                // The solution is in Nozzle order, so grab the next one.
                JobPlacement jobPlacement = result.remove(0);
                if (jobPlacement == null) {
                    continue;
                }
                jobPlacement.setStatus(Status.Processing);
                plannedPlacements.add(new PlannedPlacement(nozzle, jobPlacement));
            }
            
            if (plannedPlacements.size() == 0) {
                throw new JobProcessorException(planner, "Planner failed to plan any placements. Please contact support.");
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
            final Part part = plannedPlacement.jobPlacement.getPlacement().getPart();
            
            // If the currently loaded NozzleTip can handle the Part we're good.
            if (nozzle.getNozzleTip() != null && nozzle.getNozzleTip().canHandle(part)) {
                Logger.debug("No nozzle tip change needed for nozzle {}", nozzle);
                return this;
            }
            
            fireTextStatus("Locate nozzle tip on nozzle %s for part %s.", 
                    nozzle.getId(), 
                    part.getId());
            
            // Otherwise find a compatible tip and load it
            NozzleTip nozzleTip = findNozzleTip(nozzle, part);
            fireTextStatus("Change nozzle tip on nozzle %s to %s.", 
                    nozzle.getId(), 
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
            
            if (nozzleTip.isCalibrated()) {
                return this;
            }
            
            fireTextStatus("Calibrate nozzle tip %s", nozzleTip);
            try {
                nozzleTip.calibrate();
            }
            catch (Exception e) {
                throw new JobProcessorException(nozzleTip, e);
            }
            
            return this;
        }
    }

    protected class Pick extends PlannedPlacementStep {
        HashMap<PlannedPlacement, Integer> retries = new HashMap<>();
        
        public Pick(List<PlannedPlacement> plannedPlacements) {
            super(plannedPlacements);
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
            
            final Feeder feeder = findFeeder(machine, part);

            feed(feeder, nozzle);
            
            pick(nozzle, feeder, placement, part);

            try {
                postPick(feeder, nozzle);
                
                checkPartOn(nozzle);
            }
            catch (JobProcessorException e) {
                if (retryIncrementAndGet(plannedPlacement) >= feeder.getPickRetryCount()) {
                    // Clear the retry count because we're about to show the error. If the user
                    // decides to try again we want to do the full retry cycle.
                    retries.remove(plannedPlacement);
                    throw e;
                }
                else {
                    discard(nozzle);
                    return this;
                }
            }
            
            return this;
        }
        
        private int retryIncrementAndGet(PlannedPlacement plannedPlacement) {
            Integer retry = retries.get(plannedPlacement);
            if (retry == null) {
                retry = 0;
            }
            retry++;
            retries.put(plannedPlacement, retry);
            return retry;
        }
        
        private void feed(Feeder feeder, Nozzle nozzle) throws JobProcessorException {
            Exception lastException = null;
            for (int i = 0; i < Math.max(1, feeder.getFeedRetryCount()); i++) {
                try {
                    fireTextStatus("Feed %s on %s.", feeder.getName(), feeder.getPart().getId());
                    
                    feeder.feed(nozzle);
                    return;
                }
                catch (Exception e) {
                    lastException = e;
                }
            }
            throw new JobProcessorException(feeder, lastException);
        }
        
        private void pick(Nozzle nozzle, Feeder feeder, Placement placement, Part part) throws JobProcessorException {
            try {
                fireTextStatus("Pick %s from %s for %s.", part.getId(), feeder.getName(),
                        placement.getId());
                
                // Move to pick location.
                MovableUtils.moveToLocationAtSafeZ(nozzle, feeder.getPickLocation());


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
            if (!nozzle.isPartDetectionEnabled()) {
                return;
            }
            try {
                if(!nozzle.isPartOn()) {
                    throw new JobProcessorException(nozzle, "No part detected after pick.");
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

    protected class Align extends PlannedPlacementStep {
        public Align(List<PlannedPlacement> plannedPlacements) {
            super(plannedPlacements);
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

            final PartAlignment partAlignment = findPartAligner(machine, part);
            
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
            // TODO make retry count configurable.
            for (int i = 0; i < 3; i++) {
                fireTextStatus("Aligning %s for %s.", part.getId(), placement.getId());
                try {
                    plannedPlacement.alignmentOffsets = VisionUtils.findPartAlignmentOffsets(
                            partAlignment,
                            part,
                            boardLocation,
                            placement.getLocation(), nozzle);
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
            if (!nozzle.isPartDetectionEnabled()) {
                return;
            }
            try {
                if(!nozzle.isPartOn()) {
                    throw new JobProcessorException(nozzle, "No part detected after alignment. Part may have been lost in transit.");
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

            Location placementLocation = getPlacementLocation(plannedPlacement);
            
            scriptBeforeAssembly(plannedPlacement, placementLocation);

            checkPartOn(nozzle);
            
            place(nozzle, part, placement, placementLocation);
            
            checkPartOff(nozzle);
            
            // Mark the placement as finished
            jobPlacement.setStatus(Status.Complete);
            
            // Mark the placement as "placed"
            boardLocation.setPlaced(jobPlacement.getPlacement().getId(), true);
            
            totalPartsPlaced++;
            
            scriptComplete(plannedPlacement, placementLocation);
            
            return this;
        }
        
        private void place(Nozzle nozzle, Part part, Placement placement, Location placementLocation) throws JobProcessorException {
            fireTextStatus("Placing %s for %s.", part.getId(), placement.getId());
            
            try {
                // Move to the placement location
                MovableUtils.moveToLocationAtSafeZ(nozzle, placementLocation);

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
            if (!nozzle.isPartDetectionEnabled()) {
                return;
            }
            try {
                if (!nozzle.isPartOn()) {
                    throw new JobProcessorException(nozzle, "No part detected on nozzle before place.");
                }
            }
            catch (JobProcessorException e) {
                throw e;
            }
            catch (Exception e) {
                throw new JobProcessorException(nozzle, e);
            }
        }
        
        private void checkPartOff(Nozzle nozzle) throws JobProcessorException {
            if (!nozzle.isPartDetectionEnabled()) {
                return;
            }
            try {
                // We need vacuum on to determine the vacuum level.
                nozzle.pick(null);
                
                boolean partOff = nozzle.isPartOff();
                
                nozzle.place();
                
                if (!partOff) {
                    throw new JobProcessorException(nozzle, "Part detected on nozzle after place.");
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
            try {
                HashMap<String, Object> params = new HashMap<>();
                params.put("job", job);
                params.put("jobProcessor", this);
                params.put("part", part);
                params.put("nozzle", nozzle);
                params.put("placement", placement);
                params.put("boardLocation", boardLocation);
                params.put("placementLocation", placementLocation);
                params.put("alignmentOffsets", plannedPlacement.alignmentOffsets);
                Configuration.get().getScripting().on("Job.Placement.BeforeAssembly", params);
            }
            catch (Exception e) {
            }
        }
        
        private void scriptComplete(PlannedPlacement plannedPlacement, Location placementLocation) throws JobProcessorException {
            final Nozzle nozzle = plannedPlacement.nozzle;
            final JobPlacement jobPlacement = plannedPlacement.jobPlacement;
            final Placement placement = jobPlacement.getPlacement();
            final Part part = placement.getPart();
            final BoardLocation boardLocation = plannedPlacement.jobPlacement.getBoardLocation();
            try {
                HashMap<String, Object> params = new HashMap<>();
                params.put("job", job);
                params.put("jobProcessor", this);
                params.put("part", part);
                params.put("nozzle", nozzle);
                params.put("placement", placement);
                params.put("boardLocation", boardLocation);
                params.put("placementLocation", placementLocation);
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

            // Add the part's height to the placement location
            placementLocation = placementLocation.add(new Location(part.getHeight().getUnits(), 0,
                    0, part.getHeight().getValue(), 0));
            
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
            }
            else {
                fireTextStatus("Job finished without error, placed %s parts in %s sec. (%s CPH)", 
                        totalPartsPlaced,
                        df.format(dtSec), 
                        df.format(totalPartsPlaced / (dtSec / 3600.0)));
            }
            
            Logger.info("Errored Placements:");
            for (JobPlacement jobPlacement : erroredPlacements) {
                Logger.info("{}: {}", jobPlacement, jobPlacement.getError().getMessage());
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

    protected abstract class PlannedPlacementStep implements Step {
        protected final List<PlannedPlacement> plannedPlacements;
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
                switch (plannedPlacement.jobPlacement.getPlacement().getErrorHandling()) {
                    case Alert:
                        throw e;
                    case Suppress:
                        plannedPlacement.jobPlacement.setError(e);
                        return this;
                    default:
                        throw new Error("Unhandled Error Handling case " + plannedPlacement.jobPlacement.getPlacement().getErrorHandling());
                }
            }
        }
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
     */
    @Root
    public static class StandardPnpJobPlanner implements PnpJobPlanner {
        Head head;
        
        public List<JobPlacement> plan(Head head, List<JobPlacement> jobPlacements) {
            this.head = head;
            
            // Create a List of Lists of JobPlacements that each Nozzle can handle, including
            // one instance of null per Nozzle. The null indicates a possible "no solution"
            // for that Nozzle.
            List<List<JobPlacement>> solutions = head.getNozzles().stream().map(nozzle -> {
                return Stream.concat(jobPlacements.stream().filter(jobPlacement -> {
                    return nozzleCanHandle(nozzle, jobPlacement.getPlacement().getPart());
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
            return result;
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
                if (jpA != null && !nozzle.getNozzleTip().canHandle(jpA.getPlacement().getPart())) {
                    countA++;
                }
                if (jpB != null && !nozzle.getNozzleTip().canHandle(jpB.getPlacement().getPart())) {
                    countB++;
                }
            }
            return countA - countB;
        };
    }
    
    @Root
    public static class SimplePnpJobPlanner implements PnpJobPlanner {
        /**
         * This is a trivial planner that does not try very hard to make an optimized job, but also
         * does not fail on large jobs like the Standard one does.
         * 
         * - For each planning cycle, the planner loops through each nozzle on the head. 
         * - For each nozzle it then loops through the list of remaining placements and
         *   finds the first placement that does not require a nozzle tip change. 
         * - If none are found it next searches for a placement that can be handled with a nozzle
         *   tip change.
         * - If no compatible placement is found in the searches above the nozzle is left empty.
         */
        @Override
        public List<JobPlacement> plan(Head head, List<JobPlacement> jobPlacements) {
            // Sort the placements by number of compatible nozzles ascending. This causes the
            // planner to prefer plans that have greater nozzle diversity, leading to overall
            // better nozzle usage as fewer placements remain.
            jobPlacements.sort(new Comparator<JobPlacement>() {
                @Override
                public int compare(JobPlacement o1, JobPlacement o2) {
                    int c1 = 0;
                    for (Nozzle nozzle : head.getNozzles()) {
                        if (AbstractPnpJobProcessor.nozzleCanHandle(nozzle, o1.getPlacement().getPart())) {
                            c1++;
                        }
                    }
                    int c2 = 0;
                    for (Nozzle nozzle : head.getNozzles()) {
                        if (AbstractPnpJobProcessor.nozzleCanHandle(nozzle, o2.getPlacement().getPart())) {
                            c2++;
                        }
                    }
                    return c1 - c2;
                }
            });
            List<JobPlacement> result = new ArrayList<>();
            for (Nozzle nozzle : head.getNozzles()) {
                JobPlacement solution = null;
                
                // First, see if we can put a placement on the nozzle that will not require a
                // nozzle tip change.
                if (nozzle.getNozzleTip() != null) {
                    for (JobPlacement jobPlacement : jobPlacements) {
                        Placement placement = jobPlacement.getPlacement();
                        Part part = placement.getPart();
                        if (nozzle.getNozzleTip().canHandle(part)) {
                            solution = jobPlacement;
                            break;
                        }
                    }
                }
                if (solution != null) {
                    jobPlacements.remove(solution);
                    result.add(solution);
                    continue;
                }

                // If that didn't work, see if we can put one on with a nozzle tip change.
                for (JobPlacement jobPlacement : jobPlacements) {
                    Placement placement = jobPlacement.getPlacement();
                    Part part = placement.getPart();
                    if (nozzleCanHandle(nozzle, part)) {
                        solution = jobPlacement;
                        break;
                    }
                }
                if (solution != null) {
                    jobPlacements.remove(solution);
                    result.add(solution);
                    continue;
                }
                
                // And if that didn't work we give up on this nozzle.
                result.add(null);
            }
            return result;
        }
    }
}
