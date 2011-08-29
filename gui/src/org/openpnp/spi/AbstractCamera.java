package org.openpnp.spi;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import org.openpnp.CameraListener;

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
