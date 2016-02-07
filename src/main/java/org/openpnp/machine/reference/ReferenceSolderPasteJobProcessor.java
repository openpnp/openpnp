/*
 	Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 	
 	This file is part of OpenPnP.
 	
	OpenPnP is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenPnP is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenPnP.  If not, see <http://www.gnu.org/licenses/>.
 	
 	For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.machine.reference;

import org.openpnp.gui.support.Wizard;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.BoardPad;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.PasteDispenser;
import org.openpnp.spi.base.AbstractJobProcessor;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.Utils2D;
import org.simpleframework.xml.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferenceSolderPasteJobProcessor extends AbstractJobProcessor {
	private static final Logger logger = LoggerFactory.getLogger(ReferenceSolderPasteJobProcessor.class);
	
	@Attribute(required=false)
	private String dummy;
	
	public ReferenceSolderPasteJobProcessor() {
	}
	
	@Override
    public void run() {
		state = JobState.Running;
		fireJobStateChanged();
		
		Machine machine = Configuration.get().getMachine();
		
		for (Head head : machine.getHeads()) {
			fireDetailedStatusUpdated(String.format("Move head %s to Safe-Z.", head.getName()));		
	
			if (!shouldJobProcessingContinue()) {
				return;
			}
	
			try {
				head.moveToSafeZ(1.0);
			}
			catch (Exception e) {
				fireJobEncounteredError(JobError.MachineMovementError, e.getMessage());
				return;
			}
		}
		
		Head head = machine.getHeads().get(0);
		PasteDispenser dispenser = head.getPasteDispensers().get(0);

		for (BoardLocation boardLocation : job.getBoardLocations()) {
			if (!boardLocation.isEnabled()) {
				continue;
			}
		    for (BoardPad pad : boardLocation.getBoard().getSolderPastePads()) {
		        if (pad.getSide() != boardLocation.getSide()) {
		            continue;
		        }
	            Location location = pad.getLocation();
	            location = Utils2D.calculateBoardPlacementLocation(
	            		boardLocation, location);

	            fireDetailedStatusUpdated(String.format("Move to pad location, safe Z at (%s).", location));
	            if (!shouldJobProcessingContinue()) {
	                return;
	            }
	            try {
                    MovableUtils.moveToLocationAtSafeZ(dispenser, location, 1.0);
	            }
	            catch (Exception e) {
	                fireJobEncounteredError(JobError.MachineMovementError, e.getMessage());
	            }
	            
                fireDetailedStatusUpdated(String.format("Dispense.", location));
                if (!shouldJobProcessingContinue()) {
                    return;
                }
                try {
                    // TODO: The startLocation, endLocation and time will
                    // eventually come from an algorithm that uses the Pad's
                    // physical properties, but until we have that we just
                    // send junk data and let the driver interpret it.
                    dispenser.dispense(null, null, 0);
                }
                catch (Exception e) {
                    fireJobEncounteredError(JobError.MachineMovementError, e.getMessage());
                }
		    }
		}
		
		fireDetailedStatusUpdated("Job complete.");
		
		state = JobState.Stopped;
		fireJobStateChanged();
	}
	
    @Override
    public Wizard getConfigurationWizard() {
        return null;
    }
}
