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
import java.util.List;

/**
 *
 * @author jay
 */
public class KiPad {

    private String name;
    private KiLocation location;
    private double width;
    private double height;
    private double roundness;

    public KiPad() {
    }

    public KiPad(SExpressionParser.Node pad) {
        if (!pad.values.isEmpty()) {
            name = pad.values.get(0);
        }
        
        location = new KiLocation(SExpressionParser.findNodesByPath(pad.children, "at").get(0));

        // find pad size
        List<SExpressionParser.Node> result = SExpressionParser.findNodesByPath(pad.children, "size");
        width = Double.parseDouble(result.get(0).values.get(0));
        height = Double.parseDouble(result.get(0).values.get(1));

        switch (pad.values.get(2)) {
            case "rect":
                break;
            case "circle":
            case "oval":
                roundness = 100;
                break;
            case "roundrect":
                result = SExpressionParser.findNodesByPath(pad.children, "roundrect_rratio");
                if (!result.isEmpty() && result.get(0).values.size() == 1) {
                    roundness = Double.parseDouble(result.get(0).values.get(0));
                }
                break;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public KiLocation getLocation() {
        return location;
    }

    public void setLocation(KiLocation location) {
        this.location = location;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public double getRoundness() {
        return roundness;
    }

    public void setRoundness(double roundness) {
        this.roundness = roundness;
    }

    public List<KiPad> getKiPads(SExpressionParser.Node footprint) {
        List<KiPad> kiPads = new ArrayList<>();
        List<SExpressionParser.Node> pads = SExpressionParser.findNodesByPath(footprint.children, "pad");

        for (SExpressionParser.Node pad : pads) {
            kiPads.add(new KiPad(pad));
        }
        return kiPads;
    }
}
