package org.openpnp.machine.reference.vision.wizards;

import java.awt.Component;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.Translations;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.PipelineControls;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.reference.vision.AbstractPartAlignment;
import org.openpnp.machine.reference.vision.ReferenceBottomVision;
import org.openpnp.model.AbstractVisionSettings;
import org.openpnp.model.BottomVisionSettings;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Package;
import org.openpnp.model.Part;
import org.openpnp.model.PartSettingsHolder;
import org.openpnp.model.Placement;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.spi.PartAlignment;
import org.openpnp.util.UiUtils;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.layout.Sizes;

@SuppressWarnings("serial")
public class BottomVisionSettingsConfigurationWizard extends AbstractConfigurationWizard {
    private final ReferenceBottomVision bottomVision;
    private final BottomVisionSettings visionSettings;
    private final PartSettingsHolder settingsHolder;


    private JPanel panel;
    private JCheckBox enabledCheckbox;
    private JComboBox comboBoxPreRotate;
    private JComboBox comboBoxCheckPartSizeMethod;
    private JTextField textPartSizeTolerance;
    private JComboBox comboBoxMaxRotation;
    private JTextField tfBottomVisionOffsetX;
    private JTextField tfBottomVisionOffsetY;
    private JTextField testAlignmentAngle;

    private JCheckBox chckbxCenterAfterTest;

    private JLabel usedIn;  
    private JTextField name;
    private JLabel lblName;

    private JButton btnSpecializeSetting;
    private JButton btnGeneralizeSettings;
    private PipelineControls pipelinePanel;
    private JPanel panelDetectOffset;

    public BottomVisionSettingsConfigurationWizard(BottomVisionSettings visionSettings, 
            PartSettingsHolder settingsHolder) {
        this.visionSettings = visionSettings;
        this.settingsHolder = settingsHolder;
        this.bottomVision = (ReferenceBottomVision) AbstractPartAlignment.getPartAlignment(settingsHolder, true);
        createUi();
    }

