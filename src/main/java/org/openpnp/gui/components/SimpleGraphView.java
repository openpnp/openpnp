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
import java.util.ArrayList;
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
    private int displayCycle = -1;//all displayed
    private int displayCycleMask;

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
    public synchronized void setGraph(SimpleGraph graph) {
        this.graph = graph;
        displayCycleMask = 0;
        if (graph != null) {
            for (DataScale dataScale : graph.getDataScales()) {
                for (DataRow dataRow : dataScale.getDataRows()) {
                    if (dataRow.size() >= 2) {
                        displayCycleMask |= dataRow.getDisplayCycleMask();
                    }
                }
            }
        }
        displayCycle = displayCycleMask;
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

    protected String formatNumber(double v, double unit) {
        // format numbers without necessary digits (I'm sure there's a better/simpler way)
        int digits = Math.max(-19, (int)Math.floor(Math.log10(unit)+0.1));
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
        return format.format(v);
    }

    @Override
    public synchronized void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Font font = getFont();
        FontMetrics dfm = g2d.getFontMetrics(font);
        int fontLineHeight = dfm.getAscent()+1; // numbers are all ascent
        int fontAscent = dfm.getAscent();
        Color gridColor = SimpleGraph.getDefaultGridColor();
        g2d.setFont(font);
        int w = getWidth();
        int h = getHeight();
        if (graph != null) {
            boolean firstScale = true;
            for (DataScale dataScale : graph.getDataScales()) {
                Point2D.Double min = graph.getMinimum(dataScale);
                Point2D.Double max = graph.getMaximum(dataScale);
                if (min != null && max != null) {
                    // Convert to display x, e.g. logarithmic
                    min.x = graph.displayX(min.x); 
                    min.y = dataScale.displayY(min.y); 
                    max.x = graph.displayX(max.x); 
                    max.y = dataScale.displayY(max.y); 
                    if (dataScale.isSymmetricIfSigned()) {
                        if (min.y < 0.0 && max.y > 0) {
                            max.y = Math.max(max.y, -min.y);
                            min.y = Math.min(-max.y, min.y);
                        }
                    }
                    if (dataScale.isSquareAspectRatio()) {
                        if (dataScale.isSymmetricIfSigned()) {
                            if (min.x < 0.0 && max.x > 0) {
                                max.x = Math.max(max.x, -min.x);
                                min.x = Math.min(-max.x, min.x);
                            }
                        }
                        double maxDiffOver2 = Math.max(max.x - min.x, max.y - min.y)/2;
                        double midX = (max.x + min.x)/2;
                        double midY = (max.y + min.y)/2;
                        max.x = midX + maxDiffOver2;
                        min.x = midX - maxDiffOver2;
                        max.y = midY + maxDiffOver2;
                        min.y = midY - maxDiffOver2;
                    }
                }
                if (min != null && max != null && (max.y-min.y) > 0.0 && (max.x-min.x) > 0.0) {
                    double xOrigin = (w-1)*graph.getRelativePaddingLeft();
                    double xScale = (w-1)*(1.0-graph.getRelativePaddingLeft()-graph.getRelativePaddingRight())/(max.x-min.x);

                    double yOrigin = (h-1)*(1.0-dataScale.getRelativePaddingBottom());
                    double yScale = (h-1)*(1.0-dataScale.getRelativePaddingTop()-dataScale.getRelativePaddingBottom())/(max.y-min.y);

                    double yUnitFont = fontLineHeight/yScale;
                    double yUnit;
                    double yUnitDisplay;
                    if (dataScale.isLogarithmic()) {
                        // Increasing the log by a fraction of the font units gives our base unit.
                        double rDiff = Math.exp(0.2*yUnitFont) - 1;
                        double yUnit10 = Math.pow(10.0, Math.ceil(Math.log10(rDiff)));
                        yUnitDisplay = yUnit10;
                        if (rDiff < yUnitDisplay/5) {
                            yUnitDisplay /= 5;
                        }
                        else if (rDiff < yUnitDisplay/2) {
                            yUnitDisplay /= 2;
                        }
                        // Scan pixel by pixel.
                        yUnit = 1/yScale;
                    }
                    else {
                        double yUnit10 = Math.pow(10.0, Math.ceil(Math.log10(yUnitFont)));
                        yUnitDisplay = yUnit10;
                        if (yUnitFont < yUnitDisplay/5) {
                            yUnitDisplay /= 5;
                        }
                        else if (yUnitFont < yUnitDisplay/2) {
                            yUnitDisplay /= 2;
                        }
                        yUnit = yUnitDisplay; 
                    }

                    if (dataScale.getColor() != null) {
                        // Scale is colored -> draw it
                        g2d.setColor(dataScale.getColor());
                        double yGap = 0.5;
                        if (dataScale.isLabelShown()) {
                            String text = dataScale.getLabel();
                            //Rectangle2D bounds = dfm.getStringBounds(text, 0, text.length(), g2d);
                            g2d.drawString(text, 0, (int)(yOrigin-(max.y-min.y)*yScale+fontAscent));
                            yGap += 1.0;
                        }
                        double yUnit0 = min.y+yUnitFont*0.5; 
                        double yUnit1 = max.y-yUnitFont*yGap;
                        if (!dataScale.isLogarithmic()) {
                            yUnit0 = Math.ceil(yUnit0/yUnit)*yUnit;
                            yUnit1 = Math.floor(yUnit1/yUnit)*yUnit;
                        }
                        if (yMouse != null) {
                            double y = (-yMouse + yOrigin)/yScale + min.y;
                            drawYIndicator(g2d, dfm, fontAscent, w, min, max, yOrigin, yScale, yUnitDisplay, y,
                                    dataScale.getColor(), dataScale);
                        }
                        else {
                            // Two passes: 1. measure longest scale label, 2. draw the scale 
                            double maxWidth = 0;
                            for (int pass = 0; pass < 2; pass++) {
                                String text0 = "";
                                double yp = Double.NEGATIVE_INFINITY;
                                for (double y = yUnit0; y <= yUnit1+yUnit*0.01; y += yUnit) {
                                    double yGraph = dataScale.graphY(y);
                                    String text;
                                    if (dataScale.isLogarithmic()) {
                                        double yU = Math.pow(10.0, Math.ceil(Math.log10(yGraph)))*yUnitDisplay;
                                        double yLog = Math.ceil(yGraph/yU)*yU;
                                        text = formatNumber(yLog, yU);
                                        y = dataScale.displayY(yLog);
                                    }
                                    else {
                                        text = formatNumber(yGraph, yUnitDisplay);
                                    }
                                    if(!text.equals(text0)) {
                                        text0 = text;
                                        Rectangle2D bounds = dfm.getStringBounds(text, 0, text.length(), g2d);
                                        if (pass == 0) {
                                            maxWidth = Math.max(maxWidth, bounds.getWidth());
                                        }
                                        else {
                                            g2d.drawLine((int)maxWidth+2, (int)(yOrigin-(y-min.y)*yScale), w-1, (int)(yOrigin-(y-min.y)*yScale));
                                            if (yp + yUnitFont <= y) {
                                                g2d.drawString(text, (int)(maxWidth-bounds.getWidth()), (int)(yOrigin-(y-min.y)*yScale-bounds.getCenterY()));
                                                yp = y;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (firstScale) {
                            firstScale = false;
                            double xUnitFont = fontLineHeight/xScale;
                            double xUnit;
                            double xUnitDisplay;
                            if (graph.isLogarithmic()) {
                                // Increasing the log by a fraction of the font units gives our base unit.
                                double rDiff = Math.exp(0.2*xUnitFont) - 1;
                                double xUnit10 = Math.pow(10.0, Math.ceil(Math.log10(rDiff)));
                                xUnitDisplay = xUnit10;
                                if (rDiff < xUnitDisplay/5) {
                                    xUnitDisplay /= 5;
                                }
                                else if (rDiff < xUnitDisplay/2) {
                                    xUnitDisplay /= 2;
                                }
                                // Scan pixel by pixel.
                                xUnit = 1/xScale; 
                            }
                            else {
                                double xUnit10 = Math.pow(10.0, Math.ceil(Math.log10(xUnitFont)));
                                xUnitDisplay = xUnit10;
                                if (xUnitFont < xUnitDisplay/5) {
                                    xUnitDisplay /= 5;
                                }
                                else if (xUnitFont < xUnitDisplay/2) {
                                    xUnitDisplay /= 2;
                                }
                                xUnit = xUnitDisplay;
                            }
                            double xUnit0 = min.x+xUnitFont*0.5;
                            double xUnit1 = max.x-xUnitFont*0.5;
                            if (!graph.isLogarithmic()) {
                                xUnit0 = Math.ceil(xUnit0/xUnit)*xUnit;
                                xUnit1 = Math.floor(xUnit1/xUnit)*xUnit;
                            }
                            if (xMouse != null) {
                                setSelectedX(graph.graphX((xMouse - xOrigin)/xScale + min.x));
                            }
                            if (selectedX != null) {
                                // X indicator.
                                String text = formatNumber(selectedX, 
                                        graph.isLogarithmic() ? Math.pow(10.0, Math.ceil(Math.log10(selectedX))-3) : xUnitDisplay*0.25);
                                Rectangle2D bounds = dfm.getStringBounds(text, 0, text.length(), g2d);
                                double dispX = graph.displayX(selectedX);
                                g2d.drawLine((int)(xOrigin+(dispX-min.x)*xScale), h-1-(int)bounds.getWidth()-2, (int)(xOrigin+(dispX-min.x)*xScale), 0);
                                AffineTransform transform = g2d.getTransform();
                                int tx = (int)(xOrigin+(dispX-min.x)*xScale)+fontAscent/2;
                                int ty = (int)(h - 1);
                                g2d.rotate(-Math.PI/2.0, tx, ty);
                                g2d.drawString(text, tx, ty);
                                g2d.setTransform(transform);
                            }
                            else {
                                // Prepare a list of gaps between scales (if there are multiple).
                                ArrayList<Double> gaps = new ArrayList<>(); 
                                final double scaleGap = yUnitFont;
                                // As scales are allowed to overlap, we can't just sort them. Instead compare each with each and make
                                // sure there is no overlap. Then take the closest one and compute the gap position.
                                for (DataScale dataScale1 : graph.getDataScales()) {
                                    boolean after = false;
                                    double nearestBottom = Double.NEGATIVE_INFINITY;
                                    for (DataScale dataScale2 : graph.getDataScales()) {
                                        if (dataScale2 == dataScale1) {
                                            after = true;
                                        }
                                        else if (dataScale1.getRelativePaddingTop() > dataScale2.getRelativePaddingTop()
                                              || (dataScale1.getRelativePaddingTop() == dataScale2.getRelativePaddingTop() && after)) {
                                            if (nearestBottom < dataScale2.getRelativePaddingBottom()) {
                                                nearestBottom = dataScale2.getRelativePaddingBottom();
                                            }
                                        }
                                    }
                                    if (Double.isFinite(nearestBottom) 
                                            && nearestBottom > (1.0 - dataScale1.getRelativePaddingTop() + scaleGap)) {
                                        gaps.add((nearestBottom + 1.0 - dataScale1.getRelativePaddingTop() - scaleGap)*0.5);
                                    }
                                }
                                gaps.add(1.0);
                                gaps.sort(null);
                                // Two passes: 1. measure longest scale label, 2. draw the scale 
                                double maxWidth = 0;
                                for (int pass = 0; pass < 2; pass++) {
                                    String text0 = "";
                                    double xp = Double.NEGATIVE_INFINITY;
                                    for (double x = xUnit0; x <= xUnit1+xUnit*0.01; x += xUnit) {
                                        double xGraph = graph.graphX(x);
                                        String text;
                                        if (graph.isLogarithmic()) {
                                            double xU = Math.pow(10.0, Math.ceil(Math.log10(xGraph)))*xUnitDisplay;
                                            double xLog = Math.ceil(xGraph/xU)*xU;
                                            text = formatNumber(xLog, xU);
                                            x = graph.displayX(xLog);
                                        }
                                        else {
                                            text = formatNumber(xGraph, xUnitDisplay);
                                        }
                                        if (!text.equals(text0)) {
                                            text0 = text;
                                            Rectangle2D bounds = dfm.getStringBounds(text, 0, text.length(), g2d);
                                            if (pass == 0) {
                                                maxWidth = Math.max(maxWidth, bounds.getWidth());
                                            }
                                            else {
                                                int xl = (int)(xOrigin+(x-min.x)*xScale);
                                                int yl0 = h-1-(int)maxWidth-2;
                                                for (Double gap : gaps) {
                                                    int yl1 = (int) ((h-1)*(1.0-gap));
                                                    g2d.drawLine(
                                                            xl, yl0, 
                                                            xl, yl1);
                                                    yl0 = (int) (yl1 - scaleGap*yScale);
                                                }
                                                if (xp + xUnitFont <= x) {
                                                    AffineTransform transform = g2d.getTransform();
                                                    int tx = (int)(xOrigin+(x-min.x)*xScale - bounds.getCenterY());
                                                    int ty = (int)(h - 1 - maxWidth + bounds.getWidth());
                                                    g2d.rotate(-Math.PI/2.0, tx, ty);
                                                    g2d.drawString(text, tx, ty);
                                                    g2d.setTransform(transform);
                                                    xp = x;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Draw the actual curves.
                    for (DataRow dataRow : dataScale.getDataRows()) {
                        if ((dataRow.getDisplayCycleMask() & displayCycle) != 0) {
                            Set<Double> xAxis = dataRow.getXAxis();
                            if (xAxis != null) {
                                // Convert to pixel coordinates
                                int size = dataRow.size();
                                boolean showLine = dataRow.isLineShown();
                                boolean showMarker = dataRow.isMarkerShown();
                                boolean showMarkerOnly = showMarker && !showLine;
                                if (size >= 2) {
                                    double [] xfPlot = new double [size]; 
                                    double [] yfPlot = new double [size];
                                    int i = 0;
                                    for (double xGraph : xAxis) {
                                        double y = dataScale.displayY(dataRow.getDataPoint(xGraph));
                                        double x = graph.displayX(xGraph);
                                        xfPlot[i] = xOrigin+(x-min.x)*xScale; 
                                        yfPlot[i] = yOrigin-(y-min.y)*yScale;
                                        i++;
                                    }
                                    // Analyze the curve and only plot relevant curve points.
                                    int [] xPlot = new int [size]; 
                                    int [] yPlot = new int [size];
                                    int s = 0;
                                    // Always add first point.
                                    xPlot[s] = (int) xfPlot[0];
                                    yPlot[s] = (int) yfPlot[0];
                                    s++;
                                    for (i = 1; i < size-1; i++) {
                                        double dx0 = xfPlot[i]-xfPlot[i-1];
                                        double dy0 = yfPlot[i]-yfPlot[i-1];
                                        double dx1 = xfPlot[i+1]-xfPlot[i];
                                        double dy1 = yfPlot[i+1]-yfPlot[i];
                                        double n0 = Math.sqrt(dx0*dx0+dy0*dy0); 
                                        double n1 = Math.sqrt(dx1*dx1+dy1*dy1); 
                                        double cosine = ((dx0*dx1) + (dy0*dy1))/n0/n1;
                                        if (showMarkerOnly || cosine < 0.99 
                                                || Math.abs(xfPlot[i]-xPlot[s-1]) > 12 || Math.abs(yfPlot[i]-yPlot[s-1]) > 1.5) {
                                            // Corner point or relevant change.
                                            xPlot[s] = (int) xfPlot[i];
                                            yPlot[s] = (int) yfPlot[i];
                                            s++;
                                        }
                                    }
                                    // Always add last point.
                                    xPlot[s] = (int) xfPlot[size-1];
                                    yPlot[s] = (int) yfPlot[size-1];
                                    s++;
                                    g2d.setColor(dataRow.getColor());
                                    if (showLine) {
                                        // Draw as polyline.
                                        g2d.drawPolyline(xPlot, yPlot, s);
                                    }
                                    if (showMarker) {
                                        int dia = 4;
                                        for (int p=0; p<s; p++) {
                                            g2d.fillOval(xPlot[p]-dia/2, yPlot[p]-dia/2, dia, dia);
                                        }
                                    }
                                    if (selectedX != null) {
                                        drawYIndicator(g2d, dfm, fontAscent, w, min, max, yOrigin, yScale, yUnitDisplay, 
                                                dataScale.displayY(dataRow.getInterpolated(selectedX)), dataRow.getColor(), dataScale);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        else {
            g2d.setColor(gridColor);
            String text = "no data";
            Rectangle2D bounds = dfm.getStringBounds(text, 0, text.length(), g2d);
            g2d.drawString(text, (int)(bounds.getHeight()*2), (int)(h/2));
        }
    }
    protected void drawYIndicator(Graphics2D g2d, FontMetrics dfm, int fontAscent, int w,
            Point2D.Double min, Point2D.Double max, double yOrigin, double yScale, double yUnitDisplay, Double y,
            Color color, DataScale dataScale) {
        if (y == null) {
            return;
        }
        if (y < min.y) {
            return;
        }
        if (y > max.y) {
            return;
        }
        double graphY = dataScale.graphY(y);
        String text = formatNumber(graphY, 
                dataScale.isLogarithmic() ? Math.pow(10.0, Math.ceil(Math.log10(graphY))-3) : yUnitDisplay*0.25);
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
        if (displayCycleMask != 0) {
            displayCycle--;
            while ((displayCycle & displayCycleMask) == 0) {
                displayCycle--;
            }
        }
        repaint();
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