package org.openpnp.machine.reference.driver.wizards;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.machine.reference.driver.AbstractCommunications;
import org.openpnp.machine.reference.driver.SerialPortCommunications;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

public class AbstractCommunicationsConfigurationWizard extends AbstractConfigurationWizard implements ActionListener {
    private final AbstractCommunications driver;
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
    private JTextField commsMethod;

    public AbstractCommunicationsConfigurationWizard(AbstractCommunications driver) {
        this.driver = driver;

        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        //Selector code
        JPanel panelComms = new JPanel();
        panelComms.setBorder(new TitledBorder(null, "Communications method", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelComms);
        panelComms.setLayout(new FormLayout(new ColumnSpec[]{
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("right:default"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[]{
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

        JRadioButton radioSerial = new JRadioButton("Serial");
        radioSerial.setMnemonic(KeyEvent.VK_S);
        radioSerial.setActionCommand("serial");
        radioSerial.addActionListener(this);

        JRadioButton radioTCP = new JRadioButton("TCP");
        radioTCP.setMnemonic(KeyEvent.VK_T);
        radioTCP.setActionCommand("tcp");
        radioTCP.addActionListener(this);

        commsMethodButtonGroup = new ButtonGroup();
        commsMethodButtonGroup.add(radioSerial);
        commsMethodButtonGroup.add(radioTCP);

        panelComms.add(radioSerial, "4, 2, fill, default");
        panelComms.add(radioTCP, "4, 4, fill, default");

        commsMethod = new JTextField();
        commsMethod.setVisible(false);
        panelComms.add(commsMethod, "4, 6, fill, default");


        //Serial config code
        panelSerial = new JPanel();
        panelSerial.setBorder(new TitledBorder(null, "Serial Port", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelSerial);
        panelSerial.setLayout(new FormLayout(new ColumnSpec[]{
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("right:default"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[]{
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
        panelTcp.setLayout(new FormLayout(new ColumnSpec[]{
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("right:default"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[]{
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

        JLabel lblIpAddress = new JLabel("IP Address");
        panelTcp.add(lblIpAddress, "2, 2, right, default");

        ipAddressTextField = new JTextField(17);
        panelTcp.add(ipAddressTextField, "4, 2, fill, default");
        ipAddressTextField.setColumns(5);

        JLabel lblPort = new JLabel("Port");
        panelTcp.add(lblPort, "2, 4, right, default");

        portTextField = new JTextField(17);
        panelTcp.add(portTextField, "4, 4, fill, default");
        portTextField.setColumns(5);

        //Finally, click a radio button to initialise enabled/disabled setting
        if(driver.getCommunications().equals("serial")){ radioSerial.doClick(); }
        if(driver.getCommunications().equals("tcp")) { radioTCP.doClick(); }
    }

    private void setPanelEnabled(JPanel panel, Boolean isEnabled) {
        panel.setEnabled(isEnabled);

        for (Component cp : panel.getComponents()) {
            cp.setEnabled(isEnabled);
        }
    }

    public void actionPerformed(ActionEvent e) {
        // Enable/Disable controls for selected comms method
        if (e.getActionCommand().equals("serial")) {
            setPanelEnabled(panelSerial, true);
            setPanelEnabled(panelTcp, false);
        } else {
            setPanelEnabled(panelSerial, false);
            setPanelEnabled(panelTcp, true);
        }
        commsMethod.setText(e.getActionCommand());
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

        addWrappedBinding(driver, "communications", commsMethod, "text");

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
    }
}
