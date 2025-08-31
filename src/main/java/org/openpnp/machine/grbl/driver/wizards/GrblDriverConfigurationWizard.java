package org.openpnp.machine.grbl.driver.wizards;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.machine.grbl.driver.GrblDriver;
import org.openpnp.machine.grbl.wizards.GrblSettingsSync;
import org.pmw.tinylog.Logger;

public class GrblDriverConfigurationWizard extends AbstractConfigurationWizard {
    
    private final GrblDriver driver;
    private GrblSettingsSync settingsSync;
    private boolean isConnected;
    
    // === GRBL UI COMPONENTS ===

    // Step timing settings
    private JSpinner stepPulseSpinner;
    private JSpinner stepIdleDelaySpinner;
    
    // Homing settings
    private JCheckBox homingEnableCheckbox;
    private JCheckBox homingInvertXCheckbox;
    private JCheckBox homingInvertYCheckbox;
    private JCheckBox homingInvertZCheckbox;
    
    // Homing passes checkboxes (3x3 grid for grblHAL)
    private JCheckBox homingPass1XCheckbox;
    private JCheckBox homingPass1YCheckbox;
    private JCheckBox homingPass1ZCheckbox;
    private JCheckBox homingPass2XCheckbox;
    private JCheckBox homingPass2YCheckbox;
    private JCheckBox homingPass2ZCheckbox;
    private JCheckBox homingPass3XCheckbox;
    private JCheckBox homingPass3YCheckbox;
    private JCheckBox homingPass3ZCheckbox;
    
    // Other homing settings
    private JSpinner homingFeedRateSpinner;
    private JSpinner homingSeekRateSpinner;
    private JSpinner homingDebounceSpinner;
    private JSpinner homingPulloffSpinner;
    
    // Limits settings
    private JCheckBox softLimitsCheckbox;
    private JCheckBox hardLimitsCheckbox;
    private JCheckBox limitInvertXCheckbox;
    private JCheckBox limitInvertYCheckbox;
    private JCheckBox limitInvertZCheckbox;
    private JCheckBox limitInvertACheckbox;
    private JCheckBox limitInvertBCheckbox;
    private JCheckBox limitInvertCCheckbox;
    private JSpinner xMaxTravelSpinner;
    private JSpinner yMaxTravelSpinner;
    private JSpinner zMaxTravelSpinner;
    
    public GrblDriverConfigurationWizard(GrblDriver driver) {
        this.driver = driver;
        this.settingsSync = driver.getSettingsSync();
        this.isConnected = driver.isConnected();
        
        // SAMME pattern som GrblMachine:
        addGrblSettingsToPanel();
        
        // Setup connection tracking
        driver.addPropertyChangeListener("connected", evt -> {
            SwingUtilities.invokeLater(() -> {
                this.isConnected = (Boolean) evt.getNewValue();
                updateComponentStates();
                
                if (isConnected && settingsSync != null) {
                    syncControllerToDriverProperties();
                    loadFromModel();
                }
            });
        });
        
        updateComponentStates();
    }

