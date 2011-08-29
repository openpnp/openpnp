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

package org.openpnp.gui.support;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import org.openpnp.spi.AbstractCamera;

public class FakeCamera extends AbstractCamera implements Runnable {
	private Color color;
	
	public FakeCamera(String name) {
		super(name);
		color = new Color((float) Math.random(), (float) Math.random(), (float) Math.random());
		new Thread(this).start();
	}
	
	@Override
	public BufferedImage capture() {
		return generateFrame();
	}

	public void run() {
		while (true) {
			if (listeners.size() > 0) {
				BufferedImage frame = generateFrame();
				broadcastCapture(frame);
			}
			try {
				Thread.sleep(1000 / 24);
			}
			catch (Exception e) {
				
			}
		}
	}
	
	private BufferedImage generateFrame() {
		BufferedImage img = new BufferedImage(640, 480, BufferedImage.TYPE_INT_ARGB);
		Graphics g = img.getGraphics();
		g.setColor(Color.black);
		g.fillRect(0, 0, 640, 480);
		for (int i = 0; i < 100; i++) {
			g.setColor(color);
			g.drawLine(
					(int) (Math.random() * img.getWidth()),
					(int) (Math.random() * img.getHeight()),
					(int) (Math.random() * img.getWidth()),
					(int) (Math.random() * img.getHeight())
					);
		}
		g.dispose();
		return img;
	}
}
