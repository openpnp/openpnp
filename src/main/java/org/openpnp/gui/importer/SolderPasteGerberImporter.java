/*
    Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
    
    This file is part of OpenPnP.
    
    OpenPnP is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenPnP is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenPnP.  If not, see <http://www.gnu.org/licenses/>.
    
    For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.gui.importer;

import java.awt.Frame;
import java.io.File;

import org.openpnp.model.Board;

@SuppressWarnings("serial")
public class SolderPasteGerberImporter implements BoardImporter {
    private final static String NAME = "Gerber Solder Paste Layer";
    final static String DESCRIPTION = "Import RS-274X Gerber solder paste layers.";
    
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

    @Override
	public Board importBoard(Frame parent) throws Exception {
        SolderPasteGerberImporterDlg dlg = new SolderPasteGerberImporterDlg(this, parent);
		dlg.setVisible(true);
		return board;
	}
}
