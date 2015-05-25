package org.openpnp.machine.openbuilds;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.IntegerConverter;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

public class OpenBuildsDriverWizard extends AbstractConfigurationWizard {
    private final OpenBuildsDriver driver;
    private JTextField textFieldPortName;
    private JTextField textFieldBaudRate;
    
    public OpenBuildsDriverWizard(OpenBuildsDriver driver) {
        this.driver = driver;
        
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        
        JPanel panel = new JPanel();
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(new ColumnSpec[] {
                FormFactory.RELATED_GAP_COLSPEC,
                FormFactory.DEFAULT_COLSPEC,
                FormFactory.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),},
            new RowSpec[] {
                FormFactory.RELATED_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.RELATED_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,}));
        
        JLabel lblPortName = new JLabel("Port Name");
        panel.add(lblPortName, "2, 2, right, default");
        
        textFieldPortName = new JTextField();
        panel.add(textFieldPortName, "4, 2, fill, default");
        textFieldPortName.setColumns(10);
        
        JLabel lblBaudRate = new JLabel("Baud Rate");
        panel.add(lblBaudRate, "2, 4, right, default");
        
        textFieldBaudRate = new JTextField();
        panel.add(textFieldBaudRate, "4, 4, fill, default");
        textFieldBaudRate.setColumns(10);
    }
    
    @Override
    public void createBindings() {
    	IntegerConverter integerConverter = new IntegerConverter();

        addWrappedBinding(driver, "portName", textFieldPortName, "text");
        addWrappedBinding(driver, "baud", textFieldBaudRate, "text", integerConverter);
        
      ComponentDecorators.decorateWithAutoSelect(textFieldPortName);
      ComponentDecorators.decorateWithAutoSelect(textFieldBaudRate);
    }
}