    @Override
    public void createBindings() {
        Logger.info("Creating GrblDriverConfigurationWizard bindings...");
        
        if (homingEnableCheckbox != null) {
            try {
                addWrappedBinding(driver, "stepPulse", stepPulseSpinner, "value");
                addWrappedBinding(driver, "stepIdleDelay", stepIdleDelaySpinner, "value");

                addWrappedBinding(driver, "homingEnabled", homingEnableCheckbox, "selected");
                addWrappedBinding(driver, "homingInvertX", homingInvertXCheckbox, "selected");
                addWrappedBinding(driver, "homingInvertY", homingInvertYCheckbox, "selected");
                addWrappedBinding(driver, "homingInvertZ", homingInvertZCheckbox, "selected");
                
                addWrappedBinding(driver, "homingPass1X", homingPass1XCheckbox, "selected");
                addWrappedBinding(driver, "homingPass1Y", homingPass1YCheckbox, "selected");
                addWrappedBinding(driver, "homingPass1Z", homingPass1ZCheckbox, "selected");
                addWrappedBinding(driver, "homingPass2X", homingPass2XCheckbox, "selected");
                addWrappedBinding(driver, "homingPass2Y", homingPass2YCheckbox, "selected");
                addWrappedBinding(driver, "homingPass2Z", homingPass2ZCheckbox, "selected");
                addWrappedBinding(driver, "homingPass3X", homingPass3XCheckbox, "selected");
                addWrappedBinding(driver, "homingPass3Y", homingPass3YCheckbox, "selected");
                addWrappedBinding(driver, "homingPass3Z", homingPass3ZCheckbox, "selected");
                
                addWrappedBinding(driver, "homingFeedRate", homingFeedRateSpinner, "value");
                addWrappedBinding(driver, "homingSeekRate", homingSeekRateSpinner, "value");
                addWrappedBinding(driver, "homingDebounce", homingDebounceSpinner, "value");
                addWrappedBinding(driver, "homingPulloff", homingPulloffSpinner, "value");
                
                addWrappedBinding(driver, "softLimitsEnabled", softLimitsCheckbox, "selected");
                addWrappedBinding(driver, "hardLimitsEnabled", hardLimitsCheckbox, "selected");
                addWrappedBinding(driver, "limitInvertX", limitInvertXCheckbox, "selected");
                addWrappedBinding(driver, "limitInvertY", limitInvertYCheckbox, "selected");
                addWrappedBinding(driver, "limitInvertZ", limitInvertZCheckbox, "selected");
                addWrappedBinding(driver, "limitInvertA", limitInvertACheckbox, "selected");
                addWrappedBinding(driver, "limitInvertB", limitInvertBCheckbox, "selected");
                addWrappedBinding(driver, "limitInvertC", limitInvertCCheckbox, "selected");
                addWrappedBinding(driver, "XMaxTravel", xMaxTravelSpinner, "value");
                addWrappedBinding(driver, "YMaxTravel", yMaxTravelSpinner, "value");
                addWrappedBinding(driver, "ZMaxTravel", zMaxTravelSpinner, "value");
                
                Logger.info("All GrblDriver bindings created successfully");
                
            } catch (Exception e) {
                Logger.error("Failed to create GrblDriver bindings: {}", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void loadFromModel() {
        Logger.debug("GrblDriverConfigurationWizard.loadFromModel() called");
        
        if (settingsSync != null && isConnected) {
            syncControllerToDriverProperties();
        }
        
        super.loadFromModel();
        
        Logger.debug("loadFromModel() completed");
    }

    @Override
    public void saveToModel() {
        Logger.debug("GrblDriverConfigurationWizard.saveToModel() called");
        
        super.saveToModel();
        
        if (settingsSync != null && isConnected) {
            syncDriverPropertiesToController();
        }
        
        Logger.debug("saveToModel() completed");
    }

    /**
     * Add Grbl-specific settings panels to the main configuration panel
     * SAMME metode som GrblMachine bruker!
     */
    private void addGrblSettingsToPanel() {
        // Initialize Grbl-specific components
        initializeGrblComponents();
        
        // Create a container panel for our Grbl settings
        JPanel grblMainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Add Step Timing panel
        JPanel stepTimingPanel = createStepTimingPanel();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        grblMainPanel.add(stepTimingPanel, gbc);

        // Add Grbl Homing settings panel
        JPanel homingPanel = createGrblHomingPanel();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        grblMainPanel.add(homingPanel, gbc);
        
        // Add Grbl Limit settings panel  
        JPanel limitPanel = createGrblLimitPanel();
        gbc.gridy = 2;
        grblMainPanel.add(limitPanel, gbc);
        
        // Add fill space at bottom
        gbc.gridy = 3;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        grblMainPanel.add(new JPanel(), gbc);
        
        // Add to the main wizard panel
        //setLayout(new java.awt.BorderLayout());
        add(grblMainPanel, BorderLayout.WEST);
    }

    /**
     * Initialize all Grbl UI components
     */
    private void initializeGrblComponents() {

        // Set same preferred width for both spinners
        java.awt.Dimension spinnerSize = new java.awt.Dimension(80, 25);

        // Step timing settings
        stepPulseSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 1000, 1));
        stepPulseSpinner.setPreferredSize(spinnerSize);
        stepIdleDelaySpinner = new JSpinner(new SpinnerNumberModel(25, 0, 255, 1));
        stepIdleDelaySpinner.setPreferredSize(spinnerSize);

        homingEnableCheckbox = new JCheckBox("Enable Homing Cycle ($22)");
        homingInvertXCheckbox = new JCheckBox("Invert X-axis");
        homingInvertYCheckbox = new JCheckBox("Invert Y-axis");
        homingInvertZCheckbox = new JCheckBox("Invert Z-axis");
        
        homingPass1XCheckbox = new JCheckBox("X");
        homingPass1YCheckbox = new JCheckBox("Y");
        homingPass1ZCheckbox = new JCheckBox("Z");
        homingPass2XCheckbox = new JCheckBox("X");
        homingPass2YCheckbox = new JCheckBox("Y");
        homingPass2ZCheckbox = new JCheckBox("Z");
        homingPass3XCheckbox = new JCheckBox("X");
        homingPass3YCheckbox = new JCheckBox("Y");
        homingPass3ZCheckbox = new JCheckBox("Z");
        
        homingFeedRateSpinner = new JSpinner(new SpinnerNumberModel(25.0, 1.0, 10000.0, 1.0));
        homingSeekRateSpinner = new JSpinner(new SpinnerNumberModel(500.0, 1.0, 50000.0, 10.0));
        homingDebounceSpinner = new JSpinner(new SpinnerNumberModel(250, 1, 10000, 1));
        homingPulloffSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.1, 100.0, 0.1));
        
        softLimitsCheckbox = new JCheckBox("Enable Soft Limits ($20)");
        hardLimitsCheckbox = new JCheckBox("Enable Hard Limits ($21)");

        limitInvertXCheckbox = new JCheckBox("X");
        limitInvertYCheckbox = new JCheckBox("Y");
        limitInvertZCheckbox = new JCheckBox("Z");
        limitInvertACheckbox = new JCheckBox("A");
        limitInvertBCheckbox = new JCheckBox("B");
        limitInvertCCheckbox = new JCheckBox("C");
        
        xMaxTravelSpinner = new JSpinner(new SpinnerNumberModel(200.0, 1.0, 10000.0, 1.0));
        yMaxTravelSpinner = new JSpinner(new SpinnerNumberModel(200.0, 1.0, 10000.0, 1.0));
        zMaxTravelSpinner = new JSpinner(new SpinnerNumberModel(200.0, 1.0, 10000.0, 1.0));
    }

