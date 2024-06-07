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

import org.openpnp.Translations;
import org.openpnp.gui.importer.BoardImporter;
import org.openpnp.gui.importer.genericcsv.csv.GenericCSVParser;
import org.openpnp.model.Board;

public class ReferenceCsvImporter extends CsvImporter implements BoardImporter {
    private final static String NAME = "Reference CSV"; //$NON-NLS-1$
    private final static String DESCRIPTION = Translations.getString("ReferenceCsvImporter.Importer.Description"); //$NON-NLS-1$
    
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
        return super.importBoard(parent, this);
    }

	@Override
	public GenericCSVParser getParser() {
		return new ReferenceCsvParser();
	}
	private class ReferenceCsvParser extends GenericCSVParser {
		// methods to read of strings for each purpose to be found in the heading line
		// data read from file is converted to upper case before compare -> only list upper case pattern here
		// this is the list that is used by the default Named CSV importer
		public String[] getReferencePattern() {
			return new String[] {
					"DESIGNATOR",	//$NON-NLS-1$
					"PART",  		//$NON-NLS-1$
					"COMPONENT",	//$NON-NLS-1$
					"REFDES",		//$NON-NLS-1$
					"REF"			//$NON-NLS-1$
			};
		}
		public String[] getValuePattern() {
			return new String[] {
					"VALUE",  		//$NON-NLS-1$
					"VAL",  		//$NON-NLS-1$
					"COMMENT",		//$NON-NLS-1$
					"COMP_VALUE"	//$NON-NLS-1$
			};
		}
		public String[] getPackagePattern() {
			return new String[] {
					"FOOTPRINT",  	//$NON-NLS-1$
					"PACKAGE",  	//$NON-NLS-1$
					"PATTERN",  	//$NON-NLS-1$
					"COMP_PACKAGE" 	//$NON-NLS-1$
			};
		}
		public String[] getXPattern() {
			return new String[] {
					"X",  			//$NON-NLS-1$
					"X (MM)", 		//$NON-NLS-1$
					"REF X", 		//$NON-NLS-1$
					"POSX", 		//$NON-NLS-1$
					"REF-X(MM)", 	//$NON-NLS-1$
					"REF-X(MIL)", 	//$NON-NLS-1$
					"SYM_X"			//$NON-NLS-1$
			};
		}
		public String[] getYPattern() {
			return new String[] {
					"Y", 			//$NON-NLS-1$
					"Y (MM)", 		//$NON-NLS-1$
					"REF Y", 		//$NON-NLS-1$
					"POSY", 		//$NON-NLS-1$
					"REF-Y(MM)", 	//$NON-NLS-1$
					"REF-Y(MIL)", 	//$NON-NLS-1$
					"SYM_Y"			//$NON-NLS-1$
			};
		}
		public String[] getRotationPattern() {
			return new String[] {
					"ROTATION",		//$NON-NLS-1$
					"ROT", 			//$NON-NLS-1$
					"ROTATE", 		//$NON-NLS-1$
					"SYM_ROTATE"	//$NON-NLS-1$
			};
		}
		public String[] getSidePattern() {
			return new String[] {
					"LAYER", 		//$NON-NLS-1$
					"SIDE", 		//$NON-NLS-1$
					"TB", 			//$NON-NLS-1$
					"SYM_MIRROR"	//$NON-NLS-1$
			};
		}
		public String[] getHeightPattern() {
			return new String[] {
					"HEIGHT", 		//$NON-NLS-1$
					"HEIGHT(MIL)", 	//$NON-NLS-1$
					"HEIGHT(MM)"	//$NON-NLS-1$
			};
		}
		public String[] getCommentPattern() {
			return new String[] {
					"ADDCOMMENT"	//$NON-NLS-1$
			};
		}
	}
}
