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

import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.Board;
import org.openpnp.model.Board.Side;
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
public class NamedCSVImporter implements BoardImporter {
    private final static String NAME = "Named CSV";
    private final static String DESCRIPTION = "Import Named Comma Separated Values Files.";



    private Board board;
    private File topFile, bottomFile;

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
        Dlg dlg = new Dlg(parent);
        dlg.setVisible(true);
        return board;
    }


    //////////////////////////////////////////////////////////
    /*
     * public class CSVParseDemo { public static void main(String[] args) throws IOException { if
     * (args.length < 1) { System.out.println("Usage: java CSVParseDemo <csv_file>"); return; }
     * 
     * CSVParser parser = new CSVParser(new FileReader(args[0]), CSVStrategy.DEFAULT); String[]
     * values = parser.getLine(); while (values != null) { printValues(parser.getLineNumber(),
     * values); values = parser.getLine(); } }
     * 
     * private static void printValues(int lineNumber, String[] as) { System.out.println("Line " +
     * lineNumber + " has " + as.length + " values:"); for (String s: as) { System.out.println("\t|"
     * + s + "|"); } System.out.println(); } }
     */
    //////////////////////////////////////////////////////////
    // if((str.indexOf("val")!=-1||str.indexOf("comment"))&&str.indexOf("val")!=-1&&str.indexOf("val")!=-1&&

    private static final String Refs[] =
            {"Designator", "designator", "Part", "part", "Component", "component", "RefDes", "Ref"};
    private static final String Vals[] = {"Value", "value", "Val", "val", "Comment", "comment"};
    private static final String Packs[] =
            {"Footprint", "footprint", "Package", "package", "Pattern", "pattern"};
    private static final String Xs[] =
            {"X", "x", "X (mm)", "x (mm)", "Ref X", "ref x", "PosX", "Ref-X(mm)", "Ref-X(mil)"};
    private static final String Ys[] =
            {"Y", "y", "Y (mm)", "y (mm)", "Ref Y", "ref y", "PosY", "Ref-Y(mm)", "Ref-Y(mil)"};
    private static final String Rots[] = {"Rotation", "rotation", "Rot", "rot", "Rotate"};
    private static final String TBs[] = {"Layer", "layer", "Side", "side", "TB", "tb"};
    private static final String Heights[] = {"Height", "height", "Height(mil)", "Height(mm)"};
    //////////////////////////////////////////////////////////
    static private int Ref = -1, Val = -1, Pack = -1, X = -1, Y = -1, Rot = -1, TB = -1, HT = -1,
            Len = 0;
    static private int units_mils_x = 0, units_mils_y = 0, units_mils_height = 0; // set if units
                                                                                  // are in mils not
                                                                                  // mm

    static private char comma = ',';

    //////////////////////////////////////////////////////////

    private static int checkCSV(String str[], String val[]) {
        for (int i = 0; i < str.length; i++) {
            for (int j = 0; j < val.length; j++) {
                if (str[i].equals(val[j])) {
                    Logger.trace("checkCSV: " + val[j] + " = " + j);

                    // check for mil units
                    // TODO This should be done better, but at moment I don't know a better way...
                    // -trampas
                    if (val[j].equals("Ref-X(mil)")) {
                        units_mils_x = 1;
                        Logger.trace("X units are in mils");
                    }
                    if (val[j].equals("Ref-Y(mil)")) {
                        units_mils_y = 1;
                        Logger.trace("Y units are in mils");
                    }

                    if (val[j].equals("Height(mil)")) {
                        units_mils_height = 1;
                        Logger.trace("Height units are in mils");
                    }

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

            Len = Ref <= Len ? Len : Ref;
            Len = Val <= Len ? Len : Val;
            Len = Pack <= Len ? Len : Pack;
            Len = X <= Len ? Len : X;
            Len = Y <= Len ? Len : Y;
            Len = Rot <= Len ? Len : Rot;
            Len = TB <= Len ? Len : TB;
            Len = HT <= Len ? Len : HT;
            Logger.trace("checkCSV: Len = " + Len);
            return true;
        }
        Logger.trace("checkCSV: Ref = " + Ref);
        Logger.trace("checkCSV: Val = " + Val);
        Logger.trace("checkCSV: Pack = " + Pack);
        Logger.trace("checkCSV: X = " + X);
        Logger.trace("checkCSV: Y = " + Y);
        Logger.trace("checkCSV: Rot = " + Rot);
        Logger.trace("checkCSV: TB = " + TB);
        Logger.trace("checkCSV: HT = " + HT);
        Ref = -1;
        Val = -1;
        Pack = -1;
        X = -1;
        Y = -1;
        Rot = -1;
        TB = -1;
        HT = -1;
        Len = 0;
        return false;
    }

    private static boolean checkLine(String str) throws Exception {
        Logger.trace("checkLine: " + str);
        int e = 0;
        if (str.charAt(0) == '#') {
            str = str.substring(1);
        }
        if (str == null) {
            return false;
        }
        if (str.indexOf("X") == -1 && str.indexOf("x") == -1) {
            return false;
        }
        if (str.indexOf("Y") == -1 && str.indexOf("y") == -1) {
            return false;
        }
        if (str.indexOf("Rot") == -1 && str.indexOf("rot") == -1) {
            return false;
        }
        if (str.indexOf("val") == -1 && str.indexOf("Val") == -1 && str.indexOf("Comment") == -1
                && str.indexOf("comment") == -1) {
            return false;
        }
        if (str.indexOf("ootprint") == -1 && str.indexOf("ackage") == -1
                && str.indexOf("attern") == -1) {
            return false;
        }
        if (str.indexOf("Designator") == -1 && str.indexOf("designator") == -1
                && str.indexOf("Part") == -1 && str.indexOf("part") == -1
                && str.indexOf("Component") == -1 && str.indexOf("component") == -1
                && str.indexOf("RefDes") == -1 && str.indexOf("Ref") == -1) {
            return false;
        }
        // seems to have data
        String as[], at[][];
        CSVParser csvParser = new CSVParser(new StringReader(str));
        as = csvParser.getLine();
        comma = ',';
        if (as.length >= 6 && checkCSV(as)) {
            return true;
        }
        at = csvParser.parse(str, comma = '\t');
        if (at.length > 0 && at[0].length >= 6 && checkCSV(at[0])) {
            return true;
        }
        /*
         * at=csvParser.parse(str,comma=' '); if(at.length>0&&at[0].length>=6&&checkCSV(at[0]))
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
                new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        ArrayList<Placement> placements = new ArrayList<>();
        String line;

        for (int i = 0; i++ < 50 && (line = reader.readLine()) != null;) {
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
            throw new Exception("Unable to parse CSV File Names");
        }

        // CSVParser csvParser = new CSVParser(new FileInputStream(file));
        CSVParser csvParser = new CSVParser(reader, comma);
        for (String as[]; (as = csvParser.getLine()) != null;) {
            if (as.length <= Len) {
                continue;
            }
            else {
                double placementX = Double.parseDouble(as[X].replace(",", ".").replace(" ", "")
                        .replace("mm", "").replace("mil", ""));
                // convert mils to mmm
                if (units_mils_x == 1) {
                    placementX = placementX * 0.0254;
                }
                double placementY = Double.parseDouble(as[Y].replace(",", ".").replace(" ", "")
                        .replace("mm", "").replace("mil", ""));

                // convert mils to mmm
                if (units_mils_y == 1) {
                    placementY = placementY * 0.0254;
                }


                double heightZ = 0.0; // set default height to zero in case not included in CSV
                if (HT != -1) {
                    heightZ = Double.parseDouble(as[HT].replace(",", ".").replace(" ", "")
                            .replace("mm", "").replace("mil", ""));

                    // convert mils to mmm
                    if (units_mils_height == 1) {
                        heightZ = heightZ * 0.0254;
                    }
                }

                double placementRotation =
                        Double.parseDouble(as[Rot].replace(",", ".").replace(" ", ""));
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
                    String partId = as[Pack] + "-" + as[Val];
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
                        String partId2 = as[Pack] + "-" + as[Val];
                        Part part2 = cfg.getPart(partId2);
                        if (part2 != null) {
                            Length l = new Length(heightZ, LengthUnit.Millimeters);
                            part2.setHeight(l);
                        }
                    }
                    placement.setPart(part);

                }

                char c = 0;
                if (TB != -1) {
                    c = as[TB].charAt(0);
                }
                placement.setSide(c == 'B' || c == 'b' ? Side.Bottom : Side.Top);
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
            super(parent, DESCRIPTION, true);
            getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

            JPanel panel = new JPanel();
            panel.setBorder(new TitledBorder(null, "Files", TitledBorder.LEADING, TitledBorder.TOP,
                    null, null));
            getContentPane().add(panel);
            panel.setLayout(new FormLayout(
                    new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                            FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),
                            FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                    new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                            FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

            JLabel lblTopFilemnt = new JLabel("Centeroid File (.csv)");
            panel.add(lblTopFilemnt, "2, 2, right, default");

            textFieldTopFile = new JTextField();
            panel.add(textFieldTopFile, "4, 2, fill, default");
            textFieldTopFile.setColumns(10);

            JButton btnBrowse = new JButton("Browse");
            btnBrowse.setAction(browseTopFileAction);
            panel.add(btnBrowse, "6, 2");

            JPanel panel_1 = new JPanel();
            panel_1.setBorder(new TitledBorder(null, "Options", TitledBorder.LEADING,
                    TitledBorder.TOP, null, null));
            getContentPane().add(panel_1);
            panel_1.setLayout(new FormLayout(
                    new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                    new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                            RowSpec.decode("default:grow")}));

            chckbxCreateMissingParts = new JCheckBox("Create Missing Parts");
            chckbxCreateMissingParts.setSelected(true);
            panel_1.add(chckbxCreateMissingParts, "2, 2");

            chckbxUpdatePartHeight = new JCheckBox("Update Existing Part Heights");
            chckbxUpdatePartHeight.setSelected(true);
            panel_1.add(chckbxUpdatePartHeight, "2, 3");

            JSeparator separator = new JSeparator();
            getContentPane().add(separator);

            JPanel panel_2 = new JPanel();
            FlowLayout flowLayout = (FlowLayout) panel_2.getLayout();
            flowLayout.setAlignment(FlowLayout.RIGHT);
            getContentPane().add(panel_2);

            JButton btnCancel = new JButton("Cancel");
            btnCancel.setAction(cancelAction);
            panel_2.add(btnCancel);

            JButton btnImport = new JButton("Import");
            btnImport.setAction(importAction);
            panel_2.add(btnImport);

            setSize(400, 400);
            setLocationRelativeTo(parent);

            JRootPane rootPane = getRootPane();
            KeyStroke stroke = KeyStroke.getKeyStroke("ESCAPE");
            InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            inputMap.put(stroke, "ESCAPE");
            rootPane.getActionMap().put("ESCAPE", cancelAction);
        }

        private class SwingAction extends AbstractAction {
            public SwingAction() {
                putValue(NAME, "Browse");
                putValue(SHORT_DESCRIPTION, "Browse");
            }

            public void actionPerformed(ActionEvent e) {
                FileDialog fileDialog = new FileDialog(Dlg.this);
                fileDialog.setFilenameFilter(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return false || name.toLowerCase().endsWith(".csv")
                                || name.toLowerCase().endsWith(".txt")
                                || name.toLowerCase().endsWith(".dat");
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
                putValue(NAME, "Import");
                putValue(SHORT_DESCRIPTION, "Import");
            }

            public void actionPerformed(ActionEvent e) {
                Logger.debug("Parsing " + textFieldTopFile.getText() + " CSV FIle");
                topFile = new File(textFieldTopFile.getText());
                board = new Board();
                List<Placement> placements = new ArrayList<>();
                try {
                    if (topFile.exists()) {
                        placements.addAll(parseFile(topFile, chckbxCreateMissingParts.isSelected(),
                                chckbxUpdatePartHeight.isSelected()));
                    }
                }
                catch (Exception e1) {
                    MessageBoxes.errorBox(Dlg.this, "Import Error", e1);
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
                putValue(NAME, "Cancel");
                putValue(SHORT_DESCRIPTION, "Cancel");
            }

            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        }
    }
}
