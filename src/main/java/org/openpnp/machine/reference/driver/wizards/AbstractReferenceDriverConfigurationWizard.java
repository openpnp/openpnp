package org.openpnp.machine.reference.driver.wizards;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.machine.reference.driver.AbstractReferenceDriver;
import org.openpnp.machine.reference.driver.AbstractReferenceDriver.CommunicationsType;
import org.openpnp.machine.reference.driver.ReferenceDriverCommunications.LineEndingType;
import org.openpnp.machine.reference.driver.SerialPortCommunications;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class AbstractReferenceDriverConfigurationWizard extends AbstractConfigurationWizard {
    private final AbstractReferenceDriver driver;
    private JComboBox comboBoxPort;
    private JComboBox comboBoxBaud;
    private JComboBox flowControlComboBox;
    private JComboBox stopBitsComboBox;
    private JComboBox dataBitsComboBox;
    private JComboBox parityComboBox;
    private JCheckBox setDtrCheckbox;
    private JCheckBox setRtsCheckbox;
    private JTextField portTextField;
    private JTextField ipAddressTextField;
    private ButtonGroup commsMethodButtonGroup;
    private JPanel panelSerial;
    private JPanel panelTcp;
    private JCheckBox connectionKeepAlive;
    private JPanel panelController;
    private JLabel lblName;
    private JTextField driverName;
    private JComboBox communicationsType;
    private JLabel lblCommunicationsType;
    private JLabel lblSyncInitialLocation;
    private JCheckBox syncInitialLocation;
    private JLabel lblLineendings;
    private JComboBox lineEndingType;

    public AbstractReferenceDriverConfigurationWizard(AbstractReferenceDriver driver) {
        this.driver = driver;

        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        
        panelController = new JPanel();
        panelController.setBorder(new TitledBorder(null, "Properties", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelController);
        panelController.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblName = new JLabel("Name");
        panelController.add(lblName, "2, 2, right, default");
        
        driverName = new JTextField();
        panelController.add(driverName, "4, 2, fill, default");
        driverName.setColumns(20);
        
        lblSyncInitialLocation = new JLabel("Sync Initial Location");
        panelController.add(lblSyncInitialLocation, "2, 4, right, default");
        lblSyncInitialLocation.setToolTipText("<html>\nAfter enabling the driver, get the initial location from the controller.<br/>\nIt allows you to safely jog an unhomed machine.\n</html>");
        
        syncInitialLocation = new JCheckBox("");
        panelController.add(syncInitialLocation, "4, 4");

        //Selector code
        JPanel panelComms = new JPanel();
        panelComms.setBorder(new TitledBorder(null, "Communications method", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelComms);
        panelComms.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("right:max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        commsMethodButtonGroup = new ButtonGroup();
        
        communicationsType = new JComboBox(CommunicationsType.values());
        communicationsType.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (communicationsType.getSelectedItem() == CommunicationsType.serial) {
                    setPanelEnabled(panelSerial, true);
                    setPanelEnabled(panelTcp, false);
                } else {
                    setPanelEnabled(panelSerial, false);
                    setPanelEnabled(panelTcp, true);
                }
            }
        });
        
        lblCommunicationsType = new JLabel("Communications Type");
        panelComms.add(lblCommunicationsType, "2, 2, right, default");
        panelComms.add(communicationsType, "4, 2, fill, default");

        JLabel lblConnectionKeepAlive = new JLabel("Keep Alive");
        panelComms.add(lblConnectionKeepAlive, "2, 4, right, default");
        
        connectionKeepAlive = new JCheckBox("");
        panelComms.add(connectionKeepAlive, "4, 4");
        
        lblLineendings = new JLabel("Line-Endings");
        lblLineendings.setToolTipText("<html>\nLine-endings used in commands and responses (if the driver uses them).<br/>\n<ul>\n<li>LF = Line Feed</li>\n<li>CR = Carriage Return</li>\n<li>CRLF = Line Feed & Carriage Return</li>\n</ul>\n</html>\n");
        panelComms.add(lblLineendings, "2, 6, right, default");
        
        lineEndingType = new JComboBox(LineEndingType.values());
        panelComms.add(lineEndingType, "4, 6, fill, default");

        //Serial config code
        panelSerial = new JPanel();
        panelSerial.setBorder(new TitledBorder(null, "Serial Port", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelSerial);
        panelSerial.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("right:max(70dlu;default)"),
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
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblPortName = new JLabel("Port");
        panelSerial.add(lblPortName, "2, 2, right, default");

        comboBoxPort = new JComboBox();
        panelSerial.add(comboBoxPort, "4, 2, fill, default");

        JLabel lblBaudRate = new JLabel("Baud");
        panelSerial.add(lblBaudRate, "2, 4, right, default");

        comboBoxBaud = new JComboBox();
        panelSerial.add(comboBoxBaud, "4, 4, fill, default");

        comboBoxBaud.addItem(new Integer(110));
        comboBoxBaud.addItem(new Integer(300));
        comboBoxBaud.addItem(new Integer(600));
        comboBoxBaud.addItem(new Integer(1200));
        comboBoxBaud.addItem(new Integer(2400));
        comboBoxBaud.addItem(new Integer(4800));
        comboBoxBaud.addItem(new Integer(9600));
        comboBoxBaud.addItem(new Integer(14400));
        comboBoxBaud.addItem(new Integer(19200));
        comboBoxBaud.addItem(new Integer(38400));
        comboBoxBaud.addItem(new Integer(56000));
        comboBoxBaud.addItem(new Integer(57600));
        comboBoxBaud.addItem(new Integer(115200));
        comboBoxBaud.addItem(new Integer(128000));
        comboBoxBaud.addItem(new Integer(153600));
        comboBoxBaud.addItem(new Integer(230400));
        comboBoxBaud.addItem(new Integer(250000));
        comboBoxBaud.addItem(new Integer(256000));
        comboBoxBaud.addItem(new Integer(460800));
        comboBoxBaud.addItem(new Integer(921600));

        JLabel lblParity = new JLabel("Parity");
        panelSerial.add(lblParity, "2, 6, right, default");

        parityComboBox = new JComboBox(SerialPortCommunications.Parity.values());
        panelSerial.add(parityComboBox, "4, 6, fill, default");

        JLabel lblDataBits = new JLabel("Data Bits");
        panelSerial.add(lblDataBits, "2, 8, right, default");

        dataBitsComboBox = new JComboBox(SerialPortCommunications.DataBits.values());
        panelSerial.add(dataBitsComboBox, "4, 8, fill, default");

        JLabel lblStopBits = new JLabel("Stop Bits");
        panelSerial.add(lblStopBits, "2, 10, right, default");

        stopBitsComboBox = new JComboBox(SerialPortCommunications.StopBits.values());
        panelSerial.add(stopBitsComboBox, "4, 10, fill, default");

        JLabel lblFlowControl = new JLabel("Flow Control");
        panelSerial.add(lblFlowControl, "2, 12, right, default");

        flowControlComboBox = new JComboBox(SerialPortCommunications.FlowControl.values());
        panelSerial.add(flowControlComboBox, "4, 12, fill, default");

        JLabel lblSetDtr = new JLabel("Set DTR");
        panelSerial.add(lblSetDtr, "2, 14");

        setDtrCheckbox = new JCheckBox("");
        panelSerial.add(setDtrCheckbox, "4, 14");

        JLabel lblSetRts = new JLabel("Set RTS");
        panelSerial.add(lblSetRts, "2, 16");

        setRtsCheckbox = new JCheckBox("");
        panelSerial.add(setRtsCheckbox, "4, 16");

        comboBoxPort.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                refreshPortList();
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });

        refreshPortList();

        // TCP config code
        panelTcp = new JPanel();
        panelTcp.setBorder(new TitledBorder(null, "TCP", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelTcp);
        panelTcp.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("right:max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblIpAddress = new JLabel("IP Address");
        lblIpAddress.setToolTipText("IP address or host-name. Set to \"GcodeServer\" for an internally simulated Controller.");
        panelTcp.add(lblIpAddress, "2, 2, right, default");

        ipAddressTextField = new JTextField(17);
        panelTcp.add(ipAddressTextField, "4, 2, fill, default");
        ipAddressTextField.setColumns(10);

        JLabel lblPort = new JLabel("Port");
        panelTcp.add(lblPort, "2, 4, right, default");

        portTextField = new JTextField(17);
        panelTcp.add(portTextField, "4, 4, fill, default");
        portTextField.setColumns(10);
    }

    private void setPanelEnabled(JPanel panel, Boolean isEnabled) {
        panel.setVisible(isEnabled);

//        for (Component cp : panel.getComponents()) {
//            cp.setEnabled(isEnabled);
//        }
    }

    private void refreshPortList() {
        if (driver != null) {
            comboBoxPort.removeAllItems();
            boolean exists = false;
            String[] portNames = SerialPortCommunications.getPortNames();
            for (String portName : portNames) {
                comboBoxPort.addItem(portName);
                if (portName.equals(driver.getPortName())) {
                    exists = true;
                }
            }
            if (!exists && driver.getPortName() != null) {
                comboBoxPort.addItem(driver.getPortName());
            }
        }
    }

    @Override
    public void createBindings() {
        IntegerConverter integerConverter = new IntegerConverter();

        addWrappedBinding(driver, "name", driverName, "text");
        addWrappedBinding(driver, "syncInitialLocation", syncInitialLocation, "selected");

        addWrappedBinding(driver, "communicationsType", communicationsType, "selectedItem");
        addWrappedBinding(driver, "connectionKeepAlive", connectionKeepAlive, "selected");
        addWrappedBinding(driver, "lineEndingType", lineEndingType, "selectedItem");
        
        addWrappedBinding(driver, "portName", comboBoxPort, "selectedItem");
        addWrappedBinding(driver, "baud", comboBoxBaud, "selectedItem");
        addWrappedBinding(driver, "parity", parityComboBox, "selectedItem");
        addWrappedBinding(driver, "stopBits", stopBitsComboBox, "selectedItem");
        addWrappedBinding(driver, "dataBits", dataBitsComboBox, "selectedItem");
        addWrappedBinding(driver, "flowControl", flowControlComboBox, "selectedItem");
        addWrappedBinding(driver, "setDtr", setDtrCheckbox, "selected");
        addWrappedBinding(driver, "setRts", setRtsCheckbox, "selected");        
        
        addWrappedBinding(driver, "ipAddress", ipAddressTextField, "text");
        addWrappedBinding(driver, "port", portTextField, "text", integerConverter);

        ComponentDecorators.decorateWithAutoSelect(driverName);
    }
}
