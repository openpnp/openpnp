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
public class KicadPcb {

    private String version;
    private double originX = 0;
    private double originY = 0;
    private List<KiFootprint> kifootprints = new ArrayList<>();

    public KicadPcb(String Sexfile) {

        // parse the file into SExpressions
        List<SExpressionParser.Node> root = SExpressionParser.parse(Sexfile);

        // check the file header
        // result = SExpressionParser.findNodesByPath(root, "kicad_pcb");

        // find the version
        version = SExpressionParser.getValueByPath(root, "pcbnew/version");
        if (version.isEmpty()) {
            // busted no file version date
        }

        List<String> values = SExpressionParser.getValuesByPath(root, "kicad_pcb/setup/aux_axis_origin");
        if (values.size() == 2) {
            originX = Double.parseDouble(values.get(0));
            originY = Double.parseDouble(values.get(1));
        }

        List<SExpressionParser.Node> footPrintList = SExpressionParser.findNodesByPath(root, "kicad_pcb/footprint");
        for (SExpressionParser.Node footprint : footPrintList) {
            kifootprints.add(new KiFootprint(footprint));
        }
    }

    public String getVersion() {
        return version;
    }

    public double getOriginX() {
        return originX;
    }

    public double getOriginY() {
        return originY;
    }

    public List<KiFootprint> getKiFootprints() {
        return kifootprints;
    }

    public KiFootprint getFootprint(String uuid)
    {
        for (KiFootprint fp : kifootprints) {
            if (fp.getUUID().equals(uuid)) {
                return fp;
            }
        }
        return  null;
    }
}

