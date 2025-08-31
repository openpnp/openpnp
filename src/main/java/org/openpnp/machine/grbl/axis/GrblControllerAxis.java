package org.openpnp.machine.grbl.axis;

import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.machine.grbl.driver.GrblDriver;
import org.openpnp.machine.grbl.wizards.GrblControllerAxisConfigurationWizard;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.spi.Driver;
import org.pmw.tinylog.Logger;

/**
 * Controller axis with grbl/grblHAL settings synchronization.
 * Automatically syncs changes between OpenPnP configuration and controller flash memory.
 */
public class GrblControllerAxis extends ReferenceControllerAxis {
    
    // === STEP AND DIRECTION PIN INVERT SETTINGS ===
    private boolean stepPinInvert = false;      // Per-axis step pin invert
    private boolean dirPinInvert = false;       // Per-axis direction pin invert
    private boolean stepEnableInvert = false;   // Per-axis step enable invert
    private boolean gangedMotorInvert = false;   // Per-axis ganged motor invert

    @Override
    public void setResolution(double resolution) {
        double oldResolution = getResolution();
        
        // Call parent setter first
        super.setResolution(resolution);
        
        // Sync to controller if resolution changed
        if (Math.abs(oldResolution - resolution) > 0.0001 && isConnected()) {
            syncStepsToController();
        }
    }
    
    @Override
    public void setFeedratePerSecond(Length feedratePerSecond) {
        Length oldFeedrate = getFeedratePerSecond();
        
        // Call parent setter first
        super.setFeedratePerSecond(feedratePerSecond);
        
        // Sync to controller if feedrate changed
        if (oldFeedrate != null && !oldFeedrate.equals(feedratePerSecond) && isConnected()) {
            syncFeedrateToController();
        }
    }
    
    @Override
    public void setAccelerationPerSecond2(Length accelerationPerSecond2) {
        Length oldAcceleration = getAccelerationPerSecond2();
        
        // Call parent setter first
        super.setAccelerationPerSecond2(accelerationPerSecond2);
        
        // Sync to controller if acceleration changed
        if (oldAcceleration != null && !oldAcceleration.equals(accelerationPerSecond2) && isConnected()) {
            syncAccelerationToController();
        }
    }
    
    /**
     * Sync steps/mm setting to grbl controller
     */
    private void syncStepsToController() {
        try {
            GrblDriver grblDriver = getGrblDriver();
            if (grblDriver == null) {return;}
            
            double stepsPerMm = 1.0 / getResolution();
            
            // Apply smart rounding for common stepper values
            if (Math.abs(stepsPerMm - Math.round(stepsPerMm)) < 0.1) {
                stepsPerMm = Math.round(stepsPerMm);
            }
            
            int settingId = getStepsSettingId();
            if (settingId != -1) {
                grblDriver.syncSettingToController(settingId, stepsPerMm);
                Logger.info("Synced {}-axis steps/mm to controller: {}", getName(), stepsPerMm);
            }
            
        } catch (Exception e) {
            Logger.warn("Failed to sync steps/mm to controller for axis {}: {}", getName(), e.getMessage());
        }
    }
    
    /**
     * Sync feedrate setting to grbl controller
     */
    private void syncFeedrateToController() {
        try {
            GrblDriver grblDriver = getGrblDriver();
            if (grblDriver == null) {return;}
            
            // Convert to mm/min for grbl
            double feedratePerMin = getFeedratePerSecond()
                .convertToUnits(LengthUnit.Millimeters).getValue() * 60;
            
            int settingId = getFeedrateSettingId();
            if (settingId != -1) {
                grblDriver.syncSettingToController(settingId, feedratePerMin);
                Logger.info("Synced {}-axis feedrate to controller: {} mm/min", getName(), feedratePerMin);
            }
            
        } catch (Exception e) {
            Logger.warn("Failed to sync feedrate to controller for axis {}: {}", getName(), e.getMessage());
        }
    }
    
