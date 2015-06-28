package org.firepick.model;

public class AngleTriplet 
{
	public AngleTriplet(double x, double y, double z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		this.successfulCalc = false;
	}
	public double x = 0;
	public double y = 0;
	public double z = 0;
	public boolean successfulCalc = false;
}
