/*
 * Copyright (C) 2020 <mark@makr.zone>
 * inspired and based on work
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

package org.openpnp.model;

import java.util.function.BiFunction;

/**
 * The Motion represents one command or waypoint in a motion sequence. It contains the point in time
 * and the location vector along with a number of derivatives.
 * 
 * What the derivatives mean is subject to the MotionPlanner.   
 *
 */
public class Motion {
    public enum MotionOption {
        FixedWaypoint,
        CoordinatedWaypoint,
        Completion, 
        Stillstand;

        protected int flag(){
            return 1 << this.ordinal();
        }
    }

    private AxesLocation [] vector;
    private int options;
    private double time;

    public Motion(double time, AxesLocation[] vector, MotionOption... options) {
        super();
        this.time = time;
        this.vector = vector.clone();
        for (MotionOption option : options) {
            this.options |= option.flag();
        }
    }
    protected Motion(double time, AxesLocation[] vector, int options) {
        super();
        this.time = time;
        this.vector = vector.clone();
        this.options = options;
    }
    public Motion(double time, Motion motion) {
        this(time, motion.vector, motion.options);
    }

    public boolean hasOption(MotionOption option) {
        return (this.options & option.flag()) != 0;
    }
    public void setOption(MotionOption option) {
        this.options |= option.flag();
    }

    public double getTime() {
        return time;
    }

    int getOrder() {
        return vector.length-1;
    }
    public enum Derivative {
        Location,
        Velocity,
        Acceleration,
        Jerk
    }

    public AxesLocation getVector(int order) {
        if (order >= 0 && order < vector.length) { 
            return vector[order];
        }
        return AxesLocation.zero;
    }
    public AxesLocation getVector(Derivative order) {
        return getVector(order.ordinal());
    }

    public String toString() {
        return "{ t="+time+"s, l="+
                getVector(Derivative.Location)+", V="+
                getVector(Derivative.Velocity)+", a="+
                getVector(Derivative.Acceleration)+", j="+
                getVector(Derivative.Jerk)+" }";
    }
    
    protected Motion applyFunction(
            double time, 
            BiFunction<Double, Double, Double> locationFunction, 
            BiFunction<Double, Double, Double> derivativeFunction,
            Motion other,
            int options) {
        int maxOrder = Math.max(vector.length, other.vector.length);
        AxesLocation [] newVector = new AxesLocation[maxOrder];
        for (int order = 0; order < maxOrder; order++) {
            if (order == 0) {
                newVector[order] = new AxesLocation(locationFunction, getVector(order), other.getVector(order));
            }
            else {
                newVector[order] = new AxesLocation(derivativeFunction, getVector(order), other.getVector(order));
            }
        }
        return new Motion(time, newVector, options);
    }
    public Motion average(Motion other) {
        return applyFunction(other.getTime(),
                (a, b) -> ((a+b)/2.0), 
                (a, b) -> ((a+b)/2.0), 
                other, other.options);
    }
    public Motion max(Motion other) {
        return applyFunction(other.getTime(),
                (a, b) -> (Math.max(a, b)), 
                (a, b) -> (Math.max(a, b)), 
                other, other.options);
    }
    public Motion min(Motion other) {
        return applyFunction(other.getTime(),
                (a, b) -> (Math.min(a, b)), 
                (a, b) -> (Math.min(a, b)), 
                other, other.options);
    }
    public Motion envelope(Motion other) {
        return applyFunction(other.getTime(),
                (a, b) -> (b), 
                (a, b) -> (Math.min(a, b)), 
                other, other.options);
    }
    public Motion interpolate(Motion other, double ratio) {
        return applyFunction((1.0-ratio)*this.getTime() + ratio*other.getTime(),
                (a, b) -> ((1.0-ratio)*a+ratio*b), 
                (a, b) -> ((1.0-ratio)*a+ratio*b), 
                other, other.options);
    }
}