    /**
     * Create the step timing settings panel ($0-1)
     */
    private JPanel createStepTimingPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("Step Timing Settings"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.anchor = GridBagConstraints.WEST;
        
        int row = 0;
        
        // Step pulse time ($0)
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Step Pulse ($0):"), gbc);
        gbc.gridx = 1;
        panel.add(stepPulseSpinner, gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("μs"), gbc);
        gbc.gridx = 3;
        panel.add(new JLabel("<html><small>(pulse width for step signal)</small></html>"), gbc);
        
        // Step idle delay ($1)
        row++;
        gbc.gridy = row;
        gbc.gridx = 0;
        panel.add(new JLabel("Step Idle Delay ($1):"), gbc);
        gbc.gridx = 1;
        panel.add(stepIdleDelaySpinner, gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("ms"), gbc);
        gbc.gridx = 3;
        panel.add(new JLabel("<html><small>(delay before disabling steppers)</small></html>"), gbc);
        
        return panel;
    }

    /**
     * Create the homing settings panel
     */
    private JPanel createGrblHomingPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("Grbl Homing Settings"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.anchor = GridBagConstraints.WEST;
        
        int row = 0;
        
        // Homing enable
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 4;
        panel.add(homingEnableCheckbox, gbc);
        
        // Homing direction
        gbc.gridy = row++;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Homing Direction ($23):"), gbc);
        
        gbc.gridx = 1;
        panel.add(homingInvertXCheckbox, gbc);
        gbc.gridx = 2;
        panel.add(homingInvertYCheckbox, gbc);
        gbc.gridx = 3;
        panel.add(homingInvertZCheckbox, gbc);
        
        // Homing passes (grblHAL)
        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Homing Passes (grblHAL):"), gbc);
        
        // Pass headers
        gbc.gridx = 1;
        panel.add(new JLabel(" X"), gbc);
        gbc.gridx = 2;
        panel.add(new JLabel(" Y"), gbc);
        gbc.gridx = 3;
        panel.add(new JLabel(" Z"), gbc);
        
        // Pass X, Y, Z rows
        row++;
        gbc.gridy = row;
        gbc.gridx = 0;
        panel.add(new JLabel("Pass 1:"), gbc);
        gbc.gridx = 1;
        panel.add(homingPass1XCheckbox, gbc);
        gbc.gridx = 2;
        panel.add(homingPass1YCheckbox, gbc);
        gbc.gridx = 3;
        panel.add(homingPass1ZCheckbox, gbc);
        
        row++;
        gbc.gridy = row;
        gbc.gridx = 0;
        panel.add(new JLabel("Pass 2:"), gbc);
        gbc.gridx = 1;
        panel.add(homingPass2XCheckbox, gbc);
        gbc.gridx = 2;
        panel.add(homingPass2YCheckbox, gbc);
        gbc.gridx = 3;
        panel.add(homingPass2ZCheckbox, gbc);
        
        row++;
        gbc.gridy = row;
        gbc.gridx = 0;
        panel.add(new JLabel("Pass 3:"), gbc);
        gbc.gridx = 1;
        panel.add(homingPass3XCheckbox, gbc);
        gbc.gridx = 2;
        panel.add(homingPass3YCheckbox, gbc);
        gbc.gridx = 3;
        panel.add(homingPass3ZCheckbox, gbc);
        
        // Homing speeds and settings
        row++;
        gbc.gridy = row++;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Feed Rate ($24):"), gbc);
        gbc.gridx = 1;
        panel.add(homingFeedRateSpinner, gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("mm/min"), gbc);
        
        gbc.gridy = row++;
        gbc.gridx = 0;
        panel.add(new JLabel("Seek Rate ($25):"), gbc);
        gbc.gridx = 1;
        panel.add(homingSeekRateSpinner, gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("mm/min"), gbc);
        
        gbc.gridy = row++;
        gbc.gridx = 0;
        panel.add(new JLabel("Debounce ($26):"), gbc);
        gbc.gridx = 1;
        panel.add(homingDebounceSpinner, gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("ms"), gbc);
        
        gbc.gridy = row++;
        gbc.gridx = 0;
        panel.add(new JLabel("Pull-off ($27):"), gbc);
        gbc.gridx = 1;
        panel.add(homingPulloffSpinner, gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("mm"), gbc);
        
        return panel;
    }

