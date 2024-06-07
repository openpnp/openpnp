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

package org.openpnp.gui.importer.diptrace;

import java.awt.Frame;

import org.openpnp.Translations;
import org.openpnp.gui.importer.BoardImporter;
import org.openpnp.gui.importer.diptrace.csv.DipTraceCSVParser;
import org.openpnp.gui.importer.diptrace.gui.DiptraceBoardImporterDialog;
import org.openpnp.model.Board;

@SuppressWarnings("serial")
public class DipTraceBoardImporter implements BoardImporter {
    private final static String NAME = "Diptrace .csv"; //$NON-NLS-1$
    private final static String DESCRIPTION = Translations.getString("DipTraceImporter.Importer.Description"); //$NON-NLS-1$

    private Board board;

    @Override
    public String getImporterName() {
        return NAME;
    }

    @Override
    public String getImporterDescription() {
        return DESCRIPTION;
    }

    @Override
    public Board importBoard(Frame parent) throws Exception {
        DipTraceCSVParser parser = new DipTraceCSVParser();
        DiptraceBoardImporterDialog dialog = new DiptraceBoardImporterDialog(parent, getImporterDescription(), parser);
        dialog.setVisible(true);
        return dialog.getBoard();
    }

    public void setBoard(Board board) {
        this.board = board;
    }
}


