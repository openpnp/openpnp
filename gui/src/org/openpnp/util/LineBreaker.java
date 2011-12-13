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
			// if adding the word to the line will make it too long, flush the line
			lineLength += word.length();
			if (lineLength >= maxLength) {
				lines.add(line.toString());
				line = new StringBuffer();
				lineLength = word.length();
			}
			// if the word is longer than a line, break it up manually
			if (word.length() >= maxLength) {
				ArrayList<String> parts = forceBreakLongLine(word, maxLength);
				lines.addAll(parts);
			}
			else {
				line.append(word);
			}
			start = end;
			end = boundary.next();
		}
		
		if (line.length() > 0) {
			lines.add(line.toString());
		}
		
		String[] ret = lines.toArray(new String[] {});
		
		return ret;
	}
	
	public static ArrayList<String> forceBreakLongLine(String s, int maxLength) {
		ArrayList<String> parts = new ArrayList<String>();
		int index = 0;
		while (index <= s.length()) {
			String part = s.substring(index, Math.min(s.length(), index + maxLength));
			parts.add(part);
			index += part.length() + 1;
		}
		return parts;
	}
}
