package org.openpnp.machine.reference.vision;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import org.openpnp.ConfigurationListener;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.AbstractVisionSettings;
import org.openpnp.model.BottomVisionSettings;
import org.openpnp.model.Configuration;
import org.openpnp.model.FiducialVisionSettings;
import org.openpnp.model.Package;
import org.openpnp.model.Part;
import org.openpnp.model.PartSettingsHolder;
import org.openpnp.model.PartSettingsRoot;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.core.Persist;

public abstract class AbstractPartSettingsHolder extends AbstractModelObject implements PartSettingsHolder  {
    @Attribute(required = false)
    protected String bottomVisionId;

    @Attribute(required = false)
    protected String fiducialVisionId;

    protected BottomVisionSettings bottomVisionSettings;
    protected FiducialVisionSettings fiducialVisionSettings;

    public AbstractPartSettingsHolder() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationLoaded(Configuration configuration) {
                bottomVisionSettings = (BottomVisionSettings) configuration.getVisionSettings(bottomVisionId);
                fiducialVisionSettings = (FiducialVisionSettings) configuration.getVisionSettings(fiducialVisionId);
            }
        });
    }

    @Persist
    private void persist() {
        bottomVisionId = (bottomVisionSettings == null ? null : bottomVisionSettings.getId());
        fiducialVisionId = (fiducialVisionSettings == null ? null : fiducialVisionSettings.getId());
    }

    @Override 
    public BottomVisionSettings getBottomVisionSettings() {
        return bottomVisionSettings;
    }

    @Override
    public void setBottomVisionSettings(BottomVisionSettings visionSettings) {
        BottomVisionSettings oldValue = this.bottomVisionSettings;
        this.bottomVisionId = (visionSettings == null ? null : visionSettings.getId());
        this.bottomVisionSettings = visionSettings;
        if (oldValue != visionSettings) {
            Configuration.get().fireVisionSettingsChanged();
            firePropertyChange("bottomVisionSettings", oldValue, visionSettings);
            AbstractVisionSettings.fireUsedInProperty(oldValue);
            AbstractVisionSettings.fireUsedInProperty(visionSettings);
        }
    }

    @Override 
    public FiducialVisionSettings getFiducialVisionSettings() {
        return fiducialVisionSettings;
    }

    @Override
    public void setFiducialVisionSettings(FiducialVisionSettings visionSettings) {
        FiducialVisionSettings oldValue = this.fiducialVisionSettings;
        this.fiducialVisionId = (visionSettings == null ? null : visionSettings.getId());
        this.fiducialVisionSettings = visionSettings;
        if (oldValue != visionSettings) {
            Configuration.get().fireVisionSettingsChanged();
            firePropertyChange("fiducialVisionSettings", oldValue, visionSettings);
            AbstractVisionSettings.fireUsedInProperty(oldValue);
            AbstractVisionSettings.fireUsedInProperty(visionSettings);
        }
    }

    /**
     * @param baseHolder
     * @return the list if PartSettingsHolder that override/specialize the visions settings of this base. 
     */
    protected List<PartSettingsHolder> getSpecializedIn(PartSettingsRoot rootHolder, Function<PartSettingsHolder, AbstractVisionSettings> propertyGetter) {
        List<PartSettingsHolder> list = new ArrayList<>();
        Configuration configuration = Configuration.get();
        if (configuration != null) {
            for (Package pkg : configuration.getPackages()) {
                if (propertyGetter.apply(pkg) != null 
                        && rootHolder.getParentHolder(pkg) == this) {
                    list.add(pkg);
                }
            }
            for (Part part : configuration.getParts()) {
                if (propertyGetter.apply(part) != null 
                        && (rootHolder.getParentHolder(part) == this
                        || (part.getPackage() != null && rootHolder.getParentHolder(part.getPackage()) == this))) {
                    list.add(part);
                }
            }
        }
        list.sort(new AbstractPartSettingsHolder.PartSettingsComparator());
        return list;
    }

    @Override 
    public List<PartSettingsHolder> getSpecializedBottomVisionIn() {
        AbstractPartAlignment align = AbstractPartAlignment.getPartAlignment(this, true);
        return getSpecializedIn(align, (h) -> h.getBottomVisionSettings());
    }

    @Override 
    public List<PartSettingsHolder> getSpecializedFiducialVisionIn() {
        ReferenceFiducialLocator fiducialLocator = ReferenceFiducialLocator.getDefault();
        return getSpecializedIn(fiducialLocator, (h) -> h.getFiducialVisionSettings());
    }

    @Override 
    public void generalizeBottomVisionSettings() {
        for (PartSettingsHolder holder : getSpecializedBottomVisionIn()) {
            BottomVisionSettings assignedVisionSettings = holder.getBottomVisionSettings();
            holder.setBottomVisionSettings(null);
            if (assignedVisionSettings.getUsedBottomVisionIn().size() == 0) {
                // This was a specific specialization, remove altogether.
                Configuration.get().removeVisionSettings(assignedVisionSettings);
            }
        }
    }

    @Override 
    public void generalizeFiducialVisionSettings() {
        for (PartSettingsHolder holder : getSpecializedFiducialVisionIn()) {
            FiducialVisionSettings assignedVisionSettings = holder.getFiducialVisionSettings();
            holder.setFiducialVisionSettings(null);
            if (assignedVisionSettings.getUsedFiducialVisionIn().size() == 0) {
                // This was a specific specialization, remove altogether.
                Configuration.get().removeVisionSettings(assignedVisionSettings);
            }
        }
    }

    public final static class PartSettingsComparator implements Comparator<PartSettingsHolder> {
        @Override
        public int compare(PartSettingsHolder o1, PartSettingsHolder o2) {
            Integer level1 = holderLevel(o1);
            Integer level2 = holderLevel(o2);
            if (level1 != level2) {
                return level1.compareTo(level2);
            }
            return o1.toString().compareTo(o2.toString());
        }

        private int holderLevel(PartSettingsHolder holder) {
            if (holder instanceof Part) {
                return 2;
            }
            else if (holder instanceof org.openpnp.model.Package) {
                return 1;
            }
            else {
                return 0;
            }
        }
    }
}
