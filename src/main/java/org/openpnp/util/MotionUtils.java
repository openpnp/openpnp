package org.openpnp.util;

import java.awt.geom.Point2D;
import java.util.Set;
import java.util.TreeSet;
import java.util.Collections;
import org.pmw.tinylog.Logger;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.model.LengthUnit;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.spi.base.AbstractAxis;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.util.SimpleGraph;
import org.openpnp.util.SimpleGraph.DataScale;
import org.openpnp.util.SimpleGraph.DataRow;

public class MotionUtils {

    DataRow xrow, yrow;

    public MotionUtils() {
        xrow = getRow(AbstractAxis.Type.X);
        yrow = getRow(AbstractAxis.Type.Y);
    }

    private DataRow getRow(AbstractAxis.Type axisType) {
        // Find the timing data row in the backlash compensation data for this axis
        ReferenceMachine machine = (ReferenceMachine)Configuration.get().getMachine();
        AbstractAxis axis = machine.getDefaultAxis(axisType);
        if(axis instanceof ReferenceControllerAxis) {
            SimpleGraph graph = ((ReferenceControllerAxis)axis).getBacklashDistanceTestGraph();
            if (graph != null) {
                for(DataScale scale : graph.getScales()) {
                    if(scale.getLabel().equals("T")) {
                        for (DataRow dataRow : scale.getDataRows()) {
                            if (dataRow.getLabel().equals("T0")) {
                                if (dataRow.size()>5) {
                                    return dataRow;
                                }
                            }
                        }
                    }
                }
            }
        }
        // drop through and return null if we can not find suitable data
        return null;
    }

    // Given a x/y movement vector, return a measurement of the cost of performing that movement.
    // This return value is suitable for use in optimising a movement plan.
    //
    // It might be either:
    // 1. A time value, if the x and y axes have performed backlash calibration
    //    which records a profile of single-axis movement times over a range of distances.
    // 2. The linear distance, as a fallback if there is no way to estimate movement time.
    //
    public double getMotionCost(Location l) {
        l = l.convertToUnits(LengthUnit.Millimeters);
        return getMotionCost(l.getX(),l.getY());
    }

    public double getMotionCost(double xd,double yd) {
        if(xrow==null || yrow==null) {
            // Fallback to just returning the distance
            return Math.sqrt(xd*xd+yd*yd);
        } else {
            // Assume that each axis movement is independent of the other,
            // and return the maximum of the duration of the axis movement.
            return Math.max(getSingleAxisMotionCost(xrow,xd),getSingleAxisMotionCost(yrow,yd));
        }
    }

    private static double getSingleAxisMotionCost(DataRow dataRow,double d) {
        d = Math.abs(d);
        Double t = dataRow.getInterpolated(d);
        if (t!=null) {
            return t;
        }

        // This distance is outside the range which was tested during
        // backlash calibration. We therefore need to extrapolate
        if(d< (dataRow.getMinimum().x+dataRow.getMaximum().x)/2) {
            // This movement is smaller than the smallest calibrated movement.
            // Interpolate between that smallest, and zero time for zero distance.
            Set<Double> xaxis = dataRow.getXAxis();
            // NB "xaxis" here refers to the independent variable of the distance/time curve
            // recorded during backlash compensation. "xaxis" records distances along either
            // controller axis.
            double sd = Collections.min(xaxis);
            double st = dataRow.getDataPoint(sd);
            return d * st/sd;
        } else {
            // This movement is further than the largest. Linear extrapolation through the two largest points
            TreeSet<Double> xaxis = new TreeSet<Double>(dataRow.getXAxis()); // make a copy
            double sd = Collections.max(xaxis);
            double st = dataRow.getDataPoint(sd);
            xaxis.remove(sd); // find the second-largest; this is why we made a copy above
            double rd = Collections.max(xaxis);
            if( Math.abs(rd-sd) < 4 ) {
                // The xaxis datapoints consist of a geometric progression, plus a single point
                // which corresponds to the maximum movement from the limit of motion range back
                // to the camera. It is therefore possible that the largest and second-largest
                // are quite close together, and unsuitable for extrapolation for larger moves.
                // We should extrapolate using the largest (sd found above) and the third-largest.
                xaxis.remove(rd);
                rd = Collections.max(xaxis);
            }
            double rt = dataRow.getDataPoint(rd);
            return st + (d-sd) * (rt-st) / (rd-sd);
        }
    }
}