    /**
     * Create the limits settings panel
     */
    private JPanel createGrblLimitPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("Grbl Limits Settings"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.anchor = GridBagConstraints.WEST;
        
        int row = 0;
        
        // Limits checkboxes
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 3;
        panel.add(softLimitsCheckbox, gbc);
        
        gbc.gridy = row++;
        panel.add(hardLimitsCheckbox, gbc);
        
        // ADD LIMIT PIN INVERT SECTION HERE:
        gbc.gridy = row++;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Limit Pin Invert ($5):"), gbc);
        
        // Create a sub-panel for the checkboxes
        JPanel invertPanel = new JPanel(new GridBagLayout());
        GridBagConstraints invertGbc = new GridBagConstraints();
        invertGbc.insets = new Insets(1, 2, 1, 2);
        
        invertGbc.gridx = 0;
        invertGbc.gridy = 0;
        invertPanel.add(limitInvertXCheckbox, invertGbc);
        invertGbc.gridx = 1;
        invertPanel.add(limitInvertYCheckbox, invertGbc);
        invertGbc.gridx = 2;
        invertPanel.add(limitInvertZCheckbox, invertGbc);
        invertGbc.gridx = 3;
        invertPanel.add(limitInvertACheckbox, invertGbc);
        invertGbc.gridx = 4;
        invertPanel.add(limitInvertBCheckbox, invertGbc);
        invertGbc.gridx = 5;
        invertPanel.add(limitInvertCCheckbox, invertGbc);
        
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        panel.add(invertPanel, gbc);
        
        // Small explanation
        gbc.gridy = row++;
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        panel.add(new JLabel("<html><small>Check to invert logic (use with NO switches)</small></html>"), gbc);
        
        // Max travel settings
        gbc.gridwidth = 1;
        gbc.gridy = row++;
        gbc.gridx = 0;
        panel.add(new JLabel("X Max Travel ($130):"), gbc);
        gbc.gridx = 1;
        panel.add(xMaxTravelSpinner, gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("mm"), gbc);
        
        gbc.gridy = row++;
        gbc.gridx = 0;
        panel.add(new JLabel("Y Max Travel ($131):"), gbc);
        gbc.gridx = 1;
        panel.add(yMaxTravelSpinner, gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("mm"), gbc);
        
        gbc.gridy = row++;
        gbc.gridx = 0;
        panel.add(new JLabel("Z Max Travel ($132):"), gbc);
        gbc.gridx = 1;
        panel.add(zMaxTravelSpinner, gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("mm"), gbc);
        
        return panel;
    }

