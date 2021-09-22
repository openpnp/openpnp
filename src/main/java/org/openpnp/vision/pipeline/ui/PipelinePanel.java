package org.openpnp.vision.pipeline.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;

import org.openpnp.gui.components.ClassSelectionDialog;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.Part;
import org.openpnp.spi.Camera;
import org.openpnp.util.MovableUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;

import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.propertysheet.PropertySheetPanel;
import com.l2fprod.common.propertysheet.PropertySheetTableModel;
import com.l2fprod.common.swing.renderer.DefaultCellRenderer;

import static javax.swing.SwingConstants.TOP;

public class PipelinePanel extends JPanel {
    private final CvPipelineEditor editor;

    private JTable stagesTable;
    private JTable partsTable;
    private JTable packagesTable;
    private StagesTableModel stagesTableModel;
    private PipelineEditorPartsTableModel partsTableModel;
    private StagesTableModel packagesTableModel;
    private PropertySheetPanel propertySheetPanel;
    private PipelinePropertySheetTable pipelinePropertySheetTable;

    public PipelinePanel(CvPipelineEditor editor, boolean tabs) {
        this.editor = editor;

        pipelinePropertySheetTable = new PipelinePropertySheetTable(this);
        propertySheetPanel = new PropertySheetPanel(pipelinePropertySheetTable);
        propertySheetPanel.setDescriptionVisible(true);

        setLayout(new BorderLayout(0, 0));

        prepareToolbar();

        if (tabs) {
            add(prepareTabView());
        } else {
            add(prepareStagesSplitView(), BorderLayout.CENTER);
        }
    }

    private JTabbedPane prepareTabView() {
        JTabbedPane tabs = new JTabbedPane(TOP);

        tabs.addTab("Stages", null, prepareStagesSplitView(), null);
        tabs.addTab("Parts/Packages", null, preparePartsPackagesSplitView(), null);

        return tabs;
    }

    private JSplitPane prepareStagesSplitView() {
        JSplitPane splitPaneMain = new JSplitPane();
        splitPaneMain.setContinuousLayout(true);
        splitPaneMain.setOrientation(JSplitPane.VERTICAL_SPLIT);

        splitPaneMain.setLeftComponent(prepareSplitPaneStages());
        splitPaneMain.setRightComponent(propertySheetPanel);

        splitPaneMain.setResizeWeight(0.5);

        return splitPaneMain;
    }

