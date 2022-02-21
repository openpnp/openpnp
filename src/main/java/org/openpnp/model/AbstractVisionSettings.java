package org.openpnp.model;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.codec.digest.DigestUtils;
import org.jdesktop.beansbinding.Converter;
import org.openpnp.machine.reference.vision.AbstractPartSettingsHolder;
import org.openpnp.machine.reference.vision.ReferenceFiducialLocator;
import org.openpnp.spi.Machine;
import org.openpnp.spi.PartAlignment;
import org.openpnp.spi.VisionSettings;
import org.openpnp.util.XmlSerialize;
import org.openpnp.vision.pipeline.CvPipeline;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Serializer;

public abstract class AbstractVisionSettings extends AbstractModelObject implements VisionSettings {
    public static final String STOCK_BOTTOM_ID = "BVS_Stock";
    public static final String STOCK_FIDUCIAL_ID = "FVS_Stock";
    public static final String DEFAULT_BOTTOM_ID = "BVS_Default";
    public static final String DEFAULT_FIDUCIAL_ID = "FVS_Default";

    @Attribute
    private String id;

    @Attribute(required = false)
    private String name;

    @Attribute
    protected boolean enabled;

    @Element
    private CvPipeline cvPipeline;

    @ElementMap(required = false)
    private Map<String, Object> pipelineParameterAssignments;

    protected AbstractVisionSettings() {
    }

    protected AbstractVisionSettings(String id) {
        this.id = id;
        this.cvPipeline = new CvPipeline();
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        Object oldValue = this.name;
        this.name = name;
        firePropertyChange("name", oldValue, name);
    }

    public CvPipeline getPipeline() {
        if (cvPipeline == null) {
            cvPipeline = new CvPipeline();
        }

        return cvPipeline;
    }

    public void setPipeline(CvPipeline cvPipeline) {
        this.cvPipeline = cvPipeline;
        firePropertyChange("pipeline", null, cvPipeline);
    }

    public Map<String, Object> getPipelineParameterAssignments() {
        return pipelineParameterAssignments;
    }

    public void setPipelineParameterAssignments(Map<String, Object> pipelineParameterAssignments) {
        this.pipelineParameterAssignments = pipelineParameterAssignments;
        firePropertyChange("pipelineParameterAssignments", null, pipelineParameterAssignments);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        Object oldValue = this.enabled;
        this.enabled = enabled;
        firePropertyChange("enabled", oldValue, enabled);
    }

    public String toString() {
        return getName();
    }

    public static void fireUsedInProperty(AbstractVisionSettings settings) {
        if (settings != null) {
            settings.firePropertyChange("usedBottomVisionIn", null, settings.getUsedBottomVisionIn());
            settings.firePropertyChange("usedFiducialVisionIn", null, settings.getUsedFiducialVisionIn());
        }
    }

    /**
     * This is a pseudo PartSettingsHolder for stock settings. It provides an implicit reference to the 
     * stock settings to the stock settings cannot be deleted etc. 
     *
     */
    class StockSettingsHolder implements PartSettingsHolder {
        @Override
        public String getId() {
            return getName();
        }

        @Override
        public BottomVisionSettings getBottomVisionSettings() {
            if (AbstractVisionSettings.this instanceof BottomVisionSettings) {
                return (BottomVisionSettings) AbstractVisionSettings.this;
            }
            return null;
        }

        @Override
        public void setBottomVisionSettings(BottomVisionSettings visionSettings) {
        }

        @Override
        public List<PartSettingsHolder> getSpecializedBottomVisionIn() {
            return null;
        }

        @Override
        public void generalizeBottomVisionSettings() {
        }

        @Override
        public FiducialVisionSettings getFiducialVisionSettings() {
            if (AbstractVisionSettings.this instanceof FiducialVisionSettings) {
                return (FiducialVisionSettings) AbstractVisionSettings.this;
            }
            return null;
        }

        @Override
        public void setFiducialVisionSettings(FiducialVisionSettings visionSettings) {
        }

