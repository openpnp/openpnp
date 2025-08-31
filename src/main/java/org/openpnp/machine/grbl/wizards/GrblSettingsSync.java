package org.openpnp.machine.grbl.wizards;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.machine.reference.driver.GcodeDriver;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Driver;
import org.openpnp.spi.Machine;
import org.pmw.tinylog.Logger;

/**
 * Handles reading, writing and synchronization of grbl/grblHAL settings
 * between controller flash memory and OpenPnP configuration.
 */
public class GrblSettingsSync {
    private final GcodeDriver driver;
    private Map<Integer, String> controllerSettings;
    private boolean gangedMotorSupported = false;  // Cache $8 support status
    
    // Pattern for parsing grbl settings: $100=180.000
    private static final Pattern SETTING_PATTERN = Pattern.compile("\\$(\\d+)=([\\d\\.\\-]+)");
    
    public GrblSettingsSync(GcodeDriver driver) {
        this.driver = driver;
        this.controllerSettings = new HashMap<>();
    }
    
    /**
     * Read all settings from grbl controller using $$ command
     */
    public void readSettingsFromController() throws Exception {
        Logger.info("Reading settings from grbl controller...");
        
        try {
            // Send $$ command to get all settings
            driver.sendCommand("$$");
            
            // Wait a bit for controller to send all settings
            Thread.sleep(1000);
            
            // Collect all available responses
            List<GcodeDriver.Line> responses = driver.receiveResponses();
            
            // Build response string from all lines
            StringBuilder responseBuilder = new StringBuilder();
            for (GcodeDriver.Line line : responses) {
                String lineText = line.getLine();
                // Only include lines that look like settings ($100=180.000)
                if (lineText.matches(".*\\$\\d+=.*")) {
                    responseBuilder.append(lineText).append("\n");
                }
            }
            
            String response = responseBuilder.toString();
            if (response.trim().isEmpty()) {
                throw new Exception("No settings received from controller");
            }
            
            controllerSettings = parseSettingsResponse(response);
            
            // Check for ganged motor support ($8)
            checkGangedMotorSupport();
            
            Logger.info("Successfully read {} settings from controller", controllerSettings.size());
            
        } catch (Exception e) {
            Logger.error("Failed to read settings from controller: {}", e.getMessage());
            throw new Exception("Failed to read grbl settings: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parse grbl settings response into Map
     * Expected format: $100=180.000\n$101=180.000\n...
     */
    private Map<Integer, String> parseSettingsResponse(String response) throws Exception {
        Map<Integer, String> settings = new HashMap<>();
        
        if (response == null || response.trim().isEmpty()) {
            throw new Exception("Empty response from controller");
        }
        
        String[] lines = response.split("\n");
        int parsedCount = 0;
        
        for (String line : lines) {
            line = line.trim();
            
            if (line.isEmpty()) {
                continue;
            }
            
            Matcher matcher = SETTING_PATTERN.matcher(line);
            if (matcher.matches()) {
                try {
                    int settingId = Integer.parseInt(matcher.group(1));
                    String value = matcher.group(2);
                    
                    settings.put(settingId, value);
                    parsedCount++;
                    
                    Logger.debug("Parsed setting: ${}={}", settingId, value);
                    
                } catch (NumberFormatException e) {
                    Logger.warn("Failed to parse setting ID from line: {}", line);
                }
            } else {
                Logger.debug("Skipping non-setting line: {}", line);
            }
        }
        
        if (parsedCount == 0) {
            throw new Exception("No valid settings found in controller response");
        }
        
        Logger.info("Parsed {} settings from controller response", parsedCount);
        return settings;
    }
    
    /**
     * Check if controller supports ganged motors ($8 setting)
     * Called after reading all settings from controller
     */
    private void checkGangedMotorSupport() {
        gangedMotorSupported = controllerSettings.containsKey(8);
        
        if (gangedMotorSupported) {
            String value = controllerSettings.get(8);
            Logger.info("Ganged motor support detected: $8={}", value);
        } else {
            Logger.info("No ganged motor support detected (no $8 setting)");
        }
    }

    /**
     * Check if controller supports ganged motors
     * @return true if $8 setting was found during last sync
     */
    public boolean isGangedMotorSupported() {
        return gangedMotorSupported;
    }

    /**
     * Get a setting value from cached controller settings
     */
    public String getControllerSetting(int settingId) {
        return controllerSettings.get(settingId);
    }
    
    /**
     * Get all cached controller settings
     */
    public Map<Integer, String> getAllControllerSettings() {
        return new HashMap<>(controllerSettings); // Return copy to prevent modification
    }

    /**
     * Write a single setting to grbl controller
     * Format: $100=180.000
     */
    public void writeSettingToController(int settingId, String value) throws Exception {
        Logger.info("Writing setting ${}={} to controller", settingId, value);
        
        try {
            // Format command: $100=180.000
            String command = String.format("$%d=%s", settingId, value);
            
            // Send command to controller
            driver.sendCommand(command);
            
            // Wait a bit for controller to process
            Thread.sleep(100);
            
            // Update our cached value if write was successful
            controllerSettings.put(settingId, value);
            
            Logger.info("Successfully wrote setting ${}={}", settingId, value);
            
        } catch (Exception e) {
            Logger.error("Failed to write setting ${}={}: {}", settingId, value, e.getMessage());
            throw new Exception("Failed to write grbl setting: " + e.getMessage(), e);
        }
    }

    /**
     * Write multiple settings to controller in batch
     */
    public void writeSettingsToController(Map<Integer, String> settings) throws Exception {
        Logger.info("Writing {} settings to controller", settings.size());
        
        for (Map.Entry<Integer, String> entry : settings.entrySet()) {
            writeSettingToController(entry.getKey(), entry.getValue());
        }
        
        Logger.info("Successfully wrote all {} settings to controller", settings.size());
    }

    /**
     * Compare OpenPnP config with controller settings and return differences
     */
    public List<SettingDiscrepancy> checkForDiscrepancies() {
        List<SettingDiscrepancy> discrepancies = new ArrayList<>();
        
        try {
            // Get machine and check axis settings
            Machine machine = Configuration.get().getMachine();
            checkAxisDiscrepancies(machine, discrepancies);
            
            // TODO: Add other setting types later:
            // checkHomingDiscrepancies(machine, discrepancies);
            // checkLimitDiscrepancies(machine, discrepancies);
            
        } catch (Exception e) {
            Logger.warn("Failed to check for discrepancies: {}", e.getMessage());
        }
        
        Logger.info("Found {} setting discrepancies", discrepancies.size());
        return discrepancies;
    }

    /**
     * Check axis settings for discrepancies
     */
    private void checkAxisDiscrepancies(Machine machine, List<SettingDiscrepancy> discrepancies) {
        try {
            Logger.debug("Checking axis discrepancies...");
            
            // Find our driver in the machine's driver list
            boolean foundOurDriver = false;
            for (Driver machineDriver : machine.getDrivers()) {
                if (machineDriver == driver) {
                    foundOurDriver = true;
                    Logger.debug("Found our driver: {}", machineDriver.getName());
                    break;
                }
            }
            
            if (!foundOurDriver) {
                Logger.debug("Our driver not found in machine drivers - skipping axis check");
                return;
            }
            
            // Get all axes from machine - they all belong to this machine/driver
            for (Axis axis : machine.getAxes()) {
                Logger.debug("Found axis: {}", axis.getName());
                
                if (axis instanceof ReferenceControllerAxis) {
                    Logger.debug("Checking axis: {}", axis.getName());
                    ReferenceControllerAxis refAxis = (ReferenceControllerAxis) axis;
                    checkSingleAxisSettings(refAxis, discrepancies);
                } else {
                    Logger.debug("Skipping axis {} (not ReferenceControllerAxis)", axis.getName());
                }
            }
            
        } catch (Exception e) {
            Logger.warn("Failed to check axis discrepancies: {}", e.getMessage());
        }
    }

    /**
     * Check settings for a single axis
     */
    private void checkSingleAxisSettings(ReferenceControllerAxis axis, List<SettingDiscrepancy> discrepancies) {
        String axisName = axis.getName();
        String category = axisName + "-Axis";
        
        Logger.debug("Checking axis: {}", axisName);
        
        try {
            // Check steps/mm setting
            int stepsSettingId = getAxisSettingId(axis.getLetter(), "steps");
            Logger.debug("Steps setting ID for {}: {}", axisName, stepsSettingId);
            
            if (stepsSettingId != -1) {
                String controllerStepsStr = getControllerSetting(stepsSettingId);
                Logger.debug("Controller steps string for ${}: {}", stepsSettingId, controllerStepsStr);
                
                if (controllerStepsStr != null) {
                    double controllerSteps = Double.parseDouble(controllerStepsStr);
                    double openPnpSteps = 1.0 / axis.getResolution();
                    
                    Logger.debug("Controller steps: {}, OpenPnP steps: {}, Resolution: {}", 
                        controllerSteps, openPnpSteps, axis.getResolution());
                    
                    // Apply smart rounding for common stepper values
                    if (Math.abs(openPnpSteps - Math.round(openPnpSteps)) < 0.1) {
                        openPnpSteps = Math.round(openPnpSteps);
                    }
                    
                    Logger.debug("After rounding - OpenPnP steps: {}, Difference: {}", 
                        openPnpSteps, Math.abs(controllerSteps - openPnpSteps));
                    
                    if (Math.abs(controllerSteps - openPnpSteps) > 0.01) {
                        Logger.debug("DISCREPANCY FOUND for {}: {} vs {}", axisName, openPnpSteps, controllerSteps);
                        discrepancies.add(new SettingDiscrepancy(
                            stepsSettingId, "Steps/mm", category, openPnpSteps, controllerSteps));
                    } else {
                        Logger.debug("No discrepancy for {} steps", axisName);
                    }
                }
            }
            
            // ... resten av metoden
        } catch (Exception e) {
            Logger.warn("Failed to check settings for axis {}: {}", axis.getName(), e.getMessage());
        }
    }

    /**
     * Get grbl setting ID for axis and setting type
     */
    private int getAxisSettingId(String axisName, String settingType) {
        // grbl setting IDs based on axis and type
        int axisOffset = getAxisOffset(axisName);
        if (axisOffset == -1){ return -1;}
        
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
    private int getAxisOffset(String axisName) {
        switch (axisName.toUpperCase()) {
            case "X": return 0;
            case "Y": return 1;
            case "Z": return 2;
            case "A": return 3;
            case "B": return 4;
            case "C": return 5;
            default: return -1;
        }
    }
}