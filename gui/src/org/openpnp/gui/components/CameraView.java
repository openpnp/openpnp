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

package org.openpnp.gui.components;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;

import org.openpnp.CameraListener;
import org.openpnp.spi.Camera;

@SuppressWarnings("serial")
public class CameraView extends JComponent implements CameraListener, MouseListener {
	private Camera camera;
	private BufferedImage lastFrame;
	private int maximumFps;
	private boolean showCrosshair;
	private Color crosshairColor;
	
	public CameraView(int maximumFps) {
		this.maximumFps = maximumFps;
		setBackground(Color.black);
		setOpaque(true);
		setShowCrosshair(true);
		setCrosshairColor(Color.red);
		addMouseListener(this);
	}

	public void setCamera(Camera camera) {
		// turn off capture for the camera we are replacing, if any
		if (this.camera != null) {
			this.camera.stopContinuousCapture(this);
		}
		this.camera = camera;
		// turn on capture for the new camera
		if (this.camera != null) {
			this.camera.startContinuousCapture(this, maximumFps);
		}
	}

	@Override
	public void frameReceived(BufferedImage img) {
		lastFrame = img;
		repaint();
	}
	
	public boolean isShowCrosshair() {
		return showCrosshair;
	}

	public void setShowCrosshair(boolean showCrosshair) {
		this.showCrosshair = showCrosshair;
		repaint();
	}

	public Color getCrosshairColor() {
		return crosshairColor;
	}

	public void setCrosshairColor(Color crosshairColor) {
		this.crosshairColor = crosshairColor;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Insets ins = getInsets();
		int width = getWidth() - ins.left - ins.right;
		int height = getHeight() - ins.top - ins.bottom;
		Graphics2D g2d = (Graphics2D) g;
		g.setColor(getBackground());
		g2d.fillRect(ins.left, ins.top, width, height);
		if (lastFrame != null) {
			double destWidth = width, destHeight = height;
			double sourceWidth = lastFrame.getWidth(), sourceHeight = lastFrame.getHeight();
			int scaledWidth, scaledHeight;

			double heightRatio = sourceHeight / destHeight;
			double widthRatio = sourceWidth / destWidth;

			if (heightRatio > widthRatio) {
				double aspectRatio = sourceWidth / sourceHeight; 
				scaledHeight = (int) destHeight;
				scaledWidth = (int) (scaledHeight * aspectRatio);
			}
			else {
				double aspectRatio = sourceHeight / sourceWidth; 
				scaledWidth = (int) destWidth;
				scaledHeight = (int) (scaledWidth * aspectRatio);
			}
			
			int cx = ins.left + (width / 2) - (scaledWidth / 2);
			int cy = ins.top + (height / 2) - (scaledHeight / 2);
			
			g2d.drawImage(lastFrame, cx, cy, scaledWidth, scaledHeight, null);
		}
		if (showCrosshair) {
			g.setColor(crosshairColor);
			g.drawLine(ins.left, height / 2 + ins.top, ins.left + width, height / 2 + ins.top);
			g.drawLine(width / 2 + ins.left, ins.top, width / 2 + ins.left, ins.top + height);
		}
	}
	
	
	
	@Override
	public void mouseClicked(MouseEvent e) {
		setShowCrosshair(!isShowCrosshair());
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}
}
