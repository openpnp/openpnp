package org.openpnp.model;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.vision.ReferenceBottomVision.*;
import org.openpnp.machine.reference.vision.wizards.BottomVisionSettingsConfigurationWizard;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

import java.util.UUID;

public class BottomVisionSettings extends AbstractVisionSettings {

    @Attribute(required = false)
    protected PreRotateUsage preRotateUsage = PreRotateUsage.Default;

    @Attribute(required = false)
    protected PartSizeCheckMethod checkPartSizeMethod = PartSizeCheckMethod.Disabled;

    @Attribute(required = false)
    protected int checkSizeTolerancePercent = 20;

    @Attribute(required = false)
    protected MaxRotation maxRotation = MaxRotation.Adjust;

    @Element(required = false)
    protected Location visionOffset = new Location(LengthUnit.Millimeters);

    @Override
    public Wizard getConfigurationWizard() {
        return new BottomVisionSettingsConfigurationWizard(this);
    }

    public BottomVisionSettings() {
    }

    public BottomVisionSettings(String id) {
        super(id);
    }

    public BottomVisionSettings(PartSettings partSettings) {
        super("BVS_migration_" + UUID.randomUUID().toString().split("-")[0]);
        this.setEnabled(partSettings.isEnabled());
        this.setCvPipeline(partSettings.getPipeline());
        this.preRotateUsage = partSettings.getPreRotateUsage();
        this.checkPartSizeMethod = partSettings.getCheckPartSizeMethod();
        this.checkSizeTolerancePercent = partSettings.getCheckSizeTolerancePercent();
        this.maxRotation = partSettings.getMaxRotation();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public PreRotateUsage getPreRotateUsage() {
        return preRotateUsage;
    }

    public void setPreRotateUsage(PreRotateUsage preRotateUsage) {
        this.preRotateUsage = preRotateUsage;
    }

    public PartSizeCheckMethod getCheckPartSizeMethod() {
        return checkPartSizeMethod;
    }

    public void setCheckPartSizeMethod(PartSizeCheckMethod checkPartSizeMethod) {
        this.checkPartSizeMethod = checkPartSizeMethod;
    }

    public int getCheckSizeTolerancePercent() {
        return checkSizeTolerancePercent;
    }

    public void setCheckSizeTolerancePercent(int checkSizeTolerancePercent) {
        this.checkSizeTolerancePercent = checkSizeTolerancePercent;
    }

    public MaxRotation getMaxRotation() {
        return maxRotation;
    }

    public void setMaxRotation(MaxRotation maxRotation) {
        this.maxRotation = maxRotation;
    }

    public Location getVisionOffset() {
        return visionOffset;
    }

    public void setVisionOffset(Location visionOffset) {
        this.visionOffset = visionOffset.derive(null, null, 0.0, 0.0);
        firePropertyChange("visionOffset", null, this.visionOffset);
    }
    
    public void setValues(BottomVisionSettings another) throws CloneNotSupportedException {
        setEnabled(another.isEnabled());
        setCvPipeline(another.getCvPipeline().clone());
        setPreRotateUsage(another.getPreRotateUsage());
        setCheckPartSizeMethod(another.checkPartSizeMethod);
        setMaxRotation(another.getMaxRotation());
        setCheckSizeTolerancePercent(another.getCheckSizeTolerancePercent());

        firePropertyChange("vision-settings", null, this);
    }

}
