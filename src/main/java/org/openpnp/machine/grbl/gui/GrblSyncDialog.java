package org.openpnp.machine.grbl.gui;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.openpnp.machine.grbl.axis.GrblControllerAxis;
import org.openpnp.machine.grbl.wizards.GrblSettingsSync;
import org.openpnp.machine.grbl.wizards.SettingDiscrepancy;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Machine;
import org.pmw.tinylog.Logger;

/**
 * Dialog for handling grbl settings sync discrepancies
 */
public class GrblSyncDialog extends JDialog {
    private final List<SettingDiscrepancy> discrepancies;
    private final GrblSettingsSync settingsSync;
    private JTable discrepancyTable;
    private DiscrepancyTableModel tableModel;
    
    public GrblSyncDialog(Frame parent, List<SettingDiscrepancy> discrepancies, GrblSettingsSync settingsSync) {
        super(parent, "Grbl Settings Sync", true);
        this.discrepancies = discrepancies;
        this.settingsSync = settingsSync;
        
        initializeComponents();
        setupLayout();
        pack();
        setLocationRelativeTo(parent);
    }
    
    /**
     * Initialize the components of the dialog
     */
    private void initializeComponents() {
        tableModel = new DiscrepancyTableModel(discrepancies);
        discrepancyTable = new JTable(tableModel);
        discrepancyTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        discrepancyTable.setRowSelectionAllowed(true);
        
        // Custom renderer for highlighting differences
        discrepancyTable.setDefaultRenderer(Object.class, new DiscrepancyRenderer());
        
        // Set column widths
        discrepancyTable.getColumnModel().getColumn(0).setPreferredWidth(80);  // Category
        discrepancyTable.getColumnModel().getColumn(1).setPreferredWidth(80);  // Setting ID
        discrepancyTable.getColumnModel().getColumn(2).setPreferredWidth(120); // Setting Name
        discrepancyTable.getColumnModel().getColumn(3).setPreferredWidth(100); // OpenPnP Value
        discrepancyTable.getColumnModel().getColumn(4).setPreferredWidth(100); // Controller Value
        discrepancyTable.getColumnModel().getColumn(5).setPreferredWidth(80);  // Difference
    }
    
