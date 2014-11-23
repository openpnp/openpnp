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

import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Head;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;

public class ZippyMachine extends ReferenceMachine {
//	@Override
	public List<Class<? extends NozzleTip>> getCompatibleNozzleTipClasses() {
		List<Class<? extends NozzleTip>> l = new ArrayList<Class<? extends NozzleTip>>();
		l.add(ZippyNozzleTip.class);
		return l;
	}
	
    public List<NozzleTip> getNozzleTips() {
    	Configuration configuration = Configuration.get(); 
    	List<NozzleTip> nozzletips = new ArrayList<NozzleTip>(); 
		for (Head head : configuration.getMachine().getHeads()) { //for each head
			for (Nozzle nozzle : head.getNozzles()) { //for each nozzle
				for (NozzleTip nozzletip : nozzle.getNozzleTips()) { //for each nozzletip
					nozzletips.add(nozzletip); //add to list from above
				}
			}
		}
		return nozzletips;
   }

}
