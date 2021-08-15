package org.openpnp.vision.pipeline;

import java.awt.Image;
import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.EventSetDescriptor;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.util.List;

import org.opencv.core.Mat;
import org.openpnp.model.LengthUnit;
import org.openpnp.vision.pipeline.ui.PipelinePropertySheetTable;
import org.openpnp.vision.FluentCv;
import org.openpnp.vision.FluentCv.ColorSpace;
import org.simpleframework.xml.Attribute;

/**
 * Base class for a stage in a CvPipeline. A CvStage has a unique name within a pipeline and is able
 * to perform computer vision operations resulting in either a modified working image or a new image
 * and optional model data extracted from the image.
 */
public abstract class CvStage {
    @Attribute
    private String name;

    @Attribute(required = false)
    private boolean enabled = true;
    
    /**
     * Perform an operation in a pipeline. Typical implementations will call
     * CvPipeline#getWorkingImage(), perform some type of operation on the image and will return a
     * Result containing a modified image and model data about features found in the image.
     *
     * If the stage only modifies the working image, it is sufficient to just return null, and this
     * will typically be the most common case.
     * 
     * @param pipeline
     * @return Null or a Result object containing an optional image and optional model. If the
     *         return value is null the pipeline will store a copy of the working image as the
     *         result for this stage. Otherwise it will set the working image to the result image
     *         and store the result image.
     * @throws Exception
     */
    public abstract Result process(CvPipeline pipeline) throws Exception;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCategory() {
        try {
            Stage a = getClass().getAnnotation(Stage.class);
            return a.category();
        }
        catch (Exception e) {
            return null;
        }
    }

    public String getDescription() {
        try {
            Stage a = getClass().getAnnotation(Stage.class);
            return a.description();
        }
        catch (Exception e) {
            return null;
        }
    }

    public String getDescription(String propertyName) {
        try {
            Property a = getClass().getDeclaredField(propertyName).getAnnotation(Property.class);
            return a.description();
        }
        catch (Exception e) {
            return null;
        }
    }

    // a stage may optionally define a length unit which is handled in the pipeline editor's 
    // ResultsPanel.matView
    public LengthUnit getLengthUnit() {
        return null;
    }

    public BeanInfo getBeanInfo() {
        return new CvStageBeanInfo();
    }
    
    public class CvStageBeanInfo implements BeanInfo {
        private final BeanInfo beanInfo;

        public CvStageBeanInfo() {
            try {
                beanInfo = Introspector.getBeanInfo(CvStage.this.getClass(), CvStage.class);
            }
            catch (Exception e) {
                throw new Error(e);
            }
        }

        @Override
        public BeanDescriptor getBeanDescriptor() {
            BeanDescriptor bd = beanInfo.getBeanDescriptor();
            if (bd == null) {
                bd = new BeanDescriptor(CvStage.this.getClass());
            }
            bd.setShortDescription(CvStage.this.getDescription());
            return bd;
        }

        @Override
        public EventSetDescriptor[] getEventSetDescriptors() {
            return null;
        }

        @Override
        public int getDefaultEventIndex() {
            return 0;
        }

        @Override
        public PropertyDescriptor[] getPropertyDescriptors() {
            PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor pd : pds) {
                pd.setShortDescription(CvStage.this.getDescription(pd.getName()));
            }
            return pds;
        }

        @Override
        public int getDefaultPropertyIndex() {
            return 0;
        }

        @Override
        public MethodDescriptor[] getMethodDescriptors() {
            return null;
        }

        @Override
        public BeanInfo[] getAdditionalBeanInfo() {
            return null;
        }

