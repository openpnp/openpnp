package org.openpnp.machine.reference.driver.wizards;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
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

    public AbstractSerialPortDriverConfigurationWizard(AbstractSerialPortDriver driver) {
        this.driver = driver;

        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        JPanel panel = new JPanel();
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

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
    }
}
