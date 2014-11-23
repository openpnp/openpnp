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

import java.util.Comparator;

import org.openpnp.model.Identifiable;

public class IdentifiableComparator<T extends Identifiable> implements Comparator<T> {
	public int compare(T o1, T o2) {
		if (o1 == null) {
			return 1;
		}
		else if (o2 == null) {
			return -1;
		}
		else {
			return o1.getId().compareTo(o2.getId());
		}
	}
}
