package org.openpnp.machine.reference.driver.wizards;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.machine.reference.driver.AbstractSerialPortDriver;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class AbstractSerialPortDriverConfigurationWizard extends AbstractConfigurationWizard {
    private final AbstractSerialPortDriver driver;
    private JComboBox comboBoxPort;
    private JComboBox comboBoxBaud;
    private JComboBox flowControlComboBox;
    private JComboBox stopBitsComboBox;
    private JComboBox dataBitsComboBox;
    private JComboBox parityComboBox;
    private JCheckBox setDtrCheckbox;
    private JCheckBox setRtsCheckbox;

    public AbstractSerialPortDriverConfigurationWizard(AbstractSerialPortDriver driver) {
        this.driver = driver;

        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "Serial Port", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("right:default"),
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
        panel.add(lblPortName, "2, 2, right, default");

        comboBoxPort = new JComboBox();
        panel.add(comboBoxPort, "4, 2, fill, default");

        JLabel lblBaudRate = new JLabel("Baud");
        panel.add(lblBaudRate, "2, 4, right, default");

        comboBoxBaud = new JComboBox();
        panel.add(comboBoxBaud, "4, 4, fill, default");

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
        panel.add(lblParity, "2, 6, right, default");

        parityComboBox = new JComboBox(AbstractSerialPortDriver.Parity.values());
        panel.add(parityComboBox, "4, 6, fill, default");

        JLabel lblDataBits = new JLabel("Data Bits");
        panel.add(lblDataBits, "2, 8, right, default");

        dataBitsComboBox = new JComboBox(AbstractSerialPortDriver.DataBits.values());
        panel.add(dataBitsComboBox, "4, 8, fill, default");

        JLabel lblStopBits = new JLabel("Stop Bits");
        panel.add(lblStopBits, "2, 10, right, default");

        stopBitsComboBox = new JComboBox(AbstractSerialPortDriver.StopBits.values());
        panel.add(stopBitsComboBox, "4, 10, fill, default");

        JLabel lblFlowControl = new JLabel("Flow Control");
        panel.add(lblFlowControl, "2, 12, right, default");

        flowControlComboBox = new JComboBox(AbstractSerialPortDriver.FlowControl.values());
        panel.add(flowControlComboBox, "4, 12, fill, default");
        
        JLabel lblSetDtr = new JLabel("Set DTR");
        panel.add(lblSetDtr, "2, 14");
        
        setDtrCheckbox = new JCheckBox("");
        panel.add(setDtrCheckbox, "4, 14");
        
        JLabel lblSetRts = new JLabel("Set RTS");
        panel.add(lblSetRts, "2, 16");
        
        setRtsCheckbox = new JCheckBox("");
        panel.add(setRtsCheckbox, "4, 16");

        comboBoxPort.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                refreshPortList();
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {}
        });

        refreshPortList();
    }

    private void refreshPortList() {
        if (driver != null) {
            comboBoxPort.removeAllItems();
            boolean exists = false;
            String[] portNames = driver.getPortNames();
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

        addWrappedBinding(driver, "portName", comboBoxPort, "selectedItem");
        addWrappedBinding(driver, "baud", comboBoxBaud, "selectedItem");
        addWrappedBinding(driver, "parity", parityComboBox, "selectedItem");
        addWrappedBinding(driver, "stopBits", stopBitsComboBox, "selectedItem");
        addWrappedBinding(driver, "dataBits", dataBitsComboBox, "selectedItem");
        addWrappedBinding(driver, "flowControl", flowControlComboBox, "selectedItem");
        addWrappedBinding(driver, "setDtr", setDtrCheckbox, "selected");
        addWrappedBinding(driver, "setRts", setRtsCheckbox, "selected");
    }
}
