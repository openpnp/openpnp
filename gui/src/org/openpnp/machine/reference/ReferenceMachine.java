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
import java.util.Map;
import java.util.Set;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.openpnp.Configuration;
import org.openpnp.Job;
import org.openpnp.LengthUnit;
import org.openpnp.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Camera.Looking;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ReferenceMachine implements Machine {
	final static private LengthUnit nativeUnits = LengthUnit.Millimeters;
	private Map<String, ReferenceHead> heads = new LinkedHashMap<String, ReferenceHead>();
	private Map<String, ReferenceFeeder> feeders = new LinkedHashMap<String, ReferenceFeeder>();
	private ReferenceDriver driver;
	private List<ReferenceCamera> cameras = new ArrayList<ReferenceCamera>();
	private Set<MachineListener> listeners = Collections.synchronizedSet(new HashSet<MachineListener>());
	private boolean ready;
	
	public void configure(Node n) throws Exception {
		XPath xpath = XPathFactory.newInstance().newXPath();
		
		NodeList nodes;

		Node driverNode = (Node) xpath.evaluate("Driver", n, XPathConstants.NODE);
		driver = (ReferenceDriver) Class.forName(Configuration.getAttribute(driverNode, "class")).newInstance();
		driver.configure((Node) xpath.evaluate("Configuration", driverNode, XPathConstants.NODE)); 
		
		// TODO consider moving all this class specific configuration into the Reference* class it
		// belongs to.
		nodes = (NodeList) xpath.evaluate("Heads/Head", n, XPathConstants.NODESET);
		for (int i = 0; i < nodes.getLength(); i++) {
			Node headNode = nodes.item(i);
			ReferenceHead head = (ReferenceHead) Class.forName(Configuration.getAttribute(headNode, "class")).newInstance();
			head.setReference(Configuration.getAttribute(headNode, "reference"));
			head.setMachine(this);
			Node configNode = (Node) xpath.evaluate("Configuration", headNode, XPathConstants.NODE);
			head.configure(configNode);
			heads.put(head.getReference(), head);
		}
		
		nodes = (NodeList) xpath.evaluate("Cameras/Camera", n, XPathConstants.NODESET);
		for (int i = 0; i < nodes.getLength(); i++) {
			Node cameraNode = nodes.item(i);
			ReferenceCamera camera = (ReferenceCamera) Class.forName(Configuration.getAttribute(cameraNode, "class")).newInstance();
			camera.setName(Configuration.getAttribute(cameraNode, "name"));
			
			String headReference = Configuration.getAttribute(cameraNode, "head");
			if (headReference != null) {
				camera.setHead(getHead(headReference));
			}
			
			String looking = Configuration.getAttribute(cameraNode, "looking");
			camera.setLooking(Looking.valueOf(looking));
			
			Node locationNode = (Node) xpath.evaluate("Location", cameraNode, XPathConstants.NODE);
			Location location = new Location();
			location.parse(locationNode);
			camera.setLocation(location);
			
			Node unitsPerPixelNode = (Node) xpath.evaluate("UnitsPerPixel", cameraNode, XPathConstants.NODE);
			Location unitsPerPixel = new Location();
			unitsPerPixel.parse(unitsPerPixelNode);
			camera.setUnitsPerPixel(unitsPerPixel);
			
			Node configNode = (Node) xpath.evaluate("Configuration", cameraNode, XPathConstants.NODE);
			camera.configure(configNode);
			cameras.add(camera);
		}
		
		nodes = (NodeList) xpath.evaluate("Feeders/Feeder", n, XPathConstants.NODESET);
		for (int i = 0; i < nodes.getLength(); i++) {
			Node feederNode = nodes.item(i);
			ReferenceFeeder feeder = (ReferenceFeeder) Class.forName(Configuration.getAttribute(feederNode, "class")).newInstance();
			feeder.setReference(Configuration.getAttribute(feederNode, "reference"));
			Node configNode = (Node) xpath.evaluate("Configuration", feederNode, XPathConstants.NODE);
			feeder.configure(configNode);
			feeders.put(feeder.getReference(), feeder);
		}
	}
	
	@Override
	public void prepareJob(Configuration configuration, Job job) throws Exception {
		driver.prepareJob(configuration, job);
		for (ReferenceCamera camera : cameras) {
			camera.prepareJob(configuration, job);
		}
	}
	
	@Override
	public Feeder getFeeder(String reference) {
		return feeders.get(reference);
	}

	@Override
	public List<Head> getHeads() {
		ArrayList<Head> l = new ArrayList<Head>();
		l.addAll(heads.values());
		return l;
	}
	
	public ReferenceHead getHead(String reference) {
		return heads.get(reference);
	}
	
	@Override
	public List<Camera> getCameras() {
		ArrayList<Camera> l = new ArrayList<Camera>();
		l.addAll(cameras);
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
	public boolean isReady() {
		return ready;
	}

	@Override
	public void start() throws Exception {
		try {
			driver.setEnabled(true);
			ready = true;
		}
		catch (Exception e) {
			fireMachineStartFailed(this, e.getMessage());
			throw e;
		}
		fireMachineStarted(this);
	}

	@Override
	public void stop() throws Exception {
		try {
			driver.setEnabled(false);
			ready = false;
		}
		catch (Exception e) {
			fireMachineStopFailed(this, e.getMessage());
			throw e;
		}
		fireMachineStopped(this, "User requested stop.");
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
	
	private void fireMachineStarted(Machine machine) {
		for (MachineListener listener : listeners) {
			listener.machineStarted(machine);
		}
	}
	
	private void fireMachineStartFailed(Machine machine, String reason) {
		for (MachineListener listener : listeners) {
			listener.machineStartFailed(machine, reason);
		}
	}
	
	private void fireMachineStopped(Machine machine, String reason) {
		for (MachineListener listener : listeners) {
			listener.machineStopped(machine, reason);
		}
	}
	
	private void fireMachineStopFailed(Machine machine, String reason) {
		for (MachineListener listener : listeners) {
			listener.machineStopFailed(machine, reason);
		}
	}
}
