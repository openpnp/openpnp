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

package org.openpnp.spi;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import org.openpnp.CameraListener;

/**
 * Provides listener support for Camera subclasses.
 * @author jason
 *
 */
public abstract class AbstractCamera implements Camera {
	protected Set<ListenerEntry> listeners = new HashSet<ListenerEntry>();
	protected String name;
	
	public AbstractCamera(String name) {
		this.name = name;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public void startContinuousCapture(CameraListener listener, int maximumFps) {
		listeners.add(new ListenerEntry(listener, maximumFps));
	}

	@Override
	public void stopContinuousCapture(CameraListener listener) {
		listeners.remove(new ListenerEntry(listener, 0));
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
