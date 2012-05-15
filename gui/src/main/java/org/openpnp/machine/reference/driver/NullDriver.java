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

package org.openpnp.machine.reference.driver;

import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.model.Part;
import org.simpleframework.xml.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NullDriver implements ReferenceDriver {
	private final static Logger logger = LoggerFactory.getLogger(NullDriver.class);
	
	@Attribute(required=false)
	private String nullDriver;

	@Override
	public void home(ReferenceHead head, double feedRateMmPerMinute)
			throws Exception {
	}

	@Override
	public void moveTo(ReferenceHead head, double x, double y, double z,
			double c, double feedRateMmPerMinute) throws Exception {
		logger.info(String.format("moveTo(%f, %f, %f, %f, %f)", x, y, z, c, feedRateMmPerMinute));
	}

	@Override
	public void pick(ReferenceHead head, Part part) throws Exception {
		logger.info(String.format("pick(%s)", part.toString()));
	}

	@Override
	public void place(ReferenceHead head) throws Exception {
		logger.info(String.format("place()"));
	}

	@Override
	public void actuate(ReferenceHead head, int index, boolean on)
			throws Exception {
		logger.info(String.format("actuate(%d, %s)", index, on));
	}

	@Override
	public void setEnabled(boolean enabled) throws Exception {
	}
}
