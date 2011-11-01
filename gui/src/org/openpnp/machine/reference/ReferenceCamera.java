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

package org.openpnp.machine.reference;

import org.openpnp.Configuration;
import org.openpnp.Job;
import org.openpnp.Location;
import org.openpnp.spi.Camera;
import org.w3c.dom.Node;

/**
<pre>
	<Camera
		name="Head Tele"
		head="H1"
		class="org.openpnp.machine.reference.camera.VfwCamera"
		looking="Down"
		>
		<!-- 
			If the Camera is assigned to a Head the Location is the offset from the center of the tool
			holder to the center of the focal point of the Camera.
			
			To configure this offset, do the following:
			Measure the length of a tool and record this length as "toolLength".
			Place the tool in the tool holder.
			Touch off the tool to an object that you can use the Camera to focus on and record the 
			position of the Head as "touch".
			Now move the Camera to the center of the part that you touched off on and move the head until
			the Camera is in perfect focus and record the position of the Head as "focus".
			To calculate the offset, take touch.X - focus.X for X, touch.Y - focus.Y for Y and
			touch.Z - focus.Z + toolLength for Z.
			
			If the Camera is stationary the Location is the absolute machine coordinates of the
			center of the focal point of the camera.. 
		-->
		<Location units="Millimeters" x="-25" y="-25" z="20" rotation="0" />
		
		<UnitsPerPixel units="Millimeters" x="0.071875" y="0.070833" />
		
		<Configuration>
		</Configuration>
	</Camera>
</pre>
*/
public interface ReferenceCamera extends Camera {
	public void setName(String name);
	
	public void setLocation(Location location);
	
	public void setHead(ReferenceHead head);
	
	public void setLooking(Looking looking);
	
	public void setUnitsPerPixel(Location unitsPerPixel);
	
	public void configure(Node n) throws Exception;
	
	public void prepareJob(Configuration configuration, Job job) throws Exception;
}
