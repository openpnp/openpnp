package org.openpnp.gui.importer;

import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

import org.openpnp.gui.importer.rs274x.Rs274xParser;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.Board;
import org.openpnp.model.Board.Side;
import org.openpnp.model.BoardPad;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

class SolderPasteGerberImporterDlg extends JDialog {
    private final SolderPasteGerberImporter solderPasteGerberImporter;
    private JTextField textFieldTopFile;
    private JTextField textFieldBottomFile;
    private final Action browseTopFileAction = new SwingAction();
    private final Action browseBottomFileAction = new SwingAction_1();
    private final Action importAction = new SwingAction_2();
    private final Action cancelAction = new SwingAction_3();
    
    public SolderPasteGerberImporterDlg(SolderPasteGerberImporter solderPasteGerberImporter, Frame parent) {
        super(parent, SolderPasteGerberImporter.DESCRIPTION, true);
        this.solderPasteGerberImporter = solderPasteGerberImporter;
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
        
        JLabel lblTopFilemnt = new JLabel("Top Paste Gerber");
        panel.add(lblTopFilemnt, "2, 2, right, default");
        
        textFieldTopFile = new JTextField();
        panel.add(textFieldTopFile, "4, 2, fill, default");
        textFieldTopFile.setColumns(10);
        
        JButton btnBrowse = new JButton("Browse");
        btnBrowse.setAction(browseTopFileAction);
        panel.add(btnBrowse, "6, 2");
        
        JLabel lblBottomFilemnb = new JLabel("Bottom Paste Gerber");
        panel.add(lblBottomFilemnb, "2, 4, right, default");
        
        textFieldBottomFile = new JTextField();
        panel.add(textFieldBottomFile, "4, 4, fill, default");
        textFieldBottomFile.setColumns(10);
        
        JButton btnBrowse_1 = new JButton("Browse");
        btnBrowse_1.setAction(browseBottomFileAction);
        panel.add(btnBrowse_1, "6, 4");
        
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
            FileDialog fileDialog = new FileDialog(SolderPasteGerberImporterDlg.this);
//            fileDialog.setFilenameFilter(new FilenameFilter() {
//                @Override
//                public boolean accept(File dir, String name) {
//                    return name.toLowerCase().endsWith(".mnt");
//                }
//            });
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
            FileDialog fileDialog = new FileDialog(SolderPasteGerberImporterDlg.this);
//            fileDialog.setFilenameFilter(new FilenameFilter() {
//                @Override
//                public boolean accept(File dir, String name) {
//                    return name.toLowerCase().endsWith(".mnb");
//                }
//            });
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
            SolderPasteGerberImporterDlg.this.solderPasteGerberImporter.topFile = new File(textFieldTopFile.getText());
            SolderPasteGerberImporterDlg.this.solderPasteGerberImporter.bottomFile = new File(textFieldBottomFile.getText());
            SolderPasteGerberImporterDlg.this.solderPasteGerberImporter.board = new Board();
            List<BoardPad> pads = new ArrayList<>();
            try {
                if (SolderPasteGerberImporterDlg.this.solderPasteGerberImporter.topFile.exists()) {
                    List<BoardPad> topPads = new Rs274xParser().parseSolderPastePads(SolderPasteGerberImporterDlg.this.solderPasteGerberImporter.topFile);
                    for (BoardPad pad : topPads) {
                        pad.setSide(Side.Top);
                    }
                    pads.addAll(topPads);
                }
                if (SolderPasteGerberImporterDlg.this.solderPasteGerberImporter.bottomFile.exists()) {
                    List<BoardPad> bottomPads = new Rs274xParser().parseSolderPastePads(SolderPasteGerberImporterDlg.this.solderPasteGerberImporter.bottomFile);
                    for (BoardPad pad : bottomPads) {
                        pad.setSide(Side.Bottom);
                    }
                    pads.addAll(bottomPads);
                }
            }
            catch (Exception e1) {
                MessageBoxes.errorBox(SolderPasteGerberImporterDlg.this, "Import Error", e1);
                return;
            }
            for (BoardPad pad : pads) {
                SolderPasteGerberImporterDlg.this.solderPasteGerberImporter.board.addSolderPastePad(pad);
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