package org.openpnp.machine.grbl.wizards;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.machine.grbl.axis.GrblControllerAxis;
import org.openpnp.machine.grbl.driver.GrblDriver;
import org.pmw.tinylog.Logger;

public class GrblControllerAxisConfigurationWizard extends AbstractConfigurationWizard {
    
    private final GrblControllerAxis axis;
    private boolean isConnected = false;
    
    // Pin invert settings
    private JCheckBox stepPinInvertCheckbox;
    private JCheckBox dirPinInvertCheckbox;
    private JCheckBox stepEnableInvertCheckbox;
    private JCheckBox gangedMotorInvertCheckbox;
    
    // Connection tracking
    private PropertyChangeListener connectionListener;
    
    public GrblControllerAxisConfigurationWizard(GrblControllerAxis axis) {
        this.axis = axis;
        
        createComponents();
        layoutComponents();
        createBindings();
        setupConnectionTracking();
        updateComponentStates();
        
        Logger.info("Created GrblControllerAxisConfigurationWizard for axis: {}", axis.getName());
    }
    
    /**
     * Initialize all UI components
     */
    private void createComponents() {
        stepPinInvertCheckbox = new JCheckBox("Invert Step Pin");
        dirPinInvertCheckbox = new JCheckBox("Invert Direction Pin");  
        stepEnableInvertCheckbox = new JCheckBox("Invert Step Enable Pin");
        
        // Conditionally create ganged motor checkbox
        if (axis.shouldShowGangedMotorSettings()) {
            gangedMotorInvertCheckbox = new JCheckBox("Invert Ganged Motor");
            gangedMotorInvertCheckbox.setToolTipText("<html>Invert secondary motor direction for ganged axis<br>Use if secondary stepper motor is wired backwards</html>");
        }

        // Add tooltips
        stepPinInvertCheckbox.setToolTipText("<html>Invert step pin logic for this axis<br>Use if stepper driver requires inverted step signal</html>");
        dirPinInvertCheckbox.setToolTipText("<html>Invert direction pin logic for this axis<br>Use to reverse movement direction</html>");
        stepEnableInvertCheckbox.setToolTipText("<html>Invert step enable pin logic for this axis<br>Use if stepper driver requires inverted enable signal</html>");
    }
    
    /**
     * Layout components in panels
     */
    private void layoutComponents() {
        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        //gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Add pin invert settings panel
        JPanel pinInvertPanel = createPinInvertPanel();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        add(pinInvertPanel, gbc);
        
        // Add spacer at bottom
        gbc.gridy = 1;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        add(new JPanel(), gbc);
    }
    
    /**
     * Create pin invert settings panel
     */
    private JPanel createPinInvertPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("Grbl Settings - " + axis.getLetter() + " Axis"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.anchor = GridBagConstraints.WEST;
        
        int row = 0;
        
        // Step pin invert ($2)
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Step Pin ($2):"), gbc);
        gbc.gridx = 1;
        panel.add(stepPinInvertCheckbox, gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("<html><small>Controls step signal polarity</small></html>"), gbc);
        
        // Direction pin invert ($3)
        row++;
        gbc.gridy = row;
        gbc.gridx = 0;
        panel.add(new JLabel("Direction Pin ($3):"), gbc);
        gbc.gridx = 1;
        panel.add(dirPinInvertCheckbox, gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("<html><small>Controls movement direction</small></html>"), gbc);
        
        // Step enable invert ($4)
        row++;
        gbc.gridy = row;
        gbc.gridx = 0;
        panel.add(new JLabel("Step Enable Pin ($4):"), gbc);
        gbc.gridx = 1;
        panel.add(stepEnableInvertCheckbox, gbc);
        gbc.gridx = 2;
        panel.add(new JLabel("<html><small>Controls stepper enable signal</small></html>"), gbc);
        
        // Ganged motor invert ($8) - only if supported
        if (gangedMotorInvertCheckbox != null) {
            row++;
            gbc.gridy = row;
            gbc.gridx = 0;
            panel.add(new JLabel("Ganged Motor ($8):"), gbc);
            gbc.gridx = 1;
            panel.add(gangedMotorInvertCheckbox, gbc);
            gbc.gridx = 2;
            panel.add(new JLabel("<html><small>Invert secondary motor direction</small></html>"), gbc);
        }

        // Connection status info
        row++;
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        JLabel statusLabel = new JLabel("<html><i>Connect to grblHAL controller to enable pin invert settings</i></html>");
        panel.add(statusLabel, gbc);
        
        return panel;
    }
    
