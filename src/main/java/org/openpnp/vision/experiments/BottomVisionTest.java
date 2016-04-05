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
//    Location boardLocation = new Location(LengthUnit.Millimeters, 500, 350, 0, 5);
//    Location placementLocation = new Location(LengthUnit.Millimeters, 50, 50, 0, 5);
//    Location bottomVisionOffsets = new Location(LengthUnit.Millimeters, 200, 200, 0, 5);
    
//    Location boardLocation = new Location(LengthUnit.Millimeters, 500, 350, 0, 15);
//    Location placementLocation = new Location(LengthUnit.Millimeters, 50, 50, 0, 45);
//    Location bottomVisionOffsets = new Location(LengthUnit.Millimeters, 200, 200, 0, 32);
    
    // so the old algorithm works if there is a boardlocation rotate but not a placement locatin rotate
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
//                double centerX = bottomVisionOffsets.getX();
//                double centerY = bottomVisionOffsets.getY();
//                double point2x = 
//                double newX = centerX + (point2x-centerX)*Math.cos(x) - (point2y-centerY)*Math.sin(x);
//                double newY = centerY + (point2x-centerX)*Math.sin(x) + (point2y-centerY)*Math.cos(x);                
                
                nozzleLocation = nozzleLocation.subtractWithRotation(new Location(LengthUnit.Millimeters, 0, 0, 0, e.getWheelRotation() * 0.1));
                System.out.println(nozzleLocation);
                repaint();
            }
        });
    }
    
    private static int rand(int min, int max) {
        return new Random().nextInt(max) + min;
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

//            nozzleLocation = calculateNozzlePosition(boardLocation, placementLocation, bottomVisionOffsets);
            drawNozzle(g2d, nozzleLocation, Color.orange);
        
//        drawNozzle(g2d, new Location(LengthUnit.Millimeters, 100, 100, 0, 90), Color.white);
        
        // end work

        g2d.setTransform(tx);
    }
    
//    private static Location calculateNozzlePositionOld(Location boardLocation, Location placementLocation, Location bottomVisionOffsets) {
//        Location originalPlacementLocation = placementLocation;
//        if (bottomVisionOffsets != null) {
//            // derotate the offset using the identified angle
//            bottomVisionOffsets =
//                    bottomVisionOffsets.rotateXy(-bottomVisionOffsets.getRotation());
//            // apply the offset and the angle to the placement
//            placementLocation = placementLocation.subtractWithRotation(bottomVisionOffsets);
//        }
//        placementLocation = Utils2D.calculateBoardPlacementLocation(boardLocation, Board.Side.Top, 0, placementLocation);
//        return placementLocation;
//    }


    // think of the part (offsets) sitting on the placement location driving the nozzle around in a circle around it
    // think of the part sitting on the placement location driving the nozzle around in a circle around it
    // think of the part sitting on the placement location driving the nozzle around in a circle around it
    private static Location calculateNozzlePosition(Location boardLocation, Location placementLocation, Location bottomVisionOffsets) {
        Location location  = Utils2D.calculateBoardPlacementLocation(boardLocation, Board.Side.Top,
                0, placementLocation);
        location = location.derive(null, null, null, 0d);
        location = location.subtract(bottomVisionOffsets);
        // location now represents the placement's final location
        
        // partLocation is the location of the part on the nozzle
        Location partLocation = location.add(bottomVisionOffsets);
        
        // we want to rotate location by the difference in degrees between the bottom vision offsets
        // and the total placement rotation
        location = rotateXyCenterPoint(location, partLocation, boardLocation.getRotation() + placementLocation.getRotation() - bottomVisionOffsets.getRotation());
        
        // and we set the angle to the angle we rotated by
        location = location.derive(null, null, null, boardLocation.getRotation() + placementLocation.getRotation() - bottomVisionOffsets.getRotation());
        
        System.out.println(location);
        
        // empirical
        // (522.000000, 371.000000, 0.000000, 26.700000 mm)
        // calculated
        // (520.844653, 371.369462, 0.000000, 25.000000 mm)

        
        return location;
    }
    
    private static Location rotateXyCenterPoint(Location location, Location center, double angle) {
        location = location.subtract(center);
        location = location.rotateXy(angle);
        location = location.add(center);
        return location;
    }

    
    // this code gets the position right, but the angle is wrong by what looks like the placement angle    
//    Location originalPlacementLocation = placementLocation;
//    if (bottomVisionOffsets != null) {
//        // derotate the offset using the identified angle
//        bottomVisionOffsets =
//                bottomVisionOffsets.rotateXy(-bottomVisionOffsets.getRotation());
//        // apply the offset and the angle to the placement
//        placementLocation = placementLocation.subtractWithRotation(bottomVisionOffsets);
//    }
//    placementLocation = Utils2D.calculateBoardPlacementLocation(boardLocation, Board.Side.Top, 0, placementLocation);
//    // subtract placementLocation's original angle
//    placementLocation = placementLocation.subtractWithRotation(originalPlacementLocation.derive(0d, 0d, 0d, null));
//    return placementLocation;

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
        drawRectangle(g, location, partSize, partSize, color);

        g.translate(location.getX(), location.getY());
        g.rotate(Math.toRadians(location.getRotation()));

        // draw the part
        drawCrossHair(g, bottomVisionOffsets, new HslColor(color).getComplementary());
        drawRectangle(g, bottomVisionOffsets, partSize, partSize, new HslColor(color).getComplementary());

        g.setTransform(tx);
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

    private void drawCircle(Graphics2D g, double x, double y, double diameter, Color color) {
        g.setColor(color);
        g.drawArc((int) (x - diameter / 2), (int) (y - diameter / 2), (int) diameter,
                (int) diameter, 0, 360);
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
