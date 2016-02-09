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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class KicadPosImporter implements BoardImporter {
    private final static String NAME = "KiCAD .pos";
    private final static String DESCRIPTION = "Import KiCAD .pos Files.";

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
	
	private static List<Placement> parseFile(File file, Side side, boolean createMissingParts) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		ArrayList<Placement> placements = new ArrayList<>();
		String line;

//		See: http://bazaar.launchpad.net/~kicad-product-committers/kicad/product/view/head:/pcbnew/exporters/gen_modules_placefile.cpp
//		### Module positions - created on Tue 25 Mar 2014 03:42:43 PM PDT ###
//		### Printed by Pcbnew version pcbnew (2014-01-24 BZR 4632)-product
//		## Unit = mm, Angle = deg.
//		## Side : F.Cu
//		# Ref    Val                  Package         PosX       PosY        Rot     Side
//		C1       100u             Capacitors_SMD:c  128.9050   -52.0700       0.0    F.Cu
//		C2       100u             Capacitors_SMD:c   93.3450   -77.4700     180.0    F.Cu
//		C3       100u             Capacitors_SMD:c   67.9450   -77.4700     180.0    F.Cu
		
		Pattern pattern = Pattern.compile("(\\S+)\\s+(.*?)\\s+(.*?)\\s+(-?\\d+\\.\\d+)\\s+(-?\\d+\\.\\d+)\\s+(-?\\d+\\.\\d+)\\s(.*?)");
		
		while ((line = reader.readLine()) != null) {
			line = line.trim();
			if (line.length() == 0 || line.charAt(0) == '#') {
				continue;
			}



			Matcher matcher = pattern.matcher(line);
			matcher.matches();
			
			String placementId = matcher.group(1);
			String partValue = matcher.group(2);
			String pkgName = matcher.group(3);
			double placementX = Double.parseDouble(matcher.group(4));
			double placementY = Double.parseDouble(matcher.group(5));
			double placementRotation = Double.parseDouble(matcher.group(6));
			String placementLayer = matcher.group(7);
			
			Placement placement = new Placement(placementId);
			placement.setLocation(new Location(
			        LengthUnit.Millimeters,
			        placementX,
			        placementY,
			        0,
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

			placement.setSide(side);
			placements.add(placement);
		}
		reader.close();
		return placements;
	}
	
	class Dlg extends JDialog {
	    private JTextField textFieldTopFile;
	    private JTextField textFieldBottomFile;
	    private final Action browseTopFileAction = new SwingAction();
	    private final Action browseBottomFileAction = new SwingAction_1();
	    private final Action importAction = new SwingAction_2();
	    private final Action cancelAction = new SwingAction_3();
	    private JCheckBox chckbxCreateMissingParts;

	    public Dlg(Frame parent) {
	        super(parent, DESCRIPTION, true);
	        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
	        
	        JPanel panel = new JPanel();
	        panel.setBorder(new TitledBorder(null, "Files", TitledBorder.LEADING, TitledBorder.TOP, null, null));
	        getContentPane().add(panel);
	        panel.setLayout(new FormLayout(new ColumnSpec[] {
	                FormSpecs.RELATED_GAP_COLSPEC,
	                FormSpecs.DEFAULT_COLSPEC,
	                FormSpecs.RELATED_GAP_COLSPEC,
	                ColumnSpec.decode("default:grow"),
	                FormSpecs.RELATED_GAP_COLSPEC,
	                FormSpecs.DEFAULT_COLSPEC,},
	            new RowSpec[] {
	                FormSpecs.RELATED_GAP_ROWSPEC,
	                FormSpecs.DEFAULT_ROWSPEC,
	                FormSpecs.RELATED_GAP_ROWSPEC,
	                FormSpecs.DEFAULT_ROWSPEC,}));
	        
	        JLabel lblTopFilemnt = new JLabel("Top File (.pos)");
	        panel.add(lblTopFilemnt, "2, 2, right, default");
	        
	        textFieldTopFile = new JTextField();
	        panel.add(textFieldTopFile, "4, 2, fill, default");
	        textFieldTopFile.setColumns(10);
	        
	        JButton btnBrowse = new JButton("Browse");
	        btnBrowse.setAction(browseTopFileAction);
	        panel.add(btnBrowse, "6, 2");
	        
	        JLabel lblBottomFilemnb = new JLabel("Bottom File (.pos)");
	        panel.add(lblBottomFilemnb, "2, 4, right, default");
	        
	        textFieldBottomFile = new JTextField();
	        panel.add(textFieldBottomFile, "4, 4, fill, default");
	        textFieldBottomFile.setColumns(10);
	        
	        JButton btnBrowse_1 = new JButton("Browse");
	        btnBrowse_1.setAction(browseBottomFileAction);
	        panel.add(btnBrowse_1, "6, 4");
	        
	        JPanel panel_1 = new JPanel();
	        panel_1.setBorder(new TitledBorder(null, "Options", TitledBorder.LEADING, TitledBorder.TOP, null, null));
	        getContentPane().add(panel_1);
	        panel_1.setLayout(new FormLayout(new ColumnSpec[] {
	                FormSpecs.RELATED_GAP_COLSPEC,
	                FormSpecs.DEFAULT_COLSPEC,},
	            new RowSpec[] {
	                FormSpecs.RELATED_GAP_ROWSPEC,
	                FormSpecs.DEFAULT_ROWSPEC,}));
	        
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
	                    return name.toLowerCase().endsWith(".pos");
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
	    
	    private class SwingAction_1 extends AbstractAction {
	        public SwingAction_1() {
	            putValue(NAME, "Browse");
	            putValue(SHORT_DESCRIPTION, "Browse");
	        }
	        public void actionPerformed(ActionEvent e) {
	            FileDialog fileDialog = new FileDialog(Dlg.this);
	            fileDialog.setFilenameFilter(new FilenameFilter() {
	                @Override
	                public boolean accept(File dir, String name) {
	                    return name.toLowerCase().endsWith(".pos");
	                }
	            });
	            fileDialog.setVisible(true);
	            if (fileDialog.getFile() == null) {
	                return;
	            }
	            File file = new File(new File(fileDialog.getDirectory()), fileDialog.getFile());
	            textFieldBottomFile.setText(file.getAbsolutePath());
	        }
	    }

	    private class SwingAction_2 extends AbstractAction {
	        public SwingAction_2() {
	            putValue(NAME, "Import");
	            putValue(SHORT_DESCRIPTION, "Import");
	        }
	        public void actionPerformed(ActionEvent e) {
	            topFile = new File(textFieldTopFile.getText());
	            bottomFile = new File(textFieldBottomFile.getText());
	            board = new Board();
	            List<Placement> placements = new ArrayList<>();
	            try {
	                if (topFile.exists()) {
	                    placements.addAll(parseFile(topFile, Side.Top, chckbxCreateMissingParts.isSelected()));
	                }
	                if (bottomFile.exists()) {
	                    placements.addAll(parseFile(bottomFile, Side.Bottom, chckbxCreateMissingParts.isSelected()));
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
