package org.openpnp.machine.reference.driver.wizards;

import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.machine.reference.driver.GcodeDriver;
import org.openpnp.machine.reference.driver.GcodeDriver.Command;
import org.openpnp.machine.reference.driver.GcodeDriver.CommandType;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PasteDispenser;
import org.simpleframework.xml.Serializer;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class GcodeDriverGcodes extends AbstractConfigurationWizard {
    private final GcodeDriver driver;
    private HashMap<ChangeKey, String> changes = new HashMap<>();
    private boolean ignoreUpdates = false;

    public GcodeDriverGcodes(GcodeDriver driver) {
        this.driver = driver;

        JPanel gcodePanel = new JPanel();
        gcodePanel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "Gcode", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        contentPanel.add(gcodePanel);
        gcodePanel.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, RowSpec.decode("default:grow"),}));

        JLabel lblHeadMountable = new JLabel("Head Mountable");
        gcodePanel.add(lblHeadMountable, "2, 2");

        JLabel lblSetting = new JLabel("Setting");
        gcodePanel.add(lblSetting, "4, 2");

        comboBoxHm = new JComboBox<>();
        gcodePanel.add(comboBoxHm, "2, 4, fill, default");

        comboBoxHm.addItem(new HeadMountableItem(null));
        for (Head head : Configuration.get().getMachine().getHeads()) {
            for (Nozzle hm : head.getNozzles()) {
                comboBoxHm.addItem(new HeadMountableItem(hm));
            }
            for (PasteDispenser hm : head.getPasteDispensers()) {
                comboBoxHm.addItem(new HeadMountableItem(hm));
            }
            for (Camera hm : head.getCameras()) {
                comboBoxHm.addItem(new HeadMountableItem(hm));
            }
            for (Actuator hm : head.getActuators()) {
                comboBoxHm.addItem(new HeadMountableItem(hm));
            }
        }
        for (Actuator actuator : Configuration.get().getMachine().getActuators()) {
            comboBoxHm.addItem(new HeadMountableItem(actuator));
        }

        comboBoxCommandType = new JComboBox<>();
        gcodePanel.add(comboBoxCommandType, "4, 4, fill, default");

        comboBoxHm.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                headMountableChanged();
            }
        });

        comboBoxCommandType.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                commandTypeChanged();
            }
        });

        JScrollPane scrollPane = new JScrollPane();
        gcodePanel.add(scrollPane, "2, 6, 3, 1, fill, fill");

        textAreaCommand = new JTextArea();
        scrollPane.setViewportView(textAreaCommand);
        textAreaCommand.setRows(5);

        JPanel importExportPanel = new JPanel();
        importExportPanel.setBorder(new TitledBorder(null, "Import / Export", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        contentPanel.add(importExportPanel);
        importExportPanel.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        JButton btnExportGcodeProfile = new JButton(exportProfileAction);
        importExportPanel.add(btnExportGcodeProfile, "2, 2");

        JButton btnCopyGcodeProfile = new JButton(copyProfileToClipboardAction);
        importExportPanel.add(btnCopyGcodeProfile, "2, 4");

        headMountableChanged();
        commandTypeChanged();

        textAreaCommand.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                commandTextChanged();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                commandTextChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                commandTextChanged();
            }
        });
    }

    private HeadMountable getSelectedHeadMountable() {
        HeadMountableItem item = (HeadMountableItem) comboBoxHm.getSelectedItem();
        return item.getHeadMountable();
    }

    private CommandType getSelectedCommandType() {
        CommandType commandType = (CommandType) comboBoxCommandType.getSelectedItem();
        return commandType;
    }
    
    private void headMountableChanged() {
        comboBoxCommandType.removeAllItems();
        HeadMountable hm = getSelectedHeadMountable();
        for (CommandType type : CommandType.values()) {
            if (hm == null || type.isHeadMountable()) {
                comboBoxCommandType.addItem(type);
            }
        }
    }

    private void commandTypeChanged() {
        ignoreUpdates = true;
        try {
            HeadMountableItem item = (HeadMountableItem) comboBoxHm.getSelectedItem();
            CommandType commandType = getSelectedCommandType();
            if (item == null || commandType == null) {
                return;
            }
            // First see if there is a pending change
            String text = changes.get(new ChangeKey(getSelectedHeadMountable(), getSelectedCommandType()));
            if (text == null) {
                // If not, see if there is a command on the driver
                Command c = driver.getCommand(item.getHeadMountable(), commandType, false);
                if (c != null) {
                    text = c.getCommand();
                }
            }
            if (text == null) {
                textAreaCommand.setText("");
            }
            else {
                textAreaCommand.setText(text);
            }
        }
        finally {
            ignoreUpdates = false;
        }
    }
    
    private void commandTextChanged() {
        if (ignoreUpdates) {
            return;
        }
        String text = textAreaCommand.getText();
        changes.put(new ChangeKey(getSelectedHeadMountable(), getSelectedCommandType()), text);
        notifyChange();
    }
    
    @Override
    protected void loadFromModel() {
        super.loadFromModel();
        changes.clear();
        commandTypeChanged();
    }

    @Override
    protected void saveToModel() {
        super.saveToModel();
        for (ChangeKey key : changes.keySet()) {
            String text = changes.get(key);
            driver.setCommand(key.hm, key.command, text);
        }
        changes.clear();
    }

    @Override
    public void createBindings() {
    }

    public final Action exportProfileAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.export);
            putValue(NAME, "Export Gcode File");
            putValue(SHORT_DESCRIPTION, "Export the Gcode profile to a file.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            try {
                FileDialog fileDialog = new FileDialog(MainFrame.get(), "Save Gcode Profile As...",
                        FileDialog.SAVE);
                fileDialog.setFilenameFilter(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.toLowerCase().endsWith(".xml");
                    }
                });
                fileDialog.setVisible(true);
                String filename = fileDialog.getFile();
                if (filename == null) {
                    return;
                }
                if (!filename.toLowerCase().endsWith(".xml")) {
                    filename = filename + ".xml";
                }
                File file = new File(new File(fileDialog.getDirectory()), filename);
                if (file.exists()) {
                    int ret = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                            file.getName() + " already exists. Do you want to replace it?",
                            "Replace file?", JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                    if (ret != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
                Serializer s = Configuration.createSerializer();
                FileWriter w = new FileWriter(file);
                s.write(driver, w);
                w.close();
            }
            catch (Exception e) {
                MessageBoxes.errorBox(MainFrame.get(), "Export Failed", e);
            }
        }
    };

    public final Action importProfileAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.importt);
            putValue(NAME, "Load Gcode File");
            putValue(SHORT_DESCRIPTION, "Import the Gcode profile from a file.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            try {
                FileDialog fileDialog = new FileDialog(MainFrame.get(),
                        "Load Gcode Profile From...", FileDialog.LOAD);
                fileDialog.setFilenameFilter(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.toLowerCase().endsWith(".xml");
                    }
                });
                fileDialog.setVisible(true);
                String filename = fileDialog.getFile();
                File file = new File(new File(fileDialog.getDirectory()), filename);
                Serializer ser = Configuration.createSerializer();
                FileReader r = new FileReader(file);
                GcodeDriver d = ser.read(GcodeDriver.class, r);
                // copySettings(d, driver);
            }
            catch (Exception e) {
                MessageBoxes.errorBox(MainFrame.get(), "Import Failed", e);
            }
        }
    };

    public final Action copyProfileToClipboardAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.copy);
            putValue(NAME, "Copy Gcode to Clipboard");
            putValue(SHORT_DESCRIPTION, "Copy the Gcode profile to the clipboard.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            try {
                Serializer s = Configuration.createSerializer();
                StringWriter w = new StringWriter();
                s.write(driver, w);
                StringSelection stringSelection = new StringSelection(w.toString());
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);
                MessageBoxes.infoBox("Copied Gcode", "Copied Gcode to Clipboard");
            }
            catch (Exception e) {
                MessageBoxes.errorBox(MainFrame.get(), "Copy Failed", e);
            }
        }
    };

    public final Action pasteProfileFromClipboardAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.paste);
            putValue(NAME, "Paste Gcode from Clipboard");
            putValue(SHORT_DESCRIPTION, "Import the Gcode profile from the clipboard.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            try {
                Serializer ser = Configuration.createSerializer();
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                String s = (String) clipboard.getData(DataFlavor.stringFlavor);
                StringReader r = new StringReader(s);
                GcodeDriver d = ser.read(GcodeDriver.class, s);
                // copySettings(d, driver);
                MessageBoxes.infoBox("Pasted Gcode", "Pasted Gcode from Clipboard");
            }
            catch (Exception e) {
                MessageBoxes.errorBox(MainFrame.get(), "Paste Failed", e);
            }
        }
    };
    private JComboBox<CommandType> comboBoxCommandType;
    private JComboBox<HeadMountableItem> comboBoxHm;
    private JTextArea textAreaCommand;

    static class HeadMountableItem {
        private HeadMountable hm;

        public HeadMountableItem(HeadMountable hm) {
            this.hm = hm;
        }

        public HeadMountable getHeadMountable() {
            return hm;
        }

        @Override
        public String toString() {
            if (hm == null) {
                return "Default";
            }
            String type = null;
            if (hm instanceof Nozzle) {
                type = "Nozzle";
            }
            else if (hm instanceof PasteDispenser) {
                type = "Paste Dispenser";
            }
            else if (hm instanceof Camera) {
                type = "Camera";
            }
            else if (hm instanceof Actuator) {
                type = "Actuator";
            }
            return String.format("%s: %s %s", type, hm.getHead() == null ? "[No Head]" : hm.getHead().getName(), hm.getName());
        }
    }
    
    class ChangeKey {
        final public HeadMountable hm;
        final public CommandType command;
        
        public ChangeKey(HeadMountable hm, CommandType command) {
            this.hm = hm;
            this.command = command;
        }
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((command == null) ? 0 : command.hashCode());
            result = prime * result + ((hm == null) ? 0 : hm.hashCode());
            return result;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            ChangeKey other = (ChangeKey) obj;
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }
            if (command != other.command) {
                return false;
            }
            if (hm == null) {
                if (other.hm != null) {
                    return false;
                }
            }
            else if (!hm.equals(other.hm)) {
                return false;
            }
            return true;
        }
        
        private GcodeDriverGcodes getOuterType() {
            return GcodeDriverGcodes.this;
        }
    }
}