        @Override
        public Image getIcon(int iconKind) {
            return null;
        }
    }

    public static class Result {
        final public CvStage stage;
        final public Mat image;
        final public Object model;
        final public long processingTimeNs;
        final public ColorSpace colorSpace;

        public Result(Mat image, ColorSpace colorSpace, Object model, long processingTimeNs, CvStage stage) {
            this.image = image;
            this.model = model;
            this.processingTimeNs = processingTimeNs;
            this.stage = stage;
            this.colorSpace = colorSpace;
        }

        public Result(Mat image, Object model, long processingTimeNs) {
            this(image, null, model, processingTimeNs, null);
        }

        public Result(Mat image, ColorSpace colorSpace, Object model) {
            this(image, colorSpace, model, 0, null);
        }

        public Result(Mat image, Object model) {
            this(image, null, model);
        }

        public Result(Mat image, ColorSpace colorSpace) {
            this(image, colorSpace, null);
        }

        public Result(Mat image) {
            this(image, null);
        }
        
        public Mat getImage() {
            return image;
        }
        
        public Object getModel() {
            return model;
        }

        public ColorSpace getColorSpace() {
            return colorSpace;
        }

        public CvStage getStage() {
            return stage;
        }

        public String getName() {
            if (stage != null) {
                return stage.getName();
            }
            return "";
        }

        @SuppressWarnings("unchecked")
        public <T> T getExpectedModel(Class<T> expectedModelClass) throws Exception {
            // Due to type erasure we need to pass the class as well.
            T typedModel = null;
            if (model == null) {
                throw new Exception("Pipeline stage \""+getName()+"\" returned no "+expectedModelClass.getSimpleName()+".");
            }
            if (expectedModelClass.isInstance(model)) {
                typedModel = (T)model;
            }
            if (typedModel == null) {
                throw new Exception("Pipeline stage \""+getName()+"\" returned a "+model.getClass().getSimpleName()+" but expected a "+expectedModelClass.getSimpleName()+".");
            }
            return typedModel;
        }

        @SuppressWarnings("unchecked")
        public <T> List<T> getExpectedListModel(Class<T> expectedElementClass, Exception emptyException) throws Exception {
            @SuppressWarnings("rawtypes")
            List list = getExpectedModel(List.class);
            if (list.size() == 0) {
                if (emptyException != null) {
                    throw emptyException;
                }
                return (List<T>)list;
            }
            if (!expectedElementClass.isInstance(list.get(0))) {
                throw new Exception("Pipeline stage \""+getName()+"\" returned a "+list.get(0).getClass().getSimpleName()+" list but expected a "+expectedElementClass.getSimpleName()+" list.");
            }
            return (List<T>)list;
        }

        public static class Circle {
            public double x;
            public double y;
            public double diameter;

            public Circle(double x, double y, double diameter) {
                this.x = x;
                this.y = y;
                this.diameter = diameter;
            }
            
            public double getX() {
                return x;
            }

            public void setX(double x) {
                this.x = x;
            }

            public double getY() {
                return y;
            }

            public void setY(double y) {
                this.y = y;
            }

            public double getDiameter() {
                return diameter;
            }

            public void setDiameter(double diameter) {
                this.diameter = diameter;
            }

            @Override
            public String toString() {
                return "Circle [x=" + x + ", y=" + y + ", diameter=" + diameter + "]";
            }
        }

        public static class TemplateMatch {
            public double x;
            public double y;
            public double width;
            public double height;
            public double score;

            public TemplateMatch(double x, double y, double width, double height, double score) {
                this.x = x;
                this.y = y;
                this.width = width;
                this.height = height;
                this.score = score;
            }
            
            public double getX() {
                return x;
            }

            public void setX(double x) {
                this.x = x;
            }

            public double getY() {
                return y;
            }

            public void setY(double y) {
                this.y = y;
            }

            public double getWidth() {
                return width;
            }

            public void setWidth(double width) {
                this.width = width;
            }

            public double getHeight() {
                return height;
            }

            public void setHeight(double height) {
                this.height = height;
            }

            public double getScore() {
                return score;
            }

            public void setScore(double score) {
                this.score = score;
            }

            @Override
            public String toString() {
                return "TemplateMatch [x=" + x + ", y=" + y + ", width=" + width + ", height="
                        + height + ", score=" + score + "]";
            }
        }
    }

    /**
     * Stages can override to register customized PropertyEditors.
     * 
     * @param table
     * @param pipeline
     */
    public void customizePropertySheet(PipelinePropertySheetTable table, CvPipeline pipeline) {
    }
}