        @Override
        public List<PartSettingsHolder> getSpecializedFiducialVisionIn() {
            return null;
        }

        @Override
        public void generalizeFiducialVisionSettings() {
        }
    }

    /**
     * @return the list of PartSettingsHolder that have these bottom vision settings assigned.
     */
    protected List<PartSettingsHolder> getUsedIn(Function<PartSettingsHolder, AbstractVisionSettings> propertyGetter) {
        List<PartSettingsHolder> list = new ArrayList<>();
        if (isStockSetting()) {
            list.add(new StockSettingsHolder());
        }
        Configuration configuration = Configuration.get();
        if (configuration != null) {
            Machine machine = configuration.getMachine();
            if (machine != null) {
                for (PartAlignment partAlignment : machine.getPartAlignments()) {
                    if (propertyGetter.apply(partAlignment) == this) {
                        list.add(partAlignment);
                    }
                }
                ReferenceFiducialLocator fiducialLocator = ReferenceFiducialLocator.getDefault();
                if (propertyGetter.apply(fiducialLocator) == this) {
                    list.add(fiducialLocator);
                }
            }

            for (Package pkg : configuration.getPackages()) {
                if (propertyGetter.apply(pkg) == this) {
                    list.add(pkg);
                }
            }

            for (Part part : configuration.getParts()) {
                if (propertyGetter.apply(part) == this) {
                    list.add(part);
                }
            }
        }
        list.sort(new AbstractPartSettingsHolder.PartSettingsComparator());
        return list;
    }

    public List<PartSettingsHolder> getUsedBottomVisionIn() {
        return getUsedIn((h) -> h.getBottomVisionSettings());
    }

    public List<PartSettingsHolder> getUsedFiducialVisionIn() {
        return getUsedIn((h) -> h.getFiducialVisionSettings());
    }

    public static class ListConverter extends Converter<List<PartSettingsHolder>, String> {
        private final PartSettingsHolder settingsHolder;
        private final boolean html;

        public ListConverter(boolean html) {
            this.settingsHolder = null;
            this.html = html;
        }
        public ListConverter(boolean html, PartSettingsHolder settingsHolder) {
            this.settingsHolder = settingsHolder;
            this.html = html;
        }

        @Override
        public String convertForward(List<PartSettingsHolder> arg0) {
            StringBuilder str = new StringBuilder();
            if (html) {
                str.append("<html>");
            }
            int n = 0;
            for (PartSettingsHolder item : arg0) {
                if (n > 0) {
                    str.append(", ");
                }
                if (html) {
                    if (settingsHolder == item) {
                        str.append("<strong>");
                        str.append(XmlSerialize.escapeXml(item.getShortName()));
                        str.append("</strong>");
                    }
                    else {
                        str.append(XmlSerialize.escapeXml(item.getShortName()));
                    }
                }
                else {
                    str.append(item.getShortName());
                }
                n++;
            }
            if (html) {
                str.append("</html>");
            }
            return str.toString();
        }

        @Override
        public List<PartSettingsHolder> convertReverse(String arg0) {
            return null; // not implemented
        }
    }

    public boolean isStockSetting() {
        return getId().contains("Stock");
    }

    public static String createSettingsFingerprint(Object partSettings) {
        Serializer serOut = XmlSerialize.createSerializer();
        StringWriter sw = new StringWriter();
        try {
            serOut.write(partSettings, sw);
        }
        catch (Exception e) {
        }
        String serialized = sw.toString();
        if (partSettings instanceof AbstractVisionSettings) {
            // Must filter out the id.
            for (java.lang.reflect.Field field : AbstractVisionSettings.class.getDeclaredFields()) {
                if (field.getName().equals("id")) {
                    serialized = XmlSerialize.purgeFieldXml(serialized, field);
                }
                else if (field.getName().equals("name")) {
                    serialized = XmlSerialize.purgeFieldXml(serialized, field);
                }
            }
        }
        String partSettingsSerializedHash = DigestUtils.shaHex(serialized);
        return partSettingsSerializedHash;
    }
}
