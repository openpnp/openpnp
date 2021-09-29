package org.openpnp.vision.pipeline.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.util.*;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;

import org.openpnp.gui.components.ClassSelectionDialog;
import org.openpnp.gui.components.PartPackageSelectionDialog;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.Configuration;
import org.openpnp.model.Package;
import org.openpnp.model.Part;
import org.openpnp.spi.Camera;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.PairKey;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;

import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.propertysheet.PropertySheetPanel;
import com.l2fprod.common.propertysheet.PropertySheetTableModel;
import com.l2fprod.common.swing.renderer.DefaultCellRenderer;

import static javax.swing.SwingConstants.TOP;

public class PipelinePanel extends JPanel {
    public static final String ADD = "add";
    public static final String DELETE = "delete";
    public static final int STAGES = 0;
    public static final int PARTS_PACKAGES = 1;

    public final CvPipelineEditor editor;

    private JTable stagesTable;
    private JTable partsTable;
    private JTable packagesTable;
    private JButton btnAdd;
    private JButton btnRemove;

    private StagesTableModel stagesTableModel;
    private PipelineEditorPartsTableModel partsTableModel;
    private PipelineEditorPackagesTableModel packagesTableModel;
    private PropertySheetPanel propertySheetPanel;
    private PipelinePropertySheetTable pipelinePropertySheetTable;

    HashMap<PairKey<String, Integer>, Action> toolbarActionMap = new HashMap<>();

