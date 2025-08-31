package org.openpnp.machine.grbl.driver;

import java.util.List;
import javax.swing.SwingUtilities;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.machine.reference.driver.GcodeDriver;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.Collect;
import org.openpnp.machine.grbl.driver.wizards.GrblDriverConfigurationWizard;
import org.openpnp.machine.grbl.gui.GrblSyncDialog;
import org.openpnp.machine.grbl.wizards.GrblSettingsSync;
import org.openpnp.machine.grbl.wizards.SettingDiscrepancy;
import org.pmw.tinylog.Logger;

/**
 * Enhanced GcodeDriver with bi-directional settings synchronization for 
 * grbl and grblHAL controllers. Automatically syncs axis settings between 
 * OpenPnP configuration and controller flash/EEPROM memory.
 * 
 * Features:
 * - Automatic sync check on connect
 * - Bi-directional sync (OpenPnP <-> Controller)
 * - GUI dialog for handling discrepancies
 * - Support for steps/mm, feedrate, and acceleration
 * - Future support for homing and I/O settings
 */
public class GrblDriver extends GcodeDriver {
    
    private GrblSettingsSync settingsSync;
    
    // === GRBL SETTINGS PROPERTIES (NO @Attribute - runtime only!) ===
    private int stepPulse = 10;        // $0 - Step pulse time in microseconds (default 10)
    private int stepIdleDelay = 25;    // $1 - Step idle delay in milliseconds (default 25)
    private int stepPinInvertMask = 0;     // $2 - Step pin invert bitmask
    private int dirPinInvertMask = 0;      // $3 - Direction pin invert bitmask  
    private int stepEnableInvertMask = 0;     // $4 - Step enable invert bitmask
    private int gangedMotorInvertMask = 0;     // $8 - Ganged motor invert bitmask
    
    // Homing settings
    private boolean homingEnabled = false;
    private boolean homingInvertX = false;
    private boolean homingInvertY = false;
    private boolean homingInvertZ = false;
    private double homingFeedRate = 25.0;
    private double homingSeekRate = 500.0;
    private int homingDebounce = 250;
    private double homingPulloff = 1.0;
    
    // Homing passes (grblHAL)
    private int homingPass1 = 0; // $44
    private int homingPass2 = 0; // $45
    private int homingPass3 = 0; // $46
    
    // Limits settings
    private boolean softLimitsEnabled = false;
    private boolean hardLimitsEnabled = false;
    private double xMaxTravel = 200.0;
    private double yMaxTravel = 200.0;
    private double zMaxTravel = 200.0;
    private int limitPinInvertMask = 0;
    private boolean limitInvertX = false;
    private boolean limitInvertY = false;
    private boolean limitInvertZ = false;
    private boolean limitInvertA = false;
    private boolean limitInvertB = false;
    private boolean limitInvertC = false;

    @Override
    public synchronized void connect() throws Exception {
        // Call parent connect first
        super.connect();
        
        // Initialize settings sync after successful connection
        if (connected && isGrblFirmware()) {
            initializeSettingsSync();
            performSettingsSyncCheck();
        }
    }

    // Override to provide custom configuration wizard
    @Override
    public PropertySheetHolder.PropertySheet[] getPropertySheets() {
        // Get parent sheets from GcodeDriver (med Apply/Reset!)
        PropertySheetHolder.PropertySheet[] parentSheets = super.getPropertySheets();
        
        // Add our Grbl-specific sheet
        return Collect.concat(
            parentSheets,
            new PropertySheetHolder.PropertySheet[] {
                new PropertySheetWizardAdapter(new GrblDriverConfigurationWizard(this), "Grbl Settings")
            }
        );
    }
    
