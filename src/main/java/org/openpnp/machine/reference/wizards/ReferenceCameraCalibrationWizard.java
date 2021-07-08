package org.openpnp.machine.reference.wizards;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jdesktop.beansbinding.AutoBinding;
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
import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.processes.CalibrateCameraProcess;
import org.openpnp.gui.processes.EstimateObjectZCoordinateProcess;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.IdentifiableListCellRenderer;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.PartsComboBoxModel;
import org.openpnp.gui.tablemodel.CameraCalibrationHeightsTableModel;
import org.openpnp.gui.tablemodel.PartsTableModel;
import org.openpnp.machine.reference.ContactProbeNozzle;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.ReferenceCamera.AdvancedCalibration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.RegionOfInterest;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Camera.Looking;
import org.openpnp.util.MovableUtils;
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
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.JTable;
import javax.swing.JList;
import javax.swing.border.BevelBorder;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.ScrollPaneLayout;
import javax.swing.border.EtchedBorder;
import javax.swing.JSeparator;

@SuppressWarnings("serial")
public class ReferenceCameraCalibrationWizard extends AbstractConfigurationWizard {
    private final ReferenceCamera referenceCamera;
    private final boolean isMovable;
    private JPanel panelCameraCalibration;
    private JButton startCameraCalibrationBtn;
    private JLabel lblCalibrationRig;
    private JComboBox<?> comboBoxPart;
    private AdvancedCalibration advancedCalibration;
    private List<Length> calibrationHeights;
    private CameraCalibrationHeightsTableModel tableModel;


    /**
     * @return the calibrationHeights
     */
    public List<Length> getCalibrationHeights() {
        return calibrationHeights;
    }

    /**
     * @param calibrationHeights the calibrationHeights to set
     */
    public void setCalibrationHeights(List<Length> calibrationHeights) {
        List<Length> oldValue = this.calibrationHeights;
        this.calibrationHeights = calibrationHeights;
        firePropertyChange("calibrationHeights", oldValue, calibrationHeights);
    }

