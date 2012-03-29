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

package org.openpnp.machine.reference.camera;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import org.openpnp.CameraListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;


/**
 * An implementation of Camera that renders a viewport into a large map
 * of tiles. Tiles are PNG files stored with filenames that denote
 * their position in real space. Ex: 23.456,45.641.png.
 * 
 * TODO: Allow specifying height which will create a larger tile and then
 * scale it down.
 */
public class TableScannerCamera extends ReferenceCamera implements Runnable {
	/**
	 * Path to the directory of tile images.
	 */
	@Element
	private String tableScannerOutputDirectoryPath;
	
	private int tilesWide = 3;
	private int tilesHigh = 3;
	
	private File tableScannerOutputDirectory;
	
	/**
	 * The last X and Y position that we rendered for. Used to optimize the
	 * renderer and not generate duplicate frames.
	 */
	private double lastX = Double.MIN_VALUE, lastY = Double.MIN_VALUE;
	
	/**
	 * Two dimensional array representing the layout of the entire set of
	 * tiles.
	 */
	private Tile[][] tiles;
	
	/**
	 * List of all of the tiles. Used when searching for closest matches.
	 */
	private List<Tile> tileList = new ArrayList<Tile>();
	
	/**
	 * Buffered used to render the tiles local to the center point. This buffer
	 * is tilesWide * imageWidth by tilesHigh * imageHeight in pixels. 
	 */
	private BufferedImage buffer;
	
	/**
	 * The last frame rendered.
	 */
	private BufferedImage frame;
	
	/**
	 * The last tile that was used to compute the local tile array. Used to
	 * avoid re-rendering when the head has moved less than a tile since the
	 * last update.
	 */
	private Tile lastCenterTile;
	
	private Thread thread;
	
	private Object captureLock = new Object();
	
	@Commit
	private void commit() throws Exception {
		tableScannerOutputDirectory = new File(tableScannerOutputDirectoryPath);
		if (!tableScannerOutputDirectory.exists()) {
			throw new Exception("table-scanner-output-directory-path " + tableScannerOutputDirectoryPath + " does not exist.");
		}
		buildTiles();
	}
	
	@Override
	public void startContinuousCapture(CameraListener listener, int maximumFps) {
		synchronized (this) {
			if (thread == null) {
				thread = new Thread(this);
				thread.start();
			}
		}
		super.startContinuousCapture(listener, maximumFps);
	}

	@Override
	public BufferedImage capture() {
		synchronized (this) {
			if (thread == null) {
				thread = new Thread(this);
				thread.start();
			}
		}
		synchronized (captureLock) {
			try {
				captureLock.wait();
			}
			catch (Exception e) {
				
			}
		}
		return frame;
	}
	
	public void run() {
		while (true) {
			if (lastX != head.getAbsoluteX() || lastY != head.getAbsoluteY()) {
				// Find the closest tile to the head's current position.
				Tile closestTile = getClosestTile(head.getAbsoluteX(), head.getAbsoluteY());
				
				// If it has changed we need to render the entire buffer.
				if (closestTile != lastCenterTile) {
					lastCenterTile = closestTile;
					renderBuffer();
				}
				
				// Now we render the buffer into the frame.
				renderFrame();
				
				// And remember the last position we rendered.
				lastX = head.getAbsoluteX();
				lastY = head.getAbsoluteY();
			}
			// Finally, broadcast the frame, whether it was regenerated or not.
			// TODO broadcast a copy, not the one we'll modify next frame
			broadcastCapture(frame);
			synchronized (captureLock) {
				try {
					captureLock.notify();
				}
				catch (Exception e) {
					
				}
			}
			// And then we rest.
			try {
				Thread.sleep(1000 / 24);
			}
			catch (Exception e) {
				
			}
		}
	}
	
