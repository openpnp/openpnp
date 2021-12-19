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
     * Simple Markup to HTML conversion.
     * 
     * @param s
     * @return
     */
    public static String convertMarkupToHtml(String s) {
        final char EOF = '\u001a'; // Add EOF guard
        s += EOF;
        StringBuilder out = new StringBuilder();
        out.append("<html>\n");
        boolean paragraph = false;
        int header = 0;
        boolean code = false;
        boolean list = false;
        boolean listItem = false;
        int lineBegin = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (c == EOF || lineBegin == i) { 
                // Handle the beginning of lines.
                if (paragraph) {
                    out.append("</p><br/>");
                    paragraph = false;
                }
                else if (header > 0){
                    out.append("</h"+header+">");
                    header = 0;
                }
                if (i+2 < s.length() && c == '`' && s.charAt(i+1) == '`' && s.charAt(i+2) == '`') {
                    if (listItem) {
                        out.append("</li>");
                        listItem = false;
                    }
                    if (list) {
                        out.append("</ul>");
                        list = false;
                    }
                    do {
                        i++;
                        c = s.charAt(i);
                    }
                    while ((c == '`' || c == '\n')&& i+1 < s.length());
                    if (code) {
                        out.append("</code></pre>\n");
                        code = false;
                    }
                    else {
                        out.append("\n<pre><code>");
                        code = true;
                    }
                }
                else if (code) {
                   // no action.
                }
                else if ((c == '*' || c == '-') && i+2 < s.length() && s.charAt(i+1) == ' ') {
                    if (!list) {
                        list = true;
                        out.append("\n<ul>");
                    }
                    if (listItem) {
                        out.append("</li>");
                    }
                    listItem = true;
                    out.append("\n<li>");
                    i += 2;
                    c = s.charAt(i);
                }
                else  {
                    if (listItem) {
                        out.append("</li>");
                        listItem = false;
                    }
                    if (list) {
                        out.append("</ul>");
                        list = false;
                    }

                    if (c == '#' && i+1 < s.length()) {
                        do {
                            i++;
                            c = s.charAt(i);
                            header++;
                        }
                        while (c == '#' && i+1 < s.length());
                        out.append("\n<h"+header+">");
                    }
                    else {
                        out.append("\n<p>");
                        paragraph = true;
                    }
                }
                if (c == EOF) {
                    break;
                }
            }

            if (c > 127 || c == '"' || c == '\'' || c == '<' || c == '>' || c == '&') {
                // Escape.
                out.append("&#");
                out.append((int) c);
                out.append(';');
            } 
            else if (c == '\n' && i+1 < s.length()) {
                // Skip over whitespace.
                int skip = 1;
                int lf = 0;
                while (i+skip+1 < s.length() 
                        && (s.charAt(i+skip) == ' ' 
                        || s.charAt(i+skip) == '\t'
                        || s.charAt(i+skip) == '\n')) {
                    if (s.charAt(i+skip) == '\n') {
                        lf++;
                    }
                    skip++;
                }
                // Look for new line characters.
                if (lf > 0
                        || code
                        || s.charAt(i+skip) == '`' 
                        || s.charAt(i+skip) == '#' 
                        || s.charAt(i+skip) == '*' 
                        || s.charAt(i+skip) == '-') {
                    // Skip whitespace
                    while (skip > 0) {
                        if (s.charAt(i) == '\t') {
                            out.append("  ");
                        }
                        else {
                            out.append(s.charAt(i));
                        }
                        i++;
                        skip--;
                    }
                    i--;
                    // New line starting next.
                    lineBegin = i + 1;
                }
            }
            else if (c == '[') {
                // Potential link.
                int len = s.indexOf("]", i) - i;
                if (len < 0 || s.charAt(i+len+1) != '(') {
                    out.append(c);
                }
                else {
                    int len2 = s.indexOf(")", i + len) - i - len;
                    if (len < 0) {
                        out.append(c);
                    }
                    else {  
                        String text = s.substring(i+1, i+len);
                        String url = s.substring(i + len + 2, i + len + len2);
                        out.append("<a href=\""+url+"\">"+escapeXml(text)+"</a>");
                        i += len + len2;
                    }
                }
            }
            else if (c == 'h' && i+10 < s.length() 
                    && (s.substring(i, i+8).equals("https://")
                            || s.substring(i, i+7).equals("http://"))) {
                // Potential plain link.
                int i1 = i;
                do {
                    i1++;
                    c = s.charAt(i1);
                    if (c == ' ' || c == '\n') {
                        break;
                    }
                }
                while (i1+1 < s.length());
                String uri =  s.substring(i, i1);
                String text = uri.length() > 60 ? 
                        uri.substring(0, 27)+"..."+uri.substring(uri.length()-30, uri.length()) 
                        : uri;
                out.append("<a href=\""+uri+"\">"+escapeXml(text)+"</a>");
                i = i1-1;
            }
            else if (code && c == ' ' && i - lineBegin > 40) {
                out.append("\n                ");
                lineBegin = i;
            }
            else if (code && c == '\t') {
                out.append("  ");
            }
            else {
                // Vanilla character.
                out.append(c);
            }
        }

        out.append("</html>\n");
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
        for (Field f : cls.getDeclaredFields()) {
            serialized = purgeFieldXml(serialized, f);
        }
        return serialized;
    }

    public static String purgeFieldXml(String serialized, Field f) {
        HyphenStyle hyphenStyle = new HyphenStyle();
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
        return serialized;
    }
}
