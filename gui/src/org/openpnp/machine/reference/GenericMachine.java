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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.openpnp.Configuration;
import org.openpnp.Job;
import org.openpnp.LengthUnit;
import org.openpnp.gui.support.FakeCamera;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class GenericMachine implements Machine {
	final static private LengthUnit nativeUnits = LengthUnit.Millimeters;
	private double minX, minY, maxX, maxY;
	private ArrayList<GenericHead> heads = new ArrayList<GenericHead>();
	private Map<String, GenericFeeder> feeders = new HashMap<String, GenericFeeder>();
	private GenericDriver driver;
	private ArrayList<GenericCamera> cameras = new ArrayList<GenericCamera>();
	
	public void configure(Node n) throws Exception {
		XPath xpath = XPathFactory.newInstance().newXPath();

		NodeList nodes = (NodeList) xpath.evaluate("Axes/Axis", n, XPathConstants.NODESET);

		for (int i = 0; i < nodes.getLength(); i++) {
			Node axisNode = nodes.item(i);
			
			if (Configuration.getAttribute(axisNode, "reference").equalsIgnoreCase("x")) {
				minX = Configuration.getDoubleAttribute(axisNode, "min");
				maxX = Configuration.getDoubleAttribute(axisNode, "max");
			}
			else if (Configuration.getAttribute(axisNode, "reference").equalsIgnoreCase("y")) {
				minY = Configuration.getDoubleAttribute(axisNode, "min");
				maxY = Configuration.getDoubleAttribute(axisNode, "max");
			}
		}
		
		nodes = (NodeList) xpath.evaluate("Feeders/Feeder", n, XPathConstants.NODESET);

		for (int i = 0; i < nodes.getLength(); i++) {
			Node feederNode = nodes.item(i);
			
			GenericFeeder feeder = (GenericFeeder) Class.forName(Configuration.getAttribute(feederNode, "class")).newInstance();
			
			feeder.setReference(Configuration.getAttribute(feederNode, "reference"));
			
			Node configNode = (Node) xpath.evaluate("Configuration", feederNode, XPathConstants.NODE);
			
			feeder.configure(configNode);
			
			feeders.put(feeder.getReference(), feeder);
		}
			
		Node driverNode = (Node) xpath.evaluate("Driver", n, XPathConstants.NODE);
		
		driver = (GenericDriver) Class.forName(Configuration.getAttribute(driverNode, "class")).newInstance();
		
		driver.configure((Node) xpath.evaluate("Configuration", driverNode, XPathConstants.NODE)); 
		
		GenericHead head = new GenericHead(this);
		heads.add(head);
		
//		cameras.add(new SimulatorCamera("Head Tele", 50, 5, 0.022098, 0.021082, 0, 0, -50, 16.0998, 320, 240, head));
//		cameras.add(new SimulatorCamera("Head Wide", 50, 5, 0.022098, 0.021082, 0, 0, -100, 45, 640, 480, head));
//		cameras.add(new SimulatorCamera("Machine Wide", 50, 5, 0.022098, 0.021082, 200, 300, -800, 45, 640, 480, null));
	}
	
	@Override
	public void prepareJob(Configuration configuration, Job job) throws Exception {
		driver.prepareJob(configuration, job);
		for (GenericCamera camera : cameras) {
			camera.prepareJob(configuration, job);
		}
	}
	
	@Override
	public Feeder getFeeder(String reference) {
		return feeders.get(reference);
	}

	@Override
	public List<Head> getHeads() {
		// TODO this is really wasteful, gotta be a better way
		ArrayList<Head> l = new ArrayList<Head>();
		l.addAll(heads);
		return l;
	}
	
	@Override
	public List<Camera> getCameras() {
		// TODO this is really wasteful, gotta be a better way
		ArrayList<Camera> l = new ArrayList<Camera>();
		l.addAll(cameras);
		return l;
	}

	@Override
	public double getMaxX() {
		return maxX;
	}

	@Override
	public double getMaxY() {
		return maxY;
	}

	@Override
	public double getMinX() {
		return minX;
	}

	@Override
	public double getMinY() {
		return minY;
	}

	@Override
	public LengthUnit getNativeUnits() {
		return nativeUnits;
	}
	
	@Override
	public void home() throws Exception {
		for (Head head : heads) {
			head.home();
		}
	}
	
	GenericDriver getDriver() {
		return driver;
	}
}
