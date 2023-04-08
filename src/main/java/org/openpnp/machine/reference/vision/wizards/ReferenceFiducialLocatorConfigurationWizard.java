package org.openpnp.machine.reference.vision.wizards;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.openpnp.Translations;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.VisionSettingsComboBoxModel;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.NamedListCellRenderer;
import org.openpnp.machine.reference.vision.ReferenceFiducialLocator;
import org.openpnp.model.FiducialVisionSettings;
import org.openpnp.model.Footprint;
import org.openpnp.model.Footprint.Pad;
import org.openpnp.model.Part;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class ReferenceFiducialLocatorConfigurationWizard extends AbstractConfigurationWizard {
    private final ReferenceFiducialLocator fiducialLocator;
    private static Part defaultPart = createDefaultPart();
    
    JCheckBox enabledAveragingCheckbox; 
    JTextField textFieldRepeatFiducialRecognition;
    private JTextField maxDistance;
    private JComboBox visionSettings;

    private boolean reloadWizard;

    public ReferenceFiducialLocatorConfigurationWizard(ReferenceFiducialLocator fiducialLocator) {
        this.fiducialLocator = fiducialLocator;

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, Translations.getString(
                "ReferenceFiducialLocatorConfigurationWizard.GeneralPanel.Border.title"), //$NON-NLS-1$
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("right:max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
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
        
        JLabel lblVisionSettings = new JLabel(Translations.getString(
                "ReferenceFiducialLocatorConfigurationWizard.GeneralPanel.VisionSettingsLabel.text")); //$NON-NLS-1$
        panel.add(lblVisionSettings, "2, 2, right, default");
        
        visionSettings = new JComboBox(new VisionSettingsComboBoxModel(FiducialVisionSettings.class));
        visionSettings.setMaximumRowCount(20);
        visionSettings.setRenderer(new NamedListCellRenderer<>());
        visionSettings.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                reloadWizard = true;
            }
        });
        panel.add(visionSettings, "4, 2, 3, 1, fill, default");

        JLabel lblRepeatFiducialRecognition = new JLabel(Translations.getString(
                "ReferenceFiducialLocatorConfigurationWizard.GeneralPanel.RepeatRecognitionLabel.text")); //$NON-NLS-1$
        panel.add(lblRepeatFiducialRecognition, "2, 4");
        
        textFieldRepeatFiducialRecognition = new JTextField();
        textFieldRepeatFiducialRecognition.setToolTipText(Translations.getString(
                "ReferenceFiducialLocatorConfigurationWizard.GeneralPanel.RepeatRecognitionTextField.toolTipText")); //$NON-NLS-1$
        panel.add(textFieldRepeatFiducialRecognition, "4, 4");
        textFieldRepeatFiducialRecognition.setColumns(2);

        JLabel lblEnabledAveraging = new JLabel(Translations.getString(
                "ReferenceFiducialLocatorConfigurationWizard.GeneralPanel.AverageMatchesLabel.text")); //$NON-NLS-1$
        lblEnabledAveraging.setToolTipText(Translations.getString(
                "ReferenceFiducialLocatorConfigurationWizard.GeneralPanel.AverageMatchesLabel.toolTipText")); //$NON-NLS-1$
        panel.add(lblEnabledAveraging, "2, 6");

        enabledAveragingCheckbox = new JCheckBox("");
        panel.add(enabledAveragingCheckbox, "4, 6");
        
        JLabel lblMaxDistance = new JLabel(Translations.getString(
                "ReferenceFiducialLocatorConfigurationWizard.GeneralPanel.MaxDistanceLabel.text")); //$NON-NLS-1$
        lblMaxDistance.setToolTipText(Translations.getString(
                "ReferenceFiducialLocatorConfigurationWizard.GeneralPanel.MaxDistanceLabel.toolTipText")); //$NON-NLS-1$
        panel.add(lblMaxDistance, "2, 8, right, default");
        
        maxDistance = new JTextField();
        panel.add(maxDistance, "4, 8, fill, default");
        maxDistance.setColumns(10);

    }

    private static Part createDefaultPart() {
        Pad pad = new Pad();
        pad.setX(0.);
        pad.setY(0.);
        pad.setWidth(1.);
        pad.setHeight(1.);
        pad.setRoundness(100);
        pad.setName("1");
        Footprint footprint = new Footprint();
        footprint.addPad(pad);
        org.openpnp.model.Package pkg = new org.openpnp.model.Package("TMP");
        pkg.setFootprint(footprint);
        Part part = new Part("TMP");
        part.setPackage(pkg);
        return part;
    }

    @Override
    public void createBindings() {
        IntegerConverter intConverter = new IntegerConverter();
        LengthConverter lengthConverter = new LengthConverter();

        addWrappedBinding(fiducialLocator, "fiducialVisionSettings", visionSettings, "selectedItem");
        
        addWrappedBinding(fiducialLocator, "enabledAveraging", enabledAveragingCheckbox, "selected");
        addWrappedBinding(fiducialLocator, "repeatFiducialRecognition", textFieldRepeatFiducialRecognition, "text", intConverter);
        addWrappedBinding(fiducialLocator, "maxDistance", maxDistance, "text", lengthConverter);

        ComponentDecorators.decorateWithAutoSelect(textFieldRepeatFiducialRecognition);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(maxDistance);
    }

    @Override
    public String getWizardName() {
        return Translations.getString("ReferenceFiducialLocatorConfigurationWizard.wizardName"); //$NON-NLS-1$
    }

    @Override
    protected void saveToModel() {
        super.saveToModel();
        if (reloadWizard) {
            // Reselect the tree path to reload the wizard with potentially different property sheets. 
            MainFrame.get().getMachineSetupTab().selectCurrentTreePath();
        }
    }}