    public ReferenceCameraCalibrationWizard(ReferenceCamera referenceCamera) {
        this.referenceCamera = referenceCamera;
        isMovable = referenceCamera.getHead() != null;
        advancedCalibration = referenceCamera.getAdvancedCalibration();
        calibrationHeights = new ArrayList<>();
        try {
            int numberOfCalibrationHeights = advancedCalibration.getSavedTestPattern3dPointsList().length;
            for (int i=0; i<numberOfCalibrationHeights; i++) {
                calibrationHeights.add( 
                        new Length(advancedCalibration.getSavedTestPattern3dPointsList()[i][0][2], 
                                LengthUnit.Millimeters));
            }
        }
        catch (NullPointerException e) {
            //No previous calibration heights are available so populate the list with some default values
            if (isMovable) {
                //For moveable cameras, use NaNs which will prompt the operator to probe for the
                //height during the calibration process
                calibrationHeights.add(referenceCamera.getDefaultZ().add(new Length(Double.NaN, LengthUnit.Millimeters)));
                calibrationHeights.add(referenceCamera.getDefaultZ().add(new Length(Double.NaN, LengthUnit.Millimeters)));
            }
            else {
                //For non-movable cameras, use defaultZ and the height half-way between defaultZ and safeZ
                calibrationHeights.add(referenceCamera.getDefaultZ());
                calibrationHeights.add(referenceCamera.getDefaultZ().add(referenceCamera.getSafeZ()).multiply(0.5));
            }
        }
        panelCameraCalibration = new JPanel();
        panelCameraCalibration.setBorder(new TitledBorder(null, "Camera Calibration",
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelCameraCalibration);
        panelCameraCalibration.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(87dlu;default):grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(87dlu;default):grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(87dlu;default):grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(87dlu;default):grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
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
                FormSpecs.UNRELATED_GAP_ROWSPEC,
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
        
        chckbxAdvancedCalOverride = new JCheckBox("Enable experimental calibration to override old style image transforms and distortion correction settings");
        chckbxAdvancedCalOverride.setToolTipText("Enable this to use advanced calibration.  Disable this to restore usage of old settings.");
        chckbxAdvancedCalOverride.addActionListener(overrideAction);
        panelCameraCalibration.add(chckbxAdvancedCalOverride, "2, 2, 11, 1");
        
        sliderAlpha = new JSlider();
        sliderAlpha.setValue(100);
        sliderAlpha.setMajorTickSpacing(10);
        sliderAlpha.setMinorTickSpacing(5);
        sliderAlpha.setPaintTicks(true);
        sliderAlpha.setPaintLabels(true);
        sliderAlpha.addChangeListener(sliderAlphaChanged);
        
        lblNewLabel = new JLabel("Crop All Invalid Pixels");
        lblNewLabel.setHorizontalAlignment(SwingConstants.TRAILING);
        panelCameraCalibration.add(lblNewLabel, "2, 6");
        sliderAlpha.setToolTipText("<html><p width=\"500\">"
                + "A value of 0 crops all invalid pixels from the edge of the image but at the "
                + "risk of losing some valid pixels at the edge of the image. A value"
                + " of 100 forces all valid pixels to be displayed but at the risk of some invalid "
                + "(usually black) pixels being displayed around the edges of the image."
                + "</p></html>");
        panelCameraCalibration.add(sliderAlpha, "4, 6, 5, 1");
        
        lblNewLabel_3 = new JLabel("Show All Valid Pixels");
        panelCameraCalibration.add(lblNewLabel_3, "10, 6");
        
        lblNewLabel_1 = new JLabel("Default Working Plane Z");
        panelCameraCalibration.add(lblNewLabel_1, "2, 4, right, default");
        
        textFieldDefaultZ = new JTextField();
        panelCameraCalibration.add(textFieldDefaultZ, "4, 4, fill, default");
        textFieldDefaultZ.setColumns(10);
        
        separator = new JSeparator();
        panelCameraCalibration.add(separator, "2, 8, 13, 1");
        
        lblCalibrationRig = new JLabel("Calibration Rig");
        panelCameraCalibration.add(lblCalibrationRig, "2, 10, right, default");
        
        comboBoxPart = new JComboBox();
        comboBoxPart.setModel(new PartsComboBoxModel());
        comboBoxPart.setRenderer(new IdentifiableListCellRenderer<Part>());
        panelCameraCalibration.add(comboBoxPart, "4, 10, 5, 1, left, default");
        
        chckbxUseNozzleTip = new JCheckBox("Use Nozzle Tip");
        chckbxUseNozzleTip.setVisible(!isMovable);
        chckbxUseNozzleTip.addActionListener(new AbstractAction("Use Nozzle Tip Action") {

            @Override
            public void actionPerformed(ActionEvent e) {
                comboBoxPart.setEnabled(!chckbxUseNozzleTip.isSelected());
            }
            
        });
        panelCameraCalibration.add(chckbxUseNozzleTip, "10, 10");
        
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
        
        tableModel = new CameraCalibrationHeightsTableModel(this);
                
        panelCalibrationheights = new JPanel();
        panelCalibrationheights.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, new Color(255, 255, 255), new Color(160, 160, 160)), "Calibration Z Coordinates", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        panelCalibrationheights.setToolTipText("These are the machine Z coordinates, in the order "
                + "shown, at which calibration data will be collected.");
        panelCalibrationheights.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("60dlu"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(49dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,}));
        panelCameraCalibration.add(panelCalibrationheights, "4, 12, 5, 1, default, top");
        
        String heading;
        if (isMovable) {
            heading = "<html>Enter the machine Z coordinates of the fiducials that will be used to "
                    + "calibrate the camera. At least two different Z coordinates must be entered "
                    + "and the range should include the default working plane Z coordinate for the "
                    + "camera. The calibration data will be collected at each Z coordinate in the "
                    + "order entered here. Note: entering values of NaN will cause the operator to "
                    + "be prompted to measure the fiducials' Z coordinate at the appropriate time "
                    + "during the calibration collection process.</html>";
        }
        else {
            heading = "<html>Enter the machine Z coordinates at which the camera is to be "
                    + "calibrated. At least two different Z coordinates must be entered and the "
                    + "range should include the default working plane Z coordinate for the camera. "
                    + "The calibration data will be collected at each Z coordinate in the order "
                    + "entered here.</html>";
        }
        
        lblCalibrationHeightsHeading = new JLabel(heading);
        
        panelCalibrationheights.add(lblCalibrationHeightsHeading, "2, 2, 12, 1, fill, default");
        
                tableHeights = new AutoSelectTextTable(tableModel);
                tableHeights.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                tableHeights.setShowGrid(true);
                tableHeights.setTableHeader(null);
                
                scrollPane = new JScrollPane(tableHeights);
                scrollPane.setViewportBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
                scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
                scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                panelCalibrationheights.add(scrollPane, "2, 4, 3, 3, fill, fill");
                
