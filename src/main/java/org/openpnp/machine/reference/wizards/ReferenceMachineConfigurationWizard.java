package org.openpnp.machine.reference.wizards;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.openbuilds.OpenBuildsDriver;
import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.driver.GcodeDriver;
import org.openpnp.machine.reference.driver.GrblDriver;
import org.openpnp.machine.reference.driver.LinuxCNC;
import org.openpnp.machine.reference.driver.MarlinDriver;
import org.openpnp.machine.reference.driver.NullDriver;
import org.openpnp.machine.reference.driver.SimulatorDriver;
import org.openpnp.machine.reference.driver.SprinterDriver;
import org.openpnp.machine.reference.driver.TinygDriver;
import org.openpnp.model.Configuration;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class ReferenceMachineConfigurationWizard extends AbstractConfigurationWizard {

    final private ReferenceMachine machine;
    private JComboBox comboBoxDriver;
    private String driverClassName;
    private JTextField discardXTf;
    private JTextField discardYTf;
    private JTextField discardZTf;
    private JTextField discardCTf;

    public ReferenceMachineConfigurationWizard(ReferenceMachine machine) {
        this.machine = machine;

        JPanel panelGeneral = new JPanel();
        contentPanel.add(panelGeneral);
        panelGeneral.setBorder(new TitledBorder(null, "General", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        panelGeneral.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));
        JLabel lblDriver = new JLabel("Driver");
        panelGeneral.add(lblDriver, "2, 2");

        comboBoxDriver = new JComboBox();
        panelGeneral.add(comboBoxDriver, "4, 2");

        comboBoxDriver.addItem(NullDriver.class.getCanonicalName());
        comboBoxDriver.addItem(GcodeDriver.class.getCanonicalName());
        comboBoxDriver.addItem(GrblDriver.class.getCanonicalName());
        comboBoxDriver.addItem(LinuxCNC.class.getCanonicalName());
        comboBoxDriver.addItem(MarlinDriver.class.getCanonicalName());
        comboBoxDriver.addItem(SimulatorDriver.class.getCanonicalName());
        comboBoxDriver.addItem(SprinterDriver.class.getCanonicalName());
        comboBoxDriver.addItem(TinygDriver.class.getCanonicalName());
        comboBoxDriver.addItem(OpenBuildsDriver.class.getCanonicalName());

        JPanel panelLocations = new JPanel();
        panelLocations.setBorder(new TitledBorder(null, "Locations", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        contentPanel.add(panelLocations);
        panelLocations.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblX = new JLabel("X");
        panelLocations.add(lblX, "4, 2");
        lblX.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel lblY = new JLabel("Y");
        panelLocations.add(lblY, "6, 2");
        lblY.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel lblZ = new JLabel("Z");
        panelLocations.add(lblZ, "8, 2");
        lblZ.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel lblRotation = new JLabel("Rotation");
        panelLocations.add(lblRotation, "10, 2");
        lblRotation.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel lblDiscardPoint = new JLabel("Discard Location");
        panelLocations.add(lblDiscardPoint, "2, 4");

        discardXTf = new JTextField();
        panelLocations.add(discardXTf, "4, 4");
        discardXTf.setColumns(5);

        discardYTf = new JTextField();
        panelLocations.add(discardYTf, "6, 4");
        discardYTf.setColumns(5);

        discardZTf = new JTextField();
        panelLocations.add(discardZTf, "8, 4");
        discardZTf.setColumns(5);

        discardCTf = new JTextField();
        panelLocations.add(discardCTf, "10, 4");
        discardCTf.setColumns(5);

        LocationButtonsPanel locationButtonsPanel =
                new LocationButtonsPanel(discardXTf, discardYTf, discardZTf, discardCTf);
        panelLocations.add(locationButtonsPanel, "12, 4");

        this.driverClassName = machine.getDriver().getClass().getCanonicalName();
    }

    @Override
    public void createBindings() {
        DoubleConverter doubleConverter =
                new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        LengthConverter lengthConverter = new LengthConverter();

        addWrappedBinding(this, "driverClassName", comboBoxDriver, "selectedItem");

        MutableLocationProxy discardLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, machine, "discardLocation", discardLocation, "location");
        addWrappedBinding(discardLocation, "lengthX", discardXTf, "text", lengthConverter);
        addWrappedBinding(discardLocation, "lengthY", discardYTf, "text", lengthConverter);
        addWrappedBinding(discardLocation, "lengthZ", discardZTf, "text", lengthConverter);
        addWrappedBinding(discardLocation, "rotation", discardCTf, "text", doubleConverter);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(discardXTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(discardYTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(discardZTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(discardCTf);
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) throws Exception {
        if (machine.getDriver().getClass().getCanonicalName().equals(driverClassName)) {
            return;
        }
        ReferenceDriver driver = (ReferenceDriver) Class.forName(driverClassName).newInstance();
        machine.setDriver(driver);
        this.driverClassName = driverClassName;
        MessageBoxes.errorBox(getTopLevelAncestor(), "Restart Required",
                "Please restart OpenPnP for the changes to take effect.");
    }
}
