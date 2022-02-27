package org.openpnp.model;

import java.awt.geom.Rectangle2D;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.vision.ReferenceBottomVision.MaxRotation;
import org.openpnp.machine.reference.vision.ReferenceBottomVision.PartSettings;
import org.openpnp.machine.reference.vision.ReferenceBottomVision.PartSizeCheckMethod;
import org.openpnp.machine.reference.vision.ReferenceBottomVision.PreRotateUsage;
import org.openpnp.machine.reference.vision.wizards.BottomVisionSettingsConfigurationWizard;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

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
        return new BottomVisionSettingsConfigurationWizard(this, null);
    }

    public Wizard getConfigurationWizard(PartSettingsHolder settingsHolder) {
        return new BottomVisionSettingsConfigurationWizard(this, settingsHolder);
    }

    public BottomVisionSettings() {
        super(Configuration.createId("BVS"));
    }

    public BottomVisionSettings(String id) {
        super(id);
    }

    public BottomVisionSettings(PartSettings partSettings) {
        this();
        this.setEnabled(partSettings.isEnabled());
        this.setPipeline(partSettings.getPipeline());
        this.preRotateUsage = partSettings.getPreRotateUsage();
        this.checkPartSizeMethod = partSettings.getCheckPartSizeMethod();
        this.checkSizeTolerancePercent = partSettings.getCheckSizeTolerancePercent();
        this.maxRotation = partSettings.getMaxRotation();
        this.visionOffset = partSettings.getVisionOffset();
    }

    public PreRotateUsage getPreRotateUsage() {
        return preRotateUsage;
    }

    public void setPreRotateUsage(PreRotateUsage preRotateUsage) {
        Object oldValue = this.preRotateUsage;
        this.preRotateUsage = preRotateUsage;
        firePropertyChange("preRotateUsage", oldValue, preRotateUsage);
    }

    public PartSizeCheckMethod getCheckPartSizeMethod() {
        return checkPartSizeMethod;
    }

    public void setCheckPartSizeMethod(PartSizeCheckMethod checkPartSizeMethod) {
        Object oldValue = this.checkPartSizeMethod;
        this.checkPartSizeMethod = checkPartSizeMethod;
        firePropertyChange("checkPartSizeMethod", oldValue, checkPartSizeMethod);
    }

    public int getCheckSizeTolerancePercent() {
        return checkSizeTolerancePercent;
    }

    public void setCheckSizeTolerancePercent(int checkSizeTolerancePercent) {
        Object oldValue = this.checkSizeTolerancePercent;
        this.checkSizeTolerancePercent = checkSizeTolerancePercent;
        firePropertyChange("checkSizeTolerancePercent", oldValue, checkSizeTolerancePercent);
    }

    public MaxRotation getMaxRotation() {
        return maxRotation;
    }

    public void setMaxRotation(MaxRotation maxRotation) {
        Object oldValue = this.maxRotation;
        this.maxRotation = maxRotation;
        firePropertyChange("maxRotation", oldValue, maxRotation);
    }

    public Location getVisionOffset() {
        return visionOffset;
    }

    public void setVisionOffset(Location visionOffset) {
        Object oldValue = this.visionOffset;
        this.visionOffset = visionOffset.derive(null, null, 0.0, 0.0);
        firePropertyChange("visionOffset", oldValue, this.visionOffset);
    }

    public void setValues(BottomVisionSettings another) {
        setEnabled(another.isEnabled());
        try {
            setPipeline(another.getPipeline().clone());
        }
        catch (CloneNotSupportedException e) {
        }
        setPipelineParameterAssignments(another.getPipelineParameterAssignments());
        setPreRotateUsage(another.getPreRotateUsage());
        setCheckPartSizeMethod(another.checkPartSizeMethod);
        setMaxRotation(another.getMaxRotation());
        setCheckSizeTolerancePercent(another.getCheckSizeTolerancePercent());
        setVisionOffset(another.getVisionOffset());
        Configuration.get().fireVisionSettingsChanged();
    }

    public Location getPartCheckSize(Part part) {
        Footprint footprint = part.getPackage().getFootprint();
        double checkWidth = 0.0;
        double checkHeight = 0.0;

        // Get the part footprint body dimensions to compare to
        switch (checkPartSizeMethod) {
            case Disabled:
                return null;
            case BodySize:
                checkWidth = footprint.getBodyWidth();
                checkHeight = footprint.getBodyHeight();
                break;
            case PadExtents:
                Rectangle2D bounds = footprint.getPadsShape().getBounds2D();
                checkWidth = bounds.getWidth();
                checkHeight = bounds.getHeight();
                break;
        }
        return new Location(footprint.getUnits(), checkWidth, checkHeight, 0, 0);
    }

}
