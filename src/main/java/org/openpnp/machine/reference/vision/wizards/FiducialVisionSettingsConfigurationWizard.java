package org.openpnp.machine.reference.vision.wizards;

import java.awt.Color;
import java.awt.Component;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.Translations;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.PipelineControls;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.machine.reference.vision.ReferenceFiducialLocator;
import org.openpnp.model.AbstractVisionSettings;
import org.openpnp.model.Configuration;
import org.openpnp.model.FiducialVisionSettings;
import org.openpnp.model.Location;
import org.openpnp.model.Package;
import org.openpnp.model.Part;
import org.openpnp.model.PartSettingsHolder;
import org.openpnp.spi.Camera;
import org.openpnp.util.UiUtils;
import org.openpnp.vision.pipeline.CvPipeline;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.layout.Sizes;

@SuppressWarnings("serial")
public class FiducialVisionSettingsConfigurationWizard extends AbstractConfigurationWizard {
    private final ReferenceFiducialLocator fiducialLocator;
    private final FiducialVisionSettings visionSettings;
    private final PartSettingsHolder settingsHolder;


    private JPanel panel;
    private JCheckBox enabledCheckbox;

    private JLabel usedIn;  
    private JTextField name;
    private JLabel lblName;

    private JButton btnSpecializeSetting;
    private JButton btnGeneralizeSettings;
    private PipelineControls pipelinePanel;
    private JLabel lblParallaxDiameter;
    private JTextField parallaxDiameter;
    private JLabel lblParallaxAngle;
    private JTextField parallaxAngle;
    private JLabel lblMaxVisionPasses;
    private JTextField maxVisionPasses;
    private JLabel lblMaxLinearOffset;
    private JTextField maxLinearOffset;

    public FiducialVisionSettingsConfigurationWizard(FiducialVisionSettings visionSettings, 
            PartSettingsHolder settingsHolder) {
        this.visionSettings = visionSettings;
        this.settingsHolder = settingsHolder;
        this.fiducialLocator = (ReferenceFiducialLocator) Configuration.get().getMachine().getFiducialLocator();
        createUi();
    }

