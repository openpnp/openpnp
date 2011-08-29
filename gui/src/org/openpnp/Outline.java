package org.openpnp;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * An Outline is a polygon shape with attached units that can be used for bounds checking
 * and can be drawn to a Graphics for display to the user.
 */
public class Outline {
	private List<Point2D.Double> points = new ArrayList<Point2D.Double>();
	private LengthUnit units;
	
	public void parse(Node n) throws Exception {
		XPath xpath = XPathFactory.newInstance().newXPath();

		units = Configuration.getLengthUnitAttribute(n, "units");
		
		NodeList pointNodes = (NodeList) xpath.evaluate("Point", n, XPathConstants.NODESET);

		for (int i = 0; i < pointNodes.getLength(); i++) {
			Node pointNode = pointNodes.item(i);
			addPoint(
					Configuration.getDoubleAttribute(pointNode, "x"),
					Configuration.getDoubleAttribute(pointNode, "y"));
		}
	}
	
	public void addPoint(double x, double y) {
		points.add(new Point2D.Double(x, y));
	}
	
	public LengthUnit getUnits() {
		return units;
	}

	public void setUnits(LengthUnit units) {
		this.units = units;
	}
	
	public List<Point2D.Double> getPoints() {
		return points;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		for (Point2D.Double point : points) {
			sb.append(point.getX() + "," + point.getY() + " -> ");
		}
		if (points.size() > 0) {
			sb.append(points.get(0).getX() + "," + points.get(0).getY());
		}
		
		return String.format("units %s, points (%s)", units, sb);
	}
}
