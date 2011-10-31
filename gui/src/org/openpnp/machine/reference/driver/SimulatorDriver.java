package org.openpnp.machine.reference.driver;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.openpnp.Configuration;
import org.openpnp.Job;
import org.openpnp.Job.JobBoard;
import org.openpnp.LengthUnit;
import org.openpnp.Location;
import org.openpnp.Outline;
import org.openpnp.Part;
import org.openpnp.Part.FeederLocation;
import org.openpnp.Placement;
import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.util.LengthUtil;
import org.openpnp.util.Utils2D;
import org.w3c.dom.Node;


@SuppressWarnings("serial")
public class SimulatorDriver extends JFrame implements ReferenceDriver {
	private SimulatorPanel panel;
	
	public SimulatorDriver() throws Exception {
		getContentPane().add(panel = new SimulatorPanel());
		pack();
		setVisible(true);
	}
	
	@Override 
	public void configure(Node n) throws Exception {
		
	}
	
	@Override
	public void prepareJob(Configuration configuration, Job job) throws Exception {
		panel.setConfiguration(configuration);
		panel.setJob(job);
	}
	
	@Override
	public void home(ReferenceHead head) throws Exception {
		panel.moveTo(head, 0, 0, 0, 0);
	}

	@Override
	public void moveTo(ReferenceHead head, double x, double y, double z, double c) throws Exception {
		panel.moveTo(head, x, y, z, c);
	}

	@Override
	public void pick(ReferenceHead head, Part part) throws Exception {
		panel.pick(part);
	}

	@Override
	public void place(ReferenceHead head) throws Exception {
		panel.place();
	}

	@Override
	public void actuate(ReferenceHead head, int index, boolean on) throws Exception {
		panel.actuate(index, on);
	}

	public class SimulatorPanel extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener {
		// TODO add multiple head support, just for fun
		
		private Configuration configuration;
		private Job job;
		
		private double x, y, z, c;
		private double mmPerSecond = 250; // 250
		private int operationDelay = 200; // 200 
		
		private double scale = 1.0;
		private int offsetX, offsetY;
		private int dragStartX, dragStartY;
		private int offsetStartX, offsetStartY;
		private int mouseX, mouseY;

		private Part pickedPart;
		private boolean pinExtended;
		
		public SimulatorPanel() {
			setPreferredSize(new Dimension(400, 600));
			setBackground(Color.black);
			addMouseListener(this);
			addMouseMotionListener(this);
			addMouseWheelListener(this);
		}
		
		public void setConfiguration(Configuration configuration) {
			this.configuration = configuration;
			repaint();
		}
		
		public void setJob(Job job) {
			this.job = job;
			repaint();
		}
		
		public void moveTo(ReferenceHead head, double x, double y, double z, double c) {
			// angles over 360* are silly
			c = c % 360.0;
			
			// if the travel is more than 180* we go the opposite direction
			if (c > 180) {
				c = (360 - c) * -1;
			}
			
			double x1 = this.x;
			double y1 = this.y;
			double z1 = this.z;
			double c1 = this.c;
			double x2 = x;
			double y2 = y;
			double z2 = z;
			double c2 = c;
			
			// distances to travel in each axis
			double vx = x2 - x1;
			double vy = y2 - y1;
			double vz = z2 - z1;
			double va = c2 - c1;
			
			double mag = Math.sqrt(vx*vx + vy*vy);

			double distance = 0;
			
			while (distance < mag) {
				this.x = x1 + (vx / mag * distance);
				this.y = y1 + (vy / mag * distance);
				this.c = c1 + (va / mag * distance);
				
				head.updateCoordinates(this.x, this.y, this.z, this.c);
				
				repaint();
				
				try {
					Thread.sleep((int) (1000 / mmPerSecond));
				}
				catch (Exception e) {
					
				}
				
				distance = Math.min(mag, distance + 1);
			}
			
			this.x = x;
			this.y = y;
			this.c = c;
			
			head.updateCoordinates(this.x, this.y, this.z, this.c);
			
			repaint();

			mag = Math.abs(vz);
			
			distance = 0;
			
			while (distance < mag) {
				this.z = z1 + (vz / mag * distance);

				head.updateCoordinates(this.x, this.y, this.z, this.c);

				repaint();
				
				try {
					Thread.sleep((int) (1000 / mmPerSecond));
				}
				catch (Exception e) {
					
				}
				
				distance = Math.min(mag, distance + 1);
			}
			
			this.z = z;

			head.updateCoordinates(this.x, this.y, this.z, this.c);
			
			repaint();
		}
		
