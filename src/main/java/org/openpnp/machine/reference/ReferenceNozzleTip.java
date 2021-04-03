package org.openpnp.machine.reference;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.openpnp.ConfigurationListener;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.wizards.ReferenceNozzleTipCalibrationWizard;
import org.openpnp.machine.reference.wizards.ReferenceNozzleTipConfigurationWizard;
import org.openpnp.machine.reference.wizards.ReferenceNozzleTipPartDetectionWizard;
import org.openpnp.machine.reference.wizards.ReferenceNozzleTipToolChangerWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.AbstractNozzleTip;
import org.openpnp.util.ImageUtils;
import org.openpnp.util.LogUtils;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.SimpleGraph;
import org.openpnp.util.UiUtils;
import org.openpnp.vision.TemplateImage;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;

public class ReferenceNozzleTip extends AbstractNozzleTip {
    @Attribute(required = false)
    private int pickDwellMilliseconds;

    @Attribute(required = false)
    private int placeDwellMilliseconds;

    @Element(required = false)
    private Location changerStartLocation = new Location(LengthUnit.Millimeters);

    @Element(required = false)
    private double changerStartToMidSpeed = 1D;

    @Element(required = false)
    private Location changerMidLocation = new Location(LengthUnit.Millimeters);

    @Element(required = false)
    private double changerMidToMid2Speed = 1D;

    @Element(required = false)
    private Location changerMidLocation2 = new Location(LengthUnit.Millimeters);

    @Element(required = false)
    private double changerMid2ToEndSpeed = 1D;

    @Element(required = false)
    private Location changerEndLocation = new Location(LengthUnit.Millimeters);

    @Element(required = false)
    private Location touchLocation = new Location(LengthUnit.Millimeters);

    public enum VisionCalibration {
        None, FirstLocation, SecondLocation, ThirdLocation, LastLocation, TouchLocation;

        public Location getLocation(ReferenceNozzleTip nt) {
            switch (this) {
                case None:
                    return null;
                case FirstLocation:
                    return nt.getChangerStartLocation();
                case SecondLocation:
                    return nt.getChangerMidLocation();
                case ThirdLocation:
                    return nt.getChangerMidLocation2();
                case LastLocation:
                    return nt.getChangerEndLocation();
                case TouchLocation:
                    return nt.getTouchLocation();
            }
            return null;
        }
    }

    @Attribute(required = false)
    private VisionCalibration visionCalibration = VisionCalibration.None;
    
    public enum VisionCalibrationTrigger {
        Manual, MachineHome, NozzleTipChange
    }

    @Attribute(required = false)
    VisionCalibrationTrigger visionCalibrationTrigger = VisionCalibrationTrigger.Manual;

    @Element(required = false)
    private Length visionTemplateDimensionX = new Length(10, LengthUnit.Millimeters);

    @Element(required = false)
    private Length visionTemplateDimensionY = new Length(10, LengthUnit.Millimeters);

    @Attribute(required = false)
    private double visionMatchMinimumScore = 0.2;

    private Double visionMatchLastScore = null;

    @Element(required = false)
    private Length visionTemplateTolerance = new Length(4, LengthUnit.Millimeters);

    @Attribute(required = false)
    private int visionCalibrationMaxPasses = 3; 

    @Element(required = false)
    private Length visionCalibrationTolerance = new Length(0.7, LengthUnit.Millimeters); 

    @Element(required = false)
    private TemplateImage visionTemplateImageEmpty = null;

    @Element(required = false)
    private TemplateImage visionTemplateImageOccupied = null;

    @Element(required = false)
    private Location visionCalibrationOffset;

    public enum ZCalibrationTrigger {
        Manual, MachineHome, NozzleTipChange
    }

    @Attribute(required = false)
    private ZCalibrationTrigger zCalibrationTrigger = ZCalibrationTrigger.Manual;

    @Attribute(required = false)
    private boolean zCalibrationFailHoming = true;

    @Attribute(required = false)
    private boolean templateNozzleTip = false;

    @Attribute(required = false)
    private boolean templateLocked = false;

    @Element(required = false)
    private Length maxPartDiameter = new Length(20, LengthUnit.Millimeters);

    @Element(required = false)
    private Length maxPartHeight = new Length(10, LengthUnit.Millimeters);

    @Element(required = false)
    private ReferenceNozzleTipCalibration calibration = new ReferenceNozzleTipCalibration();

    public enum VacuumMeasurementMethod {
        None, 
        Absolute,
        Difference;
        
        public boolean isDifferenceMethod() {
            // there might be more difference methods in the future, so make this easy
            return this == Difference;
        }
    }

    @Element(required = false)
    VacuumMeasurementMethod methodPartOn = null;

    @Element(required = false)
    boolean partOnCheckAfterPick = true; 

    @Element(required = false)
    boolean partOnCheckAlign = true; 

    @Element(required = false)
    boolean partOnCheckBeforePlace = true; 

    @Attribute(required = false)
    private boolean establishPartOnLevel;

    @Element(required = false)
    private double vacuumLevelPartOnLow;

    @Element(required = false)
    private double vacuumLevelPartOnHigh;

    @Element(required = false)
    private double vacuumDifferencePartOnLow;

    @Element(required = false)
    private double vacuumDifferencePartOnHigh;

    @Element(required = false)
    VacuumMeasurementMethod methodPartOff = null;

    @Attribute(required = false)
    private boolean establishPartOffLevel;

    @Element(required = false)
    boolean partOffCheckAfterPlace = true; 

    @Element(required = false)
    boolean partOffCheckBeforePick = true; 

    @Element(required = false)
    private double vacuumLevelPartOffLow;
    
    @Element(required = false)
    private double vacuumLevelPartOffHigh;

    @Attribute(required = false)
    private int partOffProbingMilliseconds;

    @Attribute(required = false)
    private int partOffDwellMilliseconds;

