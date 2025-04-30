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

package org.openpnp.gui.importer;

import java.awt.FileDialog;
import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.openpnp.Translations;
import org.openpnp.gui.MainFrame;
import org.openpnp.model.Footprint;
import org.openpnp.model.Footprint.Pad;

/**
 * @author Jonas Lehmke <jonas@lehmke.xyz>
 * 
 *         This module reads a KiCad footprint from file (*.kicad_mod) and
 *         parses it to a Footprint instance.
 *         Rectangular, rounded rectangular, circular and oval pad shapes are
 *         supported. Trapezoid and custom
 *         pad shapes are ignored. A FileDialog is openened to select a file
 *         once this class is instantiated.
 *         Imported pads are available as List<Pad>.
 * 
 *         This module may become part of an all-in-one KiCad board import one
 *         day.
 */

public class KicadModImporter {

    Footprint footprint = new Footprint();

    public class PadParserMultiline {

        public List<String> parsePadOccurrencesMultiline(String text) {
            List<String> capturedContents = new ArrayList<>();
            Pattern pattern = Pattern.compile("\\(pad(?<content>.*?)\\)", Pattern.DOTALL | Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(text);

            while (matcher.find()) {
                // String content = matcher.group("content");
                int openCount = 1; // Start with 1 because of the initial '(' in "(pad"
                int closeCount = 0;
                int startIndex = matcher.start();

                for (int i = startIndex + 4; i < text.length(); i++) {
                    char currentChar = text.charAt(i);
                    if (currentChar == '(') {
                        openCount++;
                    } else if (currentChar == ')') {
                        closeCount++;
                    }

                    if (openCount == closeCount) {
                        capturedContents.add(text.substring(startIndex + 4, i));
                        break;
                    }
                }
            }
            return capturedContents;
        }
    }

    public class KicadPad {

        String padDefinition;

        public KicadPad(String definition) {
            padDefinition = definition;
        }

        String getName() {
            Pattern p = Pattern.compile("\\s*\\t*\"(\\w*)\"\\s*\\t*(\\w*)\\s*\\t*(\\w*)",
                    Pattern.DOTALL | Pattern.MULTILINE);
            Matcher m = p.matcher(padDefinition);
            if (m.find()) {
                return m.group(1);
            }
            return "";
        }

        String getType() {
            Pattern p = Pattern.compile("\\s*\\t*\"(\\w*)\"\\s*\\t*(\\w*)\\s*\\t*(\\w*)",
                    Pattern.DOTALL | Pattern.MULTILINE);
            Matcher m = p.matcher(padDefinition);
            if (m.find()) {
                return m.group(2);
            }
            return "";
        }

        String getShape() {
            Pattern p = Pattern.compile("\\s*\\t*\"(\\w*)\"\\s*\\t*(\\w*)\\s*\\t*(\\w*)",
                    Pattern.DOTALL | Pattern.MULTILINE);
            Matcher m = p.matcher(padDefinition);
            if (m.find()) {
                return m.group(3);
            }
            return "";
        }

        double getWidth() {
            Pattern p = Pattern.compile("\\s*\\t*\\(size\\s*\\t*([\\d\\.+-]*)\\s*\\t*([\\d\\.+-]*)",
                    Pattern.DOTALL | Pattern.MULTILINE);
            Matcher m = p.matcher(padDefinition);
            if (m.find()) {
                return Double.parseDouble(m.group(1));
            }
            return 0.;
        }

        double getHeight() {
            Pattern p = Pattern.compile("\\s*\\t*\\(size\\s*\\t*([\\d\\.+-]*)\\s*\\t*([\\d\\.+-]*)",
                    Pattern.DOTALL | Pattern.MULTILINE);
            Matcher m = p.matcher(padDefinition);
            if (m.find()) {
                return Double.parseDouble(m.group(2));
            }
            return 0.;
        }

        double getX() {
            Pattern p = Pattern.compile("\\s*\\t*\\(at\\s*\\t*([\\d\\.+-]*)\\s*\\t*([\\d\\.+-]*)\\s*\\t*([\\d\\.+-]*)",
                    Pattern.DOTALL | Pattern.MULTILINE);
            Matcher m = p.matcher(padDefinition);
            if (m.find()) {
                return Double.parseDouble(m.group(1));
            }
            return 0.;
        }

        double getY() {
            Pattern p = Pattern.compile("\\s*\\t*\\(at\\s*\\t*([\\d\\.+-]*)\\s*\\t*([\\d\\.+-]*)\\s*\\t*([\\d\\.+-]*)",
                    Pattern.DOTALL | Pattern.MULTILINE);
            Matcher m = p.matcher(padDefinition);
            if (m.find()) {
                return Double.parseDouble(m.group(2)) * (-1); // Negative because of different conventions
            }
            return 0.;
        }

        double getRotation() {
            Pattern p = Pattern.compile("\\s*\\t*\\(at\\s*\\t*([\\d\\.+-]*)\\s*\\t*([\\d\\.+-]*)\\s*\\t*([\\d\\.+-]*)",
                    Pattern.DOTALL | Pattern.MULTILINE);
            Matcher m = p.matcher(padDefinition);
            if (m.find()) {
                if (m.group(3).length() > 0) {
                    return Double.parseDouble(m.group(3));
                }
            }
            return 0.;
        }

        double getRoundness() {
            Pattern p = Pattern.compile("\\s*\\t*\\(roundrect_rratio\\s*\\t*([\\d\\.+-]*)",
                    Pattern.DOTALL | Pattern.MULTILINE);
            Matcher m = p.matcher(padDefinition);
            if (m.find()) {
                return Double.parseDouble(m.group(1)) * 100;
            }
            return 0.;
        }

        boolean isTopCu() {
            Pattern p = Pattern.compile("\\s*\\t*\\(layers\\s*\\t*([^\\)]*)\\s*\\t*\\)",
                    Pattern.DOTALL | Pattern.MULTILINE);
            Matcher m = p.matcher(padDefinition);
            if (m.find()) {
                return m.group(1).contains("F.Cu") || m.group(1).contains("*.Cu");
            }
            return false;
        }
    }

    public KicadModImporter() throws Exception {
        try {
            FileDialog fileDialog = new FileDialog(MainFrame.get());
            fileDialog.setFilenameFilter(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".kicad_mod"); //$NON-NLS-1$
                }
            });
            fileDialog.setVisible(true);
            if (fileDialog.getFile() == null) {
                return;
            }

            Path path = Paths.get(fileDialog.getDirectory() + fileDialog.getFile());
            String content = Files.readString(path);

            // find all the pads
            PadParserMultiline parserMultiline = new PadParserMultiline();
            List<String> padList = parserMultiline.parsePadOccurrencesMultiline(content);

            for (String string : padList) {
                KicadPad kipad = new KicadPad(string);
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
                        continue;
                    }

                    footprint.addPad(pad);
                }
            }

        } catch (Exception e) {
            throw new Exception(Translations.getString("KicadModImporter.LoadFile.Fail") + e.getMessage()); //$NON-NLS-1$
        }
    }

    public List<Pad> getPads() {
        return footprint.getPads();
    }
}
