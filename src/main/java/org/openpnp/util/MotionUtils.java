package org.openpnp.util;

import java.awt.geom.Point2D;
import java.util.Set;
import java.util.TreeSet;
import java.util.Collections;
import org.pmw.tinylog.Logger;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.spi.base.AbstractAxis;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.util.SimpleGraph;
import org.openpnp.util.SimpleGraph.DataScale;
import org.openpnp.util.SimpleGraph.DataRow;

public class MotionUtils {

    // Given a x/y movement vector, return a measurement of the cost of performing that movement.
    // This return value is suitable for use in optimising a movement plan.
    //
    // It might be either:
    // 1. A time value, if the x and y axes have performed backlash calibration
    //    which records a profile of single-axis movement times over a range of distances.
    // 2. The linear distance, as a fallback if there is no way to estimate movement time.
    //
    public static double getMotionCost(Location l) {
        Double xt = getSingleAxisMotionCost(AbstractAxis.Type.X,l.getX());
        Double yt = getSingleAxisMotionCost(AbstractAxis.Type.Y,l.getY());
        if (xt==null || yt==null) {
            // Fallback to just returning the distance
            return l.getLinearDistanceTo(Location.origin);
        } else {
            // Assume that each axis movement is independent of the other,
            // and return the maximum of the duration of the axis movement.
            double t=Math.max(xt,yt);
            return t;
        }
    }

    public static Double getSingleAxisMotionCost(AbstractAxis.Type axisType,double d) {
        // Find the axis. Find the graph recorded by axis backlash calibration.
        // Find the timing data rows. Check that row has enough data. Interpolate.
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
                                    d = Math.abs(d);
                                    Double t = dataRow.getInterpolated(d);
                                    if (t!=null) {
                                        return t;
                                    } else {
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
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}