    private void createUi() {
        createPanel();

        lblName = new JLabel(Translations.getString(
                "FiducialVisionSettingsConfigurationWizard.GeneralPanel.NameLabel.text")); //$NON-NLS-1$
        panel.add(lblName, "2, 2, right, default");

        name = new JTextField();
        //name.setEditable(false);
        panel.add(name, "4, 2, 11, 1, fill, default");
        name.setColumns(10);

        JPanel panelAssignedTo = new JPanel();
        panelAssignedTo.setBorder(UIManager.getBorder("TextField.border"));
        panel.add(panelAssignedTo, "4, 4, 11, 1, fill, fill");
        panelAssignedTo.setLayout(new FormLayout(new ColumnSpec[] {
                ColumnSpec.decode("min(70dlu;default):grow"),},
                new RowSpec[] {
                        FormSpecs.DEFAULT_ROWSPEC,}));
        usedIn = new JLabel("None");
        panelAssignedTo.add(usedIn, "1, 1, fill, fill");

        JLabel lblAssignedTo = new JLabel(Translations.getString(
                "FiducialVisionSettingsConfigurationWizard.GeneralPanel.AssignedToLabel.text")); //$NON-NLS-1$
        panel.add(lblAssignedTo, "2, 4");

        JLabel lblSettings = new JLabel(Translations.getString(
                "FiducialVisionSettingsConfigurationWizard.GeneralPanel.ManageSettingsLabel.text")); //$NON-NLS-1$
        panel.add(lblSettings, "2, 6");

        btnSpecializeSetting = new JButton();
        btnSpecializeSetting.setText(" ");
        if (settingsHolder != null && fiducialLocator.getParentHolder(settingsHolder) != null) {
            btnSpecializeSetting.setText(Translations.getString(
                    "FiducialVisionSettingsConfigurationWizard.GeneralPanel.SpecializeSettingsButton.SpecializeForText" //$NON-NLS-1$
            ) + " " + settingsHolder.getShortName()); //$NON-NLS-1$
            btnSpecializeSetting.setToolTipText(Translations.getString(
                    "FiducialVisionSettingsConfigurationWizard.GeneralPanel.SpecializeSettingsButton.toolTipText" //$NON-NLS-1$
            ) + " " + settingsHolder.getClass().getSimpleName()+" "+settingsHolder.getShortName()); //$NON-NLS-1$ //$NON-NLS-2$
        }
        else if (settingsHolder != null) {
            btnSpecializeSetting.setText(Translations.getString(
                    "FiducialVisionSettingsConfigurationWizard.GeneralPanel.SpecializeSettingsButton.OptimizeText" //$NON-NLS-1$
            ));
            btnSpecializeSetting.setToolTipText(Translations.getString(
                    "FiducialVisionSettingsConfigurationWizard.GeneralPanel.SpecializeSettingsButton.OptimizeText.toolTipText" //$NON-NLS-1$
            ));
        }
        else {
            btnSpecializeSetting.setEnabled(false);
            btnSpecializeSetting.setText(Translations.getString(
                    "FiducialVisionSettingsConfigurationWizard.GeneralPanel.SpecializeSettingsButton.SpecializeText")); //$NON-NLS-1$
            btnSpecializeSetting.setToolTipText("");
        }

        btnSpecializeSetting.addActionListener(e -> {
            applyAction.actionPerformed(null);
            UiUtils.messageBoxOnException(() -> {
                if (settingsHolder != null && fiducialLocator.getParentHolder(settingsHolder) != null) {
                    if (visionSettings.getUsedFiducialVisionIn().size() == 1 
                            && visionSettings.getUsedFiducialVisionIn().get(0) == settingsHolder) {
                        throw new Exception("Vision Settings already specialized for "+settingsHolder.getShortName()+".");
                    }
                    FiducialVisionSettings newSettings = new FiducialVisionSettings();
                    newSettings.setValues(visionSettings);
                    newSettings.setName(settingsHolder.getShortName());
                    settingsHolder.setFiducialVisionSettings(newSettings);
                    Configuration.get().addVisionSettings(newSettings);
                }
                else {
                    ReferenceFiducialLocator.getDefault().optimizeVisionSettings(Configuration.get());
                    Configuration.get().fireVisionSettingsChanged();
                }
            });
        });
        panel.add(btnSpecializeSetting, "4, 6, 3, 1");

        final String subjects = settingsHolder instanceof Package ? "Parts" : "Parts and Packages";
        btnGeneralizeSettings = new JButton(Translations.getString(
                "FiducialVisionSettingsConfigurationWizard.GeneralPanel.GeneralizeSettingsButton.text")); //$NON-NLS-1$
        btnGeneralizeSettings.addActionListener((e) -> {
            UiUtils.messageBoxOnException(() -> {
                List<PartSettingsHolder> list = settingsHolder.getSpecializedFiducialVisionIn();
                if (list.size() == 0) {
                    throw new Exception("There are no specializations on "+subjects+" with the "+settingsHolder.getClass().getSimpleName()+" "+settingsHolder.getShortName()+".");
                }
                int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                        "This will remove the specialized vision settings in:\n\n"+
                                new AbstractVisionSettings.ListConverter(false).convertForward(list)+"\n\n"+
                                "Are you sure?", null,
                                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (result == JOptionPane.YES_OPTION) {
                    UiUtils.messageBoxOnException(settingsHolder::generalizeFiducialVisionSettings);
                }
            });
        });
        panel.add(btnGeneralizeSettings, "8, 6, 3, 1");
        if (settingsHolder == null || settingsHolder instanceof Part) {
            btnGeneralizeSettings.setEnabled(false);
        }
        else {
            btnGeneralizeSettings.setText(Translations.getString(
                    "FiducialVisionSettingsConfigurationWizard.GeneralPanel.GeneralizeSettingsButton.GeneralizeFor.text" //$NON-NLS-1$
            ) + " " +settingsHolder.getShortName()); //$NON-NLS-1$
            btnGeneralizeSettings.setToolTipText("<html>Generalize these Fiducial Vision Settings for all the "
                    + subjects
                    + " with the "+ settingsHolder.getClass().getSimpleName()+" "+settingsHolder.getShortName()+".<br/>"
                    + "This will unassign any special Fiducial Vision Settings on "+subjects+" and delete those<br/>"
                    + "Fiducial Vision Settings that are no longer used elsewhere.</html>");
        }

        JButton resetButton = new JButton(Translations.getString(
                "FiducialVisionSettingsConfigurationWizard.GeneralPanel.ResetButton.text")); //$NON-NLS-1$
        resetButton.addActionListener(e -> {
            UiUtils.messageBoxOnException(() -> {
                int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                        "This will reset the fiducial vision settings with to the default settings. Are you sure??", null,
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (result == JOptionPane.YES_OPTION) {
                    ReferenceFiducialLocator fiducialVision = ReferenceFiducialLocator.getDefault();
                    if (fiducialVision.getFiducialVisionSettings() == visionSettings) {
                        // Already the default. Set stock.
                        visionSettings.resetToDefault();
                    }
                    else {
                        visionSettings.setValues(fiducialVision.getFiducialVisionSettings());
                    }
                }
            });
        });
        panel.add(resetButton, "12, 6");

        JLabel lblEnabled = new JLabel(Translations.getString(
                "FiducialVisionSettingsConfigurationWizard.GeneralPanel.EnabledLabel.text")); //$NON-NLS-1$
        panel.add(lblEnabled, "2, 8");

        enabledCheckbox = new JCheckBox("");
        panel.add(enabledCheckbox, "4, 8");

        pipelinePanel = new PipelineControls() {

            @Override
            public void configurePipeline(CvPipeline pipeline, Map<String, Object> pipelineParameterAssignments, boolean edit) throws Exception {
                UiUtils.messageBoxOnException(() -> {
                    if (edit) {
                        // Accept changes before edit.
                        applyAction.actionPerformed(null);
                    }
                    pipelineConfiguration(getPipeline(), getPipelineParameterAssignments(), edit);
                });
            }

            @Override
            public void resetPipeline() throws Exception {
                int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                        "This will replace the Pipeline with the default. Are you sure??", null,
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (result == JOptionPane.YES_OPTION) {
                    UiUtils.messageBoxOnException(() -> {
                        applyAction.actionPerformed(null);
                        ReferenceFiducialLocator fiducialLocator = ReferenceFiducialLocator.getDefault();
                        if (fiducialLocator.getFiducialVisionSettings() == visionSettings) {
                            // Already the default. Set stock.
                            pipelinePanel.setPipeline(ReferenceFiducialLocator.createStockPipeline("Default"));
                        }
                        else {
                            pipelinePanel.setPipeline(fiducialLocator.getFiducialVisionSettings().getPipeline().clone());
                        }
                    });
                }
            }
        };
        pipelinePanel.setResetable(true);
        pipelinePanel.setEditable(true);
        panel.add(pipelinePanel, "1, 12, 14, 1, fill, fill");

        JPanel panelAlign = new JPanel();
        contentPanel.add(panelAlign);
        panelAlign.setBorder(new TitledBorder(new LineBorder(new Color(184, 207, 229)), "Fiducial Locator", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(51, 51, 51)));
        panelAlign.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("right:max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("min(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                new ColumnSpec(ColumnSpec.FILL, Sizes.bounded(Sizes.DEFAULT, Sizes.constant("50dlu", true), Sizes.constant("70dlu", true)), 0),
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
        
        lblMaxVisionPasses = new JLabel(Translations.getString("FiducialVisionSettingsConfigurationWizard.lblMaxVisionPasses.text")); //$NON-NLS-1$
        lblMaxVisionPasses.setToolTipText(Translations.getString("FiducialVisionSettingsConfigurationWizard.lblMaxVisionPasses.toolTipText")); //$NON-NLS-1$
        panelAlign.add(lblMaxVisionPasses, "2, 2, right, default");
        
        maxVisionPasses = new JTextField();
        panelAlign.add(maxVisionPasses, "4, 2, fill, default");
        maxVisionPasses.setColumns(10);
        
        lblMaxLinearOffset = new JLabel(Translations.getString("FiducialVisionSettingsConfigurationWizard.lblMaxLinearOffset.text")); //$NON-NLS-1$
        lblMaxLinearOffset.setToolTipText(Translations.getString("FiducialVisionSettingsConfigurationWizard.lblMaxLinearOffset.toolTipText")); //$NON-NLS-1$
        panelAlign.add(lblMaxLinearOffset, "8, 2, right, default");
        
        maxLinearOffset = new JTextField();
        panelAlign.add(maxLinearOffset, "10, 2, fill, default");
        maxLinearOffset.setColumns(10);
        
        lblParallaxDiameter = new JLabel(Translations.getString("FiducialVisionSettingsConfigurationWizard.lblParallaxDiameter.text")); //$NON-NLS-1$
        lblParallaxDiameter.setToolTipText(Translations.getString("FiducialVisionSettingsConfigurationWizard.lblParallaxDiameter.toolTipText")); //$NON-NLS-1$
        panelAlign.add(lblParallaxDiameter, "2, 4, right, default");
        
        parallaxDiameter = new JTextField();
        panelAlign.add(parallaxDiameter, "4, 4, fill, default");
        parallaxDiameter.setColumns(10);
        
        lblParallaxAngle = new JLabel(Translations.getString("FiducialVisionSettingsConfigurationWizard.lblParallaxAngle.text")); //$NON-NLS-1$
        lblParallaxAngle.setToolTipText(Translations.getString("FiducialVisionSettingsConfigurationWizard.lblParallaxAngle.toolTipText")); //$NON-NLS-1$
        panelAlign.add(lblParallaxAngle, "8, 4, right, default");
        
        parallaxAngle = new JTextField();
        panelAlign.add(parallaxAngle, "10, 4, fill, default");
        parallaxAngle.setColumns(10);
        
        JButton btnTestFiducialLocator = new JButton(Translations.getString(
                "FiducialVisionSettingsConfigurationWizard.TestFiducialLocatorPanel.TestFiducialLocatorButton.text" //$NON-NLS-1$
                ));
        panelAlign.add(btnTestFiducialLocator, "4, 8");
        btnTestFiducialLocator.addActionListener((e) -> {
            applyAction.actionPerformed(null);
            UiUtils.submitUiMachineTask(() -> {
                testFiducialLocation();
            });
        });
        // Only available on Part or Package (it needs the footprint).
        btnTestFiducialLocator.setEnabled(
                settingsHolder instanceof Part 
                || settingsHolder instanceof org.openpnp.model.Package);
    }

    @Override
    public void createBindings() {
        lblName.setVisible(settingsHolder != null);
        name.setVisible(settingsHolder != null);
        if (visionSettings.isStockSetting()) {
            for (Component comp : panel.getComponents()) {
                if (comp != btnSpecializeSetting && comp != btnGeneralizeSettings) { 
                    comp.setEnabled(false);
                }
            }
        }

        LengthConverter lengthConverter = new LengthConverter();
        IntegerConverter intConverter = new IntegerConverter();
        DoubleConverter doubleConverter = new DoubleConverter(Configuration.get()
                .getLengthDisplayFormat());

        addWrappedBinding(visionSettings, "name", name, "text");
        bind(UpdateStrategy.READ, visionSettings, "usedFiducialVisionIn", usedIn, "text", 
                new AbstractVisionSettings.ListConverter(true, settingsHolder));
        addWrappedBinding(visionSettings, "enabled", enabledCheckbox, "selected");

        bind(UpdateStrategy.READ_WRITE, visionSettings, "pipeline", pipelinePanel, "pipeline");
        addWrappedBinding(visionSettings, "pipelineParameterAssignments", pipelinePanel, "pipelineParameterAssignments");

        addWrappedBinding(visionSettings, "maxVisionPasses", maxVisionPasses, "text", intConverter);
        addWrappedBinding(visionSettings, "maxLinearOffset", maxLinearOffset, "text", lengthConverter);

        addWrappedBinding(visionSettings, "parallaxDiameter", parallaxDiameter, "text", lengthConverter);
        addWrappedBinding(visionSettings, "parallaxAngle", parallaxAngle, "text", doubleConverter);

        ComponentDecorators.decorateWithAutoSelect(maxVisionPasses);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(maxLinearOffset);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(parallaxDiameter);
        ComponentDecorators.decorateWithAutoSelect(parallaxAngle);

    }

    private void createPanel() {
        panel = new JPanel();
        panel.setBorder(new TitledBorder(null, Translations.getString(
                "FiducialVisionSettingsConfigurationWizard.GeneralPanel.Border.title"), //$NON-NLS-1$
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("right:max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(50dlu;default)"),
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
                RowSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
    }

    private void pipelineConfiguration(CvPipeline pipeline, Map<String, Object> pipelineParameterAssignments, boolean edit) throws Exception {
        fiducialLocator.preparePipeline(pipeline, pipelineParameterAssignments, fiducialLocator.getVisionCamera(), settingsHolder, Location.origin);

        if (edit) {
            pipelinePanel.openPipelineEditor("Fiducial Vision Pipeline", pipeline);
        }
    }

    private void testFiducialLocation() throws Exception {
        Camera camera = fiducialLocator.getVisionCamera();
        Location location = fiducialLocator.getFiducialLocation(camera.getLocation(), settingsHolder);
        camera.moveTo(location);
    }

    @Override
    public String getWizardName() {
        return Translations.getString("FiducialVisionSettingsConfigurationWizard.wizardName"); //$NON-NLS-1$
    }

}
