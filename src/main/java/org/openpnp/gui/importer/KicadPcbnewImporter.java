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

import java.awt.Frame;
import org.openpnp.Translations;
import static org.openpnp.gui.importer.KicadPcbnewImporterDialog.RET_OK;
import org.openpnp.model.Board;

/**
 *
 * @author jay
 */
public class KicadPcbnewImporter implements BoardImporter {

    private final static String NAME = "KiCAD .kicad_pcb"; //$NON-NLS-1$
    private final static String DESCRIPTION = "JAYTEK Kicad pcbnew importer";

    @Override
    public String getImporterName() {
        return NAME;
    }

    @Override
    public String getImporterDescription() {
        return DESCRIPTION;
    }

    /**
     * @param parent
     * @return
     * @throws java.lang.Exception
     * @wbp.parser.entryPoint
     */
    @Override
    public Board importBoard(Frame parent) throws Exception {
        Board board = null;

        KicadPcbnewImporterDialog kicadPcbnewImportDialog = new KicadPcbnewImporterDialog(parent, true);
        kicadPcbnewImportDialog.pack();
        kicadPcbnewImportDialog.setLocationRelativeTo(parent);
        kicadPcbnewImportDialog.setVisible(true);
        if (kicadPcbnewImportDialog.getReturnStatus() == RET_OK) {
            board = kicadPcbnewImportDialog.getBoard();
        }
        kicadPcbnewImportDialog.dispose();
        return board;
    }
}
