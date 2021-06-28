package org.openpnp.machine.reference.wizards;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Size;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.processes.CalibrateCameraProcess;
import org.openpnp.gui.processes.EstimateObjectZCoordinateProcess;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.IdentifiableListCellRenderer;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.PartsComboBoxModel;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.ReferenceCamera.AdvancedCalibration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.RegionOfInterest;
import org.openpnp.spi.Camera.Looking;
import org.openpnp.util.UiUtils;
import org.openpnp.vision.LensCalibration.LensModel;
import org.pmw.tinylog.Logger;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import javax.swing.JCheckBox;
import javax.swing.JSlider;
import javax.swing.JTextField;

@SuppressWarnings("serial")
public class ReferenceCameraCalibrationWizard extends AbstractConfigurationWizard {
    private final ReferenceCamera referenceCamera;
    private JPanel panelCameraCalibration;
    private JButton startCameraCalibrationBtn;
    private JLabel lblCalibrationRig;
    private JComboBox<?> comboBoxPart;
    private AdvancedCalibration advancedCalibration;


    public ReferenceCameraCalibrationWizard(ReferenceCamera referenceCamera) {
        this.referenceCamera = referenceCamera;
        advancedCalibration = referenceCamera.getAdvancedCalibration();
        
        panelCameraCalibration = new JPanel();
        panelCameraCalibration.setBorder(new TitledBorder(null, "Camera Calibration",
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelCameraCalibration);
        panelCameraCalibration.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(87dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(39dlu;default):grow"),},
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
        
        chckbxAdvancedCalOverride = new JCheckBox("Override old style image transforms and distortion correction settings");
        chckbxAdvancedCalOverride.setToolTipText("Enable this to use advanced calibration.  Disable this to restore usage of old settings.");
        chckbxAdvancedCalOverride.addActionListener(overrideAction);
        panelCameraCalibration.add(chckbxAdvancedCalOverride, "2, 2, 5, 1");

        chckbxEnable = new JCheckBox("Use Calibration");
        chckbxEnable.setToolTipText("Enable this to use the new image transform and distortion correction settings.  Disable this and no calibration will be applied.");
        chckbxEnable.addActionListener(enableCalibration);
                
        lblCalibrationRig = new JLabel("Calibration Rig");
        panelCameraCalibration.add(lblCalibrationRig, "2, 4, right, default");

        comboBoxPart = new JComboBox();
        comboBoxPart.setModel(new PartsComboBoxModel());
        comboBoxPart.setRenderer(new IdentifiableListCellRenderer<Part>());
            panelCameraCalibration.add(comboBoxPart, "4, 4, left, default");
        
        lblNewLabel_1 = new JLabel("Default Working Plane Z");
        panelCameraCalibration.add(lblNewLabel_1, "2, 6, right, default");
        
        textFieldDefaultZ = new JTextField();
        panelCameraCalibration.add(textFieldDefaultZ, "4, 6, fill, default");
        textFieldDefaultZ.setColumns(10);
        panelCameraCalibration.add(chckbxEnable, "2, 8");
        if (referenceCamera.getLooking() == Looking.Down) {
            textFieldDefaultZ.setToolTipText("<html><p width=\"500\">"
                + "This is the assumed Z coordinate of objects viewed by the "
                + "camera if their true Z coordinate is unknown. Typically this "
                + "is set to the Z coordinate of the working surface of the "
                + "board(s) to be populated.</p></html>");
        }
        else {
            textFieldDefaultZ.setToolTipText("<html><p width=\"500\">"
                    + "This is the Z coordinate to which the bottom surface of "
                    + "parts carried by the nozzle will be lowered for visual "
                    + "alignment.</p></html>");
        }
        
        
        startCameraCalibrationBtn = new JButton(startCalibration);
        panelCameraCalibration.add(startCameraCalibrationBtn, "4, 8");
        
        chckbxUseSavedData = new JCheckBox("Used Saved Data");
        chckbxUseSavedData.setEnabled(referenceCamera.getAdvancedCalibration().getSavedTestPattern3dPointsList() != null);
        chckbxUseSavedData.setToolTipText("Set this to reprocess previously collected calibration data - only useful for code debugging");
        panelCameraCalibration.add(chckbxUseSavedData, "6, 8");
        
        lblNewLabel = new JLabel("Show Invalid Pixels");
        lblNewLabel.setToolTipText("<html><p width=\"500\">"
                + "A value of 0 forces only valid pixels to be displayed but some "
                + "valid pixels may be lost beyond the edge of the display. A value"
                + " of 100 forces all valid pixels to be displayed but the edges "
                + "of the display may show invalid (usually black) pixels. "
                + "</p></html>");
        panelCameraCalibration.add(lblNewLabel, "2, 10");
        
        sliderAlpha = new JSlider();
        sliderAlpha.setMajorTickSpacing(10);
        sliderAlpha.setMinorTickSpacing(5);
        sliderAlpha.setPaintTicks(true);
        sliderAlpha.setPaintLabels(true);
        sliderAlpha.addChangeListener(sliderAlphaChanged);
        panelCameraCalibration.add(sliderAlpha, "4, 10, 3, 1");
        
        lblNewLabel_2 = new JLabel("Z Axis Rotation Error [Deg]");
        panelCameraCalibration.add(lblNewLabel_2, "2, 14, right, default");
        
        textFieldZRotationError = new JTextField();
        textFieldZRotationError.setEditable(false);
        panelCameraCalibration.add(textFieldZRotationError, "4, 14, fill, default");
        textFieldZRotationError.setColumns(10);
        
        lblNewLabel_3 = new JLabel("Y Axis Rotation Error [Deg]");
        panelCameraCalibration.add(lblNewLabel_3, "2, 16, right, default");
        
        textFieldYRotationError = new JTextField();
        textFieldYRotationError.setEditable(false);
        panelCameraCalibration.add(textFieldYRotationError, "4, 16, fill, default");
        textFieldYRotationError.setColumns(10);
        
        lblNewLabel_4 = new JLabel("X Axis Rotation Error [Deg]");
        panelCameraCalibration.add(lblNewLabel_4, "2, 18, right, default");
        
        textFieldXRotationError = new JTextField();
        textFieldXRotationError.setEditable(false);
        panelCameraCalibration.add(textFieldXRotationError, "4, 18, fill, default");
        textFieldXRotationError.setColumns(10);
    }

    @Override
    public void createBindings() {
        LengthConverter lengthConverter = new LengthConverter();
        
        bind(UpdateStrategy.READ_WRITE, advancedCalibration, "overrideOldStyleSettings",
                chckbxAdvancedCalOverride, "selected");
        bind(UpdateStrategy.READ_WRITE, advancedCalibration, "enabled",
                chckbxEnable, "selected");
        bind(UpdateStrategy.READ_WRITE, advancedCalibration, "alphaPercent",
                sliderAlpha, "value");
        bind(UpdateStrategy.READ_WRITE, advancedCalibration, "calibrationRig", 
                comboBoxPart, "selectedItem");
        bind(UpdateStrategy.READ_WRITE, referenceCamera, "defaultZ", 
                textFieldDefaultZ, "text", lengthConverter);

        bind(UpdateStrategy.READ_WRITE, advancedCalibration, "zRotationError",
                textFieldZRotationError, "text");
        bind(UpdateStrategy.READ_WRITE, advancedCalibration, "yRotationError",
                textFieldYRotationError, "text");
        bind(UpdateStrategy.READ_WRITE, advancedCalibration, "xRotationError",
                textFieldXRotationError, "text");

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldDefaultZ);
        
    }

    private Action overrideAction = new AbstractAction("Override Old Style") {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (chckbxAdvancedCalOverride.isSelected()) {
                chckbxEnable.setEnabled(true);
                comboBoxPart.setEnabled(true);
                textFieldDefaultZ.setEnabled(true);
                startCameraCalibrationBtn.setEnabled(true);
                chckbxUseSavedData.setEnabled(referenceCamera.getAdvancedCalibration().getSavedTestPattern3dPointsList() != null);
                sliderAlpha.setEnabled(true);
            }
            else {
                chckbxEnable.setEnabled(false);
                comboBoxPart.setEnabled(false);
                textFieldDefaultZ.setEnabled(false);
                startCameraCalibrationBtn.setEnabled(false);
                chckbxUseSavedData.setEnabled(false);
                sliderAlpha.setEnabled(false);
            }
        }
    };
    
