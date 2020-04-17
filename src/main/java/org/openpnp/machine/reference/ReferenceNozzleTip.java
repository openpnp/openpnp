package org.openpnp.machine.reference;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

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
import org.openpnp.spi.Head;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.AbstractNozzleTip;
import org.openpnp.util.UiUtils;
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
    private ReferenceNozzleTipCalibration calibration = new ReferenceNozzleTipCalibration();

    public enum VacuumMeasurementMethod {
        None, 
        Absolute,
        RelativeTrend,
        RelativeTrendFromPump;
        
        public boolean isTrendMethod() {
            return this == RelativeTrend || this == RelativeTrendFromPump;
        }
    }

    @Element(required = false)
    VacuumMeasurementMethod methodPartOn = VacuumMeasurementMethod.Absolute;

    @Attribute(required = false)
    private int partOnDwellMilliseconds;

    @Element(required = false)
    private double vacuumLevelPartOnLow;

    @Element(required = false)
    private double vacuumLevelPartOnHigh;

    @Attribute(required = false)
    private int partOnTrendMilliseconds;

    @Element(required = false)
    private double vacuumTrendPartOnLow;

    @Element(required = false)
    private double vacuumTrendPartOnHigh;

    @Element(required = false)
    VacuumMeasurementMethod methodPartOff = VacuumMeasurementMethod.Absolute;

    @Attribute(required = false)
    private int partOffDwellMilliseconds = -1;

    @Attribute(required = false)
    private boolean valveEnabledForPartOff = true;

    @Element(required = false)
    private double vacuumLevelPartOffLow;
    
    @Element(required = false)
    private double vacuumLevelPartOffHigh;

    @Attribute(required = false)
    private int partOffTrendMilliseconds;

    @Element(required = false)
    private double vacuumTrendPartOffLow;

    @Element(required = false)
    private double vacuumTrendPartOffHigh;

    @Element(required = false)
    private Length diameterLow = new Length(0, LengthUnit.Millimeters);

    @Attribute(required = false)
    private boolean isPushAndDragAllowed = false;

    // last vacuum readings 
    private Double vacuumLevelPartOnReading = null;
    private Double vacuumTrendPartOnReading = null;
    private Double vacuumLevelPartOffReading = null;
    private Double vacuumTrendPartOffReading = null;
    
    public ReferenceNozzleTip() {
    }

    @Commit
    public void commit() {
        /**
         * Backwards compatibility.
         */
        if (partOffDwellMilliseconds == -1) {
            // the pick time was previously used for this
            partOffDwellMilliseconds = pickDwellMilliseconds;
            // also initialize part on/off test enabling
            if (vacuumLevelPartOnLow < vacuumLevelPartOnHigh) {
                // was enabled
                methodPartOn = VacuumMeasurementMethod.Absolute;
            }
            else {
                methodPartOn = VacuumMeasurementMethod.None;
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
        this.changerStartLocation = changerStartLocation;
    }

    public Location getChangerMidLocation() {
        return changerMidLocation;
    }

    public void setChangerMidLocation(Location changerMidLocation) {
        this.changerMidLocation = changerMidLocation;
    }

    public Location getChangerMidLocation2() {
        return changerMidLocation2;
    }

    public void setChangerMidLocation2(Location changerMidLocation2) {
        this.changerMidLocation2 = changerMidLocation2;
    }

    public Location getChangerEndLocation() {
        return changerEndLocation;
    }

    public void setChangerEndLocation(Location changerEndLocation) {
        this.changerEndLocation = changerEndLocation;
    }
    
    public double getChangerStartToMidSpeed() {
        return changerStartToMidSpeed;
    }

    public void setChangerStartToMidSpeed(double changerStartToMidSpeed) {
        this.changerStartToMidSpeed = changerStartToMidSpeed;
    }

    public double getChangerMidToMid2Speed() {
        return changerMidToMid2Speed;
    }

    public void setChangerMidToMid2Speed(double changerMidToMid2Speed) {
        this.changerMidToMid2Speed = changerMidToMid2Speed;
    }

    public double getChangerMid2ToEndSpeed() {
        return changerMid2ToEndSpeed;
    }

    public void setChangerMid2ToEndSpeed(double changerMid2ToEndSpeed) {
        this.changerMid2ToEndSpeed = changerMid2ToEndSpeed;
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

    public VacuumMeasurementMethod getMethodPartOn() {
        return methodPartOn;
    }

    public void setMethodPartOn(VacuumMeasurementMethod methodPartOn) {
        this.methodPartOn = methodPartOn;
    }

    public int getPartOnDwellMilliseconds() {
        return partOnDwellMilliseconds;
    }

    public void setPartOnDwellMilliseconds(int partOnDwellMilliseconds) {
        this.partOnDwellMilliseconds = partOnDwellMilliseconds;
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

    public int getPartOnTrendMilliseconds() {
        return partOnTrendMilliseconds;
    }

    public void setPartOnTrendMilliseconds(int partOnTrendMilliseconds) {
        this.partOnTrendMilliseconds = partOnTrendMilliseconds;
    }

    public double getVacuumTrendPartOnLow() {
        return vacuumTrendPartOnLow;
    }

    public void setVacuumTrendPartOnLow(double vacuumTrendPartOnLow) {
        this.vacuumTrendPartOnLow = vacuumTrendPartOnLow;
    }

    public double getVacuumTrendPartOnHigh() {
        return vacuumTrendPartOnHigh;
    }

    public void setVacuumTrendPartOnHigh(double vacuumTrendPartOnHigh) {
        this.vacuumTrendPartOnHigh = vacuumTrendPartOnHigh;
    }

    public VacuumMeasurementMethod getMethodPartOff() {
        return methodPartOff;
    }

    public void setMethodPartOff(VacuumMeasurementMethod methodPartOff) {
        this.methodPartOff = methodPartOff;
    }

    public boolean isValveEnabledForPartOff() {
        return valveEnabledForPartOff;
    }

    public void setValveEnabledForPartOff(boolean valveEnabledForPartOff) {
        this.valveEnabledForPartOff = valveEnabledForPartOff;
    }

    public int getPartOffDwellMilliseconds() {
        return partOffDwellMilliseconds;
    }

    public void setPartOffDwellMilliseconds(int partOffDwellMilliseconds) {
        this.partOffDwellMilliseconds = partOffDwellMilliseconds;
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

    public int getPartOffTrendMilliseconds() {
        return partOffTrendMilliseconds;
    }

    public void setPartOffTrendMilliseconds(int partOffTrendMilliseconds) {
        this.partOffTrendMilliseconds = partOffTrendMilliseconds;
    }

    public double getVacuumTrendPartOffLow() {
        return vacuumTrendPartOffLow;
    }

    public void setVacuumTrendPartOffLow(double vacuumTrendPartOffLow) {
        this.vacuumTrendPartOffLow = vacuumTrendPartOffLow;
    }

    public double getVacuumTrendPartOffHigh() {
        return vacuumTrendPartOffHigh;
    }

    public void setVacuumTrendPartOffHigh(double vacuumTrendPartOffHigh) {
        this.vacuumTrendPartOffHigh = vacuumTrendPartOffHigh;
    }

    public Double getVacuumLevelPartOnReading() {
        return vacuumLevelPartOnReading;
    }

    public void setVacuumLevelPartOnReading(Double vacuumLevelPartOnReading) {
        Object oldValue = vacuumLevelPartOnReading;
        this.vacuumLevelPartOnReading = vacuumLevelPartOnReading;
        if (!(oldValue == null && vacuumLevelPartOnReading == null)) { // only fire when values are set
            firePropertyChange("vacuumLevelPartOnReading", oldValue, vacuumLevelPartOnReading);
        }
    }

    public Double getVacuumTrendPartOnReading() {
        return vacuumTrendPartOnReading;
    }

    public void setVacuumTrendPartOnReading(Double vacuumTrendPartOnReading) {
        Object oldValue = vacuumLevelPartOnReading;
        this.vacuumTrendPartOnReading = vacuumTrendPartOnReading;
        if (!(oldValue == null && vacuumTrendPartOnReading == null)) { // only fire when values are set
            firePropertyChange("vacuumTrendPartOnReading", oldValue, vacuumTrendPartOnReading);
        }
    }

    public Double getVacuumLevelPartOffReading() {
        return vacuumLevelPartOffReading;
    }

    public void setVacuumLevelPartOffReading(Double vacuumLevelPartOffReading) {
        Object oldValue = vacuumLevelPartOnReading;
        this.vacuumLevelPartOffReading = vacuumLevelPartOffReading;
        if (!(oldValue == null && vacuumLevelPartOffReading == null)) { // only fire when values are set
            firePropertyChange("vacuumLevelPartOffReading", oldValue, vacuumLevelPartOffReading);
        }
    }

    public Double getVacuumTrendPartOffReading() {
        return vacuumTrendPartOffReading;
    }

    public void setVacuumTrendPartOffReading(Double vacuumTrendPartOffReading) {
        Object oldValue = vacuumLevelPartOnReading;
        this.vacuumTrendPartOffReading = vacuumTrendPartOffReading;
        if (!(oldValue == null && vacuumTrendPartOffReading == null)) { // only fire when values are set
            firePropertyChange("vacuumTrendPartOffReading", oldValue, vacuumTrendPartOffReading);
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
    

    public Action loadAction = new AbstractAction("Load") {
        {
            putValue(SMALL_ICON, Icons.nozzleTipLoad);
            putValue(NAME, "Load");
            putValue(SHORT_DESCRIPTION, "Load the currently selected nozzle tip.");
        }

        @Override
        public void actionPerformed(final ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                MainFrame.get().getMachineControls().getSelectedNozzle().loadNozzleTip(ReferenceNozzleTip.this);
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
                MainFrame.get().getMachineControls().getSelectedNozzle().unloadNozzleTip();
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
}
