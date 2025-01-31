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
import org.openpnp.util.Utils2D;
import org.pmw.tinylog.Logger;

import com.Ostermiller.util.CSVParser;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public abstract class CsvImporter {
    // this method provides a string that is used by the GUI
    abstract String getImporterDescription();

    // the following methods are called to get the pattern used to decode the data
    // each array contains possible strings that might appear in the header line.
    // If any appears, the corresponding column is decoded as the respective type.
    // This class converts the data read from file to upper case before compare. So 
    // provide upper case pattern only.
    abstract String[] getReferencePattern();
    abstract String[] getValuePattern();
    abstract String[] getPackagePattern();
    abstract String[] getXPattern();
    abstract String[] getYPattern();
    abstract String[] getRotationPattern();
    abstract String[] getSidePattern();
    abstract String[] getHeightPattern();
    abstract String[] getCommentPattern();
    
    // this variables hold the pattern to decode the data
    private String referencePattern[];
    private String valuePattern[];
    private String packagePattern[];
    private String xPattern[];
    private String yPattern[];
    private String rotationPattern[];
    private String sidePattern[];
    private String heightPattern[];
    private String commentPattern[];
    
    private Board board;
    private File file;

    // this method shall be called by the parent to open a file open dialog and import
    // the selected file.
    public Board importBoard(Frame parent) throws Exception {
    	// get strings to parse CSV context
    	referencePattern = getReferencePattern();
    	valuePattern     = getValuePattern();
    	packagePattern   = getPackagePattern();
    	xPattern         = getXPattern();
    	yPattern         = getYPattern();
    	rotationPattern  = getRotationPattern();
    	sidePattern      = getSidePattern();
    	heightPattern    = getHeightPattern();
    	commentPattern   = getCommentPattern();

    	// open the file import dialog
        Dlg dlg = new Dlg(parent);
        dlg.setVisible(true);
        return board;
    }

    // the following variable will contain the column indexes in which that data has
    // been detected using the string arrays define above.
    // This indexes are used to detect if all required properties have been found
    // and to extract the data by its function.
    private int referenceIndex;
    private int valueIndex;
    private int packageIndex;
    private int xIndex;
    private int yIndex;
    private int rotationIndex;
    private int sideIndex;
    private int heightIndex;
    private int commentIndex;
    
    // the length field contains the amount of columns required in data after the
    // header line has been detected and decoded
    private int len = 0;

    // automatic mil to mm conversion: if a property of the header ends in the specified
    // string, all values are converted from mil to mm. Again, the conversion is done with the
    // data read from file converted to upper case, so specify an upper case value here.
    static private final String MilToMM = "(MIL)";	//$NON-NLS-1$
    
    // this flags are used to remember if mil to mm conversion is required
    private boolean xUnitsMil;
    private boolean yUnitsMil;
    private boolean heightUnitsMil;
    
    // this character is used as separator when decoding the data
    // on a first pass, data is decoded using ','. On a second pass '\t' is used
    static private char separator = ',';

    // maximum number of lines at start of file to search for heading line describing the content
    private static final int maxHeaderLines = 50;

    // define that the data has to contain at least 6 columns. This is given by the fact
    // that reference, value, package, x, y and rotation are mandatory.
    private static final int minNumColumns = 6;
    
    //////////////////////////////////////////////////////////

    private static int checkCSV(String str[], String val[]) {
        for (int i = 0; i < str.length; i++) {
            for (int j = 0; j < val.length; j++) {
                if (str[i].equals(val[j])) {
                    Logger.trace("checkCSV: " + val[j] + " = " + j); //$NON-NLS-1$ //$NON-NLS-2$

                    return j;
                }
            }
        }
        // not found: return an invalid column index
        return -1;
    }

    private boolean checkCSV(String str[]) {

        // note that layer/side, height and comment are optional
        if (       (referenceIndex = checkCSV(referencePattern, str)) >= 0 
        		&& (valueIndex     = checkCSV(valuePattern,     str)) >= 0
                && (packageIndex   = checkCSV(packagePattern,   str)) >= 0  
                && (xIndex         = checkCSV(xPattern,         str)) >= 0
                && (yIndex         = checkCSV(yPattern,         str)) >= 0 
                && (rotationIndex  = checkCSV(rotationPattern,  str)) >= 0) {

            // the following fields are optional
            heightIndex  = checkCSV(heightPattern , str); // optional height field
            sideIndex    = checkCSV(sidePattern,    str); // optional top/bottom layer field
            commentIndex = checkCSV(commentPattern, str); // optional comment field

        	// test if any value requires mil to mm conversion
            xUnitsMil = str[xIndex].endsWith(MilToMM);
			if (xUnitsMil) {
                Logger.trace("X units are in mils"); //$NON-NLS-1$
            }
			yUnitsMil = str[yIndex].endsWith(MilToMM);
			if (yUnitsMil) {
                Logger.trace("Y units are in mils"); //$NON-NLS-1$
            }
			heightUnitsMil = heightIndex >= 0 && str[heightIndex].endsWith(MilToMM);
			if (heightUnitsMil) {
                Logger.trace("Height units are in mils"); //$NON-NLS-1$
            }
        	
			// find the largest index, which defines the required line length
			len = 0;
			len = Math.max(len, referenceIndex);
            len = Math.max(len, valueIndex);
            len = Math.max(len, packageIndex);
            len = Math.max(len, xIndex);
            len = Math.max(len, yIndex);
            len = Math.max(len, rotationIndex);
            len = Math.max(len, sideIndex);
            len = Math.max(len, heightIndex);
            len = Math.max(len, commentIndex);
            Logger.trace("checkCSV: Len = " + len); //$NON-NLS-1$
            return true;
        }
        // output values found
        Logger.trace("checkCSV: referenceIndex = " + referenceIndex); //$NON-NLS-1$
        Logger.trace("checkCSV: valueIndex = "     + valueIndex);     //$NON-NLS-1$
        Logger.trace("checkCSV: packageIndex = "   + packageIndex);   //$NON-NLS-1$
        Logger.trace("checkCSV: xIndex = "         + xIndex);         //$NON-NLS-1$
        Logger.trace("checkCSV: yIndex = "         + yIndex);         //$NON-NLS-1$
        Logger.trace("checkCSV: rotationIndex = "  + rotationIndex);  //$NON-NLS-1$
        Logger.trace("checkCSV: sideIndex = "      + sideIndex);      //$NON-NLS-1$
        Logger.trace("checkCSV: heightIndex = "    + heightIndex);    //$NON-NLS-1$
        Logger.trace("checkCSV: commentIndex = "   + commentIndex);   //$NON-NLS-1$
        // force length to invalid for following stages
        len = 0;
        return false;
    }

    private boolean checkLine(String str) throws Exception {
        Logger.trace("checkLine: " + str); //$NON-NLS-1$
        String input_str = str.toUpperCase();
        if (input_str.charAt(0) == '#') {
            input_str = input_str.substring(1);
        }
        if (input_str == null) {
            return false;
        }
        // sting not empty, try to decode it as header line
        String as[];		// a line of the file as string
        String at[][];		// the line as split into fields
        CSVParser csvParser = new CSVParser(new StringReader(input_str));
        as = csvParser.getLine();
        
        // first pass: try with ',' separator
        separator = ',';
        if (as.length >= minNumColumns && checkCSV(as)) {
            return true;
        }
        
        // second pass: try with '\t' separator
        separator = '\t';
        at = CSVParser.parse(input_str, separator);
        if (at.length > 0 && at[0].length >= minNumColumns && checkCSV(at[0])) {
            return true;
        }
        /*
         * at=csvParser.parse(input_str,comma=' '); if(at.length>0&&at[0].length>=6&&checkCSV(at[0]))
         * return true;
         */
        return false;
    }

    /*
     * CSVParser csvParser = new CSVParser( new FileInputStream("datei.csv") ); for ( String as[];
     * (as = csvParser.getLine()) != null; ) if(as.length<=Len) continue; else System.out.println(
     * csvParser.lastLineNumber() + " " + t );
     */
    //////////////////////////////////////////////////////////

    // convert given string taken from a .csv files field into a double
    // unified method to handle all conversions identical
    private double convert(String s) {
    	return Double.parseDouble(s
    			.replace(",", ".")    //$NON-NLS-1$ //$NON-NLS-2$
    			.replace(" ", ""));   //$NON-NLS-1$ //$NON-NLS-2$
    }
    // convert length string to double incl. units removal and mil to mm conversion
    private double convert(String s, boolean unitsInMil) {
    	double v = Double.parseDouble(s
    			.replace(",", ".")    //$NON-NLS-1$ //$NON-NLS-2$
    			.replace(" ", "")     //$NON-NLS-1$ //$NON-NLS-2$
                .replace("mm", "")    //$NON-NLS-1$ //$NON-NLS-2$
                .replace("mil", "")); //$NON-NLS-1$ //$NON-NLS-2$
    	// if units are in mil, convert them to the OpenPnP standard millimeter
    	if (unitsInMil) {
    		v = v * 0.0254;
    	}
    	return v;
    }
    
    // detect the character set required to read the file by searching for a byte order mark
    private String detectCharacterSet(File file) throws Exception {
        FileInputStream f = new FileInputStream(file);
        
        // read the first two characters and test if its a byte order mark
        // (https://en.wikipedia.org/wiki/Byte_order_mark)
        int b1 = f.read();
        int b2 = f.read();
        
        // if the file starts with 0xff 0xfe, its a byte order mark for UTF-16 encoding
        if (b1 == 0xff && b2 == 0xfe) {
            Logger.trace("Byte Order Mark detected, will read " + file + " using UTF-16 character set");
            return "UTF-16"; //$NON-NLS-1$
        }

        // return default character set
        Logger.trace("No Byte Order Mark detected, will read " + file + " using the default ISO-8859-1 character set");
        return "ISO-8859-1"; //$NON-NLS-1$
    }
    
    private List<Placement> parseFile(File file, boolean createMissingParts,
            boolean updateHeights) throws Exception {
        String characterset = detectCharacterSet(file);
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(new FileInputStream(file), characterset));
        ArrayList<Placement> placements = new ArrayList<>();
        String line;
        Configuration cfg = Configuration.get();

        // search for a maximum number of lines for headings describing the content
        for (int i = 0; i++ < maxHeaderLines && (line = reader.readLine()) != null;) {
            line = line.trim();
            if (line.length() == 0) {
                continue;
            }
            if (checkLine(line)) {
                break;
            }
        }

        if (len <= 0) {
            reader.close();
            throw new Exception("Unable to find relevant headers' names.\n See https://github.com/openpnp/openpnp/wiki/Importing-Centroid-Data for more."); //$NON-NLS-1$
        }

        // CSVParser csvParser = new CSVParser(new FileInputStream(file));
        CSVParser csvParser = new CSVParser(reader, separator);
        for (String as[]; (as = csvParser.getLine()) != null;) {
            if (as.length <= len) {
                continue;
            }
            else {
                String placementId = as[referenceIndex];
                double placementX = convert(as[xIndex], xUnitsMil);
                double placementY = convert(as[yIndex], yUnitsMil);

                double heightZ = 0.0; // set default height to zero in case its not included in CSV
                if (heightIndex >= 0) {
                    heightZ = convert(as[heightIndex], heightUnitsMil);
                }

                double placementRotation = convert(as[rotationIndex]);
                // convert rotation to [-180 .. 180]
                placementRotation = Utils2D.angleNorm(placementRotation, 180);
                
                String partId = as[packageIndex] + "-" + as[valueIndex]; //$NON-NLS-1$
                Part part = cfg.getPart(partId);

                // if part does not exist, create it
                if (part == null && createMissingParts) {
                    part = new Part(partId);
                    Length l = new Length(heightZ, LengthUnit.Millimeters);
                    part.setHeight(l);
                    Package pkg = cfg.getPackage(as[packageIndex]);
                    if (pkg == null) {
                        pkg = new Package(as[packageIndex]);
                        cfg.addPackage(pkg);
                    }
                    part.setPackage(pkg);

                    cfg.addPart(part);
                }

                // if we still don't have a part, skip this placement
                if (part == null) {
                    // no configuration -> skip placement
                    Logger.warn("no part for placement " + placementId + " (" + partId + ") found, skipped.");   //$NON-NLS-1$
                    continue;
                }
                
                // if part exists and height exist and user wants height updated do it.
                if (updateHeights && heightIndex >= 0) {
                    Length l = new Length(heightZ, LengthUnit.Millimeters);
                    part.setHeight(l);
                }

                // create new placement
                Placement placement = new Placement(placementId);

                // change placement type to Fiducial if the reference/id starts with "FID" or "REF" followed by a digit
                String id = placement.getId().toUpperCase();
                if (   (id.startsWith("FID") || id.startsWith("REF"))
                    && Character.isDigit(id.charAt(3))) {
                    placement.setType(Placement.Type.Fiducial);
                }
                
                // set placements location
                placement.setLocation(new Location(LengthUnit.Millimeters, placementX, placementY,
                        0, placementRotation));
                
                placement.setPart(part);

                // get optional comment
                if(commentIndex >= 0) {
                    placement.setComments(as[commentIndex]);
                }

                // get optional side
                char c = 0;
                if (sideIndex >= 0) {
                    c = as[sideIndex].toUpperCase().charAt(0);
                }
                placement.setSide(c == 'B' || c == 'Y' ? Side.Bottom : Side.Top);
                placements.add(placement);
            }
        }
        reader.close();
        return placements;
    }

    class Dlg extends JDialog {
        private JTextField textFieldFile;
        private final Action browseFileAction = new SwingAction();
        private final Action importAction = new SwingAction_2();
        private final Action cancelAction = new SwingAction_3();
        private JCheckBox chckbxCreateMissingParts;
        private JCheckBox chckbxUpdatePartHeight;

        public Dlg(Frame parent) {
            super(parent, getImporterDescription(), true);
            getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

            JPanel panel = new JPanel();
            panel.setBorder(new TitledBorder(null, Translations.getString("CsvImporter.FilesPanel.Border.title"), TitledBorder.LEADING, TitledBorder.TOP, //$NON-NLS-1$
                    null, null));
            getContentPane().add(panel);
            panel.setLayout(new FormLayout(
                    new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                            FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"), //$NON-NLS-1$
                            FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                    new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                            FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

            JLabel lblTopFilemnt = new JLabel(Translations.getString("CsvImporter.FilesPanel.topFilemntLabel.text")); //$NON-NLS-1$
            panel.add(lblTopFilemnt, "2, 2, right, default"); //$NON-NLS-1$

            textFieldFile = new JTextField();
            panel.add(textFieldFile, "4, 2, fill, default"); //$NON-NLS-1$
            textFieldFile.setColumns(30);

            JButton btnBrowse = new JButton(Translations.getString("CsvImporter.FilesPanel.browseButton.text")); //$NON-NLS-1$
            btnBrowse.setAction(browseFileAction);
            panel.add(btnBrowse, "6, 2"); //$NON-NLS-1$

            JPanel panel_1 = new JPanel();
            panel_1.setBorder(new TitledBorder(null, Translations.getString("CsvImporter.OptionsPanel.Border.title"), TitledBorder.LEADING, //$NON-NLS-1$
                    TitledBorder.TOP, null, null));
            getContentPane().add(panel_1);
            panel_1.setLayout(new FormLayout(
                    new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                    new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                            RowSpec.decode("default:grow")})); //$NON-NLS-1$

            chckbxCreateMissingParts = new JCheckBox(Translations.getString("CsvImporter.OptionsPanel.createMissingPartsChkbox.text")); //$NON-NLS-1$
            chckbxCreateMissingParts.setSelected(true);
            panel_1.add(chckbxCreateMissingParts, "2, 2"); //$NON-NLS-1$

            chckbxUpdatePartHeight = new JCheckBox(Translations.getString("CsvImporter.OptionsPanel.updatePartHeightChkbox.text")); //$NON-NLS-1$
            chckbxUpdatePartHeight.setSelected(true);
            panel_1.add(chckbxUpdatePartHeight, "2, 3"); //$NON-NLS-1$

            JSeparator separator = new JSeparator();
            getContentPane().add(separator);

            JPanel panel_2 = new JPanel();
            FlowLayout flowLayout = (FlowLayout) panel_2.getLayout();
            flowLayout.setAlignment(FlowLayout.RIGHT);
            getContentPane().add(panel_2);

            JButton btnCancel = new JButton(Translations.getString("CsvImporter.ButtonsPanel.cancelButton.text")); //$NON-NLS-1$
            btnCancel.setAction(cancelAction);
            panel_2.add(btnCancel);

            JButton btnImport = new JButton(Translations.getString("CsvImporter.ButtonsPanel.importButton.text")); //$NON-NLS-1$
            btnImport.setAction(importAction);
            panel_2.add(btnImport);

            // resize to the window to its preferred size
            pack();
            setLocationRelativeTo(parent);

            JRootPane rootPane = getRootPane();
            KeyStroke stroke = KeyStroke.getKeyStroke("ESCAPE"); //$NON-NLS-1$
            InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            inputMap.put(stroke, "ESCAPE"); //$NON-NLS-1$
            rootPane.getActionMap().put("ESCAPE", cancelAction); //$NON-NLS-1$
        }

        private class SwingAction extends AbstractAction {
            public SwingAction() {
                putValue(NAME, Translations.getString("CsvImporter.BrowseAction.Name")); //$NON-NLS-1$
                putValue(SHORT_DESCRIPTION, Translations.getString("CsvImporter.BrowseAction.ShortDescription")); //$NON-NLS-1$
            }

            public void actionPerformed(ActionEvent e) {
                FileDialog fileDialog = new FileDialog(Dlg.this);
                fileDialog.setFilenameFilter(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return false || name.toLowerCase().endsWith(".csv") //$NON-NLS-1$
                                || name.toLowerCase().endsWith(".txt") //$NON-NLS-1$
                                || name.toLowerCase().endsWith(".dat"); //$NON-NLS-1$
                    }
                });
                fileDialog.setVisible(true);
                if (fileDialog.getFile() == null) {
                    return;
                }
                File file = new File(new File(fileDialog.getDirectory()), fileDialog.getFile());
                textFieldFile.setText(file.getAbsolutePath());
            }
        }



        private class SwingAction_2 extends AbstractAction {
            public SwingAction_2() {
                putValue(NAME, Translations.getString("CsvImporter.Import2Action.Name")); //$NON-NLS-1$
                putValue(SHORT_DESCRIPTION, Translations.getString("CsvImporter.Import2Action.ShortDescription")); //$NON-NLS-1$
            }

            public void actionPerformed(ActionEvent e) {
                Logger.debug("Parsing " + textFieldFile.getText() + " CSV FIle"); //$NON-NLS-1$ //$NON-NLS-2$
                file = new File(textFieldFile.getText());
                board = new Board();
                List<Placement> placements = new ArrayList<>();
                try {
                    if (file.exists()) {
                        placements.addAll(parseFile(file, chckbxCreateMissingParts.isSelected(),
                                chckbxUpdatePartHeight.isSelected()));
                    }
                }
                catch (Exception e1) {
                    MessageBoxes.errorBox(Dlg.this, Translations.getString("CsvImporter.ImportErrorMessage"), e1); //$NON-NLS-1$
                    return;
                }
                for (Placement placement : placements) {
                    board.addPlacement(placement);
                }
                setVisible(false);
            }
        }

        private class SwingAction_3 extends AbstractAction {
            public SwingAction_3() {
                putValue(NAME, Translations.getString("CsvImporter.CancelAction.Name")); //$NON-NLS-1$
                putValue(SHORT_DESCRIPTION, Translations.getString("CsvImporter.CancelAction.ShortDescription")); //$NON-NLS-1$
            }

            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        }
    }
}
