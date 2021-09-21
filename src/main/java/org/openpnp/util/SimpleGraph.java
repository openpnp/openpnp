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

package org.openpnp.util;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.UIManager;

import org.openpnp.vision.pipeline.stages.convert.ColorConverter;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.convert.Convert;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;

public class SimpleGraph {

    @Attribute(required=false)
    private double relativePaddingLeft;
    @Attribute(required=false)
    private double relativePaddingRight;
    @Attribute(required=false)
    private boolean logarithmic;
    @Attribute(required=false)
    private long zeroNanoTime = Long.MIN_VALUE;
    @Attribute(required=false)
    private long lastT = 0;
    @ElementList(inline = true)
    private List<DataScale> dataScales = new ArrayList<>();

    public static class DataScale {
        @Attribute
        private String label;
        @Attribute(required=false)
        public boolean labelShown;
        @Element(required=false)
        @Convert(ColorConverter.class)
        private Color color = null;
        @Attribute(required=false)
        private double relativePaddingTop;
        @Attribute(required=false)
        private double relativePaddingBottom;
        @Attribute(required=false)
        private boolean logarithmic;
        @Attribute(required=false)
        private boolean symmetricIfSigned;
        @Attribute(required=false)
        private boolean squareAspectRatio;
        @ElementList(inline = true)
        private List<DataRow> dataRows = new ArrayList<>();

        private DataScale() {
            super();
        }

        public DataScale(String label) {
            super();
            this.label = label;
        }

        public void addDataRow(DataRow dataRow) {
            dataRows.add(dataRow);
        }
        public List<DataRow> getDataRows() {
            return dataRows;
        }

        public String getLabel() {
            return label;
        }

        public boolean isLabelShown() {
            return labelShown;
        }
        public void setLabelShown(boolean labelShown) {
            this.labelShown = labelShown;
        }
        public Color getColor() {
            return color;
        }
        public void setColor(Color color) {
            this.color = color;
        }
        public double getRelativePaddingTop() {
            return relativePaddingTop;
        }
        public void setRelativePaddingTop(double relativePaddingTop) {
            this.relativePaddingTop = relativePaddingTop;
        }
        public double getRelativePaddingBottom() {
            return relativePaddingBottom;
        }
        public void setRelativePaddingBottom(double relativePaddingBottom) {
            this.relativePaddingBottom = relativePaddingBottom;
        }
        public boolean isSymmetricIfSigned() {
            return symmetricIfSigned;
        }
        public boolean isLogarithmic() {
            return logarithmic;
        }
        public void setLogarithmic(boolean logarithmic) {
            this.logarithmic = logarithmic;
        }
        public void setSymmetricIfSigned(boolean symmetricIfSigned) {
            this.symmetricIfSigned = symmetricIfSigned;
        }

        public boolean isSquareAspectRatio() {
            return squareAspectRatio;
        }
        
        public void setSquareAspectRatio(boolean squareAspectRatio) {
            this.squareAspectRatio = squareAspectRatio;
        }
        
        public Point2D.Double getMinimum() {
            Point2D.Double minimum = null;
            for (DataRow dataRow : dataRows) {
                Point2D.Double rowMin = dataRow.getMinimum();
                if (rowMin == null) {
                    // skip
                }
                else if (minimum == null) {
                    minimum = rowMin;
                }
                else {
                    minimum.x = Math.min(rowMin.x, minimum.x);
                    minimum.y = Math.min(rowMin.y, minimum.y);
                }
            }
            return minimum; 
        }
        public Point2D.Double getMaximum() {
            Point2D.Double maximum = null;
            for (DataRow dataRow : dataRows) {
                Point2D.Double rowMax = dataRow.getMaximum();
                if (rowMax == null) {
                    // skip
                }
                else if (maximum == null) {
                    maximum = rowMax;
                }
                else {
                    maximum.x = Math.max(rowMax.x, maximum.x);
                    maximum.y = Math.max(rowMax.y, maximum.y);
                }
            }
            return maximum; 
        }

