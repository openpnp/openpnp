package org.openpnp.machine.grbl.wizards;

/**
 * Represents a discrepancy between OpenPnP configuration and grbl controller settings
 */
public class SettingDiscrepancy {
    private final int settingId;
    private final String settingName;
    private final String category;
    private final double openPnpValue;
    private final double controllerValue;
    
    public SettingDiscrepancy(int settingId, String settingName, String category, 
                             double openPnpValue, double controllerValue) {
        this.settingId = settingId;
        this.settingName = settingName;
        this.category = category;
        this.openPnpValue = openPnpValue;
        this.controllerValue = controllerValue;
    }
    
    // Getters
    public int getSettingId() { return settingId; }
    public String getSettingName() { return settingName; }
    public String getCategory() { return category; }
    public double getOpenPnpValue() { return openPnpValue; }
    public double getControllerValue() { return controllerValue; }
    
    public String getFormattedOpenPnpValue() { return String.format("%.3f", openPnpValue); }
    public String getFormattedControllerValue() { return String.format("%.3f", controllerValue); }
    public String getFormattedDifference() { return String.format("%.3f", Math.abs(openPnpValue - controllerValue)); }
    
    @Override
    public String toString() {
        return String.format("%s $%d (%s): OpenPnP=%.3f, Controller=%.3f", 
            category, settingId, settingName, openPnpValue, controllerValue);
    }
}