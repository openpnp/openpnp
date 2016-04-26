package org.openpnp.vision.experiments;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.util.Random;

import javax.swing.JComponent;
import javax.swing.JFrame;

import org.openpnp.model.Board;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Point;
import org.openpnp.util.HslColor;
import org.openpnp.util.Utils2D;

public class BottomVisionTest extends JComponent {
    private static final Random random = new Random(0xdeadbeef);
    
    Location boardLocation = new Location(LengthUnit.Millimeters, 500, 350, 0, 15);
    Location placementLocation = new Location(LengthUnit.Millimeters, 50, 50, 0, 20);
    Location bottomVisionOffsets = new Location(LengthUnit.Millimeters, 30, 30, 0, 10);
    Location nozzleLocation = calculateNozzlePosition(boardLocation, placementLocation, bottomVisionOffsets);
    
    int partSize = 50;

    public BottomVisionTest() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Random r = new Random();
                boardLocation = new Location(
                        LengthUnit.Millimeters, 
                        rand(200, 600),
                        rand(100, 500),
                        0,
                        rand(0, 90));
                placementLocation = new Location(
                        LengthUnit.Millimeters, 
                        rand(10, 100),
                        rand(10, 100),
                        0,
                        rand(0, 90));
                bottomVisionOffsets = new Location(
                        LengthUnit.Millimeters, 
                        rand(10, 100),
                        rand(10, 100),
                        0,
                        rand(0, 45));
                nozzleLocation = calculateNozzlePosition(boardLocation, placementLocation, bottomVisionOffsets);
                repaint();
            }
        });
        
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                nozzleLocation = nozzleLocation.derive((double) e.getX(), (double) (getHeight() - e.getY()), 0d, null);
                System.out.println(nozzleLocation);
                repaint();
            }
        });
        
        addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                nozzleLocation = nozzleLocation.subtractWithRotation(new Location(LengthUnit.Millimeters, 0, 0, 0, e.getWheelRotation() * 0.1));
                System.out.println(nozzleLocation);
                repaint();
            }
        });
    }
    
    private static int rand(int min, int max) {
        return random.nextInt(max) + min;
    }

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        AffineTransform tx = g2d.getTransform();

        g2d.translate(0, getHeight());
        g2d.scale(1, -1);

        g2d.setColor(new Color(0.9f, 0.9f, 0.9f));
        g2d.fillRect(0, 0, getWidth(), getHeight());
        g2d.setColor(new Color(0.8f, 0.8f, 0.8f));
        // draw horizontal lines
        for (int y = 0; y < getHeight(); y += 10) {
            g2d.drawLine(0, y, getWidth(), y);
        }
        // draw vertical lines
        for (int x = 0; x < getWidth(); x += 10) {
            g2d.drawLine(x, 0, x, getHeight());
        }

        g2d.setStroke(new BasicStroke(2));

        // do work

        drawBoard(g2d);

        drawNozzle(g2d, nozzleLocation, Color.orange);
        
        // end work

        g2d.setTransform(tx);
    }
    
    private static Location calculateNozzlePosition(Location boardLocation, Location placementLocation, Location bottomVisionOffsets) {
        // The calculated global placement location
        Location placementFinalLocation  = Utils2D.calculateBoardPlacementLocation(boardLocation, Board.Side.Top,
                0, placementLocation);

        // Rotate the point 0,0 using the bottom offsets as a center point by the angle that is
        // the difference between the bottom vision angle and the calculated global placement angle.
        Location location = new Location(LengthUnit.Millimeters).rotateXyCenterPoint(
                bottomVisionOffsets, 
                placementFinalLocation.getRotation() - bottomVisionOffsets.getRotation());
        
        // Set the angle to the difference mentioned above, aligning the part to the same angle as
        // the placement.
        location = location.derive(null, null, null, placementFinalLocation.getRotation() - bottomVisionOffsets.getRotation());
        
        // Add the placement final location to move our local coordinate into global space
        location = location.add(placementFinalLocation);

        // Subtract the bottom vision offsets to move the part to the final location, instead of
        // the nozzle.
        location = location.subtract(bottomVisionOffsets);
        
        return location;
    }
    
    private void drawBoard(Graphics2D g) {
        AffineTransform tx = g.getTransform();

        // draw the board location
        drawCrossHair(g, boardLocation, Color.green);

        g.translate(boardLocation.getX(), boardLocation.getY());
        g.rotate(Math.toRadians(boardLocation.getRotation()));

        // draw the placement location
        drawCrossHair(g, placementLocation, Color.white);

        drawRectangle(g, placementLocation, partSize, partSize, Color.red);

        g.setTransform(tx);
    }

    private void drawNozzle(Graphics2D g, Location location, Color color) {
        AffineTransform tx = g.getTransform();

        // draw the nozzle
        drawCrossHair(g, location, color);
        drawCircle(g, location, partSize, color);

        g.translate(location.getX(), location.getY());
        g.rotate(Math.toRadians(location.getRotation()));

        // draw the part
        drawCrossHair(g, bottomVisionOffsets, new HslColor(color).getComplementary());
        drawRectangle(g, bottomVisionOffsets, partSize, partSize, new HslColor(color).getComplementary());

        g.setTransform(tx);
    }

    private void drawCircle(Graphics2D g, Location location, double diameter, Color color) {
        g.setColor(color);
        double x = location.getX();
        double y = location.getY();
        g.drawArc((int) (x - diameter / 2), (int) (y - diameter / 2), (int) diameter,
                (int) diameter, 0, 360);
    }

    private void drawCrossHair(Graphics2D g, Location location, Color color) {
        int size = 20;
        Point p1;
        Point p2;
        g.setColor(color);
        p1 = Utils2D.rotatePoint(new Point(-size, 0), location.getRotation());
        p2 = Utils2D.rotatePoint(new Point(size, 0), location.getRotation());
        g.drawLine((int) (location.getX() + p1.getX()), (int) (location.getY() + p1.getY()),
                (int) (location.getX() + p2.getX()), (int) (location.getY() + p2.getY()));
        p1 = Utils2D.rotatePoint(new Point(0, -size), location.getRotation());
        p2 = Utils2D.rotatePoint(new Point(0, size), location.getRotation());
        g.drawLine((int) (location.getX() + p1.getX()), (int) (location.getY() + p1.getY()),
                (int) (location.getX() + p2.getX()), (int) (location.getY() + p2.getY()));
        g.setColor(Color.red);
        p1 = Utils2D.rotatePoint(new Point(0, size), location.getRotation());
        p2 = Utils2D.rotatePoint(new Point(0, 0), location.getRotation());
        g.drawLine((int) (location.getX() + p1.getX()), (int) (location.getY() + p1.getY()),
                (int) (location.getX() + p2.getX()), (int) (location.getY() + p2.getY()));
    }

    private void drawRectangle(Graphics2D g, Location location, double width, double height,
            Color color) {
        g.setColor(color);
        Point p1, p2;
        p1 = Utils2D.rotatePoint(new Point(-width / 2, height / 2), location.getRotation());
        p2 = Utils2D.rotatePoint(new Point(width / 2, height / 2), location.getRotation());
        g.drawLine((int) (location.getX() + p1.getX()), (int) (location.getY() + p1.getY()),
                (int) (location.getX() + p2.getX()), (int) (location.getY() + p2.getY()));

        p1 = Utils2D.rotatePoint(new Point(width / 2, height / 2), location.getRotation());
        p2 = Utils2D.rotatePoint(new Point(width / 2, -height / 2), location.getRotation());
        g.drawLine((int) (location.getX() + p1.getX()), (int) (location.getY() + p1.getY()),
                (int) (location.getX() + p2.getX()), (int) (location.getY() + p2.getY()));

        p1 = Utils2D.rotatePoint(new Point(width / 2, -height / 2), location.getRotation());
        p2 = Utils2D.rotatePoint(new Point(-width / 2, -height / 2), location.getRotation());
        g.drawLine((int) (location.getX() + p1.getX()), (int) (location.getY() + p1.getY()),
                (int) (location.getX() + p2.getX()), (int) (location.getY() + p2.getY()));

        p1 = Utils2D.rotatePoint(new Point(-width / 2, -height / 2), location.getRotation());
        p2 = Utils2D.rotatePoint(new Point(-width / 2, height / 2), location.getRotation());
        g.drawLine((int) (location.getX() + p1.getX()), (int) (location.getY() + p1.getY()),
                (int) (location.getX() + p2.getX()), (int) (location.getY() + p2.getY()));

    }

    public static void main(String[] args) throws Exception {
        JFrame frame = new JFrame("Bottom Vision Test");
        frame.setSize(1024, 768);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(new BottomVisionTest());
        frame.setVisible(true);
    }
}
