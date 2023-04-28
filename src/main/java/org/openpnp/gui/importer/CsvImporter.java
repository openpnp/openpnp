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
public abstract class CsvImporter {
    private Board board;
    private File file;

    abstract String getImporterDescription();

    // provide arrays of strings for header detection
    // has to be local here to allow overwriting later using dedicated values
    // from derived clases
    private static String Refs[];
    private static String Vals[];
    private static String Packs[];
    private static String Xs[];
    private static String Ys[];
    private static String Rots[];
    private static String TBs[];
    private static String Heights[];
    private static String Comments[];
    
    public Board importBoard(Frame parent) throws Exception {
    	// get strings to parse CSV context
    	Refs     = getRefs();
    	Vals     = getVals();
    	Packs    = getPacks();
    	Xs       = getXs();
    	Ys       = getYs();
    	Rots     = getRots();
    	TBs      = getTBs();
    	Heights  = getHeights();
    	Comments = getComments();

    	// reset any previous result to allow the importer to detect a failure
        Ref = -1;
        Val = -1;
        Pack = -1;
        X = -1;
        Y = -1;
        Rot = -1;
        TB = -1;
        HT = -1;
        Comment = -1;
        Len = 0;
    	
        Dlg dlg = new Dlg(parent);
        dlg.setVisible(true);
        return board;
    }

    // provide methods to read the string arrays that are used to decode the header line
    // each array contains possible values/string for that type.
    // This converter converts the data read from file to upper case before compare. So 
    // provide upper case pattern only.
    abstract String[] getRefs();
    abstract String[] getVals();
    abstract String[] getPacks();
    abstract String[] getXs();
    abstract String[] getYs();
    abstract String[] getRots();
    abstract String[] getTBs();
    abstract String[] getHeights();
    abstract String[] getComments();
    
    // Column in data, one of the above strings have been found in the header line.
    // This indexes are used to detect if all required properties have been found
    // and to extract the data by its function.
    static private int Ref = -1, Val = -1, Pack = -1, X = -1, Y = -1, Rot = -1, TB = -1, HT = -1, Comment = -1, Len = 0;

    // automatic mil to mm conversion: if a property of the header ends in the specified
    // string, all values are converted from mil to mm. Again, the conversion is done with the
    // data read from file converted to upper case, so specify an upper case value here.
    static private final String MilToMM = "(MIL)";	//$NON-NLS-1$
    static private int units_mils_x = 0, units_mils_y = 0, units_mils_height = 0; // set if units
                                                                                  // are in mils not
                                                                                  // mm

    static private char comma = ',';