    /**
     * Update component states based on connection status
     */
    private void updateComponentStates() {
        boolean enabled = isConnected && settingsSync != null;
        
        if (homingEnableCheckbox != null) {
            stepPulseSpinner.setEnabled(enabled);
            stepIdleDelaySpinner.setEnabled(enabled);

            homingEnableCheckbox.setEnabled(enabled);
            homingInvertXCheckbox.setEnabled(enabled);
            homingInvertYCheckbox.setEnabled(enabled);
            homingInvertZCheckbox.setEnabled(enabled);
            
            homingPass1XCheckbox.setEnabled(enabled);
            homingPass1YCheckbox.setEnabled(enabled);
            homingPass1ZCheckbox.setEnabled(enabled);
            homingPass2XCheckbox.setEnabled(enabled);
            homingPass2YCheckbox.setEnabled(enabled);
            homingPass2ZCheckbox.setEnabled(enabled);
            homingPass3XCheckbox.setEnabled(enabled);
            homingPass3YCheckbox.setEnabled(enabled);
            homingPass3ZCheckbox.setEnabled(enabled);
            
            homingFeedRateSpinner.setEnabled(enabled);
            homingSeekRateSpinner.setEnabled(enabled);
            homingDebounceSpinner.setEnabled(enabled);
            homingPulloffSpinner.setEnabled(enabled);
            
            softLimitsCheckbox.setEnabled(enabled);
            hardLimitsCheckbox.setEnabled(enabled);
            limitInvertXCheckbox.setEnabled(enabled);
            limitInvertYCheckbox.setEnabled(enabled);
            limitInvertZCheckbox.setEnabled(enabled);
            limitInvertACheckbox.setEnabled(enabled);
            limitInvertBCheckbox.setEnabled(enabled);
            limitInvertCCheckbox.setEnabled(enabled);
            xMaxTravelSpinner.setEnabled(enabled);
            yMaxTravelSpinner.setEnabled(enabled);
            zMaxTravelSpinner.setEnabled(enabled);
        }
    }

