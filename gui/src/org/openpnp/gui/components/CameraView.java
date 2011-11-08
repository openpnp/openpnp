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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;

import org.openpnp.CameraListener;
import org.openpnp.gui.components.reticle.Reticle;
import org.openpnp.spi.Camera;
import org.openpnp.util.XmlSerialize;

@SuppressWarnings("serial")
public class CameraView extends JComponent implements CameraListener {
	private Camera camera;
	private BufferedImage lastFrame;
	private int maximumFps;
	private Reticle reticle;
	
	private JPopupMenu popupMenu;
	
	private int width, height;
	private int scaledWidth, scaledHeight;
	private double sourceWidth, sourceHeight;
	private double scaleRatioX, scaleRatioY;
	private double scaledUnitsPerPixelX, scaledUnitsPerPixelY;
	private int centerX, centerY;
	
	private boolean calibrationMode;
	private int calibrationX1, calibrationY1, calibrationX2, calibrationY2;
	
	public CameraView() {
		setBackground(Color.black);
		setOpaque(true);
		
		Preferences prefs = Preferences.userNodeForPackage(this.getClass());
		String reticlePref = prefs.get("CameraView.reticle", null);
		try {
			Reticle reticle = (Reticle) XmlSerialize.deserialize(reticlePref);
			setReticle(reticle);
		}
		catch (Exception e) {
			System.out.println("Warning: Unable to load Reticle preference, error: " + e.getMessage());
		}
		
		popupMenu = new CameraViewPopupMenu(this);
		
		addMouseListener(mouseListener);
		addMouseMotionListener(mouseMotionListener);
	}
	
	public CameraView(int maximumFps) {
		this();
		setMaximumFps(maximumFps);
	}
	
	public void setMaximumFps(int maximumFps) {
		this.maximumFps = maximumFps;
		// turn off capture for the camera we are replacing, if any
		if (this.camera != null) {
			this.camera.stopContinuousCapture(this);
		}
		// turn on capture for the new camera
		if (this.camera != null) {
			this.camera.startContinuousCapture(this, maximumFps);
		}
	}
	
