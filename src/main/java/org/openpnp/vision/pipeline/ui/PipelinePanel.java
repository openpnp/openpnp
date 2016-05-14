package org.openpnp.vision.pipeline.ui;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.openpnp.gui.components.ClassSelectionDialog;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.vision.pipeline.CvStage;

import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.propertysheet.PropertySheetPanel;

public class PipelinePanel extends JPanel {
    private final CvPipelineEditor editor;

    private JTable stagesTable;
    private StagesTableModel stagesTableModel;
    private PropertySheetPanel propertySheetPanel;

    public PipelinePanel(CvPipelineEditor editor) {
        this.editor = editor;

        propertySheetPanel = new PropertySheetPanel();

        setLayout(new BorderLayout(0, 0));

        JSplitPane splitPane = new JSplitPane();
        add(splitPane, BorderLayout.CENTER);
        splitPane.setContinuousLayout(true);
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);

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

        stagesTable = new JTable(stagesTableModel = new StagesTableModel(editor.getPipeline()));
        stagesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        stagesTable.setDragEnabled(true);
        stagesTable.setDropMode(DropMode.INSERT_ROWS);
        stagesTable.setTransferHandler(new TableRowTransferHandler(stagesTable));
        stagesTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        stagesTable.getColumnModel().getColumn(1).setPreferredWidth(50);
        stagesTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        JScrollPane scrollPane = new JScrollPane(stagesTable);

        splitPane.setRightComponent(propertySheetPanel);
        splitPane.setLeftComponent(scrollPane);

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
                if (stage == null) {
                    propertySheetPanel.setProperties(new Property[] {});
                }
                else {
                    try {
                        propertySheetPanel.setBeanInfo(stage.getBeanInfo());
                        propertySheetPanel.readFromObject(stage);
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
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

        // Set the divider location after it's added to the view
        addHierarchyListener(new HierarchyListener() {
            @Override
            public void hierarchyChanged(HierarchyEvent e) {
                splitPane.setDividerLocation(0.5);
            }
        });

        // Listen for editing events in the properties and process the pipeline to update the
        // results.
        propertySheetPanel.getTable().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent e) {
                if ("tableCellEditor".equals(e.getPropertyName())) {
                    if (!propertySheetPanel.getTable().isEditing()) {
                        // editing has ended for a cell, save the values
                        propertySheetPanel.writeToObject(getSelectedStage());
                        editor.process();
                    }
                }
            }
        });
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
            putValue(NAME, "New Stage...");
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
                    JOptionPane.getFrameForComponent(PipelinePanel.this), "New Stage",
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
            putValue(NAME, "Copy Pipeline to Clipboard");
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
                MessageBoxes.errorBox(getTopLevelAncestor(), "Copy Failed", e);
            }
        }
    };

    public final Action pasteAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.paste);
            putValue(NAME, "Create Pipeline from Clipboard");
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
                MessageBoxes.errorBox(getTopLevelAncestor(), "Paste Failed", e);
            }
        }
    };

    public final Action refreshAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.refresh);
            putValue(NAME, "");
            putValue(SHORT_DESCRIPTION, "");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            editor.process();
        }
    };
}