    private void createUi() {
        createPanel();

        lblName = new JLabel(Translations.getString("BottomVisionSettingsConfigurationWizard.NameLabel.text")); //$NON-NLS-1$
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
                "BottomVisionSettingsConfigurationWizard.AssignedToLabel.text")); //$NON-NLS-1$
        panel.add(lblAssignedTo, "2, 4");

        JLabel lblSettings = new JLabel(Translations.getString(
                "BottomVisionSettingsConfigurationWizard.ManageSettingsLabel.text")); //$NON-NLS-1$
        panel.add(lblSettings, "2, 6");

        btnSpecializeSetting = new JButton();
        btnSpecializeSetting.setText(" ");
        if (settingsHolder != null && bottomVision.getParentHolder(settingsHolder) != null) {
            btnSpecializeSetting.setText(Translations.getString(
                    "BottomVisionSettingsConfigurationWizard.SpecializeSettingsButton.SpecializeForText") //$NON-NLS-1$
                    + " " +settingsHolder.getShortName()); //$NON-NLS-1$
            btnSpecializeSetting.setToolTipText(Translations.getString(
                    "BottomVisionSettingsConfigurationWizard.SpecializeSettingsButton.toolTipText") + " " //$NON-NLS-1$ //$NON-NLS-2$
                    + settingsHolder.getClass().getSimpleName()+" "+settingsHolder.getShortName());
        }
        else if (settingsHolder != null) {
            btnSpecializeSetting.setText(Translations.getString(
                    "BottomVisionSettingsConfigurationWizard.SpecializeSettingsButton.OptimizeText")); //$NON-NLS-1$
            btnSpecializeSetting.setToolTipText("<html>Optimize the Bottom Vision Settings and their assignments:<br/>"
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
                    "BottomVisionSettingsConfigurationWizard.SpecializeSettingsButton.SpecializeText")); //$NON-NLS-1$
            btnSpecializeSetting.setToolTipText("");
        }

        btnSpecializeSetting.addActionListener(e -> {
            applyAction.actionPerformed(null);
            UiUtils.messageBoxOnException(() -> {
                if (settingsHolder != null && bottomVision.getParentHolder(settingsHolder) != null) {
                    if (visionSettings.getUsedBottomVisionIn().size() == 1 
                            && visionSettings.getUsedBottomVisionIn().get(0) == settingsHolder) {
                        throw new Exception("Vision Settings already specialized for "+settingsHolder.getShortName()+".");
                    }
                    BottomVisionSettings newSettings = new BottomVisionSettings();
                    newSettings.setValues(visionSettings);
                    newSettings.setName(settingsHolder.getShortName());
                    settingsHolder.setBottomVisionSettings(newSettings);
                    Configuration.get().addVisionSettings(newSettings);
                }
                else {
                    ReferenceBottomVision.getDefault().optimizeVisionSettings(Configuration.get());
                    Configuration.get().fireVisionSettingsChanged();
                }
            });
        });
        panel.add(btnSpecializeSetting, "4, 6, 3, 1");

        final String subjects = settingsHolder instanceof Package ? "Parts" : "Parts and Packages";
        btnGeneralizeSettings = new JButton(Translations.getString(
                "BottomVisionSettingsConfigurationWizard.GeneralizeButton.text")); //$NON-NLS-1$
        btnGeneralizeSettings.addActionListener((e) -> {
            UiUtils.messageBoxOnException(() -> {
                List<PartSettingsHolder> list = settingsHolder.getSpecializedBottomVisionIn();
                if (list.size() == 0) {
                    throw new Exception("There are no specializations on "+subjects+" with the "+settingsHolder.getClass().getSimpleName()+" "+settingsHolder.getShortName()+".");
                }
                int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                        "This will remove the specialized vision settings in:\n\n"+
                                new AbstractVisionSettings.ListConverter(false).convertForward(list)+"\n\n"+
                                "Are you sure?", null,
                                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (result == JOptionPane.YES_OPTION) {
                    UiUtils.messageBoxOnException(() -> {
                        applyAction.actionPerformed(null);
                        settingsHolder.generalizeBottomVisionSettings();
                    });
                }
            });
        });
        panel.add(btnGeneralizeSettings, "8, 6, 3, 1");
        if (settingsHolder == null || settingsHolder instanceof Part) {
            btnGeneralizeSettings.setEnabled(false);
        }
        else {
            btnGeneralizeSettings.setText(Translations.getString(
                    "BottomVisionSettingsConfigurationWizard.GeneralizeButton.GeneralizeFor.text") //$NON-NLS-1$
                    + settingsHolder.getShortName());
            btnGeneralizeSettings.setToolTipText("<html>Generalize these Bottom Vision Settings for all the "
                    + subjects
                    + " with the "+ settingsHolder.getClass().getSimpleName()+" "+settingsHolder.getShortName()+".<br/>"
                    + "This will unassign any special Bottom Vision Settings on "+subjects+" and delete those<br/>"
                    + "Bottom Vision Settings that are no longer used elsewhere.</html>");
        }

        JButton resetButton = new JButton(Translations.getString(
                "BottomVisionSettingsConfigurationWizard.ResetButton.text")); //$NON-NLS-1$
        resetButton.addActionListener(e -> {
            UiUtils.messageBoxOnException(() -> {
                int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                        "This will reset the bottom vision settings with to the default settings. Are you sure??", null,
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (result == JOptionPane.YES_OPTION) {
                    ReferenceBottomVision bottomVision = ReferenceBottomVision.getDefault();
                    if (bottomVision.getBottomVisionSettings() == visionSettings) {
                        // Already the default. Set stock.
                        BottomVisionSettings stockVisionSettings = (BottomVisionSettings) Configuration.get()
                                .getVisionSettings(AbstractVisionSettings.STOCK_BOTTOM_ID);
                        visionSettings.setValues(stockVisionSettings);
                    }
                    else {
                        visionSettings.setValues(bottomVision.getBottomVisionSettings());
                    }
                }
            });
        });
        panel.add(resetButton, "12, 6");

        JLabel lblEnabled = new JLabel(Translations.getString(
                "BottomVisionSettingsConfigurationWizard.EnabledLabel.text")); //$NON-NLS-1$
        panel.add(lblEnabled, "2, 8");

        enabledCheckbox = new JCheckBox("");
        panel.add(enabledCheckbox, "4, 8");

        JLabel lblPrerotate = new JLabel(Translations.getString(
                "BottomVisionSettingsConfigurationWizard.PreRotateLabel.text")); //$NON-NLS-1$
        panel.add(lblPrerotate, "2, 10, right, default");

        comboBoxPreRotate = new JComboBox(ReferenceBottomVision.PreRotateUsage.values());
        panel.add(comboBoxPreRotate, "4, 10");

        JLabel lblMaxRotation = new JLabel(Translations.getString(
                "BottomVisionSettingsConfigurationWizard.RotationLabel.text")); //$NON-NLS-1$
        panel.add(lblMaxRotation, "6, 10, right, default");

        comboBoxMaxRotation = new JComboBox(ReferenceBottomVision.MaxRotation.values());
        comboBoxMaxRotation.setToolTipText(Translations.getString(
                "BottomVisionSettingsConfigurationWizard.ComboMaxRotation.toolTipText")); //$NON-NLS-1$
        panel.add(comboBoxMaxRotation, "8, 10, fill, default");

        JLabel lblPartCheckType = new JLabel(Translations.getString(
                "BottomVisionSettingsConfigurationWizard.PartSizeCheckLabel.text")); //$NON-NLS-1$
        panel.add(lblPartCheckType, "2, 12");

        comboBoxCheckPartSizeMethod = new JComboBox(ReferenceBottomVision.PartSizeCheckMethod.values());
        panel.add(comboBoxCheckPartSizeMethod, "4, 12, fill, default");

        JLabel lblPartSizeTolerance = new JLabel(Translations.getString(
                "BottomVisionSettingsConfigurationWizard.PartSizeToleranceLabel.text")); //$NON-NLS-1$
        panel.add(lblPartSizeTolerance, "6, 12, right, default");

        textPartSizeTolerance = new JTextField();
        panel.add(textPartSizeTolerance, "8, 12, fill, default");

        pipelinePanel = new PipelineControls() {

            @Override
            public void configurePipeline(CvPipeline pipeline, Map<String, Object> pipelineParameterAssignments, boolean edit) throws Exception {
                UiUtils.messageBoxOnException(() -> {
                    if (edit) {
                        // Accept changes before edit.
                        applyAction.actionPerformed(null);
                    }
                    pipelineConfiguration(pipeline,pipelineParameterAssignments, edit);
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
                        ReferenceBottomVision bottomVision = ReferenceBottomVision.getDefault();
                        if (bottomVision.getBottomVisionSettings() == visionSettings) {
                            // Already the default. Set stock.
                            pipelinePanel.setPipeline(ReferenceBottomVision.createStockPipeline("Default"));
                        }
                        else {
                            pipelinePanel.setPipeline(bottomVision.getBottomVisionSettings().getPipeline().clone());
                        }
                    });
                }
            }
        };
        pipelinePanel.setResetable(true);
        pipelinePanel.setEditable(true);
        panel.add(pipelinePanel, "1, 14, 14, 1, fill, fill");

        JPanel panelAlign = new JPanel();
        contentPanel.add(panelAlign);
        panelAlign.setBorder(new TitledBorder(null, Translations.getString(
                "BottomVisionSettingsConfigurationWizard.PanelAlign.Border.title"), //$NON-NLS-1$
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        panelAlign.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("right:max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                new ColumnSpec(ColumnSpec.FILL, Sizes.bounded(Sizes.DEFAULT, Sizes.constant("50dlu", true), Sizes.constant("70dlu", true)), 1),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblTestPlacementAngle = new JLabel(Translations.getString(
                "BottomVisionSettingsConfigurationWizard.PanelAlign.TestPlacementAngleLabel.text")); //$NON-NLS-1$
        panelAlign.add(lblTestPlacementAngle, "2, 2");

        testAlignmentAngle = new JTextField();
        panelAlign.add(testAlignmentAngle, "4, 2");
        testAlignmentAngle.setColumns(10);

        JButton btnTestAlighment = new JButton(Translations.getString(
                "BottomVisionSettingsConfigurationWizard.PanelAlign.TestAlignmentButton.text")); //$NON-NLS-1$
        panelAlign.add(btnTestAlighment, "6, 2");

        chckbxCenterAfterTest = new JCheckBox(Translations.getString(
                "BottomVisionSettingsConfigurationWizard.PanelAlign.CenterAfterTestChkbox.text")); //$NON-NLS-1$
        panelAlign.add(chckbxCenterAfterTest, "8, 2");
        chckbxCenterAfterTest.setToolTipText(Translations.getString(
                "BottomVisionSettingsConfigurationWizard.PanelAlign.CenterAfterTestChkbox.toolTipText")); //$NON-NLS-1$
        chckbxCenterAfterTest.setSelected(true);
        btnTestAlighment.addActionListener((e) -> {
            UiUtils.submitUiMachineTask(() -> {
                applyAction .actionPerformed(null);
                testAlignment(chckbxCenterAfterTest.isSelected());
            });
        });

        panelDetectOffset = new JPanel();
        contentPanel.add(panelDetectOffset);
        panelDetectOffset.setBorder(new TitledBorder(null, Translations.getString(
                "BottomVisionSettingsConfigurationWizard.PanelDetectOffset.Border.title"), //$NON-NLS-1$
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        panelDetectOffset.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("right:max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),},
                new RowSpec[] {
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblX = new JLabel("X");
        panelDetectOffset.add(lblX, "4, 2, center, default");

        JLabel lblY = new JLabel("Y");
        panelDetectOffset.add(lblY, "6, 2, center, default");

        JLabel lblVisionCenterOffset = new JLabel(Translations.getString(
                "BottomVisionSettingsConfigurationWizard.PanelDetectOffset.VisionCenterOffsetLabel.text")); //$NON-NLS-1$
        panelDetectOffset.add(lblVisionCenterOffset, "2, 4");
        lblVisionCenterOffset.setToolTipText(Translations.getString(
                "BottomVisionSettingsConfigurationWizard.PanelDetectOffset.VisionCenterOffsetLabel.toolTipText")); //$NON-NLS-1$

        tfBottomVisionOffsetX = new JTextField();
        panelDetectOffset.add(tfBottomVisionOffsetX, "4, 4");
        tfBottomVisionOffsetX.setColumns(10);

        tfBottomVisionOffsetY = new JTextField();
        panelDetectOffset.add(tfBottomVisionOffsetY, "6, 4");
        tfBottomVisionOffsetY.setText("");
        tfBottomVisionOffsetY.setColumns(10);

        JButton btnAutoVisionCenterOffset = new JButton(Translations.getString(
                "BottomVisionSettingsConfigurationWizard.PanelDetectOffset.AutoVisionCenterOffsetButton.text")); //$NON-NLS-1$
        panelDetectOffset.add(btnAutoVisionCenterOffset, "8, 4");
        btnAutoVisionCenterOffset.setToolTipText(Translations.getString(
                "BottomVisionSettingsConfigurationWizard.PanelDetectOffset.AutoVisionCenterOffsetButton.toolTipText")); //$NON-NLS-1$
        btnAutoVisionCenterOffset.addActionListener((e) -> {
            UiUtils.submitUiMachineTask(() -> {
                applyAction.actionPerformed(null);
                determineVisionOffset();
            });
        });


    }

    @Override
    public void createBindings() {
        IntegerConverter intConverter = new IntegerConverter();
        DoubleConverter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        LengthConverter lengthConverter = new LengthConverter();

        lblName.setVisible(settingsHolder != null);
        name.setVisible(settingsHolder != null);
        if (visionSettings.isStockSetting()) {
            for (Component comp : panel.getComponents()) {
                if (comp != btnSpecializeSetting) { 
                    comp.setEnabled(false);
                }
            }
            for (Component comp : panelDetectOffset.getComponents()) {
                comp.setEnabled(false);
            }
        }

        addWrappedBinding(visionSettings, "name", name, "text");
        bind(UpdateStrategy.READ, visionSettings, "usedBottomVisionIn", usedIn, "text", 
                new AbstractVisionSettings.ListConverter(true, settingsHolder));
        addWrappedBinding(visionSettings, "enabled", enabledCheckbox, "selected");
        addWrappedBinding(visionSettings, "preRotateUsage", comboBoxPreRotate, "selectedItem");
        addWrappedBinding(visionSettings, "checkPartSizeMethod", comboBoxCheckPartSizeMethod, "selectedItem");
        addWrappedBinding(visionSettings, "checkSizeTolerancePercent", textPartSizeTolerance, "text", intConverter);
        addWrappedBinding(visionSettings, "maxRotation", comboBoxMaxRotation, "selectedItem");
        MutableLocationProxy bottomVisionOffsetProxy = new MutableLocationProxy();
        addWrappedBinding(visionSettings, "visionOffset", bottomVisionOffsetProxy, "location");
        bind(UpdateStrategy.READ_WRITE, bottomVisionOffsetProxy, "lengthX", tfBottomVisionOffsetX, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, bottomVisionOffsetProxy, "lengthY", tfBottomVisionOffsetY, "text", lengthConverter);

        addWrappedBinding(bottomVision, "testAlignmentAngle", testAlignmentAngle, "text", doubleConverter);

        bind(UpdateStrategy.READ_WRITE, visionSettings, "pipeline", pipelinePanel, "pipeline");
        addWrappedBinding(visionSettings, "pipelineParameterAssignments", pipelinePanel, "pipelineParameterAssignments");

        ComponentDecorators.decorateWithAutoSelect(textPartSizeTolerance);
        //ComponentDecorators.decorateWithAutoSelect(testAlignmentAngle);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(tfBottomVisionOffsetX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(tfBottomVisionOffsetY);
    }

    private void createPanel() {
        panel = new JPanel();
        panel.setBorder(new TitledBorder(null, Translations.getString(
                "BottomVisionSettingsConfigurationWizard.GeneralPanel.Border.title"), //$NON-NLS-1$
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
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                RowSpec.decode("default:grow"),}));
    }

    private void pipelineConfiguration(CvPipeline pipeline, Map<String, Object> pipelineParameterAssignments, boolean edit) 
            throws Exception {
        Camera camera = VisionUtils.getBottomVisionCamera();
        Nozzle nozzle = MainFrame.get().getMachineControls().getSelectedNozzle();
        // Nominal position of the part over camera center
        double angle = new DoubleConverter(Configuration.get().getLengthDisplayFormat())
                .convertReverse(testAlignmentAngle.getText());
        Part part = nozzle.getPart();
        Package pkg = null;
        if (part != null) {
            pkg = part.getPackage();
        }
        else if (settingsHolder instanceof Part) {
            part = (Part) settingsHolder;
            pkg = part.getPackage();
        }
        else if (settingsHolder instanceof Package) {
            pkg = (Package) settingsHolder;
        }
        else if (MainFrame.get().getPartsTab().getSelectedPart() != null) {
            part = MainFrame.get().getPartsTab().getSelectedPart();
            pkg = part.getPackage();
        }
        else if (MainFrame.get().getPackagesTab().getSelectedPackage() != null) {
            pkg = MainFrame.get().getPackagesTab().getSelectedPackage();
        }
        if (pkg == null) {
            throw new Exception("A package must be designated to configure the pipeline. "
                    + "Please pick a part with selected nozzle "+nozzle.getName()+". "
                    + "Alternatively, you can select a single part or package on the Parts or "
                    + "Packages tab for a \"dry-run\" with empty nozzle (like after a failed pick).");
        }
        NozzleTip nt = nozzle.getNozzleTip();
        if (nt == null) {
            throw new Exception("A nozzle tip must be loaded on selected nozzle "+nozzle.getName()+".");
        }
        if (! pkg.getCompatibleNozzleTips().contains(nt)) {
            throw new Exception("Nozzle tip "+nt.getName()+" loaded on selected nozzle "+nozzle.getName()
            +" is not compatible with package "+pkg.getId()+".");
        }
        Location location = bottomVision.getCameraLocationAtPartHeight(part, 
                camera,
                nozzle, angle);
        bottomVision.preparePipeline(pipeline, pipelineParameterAssignments, camera, pkg, nozzle, nt, 
                location, location, visionSettings);
        if (edit) {

            pipelinePanel.openPipelineEditor("Bottom Vision Pipeline", pipeline, 
                    "move nozzle "+nozzle.getName()+" to the camera alignment location before editing the pipeline", 
                    nozzle, location);
        }
    }

    private void testAlignment(boolean centerAfterTest) throws Exception {
        Nozzle nozzle = getNozzleWithPart();

        double angle = new DoubleConverter(Configuration.get().getLengthDisplayFormat())
                .convertReverse(testAlignmentAngle.getText());

        alignAndCenter(bottomVision, nozzle, angle, centerAfterTest);
    }

    public void alignAndCenter(ReferenceBottomVision bottomVision, Nozzle nozzle, double angle, boolean centerAfterTest)
            throws Exception {
        // perform the alignment
        Placement dummy = new Placement("Dummy");
        dummy.setLocation(new Location(LengthUnit.Millimeters, 0, 0, 0, angle));
        PartAlignment.PartAlignmentOffset alignmentOffset = VisionUtils.findPartAlignmentOffsets(bottomVision, nozzle.getPart(),
                null, dummy, nozzle);
        Location offsets = alignmentOffset.getLocation();

        if (!centerAfterTest) {
            return;
        }

        // Nominal position of the part over camera center
        Location cameraLocation = bottomVision.getCameraLocationAtPartHeight(nozzle.getPart(), VisionUtils.getBottomVisionCamera(),
                nozzle, angle);

        if (alignmentOffset.getPreRotated()) {
            Location centeredLocation = cameraLocation.subtractWithRotation(alignmentOffset.getLocation());
            nozzle.moveTo(centeredLocation);
            return;
        }

        // Rotate the point 0,0 using the bottom offsets as a center point by the angle
        // that is
        // the difference between the bottom vision angle and the calculated global
        // placement angle.
        Location location = new Location(LengthUnit.Millimeters).rotateXyCenterPoint(offsets,
                cameraLocation.getRotation() - offsets.getRotation());

        // Set the angle to the difference mentioned above, aligning the part to the
        // same angle as
        // the placement.
        location = location.derive(null, null, null, cameraLocation.getRotation() - offsets.getRotation());

        // Add the placement final location to move our local coordinate into global
        // space
        location = location.add(cameraLocation);

        // Subtract the bottom vision offsets to move the part to the final location,
        // instead of the nozzle.
        location = location.subtract(offsets);

        nozzle.moveTo(location);
    }

    private void determineVisionOffset() throws Exception {
        Nozzle nozzle = getNozzleWithPart();

        // Remember the location at which the user centered the part.
        Location center = nozzle.getLocation();

        // perform the alignment
        alignAndCenter(bottomVision, nozzle, 0.0, true);

        Location visionOffset = center.subtract(nozzle.getLocation()).add(visionSettings.getVisionOffset());
        tfBottomVisionOffsetX.setText(Double.toString(visionOffset.getX()));
        tfBottomVisionOffsetY.setText(Double.toString(visionOffset.getY()));
    }

    public Nozzle getNozzleWithPart() throws Exception {
        Nozzle nozzle = MainFrame.get().getMachineControls().getSelectedNozzle();
        Part part = nozzle.getPart(); 
        if (part == null) {
            throw new Exception("Nozzle "+nozzle.getName()+" does not have a part loaded");
        }
        if (settingsHolder instanceof Part 
                && part != this.settingsHolder) {
            throw new Exception("Wrong part "+part.getId()+" on Nozzle "+nozzle.getName());
        }
        else if (settingsHolder instanceof Package 
                && part.getPackage() != this.settingsHolder) {
            throw new Exception("Wrong package "+part.getPackage().getId()+" on Nozzle "+nozzle.getName());
        }
        if (bottomVision == null) {
            throw new Exception("Bottom Vision for vision settings "+visionSettings.getName()+" not enabled.");
        }
        if (bottomVision.getInheritedVisionSettings(part) != visionSettings) {
            throw new Exception("Present Bottom Vision Settings are not effective for part "+part.getId()+" on Nozzle "+nozzle.getName()+". "
                    +"Assign to Package of Part.");
        }
        return nozzle;
    }

    @Override
    public String getWizardName() {
        return Translations.getString("BottomVisionSettingsConfigurationWizard.wizardName"); //$NON-NLS-1$
    }

}
