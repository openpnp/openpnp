package org.openpnp.machine.reference.wizards;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
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
import org.opencv.imgproc.Imgproc;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.CameraViewFilter;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.SimpleGraphView;
import org.openpnp.gui.components.VerticalLabel;
import org.openpnp.gui.components.reticle.FiducialReticle.Shape;
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
import org.openpnp.machine.reference.camera.calibration.AdvancedCalibration;
import org.openpnp.machine.reference.camera.calibration.CameraCalibrationUtils;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.RegionOfInterest;
import org.openpnp.spi.Camera;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Camera.Looking;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.SimpleGraph;
import org.openpnp.util.SimpleGraph.DataRow;
import org.openpnp.util.UiUtils;
import org.openpnp.vision.FluentCv;
import org.openpnp.vision.LensCalibration.LensModel;
import org.openpnp.vision.pipeline.ui.MatView;
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
import javax.swing.SpinnerListModel;
import javax.swing.border.EtchedBorder;
import javax.swing.JSeparator;
import javax.swing.JSpinner;

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
    private SimpleGraphView modelErrorsTimeSequenceView;
    private SimpleGraphView modelErrorsScatterPlotView;
    private VerticalLabel lblNewLabel_11;
    private JLabel lblNewLabel_12;
    private VerticalLabel lblNewLabel_13;
    private JLabel lblNewLabel_16;
    private int heightIndex = 0;



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
            //No previous calibration heights are available so populate the list with some default
            //values
            if (isMovable) {
                //For movable cameras, default to NaNs which will prompt the operator to probe for
                //the height during the calibration process
                calibrationHeights.add(referenceCamera.getDefaultZ().
                        add(new Length(Double.NaN, LengthUnit.Millimeters)));
                calibrationHeights.add(referenceCamera.getDefaultZ().
                        add(new Length(Double.NaN, LengthUnit.Millimeters)));
            }
            else {
                //For non-movable cameras, default to defaultZ and the height half-way between 
                //defaultZ and safeZ
                calibrationHeights.add(referenceCamera.getDefaultZ());
                calibrationHeights.add(referenceCamera.getDefaultZ().
                        add(referenceCamera.getSafeZ()).multiply(0.5));
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
                ColumnSpec.decode("max(87dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(87dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(87dlu;default)"),
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
                FormSpecs.UNRELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                RowSpec.decode("10dlu"),
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                RowSpec.decode("max(114dlu;default)"),
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                RowSpec.decode("10dlu"),
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                RowSpec.decode("max(178dlu;default)"),
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                RowSpec.decode("10dlu"),
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                RowSpec.decode("max(151dlu;default)"),
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                RowSpec.decode("default:grow"),}));
        
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
        
        //Someday may want to allow use of nozzle tips for bottom camera calibration but leave this
        //out for now since the pipeline for nozzle tip calibration would need to be setup first 
        //but that doesn't really make a lot of sense since the bottom camera needs to be 
        //calibrated before nozzle tip calibration can be setup
