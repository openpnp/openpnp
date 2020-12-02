/*
 * Copyright (C) 2019-2020 <mark@makr.zone>
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.openpnp.model.Configuration;
import org.openpnp.model.Part;

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
}
