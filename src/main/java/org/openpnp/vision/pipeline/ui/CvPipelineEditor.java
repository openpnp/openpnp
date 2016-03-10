package org.openpnp.vision.pipeline.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.beans.Introspector;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openpnp.gui.components.ClassSelectionDialog;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.stages.ConvertColor;
import org.openpnp.vision.pipeline.stages.LoadImage;
import org.openpnp.vision.pipeline.stages.SaveImage;

import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.propertysheet.PropertySheetPanel;

/**
 * http://docs.oracle.com/javase/tutorial/uiswing/dnd/intro.html
 * https://docs.oracle.com/javase/7/docs/api/java/beans/PropertyEditor.html
 * http://www.programcreek.com/java-api-examples/java.beans.PropertyEditor (Example 6)
 * http://www.java2s.com/Code/Java/Swing-JFC/PropertyTableUseJTabletodisplayandeditproperties.htm
 * https://docs.oracle.com/javase/7/docs/api/java/beans/PropertyEditorSupport.html
 * http://www.javaworld.com/article/2077063/learn-java/the-trick-to-controlling-bean-customization.
 * WINNER: http://www2.sys-con.com/itsg/virtualcd/java/archives/0408/crafton/index.html
 * http://stackoverflow.com/questions/638807/how-do-i-drag-and-drop-a-row-in-a-jtable
 * https://github.com/sarxos/l2fprod-common
 * http://www.java2s.com/Code/Java/Swing-Components/Propertysheettable.htm
 * https://github.com/ZenHarbinger/l2fprod-properties-editor
 */
@SuppressWarnings("serial")
public class CvPipelineEditor extends JPanel {
    private final static Set<Class<? extends CvStage>> stageClasses = new HashSet<>();

    private final CvPipeline pipeline;

    private JTable stagesTable;
    private StagesTableModel stagesTableModel;
    private PropertySheetPanel propertySheetPanel;

    public CvPipelineEditor(CvPipeline pipeline) {
        this.pipeline = pipeline;

        setLayout(new BorderLayout(0, 0));

        propertySheetPanel = new PropertySheetPanel();
                
        JToolBar toolbar = new JToolBar();
        add(toolbar, BorderLayout.NORTH);

        JButton btnAdd = new JButton(newStageAction);
        btnAdd.setHideActionText(true);
        toolbar.add(btnAdd);

        JButton btnRemove = new JButton(deleteStageAction);
        btnRemove.setHideActionText(true);
        toolbar.add(btnRemove);

        JSplitPane leftRightSplitPane = new JSplitPane();
        add(leftRightSplitPane, BorderLayout.CENTER);

        JSplitPane stagesPropertiesSplitPane = new JSplitPane();
        stagesPropertiesSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        leftRightSplitPane.setLeftComponent(stagesPropertiesSplitPane);

        stagesTable = new JTable(stagesTableModel = new StagesTableModel(pipeline));
        stagesPropertiesSplitPane.setLeftComponent(new JScrollPane(stagesTable));

        stagesPropertiesSplitPane.setRightComponent(propertySheetPanel);

        JPanel panel = new JPanel();
        leftRightSplitPane.setRightComponent(panel);
        panel.setLayout(new BorderLayout(0, 0));

        JPanel panel_1 = new JPanel();
        panel.add(panel_1, BorderLayout.SOUTH);

        JToolBar imagesToolbar = new JToolBar();
        panel_1.add(imagesToolbar);

        JButton btnFirst = new JButton("First");
        imagesToolbar.add(btnFirst);

        JButton btnPrevious = new JButton("Previous");
        imagesToolbar.add(btnPrevious);

        JLabel lblStageName = new JLabel("stage name");
        imagesToolbar.add(lblStageName);

        JButton btnNext = new JButton("Next");
        imagesToolbar.add(btnNext);

        JButton btnLast = new JButton("Last");
        imagesToolbar.add(btnLast);

        JPanel imagePanel = new JPanel();
        panel.add(imagePanel, BorderLayout.CENTER);
        addHierarchyListener(new HierarchyListener() {
            @Override
            public void hierarchyChanged(HierarchyEvent e) {
                stagesPropertiesSplitPane.setDividerLocation(0.5);
                leftRightSplitPane.setDividerLocation(0.25);
            }
        });

        stagesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                CvStage stage = getSelectedStage();
                if (stage == null) {
                    propertySheetPanel.setProperties(new Property[] {});
                }
                else {
                    try {
                        propertySheetPanel.setBeanInfo(Introspector.getBeanInfo(stage.getClass(), CvStage.class));
                        propertySheetPanel.readFromObject(stage);
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
    }

    public static void registerStageClass(Class<? extends CvStage> cls) {
        stageClasses.add(cls);
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
            ClassSelectionDialog<CvStage> dialog = new ClassSelectionDialog<>(
                    JOptionPane.getFrameForComponent(CvPipelineEditor.this), "New Stage",
                    "Please select a stage implemention from the list below.",
                    new ArrayList<>(stageClasses));
            dialog.setVisible(true);
            Class<? extends CvStage> stageClass = dialog.getSelectedClass();
            if (stageClass == null) {
                return;
            }
            try {
                CvStage stage = stageClass.newInstance();
                pipeline.add(stage);
                stagesTableModel.refresh();
                Helpers.selectLastTableRow(stagesTable);
            }
            catch (Exception e) {
                MessageBoxes.errorBox(JOptionPane.getFrameForComponent(CvPipelineEditor.this),
                        "Feeder Error", e);
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
            pipeline.remove(stage);
            stagesTableModel.refresh();
        }
    };


    public static void main(String[] args) throws Exception {
        // http://developer.apple.com/library/mac/#documentation/Java/Conceptual/Java14Development/07-NativePlatformIntegration/NativePlatformIntegration.html#//apple_ref/doc/uid/TP40001909-212952-TPXREF134
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception e) {
            throw new Error(e);
        }

        CvPipelineEditor.registerStageClass(ConvertColor.class);
        CvPipelineEditor.registerStageClass(LoadImage.class);
        CvPipelineEditor.registerStageClass(SaveImage.class);

        CvPipeline pipeline = new CvPipeline();
        pipeline.add(new LoadImage());
        pipeline.add(new ConvertColor());
        pipeline.add(new SaveImage());

        JFrame frame = new JFrame("CvPipelineEditor");
        frame.setSize(1024, 768);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(new CvPipelineEditor(pipeline));
        frame.setVisible(true);
    }
}
