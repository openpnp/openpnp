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
                ColumnSpec.decode("max(100dlu;default)"),
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
                FormSpecs.DEFAULT_ROWSPEC,}));
        
                lblCalibrationRig = new JLabel("Calibration Rig");
                panelCameraCalibration.add(lblCalibrationRig, "2, 2, right, default");
                
                        comboBoxPart = new JComboBox();
                        comboBoxPart.setModel(new PartsComboBoxModel());
                        comboBoxPart.setRenderer(new IdentifiableListCellRenderer<Part>());
//                        comboBoxPart.setSelectedItem(advancedCalibration.getCalibrationRig());
                        panelCameraCalibration.add(comboBoxPart, "4, 2, left, default");
                        
                        chckbxEnable = new JCheckBox("Use Calibration");
                        chckbxEnable.addActionListener(enableCalibration);
                        
                        lblNewLabel_1 = new JLabel("Default Z");
                        panelCameraCalibration.add(lblNewLabel_1, "2, 4, right, default");
                        
                        textFieldDefaultZ = new JTextField();
                        panelCameraCalibration.add(textFieldDefaultZ, "4, 4, fill, default");
                        textFieldDefaultZ.setColumns(10);
                        panelCameraCalibration.add(chckbxEnable, "2, 6");
                        
                        sliderAlpha = new JSlider();
                        sliderAlpha.setMajorTickSpacing(10);
                        sliderAlpha.setMinorTickSpacing(5);
                        sliderAlpha.setPaintTicks(true);
                        sliderAlpha.setPaintLabels(true);
                        sliderAlpha.addChangeListener(sliderAlphaChanged);
                        
//                                comboBoxPart.addActionListener(new ActionListener() {
//                                    public void actionPerformed(ActionEvent e) {
//                                        calibrationRig =(Part)comboBoxPart.getSelectedItem(); 
//                                    }
//                                });
                
                        startCameraCalibrationBtn = new JButton(startCalibration);
                        panelCameraCalibration.add(startCameraCalibrationBtn, "4, 6");
                        
                        lblNewLabel = new JLabel("Valid Pixels");
                        lblNewLabel.setToolTipText("<html><p width=\"500\">"
                                + "A value of zero forces all valid pixels to be displayed but "
                                + "the edges of the display may show invalid pixels. A value "
                                + "of 100 forces only valid pixels to be displayed but some "
                                + "valid pixels may be lost beyond the edge of the display."
                                + "</p></html>");
                        panelCameraCalibration.add(lblNewLabel, "2, 8");
                        panelCameraCalibration.add(sliderAlpha, "4, 8");
    }

    @Override
    public void createBindings() {
        LengthConverter lengthConverter = new LengthConverter();
        
        bind(UpdateStrategy.READ_WRITE, advancedCalibration, "enabled",
                chckbxEnable, "selected");
        bind(UpdateStrategy.READ_WRITE, advancedCalibration, "alphaPercent",
                sliderAlpha, "value");
        addWrappedBinding(advancedCalibration, "calibrationRig", 
                comboBoxPart, "selectedItem");
        addWrappedBinding(referenceCamera, "defaultZ", 
                textFieldDefaultZ, "text", lengthConverter);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldDefaultZ);
    }

    private Action startCalibration = new AbstractAction("Start Camera Calibration") {
        @Override
        public void actionPerformed(ActionEvent e) {
            MainFrame.get().getCameraViews().setSelectedCamera(referenceCamera);

            startCameraCalibrationBtn.setEnabled(false);

            CameraView cameraView = MainFrame.get().getCameraViews().getCameraView(referenceCamera);

            boolean savedEnabledState = referenceCamera.getAdvancedCalibration().isEnabled();
            
            referenceCamera.getAdvancedCalibration().setEnabled(false);
            referenceCamera.clearCalibrationCache();
            
            UiUtils.messageBoxOnException(() -> {
                new CalibrateCameraProcess(MainFrame.get(), cameraView, (Part) comboBoxPart.getSelectedItem()) {

                    @Override 
                    public void processRawCalibrationData(MatOfPoint3f testPattern3dPoints, 
                            MatOfPoint2f testPatternImagePoints, double testPatternZ, 
                            double xScaling, double yScaling, Size size) {
                        
                        Logger.trace("processResults has been called!");

                        referenceCamera.getAdvancedCalibration().processRawCalibrationData(
                                testPattern3dPoints, testPatternImagePoints, testPatternZ, 
                                xScaling, yScaling, size);
                        
                        referenceCamera.getAdvancedCalibration().setEnabled(true);
                        
                        chckbxEnable.setSelected(true);
                        
                        startCameraCalibrationBtn.setEnabled(true);
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
    
}
