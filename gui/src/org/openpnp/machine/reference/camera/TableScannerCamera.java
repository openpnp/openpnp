package org.openpnp.machine.reference.camera;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import org.openpnp.CameraListener;
import org.openpnp.LengthUnit;
import org.openpnp.model.Location;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;


/**
 * An implementation of Camera that returns a viewport into a field of
 * located images. This allows you to create a virtual camera that
 * uses multiple existing images to lay out an entire table view.
 * 
 * TODO: Allow specifying height which will create a larger tile and then
 * scale it down.
 */
public class TableScannerCamera extends AbstractCamera implements Runnable {
	@Element
	private String tableScannerOutputDirectoryPath;
	
	private int tilesWide = 3;
	private int tilesHigh = 3;
	
	private File tableScannerOutputDirectory;
	
	private double lastX = Double.MIN_VALUE, lastY = Double.MIN_VALUE;
	
	private Tile[][] tiles;
	private List<Tile> tileList = new ArrayList<Tile>();
	
	private BufferedImage buffer, frame;
	
	private Tile lastCenterTile;
	
	private Thread thread;
	
	@Commit
	private void commit() {
		tableScannerOutputDirectory = new File(tableScannerOutputDirectoryPath);
		File[] files = tableScannerOutputDirectory.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File arg0, String arg1) {
				return arg1.contains(".") && arg1.contains(",") && arg1.endsWith(".png");
			}
		});
		BufferedImage templateImage;
		try {
			templateImage = ImageIO.read(files[0]);
		}
		catch (Exception e) {
			throw new Error(e);
		}
		TreeSet<Double> uniqueX = new TreeSet<Double>();
		TreeSet<Double> uniqueY = new TreeSet<Double>();
		Map<Tile, Tile> tileMap = new HashMap<Tile, Tile>();
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
		tiles = new Tile[uniqueX.size()][uniqueY.size()];
		
		int x = 0, y = 0;
		for (Double xPos : uniqueX) {
			y = 0;
			for (Double yPos : uniqueY) {
				Tile tile = tileMap.get(new Tile(xPos, yPos, null));
				tiles[x][y] = tile;
//				System.out.println(String.format("Placing %2.3f, %2.3f at %d, %d", xPos, yPos, x, y));
				tile.setTileX(x);
				tile.setTileY(y);
				y++;
			}
			x++;
		}
		
		buffer = new BufferedImage(
				templateImage.getWidth() * tilesWide,
				templateImage.getHeight() * tilesHigh,
				BufferedImage.TYPE_INT_ARGB);
		
		frame = new BufferedImage(
				templateImage.getWidth(),
				templateImage.getHeight(),
				BufferedImage.TYPE_INT_ARGB);
	}
	
	public void run() {
		while (true) {
			if (lastX != head.getAbsoluteX() || lastY != head.getAbsoluteY()) {
				// Find the closest tile to the head
				Tile closestTile = getClosestTile(head.getAbsoluteX(), head.getAbsoluteY());
				
//				System.out.println(String.format("Closest tile to %2.3f, %2.3f is %s", 
//						head.getX(), 
//						head.getY(),
//						closestTile
//						));
				
				if (closestTile != lastCenterTile) {
					
					// determine where in the map the center tile is
					int centerTileX = closestTile.getTileX();
					int centerTileY = closestTile.getTileY();

					Graphics2D g = (Graphics2D) buffer.getGraphics();
					g.setColor(Color.black);
					g.clearRect(0, 0, buffer.getWidth(), buffer.getHeight());

					// render the tiles into the buffer
					for (int x = 0; x < tilesWide; x++) {
						for (int y = 0; y < tilesHigh; y++) {
							int tileX = centerTileX - (2 * (tilesWide / 2)) + (2 * x);
							int tileY = centerTileY - (2 * (tilesHigh / 2)) + (2 * y);
							if (tileX >= 0 && tileX < tiles.length && tileY >= 0 && tileY < tiles[tileX].length && tiles[tileX][tileY] != null) {
								Tile tile = tiles[tileX][tileY];
								BufferedImage image = tile.getImage();
							
								g.drawImage (image, 
							             image.getWidth() * x,
							             image.getHeight() * (tilesHigh - y) - image.getHeight(),
							             image.getWidth() * x + image.getWidth(),
							             image.getHeight() * (tilesHigh - y),
							             image.getWidth(),
							             image.getHeight(),
							             0,
							             0,
							             null);
							}
						}
					}
					
					g.dispose();
					
					lastCenterTile = closestTile;
				}
				
				// now the buffer is full, we need to find the portion we
				// are interested in
				
				// get the distance from the center tile to the point we
				// need to render
				// TODO: Why do these need to be opposite?
				double unitsDeltaX = head.getAbsoluteX() - closestTile.getX();
				double unitsDeltaY = closestTile.getY() - head.getAbsoluteY();
				
				double deltaX = unitsDeltaX / getUnitsPerPixel().getX();
				double deltaY = unitsDeltaY / getUnitsPerPixel().getY();
				
				double bufferStartX = (tilesWide / 2) * frame.getWidth();
				double bufferStartY = (tilesHigh / 2) * frame.getHeight();
				
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
				
				
				lastX = head.getAbsoluteX();
				lastY = head.getAbsoluteY();
			}
			broadcastCapture(frame);
			try {
				Thread.sleep(1000 / 24);
			}
			catch (Exception e) {
				
			}
		}
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
		return frame;
	}
	
	public static class Tile {
		private File file;
		private double x, y;
		private int tileX, tileY;
		private BufferedImage image;
		
		public Tile(double x, double y, File file) {
			this.x = x;
			this.y = y;
			this.file = file;
		}
		
		public synchronized BufferedImage getImage() {
			if (image == null) {
				try {
					image = ImageIO.read(file);
				}
				catch (Exception e) {
					
				}
			}
			return image;
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
