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
import org.openpnp.gui.components.PipelinePanel;
import org.openpnp.gui.support.AbstractConfigurationWizard;
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
    private PipelinePanel pipelinePanel;

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
                "FiducialVisionSettingsConfigurationWizard.GeneralPanel.NameLabel.text"));
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
                "FiducialVisionSettingsConfigurationWizard.GeneralPanel.AssignedToLabel.text"));
        panel.add(lblAssignedTo, "2, 4");

        JLabel lblSettings = new JLabel(Translations.getString(
                "FiducialVisionSettingsConfigurationWizard.GeneralPanel.ManageSettingsLabel.text"));
        panel.add(lblSettings, "2, 6");

        btnSpecializeSetting = new JButton();
        btnSpecializeSetting.setText(" ");
        if (settingsHolder != null && fiducialLocator.getParentHolder(settingsHolder) != null) {
            btnSpecializeSetting.setText(Translations.getString(
                    "FiducialVisionSettingsConfigurationWizard.GeneralPanel.SpecializeSettingsButton.SpecializeForText"
            ) + " " + settingsHolder.getShortName());
            btnSpecializeSetting.setToolTipText(Translations.getString(
                    "FiducialVisionSettingsConfigurationWizard.GeneralPanel.SpecializeSettingsButton.toolTipText"
            ) + " " + settingsHolder.getClass().getSimpleName()+" "+settingsHolder.getShortName());
        }
        else if (settingsHolder != null) {
            btnSpecializeSetting.setText(Translations.getString(
                    "FiducialVisionSettingsConfigurationWizard.GeneralPanel.SpecializeSettingsButton.OptimizeText"
            ));
            btnSpecializeSetting.setToolTipText("<html>Optimize the Fiducial Vision Settings and their assignments:<br/>"
                    + "<ul>"
                    + "<li>Consolidate duplicate settings.</li>"
                    + "<li>Remove unused settings.</li>"
                    + "<li>Configure the most common Part settings as inherited Package settings.</li>"
                    + "<li>Remove assignments where the same settings would be inherited anyway.</li>"
                    + "</ul>"
                    + "</html>");
        }
        else {
            btnSpecializeSetting.setEnabled(false);
            btnSpecializeSetting.setText(Translations.getString(
                    "FiducialVisionSettingsConfigurationWizard.GeneralPanel.SpecializeSettingsButton.SpecializeText"));
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
                "FiducialVisionSettingsConfigurationWizard.GeneralPanel.GeneralizeSettingsButton.text"));
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
                    "FiducialVisionSettingsConfigurationWizard.GeneralPanel.GeneralizeSettingsButton.GeneralizeFor.text"
            ) + " " +settingsHolder.getShortName());
            btnGeneralizeSettings.setToolTipText("<html>Generalize these Fiducial Vision Settings for all the "
                    + subjects
                    + " with the "+ settingsHolder.getClass().getSimpleName()+" "+settingsHolder.getShortName()+".<br/>"
                    + "This will unassign any special Fiducial Vision Settings on "+subjects+" and delete those<br/>"
                    + "Fiducial Vision Settings that are no longer used elsewhere.</html>");
        }

        JButton resetButton = new JButton(Translations.getString(
                "FiducialVisionSettingsConfigurationWizard.GeneralPanel.ResetButton.text"));
        resetButton.addActionListener(e -> {
            UiUtils.messageBoxOnException(() -> {
                int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                        "This will reset the fiducial vision settings with to the default settings. Are you sure??", null,
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (result == JOptionPane.YES_OPTION) {
                    ReferenceFiducialLocator fiducialVision = ReferenceFiducialLocator.getDefault();
                    if (fiducialVision.getFiducialVisionSettings() == visionSettings) {
                        // Already the default. Set stock.
                        FiducialVisionSettings stockVisionSettings = (FiducialVisionSettings) Configuration.get()
                                .getVisionSettings(AbstractVisionSettings.STOCK_FIDUCIAL_ID);
                        visionSettings.setValues(stockVisionSettings);
                    }
                    else {
                        visionSettings.setValues(fiducialVision.getFiducialVisionSettings());
                    }
                }
            });
        });
        panel.add(resetButton, "12, 6");

        JLabel lblEnabled = new JLabel(Translations.getString(
                "FiducialVisionSettingsConfigurationWizard.GeneralPanel.EnabledLabel.text"));
        panel.add(lblEnabled, "2, 8");

        enabledCheckbox = new JCheckBox("");
        panel.add(enabledCheckbox, "4, 8");

        pipelinePanel = new PipelinePanel() {

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
        panelAlign.setBorder(new TitledBorder(new LineBorder(new Color(184, 207, 229)),
                Translations.getString(
                "FiducialVisionSettingsConfigurationWizard.TestFiducialLocatorPanel.Border.title"),
                TitledBorder.LEADING, TitledBorder.TOP, null, new Color(51, 51, 51)));
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
                ColumnSpec.decode("default:grow"),},
                new RowSpec[] {
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,}));

        JButton btnTestFiducialLocator = new JButton(Translations.getString(
                "FiducialVisionSettingsConfigurationWizard.TestFiducialLocatorPanel.TestFiducialLocatorButton.text"
        ));
        panelAlign.add(btnTestFiducialLocator, "4, 2");
        btnTestFiducialLocator.addActionListener((e) -> {
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

        addWrappedBinding(visionSettings, "name", name, "text");
        bind(UpdateStrategy.READ, visionSettings, "usedFiducialVisionIn", usedIn, "text", 
                new AbstractVisionSettings.ListConverter(true, settingsHolder));
        addWrappedBinding(visionSettings, "enabled", enabledCheckbox, "selected");

        bind(UpdateStrategy.READ_WRITE, visionSettings, "pipeline", pipelinePanel, "pipeline");
        addWrappedBinding(visionSettings, "pipelineParameterAssignments", pipelinePanel, "pipelineParameterAssignments");
    }

    private void createPanel() {
        panel = new JPanel();
        panel.setBorder(new TitledBorder(null, Translations.getString(
                "FiducialVisionSettingsConfigurationWizard.GeneralPanel.Border.title"),
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
        fiducialLocator.preparePipeline(pipeline, pipelineParameterAssignments, fiducialLocator.getVisionCamera(), settingsHolder);

        if (edit) {
            pipelinePanel.openPipelineEditor(Translations.getString("PipelineEditor.FiducialVisionPipeline.title"),
                    pipeline);
        }
    }

    private void testFiducialLocation() throws Exception {
        Camera camera = fiducialLocator.getVisionCamera();
        Location location = fiducialLocator.getFiducialLocation(camera.getLocation(), settingsHolder);
        camera.moveTo(location);
    }

    @Override
    public String getWizardName() {
        return Translations.getString("FiducialVisionSettingsConfigurationWizard.wizardName");
    }

}
