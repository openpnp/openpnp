package org.openpnp.machine.reference.driver.wizards;

import java.awt.Cursor;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.StringReader;
import java.io.StringWriter;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import org.openpnp.Translations;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
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
import org.openpnp.util.UiUtils;
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
        settingsPanel.setBorder(new TitledBorder(null, Translations.getString(
                "GcodeDriverSettings.SettingsPanel.Border.title"), //$NON-NLS-1$
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(settingsPanel);
        settingsPanel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),},
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
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                RowSpec.decode("max(50dlu;default)"),}));
        
        JLabel lblMotionControlType = new JLabel(Translations.getString(
                "GcodeDriverSettings.SettingsPanel.MotionControlTypeLabel.text")); //$NON-NLS-1$
        lblMotionControlType.setToolTipText(Translations.getString(
                "GcodeDriverSettings.SettingsPanel.MotionControlTypeLabel.toolTipText")); //$NON-NLS-1$
        settingsPanel.add(lblMotionControlType, "2, 2, right, default");
        
        motionControlType = new JComboBox(MotionControlType.values());
        settingsPanel.add(motionControlType, "4, 2, 5, 1, fill, default");
        
        JLabel lblUnits = new JLabel(Translations.getString("GcodeDriverSettings.SettingsPanel.UnitsLabel.text")); //$NON-NLS-1$
        settingsPanel.add(lblUnits, "6, 4, right, default");
        
        unitsCb = new JComboBox(LengthUnit.values());
        settingsPanel.add(unitsCb, "8, 4, fill, default");
        
        JLabel lblMaxFeedRate = new JLabel(Translations.getString(
                "GcodeDriverSettings.SettingsPanel.MaxFeedRate.text")); //$NON-NLS-1$
        lblMaxFeedRate.setToolTipText(Translations.getString(
                "GcodeDriverSettings.SettingsPanel.MaxFeedRate.toolTipText")); //$NON-NLS-1$
        settingsPanel.add(lblMaxFeedRate, "6, 6, right, default");
        
        maxFeedRateTf = new JTextField();
        settingsPanel.add(maxFeedRateTf, "8, 6, fill, default");
        maxFeedRateTf.setColumns(5);
        
        JLabel lblCommandTimeoutms = new JLabel(Translations.getString(
                "GcodeDriverSettings.SettingsPanel.CommandTimeout.text")); //$NON-NLS-1$
        settingsPanel.add(lblCommandTimeoutms, "2, 4, right, default");
        
        commandTimeoutTf = new JTextField();
        settingsPanel.add(commandTimeoutTf, "4, 4, fill, default");
        commandTimeoutTf.setColumns(10);
        
        JLabel lblConnectWaitTime = new JLabel(Translations.getString(
                "GcodeDriverSettings.SettingsPanel.ConnectWaitTimeoutLabel.text")); //$NON-NLS-1$
        settingsPanel.add(lblConnectWaitTime, "2, 6, right, default");
        
        connectWaitTimeTf = new JTextField();
        settingsPanel.add(connectWaitTimeTf, "4, 6, fill, default");
        connectWaitTimeTf.setColumns(10);
        
        JLabel lblNewLabel = new JLabel("$-Command Wait Time [ms]");
        lblNewLabel.setToolTipText("<html>\n<p>Whenever a command starts with a <strong>$ </strong> sign, add this wait time before<br/>\nsending the next command. The TinyG controller is known to require this<br/>\npause, so it can write settings to the EEPROM uninterrupted. </p>\n<br/>\n<p>Note: in an GcodeAsyncDriver without Confirmation Flow Control, <br/>\nthis is only guaranteed to work, if the $-commands are sent to an idle  <br/>\ncontroller. This is the case if $-commands are sent as the first thing in <br/>\nthe <strong>CONNECT_COMMAND</strong>.</p> \n</html>");
        settingsPanel.add(lblNewLabel, "2, 8, right, default");
        
        dollarWaitTimeMilliseconds = new JTextField();
        settingsPanel.add(dollarWaitTimeMilliseconds, "4, 8, fill, default");
        dollarWaitTimeMilliseconds.setColumns(10);

        JLabel lblLetterVariables = new JLabel(Translations.getString(
                "GcodeDriverSettings.SettingsPanel.LetterVariablesLabel.text")); //$NON-NLS-1$
        lblLetterVariables.setToolTipText(Translations.getString(
                "GcodeDriverSettings.SettingsPanel.LetterVariablesLabel.toolTipText")); //$NON-NLS-1$
        settingsPanel.add(lblLetterVariables, "2, 10, right, default");
        
        letterVariables = new JCheckBox("");
        settingsPanel.add(letterVariables, "4, 10");

        JLabel lblAllowPremoveCommands = new JLabel(Translations.getString(
                "GcodeDriverSettings.SettingsPanel.AllowPreMoveCommandsLabel.text")); //$NON-NLS-1$
        settingsPanel.add(lblAllowPremoveCommands, "6, 10, right, default");
        
        supportingPreMove = new JCheckBox("");
        settingsPanel.add(supportingPreMove, "8, 10");

        JLabel lblCompressGcode = new JLabel(Translations.getString(
                "GcodeDriverSettings.SettingsPanel.CompressGCodeLabel.text")); //$NON-NLS-1$
        lblCompressGcode.setToolTipText(Translations.getString("GcodeDriverSettings.SettingsPanel.CompressGCodeLabel.toolTipText")); //$NON-NLS-1$
        settingsPanel.add(lblCompressGcode, "2, 12, right, default");

        JButton btnDetectFirmware = new JButton(Translations.getString(
                "GcodeDriverSettings.SettingsPanel.DetectFirmwareButton.text")); //$NON-NLS-1$
        btnDetectFirmware.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                firmwareConfiguration.setText(Translations.getString(
                        "GcodeDriverSettings.SettingsPanel.FirmwareConfigurationTextArea.Detecting.text")); //$NON-NLS-1$
                UiUtils.messageBoxOnException(() -> {
                    try {
                        driver.detectFirmware(false, true);
                    }
                    finally {
                        SwingUtilities.invokeLater(() -> {
                            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        });
                    }
                });
            }
        });

        compressGcode = new JCheckBox("");
        settingsPanel.add(compressGcode, "4, 12");

        JLabel lblCompressExcludes = new JLabel(Translations.getString("GcodeDriverSettings.lblCompressExcludes.text")); //$NON-NLS-1$
        lblCompressExcludes.setToolTipText(Translations.getString("GcodeDriverSettings.lblCompressExcludes.toolTipText")); //$NON-NLS-1$
        settingsPanel.add(lblCompressExcludes, "6, 12, right, default");

        compressionExcludes = new JTextField();
        compressionExcludes.setFont(UIManager.getFont("TextArea.font"));
        settingsPanel.add(compressionExcludes, "8, 12, fill, default");
        compressionExcludes.setColumns(10);

        JLabel lblRemoveComments = new JLabel(Translations.getString(
                "GcodeDriverSettings.SettingsPanel.RemoveCommentsLabel.text")); //$NON-NLS-1$
        lblRemoveComments.setToolTipText(Translations.getString(
                "GcodeDriverSettings.SettingsPanel.RemoveCommentsLabel.toolTipText")); //$NON-NLS-1$
        settingsPanel.add(lblRemoveComments, "2, 14, right, default");

        removeComments = new JCheckBox("");
        removeComments.setToolTipText("");
        settingsPanel.add(removeComments, "4, 14");

        JLabel lblBackslashEscapedCharacters = new JLabel(Translations.getString("GcodeDriverSettings.SettingsPanel.BackslashEscapedCharactersLabel.text")); //$NON-NLS-1$
        lblBackslashEscapedCharacters.setToolTipText(Translations.getString("GcodeDriverSettings.SettingsPanel.BackslashEscapedCharactersLabel.toolTipText")); //$NON-NLS-1$
        settingsPanel.add(lblBackslashEscapedCharacters, "6, 14, right, default");

        backslashEscapedCharacters = new JCheckBox("");
        backslashEscapedCharacters.setToolTipText(Translations.getString(
                "GcodeDriverSettings.SettingsPanel.BackslashEscapedCharactersLabel.toolTipText")); //$NON-NLS-1$
        settingsPanel.add(backslashEscapedCharacters, "8, 14");

        JLabel lblLogGcode = new JLabel(Translations.getString("GcodeDriverSettings.SettingsPanel.LogGCodeLabel.text")); //$NON-NLS-1$
        lblLogGcode.setToolTipText(Translations.getString(
                "GcodeDriverSettings.SettingsPanel.LogGCodeLabel.toolTipText")); //$NON-NLS-1$
        settingsPanel.add(lblLogGcode, "2, 16, right, default");

        loggingGcode = new JCheckBox("");
        settingsPanel.add(loggingGcode, "4, 16");
        
        JLabel lblSendOnChangeFeedRate = new JLabel(Translations.getString("GcodeDriverSettings.SettingsPanel.SendOnChangeFeedRate.text")); //$NON-NLS-1$
        lblSendOnChangeFeedRate.setToolTipText(Translations.getString(
                "GcodeDriverSettings.SettingsPanel.SendOnChangeFeedRate.toolTipText")); //$NON-NLS-1$
        settingsPanel.add(lblSendOnChangeFeedRate, "2, 18, right, default");

        sendOnChangeFeedRate = new JCheckBox("");
        settingsPanel.add(sendOnChangeFeedRate, "4, 18");

        JLabel lblSendOnChangeAcceleration = new JLabel(Translations.getString("GcodeDriverSettings.SettingsPanel.SendOnChangeAcceleration.text")); //$NON-NLS-1$
        lblSendOnChangeAcceleration.setToolTipText(Translations.getString(
                "GcodeDriverSettings.SettingsPanel.SendOnChangeAcceleration.toolTipText")); //$NON-NLS-1$
        settingsPanel.add(lblSendOnChangeAcceleration, "2, 20, right, default");

        sendOnChangeAcceleration = new JCheckBox("");
        settingsPanel.add(sendOnChangeAcceleration, "4, 20");
        
        JLabel lblSendOnChangeJerk = new JLabel(Translations.getString("GcodeDriverSettings.SettingsPanel.SendOnChangeJerk.text")); //$NON-NLS-1$
        lblSendOnChangeJerk.setToolTipText(Translations.getString(
                "GcodeDriverSettings.SettingsPanel.SendOnChangeJerk.toolTipText")); //$NON-NLS-1$
        settingsPanel.add(lblSendOnChangeJerk, "2, 22, right, default");

        sendOnChangeJerk = new JCheckBox("");
        settingsPanel.add(sendOnChangeJerk, "4, 22");

        JLabel label_1 = new JLabel(" ");
        settingsPanel.add(label_1, "10, 24");
        settingsPanel.add(btnDetectFirmware, "2, 26");
        
        firmwareConfiguration = new JTextArea();
        firmwareConfiguration.setBackground(UIManager.getColor("controlLtHighlight"));
        firmwareConfiguration.setBorder(BorderFactory.createLineBorder(UIManager.getColor( "Component.borderColor" )));
        firmwareConfiguration.setWrapStyleWord(true);
        firmwareConfiguration.setLineWrap(true);
        firmwareConfiguration.setEditable(false);
        firmwareConfiguration.setFont(new Font("Dialog", Font.PLAIN, 11));
        settingsPanel.add(firmwareConfiguration, "4, 26, 7, 3, fill, fill");
        
        JLabel label = new JLabel(" ");
        settingsPanel.add(label, "2, 28");
    }

    @Override
    public void createBindings() {
        IntegerConverter intConverter = new IntegerConverter();

        addWrappedBinding(driver, "motionControlType", motionControlType, "selectedItem");
        addWrappedBinding(driver, "units", unitsCb, "selectedItem");
        addWrappedBinding(driver, "maxFeedRate", maxFeedRateTf, "text", intConverter);
        addWrappedBinding(driver, "timeoutMilliseconds", commandTimeoutTf, "text", intConverter);
        addWrappedBinding(driver, "connectWaitTimeMilliseconds", connectWaitTimeTf, "text", intConverter);
        addWrappedBinding(driver, "dollarWaitTimeMilliseconds", dollarWaitTimeMilliseconds, "text", intConverter);
        addWrappedBinding(driver, "removeComments", removeComments, "selected");
        addWrappedBinding(driver, "compressGcode", compressGcode, "selected");
        addWrappedBinding(driver, "compressionExcludes", compressionExcludes, "text");
        addWrappedBinding(driver, "backslashEscapedCharactersEnabled", backslashEscapedCharacters, "selected");
        addWrappedBinding(driver, "supportingPreMove", supportingPreMove, "selected");
        addWrappedBinding(driver, "usingLetterVariables", letterVariables, "selected");
        addWrappedBinding(driver, "loggingGcode", loggingGcode, "selected");
        addWrappedBinding(driver, "sendOnChangeFeedRate", sendOnChangeFeedRate, "selected");
        addWrappedBinding(driver, "sendOnChangeAcceleration", sendOnChangeAcceleration, "selected");
        addWrappedBinding(driver, "sendOnChangeJerk", sendOnChangeJerk, "selected");
        addWrappedBinding(driver, "firmwareConfiguration", firmwareConfiguration, "text");

        ComponentDecorators.decorateWithAutoSelect(maxFeedRateTf);
        ComponentDecorators.decorateWithAutoSelect(commandTimeoutTf);
        ComponentDecorators.decorateWithAutoSelect(connectWaitTimeTf);
        ComponentDecorators.decorateWithAutoSelect(compressionExcludes);
    }

    public final Action exportProfileAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.export);
            putValue(NAME, Translations.getString(
                    "GcodeDriverSettings.Action.ExportProfile")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString(
                    "GcodeDriverSettings.Action.ExportProfile.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            try {
                FileDialog fileDialog = new FileDialog(MainFrame.get(), Translations.getString(
                        "GcodeDriverSettings.SaveFileDialog.title"), FileDialog.SAVE); //$NON-NLS-1$
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
            putValue(NAME, Translations.getString("GcodeDriverSettings.Action.ImportProfile"));
            putValue(SHORT_DESCRIPTION, Translations.getString(
                    "GcodeDriverSettings.Action.ImportProfile.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            try {
                FileDialog fileDialog = new FileDialog(MainFrame.get(), Translations.getString(
                        "GcodeDriverSettings.OpenFileDialog.title"), FileDialog.LOAD); //$NON-NLS-1$
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
            putValue(NAME, Translations.getString("GcodeDriverSettings.Action.CopyProfile")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString(
                    "GcodeDriverSettings.Action.CopyProfile.Description")); //$NON-NLS-1$
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
                MessageBoxes.infoBox(Translations.getString("CommonPhrases.copiedGcode"), //$NON-NLS-1$
                        Translations.getString("CommonPhrases.copiedGcodeToClipboard")); //$NON-NLS-1$
            }
            catch (Exception e) {
                MessageBoxes.errorBox(MainFrame.get(), "Copy Failed", e);
            }
        }
    };

    public final Action pasteProfileFromClipboardAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.paste);
            putValue(NAME, Translations.getString("GcodeDriverSettings.Action.PasteProfile")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString(
                    "GcodeDriverSettings.Action.PasteProfile.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            try {
                Serializer ser = Configuration.createSerializer();
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                String s = (String) clipboard.getData(DataFlavor.stringFlavor);
                GcodeDriver d = ser.read(GcodeDriver.class, s);
                // copySettings(d, driver);
                MessageBoxes.infoBox(Translations.getString("CommonPhrases.pastedGcode"), //$NON-NLS-1$
                        Translations.getString("CommonPhrases.pastedGcodeFromClipboard")); //$NON-NLS-1$
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

    private JCheckBox removeComments;

    private JCheckBox compressGcode;

    private JCheckBox loggingGcode;

    private JCheckBox sendOnChangeFeedRate;
    private JCheckBox sendOnChangeAcceleration;
    private JCheckBox sendOnChangeJerk;

    private JTextArea firmwareConfiguration;
    private JTextField dollarWaitTimeMilliseconds;
    private JTextField compressionExcludes;

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
