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

    private static final long serialVersionUID = 1L;
    
    private final ReferenceMachine machine;
    private JCheckBox checkBoxHomeAfterEnabled;
    private String motionPlannerClassName;
    private JTextField discard1XTf;
    private JTextField discard1YTf;
    private JTextField discard1ZTf;
    private JTextField discard1CTf;
    private JTextField discard2XTf;
    private JTextField discard2YTf;
    private JTextField discard2ZTf;
    private JTextField discard2CTf;
    private JTextField discard3XTf;
    private JTextField discard3YTf;
    private JTextField discard3ZTf;
    private JTextField discard3CTf;
    private JTextField discard4XTf;
    private JTextField discard4YTf;
    private JTextField discard4ZTf;
    private JTextField discard4CTf;
    private JTextField defaultBoardXTf;
    private JTextField defaultBoardYTf;
    private JTextField defaultBoardZTf;
    private JTextField defaultBoardCTf;
    private JComboBox motionPlannerClass;
    private boolean reloadWizard;
    private JCheckBox autoToolSelect;
    private JCheckBox safeZPark;
    private JTextField unsafeZRoamingDistance;
    private JCheckBox parkAfterHomed;
    private JCheckBox poolScriptingEngines;
    private JCheckBox autoLoadMostRecentJob;

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
        lblNewLabel.setToolTipText(Translations.getString(
                "ReferenceMachineConfigurationWizard.PanelGeneral.UnsafeZRoamingLabel.toolTipText")); //$NON-NLS-1$
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

        JLabel lblAutoLoadMostRecentJob = new JLabel("Auto-load most recent job?");
        panelGeneral.add(lblAutoLoadMostRecentJob, "2, 16, right, default");

        autoLoadMostRecentJob = new JCheckBox("");
        panelGeneral.add(autoLoadMostRecentJob, "4, 16");

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
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
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
        
        // discard location 1 (default)
        JLabel lblDiscardPoint = new JLabel(Translations.getString(
                        "ReferenceMachineConfigurationWizard.PanelLocations.DiscardLocationLabel.text")); //$NON-NLS-1$
        panelLocations.add(lblDiscardPoint, "2, 4");
        
        discard1XTf = new JTextField();
        panelLocations.add(discard1XTf, "4, 4");
        discard1XTf.setColumns(5);
        
        discard1YTf = new JTextField();
        panelLocations.add(discard1YTf, "6, 4");
        discard1YTf.setColumns(5);
        
        discard1ZTf = new JTextField();
        panelLocations.add(discard1ZTf, "8, 4");
        discard1ZTf.setColumns(5);
        
        discard1CTf = new JTextField();
        panelLocations.add(discard1CTf, "10, 4");
        discard1CTf.setColumns(5);
        
        LocationButtonsPanel discardLocationButtonsPanel =
                        new LocationButtonsPanel(discard1XTf, discard1YTf, discard1ZTf, discard1CTf);
        panelLocations.add(discardLocationButtonsPanel, "12, 4");

        // discard location 2
        lblDiscardPoint = new JLabel(Translations.getString(
                        "ReferenceMachineConfigurationWizard.PanelLocations.DiscardLocationLabel2.text")); //$NON-NLS-1$
        panelLocations.add(lblDiscardPoint, "2, 6");
        
        discard2XTf = new JTextField();
        panelLocations.add(discard2XTf, "4, 6");
        discard2XTf.setColumns(5);
        
        discard2YTf = new JTextField();
        panelLocations.add(discard2YTf, "6, 6");
        discard2YTf.setColumns(5);
        
        discard2ZTf = new JTextField();
        panelLocations.add(discard2ZTf, "8, 6");
        discard2ZTf.setColumns(5);
        
        discard2CTf = new JTextField();
        panelLocations.add(discard2CTf, "10, 6");
        discard2CTf.setColumns(5);
        
        discardLocationButtonsPanel =
                        new LocationButtonsPanel(discard2XTf, discard2YTf, discard2ZTf, discard2CTf);
        panelLocations.add(discardLocationButtonsPanel, "12, 6");

        // discard location 3
        lblDiscardPoint = new JLabel(Translations.getString(
                        "ReferenceMachineConfigurationWizard.PanelLocations.DiscardLocationLabel3.text")); //$NON-NLS-1$
        panelLocations.add(lblDiscardPoint, "2, 8");
        
        discard3XTf = new JTextField();
        panelLocations.add(discard3XTf, "4, 8");
        discard3XTf.setColumns(5);
        
        discard3YTf = new JTextField();
        panelLocations.add(discard3YTf, "6, 8");
        discard3YTf.setColumns(5);
        
        discard3ZTf = new JTextField();
        panelLocations.add(discard3ZTf, "8, 8");
        discard3ZTf.setColumns(5);
        
        discard3CTf = new JTextField();
        panelLocations.add(discard3CTf, "10, 8");
        discard3CTf.setColumns(5);
        
        discardLocationButtonsPanel =
                        new LocationButtonsPanel(discard3XTf, discard3YTf, discard3ZTf, discard3CTf);
        panelLocations.add(discardLocationButtonsPanel, "12, 8");

        // discard location 4
        lblDiscardPoint = new JLabel(Translations.getString(
                        "ReferenceMachineConfigurationWizard.PanelLocations.DiscardLocationLabel4.text")); //$NON-NLS-1$
        panelLocations.add(lblDiscardPoint, "2, 10");
        
        discard4XTf = new JTextField();
        panelLocations.add(discard4XTf, "4, 10");
        discard4XTf.setColumns(5);
        
        discard4YTf = new JTextField();
        panelLocations.add(discard4YTf, "6, 10");
        discard4YTf.setColumns(5);
        
        discard4ZTf = new JTextField();
        panelLocations.add(discard4ZTf, "8, 10");
        discard4ZTf.setColumns(5);
        
        discard4CTf = new JTextField();
        panelLocations.add(discard4CTf, "10, 10");
        discard4CTf.setColumns(5);
        
        discardLocationButtonsPanel =
                        new LocationButtonsPanel(discard4XTf, discard4YTf, discard4ZTf, discard4CTf);
        panelLocations.add(discardLocationButtonsPanel, "12, 10");

        // default board location
		        JLabel lblDefaultBoardPoint = new JLabel(Translations.getString(
		                "ReferenceMachineConfigurationWizard.PanelLocations.DefaultBoardLocationLabel.text")); //$NON-NLS-1$
		panelLocations.add(lblDefaultBoardPoint, "2, 12");
		
		        defaultBoardXTf = new JTextField();
		panelLocations.add(defaultBoardXTf, "4, 12");
		defaultBoardXTf.setColumns(5);
		
		        defaultBoardYTf = new JTextField();
		panelLocations.add(defaultBoardYTf, "6, 12");
		defaultBoardYTf.setColumns(5);
		
		        defaultBoardZTf = new JTextField();
		panelLocations.add(defaultBoardZTf, "8, 12");
		defaultBoardZTf.setColumns(5);
		
		        defaultBoardCTf = new JTextField();
		panelLocations.add(defaultBoardCTf, "10, 12");
		defaultBoardCTf.setColumns(5);
		
		        LocationButtonsPanel defaultBoardLocationButtonsPanel =
		                new LocationButtonsPanel(defaultBoardXTf, defaultBoardYTf, defaultBoardZTf, defaultBoardCTf);
		panelLocations.add(defaultBoardLocationButtonsPanel, "12, 12");
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

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(unsafeZRoamingDistance);

        motionPlannerClassName = machine.getMotionPlanner().getClass().getSimpleName();
        addWrappedBinding(this, "motionPlannerClassName", motionPlannerClass, "selectedItem");

        addWrappedBinding(machine, "poolScriptingEngines", poolScriptingEngines, "selected");
        addWrappedBinding(machine, "autoLoadMostRecentJob", autoLoadMostRecentJob, "selected");

        // discard location 1 (default)
        MutableLocationProxy discardLocation1 = new MutableLocationProxy();
        // blind first/default discard location to "discardLocation" which is available in old versions
        bind(UpdateStrategy.READ_WRITE, machine, "discardLocation", discardLocation1, "location");
        addWrappedBinding(discardLocation1, "lengthX", discard1XTf, "text", lengthConverter);
        addWrappedBinding(discardLocation1, "lengthY", discard1YTf, "text", lengthConverter);
        addWrappedBinding(discardLocation1, "lengthZ", discard1ZTf, "text", lengthConverter);
        addWrappedBinding(discardLocation1, "rotation", discard1CTf, "text", doubleConverter);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(discard1XTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(discard1YTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(discard1ZTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(discard1CTf);

        // discard location 2
        MutableLocationProxy discardLocation2 = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, machine, "discardLocation2", discardLocation2, "location");
        addWrappedBinding(discardLocation2, "lengthX", discard2XTf, "text", lengthConverter);
        addWrappedBinding(discardLocation2, "lengthY", discard2YTf, "text", lengthConverter);
        addWrappedBinding(discardLocation2, "lengthZ", discard2ZTf, "text", lengthConverter);
        addWrappedBinding(discardLocation2, "rotation", discard2CTf, "text", doubleConverter);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(discard2XTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(discard2YTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(discard2ZTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(discard2CTf);

        // discard location 3
        MutableLocationProxy discardLocation3 = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, machine, "discardLocation3", discardLocation3, "location");
        addWrappedBinding(discardLocation3, "lengthX", discard3XTf, "text", lengthConverter);
        addWrappedBinding(discardLocation3, "lengthY", discard3YTf, "text", lengthConverter);
        addWrappedBinding(discardLocation3, "lengthZ", discard3ZTf, "text", lengthConverter);
        addWrappedBinding(discardLocation3, "rotation", discard3CTf, "text", doubleConverter);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(discard3XTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(discard3YTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(discard3ZTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(discard3CTf);

        // discard location 4
        MutableLocationProxy discardLocation4 = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, machine, "discardLocation3", discardLocation4, "location");
        addWrappedBinding(discardLocation4, "lengthX", discard4XTf, "text", lengthConverter);
        addWrappedBinding(discardLocation4, "lengthY", discard4YTf, "text", lengthConverter);
        addWrappedBinding(discardLocation4, "lengthZ", discard4ZTf, "text", lengthConverter);
        addWrappedBinding(discardLocation4, "rotation", discard4CTf, "text", doubleConverter);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(discard4XTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(discard4YTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(discard4ZTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(discard4CTf);

        MutableLocationProxy defaultBoardLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, machine, "defaultBoardLocation", defaultBoardLocation, "location");
        addWrappedBinding(defaultBoardLocation, "lengthX", defaultBoardXTf, "text", lengthConverter);
        addWrappedBinding(defaultBoardLocation, "lengthY", defaultBoardYTf, "text", lengthConverter);
        addWrappedBinding(defaultBoardLocation, "lengthZ", defaultBoardZTf, "text", lengthConverter);
        addWrappedBinding(defaultBoardLocation, "rotation", defaultBoardCTf, "text", doubleConverter);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(defaultBoardXTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(defaultBoardYTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(defaultBoardZTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(defaultBoardCTf);
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
