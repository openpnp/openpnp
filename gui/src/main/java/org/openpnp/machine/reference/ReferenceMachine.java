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

import org.openpnp.RequiresConfigurationResolution;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.camera.LtiCivilCamera;
import org.openpnp.machine.reference.camera.OpenCvCamera;
import org.openpnp.machine.reference.camera.TableScannerCamera;
import org.openpnp.machine.reference.camera.VfwCamera;
import org.openpnp.machine.reference.feeder.ReferenceTapeFeeder;
import org.openpnp.machine.reference.feeder.ReferenceTrayFeeder;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;

// TODO: See if any of the Reference* classes can be done away with and use only the SPI classes.
public class ReferenceMachine implements Machine, RequiresConfigurationResolution {
	final static private LengthUnit nativeUnits = LengthUnit.Millimeters;
	
	@Element
	private ReferenceDriver driver;
	@ElementList(name="heads")
	private ArrayList<ReferenceHead> headsList = new ArrayList<ReferenceHead>();
	@ElementList
	private ArrayList<ReferenceCamera> cameras = new ArrayList<ReferenceCamera>();
	@ElementList(name="feeders")
	private ArrayList<ReferenceFeeder> feedersList = new ArrayList<ReferenceFeeder>();

	private LinkedHashMap<String, ReferenceHead> heads = new LinkedHashMap<String, ReferenceHead>();
	private LinkedHashMap<String, ReferenceFeeder> feeders = new LinkedHashMap<String, ReferenceFeeder>();
	
	private Set<MachineListener> listeners = Collections.synchronizedSet(new HashSet<MachineListener>());
	private boolean enabled;
	
	@SuppressWarnings("unused")
	@Commit
	private void commit() {
		for (ReferenceHead head : headsList) {
			heads.put(head.getId(), head);
		}
		for (ReferenceFeeder feeder : feedersList) {
			feeders.put(feeder.getId(), feeder);
		}
	}
	
	@SuppressWarnings("unused")
	@Persist
	private void persist() {
		headsList.clear();
		headsList.addAll(heads.values());
		feedersList.clear();
		feedersList.addAll(feeders.values());
	}
	
	@Override
	public void resolve(Configuration configuration) throws Exception {
		configuration.resolve(driver);
		for (ReferenceHead head : heads.values()) {
			configuration.resolve(head);
		}
		for (ReferenceCamera camera : cameras) {
			configuration.resolve(camera);
		}
		for (ReferenceFeeder feeder : feeders.values()) {
			configuration.resolve(feeder);
		}
	}

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
	
	@Override
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
	
	@Override
	public Wizard getConfigurationWizard() {
		return null;
	}

	@Override
	public List<Class<? extends Feeder>> getCompatibleFeederClasses() {
		List<Class<? extends Feeder>> l = new ArrayList<Class<? extends Feeder>>();
		l.add(ReferenceTrayFeeder.class);
		l.add(ReferenceTapeFeeder.class);
		return l;
	}

	@Override
	public List<Class<? extends Camera>>  getCompatibleCameraClasses() {
		List<Class<? extends Camera>> l = new ArrayList<Class<? extends Camera>>();
		l.add(LtiCivilCamera.class);
		l.add(VfwCamera.class);
		l.add(TableScannerCamera.class);
		l.add(OpenCvCamera.class);
		return l;
	}
	
	@Override
	public void addFeeder(Feeder feeder) throws Exception {
		if (! (feeder instanceof ReferenceFeeder)) {
			throw new Exception("Can't add a Feeder that is not an instance of ReferenceFeeder.");
		}
		ReferenceFeeder referenceFeeder = (ReferenceFeeder) feeder;
		// Create an Id for the feeder.
		for (int i = 0; i < Integer.MAX_VALUE; i++) {
			String id = "F" + Integer.toString(i);
			if (!feeders.containsKey(id)) {
				referenceFeeder.setId(id);
				feeders.put(id, referenceFeeder);
				return;
			}
		}
		throw new Exception("Could not create a new Id for the Feeder.");
	}
	
	@Override
	public void removeFeeder(Feeder feeder) {
	}

	@Override
	public void addCamera(Camera camera) throws Exception {
		if (! (camera instanceof ReferenceCamera)) {
			throw new Exception("Can't add a Camera that is not an instance of ReferenceFeeder.");
		}
		ReferenceCamera referenceCamera = (ReferenceCamera) camera;
		cameras.add(referenceCamera);
	}
	
	@Override
	public void removeCamera(Camera camera) {
		// TODO Auto-generated method stub
		
	}
}
