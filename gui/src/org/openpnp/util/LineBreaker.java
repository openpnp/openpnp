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

package org.openpnp.util;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Locale;

public class LineBreaker {
	public static String[] formatLines(String target, int maxLength) {
		
		ArrayList<String> lines = new ArrayList<String>();
		Locale currentLocale = Locale.getDefault();

		BreakIterator boundary = BreakIterator.getLineInstance(currentLocale);
		boundary.setText(target);
		int start = boundary.first();
		int end = boundary.next();
		int lineLength = 0;

		StringBuffer line = new StringBuffer();
		
		while (end != BreakIterator.DONE) {
			String word = target.substring(start, end);
			lineLength = lineLength + word.length();
			if (lineLength >= maxLength) {
				lines.add(line.toString());
				line = new StringBuffer();
				lineLength = word.length();
			}
			line.append(word);
			start = end;
			end = boundary.next();
		}
		
		return lines.toArray(new String[] {});
	}
}