    private Action startCalibration = new AbstractAction("Start Camera Calibration") {
        @Override
        public void actionPerformed(ActionEvent e) {
            startCameraCalibrationBtn.setEnabled(false);
            
            MainFrame.get().getCameraViews().setSelectedCamera(referenceCamera);

            boolean savedEnabledState = referenceCamera.getAdvancedCalibration().isEnabled();
            
            referenceCamera.getAdvancedCalibration().setEnabled(false);
            referenceCamera.clearCalibrationCache();
            
            if (!chckbxUseSavedData.isSelected()) {
                CameraView cameraView = MainFrame.get().getCameraViews().getCameraView(referenceCamera);
    
                UiUtils.messageBoxOnException(() -> {
                    new CalibrateCameraProcess(MainFrame.get(), cameraView, (Part) comboBoxPart.getSelectedItem()) {
    
                        @Override 
                        public void processRawCalibrationData(double[][][] testPattern3dPointsList, 
                                double[][][] testPatternImagePointsList, Size size) {
                            
                            Logger.trace("processResults has been called!");
    
                            referenceCamera.getAdvancedCalibration().processRawCalibrationData(
                                    testPattern3dPointsList, testPatternImagePointsList, 
                                    size, referenceCamera.getDefaultZ());
                            
                            referenceCamera.getAdvancedCalibration().setEnabled(true);
                            
                            chckbxEnable.setSelected(true);
                            
                            startCameraCalibrationBtn.setEnabled(true);
                            
                            chckbxUseSavedData.setEnabled(true);
                        }
    
                        @Override
                        protected void processCanceled() {
                            referenceCamera.getAdvancedCalibration().setEnabled(true);
                            
                            chckbxEnable.setSelected(savedEnabledState);
                            
                            startCameraCalibrationBtn.setEnabled(savedEnabledState);
                        }
                    };
                });
            }
            else {
                referenceCamera.getAdvancedCalibration().processRawCalibrationData(
                        new Size(referenceCamera.getWidth(), referenceCamera.getHeight()), 
                        referenceCamera.getDefaultZ());
                
                referenceCamera.getAdvancedCalibration().setEnabled(true);
                
                chckbxEnable.setSelected(true);
                
                startCameraCalibrationBtn.setEnabled(true);
            }
        }
    };

