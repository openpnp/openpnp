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

class KicadPosImporterDialog extends JDialog {
    /**
     * 
     */
    private final KicadPosImporter importer;
    private JTextField textFieldTopFile;
    private JTextField textFieldBottomFile;
    private final Action browseTopFileAction = new SwingAction();
    private final Action browseBottomFileAction = new SwingAction_1();
    private final Action importAction = new SwingAction_2();
    private final Action cancelAction = new SwingAction_3();
    private JCheckBox chckbxCreateMissingParts;
    private JCheckBox chckbxUseValueOnlyAsPartId;

    public KicadPosImporterDialog(KicadPosImporter kicadPosImporter, Frame parent) {
        super(parent, KicadPosImporter.DESCRIPTION, true);
        importer = kicadPosImporter;
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
                FormSpecs.DEFAULT_ROWSPEC,}));
                
                        chckbxCreateMissingParts = new JCheckBox("Create Missing Parts");
                        chckbxCreateMissingParts.setSelected(false);
                        chckbxCreateMissingParts.setToolTipText("PartId = 'Package'-'Value'");
                        panel_1.add(chckbxCreateMissingParts, "2, 2");
        
                chckbxUseValueOnlyAsPartId = new JCheckBox("Use only Value as PartId");
                chckbxUseValueOnlyAsPartId.setSelected(false);
                chckbxUseValueOnlyAsPartId.setToolTipText("Check this, if Value is unique (e.g. company internal part number)");
                panel_1.add(chckbxUseValueOnlyAsPartId, "2, 4");

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
            FileDialog fileDialog = new FileDialog(KicadPosImporterDialog.this);
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
            FileDialog fileDialog = new FileDialog(KicadPosImporterDialog.this);
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
            KicadPosImporterDialog.this.importer.topFile = new File(textFieldTopFile.getText());
            KicadPosImporterDialog.this.importer.bottomFile = new File(textFieldBottomFile.getText());
            KicadPosImporterDialog.this.importer.board = new Board();
            List<Placement> placements = new ArrayList<>();
            try {
                if (KicadPosImporterDialog.this.importer.topFile.exists()) {
                    placements.addAll(KicadPosImporter.parseFile(KicadPosImporterDialog.this.importer.topFile, Side.Top,
                            chckbxCreateMissingParts.isSelected(), 
                            chckbxUseValueOnlyAsPartId.isSelected()));
                }
                if (KicadPosImporterDialog.this.importer.bottomFile.exists()) {
                    placements.addAll(KicadPosImporter.parseFile(KicadPosImporterDialog.this.importer.bottomFile, Side.Bottom,
                            chckbxCreateMissingParts.isSelected(), 
                            chckbxUseValueOnlyAsPartId.isSelected()));
                }
            }
            catch (Exception e1) {
                MessageBoxes.errorBox(KicadPosImporterDialog.this, "Import Error", e1);
                return;
            }
            for (Placement placement : placements) {
                KicadPosImporterDialog.this.importer.board.addPlacement(placement);
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