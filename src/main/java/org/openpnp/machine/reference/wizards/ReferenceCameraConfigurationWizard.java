package org.openpnp.machine.reference.wizards;

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
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

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ReferenceCameraConfigurationWizard extends
        AbstractConfigurationWizard {
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
    private JLabel lblCameraMatrix;
    private JTextField camMatrix1Tf;
    private JTextField camMatrix2Tf;
    private JTextField camMatrix3Tf;
    private JTextField camMatrix4Tf;
    private JTextField camMatrix5Tf;
    private JTextField camMatrix6Tf;
    private JTextField camMatrix7Tf;
    private JTextField camMatrix8Tf;
    private JTextField camMatrix9Tf;
    private JLabel lblDistortionCoefficients;
    private JTextField distCoeff1Tf;
    private JTextField distCoeff2Tf;
    private JTextField distCoeff3Tf;
    private JTextField distCoeff4Tf;
    private JTextField distCoeff5Tf;
    private JLabel lblApplyCalibration;
    private JCheckBox calibrationEnabledChk;
    private JButton btnStartLensCalibration;
    
    
    public ReferenceCameraConfigurationWizard(ReferenceCamera referenceCamera) {
        this.referenceCamera = referenceCamera;
        
                panelOffsets = new JPanel();
                contentPanel.add(panelOffsets);
                panelOffsets.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "Offsets", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
                panelOffsets.setLayout(new FormLayout(new ColumnSpec[] {
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
                        FormSpecs.DEFAULT_ROWSPEC,}));
                
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
                                                        panelSafeZ.setBorder(new TitledBorder(null, "Safe Z", TitledBorder.LEADING, TitledBorder.TOP, null, null));
                                                        contentPanel.add(panelSafeZ);
                                                        panelSafeZ.setLayout(new FormLayout(new ColumnSpec[] {
                                                        		FormSpecs.RELATED_GAP_COLSPEC,
                                                        		FormSpecs.DEFAULT_COLSPEC,
                                                        		FormSpecs.RELATED_GAP_COLSPEC,
                                                        		FormSpecs.DEFAULT_COLSPEC,},
                                                        	new RowSpec[] {
                                                        		FormSpecs.RELATED_GAP_ROWSPEC,
                                                        		FormSpecs.DEFAULT_ROWSPEC,}));
                                                        
                                                        JLabel lblSafeZ = new JLabel("Safe Z");
                                                        panelSafeZ.add(lblSafeZ, "2, 2, right, default");
                                                        
                                                        textFieldSafeZ = new JTextField();
                                                        panelSafeZ.add(textFieldSafeZ, "4, 2, fill, default");
                                                        textFieldSafeZ.setColumns(10);
                                                        
        
        panelGeneral = new JPanel();
        panelGeneral.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "Transformation", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
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
        
        panelLocation = new JPanel();
        panelLocation.setBorder(new TitledBorder(null, "Location", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelLocation);
        panelLocation.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                RowSpec.decode("default:grow"),}));
        
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
        panelLensCalibration.setBorder(new TitledBorder(null, "Lens Calibration", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelLensCalibration);
        panelLensCalibration.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
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
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        btnStartLensCalibration = new JButton(startCalibration);
        panelLensCalibration.add(btnStartLensCalibration, "2, 2, 11, 1");
        
        lblApplyCalibration = new JLabel("Apply Calibration?");
        panelLensCalibration.add(lblApplyCalibration, "2, 4, right, default");
        
        calibrationEnabledChk = new JCheckBox("");
        panelLensCalibration.add(calibrationEnabledChk, "4, 4");
        
        lblCameraMatrix = new JLabel("Camera Matrix");
        panelLensCalibration.add(lblCameraMatrix, "2, 6, right, default");
        
        camMatrix1Tf = new JTextField();
        panelLensCalibration.add(camMatrix1Tf, "4, 6, fill, default");
        camMatrix1Tf.setColumns(10);
        
        camMatrix2Tf = new JTextField();
        panelLensCalibration.add(camMatrix2Tf, "6, 6, fill, default");
        camMatrix2Tf.setColumns(10);
        
        camMatrix3Tf = new JTextField();
        panelLensCalibration.add(camMatrix3Tf, "8, 6, fill, default");
        camMatrix3Tf.setColumns(10);
        
        camMatrix4Tf = new JTextField();
        panelLensCalibration.add(camMatrix4Tf, "4, 8, fill, default");
        camMatrix4Tf.setColumns(10);
        
        camMatrix5Tf = new JTextField();
        panelLensCalibration.add(camMatrix5Tf, "6, 8, fill, default");
        camMatrix5Tf.setColumns(10);
        
        camMatrix6Tf = new JTextField();
        panelLensCalibration.add(camMatrix6Tf, "8, 8, fill, default");
        camMatrix6Tf.setColumns(10);
        
        camMatrix7Tf = new JTextField();
        panelLensCalibration.add(camMatrix7Tf, "4, 10, fill, default");
        camMatrix7Tf.setColumns(10);
        
        camMatrix8Tf = new JTextField();
        panelLensCalibration.add(camMatrix8Tf, "6, 10, fill, default");
        camMatrix8Tf.setColumns(10);
        
        camMatrix9Tf = new JTextField();
        panelLensCalibration.add(camMatrix9Tf, "8, 10, fill, default");
        camMatrix9Tf.setColumns(10);
        
        lblDistortionCoefficients = new JLabel("Distortion Coefficients");
        panelLensCalibration.add(lblDistortionCoefficients, "2, 14, right, default");
        
        distCoeff1Tf = new JTextField();
        panelLensCalibration.add(distCoeff1Tf, "4, 14, fill, default");
        distCoeff1Tf.setColumns(10);
        
        distCoeff2Tf = new JTextField();
        panelLensCalibration.add(distCoeff2Tf, "6, 14, fill, default");
        distCoeff2Tf.setColumns(10);
        
        distCoeff3Tf = new JTextField();
        panelLensCalibration.add(distCoeff3Tf, "8, 14, fill, default");
        distCoeff3Tf.setColumns(10);
        
        distCoeff4Tf = new JTextField();
        panelLensCalibration.add(distCoeff4Tf, "10, 14, fill, default");
        distCoeff4Tf.setColumns(10);
        
        distCoeff5Tf = new JTextField();
        panelLensCalibration.add(distCoeff5Tf, "12, 14, fill, default");
        distCoeff5Tf.setColumns(10);
        
        try {
            // Causes WindowBuilder to fail, so just throw away the error.
            if (referenceCamera.getHead() == null) {
                locationButtonsPanel = new LocationButtonsPanel(textFieldLocationX, textFieldLocationY, textFieldLocationZ, textFieldLocationRotation);
                panelLocation.add(locationButtonsPanel, "10, 4, fill, fill");
                panelOffsets.setVisible(false);
            }
            else {
                panelLocation.setVisible(false);
            }
        }
        catch (Exception e) {
            
        }
    }
    
    @Override
    public void createBindings() {
    	IntegerConverter intConverter = new IntegerConverter();
        DoubleConverter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        LengthConverter lengthConverter = new LengthConverter();
        DoubleConverter doubleConverterCalibration = new DoubleConverter("%s");

        if (referenceCamera.getHead() == null) {
            // fixed camera
            MutableLocationProxy headOffsets = new MutableLocationProxy();
            bind(UpdateStrategy.READ_WRITE, referenceCamera, "headOffsets", headOffsets, "location");
            addWrappedBinding(headOffsets, "lengthX", textFieldLocationX, "text", lengthConverter);
            addWrappedBinding(headOffsets, "lengthY", textFieldLocationY, "text", lengthConverter);
            addWrappedBinding(headOffsets, "lengthZ", textFieldLocationZ, "text", lengthConverter);
            addWrappedBinding(headOffsets, "rotation", textFieldLocationRotation, "text", doubleConverter);
        }
        else {
            // moving camera
            MutableLocationProxy headOffsets = new MutableLocationProxy();
            bind(UpdateStrategy.READ_WRITE, referenceCamera, "headOffsets", headOffsets, "location");
            addWrappedBinding(headOffsets, "lengthX", textFieldOffX, "text", lengthConverter);
            addWrappedBinding(headOffsets, "lengthY", textFieldOffY, "text", lengthConverter);
            addWrappedBinding(headOffsets, "lengthZ", textFieldOffZ, "text", lengthConverter);
        }

        addWrappedBinding(referenceCamera, "rotation", textFieldRotation, "text", doubleConverter);
        addWrappedBinding(referenceCamera, "offsetX", textFieldOffsetX, "text", intConverter);
        addWrappedBinding(referenceCamera, "offsetY", textFieldOffsetY, "text", intConverter);
        addWrappedBinding(referenceCamera, "flipX", chckbxFlipX, "selected");
        addWrappedBinding(referenceCamera, "flipY", checkBoxFlipY, "selected");
        addWrappedBinding(referenceCamera, "safeZ", textFieldSafeZ, "text", lengthConverter);
        
        addWrappedBinding(referenceCamera.getCalibration(), "cameraMatrix0", camMatrix1Tf, "text", doubleConverterCalibration);
        addWrappedBinding(referenceCamera.getCalibration(), "cameraMatrix1", camMatrix2Tf, "text", doubleConverterCalibration);
        addWrappedBinding(referenceCamera.getCalibration(), "cameraMatrix2", camMatrix3Tf, "text", doubleConverterCalibration);
        addWrappedBinding(referenceCamera.getCalibration(), "cameraMatrix3", camMatrix4Tf, "text", doubleConverterCalibration);
        addWrappedBinding(referenceCamera.getCalibration(), "cameraMatrix4", camMatrix5Tf, "text", doubleConverterCalibration);
        addWrappedBinding(referenceCamera.getCalibration(), "cameraMatrix5", camMatrix6Tf, "text", doubleConverterCalibration);
        addWrappedBinding(referenceCamera.getCalibration(), "cameraMatrix6", camMatrix7Tf, "text", doubleConverterCalibration);
        addWrappedBinding(referenceCamera.getCalibration(), "cameraMatrix7", camMatrix8Tf, "text", doubleConverterCalibration);
        addWrappedBinding(referenceCamera.getCalibration(), "cameraMatrix8", camMatrix9Tf, "text", doubleConverterCalibration);
        addWrappedBinding(referenceCamera.getCalibration(), "distCoeff0", distCoeff1Tf, "text", doubleConverterCalibration);
        addWrappedBinding(referenceCamera.getCalibration(), "distCoeff1", distCoeff2Tf, "text", doubleConverterCalibration);
        addWrappedBinding(referenceCamera.getCalibration(), "distCoeff2", distCoeff3Tf, "text", doubleConverterCalibration);
        addWrappedBinding(referenceCamera.getCalibration(), "distCoeff3", distCoeff4Tf, "text", doubleConverterCalibration);
        addWrappedBinding(referenceCamera.getCalibration(), "distCoeff4", distCoeff5Tf, "text", doubleConverterCalibration);
        bind(UpdateStrategy.READ_WRITE, referenceCamera.getCalibration(), "enabled", calibrationEnabledChk, "selected");
//        addWrappedBinding(referenceCamera.getCalibration(), "enabled", calibrationEnabledChk, "selected");
        
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
        
        ComponentDecorators.decorateWithAutoSelect(camMatrix1Tf);
        ComponentDecorators.decorateWithAutoSelect(camMatrix2Tf);
        ComponentDecorators.decorateWithAutoSelect(camMatrix3Tf);
        ComponentDecorators.decorateWithAutoSelect(camMatrix4Tf);
        ComponentDecorators.decorateWithAutoSelect(camMatrix5Tf);
        ComponentDecorators.decorateWithAutoSelect(camMatrix6Tf);
        ComponentDecorators.decorateWithAutoSelect(camMatrix7Tf);
        ComponentDecorators.decorateWithAutoSelect(camMatrix8Tf);
        ComponentDecorators.decorateWithAutoSelect(camMatrix9Tf);
        ComponentDecorators.decorateWithAutoSelect(distCoeff1Tf);
        ComponentDecorators.decorateWithAutoSelect(distCoeff2Tf);
        ComponentDecorators.decorateWithAutoSelect(distCoeff3Tf);
        ComponentDecorators.decorateWithAutoSelect(distCoeff4Tf);
        ComponentDecorators.decorateWithAutoSelect(distCoeff5Tf);
    }
    
    private Action startCalibration = new AbstractAction("Start Lens Calibration") {
        @Override
        public void actionPerformed(ActionEvent e) {
            MainFrame.cameraPanel.setSelectedCamera(referenceCamera);
            
            btnStartLensCalibration.setAction(cancelCalibration);
            
            CameraView cameraView = MainFrame.cameraPanel.getCameraView(referenceCamera);
            String message = 
                    "Go to https://github.com/openpnp/openpnp/wiki/Camera-Lens-Calibration for detailed instructions.\n" +
                    "When you have your calibration card ready, hold it in front of the camera so that the entire card is visible.\n" +
                    "Each time the screen flashes an image is captured. After the flash you should move the card to a new orientation.";
            cameraView.setText(message);
            cameraView.flash();
            
            referenceCamera.beginCalibration((progressCurrent, progressMax, finished) -> {
                if (finished) {
                    cameraView.setText(null);
                    btnStartLensCalibration.setAction(startCalibration);
                }
                else {
                    cameraView.setText(String.format("Captured %d of %d.\nMove the card to a new position and angle each time the screen flashes.", progressCurrent, progressMax));
                }
                cameraView.flash();
            });
        }
    };
    
    private Action cancelCalibration = new AbstractAction("Cancel Lens Calibration") {
        @Override
        public void actionPerformed(ActionEvent e) {
            btnStartLensCalibration.setAction(startCalibration);
            
            referenceCamera.cancelCalibration();
            
            CameraView cameraView = MainFrame.cameraPanel.getCameraView(referenceCamera);
            cameraView.setText(null);
            cameraView.flash();
        }
    };
}