    /**
     * Sync settings from controller to driver properties
     */
    private void syncControllerToDriverProperties() {
        try {
            Logger.info("Loading controller settings and syncing to driver properties...");
            
            // Read step timing settings
            String stepPulseStr = settingsSync.getControllerSetting(0);
            String stepIdleDelayStr = settingsSync.getControllerSetting(1);
        

            // Read current values from controller
            String limitPinInvertStr = settingsSync.getControllerSetting(5);
            String homingEnabledStr = settingsSync.getControllerSetting(22);
            String homingDirectionStr = settingsSync.getControllerSetting(23);
            String homingFeedRateStr = settingsSync.getControllerSetting(24);
            String homingSeekRateStr = settingsSync.getControllerSetting(25);
            String homingDebounceStr = settingsSync.getControllerSetting(26);
            String homingPulloffStr = settingsSync.getControllerSetting(27);
            String softLimitsStr = settingsSync.getControllerSetting(20);
            String hardLimitsStr = settingsSync.getControllerSetting(21);
            String xMaxTravelStr = settingsSync.getControllerSetting(130);
            String yMaxTravelStr = settingsSync.getControllerSetting(131);
            String zMaxTravelStr = settingsSync.getControllerSetting(132);
            
            // grblHAL homing passes
            String homingPass1Str = settingsSync.getControllerSetting(44);
            String homingPass2Str = settingsSync.getControllerSetting(45);
            String homingPass3Str = settingsSync.getControllerSetting(46);
            
            // Update step timing properties
            if (stepPulseStr != null) {
                try {
                    int stepPulse = Integer.parseInt(stepPulseStr);
                    driver.setStepPulse(stepPulse);
                } catch (NumberFormatException e) {
                    Logger.warn("Invalid step pulse value: {}", stepPulseStr);
                }
            }
            
            if (stepIdleDelayStr != null) {
                try {
                    int stepIdleDelay = Integer.parseInt(stepIdleDelayStr);
                    driver.setStepIdleDelay(stepIdleDelay);
                } catch (NumberFormatException e) {
                    Logger.warn("Invalid step idle delay value: {}", stepIdleDelayStr);
                }
            }

            // Update driver properties
            if (homingEnabledStr != null) {
                driver.setHomingEnabled("1".equals(homingEnabledStr));
            }
            
            if (homingDirectionStr != null) {
                try {
                    int direction = Integer.parseInt(homingDirectionStr);
                    driver.setHomingDirectionMask(direction);
                } catch (NumberFormatException e) {
                    Logger.warn("Invalid homing direction value: {}", homingDirectionStr);
                }
            }
            
            if (homingFeedRateStr != null) {
                try {
                    double feedRate = Double.parseDouble(homingFeedRateStr);
                    driver.setHomingFeedRate(feedRate);
                } catch (NumberFormatException e) {
                    Logger.warn("Invalid homing feed rate value: {}", homingFeedRateStr);
                }
            }
            
            if (homingSeekRateStr != null) {
                try {
                    double seekRate = Double.parseDouble(homingSeekRateStr);
                    driver.setHomingSeekRate(seekRate);
                } catch (NumberFormatException e) {
                    Logger.warn("Invalid homing seek rate value: {}", homingSeekRateStr);
                }
            }
            
            if (homingDebounceStr != null) {
                try {
                    int debounce = Integer.parseInt(homingDebounceStr);
                    driver.setHomingDebounce(debounce);
                } catch (NumberFormatException e) {
                    Logger.warn("Invalid homing debounce value: {}", homingDebounceStr);
                }
            }
            
            if (homingPulloffStr != null) {
                try {
                    double pulloff = Double.parseDouble(homingPulloffStr);
                    driver.setHomingPulloff(pulloff);
                } catch (NumberFormatException e) {
                    Logger.warn("Invalid homing pulloff value: {}", homingPulloffStr);
                }
            }
            
            // Homing passes (grblHAL)
            if (homingPass1Str != null) {
                try {
                    int pass1 = Integer.parseInt(homingPass1Str);
                    driver.setHomingPass1(pass1);
                } catch (NumberFormatException e) {
                    Logger.warn("Invalid homing pass 1 value: {}", homingPass1Str);
                }
            }
            
            if (homingPass2Str != null) {
                try {
                    int pass2 = Integer.parseInt(homingPass2Str);
                    driver.setHomingPass2(pass2);
                } catch (NumberFormatException e) {
                    Logger.warn("Invalid homing pass 2 value: {}", homingPass2Str);
                }
            }
            
            if (homingPass3Str != null) {
                try {
                    int pass3 = Integer.parseInt(homingPass3Str);
                    driver.setHomingPass3(pass3);
                } catch (NumberFormatException e) {
                    Logger.warn("Invalid homing pass 3 value: {}", homingPass3Str);
                }
            }
            
            // Limits
            if (softLimitsStr != null) {
                driver.setSoftLimitsEnabled("1".equals(softLimitsStr));
            }
            
            if (hardLimitsStr != null) {
                driver.setHardLimitsEnabled("1".equals(hardLimitsStr));
            }
            
            if (limitPinInvertStr != null) {
                try {
                    int limitInvertMask = Integer.parseInt(limitPinInvertStr);
                    driver.setLimitPinInvertMask(limitInvertMask);
                } catch (NumberFormatException e) {
                    Logger.warn("Invalid limit pin invert value: {}", limitPinInvertStr);
                }
            }

            // Max travel
            if (xMaxTravelStr != null) {
                try {
                    double xMaxTravel = Double.parseDouble(xMaxTravelStr);
                    driver.setXMaxTravel(xMaxTravel);
                } catch (NumberFormatException e) {
                    Logger.warn("Invalid X max travel value: {}", xMaxTravelStr);
                }
            }
            
            if (yMaxTravelStr != null) {
                try {
                    double yMaxTravel = Double.parseDouble(yMaxTravelStr);
                    driver.setYMaxTravel(yMaxTravel);
                } catch (NumberFormatException e) {
                    Logger.warn("Invalid Y max travel value: {}", yMaxTravelStr);
                }
            }
            
            if (zMaxTravelStr != null) {
                try {
                    double zMaxTravel = Double.parseDouble(zMaxTravelStr);
                    driver.setZMaxTravel(zMaxTravel);
                } catch (NumberFormatException e) {
                    Logger.warn("Invalid Z max travel value: {}", zMaxTravelStr);
                }
            }
            
            Logger.info("Controller settings loaded and synced to driver properties");
            
        } catch (Exception e) {
            Logger.error("Failed to sync controller settings to driver properties: {}", e.getMessage());
        }
    }

