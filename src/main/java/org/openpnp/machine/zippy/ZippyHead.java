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

import java.util.Collections;
import java.util.List;

import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.spi.Nozzle;

public class ZippyHead extends ReferenceHead {



    @Override
	public void home() throws Exception {
		logger.debug("{}.home()", getId());
	    driver.home(this);
	    ((ZippyNozzle) nozzles.get(0)).clearAppliedOffset();
	    
	    machine.fireMachineHeadActivity(this);
	}
    @Override
    public List<Nozzle> getNozzles() {
        return Collections.unmodifiableList(nozzles);
    }

}
