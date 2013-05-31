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

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class MountsmdPosImporter extends JDialog implements BoardImporter {
	private Board board;
	private File topFile, bottomFile;
	private JTextField textFieldTopFile;
	private JTextField textFieldBottomFile;
	private final Action browseTopFileAction = new SwingAction();
	private final Action browseBottomFileAction = new SwingAction_1();
	private final Action importAction = new SwingAction_2();
	private final Action cancelAction = new SwingAction_3();
	private JCheckBox chckbxCreateMissingParts;
	
	public MountsmdPosImporter(Frame parent) {
		super(parent, "Import KiCAD .pos Files", true);
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
		        FormFactory.RELATED_GAP_COLSPEC,
		        FormFactory.DEFAULT_COLSPEC,},
		    new RowSpec[] {
		        FormFactory.RELATED_GAP_ROWSPEC,
		        FormFactory.DEFAULT_ROWSPEC,}));
		
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
	
	@Override
	public Board importBoard() throws Exception {
		setVisible(true);
		return board;
	}
	
	private static List<Placement> parseFile(File file, Side side, boolean createMissingParts) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		ArrayList<Placement> placements = new ArrayList<Placement>();
		String line;
		while ((line = reader.readLine()) != null) {
			line = line.trim();
			if (line.length() == 0 || line.charAt(0) == '#') {
				continue;
			}

			// C1 41.91 34.93 180 0.1uF C0805
			// T10 21.59 14.22  90  SOT23-BEC
			// printf("%s %5.2f %5.2f %3.0f %s %s\n",

			//Pattern pattern = Pattern.compile("(\\S+)\\s+(\\d+\\.\\d+)\\s+(\\d+\\.\\d+)\\s+(\\d{1,3})\\s(.*?)\\s(.*)");
			Pattern pattern = Pattern.compile("(\\S+)\\s+(.*?)\\s(\\d+\\.\\d+)\\s+(\\d+\\.\\d+)\\s+(\\d+\\.\\d+)\\s(.*)");

			Matcher matcher = pattern.matcher(line);
			matcher.matches();
			Placement placement = new Placement(matcher.group(1));
			placement.setLocation(new Location(
			        LengthUnit.Millimeters,
			        Double.parseDouble(matcher.group(3)),
			        Double.parseDouble(matcher.group(4)),
			        0,
			        Double.parseDouble(matcher.group(5))));
			Configuration cfg = Configuration.get();
            if (cfg != null && createMissingParts) {
                //String packageId = matcher.group(6);
                //String partId = packageId + "-" + matcher.group(2);
		String packageId = "";
                String partId = matcher.group(2);
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

			placement.setSide(side);
			placements.add(placement);
		}
		return placements;
	}
	private class SwingAction extends AbstractAction {
		public SwingAction() {
			putValue(NAME, "Browse");
			putValue(SHORT_DESCRIPTION, "Browse");
		}
		public void actionPerformed(ActionEvent e) {
			FileDialog fileDialog = new FileDialog(MountsmdPosImporter.this);
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
			FileDialog fileDialog = new FileDialog(MountsmdPosImporter.this);
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
			List<Placement> placements = new ArrayList<Placement>();
			try {
				placements.addAll(parseFile(topFile, Side.Top, chckbxCreateMissingParts.isSelected()));
				placements.addAll(parseFile(bottomFile, Side.Bottom, chckbxCreateMissingParts.isSelected()));
			}
			catch (Exception e1) {
				MessageBoxes.errorBox(MountsmdPosImporter.this, "Import Error", e1);
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
