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
import java.util.List;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.camera.LtiCivilCamera;
import org.openpnp.machine.reference.camera.OpenCvCamera;
import org.openpnp.machine.reference.camera.TableScannerCamera;
import org.openpnp.machine.reference.camera.VfwCamera;
import org.openpnp.machine.reference.feeder.ReferenceTapeFeeder;
import org.openpnp.machine.reference.feeder.ReferenceTrayFeeder;
import org.openpnp.machine.reference.feeder.ReferenceTubeFeeder;
import org.openpnp.spi.*;
import org.openpnp.spi.base.AbstractMachine;
import org.simpleframework.xml.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferenceMachine extends AbstractMachine {
	private static Logger logger = LoggerFactory.getLogger(ReferenceMachine.class);

	@Element
	private ReferenceDriver driver;
	
	private boolean enabled;
	
	ReferenceDriver getDriver() {
		return driver;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}
	
	@Override
	public void setEnabled(boolean enabled) throws Exception {
		logger.debug("setEnabled({})", enabled);
		if (enabled) {
			try {
				driver.setEnabled(true);
				this.enabled = true;
			}
			catch (Exception e) {
				fireMachineEnableFailed(e.getMessage());
				throw e;
			}
			fireMachineEnabled();
		}
		else {
			try {
				driver.setEnabled(false);
				this.enabled = false;
			}
			catch (Exception e) {
				fireMachineDisableFailed(e.getMessage());
				throw e;
			}
			fireMachineDisabled("User requested stop.");
		}
	}

	@Override
	public Wizard getConfigurationWizard() {
		return driver.getConfigurationWizard();
	}

	@Override
	public List<Class<? extends Feeder>> getCompatibleFeederClasses() {
		List<Class<? extends Feeder>> l = new ArrayList<Class<? extends Feeder>>();
		l.add(ReferenceTrayFeeder.class);
		l.add(ReferenceTapeFeeder.class);
		l.add(ReferenceTubeFeeder.class);
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
	public List<Head> getHeads() {
		logger.trace("getHeads");
		return super.getHeads();
	}

	@Override
	public Head getHead(String s) {
		Head result = super.getHead(s);
		logger.debug("getHead({}) => {}", s, result);
		return result;
	}

	@Override
	public List<Feeder> getFeeders() {
		logger.trace("getFeeders");
		return super.getFeeders();
	}

	@Override
	public Feeder getFeeder(String s) {
		Feeder result = super.getFeeder(s);
		logger.debug("getFeeder({}) => {}", s, result);
		return result;
	}

	@Override
	public List<Camera> getCameras() {
		logger.trace("getCameras");
		return super.getCameras();
	}

	@Override
	public Camera getCamera(String s) {
		Camera result = super.getCamera(s);
		logger.debug("getCamera({}) => ", s, result);
		return result;
	}

	@Override
	public void home() throws Exception {
		logger.debug("home");
		super.home();
	}

	@Override
	public void addListener(MachineListener machineListener) {
		logger.trace("addListener({})", machineListener);
		super.addListener(machineListener);
	}

	@Override
	public void removeListener(MachineListener machineListener) {
		logger.trace("removeListener({})", machineListener);
		super.removeListener(machineListener);
	}

	@Override
	public void addFeeder(Feeder feeder) throws Exception {
		logger.debug("addFeeder({})", feeder);
		super.addFeeder(feeder);
	}

	@Override
	public void removeFeeder(Feeder feeder) {
		logger.debug("removeFeeder({})", feeder);
		super.removeFeeder(feeder);
	}

	@Override
	public void addCamera(Camera camera) throws Exception {
		logger.debug("addCamera({})", camera);
		super.addCamera(camera);
	}

	@Override
	public void removeCamera(Camera camera) {
		logger.debug("removeCamera({})", camera);
		super.removeCamera(camera);
	}

	@Override
	public JobPlanner getJobPlanner() {
		JobPlanner result = super.getJobPlanner();
		logger.debug("getJobPlanner => {}", result);
		return result;
	}

}
