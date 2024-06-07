package org.openpnp.gui.importer.genericcsv.gui;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import org.openpnp.Translations;
import org.openpnp.gui.importer.genericcsv.csv.GenericCSVParser;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.Board;
import org.openpnp.model.Placement;
import org.pmw.tinylog.Logger;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

public class GenericCSVImporterDialog extends JDialog {
    private final GenericCSVParser parser;

    private JTextField textFieldFile;
    private final Action browseFileAction = new SwingAction();
    private final Action importAction = new SwingAction_2();
    private final Action cancelAction = new SwingAction_3();
    private JCheckBox chckbxCreateMissingParts;
    private JCheckBox chckbxUpdatePartHeight;
    private Board board;

    public GenericCSVImporterDialog(Frame parent, String title, GenericCSVParser parser) {
        super(parent, title, true);

        this.parser = parser;
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, Translations.getString("CsvImporter.FilesPanel.Border.title"), TitledBorder.LEADING, TitledBorder.TOP, //$NON-NLS-1$
                null, null));
        getContentPane().add(panel);
        panel.setLayout(new FormLayout(
                new ColumnSpec[]{FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"), //$NON-NLS-1$
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[]{FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblTopFilemnt = new JLabel(Translations.getString("CsvImporter.FilesPanel.topFilemntLabel.text")); //$NON-NLS-1$
        panel.add(lblTopFilemnt, "2, 2, right, default"); //$NON-NLS-1$

        textFieldFile = new JTextField();
        panel.add(textFieldFile, "4, 2, fill, default"); //$NON-NLS-1$
        textFieldFile.setColumns(30);

        JButton btnBrowse = new JButton(Translations.getString("CsvImporter.FilesPanel.browseButton.text")); //$NON-NLS-1$
        btnBrowse.setAction(browseFileAction);
        panel.add(btnBrowse, "6, 2"); //$NON-NLS-1$

        JPanel panel_1 = new JPanel();
        panel_1.setBorder(new TitledBorder(null, Translations.getString("CsvImporter.OptionsPanel.Border.title"), TitledBorder.LEADING, //$NON-NLS-1$
                TitledBorder.TOP, null, null));
        getContentPane().add(panel_1);
        panel_1.setLayout(new FormLayout(
                new ColumnSpec[]{FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[]{FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        RowSpec.decode("default:grow")})); //$NON-NLS-1$

        chckbxCreateMissingParts = new JCheckBox(Translations.getString("CsvImporter.OptionsPanel.createMissingPartsChkbox.text")); //$NON-NLS-1$
        chckbxCreateMissingParts.setSelected(true);
        panel_1.add(chckbxCreateMissingParts, "2, 2"); //$NON-NLS-1$

        chckbxUpdatePartHeight = new JCheckBox(Translations.getString("CsvImporter.OptionsPanel.updatePartHeightChkbox.text")); //$NON-NLS-1$
        chckbxUpdatePartHeight.setSelected(true);
        panel_1.add(chckbxUpdatePartHeight, "2, 3"); //$NON-NLS-1$

        JSeparator separator = new JSeparator();
        getContentPane().add(separator);

        JPanel panel_2 = new JPanel();
        FlowLayout flowLayout = (FlowLayout) panel_2.getLayout();
        flowLayout.setAlignment(FlowLayout.RIGHT);
        getContentPane().add(panel_2);

        JButton btnCancel = new JButton(Translations.getString("CsvImporter.ButtonsPanel.cancelButton.text")); //$NON-NLS-1$
        btnCancel.setAction(cancelAction);
        panel_2.add(btnCancel);

        JButton btnImport = new JButton(Translations.getString("CsvImporter.ButtonsPanel.importButton.text")); //$NON-NLS-1$
        btnImport.setAction(importAction);
        panel_2.add(btnImport);

        // resize to the window to its preferred size
        pack();
        setLocationRelativeTo(parent);

        JRootPane rootPane = getRootPane();
        KeyStroke stroke = KeyStroke.getKeyStroke("ESCAPE"); //$NON-NLS-1$
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(stroke, "ESCAPE"); //$NON-NLS-1$
        rootPane.getActionMap().put("ESCAPE", cancelAction); //$NON-NLS-1$
    }

    public Board getBoard() {
        return board;
    }

    private class SwingAction extends AbstractAction {
        public SwingAction() {
            putValue(NAME, Translations.getString("CsvImporter.BrowseAction.Name")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("CsvImporter.BrowseAction.ShortDescription")); //$NON-NLS-1$
        }

        public void actionPerformed(ActionEvent e) {
            FileDialog fileDialog = new FileDialog(GenericCSVImporterDialog.this);
            fileDialog.setFilenameFilter(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return false || name.toLowerCase().endsWith(".csv") //$NON-NLS-1$
                            || name.toLowerCase().endsWith(".txt") //$NON-NLS-1$
                            || name.toLowerCase().endsWith(".dat"); //$NON-NLS-1$
                }
            });
            fileDialog.setVisible(true);
            if (fileDialog.getFile() == null) {
                return;
            }
            File file = new File(new File(fileDialog.getDirectory()), fileDialog.getFile());
            textFieldFile.setText(file.getAbsolutePath());
        }
    }


    private class SwingAction_2 extends AbstractAction {
        public SwingAction_2() {
            putValue(NAME, Translations.getString("CsvImporter.Import2Action.Name")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("CsvImporter.Import2Action.ShortDescription")); //$NON-NLS-1$
        }

        public void actionPerformed(ActionEvent e) {
            Logger.debug("Parsing " + textFieldFile.getText() + " CSV FIle"); //$NON-NLS-1$ //$NON-NLS-2$
            File file = new File(textFieldFile.getText());
            board = new Board();
            List<Placement> placements = new ArrayList<>();
            try {
                if (file.exists()) {
                    placements.addAll(parser.parseFile(file, chckbxCreateMissingParts.isSelected(),
                            chckbxUpdatePartHeight.isSelected()));
                }
            } catch (Exception e1) {
                MessageBoxes.errorBox(GenericCSVImporterDialog.this, Translations.getString("CsvImporter.ImportErrorMessage"), e1); //$NON-NLS-1$
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
            putValue(NAME, Translations.getString("CsvImporter.CancelAction.Name")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("CsvImporter.CancelAction.ShortDescription")); //$NON-NLS-1$
        }

        public void actionPerformed(ActionEvent e) {
            setVisible(false);
        }
    }
}
