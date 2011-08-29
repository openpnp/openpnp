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
