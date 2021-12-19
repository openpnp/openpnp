package org.openpnp.model;

import java.util.ArrayList;
import java.util.List;

import org.jdesktop.beansbinding.Converter;
import org.openpnp.machine.reference.vision.AbstractPartSettingsHolder;
import org.openpnp.spi.Machine;
import org.openpnp.spi.PartAlignment;
import org.openpnp.spi.VisionSettings;
import org.openpnp.util.XmlSerialize;
import org.openpnp.vision.pipeline.CvPipeline;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public abstract class AbstractVisionSettings extends AbstractModelObject implements VisionSettings {
    public static final String STOCK_ID = "BVS_Stock";
    public static final String DEFAULT_ID = "BVS_Default";

    @Attribute()
    private String id;

    @Attribute(required = false)
    private String name;

    @Attribute
    protected boolean enabled;

    @Element()
    private CvPipeline cvPipeline;

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

    public CvPipeline getCvPipeline() {
        if (cvPipeline == null) {
            cvPipeline = new CvPipeline();
        }

        return cvPipeline;
    }

    public void setCvPipeline(CvPipeline cvPipeline) {
        Object oldValue = this.cvPipeline;
        this.cvPipeline = cvPipeline;
        firePropertyChange("cvPipeline", oldValue, cvPipeline);
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
            settings.firePropertyChange("usedIn", null, settings.getUsedIn());
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
        public BottomVisionSettings getVisionSettings() {
            return (BottomVisionSettings) AbstractVisionSettings.this;
        }

        @Override
        public void setVisionSettings(BottomVisionSettings visionSettings) {
        }

        @Override
        public PartSettingsHolder getParentHolder() {
            return null;
        }

        @Override
        public List<PartSettingsHolder> getSpecializedIn() {
            return null;
        }

        @Override
        public void resetSpecializedVisionSettings() {
        }
    }

    /**
     * @return the list of PartSettingsHolder that have these vision settings assigned.
     */
    public List<PartSettingsHolder> getUsedIn() {
        List<PartSettingsHolder> list = new ArrayList<>();
        if (getId().equals(STOCK_ID)) {
            list.add(new StockSettingsHolder());
        }
        Configuration configuration = Configuration.get();
        if (configuration != null) {
            Machine machine = configuration.getMachine();
            if (machine != null) {
                for (PartAlignment partAlignment : machine.getPartAlignments()) {
                    if (partAlignment.getVisionSettings() == this) {
                        list.add(partAlignment);
                    }
                }
            }

            for (Package pkg : configuration.getPackages()) {
                if (pkg.getVisionSettings() == this) {
                    list.add(pkg);
                }
            }

            for (Part part : configuration.getParts()) {
                if (part.getVisionSettings() == this) {
                    list.add(part);
                }
            }
        }
        list.sort(new AbstractPartSettingsHolder.PartSettingsComparator());
        return list;
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
        return getId().equals(STOCK_ID);
    }
}
