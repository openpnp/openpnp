/*
 * Copyright (C) 2020 <mark@makr.zone>
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;

public class TravellingSalesman<T> {
    public class TravelLocation {
        public double x, y, z;
        public int index;

        public TravelLocation(int index, double x, double y, double z) {
            super();
            this.index = index;
            this.x = x;
            this.y = y;
            this.z = z;
        }
        public TravelLocation(int index, Location l) {
            super();
            this.index = index;
            l = l.convertToUnits(LengthUnit.Millimeters);
            this.x = l.getX();
            this.y = l.getY();
            this.z = l.getZ();
        }
        public double getLinearDistanceTo(TravelLocation other) {
            return Math.sqrt(Math.pow(this.x-other.x, 2.0) + Math.pow(this.y-other.y, 2.0) + Math.pow(this.z-other.z, 2.0));
        }
    }
    public interface Locator<T> {
        public Location getLocation(T locatable);
    }

    public TravellingSalesman(List<T> travelInput, Locator<? super T> locator, Location startLocation, Location endLocation) {
        super();
        this.travelInput = travelInput;
        this.locator = locator;
        this.startLocation = startLocation != null ? new TravelLocation(-1, startLocation) : null;
        this.endLocation = endLocation != null ? new TravelLocation(travelInput.size(), endLocation) : null;
        // convert to the working List
        this.travel = new ArrayList<>();
        for (int i = 0; i < travelInput.size(); i++) {
            this.travel.add(new TravelLocation(i, this.locator.getLocation(travelInput.get(i))));
        }
    }
    private List<T> travelInput; 
    private Locator<? super T> locator;
    private TravelLocation startLocation = null;
    private TravelLocation endLocation = null;
    private List<TravelLocation> travel = null;
    private long solverDuration = 0; 

    private TravelLocation getLocation(int i) {
        if (i < 0) {
            return this.startLocation;
        }
        else if (i >= this.travel.size()) {
            return this.endLocation;
        }
        return this.travel.get(i);
    }

    private double getDistance(int a, int b) {
        TravelLocation la = this.getLocation(a);
        TravelLocation lb = this.getLocation(b);
        if (la == null || lb == null) {
            // no start and/or end location, so the distance is just 0.0
            return 0.0;
        }
        return la.getLinearDistanceTo(lb);
    }

    private double getTravellingDistance() {
        double distance = 0.0;
        for (int i = 0; i <= this.travel.size(); i++) {
            distance += this.getDistance(i-1,  i);
        }
        return distance;
    }

    private double getSwapDistance(int a, int b, boolean twist) {
        if (a > b) {
            // a must come before b
            int s = a;
            a = b;
            b = s;
        }
        if (twist) {
            // twist the loop around
            double oldSegmentDistance = 
                    this.getDistance(a-1, a) + this.getDistance(b, b+1);
            double newSegmentDistance = 
                    this.getDistance(a-1, b) + this.getDistance(a, b+1);
            return newSegmentDistance - oldSegmentDistance;
        }
        else {
            // swap out the locations
            if (a + 1 == b ) {
                // consecutive
                double oldSegmentDistance = 
                        this.getDistance(a-1, a) + this.getDistance(a, b) + this.getDistance(b, b+1);
                double newSegmentDistance = 
                        this.getDistance(a-1, b) + this.getDistance(b, a) + this.getDistance(a, b+1);
                return newSegmentDistance - oldSegmentDistance;
            }
            else {
                // apart
                double oldSegmentDistance = 
                        this.getDistance(a-1, a) + this.getDistance(a, a+1) 
                        +  this.getDistance(b-1, b) + this.getDistance(b, b+1);
                double newSegmentDistance = 
                        this.getDistance(a-1, b) + this.getDistance(b, a+1) 
                        +  this.getDistance(b-1, a) + this.getDistance(a, b+1);
                return newSegmentDistance - oldSegmentDistance;
            }
        }
    }

    private void swap(int a, int b) {
        TravelLocation la = this.travel.get(a);
        TravelLocation lb = this.travel.get(b);
        this.travel.set(a, lb);
        this.travel.set(b, la);
    }

    private void swapLocations(int a, int b, boolean twist) {
        if (twist) {
            if (a > b) {
                // a must come before b
                int s = a;
                a = b;
                b = s;
            }
            // twist the loop around
            for (int i = 0; i < (b - a + 1)/2; i++) {
                this.swap(a+i, b-i);
            }
        }
        else {
            // swap out the two locations
            this.swap(a, b);
        }
    }


    public double simulateAnnealing(double startingTemperature, double coolingRate, int maxIterations, boolean debug) {
        long startTime = System.currentTimeMillis();
        if (debug) {
            System.out.println("Simulated Annealing, size: "+this.travel.size()+" temperature: " + startingTemperature + ", max iterations: " + maxIterations + ", cooling rate: " + coolingRate);
        }
        int i = maxIterations;
        double bestDistance = getTravellingDistance();
        double t = startingTemperature;
        if (debug) {
            System.out.println("Initial distance of travel: " + bestDistance);
        }
        if (this.travel.size() > 1) {
            // make this repeatable by seeding the random generator
            Random rnd = new java.util.Random(0);
            for (; i > 0; i--) {
                if (t > 0.1) {
                    int a = (int) (rnd.nextDouble() * this.travel.size());
                    int b;
                    do {
                        b = (int) (rnd.nextDouble() * this.travel.size());
                    }
                    while (b == a);
                    /* creates a locale emphasis when swapping
                    do {

                        b = (int) (Math.pow(rnd.nextDouble()*2.0-1.0, 3.0) * this.travel.size());
                    }
                    while(b < 0 || a == b || b > this.travel.size() );
                     */
                    boolean twist = (i % 2 == 0);
                    double swapDistance = getSwapDistance(a, b, twist);
                    /* This code segment would each time choose the better swap option. 
                    double twistDistance = getSwapDistance(a, b, true);
                    if (twistDistance < swapDistance) {
                        twist = true;
                        swapDistance = twistDistance;
                    }
                     */
                    /* This code segment validates the swapDistance.
                    if (debug) {
                        // validate the differential swapDistance
                        bestDistance = getTravellingDistance();
                        this.swapLocations(a, b, twist);
                        double newDistance = getTravellingDistance();
                        this.swapLocations(a, b, twist);
                        if (Math.abs((newDistance - bestDistance) - swapDistance) > 0.1) {
                            System.err.println("** Swap distance wrong - newDistance:" + newDistance + ", bestDistance:" + bestDistance +", swapDistance: "+swapDistance + " != "+(newDistance - bestDistance)+", twist: "+twist);
                        }
                    }
                     */

                    if (swapDistance < 0.0 || (Math.exp(-swapDistance / t) >= rnd.nextDouble())) {
                        // better or within annealing probability
                        this.swapLocations(a, b, twist);
                    }
                    t *= coolingRate;
                } else {
                    break;
                }
                if (debug) {
                    if (i % 100000 == 0) {
                        bestDistance = getTravellingDistance();
                        System.out.println("Iterations #" + i +", temperature: "+t+", distance of travel:" + bestDistance);
                        //System.out.println(this.asSvg());
                    }
                }
            }
        }
        bestDistance = getTravellingDistance();
        if (debug) {
            System.out.println("Iterations #" + i +", temperature: "+t+",  distance of travel:" + bestDistance);
        }
        long endTime = System.currentTimeMillis();
        this.solverDuration = endTime - startTime;
        return bestDistance;
    }

    public double solve(boolean debug) {
        // heuristic for the simulated annealing params
        int size = Math.max(1, this.travel.size());
        return simulateAnnealing(getTravellingDistance()/size*2.0, 1.0-0.001/size, size*1000+10000000, debug);
    }

    public List<T> getTravel() {
        // convert 
        List<T> travelOutput = new ArrayList<>();
        for (TravelLocation t : this.travel) {
            travelOutput.add(this.travelInput.get(t.index));
        }
        return travelOutput;
    }

    public Location getStartLocation() {
        return new Location(LengthUnit.Millimeters, startLocation.x, startLocation.y, startLocation.z, 0.);
    }

    public Location getEndLocation() {
        return new Location(LengthUnit.Millimeters, endLocation.x, endLocation.y, endLocation.z, 0.);
    }

    public long getSolverDuration() {
        return solverDuration;
    }

    public String asSvg() {
        double minX = Double.NaN, minY = Double.NaN;
        double maxX = Double.NaN, maxY = Double.NaN;
        for (int i = -1; i <= this.travel.size(); i++) {
            TravelLocation l = this.getLocation(i);
            if (l != null) {
                if (Double.isNaN(minX) || minX > l.x) {
                    minX = l.x;
                }
                if (Double.isNaN(minY) || minY > l.y) {
                    minY = l.y;
                }
                if (Double.isNaN(maxX) || maxX < l.x) {
                    maxX = l.x;
                }
                if (Double.isNaN(maxY) || maxY < l.y) {
                    maxY = l.y;
                }
            }
        }
        // round to centimeters
        minX = Math.floor(minX/10.0-1.0)*10.0;
        minY = Math.floor(minY/10.0-1.0)*10.0;
        maxX = Math.ceil(maxX/10.0+1.0)*10.0; // why +2.0? I don't know
        maxY = Math.ceil(maxY/10.0+1.0)*10.0;
        StringBuilder svg = new StringBuilder();
        svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
                "<svg xmlns=\"http://www.w3.org/2000/svg\"\n" + 
                "  xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n" + 
                "  version=\"1.1\" baseProfile=\"full\"\n" + 
                "  width=\"100%\" height=\"100%\"\n"+
                // "  width=\""+(maxX-minX)+"mm\" height=\""+(maxY-minY)+"mm\""+
                "  viewBox=\""+minX+" "+minY+" "+(maxX-minX)+" "+(maxY-minY)+"\">\r\n");
        svg.append("<title>Travelling Salesman ("+this.travel.size()+" locations, "+Math.round(this.getTravellingDistance())+"mm, "+this.solverDuration+"ms)</title>\n");
        for (int i = -1; i < this.travel.size(); i++) {
            TravelLocation la = this.getLocation(i);
            TravelLocation lb = this.getLocation(i+1);
            if (la != null && lb != null) {
                svg.append("<line x1=\""+(la.x+la.z)+"\" y1=\""+(la.y+la.z)+"\" x2=\""+(lb.x+lb.z)+"\" y2=\""+(lb.y+lb.z)+"\" style=\"stroke:lightgrey;\"/>");
                svg.append("<circle cx=\""+(lb.x+lb.z)+"\" cy=\""+(lb.y+lb.z)+"\" r=\"2\" style=\"fill:lightgrey;\"/>\n");
            }
        }
        for (int i = -1; i < this.travel.size(); i++) {
            TravelLocation la = this.getLocation(i);
            TravelLocation lb = this.getLocation(i+1);
            if (la != null && lb != null) {
                svg.append("<line x1=\""+la.x+"\" y1=\""+la.y+"\" x2=\""+lb.x+"\" y2=\""+lb.y+"\" style=\"stroke:black;\"/>");
                svg.append("<circle cx=\""+lb.x+"\" cy=\""+lb.y+"\" r=\"2\" style=\"fill:red;\"/>\n");
            }
        }
        svg.append("</svg>\n");
        return svg.toString();
    }
}
