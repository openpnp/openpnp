/*
 * Copyright (C) 2022 <mark@makr.zone>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.gui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.UIManager;

import org.openpnp.model.Footprint;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Package;
import org.openpnp.model.VisionCompositing;
import org.openpnp.model.VisionCompositing.Corner;
import org.openpnp.model.VisionCompositing.Shot;
import org.openpnp.spi.Camera;

public class VisionCompositingPreview extends JComponent implements MouseMotionListener, MouseListener {

    public VisionCompositingPreview() {
        super();
        addMouseMotionListener(this);
        addMouseListener(this);
    }

    VisionCompositing.Composite composite;

    public VisionCompositing.Composite getComposite() {
        return composite;
    }
    public void setComposite(VisionCompositing.Composite composite) {
        this.composite = composite;
        repaint();
    }

    private Integer xMouse;
    private Integer yMouse;
    private boolean pressedMouse;


    @Override
    public Dimension getPreferredSize() {
        Dimension superDim = super.getPreferredSize();
        int width = (int)Math.max(superDim.getWidth(), 480);
        int height = (int)Math.max(superDim.getHeight(), 320); 
        return new Dimension(width, height);
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        if (composite != null) {
            final Package pkg = composite.getPackage();
            final Footprint footprint = composite.getFootprint();
            final LengthUnit units = composite.getUnits();
            final Camera camera = composite.getCamera(); 
            VisionCompositing compositing = composite.getParent();
            AffineTransform txOld = g2d.getTransform();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            // Get the shape.
            Shape bodyShape = footprint.getBodyShape();
            Shape padsShape = footprint.getPadsShape();
            // Transform to right-hand coordinate system.
            AffineTransform txShape = new AffineTransform();
            txShape.scale(1, -1);
            padsShape = txShape.createTransformedShape(padsShape);
            bodyShape = txShape.createTransformedShape(bodyShape);
            Path2D.Double unionShape = new Path2D.Double();
            unionShape.append(bodyShape, false);
            unionShape.append(padsShape, false);
            Rectangle2D bounds = unionShape.getBounds2D();
            // Compute scale.
            Location upp = camera.getUnitsPerPixel().convertToUnits(footprint.getUnits());
            double cameraWidth = camera.getWidth()*upp.getX();
            double cameraHeight = camera.getHeight()*upp.getY();
            double cameraRoamingRadius = camera.getRoamingRadius().convertToUnits(units).getValue();
            double cameraMaxRadius = Math.min(cameraWidth, cameraHeight) / 2;
            double scale = Math.min((width - 16) / (bounds.getWidth() + cameraMaxRadius*2),
                    (height - 16) / (bounds.getHeight() + cameraMaxRadius*2));
            g2d.setBackground(Color.black);
            g2d.clearRect(0, 0, width, height);
            // Create a transform to scale the shape by
            AffineTransform tx = new AffineTransform(txOld);
            tx.translate(width/2, height/2);
            tx.scale(scale, scale);
            g2d.setTransform(tx);

            // Draw the package.
            g2d.setColor(Color.darkGray);
            g2d.fill(bodyShape);
            g2d.setColor(Color.white);
            g2d.fill(padsShape);

            // Draw fused pads.
            if (pressedMouse && composite.getRectifiedPads() != null) {
                g2d.setColor(new Color(255, 255, 255, 220));
                for (Footprint.Pad pad : composite.getRectifiedPads()) {
                    g2d.fill(pad.getShape());
                }
            }

            List<Shot> shots = composite.getCompositeShots();
            if (composite.getCompositingSolution().isInvalid()) {
                // Invalid shots.
                for (Shot shot : shots) {
                    drawCameraView(g2d, scale, width, height, 0, 0, cameraWidth, cameraHeight, cameraRoamingRadius, Color.red, shot);
                    drawShot(g2d, scale, cameraHeight, cameraWidth, shot, Color.red);
                    g2d.draw(new Line2D.Double(
                            -width/scale, -height/scale,
                            +width/scale, +height/scale));
                }
            }
            else {
                Shot mouseShot = null;
                double bestDistance = Double.POSITIVE_INFINITY; 
                for (Shot shot : shots) {
                    // check if mouse over
                    if (xMouse != null && yMouse != null) {
                        double xMouse = (this.xMouse - width/2.0)/scale;
                        double yMouse = (this.yMouse - height/2.0)/scale;
                        double distance = Math.hypot(xMouse - shot.getX(), yMouse - shot.getY());
                        if (distance < shot.getMaxMaskRadius()
                                && bestDistance > distance) {
                            bestDistance = distance;
                            mouseShot = shot;
                        }
                    }
                }
                if (mouseShot != null) {
                    drawCameraView(g2d, scale, width, height, mouseShot.getX(), mouseShot.getY(), cameraWidth, cameraHeight, cameraRoamingRadius, Color.black, mouseShot);
                    drawShot(g2d, scale, cameraHeight, cameraWidth, mouseShot, Color.yellow);
                }
                else {
                    // Overdraw in reverse. 
                    for (int i = shots.size()-1; i >= 0; --i) {
                        Shot shot = shots.get(i);
                        Color color = shot.isOptional() ? 
                                Color.blue : Color.red;
                        drawShot(g2d, scale, cameraHeight, cameraWidth, shot, color);
                    }
                }
            }

            // Restore
            g2d.setTransform(txOld);
        }
        else {
            Font font = getFont();
            FontMetrics dfm = g2d.getFontMetrics(font);
            Color gridColor = getDefaultGridColor();
            g2d.setFont(font);
            g2d.setColor(gridColor);
            String text = "no data";
            Rectangle2D bounds = dfm.getStringBounds(text, 0, text.length(), g2d);
            g2d.drawString(text, (int)(getWidth() - bounds.getWidth())/2, (int)(getHeight()/2));
        }
    }

    private void drawCameraView(Graphics2D g2d, double scale, int width, int height,
            double cameraX, double cameraY, double cameraWidth, double cameraHeight, double cameraRoamingRadius, Color color, Shot shot) {
        Area shade = new Area(new Rectangle2D.Double(
                -width/scale/2, 
                -height/scale/2, 
                width/scale, 
                height/scale));
        Area camView = new Area(new Rectangle2D.Double(
                cameraX - cameraWidth/2, 
                cameraY - cameraHeight/2, 
                cameraWidth, 
                cameraHeight));
        if (shot != null) {
            double radius = shot.getMaxMaskRadius();
            Area mask = new Area(new Ellipse2D.Double(
                    shot.getX() - radius, shot.getY() - radius,
                    radius*2,  radius*2));
            shade.subtract(mask);
            g2d.setColor(new Color(color.getRed()/4, color.getGreen()/4, color.getBlue()/4, 180));
            g2d.fill(shade);
            g2d.setColor(new Color(255, 255, 255, 32));
            g2d.fill(mask);
            float stroke = (float)(1.0/scale);
            g2d.setStroke(new BasicStroke(stroke));
            g2d.setColor(color);
            drawCircle(g2d, shot.getX(), shot.getY(), shot.getMinMaskRadius());
        }
        else {
            g2d.setColor(new Color(255, 255, 255, 32));
            g2d.fill(camView);
        }
        shade.subtract(camView);
        g2d.setColor(new Color(color.getRed()/4, color.getGreen()/4, color.getBlue()/4, 64));
        g2d.fill(shade);
        float stroke = (float)(1.0/scale);
        g2d.setStroke(new BasicStroke(stroke));
        g2d.setColor(Color.gray);
        g2d.draw(camView);
        if (cameraRoamingRadius > 0) {
            // Draw the roaming circle. 
            stroke = (float)(composite.getTolerance());
            float[] dash1 = { (float) composite.getTolerance(), (float) composite.getTolerance()*0.25f  };
            g2d.setStroke(new BasicStroke(stroke,
                    BasicStroke.CAP_BUTT, 
                    BasicStroke.JOIN_ROUND, 
                    dash1[0], 
                    dash1,
                    0f));
            g2d.setColor(new Color(255, 0, 0, 128));
            drawCircle(g2d, shot.getX(), shot.getY(), cameraRoamingRadius+composite.getTolerance()/2);
        }
    }

    private void drawShot(Graphics2D g2d, double scale, double cameraHeight, double cameraWidth, Shot shot, Color color) {
        float stroke = (float)(2.0/scale);
        g2d.setStroke(new BasicStroke(stroke));
        g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 128));
        drawCircle(g2d, shot.getX(), shot.getY(), shot.getMinMaskRadius());
        g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 64));
        drawCircle(g2d, shot.getX(), shot.getY(), shot.getMaxMaskRadius());
        for (Corner corner : shot.getCorners()) {
            stroke = (float)(4.0/scale);
            g2d.setStroke(new BasicStroke(stroke));
            g2d.setColor(color);
            double x = corner.getX();
            double y = corner.getY();
            int xSign = corner.getXSign();
            int ySign = corner.getYSign();
            g2d.draw(new Line2D.Double(
                    x + xSign*stroke,                    y + ySign*stroke,
                    x - xSign*corner.getMinMaskRadius() + xSign*stroke/2, y + ySign*stroke));
            g2d.draw(new Line2D.Double(
                    x + xSign*stroke,  y,
                    x + xSign*stroke,  y - ySign*corner.getMinMaskRadius() + ySign*stroke/2));
            if (pressedMouse) {
                stroke = (float)(0.5/scale);
                g2d.setStroke(new BasicStroke(stroke));
                g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 64));
                //drawCircle(g2d, corner.getX(), corner.getY(), corner.getMinMaskRadius());
                drawCircle(g2d, corner.getX(), corner.getY(), corner.getMaxMaskRadius());
            }
        }
    }

    private void drawCircle(Graphics2D g2d, double x, double y, double r) {
        g2d.draw(new Ellipse2D.Double(x - r, y - r, r*2, r*2));
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }
    @Override
    public void mouseMoved(MouseEvent e) {
        xMouse = e.getX();
        yMouse = e.getY();
        repaint();
    }
    @Override
    public void mouseClicked(MouseEvent e) {
        //pressedMouse = true;
        repaint();
    }
    @Override
    public void mousePressed(MouseEvent e) {
        pressedMouse = true;
        repaint();
    }
    @Override
    public void mouseReleased(MouseEvent e) {
        pressedMouse = false;
        repaint();
    }
    @Override
    public void mouseEntered(MouseEvent e) {
    }
    @Override
    public void mouseExited(MouseEvent e) {
        xMouse = null;
        yMouse = null;
        pressedMouse = false;
        repaint();
    }

    public static Color getDefaultGridColor() {
        Color gridColor = UIManager.getColor ( "PasswordField.capsLockIconColor" );
        if (gridColor == null) {
            gridColor = new Color(0, 0, 0, 64);
        } else {
            gridColor = new Color(gridColor.getRed(), gridColor.getGreen(), gridColor.getBlue(), 64);
        }
        return gridColor;
    }
}
