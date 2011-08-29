package org.openpnp.machine.generic;

import org.openpnp.Location;
import org.openpnp.Part;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;

public class GenericHead implements Head {
	static final int ACTUATOR_PIN = 0;
	
	private final GenericMachine machine;
	private final double minX, maxX, homeX, minY, maxY, homeY, minZ, maxZ, homeZ, minA, maxA, homeA;
	double x, y, z, a;
	
	public GenericHead(GenericMachine machine) {
		this.machine = machine;
		
		minX = 0;
		maxX = 400;
		homeX = 0;
		
		minY = 0;
		maxY = 600;
		homeY = 0;
		
		minZ = 0;
		maxZ = 100;
		homeZ = 0;
		
		minA = 0;
		maxA = 360;
		homeA = 0;
	}

	@Override
	public void home() throws Exception {
		moveTo(x, y, homeZ, a);
		moveTo(homeX, homeY, homeZ, homeA);
	}

	@Override
	public void moveTo(double x, double y, double z, double a) throws Exception {
		machine.getDriver().moveTo(this, x, y, z, a);
		this.x = x;
		this.y = y;
		this.z = z;
		this.a = a;
	}
	
	public void updateCoordinates(double x, double y, double z, double a) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.a = a;
	}
	
	@Override
	public boolean canPickAndPlace(Feeder feeder, Location pickLocation,
			Location placeLocation) {
		return true;
	}
	
	@Override
	public void pick(Part part, Feeder feeder, Location pickLocation) throws Exception{
		// move to the pick location
		moveTo(pickLocation.getX(), pickLocation.getY(), z, pickLocation.getRotation());
		// lower the nozzle
		moveTo(x, y, pickLocation.getZ(), a);
		
		// pick the part
		machine.getDriver().pick(this, part);
	}

	@Override
	public void place(Part part, Location placeLocation) throws Exception {
		// move to the place location
		moveTo(placeLocation.getX(), placeLocation.getY(), z, placeLocation.getRotation());
		// lower the nozzle
		moveTo(x, y, placeLocation.getZ(), a);
		// place the part
		machine.getDriver().place(this);
	}
	
	public void actuate(int index, boolean on) throws Exception {
		machine.getDriver().actuate(this, index, on);
	}

	@Override
	public double getX() {
		return x;
	}

	@Override
	public double getY() {
		return y;
	}

	@Override
	public double getZ() {
		return z;
	}

	@Override
	public double getA() {
		return a;
	}
}
