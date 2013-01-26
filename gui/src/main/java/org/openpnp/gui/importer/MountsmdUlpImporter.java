
// Change log:
// 03/10/2012 Ami: Add part and package

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
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Placement;
import org.openpnp.model.Configuration;
import org.openpnp.model.Part;
import org.openpnp.model.Package;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

//C1 41.91 34.93 180 0.1uF C0805
//T10 21.59 14.22  90  SOT23-BEC

// printf("%s %5.2f %5.2f %3.0f %s %s\n",

@SuppressWarnings("serial")
public class MountsmdUlpImporter extends JDialog implements BoardImporter {
	private Board board;
	private File topFile, bottomFile;
	private JTextField textFieldTopFile;
	private JTextField textFieldBottomFile;
	private final Action browseTopFileAction = new SwingAction();
	private final Action browseBottomFileAction = new SwingAction_1();
	private final Action importAction = new SwingAction_2();
	private final Action cancelAction = new SwingAction_3();
	
	public MountsmdUlpImporter(Frame parent) {
		super(parent, "Import EAGLE mountsmd.ulp Files", true);
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
		
		JLabel lblTopFilemnt = new JLabel("Top File (.mnt)");
		panel.add(lblTopFilemnt, "2, 2, right, default");
		
		textFieldTopFile = new JTextField();
		panel.add(textFieldTopFile, "4, 2, fill, default");
		textFieldTopFile.setColumns(10);
		
		JButton btnBrowse = new JButton("Browse");
		btnBrowse.setAction(browseTopFileAction);
		panel.add(btnBrowse, "6, 2");
		
		JLabel lblBottomFilemnb = new JLabel("Bottom File (.mnb)");
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
	
	private static List<Placement> parseFile(File file, Side side) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		ArrayList<Placement> placements = new ArrayList<Placement>();
		String line;
		while ((line = reader.readLine()) != null) {
			line = line.trim();
			if (line.length() == 0) {
				continue;
			}

			Pattern pattern = Pattern.compile("(\\S+)\\s+(\\d+\\.\\d+)\\s+(\\d+\\.\\d+)\\s+(\\d{1,3})\\s(.*?)\\s(.*)");
			Matcher matcher = pattern.matcher(line);
			matcher.matches();
			Placement placement = new Placement(matcher.group(1));
			placement.getLocation().setUnits(LengthUnit.Millimeters);
			placement.getLocation().setX(Double.parseDouble(matcher.group(2)));
			placement.getLocation().setY(Double.parseDouble(matcher.group(3)));
			placement.getLocation().setRotation(Double.parseDouble(matcher.group(4)));
			Configuration cfg = Configuration.get();
			if(cfg != null)
			{
			    String packageId = matcher.group(6);


			    String partId = packageId +"-"+matcher.group(5);
			    Part part = cfg.getPart(partId);
			    if(part == null)
			    {
				part = new Part(partId);
				Package pkg = cfg.getPackage(packageId);
				if(pkg == null)
				{
				    pkg = new Package(packageId);
				    cfg.addPackage(pkg);
				}
				part.setPackage(pkg);

				cfg.addPart(part);
			    }
			    placement.setPart(part);

			}
			// Ami. end



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
			FileDialog fileDialog = new FileDialog(MountsmdUlpImporter.this);
			fileDialog.setFilenameFilter(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.toLowerCase().endsWith(".mnt");
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
			FileDialog fileDialog = new FileDialog(MountsmdUlpImporter.this);
			fileDialog.setFilenameFilter(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.toLowerCase().endsWith(".mnb");
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
				placements.addAll(parseFile(topFile, Side.Top));
				placements.addAll(parseFile(bottomFile, Side.Bottom));
			}
			catch (Exception e1) {
				MessageBoxes.errorBox(MountsmdUlpImporter.this, "Import Error", e1);
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
