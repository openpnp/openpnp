package org.openpnp.machine.reference.vision.wizards;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.reference.vision.ReferenceBottomVision;
import org.openpnp.model.*;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PartAlignment;
import org.openpnp.util.UiUtils;
import org.openpnp.util.VisionUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class ReferenceBottomVisionPartConfigurationWizard extends AbstractConfigurationWizard {
    private final ReferenceBottomVision bottomVision;
    private final Part part;
    
    private JCheckBox chckbxCenterAfterTest;
    private JTextField tfBottomVisionOffsetX;
    private JTextField tfBottomVisionOffsetY;
    private JTextField testAlignmentAngle;

    public ReferenceBottomVisionPartConfigurationWizard(ReferenceBottomVision bottomVision, Part part) {
        this.bottomVision = bottomVision;
        this.part = part;

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "General", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("right:max(70dlu;default)"),
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
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblTestAngle = new JLabel("Test Placement Angle");
        panel.add(lblTestAngle, "2, 6, right, default");

        JButton btnTestAlighment = new JButton("Test Alignment");
        btnTestAlighment.addActionListener((e) -> {
            UiUtils.submitUiMachineTask(() -> {
                testAlignment(chckbxCenterAfterTest.isSelected());
            });
        });

        testAlignmentAngle = new JTextField();
        testAlignmentAngle.setText("0.000");
        panel.add(testAlignmentAngle, "4, 6, right, default");
        testAlignmentAngle.setColumns(10);
        panel.add(btnTestAlighment, "6, 6");

        chckbxCenterAfterTest = new JCheckBox("Center After Test");
        chckbxCenterAfterTest.setToolTipText("Center and rotate the part after the test.");
        chckbxCenterAfterTest.setSelected(true);
        panel.add(chckbxCenterAfterTest, "8, 6");
        
        JLabel lblBottomVisionX = new JLabel("X");
        panel.add(lblBottomVisionX, "4, 16");
        
        JLabel lblBottomVisionY = new JLabel("Y");
        panel.add(lblBottomVisionY, "6, 16");
        
        JLabel lblVisionCenterOffset = new JLabel("Vision center offset");
        lblVisionCenterOffset.setToolTipText("Offset relative to the pick location/center of the part to the center of the rectangle detected by the bottom vision");
        panel.add(lblVisionCenterOffset, "2, 18");
        
        tfBottomVisionOffsetX = new JTextField();
        panel.add(tfBottomVisionOffsetX, "4, 18, fill, default");
        tfBottomVisionOffsetX.setColumns(10);
        
        tfBottomVisionOffsetY = new JTextField();
        panel.add(tfBottomVisionOffsetY, "6, 18, fill, default");
        tfBottomVisionOffsetY.setColumns(10);
        
        JButton btnAutoVisionCenterOffset = new JButton("Detect");
        btnAutoVisionCenterOffset.setToolTipText("Center part over bottom vision camera. Button will run bottom vision and calculates the offset.");
        panel.add(btnAutoVisionCenterOffset, "8, 18");
        btnAutoVisionCenterOffset.addActionListener((e) -> {
            UiUtils.submitUiMachineTask(() -> {
                determineVisionOffset();
            });
        });

    }

    private void testAlignment(boolean centerAfterTest) throws Exception {
        if (!bottomVision.isEnabled()) {
            MessageBoxes.errorBox(getTopLevelAncestor(), "Error", "Bottom vision is not enabled in this part Bottom Vision Setup.");
            return;
        }

        Nozzle nozzle = MainFrame.get().getMachineControls().getSelectedNozzle();
        double angle = new DoubleConverter(Configuration.get().getLengthDisplayFormat())
                .convertReverse(testAlignmentAngle.getText());

        // perform the alignment
        PartAlignment.PartAlignmentOffset alignmentOffset = VisionUtils.findPartAlignmentOffsets(bottomVision, part,
                null, new Location(LengthUnit.Millimeters, 0, 0, 0, angle), nozzle);
        Location offsets = alignmentOffset.getLocation();

        if (!centerAfterTest) {
            return;
        }

        // Nominal position of the part over camera center
        Location cameraLocation = bottomVision.getCameraLocationAtPartHeight(part, VisionUtils.getBottomVisionCamera(),
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
        if (!bottomVision.isEnabled()) {
            MessageBoxes.errorBox(getTopLevelAncestor(), "Error", "Bottom vision is not enabled in this part Bottom Vision Setup.");
            return;
        }

        Nozzle nozzle = MainFrame.get().getMachineControls().getSelectedNozzle();
        
        Location center = nozzle.getLocation();

        // perform the alignment
        testAlignment(true);
        
        Location visionOffset = center.subtract(nozzle.getLocation()).add(part.getVisionOffset());
        tfBottomVisionOffsetX.setText(Double.toString(visionOffset.getX()));
        tfBottomVisionOffsetY.setText(Double.toString(visionOffset.getY()));
    }

    @Override
    public String getWizardName() {
        return "ReferenceBottomVision";
    }

    @Override
    public void createBindings() {
        LengthConverter lengthConverter = new LengthConverter();
        MutableLocationProxy bottomVisionOffsetProxy = new MutableLocationProxy();
        addWrappedBinding(part, "visionOffset", bottomVisionOffsetProxy, "location");
        bind(UpdateStrategy.READ_WRITE, bottomVisionOffsetProxy, "lengthX", tfBottomVisionOffsetX, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, bottomVisionOffsetProxy, "lengthY", tfBottomVisionOffsetY, "text", lengthConverter);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(tfBottomVisionOffsetX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(tfBottomVisionOffsetY);
    }
}
