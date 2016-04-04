package org.openpnp.vision.experiments;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;

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
    final Location boardLocation = new Location(LengthUnit.Millimeters, 500, 350, 0, 0);
    final Location placementLocation = new Location(LengthUnit.Millimeters, 50, 50, 0, 20);
    final Location bottomVisionOffsets = new Location(LengthUnit.Millimeters, 30, 30, 0, 8);
    Location nozzleLocation = calculateNozzlePosition(boardLocation, placementLocation, bottomVisionOffsets);
    
    int partSize = 50;
    boolean show = true;

    public BottomVisionTest() {
        Thread t = new Thread(() -> {
            while (true) {
                double r = nozzleLocation.getRotation();
                r += 1;
                if (r > 360) {
                    r = 0;
                }
                nozzleLocation = nozzleLocation.derive(null, null, null, r);
                repaint();
                try {
                    Thread.sleep(1000 / 10);
                }
                catch (Exception e) {

                }
            }
        });
//        t.start();
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                show = !show;
                repaint();
            }
        });
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

        if (show) {
//            nozzleLocation = calculateNozzlePosition(boardLocation, placementLocation, bottomVisionOffsets);
            drawNozzle(g2d, nozzleLocation, Color.orange);
        }
        
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
        placementLocation = Utils2D.calculateBoardPlacementLocation(boardLocation, Board.Side.Top,
                0, placementLocation);
        Location placementLocation2 = placementLocation;
        placementLocation = placementLocation.derive(null, null, null, 0d);
        placementLocation = placementLocation.subtract(bottomVisionOffsets);
        // now rotate placementLocation using placementLocation2 as the center point
//        placementLocation = placementLocation.subtract(placementLocation2);
//        placementLocation = placementLocation.rotateXy(bottomVisionOffsets.getRotation());
//        placementLocation = placementLocation.add(placementLocation2);
        
        // try calculating the difference in angle between this current result and the placementLocation2 to see if that helps understand.
        return placementLocation;
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

    public static void main(String[] args) {
        JFrame frame = new JFrame("Bottom Vision Test");
        frame.setSize(1024, 768);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(new BottomVisionTest());
        frame.setVisible(true);
    }
}