    /**
     * Sync acceleration setting to grbl controller
     */
    private void syncAccelerationToController() {
        try {
            GrblDriver grblDriver = getGrblDriver();
            if (grblDriver == null) {return;}
            
            // Already in mm/s² for grbl
            double acceleration = getAccelerationPerSecond2()
                .convertToUnits(LengthUnit.Millimeters).getValue();
            
            int settingId = getAccelerationSettingId();
            if (settingId != -1) {
                grblDriver.syncSettingToController(settingId, acceleration);
                Logger.info("Synced {}-axis acceleration to controller: {} mm/s²", getName(), acceleration);
            }
            
        } catch (Exception e) {
            Logger.warn("Failed to sync acceleration to controller for axis {}: {}", getName(), e.getMessage());
        }
    }
    
    /**
     * Sync settings from controller to OpenPnP (reverse direction)
     */
    public void syncFromController() {
        try {
            GrblDriver grblDriver = getGrblDriver();
            if (grblDriver == null || grblDriver.getSettingsSync() == null) {return;}
            
            Logger.info("Syncing {}-axis settings from controller to OpenPnP", getName());
            
            // Sync steps/mm from controller
            syncStepsFromController();
            
            // Sync feedrate from controller  
            syncFeedrateFromController();
            
            // Sync acceleration from controller
            syncAccelerationFromController();

            // Sync pin invert settings from controller
            syncPinInvertsFromController();
            
        } catch (Exception e) {
            Logger.warn("Failed to sync from controller for axis {}: {}", getName(), e.getMessage());
        }
    }
    
    /**
     * Sync steps/mm from controller to OpenPnP
     */
    private void syncStepsFromController() {
        try {
            GrblDriver grblDriver = getGrblDriver();
            int settingId = getStepsSettingId();
            
            if (settingId != -1) {
                String stepsStr = grblDriver.getSettingsSync().getControllerSetting(settingId);
                if (stepsStr != null) {
                    double stepsPerMm = Double.parseDouble(stepsStr);
                    double newResolution = 1.0 / stepsPerMm;
                    
                    // Set directly to avoid triggering sync back to controller
                    super.setResolution(newResolution);
                    Logger.info("Synced {}-axis steps/mm from controller: {} (resolution: {})", 
                        getName(), stepsPerMm, newResolution);
                }
            }
        } catch (Exception e) {
            Logger.warn("Failed to sync steps from controller for axis {}: {}", getName(), e.getMessage());
        }
    }
    
    /**
     * Sync feedrate from controller to OpenPnP
     */
    private void syncFeedrateFromController() {
        try {
            GrblDriver grblDriver = getGrblDriver();
            int settingId = getFeedrateSettingId();
            
            if (settingId != -1) {
                String feedrateStr = grblDriver.getSettingsSync().getControllerSetting(settingId);
                if (feedrateStr != null) {
                    double feedratePerMin = Double.parseDouble(feedrateStr);
                    double feedratePerSec = feedratePerMin / 60.0; // Convert to mm/s
                    
                    Length newFeedrate = new Length(feedratePerSec, LengthUnit.Millimeters);
                    
                    // Set directly to avoid triggering sync back to controller
                    super.setFeedratePerSecond(newFeedrate);
                    Logger.info("Synced {}-axis feedrate from controller: {} mm/min", getName(), feedratePerMin);
                }
            }
        } catch (Exception e) {
            Logger.warn("Failed to sync feedrate from controller for axis {}: {}", getName(), e.getMessage());
        }
    }
    
    /**
     * Sync acceleration from controller to OpenPnP
     */
    private void syncAccelerationFromController() {
        try {
            GrblDriver grblDriver = getGrblDriver();
            int settingId = getAccelerationSettingId();
            
            if (settingId != -1) {
                String accelStr = grblDriver.getSettingsSync().getControllerSetting(settingId);
                if (accelStr != null) {
                    double acceleration = Double.parseDouble(accelStr);
                    Length newAcceleration = new Length(acceleration, LengthUnit.Millimeters);
                    
                    // Set directly to avoid triggering sync back to controller
                    super.setAccelerationPerSecond2(newAcceleration);
                    Logger.info("Synced {}-axis acceleration from controller: {} mm/s²", getName(), acceleration);
                }
            }
        } catch (Exception e) {
            Logger.warn("Failed to sync acceleration from controller for axis {}: {}", getName(), e.getMessage());
        }
    }
    
    // Step enable invert
    public boolean isStepEnableInvert() {
        return stepEnableInvert;
    }
    
