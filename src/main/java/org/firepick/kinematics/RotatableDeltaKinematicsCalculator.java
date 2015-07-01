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

package org.firepick.kinematics;

import org.firepick.model.AngleTriplet;
import org.firepick.model.RawStepTriplet;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;

public class RotatableDeltaKinematicsCalculator {

    //TODO: These should eventually become configurable.
	private double DELTA_Z_OFFSET = 268.000;
	private double DELTA_EE_OFFS = 15.000;
	private double TOOL_OFFSET = 30.500;
	private double Z_CALC_OFFSET = ((DELTA_Z_OFFSET - TOOL_OFFSET - DELTA_EE_OFFS) * -1);
	
	private double HOME_ANGLE_X  = -67.200; // Angle of the "X" endstop sensor (0=horizontal)
	private double HOME_ANGLE_Y  = -67.200; // Angle of the "Y" endstop sensor (0=horizontal)
	private double HOME_ANGLE_Z  = -67.200; // Angle of the "Z" endstop sensor (0=horizontal)

	private double deltaE  = 131.636; // End effector length
	private double deltaF  = 190.526; // Base length
	private double deltaRe = 270.000; // Carbon rod length
	private double deltaRf = 90.000;  // Servo horn length
	
	private double XYZ_FULL_STEPS_PER_ROTATION = 200.0;
	private double XYZ_MICROSTEPS = 16.0;
	private double SMALL_PULLEY_TEETH = 16.0;
	private double BIG_PULLEY_TEETH = 150.0;
	private double PULLEY_REDUCTION = BIG_PULLEY_TEETH/SMALL_PULLEY_TEETH;
	private double XYZ_STEPS = (XYZ_FULL_STEPS_PER_ROTATION*XYZ_MICROSTEPS*PULLEY_REDUCTION)/360.0;
	
	public class RotatableDeltaKinematicsException extends Exception {
		public RotatableDeltaKinematicsException(String message){
		     super(message);
		}
	}
	
	//Return raw steps, given an angle
	public int getRawStepsFromAngle(double angle)
	{
		return (int)(angle * XYZ_STEPS + 0.5d);
	}
	
	//Get the raw step home positions for the three axes
	public RawStepTriplet getHomePosRaw()
	{
		return new RawStepTriplet(getRawStepsFromAngle(HOME_ANGLE_X),getRawStepsFromAngle(HOME_ANGLE_Y),getRawStepsFromAngle(HOME_ANGLE_Z));
	}
	
	//Get the homing angles for the three axes
	public AngleTriplet getHomePos()
	{
		return new AngleTriplet(HOME_ANGLE_X, HOME_ANGLE_Y, HOME_ANGLE_Z);
	}
	
	public Location getHomePosCartesian() throws RotatableDeltaKinematicsException {
		return delta_calcForward(getHomePos());
	}
	
	//Get the raw steps for a specified angle
	public RawStepTriplet getRawSteps(AngleTriplet deltaCalc)
	{
		return new RawStepTriplet(getRawStepsFromAngle(deltaCalc.x),getRawStepsFromAngle(deltaCalc.y),getRawStepsFromAngle(deltaCalc.z));
	}
	
	
	//Delta calc stuff
    private static final double sin120 = Math.sqrt(3.0) / 2.0;
    private static final double cos120 = -0.5;
    private static final double tan60 = Math.sqrt(3.0);
    private static final double sin30 = 0.5;
    private static final double tan30 = 1 / Math.sqrt(3.0);
    
    public AngleTriplet calculateDelta(Location cartesianLoc) throws RotatableDeltaKinematicsException 
    {
  	  //trossen tutorial puts the "X" in the front/middle. FPD puts this arm in the back/middle for aesthetics.
  	  double rotated_x = -1 * cartesianLoc.getX();
  	  double rotated_y = -1 * cartesianLoc.getY();
  	  double z_with_offset = cartesianLoc.getZ() + Z_CALC_OFFSET; //The delta calc below places zero at the top.  Subtract the Z offset to make zero at the bottom.
  	  
	  AngleTriplet solution = new AngleTriplet(0,0,0);
  	  try
  	  {
  	  	  solution.x = calculateYZ(rotated_x,                           rotated_y,                         z_with_offset);
  	  	  solution.y = calculateYZ(rotated_x*cos120 + rotated_y*sin120, rotated_y*cos120-rotated_x*sin120, z_with_offset);  // rotate coords to +120 deg
  	  	  solution.z = calculateYZ(rotated_x*cos120 - rotated_y*sin120, rotated_y*cos120+rotated_x*sin120, z_with_offset);  // rotate coords to -120 deg
  	  	  solution.successfulCalc = true;
  	  }
  	  catch (RotatableDeltaKinematicsException e)
  	  {
  		  throw new RotatableDeltaKinematicsException(String.format("Delta calcInverse: Non-existing point for Cartesian location x=%.3f, y=%.3f, z=%.3f, Z_CALC_OFFSET=%.3f", cartesianLoc.getX(), cartesianLoc.getY(), cartesianLoc.getZ(),Z_CALC_OFFSET ));
  	  }
  	  return solution;
    }
    
