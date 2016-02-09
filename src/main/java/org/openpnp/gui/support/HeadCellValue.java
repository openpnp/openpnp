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

package org.openpnp.gui.support;

import org.openpnp.model.Configuration;
import org.openpnp.spi.Head;

public class HeadCellValue {
	private static Configuration configuration;
	private Head head;

	public static void setConfiguration(Configuration configuration) {
		HeadCellValue.configuration = configuration;
	}

	public HeadCellValue(Head head) {
		this.head = head;
	}

	public HeadCellValue(String value) {
		Head head = configuration.getMachine().getHead(value);
		if (head == null) {
			throw new NullPointerException();
		}
		this.head = head;
	}

	public Head getHead() {
		return head;
	}

	public void setHead(Head head) {
		this.head = head;
	}

	@Override
	public String toString() {
		return head == null ? "NONE" : head.getName();
	}

	@Override
	public boolean equals(Object obj) {
		if (! (obj instanceof HeadCellValue)) {
			return false;
		}
		return ((HeadCellValue) obj).head == this.head;
	}

	@Override
	public int hashCode() {
		return this.head != null ? this.head.hashCode() : 0;
	}
}
