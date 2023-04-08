package org.openpnp.vision.pipeline.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.openpnp.gui.components.ClassSelectionDialog;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.spi.Camera;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;

import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.propertysheet.PropertySheetPanel;
import com.l2fprod.common.propertysheet.PropertySheetTableModel;
import com.l2fprod.common.swing.renderer.DefaultCellRenderer;

public class PipelinePanel extends JPanel {
    private final CvPipelineEditor editor;

    private JTable stagesTable;
    private StagesTableModel stagesTableModel;
    private PropertySheetPanel propertySheetPanel;
    private JEditorPane descriptionTa;
    private PipelinePropertySheetTable pipelinePropertySheetTable;

    public PipelinePanel(CvPipelineEditor editor) {
        this.editor = editor;

        pipelinePropertySheetTable = new PipelinePropertySheetTable(this);
        propertySheetPanel = new PropertySheetPanel(pipelinePropertySheetTable);
        propertySheetPanel.setDescriptionVisible(true);

        setLayout(new BorderLayout(0, 0));

        JSplitPane splitPaneMain = new JSplitPane();
        add(splitPaneMain, BorderLayout.CENTER);
        splitPaneMain.setContinuousLayout(true);
        splitPaneMain.setOrientation(JSplitPane.VERTICAL_SPLIT);

        JToolBar toolbar = new JToolBar();
        add(toolbar, BorderLayout.NORTH);

        JButton refreshButton = new JButton(refreshAction);
        refreshButton.setHideActionText(true);
        toolbar.add(refreshButton);

        if (editor.getPipeline() != null 
                && editor.getPipeline().getPipelineShotsCount() > 1) {
            JButton stepNextButton = new JButton(stepNextShotAction);
            stepNextButton.setHideActionText(true);
            toolbar.add(stepNextButton);
        }

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

        JSplitPane splitPaneStages = new JSplitPane();
        splitPaneStages.setOrientation(JSplitPane.VERTICAL_SPLIT);

        stagesTable = new JTable(stagesTableModel = new StagesTableModel(editor.getPipeline()));
        stagesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        stagesTable.setDragEnabled(true);
        stagesTable.setDropMode(DropMode.INSERT_ROWS);
        stagesTable.setTransferHandler(new TableRowTransferHandler(stagesTable));
        stagesTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        stagesTable.getColumnModel().getColumn(1).setPreferredWidth(50);
        stagesTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        JScrollPane scrollPaneStages = new JScrollPane(stagesTable);
        splitPaneStages.setLeftComponent(scrollPaneStages);

        JScrollPane scrollPaneDescription = new JScrollPane();
        splitPaneStages.setRightComponent(scrollPaneDescription);
        scrollPaneDescription.setMinimumSize(new Dimension(50, 50));
        descriptionTa = new JEditorPane("text/html", "<html/>");
        descriptionTa.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        descriptionTa.setBackground(UIManager.getColor("Panel.background")); //$NON-NLS-1$
        scrollPaneDescription.setViewportView(descriptionTa);
        descriptionTa.setText("");
        descriptionTa.setEditable(false);

        // Listen for changes to the selection of the table and update the properties for the
        // selected stage.
        stagesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                CvStage stage = getSelectedStage();
                editor.stageSelected(stage);
                refreshDescription();
                refreshProperties();
            }
        });

        // Listen for changes to the structure of the table (adding or removing rows) and process
        // the pipeline to update the results.
        stagesTable.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                editor.process();
            }
        });

        // Listen for editing events in the stages table and process the pipeline to update the
        // results.
        stagesTable.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent e) {
                if ("tableCellEditor".equals(e.getPropertyName())) {
                    if (!propertySheetPanel.getTable().isEditing()) {
                        // editing has ended for a cell, save the values
                        editor.process();
                    }
                }
            }
        });
        stagesTable.changeSelection(stagesTable.getRowCount()-1,  0,  false, false);
          
        splitPaneMain.setLeftComponent(splitPaneStages);
        splitPaneMain.setRightComponent(propertySheetPanel);
        
        splitPaneMain.setResizeWeight(0.5);
        splitPaneStages.setResizeWeight(0.80);
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

    public void onStagePropertySheetValueChanged(Object aValue, int row, int column) {
        // editing has ended for a cell, save the values
        refreshDescription();

        // Don't use propertySheetPanel.writeToObject(stage) as it will cause infinitely recursive setValueAt() calls
        // due to it calling getTable().commitEditing() (which is what called this function originally)
        PropertySheetTableModel propertySheetTableModel = propertySheetPanel.getTable().getSheetModel();
        PropertySheetTableModel.Item propertySheetElement = propertySheetTableModel.getPropertySheetElement(row);
        Property property = propertySheetElement.getProperty();
        property.writeToObject(getSelectedStage());

        editor.process();
        if (property.getName().equals(VisionUtils.PIPELINE_CONTROL_PROPERTY_NAME)) {
            // If the control property (a special name by convention) was changed, this means the stage will be 
            // controlled/not controlled by the pipeline caller, and we need to refresh the properties altogether. 
            SwingUtilities.invokeLater(() -> refreshProperties());
        }
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

    public Action newStageAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.add);
            putValue(NAME, "New stage...");
            putValue(SHORT_DESCRIPTION, "Create a new stage.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            List<Class<? extends CvStage>> stageClasses = new ArrayList<>(editor.getStageClasses());
            stageClasses.sort(new Comparator<Class<? extends CvStage>>() {
                @Override
                public int compare(Class<? extends CvStage> o1, Class<? extends CvStage> o2) {
                    return o1.getSimpleName().toLowerCase()
                            .compareTo(o2.getSimpleName().toLowerCase());
                }
            });
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

    public final Action stepNextShotAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.step);
            putValue(NAME, "Step to the next shot.");
            putValue(SHORT_DESCRIPTION, "Step to the next shot.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.messageBoxOnException(() -> {
                editor.getPipeline().stepToNextPipelineShot();
                editor.process();
            });
        }
    };
}
