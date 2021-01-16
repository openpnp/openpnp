package org.openpnp.machine.reference.wizards;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.model.Configuration;
import org.openpnp.spi.MotionPlanner;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class ReferenceMachineConfigurationWizard extends AbstractConfigurationWizard {

    private final ReferenceMachine machine;
    private JCheckBox checkBoxHomeAfterEnabled;
    private String motionPlannerClassName;
    private JTextField discardXTf;
    private JTextField discardYTf;
    private JTextField discardZTf;
    private JTextField discardCTf;
    private JComboBox motionPlannerClass;
    private boolean reloadWizard;
    private JCheckBox autoToolSelect;

    public ReferenceMachineConfigurationWizard(ReferenceMachine machine) {
        this.machine = machine;

        JPanel panelGeneral = new JPanel();
        contentPanel.add(panelGeneral);
        panelGeneral.setBorder(new TitledBorder(null, "General", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        panelGeneral.setLayout(new FormLayout(new ColumnSpec[] {
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
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        JLabel lblHomeAfterEnabled = new JLabel("Home after enabled?");
        panelGeneral.add(lblHomeAfterEnabled, "2, 2, right, default");
        
        checkBoxHomeAfterEnabled = new JCheckBox("");
        panelGeneral.add(checkBoxHomeAfterEnabled, "4, 2");
        
        JLabel lblAutoToolSelect = new JLabel("Auto tool select?");
        panelGeneral.add(lblAutoToolSelect, "2, 4, right, default");
        
        autoToolSelect = new JCheckBox("");
        panelGeneral.add(autoToolSelect, "4, 4");
        
        JLabel lblMotionPlanning = new JLabel("Motion Planning");
        panelGeneral.add(lblMotionPlanning, "2, 6, right, default");
        
        Object[] classNames = machine.getCompatibleMotionPlannerClasses().stream()
        .map(c -> c.getSimpleName()).toArray();
        motionPlannerClass = new JComboBox(classNames);
        panelGeneral.add(motionPlannerClass, "4, 6, fill, default");
        
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
    }

    @Override
    public void createBindings() {
        DoubleConverter doubleConverter =
                new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        LengthConverter lengthConverter = new LengthConverter();

        addWrappedBinding(machine, "homeAfterEnabled", checkBoxHomeAfterEnabled, "selected");
        addWrappedBinding(machine, "autoToolSelect", autoToolSelect, "selected");

        motionPlannerClassName = machine.getMotionPlanner().getClass().getSimpleName();
        addWrappedBinding(this, "motionPlannerClassName", motionPlannerClass, "selectedItem");

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

    public String getMotionPlannerClassName() {
        return motionPlannerClassName;
    }

    public void setMotionPlannerClassName(String motionPlannerClassName) throws Exception {
        if (machine.getMotionPlanner().getClass().getSimpleName().equals(motionPlannerClassName)) {
            return;
        }
        for (Class<? extends MotionPlanner> motionPlannerClass : machine.getCompatibleMotionPlannerClasses()) {
            if (motionPlannerClass.getSimpleName().equals(motionPlannerClassName)) {
                MotionPlanner motionPlanner = (MotionPlanner) motionPlannerClass.newInstance();
                machine.setMotionPlanner(motionPlanner);
                this.motionPlannerClassName = motionPlannerClassName;
                reloadWizard = true;
                break;
            }
        }
    }

    @Override
    protected void saveToModel() {
        super.saveToModel();
        if (reloadWizard) {
            // Reselect the tree path to reload the wizard with potentially different property sheets. 
            MainFrame.get().getMachineSetupTab().selectCurrentTreePath();
        }
    }
}
