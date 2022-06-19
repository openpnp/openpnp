package org.openpnp.vision.pipeline;

import java.awt.Image;
import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.EventSetDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opencv.core.Mat;
import org.openpnp.model.Area;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.FluentCv.ColorSpace;
import org.openpnp.vision.pipeline.ui.PipelinePropertySheetTable;
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

    private Map<String, Object> propertyOverrides;

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

    void processPrepare(CvPipeline cvPipeline) {
        // Reset any property overrides.
        propertyOverrides = null;
    }

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

    private Object getPropertyOverride(String name) {
        return propertyOverrides == null ? null : propertyOverrides.get(name);
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

    /**
     * @param propertyName
     * @return The @Property.description annotation of the given field name.
     */
    public String getDescription(String propertyName) {
        Class<?> cls = getClass();
        while (cls != null) {
            Field fld = null;
            try {
                fld = cls.getDeclaredField(propertyName);
                Property a = fld.getAnnotation(Property.class);
                return a.description();
            }
            catch (Exception e) {
            }
            // Also look in super classes.
            cls = cls.getSuperclass();
        }
        return null;
    }

    /**
     * @param propertyName
     * @return A declaration sequence index for the given field name. It is used for sorting the properties in the order of declaration.
     */
    private int getPropertySequence(String propertyName) {
        int index = 1000000;
        Class<?> cls = getClass();
        while (cls != null) {
            try {
                for (Field field : cls.getDeclaredFields()) {
                    index++;
                    if (field.getName().equals(propertyName)) {
                        return index;
                    }
                }
            }
            catch (SecurityException e) {
            }
            // Also look in super classes.
            cls = cls.getSuperclass();
            index = (index/1000 - 1)*1000;
        }
        return 0;
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
                String propertyName = pd.getName();
                Object overrideProperty = getPropertyOverride(propertyName);
                String description = CvStage.this.getDescription(propertyName);
                if (overrideProperty != null) {
                    try {
                        pd.setReadMethod(null);
                        pd.setWriteMethod(null);
                    }
                    catch (IntrospectionException e) {
                    }
                    description = "<strong color=\"red\">Controlled by pipeline caller: "+propertyName+"="+overrideProperty+"</strong>"
                            + "<br/><br/>"
                            +description;
                }
                pd.setShortDescription(description);
            }
            // Sort by declaration sequence. 
            Arrays.sort(pds, new Comparator<PropertyDescriptor>() {
                @Override
                public int compare(PropertyDescriptor o1,
                        PropertyDescriptor o2) {
                    return CvStage.this.getPropertySequence(o1.getName()) 
                            - CvStage.this.getPropertySequence(o2.getName());
                }
            });
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
    
    /**
     * Attempts to override a generic parameter value using a property from the pipeline.  If no
     * overriding property is found on the pipeline, the parameter passes through unchanged.
     * Handles the following conversions:
     * 
     * <pre>
     * Parameter Type(s)            Acceptable Pipeline Property Type(s)
     * -----------------------      --------------------------------------------------------
     * boolean, Boolean         <-  Boolean
     * double, Double           <-  Double, Integer, Long, Area, Length
     * int, Integer             <-  Double, Integer, Long, Area, Length
     * long, Long               <-  Double, Integer, Long, Area, Length
     * String                   <-  String
     * org.opencv.core.Point    <-  org.opencv.core.Point, org.openpnp.model.Point, Location 
     * org.openpnp.model.Point  <-  org.opencv.core.Point, org.openpnp.model.Point, Location
     * Area                     <-  Area
     * Length                   <-  Length
     * Location                 <-  Location
     * T                        <-  T (no conversion)
     * </pre>
     * 
     * Note: Doubles are rounded before assignment to integer types; Java's standard narrowing 
     * conversions are applied where necessary; Area, Length, and Location are converted to 
     * pixels via the camera's units per pixel scaling; and Locations are taken as offsets from 
     * the image center
     * 
     * @param parameter - the generic parameter value to be possibly overridden
     * @param pipeline - the pipeline with the overriding property
     * @param pipelinePropertyName - the name of the overriding pipeline property
     * @param acceptablePropertyTypes - Zero or more acceptable types for the overriding property, 
     * if none are specified, the only acceptable type is the same type as the input parameter
     * @return the overridden parameter value if a pipeline property override was found, otherwise 
     * the original parameter value
     * @throws Exception If a property is defined on the pipeline with the expected name but it 
     * could not be converted to the type of the input parameter
     */
    @SuppressWarnings("unchecked")
    public <T> T getPossiblePipelinePropertyOverride(T parameter, String stagePropertyName, CvPipeline pipeline, 
            String pipelinePropertyName, Class<?>... acceptablePropertyTypes) throws Exception {
        Object pipelineProperty = pipeline.getProperty(pipelinePropertyName);
        if (pipelineProperty == null) {
            return parameter;
        }
        else {
            
        }
        T stageProperty = getConvertedPipelineProperty(parameter, pipeline, pipelinePropertyName,
                pipelineProperty, acceptablePropertyTypes);
        recordPropertyOverride(stagePropertyName, stageProperty);
        return stageProperty;
    }

    /**
     * Record the override of a stage property. This is used to indicate it on the property editor UI.
     * 
     * @param stagePropertyName
     * @param stageProperty
     */
    public void recordPropertyOverride(String stagePropertyName, Object stageProperty) {
        if (propertyOverrides == null) {
            propertyOverrides = new HashMap<>();
        }
        propertyOverrides.put(stagePropertyName, stageProperty);
    }

    private <T> T getConvertedPipelineProperty(T parameter, CvPipeline pipeline,
            String pipelinePropertyName, Object pipelineProperty, Class<?>... acceptablePropertyTypes)
            throws Exception {
        if (acceptablePropertyTypes.length == 0) {
            acceptablePropertyTypes = new Class<?>[] {parameter.getClass()};
        }
        Camera camera = (Camera) pipeline.getProperty("camera");
        String acceptableTypeList = "";
        for (Class<?> acceptablePropertyClass : acceptablePropertyTypes) {
            if (acceptablePropertyClass.isInstance(pipelineProperty)) {
                if (parameter.getClass().isInstance(pipelineProperty)) {
                    // No conversion needed.
                    return (T) pipelineProperty;
                }
                else if (acceptablePropertyClass == Double.class) {
                    if (parameter instanceof Integer) {
                        return (T) (Integer) ((Long) Math.round((Double) pipelineProperty)).intValue();
                    }
                    if (parameter instanceof Long) {
                        return (T) (Long) Math.round((Double) pipelineProperty);
                    }
                }
                else if (acceptablePropertyClass == Integer.class) {
                    if (parameter instanceof Double) {
                        return (T) (Double) ((Integer) pipelineProperty).doubleValue();
                    }
                    if (parameter instanceof Long) {
                        return (T) (Long) ((Integer) pipelineProperty).longValue();
                    }
                }
                else if (acceptablePropertyClass == Long.class) {
                    if (parameter instanceof Double) {
                        return (T) (Double) ((Long) pipelineProperty).doubleValue();
                    }
                    if (parameter instanceof Integer) {
                        return (T) (Integer) ((Long) pipelineProperty).intValue();
                    }
                }
                else if (acceptablePropertyClass == org.opencv.core.Point.class) {
                    if (parameter instanceof org.openpnp.model.Point) {
                        return (T) org.openpnp.model.Point.fromOpencv((org.opencv.core.Point) pipelineProperty);
                    }
                }
                else if (acceptablePropertyClass == org.openpnp.model.Point.class) {
                    if (parameter instanceof org.opencv.core.Point) {
                        return (T) ((org.openpnp.model.Point) pipelineProperty).toOpencv();
                    }
                }
                else if (acceptablePropertyClass == Area.class) {
                    if (camera == null) {
                        throw new Exception("Unable to convert to pixels because pipeline property \"camera\" is not set");
                    }
                    double p = VisionUtils.toPixels((Area) pipelineProperty, camera);
                    if (parameter instanceof Double) {
                        return (T) (Double) p;
                    }
                    if (parameter instanceof Integer) {
                        return (T) (Integer) ((Long) Math.round(p)).intValue();
                    }
                    if (parameter instanceof Long) {
                        return (T) (Long) Math.round(p);
                    }
                }
                else if (acceptablePropertyClass == Length.class) {
                    if (camera == null) {
                        throw new Exception("Unable to convert to pixels because pipeline property \"camera\" is not set");
                    }
                    double p = VisionUtils.toPixels((Length) pipelineProperty, camera);
                    if (parameter instanceof Double) {
                        return (T) (Double) p;
                    }
                    if (parameter instanceof Integer) {
                        return (T) (Integer) ((Long) Math.round(p)).intValue();
                    }
                    if (parameter instanceof Long) {
                        return (T) (Long) Math.round(p);
                    }
                }
                else if (acceptablePropertyClass == Location.class) {
                    if (camera == null) {
                        throw new Exception("Unable to convert to pixels because pipeline property \"camera\" is not set");
                    }
                    org.openpnp.model.Point p = VisionUtils.getLocationPixels(camera, (Location) pipelineProperty);
                    if (parameter instanceof org.opencv.core.Point) {
                        return (T) new org.opencv.core.Point(p.x, p.y);
                    }
                    if (parameter instanceof org.openpnp.model.Point) {
                        return (T) p;
                    }
                }
                throw new Exception("Can't convert pipeline property \"" + pipelinePropertyName + "\" of type \"" + acceptablePropertyClass + "\" to type \"" + parameter.getClass() + "\"");
            }
            acceptableTypeList += (acceptableTypeList.length() != 0 ? " or \"" : "\"") + acceptablePropertyClass.getName() + "\"";
        }
        throw new Exception("Pipeline property \"" + pipelinePropertyName + "\" must be of type " + acceptableTypeList);
    }

    /**
     * Calls {@link #getPossiblePipelinePropertyOverride(Object, String, CvPipeline, String, Class...)} with the
     * stagePropertyName deducted from the pipelinePropertyName. 
     * 
     * @param <T>
     * @param parameter
     * @param pipeline
     * @param pipelinePropertyName
     * @param acceptablePropertyTypes
     * @return
     * @throws Exception
     */
    public <T> T getPossiblePipelinePropertyOverride(T parameter, CvPipeline pipeline, 
            String pipelinePropertyName, Class<?>... acceptablePropertyTypes) throws Exception {
        // Deduct the stage property name from the pipeline property name, according to convention.
        String stagePropertyName = pipelinePropertyName;
        int pos = stagePropertyName.lastIndexOf(".");
        if (pos > 0) {
            stagePropertyName = stagePropertyName.substring(pos+1);
        }
        return getPossiblePipelinePropertyOverride(parameter, stagePropertyName, pipeline, pipelinePropertyName, acceptablePropertyTypes);
    }
}