                 btnDeleteHeight = new JButton();
                 btnDeleteHeight.setIcon(Icons.delete);
                 btnDeleteHeight.setToolTipText("Delete the selected coordinate from the list");
                 btnDeleteHeight.addActionListener(new AbstractAction("Delete height action") {

                     @Override
                     public void actionPerformed(ActionEvent e) {
                         int row = tableHeights.getSelectedRow();
                         if ((row >= 0) && (calibrationHeights.size() > 2)) {
                             calibrationHeights.remove(row);
                             tableModel.refresh();
                             if (calibrationHeights.size() > row) {
                                 tableHeights.clearSelection();
                                 tableHeights.addRowSelectionInterval(row, row);
                             }
                         }
                         
                     }
                     
                 });
                 
                 btnAddHeight = new JButton();
                 btnAddHeight.setIcon(Icons.add);
                 btnAddHeight.setToolTipText("Add a new coordinate to the list");
                 btnAddHeight.addActionListener(new AbstractAction("Add height action") {

                     @Override
                     public void actionPerformed(ActionEvent e) {
                         if (referenceCamera.getHead() == null) {
                             calibrationHeights.add(referenceCamera.getDefaultZ());
                         }
                         else {
                             calibrationHeights.add(new Length(Double.NaN, LengthUnit.Millimeters));
                         }
                         int row = calibrationHeights.size()-1;
                         tableModel.refresh();
                         tableHeights.clearSelection();
                         tableHeights.addRowSelectionInterval(row, row);
                     }
                     
                 });
                 panelCalibrationheights.add(btnAddHeight, "6, 4");
                 
                 btnCaptureZ = new JButton("");
                 btnCaptureZ.setIcon(Icons.captureTool);
                 btnCaptureZ.setToolTipText("Set the selected entry to the selected tool's Z coordinate");
                 btnCaptureZ.addActionListener(new AbstractAction("Capture Z height of selected tool" ) {

                     @Override
                     public void actionPerformed(ActionEvent e) {
                         int row = tableHeights.getSelectedRow();
                         if (row >= 0) {
                             UiUtils.messageBoxOnException(() -> {
                                 Location l = MainFrame.get()
                                                       .getMachineControls()
                                                       .getSelectedTool()
                                                       .getLocation();
                                 calibrationHeights.set(row, l.getLengthZ());
                                 tableModel.refresh();
                             });
                         }
                     }
                     
                 });
                 panelCalibrationheights.add(btnCaptureZ, "8, 4");
                 
                 btnHeightProbe = new JButton("");
                 btnHeightProbe.setIcon(Icons.contactProbeNozzle);
                 btnHeightProbe.setToolTipText("Set the selected entry by probing with a contact probe nozzle");
                 btnHeightProbe.setVisible(isMovable);
                 btnHeightProbe.addActionListener(new AbstractAction("Probe Z height with contact probe nozzle" ) {

                     @Override
                     public void actionPerformed(ActionEvent e) {
                         int row = tableHeights.getSelectedRow();
                         if (row >= 0) {
                             UiUtils.submitUiMachineTask(() -> {
                                 HeadMountable tool = MainFrame.get()
                                         .getMachineControls()
                                         .getSelectedTool();
                                 if (!(tool instanceof ContactProbeNozzle)) {
                                     throw new Exception("Nozzle " + tool.getName() + " is not a ContactProbeNozzle.");
                                 }
                                 ContactProbeNozzle nozzle = (ContactProbeNozzle)tool;
                                 final Location probedLocation = nozzle.contactProbeCycle(nozzle.getLocation());
                                 MovableUtils.fireTargetedUserAction(nozzle);
                                 SwingUtilities.invokeAndWait(() -> {
                                     calibrationHeights.set(row, probedLocation.getLengthZ());
                                     tableModel.refresh();
                                 });
                             });
                         }
                     }
                     
                 });
                 panelCalibrationheights.add(btnHeightProbe, "10, 4");
                 
                 panelCalibrationheights.add(btnDeleteHeight, "6, 6");
                 
                 btnHeightUp = new JButton();
                 btnHeightUp.setIcon(Icons.arrowUp);
                 btnHeightUp.setToolTipText("Move the selected height up towards the top of the list");
                 btnHeightUp.addActionListener(new AbstractAction("Move height up action") {

                     @Override
                     public void actionPerformed(ActionEvent e) {
                         int row = tableHeights.getSelectedRow();
                         if (row >= 1) {
                             Length item = calibrationHeights.remove(row);
                             calibrationHeights.add(row-1, item);
                             tableModel.refresh();
                             tableHeights.clearSelection();
                             tableHeights.addRowSelectionInterval(row-1, row-1);
                         }
                         
                     }
                     
                 });
                 panelCalibrationheights.add(btnHeightUp, "8, 6");
                 
