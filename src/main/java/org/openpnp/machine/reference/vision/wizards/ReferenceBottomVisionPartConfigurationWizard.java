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
import org.openpnp.machine.reference.vision.ReferenceBottomVision;
import org.openpnp.machine.reference.vision.ReferenceBottomVision.PartSettings;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.UiUtils;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.ui.CvPipelineEditor;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class ReferenceBottomVisionPartConfigurationWizard extends AbstractConfigurationWizard {
    private final ReferenceBottomVision bottomVision;
    private final Part part;
    private final PartSettings partSettings;

    private JCheckBox enabledCheckbox;
    private JCheckBox chckbxCenterAfterTest;

    public ReferenceBottomVisionPartConfigurationWizard(ReferenceBottomVision bottomVision,
            Part part) {
        this.bottomVision = bottomVision;
        this.part = part;
        this.partSettings = bottomVision.getPartSettings(part);

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "General", TitledBorder.LEADING, TitledBorder.TOP,
                null, null));
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("right:default"),
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

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

        JLabel lblTest = new JLabel("Test");
        panel.add(lblTest, "2, 4");
        panel.add(btnTestAlighment, "4, 4");

        chckbxCenterAfterTest = new JCheckBox("Center After Test");
        chckbxCenterAfterTest.setSelected(true);
        panel.add(chckbxCenterAfterTest, "6, 4");

        JLabel lblPipeline = new JLabel("Pipeline");
        panel.add(lblPipeline, "2, 6");

        JButton editPipelineButton = new JButton("Edit");
        editPipelineButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.messageBoxOnException(() -> {
                    editPipeline();
                });
            }
        });
        panel.add(editPipelineButton, "4, 6");

        JButton btnLoadDefault = new JButton("Reset to Default");
        btnLoadDefault.addActionListener((e) -> {
            int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                    "This will replace the current part pipeline with the default pipeline. Are you sure?",
                    null, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (result == JOptionPane.YES_OPTION) {
                UiUtils.messageBoxOnException(() -> {
                    partSettings.setPipeline(bottomVision.getPipeline().clone());
                    editPipeline();
                });
            }
        });
        panel.add(btnLoadDefault, "6, 6");
    }

    private void testAlignment() throws Exception {
        Nozzle nozzle = MainFrame.machineControlsPanel.getSelectedNozzle();

        // perform the alignment
        Location offsets = bottomVision.findOffsets(part, nozzle);

        if (!chckbxCenterAfterTest.isSelected()) {
            return;
        }

        // position the part over camera center
        Location cameraLocation = VisionUtils.getBottomVisionCamera().getLocation();

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
        // instead of
        // the nozzle.
        location = location.subtract(offsets);

        nozzle.moveTo(location);
    }

    private void editPipeline() throws Exception {
        CvPipeline pipeline = partSettings.getPipeline();
        pipeline.setCamera(VisionUtils.getBottomVisionCamera());
        CvPipelineEditor editor = new CvPipelineEditor(pipeline);
        JDialog dialog = new JDialog(MainFrame.mainFrame, "Bottom Vision Pipeline");
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(editor);
        dialog.setSize(1024, 768);
        dialog.setVisible(true);
    }

    @Override
    public void createBindings() {
        addWrappedBinding(partSettings, "enabled", enabledCheckbox, "selected");
    }
}
