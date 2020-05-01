/*
 * Copyright (C) 2020 <mark@makr.zone>
 * inspired and based on work by
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Set;

import javax.swing.JComponent;

import org.openpnp.util.SimpleGraph;
import org.openpnp.util.SimpleGraph.DataRow;
import org.openpnp.util.SimpleGraph.DataScale;

@SuppressWarnings("serial")
public class SimpleGraphView extends JComponent implements MouseMotionListener, MouseListener {  

    private SimpleGraph graph;
    private final Dimension preferredSize = new Dimension(100, 80);
    private Integer xMouse;
    private Integer yMouse;
    private Double selectedX;

    public SimpleGraphView() {
        addMouseMotionListener(this);
        addMouseListener(this);
    }
    public SimpleGraphView(SimpleGraph graph) {
        this();
        this.graph = graph;
    }

    public SimpleGraph getGraph() {
        return graph;
    }
    public void setGraph(SimpleGraph graph) {
        this.graph = graph;
        repaint();
    }

    public Double getSelectedX() {
        return selectedX;
    }
    public void setSelectedX(Double selectedX) {
        Object oldValue = this.selectedX;
        this.selectedX = selectedX;
        firePropertyChange("selectedX", oldValue, selectedX);
    }

    protected String formatNumber(double y, double unit) {
        // format numbers without necessary digits (I'm sure there's a better/simpler way)
        int digits = (int)Math.floor(Math.log10(unit)+0.1);
        StringBuilder formatPattern = new StringBuilder();
        if (digits < 0) {
            formatPattern.append("0.");
            while (digits++ < 0) {
                formatPattern.append("0");
            }
        }
        else {
            formatPattern.append("0");
        }
        DecimalFormat format = new DecimalFormat(formatPattern.toString());
        format.setRoundingMode(RoundingMode.HALF_UP); 
        return format.format(y);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Font font = getFont();
        FontMetrics dfm = g2d.getFontMetrics(font);
        int fontLineHeight = dfm.getAscent()+1; // numbers are all ascent
        int fontAscent = dfm.getAscent();
        g2d.setFont(font);
        int w = getWidth();
        int h = getHeight();
        if (graph != null) {
            boolean firstScale = true;
            for (DataScale dataScale : graph.getDataScales()) {
                Point2D.Double min = graph.getMinimum(dataScale);
                Point2D.Double max = graph.getMaximum(dataScale);
                if (min != null && max != null && (max.y-min.y) > 0.0 && (max.x-min.x) > 0.0) {
                    double xOrigin = (w-1)*graph.getRelativePaddingLeft();
                    double xScale = (w-1)*(1.0-graph.getRelativePaddingLeft()-graph.getRelativePaddingRight())/(max.x-min.x);

                    double yOrigin = (h-1)*(1.0-dataScale.getRelativePaddingBottom());
                    double yScale = (h-1)*(1.0-dataScale.getRelativePaddingTop()-dataScale.getRelativePaddingBottom())/(max.y-min.y);

                    double yUnitFont = fontLineHeight/yScale;
                    double yUnit10 = Math.pow(10.0, Math.ceil(Math.log10(yUnitFont)));
                    double yUnit = yUnit10;
                    if (yUnitFont < yUnit/5) {
                        yUnit /= 5;
                    }
                    else if (yUnitFont < yUnit/2) {
                        yUnit /= 2;
                    }
                    selectedX = null;

                    if (dataScale.getColor() != null) {
                        // Scale is colored -> draw it
                        g2d.setColor(dataScale.getColor());
                        double yUnit0 = Math.ceil((min.y+yUnitFont*0.5)/yUnit)*yUnit;
                        double yUnit1 = Math.floor((max.y-yUnitFont*0.5)/yUnit)*yUnit;

                        if (yMouse != null) {
                            double y = (-yMouse + yOrigin)/yScale + min.y;
                            drawYIndicator(g2d, dfm, fontAscent, w, min, yOrigin, yScale, yUnit, y,
                                    dataScale.getColor());
                        }
                        else {
                            // measure longest scale label
                            double maxWidth = 0;
                            for (double y = yUnit0; y <= yUnit1+yUnit*0.5; y += yUnit) {
                                String text = formatNumber(y, yUnit);
                                Rectangle2D bounds = dfm.getStringBounds(text, 0, text.length(), g2d);
                                maxWidth = Math.max(maxWidth, bounds.getWidth());
                            }
                            // draw the scale 
                            for (double y = yUnit0; y <= yUnit1+yUnit*0.5; y += yUnit) {
                                g2d.drawLine((int)maxWidth+2, (int)(yOrigin-(y-min.y)*yScale), w-1, (int)(yOrigin-(y-min.y)*yScale));
                                String text = formatNumber(y, yUnit);
                                Rectangle2D bounds = dfm.getStringBounds(text, 0, text.length(), g2d);
                                g2d.drawString(text, (int)(maxWidth-bounds.getWidth()), (int)(yOrigin-(y-min.y)*yScale)+fontAscent/2);
                            }
                        }
                        if (firstScale) {
                            firstScale = false;
                            double xUnitFont = fontLineHeight/xScale;
                            double xUnit10 = Math.pow(10.0, Math.ceil(Math.log10(xUnitFont)));
                            double xUnit = xUnit10;
                            if (xUnitFont < xUnit/5) {
                                xUnit /= 5;
                            }
                            else if (xUnitFont < xUnit/2) {
                                xUnit /= 2;
                            }
                            double xUnit0;
                            double xUnit1;
                            xUnit0 = Math.ceil((min.x+xUnitFont*0.5)/xUnit)*xUnit;
                            xUnit1 = Math.floor((max.x-xUnitFont*0.5)/xUnit)*xUnit;
                            if (xMouse != null) {
                                setSelectedX((xMouse - xOrigin)/xScale + min.x);
                            }
                            if (selectedX != null) {
                                String text = formatNumber(selectedX, xUnit*0.25);
                                Rectangle2D bounds = dfm.getStringBounds(text, 0, text.length(), g2d);
                                g2d.drawLine((int)(xOrigin+(selectedX-min.x)*xScale), h-1-(int)bounds.getWidth()-2, (int)(xOrigin+(selectedX-min.x)*xScale), 0);
                                AffineTransform transform = g2d.getTransform();
                                int tx = (int)(xOrigin+(selectedX-min.x)*xScale)+fontAscent/2;
                                int ty = (int)(h - 1);
                                g2d.rotate(-Math.PI/2.0, tx, ty);
                                g2d.drawString(text, tx, ty);
                                g2d.setTransform(transform);
                            }
                            else {
                                // measure longest scale label
                                double maxWidth = 0;
                                for (double x = xUnit0; x <= xUnit1+xUnit*0.5; x += xUnit) {
                                    String text = formatNumber(x, xUnit);
                                    Rectangle2D bounds = dfm.getStringBounds(text, 0, text.length(), g2d);
                                    maxWidth = Math.max(maxWidth, bounds.getWidth());
                                }
                                // draw the scale 
                                for (double x = xUnit0; x <= xUnit1+xUnit*0.5; x += xUnit) {
                                    g2d.drawLine((int)(xOrigin+(x-min.x)*xScale), h-1-(int)maxWidth-2, (int)(xOrigin+(x-min.x)*xScale), 0);
                                    String text = formatNumber(x, xUnit);
                                    Rectangle2D bounds = dfm.getStringBounds(text, 0, text.length(), g2d);
                                    AffineTransform transform = g2d.getTransform();
                                    int tx = (int)(xOrigin+(x-min.x)*xScale)+fontAscent/2;
                                    int ty = (int)(h - 1 - maxWidth  + bounds.getWidth());
                                    g2d.rotate(-Math.PI/2.0, tx, ty);
                                    g2d.drawString(text, tx, ty);
                                    g2d.setTransform(transform); 
                                }
                            }
                        }

                    }
                    for (DataRow dataRow : dataScale.getDataRows()) {
                        Set<Double> xAxis = dataRow.getXAxis();
                        if (xAxis != null) {
                            double y0 = Double.NaN;
                            double x0 = Double.NaN;
                            g2d.setColor(dataRow.getColor());
                            for (double x : xAxis) {
                                double y = dataRow.getDataPoint(x);
                                if (!Double.isNaN(x0)) {
                                    g2d.drawLine((int)(xOrigin+(x0-min.x)*xScale), (int)(yOrigin-(y0-min.y)*yScale), 
                                            (int)(xOrigin+(x-min.x)*xScale), (int)(yOrigin-(y-min.y)*yScale));
                                }
                                x0 = x;
                                y0 = y;
                            }
                            if (selectedX != null) {
                                drawYIndicator(g2d, dfm, fontAscent, w, min, yOrigin, yScale, yUnit, 
                                        dataRow.getInterpolated(selectedX), dataRow.getColor());
                            }
                        }
                    }
                }
            }
        }
        else {
            g2d.setColor(new Color(0, 0, 0, 64));
            String text = "no data";
            Rectangle2D bounds = dfm.getStringBounds(text, 0, text.length(), g2d);
            g2d.drawString(text, (int)(Math.min(h, w)/2-bounds.getWidth()/2), (int)(h/2));
        }
    }
    protected void drawYIndicator(Graphics2D g2d, FontMetrics dfm, int fontAscent, int w,
            Point2D.Double min, double yOrigin, double yScale, double yUnit, Double y,
            Color color) {
        if (y == null) {
            return;
        }
        String text = formatNumber(y, yUnit*0.25);
        Rectangle2D bounds = dfm.getStringBounds(text, 0, text.length(), g2d);
        g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 64));
        g2d.drawLine((int)bounds.getWidth()+2, (int)(yOrigin-(y-min.y)*yScale), w-1, (int)(yOrigin-(y-min.y)*yScale));
        g2d.setColor(color);
        g2d.drawString(text, 0, (int)(yOrigin-(y-min.y)*yScale)+fontAscent/2);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension superDim = super.getPreferredSize();
        int width = (int)Math.max(superDim.getWidth(), preferredSize.getWidth());
        int height = (int)Math.max(superDim.getHeight(), preferredSize.getHeight()); 
        return new Dimension(width, height);
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
    }
    @Override
    public void mousePressed(MouseEvent e) {
    }
    @Override
    public void mouseReleased(MouseEvent e) {
    }
    @Override
    public void mouseEntered(MouseEvent e) {
    }
    @Override
    public void mouseExited(MouseEvent e) {
        xMouse = null;
        yMouse = null;
        setSelectedX(null);
        repaint();
    }
}