    public void setStepEnableInvert(boolean stepEnableInvert) {
        boolean oldValue = this.stepEnableInvert;
        this.stepEnableInvert = stepEnableInvert;
        
        // Sync to controller if changed and connected
        if (oldValue != stepEnableInvert && isConnected()) {
            syncStepEnableInvertToController();
        }
        
        firePropertyChange("stepEnableInvert", oldValue, stepEnableInvert);
    }
    
    /**
     * Sync step enable invert setting to grbl controller ($4)
     */
    private void syncStepEnableInvertToController() {
        try {
            GrblDriver grblDriver = getGrblDriver();
            if (grblDriver == null) {return;}
            
            // Update the driver's bitmask for $4
            grblDriver.updateStepEnableInvertBit(getAxisOffset(), stepEnableInvert);
            
            Logger.info("Synced {}-axis step enable invert to controller: {}", getName(), stepEnableInvert);
            
        } catch (Exception e) {
            Logger.warn("Failed to sync step enable invert to controller for axis {}: {}", getName(), e.getMessage());
        }
    }

    // Step pin invert
    public boolean isStepPinInvert() {
        return stepPinInvert;
    }
    
    public void setStepPinInvert(boolean stepPinInvert) {
        boolean oldValue = this.stepPinInvert;
        this.stepPinInvert = stepPinInvert;
        
        // Sync to controller if changed and connected
        if (oldValue != stepPinInvert && isConnected()) {
            syncStepPinInvertToController();
        }
        
        firePropertyChange("stepPinInvert", oldValue, stepPinInvert);
    }
    
    // Direction pin invert
    public boolean isDirPinInvert() {
        return dirPinInvert;
    }
    
    public void setDirPinInvert(boolean dirPinInvert) {
        boolean oldValue = this.dirPinInvert;
        this.dirPinInvert = dirPinInvert;
        
        // Sync to controller if changed and connected
        if (oldValue != dirPinInvert && isConnected()) {
            syncDirPinInvertToController();
        }
        
        firePropertyChange("dirPinInvert", oldValue, dirPinInvert);
    }
    
    /**
     * Sync step pin invert setting to grbl controller ($2)
     */
    private void syncStepPinInvertToController() {
        try {
            GrblDriver grblDriver = getGrblDriver();
            if (grblDriver == null) {return;}
            
            // Update the driver's bitmask for $2
            grblDriver.updateStepPinInvertBit(getAxisOffset(), stepPinInvert);
            
            Logger.info("Synced {}-axis step pin invert to controller: {}", getName(), stepPinInvert);
            
        } catch (Exception e) {
            Logger.warn("Failed to sync step pin invert to controller for axis {}: {}", getName(), e.getMessage());
        }
    }
    
    /**
     * Sync direction pin invert setting to grbl controller ($3)
     */
    private void syncDirPinInvertToController() {
        try {
            GrblDriver grblDriver = getGrblDriver();
            if (grblDriver == null) {return;}
            
            // Update the driver's bitmask for $3
            grblDriver.updateDirPinInvertBit(getAxisOffset(), dirPinInvert);
            
            Logger.info("Synced {}-axis dir pin invert to controller: {}", getName(), dirPinInvert);
            
        } catch (Exception e) {
            Logger.warn("Failed to sync dir pin invert to controller for axis {}: {}", getName(), e.getMessage());
        }
    }
    
