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
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;
import org.simpleframework.xml.stream.HyphenStyle;
import org.simpleframework.xml.stream.Style;

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

    /**
     * Create a standard OpenPnP Configuration serializer.
     * 
     * @return
     */
    public static Serializer createSerializer() {
        Style style = new HyphenStyle();
        Format format = new Format(style);
        AnnotationStrategy strategy = new AnnotationStrategy();
        Serializer serializer = new Persister(strategy, format);
        return serializer;
    }

    /**
     * Purges serialized properties of the given class from the serialized XML.
     * Note this does not care about XML structure (no CDATA support etc.), it just assumes each element or
     * attribute pattern is unique within the serialized string (which is not unreasonable).
     * 
     * @param cls
     * @param serialized
     * @return
     * @throws SecurityException
     */
    public static String purgeSubclassXml(Class cls, String serialized) throws SecurityException {
        HyphenStyle hyphenStyle = new HyphenStyle();
        for (Field f : cls.getDeclaredFields()) {
            // Handle all fields with xml annotation.
            for (Annotation annotation : f.getAnnotations()) {
                if (annotation.annotationType().getPackage().getName().equals("org.simpleframework.xml")) {
                    // Try element syntax.
                    String elementName = hyphenStyle.getElement(f.getName());
                    int begin = Math.max(serialized.indexOf("<"+elementName+">"),
                            serialized.indexOf("<"+elementName+" "));
                    if (begin >= 0) {
                        // Element without closing tag.
                        int end = serialized.indexOf("/>", begin+elementName.length()+2);
                        if (end > begin) {
                            end += 2;
                            serialized = serialized.substring(0, begin)
                                    + serialized.substring(end);
                        }
                        else {
                            // Element with closing tag.
                            end = serialized.indexOf("</"+elementName+">", begin+elementName.length()+2);
                            if (end > begin) {
                                end += elementName.length()+3;
                                serialized = serialized.substring(0, begin)
                                        + serialized.substring(end);
                            }
                        }
                    }
                    else {
                        // Empty Element.
                        begin = serialized.indexOf("<"+elementName+"/>");
                        if (begin >= 0) {
                            int end = begin+elementName.length()+3;
                            serialized = serialized.substring(0, begin)
                                    + serialized.substring(end);
                        }
                    }
                    // Try attribute syntax.
                    String attributeName = hyphenStyle.getAttribute(f.getName());
                    begin = serialized.indexOf(" "+attributeName+"=\"");
                    if (begin >= 0) {
                        int end = serialized.indexOf("\"", begin+attributeName.length()+3);
                        if (end >= begin) {
                            end += 1;
                            serialized = serialized.substring(0, begin)
                                    + serialized.substring(end);
                        }
                    }
                    break;//annotation
                }
            }
        }
        return serialized;
    }
}
