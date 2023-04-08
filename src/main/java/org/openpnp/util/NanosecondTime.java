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

package org.openpnp.util;

import java.math.BigInteger;

/**
 * Simple Nanosecond time-keeper. Combines real-time (epoch) with nanosecond resolution by using BigInteger math.
 * Converted to String this makes for ideal ordered IDs.
 * Makes sure each obtained nanosecond time is unique and monotonically increasing.
 *
 */
public class NanosecondTime implements Comparable<NanosecondTime> {

    private static long nanosecondsLast = Long.MIN_VALUE;
    public static long getRuntime() {
        long nanoTime = System.nanoTime();
        if (nanoTime <= nanosecondsLast) {
            // Make it unique even if the calls are more frequent than the underlying nanoTime timer resolution. 
            nanoTime = ++nanosecondsLast;
        }
        else {
            nanosecondsLast = nanoTime;
        }
        return nanoTime;
    }
    public static double getRuntimeSeconds() {
        return (double) getRuntime()*1e-9;
    }
    public static long getRuntimeMilliseconds() {
        return getRuntime()/1000000;
    }

    private static NanosecondTime systemStartTime = null;
    public static NanosecondTime get() {
        if (systemStartTime == null) {
            // Initialize
            systemStartTime = new NanosecondTime(BigInteger.valueOf(System.currentTimeMillis())
                    .multiply(BigInteger.valueOf(1000000))
                        .subtract(BigInteger.valueOf(getRuntime())));
        }
        long nanoTime = getRuntime();
        return systemStartTime.add(nanoTime);
    }

    private BigInteger nanoTime;

    public NanosecondTime(BigInteger t) {
        this.nanoTime = t;
    }
    public NanosecondTime(long t) {
        this.nanoTime = BigInteger.valueOf(t);
    }
    public long subtract(NanosecondTime t) {
        return nanoTime.subtract(t.nanoTime).longValue();
    }
    public NanosecondTime add(NanosecondTime t) {
        return new NanosecondTime(nanoTime.add(t.nanoTime));
    }
    public NanosecondTime add(long t) {
        return new NanosecondTime(nanoTime.add(BigInteger.valueOf(t)));
    }
    public String toString() {
        return nanoTime.toString();
    }
    public String toString(int radix) {
        return nanoTime.toString(radix);
    }
    public double doubleValue() {
        return nanoTime.doubleValue();
    }
    public int compareTo(NanosecondTime t) {
        return nanoTime.compareTo(t.nanoTime);
    }
    public double getSeconds() {
        return nanoTime.doubleValue()*1e-9;
    }
    public long getMilliseconds() {
        return nanoTime.divide(BigInteger.valueOf(1000000)).longValue();
    }
    public long getNanosecondsFraction() {
        return nanoTime.mod(BigInteger.valueOf(1000000)).longValue();
    }
    public long getMicrosecondsFraction() {
        return getNanosecondsFraction()/1000;
    }
}
