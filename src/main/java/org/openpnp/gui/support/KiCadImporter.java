/*
 * Copyright (C) 2023 Jason von Nieda <jason@vonnieda.org>
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

package org.openpnp.gui.support;

import java.awt.FileDialog;
import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileReader;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.Footprint;
import org.openpnp.model.Footprint.Pad;

/**
 * @author Jonas Lehmke <jonas@lehmke.xyz>
 * 
 * This module reads a KiCad footprint from file (*.kicad_mod) and parses it to a Footprint instance.
 * Rectangular, rounded rectangular, circular and oval pad shapes are supported. Trapezoid and custom 
 * pad shapes are ignored. A FileDialog is openened to select a file once this class is instantiated. 
 * Imported pads are available as List<Pad>.
 */

 
public class KiCadImporter {

    Footprint footprint = new Footprint();

    public class KiCadPad {
    
        String padDefinition;

        public KiCadPad(String definition) {
            padDefinition = definition;
        }

        String getName() {
            Pattern p = Pattern.compile("^\\(pad\\s\"(\\w*)\"\\s(\\w*)\\s(\\w*)");
            Matcher m = p.matcher(padDefinition);
            if(m.find()) {
                return m.group(1);
            }
            return "";
        }

        String getType() {
            Pattern p = Pattern.compile("^\\(pad\\s\"(\\w*)\"\\s(\\w*)\\s(\\w*)");
            Matcher m = p.matcher(padDefinition);
            if(m.find()) {
                return m.group(2);
            }
            return "";
        }

        String getShape() {
            Pattern p = Pattern.compile("^\\(pad\\s\"(\\w*)\"\\s(\\w*)\\s(\\w*)");
            Matcher m = p.matcher(padDefinition);
            if(m.find()) {
                return m.group(3);
            }
            return "";
        }

        double getWidth() {
            Pattern p = Pattern.compile("\\(size ([\\-0-9.]*) ([\\-0-9\\.]*)\\)");
            Matcher m = p.matcher(padDefinition);
            if(m.find()) {
                return Double.parseDouble(m.group(1));
            }
            return 0.;
        }

        double getHeight() {
            Pattern p = Pattern.compile("\\(size ([\\-0-9\\.]*) ([\\-0-9\\.]*)\\)");
            Matcher m = p.matcher(padDefinition);
            if(m.find()) {
                return Double.parseDouble(m.group(2));
            }
            return 0.;
        }

        double getX() {
            Pattern p = Pattern.compile("\\(at ([\\-0-9.]*) ([\\-0-9.]*)\\s?([^\\)]*)\\)");
            Matcher m = p.matcher(padDefinition);
            if(m.find()) {
                return Double.parseDouble(m.group(1));
            }
            return 0.;
        }

        double getY() {
            Pattern p = Pattern.compile("\\(at ([\\-0-9.]*) ([\\-0-9.]*)\\s?([^\\)]*)\\)");
            Matcher m = p.matcher(padDefinition);
            if(m.find()) {
                return Double.parseDouble(m.group(2)) * (-1); // Negative because of different conventions
            }
            return 0.;
        }

        double getRotation() {
            Pattern p = Pattern.compile("\\(at ([\\-0-9.]*) ([\\-0-9.]*)\\s?([^\\)]*)\\)");
            Matcher m = p.matcher(padDefinition);
            if(m.find()) {
                if (m.group(3).length() > 0) {
                    return Double.parseDouble(m.group(3));
                }
            }
            return 0.;
        }

        double getRoundness() {
            Pattern p = Pattern.compile("\\(roundrect_rratio ([\\-0-9.]*)\\)");
            Matcher m = p.matcher(padDefinition);
            if(m.find()) {
                return Double.parseDouble(m.group(1)) * 100;
            }
            return 0.;
        }

        boolean isTopCu() {
            Pattern p = Pattern.compile("\\(layers ([^\\)]*)\\)");
            Matcher m = p.matcher(padDefinition);
            if(m.find()) {
                return m.group(1).contains("F.Cu") || m.group(1).contains("*.Cu");
            }
            return false;
        }
    }

    public KiCadImporter() {
        try {
            FileDialog fileDialog = new FileDialog(MainFrame.get());
            fileDialog.setFilenameFilter(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".kicad_mod");
                }
            });
            fileDialog.setVisible(true);
            if (fileDialog.getFile() == null) {
                return;
            }

            File file = new File(new File(fileDialog.getDirectory()), fileDialog.getFile());
            BufferedReader reader = new BufferedReader(new FileReader(file));
            
            String line = reader.readLine();
            while (line != null) {
                if (line.trim().startsWith("(pad ")) {
                    KiCadPad kipad = new KiCadPad(line.trim());
                    if (kipad.getType().equals("smd") && kipad.isTopCu()) {
                        Pad pad = new Pad();
                        pad.setName(kipad.getName());
                        pad.setWidth(kipad.getWidth());
                        pad.setHeight(kipad.getHeight());
                        pad.setX(kipad.getX());
                        pad.setY(kipad.getY());
                        pad.setRotation(kipad.getRotation());

                        if (kipad.getShape().equals("rect")) {
                            pad.setRoundness(0);
                        } else if (kipad.getShape().equals("circle")) {
                            pad.setRoundness(100);
                        } else if (kipad.getShape().equals("oval")) {
                            pad.setRoundness(100);
                        } else if (kipad.getShape().equals("roundrect")) {
                            pad.setRoundness(kipad.getRoundness());
                        } else {
                            System.out.println("Warning: Unsupported pad type: " + kipad.getShape());
                            line = reader.readLine();
                            continue;
                        }

                        footprint.addPad(pad);
                    }
                }

                line = reader.readLine();
            }

            reader.close();
        }
        catch (Exception e) {
            MessageBoxes.errorBox(MainFrame.get(), "KiCad Footprint Load Error", e.getMessage());
        }
    }

    public List<Pad> getPads() {
        return footprint.getPads();
    }
}