    /**
     * Sync settings from driver properties to controller
     */
    private void syncDriverPropertiesToController() {
        try {
            Logger.info("Syncing driver properties to controller...");
            
            int settingsWritten = 0;
            
            // Sync step timing settings FIRST
            settingsWritten += writeSettingIfChanged(0, String.valueOf(driver.getStepPulse()), "stepPulse");
            settingsWritten += writeSettingIfChanged(1, String.valueOf(driver.getStepIdleDelay()), "stepIdleDelay");

            // Sync all settings
            settingsWritten += writeSettingIfChanged(22, driver.isHomingEnabled() ? "1" : "0", "homingEnabled");
            settingsWritten += writeSettingIfChanged(23, String.valueOf(driver.getHomingDirectionMask()), "homingDirection");
            settingsWritten += writeSettingIfChanged(24, String.valueOf(driver.getHomingFeedRate()), "homingFeedRate");
            settingsWritten += writeSettingIfChanged(25, String.valueOf(driver.getHomingSeekRate()), "homingSeekRate");
            settingsWritten += writeSettingIfChanged(26, String.valueOf(driver.getHomingDebounce()), "homingDebounce");
            settingsWritten += writeSettingIfChanged(27, String.valueOf(driver.getHomingPulloff()), "homingPulloff");
            
            // grblHAL homing passes
            settingsWritten += writeSettingIfChanged(44, String.valueOf(driver.getHomingPass1()), "homingPass1");
            settingsWritten += writeSettingIfChanged(45, String.valueOf(driver.getHomingPass2()), "homingPass2");
            settingsWritten += writeSettingIfChanged(46, String.valueOf(driver.getHomingPass3()), "homingPass3");
            
            // Limits
            settingsWritten += writeSettingIfChanged(20, driver.isSoftLimitsEnabled() ? "1" : "0", "softLimits");
            settingsWritten += writeSettingIfChanged(21, driver.isHardLimitsEnabled() ? "1" : "0", "hardLimits");
            settingsWritten += writeSettingIfChanged(5, String.valueOf(driver.getLimitPinInvertMask()), "limitPinInvert");

            // Max travel
            settingsWritten += writeSettingIfChanged(130, String.valueOf(driver.getXMaxTravel()), "xMaxTravel");
            settingsWritten += writeSettingIfChanged(131, String.valueOf(driver.getYMaxTravel()), "yMaxTravel");
            settingsWritten += writeSettingIfChanged(132, String.valueOf(driver.getZMaxTravel()), "zMaxTravel");
            
            if (settingsWritten > 0) {
                Logger.info("Wrote {} settings to controller", settingsWritten);
            } else {
                Logger.info("No settings changed - nothing written to controller");
            }
            
        } catch (Exception e) {
            Logger.error("Failed to sync driver properties to controller: {}", e.getMessage());
        }
    }

    /**
     * Write setting to controller only if it has changed
     */
    private int writeSettingIfChanged(int settingId, String newValue, String settingName) {
        try {
            String currentValue = settingsSync.getControllerSetting(settingId);
            
            if (!valuesEqual(currentValue, newValue)) {
                settingsSync.writeSettingToController(settingId, newValue);
                Logger.debug("Updated {}: {} -> {}", settingName, currentValue, newValue);
                return 1;
            }
            
            return 0;
            
        } catch (Exception e) {
            Logger.error("Failed to write setting ${}: {}", settingId, e.getMessage());
            return 0;
        }
    }
    
    /**
     * Compare two setting values with tolerance for floating point numbers
     */
    private boolean valuesEqual(String value1, String value2) {
        if (value1 == null || value2 == null) {
            return value1 == value2;
        }
        
        try {
            double num1 = Double.parseDouble(value1);
            double num2 = Double.parseDouble(value2);
            return Math.abs(num1 - num2) < 0.001; // Small tolerance for floating point
        } catch (NumberFormatException e) {
            // Not numbers, use string comparison
            return value1.equals(value2);
        }
    }
}