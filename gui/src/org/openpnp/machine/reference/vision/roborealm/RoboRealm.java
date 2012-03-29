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
		api.setCamera("off");
	}
	
	public boolean setImage(BufferedImage image) {
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
			return api.setImage(rgbByte, width, height);
		}
	}
	
	public boolean execute(String source) {
		synchronized (api) {
			return api.execute(source);
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
	
	public boolean setParameter(String module, int moduleIndex, String parameter, String value) {
		synchronized (api) {
			return api.setParameter(module, moduleIndex, parameter, value);
		}
	}
}