    @Element(required = false)
    private double vacuumDifferencePartOffLow;

    @Element(required = false)
    private double vacuumDifferencePartOffHigh;

    @Element(required = false)
    private Length diameterLow = new Length(0, LengthUnit.Millimeters);

    @Attribute(required = false)
    private boolean isPushAndDragAllowed = false;
    
    @Element(required = false)
    protected String changerActuatorPostStepOne;

    @Element(required = false)
    protected String changerActuatorPostStepTwo;
    
    @Element(required = false)
    protected String changerActuatorPostStepThree;

    public ReferenceNozzleTip() {
    }

    @Commit
    public void commit() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationComplete(Configuration configuration) throws Exception {
                /**
                 * Backwards compatibility.
                 */
                if (methodPartOn == null) {
                    if (vacuumLevelPartOnLow < vacuumLevelPartOnHigh) {
                        // was enabled
                        methodPartOn = VacuumMeasurementMethod.Absolute;
                    }
                    else {
                        methodPartOn = VacuumMeasurementMethod.None;
                    }
                }
                if (methodPartOff == null) {
                    // use the same as the former pick dwell time.
                    partOffProbingMilliseconds = pickDwellMilliseconds;
                    try {
                        // also add the nozzle's pick dwell time
                        Nozzle nozzle = Configuration.get()
                                .getMachine()
                                .getDefaultHead()
                                .getDefaultNozzle();
                        if (nozzle instanceof ReferenceNozzle) {
                            ReferenceNozzle refNozzle = (ReferenceNozzle) nozzle;
                            partOffProbingMilliseconds += refNozzle.getPickDwellMilliseconds();
                        } 
                    }
                    catch (Exception e) {
                        Logger.info("Cannot fully upgrade partOffProbingMilliseconds time", e);
                    }
                    if (vacuumLevelPartOffLow < vacuumLevelPartOffHigh) {
                        // was enabled
                        methodPartOff = VacuumMeasurementMethod.Absolute;
                    }
                    else {
                        methodPartOff = VacuumMeasurementMethod.None;
                    }
                }
            }
        });
    }

    @Override
    public String toString() {
        return getName() + " " + getId();
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceNozzleTipConfigurationWizard(this);
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return null;
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return new Action[] {unloadAction, loadAction, deleteAction};
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
                new PropertySheetWizardAdapter(getConfigurationWizard()),
                new PropertySheetWizardAdapter(new ReferenceNozzleTipPartDetectionWizard(this), "Part Detection"),
                new PropertySheetWizardAdapter(new ReferenceNozzleTipToolChangerWizard(this), "Tool Changer"),
                new PropertySheetWizardAdapter(new ReferenceNozzleTipCalibrationWizard(this), "Calibration")
                };
    }

    public Length getMaxPartHeight() {
        return maxPartHeight;
    }

    public void setMaxPartHeight(Length maxPartHeight) {
        this.maxPartHeight = maxPartHeight;
    }

    public Length getMaxPartDiameter() {
        return maxPartDiameter;
    }

    public void setMaxPartDiameter(Length maxPartDiameter) {
        this.maxPartDiameter = maxPartDiameter;
    }

    public int getPickDwellMilliseconds() {
        return pickDwellMilliseconds;
    }

    public void setPickDwellMilliseconds(int pickDwellMilliseconds) {
        this.pickDwellMilliseconds = pickDwellMilliseconds;
    }

    public int getPlaceDwellMilliseconds() {
        return placeDwellMilliseconds;
    }

    public void setPlaceDwellMilliseconds(int placeDwellMilliseconds) {
        this.placeDwellMilliseconds = placeDwellMilliseconds;
    }

    public Location getChangerStartLocation() {
        return changerStartLocation;
    }

    public void setChangerStartLocation(Location changerStartLocation) {
        Object oldValue = this.changerStartLocation;
        this.changerStartLocation = changerStartLocation;
        firePropertyChange("changerStartLocation", oldValue, changerStartLocation);
    }

    public Location getChangerMidLocation() {
        return changerMidLocation;
    }

    public void setChangerMidLocation(Location changerMidLocation) {
        Object oldValue = this.changerMidLocation;
        this.changerMidLocation = changerMidLocation;
        firePropertyChange("changerMidLocation", oldValue, changerMidLocation);
    }

    public Location getChangerMidLocation2() {
        return changerMidLocation2;
    }

    public void setChangerMidLocation2(Location changerMidLocation2) {
        Object oldValue = this.changerMidLocation2;
        this.changerMidLocation2 = changerMidLocation2;
        firePropertyChange("changerMidLocation2", oldValue, changerMidLocation2);
    }

    public Location getChangerEndLocation() {
        return changerEndLocation;
    }

    public void setChangerEndLocation(Location changerEndLocation) {
        Object oldValue = this.changerEndLocation;
        this.changerEndLocation = changerEndLocation;
        firePropertyChange("changerEndLocation", oldValue, changerEndLocation);
    }
    
    public double getChangerStartToMidSpeed() {
        return changerStartToMidSpeed;
    }

    public void setChangerStartToMidSpeed(double changerStartToMidSpeed) {
        Object oldValue = this.changerStartToMidSpeed;
        this.changerStartToMidSpeed = changerStartToMidSpeed;
        firePropertyChange("changerStartToMidSpeed", oldValue, changerStartToMidSpeed);
    }

    public double getChangerMidToMid2Speed() {
        return changerMidToMid2Speed;
    }

    public void setChangerMidToMid2Speed(double changerMidToMid2Speed) {
        Object oldValue = this.changerMidToMid2Speed;
        this.changerMidToMid2Speed = changerMidToMid2Speed;
        firePropertyChange("changerMidToMid2Speed", oldValue, changerMidToMid2Speed);
    }

    public double getChangerMid2ToEndSpeed() {
        return changerMid2ToEndSpeed;
    }

    public void setChangerMid2ToEndSpeed(double changerMid2ToEndSpeed) {
        Object oldValue = this.changerMid2ToEndSpeed;
        this.changerMid2ToEndSpeed = changerMid2ToEndSpeed;
        firePropertyChange("changerMid2ToEndSpeed", oldValue, changerMid2ToEndSpeed);
    }

    public String getChangerActuatorPostStepOne() {
        return changerActuatorPostStepOne;
    }

    public void setChangerActuatorPostStepOne(String changerActuatorPostStepOne) {
        Object oldValue = this.changerActuatorPostStepOne;
        this.changerActuatorPostStepOne = changerActuatorPostStepOne;
        firePropertyChange("changerActuatorPostStepOne", oldValue, changerActuatorPostStepOne);
    }

    public String getChangerActuatorPostStepTwo() {
        return changerActuatorPostStepTwo;
    }

    public void setChangerActuatorPostStepTwo(String changerActuatorPostStepTwo) {
        Object oldValue = this.changerActuatorPostStepTwo;
        this.changerActuatorPostStepTwo = changerActuatorPostStepTwo;
        firePropertyChange("changerActuatorPostStepTwo", oldValue, changerActuatorPostStepTwo);
    }
    
    public String getChangerActuatorPostStepThree() {
        return changerActuatorPostStepThree;
    }

    public void setChangerActuatorPostStepThree(String changerActuatorPostStepThree) {
        Object oldValue = this.changerActuatorPostStepThree;
        this.changerActuatorPostStepThree = changerActuatorPostStepThree;
        firePropertyChange("changerActuatorPostStepThree", oldValue, changerActuatorPostStepThree);
    }

    public VisionCalibrationTrigger getVisionCalibrationTrigger() {
        return visionCalibrationTrigger;
    }

    public void setVisionCalibrationTrigger(VisionCalibrationTrigger visionCalibrationTrigger) {
        Object oldValue = this.visionCalibrationTrigger;
        this.visionCalibrationTrigger = visionCalibrationTrigger;
        firePropertyChange("visionCalibrationTrigger", oldValue, visionCalibrationTrigger);
    }

    public ReferenceNozzle getNozzleAttachedTo() {
        for (Head head : Configuration.get().getMachine().getHeads()) {
            for (Nozzle nozzle : head.getNozzles()) {
                if (nozzle instanceof ReferenceNozzle) {
                    // Note this also includes support for the "unloaded" nozzle tip stand-in to calibrate the
                    // naked nozzle. But it will default to the first naked nozzle
                    // See also: ReferenceNozzleTipCalibration.getUiCalibrationNozzle().
                    if (this == ((ReferenceNozzle)nozzle).getCalibrationNozzleTip()) {
                        return ((ReferenceNozzle)nozzle);
                    }
                }
            }
        }
        return null;
    }

    public Location getTouchLocation() {
        return touchLocation;
    }

    public void setTouchLocation(Location touchLocation) {
        Object oldValue = this.touchLocation;
        this.touchLocation = touchLocation;
        firePropertyChange("touchLocation", oldValue, touchLocation);
    }

    public VisionCalibration getVisionCalibration() {
        return visionCalibration;
    }

    public void setVisionCalibration(VisionCalibration visionCalibration) {
        Object oldValue = this.visionCalibration;
        this.visionCalibration = visionCalibration;
        firePropertyChange("visionCalibration", oldValue, visionCalibration);
    }

    public Length getVisionTemplateDimensionX() {
        return visionTemplateDimensionX;
    }

    public void setVisionTemplateDimensionX(Length visionTemplateDimensionX) {
        Object oldValue = this.visionTemplateDimensionX;
        this.visionTemplateDimensionX = visionTemplateDimensionX;
        firePropertyChange("visionTemplateDimensionX", oldValue, visionTemplateDimensionX);
    }

    public Length getVisionTemplateDimensionY() {
        return visionTemplateDimensionY;
    }

    public void setVisionTemplateDimensionY(Length visionTemplateDimensionY) {
        Object oldValue = this.visionTemplateDimensionY;
        this.visionTemplateDimensionY = visionTemplateDimensionY;
        firePropertyChange("visionTemplateDimensionY", oldValue, visionTemplateDimensionY);
    }

    public double getVisionMatchMinimumScore() {
        return visionMatchMinimumScore;
    }

    public void setVisionMatchMinimumScore(double visionMatchMinimumScore) {
        Object oldValue = this.visionMatchMinimumScore;
        this.visionMatchMinimumScore = visionMatchMinimumScore;
        firePropertyChange("visionMatchMinimumScore", oldValue, visionMatchMinimumScore);
    }

    public Double getVisionMatchLastScore() {
        return visionMatchLastScore;
    }

    public void setVisionMatchLastScore(Double visionMatchLastScore) {
        Object oldValue = this.visionMatchLastScore;
        this.visionMatchLastScore = visionMatchLastScore;
        firePropertyChange("visionMatchLastScore", oldValue, visionMatchLastScore);
    }

    public Length getVisionTemplateTolerance() {
        return visionTemplateTolerance;
    }

    public void setVisionTemplateTolerance(Length visionTemplateTolerance) {
        Object oldValue = this.visionTemplateTolerance;
        this.visionTemplateTolerance = visionTemplateTolerance;
        firePropertyChange("visionTemplateTolerance", oldValue, visionTemplateTolerance);
    }

    public int getVisionCalibrationMaxPasses() {
        return visionCalibrationMaxPasses;
    }

    public void setVisionCalibrationMaxPasses(int visionCalibrationMaxPasses) {
        Object oldValue = this.visionCalibrationMaxPasses;
        this.visionCalibrationMaxPasses = visionCalibrationMaxPasses;
        firePropertyChange("visionCalibrationMaxPasses", oldValue, visionCalibrationMaxPasses);
    }

    public Length getVisionCalibrationTolerance() {
        return visionCalibrationTolerance;
    }

    public void setVisionCalibrationTolerance(Length visionCalibrationTolerance) {
        Object oldValue = this.visionCalibrationTolerance;
        this.visionCalibrationTolerance = visionCalibrationTolerance;
        firePropertyChange("visionCalibrationTolerance", oldValue, visionCalibrationTolerance);
    }

    public TemplateImage getVisionTemplateImageEmpty() {
        return visionTemplateImageEmpty;
    }

    public void setVisionTemplateImageEmpty(TemplateImage visionTemplateImageEmpty) {
        Object oldValue = this.visionTemplateImageEmpty;
        this.visionTemplateImageEmpty = visionTemplateImageEmpty;
        firePropertyChange("visionTemplateImageEmpty", oldValue, visionTemplateImageEmpty);
    }

    public TemplateImage getVisionTemplateImageOccupied() {
        return visionTemplateImageOccupied;
    }

    public void setVisionTemplateImageOccupied(TemplateImage visionTemplateImageOccupied) {
        Object oldValue = this.visionTemplateImageOccupied;
        this.visionTemplateImageOccupied = visionTemplateImageOccupied;
        firePropertyChange("visionTemplateImageOccupied", oldValue, visionTemplateImageOccupied);
    }

    public ZCalibrationTrigger getzCalibrationTrigger() {
        return zCalibrationTrigger;
    }

    public void setzCalibrationTrigger(ZCalibrationTrigger zCalibrationTrigger) {
        Object oldValue = this.zCalibrationTrigger;
        this.zCalibrationTrigger = zCalibrationTrigger;
        firePropertyChange("zCalibrationTrigger", oldValue, zCalibrationTrigger);
    }

    public boolean iszCalibrationFailHoming() {
        return zCalibrationFailHoming;
    }

    public void setzCalibrationFailHoming(boolean zCalibrationFailHoming) {
        Object oldValue = this.zCalibrationFailHoming;
        this.zCalibrationFailHoming = zCalibrationFailHoming;
        firePropertyChange("zCalibrationFailHoming", oldValue, zCalibrationFailHoming);
    }

    public boolean isTemplateNozzleTip() {
        return templateNozzleTip;
    }

    public void setTemplateNozzleTip(boolean templateNozzleTip) {
        boolean oldValue = this.templateNozzleTip;
        this.templateNozzleTip = templateNozzleTip;
        firePropertyChange("templateNozzleTip", oldValue, templateNozzleTip);
        if (templateNozzleTip && ! oldValue) {
            // Make sure, only one nozzle tip is template. 
            for (NozzleTip nt : Configuration.get().getMachine().getNozzleTips()) {
                if (nt != this
                        && nt instanceof ReferenceNozzleTip
                        && ((ReferenceNozzleTip) nt).isTemplateNozzleTip()) {
                    ((ReferenceNozzleTip) nt).setTemplateNozzleTip(false);
                }
            }
        }
    }

    public boolean isTemplateLocked() {
        return templateLocked;
    }

    public void setTemplateLocked(boolean templateLocked) {
        this.templateLocked = templateLocked;
    }

    /**
     * @return the calibration offset Z of the nozzle this nozzle tip is attached to.
     */
    public Length getCalibrationOffsetZ() {
        Length offsetZ = null;
        Nozzle nozzle = getNozzleAttachedTo();
        if (nozzle instanceof ContactProbeNozzle) {
            offsetZ = ((ContactProbeNozzle) nozzle).getCalibrationOffsetZ();
        }
        return offsetZ;
    }

    public void setCalibrationOffsetZ(Length calibrationOffsetZ) {
        firePropertyChange("calibrationOffsetZ", null, calibrationOffsetZ);
    }

    public VacuumMeasurementMethod getMethodPartOn() {
        if (methodPartOn == null) {
            // First time access after creation and no @Commit handler: initialize.
            methodPartOn = VacuumMeasurementMethod.None;
        }
        return methodPartOn;
    }

    public void setMethodPartOn(VacuumMeasurementMethod methodPartOn) {
        this.methodPartOn = methodPartOn;
    }

    public boolean isEstablishPartOnLevel() {
        return establishPartOnLevel;
    }

    public void setEstablishPartOnLevel(boolean establishPartOnLevel) {
        this.establishPartOnLevel = establishPartOnLevel;
    }

    public boolean isPartOnCheckAfterPick() {
        return partOnCheckAfterPick;
    }

    public void setPartOnCheckAfterPick(boolean partOnCheckAfterPick) {
        this.partOnCheckAfterPick = partOnCheckAfterPick;
    }

    public boolean isPartOnCheckAlign() {
        return partOnCheckAlign;
    }

    public void setPartOnCheckAlign(boolean partOnCheckAlign) {
        this.partOnCheckAlign = partOnCheckAlign;
    }

    public boolean isPartOnCheckBeforePlace() {
        return partOnCheckBeforePlace;
    }

    public void setPartOnCheckBeforePlace(boolean partOnCheckBeforePlace) {
        this.partOnCheckBeforePlace = partOnCheckBeforePlace;
    }

    public double getVacuumLevelPartOnLow() {
        return vacuumLevelPartOnLow;
    }

    public void setVacuumLevelPartOnLow(double vacuumLevelPartOnLow) {
        this.vacuumLevelPartOnLow = vacuumLevelPartOnLow;
    }

    public double getVacuumLevelPartOnHigh() {
        return vacuumLevelPartOnHigh;
    }

    public void setVacuumLevelPartOnHigh(double vacuumLevelPartOnHigh) {
        this.vacuumLevelPartOnHigh = vacuumLevelPartOnHigh;
    }

    public double getVacuumDifferencePartOnLow() {
        return vacuumDifferencePartOnLow;
    }

    public void setVacuumDifferencePartOnLow(double vacuumDifferencePartOnLow) {
        this.vacuumDifferencePartOnLow = vacuumDifferencePartOnLow;
    }

    public double getVacuumDifferencePartOnHigh() {
        return vacuumDifferencePartOnHigh;
    }

    public void setVacuumDifferencePartOnHigh(double vacuumDifferencePartOnHigh) {
        this.vacuumDifferencePartOnHigh = vacuumDifferencePartOnHigh;
    }

    public VacuumMeasurementMethod getMethodPartOff() {
        if (methodPartOff == null) {
            // First time access after creation and no @Commit handler: initialize.
            methodPartOff = VacuumMeasurementMethod.None;
        }
        return methodPartOff;
    }

    public void setMethodPartOff(VacuumMeasurementMethod methodPartOff) {
        this.methodPartOff = methodPartOff;
    }

    public boolean isEstablishPartOffLevel() {
        return establishPartOffLevel;
    }

    public void setEstablishPartOffLevel(boolean establishPartOffLevel) {
        this.establishPartOffLevel = establishPartOffLevel;
    }

    public boolean isPartOffCheckAfterPlace() {
        return partOffCheckAfterPlace;
    }

    public void setPartOffCheckAfterPlace(boolean partOffCheckAfterPlace) {
        this.partOffCheckAfterPlace = partOffCheckAfterPlace;
    }

    public boolean isPartOffCheckBeforePick() {
        return partOffCheckBeforePick;
    }

    public void setPartOffCheckBeforePick(boolean partOffCheckBeforePick) {
        this.partOffCheckBeforePick = partOffCheckBeforePick;
    }

    public double getVacuumLevelPartOffLow() {
        return vacuumLevelPartOffLow;
    }

    public void setVacuumLevelPartOffLow(double vacuumLevelPartOffLow) {
        this.vacuumLevelPartOffLow = vacuumLevelPartOffLow;
    }

    public double getVacuumLevelPartOffHigh() {
        return vacuumLevelPartOffHigh;
    }

    public void setVacuumLevelPartOffHigh(double vacuumLevelPartOffHigh) {
        this.vacuumLevelPartOffHigh = vacuumLevelPartOffHigh;
    }

    public int getPartOffProbingMilliseconds() {
        return partOffProbingMilliseconds;
    }

    public void setPartOffProbingMilliseconds(int partOffProbingMilliseconds) {
        this.partOffProbingMilliseconds = partOffProbingMilliseconds;
    }

    public int getPartOffDwellMilliseconds() {
        return partOffDwellMilliseconds;
    }

    public void setPartOffDwellMilliseconds(int partOffDwellMilliseconds) {
        this.partOffDwellMilliseconds = partOffDwellMilliseconds;
    }

    public double getVacuumDifferencePartOffLow() {
        return vacuumDifferencePartOffLow;
    }

    public void setVacuumDifferencePartOffLow(double vacuumDifferencePartOffLow) {
        this.vacuumDifferencePartOffLow = vacuumDifferencePartOffLow;
    }

    public double getVacuumDifferencePartOffHigh() {
        return vacuumDifferencePartOffHigh;
    }

    public void setVacuumDifferencePartOffHigh(double vacuumDifferencePartOffHigh) {
        this.vacuumDifferencePartOffHigh = vacuumDifferencePartOffHigh;
    }

    public Double getVacuumLevelPartOnReading() {
        return vacuumLevelPartOnReading;
    }

    public void setVacuumLevelPartOnReading(Double vacuumLevelPartOnReading) {
        Object oldValue = this.vacuumLevelPartOnReading;
        this.vacuumLevelPartOnReading = vacuumLevelPartOnReading;
        if (!(oldValue == null && vacuumLevelPartOnReading == null)) { // only fire when values are set
            firePropertyChange("vacuumLevelPartOnReading", oldValue, vacuumLevelPartOnReading);
        }
    }

    public Double getVacuumDifferencePartOnReading() {
        return vacuumDifferencePartOnReading;
    }

    public void setVacuumDifferencePartOnReading(Double vacuumDifferencePartOnReading) {
        Object oldValue = this.vacuumDifferencePartOnReading;
        this.vacuumDifferencePartOnReading = vacuumDifferencePartOnReading;
        if (!(oldValue == null && vacuumDifferencePartOnReading == null)) { // only fire when values are set
            firePropertyChange("vacuumDifferencePartOnReading", oldValue, vacuumDifferencePartOnReading);
        }
    }

    public Double getVacuumLevelPartOffReading() {
        return vacuumLevelPartOffReading;
    }

    public void setVacuumLevelPartOffReading(Double vacuumLevelPartOffReading) {
        Object oldValue = this.vacuumLevelPartOffReading;
        this.vacuumLevelPartOffReading = vacuumLevelPartOffReading;
        if (!(oldValue == null && vacuumLevelPartOffReading == null)) { // only fire when values are set
            firePropertyChange("vacuumLevelPartOffReading", oldValue, vacuumLevelPartOffReading);
        }
    }

    public Double getVacuumDifferencePartOffReading() {
        return vacuumDifferencePartOffReading;
    }

    public void setVacuumDifferencePartOffReading(Double vacuumDifferencePartOffReading) {
        Object oldValue = this.vacuumDifferencePartOffReading;
        this.vacuumDifferencePartOffReading = vacuumDifferencePartOffReading;
        if (!(oldValue == null && vacuumDifferencePartOffReading == null)) { // only fire when values are set
            firePropertyChange("vacuumDifferencePartOffReading", oldValue, vacuumDifferencePartOffReading);
        }
    }

    public SimpleGraph getVacuumPartOnGraph() {
        return vacuumPartOnGraph;
    }

    public void setVacuumPartOnGraph(SimpleGraph vacuumPartOnGraph) {
        Object oldValue = this.vacuumPartOnGraph;
        this.vacuumPartOnGraph = vacuumPartOnGraph;
        if (!(oldValue == null && vacuumPartOnGraph == null)) { // only fire when values are set
            firePropertyChange("vacuumPartOnGraph", null /*always treat as change*/, vacuumPartOnGraph); 
        }
    }

    public SimpleGraph getVacuumPartOffGraph() {
        return vacuumPartOffGraph;
    }

    public void setVacuumPartOffGraph(SimpleGraph vacuumPartOffGraph) {
        Object oldValue = this.vacuumPartOffGraph;
        this.vacuumPartOffGraph = vacuumPartOffGraph;
        if (!(oldValue == null && vacuumPartOffGraph == null)) { // only fire when values are set
            firePropertyChange("vacuumPartOffGraph", null /*always treat as change*/, vacuumPartOffGraph);
        }
    }

    @Override
    public Length getDiameterLow() {
        return diameterLow;
    }

    public void setDiameterLow(Length diameterLow) {
        this.diameterLow = diameterLow;
    }

    @Override
    public boolean isPushAndDragAllowed() {
        return isPushAndDragAllowed;
    }

    public void setPushAndDragAllowed(boolean isPushAndDragAllowed) {
        this.isPushAndDragAllowed = isPushAndDragAllowed;
    }

    public boolean isUnloadedNozzleTipStandin() {
        return getName().startsWith("unloaded");
    }

    public ReferenceNozzleTipCalibration getCalibration() {
        return calibration;
    }

    // Recorded vacuum readings 
    private Double vacuumLevelPartOnReading = null;
    private Double vacuumDifferencePartOnReading = null;
    private Double vacuumLevelPartOffReading = null;
    private Double vacuumDifferencePartOffReading = null;
    private SimpleGraph vacuumPartOnGraph = null;
    private SimpleGraph vacuumPartOffGraph = null;

    public static final String PRESSURE = "P"; 
    public static final String BOOLEAN = "B"; 
    public static final String VACUUM = "V"; 
    public static final String VALVE_ON = "ON"; 

    protected final SimpleGraph startNewVacuumGraph(double vacuumLevel, boolean valveSwitchingOn) {
        Color gridColor = UIManager.getColor ( "PasswordField.capsLockIconColor" );
        if (gridColor == null) {
            gridColor = new Color(0, 0, 0, 64);
        } else {
            gridColor = new Color(gridColor.getRed(), gridColor.getGreen(), gridColor.getBlue(), 64);
        }
        // start a new graph 
        SimpleGraph vacuumGraph = new SimpleGraph();
        vacuumGraph.setRelativePaddingLeft(0.05);
        double t = vacuumGraph.getT();
        // init pressure scale
        SimpleGraph.DataScale vacuumScale =  vacuumGraph.getScale(PRESSURE);
        vacuumScale.setRelativePaddingBottom(0.3);
        vacuumScale.setColor(gridColor);
        // init valve scale
        SimpleGraph.DataScale valveScale =  vacuumGraph.getScale(BOOLEAN);
        valveScale.setRelativePaddingTop(0.75);
        valveScale.setRelativePaddingBottom(0.2);
        // record the current pressure
        SimpleGraph.DataRow vacuumData = vacuumGraph.getRow(PRESSURE, VACUUM);
        vacuumData.setColor(new Color(255, 0, 0));
        vacuumData.recordDataPoint(t, vacuumLevel);
        // record the valve switching off
        SimpleGraph.DataRow valveData = vacuumGraph.getRow(BOOLEAN, VALVE_ON);
        valveData.setColor(new Color(00, 0x5B, 0xD9)); // the OpenPNP color
        valveData.recordDataPoint(t, valveSwitchingOn ? 0 : 1);
        valveData.recordDataPoint(vacuumGraph.getT(), valveSwitchingOn ? 1 : 0);
        return vacuumGraph;
    }

    public static ReferenceNozzleTip getTemplateNozzleTip() throws Exception {
        for (NozzleTip nt : Configuration.get().getMachine().getNozzleTips()) {
            if (nt instanceof ReferenceNozzleTip
                    && ((ReferenceNozzleTip)nt).isTemplateNozzleTip()) {
                return (ReferenceNozzleTip)nt;
            }
        }
        throw new Exception("No Nozzle Tip is defined as template.");
    }

    public void assignNozzleTipChangerSettings(ReferenceNozzleTip templateNozzleTip, 
            boolean changerLocations, boolean zCalibration, boolean visionCalibration) {
        if (getChangerStartLocation().getLinearDistanceTo(Location.origin) != 0
                && !isTemplateLocked()) {
            if (changerLocations) {
                Location offsets = getChangerStartLocation()
                        .subtract(templateNozzleTip.getChangerStartLocation());
                setChangerMidLocation(templateNozzleTip.getChangerMidLocation().add(offsets));
                setChangerMidLocation2(templateNozzleTip.getChangerMidLocation2().add(offsets));
                setChangerEndLocation(templateNozzleTip.getChangerEndLocation().add(offsets));
                setChangerActuatorPostStepOne(templateNozzleTip.getChangerActuatorPostStepOne());
                setChangerActuatorPostStepTwo(templateNozzleTip.getChangerActuatorPostStepTwo());
                setChangerActuatorPostStepThree(templateNozzleTip.getChangerActuatorPostStepThree());
                setChangerStartToMidSpeed(templateNozzleTip.getChangerStartToMidSpeed());
                setChangerMidToMid2Speed(templateNozzleTip.getChangerMidToMid2Speed());
                setChangerMid2ToEndSpeed(templateNozzleTip.getChangerMid2ToEndSpeed());
                setTouchLocation(templateNozzleTip.getTouchLocation().add(offsets));
            }
            if (zCalibration) {
                setzCalibrationTrigger(templateNozzleTip.getzCalibrationTrigger());
                setzCalibrationFailHoming(templateNozzleTip.iszCalibrationFailHoming());
            }
            if (visionCalibration) {
                setVisionCalibration(templateNozzleTip.getVisionCalibration());
                setVisionCalibrationTrigger(templateNozzleTip.getVisionCalibrationTrigger());
                setVisionTemplateDimensionX(templateNozzleTip.getVisionTemplateDimensionX());
                setVisionTemplateDimensionY(templateNozzleTip.getVisionTemplateDimensionY());
                setVisionTemplateTolerance(templateNozzleTip.getVisionTemplateTolerance());
                setVisionCalibrationTolerance(templateNozzleTip.getVisionCalibrationTolerance());
                setVisionCalibrationMaxPasses(templateNozzleTip.getVisionCalibrationMaxPasses());
                setVisionMatchMinimumScore(templateNozzleTip.getVisionMatchMinimumScore());
                setVisionTemplateImageEmpty(templateNozzleTip.getVisionTemplateImageEmpty());
                setVisionTemplateImageOccupied(templateNozzleTip.getVisionTemplateImageOccupied());
            }
        }
    }

    public Action loadAction = new AbstractAction("Load") {
        {
            putValue(SMALL_ICON, Icons.nozzleTipLoad);
            putValue(NAME, "Load");
            putValue(SHORT_DESCRIPTION, "Load the currently selected nozzle tip.");
        }

        @Override
        public void actionPerformed(final ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                Nozzle nozzle = MainFrame.get().getMachineControls().getSelectedNozzle();
                nozzle.loadNozzleTip(ReferenceNozzleTip.this);
                MovableUtils.fireTargetedUserAction(nozzle);
            });
        }
    };

    public Action unloadAction = new AbstractAction("Unload") {
        {
            putValue(SMALL_ICON, Icons.nozzleTipUnload);
            putValue(NAME, "Unload");
            putValue(SHORT_DESCRIPTION, "Unload the currently loaded nozzle tip.");
        }

        @Override
        public void actionPerformed(final ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                Nozzle nozzle = MainFrame.get().getMachineControls().getSelectedNozzle();
                nozzle.unloadNozzleTip();
                MovableUtils.fireTargetedUserAction(nozzle);
            });
        }
    };
    public Action deleteAction = new AbstractAction("Delete Nozzle Tip") {
        {
            putValue(SMALL_ICON, Icons.nozzleTipRemove);
            putValue(NAME, "Delete Nozzle Tip");
            putValue(SHORT_DESCRIPTION, "Delete the currently selected nozzle tip.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            int ret = JOptionPane.showConfirmDialog(MainFrame.get(),
                    "Are you sure you want to delete " + getName() + "?",
                    "Delete " + getName() + "?", JOptionPane.YES_NO_OPTION);
            if (ret == JOptionPane.YES_OPTION) {
                Configuration.get().getMachine().removeNozzleTip(ReferenceNozzleTip.this);
            }
        }
    };

    public Location getChangerStartLocationCalibrated(boolean nozzleTipChange) throws Exception {
        return applyVisionCalibration(getChangerStartLocation(), nozzleTipChange);
    }

    public Location getChangerMidLocationCalibrated(boolean nozzleTipChange) throws Exception {
        return applyVisionCalibration(getChangerMidLocation(), nozzleTipChange);
    }

    public Location getChangerMidLocation2Calibrated(boolean nozzleTipChange) throws Exception {
        return applyVisionCalibration(getChangerMidLocation2(), nozzleTipChange);
    }

    public Location getChangerEndLocationCalibrated(boolean nozzleTipChange) throws Exception {
        return applyVisionCalibration(getChangerEndLocation(), nozzleTipChange);
    }

    private Location applyVisionCalibration(Location location, boolean nozzleTipChange) throws Exception {
        ensureVisionCalibration(nozzleTipChange);
        if (visionCalibrationOffset != null) {
            return location.add(visionCalibrationOffset);
        }
        else {
            return location;
        }
    }

    public void resetVisionCalibration() {
        visionCalibrationOffset = null;
    }

    public void ensureVisionCalibration(boolean nozzleTipChange) throws Exception {
        Location location = getVisionCalibration().getLocation(this);
        if (location != null) {
            // When location is not null, the calibrations is enabled.
            if (visionCalibrationOffset == null 
                    || (getVisionCalibrationTrigger() == VisionCalibrationTrigger.NozzleTipChange && nozzleTipChange)) {
                // Not yet calibrated.
                if (getVisionTemplateImageEmpty() == null) {
                    throw new Exception("Nozzle tip "+getName()+" changer slot vision calibration: Template Empty missing.");
                }
                if (getVisionTemplateImageOccupied() == null) {
                    throw new Exception("Nozzle tip "+getName()+" changer slot vision calibration: Template Occupied missing.");
                }
                
                Camera camera = MainFrame.get()
                                         .getMachineControls()
                                         .getSelectedTool()
                                         .getHead()
                                         .getDefaultCamera();

                Location upp = camera.getUnitsPerPixel();
                int width =
                        ((int) Math.ceil(getVisionTemplateDimensionX().add(visionTemplateTolerance)
                                                                      .divide(upp.getLengthX())))
                                & ~1; // divisible by 2
                int height =
                        ((int) Math.ceil(getVisionTemplateDimensionY().add(visionTemplateTolerance)
                                                                      .divide(upp.getLengthY())))
                                & ~1; // divisible by 2
                
                BufferedImage templateEmpty = getVisionTemplateImageEmpty().getImage();
                BufferedImage templateOccupied = getVisionTemplateImageOccupied().getImage();

                Mat templateMatEmpty = OpenCvUtils.toMat(templateEmpty);
                Mat templateMatOccupied = OpenCvUtils.toMat(templateOccupied);
                try {
                    Location originalLocation = location;
                    boolean shouldBeOccupied = (getNozzleAttachedTo() == null);
                    for (int pass = 0; pass < visionCalibrationMaxPasses; ++pass) {
                        MovableUtils.moveToLocationAtSafeZ(camera, location);
                        BufferedImage cameraImage = camera.lightSettleAndCapture();
                        int x = (cameraImage.getWidth() - width) / 2;
                        int y = (cameraImage.getHeight() - height) / 2;
                        // Convert the camera image to the same type as the templates. This
                        // is required by the matchTemplate call.
                        cameraImage =
                                ImageUtils.convertBufferedImage(cameraImage, templateEmpty.getType());
                        // Crop to a center area with the given tolerance arount the template.  
                        Mat cameraImageMat = OpenCvUtils.toMat(cameraImage);
                        Mat cameraCropMat = new Mat(cameraImageMat, new Rect(x, y, width, height));
                        cameraImageMat.release();
                        if (LogUtils.isDebugEnabled()) {
                            File file;
                            file = Configuration.get().createResourceFile(getClass(), "camera-cropped", ".png");
                            Imgcodecs.imwrite(file.getAbsolutePath(), cameraCropMat);
                            file = Configuration.get().createResourceFile(getClass(), "template-empty", ".png");
                            Imgcodecs.imwrite(file.getAbsolutePath(), templateMatEmpty);
                            file = Configuration.get().createResourceFile(getClass(), "template-occupied", ".png");
                            Imgcodecs.imwrite(file.getAbsolutePath(), templateMatOccupied);
                        }
                        Mat resultEmptyMat = new Mat();
                        Mat resultOccupiedMat = new Mat();
                        Imgproc.matchTemplate(cameraCropMat, templateMatEmpty, resultEmptyMat,
                                Imgproc.TM_CCOEFF_NORMED);
                        MinMaxLocResult emptyMatch = Core.minMaxLoc(resultEmptyMat);
                        emptyMatch.maxLoc.x -= resultEmptyMat.cols()/2;
                        emptyMatch.maxLoc.y -= resultEmptyMat.rows()/2;
                        Imgproc.matchTemplate(cameraCropMat, templateMatOccupied, resultOccupiedMat,
                                Imgproc.TM_CCOEFF_NORMED);
                        MinMaxLocResult occupiedMatch = Core.minMaxLoc(resultOccupiedMat);
                        occupiedMatch.maxLoc.x -= resultOccupiedMat.cols()/2.;
                        occupiedMatch.maxLoc.y -= resultOccupiedMat.rows()/2.;
                        if (LogUtils.isDebugEnabled()) {
                            File file;
                            file = Configuration.get().createResourceFile(getClass(), "match-empty", ".png");
                            // this is a 3x32bit image, cannot save these as .png, need to convert to known image format first
                            BufferedImage img;
                            img = OpenCvUtils.toBufferedImage(resultEmptyMat);
                            ImageIO.write(img, "png", file);
                            file = Configuration.get().createResourceFile(getClass(), "match-occupied", ".png");
                            img = OpenCvUtils.toBufferedImage(resultOccupiedMat);
                            ImageIO.write(img, "png", file);
                        }
                        resultEmptyMat.release();
                        resultOccupiedMat.release();
                        cameraCropMat.release();

                        Logger.trace(String.format(
                                "Nozzle tip %s changer slot empty template at pixel offset %f, %f, score %f", 
                                getName(), emptyMatch.maxLoc.x, emptyMatch.maxLoc.y, emptyMatch.maxVal));
                        Logger.trace(String.format(
                                "Nozzle tip %s changer slot occupied template at pixel offset %f, %f, score %f",
                                getName(), occupiedMatch.maxLoc.x, occupiedMatch.maxLoc.y, occupiedMatch.maxVal));
                        
                        if (Math.max(emptyMatch.maxVal, occupiedMatch.maxVal) < visionMatchMinimumScore) {
                            throw new Exception(String.format(
                                    "Nozzle tip %s changer slot vision calibration failed. Score %f lower than minmum %f.",
                                    getName(), Math.max(emptyMatch.maxVal, occupiedMatch.maxVal), visionMatchMinimumScore));
                        }
                        boolean foundOccupied = (emptyMatch.maxVal < occupiedMatch.maxVal);
                        if (foundOccupied != shouldBeOccupied) {
                            throw new Exception(String.format(
                                    "Nozzle tip %s changer slot was expected %s, but found %s.",
                                    getName(), shouldBeOccupied ? "occupied" : "empty",
                                    foundOccupied ? "occupied" : "empty"));
                        }
                        MinMaxLocResult match = shouldBeOccupied ? occupiedMatch : emptyMatch;
                        Location lastOffset = upp.multiply(match.maxLoc.x, match.maxLoc.y, 0, 0);
                        location = location.add(lastOffset);
                        setVisionMatchLastScore(match.maxVal);
                        if (lastOffset.getLinearLengthTo(Location.origin)
                                                   .compareTo(visionCalibrationTolerance) < 0) {
                            // Good enough, done.
                            break;
                        }
                    } 
                    visionCalibrationOffset = location.subtract(originalLocation);
                    Logger.trace(String.format(
                            "Nozzle tip %s changer slot calibration offset %s, %s, distance %s",
                            getName(), visionCalibrationOffset.getLengthX(), visionCalibrationOffset.getLengthY(), 
                            visionCalibrationOffset.getLinearLengthTo(Location.origin)));
                }
                finally {
                    templateMatEmpty.release();
                    templateMatOccupied.release();
                }
            }
        }
        else if (visionCalibrationOffset != null) {
            visionCalibrationOffset = null;
        }
    }

    @Override
    public void home() throws Exception {
        if (getVisionCalibrationTrigger() != VisionCalibrationTrigger.Manual) {
            resetVisionCalibration();
        }
    }
}