    /**
     * Setup the layout of the dialog
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        
        JLabel titleLabel = new JLabel("Grbl Settings Sync Discrepancies");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        titlePanel.add(titleLabel, BorderLayout.NORTH);
        
        JLabel subtitleLabel = new JLabel(String.format(
            "Found %d differences between OpenPnP configuration and grbl controller flash memory:",
            discrepancies.size()));
        titlePanel.add(subtitleLabel, BorderLayout.SOUTH);
        
        add(titlePanel, BorderLayout.NORTH);
        
        // Table panel
        JScrollPane scrollPane = new JScrollPane(discrepancyTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        scrollPane.setPreferredSize(new Dimension(700, 250));
        add(scrollPane, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        
        JButton syncToControllerBtn = new JButton("Sync Selected to Controller");
        syncToControllerBtn.addActionListener(e -> syncSelectedToController());
        
        JButton syncToOpenPnPBtn = new JButton("Sync Selected to OpenPnP");
        syncToOpenPnPBtn.addActionListener(e -> syncSelectedToOpenPnP());
        
        JButton syncAllToControllerBtn = new JButton("Sync All to Controller");
        syncAllToControllerBtn.addActionListener(e -> syncAllToController());
        
        JButton syncAllToOpenPnPBtn = new JButton("Sync All to OpenPnP");
        syncAllToOpenPnPBtn.addActionListener(e -> syncAllToOpenPnP());
        
        JButton ignoreBtn = new JButton("Ignore");
        ignoreBtn.addActionListener(e -> dispose());
        
        buttonPanel.add(syncToControllerBtn);
        buttonPanel.add(syncToOpenPnPBtn);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(syncAllToControllerBtn);
        buttonPanel.add(syncAllToOpenPnPBtn);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(ignoreBtn);
        
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Sync selected discrepancies to controller or OpenPnP
     */
    private void syncSelectedToController() {
        int[] selectedRows = discrepancyTable.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, "Please select discrepancies to sync.");
            return;
        }
        
        syncDiscrepanciesToController(getSelectedDiscrepancies(selectedRows));
    }
    
    /**
     * Sync selected discrepancies to OpenPnP
     */
    private void syncSelectedToOpenPnP() {
        int[] selectedRows = discrepancyTable.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, "Please select discrepancies to sync.");
            return;
        }
        
        syncDiscrepanciesToOpenPnP(getSelectedDiscrepancies(selectedRows));
    }
    
    /**
     * Sync all discrepancies to controller or OpenPnP
     */
    private void syncAllToController() {
        syncDiscrepanciesToController(discrepancies);
    }
    
    /**
     * Sync all discrepancies to OpenPnP
     */
    private void syncAllToOpenPnP() {
        syncDiscrepanciesToOpenPnP(discrepancies);
    }
    
    /**
     * Get the selected discrepancies based on the table selection
     */
    private List<SettingDiscrepancy> getSelectedDiscrepancies(int[] selectedRows) {
        return Arrays.stream(selectedRows)
            .mapToObj(row -> discrepancies.get(row))
            .collect(Collectors.toList());
    }
    
    /**
     * Sync discrepancies to controller
     */
    private void syncDiscrepanciesToController(List<SettingDiscrepancy> toSync) {
        try {
            int count = 0;
            
            for (SettingDiscrepancy discrepancy : toSync) {
                // Sync OpenPnP value to controller
                settingsSync.writeSettingToController(discrepancy.getSettingId(), 
                                                    discrepancy.getFormattedOpenPnpValue());
                count++;
                Logger.info("Synced ${} {} to controller: {} -> {}", 
                    discrepancy.getSettingId(), discrepancy.getSettingName(),
                    discrepancy.getFormattedControllerValue(), discrepancy.getFormattedOpenPnpValue());
            }
            
            JOptionPane.showMessageDialog(this, 
                String.format("Successfully synced %d settings to controller.", count),
                "Sync Complete", JOptionPane.INFORMATION_MESSAGE);
            
            dispose();
            
        } catch (Exception e) {
            Logger.error("Failed to sync to controller: {}", e.getMessage());
            JOptionPane.showMessageDialog(this, 
                "Failed to sync to controller: " + e.getMessage(),
                "Sync Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Sync discrepancies to OpenPnP
     */
    private void syncDiscrepanciesToOpenPnP(List<SettingDiscrepancy> toSync) {
        Logger.debug("Starting sync to OpenPnP for {} discrepancies", toSync.size());
        
        try {
            int count = 0;
            
            for (SettingDiscrepancy discrepancy : toSync) {
                Logger.debug("Processing discrepancy: Setting ${} ({})", 
                    discrepancy.getSettingId(), discrepancy.getSettingName());
                
                // Find the axis for this setting (only returns GrblControllerAxis or null)
                GrblControllerAxis grblAxis = (GrblControllerAxis) findAxisForSetting(discrepancy.getSettingId());
                
                if (grblAxis != null) {
                    Logger.debug("Found GrblControllerAxis: {} - calling syncFromController()", grblAxis.getName());
                    
                    // Sync from controller to OpenPnP
                    grblAxis.syncFromController();
                    count++;
                    
                    Logger.info("Synced {} {} from controller: {} -> {}", 
                        discrepancy.getCategory(), discrepancy.getSettingName(),
                        discrepancy.getFormattedOpenPnpValue(), discrepancy.getFormattedControllerValue());
                } else {
                    Logger.debug("No GrblControllerAxis found for setting ${} - skipping sync", discrepancy.getSettingId());
                }
            }
            
            Logger.debug("Sync to OpenPnP completed - synced {} out of {} discrepancies", count, toSync.size());
            
            JOptionPane.showMessageDialog(this, 
                String.format("Successfully synced %d settings to OpenPnP.\nRestart may be required for changes to take effect.", count),
                "Sync Complete", JOptionPane.INFORMATION_MESSAGE);
            
            dispose();
            
        } catch (Exception e) {
            Logger.error("Failed to sync to OpenPnP: {}", e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Failed to sync to OpenPnP: " + e.getMessage(),
                "Sync Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Find the axis that corresponds to a grbl setting ID
     */
    private Axis findAxisForSetting(int settingId) {
        Logger.debug("Finding axis for setting ID: ${}", settingId);
        
        try {
            // Get machine and find axis based on setting ID
            Configuration configuration = Configuration.get();
            Machine machine = configuration.getMachine();
            
            Logger.debug("Machine has {} axes", machine.getAxes().size());
            
            // Map setting ID to axis letter (what controller uses)
            String axisLetter = getAxisNameForSettingId(settingId);
            Logger.debug("Setting ${} maps to axis letter: {}", settingId, axisLetter);
            
            if (axisLetter != null) {
                for (Axis axis : machine.getAxes()) {
                    // Only check GrblControllerAxis first
                    if (axis instanceof GrblControllerAxis) {
                        // Now we can safely call getLetter() on GrblControllerAxis
                        String currentAxisLetter = ((GrblControllerAxis) axis).getLetter();
                        Logger.debug("Checking GrblControllerAxis: {} (letter: {})", axis.getName(), currentAxisLetter);
                        
                        if (currentAxisLetter.equalsIgnoreCase(axisLetter)) {
                            Logger.debug("Found matching GrblControllerAxis: {} for letter {}", axis.getName(), axisLetter);
                            return axis;
                        }
                    } else {
                        Logger.debug("Skipping non-GrblControllerAxis: {} (type: {})", 
                            axis.getName(), axis.getClass().getSimpleName());
                    }
                }
                Logger.debug("No GrblControllerAxis found with letter: {}", axisLetter);
            }
            
        } catch (Exception e) {
            Logger.warn("Failed to find axis for setting ID {}: {}", settingId, e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }

    /**
     * Get axis name from grbl setting ID
     */
    private String getAxisNameForSettingId(int settingId) {
        // Steps, feedrate, and acceleration settings
        if (settingId >= 100 && settingId <= 105) { // Steps
            return getAxisName(settingId - 100);
        } else if (settingId >= 110 && settingId <= 115) { // Feedrate
            return getAxisName(settingId - 110);
        } else if (settingId >= 120 && settingId <= 125) { // Acceleration
            return getAxisName(settingId - 120);
        }
        
        return null;
    }

    /**
     * Get axis name from offset
     */
    private String getAxisName(int offset) {
        switch (offset) {
            case 0: return "X";
            case 1: return "Y";
            case 2: return "Z";
            case 3: return "A";
            case 4: return "B";
            case 5: return "C";
            default: return null;
        }
    }

    /**
     * Table model for displaying discrepancies
     */
    private static class DiscrepancyTableModel extends AbstractTableModel {
        private final List<SettingDiscrepancy> discrepancies;
        private final String[] columnNames = {
            "Category", "Setting ID", "Setting Name", "OpenPnP Value", "Controller Value", "Difference"
        };
        
        public DiscrepancyTableModel(List<SettingDiscrepancy> discrepancies) {
            this.discrepancies = discrepancies;
        }
        
        @Override
        public int getRowCount() {
            return discrepancies.size();
        }
        
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }
        
        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }
        
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            SettingDiscrepancy discrepancy = discrepancies.get(rowIndex);
            
            switch (columnIndex) {
                case 0: return discrepancy.getCategory();
                case 1: return "$" + discrepancy.getSettingId();
                case 2: return discrepancy.getSettingName();
                case 3: return discrepancy.getFormattedOpenPnpValue();
                case 4: return discrepancy.getFormattedControllerValue();
                case 5: return discrepancy.getFormattedDifference();
                default: return "";
            }
        }
    }
    
    /**
     * Custom renderer for highlighting discrepancies in the table
     */
    private static class DiscrepancyRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            if (!isSelected) {
                // Highlight different columns with colors
                switch (column) {
                    case 0: // Category
                        c.setBackground(new Color(245, 245, 255)); // Light blue
                        break;
                    case 1: // Setting ID
                        c.setBackground(new Color(250, 250, 250)); // Light gray
                        break;
                    case 2: // Setting Name
                        c.setBackground(new Color(250, 250, 250)); // Light gray
                        break;
                    case 3: // OpenPnP Value
                        c.setBackground(new Color(240, 255, 240)); // Light green
                        break;
                    case 4: // Controller Value
                        c.setBackground(new Color(255, 245, 240)); // Light orange
                        break;
                    case 5: // Difference
                        c.setBackground(new Color(255, 240, 240)); // Light red
                        break;
                    default:
                        c.setBackground(table.getBackground());
                }
            }
            
            return c;
        }
    }
}