	private void buildTiles() {
		// Load all png files from the directory that look like they match what
		// we are expecting.
		File[] files = tableScannerOutputDirectory.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File arg0, String arg1) {
				return arg1.contains(".") && arg1.contains(",") && arg1.endsWith(".png");
			}
		});
		// Load the first image we found and use it's properties as a template
		// for the rest of the images.
		BufferedImage templateImage;
		try {
			templateImage = ImageIO.read(files[0]);
		}
		catch (Exception e) {
			throw new Error(e);
		}
		// We build a set of unique X and Y positions that we see so we can
		// later build a two dimensional array of the riles
		TreeSet<Double> uniqueX = new TreeSet<Double>();
		TreeSet<Double> uniqueY = new TreeSet<Double>();
		// Create a map of the tiles so that we can quickly find them when we
		// build the array.
		Map<Tile, Tile> tileMap = new HashMap<Tile, Tile>();
		// Parse the filenames of the all the files and add their coordinates
		// to the sets and map.
		for (File file : files) {
			Location location = new Location();
			location.setUnits(LengthUnit.Millimeters);
			String filename = file.getName();
			filename = filename.substring(0, filename.indexOf(".png"));
			String[] xy = filename.split(",");
			double x = Double.parseDouble(xy[0]);
			double y = Double.parseDouble(xy[1]);
			Tile tile = new Tile(x, y, file);
			uniqueX.add(x);
			uniqueY.add(y);
			tileMap.put(tile, tile);
			tileList.add(tile);
		}
		// Create a two dimensional array to store all the of the tiles
		tiles = new Tile[uniqueX.size()][uniqueY.size()];
		
		// Iterate through all the unique X and Y positions that were found
		// and add each file to the two dimensional array in the position
		// where it belongs
		int x = 0, y = 0;
		for (Double xPos : uniqueX) {
			y = 0;
			for (Double yPos : uniqueY) {
				Tile tile = tileMap.get(new Tile(xPos, yPos, null));
				tiles[x][y] = tile;
				tile.setTileX(x);
				tile.setTileY(y);
				y++;
			}
			x++;
		}
		
		/*
		 * Create a buffer that we will render the center tile and it's
		 * surrounding tiles to. 
		 */
		buffer = new BufferedImage(
				templateImage.getWidth() * tilesWide,
				templateImage.getHeight() * tilesHigh,
				BufferedImage.TYPE_INT_ARGB);
		
		/*
		 * Create the buffer that we will render final output to and return
		 * to clients.
		 */
		frame = new BufferedImage(
				templateImage.getWidth(),
				templateImage.getHeight(),
				BufferedImage.TYPE_INT_ARGB);
	}
	
	private void renderBuffer() {
		// determine where in the map the center tile is
		int centerTileX = lastCenterTile.getTileX();
		int centerTileY = lastCenterTile.getTileY();

		Graphics2D g = (Graphics2D) buffer.getGraphics();
		g.setColor(Color.black);
		g.clearRect(0, 0, buffer.getWidth(), buffer.getHeight());
		g.setColor(Color.white);

		/*
		 * Render the tiles into the buffer. Our goal is to render an area that
		 * is tilesWide x tilesHigh with the center tile in the middle. Any
		 * locations that fall outside of the tiles array are not rendered and
		 * as such are left black.
		 */
		for (int x = 0; x < tilesWide; x++) {
			for (int y = 0; y < tilesHigh; y++) {
				/*
				 * We multiply everything by two here because the TableScanner
				 * takes two images per width of the camera. By doing this
				 * we increase the effective resolution of this image because
				 * decrease the distance between tiles.
				 * TODO: This should be made configurable.
				 */
				int tileX = centerTileX - (2 * (tilesWide / 2)) + (2 * x);
				int tileY = centerTileY - (2 * (tilesHigh / 2)) + (2 * y);
				// If the position is within the array's bounds we'll render it.
				if (tileX >= 0 && tileX < tiles.length && tileY >= 0 && tileY < tiles[tileX].length && tiles[tileX][tileY] != null) {
					Tile tile = tiles[tileX][tileY];
					BufferedImage image = tile.getImage();
					
					/*
					 * The source images are flipped in both dimensions, and
					 * we're rendering the local array from top to bottom
					 * instead of bottom to top, so we have to flip the images
					 * and then render right to left, bottom to top.
					 */
					int dx1 = image.getWidth() * x;
					int dy1 = image.getHeight() * (tilesHigh - y) - image.getHeight();
					int dx2 = image.getWidth() * x + image.getWidth();
					int dy2 = image.getHeight() * (tilesHigh - y);
					
					int sx1 = image.getWidth();
					int sy1 = image.getHeight();
					int sx2 = 0;
					int sy2 = 0;

					g.drawImage (image,
							dx1, dy1, dx2, dy2,
				            sx1, sy1, sx2, sy2,
				            null);
				}
			}
		}
		
		g.dispose();
	}
	
	private void renderFrame() {
		/*
		 * Get the distance from the center tile to the point we need to render.
		 * TODO: Had to invert these from experimentation. Need to figure out
		 * why and maybe make it configurable. I was too tired to figure it out.
		 */
		double unitsDeltaX = head.getAbsoluteX() - lastCenterTile.getX();
		double unitsDeltaY = lastCenterTile.getY() - head.getAbsoluteY();
		
		/*
		 * Get the distance in pixels from the center tile to the head.
		 */
		double deltaX = unitsDeltaX / getUnitsPerPixel().getX();
		double deltaY = unitsDeltaY / getUnitsPerPixel().getY();
		
		/*
		 * Get the position within the buffer of the top left pixel of the
		 * frame sized chunk we'll grab.
		 */
		double bufferStartX = (buffer.getWidth() / 2) - (frame.getWidth() / 2);
		double bufferStartY = (buffer.getHeight() / 2) - (frame.getHeight() / 2);
		
		/*
		 * Render the frame sized chunk from the center of the buffer offset
		 * by the distance of the head from the center tile to the frame
		 * buffer for final output.
		 */
		Graphics2D g = (Graphics2D) frame.getGraphics();
		g.drawImage(
				buffer, 
				0, 
				0, 
				frame.getWidth(), 
				frame.getHeight(), 
				(int) (bufferStartX + deltaX), 
				(int) (bufferStartY + deltaY),
				(int) (bufferStartX + frame.getWidth() + deltaX), 
				(int) (bufferStartY + frame.getHeight() + deltaY),
				null
				);
		g.dispose();
	}
	
	private Tile getClosestTile(double x, double y) {
		Tile closestTile = tileList.get(0);
		double closestDistance = Math.sqrt(Math.pow(x - closestTile.getX(), 2) + Math.pow(y - closestTile.getY(), 2));
		for (Tile tile : tileList) {
			double distance = Math.sqrt(Math.pow(x - tile.getX(), 2) + Math.pow(y - tile.getY(), 2));
			if (distance <= closestDistance) {
				closestTile = tile;
				closestDistance = distance;
			}
		}
		return closestTile;
	}
	
	@Override
	public Wizard getConfigurationWizard() {
		return null;
	}
	
	public static class Tile {
		private File file;
		private double x, y;
		private int tileX, tileY;
		private SoftReference<BufferedImage> image;
		
		public Tile(double x, double y, File file) {
			this.x = x;
			this.y = y;
			this.file = file;
		}
		
		public synchronized BufferedImage getImage() {
			if (image == null || image.get() == null) {
				try {
					image = new SoftReference<BufferedImage>(ImageIO.read(file));
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			return image.get();
		}
		
		public double getX() {
			return x;
		}

		public void setX(double x) {
			this.x = x;
		}

		public double getY() {
			return y;
		}

		public void setY(double y) {
			this.y = y;
		}

		public File getFile() {
			return file;
		}
		
		public void setFile(File file) {
			this.file = file;
		}

		public int getTileX() {
			return tileX;
		}

		public void setTileX(int tileX) {
			this.tileX = tileX;
		}

		public int getTileY() {
			return tileY;
		}

		public void setTileY(int tileY) {
			this.tileY = tileY;
		}
		
		@Override
		public String toString() {
			return String.format("[%2.3f, %2.3f (%d, %d)]", x, y, tileX, tileY);
		}

		@Override
		public boolean equals(Object obj) {
			Tile other = (Tile) obj;
			return other.x == x && other.y == y;
		}

		@Override
		public int hashCode() {
			return ("" + x + "," + y).hashCode();
		}
	}
}
