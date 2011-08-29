/*
 	Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 	
 	This file is part of OpenPnP.
 	
	OpenPnP is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenPnP is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenPnP.  If not, see <http://www.gnu.org/licenses/>.
 	
 	For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.machine.generic.driver;


/**
 * Motion controller
 */
public class MapleDriver {
	// 28.1192 steps per mm
	Axis xAxis = new Axis(28.1192, 16871);
	Axis yAxis = new Axis(28.1192, 16871);
	Axis zAxis = new Axis(28.1192, 16871);
	// 5.5555 steps per degree
	// a homes at 0* with a travel of +/-180*
	Axis aAxis = new Axis(5.5555, 16871);
	
	Axis[] axes = new Axis[] { xAxis, yAxis, zAxis, aAxis };

	public void moveTo(double x, double y, double z, double a) {
		// angles over 360* are silly
		a = a % 360.0;
		
		// if the travel is more than 180* we go the opposite direction
		if (a > 180) {
			a = (360 - a) * -1;
		}
		
		// first we determine how far each axis needs to travel to get
		// to it's target
		xAxis.stepsToGo = Math.round(x * xAxis.stepsPerUnit) - xAxis.currentStep;
		yAxis.stepsToGo = Math.round(y * yAxis.stepsPerUnit) - yAxis.currentStep;
		zAxis.stepsToGo = Math.round(z * zAxis.stepsPerUnit) - zAxis.currentStep;
		aAxis.stepsToGo = Math.round(a * aAxis.stepsPerUnit) - aAxis.currentStep;
		
		double timeToCompleteMovement = 0;
		for (Axis axis : axes) {
			// determine which direction we'll be stepping
			axis.direction = axis.stepsToGo > 0;
			// and switch to absolute number of steps
			axis.stepsToGo = Math.abs(axis.stepsToGo);
			// and determine the max time to complete the movement in all axes
			timeToCompleteMovement = Math.max(timeToCompleteMovement, Math.abs(axis.stepsToGo) / axis.stepsPerSecond);
		}
		
		// calculate the step time for each axis based on how long it will take the longest axis to move
		for (Axis axis : axes) {
			axis.stepTime = Math.round(timeToCompleteMovement / Math.abs(axis.stepsToGo) * 100000);
		}

		// perform the steps
		while (true) {
			boolean keepGoing = false;
			int output = 0;

			for (int i = 0; i < axes.length; i++) {
				Axis axis = axes[i];
				if (axis.stepsToGo > 0) {
					keepGoing = true;
					axis.stepTimeCount = Math.max(0, axis.stepTimeCount - 1);
					if (axis.stepTimeCount == 0) {
						output |= (1 << i);
						axis.stepTimeCount = axis.stepTime;
						axis.stepsToGo--;
					}
				}
				i++;
			}
			
			System.out.println(Integer.toBinaryString(output));
			
			if (!keepGoing) {
				break;
			}
		}
	}
	
	public static void main(String[] args) {
		MapleDriver driver = new MapleDriver();
		
		driver.moveTo(600, 20, 30, 40);
		driver.moveTo(20, 30, 40, 50);
		driver.moveTo(20, 10, 20, 35);
		driver.moveTo(10, 20, 30, 40);
	}
	
	
	class Axis {
		double stepsPerUnit;
		double stepsPerSecond;
		long currentStep;
		long stepsToGo;
		boolean direction;
		long stepTime;
		long stepTimeCount;
		
		public Axis(double stepsPerUnit, double stepsPerSecond) {
			this.stepsPerUnit = stepsPerUnit;
			this.stepsPerSecond = stepsPerSecond;
		}
	}
}
