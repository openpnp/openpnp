package org.openpnp.machine.reference.driver.wizards;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.machine.reference.driver.TcpCommunications;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import org.openpnp.gui.components.ComponentDecorators;

public class AbstractTcpDriverConfigurationWizard extends AbstractConfigurationWizard {
    private final TcpCommunications driver;

    public AbstractTcpDriverConfigurationWizard(TcpCommunications driver) {
        this.driver = driver;

        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "TCP", TitledBorder.LEADING, TitledBorder.TOP, null, null));
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

        JLabel lblIpAddress = new JLabel("IP Address");
        contentPanel.add(lblIpAddress, "2, 2, right, default");
        
        ipAddress = new JTextField();
        contentPanel.add(ipAddress, "4, 2, fill, default");
        ipAddress.setColumns(5);
        
        JLabel lblPort = new JLabel("Port");
        contentPanel.add(lblPort, "2, 4, right, default");
        
        port = new JTextField();
        contentPanel.add(port, "4, 4, fill, default");
        port.setColumns(5);
        
        

    }

    @Override
    public void createBindings() {
        IntegerConverter intConverter = new IntegerConverter();

        addWrappedBinding(driver, "port", port, "text", intConverter);
        addWrappedBinding(driver, "ipAddress", ipAddress, "text");
        
        ComponentDecorators.decorateWithAutoSelect(port);
        ComponentDecorators.decorateWithAutoSelect(ipAddress);
    }
    
    private JTextField port;
    private JTextField ipAddress;
}
