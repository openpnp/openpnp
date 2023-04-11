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

import org.openpnp.Translations;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.Board;
import org.openpnp.model.Abstract2DLocatable.Side;
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
    private JCheckBox chckbxAssignParts;
    public KicadPosImporterDialog(KicadPosImporter kicadPosImporter, Frame parent) {
        super(parent, KicadPosImporter.DESCRIPTION, true);
        importer = kicadPosImporter;
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, Translations.getString("KicadPosImporterDialog.FilesPanel.Border.title"), TitledBorder.LEADING, TitledBorder.TOP, //$NON-NLS-1$
                null, null));
        getContentPane().add(panel);
        panel.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"), //$NON-NLS-1$
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblTopFilemnt = new JLabel(Translations.getString("KicadPosImporterDialog.FilesPanel.topFilemntLabel.text")); //$NON-NLS-1$
        panel.add(lblTopFilemnt, "2, 2, right, default"); //$NON-NLS-1$

        textFieldTopFile = new JTextField();
        panel.add(textFieldTopFile, "4, 2, fill, default"); //$NON-NLS-1$
        textFieldTopFile.setColumns(10);

        JButton btnBrowse = new JButton(Translations.getString("KicadPosImporterDialog.FilesPanel.browseButton.text")); //$NON-NLS-1$
        btnBrowse.setAction(browseTopFileAction);
        panel.add(btnBrowse, "6, 2"); //$NON-NLS-1$

        JLabel lblBottomFilemnb = new JLabel(Translations.getString("KicadPosImporterDialog.FilesPanel.bottomFilemnbLabel.text")); //$NON-NLS-1$
        panel.add(lblBottomFilemnb, "2, 4, right, default"); //$NON-NLS-1$

        textFieldBottomFile = new JTextField();
        panel.add(textFieldBottomFile, "4, 4, fill, default"); //$NON-NLS-1$
        textFieldBottomFile.setColumns(10);

        JButton btnBrowse_1 = new JButton(Translations.getString("KicadPosImporterDialog.FilesPanel.browseButton.text")); //$NON-NLS-1$
        btnBrowse_1.setAction(browseBottomFileAction);
        panel.add(btnBrowse_1, "6, 4"); //$NON-NLS-1$

        JPanel panel_1 = new JPanel();
        panel_1.setBorder(new TitledBorder(null, Translations.getString("KicadPosImporterDialog.OptionsPanel.Border.title"), TitledBorder.LEADING, //$NON-NLS-1$
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

        chckbxAssignParts = new JCheckBox(Translations.getString("KicadPosImporterDialog.OptionsPanel.assignPartsChkbox.text")); //$NON-NLS-1$
        chckbxAssignParts.setSelected(true);
        panel_1.add(chckbxAssignParts, "2, 2"); //$NON-NLS-1$

        chckbxCreateMissingParts = new JCheckBox(Translations.getString("KicadPosImporterDialog.OptionsPanel.createMissingPartsChkbox.text")); //$NON-NLS-1$
        chckbxCreateMissingParts.setSelected(false);
        chckbxCreateMissingParts.setToolTipText("PartId = 'Package'-'Value'"); //$NON-NLS-1$
        panel_1.add(chckbxCreateMissingParts, "2, 4"); //$NON-NLS-1$

        chckbxUseValueOnlyAsPartId = new JCheckBox(Translations.getString("KicadPosImporterDialog.OptionsPanel.useValueOnlyAsPartIdChkbox.text")); //$NON-NLS-1$
        chckbxUseValueOnlyAsPartId.setSelected(false);
        chckbxUseValueOnlyAsPartId.setToolTipText(Translations.getString("KicadPosImporterDialog.OptionsPanel.useValueOnlyAsPartIdChkbox.toolTipText")); //$NON-NLS-1$
        panel_1.add(chckbxUseValueOnlyAsPartId, "2, 6"); //$NON-NLS-1$

        JSeparator separator = new JSeparator();
        getContentPane().add(separator);

        JPanel panel_2 = new JPanel();
        FlowLayout flowLayout = (FlowLayout) panel_2.getLayout();
        flowLayout.setAlignment(FlowLayout.RIGHT);
        getContentPane().add(panel_2);

        JButton btnCancel = new JButton(Translations.getString("KicadPosImporterDialog.ButtonsPanel.cancelButton.text")); //$NON-NLS-1$
        btnCancel.setAction(cancelAction);
        panel_2.add(btnCancel);

        JButton btnImport = new JButton(Translations.getString("KicadPosImporterDialog.ButtonsPanel.importButton.text")); //$NON-NLS-1$
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
            putValue(NAME, Translations.getString("KicadPosImporterDialog.BrowseAction.Name")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("KicadPosImporterDialog.BrowseAction.ShortDescription")); //$NON-NLS-1$
        }

        public void actionPerformed(ActionEvent e) {
            FileDialog fileDialog = new FileDialog(KicadPosImporterDialog.this);
            fileDialog.setFilenameFilter(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".pos"); //$NON-NLS-1$
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
            putValue(NAME, Translations.getString("KicadPosImporterDialog.Browse1Action.Name")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("KicadPosImporterDialog.Browse1Action.ShortDescription")); //$NON-NLS-1$
        }

        public void actionPerformed(ActionEvent e) {
            FileDialog fileDialog = new FileDialog(KicadPosImporterDialog.this);
            fileDialog.setFilenameFilter(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".pos"); //$NON-NLS-1$
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
            putValue(NAME, Translations.getString("KicadPosImporterDialog.ImportAction.Name")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("KicadPosImporterDialog.ImportAction.ShortDescription")); //$NON-NLS-1$
        }

        public void actionPerformed(ActionEvent e) {
            KicadPosImporterDialog.this.importer.topFile = new File(textFieldTopFile.getText());
            KicadPosImporterDialog.this.importer.bottomFile = new File(textFieldBottomFile.getText());
            KicadPosImporterDialog.this.importer.board = new Board();
            List<Placement> placements = new ArrayList<>();
            try {
                if (KicadPosImporterDialog.this.importer.topFile.exists()) {
                    placements.addAll(KicadPosImporter.parseFile(KicadPosImporterDialog.this.importer.topFile, Side.Top,
                            chckbxAssignParts.isSelected(),
                            chckbxCreateMissingParts.isSelected(), 
                            chckbxUseValueOnlyAsPartId.isSelected()));
                }
                if (KicadPosImporterDialog.this.importer.bottomFile.exists()) {
                    placements.addAll(KicadPosImporter.parseFile(KicadPosImporterDialog.this.importer.bottomFile, Side.Bottom,
                            chckbxAssignParts.isSelected(),
                            chckbxCreateMissingParts.isSelected(), 
                            chckbxUseValueOnlyAsPartId.isSelected()));
                }
            }
            catch (Exception e1) {
                MessageBoxes.errorBox(KicadPosImporterDialog.this, Translations.getString("KicadPosImporterDialog.ImportErrorMessage"), e1); //$NON-NLS-1$
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
            putValue(NAME, Translations.getString("KicadPosImporterDialog.CancelAction.Name")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("KicadPosImporterDialog.CancelAction.ShortDescription")); //$NON-NLS-1$
        }

        public void actionPerformed(ActionEvent e) {
            setVisible(false);
        }
    }
}