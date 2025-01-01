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

/**
 * A simple solver for the Travelling Salesman Problem. 
 * 
 * The solver tries to find good solution for the following question:  
 * "Given a list of Locations, what is the shortest possible route that visits each Location?"
 * 
 * Optionally you can provide a start and/or end Location, e.g. the current machine Location, as the start Location
 * and/or a Location for the next task after that, as the end Location. These Locations can also be the same, to form 
 * a loop. If left open (null) the solver will choose the best start and/or end Location for the route freely.
 * 
 * The solver uses Simulated Annealing.
 * 
 * The implementation is a bit extended from the typical school book examples to not only use "swaps" of two Locations 
 * but also "twists", that reverse the travel direction between the swapped out Locations. The latter really improves the 
 * solutions a lot, because it allows the solver to quickly "untwist" routes at (or near) crossing points. These crossing 
 * points appear frequently for the rectangularly arrayed Location patterns assumed to be typically found on a PNP machine. 
 * 
 * @param <T> The class of the objects to be travelled to. Use a Locator<T> to query the Location from these objects.  
 */
public class TravellingSalesman<T> {

    /**
     * @param travelInput Contains the travelling problem to be solved. 
     * @param locator Lets the solver query the given list object for the relevant Location.
     * @param startLocation Optional start Location, e.g. the current machine Location. If left open, the solver will choose 
     * the start of the route freely. 
     * @param endLocation Optional end Location, e.g. the Location for the next task after this. If left open, the solver will choose 
     * the end of the route freely. 
     */
    public TravellingSalesman(List<T> travelInput, Locator<? super T> locator, Location startLocation, Location endLocation) {
        super();
        // register the problem
        this.travelInput = travelInput;
        this.locator = locator;
        // convert to the working List
        this.travel = new ArrayList<>();
        this.travelSize = travelInput.size();
        for (int i = 0; i < this.travelSize; i++) {
            this.travel.add(new TravelLocation(i, this.locator.getLocation(travelInput.get(i))));
        }
        // register start/end Locations
        this.startLocation = startLocation != null ? new TravelLocation(-1, startLocation) : null;
        this.endLocation = endLocation != null ? new TravelLocation(this.travelSize, endLocation) : null;
    }
    
    public interface Locator<T> {
        public Location getLocation(T locatable);
    }

    /**
     * Sets the debugLevel > 0 
     * level 0: no debugging 
     * level 1: console messages showing the solving progress
     * level 2: additional consistency checks
     */
    private static final int debugLevel = 0;

    /**
     * Factor to scale current travel distance before coping it over to global
     * best distance to avoid excessive copies due to rounding effects.
     */
    private static final double globalBestDistanceScalingFactor = 1.0 - 1e-5;
    
    /**
     * Plain old data TravelLocation for faster processing. Improved solving by a factor of 6 from using
     * OpenPNP Locations directly. These are always in Millimeters, no conversions needed.  
     */
    private static class TravelLocation {
        private  double x, y, z;
        private  int index;

        private  TravelLocation(int index, Location l) {
            super();
            this.index = index;
            l = l.convertToUnits(LengthUnit.Millimeters);
            this.x = l.getX();
            this.y = l.getY();
            this.z = l.getZ();
        }
        private double getLinearDistanceTo(TravelLocation other) {
            return Math.sqrt(Math.pow(this.x-other.x, 2.0) + Math.pow(this.y-other.y, 2.0) + Math.pow(this.z-other.z, 2.0));
        }
    }

    private final List<T> travelInput; 
    private final int travelSize;
    private final Locator<? super T> locator;
    private final TravelLocation startLocation;
    private final TravelLocation endLocation;
    private final List<TravelLocation> travel;
    
    private long solverDuration = 0; 

