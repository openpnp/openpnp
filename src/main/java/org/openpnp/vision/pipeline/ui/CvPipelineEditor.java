package org.openpnp.vision.pipeline.ui;

import java.awt.BorderLayout;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.stages.BlurGaussian;
import org.openpnp.vision.pipeline.stages.BlurMedian;
import org.openpnp.vision.pipeline.stages.ClosestModel;
import org.openpnp.vision.pipeline.stages.Add;
import org.openpnp.vision.pipeline.stages.ComposeResult;
import org.openpnp.vision.pipeline.stages.ConvertColor;
import org.openpnp.vision.pipeline.stages.ConvertModelToKeyPoints;
import org.openpnp.vision.pipeline.stages.ConvertModelToPoints;
import org.openpnp.vision.pipeline.stages.CreateFootprintTemplateImage;
import org.openpnp.vision.pipeline.stages.CreateModelTemplateImage;
import org.openpnp.vision.pipeline.stages.DetectCirclesHough;
import org.openpnp.vision.pipeline.stages.DetectEdgesCanny;
import org.openpnp.vision.pipeline.stages.DetectEdgesLaplacian;
import org.openpnp.vision.pipeline.stages.DetectEdgesRobertsCross;
import org.openpnp.vision.pipeline.stages.DetectFixedCirclesHough;
import org.openpnp.vision.pipeline.stages.DilateModel;
import org.openpnp.vision.pipeline.stages.DrawCircles;
import org.openpnp.vision.pipeline.stages.DrawContours;
import org.openpnp.vision.pipeline.stages.DrawImageCenter;
import org.openpnp.vision.pipeline.stages.DrawKeyPoints;
import org.openpnp.vision.pipeline.stages.DrawRotatedRects;
import org.openpnp.vision.pipeline.stages.DrawTemplateMatches;
import org.openpnp.vision.pipeline.stages.FilterContours;
import org.openpnp.vision.pipeline.stages.FilterRects;
import org.openpnp.vision.pipeline.stages.FindContours;
import org.openpnp.vision.pipeline.stages.GrabCut;
import org.openpnp.vision.pipeline.stages.HistogramEqualize;
import org.openpnp.vision.pipeline.stages.ImageCapture;
import org.openpnp.vision.pipeline.stages.ImageRead;
import org.openpnp.vision.pipeline.stages.ImageRecall;
import org.openpnp.vision.pipeline.stages.ImageWrite;
import org.openpnp.vision.pipeline.stages.ImageWriteDebug;
import org.openpnp.vision.pipeline.stages.MaskCircle;
import org.openpnp.vision.pipeline.stages.MaskHsv;
import org.openpnp.vision.pipeline.stages.MaskModel;
import org.openpnp.vision.pipeline.stages.MaskPolygon;
import org.openpnp.vision.pipeline.stages.MaskRectangle;
import org.openpnp.vision.pipeline.stages.MatchPartTemplate;
import org.openpnp.vision.pipeline.stages.MatchTemplate;
import org.openpnp.vision.pipeline.stages.MinAreaRect;
import org.openpnp.vision.pipeline.stages.MinAreaRectContours;
import org.openpnp.vision.pipeline.stages.Normalize;
import org.openpnp.vision.pipeline.stages.OrientRotatedRects;
import org.openpnp.vision.pipeline.stages.ReadModelProperty;
import org.openpnp.vision.pipeline.stages.ReadPartTemplateImage;
import org.openpnp.vision.pipeline.stages.Rotate;
import org.openpnp.vision.pipeline.stages.ScriptRun;
import org.openpnp.vision.pipeline.stages.SetColor;
import org.openpnp.vision.pipeline.stages.SimpleBlobDetector;
import org.openpnp.vision.pipeline.stages.Threshold;
import org.openpnp.vision.pipeline.stages.ThresholdAdaptive;
import org.openpnp.vision.pipeline.stages.WritePartTemplateImage;

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
        registerStageClass(DetectCirclesHough.class);
        registerStageClass(DetectEdgesCanny.class);
        registerStageClass(DetectEdgesRobertsCross.class);
        registerStageClass(DetectEdgesLaplacian.class);
        registerStageClass(DetectFixedCirclesHough.class);
        registerStageClass(DilateModel.class);
        registerStageClass(DrawCircles.class);
        registerStageClass(DrawContours.class);
        registerStageClass(DrawImageCenter.class);
        registerStageClass(DrawKeyPoints.class);
        registerStageClass(DrawRotatedRects.class);
        registerStageClass(DrawTemplateMatches.class);
        registerStageClass(FilterContours.class);
        registerStageClass(FilterRects.class);
        registerStageClass(FindContours.class);
        registerStageClass(GrabCut.class);
        registerStageClass(HistogramEqualize.class);
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
        registerStageClass(MinAreaRect.class);
        registerStageClass(MinAreaRectContours.class);
        registerStageClass(Normalize.class);
        registerStageClass(OrientRotatedRects.class);
        registerStageClass(ReadModelProperty.class);
        registerStageClass(ReadPartTemplateImage.class);
        registerStageClass(Rotate.class);
        registerStageClass(SetColor.class);
        registerStageClass(ScriptRun.class);
        registerStageClass(SimpleBlobDetector.class);
        registerStageClass(Threshold.class);
        registerStageClass(ThresholdAdaptive.class);
        registerStageClass(WritePartTemplateImage.class);
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
        getPipeline().process();
        resultsPanel.refresh();
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
}