//        chckbxUseNozzleTip = new JCheckBox("Use Nozzle Tip");
//        chckbxUseNozzleTip.setVisible(!isMovable);
//        chckbxUseNozzleTip.addActionListener(new AbstractAction("Use Nozzle Tip Action") {
//
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                comboBoxPart.setEnabled(!chckbxUseNozzleTip.isSelected());
//            }
//            
//        });
//        panelCameraCalibration.add(chckbxUseNozzleTip, "10, 10");
        
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
                    + "and ideally, the range should include the default working plane Z "
                    + "coordinate for the camera. The calibration data will be collected at each Z "
                    + "coordinate in the order entered here. NOTE: Entering values of NaN will "
                    + "cause the operator to be prompted to measure the fiducials' Z coordinate at "
                    + "the appropriate time during the calibration collection process.</html>";
        }
        else {
            heading = "<html>Enter the machine Z coordinates at which the camera is to be "
                    + "calibrated. At least two different Z coordinates must be entered and "
                    + "ideally, the range should include the default working plane Z coordinate "
                    + "for the camera. The calibration data will be collected at each Z coordinate "
                    + "in the order entered here.</html>";
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
                 btnHeightUp.setToolTipText("Move the selected coordinate up towards the top of the list");
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
                             + "of view during calibration data collection. Ensure there is sufficent "
                             + "clearance to any obstructions at or above these heights near the camera "
                             + "or machine damage may occur!<b></html>";
                 }
                
        lblCaution = new JLabel(labelCautionString);
        panelCalibrationheights.add(lblCaution, "2, 8, 12, 1, fill, default");
        
        
        startCameraCalibrationBtn = new JButton(startCalibration);
        startCameraCalibrationBtn.setText("Start Calibration");
        panelCameraCalibration.add(startCameraCalibrationBtn, "4, 14");
                
        chckbxUseSavedData = new JCheckBox("Skip New Collection And Only Reprocess Prior Calibration Data");
        chckbxUseSavedData.setEnabled(advancedCalibration.isValid());
        chckbxUseSavedData.setToolTipText("Set this to skip collection of new calibration data and just reprocess previously collected calibration data - only useful for code debugging");
        panelCameraCalibration.add(chckbxUseSavedData, "6, 14, 3, 1");

        chckbxEnable = new JCheckBox("Apply Calibration");
        chckbxEnable.setToolTipText("Enable this to apply the new image transform and distortion correction settings.  Disable this and no calibration will be applied (raw images will be displayed).");
        chckbxEnable.setEnabled(advancedCalibration.isValid());
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
        lblNewLabel_4.setToolTipText("<html><p width=\"500\">"
                + "This is a measure of how badly the pixel locations computed by the mathematical "
                + "camera model compare to the observed pixel locations collected during the "
                + "calibration process.</p></html>");
        panelCameraCalibration.add(lblNewLabel_4, "2, 24, right, default");
        
        textFieldRmsError = new JTextField();
        textFieldRmsError.setEditable(false);
        panelCameraCalibration.add(textFieldRmsError, "4, 24, fill, default");
        textFieldRmsError.setColumns(10);
        
        lblNewLabel_8 = new JLabel("View Errors For Cal Height");
        panelCameraCalibration.add(lblNewLabel_8, "2, 26");
        
        spinnerIndex = new JSpinner(new SpinnerListModel(calibrationHeights));
        spinnerIndex.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                Length selectedHeight = (Length) spinnerIndex.getValue();
                Logger.trace("selectedHeight = " + selectedHeight);
                heightIndex  = calibrationHeights.indexOf(selectedHeight);
                Logger.trace("heightIndex = " + heightIndex);
                updateDiagnosticsDisplay();
            }
            
        });
        panelCameraCalibration.add(spinnerIndex, "4, 26");

        
        lblNewLabel_14 = new JLabel("Residual Errors In Order Collected");
        lblNewLabel_14.setFont(new Font("Tahoma", Font.BOLD, 13));
        lblNewLabel_14.setHorizontalAlignment(SwingConstants.CENTER);
        panelCameraCalibration.add(lblNewLabel_14, "4, 28, 3, 1");

        lblNewLabel_9 = new VerticalLabel("Residual Error [pixels]");
        lblNewLabel_9.setVerticalAlignment(SwingConstants.BOTTOM);
        lblNewLabel_9.setRotation(VerticalLabel.ROTATE_LEFT);
        panelCameraCalibration.add(lblNewLabel_9, "2, 30");
        
        modelErrorsTimeSequenceView = new SimpleGraphView();
        modelErrorsTimeSequenceView.setFont(new Font("Dialog", Font.PLAIN, 11));
        panelCameraCalibration.add(modelErrorsTimeSequenceView, "4, 30, 3, 1, fill, fill");
        
        String legend = "\r\n<p><body style=\"text-align:left\">\r\n<p>\r\nX Residual <span style=\"color:#FF0000\">&mdash;&mdash;</span>\r\n</p>\r\n<p>\r\nY Residual <span style=\"color:#00BB00\">&mdash;&mdash;</span>\r\n</p>\r\n</body></p>\r\n";
        lblNewLabel_17 = new JLabel("<html><p width=\"500\">"
                + "This plot displays the residual pixel error (that is, the remaining error after "
                + "calibration has been applied) of each calibration point in the order it "
                + "was collected. The residual errors should have zero mean and appear as random "
                + "noise. If there are significant jumps or trends in the errors; depending on "
                + "their magnitude, there may be a problem with the calibration. Some possible "
                + "causes are: calibration rig movement/slippage during the collection; camera or "
                + "lens moving in its mount; motors missing steps; belt/cog slippage; thermal "
                + "expansion; etcetera."
                + "</p>\r\n" + legend + "</html>");
        panelCameraCalibration.add(lblNewLabel_17, "8, 30, 3, 1, left, default");
        
        lblNewLabel_10 = new JLabel("Collection Sequence Number");
        lblNewLabel_10.setHorizontalAlignment(SwingConstants.CENTER);
        panelCameraCalibration.add(lblNewLabel_10, "4, 32, 3, 1");

        
        lblNewLabel_15 = new JLabel("Residual Error Scatter Plot");
        lblNewLabel_15.setFont(new Font("Tahoma", Font.BOLD, 13));
        lblNewLabel_15.setHorizontalAlignment(SwingConstants.CENTER);
        panelCameraCalibration.add(lblNewLabel_15, "4, 34, 3, 1");

        lblNewLabel_11 = new VerticalLabel("Y Residual Error [pixels]");
        lblNewLabel_11.setVerticalAlignment(SwingConstants.BOTTOM);
        lblNewLabel_11.setRotation(VerticalLabel.ROTATE_LEFT);
        panelCameraCalibration.add(lblNewLabel_11, "2, 36");

        modelErrorsScatterPlotView = new SimpleGraphView();
        modelErrorsScatterPlotView.setFont(new Font("Dialog", Font.PLAIN, 11));
        panelCameraCalibration.add(modelErrorsScatterPlotView, "4, 36, 3, 1, fill, fill");
        
        lblNewLabel_18 = new JLabel("<html><p width=\"500\">"
                + "This plot displays the residual pixel location error of each point collected "
                + "during the calibration process. The green circle marks the approximate boundary "
                + "at which points are considered to be outliers and are not used for determining "
                + "the camera calibration parameters. The residual errors should form a single "
                + "circular cluster centered at (0, 0) and should appear randomly distributed. If "
                + "two or more distinct clusters are present or the cluster is significantly "
                + "non-circular; depending on the magnitude of the errors, there may be a problem "
                + "with the calibration. Some possible causes are: bad vision detection of the "
                + "calibration fiducial, calibration rig movement/slippage during the collection; "
                + "loose camera mount; under or over compensated backlash; motors missing steps; "
                + "belt/cog slippage; etcetera."
                + "</p></html>");
        panelCameraCalibration.add(lblNewLabel_18, "8, 36, 3, 1, left, default");
        
        lblNewLabel_12 = new JLabel("X Residual Error [pixels]");
        lblNewLabel_12.setHorizontalAlignment(SwingConstants.CENTER);
        panelCameraCalibration.add(lblNewLabel_12, "4, 38, 3, 1");
        

        lblNewLabel_16 = new JLabel("Residual Error Heat Map");
        lblNewLabel_16.setFont(new Font("Tahoma", Font.BOLD, 13));
        lblNewLabel_16.setHorizontalAlignment(SwingConstants.CENTER);
        panelCameraCalibration.add(lblNewLabel_16, "4, 40, 3, 1");

        lblNewLabel_13 = new VerticalLabel("Image Y Location [pixels]");
        lblNewLabel_13.setVerticalAlignment(SwingConstants.BOTTOM);
        lblNewLabel_13.setRotation(VerticalLabel.ROTATE_LEFT);
        panelCameraCalibration.add(lblNewLabel_13, "2, 42");

        modelErrorsView = new MatView();
        panelCameraCalibration.add(modelErrorsView, "4, 42, 3, 1, fill, fill");
        
        lblNewLabel_19 = new JLabel("<html><p width=\"500\">"
                + "This plot displays the magnitude of the residual pixel location error as a "
                + "function of the expected location of the pixel in the image. Dark blue areas "
                + "have very low errors while dark red areas have the highest errors.  Note that "
                + "the color range is always scaled so that zero error is the darkest blue and "
                + "the maximum magnitude error is the darkest red. This means this plot cannot "
                + "be used to judge the magnitude of the error but only its distribution about "
                + "the image. This distribution should look more or less random with no "
                + "discernible patterns. If patterns such as rings or stripes are clearly "
                + "visible, the mathematical model of the camera does not fit very well with the "
                + "physical reality of the camera and may indicate something is physically wrong "
                + "with the camera."
                + "</p></html>");
        panelCameraCalibration.add(lblNewLabel_19, "8, 42, 3, 1, left, default");
        
        lblNewLabel_12 = new JLabel("Image X Location [pixels]");
        lblNewLabel_12.setHorizontalAlignment(SwingConstants.CENTER);
        panelCameraCalibration.add(lblNewLabel_12, "4, 44, 3, 1");
    }

    @Override
    public void createBindings() {
        LengthConverter lengthConverter = new LengthConverter();
        DoubleConverter doubleConverter = new DoubleConverter("%.3f");
        
        bind(UpdateStrategy.READ_WRITE, advancedCalibration, 
                "overridingOldTransformsAndDistortionCorrectionSettings",
                chckbxAdvancedCalOverride, "selected");
        bind(UpdateStrategy.READ_WRITE, advancedCalibration, "enabled",
                chckbxEnable, "selected");
        bind(UpdateStrategy.READ, advancedCalibration, "valid",
                chckbxEnable, "enabled");
        bind(UpdateStrategy.READ_WRITE, advancedCalibration, "alphaPercent",
                sliderAlpha, "value");
        bind(UpdateStrategy.READ_WRITE, advancedCalibration, "calibrationRig", 
                comboBoxPart, "selectedItem");
        bind(UpdateStrategy.READ_WRITE, referenceCamera, "defaultZ", 
                textFieldDefaultZ, "text", lengthConverter);
        
        bind(UpdateStrategy.READ, advancedCalibration, "rmsError",
                textFieldRmsError, "text", doubleConverter);
        bind(UpdateStrategy.READ, advancedCalibration, "rotationErrorZ",
                textFieldZRotationError, "text", doubleConverter);
        bind(UpdateStrategy.READ, advancedCalibration, "rotationErrorY",
                textFieldYRotationError, "text", doubleConverter);
        bind(UpdateStrategy.READ, advancedCalibration, "rotationErrorX",
                textFieldXRotationError, "text", doubleConverter);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldDefaultZ);
        
        enableControls(advancedCalibration.isOverridingOldTransformsAndDistortionCorrectionSettings());
        
        updateDiagnosticsDisplay();
    }

    private void enableControls(boolean b) {
        if (b) {
            chckbxEnable.setEnabled(advancedCalibration.isValid());
            comboBoxPart.setEnabled(true);
//            chckbxUseNozzleTip.setEnabled(true);
            textFieldDefaultZ.setEnabled(true);
            tableHeights.setEnabled(true);
            btnAddHeight.setEnabled(true);
            btnDeleteHeight.setEnabled(true);
            btnHeightUp.setEnabled(true);
            btnHeightDown.setEnabled(true);
            btnCaptureZ.setEnabled(true);
            btnHeightProbe.setEnabled(true);
            startCameraCalibrationBtn.setEnabled(true);
            chckbxUseSavedData.setEnabled(advancedCalibration.isValid());
            sliderAlpha.setEnabled(true);
        }
        else {
            chckbxEnable.setEnabled(false);
            comboBoxPart.setEnabled(false);
//            chckbxUseNozzleTip.setEnabled(false);
            textFieldDefaultZ.setEnabled(false);
            tableHeights.setEnabled(false);
            btnAddHeight.setEnabled(false);
            btnDeleteHeight.setEnabled(false);
            btnHeightUp.setEnabled(false);
            btnHeightDown.setEnabled(false);
            btnCaptureZ.setEnabled(false);
            btnHeightProbe.setEnabled(false);
            panelCalibrationheights.setEnabled(false);
            startCameraCalibrationBtn.setEnabled(false);
            chckbxUseSavedData.setEnabled(false);
            sliderAlpha.setEnabled(false);
        }
    }
    
    private Action overrideAction = new AbstractAction("Override Old Style") {
        @Override
        public void actionPerformed(ActionEvent e) {
            enableControls(chckbxAdvancedCalOverride.isSelected());
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
                            
                            try {
                                referenceCamera.getAdvancedCalibration().processRawCalibrationData(
                                        testPattern3dPointsList, testPatternImagePointsList, 
                                        size, referenceCamera.getDefaultZ());
                                
                                //Reload the calibration heights and refresh the table
                                calibrationHeights.clear();
                                int numberOfCalibrationHeights = advancedCalibration.getSavedTestPattern3dPointsList().length;
                                for (int i=0; i<numberOfCalibrationHeights; i++) {
                                    calibrationHeights.add( 
                                            new Length(advancedCalibration.getSavedTestPattern3dPointsList()[i][0][2], 
                                                    LengthUnit.Millimeters));
                                }
                                tableModel.refresh();
                                
                                advancedCalibration.setValid(true);
                                
                                chckbxEnable.setSelected(true);
                                
                                chckbxUseSavedData.setEnabled(true);
                                
                                updateDiagnosticsDisplay();
                            }
                            catch (Exception e) {
                                advancedCalibration.setValid(false);
                            }
                            
                            startCameraCalibrationBtn.setEnabled(true);
                        }
    
                        @Override
                        protected void processCanceled() {
                            Logger.trace("cancelling thread = " + Thread.currentThread());
                            
                            chckbxEnable.setSelected(savedEnabledState);
                            
                            startCameraCalibrationBtn.setEnabled(true);
                        }
                    };
                });
            }
            else {
                try {
                    referenceCamera.getAdvancedCalibration().processRawCalibrationData(
                            new Size(referenceCamera.getWidth(), referenceCamera.getHeight()), 
                            referenceCamera.getDefaultZ());
                
                    advancedCalibration.setValid(true);
                    
                    chckbxEnable.setSelected(true);
                    
                    updateDiagnosticsDisplay();
                }
                catch (Exception ex) {
                    Logger.trace(ex);
                    advancedCalibration.setValid(false);
                }
                
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
    
    private void updateDiagnosticsDisplay() {
        List<double[]> residuals = CameraCalibrationUtils.computeResidualErrors(advancedCalibration.getSavedTestPatternImagePointsList(), advancedCalibration.getModeledImagePointsList(), heightIndex);

        SimpleGraph sequentialErrorGraph = new SimpleGraph();
        sequentialErrorGraph.setRelativePaddingLeft(0.15);
        SimpleGraph.DataScale dataScale = new SimpleGraph.DataScale("Residual Error");
        dataScale.setRelativePaddingTop(0.05);
        dataScale.setRelativePaddingBottom(0.05);
        dataScale.setSymmetricIfSigned(true);
        dataScale.setColor(Color.GRAY);
        DataRow dataRowX = new SimpleGraph.DataRow("X", Color.RED);
        DataRow dataRowY = new SimpleGraph.DataRow("Y", Color.GREEN);
        int iPoint = 0;
        for (double[] residual : residuals) {
            dataRowX.recordDataPoint(iPoint, residual[0]);
            dataRowY.recordDataPoint(iPoint, residual[1]);
            iPoint++;
        }
        dataScale.addDataRow(dataRowX);
        dataScale.addDataRow(dataRowY);
        sequentialErrorGraph.addDataScale(dataScale);
        modelErrorsTimeSequenceView.setGraph(sequentialErrorGraph);
        
        SimpleGraph scatterErrorGraph = new SimpleGraph();
        scatterErrorGraph.setRelativePaddingLeft(0.15);
        SimpleGraph.DataScale dataScaleScatter = new SimpleGraph.DataScale("Residual Error");
        dataScaleScatter.setRelativePaddingTop(0.05);
        dataScaleScatter.setRelativePaddingBottom(0.05);
        dataScaleScatter.setSymmetricIfSigned(true);
        dataScaleScatter.setSquareAspectRatio(true);
        dataScaleScatter.setColor(Color.GRAY);
        
        DataRow dataRowXY = new SimpleGraph.DataRow("XY", Color.RED);
        dataRowXY.setLineShown(false);
        dataRowXY.setMarkerShown(true);
        iPoint = 0;
        for (double[] residual : residuals) {
            dataRowXY.recordDataPoint(residual[0], residual[1]);
            iPoint++;
        }
        dataScaleScatter.addDataRow(dataRowXY);
        
        DataRow dataRowCircleTop = new SimpleGraph.DataRow("CircleT", Color.GREEN);
        DataRow dataRowCircleBottom = new SimpleGraph.DataRow("CircleB", Color.GREEN);
        double radius = advancedCalibration.getRmsError() * CameraCalibrationUtils.sigmaThresholdForRejectingOutliers;
        for (int i=0; i<=45; i++) {
            dataRowCircleTop.recordDataPoint(radius*Math.cos(i*2*Math.PI/90), radius*Math.sin(i*2*Math.PI/90));
            dataRowCircleBottom.recordDataPoint(radius*Math.cos((i+45)*2*Math.PI/90), radius*Math.sin((i+45)*2*Math.PI/90));
        }
        dataScaleScatter.addDataRow(dataRowCircleTop);
        dataScaleScatter.addDataRow(dataRowCircleBottom);
        
        scatterErrorGraph.addDataScale(dataScaleScatter);
        modelErrorsScatterPlotView.setGraph(scatterErrorGraph);
        
        Mat errorImage = CameraCalibrationUtils.generateErrorImage(new Size(referenceCamera.getWidth(), referenceCamera.getHeight()), heightIndex, 
                advancedCalibration.getSavedTestPatternImagePointsList(), advancedCalibration.getModeledImagePointsList(), advancedCalibration.getOutlierPoints());
        modelErrorsView.setMat(errorImage);
        errorImage.release();

    }

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
    private JLabel lblCaution;
    private JLabel lblCalibrationHeightsHeading;
    private JScrollPane scrollPane;
    private JTextField textFieldRmsError;
    private JLabel lblNewLabel_4;
    private JSeparator separator;
    private JSeparator separator_1;
    private MatView modelErrorsView;
    private JSpinner spinnerIndex;
    private JLabel lblNewLabel_8;
    private VerticalLabel lblNewLabel_9;
    private JLabel lblNewLabel_10;
    private JLabel lblNewLabel_14;
    private JLabel lblNewLabel_15;
    private JLabel lblNewLabel_17;
    private JLabel lblNewLabel_18;
    private JLabel lblNewLabel_19;
    
}
