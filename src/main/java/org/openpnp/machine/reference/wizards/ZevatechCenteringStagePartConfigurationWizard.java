package org.openpnp.machine.reference.wizards;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.machine.reference.ZevatechCenteringStage;
import org.openpnp.machine.reference.ZevatechCenteringStage.PartSettings;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PartAlignment;
import org.openpnp.util.UiUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ZevatechCenteringStagePartConfigurationWizard extends AbstractConfigurationWizard {
    private final ZevatechCenteringStage centeringStage;
    private final Part part;
    private final PartSettings partSettings;

    private JCheckBox enabledCheckbox;

    public ZevatechCenteringStagePartConfigurationWizard(ZevatechCenteringStage centeringStage,
                                                         Part part) {
        this.centeringStage = centeringStage;
        this.part = part;
        this.partSettings = centeringStage.getPartSettings(part);

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


    }

    private void testAlignment() throws Exception {
        Nozzle nozzle = MainFrame.get().getMachineControls().getSelectedNozzle();

        // perform the alignment


        PartAlignment.PartAlignmentOffset alignmentOffset = centeringStage.findOffsets(part, null, null, nozzle);
        Location offsets = alignmentOffset.getLocation();



        // position the part over camera center
        Location location = centeringStage.getLocation();

        /*
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

        */

        nozzle.moveTo(location);
    }


    @Override
    public void createBindings() {
        addWrappedBinding(partSettings, "enabled", enabledCheckbox, "selected");
    }
}