                 btnHeightDown = new JButton();
                 btnHeightDown.setIcon(Icons.arrowDown);
                 btnHeightDown.setToolTipText("Move the selected coordinate down towards the bottom of the list");
                 btnHeightDown.addActionListener(new AbstractAction("Move height down action") {

                     @Override
                     public void actionPerformed(ActionEvent e) {
                         int row = tableHeights.getSelectedRow();
                         if ((row >= 0) && (row < calibrationHeights.size()-1)) {
                             Length item = calibrationHeights.remove(row);
                             calibrationHeights.add(row+1, item);
                             tableModel.refresh();
                             tableHeights.clearSelection();
                             tableHeights.addRowSelectionInterval(row+1, row+1);
                         }
                         
                     }
                     
                 });
                 panelCalibrationheights.add(btnHeightDown, "10, 6");
                 
                 String labelCautionString = " ";
                 if (!isMovable) {
                     labelCautionString = "<html><p style=\"color:Black;background-color:Yellow;\"><b>"
                             + "CAUTION: The nozzle tip (with calibration rig, if used) will be lowered to "
                             + "each of the heights listed here and scanned over the camera's entire field "
                             + "of view during calibration data collection. Ensure there are no "
                             + "obstructions at or above these heights near the camera or machine damage "
                             + "may occur!<b></html>";
                 }
                
         lblCalibrationHeights = new JLabel(labelCautionString);
         panelCalibrationheights.add(lblCalibrationHeights, "2, 8, 12, 1, fill, default");
        
        
        startCameraCalibrationBtn = new JButton(startCalibration);
        startCameraCalibrationBtn.setText("Start Calibration");
        panelCameraCalibration.add(startCameraCalibrationBtn, "4, 14");
                
                chckbxUseSavedData = new JCheckBox("Just Reprocess Prior Data");
                chckbxUseSavedData.setEnabled(referenceCamera.getAdvancedCalibration().getSavedTestPattern3dPointsList() != null);
                chckbxUseSavedData.setToolTipText("Set this skip collection of new calibration data and just reprocess previously collected calibration data - only useful for code debugging");
                panelCameraCalibration.add(chckbxUseSavedData, "6, 14, 3, 1");
        
                chckbxEnable = new JCheckBox("Apply Calibration");
                chckbxEnable.setToolTipText("Enable this to use the new image transform and distortion correction settings.  Disable this and no calibration will be applied.");
                panelCameraCalibration.add(chckbxEnable, "2, 16");
        
        separator_1 = new JSeparator();
        panelCameraCalibration.add(separator_1, "2, 18, 13, 1");
        
        lblNewLabel_5 = new JLabel("X Axis");
        panelCameraCalibration.add(lblNewLabel_5, "4, 20");
        
        lblNewLabel_6 = new JLabel("Y Axis");
        panelCameraCalibration.add(lblNewLabel_6, "6, 20");
        
        lblNewLabel_7 = new JLabel("Z Axis");
        panelCameraCalibration.add(lblNewLabel_7, "8, 20");
        
        lblNewLabel_2 = new JLabel("Camera Mounting Errors [Deg]");
        lblNewLabel_2.setToolTipText("<html><p width=\"500\">"
                + "The estimated camera mounting errors using the right hand rule about "
                + "each machine axis</p></html>");
        panelCameraCalibration.add(lblNewLabel_2, "2, 22, right, default");
        
        textFieldXRotationError = new JTextField();
        textFieldXRotationError.setEditable(false);
        panelCameraCalibration.add(textFieldXRotationError, "4, 22, fill, default");
        textFieldXRotationError.setColumns(10);
        
        textFieldYRotationError = new JTextField();
        textFieldYRotationError.setEditable(false);
        panelCameraCalibration.add(textFieldYRotationError, "6, 22, fill, default");
        textFieldYRotationError.setColumns(10);
        
        textFieldZRotationError = new JTextField();
        textFieldZRotationError.setEditable(false);
        panelCameraCalibration.add(textFieldZRotationError, "8, 22, fill, default");
        textFieldZRotationError.setColumns(10);
        
        lblNewLabel_4 = new JLabel("Model RMS Error [pixels]");
        panelCameraCalibration.add(lblNewLabel_4, "2, 24, right, default");
        
