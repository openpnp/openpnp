package org.openpnp.machine.reference.driver.wizards;

import java.awt.FileDialog;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.StringReader;
import java.io.StringWriter;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.machine.reference.driver.GcodeDriver;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Driver.MotionControlType;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Nozzle;
import org.simpleframework.xml.Serializer;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class GcodeDriverSettings extends AbstractConfigurationWizard {
    private final GcodeDriver driver;

    public GcodeDriverSettings(GcodeDriver driver) {
        this.driver = driver;
        
        JPanel settingsPanel = new JPanel();
        settingsPanel.setBorder(new TitledBorder(null, "Settings", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(settingsPanel);
        settingsPanel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        JLabel lblMotionControlType = new JLabel("Motion Control Type");
        lblMotionControlType.setToolTipText("<html>\r\n<p>Determines how the OpenPnP MotionPlanner will plan the motion and how it will talk <br/>\r\nto the controller:</p>\r\n<ul>\r\n<li><strong>ToolpathFeedRate:</strong><br/>\r\nApply the nominal driver feed-rate limit multiplied by the speed factor to the tool-path.<br/>\r\nThe driver feed-rate must be specified. No acceleration control is applied.</li>\r\n<li><strong>EuclideanAxisLimits:</strong><br/>\r\nApply axis feed-rate, acceleration and jerk limits multiplied by the proper speed factors. <br/>\r\nThe Euclidean Metric is calculated to allow the machine to run faster in a diagonal.<br/>\r\nAll profile motion control is left to the controller. </li>  \r\n<li><strong>ConstantAcceleration:</strong><br/>\r\nApply motion planning assuming a controller with constant acceleration motion control. </li>\r\n<li><strong>SimpleSCurve:</strong><br/>\r\nApply motion planning assuming a controller with simplified S-Curve motion control. <br/>\r\nSimplified S-Curves have no constant acceleration phase, only jerk phases (e.g. TinyG, Marlin).</li>\r\n<li><strong>Full3rdOrderControl:</strong><br/>\r\nApply motion planning assuming a controller with full 3rd order motion control.</li> \r\n</html>");
        settingsPanel.add(lblMotionControlType, "2, 2, right, default");
        
        motionControlType = new JComboBox(MotionControlType.values());
        settingsPanel.add(motionControlType, "4, 2, 5, 1, fill, default");
        
        JLabel lblUnits = new JLabel("Units");
        settingsPanel.add(lblUnits, "6, 4, right, default");
        
        unitsCb = new JComboBox(LengthUnit.values());
        settingsPanel.add(unitsCb, "8, 4, fill, default");
        
        JLabel lblMaxFeedRate = new JLabel("Max Feed Rate [/min]");
        lblMaxFeedRate.setToolTipText("<html><p>Maximum tool-path feed-rate in driver units per minute. </p>\r\n<p>Set to 0 to disable and only use axis feed-rate limits. Diagonal moves will then be faster. </p>\r\n</html>");
        settingsPanel.add(lblMaxFeedRate, "6, 6, right, default");
        
        maxFeedRateTf = new JTextField();
        settingsPanel.add(maxFeedRateTf, "8, 6, fill, default");
        maxFeedRateTf.setColumns(5);
        
        JLabel lblCommandTimeoutms = new JLabel("Command Timeout [ms]");
        settingsPanel.add(lblCommandTimeoutms, "2, 4, right, default");
        
        commandTimeoutTf = new JTextField();
        settingsPanel.add(commandTimeoutTf, "4, 4, fill, default");
        commandTimeoutTf.setColumns(10);
        
        JLabel lblConnectWaitTime = new JLabel("Connect Wait Time [ms]");
        settingsPanel.add(lblConnectWaitTime, "2, 6, right, default");
        
        connectWaitTimeTf = new JTextField();
        settingsPanel.add(connectWaitTimeTf, "4, 6, fill, default");
        connectWaitTimeTf.setColumns(10);
        
        JLabel lblBackslashEscapedCharacters = new JLabel("Backslash Escaped Characters?");
        lblBackslashEscapedCharacters.setToolTipText("Allows insertion of unicode characters into Gcode strings as \\uxxxx "
                + "where xxxx is four hexidecimal characters.  Also permits \\t for tab, \\b for backspace, \\n for line "
                + "feed, \\r for carriage return, and \\f for form feed.");
        settingsPanel.add(lblBackslashEscapedCharacters, "2, 10, right, default");
        
        backslashEscapedCharacters = new JCheckBox("");
        backslashEscapedCharacters.setToolTipText("Allows insertion of unicode characters into Gcode strings as \\uxxxx "
                + "where xxxx is four hexidecimal characters.  Also permits \\t for tab, \\b for backspace, \\n for line "
                + "feed, \\r for carriage return, and \\f for form feed.");
        settingsPanel.add(backslashEscapedCharacters, "4, 10");
        
        JLabel lblLetterVariables = new JLabel("Letter Variables?");
        lblLetterVariables.setToolTipText("Axis variables in Gcode are named using the Axis Letters rather than the Axis Type.");
        settingsPanel.add(lblLetterVariables, "6, 10, right, default");
        
        letterVariables = new JCheckBox("");
        settingsPanel.add(letterVariables, "8, 10");
        
        JLabel lblAllowPremoveCommands = new JLabel("Allow Pre-Move Commands?");
        settingsPanel.add(lblAllowPremoveCommands, "2, 12, right, default");
        
        supportingPreMove = new JCheckBox("");
        settingsPanel.add(supportingPreMove, "4, 12");
    }

    @Override
    public void createBindings() {
        IntegerConverter intConverter = new IntegerConverter();
        DoubleConverter doubleConverter =
                new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        DoubleConverter doubleConverterFine = new DoubleConverter("%f");
        
        addWrappedBinding(driver, "motionControlType", motionControlType, "selectedItem");
        addWrappedBinding(driver, "units", unitsCb, "selectedItem");
        addWrappedBinding(driver, "maxFeedRate", maxFeedRateTf, "text", intConverter);
        addWrappedBinding(driver, "timeoutMilliseconds", commandTimeoutTf, "text", intConverter);
        addWrappedBinding(driver, "connectWaitTimeMilliseconds", connectWaitTimeTf, "text", intConverter);
        addWrappedBinding(driver, "backslashEscapedCharactersEnabled", backslashEscapedCharacters, "selected");
        addWrappedBinding(driver, "supportingPreMove", supportingPreMove, "selected");
        addWrappedBinding(driver, "usingLetterVariables", letterVariables, "selected");
        
        ComponentDecorators.decorateWithAutoSelect(maxFeedRateTf);
        ComponentDecorators.decorateWithAutoSelect(commandTimeoutTf);
        ComponentDecorators.decorateWithAutoSelect(connectWaitTimeTf);
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
    private JComboBox motionControlType;
    private JTextField maxFeedRateTf;
    private JTextField commandTimeoutTf;
    private JTextField connectWaitTimeTf;
    private JComboBox unitsCb;
    private JCheckBox supportingPreMove;
    private JCheckBox letterVariables;
    private JCheckBox backslashEscapedCharacters;

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
            else if (hm instanceof Camera) {
                type = "Camera";
            }
            else if (hm instanceof Actuator) {
                type = "Actuator";
            }
            return String.format("%s: %s %s", type, hm.getHead() == null ? "[No Head]" : hm.getHead().getName(), hm.getName());
        }
    }
}