		public void pick(Part part) {
			try {
				Thread.sleep(operationDelay);
			}
			catch (Exception e) {
				
			}
			pickedPart = part;
			repaint();
		}
		
		public void place() {
			try {
				Thread.sleep(operationDelay);
			}
			catch (Exception e) {
				
			}
			pickedPart = null;
			repaint();
		}
		
		public void actuate(int index, boolean on) {
			try {
				Thread.sleep(operationDelay);
			}
			catch (Exception e) {
				
			}
			pinExtended = (index == 0 && on);
			repaint();
		}
		
		@Override
		public void paint(Graphics g) {
			super.paint(g);
			
			Graphics2D g2d = (Graphics2D) g;
			
			// draw the current position of the machine in text
			g.setColor(Color.white);
			g.drawString(
				String.format("X %3.3f, Y %3.3f, Z %3.3f, C %3.3f", x, y, z, c), 
				10, 
				20);
			
			// draw some stats about the display
			g.setColor(Color.red);
			g.drawString(
				String.format("offsetX %d, offsetY %d, scale %3.3f", offsetX, offsetY, scale), 
				10, 
				40);

			// draw some stats about the mouse
			g.setColor(Color.yellow);
			g.drawString(
				String.format("mouseX %d, mouseY %d", mouseX, mouseY), 
				10, 
				60);
			
			g2d.translate(offsetX, offsetY);
			
			if (job != null && configuration != null) {
			
				// draw the boards
				for (JobBoard jobBoard : job.getBoards()) {
					Outline o = jobBoard.getBoard().getOutline();
					Location l = jobBoard.getLocation();
					
					l = LengthUtil.convertLocation(l, LengthUnit.Millimeters);
					
					o = LengthUtil.convertOutline(o, LengthUnit.Millimeters);
					o = Utils2D.rotateTranslateScaleOutline(o, l.getRotation(), l.getX(), l.getY(), scale);
					
					g.setColor(Color.green);
					drawOutline(g, o);
				}
				
				// draw the placements
				for (JobBoard jobBoard : job.getBoards()) {
					for (Placement placement : jobBoard.getBoard().getPlacements()) {
						Outline o = placement.getPart().getPackage().getOutline();
						Location boardLocation = jobBoard.getLocation();
						Location placementLocation = placement.getLocation();
	
						boardLocation = LengthUtil.convertLocation(boardLocation, LengthUnit.Millimeters);
						placementLocation = LengthUtil.convertLocation(placementLocation, LengthUnit.Millimeters);
						
						o = LengthUtil.convertOutline(o, LengthUnit.Millimeters);
	
						o = Utils2D.rotateTranslateScaleOutline(o, placementLocation.getRotation(), placementLocation.getX(), placementLocation.getY(), 1.0);
						o = Utils2D.rotateTranslateScaleOutline(o, boardLocation.getRotation(), boardLocation.getX(), boardLocation.getY(), scale);
						
						g.setColor(Color.cyan);
						drawOutline(g, o);
						
						// draw the crosshairs representing the position and rotation of the placement
						Point2D.Double chLeft = new Point2D.Double(-1, 0);
						Point2D.Double chRight = new Point2D.Double(1, 0);
						Point2D.Double chTop = new Point2D.Double(0, 1);
						Point2D.Double chBottom = new Point2D.Double(0, -1);
						Point2D.Double chCenter = new Point2D.Double(0, 0);
						
						chLeft = Utils2D.rotateTranslateScalePoint(chLeft, placementLocation.getRotation(), placementLocation.getX(), placementLocation.getY(), 1.0);
						chRight = Utils2D.rotateTranslateScalePoint(chRight, placementLocation.getRotation(), placementLocation.getX(), placementLocation.getY(), 1.0);
						chTop = Utils2D.rotateTranslateScalePoint(chTop, placementLocation.getRotation(), placementLocation.getX(), placementLocation.getY(), 1.0);
						chBottom = Utils2D.rotateTranslateScalePoint(chBottom, placementLocation.getRotation(), placementLocation.getX(), placementLocation.getY(), 1.0);
						chCenter = Utils2D.rotateTranslateScalePoint(chCenter, placementLocation.getRotation(), placementLocation.getX(), placementLocation.getY(), 1.0);
						
						chLeft = Utils2D.rotateTranslateScalePoint(chLeft, boardLocation.getRotation(), boardLocation.getX(), boardLocation.getY(), scale);
						chRight = Utils2D.rotateTranslateScalePoint(chRight, boardLocation.getRotation(), boardLocation.getX(), boardLocation.getY(), scale);
						chTop = Utils2D.rotateTranslateScalePoint(chTop, boardLocation.getRotation(), boardLocation.getX(), boardLocation.getY(), scale);
						chBottom = Utils2D.rotateTranslateScalePoint(chBottom, boardLocation.getRotation(), boardLocation.getX(), boardLocation.getY(), scale);
						chCenter = Utils2D.rotateTranslateScalePoint(chCenter, boardLocation.getRotation(), boardLocation.getX(), boardLocation.getY(), scale);
						
						g.setColor(Color.red);
						g.drawLine((int) chLeft.getX(), getHeight() - (int) chLeft.getY(), (int) chRight.getX(), getHeight() - (int) chRight.getY());
						g.drawLine((int) chTop.getX(), getHeight() - (int) chTop.getY(), (int) chBottom.getX(), getHeight() - (int) chBottom.getY());
						g.setColor(Color.green);
						g.drawLine((int) chTop.getX(), getHeight() - (int) chTop.getY(), (int) chCenter.getX(), getHeight() - (int) chCenter.getY());
					}
				}
				
				// draw the parts in feeders
				for (Part part : configuration.getParts()) {
					for (FeederLocation fl : part.getFeederLocations()) {
						Outline o = part.getPackage().getOutline();
						Location l = fl.getLocation();
						
						o = LengthUtil.convertOutline(o, LengthUnit.Millimeters);
						o = Utils2D.rotateTranslateScaleOutline(o, l.getRotation(), l.getX(), l.getY(), scale);
						
						g.setColor(Color.lightGray);
						drawOutline(g, o);
					}
				}
			}
			
			double heightScaleFactor = 1 + ((100 - z) * 0.03);
			
			// draw the crosshairs representing the position and rotation of the head
			Point2D.Double chLeft = new Point2D.Double(-10, 0);
			Point2D.Double chRight = new Point2D.Double(10, 0);
			Point2D.Double chTop = new Point2D.Double(0, 10);
			Point2D.Double chBottom = new Point2D.Double(0, -10);
			Point2D.Double chCenter = new Point2D.Double(0, 0);
			
			chLeft = Utils2D.rotateTranslateScalePoint(chLeft, 0, 0, 0, heightScaleFactor);
			chRight = Utils2D.rotateTranslateScalePoint(chRight, 0, 0, 0, heightScaleFactor);
			chTop = Utils2D.rotateTranslateScalePoint(chTop, 0, 0, 0, heightScaleFactor);
			chBottom = Utils2D.rotateTranslateScalePoint(chBottom, 0, 0, 0, heightScaleFactor);
			chCenter = Utils2D.rotateTranslateScalePoint(chCenter, 0, 0, 0, heightScaleFactor);
			
			chLeft = Utils2D.rotateTranslateScalePoint(chLeft, c, x, y, scale);
			chRight = Utils2D.rotateTranslateScalePoint(chRight, c, x, y, scale);
			chTop = Utils2D.rotateTranslateScalePoint(chTop, c, x, y, scale);
			chBottom = Utils2D.rotateTranslateScalePoint(chBottom, c, x, y, scale);
			chCenter = Utils2D.rotateTranslateScalePoint(chCenter, c, x, y, scale);
			
			if (pickedPart != null) {
				// If we have a part on the nozzle we show it's outline while we carry it
				Outline outline = pickedPart.getPackage().getOutline();
				outline = LengthUtil.convertOutline(outline, LengthUnit.Millimeters);
				outline = Utils2D.rotateTranslateScaleOutline(outline, 0, 0, 0, heightScaleFactor);
				outline = Utils2D.rotateTranslateScaleOutline(outline, c, x, y, scale);
				
				g.setColor(Color.white);
				drawOutline(g, outline);
			}
			g.setColor(Color.red);
			g.drawLine((int) chLeft.getX(), getHeight() - (int) chLeft.getY(), (int) chRight.getX(), getHeight() - (int) chRight.getY());
			g.drawLine((int) chTop.getX(), getHeight() - (int) chTop.getY(), (int) chBottom.getX(), getHeight() - (int) chBottom.getY());
			g.setColor(Color.green);
			g.drawLine((int) chTop.getX(), getHeight() - (int) chTop.getY(), (int) chCenter.getX(), getHeight() - (int) chCenter.getY());
			
			if (pinExtended) {
				// draw the index pin
				Point2D.Double pin = new Point2D.Double(0, -48);
				pin = Utils2D.rotateTranslateScalePoint(pin, c, x, y, scale);
				g.setColor(Color.magenta);
				g.fillArc((int) pin.getX(), getHeight() - (int) pin.getY(), (int) (4 * heightScaleFactor), (int) (4 * heightScaleFactor), 0, 360);
			}
		}
		
