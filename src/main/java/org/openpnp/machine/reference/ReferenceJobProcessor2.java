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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kinda seems like we need different processor types for different job types.
 * Maybe generic? ReferenceJobProcessor2<PickAndPlace>?
 * The question is whether we want to limit the interface to be able to handle
 * only the things the UI can handle i.e. different states.
 * 
 *  How do we transition to stopped?
 *  How do we pause?
 *  
 *  
 *  Design Notes:
 *  Finite state machine with fixed transitions between states.
 *  
 *  Transitions can succeed or fail. If the transistion succeeds the state
 *  changes to the new state. If it fails an exception is thrown and the
 *  state does not change. 
 *  
 *  States need the ability to determine which state to transfer to when they
 *  succeed, and maybe when they fail.
 *  
 *  The user needs to be able to indicate a specific, valid, transition such
 *  as indicating the machine should stop.
 *  
 *  Maybe thinking about this from the wrong perspective. What interface do we
 *  want the UI or users of the class to have? Is the state machine interface
 *  too generic?
 *  
 */
public class ReferenceJobProcessor2 {
	private static final Logger logger = LoggerFactory.getLogger(ReferenceJobProcessor2.class);
	
	enum State {
		Stopped,
		Preflight,
		FiducialCheck,
		Plan,
		Feed,
		Pick,
		Align,
		Place,
		Complete
	}
	
	private static final Map<State, State[]> transitions = new HashMap<>();
	
	static {
		transitions.put(State.Stopped, new State[] 			{ State.Preflight });
		transitions.put(State.Preflight, new State[] 		{ State.FiducialCheck, State.Stopped });
		transitions.put(State.FiducialCheck, new State[] 	{ State.Plan, State.Stopped });
		transitions.put(State.Plan, new State[] 			{ State.Feed, State.Complete, State.Stopped });
		transitions.put(State.Feed, new State[] 			{ State.Pick, State.Stopped });
		transitions.put(State.Pick, new State[] 			{ State.Align, State.Stopped });
		transitions.put(State.Align, new State[] 			{ State.Place, State.Stopped });
		transitions.put(State.Place, new State[] 			{ State.Plan, State.Stopped });
		transitions.put(State.Complete, new State[] 		{ State.Stopped });
	}

	private State state = State.Stopped;
	
	public ReferenceJobProcessor2() {
	}
	
	public void next() throws Exception {
	}
	
	public void go(State state) throws Exception {
		State lastState = this.state;
	}
}
