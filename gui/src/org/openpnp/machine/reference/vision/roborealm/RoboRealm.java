package org.openpnp.machine.reference.vision.roborealm;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This class is a wrapper around the primitive RR_API supplied with RoboRealm.
 * It simplifies many of the functions, provides thread safety and handles
 * disconnects and automatic reconnects.
 * TODO: handle disconnect
 */
public class RoboRealm {
	private String host;
	private int port;

	private RR_API api = new RR_API();
	
	public RoboRealm(String host, int port) {
		this.host = host;
		this.port = port;
		api.connect(host, port);
	}
	
	public void setImage(BufferedImage image) {
		int width = image.getWidth();
		int height = image.getHeight();
		int rgbInt[] = new int[width * height];
		image.getRGB(0, 0, width, height, rgbInt, 0, width);
		byte rgbByte[] = new byte[width * height * 3];
		int i, j;
		for (j = i = 0; i < width * height;) {
			int num = rgbInt[i++];
			rgbByte[j++] = (byte) (num & 255);
			rgbByte[j++] = (byte) ((num >> 8) & 255);
			rgbByte[j++] = (byte) ((num >> 16) & 255);
		}
		synchronized (api) {
			api.setImage(rgbByte, width, height);
		}
	}
	
	public void execute(String source) {
		synchronized (api) {
			api.execute(source);
		}
	}
	
	public String getVariable(String name) {
		synchronized (api) {
			return api.getVariable(name);
		}
	}
	
	public Map<String, String> getVariables(String names) {
		synchronized (api) {
			Hashtable v = api.getVariables(names);
			if (v == null) {
				return null;
			}
			Map<String, String> map = new HashMap<String, String>();
			for (Object entry : v.entrySet()) {
				String key = (String) ((Entry) entry).getKey();
				String value = (String) ((Entry) entry).getValue();
				map.put(key, value);
			}
			return map;
		}
	}
}