    /**
     * Sync pin invert settings from controller to OpenPnP
     */
    private void syncPinInvertsFromController() {
        try {
            GrblDriver grblDriver = getGrblDriver();
            if (grblDriver == null || grblDriver.getSettingsSync() == null) {
                Logger.debug("Cannot sync pin inverts - no driver or settings sync available");
                return;
            }
            
            int axisOffset = getAxisOffset();
            if (axisOffset == -1) {
                Logger.debug("Cannot sync pin inverts - invalid axis offset for axis {}", getName());
                return;
            }
            
            Logger.info("Syncing pin invert settings from controller for {}-axis (letter={}, offset={})", 
                    getName(), getLetter(), axisOffset);
            
            // Sync step pin invert from controller ($2)
            String stepInvertStr = grblDriver.getSettingsSync().getControllerSetting(2);
            if (stepInvertStr != null) {
                try {
                    int stepInvertMask = Integer.parseInt(stepInvertStr);
                    boolean axisStepInvert = (stepInvertMask & (1 << axisOffset)) != 0;
                    
                    // Set directly to avoid triggering sync back to controller
                    if (this.stepPinInvert != axisStepInvert) {
                        this.stepPinInvert = axisStepInvert;
                        firePropertyChange("stepPinInvert", !axisStepInvert, axisStepInvert);
                        Logger.info("Synced {}-axis step pin invert from controller: {}", getName(), axisStepInvert);
                    }
                } catch (NumberFormatException e) {
                    Logger.warn("Invalid step pin invert mask from controller: {}", stepInvertStr);
                }
            }
            
            // Sync dir pin invert from controller ($3)
            String dirInvertStr = grblDriver.getSettingsSync().getControllerSetting(3);
            if (dirInvertStr != null) {
                try {
                    int dirInvertMask = Integer.parseInt(dirInvertStr);
                    boolean axisDirInvert = (dirInvertMask & (1 << axisOffset)) != 0;
                    
                    // Set directly to avoid triggering sync back to controller
                    if (this.dirPinInvert != axisDirInvert) {
                        this.dirPinInvert = axisDirInvert;
                        firePropertyChange("dirPinInvert", !axisDirInvert, axisDirInvert);
                        Logger.info("Synced {}-axis dir pin invert from controller: {}", getName(), axisDirInvert);
                    }
                } catch (NumberFormatException e) {
                    Logger.warn("Invalid dir pin invert mask from controller: {}", dirInvertStr);
                }
            }
            
            // Sync step enable invert from controller ($4)
            String stepEnableInvertStr = grblDriver.getSettingsSync().getControllerSetting(4);
            if (stepEnableInvertStr != null) {
                try {
                    int stepEnableInvertMask = Integer.parseInt(stepEnableInvertStr);
                    boolean axisStepEnableInvert = (stepEnableInvertMask & (1 << axisOffset)) != 0;
                    
                    // Set directly to avoid triggering sync back to controller
                    if (this.stepEnableInvert != axisStepEnableInvert) {
                        this.stepEnableInvert = axisStepEnableInvert;
                        firePropertyChange("stepEnableInvert", !axisStepEnableInvert, axisStepEnableInvert);
                        Logger.info("Synced {}-axis step enable invert from controller: {}", getName(), axisStepEnableInvert);
                    }
                } catch (NumberFormatException e) {
                    Logger.warn("Invalid step enable invert mask from controller: {}", stepEnableInvertStr);
                }
            }

            // Sync ganged motor invert from controller ($8) - only if supported
            if (shouldShowGangedMotorSettings()) {
                String gangedInvertStr = grblDriver.getSettingsSync().getControllerSetting(8);
                if (gangedInvertStr != null) {
                    try {
                        int gangedInvertMask = Integer.parseInt(gangedInvertStr);
                        boolean axisGangedInvert = (gangedInvertMask & (1 << axisOffset)) != 0;
                        
                        // Set directly to avoid triggering sync back to controller
                        if (this.gangedMotorInvert != axisGangedInvert) {
                            this.gangedMotorInvert = axisGangedInvert;
                            firePropertyChange("gangedMotorInvert", !axisGangedInvert, axisGangedInvert);
                            Logger.info("Synced {}-axis ganged motor invert from controller: {}", getName(), axisGangedInvert);
                        }
                    } catch (NumberFormatException e) {
                        Logger.warn("Invalid ganged motor invert mask from controller: {}", gangedInvertStr);
                    }
                }
            }
            
        } catch (Exception e) {
            Logger.warn("Failed to sync pin inverts from controller for axis {}: {}", getName(), e.getMessage());
        }
    }

    /**
     * Sync ganged motor invert setting to grbl controller ($8)
     */
    private void syncGangedMotorInvertToController() {
        try {
            GrblDriver grblDriver = getGrblDriver();
            if (grblDriver == null) {return;}
            
            // Update the driver's bitmask for $8
            grblDriver.updateGangedMotorInvertBit(getAxisOffset(), gangedMotorInvert);
            
            Logger.info("Synced {}-axis ganged motor invert to controller: {}", getName(), gangedMotorInvert);
            
        } catch (Exception e) {
            Logger.warn("Failed to sync ganged motor invert to controller for axis {}: {}", getName(), e.getMessage());
        }
    }
    
    
    /**
     * Get GrblDriver instance
     */
    private GrblDriver getGrblDriver() {
        if (getDriver() instanceof GrblDriver) {
            return (GrblDriver) getDriver();
        }
        return null;
    }
    
