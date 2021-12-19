package org.openpnp.machine.reference.vision;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.openpnp.ConfigurationListener;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.AbstractVisionSettings;
import org.openpnp.model.BottomVisionSettings;
import org.openpnp.model.Configuration;
import org.openpnp.model.Package;
import org.openpnp.model.Part;
import org.openpnp.model.PartSettingsHolder;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.core.Persist;

public abstract class AbstractPartSettingsHolder extends AbstractModelObject implements PartSettingsHolder  {
    @Attribute(required = false)
    protected String bottomVisionId;

    protected BottomVisionSettings visionSettings;

    public AbstractPartSettingsHolder() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationLoaded(Configuration configuration) {
                visionSettings = configuration.getVisionSettings(bottomVisionId);
            }
        });
    }

    @Persist
    private void persist() {
        bottomVisionId = (visionSettings == null ? null : visionSettings.getId());
    }

    @Override 
    public BottomVisionSettings getVisionSettings() {
        return visionSettings;
    }

    @Override
    public void setVisionSettings(BottomVisionSettings visionSettings) {
        BottomVisionSettings oldValue = this.visionSettings;
        this.bottomVisionId = (visionSettings == null ? null : visionSettings.getId());
        this.visionSettings = visionSettings;
        if (oldValue != visionSettings) {
            Configuration.get().fireVisionSettingsChanged();
            firePropertyChange("visionSettings", oldValue, visionSettings);
            AbstractVisionSettings.fireUsedInProperty(oldValue);
            AbstractVisionSettings.fireUsedInProperty(visionSettings);
        }
    }

    /**
     * @param baseHolder
     * @return the list if PartSettingsHolder that override/specialize the visions settings of this base. 
     */
    @Override 
    public List<PartSettingsHolder> getSpecializedIn() {
        List<PartSettingsHolder> list = new ArrayList<>();
        Configuration configuration = Configuration.get();
        if (configuration != null) {
            for (Package pkg : configuration.getPackages()) {
                if (pkg.getVisionSettings() != null 
                        && pkg.getParentHolder() == this) {
                    list.add(pkg);
                }
            }
            for (Part part : configuration.getParts()) {
                if (part.getVisionSettings() != null 
                        && (part.getParentHolder() == this
                        || (part.getPackage() != null && part.getPackage().getParentHolder() == this))) {
                    list.add(part);
                }
            }
        }
        list.sort(new AbstractPartSettingsHolder.PartSettingsComparator());
        return list;
    }

    @Override 
    public void resetSpecializedVisionSettings() {
        for (PartSettingsHolder holder : getSpecializedIn()) {
            BottomVisionSettings assignedVisionSettings = holder.getVisionSettings();
            holder.resetVisionSettings();
            if (assignedVisionSettings.getUsedIn().size() == 0) {
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
            int level = 0;
            PartSettingsHolder parent = holder.getParentHolder();
            while (parent != null) {
                level++;
                parent = parent.getParentHolder();
            }
            return level;
        }
    }
}
