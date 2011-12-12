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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.openpnp.Configuration;
import org.openpnp.Job;
import org.openpnp.LengthUnit;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.core.Commit;

// TODO: See if any of the Reference* classes can be done away with and use only the SPI classes.
public class ReferenceMachine implements Machine {
	final static private LengthUnit nativeUnits = LengthUnit.Millimeters;
	
	@Element
	private ReferenceDriver driver;
	@ElementMap(attribute=true, entry="head", key="id")
	private LinkedHashMap<String, ReferenceHead> heads = new LinkedHashMap<String, ReferenceHead>();
	@ElementList
	private ArrayList<ReferenceCamera> cameras = new ArrayList<ReferenceCamera>();
	@ElementMap(attribute=true, entry="feeder", key="id")
	private LinkedHashMap<String, ReferenceFeeder> feeders = new LinkedHashMap<String, ReferenceFeeder>();
	
	private Set<MachineListener> listeners = Collections.synchronizedSet(new HashSet<MachineListener>());
	private boolean started;
	private boolean enabled;
	
	@Override
	public Feeder getFeeder(String id) {
		return feeders.get(id);
	}

	@Override
	public List<Head> getHeads() {
		ArrayList<Head> l = new ArrayList<Head>();
		l.addAll(heads.values());
		return l;
	}
	
	public ReferenceHead getHead(String id) {
		return heads.get(id);
	}
	
	@Override
	public List<Camera> getCameras() {
		ArrayList<Camera> l = new ArrayList<Camera>();
		l.addAll(cameras);
		return l;
	}

	@Override
	public List<Feeder> getFeeders() {
		ArrayList<Feeder> l = new ArrayList<Feeder>();
		l.addAll(feeders.values());
		return l;
	}

	@Override
	public LengthUnit getNativeUnits() {
		return nativeUnits;
	}
	
	@Override
	public void home() throws Exception {
		for (Head head : heads.values()) {
			head.home();
		}
	}
	
	ReferenceDriver getDriver() {
		return driver;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}
	
	@Override
	public void setEnabled(boolean enabled) throws Exception {
		if (enabled) {
			try {
				driver.setEnabled(true);
				this.enabled = true;
			}
			catch (Exception e) {
				fireMachineEnableFailed(this, e.getMessage());
				throw e;
			}
			fireMachineEnabled(this);
		}
		else {
			try {
				driver.setEnabled(false);
				this.enabled = false;
			}
			catch (Exception e) {
				fireMachineDisableFailed(this, e.getMessage());
				throw e;
			}
			fireMachineDisabled(this, "User requested stop.");
		}
	}

	public void start() throws Exception {
		if (started) {
			throw new Exception("ReferenceMachine was already started.");
		}
		driver.start(this);
		for (ReferenceHead head : heads.values()) {
			head.start(this);
		}
		for (ReferenceCamera camera : cameras) {
			camera.start(this);
		}
		for (ReferenceFeeder feeder : feeders.values()) {
			feeder.start(this);
		}
		started = true;
	}

	@Override
	public void addListener(MachineListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener(MachineListener listener) {
		listeners.remove(listener);
	}
	
	void fireMachineHeadActivity(Machine machine, Head head) {
		for (MachineListener listener : listeners) {
			listener.machineHeadActivity(machine, head);
		}
	}
	
	private void fireMachineEnabled(Machine machine) {
		for (MachineListener listener : listeners) {
			listener.machineEnabled(machine);
		}
	}
	
	private void fireMachineEnableFailed(Machine machine, String reason) {
		for (MachineListener listener : listeners) {
			listener.machineEnableFailed(machine, reason);
		}
	}
	
	private void fireMachineDisabled(Machine machine, String reason) {
		for (MachineListener listener : listeners) {
			listener.machineDisabled(machine, reason);
		}
	}
	
	private void fireMachineDisableFailed(Machine machine, String reason) {
		for (MachineListener listener : listeners) {
			listener.machineDisableFailed(machine, reason);
		}
	}
}
