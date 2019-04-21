package org.openpnp.machine.reference.vision.wizards;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.machine.reference.ReferenceNozzleTip;
import org.openpnp.machine.reference.vision.ReferenceBottomVision;
import org.openpnp.machine.reference.vision.ReferenceBottomVision.PartSettings;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PartAlignment;
import org.openpnp.util.UiUtils;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.ui.CvPipelineEditor;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import javax.swing.JComboBox;

public class ReferenceBottomVisionPartConfigurationWizard extends AbstractConfigurationWizard {
    private final ReferenceBottomVision bottomVision;
    private final Part part;
    private final PartSettings partSettings;

    private JCheckBox enabledCheckbox;
    private JCheckBox chckbxCenterAfterTest;
    private JComboBox comboBoxPreRotate;

    public ReferenceBottomVisionPartConfigurationWizard(ReferenceBottomVision bottomVision,
            Part part) {
        this.bottomVision = bottomVision;
        this.part = part;
        this.partSettings = bottomVision.getPartSettings(part);

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "General", TitledBorder.LEADING, TitledBorder.TOP,
                null, null));
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("right:default"),
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

        JLabel lblEnabled = new JLabel("Enabled?");
        panel.add(lblEnabled, "2, 2");

        enabledCheckbox = new JCheckBox("");
        panel.add(enabledCheckbox, "4, 2");

        JButton btnTestAlighment = new JButton("Test Alignment");
        btnTestAlighment.addActionListener((e) -> {
            UiUtils.submitUiMachineTask(() -> {
                testAlignment();
            });
        });
        
        JLabel lblPrerotate = new JLabel("Pre-rotate");
        panel.add(lblPrerotate, "2, 4, right, default");
        
        comboBoxPreRotate = new JComboBox(ReferenceBottomVision.PreRotateUsage.values());
        panel.add(comboBoxPreRotate, "4, 4");

        JLabel lblTest = new JLabel("Test");
        panel.add(lblTest, "2, 6");
        panel.add(btnTestAlighment, "4, 6");

        chckbxCenterAfterTest = new JCheckBox("Center After Test");
        chckbxCenterAfterTest.setSelected(true);
        panel.add(chckbxCenterAfterTest, "6, 6");

        JLabel lblPipeline = new JLabel("Pipeline");
        panel.add(lblPipeline, "2, 8");

        JButton editPipelineButton = new JButton("Edit");
        editPipelineButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.messageBoxOnException(() -> {
                    editPipeline();
                });
            }
        });
        panel.add(editPipelineButton, "4, 8");

        JButton btnLoadDefault = new JButton("Reset to Default");
        btnLoadDefault.addActionListener((e) -> {
            int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                    "This will replace the current part pipeline with the default pipeline. Are you sure?",
                    null, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (result == JOptionPane.YES_OPTION) {
                UiUtils.messageBoxOnException(() -> {
                    partSettings.setPipeline(bottomVision.getPipeline()
                                                         .clone());
                    editPipeline();
                });
            }
        });
        panel.add(btnLoadDefault, "6, 8");
    }

    private void testAlignment() throws Exception {
        if (!bottomVision.isEnabled()) {
            MessageBoxes.errorBox(getTopLevelAncestor(), "Error",
                    "Bottom vision is not enabled in Machine Setup.");
            return;
        }

        if (!enabledCheckbox.isSelected()) {
            MessageBoxes.errorBox(getTopLevelAncestor(), "Error",
                    "Bottom vision is not enabled for this part.");
            return;
        }

        Nozzle nozzle = MainFrame.get()
                                 .getMachineControls()
                                 .getSelectedNozzle();

        // perform the alignment
        PartAlignment.PartAlignmentOffset alignmentOffset = VisionUtils.findPartAlignmentOffsets(
                bottomVision, part, null, new Location(LengthUnit.Millimeters), nozzle);
        Location offsets = alignmentOffset.getLocation();

        if (!chckbxCenterAfterTest.isSelected()) {
            return;
        }

        // position the part over camera center
        Location cameraLocation = bottomVision.getCameraLocationAtPartHeight(part, 
                VisionUtils.getBottomVisionCamera(), 0.);

        if (alignmentOffset.getPreRotated()) {
            // See https://github.com/openpnp/openpnp/pull/590 for explanations of the magic
            // value below.
            if (Math.abs(alignmentOffset.getLocation()
                                        .convertToUnits(LengthUnit.Millimeters)
                                        .getLinearDistanceTo(0., 0.)) > 19.999) {
                throw new Exception("Offset too big");
            }
            nozzle.moveTo(cameraLocation
                                .subtractWithRotation(alignmentOffset.getLocation()));
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
        location = location.derive(null, null, null,
                cameraLocation.getRotation() - offsets.getRotation());

        // Add the placement final location to move our local coordinate into global
        // space
        location = location.add(cameraLocation);

        // Subtract the bottom vision offsets to move the part to the final location,
        // instead of the nozzle.
        location = location.subtract(offsets);

        nozzle.moveTo(location);
    }

    private void editPipeline() throws Exception {
        CvPipeline pipeline = partSettings.getPipeline();
        pipeline.setProperty("camera", VisionUtils.getBottomVisionCamera());
		pipeline.setProperty("nozzle", MainFrame.get().getMachineControls().getSelectedNozzle());

        CvPipelineEditor editor = new CvPipelineEditor(pipeline);
        JDialog dialog = new JDialog(MainFrame.get(), "Bottom Vision Pipeline");
        dialog.getContentPane()
              .setLayout(new BorderLayout());
        dialog.getContentPane()
              .add(editor);
        dialog.setSize(1024, 768);
        dialog.setVisible(true);
    }

    @Override
    public String getWizardName() {
        return "ReferenceBottomVision";
    }

    @Override
    public void createBindings() {
        addWrappedBinding(partSettings, "enabled", enabledCheckbox, "selected");
        addWrappedBinding(partSettings, "preRotateUsage", comboBoxPreRotate, "selectedItem");
    }
}
