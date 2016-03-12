package org.openpnp.vision.pipeline.ui;

import java.awt.BorderLayout;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.UIManager;

import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.stages.BlurGaussian;
import org.openpnp.vision.pipeline.stages.ConvertColor;
import org.openpnp.vision.pipeline.stages.EdgeDetectCanny;
import org.openpnp.vision.pipeline.stages.LoadImage;
import org.openpnp.vision.pipeline.stages.SaveImage;

/**
 * A JPanel based component for editing a CvPipeline. Allows the user to add and remove stages,
 * modify properties of each stage, see the image and model results from each stage and export and
 * import the pipeline from the clipboard.
 * 
 * The static method #registerStageClass can be used to register a CvStage implementation with this
 * component, allowing the user to select the implementation from the list when creating a new
 * stage.
 * 
 * The core CvStage classes are automatically registered during startup.
 */
@SuppressWarnings("serial")
public class CvPipelineEditor extends JPanel {
    static {
        stageClasses = new HashSet<>();
        registerStageClass(ConvertColor.class);
        registerStageClass(LoadImage.class);
        registerStageClass(SaveImage.class);
        registerStageClass(EdgeDetectCanny.class);
        registerStageClass(BlurGaussian.class);
    }

    private final static Set<Class<? extends CvStage>> stageClasses;

    private final CvPipeline pipeline;
    private PipelinePanel pipelinePanel;
    private ResultsPanel resultsPanel;

    public CvPipelineEditor(CvPipeline pipeline) {
        this.pipeline = pipeline;

        setLayout(new BorderLayout(0, 0));

        JSplitPane inputAndOutputSplitPane = new JSplitPane();
        inputAndOutputSplitPane.setContinuousLayout(true);
        add(inputAndOutputSplitPane, BorderLayout.CENTER);

        pipelinePanel = new PipelinePanel(this);
        inputAndOutputSplitPane.setLeftComponent(pipelinePanel);
        resultsPanel = new ResultsPanel(this);
        inputAndOutputSplitPane.setRightComponent(resultsPanel);

        addHierarchyListener(new HierarchyListener() {
            @Override
            public void hierarchyChanged(HierarchyEvent e) {
                inputAndOutputSplitPane.setDividerLocation(0.25);
            }
        });
        
        process();
    }
    
    public CvPipeline getPipeline() {
        return pipeline;
    }
    
    public void process() {
        try {
            getPipeline().process();
            resultsPanel.refresh();
        }
        catch (Exception e) {
            MessageBoxes.errorBox(getTopLevelAncestor(), "Pipeline Processing Error", e);
        }
    }
    
    public void stageSelected(CvStage stage) {
        resultsPanel.setSelectedStage(stage);
    }
    
    public static Set<Class<? extends CvStage>> getStageClasses() {
        return Collections.unmodifiableSet(stageClasses);
    }

    public static void registerStageClass(Class<? extends CvStage> cls) {
        stageClasses.add(cls);
    }

    public static void main(String[] args) throws Exception {
        // http://developer.apple.com/library/mac/#documentation/Java/Conceptual/Java14Development/07-NativePlatformIntegration/NativePlatformIntegration.html#//apple_ref/doc/uid/TP40001909-212952-TPXREF134
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception e) {
            throw new Error(e);
        }

        CvPipeline pipeline = new CvPipeline();
        pipeline.fromXmlString("<cv-pipeline>\n   <stages>\n      <cv-stage class=\"org.openpnp.vision.pipeline.stages.LoadImage\" name=\"0\" file=\"/Users/jason/Desktop/t.png\"/>\n      <cv-stage class=\"org.openpnp.vision.pipeline.stages.ConvertColor\" name=\"1\" conversion=\"Bgr2Gray\"/>\n      <cv-stage class=\"org.openpnp.vision.pipeline.stages.BlurGaussian\" name=\"3\" kernel-size=\"21\"/>\n      <cv-stage class=\"org.openpnp.vision.pipeline.stages.EdgeDetectCanny\" name=\"4\" threshold-1=\"40.0\" threshold-2=\"180.0\"/>\n            <cv-stage class=\"org.openpnp.vision.pipeline.stages.SaveImage\" name=\"2\" file=\"/Users/jason/Desktop/t_gray.png\"/>\n   </stages>\n</cv-pipeline>");

        JFrame frame = new JFrame("CvPipelineEditor");
        frame.setSize(1024, 768);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(new CvPipelineEditor(pipeline));
        frame.setVisible(true);
    }
}
