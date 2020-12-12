/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.util;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class XmlSerialize {
    public static String serialize(Object o) {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        XMLEncoder xmlEncoder = new XMLEncoder(bOut);
        xmlEncoder.writeObject(o);
        xmlEncoder.close();
        return bOut.toString();
    }

    public static Object deserialize(String s) {
        XMLDecoder xmlDecoder = new XMLDecoder(new ByteArrayInputStream(s.getBytes()));
        return xmlDecoder.readObject();
    }
    
    /**
     * From https://stackoverflow.com/questions/1265282/recommended-method-for-escaping-html-in-java
     * @param s
     * @return
     */
    public static String escapeXml(String s) {
        StringBuilder out = new StringBuilder(Math.max(16, s.length()));
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c > 127 || c == '"' || c == '\'' || c == '<' || c == '>' || c == '&') {
                out.append("&#");
                out.append((int) c);
                out.append(';');
            } 
            else if (c == '\n') {
                out.append("<br/>");
            }
            else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
