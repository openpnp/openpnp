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

import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
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

import java.awt.FileDialog;
import java.io.FilenameFilter;

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
import org.pmw.tinylog.Logger;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class LabcenterProteusImporter implements BoardImporter {
    private final static String NAME = "Labcenter Proteus .pkp";
    private final static String DESCRIPTION = "Import Labcenter Proteus (.pkp) Pick amd Place Files.";
    
    static File lastSelectedDirectory = null;

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

    private static List<Placement> parseFile(File file, boolean createMissingParts, boolean stockCodesIncluded)
            throws Exception {
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        ArrayList<Placement> placements = new ArrayList<>();
        String line;
		double mul = 1.0;
        int lineCount = 0;
		int [] ind = {0,1,2,3,4,5,6};
		if (stockCodesIncluded) {
			
			 ind = new int[]{0,1,2,4,5,6,7};
		}
		// state of the check boxes in log file
		Logger.trace("include stock codes " + stockCodesIncluded);
		Logger.trace("create Missing Parts " + createMissingParts);
        // 
        // Default format for Proteus pkp file 
        // Part ID, Value, Package,[Stock Code,] Layer, Rotation, X, Y
        // "R1","10k","0402",[Stock Code,]TOP,270,15.1678,15.24
		// "R18","10k","0402",[Stock Code,]TOP,270,16.7172,15.24
		// "R11","22","0402",[Stock Code,]TOP,-180,2.6416,11.5062
        // <etc>
		// [Stock Code] is optional, chosen when the file is exported from Proteus. 
        
		while ((line = reader.readLine()) != null) {
        	
        	// Skip blank lines
        	if (line.length() == 0)  {
                Logger.trace("Blank line must skip this"); // helpful to know what's happening during parse
				continue;
            }
			// check for units in thou. default is mm
			if(line.matches("^.*?\\bUnits\\b.*?\\bthou\\b.*?$"))
				{
					Logger.trace("units are inches");
					mul = .0254;
					continue;
				}
			// Skip line if it does not start with "
			if (line.charAt(0) != '"') {
				Logger.trace("skipping : " + line); // helpful to see the line being skipped
				continue;
			}
			
			// Looks like we have a valid line of data, parse it now
            line = line.trim();
            String[] tokens = line.split(",");
            
            String placementId = tokens[ind[0]].replaceAll("^\"|\"$", "");	// RefDes in Proteus pkp file
            String partValue = tokens[ind[1]].replaceAll("^\"|\"$", "");    // Value in Proteus pkp file
            String pkgName = tokens[ind[2]].replaceAll("^\"|\"$", "");      // Name in Proteus pkp file
            double placementX = Double.parseDouble(tokens[ind[5]])*mul;   		// X (mm) in Proteus pkp file
            double placementY = Double.parseDouble(tokens[ind[6]])*mul;   		// Y (mm) in Proteus pkp file
            double placementRotation = Double.parseDouble(tokens[ind[4]]); 	// Rotate in Proteus pkp file
            String placementLayer = tokens[ind[3]];    						// Layer in Proteus pkp file
			
			
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
		private JCheckBox chckbxStockCodesIncluded;
		
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

            JLabel lblTopFilemnt = new JLabel("Import File (.pkp)");
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
                    new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
					RowSpec.decode("default:grow")}));

            chckbxCreateMissingParts = new JCheckBox("Create Missing Parts");
            chckbxCreateMissingParts.setSelected(true);
            panel_1.add(chckbxCreateMissingParts, "2, 2");
			
			chckbxStockCodesIncluded = new JCheckBox("Stock Codes Included");
            chckbxStockCodesIncluded.setSelected(false);
            panel_1.add(chckbxStockCodesIncluded, "2, 3");

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
                        return name.toLowerCase().endsWith(".pkp");
                    }
                });
                fileDialog.setFile("*.pkp");
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
                        placements.addAll(parseFile(fileName, chckbxCreateMissingParts.isSelected(), chckbxStockCodesIncluded.isSelected()));
                        
                    }
                }
                catch (Exception e1) {
                    MessageBoxes.errorBox(Dlg.this, "Import Error", "The expected file format is the default file export in Labcenter Proteus "
                    		+ "Data after header information should be :\n"
							+ "Part ID, Value, Package,[Stock Code,] Layer, Rotation, X, Y\n"
                    		+ "Likely cause: the number of data fields does not match expected input\n"
							+ "ie: Include stock codes check box is not checked but file has stock codes");
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