	public int getMaximumFps() {
		return maximumFps;
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
	
	public void setCalibrationMode(boolean calibrationMode) {
		this.calibrationMode = calibrationMode;
	}
	
	public boolean isCalibrationMode() {
		return calibrationMode;
	}
	
	public void setReticle(Reticle reticle) {
		this.reticle = reticle;
		
		// TODO: Make more global, this is temporary cause it hurts to use
		Preferences prefs = Preferences.userNodeForPackage(this.getClass());
		prefs.put("CameraView.reticle", XmlSerialize.serialize(reticle));
		System.out.println(XmlSerialize.serialize(reticle));
		try {
			prefs.flush();
		}
		catch (Exception e) {
			
		}
	}
	
	public Reticle getReticle() {
		return reticle;
	}

	@Override
	public void frameReceived(BufferedImage img) {
		lastFrame = img;
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
			if (this.width != width || this.height != height) {
				double destWidth = width, destHeight = height;
				sourceWidth = lastFrame.getWidth();
				sourceHeight = lastFrame.getHeight();

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
				
				centerX = ins.left + (width / 2) - (scaledWidth / 2);
				centerY = ins.top + (height / 2) - (scaledHeight / 2);
				
				scaleRatioX = sourceWidth / (double) scaledWidth;
				scaleRatioY = sourceHeight / (double) scaledHeight;
				
				scaledUnitsPerPixelX = camera.getUnitsPerPixel().getX() * scaleRatioX;
				scaledUnitsPerPixelY = camera.getUnitsPerPixel().getY() * scaleRatioY;
			}
				
			g2d.drawImage(lastFrame, centerX, centerY, scaledWidth, scaledHeight, null);
			
			// TODO need to handle rotation
			
			if (reticle != null) {
				reticle.draw(
						g2d, 
						camera.getUnitsPerPixel().getUnits(), 
						scaledUnitsPerPixelX, 
						scaledUnitsPerPixelY, 
						ins.left + (width / 2), 
						ins.top + (height / 2), 
						scaledWidth, 
						scaledHeight);
			}
			
			if (calibrationMode) {
				g.setColor(Color.green);
				g.drawLine(calibrationX1 - 20, calibrationY1, calibrationX1 + 20, calibrationY1);
				g.drawLine(calibrationX1, calibrationY1 - 20, calibrationX1, calibrationY1 + 20);
				
				g.setColor(Color.blue);
				g.drawLine(calibrationX2 - 20, calibrationY2, calibrationX2 + 20, calibrationY2);
				g.drawLine(calibrationX2, calibrationY2 - 20, calibrationX2, calibrationY2 + 20);
				
				double differenceX = calibrationX1 - calibrationX2;
				double differenceY = calibrationY1 - calibrationY2;
				
				differenceX *= scaleRatioX;
				differenceY *= scaleRatioY;
	
				double unitsPerPixelX = Math.abs((sourceWidth / differenceX) / sourceWidth);
				double unitsPerPixelY = Math.abs((sourceHeight / differenceY) / sourceHeight);
				
				String text = "Left mouse drag to set the first mark.\nRight mouse drag to set the second.";
				if (unitsPerPixelX != Double.POSITIVE_INFINITY || unitsPerPixelY != Double.POSITIVE_INFINITY) {
					text += "\n";
					text += String.format("%3.6f %s per pixel X.", unitsPerPixelX, camera.getUnitsPerPixel().getUnits());
					text += "\n";
					text += String.format("%3.6f %s per pixel Y.", unitsPerPixelY, camera.getUnitsPerPixel().getUnits());
				}
				drawTextOverlay(g2d, ins.left + 10, ins.top + 10, text);
			}
			
		}
		else {
			g.setColor(Color.red);
			g.drawLine(ins.left, ins.top, ins.right, ins.bottom);
			g.drawLine(ins.right, ins.top, ins.left, ins.bottom);
		}
	}
	
	private void drawTextOverlay(Graphics2D g2d, int topLeftX, int topLeftY, String text) {
		String[] lines = text.split("\n");
		List<TextLayout> textLayouts = new ArrayList<TextLayout>();
		int textWidth = 0, textHeight = 0;
		for (String line : lines) {
			TextLayout textLayout = new TextLayout(line, g2d.getFont(), g2d.getFontRenderContext());
			textWidth = (int) Math.max(textWidth, textLayout.getBounds().getWidth());
			textHeight += (int) textLayout.getBounds().getHeight();
			textLayouts.add(textLayout);
		}
		g2d.setColor(new Color(0, 0, 0, 0.7f));
		g2d.fillRoundRect(topLeftX, topLeftY, textWidth + 10, textHeight + 10, 6, 6);
		g2d.setColor(Color.white);
		g2d.drawRoundRect(topLeftX, topLeftY, textWidth + 10, textHeight + 10, 6, 6);
		int yPen = topLeftY + 5;
		for (TextLayout textLayout : textLayouts) {
			yPen += textLayout.getBounds().getHeight();
			textLayout.draw(g2d, topLeftX + 5, yPen);
		}
	}
	
	private MouseListener mouseListener = new MouseAdapter() {
		@Override
		public void mouseClicked(MouseEvent e) {
			popupMenu.show(e.getComponent(), e.getX(), e.getY());
		}
	};
	
	private MouseMotionListener mouseMotionListener = new MouseMotionAdapter() {
		@Override
		public void mouseDragged(MouseEvent e) {
			if (e.getButton() == MouseEvent.BUTTON1) {
				calibrationX1 = e.getX();
				calibrationY1 = e.getY();
			}
			else {
				calibrationX2 = e.getX();
				calibrationY2 = e.getY();
			}
			repaint();
		}
	};
	
}
