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
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Package;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class DipTraceImporter implements BoardImporter {
    private final static String NAME = "Diptrace .csv";
    private final static String DESCRIPTION = "Import Diptrace .csv Files.";

    private Board board;
    private File fileName;
    //, bottomFile;

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

    private static List<Placement> parseFile(File file, boolean createMissingParts)
            throws Exception {
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        ArrayList<Placement> placements = new ArrayList<>();
        String line;
        int lineCount = 0;

        // 
        // Default format for DIPTRACE pick and place export is 
        // RefDes,Name,X (mm),Y (mm),Side,Rotate,Value
        // C1,C0603,8.6,7.2,Top,0,1nF
        // C2,C0402,10.81,22.99,Top,180,0.1uF/16V
        // <etc>

        while ((line = reader.readLine()) != null) {
        	
        	// Skip first line as it's always header
        	if (lineCount++ == 0 || line.length() == 0)  {
                continue;
            }
            line = line.trim();
            
            String[] tokens = line.split(",");
            
            String placementId = tokens[0];  							// RefDes in Diptrace export
            String partValue = tokens[6];    							// Value in Diptrace export
            String pkgName = tokens[1];      							// Name in Diptrace export
            double placementX = Double.parseDouble(tokens[2]);   		// X (mm) in Diptrace export
            double placementY = Double.parseDouble(tokens[3]);   		// Y (mm) in Diptrace export
            double placementRotation = Double.parseDouble(tokens[5]); 	// Rotate in Diptrace export
            String placementLayer = tokens[4];    						// Side in Diptrace export

            Placement placement = new Placement(placementId);
            placement.setLocation(new Location(LengthUnit.Millimeters, placementX, placementY, 0,
                    placementRotation));
            Configuration cfg = Configuration.get();
            if (cfg != null && createMissingParts) {
                String partId = pkgName + "-" + partValue;
                Part part = cfg.getPart(partId);
                if (part == null) {
                    part = new Part(partId);
                    Package pkg = cfg.getPackage(pkgName);
                    if (pkg == null) {
                        pkg = new Package(pkgName);
                        cfg.addPackage(pkg);
                    }
                    part.setPackage(pkg);

                    cfg.addPart(part);
                }
                placement.setPart(part);

            }

            placement.setSide(placementLayer.charAt(0) == 'T' ? Side.Top : Side.Bottom);
            placements.add(placement);
        }
        reader.close();
        return placements;
    }

    class Dlg extends JDialog {
        private JTextField textFieldFileName;
        private JTextField textFieldBottomFile;
        private final Action browseTopFileAction = new SwingAction();
        private final Action importAction = new SwingAction_2();
        private final Action cancelAction = new SwingAction_3();
        private JCheckBox chckbxCreateMissingParts;

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

            JLabel lblTopFilemnt = new JLabel("Export File (.csv)");
            panel.add(lblTopFilemnt, "2, 2, right, default");

            textFieldFileName = new JTextField();
            panel.add(textFieldFileName, "4, 2, fill, default");
            textFieldFileName.setColumns(10);

            JButton btnBrowse = new JButton("Browse");
            btnBrowse.setAction(browseTopFileAction);
            panel.add(btnBrowse, "6, 2");

            JPanel panel_1 = new JPanel();
            panel_1.setBorder(new TitledBorder(null, "Options", TitledBorder.LEADING,
                    TitledBorder.TOP, null, null));
            getContentPane().add(panel_1);
            panel_1.setLayout(new FormLayout(
                    new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                    new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

            chckbxCreateMissingParts = new JCheckBox("Create Missing Parts");
            chckbxCreateMissingParts.setSelected(true);
            panel_1.add(chckbxCreateMissingParts, "2, 2");

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
                        return name.toLowerCase().endsWith(".csv");
                    }
                });
                fileDialog.setVisible(true);
                if (fileDialog.getFile() == null) {
                    return;
                }
                File file = new File(new File(fileDialog.getDirectory()), fileDialog.getFile());
                textFieldFileName.setText(file.getAbsolutePath());
            }
        }

        private class SwingAction_2 extends AbstractAction {
            public SwingAction_2() {
                putValue(NAME, "Import");
                putValue(SHORT_DESCRIPTION, "Import");
            }

            public void actionPerformed(ActionEvent e) {
                fileName = new File(textFieldFileName.getText());
                board = new Board();
                List<Placement> placements = new ArrayList<>();
                try {
                    if (fileName.exists()) {
                        placements.addAll(parseFile(fileName, chckbxCreateMissingParts.isSelected()));
                        
                    }
                }
                catch (Exception e1) {
                    MessageBoxes.errorBox(Dlg.this, "Import Error", "The expected file format is the default file export in DipTrace "
                    		+ "PCB: File -> Export -> Pick and Place. The first line indicates RefDes, Name, X (mm), Y (mm), Side, Rotate, Value."
                    		+ "The lines that follow are data.");
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


