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

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.openpnp.CameraListener;
import org.openpnp.RequiresConfigurationResolution;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.VisionProvider;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Persist;

public abstract class ReferenceCamera implements Camera, RequiresConfigurationResolution {
	@Attribute
	protected String name;

	@Element
	protected Location location = new Location(LengthUnit.Millimeters);
	
	@Attribute
	protected Looking looking = Looking.Down;
	
	@Element
	protected Location unitsPerPixel = new Location(LengthUnit.Millimeters);
	
	@Element(required=false)
	protected VisionProvider visionProvider;
	
	@Attribute(required=false)
	protected String headId;

	protected Head head;

	protected Set<ListenerEntry> listeners = Collections.synchronizedSet(new HashSet<ListenerEntry>());
	
	public ReferenceCamera(String name) {
		setName(name);
	}

	@Override
	public void resolve(Configuration configuration) throws Exception {
		if (head == null) {
			head = configuration.getMachine().getHead(headId);
		}
		if (visionProvider != null) {
			visionProvider.setCamera(this);
		}
	}
	
	@SuppressWarnings("unused")
	@Persist
	private void persist() {
		headId = (head == null ? null : head.getId());
	}
	
	@Override
	public Location getUnitsPerPixel() {
		return unitsPerPixel;
	}
	
	@Override
	public void setUnitsPerPixel(Location unitsPerPixel) {
		this.unitsPerPixel = unitsPerPixel;
	}

	@Override
	public Head getHead() {
		return head;
	}
	
	@Override
	public void setHead(Head head) {
		this.head = head;
	}
	
	@Override
	public void setLooking(Looking looking) {
		this.looking = looking;
	}

	@Override
	public Looking getLooking() {
		return looking;
	}
	
	@Override
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public void setLocation(Location location) {
		this.location = location;
	}

	@Override
	public Location getLocation() {
		return location;
	}

	@Override
	public void startContinuousCapture(CameraListener listener, int maximumFps) {
		listeners.add(new ListenerEntry(listener, maximumFps));
	}

	@Override
	public void stopContinuousCapture(CameraListener listener) {
		listeners.remove(new ListenerEntry(listener, 0));
	}
	
	@Override
	public VisionProvider getVisionProvider() {
		return visionProvider;
	}

	protected void broadcastCapture(BufferedImage img) {
		for (ListenerEntry listener : listeners) {
			if (listener.lastFrameSent < (System.currentTimeMillis() - (1000 / listener.maximumFps))) {
				listener.listener.frameReceived(img);
				listener.lastFrameSent = System.currentTimeMillis();
			}
		}
	}

	protected class ListenerEntry {
		public CameraListener listener;
		public int maximumFps;
		public long lastFrameSent;

		public ListenerEntry(CameraListener listener, int maximumFps) {
			this.listener = listener;
			this.maximumFps = maximumFps;
		}

		@Override
		public int hashCode() {
			return listener.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return obj.equals(listener);
		}
	}
}