        textFieldRmsError = new JTextField();
        textFieldRmsError.setEditable(false);
        panelCameraCalibration.add(textFieldRmsError, "4, 24, fill, default");
        textFieldRmsError.setColumns(10);
        
    }

    @Override
    public void createBindings() {
        LengthConverter lengthConverter = new LengthConverter();
        DoubleConverter doubleConverter = new DoubleConverter("%.3f");
        
        bind(UpdateStrategy.READ_WRITE, advancedCalibration, 
                "overrideOldTransformsAndDistortionCorrectionSettings",
                chckbxAdvancedCalOverride, "selected");
        bind(UpdateStrategy.READ_WRITE, advancedCalibration, "enabled",
                chckbxEnable, "selected");
        bind(UpdateStrategy.READ_WRITE, advancedCalibration, "alphaPercent",
                sliderAlpha, "value");
        bind(UpdateStrategy.READ_WRITE, advancedCalibration, "calibrationRig", 
                comboBoxPart, "selectedItem");
        bind(UpdateStrategy.READ_WRITE, referenceCamera, "defaultZ", 
                textFieldDefaultZ, "text", lengthConverter);
        
        bind(UpdateStrategy.READ, advancedCalibration, "rotationErrorZ",
                textFieldZRotationError, "text", doubleConverter);
        bind(UpdateStrategy.READ, advancedCalibration, "rotationErrorY",
                textFieldYRotationError, "text", doubleConverter);
        bind(UpdateStrategy.READ, advancedCalibration, "rotationErrorX",
                textFieldXRotationError, "text", doubleConverter);

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
                chckbxUseSavedData.setEnabled(referenceCamera.getAdvancedCalibration().
                        getSavedTestPattern3dPointsList() != null);
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
    
    private Action startCalibration = new AbstractAction("Start Calibration Data Collection") {
        @Override
        public void actionPerformed(ActionEvent e) {
            Logger.trace("thread = " + Thread.currentThread());
            startCameraCalibrationBtn.setEnabled(false);
            
            MainFrame.get().getCameraViews().setSelectedCamera(referenceCamera);

            boolean savedEnabledState = referenceCamera.getAdvancedCalibration().isEnabled();
            
            chckbxEnable.setSelected(false);
            referenceCamera.clearCalibrationCache();
            
            if (!chckbxUseSavedData.isSelected()) {
                CameraView cameraView = MainFrame.get().getCameraViews().getCameraView(referenceCamera);
    
                UiUtils.messageBoxOnException(() -> {
                    new CalibrateCameraProcess(MainFrame.get(), cameraView, (Part) comboBoxPart.getSelectedItem(), calibrationHeights) {
    
                        @Override 
                        public void processRawCalibrationData(double[][][] testPattern3dPointsList, 
                                double[][][] testPatternImagePointsList, Size size) {
                            
                            Logger.trace("processing thread = " + Thread.currentThread());
                            
                            referenceCamera.getAdvancedCalibration().processRawCalibrationData(
                                    testPattern3dPointsList, testPatternImagePointsList, 
                                    size, referenceCamera.getDefaultZ());
                            
                            chckbxEnable.setSelected(true);
//                            referenceCamera.getAdvancedCalibration().setEnabled(true);
                            
                            chckbxUseSavedData.setEnabled(true);
                            
                            startCameraCalibrationBtn.setEnabled(true);
                        }
    
                        @Override
                        protected void processCanceled() {
                            Logger.trace("cancelling thread = " + Thread.currentThread());
                            
                            chckbxEnable.setSelected(savedEnabledState);
//                            referenceCamera.getAdvancedCalibration().setEnabled(savedEnabledState);
                            
                            startCameraCalibrationBtn.setEnabled(true);
                        }
                    };
                });
            }
            else {
                referenceCamera.getAdvancedCalibration().processRawCalibrationData(
                        new Size(referenceCamera.getWidth(), referenceCamera.getHeight()), 
                        referenceCamera.getDefaultZ());
                
                chckbxEnable.setSelected(true);
//                referenceCamera.getAdvancedCalibration().setEnabled(true);
                
                startCameraCalibrationBtn.setEnabled(true);
            }
        }
    };

    private Action enableCalibration = new AbstractAction("Enable Camera Calibration") {
        @Override
        public void actionPerformed(ActionEvent e) {
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
    private JLabel lblNewLabel_5;
    private JLabel lblNewLabel_6;
    private JLabel lblNewLabel_7;
    private JLabel lblNewLabel_3;
    private JCheckBox chckbxUseNozzleTip;
    private JTable tableHeights;
    private JPanel panelCalibrationheights;
    private JButton btnAddHeight;
    private JButton btnDeleteHeight;
    private JButton btnHeightUp;
    private JButton btnHeightDown;
    private JButton btnCaptureZ;
    private JButton btnHeightProbe;
    private JLabel lblCalibrationHeights;
    private JLabel lblCalibrationHeightsHeading;
    private JScrollPane scrollPane;
    private JTextField textFieldRmsError;
    private JLabel lblNewLabel_4;
    private JSeparator separator;
    private JSeparator separator_1;
    
}
