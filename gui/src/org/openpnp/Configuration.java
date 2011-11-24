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

package org.openpnp;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.openpnp.Part.FeederLocation;
import org.openpnp.spi.Machine;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;
import org.simpleframework.xml.stream.HyphenStyle;
import org.simpleframework.xml.stream.Style;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class Configuration {
	private HashMap<String, PackageDef> packages = new HashMap<String, PackageDef>();
	private HashMap<String, Part> parts = new HashMap<String, Part>();
	private Machine machine;
	
	public Configuration(String configurationDirectoryPath) throws Exception {
		File configurationDirectory = new File(configurationDirectoryPath);
		
		loadMachine(new File(configurationDirectory, "machine.xml"));
		loadPackages(new File(configurationDirectory, "packages.xml"));
		loadParts(new File(configurationDirectory, "parts.xml"));
		
//		Serializer serializer = createSerializer();
//		StringWriter writer = new StringWriter();
//		PartsConfigurationHolder holder = new PartsConfigurationHolder();
//		holder.parts = new ArrayList<Part>(parts.values());
//		serializer.write(holder, writer);
//		System.out.println(writer.toString());
//		System.exit(0);
	}
	
	public PackageDef getPackage(String ref) {
		return packages.get(ref);
	}
	
	public Part getPart(String ref) {
		return parts.get(ref);
	}
	
	public Collection<Part> getParts() {
		return parts.values();
	}
	
	public Machine getMachine() {
		return machine;
	}
	
	private void loadPackages(File file) throws Exception {
		Serializer serializer = createSerializer();
		PackagesConfigurationHolder holder = serializer.read(PackagesConfigurationHolder.class, file);
		for (PackageDef pkg : holder.packages) {
			packages.put(pkg.getId(), pkg);
		}
	}
	
	private void loadParts(File file) throws Exception {
		Serializer serializer = createSerializer();
		PartsConfigurationHolder holder = serializer.read(PartsConfigurationHolder.class, file);
		for (Part part : holder.parts) {
			parts.put(part.getId(), part);
		}
	}
	
	private void loadMachine(File file) throws Exception {
		Serializer serializer = createSerializer();
		MachineConfigurationHolder holder = serializer.read(MachineConfigurationHolder.class, file);
		machine = holder.machine;
	}
	
	private static Serializer createSerializer() {
		Style style = new HyphenStyle();
		Format format = new Format(style);
		AnnotationStrategy strategy = new AnnotationStrategy();
		Serializer serializer = new Persister(strategy, format);
		return serializer;
	}
	
	public static Document loadDocument(InputStream in) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse( 
        		new InputSource(in));
	}
	
	public static String getAttribute(Node n, String attribute) {
		return getAttribute(n, attribute, null);
	}
	
	public static String getAttribute(Node n, String attribute, String def) {
		Node node = n.getAttributes().getNamedItem(attribute);
		if (node == null) {
			return def;
		}
		return node.getNodeValue();
	}
	
	public static double getDoubleAttribute(Node n, String attribute) {
		return getDoubleAttribute(n, attribute, 0);
	}
	
	public static double getDoubleAttribute(Node n, String attribute, double def) {
		String s = getAttribute(n, attribute);
		if (s == null) {
			return def;
		}
		return Double.valueOf(s);
	}
	
	public static boolean getBooleanAttribute(Node n, String attribute) {
		return getBooleanAttribute(n, attribute, false);
	}
	
	public static boolean getBooleanAttribute(Node n, String attribute, boolean def) {
		String s = getAttribute(n, attribute);
		if (s == null) {
			return def;
		}
		return Boolean.valueOf(s);
	}
	
	public static LengthUnit getLengthUnitAttribute(Node n, String attribute) {
		return LengthUnit.valueOf(n.getAttributes().getNamedItem(attribute).getNodeValue());
	}
	
	@Root(name="openpnp-machine")
	public static class MachineConfigurationHolder {
		@Element
		private Machine machine;
	}
	
	@Root(name="openpnp-packages")
	public static class PackagesConfigurationHolder {
		@ElementList(inline=true, entry="package")
		private ArrayList<PackageDef> packages;
	}
	
	@Root(name="openpnp-parts")
	public static class PartsConfigurationHolder {
		@ElementList(inline=true, entry="part")
		private ArrayList<Part> parts;
	}
}