    // maximum number of lines at start of file to search for heading line describing the content
    private static final int maxHeaderLines = 50;

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
        return -1;
    }

    private static boolean checkCSV(String str[]) {

        // note that layer (TB) and Height (HT) are optional and thus checked against -2
        if ((Ref = checkCSV(Refs, str)) != -1 && (Val = checkCSV(Vals, str)) != -1
                && (Pack = checkCSV(Packs, str)) != -1 && (X = checkCSV(Xs, str)) != -1
                && (Y = checkCSV(Ys, str)) != -1 && (Rot = checkCSV(Rots, str)) != -1) {

            // the following fields are optional
            HT = checkCSV(Heights, str); // optional height field
            TB = checkCSV(TBs, str); // optional top/bottom layer field
            Comment = checkCSV(Comments, str); // optional comment field

        	// test if any value requires mil to mm conversion
            if (str[X].endsWith(MilToMM)) {
                units_mils_x = 1;
                Logger.trace("X units are in mils"); //$NON-NLS-1$
            }
            if (str[Y].endsWith(MilToMM)) {
                units_mils_y = 1;
                Logger.trace("Y units are in mils"); //$NON-NLS-1$
            }
            if (HT != -1 && str[HT].endsWith(MilToMM)) {
                units_mils_height = 1;
                Logger.trace("Height units are in mils"); //$NON-NLS-1$
            }
        	
            Len = Ref <= Len ? Len : Ref;
            Len = Val <= Len ? Len : Val;
            Len = Pack <= Len ? Len : Pack;
            Len = X <= Len ? Len : X;
            Len = Y <= Len ? Len : Y;
            Len = Rot <= Len ? Len : Rot;
            Len = TB <= Len ? Len : TB;
            Len = HT <= Len ? Len : HT;
            Logger.trace("checkCSV: Len = " + Len); //$NON-NLS-1$
            return true;
        }
        Logger.trace("checkCSV: Ref = " + Ref); //$NON-NLS-1$
        Logger.trace("checkCSV: Val = " + Val); //$NON-NLS-1$
        Logger.trace("checkCSV: Pack = " + Pack); //$NON-NLS-1$
        Logger.trace("checkCSV: X = " + X); //$NON-NLS-1$
        Logger.trace("checkCSV: Y = " + Y); //$NON-NLS-1$
        Logger.trace("checkCSV: Rot = " + Rot); //$NON-NLS-1$
        Logger.trace("checkCSV: TB = " + TB); //$NON-NLS-1$
        Logger.trace("checkCSV: HT = " + HT); //$NON-NLS-1$
        Logger.trace("checkCSV: Comment = " + Comment); //$NON-NLS-1$
        Ref = -1;
        Val = -1;
        Pack = -1;
        X = -1;
        Y = -1;
        Rot = -1;
        TB = -1;
        HT = -1;
        Comment = -1;
        Len = 0;
        return false;
    }

    private static boolean checkLine(String str) throws Exception {
        Logger.trace("checkLine: " + str); //$NON-NLS-1$
        String input_str = str.toUpperCase();
        if (input_str.charAt(0) == '#') {
            input_str = input_str.substring(1);
        }
        if (input_str == null) {
            return false;
        }
        // sting not empty, try to decode it as header line
        String as[], at[][];
        CSVParser csvParser = new CSVParser(new StringReader(input_str));
        as = csvParser.getLine();
        comma = ',';
        if (as.length >= 6 && checkCSV(as)) {
            return true;
        }
        at = CSVParser.parse(input_str, comma = '\t');
        if (at.length > 0 && at[0].length >= 6 && checkCSV(at[0])) {
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

    private static List<Placement> parseFile(File file, boolean createMissingParts,
            boolean updateHeights) throws Exception {
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(new FileInputStream(file), "ISO-8859-1")); //$NON-NLS-1$
        ArrayList<Placement> placements = new ArrayList<>();
        String line;

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


        if (Len == 0) {
            reader.close();
            throw new Exception("Unable to find relevant headers' names.\n See https://github.com/openpnp/openpnp/wiki/Importing-Centroid-Data for more."); //$NON-NLS-1$
        }

        // CSVParser csvParser = new CSVParser(new FileInputStream(file));
        CSVParser csvParser = new CSVParser(reader, comma);
        for (String as[]; (as = csvParser.getLine()) != null;) {
            if (as.length <= Len) {
                continue;
            }
            else {
                double placementX = Double.parseDouble(as[X].replace(",", ".").replace(" ", "") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                        .replace("mm", "").replace("mil", "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                // convert mils to mmm
                if (units_mils_x == 1) {
                    placementX = placementX * 0.0254;
                }
                double placementY = Double.parseDouble(as[Y].replace(",", ".").replace(" ", "") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                        .replace("mm", "").replace("mil", "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

                // convert mils to mmm
                if (units_mils_y == 1) {
                    placementY = placementY * 0.0254;
                }


                double heightZ = 0.0; // set default height to zero in case not included in CSV
                if (HT != -1) {
                    heightZ = Double.parseDouble(as[HT].replace(",", ".").replace(" ", "") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                            .replace("mm", "").replace("mil", "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

                    // convert mils to mmm
                    if (units_mils_height == 1) {
                        heightZ = heightZ * 0.0254;
                    }
                }

                double placementRotation =
                        Double.parseDouble(as[Rot].replace(",", ".").replace(" ", "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                while (placementRotation > 180.0) {
                    placementRotation -= 360.0;
                }
                while (placementRotation < -180.0) {
                    placementRotation += 360.0;
                }


                Placement placement = new Placement(as[Ref]);
                placement.setLocation(new Location(LengthUnit.Millimeters, placementX, placementY,
                        0, placementRotation));
                Configuration cfg = Configuration.get();
                if (cfg != null && createMissingParts) {
                    String partId = as[Pack] + "-" + as[Val]; //$NON-NLS-1$
                    Part part = cfg.getPart(partId);

                    if (part == null) {
                        part = new Part(partId);
                        Length l = new Length(heightZ, LengthUnit.Millimeters);
                        part.setHeight(l);
                        Package pkg = cfg.getPackage(as[Pack]);
                        if (pkg == null) {
                            pkg = new Package(as[Pack]);
                            cfg.addPackage(pkg);
                        }
                        part.setPackage(pkg);

                        cfg.addPart(part);
                    }

                    // if part exists and height exist and user wants height updated do it.
                    if (cfg != null && updateHeights && HT != -1) {
                        String partId2 = as[Pack] + "-" + as[Val]; //$NON-NLS-1$
                        Part part2 = cfg.getPart(partId2);
                        if (part2 != null) {
                            Length l = new Length(heightZ, LengthUnit.Millimeters);
                            part2.setHeight(l);
                        }
                    }
                    placement.setPart(part);

                }

                if(Comment != -1) {
                    placement.setComments(as[Comment]);
                }

                char c = 0;
                if (TB != -1) {
                    c = as[TB].toUpperCase().charAt(0);
                }
                placement.setSide(c == 'B' || c == 'Y' ? Side.Bottom : Side.Top);
                c = 0;
                placements.add(placement);
            }
        }
        reader.close();
        return placements;
    }

    class Dlg extends JDialog {
        private JTextField textFieldTopFile;
        private final Action browseTopFileAction = new SwingAction();
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

            textFieldTopFile = new JTextField();
            panel.add(textFieldTopFile, "4, 2, fill, default"); //$NON-NLS-1$
            textFieldTopFile.setColumns(10);

            JButton btnBrowse = new JButton(Translations.getString("CsvImporter.FilesPanel.browseButton.text")); //$NON-NLS-1$
            btnBrowse.setAction(browseTopFileAction);
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

            setSize(400, 400);
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
                textFieldTopFile.setText(file.getAbsolutePath());
            }
        }



        private class SwingAction_2 extends AbstractAction {
            public SwingAction_2() {
                putValue(NAME, Translations.getString("CsvImporter.Import2Action.Name")); //$NON-NLS-1$
                putValue(SHORT_DESCRIPTION, Translations.getString("CsvImporter.Import2Action.ShortDescription")); //$NON-NLS-1$
            }

            public void actionPerformed(ActionEvent e) {
                Logger.debug("Parsing " + textFieldTopFile.getText() + " CSV FIle"); //$NON-NLS-1$ //$NON-NLS-2$
                file = new File(textFieldTopFile.getText());
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
