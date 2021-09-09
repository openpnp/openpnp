package org.openpnp.machine.reference.wizards;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.SimpleGraphView;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.PercentIntegerConverter;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.model.Configuration;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.SimpleGraph;
import org.openpnp.util.UiUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ReferenceCameraTransformsConfigurationWizard extends AbstractConfigurationWizard {
    private final ReferenceCamera referenceCamera;
    private JPanel panelTransforms;
    private JLabel lblRotation;
    private JTextField textFieldRotation;
    private JCheckBox chckbxFlipX;
    private JLabel lblFlipX;
    private JLabel lblFlipY;
    private JCheckBox checkBoxFlipY;
    private JLabel lblOffsetX;
    private JLabel lblOffsetY;
    private JTextField textFieldOffsetX;
    private JTextField textFieldOffsetY;
    private JPanel panelColorBalance;


    public ReferenceCameraTransformsConfigurationWizard(ReferenceCamera referenceCamera) {
        this.referenceCamera = referenceCamera;


        panelTransforms = new JPanel();
        panelTransforms.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
                "Image Transforms", TitledBorder.LEADING, TitledBorder.TOP, null));
        contentPanel.add(panelTransforms);
        panelTransforms.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.LABEL_COMPONENT_GAP_COLSPEC,
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

        lblRotation = new JLabel("Rotation");
        panelTransforms.add(lblRotation, "2, 2, right, default");

        textFieldRotation = new JTextField();
        panelTransforms.add(textFieldRotation, "4, 2");
        textFieldRotation.setColumns(10);

        lblOffsetX = new JLabel("Offset X");
        panelTransforms.add(lblOffsetX, "2, 4, right, default");

        textFieldOffsetX = new JTextField();
        panelTransforms.add(textFieldOffsetX, "4, 4");
        textFieldOffsetX.setColumns(10);

        lblOffsetY = new JLabel("Offset Y");
        panelTransforms.add(lblOffsetY, "2, 6, right, default");

        textFieldOffsetY = new JTextField();
        panelTransforms.add(textFieldOffsetY, "4, 6");
        textFieldOffsetY.setColumns(10);

        lblFlipX = new JLabel("Flip Vertical?");
        panelTransforms.add(lblFlipX, "2, 8, right, default");

        chckbxFlipX = new JCheckBox("");
        panelTransforms.add(chckbxFlipX, "4, 8");

        lblFlipY = new JLabel("Flip Horizontal?");
        panelTransforms.add(lblFlipY, "2, 10, right, default");

        checkBoxFlipY = new JCheckBox("");
        panelTransforms.add(checkBoxFlipY, "4, 10");
        
        lblCropX = new JLabel("Crop Width");
        panelTransforms.add(lblCropX, "2, 12, right, default");
        
        cropWidthTextField = new JTextField();
        panelTransforms.add(cropWidthTextField, "4, 12");
        cropWidthTextField.setColumns(10);
        
        lblNewLabel = new JLabel("(Use 0 for no cropping)");
        panelTransforms.add(lblNewLabel, "7, 12");
        
        lblCropHeight = new JLabel("Crop Height");
        panelTransforms.add(lblCropHeight, "2, 14, right, default");
        
        cropHeightTextField = new JTextField();
        panelTransforms.add(cropHeightTextField, "4, 14");
        cropHeightTextField.setColumns(10);
        
        lblNewLabel_1 = new JLabel("(Use 0 for no cropping)");
        panelTransforms.add(lblNewLabel_1, "7, 14");
        
        lblScaleWidth = new JLabel("Scale Width");
        panelTransforms.add(lblScaleWidth, "2, 16, right, default");
        
        scaleWidthTf = new JTextField();
        panelTransforms.add(scaleWidthTf, "4, 16, fill, default");
        scaleWidthTf.setColumns(10);
        
        lbluseFor = new JLabel("(Use 0 for no scaling)");
        panelTransforms.add(lbluseFor, "7, 16");
        
        lblScaleHeight = new JLabel("Scale Height");
        panelTransforms.add(lblScaleHeight, "2, 18, right, default");
        
        scaleHeightTf = new JTextField();
        panelTransforms.add(scaleHeightTf, "4, 18, fill, default");
        scaleHeightTf.setColumns(10);
        
        label = new JLabel("(Use 0 for no scaling)");
        panelTransforms.add(label, "7, 18");
        
        lblDeinterlace = new JLabel("De-Interlace?");
        panelTransforms.add(lblDeinterlace, "2, 20, right, default");
        
        deinterlaceChk = new JCheckBox("");
        panelTransforms.add(deinterlaceChk, "4, 20");
        
        lblremovesInterlacingFrom = new JLabel("(Removes interlacing from stacked frames)");
        panelTransforms.add(lblremovesInterlacingFrom, "7, 20");
        
        panelColorBalance = new JPanel();
        panelColorBalance.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
                "Color Balance", TitledBorder.LEADING, TitledBorder.TOP, null));
        contentPanel.add(panelColorBalance);
        panelColorBalance.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
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
                RowSpec.decode("max(100dlu;default)"),
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        
        lblRedBalance = new JLabel("Red Balance");
        panelColorBalance.add(lblRedBalance, "2, 2, right, default");
        
        redBalance = new JSlider();
        redBalance.setMajorTickSpacing(100);
        redBalance.setMaximum(200);
        redBalance.setPaintTicks(true);
        redBalance.setValue(100);
        panelColorBalance.add(redBalance, "4, 2, fill, default");
        
        lblGreenBalance = new JLabel("Green Balance");
        panelColorBalance.add(lblGreenBalance, "2, 4, right, default");
        
        greenBalance = new JSlider();
        greenBalance.setPaintTicks(true);
        greenBalance.setValue(100);
        greenBalance.setMaximum(200);
        greenBalance.setMajorTickSpacing(100);
        panelColorBalance.add(greenBalance, "4, 4, fill, default");
        
        lblBlueBalance = new JLabel("Blue Balance");
        panelColorBalance.add(lblBlueBalance, "2, 6, right, default");
        
        blueBalance = new JSlider();
        blueBalance.setValue(100);
        blueBalance.setMajorTickSpacing(100);
        blueBalance.setMaximum(200);
        blueBalance.setPaintTicks(true);
        panelColorBalance.add(blueBalance, "4, 6, fill, default");
        
        lblRedGamma = new JLabel("Red Gamma");
        panelColorBalance.add(lblRedGamma, "2, 10, right, default");
        
        redGammaPercent = new JSlider();
        redGammaPercent.setMajorTickSpacing(100);
        redGammaPercent.setValue(100);
        redGammaPercent.setMaximum(200);
        redGammaPercent.setPaintTicks(true);
        panelColorBalance.add(redGammaPercent, "4, 10, fill, default");
        
        lblGreenGamma = new JLabel("Green Gamma");
        panelColorBalance.add(lblGreenGamma, "2, 12, right, default");
        
        greenGammaPercent = new JSlider();
        greenGammaPercent.setValue(100);
        greenGammaPercent.setPaintTicks(true);
        greenGammaPercent.setMaximum(200);
        greenGammaPercent.setMajorTickSpacing(100);
        panelColorBalance.add(greenGammaPercent, "4, 12, fill, default");
        
        lblBlueGamma = new JLabel("Blue Gamma");
        panelColorBalance.add(lblBlueGamma, "2, 14, right, default");
        
        blueGammaPercent = new JSlider();
        blueGammaPercent.setValue(100);
        blueGammaPercent.setPaintTicks(true);
        blueGammaPercent.setMaximum(200);
        blueGammaPercent.setMajorTickSpacing(100);
        panelColorBalance.add(blueGammaPercent, "4, 14, fill, default");
        
        colorGraph = new SimpleGraphView();
        colorGraph.setPreferredSize(new Dimension(300, 300));
        panelColorBalance.add(colorGraph, "4, 18, 1, 15, left, fill");
        
        lblOutput = new JLabel("Output Level");
        panelColorBalance.add(lblOutput, "2, 18, 1, 15, right, default");
        
        lblAutoWhitebalance = new JLabel("Auto White-Balance");
        panelColorBalance.add(lblAutoWhitebalance, "6, 18, center, default");
        
        btnAutowhitebalance = new JButton(autoWhiteBalanceAction);
        panelColorBalance.add(btnAutowhitebalance, "6, 20, fill, fill");
        
        btnAutowhitebalance_1 = new JButton(autoWhiteBalanceBrightAction);
        panelColorBalance.add(btnAutowhitebalance_1, "6, 22, default, fill");
        
        btnAutowhiteBalanceAdvanced = new JButton(autoWhiteBalanceRoughMapAction);
        panelColorBalance.add(btnAutowhiteBalanceAdvanced, "6, 26, default, fill");
        
        btnAutowhiteBalanceAdvanced_1 = new JButton(autoWhiteBalanceFineMapAction);
        panelColorBalance.add(btnAutowhiteBalanceAdvanced_1, "6, 28, default, fill");
        
        btnReset = new JButton(resetAction);
        panelColorBalance.add(btnReset, "6, 32");
        
        lblInputLevel = new JLabel("Input Level");
        panelColorBalance.add(lblInputLevel, "4, 34, center, default");
        initDataBindings();
    }

    @Override
    public void createBindings() {
        IntegerConverter intConverter = new IntegerConverter();
        DoubleConverter doubleConverter =
                new DoubleConverter(Configuration.get().getLengthDisplayFormat());

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


        ComponentDecorators.decorateWithAutoSelect(textFieldRotation);
        ComponentDecorators.decorateWithAutoSelect(textFieldOffsetX);
        ComponentDecorators.decorateWithAutoSelect(textFieldOffsetY);

        ComponentDecorators.decorateWithAutoSelect(cropWidthTextField);
        ComponentDecorators.decorateWithAutoSelect(cropHeightTextField);
        ComponentDecorators.decorateWithAutoSelect(scaleWidthTf);
        ComponentDecorators.decorateWithAutoSelect(scaleHeightTf);
    }

    private final Action autoWhiteBalanceAction = new AbstractAction() {
        {
            putValue(NAME, "Overall");
            putValue(SHORT_DESCRIPTION, 
                    "<html>Hold a neutral bright object in front of the camera and<br/>"
                    + "press this button to automatically calibrate the white-balance.<br/><br/>"
                    + "This method looks at where the bulk of the color is distributed,<br/>"
                    + "overall.</html>");
        }

        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                referenceCamera.autoAdjustWhiteBalance(false);
                CameraView cameraView = MainFrame.get().getCameraViews().getCameraView(referenceCamera);
                cameraView.setShowImageInfo(true);
                MovableUtils.fireTargetedUserAction(referenceCamera);
            });
        }
    };

    private final Action autoWhiteBalanceBrightAction = new AbstractAction() {
        {
            putValue(NAME, "Brightest");
            putValue(SHORT_DESCRIPTION, 
                    "<html>Hold a neutral bright object in front of the camera and<br/>"
                    + "press this button to automatically calibrate the white-balance.<br/><br/>"
                    + "This method looks at the brightest parts and averages the color<br/>"
                    + "in those parts.</html>");
        }

        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                referenceCamera.autoAdjustWhiteBalance(true);
                CameraView cameraView = MainFrame.get().getCameraViews().getCameraView(referenceCamera);
                cameraView.setShowImageInfo(true);
                MovableUtils.fireTargetedUserAction(referenceCamera);
            });
        }
    };

    private final Action autoWhiteBalanceRoughMapAction = new AbstractAction() {
        {
            putValue(NAME, "Mapped Roughly");
            putValue(SHORT_DESCRIPTION, 
                    "<html>Hold a gradiented gray object in front of the camera and<br/>"
                    + "press this button to automatically calibrate the white-balance.<br/><br/>"
                    + "This method uses a rough mapped approach, the image must contain all shades.</html>");
        }

        public void actionPerformed(ActionEvent e) {
            autoMapped(8);
        }

    };

    private final Action autoWhiteBalanceFineMapAction = new AbstractAction() {
        {
            putValue(NAME, "Mapped Finely");
            putValue(SHORT_DESCRIPTION, 
                    "<html>Hold a gradiented gray object in front of the camera and<br/>"
                    + "press this button to automatically calibrate the white-balance.<br/><br/>"
                    + "This method uses a fine mapped approach, the image must contain all shades.</html>");
        }

        public void actionPerformed(ActionEvent e) {
            autoMapped(32);
        }
    };

    protected void autoMapped(int levels) {
        UiUtils.messageBoxOnException(() -> {
            referenceCamera.autoAdjustWhiteBalanceMapped(levels);
            CameraView cameraView = MainFrame.get().getCameraViews().getCameraView(referenceCamera);
            cameraView.setShowImageInfo(true);
            MovableUtils.fireTargetedUserAction(referenceCamera);
        });
    }

    private final Action resetAction = new AbstractAction() {
        {
            putValue(NAME, "Reset");
            putValue(SHORT_DESCRIPTION, "Switch off the white-balance / Reset to neutral.");
        }

        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                referenceCamera.resetWhiteBalance();
                CameraView cameraView = MainFrame.get().getCameraViews().getCameraView(referenceCamera);
                cameraView.setShowImageInfo(true);
            });
        }
    };

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
    private JLabel lblRedBalance;
    private JLabel lblGreenBalance;
    private JLabel lblBlueBalance;
    private JSlider redBalance;
    private JSlider greenBalance;
    private JSlider blueBalance;
    private JButton btnAutowhitebalance;
    private JButton btnReset;
    private JButton btnAutowhitebalance_1;
    private JLabel lblRedGamma;
    private JLabel lblGreenGamma;
    private JLabel lblBlueGamma;
    private JSlider redGammaPercent;
    private JSlider greenGammaPercent;
    private JSlider blueGammaPercent;
    private JButton btnAutowhiteBalanceAdvanced;
    private SimpleGraphView colorGraph;
    private JButton btnAutowhiteBalanceAdvanced_1;
    private JLabel lblOutput;
    private JLabel lblInputLevel;
    private JLabel lblAutoWhitebalance;

    protected void initDataBindings() {
        BeanProperty<ReferenceCamera, Double> referenceCameraBeanProperty = BeanProperty.create("redBalance");
        BeanProperty<JSlider, Integer> jSliderBeanProperty = BeanProperty.create("value");
        AutoBinding<ReferenceCamera, Double, JSlider, Integer> autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, referenceCamera, referenceCameraBeanProperty, redBalance, jSliderBeanProperty);
        autoBinding.setConverter(new PercentIntegerConverter());
        autoBinding.bind();
        //
        BeanProperty<ReferenceCamera, Double> referenceCameraBeanProperty_1 = BeanProperty.create("greenBalance");
        BeanProperty<JSlider, Integer> jSliderBeanProperty_1 = BeanProperty.create("value");
        AutoBinding<ReferenceCamera, Double, JSlider, Integer> autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, referenceCamera, referenceCameraBeanProperty_1, greenBalance, jSliderBeanProperty_1);
        autoBinding_1.setConverter(new PercentIntegerConverter());
        autoBinding_1.bind();
        //
        BeanProperty<ReferenceCamera, Double> referenceCameraBeanProperty_2 = BeanProperty.create("blueBalance");
        BeanProperty<JSlider, Integer> jSliderBeanProperty_2 = BeanProperty.create("value");
        AutoBinding<ReferenceCamera, Double, JSlider, Integer> autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, referenceCamera, referenceCameraBeanProperty_2, blueBalance, jSliderBeanProperty_2);
        autoBinding_2.setConverter(new PercentIntegerConverter());
        autoBinding_2.bind();
        //
        BeanProperty<ReferenceCamera, Double> referenceCameraBeanProperty_3 = BeanProperty.create("redGamma");
        BeanProperty<JSlider, Integer> jSliderBeanProperty_3 = BeanProperty.create("value");
        AutoBinding<ReferenceCamera, Double, JSlider, Integer> autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, referenceCamera, referenceCameraBeanProperty_3, redGammaPercent, jSliderBeanProperty_3);
        autoBinding_3.setConverter(new PercentIntegerConverter());
        autoBinding_3.bind();
        //
        BeanProperty<ReferenceCamera, Double> referenceCameraBeanProperty_4 = BeanProperty.create("greenGamma");
        BeanProperty<JSlider, Integer> jSliderBeanProperty_4 = BeanProperty.create("value");
        AutoBinding<ReferenceCamera, Double, JSlider, Integer> autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, referenceCamera, referenceCameraBeanProperty_4, greenGammaPercent, jSliderBeanProperty_4);
        autoBinding_4.setConverter(new PercentIntegerConverter());
        autoBinding_4.bind();
        //
        BeanProperty<ReferenceCamera, Double> referenceCameraBeanProperty_5 = BeanProperty.create("blueGamma");
        BeanProperty<JSlider, Integer> jSliderBeanProperty_5 = BeanProperty.create("value");
        AutoBinding<ReferenceCamera, Double, JSlider, Integer> autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, referenceCamera, referenceCameraBeanProperty_5, blueGammaPercent, jSliderBeanProperty_5);
        autoBinding_5.setConverter(new PercentIntegerConverter());
        autoBinding_5.bind();
        //
        BeanProperty<ReferenceCamera, SimpleGraph> referenceCameraBeanProperty_6 = BeanProperty.create("colorBalanceGraph");
        BeanProperty<SimpleGraphView, SimpleGraph> simpleGraphViewBeanProperty = BeanProperty.create("graph");
        AutoBinding<ReferenceCamera, SimpleGraph, SimpleGraphView, SimpleGraph> autoBinding_6 = Bindings.createAutoBinding(UpdateStrategy.READ, referenceCamera, referenceCameraBeanProperty_6, colorGraph, simpleGraphViewBeanProperty);
        autoBinding_6.bind();
    }
}
