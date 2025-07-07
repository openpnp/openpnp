package org.openpnp.machine.grbl.axis;

import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.machine.grbl.driver.GrblDriver;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.spi.Driver;
import org.pmw.tinylog.Logger;

/**
 * Controller axis with grbl/grblHAL settings synchronization.
 * Automatically syncs changes between OpenPnP configuration and controller flash memory.
 */
public class GrblControllerAxis extends ReferenceControllerAxis {
    
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
}