        public double graphY(double y) {
            return isLogarithmic() ? Math.exp(y) : y;
        }
        public Double displayY(Double y) {
            return y == null ? null : (isLogarithmic() ? Math.log(y > 0 ? y : 1e-4) : y);
        }
    } 

    public void addDataScale(DataScale dataScale) {
        dataScales.add(dataScale);
    }
    public DataScale getScale(String label) {
        for (DataScale dataScale : dataScales) {
            if (dataScale.getLabel().equals(label)) {
                return dataScale;
            }
        }
        DataScale dataScale = new DataScale(label);
        addDataScale(dataScale);
        return dataScale;
    }
    public List<DataScale> getDataScales() {
        return dataScales;
    }
    public DataRow getRow(String scaleLabel, String label) {
        DataScale dataScale = getScale(scaleLabel);
        for (DataRow dataRow : dataScale.dataRows) {
            if (dataRow.getLabel().equals(label)) {
                return dataRow;
            }
        }
        DataRow dataRow = new DataRow(label, new Color(0, 0, 0));
        dataScale.addDataRow(dataRow);
        return dataRow;
    }

    /**
     * @return Time in Milliseconds since the first call on this instance. System.nanoTime() based. 
     * Guaranteed to be monotone and unique.
     *
     */
    public double getT() {
        if (zeroNanoTime == Long.MIN_VALUE) {
            zeroNanoTime = System.nanoTime();
            lastT = 0;
            return 0.0;
        }
        long dt = (System.nanoTime() - zeroNanoTime);
        if (dt <= lastT) {
            dt = lastT++;
        }
        else {
            lastT  = dt;
        }
        // From Nano to Milliseconds
        return 1e-6*dt;
    }

    public double getRelativePaddingLeft() {
        return relativePaddingLeft;
    }
    public void setRelativePaddingLeft(double relativePaddingLeft) {
        this.relativePaddingLeft = relativePaddingLeft;
    }
    public double getRelativePaddingRight() {
        return relativePaddingRight;
    }
    public void setRelativePaddingRight(double relativePaddingRight) {
        this.relativePaddingRight = relativePaddingRight;
    }
    public boolean isLogarithmic() {
        return logarithmic;
    }
    public void setLogarithmic(boolean logarithmic) {
        this.logarithmic = logarithmic;
    }

    public static class DataRow {
        @Attribute
        private String label;
        @Element(required=false)
        @Convert(ColorConverter.class)
        private Color color;
        @Attribute(required=false)
        private boolean markerShown = false;
        @Attribute(required=false)
        private boolean lineShown = true;
        @Attribute(required=false)
        private int displayCycleMask = 1; // Displayed on mask 1
        //@ElementMap too large in xml, instead we stream it into a simple x, y array.
        private TreeMap<Double, Double> data = new TreeMap<>();
        @Element(required=false)
        private double [] xyValues;

        @Persist
        void persist() {
            xyValues = new double[data.size()*2];
            int i= 0;
            for (Entry<Double, Double> entry : data.entrySet()) {
                xyValues[i++] = entry.getKey();
                xyValues[i++] = entry.getValue();
            }
        }
        @Commit
        void commit() {
            if (xyValues != null) {
                for (int i = 0; i < xyValues.length;) {
                    double x = xyValues[i++];
                    double y = xyValues[i++];
                    data.put(x, y);
                }
                xyValues = null;
            }
        }

        // housekeeping
        boolean dirty = true;
        private Point2D.Double minimum = null;
        private Point2D.Double maximum = null;

        private DataRow() {
            super();
        }

        public DataRow(String label, Color color) {
            super();
            this.label = label;
            this.color = color;
        }

