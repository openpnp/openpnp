package org.openpnp.machine.reference.wizards;

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.model.Configuration;
import org.simpleframework.xml.Attribute;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ReferenceCameraConfigurationWizard extends AbstractConfigurationWizard {
    private final ReferenceCamera referenceCamera;

    private JTextField textFieldOffX;
    private JTextField textFieldOffY;
    private JTextField textFieldOffZ;
    private JPanel panelOffsets;
    private JPanel panelGeneral;
    private JLabel lblRotation;
    private JTextField textFieldRotation;
    private JPanel panelLocation;
    private JLabel lblX;
    private JLabel lblY;
    private JLabel lblZ;
    private JLabel lblRotation_1;
    private JTextField textFieldLocationX;
    private JTextField textFieldLocationY;
    private JTextField textFieldLocationZ;
    private JTextField textFieldLocationRotation;
    private LocationButtonsPanel locationButtonsPanel;
    private JCheckBox chckbxFlipX;
    private JLabel lblFlipX;
    private JLabel lblFlipY;
    private JCheckBox checkBoxFlipY;
    private JTextField textFieldSafeZ;
    private JLabel lblOffsetX;
    private JLabel lblOffsetY;
    private JTextField textFieldOffsetX;
    private JTextField textFieldOffsetY;
    private JPanel panelLensCalibration;
    private JLabel lblApplyCalibration;
    private JCheckBox calibrationEnabledChk;


    public ReferenceCameraConfigurationWizard(ReferenceCamera referenceCamera) {
        this.referenceCamera = referenceCamera;

        panelOffsets = new JPanel();
        contentPanel.add(panelOffsets);
        panelOffsets.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
                "Offsets", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        panelOffsets.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel olblX = new JLabel("X");
        panelOffsets.add(olblX, "2, 2");

        JLabel olblY = new JLabel("Y");
        panelOffsets.add(olblY, "4, 2");

        JLabel olblZ = new JLabel("Z");
        panelOffsets.add(olblZ, "6, 2");


        textFieldOffX = new JTextField();
        panelOffsets.add(textFieldOffX, "2, 4");
        textFieldOffX.setColumns(8);

        textFieldOffY = new JTextField();
        panelOffsets.add(textFieldOffY, "4, 4");
        textFieldOffY.setColumns(8);

        textFieldOffZ = new JTextField();
        panelOffsets.add(textFieldOffZ, "6, 4");
        textFieldOffZ.setColumns(8);

        JPanel panelSafeZ = new JPanel();
        panelSafeZ.setBorder(new TitledBorder(null, "Safe Z", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        contentPanel.add(panelSafeZ);
        panelSafeZ.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblSafeZ = new JLabel("Safe Z");
        panelSafeZ.add(lblSafeZ, "2, 2, right, default");

        textFieldSafeZ = new JTextField();
        panelSafeZ.add(textFieldSafeZ, "4, 2, fill, default");
        textFieldSafeZ.setColumns(10);


        panelGeneral = new JPanel();
        panelGeneral.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
                "Transformation", TitledBorder.LEADING, TitledBorder.TOP, null,
                new Color(0, 0, 0)));
        contentPanel.add(panelGeneral);
        panelGeneral.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
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
                FormSpecs.DEFAULT_ROWSPEC,}));

        lblRotation = new JLabel("Rotation");
        panelGeneral.add(lblRotation, "2, 2, right, default");

        textFieldRotation = new JTextField();
        panelGeneral.add(textFieldRotation, "4, 2");
        textFieldRotation.setColumns(10);

        lblOffsetX = new JLabel("Offset X");
        panelGeneral.add(lblOffsetX, "2, 4, right, default");

        textFieldOffsetX = new JTextField();
        panelGeneral.add(textFieldOffsetX, "4, 4");
        textFieldOffsetX.setColumns(10);

        lblOffsetY = new JLabel("Offset Y");
        panelGeneral.add(lblOffsetY, "2, 6, right, default");

        textFieldOffsetY = new JTextField();
        panelGeneral.add(textFieldOffsetY, "4, 6");
        textFieldOffsetY.setColumns(10);

        lblFlipX = new JLabel("Flip Vertical");
        panelGeneral.add(lblFlipX, "2, 8, right, default");

        chckbxFlipX = new JCheckBox("");
        panelGeneral.add(chckbxFlipX, "4, 8");

        lblFlipY = new JLabel("Flip Horizontal");
        panelGeneral.add(lblFlipY, "2, 10, right, default");

        checkBoxFlipY = new JCheckBox("");
        panelGeneral.add(checkBoxFlipY, "4, 10");
        
        lblCropX = new JLabel("Crop Width");
        panelGeneral.add(lblCropX, "2, 12, right, default");
        
        cropWidthTextField = new JTextField();
        panelGeneral.add(cropWidthTextField, "4, 12");
        cropWidthTextField.setColumns(10);
        
        lblNewLabel = new JLabel("(Use 0 for no cropping)");
        panelGeneral.add(lblNewLabel, "5, 12");
        
        lblCropHeight = new JLabel("Crop Height");
        panelGeneral.add(lblCropHeight, "2, 14, right, default");
        
        cropHeightTextField = new JTextField();
        panelGeneral.add(cropHeightTextField, "4, 14");
        cropHeightTextField.setColumns(10);
        
        lblNewLabel_1 = new JLabel("(Use 0 for no cropping)");
        panelGeneral.add(lblNewLabel_1, "5, 14");
        
        lblScaleWidth = new JLabel("Scale Width");
        panelGeneral.add(lblScaleWidth, "2, 16, right, default");
        
        scaleWidthTf = new JTextField();
        panelGeneral.add(scaleWidthTf, "4, 16, fill, default");
        scaleWidthTf.setColumns(10);
        
        lbluseFor = new JLabel("(Use 0 for no scaling)");
        panelGeneral.add(lbluseFor, "5, 16");
        
        lblScaleHeight = new JLabel("Scale Height");
        panelGeneral.add(lblScaleHeight, "2, 18, right, default");
        
        scaleHeightTf = new JTextField();
        panelGeneral.add(scaleHeightTf, "4, 18, fill, default");
        scaleHeightTf.setColumns(10);
        
        label = new JLabel("(Use 0 for no scaling)");
        panelGeneral.add(label, "5, 18");
        
        lblDeinterlace = new JLabel("De-Interlace");
        panelGeneral.add(lblDeinterlace, "2, 20");
        
        deinterlaceChk = new JCheckBox("");
        panelGeneral.add(deinterlaceChk, "4, 20");
        
        lblremovesInterlacingFrom = new JLabel("(Removes interlacing from stacked frames)");
        panelGeneral.add(lblremovesInterlacingFrom, "5, 20");

        panelLocation = new JPanel();
        panelLocation.setBorder(new TitledBorder(null, "Location", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        contentPanel.add(panelLocation);
        panelLocation.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, RowSpec.decode("default:grow"),}));

        lblX = new JLabel("X");
        panelLocation.add(lblX, "2, 2");

        lblY = new JLabel("Y");
        panelLocation.add(lblY, "4, 2");

        lblZ = new JLabel("Z");
        panelLocation.add(lblZ, "6, 2");

        lblRotation_1 = new JLabel("Rotation");
        panelLocation.add(lblRotation_1, "8, 2");

        textFieldLocationX = new JTextField();
        panelLocation.add(textFieldLocationX, "2, 4, fill, default");
        textFieldLocationX.setColumns(8);

        textFieldLocationY = new JTextField();
        panelLocation.add(textFieldLocationY, "4, 4, fill, default");
        textFieldLocationY.setColumns(8);

        textFieldLocationZ = new JTextField();
        panelLocation.add(textFieldLocationZ, "6, 4, fill, default");
        textFieldLocationZ.setColumns(8);

        textFieldLocationRotation = new JTextField();
        panelLocation.add(textFieldLocationRotation, "8, 4, fill, default");
        textFieldLocationRotation.setColumns(8);

        panelLensCalibration = new JPanel();
        panelLensCalibration.setBorder(new TitledBorder(null, "Lens Calibration",
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelLensCalibration);
        panelLensCalibration.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        startLensCalibrationBtn = new JButton(startCalibration);
        panelLensCalibration.add(startLensCalibrationBtn, "2, 2, 3, 1");

        lblApplyCalibration = new JLabel("Apply Calibration?");
        panelLensCalibration.add(lblApplyCalibration, "2, 4, right, default");

        calibrationEnabledChk = new JCheckBox("");
        panelLensCalibration.add(calibrationEnabledChk, "4, 4");

        try {
            // Causes WindowBuilder to fail, so just throw away the error.
            if (referenceCamera.getHead() == null) {
                // Fixed camera, add the location fields and buttons and turn off offsets.
                locationButtonsPanel = new LocationButtonsPanel(textFieldLocationX,
                        textFieldLocationY, textFieldLocationZ, textFieldLocationRotation);
                panelLocation.add(locationButtonsPanel, "10, 4, fill, fill");
                panelOffsets.setVisible(false);    
                panelSafeZ.setVisible(false);
            }
            else {
                // Moving camera, hide location and show only offsets.
                panelLocation.setVisible(false);
            }
        }
        catch (Exception e) {

        }
    }

    @Override
    public void createBindings() {
        IntegerConverter intConverter = new IntegerConverter();
        DoubleConverter doubleConverter =
                new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        LengthConverter lengthConverter = new LengthConverter();

        if (referenceCamera.getHead() == null) {
            // fixed camera
            MutableLocationProxy headOffsets = new MutableLocationProxy();
            bind(UpdateStrategy.READ_WRITE, referenceCamera, "headOffsets", headOffsets,
                    "location");
            addWrappedBinding(headOffsets, "lengthX", textFieldLocationX, "text", lengthConverter);
            addWrappedBinding(headOffsets, "lengthY", textFieldLocationY, "text", lengthConverter);
            addWrappedBinding(headOffsets, "lengthZ", textFieldLocationZ, "text", lengthConverter);
            addWrappedBinding(headOffsets, "rotation", textFieldLocationRotation, "text",
                    doubleConverter);
        }
        else {
            // moving camera
            MutableLocationProxy headOffsets = new MutableLocationProxy();
            bind(UpdateStrategy.READ_WRITE, referenceCamera, "headOffsets", headOffsets,
                    "location");
            addWrappedBinding(headOffsets, "lengthX", textFieldOffX, "text", lengthConverter);
            addWrappedBinding(headOffsets, "lengthY", textFieldOffY, "text", lengthConverter);
            addWrappedBinding(headOffsets, "lengthZ", textFieldOffZ, "text", lengthConverter);
            addWrappedBinding(referenceCamera, "safeZ", textFieldSafeZ, "text", lengthConverter);
        }

        addWrappedBinding(referenceCamera, "rotation", textFieldRotation, "text", doubleConverter);
        addWrappedBinding(referenceCamera, "offsetX", textFieldOffsetX, "text", intConverter);
        addWrappedBinding(referenceCamera, "offsetY", textFieldOffsetY, "text", intConverter);
        addWrappedBinding(referenceCamera, "flipX", chckbxFlipX, "selected");
        addWrappedBinding(referenceCamera, "flipY", checkBoxFlipY, "selected");
        addWrappedBinding(referenceCamera, "cropWidth", cropWidthTextField, "text", intConverter);
        addWrappedBinding(referenceCamera, "cropHeight", cropHeightTextField, "text", intConverter);
        addWrappedBinding(referenceCamera, "scaleWidth", scaleWidthTf, "text", intConverter);
        addWrappedBinding(referenceCamera, "scaleHeight", scaleHeightTf, "text", intConverter);
        addWrappedBinding(referenceCamera, "deinterlace", deinterlaceChk, "selected");


        bind(UpdateStrategy.READ_WRITE, referenceCamera.getCalibration(), "enabled",
                calibrationEnabledChk, "selected");
        // addWrappedBinding(referenceCamera.getCalibration(), "enabled", calibrationEnabledChk,
        // "selected");

        ComponentDecorators.decorateWithAutoSelect(textFieldRotation);
        ComponentDecorators.decorateWithAutoSelect(textFieldOffsetX);
        ComponentDecorators.decorateWithAutoSelect(textFieldOffsetY);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldOffX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldOffY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldOffZ);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLocationX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLocationY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLocationZ);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLocationRotation);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldSafeZ);
        ComponentDecorators.decorateWithAutoSelect(cropWidthTextField);
        ComponentDecorators.decorateWithAutoSelect(cropHeightTextField);
        ComponentDecorators.decorateWithAutoSelect(scaleWidthTf);
        ComponentDecorators.decorateWithAutoSelect(scaleHeightTf);
    }

    private Action startCalibration = new AbstractAction("Start Lens Calibration") {
        @Override
        public void actionPerformed(ActionEvent e) {
            MainFrame.get().getCameraViews().setSelectedCamera(referenceCamera);

            startLensCalibrationBtn.setAction(cancelCalibration);

            CameraView cameraView = MainFrame.get().getCameraViews().getCameraView(referenceCamera);
            String message =
                    "Go to https://github.com/openpnp/openpnp/wiki/Camera-Lens-Calibration for detailed instructions.\n"
                            + "When you have your calibration card ready, hold it in front of the camera so that the entire card is visible.\n"
                            + "Each time the screen flashes an image is captured. After the flash you should move the card to a new orientation.";
            cameraView.setText(message);
            cameraView.flash();

            referenceCamera.startCalibration((progressCurrent, progressMax, finished) -> {
                if (finished) {
                    cameraView.setText(null);
                    startLensCalibrationBtn.setAction(startCalibration);
                }
                else {
                    cameraView.setText(String.format(
                            "Captured %d of %d.\nMove the card to a new position and angle each time the screen flashes.",
                            progressCurrent, progressMax));
                }
                cameraView.flash();
            });
        }
    };

    private Action cancelCalibration = new AbstractAction("Cancel Lens Calibration") {
        @Override
        public void actionPerformed(ActionEvent e) {
            startLensCalibrationBtn.setAction(startCalibration);

            referenceCamera.cancelCalibration();

            CameraView cameraView = MainFrame.get().getCameraViews().getCameraView(referenceCamera);
            cameraView.setText(null);
            cameraView.flash();
        }
    };
    private JButton startLensCalibrationBtn;
    private JLabel lblCropX;
    private JLabel lblCropHeight;
    private JTextField cropWidthTextField;
    private JTextField cropHeightTextField;
    private JLabel lblNewLabel;
    private JLabel lblNewLabel_1;
    private JLabel lblScaleWidth;
    private JLabel lblScaleHeight;
    private JTextField scaleWidthTf;
    private JTextField scaleHeightTf;
    private JLabel lbluseFor;
    private JLabel label;
    private JCheckBox deinterlaceChk;
    private JLabel lblDeinterlace;
    private JLabel lblremovesInterlacingFrom;
}
