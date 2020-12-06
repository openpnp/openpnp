/*
 * Copyright (C) 2020 <mark@makr.zone>
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

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.openpnp.model.Configuration;
import org.openpnp.model.Part;
import org.openpnp.spi.Feeder;
import org.openpnp.vision.pipeline.stages.SimpleOcr.OcrModel;

public class OcrUtil {
    /**
     * Compose a font list of the system. Always add the one currently selected, even if the system does not know it (yet), 
     * and optionally the empty selection.
     *  
     * @param selectedFont
     * @param addEmpty
     * @return
     */
    public static List<String> createFontSelectionList(String selectedFont, boolean addEmpty) {
        List<String> systemFontList = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames()); 
        List<String> fontList = new ArrayList<>();
        if (!systemFontList.contains(selectedFont)) {
            fontList.add(selectedFont);
        }
        if (addEmpty) {
            fontList.add("");
        }
        fontList.addAll(systemFontList);
        return fontList;
    }

    /**
     * From all the optionally filtered parts in the system, create the alphabet for OCR operation.
     * 
     * @param partFilter optional part filtering Function.
     * @param stockAlphabet TODO
     * @return
     */
    public static String getConsolidatedPartsAlphabet(Function<Part, Boolean> partFilter, String stockAlphabet) {
        Set<Character> characterSet = new HashSet<>();
        for (char ch : stockAlphabet.toCharArray()) {
            characterSet.add(ch);
        }
        for (Part part : Configuration.get().getParts()) {
            if (partFilter == null || partFilter.apply(part)) {
                for (char ch : part.getId().toCharArray()) {
                    characterSet.add(ch);
                }
            }
        }
        StringBuilder alphabet = new StringBuilder();
        for (char ch : characterSet) {
            if (ch != ' ') {
                alphabet.append(ch);
            }
        }
        return alphabet.toString();
    }
    /**
     * Identify an OCR detected part. This will automatically fuse line breaks, determine whether spaces are allowed 
     * and allow for partial matches in the typical form *-pattern (for redundant package prefixes etc.), but checking
     * for ambiguity.  
     * 
     * @param detectedOcrModel
     * @param feeder
     * @return
     * @throws Exception
     */
    public static Part identifyDetectedPart(OcrModel detectedOcrModel, Feeder feeder) throws Exception {
        String ocrText = detectedOcrModel.getText();
        Configuration cfg = Configuration.get();
        // Undo any forced line-breaking, marked by "\" at the end of the line.
        ocrText = ocrText.replace("\\\n", "");
        int pos = ocrText.indexOf('\n');
        if (pos >= 0) {
            ocrText = ocrText.substring(0, pos);
        }
        pos = ocrText.indexOf(' ');
        if (pos >= 0) {
            boolean allowSpace = false;
            for (Part part : cfg.getParts()) {
                if (part.getId().contains(" ")) {
                    allowSpace = true;
                    break;
                }
            }
            if (!allowSpace) {
                ocrText = ocrText.substring(0, pos);
            }
        }
        Part ocrPart = null;
        for (Part part : cfg.getParts()) {
            if (part.getId().equals(ocrText)) {
                // Direct match
                ocrPart = part;
                break;
            }
            else if (part.getId().endsWith("-"+ocrText)) { 
                // Partial match
                if (ocrPart != null) {
                    // Uh-oh, ambiguous!
                    throw new Exception("OCR part id "+ocrText+" on feeder "+feeder.getName()
                    +" matches multiple parts "+ocrPart.getId()+" and "+part.getId());
                }
                ocrPart = part;
            }
        }
        if (ocrPart == null) {
            throw new Exception("OCR could not identify/find part id on feeder "+feeder.getName()
            +", OCR detected part id "+ocrText+" (avg. score="+detectedOcrModel.getAvgScore()+")");
        }
        return ocrPart;
    }
}