    //Helper function for calculateDelta()
	private double calculateYZ(double x, double y, double z) throws RotatableDeltaKinematicsException {

  	  double y1 = -0.5 * 0.57735 * deltaF; // f/2 * tg 30
      double y0 = y - (0.5 * 0.57735       * deltaE);    // shift center to edge
      // z = a + b*y
      double a = (x*x + y0*y0 + z*z +deltaRf*deltaRf - deltaRe*deltaRe - y1*y1) / (2*z);
      double b = (y1-y0)/z;
      // discriminant
      double d = -(a+b*y1)*(a+b*y1)+deltaRf*(b*b*deltaRf+deltaRf); 
      if (d < 0)
      {
    	  throw new RotatableDeltaKinematicsException("Delta calcInverse: Non-existing point"); // non-existing point
      }
      else
      {
          double yj = (y1 - a*b - Math.sqrt(d))/(b*b + 1); // choosing outer point
          double zj = a + b*yj;
          return (180.0*Math.atan(-zj/(y1 - yj))/Math.PI + ((yj>y1)?180.0:0.0));
      }
      //return 0;

	}
	
	// forward kinematics: (theta1, theta2, theta3) -> (x0, y0, z0)
	// returned status: 0=OK, -1=non-existing position
	public Location delta_calcForward(AngleTriplet angles)  throws RotatableDeltaKinematicsException 
	{
	    double t = (deltaF-deltaE)*tan30/2;
	    double dtr = Math.PI/180.0;
	    double theta1 = angles.x * dtr;
	    double theta2 = angles.y * dtr;
	    double theta3 = angles.z * dtr;
	 
	    double y1 = -(t + deltaRf * Math.cos(theta1));
	    double z1 = -deltaRf * Math.sin(theta1);
	 
	    double y2 = (t + deltaRf * Math.cos(theta2)) * sin30;
	    double x2 = y2 * tan60;
	    double z2 = -deltaRf * Math.sin(theta2);
	 
	    double y3 = (t + deltaRf * Math.cos(theta3)) * sin30;
	    double x3 = -y3 * tan60;
	    double z3 = -deltaRf * Math.sin(theta3);
	 
	    double dnm = (y2-y1)*x3-(y3-y1)*x2;
	 
	    double w1 = y1*y1 + z1*z1;
	    double w2 = x2*x2 + y2*y2 + z2*z2;
	    double w3 = x3*x3 + y3*y3 + z3*z3;
	     
	    // x = (a1*z + b1)/dnm
	    double a1 = (z2-z1)*(y3-y1)-(z3-z1)*(y2-y1);
	    double b1 = -((w2-w1)*(y3-y1)-(w3-w1)*(y2-y1))/2.0;
	 
	    // y = (a2*z + b2)/dnm;
	    double a2 = -(z2-z1)*x3+(z3-z1)*x2;
	    double b2 = ((w2-w1)*x3 - (w3-w1)*x2)/2.0;
	 
	    // a*z^2 + b*z + c = 0
	    double a = a1*a1 + a2*a2 + dnm*dnm;
	    double b = 2*(a1*b1 + a2*(b2-y1*dnm) - z1*dnm*dnm);
	    double c = (b2-y1*dnm)*(b2-y1*dnm) + b1*b1 + dnm*dnm*(z1*z1 - deltaRe*deltaRe);
	  
	    // discriminant
	    double d = b*b - (float)4.0*a*c;
	    if (d < 0)
    	{
	    	throw new RotatableDeltaKinematicsException(String.format("Delta calcForward: Non-existing point for angles x=%.3f, y=%.3f, z=%.3f", angles.x, angles.y, angles.z));
    	}
	 
	    
	    double z = -(double)0.5 * (b+Math.sqrt(d))/a;
	    double x = (a1*z + b1)/dnm;
	    double y = (a2*z + b2)/dnm;
	    
	    z -= Z_CALC_OFFSET; //NJ
	    
	    return new Location(LengthUnit.Millimeters, x, y, z, 0);
	}
}
