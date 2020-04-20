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
import java.util.HashMap;
import java.util.List;

public class SimpleGraph {

    private boolean offsetMode = false;
    private double relativePaddingLeft;
    private double relativePaddingRight;
    private List<DataScale> dataScales = new ArrayList<>();  

    public static class DataScale {
        private String label;
        private Color color = null;
        private List<DataRow> dataRows = new ArrayList<>();
        private double relativePaddingTop;
        private double relativePaddingBottom;


        public void addDataRow(DataRow dataRow) {
            dataRows.add(dataRow);
        }
        public List<DataRow> getDataRows() {
            return dataRows;
        }

        public DataScale(String label) {
            super();
            this.label = label;
        }

        public Point2D.Double getMinimum() {
            Point2D.Double minimum = null;
            for (DataRow dataRow : dataRows) {
                Point2D.Double rowMin = dataRow.getMinimum();
                if (minimum == null) {
                    minimum = rowMin;
                }
                else {
                    minimum.x = Math.min(rowMin.x, minimum.x);
                    minimum.y = Math.min(rowMin.y, minimum.y);
                }
            }
            return minimum; 
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

        public Point2D.Double getMaximum() {
            Point2D.Double maximum = null;
            for (DataRow dataRow : dataRows) {
                Point2D.Double rowMax = dataRow.getMaximum();
                if (maximum == null) {
                    maximum = rowMax;
                }
                else {
                    maximum.x = Math.max(rowMax.x, maximum.x);
                    maximum.y = Math.max(rowMax.y, maximum.y);
                }
            }
            return maximum; 
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

    public boolean isOffsetMode() {
        return offsetMode;
    }
    public void setOffsetMode(boolean offsetMode) {
        this.offsetMode = offsetMode;
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

    public static class DataRow {
        private String label;
        private Color color;
        private HashMap<Double, Double> data = new HashMap<>();

        // housekeeping
        boolean dirty = false;
        private List<Double> sortedKeys;
        private Point2D.Double minimum = null;
        private Point2D.Double maximum = null;

        public DataRow(String label, Color color) {
            super();
            this.label = label;
            this.color = color;
        }

        public void recordDataPoint(double x, double y) {
            data.put(x, y);
            dirty = true;
        }
        public void recordDataPoint(long x, double y) {
            data.put((double)x, y);
            dirty = true;
        }
        public Double getDataPoint(double x) {
            return data.get(x);
        }
        public int size() {
            return data.size();
        }

        protected void recalc() {
            if (dirty) {
                sortedKeys = new ArrayList<Double>(data.size());
                sortedKeys.addAll(data.keySet());
                Collections.sort(sortedKeys);
                minimum = null;
                maximum = null;
                for (double x : sortedKeys) {
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
                dirty = false;
            }
        }

        public List<Double> getXAxis() {
            recalc();
            return sortedKeys;
        }
        public Point2D.Double getMinimum() {
            recalc();
            if (minimum == null) {
                return new Point2D.Double();
            }
            else {
                return (Point2D.Double)minimum.clone();
            }
        }
        public Point2D.Double getMaximum() {
            recalc();
            if (maximum == null) {
                return new Point2D.Double();
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
        public HashMap<Double, Double> getData() {
            return data;
        }
        public void setData(HashMap<Double, Double> data) {
            this.data = data;
        }

    }


    public Point2D.Double getMinimum(DataScale dataScaleForY) {
        Point2D.Double minimum = dataScaleForY.getMinimum();
        // expand x axis over all the scales 
        for (DataScale dataScale : dataScales) {
            if (dataScale != dataScaleForY) {
                Point2D.Double scaleMin = dataScale.getMinimum();
                minimum.x = Math.min(scaleMin.x, minimum.x);
            }
        }
        return minimum; 
    }
    public Point2D.Double getMaximum(DataScale dataScaleForY) {
        Point2D.Double maximum = dataScaleForY.getMaximum();
        // expand x axis over all the scales 
        for (DataScale dataScale : dataScales) {
            if (dataScale != dataScaleForY) {
                Point2D.Double scaleMin = dataScale.getMaximum();
                maximum.x = Math.max(scaleMin.x, maximum.x);
            }
        }
        return maximum; 
    }
}