    private Action enableCalibration = new AbstractAction("Enable Camera Calibration") {
        @Override
        public void actionPerformed(ActionEvent e) {
            referenceCamera.getAdvancedCalibration().setEnabled(chckbxEnable.isSelected());
            if (chckbxEnable.isSelected()) {
                referenceCamera.clearCalibrationCache();
                
            }

        }
    };
    
    private ChangeListener sliderAlphaChanged = new ChangeListener() {

        @Override
        public void stateChanged(ChangeEvent e) {
            if (!sliderAlpha.getValueIsAdjusting()) {
                double alphaPercent = (int)sliderAlpha.getValue();
                Logger.trace("alphaPercent = " + alphaPercent);
                referenceCamera.clearCalibrationCache();
            }
        }
        
    };
    
    private JCheckBox chckbxEnable;
    private JSlider sliderAlpha;
    private JLabel lblNewLabel;
    private JTextField textFieldDefaultZ;
    private JLabel lblNewLabel_1;
    private JCheckBox chckbxUseSavedData;
    private JCheckBox chckbxAdvancedCalOverride;
    private JTextField textFieldZRotationError;
    private JTextField textFieldYRotationError;
    private JTextField textFieldXRotationError;
    private JLabel lblNewLabel_2;
    private JLabel lblNewLabel_3;
    private JLabel lblNewLabel_4;
    
}
