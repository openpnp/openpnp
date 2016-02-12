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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.openpnp.CameraListener;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.reticle.Reticle;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.util.XmlSerialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class CameraView extends JComponent implements CameraListener {
	private final static Logger logger = LoggerFactory
			.getLogger(CameraView.class);
	
	private static final String PREF_RETICLE = "CamerView.reticle";
	
	private static final String DEFAULT_RETICLE_KEY = "DEFAULT_RETICLE_KEY";

	private final static int HANDLE_DIAMETER = 8;

	private enum HandlePosition {
		NW, N, NE, E, SE, S, SW, W
	}

	private enum SelectionMode {
		Resizing, Moving, Creating
	}

	/**
	 * The Camera we are viewing.
	 */
	private Camera camera;

	/**
	 * The last frame received, reported by the Camera.
	 */
	private BufferedImage lastFrame;

	/**
	 * The maximum frames per second that we'll display.
	 */
	private int maximumFps;

	private LinkedHashMap<Object, Reticle> reticles = new LinkedHashMap<>();

	private JPopupMenu popupMenu;

	/**
	 * The last width and height of the component that we painted for. If the
	 * width or height is different from these values at the start of paint
	 * we'll recalculate all the scaling data.
	 */
	private double lastWidth, lastHeight;

	/**
	 * The last width and height of the image that we painted for. If the width
	 * or height is different from these values at the start of paint we'll
	 * recalculate all the scaling data.
	 */
	private double lastSourceWidth, lastSourceHeight;
	
	private Location lastUnitsPerPixel;

	/**
	 * The width and height of the image after it has been scaled to fit the
	 * bounds of the component.
	 */
	private int scaledWidth, scaledHeight;

	/**
	 * The ratio of scaled width and height to unscaled width and height.
	 * scaledWidth * scaleRatioX = sourceWidth. scaleRatioX = sourceWidth /
	 * scaledWidth
	 */
	private double scaleRatioX, scaleRatioY;

	/**
	 * The Camera's units per pixel scaled at the same ratio as the image. That
	 * is, each pixel in the scaled image is scaledUnitsPerPixelX wide and
	 * scaledUnitsPerPixelY high.
	 */
	private double scaledUnitsPerPixelX, scaledUnitsPerPixelY;

	/**
	 * The top left position within the component at which the scaled image can
	 * be drawn for it to be centered.
	 */
	private int imageX, imageY;

	private boolean selectionEnabled;
	/**
	 * Rectangle describing the bounds of the selection in image coordinates.
	 */
	private Rectangle selection;
	/**
	 * The scaled version of the selection Rectangle. Rescaled any time the
	 * component's size is changed.
	 */
	private Rectangle selectionScaled;
	private SelectionMode selectionMode;
	private HandlePosition selectionActiveHandle;
	private int selectionStartX, selectionStartY;
	private float selectionFlashOpacity;
	private float selectionDashPhase;
	private static float[] selectionDashProfile = new float[] { 6f, 6f };
	// 11 is the sum of the dash lengths minus 1.
	private static float selectionDashPhaseStart = 11f;

	private CameraViewSelectionTextDelegate selectionTextDelegate;

	private ScheduledExecutorService scheduledExecutor;

	private Preferences prefs = Preferences
			.userNodeForPackage(CameraView.class);
	
	private String text;
	
	private boolean showImageInfo;
	
	private List<CameraViewActionListener> actionListeners = new ArrayList<>();
	
	private CameraViewFilter cameraViewFilter;
	
	private long flashStartTimeMs;
	private long flashLengthMs = 250;

	public CameraView() {
		setBackground(Color.black);
		setOpaque(true);

		String reticlePref = prefs.get(PREF_RETICLE, null);
		try {
			Reticle reticle = (Reticle) XmlSerialize.deserialize(reticlePref);
			setDefaultReticle(reticle);
		}
		catch (Exception e) {
			// logger.warn("Warning: Unable to load Reticle preference");
		}

		popupMenu = new CameraViewPopupMenu(this);

		addMouseListener(mouseListener);
		addMouseMotionListener(mouseMotionListener);
		addComponentListener(componentListener);

		scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

		// TODO: Cancel this when it's not being used instead of spinning,
		// or maybe create a real thread and wait().
		scheduledExecutor.scheduleAtFixedRate(new Runnable() {
			public void run() {
				if (selectionEnabled && selection != null) {
					// Adjust the dash phase so the line marches on the next
					// paint
					selectionDashPhase -= 1f;
					if (selectionDashPhase < 0) {
						selectionDashPhase = selectionDashPhaseStart;
					}
					repaint();
				}
			}
		}, 0, 50, TimeUnit.MILLISECONDS);
	}

	public CameraView(int maximumFps) {
		this();
		setMaximumFps(maximumFps);
	}
	
	public void addActionListener(CameraViewActionListener listener) {
		if (!actionListeners.contains(listener)) {
			actionListeners.add(listener);
		}
	}
	
	public boolean removeActionListener(CameraViewActionListener listener) {
		return actionListeners.remove(listener);
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

	public Camera getCamera() {
		return camera;
	}

	public void setDefaultReticle(Reticle reticle) {
		setReticle(DEFAULT_RETICLE_KEY, reticle);

		prefs.put(PREF_RETICLE, XmlSerialize.serialize(reticle));
		try {
			prefs.flush();
		}
		catch (Exception e) {

		}
	}
	
	public Reticle getDefaultReticle() {
		return reticles.get(DEFAULT_RETICLE_KEY);
	}
	
	public void setReticle(Object key, Reticle reticle) {
		if (reticle == null) {
			removeReticle(key);
		}
		else {
			reticles.put(key, reticle);
		}
	}
	
	public Reticle getReticle(Object key) {
		return reticles.get(key);
	}
	
	public Reticle removeReticle(Object key) {
		return reticles.remove(key);
	}

	public CameraViewSelectionTextDelegate getSelectionTextDelegate() {
		return selectionTextDelegate;
	}

	public void setSelectionTextDelegate(
			CameraViewSelectionTextDelegate selectionTextDelegate) {
		this.selectionTextDelegate = selectionTextDelegate;
	}
	
	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
	
	/**
	 * Causes a short flash in the CameraView to get the user's attention.
	 */
	public void flash() {
		flashStartTimeMs = System.currentTimeMillis();
		scheduledExecutor.scheduleAtFixedRate(
			new Runnable() {
				public void run() {
					if (System.currentTimeMillis() - flashStartTimeMs < flashLengthMs) {
						repaint();
					}
					else {
						flashStartTimeMs = 0;
						throw new RuntimeException();
					}
				}
			}, 0, 30, TimeUnit.MILLISECONDS);
	}
	
	public void setCameraViewFilter(CameraViewFilter cameraViewFilter) {
		this.cameraViewFilter = cameraViewFilter;
	}

	/**
	 * Show image instead of the camera image for milliseconds. After
	 * milliseconds elapses the view goes back to showing the camera image.
	 * The image should be the same width and height as the camera image
	 * otherwise the behavior is undefined. This function is intended to
	 * be used to briefly show the result of image processing. This is
	 * a shortcut to setCameraViewFilter(CameraViewFilter) which simply
	 * removes itself after the specified time.
	 * @param image
	 * @param millseconds
	 */
	public void showFilteredImage(final BufferedImage filteredImage, final long milliseconds) {
		setCameraViewFilter(new CameraViewFilter() {
        	long t = System.currentTimeMillis();
        	
			@Override
			public BufferedImage filterCameraImage(Camera camera, BufferedImage image) {
				if ((System.currentTimeMillis() - t) < milliseconds) {
					return filteredImage;
				}
				else {
					setCameraViewFilter(null);
					return image;
				}
			}
		});	
	}

	public BufferedImage captureSelectionImage() {
		if (selection == null || lastFrame == null) {
			return null;
		}

		selectionFlashOpacity = 1.0f;

		ScheduledFuture future = scheduledExecutor.scheduleAtFixedRate(
				new Runnable() {
					public void run() {
						if (selectionFlashOpacity > 0) {
							selectionFlashOpacity -= 0.07;
							selectionFlashOpacity = Math.max(0,
									selectionFlashOpacity);
							repaint();
						}
						else {
							throw new RuntimeException();
						}
					}
				}, 0, 30, TimeUnit.MILLISECONDS);

		int sx = selection.x;
		int sy = selection.y;
		int sw = selection.width;
		int sh = selection.height;

		BufferedImage image = new BufferedImage(sw, sh,
				BufferedImage.TYPE_INT_ARGB);
		Graphics g = image.getGraphics();
		g.drawImage(lastFrame, 0, 0, sw, sh, sx, sy, sx + sw, sy + sh, null);
		g.dispose();

		while (!future.isDone())
			;

		return image;
	}

	public Rectangle getSelection() {
		return selection;
	}

	@Override
	public void frameReceived(BufferedImage img) {
		if (cameraViewFilter != null) {
			img = cameraViewFilter.filterCameraImage(camera, img);
		}
		BufferedImage oldFrame = lastFrame;
		lastFrame = img;
		if (oldFrame == null
				|| (oldFrame.getWidth() != img.getWidth() 
				|| oldFrame.getHeight() != img.getHeight()
				|| camera.getUnitsPerPixel() != lastUnitsPerPixel)) {
			calculateScalingData();
		}
		repaint();
	}

	/**
	 * Calculates a bunch of scaling data that we cache to speed up painting.
	 * This is recalculated when the size of the component or the size of the
	 * source changes. This method is synchronized, along with paintComponent()
	 * so that the updates to the cached data are atomic.
	 * TODO: Also need to update if the camera's units per pixels changes.
	 */
	private synchronized void calculateScalingData() {
		BufferedImage image = lastFrame;

		if (image == null) {
			return;
		}

		Insets ins = getInsets();
		int width = getWidth() - ins.left - ins.right;
		int height = getHeight() - ins.top - ins.bottom;

		double destWidth = width, destHeight = height;

		lastWidth = width;
		lastHeight = height;

		lastSourceWidth = image.getWidth();
		lastSourceHeight = image.getHeight();

		double heightRatio = lastSourceHeight / destHeight;
		double widthRatio = lastSourceWidth / destWidth;

		if (heightRatio > widthRatio) {
			double aspectRatio = lastSourceWidth / lastSourceHeight;
			scaledHeight = (int) destHeight;
			scaledWidth = (int) (scaledHeight * aspectRatio);
		}
		else {
			double aspectRatio = lastSourceHeight / lastSourceWidth;
			scaledWidth = (int) destWidth;
			scaledHeight = (int) (scaledWidth * aspectRatio);
		}

		imageX = ins.left + (width / 2) - (scaledWidth / 2);
		imageY = ins.top + (height / 2) - (scaledHeight / 2);

		scaleRatioX = lastSourceWidth / (double) scaledWidth;
		scaleRatioY = lastSourceHeight / (double) scaledHeight;

		lastUnitsPerPixel = camera.getUnitsPerPixel();
		scaledUnitsPerPixelX = lastUnitsPerPixel.getX() * scaleRatioX;
		scaledUnitsPerPixelY = lastUnitsPerPixel.getY() * scaleRatioY;

		if (selectionEnabled && selection != null) {
			// setSelection() handles updating the scaled rectangle
			setSelection(selection);
		}
	}

	@Override
	protected synchronized void paintComponent(Graphics g) {
		super.paintComponent(g);
		BufferedImage image = lastFrame;
		Insets ins = getInsets();
		int width = getWidth() - ins.left - ins.right;
		int height = getHeight() - ins.top - ins.bottom;
		Graphics2D g2d = (Graphics2D) g;
		g.setColor(getBackground());
		g2d.fillRect(ins.left, ins.top, width, height);
		if (image != null) {
			// Only render if there is a valid image.
			g2d.drawImage(lastFrame, imageX, imageY, scaledWidth, scaledHeight,
					null);

			double c = camera.getLocation().getRotation();
			
			for (Reticle reticle : reticles.values()) {
				reticle.draw(
						g2d, 
						camera.getUnitsPerPixel().getUnits(),
						scaledUnitsPerPixelX, 
						scaledUnitsPerPixelY, 
						ins.left + (width / 2), 
						ins.top + (height / 2),
						scaledWidth, 
						scaledHeight,
						c);
			}
			
			if (text != null) {
				drawTextOverlay(g2d, 10, 10, text);
			}
			
			if (showImageInfo && text == null) {
			    drawImageInfo(g2d, 10, 10, image);
			}

			if (selectionEnabled && selection != null) {
				paintSelection(g2d);
			}
		}
		else {
			g.setColor(Color.red);
			g.drawLine(ins.left, ins.top, ins.right, ins.bottom);
			g.drawLine(ins.right, ins.top, ins.left, ins.bottom);
		}
		
		if (flashStartTimeMs > 0) {
			long timeLeft = flashLengthMs - (System.currentTimeMillis() - flashStartTimeMs);
			float alpha = (1f / flashLengthMs) * timeLeft;
			alpha = Math.min(alpha, 1);
			alpha = Math.max(alpha, 0);
			g2d.setColor(new Color(1f, 1f, 1f, alpha));
			g2d.fillRect(0, 0, getWidth(), getHeight());
		}
	}

	private void paintSelection(Graphics2D g2d) {
		int rx = selectionScaled.x;
		int ry = selectionScaled.y;
		int rw = selectionScaled.width;
		int rh = selectionScaled.height;
		int rx2 = rx + rw;
		int ry2 = ry + rh;
		int rxc = rx + rw / 2;
		int ryc = ry + rh / 2;

		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		// Draw the dashed rectangle, black background, white dashes
		g2d.setColor(Color.black);
		g2d.setStroke(new BasicStroke(1f));
		g2d.drawRect(rx, ry, rw, rh);
		g2d.setColor(Color.white);
		g2d.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT,
				BasicStroke.JOIN_BEVEL, 0, selectionDashProfile,
				selectionDashPhase));
		g2d.drawRect(rx, ry, rw, rh);

		if (selectionMode != SelectionMode.Creating) {
			// If we're not drawing a whole new rectangle, draw the
			// handles for the existing one.
			drawHandle(g2d, rx, ry);
			drawHandle(g2d, rx2, ry);
			drawHandle(g2d, rx, ry2);
			drawHandle(g2d, rx2, ry2);

			drawHandle(g2d, rxc, ry);
			drawHandle(g2d, rx2, ryc);
			drawHandle(g2d, rxc, ry2);
			drawHandle(g2d, rx, ryc);
		}

		if (selectionTextDelegate != null) {
			String text = selectionTextDelegate.getSelectionText(this);
			if (text != null) {
				// TODO: Be awesome like Apple and move the overlay inside
				// the rect if it goes past the edge of the window
				drawTextOverlay(g2d, (int) (rx + rw + 6), (int) (ry + rh + 6),
						text);
			}
		}

		if (selectionFlashOpacity > 0) {
			g2d.setColor(new Color(1.0f, 1.0f, 1.0f, selectionFlashOpacity));
			g2d.fillRect(rx, ry, rw, rh);
		}
	}

	/**
	 * Draws a standard handle centered on the given x and y position.
	 * 
	 * @param g2d
	 * @param x
	 * @param y
	 */
	private static void drawHandle(Graphics2D g2d, int x, int y) {
		g2d.setStroke(new BasicStroke(1f));
		g2d.setColor(new Color(153, 153, 187));
		g2d.fillArc(x - HANDLE_DIAMETER / 2, y - HANDLE_DIAMETER / 2,
				HANDLE_DIAMETER, HANDLE_DIAMETER, 0, 360);
		g2d.setColor(Color.white);
		g2d.drawArc(x - HANDLE_DIAMETER / 2, y - HANDLE_DIAMETER / 2,
				HANDLE_DIAMETER, HANDLE_DIAMETER, 0, 360);
	}

	/**
	 * Gets the HandlePosition, if any, at the given x and y. Returns null if
	 * there is not a handle at that position.
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	private HandlePosition getSelectionHandleAtPosition(int x, int y) {
		if (selection == null) {
			return null;
		}

		int rx = selectionScaled.x;
		int ry = selectionScaled.y;
		int rw = selectionScaled.width;
		int rh = selectionScaled.height;
		int rx2 = rx + rw;
		int ry2 = ry + rh;
		int rxc = rx + rw / 2;
		int ryc = ry + rh / 2;

		if (isWithinHandle(x, y, rx, ry)) {
			return HandlePosition.NW;
		}
		else if (isWithinHandle(x, y, rx2, ry)) {
			return HandlePosition.NE;
		}
		else if (isWithinHandle(x, y, rx, ry2)) {
			return HandlePosition.SW;
		}
		else if (isWithinHandle(x, y, rx2, ry2)) {
			return HandlePosition.SE;
		}
		else if (isWithinHandle(x, y, rxc, ry)) {
			return HandlePosition.N;
		}
		else if (isWithinHandle(x, y, rx2, ryc)) {
			return HandlePosition.E;
		}
		else if (isWithinHandle(x, y, rxc, ry2)) {
			return HandlePosition.S;
		}
		else if (isWithinHandle(x, y, rx, ryc)) {
			return HandlePosition.W;
		}
		return null;
	}

	/**
	 * A specialization of isWithin() that uses uses the bounding box of a
	 * handle.
	 * 
	 * @param x
	 * @param y
	 * @param handleX
	 * @param handleY
	 * @return
	 */
	private static boolean isWithinHandle(int x, int y, int handleX, int handleY) {
		return isWithin(x, y, handleX - 4, handleY - 4, 8, 8);
	}

	private static boolean isWithin(int pointX, int pointY, int boundsX,
			int boundsY, int boundsWidth, int boundsHeight) {
		return pointX >= boundsX && pointX <= (boundsX + boundsWidth)
				&& pointY >= boundsY && pointY <= (boundsY + boundsHeight);
	}

	private static Rectangle normalizeRectangle(Rectangle r) {
		return normalizeRectangle(r.x, r.y, r.width, r.height);
	}

	/**
	 * Builds a rectangle with the given parameters. If the width or height is
	 * negative the corresponding x or y value is modified and the width or
	 * height is made positive.
	 * 
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @return
	 */
	private static Rectangle normalizeRectangle(int x, int y, int width,
			int height) {
		if (width < 0) {
			width *= -1;
			x -= width;
		}
		if (height < 0) {
			height *= -1;
			y -= height;
		}
		return new Rectangle(x, y, width, height);
	}

	/**
	 * Draws text in a nice bubble at the given position. Newline characters in
	 * the text cause line breaks.
	 * 
	 * @param g2d
	 * @param topLeftX
	 * @param topLeftY
	 * @param text
	 */
	private static void drawTextOverlay(Graphics2D g2d, int topLeftX,
			int topLeftY, String text) {
		Insets insets = new Insets(10, 10, 10, 10);
		int interLineSpacing = 4;
		int cornerRadius = 8;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setStroke(new BasicStroke(1.0f));
		g2d.setFont(g2d.getFont().deriveFont(12.0f));
		String[] lines = text.split("\n");
		List<TextLayout> textLayouts = new ArrayList<>();
		int textWidth = 0, textHeight = 0;
		for (String line : lines) {
			TextLayout textLayout = new TextLayout(line, g2d.getFont(),
					g2d.getFontRenderContext());
			textWidth = (int) Math.max(textWidth, textLayout.getBounds()
					.getWidth());
			textHeight += (int) textLayout.getBounds().getHeight() + interLineSpacing;
			textLayouts.add(textLayout);
		}
		textHeight -= interLineSpacing;
		g2d.setColor(new Color(0, 0, 0, 0.75f));
		g2d.fillRoundRect(
				topLeftX, 
				topLeftY, 
				textWidth + insets.left + insets.right, 
				textHeight + insets.top + insets.bottom,
				cornerRadius,
				cornerRadius);
		g2d.setColor(Color.white);
		g2d.drawRoundRect(
				topLeftX, 
				topLeftY, 
				textWidth + insets.left + insets.right, 
				textHeight + insets.top + insets.bottom,
				cornerRadius,
				cornerRadius);
		int yPen = topLeftY + insets.top;
		for (TextLayout textLayout : textLayouts) {
			yPen += textLayout.getBounds().getHeight();
			textLayout.draw(g2d, topLeftX + insets.left, yPen);
			yPen += interLineSpacing;
		}
	}

    private static void drawImageInfo(Graphics2D g2d, int topLeftX, int topLeftY, BufferedImage image) {
        if (image == null) {
        	return;
        }
    	String text = String.format("Resolution: %d x %d\nHistogram:",
                image.getWidth(), 
                image.getHeight());
        Insets insets = new Insets(10, 10, 10, 10);
        int interLineSpacing = 4;
        int cornerRadius = 8;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setStroke(new BasicStroke(1.0f));
        g2d.setFont(g2d.getFont().deriveFont(12.0f));
        String[] lines = text.split("\n");
        List<TextLayout> textLayouts = new ArrayList<>();
        int textWidth = 0, textHeight = 0;
        for (String line : lines) {
            TextLayout textLayout = new TextLayout(line, g2d.getFont(),
                    g2d.getFontRenderContext());
            textWidth = (int) Math.max(textWidth, textLayout.getBounds()
                    .getWidth());
            textHeight += (int) textLayout.getBounds().getHeight() + interLineSpacing;
            textLayouts.add(textLayout);
        }
        textHeight -= interLineSpacing;
        
        int histogramHeight = 50 + 2;
        int histogramWidth = 255 + 2;
        
        int width = Math.max(textWidth, histogramWidth);
        int height = textHeight + histogramHeight;
        
        g2d.setColor(new Color(0, 0, 0, 0.75f));
        g2d.fillRoundRect(
                topLeftX, 
                topLeftY, 
                width + insets.left + insets.right, 
                height + insets.top + insets.bottom,
                cornerRadius,
                cornerRadius);
        g2d.setColor(Color.white);
        g2d.drawRoundRect(
                topLeftX, 
                topLeftY, 
                width + insets.left + insets.right, 
                height + insets.top + insets.bottom,
                cornerRadius,
                cornerRadius);
        int yPen = topLeftY + insets.top;
        for (TextLayout textLayout : textLayouts) {
            yPen += textLayout.getBounds().getHeight();
            textLayout.draw(g2d, topLeftX + insets.left, yPen);
            yPen += interLineSpacing;
        }
        
        g2d.setColor(new Color(1, 1, 1, 0.20f));
        g2d.fillRect(
            topLeftX + insets.left,
            yPen,
            histogramWidth,
            histogramHeight);
        
        // Calculate the histogram
        long[][] histogram = new long[3][256];
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = (rgb >> 0) & 0xff;
                histogram[0][r]++;
                histogram[1][g]++;
                histogram[2][b]++;
            }
        }
        // find the highest value in the histogram
        long maxVal = 0;
        for (int channel = 0; channel < 3; channel++) {
            for (int bucket = 0; bucket < 256; bucket++) {
                maxVal = Math.max(maxVal, histogram[channel][bucket]);
            }
        }
        // and scale it to 50 pixels tall
        double scale = 50.0 / maxVal;
        Color[] colors = new Color[] {
                Color.red,
                Color.green,
                Color.blue
        };
        for (int channel = 0; channel < 3; channel++) {
            g2d.setColor(colors[channel]);
            for (int bucket = 0; bucket < 256; bucket++) {
                int value = (int) (histogram[channel][bucket] * scale);
                g2d.drawLine(
                        topLeftX + insets.left + 1 + bucket,
                        yPen + 1 + 50 - value,
                        topLeftX + insets.left + 1 + bucket,
                        yPen + 1 + 50 - value);
            }
        }
    }

	/**
	 * Changes the HandlePosition to it's inverse if the given rectangle has a
	 * negative width, height or both.
	 * 
	 * @param r
	 */
	private static HandlePosition getOpposingHandle(Rectangle r,
			HandlePosition handlePosition) {
		if (r.getWidth() < 0 && r.getHeight() < 0) {
			if (handlePosition == HandlePosition.NW) {
				return HandlePosition.SE;
			}
			else if (handlePosition == HandlePosition.NE) {
				return HandlePosition.SW;
			}
			else if (handlePosition == HandlePosition.SE) {
				return HandlePosition.NW;
			}
			else if (handlePosition == HandlePosition.SW) {
				return HandlePosition.NE;
			}
		}
		else if (r.getWidth() < 0) {
			if (handlePosition == HandlePosition.NW) {
				return HandlePosition.NE;
			}
			else if (handlePosition == HandlePosition.NE) {
				return HandlePosition.NW;
			}
			else if (handlePosition == HandlePosition.SE) {
				return HandlePosition.SW;
			}
			else if (handlePosition == HandlePosition.SW) {
				return HandlePosition.SE;
			}
			else if (handlePosition == HandlePosition.E) {
				return HandlePosition.W;
			}
			else if (handlePosition == HandlePosition.W) {
				return HandlePosition.E;
			}
		}
		else if (r.getHeight() < 0) {
			if (handlePosition == HandlePosition.SW) {
				return HandlePosition.NW;
			}
			else if (handlePosition == HandlePosition.SE) {
				return HandlePosition.NE;
			}
			else if (handlePosition == HandlePosition.NW) {
				return HandlePosition.SW;
			}
			else if (handlePosition == HandlePosition.NE) {
				return HandlePosition.SE;
			}
			else if (handlePosition == HandlePosition.S) {
				return HandlePosition.N;
			}
			else if (handlePosition == HandlePosition.N) {
				return HandlePosition.S;
			}
		}
		return handlePosition;
	}

	/**
	 * Set the selection rectangle in image coordinates.
	 * 
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 */
	public void setSelection(int x, int y, int width, int height) {
		setSelection(new Rectangle(x, y, width, height));
	}

	/**
	 * Set the selection rectangle in image coordinates.
	 * 
	 * @param r
	 */
	public void setSelection(Rectangle r) {
		if (r == null) {
			selection = null;
			selectionMode = null;
		}
		else {
			selectionActiveHandle = getOpposingHandle(r, selectionActiveHandle);
			selection = normalizeRectangle(r);

			int rx = (int) (imageX + selection.x / scaleRatioX);
			int ry = (int) (imageY + selection.y / scaleRatioY);
			int rw = (int) (selection.width / scaleRatioX);
			int rh = (int) (selection.height / scaleRatioY);
			selectionScaled = new Rectangle(rx, ry, rw, rh);
		}
	}

	/**
	 * Set the selection rectangle in component coordinates. Updates the
	 * selection property with the properly scaled coordinates.
	 * 
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 */
	private void setScaledSelection(int x, int y, int width, int height) {
		selectionScaled = new Rectangle(x, y, width, height);
		selectionActiveHandle = getOpposingHandle(selectionScaled,
				selectionActiveHandle);
		selectionScaled = normalizeRectangle(selectionScaled);

		int rx = (int) ((x - imageX) * scaleRatioX);
		int ry = (int) ((y - imageY) * scaleRatioY);
		int rw = (int) (width * scaleRatioX);
		int rh = (int) (height * scaleRatioY);

		selection = new Rectangle(rx, ry, rw, rh);
	}

	public boolean isSelectionEnabled() {
		return selectionEnabled;
	}

	public void setSelectionEnabled(boolean selectionEnabled) {
		this.selectionEnabled = selectionEnabled;
	}
	
	public boolean isShowImageInfo() {
        return showImageInfo;
    }

    public void setShowImageInfo(boolean showImageInfo) {
        this.showImageInfo = showImageInfo;
    }

    public static Cursor getCursorForHandlePosition(
			HandlePosition handlePosition) {
		switch (handlePosition) {
		case NW:
			return Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
		case N:
			return Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
		case NE:
			return Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
		case E:
			return Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
		case SE:
			return Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);
		case S:
			return Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
		case SW:
			return Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
		case W:
			return Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
		}
		return null;
	}

	/**
	 * Updates the Cursor to reflect the current state of the component.
	 */
	private void updateCursor() {
		if (selectionEnabled) {
			if (selectionMode == SelectionMode.Moving) {
				setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
			}
			else if (selectionMode == SelectionMode.Resizing) {
				setCursor(getCursorForHandlePosition(selectionActiveHandle));
			}
			else if (selectionMode == null && selection != null) {
				int x = getMousePosition().x;
				int y = getMousePosition().y;

				HandlePosition handlePosition = getSelectionHandleAtPosition(x,
						y);
				if (handlePosition != null) {
					setCursor(getCursorForHandlePosition(handlePosition));
				}
				else if (selectionScaled.contains(x, y)) {
					setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
				}
				else {
					setCursor(Cursor
							.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
				}
			}
			else {
				setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
			}
		}
		else {
			setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		}
	}
	
	/**
	 * Capture the current image (unscaled, unmodified) and write it to disk.
	 */
	private void captureSnapshot() {
		try {
			flash();
			File dir = new File(Configuration.get().getConfigurationDirectory(), "snapshots");
			dir.mkdirs();
			DateFormat df = new SimpleDateFormat("YYYY-MM-dd_HH.mm.ss.SSS");
			File file = new File(dir, camera.getName() + "_" + df.format(new Date()) + ".png");
			ImageIO.write(lastFrame, "png", file);
		}
		catch (Exception e1) {
			e1.printStackTrace();
		}
	}
	
	private void fireActionEvent(MouseEvent e) {
		if (actionListeners.isEmpty()) {
			return;
		}
		
        int x = e.getX();
        int y = e.getY();

        // Find the difference in X and Y from the center of the image
        // to the mouse click.
        double offsetX = (scaledWidth / 2.0D) - (x - imageX);
        double offsetY = (scaledHeight / 2.0D) - (y - imageY);

        // Invert the X so that the offsets represent a bottom left to
        // top right coordinate system.
        offsetX = -offsetX;

        // Scale the offsets by the units per pixel for the camera.
        offsetX *= scaledUnitsPerPixelX;
        offsetY *= scaledUnitsPerPixelY;

        // The offsets now represent the distance to move the camera
        // in the Camera's units per pixel's units.

        // Create a location in the Camera's units per pixel's units
        // and with the values of the offsets.
        Location offsets = camera.getUnitsPerPixel().derive(offsetX,
                offsetY, 0.0, 0.0);
        // Add the offsets to the Camera's position.
        Location location = camera.getLocation().add(offsets);
		CameraViewActionEvent action = new CameraViewActionEvent(
				CameraView.this, 
				e.getX(), 
				e.getY(), 
				e.getX() * scaledUnitsPerPixelX, 
				e.getY() * scaledUnitsPerPixelY,
				location);
		for (CameraViewActionListener listener : new ArrayList<>(actionListeners)) {
			listener.actionPerformed(action);
		}
	}
	
	private void moveToClick(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();

        // Find the difference in X and Y from the center of the image
        // to the mouse click.
        double offsetX = (scaledWidth / 2.0D) - (x - imageX);
        double offsetY = (scaledHeight / 2.0D) - (y - imageY) + 1;

        // Invert the X so that the offsets represent a bottom left to
        // top right coordinate system.
        offsetX = -offsetX;

        // Scale the offsets by the units per pixel for the camera.
        offsetX *= scaledUnitsPerPixelX;
        offsetY *= scaledUnitsPerPixelY;

        // The offsets now represent the distance to move the camera
        // in the Camera's units per pixel's units.

        // Create a location in the Camera's units per pixel's units
        // and with the values of the offsets.
        Location offsets = camera.getUnitsPerPixel().derive(offsetX,
                offsetY, 0.0, 0.0);
        // Add the offsets to the Camera's position.
        Location location = camera.getLocation().add(offsets);
        // And move there.
        UiUtils.submitUiMachineTask(() -> {
            MovableUtils.moveToLocationAtSafeZ(camera, location, 1.0);
        });
	}
	
	private void beginSelection(MouseEvent e) {
        // If we're not doing anything currently, we can start
        // a new operation.
        if (selectionMode == null) {
            int x = e.getX();
            int y = e.getY();
            
            // See if there is a handle under the cursor.
            HandlePosition handlePosition = getSelectionHandleAtPosition(
                    x, y);
            if (handlePosition != null) {
                selectionMode = SelectionMode.Resizing;
                selectionActiveHandle = handlePosition;
            }
            // If not, perhaps they want to move the rectangle
            else if (selection != null
                    && selectionScaled.contains(x, y)) {

                selectionMode = SelectionMode.Moving;
                // Store the distance between the rectangle's origin and
                // where they started moving it from.
                selectionStartX = x - selectionScaled.x;
                selectionStartY = y - selectionScaled.y;
            }
            // If not those, it's time to create a rectangle
            else {
                selectionMode = SelectionMode.Creating;
                selectionStartX = x;
                selectionStartY = y;
            }
        }
	}
	
	private void continueSelection(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();

		if (selectionMode == SelectionMode.Resizing) {
			int rx = selectionScaled.x;
			int ry = selectionScaled.y;
			int rw = selectionScaled.width;
			int rh = selectionScaled.height;

			if (selectionActiveHandle == HandlePosition.NW) {
				setScaledSelection(x, y, (rw - (x - rx)),
						(rh - (y - ry)));
			}
			else if (selectionActiveHandle == HandlePosition.NE) {
				setScaledSelection(rx, y, x - rx, (rh - (y - ry)));
			}
			else if (selectionActiveHandle == HandlePosition.N) {
				setScaledSelection(rx, y, rw, (rh - (y - ry)));
			}
			else if (selectionActiveHandle == HandlePosition.E) {
				setScaledSelection(rx, ry, rw + (x - (rx + rw)), rh);
			}
			else if (selectionActiveHandle == HandlePosition.SE) {
				setScaledSelection(rx, ry, rw + (x - (rx + rw)), rh
						+ (y - (ry + rh)));
			}
			else if (selectionActiveHandle == HandlePosition.S) {
				setScaledSelection(rx, ry, rw, rh + (y - (ry + rh)));
			}
			else if (selectionActiveHandle == HandlePosition.SW) {
				setScaledSelection(x, ry, (rw - (x - rx)), rh
						+ (y - (ry + rh)));
			}
			else if (selectionActiveHandle == HandlePosition.W) {
				setScaledSelection(x, ry, (rw - (x - rx)), rh);
			}
		}
		else if (selectionMode == SelectionMode.Moving) {
			setScaledSelection(x - selectionStartX,
					y - selectionStartY, selectionScaled.width,
					selectionScaled.height);
		}
		else if (selectionMode == SelectionMode.Creating) {
			int sx = selectionStartX;
			int sy = selectionStartY;
			int w = x - sx;
			int h = y - sy;
			setScaledSelection(sx, sy, w, h);
		}
		updateCursor();
		repaint();
	}
	
	private void endSelection() {
		selectionMode = null;
		selectionActiveHandle = null;
	}

	private MouseListener mouseListener = new MouseAdapter() {
		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.isPopupTrigger() || e.isShiftDown() || SwingUtilities.isRightMouseButton(e)) {
				return;
			}
			// double click captures an image from the camera and writes it to disk. 
			if (e.getClickCount() == 2) {
				captureSnapshot();
			}
			else {
				fireActionEvent(e);
			}
		}
		
        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                popupMenu.show(e.getComponent(), e.getX(), e.getY());
                return;
            }
            else if (e.isShiftDown()) {
            	moveToClick(e);
            }
            else if (selectionEnabled) {
            	beginSelection(e);
            }
        }

		@Override
		public void mouseReleased(MouseEvent e) {
			if (e.isPopupTrigger()) {
				popupMenu.show(e.getComponent(), e.getX(), e.getY());
				return;
			}
			else {
				endSelection();
			}
		}
	};

	private MouseMotionListener mouseMotionListener = new MouseMotionAdapter() {
		@Override
		public void mouseMoved(MouseEvent e) {
			updateCursor();
		}
                
		@Override
		public void mouseDragged(MouseEvent e) {
			if (selectionEnabled) {
				continueSelection(e);
			}
		}
	};

	private ComponentListener componentListener = new ComponentAdapter() {
		@Override
		public void componentResized(ComponentEvent e) {
			calculateScalingData();
		}
	};

	public CameraViewSelectionTextDelegate pixelsAndUnitsTextSelectionDelegate = new CameraViewSelectionTextDelegate() {
		@Override
		public String getSelectionText(CameraView cameraView) {
			double widthInUnits = selection.width
					* camera.getUnitsPerPixel().getX();
			double heightInUnits = selection.height
					* camera.getUnitsPerPixel().getY();

			String text = String.format(Locale.US,"%dpx, %dpx\n%2.3f%s, %2.3f%s",
					(int) selection.getWidth(), (int) selection.getHeight(),
					widthInUnits, camera.getUnitsPerPixel().getUnits()
							.getShortName(), heightInUnits, camera
							.getUnitsPerPixel().getUnits().getShortName());
			return text;
		}
	};
}