    public PipelinePanel(CvPipelineEditor editor, boolean tabs) {
        this.editor = editor;

        initializeToolbarActionMap();

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

    private void initializeToolbarActionMap() {
        toolbarActionMap.put(new PairKey<>(ADD, STAGES), new NewStageAction("New stage...", "Create a new stage."));
        toolbarActionMap.put(new PairKey<>(DELETE, STAGES), new DeleteStageAction("Delete Stage...", "Delete the selected stage."));
        toolbarActionMap.put(new PairKey<>(ADD, PARTS_PACKAGES), new NewPartPackageAction("New part/package", "Add new part of package"));
        toolbarActionMap.put(new PairKey<>(DELETE, PARTS_PACKAGES), new DeletePartPackageAction("Delete Part/Package...", "Delete the selected Part or Package."));
    }

    private void prepareToolbar() {
        JToolBar toolbar = new JToolBar();
        add(toolbar, BorderLayout.NORTH);

        JButton refreshButton = new JButton(new RefreshAction(Icons.refresh, "Update picture from current view.", "Update picture from current view."));
        refreshButton.setHideActionText(true);
        toolbar.add(refreshButton);

        btnAdd = new JButton("Add new ...");
        toolbar.add(btnAdd);
        btnAdd.setHideActionText(true);

        btnRemove = new JButton("Delete ...");
        toolbar.add(btnRemove);
        btnRemove.setHideActionText(true);

        toolbar.addSeparator();

        JButton copyButton = new JButton(new CopyPipelineAction("Copy pipeline to clipboard", "Copy the pipeline to the clipboard in text format."));
        toolbar.add(copyButton);
        copyButton.setHideActionText(true);

        JButton pasteButton = new JButton(new PastePipelineAction("Paste pipeline from clipboard", "Paste new pipeline from a definition on the clipboard."));
        toolbar.add(pasteButton);
        pasteButton.setHideActionText(true);

        setToolbarButtonsActions(STAGES);
    }

    private JTabbedPane prepareTabView() {
        JTabbedPane tabs = new JTabbedPane(TOP);

        tabs.addTab("Stages", null, prepareStagesSplitView(), null);
        tabs.addTab("Parts/Packages", null, preparePartsPackagesSplitView(), null);

        tabs.addChangeListener(e -> setToolbarButtonsActions(tabs.getSelectedIndex()));

        return tabs;
    }

    private void setToolbarButtonsActions(int selectedPane) {
        btnAdd.setAction(toolbarActionMap.get(new PairKey<>(ADD, selectedPane)));
        btnRemove.setAction(toolbarActionMap.get(new PairKey<>(DELETE, selectedPane)));
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

    private JSplitPane preparePartsPackagesSplitView() {
        JSplitPane splitPanePartsPackages = new JSplitPane();
        splitPanePartsPackages.setContinuousLayout(true);
        splitPanePartsPackages.setOrientation(JSplitPane.VERTICAL_SPLIT);

        splitPanePartsPackages.setLeftComponent(preparePartsPane());
        splitPanePartsPackages.setRightComponent(preparePackagesPane());

        splitPanePartsPackages.setResizeWeight(0.4);

        return splitPanePartsPackages;
    }

    private JScrollPane preparePartsPane() {
        partsTableModel = new PipelineEditorPartsTableModel(editor.getUpperPipeline());
        partsTable = preparePartsPackagesTable(partsTableModel);

        return new JScrollPane(partsTable);
    }

    private JScrollPane preparePackagesPane() {
        packagesTableModel = new PipelineEditorPackagesTableModel(editor.getUpperPipeline());
        packagesTable = preparePartsPackagesTable(packagesTableModel);

        packagesTable.changeSelection(packagesTable.getRowCount()-1,  0,  false, false);

        return new JScrollPane(packagesTable);
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

    class Action extends AbstractAction {
        Action(Icon icon, String name, String description) {
            putValue(SMALL_ICON, icon);
            putValue(NAME, name);
            putValue(SHORT_DESCRIPTION, description);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            //TODO NK: throw an exception
        }
    }

    class NewStageAction extends Action {
        NewStageAction(String name, String description) {
            super(Icons.add, name, description);
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
    }

    class DeleteStageAction extends Action {
        DeleteStageAction(String name, String description) {
            super(Icons.delete, name, description);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            CvStage stage = getSelectedStage();
            editor.getPipeline().remove(stage);
            stagesTableModel.refresh();
            editor.process();
        }
    }

    class CopyPipelineAction extends Action {
        CopyPipelineAction(String name, String description) {
            super(Icons.copy, name, description);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                StringSelection stringSelection =
                        new StringSelection(editor.getPipeline().toXmlString());
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);
            }
            catch (Exception exception) {
                MessageBoxes.errorBox(getTopLevelAncestor(), "Copy failed", exception);
            }
        }
    }

    class PastePipelineAction extends Action {
        PastePipelineAction(String name, String description) {
            super(Icons.paste, name, description);
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
    }

    class NewPartPackageAction extends Action {
        NewPartPackageAction(String name, String description) {
            super(Icons.add, name, description);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            List<Part> parts = Configuration.get().getParts();
            List<Package> packages = Configuration.get().getPackages();
            //TODO NK: unmodifiable list, cannot sort
            PartPackageSelectionDialog dialog = new PartPackageSelectionDialog(JOptionPane.getFrameForComponent(PipelinePanel.this), "New Part",
                    "Please select a part/package from the lists below.", parts, packages);
            dialog.setVisible(true);

            if (dialog.getSelectedPane() == 0) {
                addSelectedPart(dialog.getSelectedPart());
            } else {
                addSelectedPackage(dialog.getSelectedPackage());
            }
        }
    }

    private void addSelectedPart(Part selected) {
        if (selected == null) {
            return;
        }

        if (!selected.getPipeline().getId().equals("CVP_DEF")) {
            int selection = JOptionPane.showConfirmDialog(editor,
                    "Part already has a pipeline assigned, do you want to rewrite it?",
                    "Rewrite pipeline",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null
            );
            if (selection == JOptionPane.NO_OPTION) {
                return;
            }
        }

        try {
            Configuration.get().assignPipelineToPart(selected, editor.getUpperPipeline());
            partsTableModel = new PipelineEditorPartsTableModel(editor.getUpperPipeline());
            partsTable.setModel(partsTableModel);
        }
        catch (Exception e) {
            MessageBoxes.errorBox(JOptionPane.getFrameForComponent(PipelinePanel.this), "Error",
                    e);
        }
    }

    private void addSelectedPackage(Package selected) {
        if (selected == null) {
            return;
        }

        if (!selected.getPipeline().getId().equals("CVP_DEF")) {
            int selection = JOptionPane.showConfirmDialog(editor,
                    "Package already has a pipeline assigned, its and all its parts' pipeline will be set to default. Do you want to proceed?",
                    "Rewrite pipeline",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null
            );
            if (selection == JOptionPane.NO_OPTION) {
                return;
            }
        }

        try {
            Configuration.get().assignPipelineToPackage(selected, editor.getUpperPipeline());
            partsTableModel = new PipelineEditorPartsTableModel(editor.getUpperPipeline());
            packagesTableModel = new PipelineEditorPackagesTableModel(editor.getUpperPipeline());
            partsTable.setModel(partsTableModel);
            packagesTable.setModel(packagesTableModel);
        }
        catch (Exception e) {
            MessageBoxes.errorBox(JOptionPane.getFrameForComponent(PipelinePanel.this), "Error",
                    e);
        }
    }

    class DeletePartPackageAction extends Action {
        DeletePartPackageAction(String name, String description) {
            super(Icons.delete, name, description);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Part selectedPart = getSelectedPart();
            selectedPart.setPipeline(Configuration.get().getDefaultPipeline());
            partsTableModel = new PipelineEditorPartsTableModel(editor.getUpperPipeline());
            partsTable.setModel(partsTableModel);
        }
    }

    class RefreshAction extends Action {
        RefreshAction(Icon icon, String name, String description) {
            super(icon, name, description);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            editor.process();
        }
    }
    private JEditorPane descriptionTa;
}
