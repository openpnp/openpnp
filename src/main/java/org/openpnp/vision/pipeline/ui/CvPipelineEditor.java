package org.openpnp.vision.pipeline.ui;

import java.awt.BorderLayout;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.openpnp.util.UiUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.stages.*;

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
        registerStageClass(BlurMedian.class);
        registerStageClass(BlurGaussian.class);
        registerStageClass(ClosestModel.class);
        registerStageClass(Add.class);
        registerStageClass(ComposeResult.class);
        registerStageClass(ConvertColor.class);
        registerStageClass(ConvertModelToPoints.class);
        registerStageClass(ConvertModelToKeyPoints.class);
        registerStageClass(CreateFootprintTemplateImage.class);
        registerStageClass(CreateModelTemplateImage.class);
        registerStageClass(CreateShapeTemplateImage.class);
        registerStageClass(DetectCirclesHough.class);
        registerStageClass(DetectLinesHough.class);
        registerStageClass(DetectRectangleHough.class);
        registerStageClass(DetectEdgesCanny.class);
        registerStageClass(DetectEdgesRobertsCross.class);
        registerStageClass(DetectEdgesLaplacian.class);
        registerStageClass(DetectFixedCirclesHough.class);
        registerStageClass(DetectCircularSymmetry.class);
        registerStageClass(DilateModel.class);
        registerStageClass(DrawCircles.class);
        registerStageClass(DrawContours.class);
        registerStageClass(DrawImageCenter.class);
        registerStageClass(DrawKeyPoints.class);
        registerStageClass(DrawRotatedRects.class);
        registerStageClass(DrawEllipses.class);
        registerStageClass(DrawTemplateMatches.class);
        registerStageClass(FilterContours.class);
        registerStageClass(FilterRects.class);
        registerStageClass(FindContours.class);
        registerStageClass(GrabCut.class);
        registerStageClass(HistogramEqualize.class);
        registerStageClass(HistogramEqualizeAdaptive.class);
        registerStageClass(ImageCapture.class);
        registerStageClass(ImageRead.class);
        registerStageClass(ImageRecall.class);
        registerStageClass(ImageWrite.class);
        registerStageClass(ImageWriteDebug.class);
        registerStageClass(MaskCircle.class);
        registerStageClass(MaskHsv.class);
        registerStageClass(MaskModel.class);
        registerStageClass(MaskPolygon.class);
        registerStageClass(MaskRectangle.class);
        registerStageClass(MatchTemplate.class);
        registerStageClass(MatchPartTemplate.class);
        registerStageClass(MatchPartsTemplate.class);
        registerStageClass(MinAreaRect.class);
        registerStageClass(MinAreaRectContours.class);
        registerStageClass(MinEnclosingCircle.class);
        registerStageClass(FitEllipseContours.class);
        registerStageClass(Normalize.class);
        registerStageClass(OrientRotatedRects.class);
        registerStageClass(ReadModelProperty.class);
        registerStageClass(ReadPartTemplateImage.class);
        registerStageClass(Rotate.class);
        registerStageClass(SelectSingleRect.class);
        registerStageClass(SetColor.class);
        registerStageClass(ScriptRun.class);
        registerStageClass(SimpleBlobDetector.class);
        registerStageClass(SizeCheck.class);
        registerStageClass(Threshold.class);
        registerStageClass(ThresholdAdaptive.class);
        registerStageClass(WritePartTemplateImage.class);
        registerStageClass(ActuatorWrite.class);
        registerStageClass(AffineWarp.class);
        registerStageClass(AffineUnwarp.class);
        registerStageClass(SimpleOcr.class);
        
    }

    private final static Set<Class<? extends CvStage>> stageClasses;

    private final CvPipeline pipeline;
    private PipelinePanel pipelinePanel;
    private ResultsPanel resultsPanel;
    
    private String originalVersion = "";

    public CvPipelineEditor(CvPipeline pipeline) {
        this.pipeline = pipeline;
        try {
            originalVersion = pipeline.toXmlString();
        }
        catch (Exception e1) {
            // Do nothing
        }
        
        setLayout(new BorderLayout(0, 0));

        JSplitPane inputAndOutputSplitPane = new JSplitPane();
        inputAndOutputSplitPane.setContinuousLayout(true);
        add(inputAndOutputSplitPane, BorderLayout.CENTER);

        resultsPanel = new ResultsPanel(this);
        inputAndOutputSplitPane.setRightComponent(resultsPanel);
        pipelinePanel = new PipelinePanel(this);
        inputAndOutputSplitPane.setLeftComponent(pipelinePanel);
        
        addHierarchyListener(new HierarchyListener() {
            @Override
            public void hierarchyChanged(HierarchyEvent e) {
                inputAndOutputSplitPane.setDividerLocation(0.25);
            }
        });
        
        process();
    }
    
    public void initializeFocus() {
        pipelinePanel.initializeFocus();    	
    }
    
    public CvPipeline getPipeline() {
        return pipeline;
    }

    public void process() {
        UiUtils.messageBoxOnException(() -> getPipeline().process());
        resultsPanel.refresh();
    }

    public void stageSelected(CvStage stage) {
        resultsPanel.setSelectedStage(stage);
    }

    public boolean isDirty( ) {
        String editedVersion = "";
        try {
            editedVersion = pipeline.toXmlString();
        }
        catch (Exception e) {
            // Do nothing
        }
        return !editedVersion.equals(originalVersion);
    }
    
    public void undoEdits() {
        try {
            pipeline.fromXmlString(originalVersion);
        }
        catch (Exception e) {
            // Do nothing
        }
    }
    
    public static Set<Class<? extends CvStage>> getStageClasses() {
        return Collections.unmodifiableSet(stageClasses);
    }

    public static void registerStageClass(Class<? extends CvStage> cls) {
        stageClasses.add(cls);
    }
}
