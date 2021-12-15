package org.openpnp.model;

import java.util.ArrayList;
import java.util.List;

import org.openpnp.machine.reference.vision.ReferenceBottomVision;
import org.openpnp.spi.Machine;
import org.openpnp.spi.PartAlignment;
import org.openpnp.spi.VisionSettings;
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

    public List<String> getUsedIn() {
        List<String> usedIn = new ArrayList<>();
        if (getId().equals(STOCK_ID)) {
            usedIn.add(getName());
        }
        Configuration configuration = Configuration.get();
        if (configuration != null) {
            Machine machine = configuration.getMachine();
            if (machine != null) {
                for (PartAlignment partAlignment : machine.getPartAlignments()) {
                    if (partAlignment instanceof ReferenceBottomVision) {
                        if (((ReferenceBottomVision) partAlignment).getVisionSettings() == this) {
                            usedIn.add(partAlignment.getClass().getSimpleName());
                        }
                    }
                }
            }

            for (Package pkg : configuration.getPackages()) {
                if (pkg.getVisionSettings() == this) {
                    usedIn.add(pkg.getId());
                }
            }

            for (Part part : configuration.getParts()) {
                if (part.getVisionSettings() == this) {
                    usedIn.add(part.getId());
                }
            }
        }
        usedIn.sort(null);
        return usedIn;
    }

    public boolean isStockSetting() {
        return getId().equals(STOCK_ID);
    }
}
