package org.openpnp.machine.reference.vision.wizards;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.ComponentDecorators;
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
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PartAlignment;
import org.openpnp.util.UiUtils;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.ui.CvPipelineEditor;
import org.openpnp.vision.pipeline.ui.CvPipelineEditorDialog;

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

    public BottomVisionSettingsConfigurationWizard(BottomVisionSettings visionSettings, 
            PartSettingsHolder settingsHolder) {
        this.visionSettings = visionSettings;
        this.settingsHolder = settingsHolder;
        this.bottomVision = (ReferenceBottomVision) AbstractPartAlignment.getPartAlignment(settingsHolder, true);
        createUi();
    }

    private void createUi() {
        createPanel();

        lblName = new JLabel("Name");
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

        JLabel lblAssignedTo = new JLabel("Assigned to");
        panel.add(lblAssignedTo, "2, 4");

        JLabel lblSettings = new JLabel("Manage Settings");
        panel.add(lblSettings, "2, 6");

        btnSpecializeSetting = new JButton();
        btnSpecializeSetting.setText(" ");
        if (settingsHolder != null && settingsHolder.getParentHolder() != null) {
            btnSpecializeSetting.setText("Specialize for "+settingsHolder.getShortName());
            btnSpecializeSetting.setToolTipText("Create a copy of these Bottom Vision Settings and assign to "
                    +settingsHolder.getClass().getSimpleName()+" "+settingsHolder.getShortName());
        }
        else if (settingsHolder != null) {
            btnSpecializeSetting.setText("Optimize");
            btnSpecializeSetting.setToolTipText("<html>Optimize the Bottom Vision Settings and their assignments:<br/>"
                    + "<ul>"
                    + "<li>Consolidate duplicate settings.</li>"
                    + "<li>Remove unused settings.</li>"
                    + "<li>Assign the most common Part settings as the Package settings, inherit on such Parts, and override on the exceptions (if any).</li>"
                    + "<li>Remove specific assignments where the same settings would be inherited anyway.</li>"
                    + "</ul>"
                    + "</html>");
        }
        else {
            btnSpecializeSetting.setEnabled(false);
            btnSpecializeSetting.setText("Specialize");
            btnSpecializeSetting.setToolTipText("");
        }

        btnSpecializeSetting.addActionListener(e -> {
            applyAction.actionPerformed(null);
            UiUtils.messageBoxOnException(() -> {
                if (settingsHolder != null && settingsHolder.getParentHolder() != null) {
                    if (visionSettings.getUsedIn().size() == 1 
                            && visionSettings.getUsedIn().get(0) == settingsHolder) {
                        throw new Exception("Vision Settings already specialized for "+settingsHolder.getShortName()+".");
                    }
                    BottomVisionSettings newSettings = new BottomVisionSettings();
                    newSettings.setValues(visionSettings);
                    newSettings.setName(settingsHolder.getShortName());
                    settingsHolder.setVisionSettings(newSettings);
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
        btnGeneralizeSettings = new JButton("Generalize");
        btnGeneralizeSettings.addActionListener((e) -> {
            UiUtils.messageBoxOnException(() -> {
                List<PartSettingsHolder> list = settingsHolder.getSpecializedIn();
                if (list.size() == 0) {
                    throw new Exception("There are no specializations on "+subjects+" with the "+settingsHolder.getClass().getSimpleName()+" "+settingsHolder.getShortName()+".");
                }
                int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                        "This will remove the specialized vision settings in:\n\n"+
                                new AbstractVisionSettings.ListConverter(false).convertForward(list)+"\n\n"+
                                "Are you sure?", null,
                                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (result == JOptionPane.YES_OPTION) {
                    UiUtils.messageBoxOnException(settingsHolder::resetSpecializedVisionSettings);
                }
            });
        });
        panel.add(btnGeneralizeSettings, "8, 6, 3, 1");
        if (settingsHolder == null || settingsHolder instanceof Part) {
            btnGeneralizeSettings.setEnabled(false);
        }
        else {
            btnGeneralizeSettings.setText("Generalize for "+settingsHolder.getShortName());
            btnGeneralizeSettings.setToolTipText("<html>Generalize these Bottom Vision Settings for all the "
                    + subjects
                    + " with the "+ settingsHolder.getClass().getSimpleName()+" "+settingsHolder.getShortName()+".<br/>"
                    + "This will unassign any special Bottom Vision Settings on "+subjects+" and delete those<br/>"
                            + "Bottom Vision Settings that are no longer used elsewhere.</html>");
        }

        JButton resetButton = new JButton("Reset to Default");
        resetButton.addActionListener(e -> {
            UiUtils.messageBoxOnException(() -> {
                int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                        "This will reset the bottom vision settings with to the default settings. Are you sure??", null,
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (result == JOptionPane.YES_OPTION) {
                    ReferenceBottomVision bottomVision = ReferenceBottomVision.getDefault();
                    visionSettings.setValues(bottomVision.getVisionSettings());
                }
            });
        });
        panel.add(resetButton, "12, 6");

        JLabel lblEnabled = new JLabel("Enabled?");
        panel.add(lblEnabled, "2, 8");

        enabledCheckbox = new JCheckBox("");
        panel.add(enabledCheckbox, "4, 8");

        JLabel lblPrerotate = new JLabel("Pre-rotate");
        panel.add(lblPrerotate, "2, 10, right, default");

        comboBoxPreRotate = new JComboBox(ReferenceBottomVision.PreRotateUsage.values());
        panel.add(comboBoxPreRotate, "4, 10");

        JLabel lblMaxRotation = new JLabel("Rotation");
        panel.add(lblMaxRotation, "2, 12, right, default");

        comboBoxMaxRotation = new JComboBox(ReferenceBottomVision.MaxRotation.values());
        comboBoxMaxRotation.setToolTipText(
                "Adjust for all parts, where only some minor offset is expected. Full for parts, where bottom vision detects pin 1");
        panel.add(comboBoxMaxRotation, "4, 12, fill, default");

        JLabel lblPartCheckType = new JLabel("Part size check");
        panel.add(lblPartCheckType, "2, 14");

        comboBoxCheckPartSizeMethod = new JComboBox(ReferenceBottomVision.PartSizeCheckMethod.values());
        panel.add(comboBoxCheckPartSizeMethod, "4, 14, fill, default");

        JLabel lblPartSizeTolerance = new JLabel("Size tolerance (%)");
        panel.add(lblPartSizeTolerance, "2, 16");

        textPartSizeTolerance = new JTextField();
        panel.add(textPartSizeTolerance, "4, 16, fill, default");

        JLabel lblX = new JLabel("X");
        panel.add(lblX, "4, 18, center, default");

        JLabel lblY = new JLabel("Y");
        panel.add(lblY, "6, 18, center, default");

        JLabel lblVisionCenterOffset = new JLabel("Vision Center Offset");
        lblVisionCenterOffset.setToolTipText("Offset relative to the pick location/center of the part to the center of the rectangle detected by the bottom vision");
        panel.add(lblVisionCenterOffset, "2, 20, right, default");

        tfBottomVisionOffsetX = new JTextField();
        panel.add(tfBottomVisionOffsetX, "4, 20, fill, default");
        tfBottomVisionOffsetX.setColumns(10);

        tfBottomVisionOffsetY = new JTextField();
        tfBottomVisionOffsetY.setText("");
        panel.add(tfBottomVisionOffsetY, "6, 20, fill, default");
        tfBottomVisionOffsetY.setColumns(10);

        JButton btnAutoVisionCenterOffset = new JButton("Detect");
        btnAutoVisionCenterOffset.setToolTipText("Center part over bottom vision camera. Button will run bottom vision and calculates the offset.");
        panel.add(btnAutoVisionCenterOffset, "8, 20");
        btnAutoVisionCenterOffset.addActionListener((e) -> {
            UiUtils.submitUiMachineTask(() -> {
                determineVisionOffset();
            });
        });

        JLabel lblPipeline = new JLabel("Pipeline");
        panel.add(lblPipeline, "2, 22, right, default");

        JButton editPipelineButton = new JButton("Edit");
        editPipelineButton.addActionListener(e -> UiUtils.messageBoxOnException(this::editPipeline));
        panel.add(editPipelineButton, "4, 22");

        JButton resetPipelineButton = new JButton("Reset Pipeline to Default");
        resetPipelineButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                    "This will replace the Pipeline with the built-in default. Are you sure??", null,
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (result == JOptionPane.YES_OPTION) {
                UiUtils.messageBoxOnException(() -> {
                    ReferenceBottomVision bottomVision = ReferenceBottomVision.getDefault();
                    visionSettings.setCvPipeline(bottomVision.getVisionSettings().getCvPipeline().clone());
                    editPipeline();
                });
            }
        });
        panel.add(resetPipelineButton, "6, 22, 3, 1");

        JPanel panelAlign = new JPanel();
        contentPanel.add(panelAlign);
        panelAlign.setBorder(new TitledBorder(null, "Test Alignment", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        panelAlign.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("right:max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("min(70dlu;default)"),
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

        JLabel lblTestPlacementAngle = new JLabel("Placement Angle");
        panelAlign.add(lblTestPlacementAngle, "2, 2");

        testAlignmentAngle = new JTextField();
        panelAlign.add(testAlignmentAngle, "4, 2");
        testAlignmentAngle.setColumns(10);

        JButton btnTestAlighment = new JButton("Test Alignment");
        panelAlign.add(btnTestAlighment, "6, 2");

        chckbxCenterAfterTest = new JCheckBox("Center After Test");
        panelAlign.add(chckbxCenterAfterTest, "8, 2");
        chckbxCenterAfterTest.setToolTipText("Center and rotate the part after the test.");
        chckbxCenterAfterTest.setSelected(true);
        btnTestAlighment.addActionListener((e) -> {
            UiUtils.submitUiMachineTask(() -> {
                testAlignment(chckbxCenterAfterTest.isSelected());
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
        }

        addWrappedBinding(visionSettings, "name", name, "text");
        bind(UpdateStrategy.READ, visionSettings, "usedIn", usedIn, "text", 
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

        ComponentDecorators.decorateWithAutoSelect(textPartSizeTolerance);
        //ComponentDecorators.decorateWithAutoSelect(testAlignmentAngle);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(tfBottomVisionOffsetX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(tfBottomVisionOffsetY);
    }

    private void createPanel() {
        panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "General", TitledBorder.LEADING, TitledBorder.TOP, null, null));
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
    }

    private void editPipeline() throws Exception {
        CvPipeline pipeline = visionSettings.getCvPipeline();
        pipeline.setProperty("camera", VisionUtils.getBottomVisionCamera());
        pipeline.setProperty("nozzle", MainFrame.get().getMachineControls().getSelectedNozzle());

        CvPipelineEditor editor = new CvPipelineEditor(pipeline);
        JDialog dialog = new CvPipelineEditorDialog(MainFrame.get(), "Bottom Vision Pipeline", editor);
        dialog.setVisible(true);
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
        PartAlignment.PartAlignmentOffset alignmentOffset = VisionUtils.findPartAlignmentOffsets(bottomVision, nozzle.getPart(),
                null, new Location(LengthUnit.Millimeters, 0, 0, 0, angle), nozzle);
        Location offsets = alignmentOffset.getLocation();

        if (!centerAfterTest) {
            return;
        }

        // Nominal position of the part over camera center
        Location cameraLocation = bottomVision.getCameraLocationAtPartHeight(nozzle.getPart(), VisionUtils.getBottomVisionCamera(),
                nozzle, angle);

        if (alignmentOffset.getPreRotated()) {
            // See https://github.com/openpnp/openpnp/pull/590 for explanations of the magic
            // value below.
            if (Math.abs(alignmentOffset.getLocation().convertToUnits(LengthUnit.Millimeters).getLinearDistanceTo(0.,
                    0.)) > 19.999) {
                throw new Exception("Offset too big");
            }
            nozzle.moveTo(cameraLocation.subtractWithRotation(alignmentOffset.getLocation()));
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
        return "Bottom Vision Settings";
    }

    private class SwingAction extends AbstractAction {
        public SwingAction() {
            putValue(NAME, "SwingAction");
            putValue(SHORT_DESCRIPTION, "Some short description");
        }
        public void actionPerformed(ActionEvent e) {
        }
    }
}
