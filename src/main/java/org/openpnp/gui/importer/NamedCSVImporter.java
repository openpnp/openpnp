/*
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

import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.TitledBorder;

import org.openpnp.Translations;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.Board;
import org.openpnp.model.Abstract2DLocatable.Side;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Package;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.pmw.tinylog.Logger;

import com.Ostermiller.util.CSVParser;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;


@SuppressWarnings("serial")
public class NamedCSVImporter extends CSVImporter implements BoardImporter {
    private final static String NAME = "Named CSV"; //$NON-NLS-1$
    private final static String DESCRIPTION = Translations.getString("NamedCSVImporter.Importer.Description"); //$NON-NLS-1$
    
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
        return super.importBoard(parent);
    }

    // lists of strings for each purpose to be found in the heading line
    // data read from file is converted to upper case before compare -> only list upper case pattern here
    // this is the list that is used by the default Named CSV importer
    private static final String cRefs[] = {"DESIGNATOR", "PART", "COMPONENT", "REFDES", "REF"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
    private static final String cVals[] = {"VALUE", "VAL", "COMMENT", "COMP_VALUE"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    private static final String cPacks[] = {"FOOTPRINT", "PACKAGE", "PATTERN", "COMP_PACKAGE"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    private static final String cXs[] = {"X", "X (MM)", "REF X", "POSX", "REF-X(MM)", "REF-X(MIL)", "SYM_X"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
    private static final String cYs[] = {"Y", "Y (MM)", "REF Y", "POSY", "REF-Y(MM)", "REF-Y(MIL)", "SYM_Y"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
    private static final String cRots[] = {"ROTATION", "ROT", "ROTATE", "SYM_ROTATE"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    private static final String cTBs[] = {"LAYER", "SIDE", "TB", "SYM_MIRROR"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    private static final String cHeights[] = {"HEIGHT", "HEIGHT(MIL)", "HEIGHT(MM)"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    private static final String cComments[] = {"ADDCOMMENT"}; //$NON-NLS-1$

    // provide methods to read the string arrays above
    public String[] getRefs()     { return cRefs; }
    public String[] getVals()     { return cVals; }
    public String[] getPacks()    { return cPacks; }
    public String[] getXs()       { return cXs; }
    public String[] getYs()       { return cYs; }
    public String[] getRots()     { return cRots; }
    public String[] getTBs()      { return cTBs; }
    public String[] getHeights()  { return cHeights; }
    public String[] getComments() { return cComments; }
}
