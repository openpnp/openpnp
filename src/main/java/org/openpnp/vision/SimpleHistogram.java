package org.openpnp.vision;

import java.util.HashMap;
import java.util.Map;

/** 
 * Very simple histogram with zero knowledge about the range. 
 * Good only for low numbers of measurements.
 * The bins are filled with an 2-bin wide square kernel centered on the measurement, recording 
 * the overlap of the kernel with the bins. If the distribution of measurements clusters near the 
 * edge between two bins (dithering between the two), the recorded weight is still comparable to a 
 * similar distribution centered in the middle of a bin. Therefore the minimum/maximum is 
 * still reliably captured in these cases, even in the presence of background noise. 
 */
public class SimpleHistogram {
    private Map<Integer, Double> histogram = new HashMap<>();
    private Integer minimum = null;
    private double minimumVal = Double.NaN;
    private Integer maximum = null;
    private double maximumVal = Double.NaN;
    private double resolution;
    public SimpleHistogram(double resolution) {
        this.resolution = resolution;
    }
    public void add(double key, double val) {
        // the three histogram bins around the key position
        int binY1 = (int)Math.round(key/resolution);   
        int binY0 = binY1-1; 
        int binY2 = binY1+1;
        // calculate the weight according to overlap with the kernel spreading 2 bins 
        double w1 = 1.0;
        double w0 = ((binY0+1.5)*resolution - key)/resolution;
        double w2 = (-(binY2-1.5)*resolution + key)/resolution;
        add(binY1, w1*val);
        add(binY0, w0*val);
        add(binY2, w2*val);
    }
    public void add(int key, double val) {

        double newVal = histogram.get(key) == null ? val : histogram.get(key)+val;
        histogram.put(key, newVal);
        if (maximum == null || maximumVal < newVal) {
            maximumVal = newVal;
            maximum = key;
        }
        if (minimum == null || minimumVal > newVal) {
            minimumVal = newVal;
            minimum = key;
        }
    }
    public Map<Integer, Double> getHistogram() {
        return histogram;
    }
    public Integer getMinimum() {
        return minimum;
    }
    public double getMinimumKey() {
        return minimum == null ? Double.NaN : minimum*resolution;
    }
    public double getMinimumVal() {
        return minimumVal;
    }
    public Integer getMaximum() {
        return maximum;
    }
    public double getMaximumKey() {
        return maximum == null ? Double.NaN : maximum*resolution;
    }
    public double getMaximumVal() {
        return maximumVal;
    }
    public double getResolution() {
        return resolution;
    }
}
