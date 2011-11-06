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