		private void drawOutline(Graphics g, Outline o) {
			for (int i = 0; i < o.getPoints().size() - 1; i++) {
				Point2D.Double p1 = o.getPoints().get(i);
				Point2D.Double p2 = o.getPoints().get(i + 1);
				
				g.drawLine((int) p1.getX(), getHeight() - (int) p1.getY(), (int) p2.getX(), getHeight() - (int) p2.getY());
			}
			
			Point2D.Double p1 = o.getPoints().get(o.getPoints().size() - 1);
			Point2D.Double p2 = o.getPoints().get(0);
			
			g.drawLine((int) p1.getX(), getHeight() - (int) p1.getY(), (int) p2.getX(), getHeight() - (int) p2.getY());
		}
		
		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			scale += (e.getUnitsToScroll() * 0.1 * -1);
			scale = Math.max(1.0, scale);

			if (scale == 1.0) {
				offsetX = 0;
				offsetY = 0;
			}
			else {
				// TODO there is a weird bug happening on zoom out, things end up
				// where I would not expect them to be
				offsetX += e.getX() * (e.getUnitsToScroll() * 0.1);
				offsetY += (getHeight() - e.getY()) * (e.getUnitsToScroll() * 0.1 * -1);
			}
			
			repaint();
		}
		
		@Override
		public void mouseMoved(MouseEvent e) {
			mouseX = e.getX();
			mouseY = getHeight() - e.getY();
			repaint();
		}
		
		@Override
		public void mouseDragged(MouseEvent e) {
			offsetX = offsetStartX + ((dragStartX - e.getX()) * -1);
			offsetY = offsetStartY + ((dragStartY - e.getY()) * -1);
			repaint();
		}
		
		@Override
		public void mouseReleased(MouseEvent arg0) {
		}
		
		@Override
		public void mousePressed(MouseEvent arg0) {
			dragStartX = arg0.getX();
			dragStartY = arg0.getY();
			
			offsetStartX = offsetX;
			offsetStartY = offsetY;
		}
		
		@Override
		public void mouseExited(MouseEvent arg0) {
		}
		
		@Override
		public void mouseEntered(MouseEvent arg0) {
		}
		
		@Override
		public void mouseClicked(MouseEvent arg0) {
		}
	}
}
