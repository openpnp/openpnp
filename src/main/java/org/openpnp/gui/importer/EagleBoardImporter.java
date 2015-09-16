/*
    Copyright (C) 2015 Douglas Pearless <Douglas.Pearless@gmail.com>
    
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

import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FilenameFilter;
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
import org.openpnp.model.eagle.*;
import org.openpnp.model.eagle.xml.Element;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class EagleBoardImporter implements BoardImporter {
    private final static String NAME = "CadSoft EAGLE Board";
    private final static String DESCRIPTION = "Import files directly from EAGLE's <filename>.brd file.";
    
	private Board board;
	private File boardFile;
	
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
    
	private static List<Placement> parseFile(File file, Side side, boolean createMissingParts) throws Exception {
		ArrayList<Placement> placements = new ArrayList<Placement>();
		//we don't use the 'side' parameter as we cab read this from the .brd file
		//in the future we could use the side parameter to restrict this from only parsing one side or the other or both
		
		EagleLoader boardToProcess = new EagleLoader(file);
		if (boardToProcess.board != null ) {
			if( ! boardToProcess.board.getElements().getElement().isEmpty()){
				// Process each of the element items
				for (Element element : boardToProcess.board.getElements().getElement() ) {
					//first we determine if the part is on the top layer or bottom layer
					
					Side element_side;
					String rot = element.getRot();
					if (rot.toUpperCase().startsWith("M"))
						//The part is mirrored and therefore is on the bottom of the board
						element_side = Side.Bottom;
					else
						element_side = Side.Top;
					
					//Now determine if we want to process this part based on which sideof th eboard it is on
					
					if (side != null) { //null means process both sides
						if (side != element_side) continue; //exit this loop and process the next element
					}
					
					String rot_number = rot.replaceAll("[A-Za-z ]", ""); //remove all letters, i.e. R180 becomes 180

					Placement placement = new Placement(element.getName());
					double rotation = Double.parseDouble(rot_number);
					double x = Double.parseDouble(element.getX());
					double y = Double.parseDouble(element.getY());
					placement.setLocation(new Location(
					        LengthUnit.Millimeters,
					        x,
					        y,
					        0,
					        rotation));
					Configuration cfg = Configuration.get();
		            if (cfg != null && createMissingParts) {
		                String value = element.getValue(); // Value
		                String packageId = element.getPackage(); //Package

		                String partId = packageId;
		                if (value.trim().length() > 0) {
		                    partId += "-" + value;
		                }
		                Part part = cfg.getPart(partId);
		                if (part == null) {
		                    part = new Part(partId);
		                    Package pkg = cfg.getPackage(packageId);
		                    if (pkg == null) {
		                        pkg = new Package(packageId);
		                        cfg.addPackage(pkg);
		                    }
		                    part.setPackage(pkg);

		                    cfg.addPart(part);
		                }
		                placement.setPart(part);

		            }

					placement.setSide(element_side);
					placements.add(placement);
				}
			}
		}
		if (boardToProcess.library != null ) {
			
		}
		if (boardToProcess.schematic != null ) {
			
		}

		return placements;
	}
	
    class Dlg extends JDialog {
        private JTextField textFieldBoardFile;
        private final Action browseBoardFileAction = new SwingAction();
        private final Action importAction = new SwingAction_2();
        private final Action cancelAction = new SwingAction_3();
        private JCheckBox chckbxCreateMissingParts;
        private JCheckBox chckbxImportTop;
        private JCheckBox chckbxImportBottom;
        
        public Dlg(Frame parent) {
            super(parent, DESCRIPTION, true);
            getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
            
            JPanel panel = new JPanel();
            panel.setBorder(new TitledBorder(null, "Files", TitledBorder.LEADING, TitledBorder.TOP, null, null));
            getContentPane().add(panel);
            panel.setLayout(new FormLayout(new ColumnSpec[] {
                    FormFactory.RELATED_GAP_COLSPEC,
                    FormFactory.DEFAULT_COLSPEC,
                    FormFactory.RELATED_GAP_COLSPEC,
                    ColumnSpec.decode("default:grow"),
                    FormFactory.RELATED_GAP_COLSPEC,
                    FormFactory.DEFAULT_COLSPEC,},
                new RowSpec[] {
                    FormFactory.RELATED_GAP_ROWSPEC,
                    FormFactory.DEFAULT_ROWSPEC,
                    FormFactory.RELATED_GAP_ROWSPEC,
                    FormFactory.DEFAULT_ROWSPEC,}));
            
            JLabel lblBoardFilebrd = new JLabel("Eagle PCB Board File (.brd)");
            panel.add(lblBoardFilebrd, "2, 2, right, default");
            
            textFieldBoardFile = new JTextField();
            panel.add(textFieldBoardFile, "4, 2, fill, default");
            textFieldBoardFile.setColumns(10);
            
            JButton btnBrowse = new JButton("Browse");
            btnBrowse.setAction(browseBoardFileAction);
            panel.add(btnBrowse, "6, 2");
                       
            JPanel panel_1 = new JPanel();
            panel_1.setBorder(new TitledBorder(null, "Options", TitledBorder.LEADING, TitledBorder.TOP, null, null));
            getContentPane().add(panel_1);
            panel_1.setLayout(new FormLayout(new ColumnSpec[] {
                    FormFactory.RELATED_GAP_COLSPEC,
                    FormFactory.DEFAULT_COLSPEC,},
                new RowSpec[] {
                	FormFactory.RELATED_GAP_ROWSPEC,
                    FormFactory.DEFAULT_ROWSPEC,
                	FormFactory.RELATED_GAP_ROWSPEC,
                    FormFactory.DEFAULT_ROWSPEC,
                    FormFactory.RELATED_GAP_ROWSPEC,
                    FormFactory.DEFAULT_ROWSPEC,}));
            
            chckbxCreateMissingParts = new JCheckBox("Create Missing Parts");
            chckbxCreateMissingParts.setSelected(true);
            panel_1.add(chckbxCreateMissingParts, "2, 2");
            
            chckbxImportTop =    new JCheckBox("Import Parts on the Top of the board");
            chckbxImportTop.setSelected(true);
            panel_1.add(chckbxImportTop, "2, 4");
            
            chckbxImportBottom = new JCheckBox("Import Parts on the Bottom of the board");
            chckbxImportBottom.setSelected(true);
            panel_1.add(chckbxImportBottom, "2, 6");
            
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
            InputMap inputMap = rootPane
                    .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
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
                        return name.toLowerCase().endsWith(".brd");
                    }
                });
                fileDialog.setVisible(true);
                if (fileDialog.getFile() == null) {
                    return;
                }
                File file = new File(new File(fileDialog.getDirectory()), fileDialog.getFile());
                textFieldBoardFile.setText(file.getAbsolutePath());
            }
        }

        private class SwingAction_2 extends AbstractAction {
            public SwingAction_2() {
                putValue(NAME, "Import");
                putValue(SHORT_DESCRIPTION, "Import");
            }
            public void actionPerformed(ActionEvent e) {
                boardFile = new File(textFieldBoardFile.getText());
                board = new Board();
                List<Placement> placements = new ArrayList<Placement>();
                try {
                    if (boardFile.exists()) {
                    	if (chckbxImportTop.isSelected() && chckbxImportBottom.isSelected())
                    		placements.addAll(parseFile(boardFile, null, chckbxCreateMissingParts.isSelected())); //both Top and Bottom of the board
                    	else if(chckbxImportTop.isSelected())
                    		placements.addAll(parseFile(boardFile, Side.Top, chckbxCreateMissingParts.isSelected())); //Just the Top side of the board
                    	else if(chckbxImportBottom.isSelected())
                    		placements.addAll(parseFile(boardFile, Side.Bottom, chckbxCreateMissingParts.isSelected())); //Just the Bottom side of the board                    	
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
