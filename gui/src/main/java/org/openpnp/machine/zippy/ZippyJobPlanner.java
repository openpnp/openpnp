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
import java.util.List;

import org.openpnp.model.Part;
import org.openpnp.planner.SimpleJobPlanner;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;

public class ZippyJobPlanner extends SimpleJobPlanner {
	
    private static Feeder getNozzletipSolution(Machine machine, Nozzle nozzle, Part part) {
        // Get a list of Feeders that can source the part
        List<Feeder> feeders = new ArrayList<Feeder>();
        for (Feeder feeder : machine.getFeeders()) {
            if (feeder.getPart() == part && feeder.canFeedToNozzle(nozzle) && feeder.isEnabled()) {
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

	
}
