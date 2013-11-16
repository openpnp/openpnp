/*
 	Copyright (C) 2013 Richard Spelling <openpnp@chebacco.com>
 	
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
package org.openpnp.machine.zippy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.openpnp.model.Configuration;
import org.openpnp.model.Package;
import org.openpnp.model.Part;
import org.openpnp.planner.SimpleJobPlanner;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZippyJobPlanner extends SimpleJobPlanner {
	private final static Logger logger = LoggerFactory.getLogger(ZippyJobPlanner.class);
	
   @Override
    public synchronized Set<PlacementSolution> getNextPlacementSolutions(Head head) {
        // TODO: Make sure the head and nozzle can reach the feeder and placement
        // might need to actually translate everything to final coordinates first, maybe
        // that gets weird
        Set<PlacementSolution> results = new LinkedHashSet<PlacementSolution>();
        Iterator<PlacementSolution> i = solutions.iterator();
        for (Nozzle nozzle : head.getNozzles()) {
            if (!i.hasNext()) {
                break;
            }
            PlacementSolution solution = i.next();
            i.remove();
            Feeder feeder = getFeederSolution(Configuration.get().getMachine(), nozzle, solution.placement.getPart());
            NozzleTip nozzletip = getNozzletipSolution(Configuration.get().getMachine(), nozzle, solution.placement.getPart());
            // We potentially return null here for Feeder, which lets the JobProcessor know that no applicable
            // Feeder was found. 
            solution = new PlacementSolution(solution.placement, solution.boardLocation, solution.head, nozzle, nozzletip, feeder);
            results.add(solution);
        }
        return results.size() > 0 ? results : null;
    }
	//@Override
    protected static Feeder getFeederSolution(Machine machine, Nozzle nozzle, Part part) {
        // Get a list of Feeders that can source the part
        List<Feeder> feeders = new ArrayList<Feeder>();
        for (Feeder feeder : machine.getFeeders()) {
            if (feeder.getPart() == part && feeder.isEnabled()) {
                feeders.add(feeder);
            }
        }
        if (feeders.size() < 1) {
            return null;
        }
        // For now we just take the first Feeder that can feed the part.
        Feeder feeder = feeders.get(0);
        return feeder;
    }


    private static NozzleTip getNozzletipSolution(Machine machine, Nozzle nozzle, Part part) {
        // Get nozzletip that works with this package
    	Package this_package = part.getPackage();
        List<NozzleTip> nozzletips = new ArrayList<NozzleTip>();
        for (NozzleTip nozzletip : nozzle.getNozzleTips()) {
            if (nozzletip.getId().equals(this_package.getNozzleTipId())) {
                nozzletips.add(nozzletip);
            }
        }
        if (nozzletips.size() < 1) {
            return null;
        }
        // For now we just take the first Feeder that can feed the part.
        NozzleTip nozzletip = nozzletips.get(0);
        return nozzletip;
    }

	
}