        public void recordDataPoint(double x, double y) {
            if (Double.isFinite(x) && Double.isFinite(y)) {
                data.put(x, y);
                dirty = true;
            }
        }
        public Double getDataPoint(double x) {
            return data.get(x);
        }
        public Double getInterpolated(double x) {
            Entry<Double, Double> entry0 = data.floorEntry(x);
            Entry<Double, Double> entry1 = data.ceilingEntry(x);
            if (entry0 != null && entry1 != null) {
                double x0 = entry0.getKey();
                double x1 = entry1.getKey();
                double y0 = data.get(x0);
                double y1 = data.get(x1);
                double r;
                if (isLineShown()) {
                    // interpolate
                    r = (x-x0)/(x1-x0);
                }
                else {
                    // step to nearest
                    r = x - x0 < x1 - x ? 0 : 1;
                }
                return y0+r*(y1-y0);
            }
            return null;
        }
        public int size() {
            return data.size();
        }

        protected void recalc() {
            if (dirty) {
                minimum = null;
                maximum = null;
                for (Entry<Double, Double> entry : data.entrySet()) {
                    double x = entry.getKey();
                    double y = data.get(x);
                    if (minimum == null) {
                        minimum = new Point2D.Double(x, y);
                    }
                    else {
                        minimum.x = Math.min(x, minimum.x);
                        minimum.y = Math.min(y, minimum.y);
                    }
                    if (maximum == null) {
                        maximum = new Point2D.Double(x, y);
                    }
                    else {
                        maximum.x = Math.max(x, maximum.x);
                        maximum.y = Math.max(y, maximum.y);
                    }
                }
                if (minimum != null) {
                    if (minimum.x != maximum.x && minimum.y == maximum.y) {
                        // Flatline detected, just add a pseudo min/max.
                        minimum.y -= 0.0001;
                        maximum.y += 0.0001;
                    }
                }
                dirty = false;
            }
        }

        public Set<Double> getXAxis() {
            return data.keySet();
        }
        public Point2D.Double getMinimum() {
            recalc();
            if (minimum == null) {
                return null;
            }
            else {
                return (Point2D.Double)minimum.clone();
            }
        }
        public Point2D.Double getMaximum() {
            recalc();
            if (maximum == null) {
                return null;
            }
            else {
                return (Point2D.Double)maximum.clone();
            }
        }
        public String getLabel() {
            return label;
        }
        public Color getColor() {
            return color;
        }
        public void setColor(Color color) {
            this.color = color;
        }

        public boolean isMarkerShown() {
            return markerShown;
        }

        public void setMarkerShown(boolean markerShown) {
            this.markerShown = markerShown;
        }

        public boolean isLineShown() {
            return lineShown;
        }

        public void setLineShown(boolean lineShown) {
            this.lineShown = lineShown;
        }

        public int getDisplayCycleMask() {
            return displayCycleMask;
        }

        public void setDisplayCycleMask(int displayCycleMask) {
            this.displayCycleMask = displayCycleMask;
        }
    }


    public Point2D.Double getMinimum(DataScale dataScaleForY) {
        Point2D.Double minimum = dataScaleForY.getMinimum();
        if (minimum == null) {
            return null;
        }
        // expand x axis over all the scales 
        for (DataScale dataScale : dataScales) {
            if (dataScale != dataScaleForY) {
                Point2D.Double scaleMin = dataScale.getMinimum();
                if (scaleMin != null) {
                    minimum.x = Math.min(scaleMin.x, minimum.x);
                }
            }
        }
        return minimum; 
    }
    public Point2D.Double getMaximum(DataScale dataScaleForY) {
        Point2D.Double maximum = dataScaleForY.getMaximum();
        if (maximum == null) {
            return null;
        }
        // expand x axis over all the scales 
        for (DataScale dataScale : dataScales) {
            if (dataScale != dataScaleForY) {
                Point2D.Double scaleMax = dataScale.getMaximum();
                if (scaleMax != null) {
                    maximum.x = Math.max(scaleMax.x, maximum.x);
                }
            }
        }
        return maximum; 
    }

    public double graphX(double x) {
        return isLogarithmic() ? Math.exp(x) : x;
    }
    public Double displayX(Double x) {
        return x == null ? null : (isLogarithmic() ? Math.log(x > 0 ? x : 1e-4) : x);
    }

    public List<DataScale> getScales() {
        return Collections.unmodifiableList(dataScales);
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
