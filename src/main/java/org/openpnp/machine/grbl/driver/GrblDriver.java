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
}