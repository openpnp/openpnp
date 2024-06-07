package org.openpnp.gui.importer.diptrace.gui;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import org.openpnp.Translations;
import org.openpnp.gui.importer.diptrace.DipTraceBoardImporter;
import org.openpnp.gui.importer.diptrace.csv.DipTraceCSVParser;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.Board;
import org.openpnp.model.Placement;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

public class DiptraceBoardImporterDialog extends JDialog {

    private final DipTraceCSVParser parser;
    private final DipTraceBoardImporter boardImporter;
    private File fileName;

    //, bottomFile;

    private JTextField textFieldFileName;
    private JTextField textFieldBottomFile;
    private final Action browseTopFileAction = new SwingAction();
    private final Action importAction = new SwingAction_2();
    private final Action cancelAction = new SwingAction_3();
    private JCheckBox chckbxCreateMissingParts;

    public DiptraceBoardImporterDialog(Frame parent, DipTraceBoardImporter boardImporter, DipTraceCSVParser parser) {
        super(parent, boardImporter.getImporterDescription(), true);
        this.boardImporter = boardImporter;
        this.parser = parser;
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, Translations.getString("DipTraceImporter.FilesPanel.Border.title"), TitledBorder.LEADING, TitledBorder.TOP, //$NON-NLS-1$
                null, null));
        getContentPane().add(panel);
        panel.setLayout(new FormLayout(
                new ColumnSpec[]{FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"), //$NON-NLS-1$
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[]{FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblTopFilemnt = new JLabel(Translations.getString("DipTraceImporter.FilesPanel.TopFilemntLabel.text")); //$NON-NLS-1$
        panel.add(lblTopFilemnt, "2, 2, right, default"); //$NON-NLS-1$

        textFieldFileName = new JTextField();
        panel.add(textFieldFileName, "4, 2, fill, default"); //$NON-NLS-1$
        textFieldFileName.setColumns(10);

        JButton btnBrowse = new JButton(Translations.getString("DipTraceImporter.FilesPanel.browseButton.text")); //$NON-NLS-1$
        btnBrowse.setAction(browseTopFileAction);
        panel.add(btnBrowse, "6, 2"); //$NON-NLS-1$

        JPanel panel_1 = new JPanel();
        panel_1.setBorder(new TitledBorder(null, Translations.getString("DipTraceImporter.OptionsPanel.Border.title"), TitledBorder.LEADING, //$NON-NLS-1$
                TitledBorder.TOP, null, null));
        getContentPane().add(panel_1);
        panel_1.setLayout(new FormLayout(
                new ColumnSpec[]{FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[]{FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        chckbxCreateMissingParts = new JCheckBox(Translations.getString("DipTraceImporter.OptionsPanel.createMissingPartsCheckbox.text")); //$NON-NLS-1$
        chckbxCreateMissingParts.setSelected(true);
        panel_1.add(chckbxCreateMissingParts, "2, 2"); //$NON-NLS-1$

        JSeparator separator = new JSeparator();
        getContentPane().add(separator);

        JPanel panel_2 = new JPanel();
        FlowLayout flowLayout = (FlowLayout) panel_2.getLayout();
        flowLayout.setAlignment(FlowLayout.RIGHT);
        getContentPane().add(panel_2);

        JButton btnCancel = new JButton(Translations.getString("DipTraceImporter.ButtonsPanel.cancelButton.text")); //$NON-NLS-1$
        btnCancel.setAction(cancelAction);
        panel_2.add(btnCancel);

        JButton btnImport = new JButton(Translations.getString("DipTraceImporter.ButtonsPanel.importButton.text")); //$NON-NLS-1$
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
            putValue(NAME, Translations.getString("DipTraceImporter.BrowseAction.Name")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("DipTraceImporter.BrowseAction.ShortDescription")); //$NON-NLS-1$
        }

        public void actionPerformed(ActionEvent e) {
            FileDialog fileDialog = new FileDialog(DiptraceBoardImporterDialog.this);
            fileDialog.setFilenameFilter(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".csv"); //$NON-NLS-1$
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
            putValue(NAME, Translations.getString("DipTraceImporter.ImportAction.Name")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("DipTraceImporter.ImportAction.ShortDescription")); //$NON-NLS-1$
        }

        public void actionPerformed(ActionEvent e) {
            fileName = new File(textFieldFileName.getText());
            Board board = new Board();
            List<Placement> placements = new ArrayList<>();
            try {
                if (fileName.exists()) {
                    placements.addAll(parser.parseFile(fileName, chckbxCreateMissingParts.isSelected()));

                }
            } catch (Exception e1) {
                MessageBoxes.errorBox(DiptraceBoardImporterDialog.this, "Import Error", "The expected file format is the default file export in DipTrace " //$NON-NLS-1$ //$NON-NLS-2$
                        + "PCB: File -> Export -> Pick and Place. The first line indicates RefDes, Name, X (mm), Y (mm), Side, Rotate, Value." //$NON-NLS-1$
                        + "The lines that follow are data."); //$NON-NLS-1$
                return;
            }
            for (Placement placement : placements) {
                board.addPlacement(placement);
            }

            boardImporter.setBoard(board);
            setVisible(false);
        }
    }

    private class SwingAction_3 extends AbstractAction {
        public SwingAction_3() {
            putValue(NAME, Translations.getString("DipTraceImporter.CancelAction.Name")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("DipTraceImporter.CancelAction.ShortDescription")); //$NON-NLS-1$
        }

        public void actionPerformed(ActionEvent e) {
            setVisible(false);
        }
    }
}
