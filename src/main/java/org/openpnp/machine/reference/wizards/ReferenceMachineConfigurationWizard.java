package org.openpnp.machine.reference.wizards;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.Translations;
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
    private JCheckBox safeZPark;
    private JTextField unsafeZRoamingDistance;
    private JCheckBox parkAfterHomed;
    private JCheckBox poolScriptingEngines;

    public ReferenceMachineConfigurationWizard(ReferenceMachine machine) {
        this.machine = machine;

        JPanel panelGeneral = new JPanel();
        contentPanel.add(panelGeneral);
        panelGeneral.setBorder(new TitledBorder(null, Translations.getString(
                "ReferenceMachineConfigurationWizard.PanelGeneral.Border.title"), //$NON-NLS-1$
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        panelGeneral.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
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
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        JLabel lblHomeAfterEnabled = new JLabel(Translations.getString(
                "ReferenceMachineConfigurationWizard.PanelGeneral.HomeAfterEnabledLabel.text")); //$NON-NLS-1$
        panelGeneral.add(lblHomeAfterEnabled, "2, 2, right, default");
        
        checkBoxHomeAfterEnabled = new JCheckBox("");
        panelGeneral.add(checkBoxHomeAfterEnabled, "4, 2");
        
        JLabel lblParkAfterHomed = new JLabel(Translations.getString(
                "ReferenceMachineConfigurationWizard.PanelGeneral.ParkAfterHomedLabel.text")); //$NON-NLS-1$
        panelGeneral.add(lblParkAfterHomed, "2, 4, right, default");
        
        parkAfterHomed = new JCheckBox("");
        panelGeneral.add(parkAfterHomed, "4, 4");
        
        JLabel lblParkAllAtSafeZ = new JLabel(Translations.getString(
                "ReferenceMachineConfigurationWizard.PanelGeneral.ParkAllAtSafeZLabel.text")); //$NON-NLS-1$
        lblParkAllAtSafeZ.setToolTipText(Translations.getString(
                "ReferenceMachineConfigurationWizard.PanelGeneral.ParkAllAtSafeZLabel.toolTipText")); //$NON-NLS-1$
        panelGeneral.add(lblParkAllAtSafeZ, "2, 6, right, default");
        
        safeZPark = new JCheckBox("");
        panelGeneral.add(safeZPark, "4, 6");
        
        JLabel lblAutoToolSelect = new JLabel(Translations.getString(
                "ReferenceMachineConfigurationWizard.PanelGeneral.AutoToolSelectLabel.text")); //$NON-NLS-1$
        lblAutoToolSelect.setToolTipText(Translations.getString("ReferenceMachineConfigurationWizard.lblAutoToolSelect.toolTipText")); //$NON-NLS-1$
        panelGeneral.add(lblAutoToolSelect, "2, 8, right, default");
        
        autoToolSelect = new JCheckBox("");
        panelGeneral.add(autoToolSelect, "4, 8");
        
        JLabel lblNewLabel = new JLabel(Translations.getString(
                "ReferenceMachineConfigurationWizard.PanelGeneral.UnsafeZRoamingLabel.text")); //$NON-NLS-1$
        lblNewLabel.setToolTipText("<html>Maximum allowable roaming distance at unsafe Z.<br/><br/>\r\nVirtual Z axes (typically on cameras) are invisible, therefore it can easily be overlooked<br/>\r\nthat you are at unsafe Z. When you later press the <strong>Move tool to camera location</strong><br/>\r\nbutton, an unexpected Z down-move will result, potentially crashing the tool.<br/>\r\nThe maximum allowable roaming distance at unsafe Z therefore limits the jogging area<br/>\r\nwithin which an unsafe virtual Z is kept, it should be enough to fine-adjust a captured<br/>\r\nlocation. Jogging further away will automatically move the virtual axis to Safe Z.\r\n</html>");
        panelGeneral.add(lblNewLabel, "2, 10, right, default");
        
        unsafeZRoamingDistance = new JTextField();
        panelGeneral.add(unsafeZRoamingDistance, "4, 10, left, default");
        unsafeZRoamingDistance.setColumns(10);
        
        JLabel lblMotionPlanning = new JLabel(Translations.getString(
                "ReferenceMachineConfigurationWizard.PanelGeneral.MotionPlanningLabel.text")); //$NON-NLS-1$
        panelGeneral.add(lblMotionPlanning, "2, 12, right, default");

        Object[] classNames = machine.getCompatibleMotionPlannerClasses().stream()
        .map(c -> c.getSimpleName()).toArray();
        motionPlannerClass = new JComboBox(classNames);
        panelGeneral.add(motionPlannerClass, "4, 12, fill, default");
        
        JLabel lblPoolScriptingEngines = new JLabel("Pool scripting engines?");
        panelGeneral.add(lblPoolScriptingEngines, "2, 14, right, default");

        poolScriptingEngines = new JCheckBox("");
        panelGeneral.add(poolScriptingEngines, "4, 14");

                JPanel panelLocations = new JPanel();
        panelLocations.setBorder(new TitledBorder(null, Translations.getString(
                "ReferenceMachineConfigurationWizard.PanelLocations.Border.title"), //$NON-NLS-1$
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelLocations);
        panelLocations.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
                JLabel lblX = new JLabel("X");
        panelLocations.add(lblX, "4, 2");
        lblX.setHorizontalAlignment(SwingConstants.CENTER);
        
                JLabel lblY = new JLabel("Y");
        panelLocations.add(lblY, "6, 2");
        lblY.setHorizontalAlignment(SwingConstants.CENTER);
        
                JLabel lblZ = new JLabel("Z");
        panelLocations.add(lblZ, "8, 2");
        lblZ.setHorizontalAlignment(SwingConstants.CENTER);
        
                JLabel lblRotation = new JLabel(Translations.getString(
                        "ReferenceMachineConfigurationWizard.PanelLocations.RotationLabel.text")); //$NON-NLS-1$
        panelLocations.add(lblRotation, "10, 2");
        lblRotation.setHorizontalAlignment(SwingConstants.CENTER);
        
                JLabel lblDiscardPoint = new JLabel(Translations.getString(
                        "ReferenceMachineConfigurationWizard.PanelLocations.DiscardLocationLabel.text")); //$NON-NLS-1$
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
        addWrappedBinding(machine, "parkAfterHomed", parkAfterHomed, "selected");
        addWrappedBinding(machine, "autoToolSelect", autoToolSelect, "selected");
        addWrappedBinding(machine, "safeZPark", safeZPark, "selected");
        addWrappedBinding(machine, "unsafeZRoamingDistance", unsafeZRoamingDistance, "text", lengthConverter);

        motionPlannerClassName = machine.getMotionPlanner().getClass().getSimpleName();
        addWrappedBinding(this, "motionPlannerClassName", motionPlannerClass, "selectedItem");

        addWrappedBinding(machine, "poolScriptingEngines", poolScriptingEngines, "selected");

        MutableLocationProxy discardLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, machine, "discardLocation", discardLocation, "location");
        addWrappedBinding(discardLocation, "lengthX", discardXTf, "text", lengthConverter);
        addWrappedBinding(discardLocation, "lengthY", discardYTf, "text", lengthConverter);
        addWrappedBinding(discardLocation, "lengthZ", discardZTf, "text", lengthConverter);
        addWrappedBinding(discardLocation, "rotation", discardCTf, "text", doubleConverter);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(unsafeZRoamingDistance);
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
