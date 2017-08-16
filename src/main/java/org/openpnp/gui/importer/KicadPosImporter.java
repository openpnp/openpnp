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

package org.openpnp.gui.importer;

import java.awt.Frame;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openpnp.model.Board;
import org.openpnp.model.Board.Side;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Package;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;

@SuppressWarnings("serial")
public class KicadPosImporter implements BoardImporter {
    private final static String NAME = "KiCAD .pos";
    final static String DESCRIPTION = "Import KiCAD .pos Files.";

    Board board;
    File topFile;
    File bottomFile;

    @Override
    public String getImporterName() {
        return NAME;
    }

    @Override
    public String getImporterDescription() {
        return DESCRIPTION;
    }

    /**
     * @wbp.parser.entryPoint
     */
    @Override
    public Board importBoard(Frame parent) throws Exception {
        KicadPosImporterDialog dlg = new KicadPosImporterDialog(this, parent);
        dlg.setVisible(true);
        return board;
    }

    static List<Placement> parseFile(File file, Side side, boolean createMissingParts, 
    		boolean useOnlyValueAsPartId)
            throws Exception {
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        ArrayList<Placement> placements = new ArrayList<>();
        String line;

        // See:
        // http://bazaar.launchpad.net/~kicad-product-committers/kicad/product/view/head:/pcbnew/exporters/gen_modules_placefile.cpp
        // ### Module positions - created on Tue 25 Mar 2014 03:42:43 PM PDT ###
        // ### Printed by Pcbnew version pcbnew (2014-01-24 BZR 4632)-product
        // ## Unit = mm, Angle = deg.
        // ## Side : F.Cu
        // # Ref Val Package PosX PosY Rot Side
        // C1 100u Capacitors_SMD:c 128.9050 -52.0700 0.0 F.Cu
        // C2 100u Capacitors_SMD:c 93.3450 -77.4700 180.0 F.Cu
        // C3 100u Capacitors_SMD:c 67.9450 -77.4700 180.0 F.Cu

        Pattern pattern = Pattern.compile(
                "(\\S+)\\s+(.*?)\\s+(.*?)\\s+(-?\\d+\\.\\d+)\\s+(-?\\d+\\.\\d+)\\s+(-?\\d+\\.\\d+)\\s(.*?)");

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.length() == 0 || line.charAt(0) == '#') {
                continue;
            }



            Matcher matcher = pattern.matcher(line);
            matcher.matches();

            String placementId = matcher.group(1);
            String partValue = matcher.group(2);
            String pkgName = matcher.group(3);
            double placementX = Double.parseDouble(matcher.group(4));
            double placementY = Double.parseDouble(matcher.group(5));
            double placementRotation = Double.parseDouble(matcher.group(6));
            String placementLayer = matcher.group(7);

            Placement placement = new Placement(placementId);
            placement.setLocation(new Location(LengthUnit.Millimeters, placementX, placementY, 0,
                    placementRotation));
            Configuration cfg = Configuration.get();
            if (cfg != null && createMissingParts) {
                String partId;
                if(useOnlyValueAsPartId == true) {
                	partId = partValue;
                }else {
                	partId = pkgName + "-" + partValue;
                }
                Part part = cfg.getPart(partId);
                if (part == null) {
                    part = new Part(partId);
                    Package pkg = cfg.getPackage(pkgName);
                    if (pkg == null) {
                        pkg = new Package(pkgName);
                        cfg.addPackage(pkg);
                    }
                    part.setPackage(pkg);

                    cfg.addPart(part);
                }
                placement.setPart(part);

            }

            placement.setSide(side);
            placements.add(placement);
        }
        reader.close();
        return placements;
    }
}
