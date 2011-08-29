package org.openpnp;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.openpnp.Part.FeederLocation;
import org.openpnp.spi.Machine;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class Configuration {
	Map<String, PackageDef> packages = new HashMap<String, PackageDef>();
	Map<String, Part> parts = new HashMap<String, Part>();
	Machine machine;
	
	public Configuration(String configurationDirectory) throws Exception {
		loadMachine(new File(new File(configurationDirectory), "openpnp.xml"));
		loadPackages(new File(new File(configurationDirectory), "packages.xml"));
		loadParts(new File(new File(configurationDirectory), "parts.xml"));
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
        Document document = loadDocument(new FileInputStream(file));
        
		XPath xpath = XPathFactory.newInstance().newXPath();

		NodeList nodes = (NodeList) xpath.evaluate("/Packages/Package", document, XPathConstants.NODESET);

		for (int i = 0; i < nodes.getLength(); i++) {
			Node n = nodes.item(i);
			PackageDef pkg = new PackageDef();

			pkg.setReference(getAttribute(n, "reference"));
			pkg.setName(getAttribute(n, "name"));
			pkg.setOutline(new Outline());
			pkg.getOutline().parse((Node) xpath.evaluate("Outline", n, XPathConstants.NODE));
			
			packages.put(pkg.getReference(), pkg);
		}
	}
	
	private void loadParts(File file) throws Exception {
        Document document = loadDocument(new FileInputStream(file));
        
		XPath xpath = XPathFactory.newInstance().newXPath();

		NodeList nodes = (NodeList) xpath.evaluate("/Parts/Part", document, XPathConstants.NODESET);

		for (int i = 0; i < nodes.getLength(); i++) {
			Node n = nodes.item(i);
			Part part = new Part();
			
			part.setReference(getAttribute(n, "reference"));
			part.setName(getAttribute(n, "name"));
			part.setHeight(Double.valueOf(n.getAttributes().getNamedItem("height").getNodeValue()));
			part.setHeightUnits(LengthUnit.valueOf(n.getAttributes().getNamedItem("height-units").getNodeValue()));
			part.setPackage(getPackage(n.getAttributes().getNamedItem("package").getNodeValue()));
				
			parts.put(part.getReference(), part);
			
			NodeList feederLocationNodes = (NodeList) xpath.evaluate("FeederLocations/FeederLocation", n, XPathConstants.NODESET);
			part.setFeederLocations(loadFeederLocations(feederLocationNodes));
		}
	}
	
	private void loadMachine(File file) throws Exception {
        Document document = loadDocument(new FileInputStream(file));
        
		XPath xpath = XPathFactory.newInstance().newXPath();

		Node machineNode = (Node) xpath.evaluate("/OpenPnP/Machine", document, XPathConstants.NODE);
		
		machine = (Machine) Class.forName(getAttribute(machineNode, "class")).newInstance();
		
		Node configNode = (Node) xpath.evaluate("Configuration", machineNode, XPathConstants.NODE);
		
		machine.configure(configNode);
	}
	
	private List<FeederLocation> loadFeederLocations(NodeList nodes) throws Exception {
		XPath xpath = XPathFactory.newInstance().newXPath();
		
		List<FeederLocation> feederLocations = new ArrayList<FeederLocation>();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node n = nodes.item(i);
			FeederLocation feederLocation = new FeederLocation();
			
			feederLocation.setFeeder(machine.getFeeder(Configuration.getAttribute(n, "feeder")));
			
			Location location = new Location();
			location.parse((Node) xpath.evaluate("Location", n, XPathConstants.NODE));
			feederLocation.setLocation(location);
			
			feederLocations.add(feederLocation);
		}
		return feederLocations; 
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
	
	public static LengthUnit getLengthUnitAttribute(Node n, String attribute) {
		return LengthUnit.valueOf(n.getAttributes().getNamedItem(attribute).getNodeValue());
	}
}