    private TravelLocation getLocation(int i) {
        if (i < 0) {
            return this.startLocation;
        }
        else if (i >= this.travelSize) {
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

    public double getTravellingDistance() {
        double distance = 0.0;
        for (int i = 0; i <= this.travelSize; i++) {
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

    @SuppressWarnings("unused")
    public double simulateAnnealing(double startingTemperature, double coolingRate, int maxIterations) {
        long startTime = System.currentTimeMillis();
        if (debugLevel > 0) {
            System.out.println("Simulated Annealing, size: "+this.travelSize+" temperature: " + startingTemperature + ", max iterations: " + maxIterations + ", cooling rate: " + coolingRate);
        }
        int i = maxIterations;
        int swaps = 0, twists = 0, copies = 0;
        double bestDistance = getTravellingDistance();
        double t = startingTemperature;
        if (debugLevel > 0) {
            System.out.println("Initial distance of travel: " + bestDistance);
        }
        if (this.travelSize > 1) {
            List<TravelLocation> globalTravel = new ArrayList<>(this.travel);           // global best route
            double globalBestDistance = globalBestDistanceScalingFactor * bestDistance; // cost of global best route

            // make this repeatable by seeding the random generator
            Random rnd = new java.util.Random(0);
            for (; i > 0; i--) {
                if (t > 0.1) {
                    int a = rnd.nextInt(this.travelSize);
                    int b;
                    do {
                        b = rnd.nextInt(this.travelSize);
                    }
                    while (b == a);
                    /* creates a locale emphasis when swapping
                    do {

                        b = (int) (Math.pow(rnd.nextDouble()*2.0-1.0, 3.0) * this.travelSize);
                    }
                    while(b < 0 || a == b || b > this.travelSize );
                     */
                    boolean twist = false;//(i % 2 == 0);
                    double swapDistance = getSwapDistance(a, b, false);
                    double twistDistance = getSwapDistance(a, b, true);
                    // choose the better option
                    if (twistDistance < swapDistance) {
                        twist = true;
                        swapDistance = twistDistance;
                    }

                    if (debugLevel > 1) {
                        // validate the differential swapDistance
                        double oldDistance = getTravellingDistance();
                        this.swapLocations(a, b, twist);
                        double newDistance = getTravellingDistance();
                        this.swapLocations(a, b, twist);
                        if (Math.abs((newDistance - oldDistance) - swapDistance) > 0.1) {
                            System.err.println("** Swap distance wrong - newDistance: " + newDistance + ", oldDistance: " + oldDistance +", swapDistance: "+swapDistance + " != "+(newDistance - oldDistance)+", twist: "+twist);
                        }
                    }

                    if (swapDistance < 0.0 || (Math.exp(-swapDistance / t) >= rnd.nextDouble())) {
                        // better or within annealing probability
                        this.swapLocations(a, b, twist);
                        bestDistance += swapDistance;   // keep bestDistance up-to-date
                        // if the new route is better then the best, remember it
                        if (bestDistance < globalBestDistance) {
                            // remember slightly worth distance do avoid excessive copies due to rounding effects
                            globalBestDistance = globalBestDistanceScalingFactor * bestDistance;
                            globalTravel = new ArrayList<>(this.travel);
                            copies++;
                        }
                        swaps++;
                        twists += twist ? 1 : 0;
                    }
                    t *= coolingRate;
                } else {
                    break;
                }
                if (debugLevel > 0) {
                    if (i % 100000 == 0) {
                        double distance = getTravellingDistance();
                        System.out.println("Iterations #" + i +", temperature: "+t+", distance of travel: " + distance + ", best distance to travel: " + globalBestDistance + ", swaps: "+swaps+", twists: "+twists+", copies: "+copies);
                        //System.out.println(this.asSvg());
                    }
                }
            }
            // log if global best route is better then last best route
            if (globalBestDistance < globalBestDistanceScalingFactor * bestDistance) {
                System.out.println("Simulated Annealing: global best route better then last best route");
            }
            // global best route is always the best route we have
            this.travel.clear();
            this.travel.addAll(globalTravel);
        }
        bestDistance = getTravellingDistance();
        if (debugLevel > 0) {
            System.out.println("Iterations #" + i +", temperature: "+t+",  distance of travel: " + bestDistance+", swaps: "+swaps+", twists: "+twists+", copies: "+copies);
        }
        long endTime = System.currentTimeMillis();
        this.solverDuration = endTime - startTime;
        return bestDistance;
    }

    public double solve() {
        // heuristic for the simulated annealing params
        int size = Math.max(1, this.travelSize);
        return simulateAnnealing(getTravellingDistance()/size*2.0, 1.0-0.001/size, size*1000+10000000);
    }

    public List<T> getTravel() {
        // convert the working list back to a list of the input objects using the now rearranged t.index order. 
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
        for (int i = -1; i <= this.travelSize; i++) {
            TravelLocation l = this.getLocation(i);
            if (l != null) {
                if (Double.isNaN(minX) || minX > l.x) {
                    minX = l.x;
                }
                if (Double.isNaN(minY) || minY > l.y) {
                    minY = l.y;
                }
                if (Double.isNaN(maxX) || maxX < l.x + l.z) {
                    maxX = l.x + l.z;
                }
                if (Double.isNaN(maxY) || maxY < l.y + l.z) {
                    maxY = l.y + l.z;
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
        svg.append("<title>Travelling Salesman ("+this.travelSize+" locations, "+Math.round(this.getTravellingDistance())+"mm, "+this.solverDuration+"ms)</title>\n");
        // shadows
        for (int i = -1; i < this.travelSize; i++) {
            TravelLocation la = this.getLocation(i);
            TravelLocation lb = this.getLocation(i+1);
            if (la != null && lb != null) {
                svg.append("<line x1=\""+(la.x+la.z)+"\" y1=\""+(la.y+la.z)+"\" x2=\""+(lb.x+lb.z)+"\" y2=\""+(lb.y+lb.z)+"\" style=\"stroke:lightgrey;\"/>");
                svg.append("<circle cx=\""+(lb.x+lb.z)+"\" cy=\""+(lb.y+lb.z)+"\" r=\"2\" style=\"fill:lightgrey;\"/>\n");
            }
        }
        // lines
        for (int i = -1; i < this.travelSize; i++) {
            TravelLocation la = this.getLocation(i);
            TravelLocation lb = this.getLocation(i+1);
            if (la != null && lb != null) {
                svg.append("<line x1=\""+la.x+"\" y1=\""+la.y+"\" x2=\""+lb.x+"\" y2=\""+lb.y+"\" style=\"stroke:black;\"/>");
            }
        }
        // nodes
        for (int i = -1; i <= this.travelSize; i++) {
            TravelLocation la = this.getLocation(i);
            if (la != null) {
                svg.append("<circle cx=\""+la.x+"\" cy=\""+la.y+"\" r=\"2\" style=\"");
                if (la == this.startLocation) { 
                    svg.append("stroke:blue; fill:white;");
                } 
                else if (la == this.endLocation) {
                    svg.append("stroke:green; fill:white;");
                }
                else {
                    svg.append("fill:red;");
                }
                svg.append("\"/>\n");
            }
        }
        svg.append("</svg>\n");
        return svg.toString();
    }
}
