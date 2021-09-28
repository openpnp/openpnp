package org.openpnp.vision.pipeline.ui;

import java.awt.BorderLayout;
import java.util.*;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.openpnp.model.*;
import org.openpnp.util.UiUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.stages.ActuatorWrite;
import org.openpnp.vision.pipeline.stages.Add;
import org.openpnp.vision.pipeline.stages.AffineUnwarp;
import org.openpnp.vision.pipeline.stages.AffineWarp;
import org.openpnp.vision.pipeline.stages.BlurGaussian;
import org.openpnp.vision.pipeline.stages.BlurMedian;
import org.openpnp.vision.pipeline.stages.ClosestModel;
import org.openpnp.vision.pipeline.stages.ComposeResult;
import org.openpnp.vision.pipeline.stages.ConvertColor;
import org.openpnp.vision.pipeline.stages.ConvertModelToKeyPoints;
import org.openpnp.vision.pipeline.stages.ConvertModelToPoints;
import org.openpnp.vision.pipeline.stages.CreateFootprintTemplateImage;
import org.openpnp.vision.pipeline.stages.CreateModelTemplateImage;
import org.openpnp.vision.pipeline.stages.CreateShapeTemplateImage;
import org.openpnp.vision.pipeline.stages.DetectCirclesHough;
import org.openpnp.vision.pipeline.stages.DetectCircularSymmetry;
import org.openpnp.vision.pipeline.stages.DetectEdgesCanny;
import org.openpnp.vision.pipeline.stages.DetectEdgesLaplacian;
import org.openpnp.vision.pipeline.stages.DetectEdgesRobertsCross;
import org.openpnp.vision.pipeline.stages.DetectFixedCirclesHough;
import org.openpnp.vision.pipeline.stages.DetectLinesHough;
import org.openpnp.vision.pipeline.stages.DetectRectangleHough;
import org.openpnp.vision.pipeline.stages.DilateModel;
import org.openpnp.vision.pipeline.stages.DrawCircles;
import org.openpnp.vision.pipeline.stages.DrawContours;
import org.openpnp.vision.pipeline.stages.DrawEllipses;
import org.openpnp.vision.pipeline.stages.DrawImageCenter;
import org.openpnp.vision.pipeline.stages.DrawKeyPoints;
import org.openpnp.vision.pipeline.stages.DrawRotatedRects;
import org.openpnp.vision.pipeline.stages.DrawTemplateMatches;
import org.openpnp.vision.pipeline.stages.FilterContours;
import org.openpnp.vision.pipeline.stages.FilterRects;
import org.openpnp.vision.pipeline.stages.FindContours;
import org.openpnp.vision.pipeline.stages.FitEllipseContours;
import org.openpnp.vision.pipeline.stages.GrabCut;
import org.openpnp.vision.pipeline.stages.HistogramEqualize;
import org.openpnp.vision.pipeline.stages.HistogramEqualizeAdaptive;
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
import org.openpnp.vision.pipeline.stages.MatchPartsTemplate;
import org.openpnp.vision.pipeline.stages.MatchTemplate;
import org.openpnp.vision.pipeline.stages.MinAreaRect;
import org.openpnp.vision.pipeline.stages.MinAreaRectContours;
import org.openpnp.vision.pipeline.stages.Normalize;
import org.openpnp.vision.pipeline.stages.OrientRotatedRects;
import org.openpnp.vision.pipeline.stages.ReadModelProperty;
import org.openpnp.vision.pipeline.stages.ReadPartTemplateImage;
import org.openpnp.vision.pipeline.stages.Rotate;
import org.openpnp.vision.pipeline.stages.ScriptRun;
import org.openpnp.vision.pipeline.stages.SelectSingleRect;
import org.openpnp.vision.pipeline.stages.SetColor;
import org.openpnp.vision.pipeline.stages.SimpleBlobDetector;
import org.openpnp.vision.pipeline.stages.SimpleOcr;
import org.openpnp.vision.pipeline.stages.SizeCheck;
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

    private static final Set<Class<? extends CvStage>> stageClasses;

    private final Pipeline pipeline;
    private PipelinePanel pipelinePanel;
    private ResultsPanel resultsPanel;
    
    private String originalVersion = "";
    private Set<String> partsOriginal;
    private HashMap<String, String> partsPipelines = new HashMap<>();
    private Set<String> packagesOriginal;
    private HashMap<String, String> packagesPipelines = new HashMap<>();

    public CvPipelineEditor(CvPipeline pipeline) {
        this(null, pipeline, false);
    }

    public CvPipelineEditor(Pipeline pipeline, boolean tabs) {
        this(pipeline, pipeline.getCvPipeline(), tabs);
    }

    public CvPipelineEditor(Pipeline pipeline, CvPipeline cvPipeline, boolean tabs) {
        if (pipeline == null) {
            //TODO NK: investigate where is this coming from and avoid it
            this.pipeline = new Pipeline("To be deleted");
            this.pipeline.setCvPipeline(cvPipeline);
        } else {
            this.pipeline = pipeline;
        }

        setPipelinesMap();

        try {
            originalVersion = this.pipeline.getCvPipeline().toXmlString();
            partsOriginal = getParts();
            packagesOriginal = getPackages();
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
        pipelinePanel = new PipelinePanel(this, tabs);
        inputAndOutputSplitPane.setLeftComponent(pipelinePanel);
        
        addHierarchyListener(e -> inputAndOutputSplitPane.setDividerLocation(0.25));
        
        process();
    }
    
    public void initializeFocus() {
        pipelinePanel.initializeFocus();    	
    }
    
    public Pipeline getUpperPipeline() {
        return pipeline;
    }

    public CvPipeline getPipeline() {
        return pipeline.getCvPipeline();
    }

    public void process() {
        UiUtils.messageBoxOnException(() -> pipeline.getCvPipeline().process());
        resultsPanel.refresh();
    }

    public void stageSelected(CvStage stage) {
        resultsPanel.setSelectedStage(stage);
    }

    private void setPipelinesMap() {
        Configuration.get().getParts().forEach(part -> partsPipelines.put(part.getId(), part.getPipeline().getId()));
        Configuration.get().getPackages().forEach(pkg -> packagesPipelines.put(pkg.getId(), pkg.getPipeline().getId()));
    }

    private Set<String> getParts() {
        Set<String> result = new HashSet<>();
        Configuration.get().getParts().forEach(part -> {
            if (part.getPipeline().getId() != null && part.getPipeline().getId().equals(pipeline.getId())) {
                result.add(part.getId());
            }
        });

        return result;
    }

    private Set<String> getPackages() {
        Set<String> result = new HashSet<>();
        Configuration.get().getPackages().forEach(pkg -> {
            if (pkg.getPipeline().getId() != null && pkg.getPipeline().getId().equals(pipeline.getId())) {
                result.add(pkg.getId());
            }
        });

        return result;
    }

    public boolean isDirty( ) {
        String editedVersion = "";
        Set<String> partsEdited = new HashSet<>();
        Set<String> packagesEdited = new HashSet<>();
        try {
            editedVersion = pipeline.getCvPipeline().toXmlString();
            partsEdited = getParts();
            packagesEdited = getPackages();
        }
        catch (Exception e) {
            // Do nothing
        }
        return !editedVersion.equals(originalVersion) ||
                !partsEdited.equals(partsOriginal) ||
                !packagesEdited.equals(packagesOriginal);
    }
    
    public void undoEdits() {
        try {
            pipeline.getCvPipeline().fromXmlString(originalVersion);
            restorePipelines();
        }
        catch (Exception e) {
            // Do nothing
        }
    }

    private void restorePipelines() {
        //TODO NK: leave that to Configuration?
        partsPipelines.forEach((k,v) -> {
            Configuration.get().getPart(k).setPipeline(Configuration.get().getPipeline(v));
        });

        packagesPipelines.forEach((k,v) -> {
            Configuration.get().getPackage(k).setPipeline(Configuration.get().getPipeline(v));
        });
    }
    
    public static Set<Class<? extends CvStage>> getStageClasses() {
        return Collections.unmodifiableSet(stageClasses);
    }

    public static void registerStageClass(Class<? extends CvStage> cls) {
        stageClasses.add(cls);
    }
}
