package org.openpnp.vision.pipeline.ui;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.UIManager;

import org.openpnp.vision.FluentCv;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.stages.ConvertColor;
import org.openpnp.vision.pipeline.stages.LoadImage;
import org.openpnp.vision.pipeline.stages.SaveImage;

public class CvPipelineEditor extends JPanel {
    private JTable propertiesTable;
    private JTable stagesTable;
    public CvPipelineEditor(CvPipeline pipeline) {
        setLayout(new BorderLayout(0, 0));
        
        JToolBar toolbar = new JToolBar();
        add(toolbar, BorderLayout.NORTH);
        
        JButton btnAdd = new JButton("Add");
        toolbar.add(btnAdd);
        
        JButton btnRemove = new JButton("Remove");
        toolbar.add(btnRemove);
        
        JSplitPane leftRightSplitPane = new JSplitPane();
        add(leftRightSplitPane, BorderLayout.CENTER);
        
        JSplitPane stagesPropertiesSplitPane = new JSplitPane();
        stagesPropertiesSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        leftRightSplitPane.setLeftComponent(stagesPropertiesSplitPane);
        
        stagesTable = new JTable(new StagesTableModel(pipeline));
        stagesPropertiesSplitPane.setLeftComponent(new JScrollPane(stagesTable));
        
        propertiesTable = new JTable(new Object[][] {
            new Object[] { "color", true },
            new Object[] { "thing", "ConvertColor" }
        }, new String[] { "Property", "Value" });
        stagesPropertiesSplitPane.setRightComponent(new JScrollPane(propertiesTable));

        
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
    }

    public static void main(String[] args) {
        // http://developer.apple.com/library/mac/#documentation/Java/Conceptual/Java14Development/07-NativePlatformIntegration/NativePlatformIntegration.html#//apple_ref/doc/uid/TP40001909-212952-TPXREF134
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception e) {
            throw new Error(e);
        }
        
        CvPipeline pipeline = new CvPipeline();
        pipeline.add(new LoadImage().setPath("/Users/jason/Desktop/t.png"));
        pipeline.add(new ConvertColor().setConversion(FluentCv.ColorCode.Bgr2Gray));
        pipeline.add(new SaveImage().setPath("/Users/jason/Desktop/t_gray.png"));

        JFrame frame = new JFrame("CvPipelineEditor");
        frame.setSize(1024,  768);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(new CvPipelineEditor(pipeline));
        frame.setVisible(true);
    }
}
