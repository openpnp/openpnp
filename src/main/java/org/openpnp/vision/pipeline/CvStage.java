package org.openpnp.vision.pipeline;

import java.awt.Image;
import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.EventSetDescriptor;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;

import org.opencv.core.Mat;
import org.simpleframework.xml.Attribute;

/**
 * Base class for a stage in a CvPipeline. A CvStage has a unique name within a pipeline and is able
 * to perform computer vision operations reuslting in either a modified working image or a new image
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
        final public Mat image;
        final public Object model;
        final public long processingTimeNs;

        public Result(Mat image, Object model, long processingTimeNs) {
            this.image = image;
            this.model = model;
            this.processingTimeNs = processingTimeNs;
        }

        public Result(Mat image, Object model) {
            this(image, model, 0);
        }

        public Result(Mat image) {
            this(image, null, 0);
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

            @Override
            public String toString() {
                return "TemplateMatch [x=" + x + ", y=" + y + ", width=" + width + ", height="
                        + height + ", score=" + score + "]";
            }
        }
    }
}