    /**
     * Setup connection status tracking
     */
    private void setupConnectionTracking() {
        GrblDriver grblDriver = getGrblDriver();
        if (grblDriver != null) {
            // Track initial connection state
            isConnected = grblDriver.isConnected();
            
            // If already connected, sync immediately
            if (isConnected) {
                Logger.info("Already connected - syncing pin invert settings for axis {}", axis.getName());
                axis.syncFromController();
            }
            
            // Listen for connection changes
            connectionListener = new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if ("connected".equals(evt.getPropertyName())) {
                        boolean newConnectionState = (Boolean) evt.getNewValue();
                        if (newConnectionState != isConnected) {
                            isConnected = newConnectionState;
                            updateComponentStates();
                            
                            // If just connected, sync from controller
                            if (isConnected) {
                                Logger.info("Connected to controller - syncing pin invert settings for axis {}", axis.getName());
                                // Use SwingUtilities to avoid EDT issues
                                javax.swing.SwingUtilities.invokeLater(() -> {
                                    axis.syncFromController();
                                });
                            }
                        }
                    }
                }
            };
            
            grblDriver.addPropertyChangeListener(connectionListener);
        }
    }
    
    /**
     * Update component states based on connection status
     */
    private void updateComponentStates() {
        boolean enabled = isConnected;
        
        stepPinInvertCheckbox.setEnabled(enabled);
        dirPinInvertCheckbox.setEnabled(enabled);
        stepEnableInvertCheckbox.setEnabled(enabled);
        
        // Enable ganged motor checkbox if it exists
        if (gangedMotorInvertCheckbox != null) {
            gangedMotorInvertCheckbox.setEnabled(enabled);
        }

        // Update tooltips to reflect connection state
        String connectionSuffix = enabled ? "" : " (Connect to controller to enable)";
        stepPinInvertCheckbox.setToolTipText("<html>Invert step pin logic for this axis<br>Use if stepper driver requires inverted step signal" + connectionSuffix + "</html>");
        dirPinInvertCheckbox.setToolTipText("<html>Invert direction pin logic for this axis<br>Use to reverse movement direction" + connectionSuffix + "</html>");
        stepEnableInvertCheckbox.setToolTipText("<html>Invert step enable pin logic for this axis<br>Use if stepper driver requires inverted enable signal" + connectionSuffix + "</html>");
        
        // Update ganged motor tooltip
        if (gangedMotorInvertCheckbox != null) {
            gangedMotorInvertCheckbox.setToolTipText("<html>Invert secondary motor direction for ganged axis<br>Use if secondary stepper motor is wired backwards" + connectionSuffix + "</html>");
        }

        Logger.debug("Updated pin invert component states for axis {}: enabled={}", axis.getLetter(), enabled);
    }
    
    /**
     * Get GrblDriver instance
     */
    private GrblDriver getGrblDriver() {
        if (axis.getDriver() instanceof GrblDriver) {
            return (GrblDriver) axis.getDriver();
        }
        return null;
    }
    
    /**
     * Create property bindings
     */
    @Override
    public void createBindings() {
        try {
            addWrappedBinding(axis, "stepPinInvert", stepPinInvertCheckbox, "selected");
            addWrappedBinding(axis, "dirPinInvert", dirPinInvertCheckbox, "selected");
            addWrappedBinding(axis, "stepEnableInvert", stepEnableInvertCheckbox, "selected");

            // Add ganged motor binding if checkbox exists
            if (gangedMotorInvertCheckbox != null) {
                addWrappedBinding(axis, "gangedMotorInvert", gangedMotorInvertCheckbox, "selected");
            }
            
            Logger.info("Created property bindings for GrblControllerAxis: {}", axis.getName());
            
        } catch (Exception e) {
            Logger.error("Failed to create bindings for GrblControllerAxis {}: {}", axis.getName(), e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Cleanup when wizard is disposed
     */
    @Override
    public void dispose() {
        // Remove connection listener to prevent memory leaks
        if (connectionListener != null && getGrblDriver() != null) {
            getGrblDriver().removePropertyChangeListener(connectionListener);
        }
        super.dispose();
    }
}