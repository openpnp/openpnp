package org.openpnp.machine.reference.wizards;

import javax.swing.JComboBox;
import javax.swing.JLabel;

import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.machine.openbuilds.OpenBuildsDriver;
import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.driver.GrblDriver;
import org.openpnp.machine.reference.driver.LinuxCNC;
import org.openpnp.machine.reference.driver.MarlinDriver;
import org.openpnp.machine.reference.driver.NullDriver;
import org.openpnp.machine.reference.driver.SimulatorDriver;
import org.openpnp.machine.reference.driver.SprinterDriver;
import org.openpnp.machine.reference.driver.TinygDriver;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class ReferenceMachineConfigurationWizard extends
        AbstractConfigurationWizard {
    
    final private ReferenceMachine machine;
    private JComboBox comboBoxDriver;
    private String driverClassName;
    
    public ReferenceMachineConfigurationWizard(ReferenceMachine machine) {
        this.machine = machine;
        contentPanel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        JLabel lblDriver = new JLabel("Driver");
        contentPanel.add(lblDriver, "2, 2, right, default");
        
        comboBoxDriver = new JComboBox();
        contentPanel.add(comboBoxDriver, "4, 2, fill, default");
        
        comboBoxDriver.addItem(NullDriver.class.getCanonicalName());
        comboBoxDriver.addItem(GrblDriver.class.getCanonicalName());
        comboBoxDriver.addItem(LinuxCNC.class.getCanonicalName());
        comboBoxDriver.addItem(MarlinDriver.class.getCanonicalName());
        comboBoxDriver.addItem(SimulatorDriver.class.getCanonicalName());
        comboBoxDriver.addItem(SprinterDriver.class.getCanonicalName());
        comboBoxDriver.addItem(TinygDriver.class.getCanonicalName());
        comboBoxDriver.addItem(OpenBuildsDriver.class.getCanonicalName());
        comboBoxDriver.addItem(org.firepick.driver.MarlinDriver.class.getCanonicalName());
        comboBoxDriver.addItem(org.firepick.driver.FireStepDriver.class.getCanonicalName());
        
        this.driverClassName = machine.getDriver().getClass().getCanonicalName();
    }

    @Override
    public void createBindings() {
        addWrappedBinding(this, "driverClassName", comboBoxDriver, "selectedItem");
    }
    
    @Override
    protected void saveToModel() {
        super.saveToModel();
        MessageBoxes.errorBox(
                getTopLevelAncestor(), 
                "Restart Required", 
                "Please restart OpenPnP for the changes to take effect.");
    }

    public String getDriverClassName() {
        return driverClassName;
    }
    
    public void setDriverClassName(String driverClassName) throws Exception {
        ReferenceDriver driver = (ReferenceDriver) Class.forName(driverClassName).newInstance();
        machine.setDriver(driver);
        this.driverClassName = driverClassName;
    }    
}
