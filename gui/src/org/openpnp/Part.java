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

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;



/**
 * A Part is a single part that can be picked and placed. It has a graphical outline, is retrieved from one or more Feeders
 * and is placed at a Placement as part of a Job. Parts can be used across many boards and should generally represent
 * a single part in the real world.
 */
public class Part {
	@Attribute
	private String id;
	@Attribute(required=false)
	private String name;
	@Attribute
	private String packageId;
	@Attribute
	private LengthUnit heightUnits;
	@Attribute
	private double height;
	@ElementList
	private ArrayList<FeederLocation> feederLocations = new ArrayList<FeederLocation>();;

	public String getId() {
		return id;
	}

	public void getId(String reference) {
		this.id = reference;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	public void setFeederLocations(ArrayList<FeederLocation> feederLocations) {
		this.feederLocations = feederLocations;
	}
	
	public String getPackageId() {
		return packageId;
	}

	@Override
	public String toString() {
		return String.format("ref %s, name %s, heightUnits %s, height %f, packageId (%s), feederLocations %s", id, name, heightUnits, height, packageId, feederLocations);
	}
	
	public static class FeederLocation {
		@Attribute
		private String feederId;
		
		@Element
		private Location location;
		
		public Location getLocation() {
			return location;
		}

		public void setLocation(Location location) {
			this.location = location;
		}
		
		public String getFeederId() {
			return feederId;
		}

		public void setFeederId(String feederId) {
			this.feederId = feederId;
		}

		@Override
		public String toString() {
			return String.format("feederId (%s), location (%s)", feederId, location);
		}
	}
}
