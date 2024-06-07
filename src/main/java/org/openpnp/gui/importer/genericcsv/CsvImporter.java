/*
 * Copyright (C) 2023 <99149230+janm012012@users.noreply.github.com> 
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

package org.openpnp.gui.importer.genericcsv;

import java.awt.Frame;

import org.openpnp.gui.importer.BoardImporter;
import org.openpnp.gui.importer.genericcsv.csv.GenericCSVParser;
import org.openpnp.gui.importer.genericcsv.gui.Dlg;
import org.openpnp.model.Board;

@SuppressWarnings("serial")
public abstract class CsvImporter {
    // this method provides a string that is used by the GUI
    public abstract String getImporterDescription();

    // this method shall be called by the parent to open a file open dialog and import
    // the selected file.
    public Board importBoard(Frame parent, BoardImporter importer) throws Exception {

        GenericCSVParser parser = getParser();

    	// open the file import dialog
        Dlg dlg = new Dlg(parent, this.getImporterDescription(), parser);
        dlg.setVisible(true);
        return dlg.getBoard();
    }

    public abstract GenericCSVParser getParser();
}
