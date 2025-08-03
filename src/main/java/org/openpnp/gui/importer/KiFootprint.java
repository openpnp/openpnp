/*
 * Copyright (C) 2025 <jaytektas@github.com> 
 * inspired and based on work
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org> and Cri.S <phone.cri@gmail.com>
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

package org.openpnp.gui.importer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author jay
 */
public class KiFootprint {

    private String uuid;
    private String id;
    private String packageID;
    private String partID;
    private String descr;
    private KiLocation location;
    private List<String> layer = new ArrayList<>();
    private List<String> attr = new ArrayList<>();
    private List<KiPad> kipads = new ArrayList<>();
    private Map<String, String> properties = new HashMap<>();

    public KiFootprint(SExpressionParser.Node footprint) {
        uuid = SExpressionParser.getValueByPath(footprint.children, "uuid");
        id = footprint.values.get(0);

        descr = SExpressionParser.getValueByPath(footprint.children, "descr");

        List<SExpressionParser.Node> result;

        result = SExpressionParser.findNodesByPath(footprint.children, "attr");
        if (!result.isEmpty()) {
            if (result.get(0).values != null) {
                attr = result.get(0).values;
            }
        }

        result = SExpressionParser.findNodesByPath(footprint.children, "layer");
        if (!result.isEmpty()) {
            if (result.get(0).values != null) {
                layer = result.get(0).values;
            }
        }

        location = new KiLocation(SExpressionParser.findNodesByPath(footprint.children, "at").get(0));

        result = SExpressionParser.findNodesByPath(footprint.children, "property");
        if (!result.isEmpty()) {
            for (SExpressionParser.Node property : result) {
                setProperty(property.values.get(0), property.values.get(1));
            }
        }

        if (descr.isBlank()) {
            descr = getStringProperty("description");
        }

        packageID = getRightSideOfColon(id);
        partID = packageID + "-" + properties.get("value");

        // pads
        result = SExpressionParser.findNodesByPath(footprint.children, "pad");
        if (!result.isEmpty()) {
            for (SExpressionParser.Node pad : result) {
                kipads.add(new KiPad(pad));
                // we should adjust the relative pad rotation here as it considers the footprint rotation
            }
        }
    }

    public List<KiPad> getKiPads() {
        return kipads;
    }

    public String getUUID() {
        return uuid;
    }

    public String getID() {
        return id;
    }

    public String getPartID() {
        return partID;
    }

    public String getPackageID() {
        return packageID;
    }

    public String getDescription() {
        return descr;
    }

    public KiLocation getLocation() {
        return location;
    }

    public double getDoubleProperty(String property) {
        Double v = 0.0;
        try {
            String s = getStringProperty(property);
            if (!s.isBlank()) {
                v = Double.valueOf(getStringProperty(property));
            }
        } catch (NumberFormatException e) {
            System.out.println("getDoubleProperty(" + property + ") caused " + e.getMessage());
        }
        return v;
    }

    private void setProperty(String property, String value) {
        property = property.toLowerCase();
        properties.put(property, value);
    }

    public String getStringProperty(String property) {
        property = property.toLowerCase();
        String s = properties.get(property);
        if (s == null) {
            s = new String();
        }
        return s;
    }

    public List<String> getAttr() {
        return attr;
    }

    public List<String> getLayers() {
        return layer;
    }

    private String getRightSideOfColon(String inputString) {
        int colonIndex = inputString.indexOf(":");
        if (colonIndex != -1) {
            return inputString.substring(colonIndex + 1);
        } else {
            return inputString;
        }
    }
}