    /**
     * Check if connected firmware is grbl or grblHAL
     */
    private boolean isGrblFirmware() {
        try {
            String firmwareName = getFirmwareProperty("FIRMWARE_NAME", "").toLowerCase();
            boolean isGrbl = firmwareName.contains("grbl");
            
            if (isGrbl) {
                Logger.info("Detected grbl-compatible firmware: {}", firmwareName);
            } else {
                Logger.info("Non-grbl firmware detected: {} - settings sync disabled", firmwareName);
            }
            
            return isGrbl;
            
        } catch (Exception e) {
            Logger.warn("Failed to detect firmware type: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Initialize the settings synchronization system
     */
    private void initializeSettingsSync() {
        try {
            settingsSync = new GrblSettingsSync(this);
            Logger.info("Grbl settings synchronization initialized");
        } catch (Exception e) {
            Logger.error("Failed to initialize settings sync: {}", e.getMessage());
            settingsSync = null;
        }
    }
    
    /**
     * Perform settings sync check between OpenPnP and controller
     */
    private void performSettingsSyncCheck() {
        if (settingsSync == null) {
            Logger.debug("Settings sync not available - skipping sync check");
            return;
        }
        
        try {
            Logger.info("Performing grbl settings sync check...");
            
            // Read current settings from controller
            settingsSync.readSettingsFromController();
            
            // Check for discrepancies
            List<SettingDiscrepancy> discrepancies = settingsSync.checkForDiscrepancies();
            
            // Handle discrepancies
            if (!discrepancies.isEmpty()) {
                handleSettingsDiscrepancies(discrepancies);
            } else {
                Logger.info("Settings sync check: All settings match between OpenPnP and controller");
            }
            
        } catch (Exception e) {
            Logger.warn("Failed to perform settings sync check: {}", e.getMessage());
        }
    }
    
    /**
     * Handle settings discrepancies with GUI dialog
     */
    private void handleSettingsDiscrepancies(List<SettingDiscrepancy> discrepancies) {
        Logger.warn("Found {} settings discrepancies between OpenPnP and controller", discrepancies.size());
        
        // Log discrepancies
        for (SettingDiscrepancy discrepancy : discrepancies) {
            Logger.warn("  {}", discrepancy.toString());
        }
        
        // Show GUI dialog on EDT thread
        SwingUtilities.invokeLater(() -> {
            try {
                GrblSyncDialog dialog = new GrblSyncDialog(
                    MainFrame.get(), 
                    discrepancies, 
                    settingsSync
                );
                dialog.setVisible(true);
            } catch (Exception e) {
                Logger.error("Failed to show sync dialog: {}", e.getMessage());
            }
        });
    }
    
    /**
     * Get the settings sync manager (for external access)
     */
    public GrblSettingsSync getSettingsSync() {
        return settingsSync;
    }
    
    /**
     * Manually trigger a settings sync check
     */
    public void triggerSettingsSyncCheck() {
        if (connected && settingsSync != null) {
            performSettingsSyncCheck();
        } else {
            Logger.warn("Cannot perform settings sync - driver not connected or sync not available");
        }
    }
    
    /**
     * Sync specific setting from OpenPnP to controller
     */
    public void syncSettingToController(int settingId, double value) throws Exception {
        if (settingsSync == null) {
            throw new Exception("Settings sync not available");
        }
        
        settingsSync.writeSettingToController(settingId, String.format("%.3f", value));
        Logger.info("Synced setting ${} to controller: {}", settingId, value);
    }
    
    /**
     * Re-read settings from controller and update cache
     */
    public void refreshControllerSettings() throws Exception {
        if (settingsSync == null) {
            throw new Exception("Settings sync not available");
        }
        
        settingsSync.readSettingsFromController();
        Logger.info("Refreshed controller settings cache");
    }

    /**
     * Check if the driver is currently connected to the controller
     */
    public boolean isConnected() {
        return connected;
    }

    // Step pulse ($0)
    public int getStepPulse() {
        return stepPulse;
    }

    public void setStepPulse(int stepPulse) {
        int oldValue = this.stepPulse;
        this.stepPulse = stepPulse;
        firePropertyChange("stepPulse", oldValue, stepPulse);
    }

    // Step idle delay ($1)  
    public int getStepIdleDelay() {
        return stepIdleDelay;
    }

    public void setStepIdleDelay(int stepIdleDelay) {
        int oldValue = this.stepIdleDelay;
        this.stepIdleDelay = stepIdleDelay;
        firePropertyChange("stepIdleDelay", oldValue, stepIdleDelay);
    }

    // === STEP PIN INVERT ($2) ===

    public int getStepPinInvertMask() {
        return stepPinInvertMask;
    }

    public void setStepPinInvertMask(int mask) {
        int oldValue = this.stepPinInvertMask;
        this.stepPinInvertMask = mask;
        firePropertyChange("stepPinInvertMask", oldValue, mask);
    }

    // Helper methods for individual bits (for GUI convenience)
    public boolean isStepInvertX() { return (stepPinInvertMask & 1) != 0; }
    public boolean isStepInvertY() { return (stepPinInvertMask & 2) != 0; }
    public boolean isStepInvertZ() { return (stepPinInvertMask & 4) != 0; }
    public boolean isStepInvertA() { return (stepPinInvertMask & 8) != 0; }
    public boolean isStepInvertB() { return (stepPinInvertMask & 16) != 0; }
    public boolean isStepInvertC() { return (stepPinInvertMask & 32) != 0; }

    public void setStepInvertX(boolean invert) { 
        setStepPinInvertMask(invert ? (stepPinInvertMask | 1) : (stepPinInvertMask & ~1));
    }
    public void setStepInvertY(boolean invert) { 
        setStepPinInvertMask(invert ? (stepPinInvertMask | 2) : (stepPinInvertMask & ~2));
    }
    public void setStepInvertZ(boolean invert) { 
        setStepPinInvertMask(invert ? (stepPinInvertMask | 4) : (stepPinInvertMask & ~4));
    }
    public void setStepInvertA(boolean invert) { 
        setStepPinInvertMask(invert ? (stepPinInvertMask | 8) : (stepPinInvertMask & ~8));
    }
    public void setStepInvertB(boolean invert) { 
        setStepPinInvertMask(invert ? (stepPinInvertMask | 16) : (stepPinInvertMask & ~16));
    }
    public void setStepInvertC(boolean invert) { 
        setStepPinInvertMask(invert ? (stepPinInvertMask | 32) : (stepPinInvertMask & ~32));
    }

    // === DIRECTION PIN INVERT ($3) ===

    public int getDirPinInvertMask() {
        return dirPinInvertMask;
    }

    public void setDirPinInvertMask(int mask) {
        int oldValue = this.dirPinInvertMask;
        this.dirPinInvertMask = mask;
        firePropertyChange("dirPinInvertMask", oldValue, mask);
    }

    // Helper methods for individual bits
    public boolean isDirInvertX() { return (dirPinInvertMask & 1) != 0; }
    public boolean isDirInvertY() { return (dirPinInvertMask & 2) != 0; }
    public boolean isDirInvertZ() { return (dirPinInvertMask & 4) != 0; }
    public boolean isDirInvertA() { return (dirPinInvertMask & 8) != 0; }
    public boolean isDirInvertB() { return (dirPinInvertMask & 16) != 0; }
    public boolean isDirInvertC() { return (dirPinInvertMask & 32) != 0; }

    public void setDirInvertX(boolean invert) { 
        setDirPinInvertMask(invert ? (dirPinInvertMask | 1) : (dirPinInvertMask & ~1));
    }
    public void setDirInvertY(boolean invert) { 
        setDirPinInvertMask(invert ? (dirPinInvertMask | 2) : (dirPinInvertMask & ~2));
    }
    public void setDirInvertZ(boolean invert) { 
        setDirPinInvertMask(invert ? (dirPinInvertMask | 4) : (dirPinInvertMask & ~4));
    }
    public void setDirInvertA(boolean invert) { 
        setDirPinInvertMask(invert ? (dirPinInvertMask | 8) : (dirPinInvertMask & ~8));
    }
    public void setDirInvertB(boolean invert) { 
        setDirPinInvertMask(invert ? (dirPinInvertMask | 16) : (dirPinInvertMask & ~16));
    }
    public void setDirInvertC(boolean invert) { 
        setDirPinInvertMask(invert ? (dirPinInvertMask | 32) : (dirPinInvertMask & ~32));
    }   
    
    // === STEP ENABLE INVERT ($4) ===

    public int getStepEnableInvertMask() {
        return stepEnableInvertMask;
    }

    public void setStepEnableInvertMask(int mask) {
        int oldValue = this.stepEnableInvertMask;
        this.stepEnableInvertMask = mask;
        firePropertyChange("stepEnableInvertMask", oldValue, mask);
    }

    // Helper methods for individual bits
    public boolean isStepEnableInvertX() { return (stepEnableInvertMask & 1) != 0; }
    public boolean isStepEnableInvertY() { return (stepEnableInvertMask & 2) != 0; }
    public boolean isStepEnableInvertZ() { return (stepEnableInvertMask & 4) != 0; }
    public boolean isStepEnableInvertA() { return (stepEnableInvertMask & 8) != 0; }
    public boolean isStepEnableInvertB() { return (stepEnableInvertMask & 16) != 0; }
    public boolean isStepEnableInvertC() { return (stepEnableInvertMask & 32) != 0; }
    
    public void setStepEnableInvertX(boolean invert) { 
        setStepEnableInvertMask(invert ? (stepEnableInvertMask | 1) : (stepEnableInvertMask & ~1));
    }
    public void setStepEnableInvertY(boolean invert) { 
        setStepEnableInvertMask(invert ? (stepEnableInvertMask | 2) : (stepEnableInvertMask & ~2));
    }
    public void setStepEnableInvertZ(boolean invert) { 
        setStepEnableInvertMask(invert ? (stepEnableInvertMask | 4) : (stepEnableInvertMask & ~4));
    }
    public void setStepEnableInvertA(boolean invert) { 
        setStepEnableInvertMask(invert ? (stepEnableInvertMask | 8) : (stepEnableInvertMask & ~8));
    }
    public void setStepEnableInvertB(boolean invert) { 
        setStepEnableInvertMask(invert ? (stepEnableInvertMask | 16) : (stepEnableInvertMask & ~16));
    }
    public void setStepEnableInvertC(boolean invert) { 
        setStepEnableInvertMask(invert ? (stepEnableInvertMask | 32) : (stepEnableInvertMask & ~32));
    }

    // === GANGED MOTOR INVERT ($8) ===
    public int getGangedMotorInvertMask() {
        return gangedMotorInvertMask;
    }

    public void setGangedMotorInvertMask(int mask) {
        int oldValue = this.gangedMotorInvertMask;
        this.gangedMotorInvertMask = mask;
        firePropertyChange("gangedMotorInvertMask", oldValue, mask);
    }

    // Helper methods for individual bits
    public boolean isGangedInvertX() { return (gangedMotorInvertMask & 1) != 0; }
    public boolean isGangedInvertY() { return (gangedMotorInvertMask & 2) != 0; }
    public boolean isGangedInvertZ() { return (gangedMotorInvertMask & 4) != 0; }
    public boolean isGangedInvertA() { return (gangedMotorInvertMask & 8) != 0; }
    public boolean isGangedInvertB() { return (gangedMotorInvertMask & 16) != 0; }
    public boolean isGangedInvertC() { return (gangedMotorInvertMask & 32) != 0; }

    public void setGangedInvertX(boolean invert) { 
        setGangedMotorInvertMask(invert ? (gangedMotorInvertMask | 1) : (gangedMotorInvertMask & ~1));
    }
    public void setGangedInvertY(boolean invert) { 
        setGangedMotorInvertMask(invert ? (gangedMotorInvertMask | 2) : (gangedMotorInvertMask & ~2));
    }
    public void setGangedInvertZ(boolean invert) { 
        setGangedMotorInvertMask(invert ? (gangedMotorInvertMask | 4) : (gangedMotorInvertMask & ~4));
    }
    public void setGangedInvertA(boolean invert) { 
        setGangedMotorInvertMask(invert ? (gangedMotorInvertMask | 8) : (gangedMotorInvertMask & ~8));
    }
    public void setGangedInvertB(boolean invert) { 
        setGangedMotorInvertMask(invert ? (gangedMotorInvertMask | 16) : (gangedMotorInvertMask & ~16));
    }
    public void setGangedInvertC(boolean invert) { 
        setGangedMotorInvertMask(invert ? (gangedMotorInvertMask | 32) : (gangedMotorInvertMask & ~32));
    }

    // Homing enabled
    public boolean isHomingEnabled() {
        return homingEnabled;
    }
    
    public void setHomingEnabled(boolean homingEnabled) {
        boolean oldValue = this.homingEnabled;
        this.homingEnabled = homingEnabled;
        firePropertyChange("homingEnabled", oldValue, homingEnabled);
    }
    
    // Homing direction individual checkboxes
    public boolean isHomingInvertX() {
        return homingInvertX;
    }
    
    public void setHomingInvertX(boolean homingInvertX) {
        boolean oldValue = this.homingInvertX;
        this.homingInvertX = homingInvertX;
        firePropertyChange("homingInvertX", oldValue, homingInvertX);
    }
    
    public boolean isHomingInvertY() {
        return homingInvertY;
    }
    
    public void setHomingInvertY(boolean homingInvertY) {
        boolean oldValue = this.homingInvertY;
        this.homingInvertY = homingInvertY;
        firePropertyChange("homingInvertY", oldValue, homingInvertY);
    }
    
    public boolean isHomingInvertZ() {
        return homingInvertZ;
    }
    
    public void setHomingInvertZ(boolean homingInvertZ) {
        boolean oldValue = this.homingInvertZ;
        this.homingInvertZ = homingInvertZ;
        firePropertyChange("homingInvertZ", oldValue, homingInvertZ);
    }
    
    // Homing direction mask (converts from individual checkboxes)
    public int getHomingDirectionMask() {
        int mask = 0;
        if (homingInvertX) {
            mask |= 1;
        }
        if (homingInvertY) {
            mask |= 2;
        }
        if (homingInvertZ) {
            mask |= 4;
        }
        return mask;
    }
    
    public void setHomingDirectionMask(int mask) {
        setHomingInvertX((mask & 1) != 0);
        setHomingInvertY((mask & 2) != 0);
        setHomingInvertZ((mask & 4) != 0);
        firePropertyChange("homingDirectionMask", null, mask);
    }
    
    // Homing feed rate
    public double getHomingFeedRate() {
        return homingFeedRate;
    }
    
    public void setHomingFeedRate(double homingFeedRate) {
        double oldValue = this.homingFeedRate;
        this.homingFeedRate = homingFeedRate;
        firePropertyChange("homingFeedRate", oldValue, homingFeedRate);
    }
    
    // Homing seek rate
    public double getHomingSeekRate() {
        return homingSeekRate;
    }
    
    public void setHomingSeekRate(double homingSeekRate) {
        double oldValue = this.homingSeekRate;
        this.homingSeekRate = homingSeekRate;
        firePropertyChange("homingSeekRate", oldValue, homingSeekRate);
    }
    
    // Homing debounce
    public int getHomingDebounce() {
        return homingDebounce;
    }
    
    public void setHomingDebounce(int homingDebounce) {
        int oldValue = this.homingDebounce;
        this.homingDebounce = homingDebounce;
        firePropertyChange("homingDebounce", oldValue, homingDebounce);
    }
    
    // Homing pulloff
    public double getHomingPulloff() {
        return homingPulloff;
    }
    
    public void setHomingPulloff(double homingPulloff) {
        double oldValue = this.homingPulloff;
        this.homingPulloff = homingPulloff;
        firePropertyChange("homingPulloff", oldValue, homingPulloff);
    }

    // === HOMING PASSES (grblHAL) ===
    
    // Pass 1 ($44)
    public int getHomingPass1() {
        return homingPass1;
    }
    
    public void setHomingPass1(int homingPass1) {
        int oldValue = this.homingPass1;
        this.homingPass1 = homingPass1;
        setHomingPass1X((homingPass1 & 1) != 0);
        setHomingPass1Y((homingPass1 & 2) != 0);
        setHomingPass1Z((homingPass1 & 4) != 0);
        firePropertyChange("homingPass1X", oldValue, homingPass1);
    }
    
    // Pass 2 ($45)
    public int getHomingPass2() {
        return homingPass2;
    }
    
    public void setHomingPass2(int homingPass2) {
        int oldValue = this.homingPass2;
        this.homingPass2 = homingPass2;
        setHomingPass2X((homingPass2 & 1) != 0);
        setHomingPass2Y((homingPass2 & 2) != 0);
        setHomingPass2Z((homingPass2 & 4) != 0);
        firePropertyChange("homingPass2", oldValue, homingPass2);
    }
    
    // Pass 3 ($46)
    public int getHomingPass3() {
        return homingPass3;
    }
    
    public void setHomingPass3(int homingPass3) {
        int oldValue = this.homingPass3;
        this.homingPass3 = homingPass3;
        setHomingPass3X((homingPass3 & 1) != 0);
        setHomingPass3Y((homingPass3 & 2) != 0);
        setHomingPass3Z((homingPass3 & 4) != 0);
        firePropertyChange("homingPass3", oldValue, homingPass3);
    }

    // Helper methods for Pass 1
    public boolean isHomingPass1X() {
        return (homingPass1 & 1) != 0;
    }
    
    public boolean isHomingPass1Y() {
        return (homingPass1 & 2) != 0;
    }
    
    public boolean isHomingPass1Z() {
        return (homingPass1 & 4) != 0;
    }

    public void setHomingPass1X(boolean enabled) {
        if (enabled) {
            homingPass1 |= 1;
        } else {
            homingPass1 &= ~1;
        }
        firePropertyChange("homingPass1X", !enabled, enabled);
    }
    
    public void setHomingPass1Y(boolean enabled) {
        if (enabled) {
            homingPass1 |= 2;
        } else {
            homingPass1 &= ~2;
        }
        firePropertyChange("homingPass1Y", !enabled, enabled);
    }
    
    public void setHomingPass1Z(boolean enabled) {
        if (enabled) {
            homingPass1 |= 4;
        } else {
            homingPass1 &= ~4;
        }
        firePropertyChange("homingPass1Z", !enabled, enabled);
    }

    // Helper methods for Pass 2
    public boolean isHomingPass2X() {
        return (homingPass2 & 1) != 0;
    }
    
    public boolean isHomingPass2Y() {
        return (homingPass2 & 2) != 0;
    }
    
    public boolean isHomingPass2Z() {
        return (homingPass2 & 4) != 0;
    }

    public void setHomingPass2X(boolean enabled) {
        if (enabled) {
            homingPass2 |= 1;
        } else {
            homingPass2 &= ~1;
        }
        firePropertyChange("homingPass2X", !enabled, enabled);
    }
    
    public void setHomingPass2Y(boolean enabled) {
        if (enabled) {
            homingPass2 |= 2;
        } else {
            homingPass2 &= ~2;
        }
        firePropertyChange("homingPass2Y", !enabled, enabled);
    }
    
    public void setHomingPass2Z(boolean enabled) {
        if (enabled) {
            homingPass2 |= 4;
        } else {
            homingPass2 &= ~4;
        }
        firePropertyChange("homingPass2Z", !enabled, enabled);
    }

    // Helper methods for Pass 3
    public boolean isHomingPass3X() {
        return (homingPass3 & 1) != 0;
    }
    
    public boolean isHomingPass3Y() {
        return (homingPass3 & 2) != 0;
    }
    
    public boolean isHomingPass3Z() {
        return (homingPass3 & 4) != 0;
    }

    public void setHomingPass3X(boolean enabled) {
        if (enabled) {
            homingPass3 |= 1;
        } else {
            homingPass3 &= ~1;
        }
        firePropertyChange("homingPass3X", !enabled, enabled);
    }
    
    public void setHomingPass3Y(boolean enabled) {
        if (enabled) {
            homingPass3 |= 2;
        } else {
            homingPass3 &= ~2;
        }
        firePropertyChange("homingPass3Y", !enabled, enabled);
    }
    
    public void setHomingPass3Z(boolean enabled) {
        if (enabled) {
            homingPass3 |= 4;
        } else {
            homingPass3 &= ~4;
        }
        firePropertyChange("homingPass3Z", !enabled, enabled);
    }

    // Soft and hard limits  
    public boolean isSoftLimitsEnabled() {
        return softLimitsEnabled;
    }
    
    public void setSoftLimitsEnabled(boolean softLimitsEnabled) {
        boolean oldValue = this.softLimitsEnabled;
        this.softLimitsEnabled = softLimitsEnabled;
        firePropertyChange("softLimitsEnabled", oldValue, softLimitsEnabled);
    }
    
    public boolean isHardLimitsEnabled() {
        return hardLimitsEnabled;
    }
    
    public void setHardLimitsEnabled(boolean hardLimitsEnabled) {
        boolean oldValue = this.hardLimitsEnabled;
        this.hardLimitsEnabled = hardLimitsEnabled;
        firePropertyChange("hardLimitsEnabled", oldValue, hardLimitsEnabled);
    }
    
    // Max travel settings
    public double getXMaxTravel() {
        return xMaxTravel;
    }
    
    public void setXMaxTravel(double xMaxTravel) {
        double oldValue = this.xMaxTravel;
        this.xMaxTravel = xMaxTravel;
        firePropertyChange("XMaxTravel", oldValue, xMaxTravel);
    }
    
    public double getYMaxTravel() {
        return yMaxTravel;
    }
    
    public void setYMaxTravel(double yMaxTravel) {
        double oldValue = this.yMaxTravel;
        this.yMaxTravel = yMaxTravel;
        firePropertyChange("YMaxTravel", oldValue, yMaxTravel);
    }
    
    public double getZMaxTravel() {
        return zMaxTravel;
    }
    
    public void setZMaxTravel(double zMaxTravel) {
        double oldValue = this.zMaxTravel;
        this.zMaxTravel = zMaxTravel;
        firePropertyChange("ZMaxTravel", oldValue, zMaxTravel);
    }

        // Limit pin invert mask ($5)
    public int getLimitPinInvertMask() {
        return limitPinInvertMask;
    }
    
    public void setLimitPinInvertMask(int mask) {
        int oldValue = this.limitPinInvertMask;
        this.limitPinInvertMask = mask;
        
        // Update individual checkboxes from mask
        setLimitInvertX((mask & 1) != 0);
        setLimitInvertY((mask & 2) != 0);
        setLimitInvertZ((mask & 4) != 0);
        setLimitInvertA((mask & 8) != 0);
        setLimitInvertB((mask & 16) != 0);
        setLimitInvertC((mask & 32) != 0);
        
        firePropertyChange("limitPinInvertMask", oldValue, mask);
    }
    
    // Individual limit invert getters/setters
    public boolean isLimitInvertX() {
        return limitInvertX;
    }
    
    public void setLimitInvertX(boolean limitInvertX) {
        boolean oldValue = this.limitInvertX;
        this.limitInvertX = limitInvertX;
        updateLimitInvertMask();
        firePropertyChange("limitInvertX", oldValue, limitInvertX);
    }
    
    public boolean isLimitInvertY() {
        return limitInvertY;
    }
    
    public void setLimitInvertY(boolean limitInvertY) {
        boolean oldValue = this.limitInvertY;
        this.limitInvertY = limitInvertY;
        updateLimitInvertMask();
        firePropertyChange("limitInvertY", oldValue, limitInvertY);
    }
    
    public boolean isLimitInvertZ() {
        return limitInvertZ;
    }
    
    public void setLimitInvertZ(boolean limitInvertZ) {
        boolean oldValue = this.limitInvertZ;
        this.limitInvertZ = limitInvertZ;
        updateLimitInvertMask();
        firePropertyChange("limitInvertZ", oldValue, limitInvertZ);
    }
    
    public boolean isLimitInvertA() {
        return limitInvertA;
    }
    
    public void setLimitInvertA(boolean limitInvertA) {
        boolean oldValue = this.limitInvertA;
        this.limitInvertA = limitInvertA;
        updateLimitInvertMask();
        firePropertyChange("limitInvertA", oldValue, limitInvertA);
    }
    
    public boolean isLimitInvertB() {
        return limitInvertB;
    }
    
    public void setLimitInvertB(boolean limitInvertB) {
        boolean oldValue = this.limitInvertB;
        this.limitInvertB = limitInvertB;
        updateLimitInvertMask();
        firePropertyChange("limitInvertB", oldValue, limitInvertB);
    }
    
    public boolean isLimitInvertC() {
        return limitInvertC;
    }
    
    public void setLimitInvertC(boolean limitInvertC) {
        boolean oldValue = this.limitInvertC;
        this.limitInvertC = limitInvertC;
        updateLimitInvertMask();
        firePropertyChange("limitInvertC", oldValue, limitInvertC);
    }
    
    /**
     * Update the limit invert mask from individual checkbox states
     */
    private void updateLimitInvertMask() {
        int mask = 0;
        if (limitInvertX) mask |= 1;
        if (limitInvertY) mask |= 2;
        if (limitInvertZ) mask |= 4;
        if (limitInvertA) mask |= 8;
        if (limitInvertB) mask |= 16;
        if (limitInvertC) mask |= 32;
        
        if (mask != limitPinInvertMask) {
            int oldValue = limitPinInvertMask;
            limitPinInvertMask = mask;
            firePropertyChange("limitPinInvertMask", oldValue, mask);
        }
    }

    // === METHODS FOR GRBLCONTROLLERAXIS TO UPDATE BITMASKS ===
    
    /**
     * Update a specific bit in the step pin invert mask ($2)
     * Called by GrblControllerAxis when individual axis setting changes
     */
    public void updateStepPinInvertBit(int axisOffset, boolean invert) throws Exception {
        if (axisOffset < 0 || axisOffset > 5) {
            throw new Exception("Invalid axis offset: " + axisOffset);
        }
        
        if (settingsSync == null) {
            throw new Exception("Settings sync not available");
        }
        
        // Read current mask from controller
        String currentMaskStr = settingsSync.getControllerSetting(2);
        int currentMask = (currentMaskStr != null) ? Integer.parseInt(currentMaskStr) : 0;
        
        // Update the specific bit
        int bitMask = 1 << axisOffset;
        int newMask;
        if (invert) {
            newMask = currentMask | bitMask;  // Set bit
        } else {
            newMask = currentMask & ~bitMask; // Clear bit
        }
        
        // Write back to controller if changed
        if (newMask != currentMask) {
            settingsSync.writeSettingToController(2, String.valueOf(newMask));
            Logger.info("Updated step pin invert mask $2 from {} to {}", currentMask, newMask);
            
            // Update our local copy
            setStepPinInvertMask(newMask);
        }
    }
    
    /**
     * Update a specific bit in the direction pin invert mask ($3)
     * Called by GrblControllerAxis when individual axis setting changes
     */
    public void updateDirPinInvertBit(int axisOffset, boolean invert) throws Exception {
        if (axisOffset < 0 || axisOffset > 5) {
            throw new Exception("Invalid axis offset: " + axisOffset);
        }
        
        if (settingsSync == null) {
            throw new Exception("Settings sync not available");
        }
        
        // Read current mask from controller
        String currentMaskStr = settingsSync.getControllerSetting(3);
        int currentMask = (currentMaskStr != null) ? Integer.parseInt(currentMaskStr) : 0;
        
        // Update the specific bit
        int bitMask = 1 << axisOffset;
        int newMask;
        if (invert) {
            newMask = currentMask | bitMask;  // Set bit
        } else {
            newMask = currentMask & ~bitMask; // Clear bit
        }
        
        // Write back to controller if changed
        if (newMask != currentMask) {
            settingsSync.writeSettingToController(3, String.valueOf(newMask));
            Logger.info("Updated dir pin invert mask $3 from {} to {}", currentMask, newMask);
            
            // Update our local copy
            setDirPinInvertMask(newMask);
        }
    }

    /**
     * Update a specific bit in the step enable invert mask ($4)
     * Called by GrblControllerAxis when individual axis setting changes
     */
    public void updateStepEnableInvertBit(int axisOffset, boolean invert) throws Exception {
        if (axisOffset < 0 || axisOffset > 5) {
            throw new Exception("Invalid axis offset: " + axisOffset);
        }
        
        if (settingsSync == null) {
            throw new Exception("Settings sync not available");
        }
        
        // Read current mask from controller
        String currentMaskStr = settingsSync.getControllerSetting(4);
        int currentMask = (currentMaskStr != null) ? Integer.parseInt(currentMaskStr) : 0;
        
        // Update the specific bit
        int bitMask = 1 << axisOffset;
        int newMask;
        if (invert) {
            newMask = currentMask | bitMask;  // Set bit
        } else {
            newMask = currentMask & ~bitMask; // Clear bit
        }
        
        // Write back to controller if changed
        if (newMask != currentMask) {
            settingsSync.writeSettingToController(4, String.valueOf(newMask));
            Logger.info("Updated step enable invert mask $4 from {} to {}", currentMask, newMask);
            
            // Update our local copy
            setStepEnableInvertMask(newMask);
        }
    }

    /**
     * Update a specific bit in the ganged motor invert mask ($8)
     * Called by GrblControllerAxis when individual axis setting changes
     */
    public void updateGangedMotorInvertBit(int axisOffset, boolean invert) throws Exception {
        if (axisOffset < 0 || axisOffset > 5) {
            throw new Exception("Invalid axis offset: " + axisOffset);
        }
        
        if (settingsSync == null) {
            throw new Exception("Settings sync not available");
        }
        
        // Read current mask from controller
        String currentMaskStr = settingsSync.getControllerSetting(8);
        int currentMask = (currentMaskStr != null) ? Integer.parseInt(currentMaskStr) : 0;
        
        // Update the specific bit
        int bitMask = 1 << axisOffset;
        int newMask;
        if (invert) {
            newMask = currentMask | bitMask;  // Set bit
        } else {
            newMask = currentMask & ~bitMask; // Clear bit
        }
        
        // Write back to controller if changed
        if (newMask != currentMask) {
            settingsSync.writeSettingToController(8, String.valueOf(newMask));
            Logger.info("Updated ganged motor invert mask $8 from {} to {}", currentMask, newMask);
            
            // Update our local copy
            setGangedMotorInvertMask(newMask);
        }
    }
}