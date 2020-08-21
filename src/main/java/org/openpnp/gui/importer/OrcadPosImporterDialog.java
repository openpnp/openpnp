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
import org.openpnp.model.Placement;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

class OrcadPosImporterDialog extends JDialog {
    /**
     * 
     */
    private final OrcadPosImporter importer;
    private JTextField textFieldTopFile;
    private JTextField textFieldBottomFile;
    private final Action browseFileAction = new SwingAction();
    private final Action importAction = new SwingAction_2();
    private final Action cancelAction = new SwingAction_3();
    private JCheckBox chckbxCreateMissingParts;
    private JCheckBox chckbxUseValueOnlyAsPartId;
    private JCheckBox chckbxAssignParts;
    public OrcadPosImporterDialog(OrcadPosImporter OrcadPosImporter, Frame parent) {
        super(parent, OrcadPosImporter.DESCRIPTION, true);
        importer = OrcadPosImporter;
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

        JLabel lblTopFilemnt = new JLabel("PnP File (.txt)");
        panel.add(lblTopFilemnt, "2, 2, right, default");

        textFieldTopFile = new JTextField();
        panel.add(textFieldTopFile, "4, 2, fill, default");
        textFieldTopFile.setColumns(10);

        JButton btnBrowse = new JButton("Browse");
        btnBrowse.setAction(browseFileAction);
        panel.add(btnBrowse, "6, 2");

        JPanel panel_1 = new JPanel();
        panel_1.setBorder(new TitledBorder(null, "Options", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        getContentPane().add(panel_1);
        panel_1.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,}));

        chckbxAssignParts = new JCheckBox("Assign Parts");
        chckbxAssignParts.setSelected(true);
        panel_1.add(chckbxAssignParts, "2, 2");

        chckbxCreateMissingParts = new JCheckBox("Create Missing Parts");
        chckbxCreateMissingParts.setSelected(true);
        chckbxCreateMissingParts.setToolTipText("PartId = 'Package'-'Value'");
        panel_1.add(chckbxCreateMissingParts, "2, 4");

        chckbxUseValueOnlyAsPartId = new JCheckBox("Use only Value as PartId");
        chckbxUseValueOnlyAsPartId.setSelected(false);
        chckbxUseValueOnlyAsPartId.setToolTipText("Check this, if Value is unique (e.g. company internal part number)");
        panel_1.add(chckbxUseValueOnlyAsPartId, "2, 6");

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
            FileDialog fileDialog = new FileDialog(OrcadPosImporterDialog.this);
            fileDialog.setFilenameFilter(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".txt");
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
            OrcadPosImporterDialog.this.importer.topFile = new File(textFieldTopFile.getText());
            OrcadPosImporterDialog.this.importer.board = new Board();
            List<Placement> placements = new ArrayList<>();
            try {
                if (OrcadPosImporterDialog.this.importer.topFile.exists()) {
                    placements.addAll(OrcadPosImporter.parseFile(OrcadPosImporterDialog.this.importer.topFile, Side.Top,
                            chckbxAssignParts.isSelected(),
                            chckbxCreateMissingParts.isSelected(), 
                            chckbxUseValueOnlyAsPartId.isSelected()));
                }
            }

            catch (Exception e1) {
                MessageBoxes.errorBox(OrcadPosImporterDialog.this, "Import Error", e1);
                return;
            }
            for (Placement placement : placements) {
                OrcadPosImporterDialog.this.importer.board.addPlacement(placement);
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