    private JSplitPane prepareSplitPaneStages() {
        JSplitPane splitPaneStages = new JSplitPane();
        splitPaneStages.setOrientation(JSplitPane.VERTICAL_SPLIT);

        stagesTableModel = new StagesTableModel(editor.getPipeline());
        stagesTable = prepareTable(stagesTableModel);

        JScrollPane scrollPaneStages = new JScrollPane(stagesTable);
        splitPaneStages.setLeftComponent(scrollPaneStages);

        JScrollPane scrollPaneDescription = new JScrollPane();
        splitPaneStages.setRightComponent(scrollPaneDescription);
        scrollPaneDescription.setMinimumSize(new Dimension(50, 50));
        descriptionTa = new JEditorPane("text/html", "<html/>");
        scrollPaneDescription.setViewportView(descriptionTa);
        descriptionTa.setText("");
        descriptionTa.setEditable(false);

        // Listen for changes to the selection of the table and update the properties for the selected stage.
        stagesTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            CvStage stage = getSelectedStage();
            editor.stageSelected(stage);
            refreshDescription();
            refreshProperties();
        });

        // Listen for changes to the structure of the table (adding or removing rows) and process
        // the pipeline to update the results.
        stagesTable.getModel().addTableModelListener(e -> editor.process());

        stagesTable.changeSelection(stagesTable.getRowCount()-1,  0,  false, false);

        // Listen for editing events in the stages table and process the pipeline to update the results.
        stagesTable.addPropertyChangeListener(e -> {
            if ("tableCellEditor".equals(e.getPropertyName()) && !propertySheetPanel.getTable().isEditing()) {
                // editing has ended for a cell, save the values
                editor.process();
            }
        });

        splitPaneStages.setResizeWeight(0.80);

        return splitPaneStages;
    }

    private JScrollPane preparePartsPane() {
        partsTableModel = new PipelineEditorPartsTableModel(editor.getUpperPipeline());
        partsTable = preparePartsPackagesTable(partsTableModel);

        JScrollPane scrollPaneStages = new JScrollPane(partsTable);

        partsTable.changeSelection(partsTable.getRowCount()-1,  0,  false, false);

        return scrollPaneStages;
    }

    private void prepareToolbar() {
        JToolBar toolbar = new JToolBar();
        add(toolbar, BorderLayout.NORTH);

        JButton refreshButton = new JButton(refreshAction);
        refreshButton.setHideActionText(true);
        toolbar.add(refreshButton);

        JButton btnAdd = new JButton(newStageAction);
        btnAdd.setHideActionText(true);
        toolbar.add(btnAdd);

        JButton btnRemove = new JButton(deleteStageAction);
        btnRemove.setHideActionText(true);
        toolbar.add(btnRemove);

        toolbar.addSeparator();

        JButton copyButton = new JButton(copyAction);
        copyButton.setHideActionText(true);
        toolbar.add(copyButton);

        JButton pasteButton = new JButton(pasteAction);
        pasteButton.setHideActionText(true);
        toolbar.add(pasteButton);
    }

    private JSplitPane preparePartsPackagesSplitView() {
        JSplitPane splitPanePartsPackages = new JSplitPane();
        splitPanePartsPackages.setContinuousLayout(true);
        splitPanePartsPackages.setOrientation(JSplitPane.VERTICAL_SPLIT);

        JScrollPane scrollPanePackages = new JScrollPane(packagesTable);

        splitPanePartsPackages.setLeftComponent(preparePartsPane());
        splitPanePartsPackages.setRightComponent(scrollPanePackages);

        return splitPanePartsPackages;
    }

    private JTable preparePartsPackagesTable(AbstractTableModel tableModel) {
        JTable table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDragEnabled(true);
        table.setDropMode(DropMode.INSERT_ROWS);
        table.setTransferHandler(new TableRowTransferHandler(table));
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        return table;
    }

    private JTable prepareTable(AbstractTableModel tableModel) {
        JTable table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDragEnabled(true);
        table.setDropMode(DropMode.INSERT_ROWS);
        table.setTransferHandler(new TableRowTransferHandler(table));
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(50);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        return table;
    }

    public void initializeFocus() {
        stagesTable.grabFocus();
        try {
            CvPipeline pipeline = editor.getPipeline();
            Camera camera = (Camera) pipeline.getProperty("camera");
            if (camera != null) {
                MovableUtils.fireTargetedUserAction(camera);
            }
        }
        catch (Exception e) {
        }
    }

    public void onStagePropertySheetValueChanged(int row) {
        // editing has ended for a cell, save the values
        refreshDescription();

        // Don't use propertySheetPanel.writeToObject(stage) as it will cause infinitely recursive setValueAt() calls
        // due to it calling getTable().commitEditing() (which is what called this function originally)
        PropertySheetTableModel propertySheetTableModel = propertySheetPanel.getTable().getSheetModel();
        PropertySheetTableModel.Item propertySheetElement = propertySheetTableModel.getPropertySheetElement(row);
        Property property = propertySheetElement.getProperty();
        property.writeToObject(getSelectedStage());

        editor.process();
    }

    private void refreshProperties() {
        CvStage stage = getSelectedStage();
        if (stage == null) {
            propertySheetPanel.setProperties(new Property[] {});
        }
        else {
            try {
                propertySheetPanel.setBeanInfo(stage.getBeanInfo());
                propertySheetPanel.readFromObject(stage);
                // Set the Object.class DefaultCellRenderer, it might have been customized in a previously selected stage.
                pipelinePropertySheetTable.getRendererRegistry().registerRenderer(Object.class, new DefaultCellRenderer());
                stage.customizePropertySheet(pipelinePropertySheetTable, editor.getPipeline());
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void refreshDescription() {
        CvStage stage = getSelectedStage();
        if (stage == null) {
            descriptionTa.setText("");
        }
        else {
            try {
                descriptionTa.setText(stage.getDescription());
                descriptionTa.setCaretPosition(0);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public CvStage getSelectedStage() {
        int index = stagesTable.getSelectedRow();
        if (index == -1) {
            return null;
        }
        else {
            index = stagesTable.convertRowIndexToModel(index);
            return stagesTableModel.getStage(index);
        }
    }

    public Part getSelectedPart() {
        int index = partsTable.getSelectedRow();
        if (index == -1) {
            return null;
        }
        else {
            index = partsTable.convertRowIndexToModel(index);
            return partsTableModel.getPart(index);
        }
    }

    public Action newStageAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.add);
            putValue(NAME, "New stage...");
            putValue(SHORT_DESCRIPTION, "Create a new stage.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            List<Class<? extends CvStage>> stageClasses = new ArrayList<>(CvPipelineEditor.getStageClasses());
            stageClasses.sort(Comparator.comparing(o -> o.getSimpleName().toLowerCase()));
            ClassSelectionDialog<CvStage> dialog = new ClassSelectionDialog<>(
                    JOptionPane.getFrameForComponent(PipelinePanel.this), "New stage",
                    "Please select a stage implemention from the list below.", stageClasses);
            dialog.setVisible(true);
            Class<? extends CvStage> stageClass = dialog.getSelectedClass();
            if (stageClass == null) {
                return;
            }
            try {
                CvStage stage = stageClass.newInstance();
                editor.getPipeline().add(stage);
                stagesTableModel.refresh();
                Helpers.selectLastTableRow(stagesTable);
                editor.process();
            }
            catch (Exception e) {
                MessageBoxes.errorBox(JOptionPane.getFrameForComponent(PipelinePanel.this), "Error",
                        e);
            }
        }
    };

    public Action deleteStageAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.delete);
            putValue(NAME, "Delete Stage...");
            putValue(SHORT_DESCRIPTION, "Delete the selected stage.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            CvStage stage = getSelectedStage();
            editor.getPipeline().remove(stage);
            stagesTableModel.refresh();
            editor.process();
        }
    };

    public final Action copyAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.copy);
            putValue(NAME, "Copy pipeline to clipboard");
            putValue(SHORT_DESCRIPTION, "Copy the pipeline to the clipboard in text format.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            try {
                StringSelection stringSelection =
                        new StringSelection(editor.getPipeline().toXmlString());
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);
            }
            catch (Exception e) {
                MessageBoxes.errorBox(getTopLevelAncestor(), "Copy failed", e);
            }
        }
    };

    public final Action pasteAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.paste);
            putValue(NAME, "Create pipeline from clipboard");
            putValue(SHORT_DESCRIPTION,
                    "Create a new pipeline from a definition on the clipboard.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            try {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                String s = (String) clipboard.getData(DataFlavor.stringFlavor);
                editor.getPipeline().fromXmlString(s);
                stagesTableModel.refresh();
                Helpers.selectLastTableRow(stagesTable);
                editor.process();
            }
            catch (Exception e) {
                MessageBoxes.errorBox(getTopLevelAncestor(), "Paste failed", e);
            }
        }
    };

    public final Action refreshAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.refresh);
            putValue(NAME, "Update picture from current view.");
            putValue(SHORT_DESCRIPTION, "Update picture from current view.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            editor.process();
        }
    };
    private JEditorPane descriptionTa;
}
