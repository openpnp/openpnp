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

package org.openpnp;

import java.util.ArrayList;
import java.util.List;

import org.openpnp.spi.Feeder;



/**
 * A Part is a single part that can be picked and placed. It has a graphical outline, is retrieved from one or more Feeders
 * and is placed at a Placement as part of a Job. Parts can be used across many boards and should generally represent
 * a single part in the real world.
 */
public class Part {
	private String reference;
	private String name;
	private PackageDef pkg;
	private LengthUnit heightUnits;
	private double height;
	private List<FeederLocation> feederLocations = new ArrayList<FeederLocation>();;

	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public PackageDef getPackage() {
		return pkg;
	}
	
	public void setPackage(PackageDef pkg) {
		this.pkg = pkg;
	}
	
	public LengthUnit getHeightUnits() {
		return heightUnits;
	}

	public void setHeightUnits(LengthUnit heightUnits) {
		this.heightUnits = heightUnits;
	}

	public double getHeight() {
		return height;
	}

	public void setHeight(double height) {
		this.height = height;
	}
	
	public List<FeederLocation> getFeederLocations() {
		return feederLocations;
	}

	public void setFeederLocations(List<FeederLocation> feederLocations) {
		this.feederLocations = feederLocations;
	}

	@Override
	public String toString() {
		return String.format("ref %s, name %s, heightUnits %s, height %f, package (%s), feederLocations %s", reference, name, heightUnits, height, pkg, feederLocations);
	}
	
	public static class FeederLocation {
		private Feeder feeder;
		private Location location;
		
		public Feeder getFeeder() {
			return feeder;
		}
		
		public void setFeeder(Feeder feeder) {
			this.feeder = feeder;
		}
		
		public Location getLocation() {
			return location;
		}

		public void setLocation(Location location) {
			this.location = location;
		}
		
		@Override
		public String toString() {
			return String.format("feeder (%s), location (%s)", feeder, location);
		}
	}
}