    /**
     * Check if driver is connected
     */
    private boolean isConnected() {
        Driver driver = getDriver();
        if (driver instanceof GrblDriver) {
            return ((GrblDriver) driver).isConnected();
        }
        return false;
    }
    
    /**
     * Get grbl setting ID for steps/mm
     */
    private int getStepsSettingId() {
        return getAxisSettingId("steps");
    }
    
    /**
     * Get grbl setting ID for feedrate
     */
    private int getFeedrateSettingId() {
        return getAxisSettingId("feedrate");
    }
    
    /**
     * Get grbl setting ID for acceleration
     */
    private int getAccelerationSettingId() {
        return getAxisSettingId("acceleration");
    }
    
    /**
     * Get grbl setting ID for axis and setting type
     */
    private int getAxisSettingId(String settingType) {
        int axisOffset = getAxisOffset();
        if (axisOffset == -1) {return -1;}
        
        switch (settingType.toLowerCase()) {
            case "steps":
                return 100 + axisOffset; // $100-$105 for X,Y,Z,A,B,C
            case "feedrate":
                return 110 + axisOffset; // $110-$115 for X,Y,Z,A,B,C
            case "acceleration":
                return 120 + axisOffset; // $120-$125 for X,Y,Z,A,B,C
            default:
                return -1;
        }
    }
    
    /**
     * Get axis offset for grbl setting IDs
     */
    private int getAxisOffset() {
        switch (getLetter().toUpperCase()) {
            case "X": return 0;
            case "Y": return 1;
            case "Z": return 2;
            case "A": return 3;
            case "B": return 4;
            case "C": return 5;
            default: 
                Logger.warn("Unknown axis name for grbl settings: {}", getName());
                return -1;
        }
    }

    public boolean isGangedMotorInvert() {
        return gangedMotorInvert;
    }

    public void setGangedMotorInvert(boolean gangedMotorInvert) {
        boolean oldValue = this.gangedMotorInvert;
        this.gangedMotorInvert = gangedMotorInvert;
        
        // Sync to controller if changed and connected
        if (oldValue != gangedMotorInvert && isConnected()) {
            syncGangedMotorInvertToController();
        }
        
        firePropertyChange("gangedMotorInvert", oldValue, gangedMotorInvert);
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        // Get parent's property sheets first
        PropertySheet[] parentSheets = super.getPropertySheets();
        
        // Create our Grbl-specific sheet
        PropertySheet grblSheet = new PropertySheetWizardAdapter(createConfigurationWizard(), "Grbl Settings");
        
        // Combine parent sheets with our new sheet
        PropertySheet[] combinedSheets = new PropertySheet[parentSheets.length + 1];
        System.arraycopy(parentSheets, 0, combinedSheets, 0, parentSheets.length);
        combinedSheets[parentSheets.length] = grblSheet;
        
        return combinedSheets;
    }
    
    /**
     * Create configuration wizard for this axis
     */
    public AbstractConfigurationWizard createConfigurationWizard() {
        return new GrblControllerAxisConfigurationWizard(this);
    }

    /**
     * Check if this axis should show ganged motor settings
     * Uses cached result from GrblSettingsSync
     */
    public boolean shouldShowGangedMotorSettings() {
        // Check if this axis type commonly uses ganged motors
        String axisLetter = getLetter();
        if (axisLetter == null) {
            return false;
        }
        
        String letter = axisLetter.toUpperCase();
        boolean isCommonGangedAxis = "Y".equals(letter) || "Z".equals(letter);
        
        if (!isCommonGangedAxis) {
            return false;  // Only Y/Z axes typically have ganged motors
        }
        
        // Check if controller supports ganged motors (from cached sync result)
        GrblDriver grblDriver = getGrblDriver();
        if (grblDriver == null || grblDriver.getSettingsSync() == null) {
            Logger.debug("No driver or settings sync available for ganged motor check");
            return false;
        }
        
        boolean supported = grblDriver.getSettingsSync().isGangedMotorSupported();
        Logger.debug("Ganged motor support for {}-axis: {}", letter, supported);
        
        return supported;
    }
}