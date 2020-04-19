package org.openpnp.machine.reference;

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

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
        RelativeTrend;
        
        public boolean isRelativeMethod() {
            return this == RelativeTrend;
        }
    }

    @Element(required = false)
    VacuumMeasurementMethod methodPartOn = null;

    @Attribute(required = false)
    private boolean establishPartOnLevel;

    @Element(required = false)
    private double vacuumLevelPartOnLow;

    @Element(required = false)
    private double vacuumLevelPartOnHigh;

    @Element(required = false)
    private double vacuumRelativePartOnLow;

    @Element(required = false)
    private double vacuumRelativePartOnHigh;

    @Element(required = false)
    VacuumMeasurementMethod methodPartOff = null;

    @Attribute(required = false)
    private boolean establishPartOffLevel;

    @Element(required = false)
    private double vacuumLevelPartOffLow;
    
    @Element(required = false)
    private double vacuumLevelPartOffHigh;

    @Attribute(required = false)
    private int partOffProbingMilliseconds;

    @Element(required = false)
    private double vacuumRelativePartOffLow;

    @Element(required = false)
    private double vacuumRelativePartOffHigh;

    @Element(required = false)
    private Length diameterLow = new Length(0, LengthUnit.Millimeters);

    @Attribute(required = false)
    private boolean isPushAndDragAllowed = false;

    // last vacuum readings 
    private Double vacuumLevelPartOnReading = null;
    private Double vacuumRelativePartOnReading = null;
    private Double vacuumLevelPartOffReading = null;
    private Double vacuumRelativePartOffReading = null;
    private Map<Double, Double> vacuumPartOnGraph = null;
    private Map<Double, Double> vacuumPartOffGraph = null;
    
    public ReferenceNozzleTip() {
    }

    @Commit
    public void commit() {
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

    public boolean isEstablishPartOnLevel() {
        return establishPartOnLevel;
    }

    public void setEstablishPartOnLevel(boolean establishPartOnLevel) {
        this.establishPartOnLevel = establishPartOnLevel;
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

    public double getVacuumRelativePartOnLow() {
        return vacuumRelativePartOnLow;
    }

    public void setVacuumRelativePartOnLow(double vacuumRelativePartOnLow) {
        this.vacuumRelativePartOnLow = vacuumRelativePartOnLow;
    }

    public double getVacuumRelativePartOnHigh() {
        return vacuumRelativePartOnHigh;
    }

    public void setVacuumRelativePartOnHigh(double vacuumRelativePartOnHigh) {
        this.vacuumRelativePartOnHigh = vacuumRelativePartOnHigh;
    }

    public VacuumMeasurementMethod getMethodPartOff() {
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

    public double getVacuumRelativePartOffLow() {
        return vacuumRelativePartOffLow;
    }

    public void setVacuumRelativePartOffLow(double vacuumRelativePartOffLow) {
        this.vacuumRelativePartOffLow = vacuumRelativePartOffLow;
    }

    public double getVacuumRelativePartOffHigh() {
        return vacuumRelativePartOffHigh;
    }

    public void setVacuumRelativePartOffHigh(double vacuumRelativePartOffHigh) {
        this.vacuumRelativePartOffHigh = vacuumRelativePartOffHigh;
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

    public Double getVacuumRelativePartOnReading() {
        return vacuumRelativePartOnReading;
    }

    public void setVacuumRelativePartOnReading(Double vacuumRelativePartOnReading) {
        Object oldValue = vacuumLevelPartOnReading;
        this.vacuumRelativePartOnReading = vacuumRelativePartOnReading;
        if (!(oldValue == null && vacuumRelativePartOnReading == null)) { // only fire when values are set
            firePropertyChange("vacuumRelativePartOnReading", oldValue, vacuumRelativePartOnReading);
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

    public Double getVacuumRelativePartOffReading() {
        return vacuumRelativePartOffReading;
    }

    public void setVacuumRelativePartOffReading(Double vacuumRelativePartOffReading) {
        Object oldValue = vacuumLevelPartOnReading;
        this.vacuumRelativePartOffReading = vacuumRelativePartOffReading;
        if (!(oldValue == null && vacuumRelativePartOffReading == null)) { // only fire when values are set
            firePropertyChange("vacuumRelativePartOffReading", oldValue, vacuumRelativePartOffReading);
        }
    }

    public Map<Double, Double> getVacuumPartOnGraph() {
        return vacuumPartOnGraph;
    }

    public void setVacuumPartOnGraph(Map<Double, Double> vacuumPartOnGraph) {
        Object oldValue = vacuumPartOnGraph;
        this.vacuumPartOnGraph = vacuumPartOnGraph;
        if (!(oldValue == null && vacuumPartOnGraph == null)) { // only fire when values are set
            firePropertyChange("vacuumPartOnGraph", oldValue, vacuumPartOnGraph);
        }
    }

    public Map<Double, Double> getVacuumPartOffGraph() {
        return vacuumPartOffGraph;
    }

    public void setVacuumPartOffGraph(Map<Double, Double> vacuumPartOffGraph) {
        Object oldValue = vacuumPartOffGraph;
        this.vacuumPartOffGraph = vacuumPartOffGraph;
        if (!(oldValue == null && vacuumPartOffGraph == null)) { // only fire when values are set
            firePropertyChange("vacuumPartOffGraph", oldValue, vacuumPartOffGraph);
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
