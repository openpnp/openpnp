package org.openpnp.machine.reference.wizards;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.openpnp.Translations;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.model.Configuration;

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


    public ReferenceCameraTransformsConfigurationWizard(ReferenceCamera referenceCamera) {
        this.referenceCamera = referenceCamera;


        panelTransforms = new JPanel();
        panelTransforms.setBorder(new TitledBorder(null,
                Translations.getString(
                        "ReferenceCameraTransformsConfigurationWizard.TransformsPanel.Border.title"),
                TitledBorder.LEADING, TitledBorder.TOP, null));
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

        lblRotation = new JLabel(Translations.getString(
                "ReferenceCameraTransformsConfigurationWizard.TransformsPanel.RotationLabel.text"));
        panelTransforms.add(lblRotation, "2, 2, right, default");

        textFieldRotation = new JTextField();
        panelTransforms.add(textFieldRotation, "4, 2");
        textFieldRotation.setColumns(10);
        
        advancedCalWarning = new JLabel(Translations.getString(
                "ReferenceCameraTransformsConfigurationWizard.TransformsPanel.AdvancedCalibrationActiveLabel.text"
        ));
        advancedCalWarning.setForeground(Color.RED);
        panelTransforms.add(advancedCalWarning, "7, 2, right, default");

        lblOffsetX = new JLabel(Translations.getString(
                "ReferenceCameraTransformsConfigurationWizard.TransformsPanel.OffsetXLabel.text"));
        panelTransforms.add(lblOffsetX, "2, 4, right, default");

        textFieldOffsetX = new JTextField();
        panelTransforms.add(textFieldOffsetX, "4, 4");
        textFieldOffsetX.setColumns(10);

        lblOffsetY = new JLabel(Translations.getString(
                "ReferenceCameraTransformsConfigurationWizard.TransformsPanel.OffsetYLabel.text"));
        panelTransforms.add(lblOffsetY, "2, 6, right, default");

        textFieldOffsetY = new JTextField();
        panelTransforms.add(textFieldOffsetY, "4, 6");
        textFieldOffsetY.setColumns(10);

        lblFlipX = new JLabel(Translations.getString(
                "ReferenceCameraTransformsConfigurationWizard.TransformsPanel.FlipVerticalLabel.text"));
        panelTransforms.add(lblFlipX, "2, 8, right, default");

        chckbxFlipX = new JCheckBox("");
        panelTransforms.add(chckbxFlipX, "4, 8");

        lblFlipY = new JLabel(Translations.getString(
                "ReferenceCameraTransformsConfigurationWizard.TransformsPanel.FlipHorizontalLabel.text"));
        panelTransforms.add(lblFlipY, "2, 10, right, default");

        checkBoxFlipY = new JCheckBox("");
        panelTransforms.add(checkBoxFlipY, "4, 10");
        
        lblCropX = new JLabel(Translations.getString(
                "ReferenceCameraTransformsConfigurationWizard.TransformsPanel.CropWidthLabel.text"));
        panelTransforms.add(lblCropX, "2, 12, right, default");
        
        cropWidthTextField = new JTextField();
        panelTransforms.add(cropWidthTextField, "4, 12");
        cropWidthTextField.setColumns(10);
        
        lblNewLabel = new JLabel(Translations.getString(
                "ReferenceCameraTransformsConfigurationWizard.TransformsPanel.Use0ForNoCropping1Label.text"));
        panelTransforms.add(lblNewLabel, "7, 12");
        
        lblCropHeight = new JLabel(Translations.getString(
                "ReferenceCameraTransformsConfigurationWizard.TransformsPanel.CropHeightLabel.text"));
        panelTransforms.add(lblCropHeight, "2, 14, right, default");
        
        cropHeightTextField = new JTextField();
        panelTransforms.add(cropHeightTextField, "4, 14");
        cropHeightTextField.setColumns(10);
        
        lblNewLabel_1 = new JLabel(Translations.getString(
                "ReferenceCameraTransformsConfigurationWizard.TransformsPanel.Use0ForNoCropping2Label.text"));
        panelTransforms.add(lblNewLabel_1, "7, 14");
        
        lblScaleWidth = new JLabel(Translations.getString(
                "ReferenceCameraTransformsConfigurationWizard.TransformsPanel.ScaleWidthLabel.text"));
        panelTransforms.add(lblScaleWidth, "2, 16, right, default");
        
        scaleWidthTf = new JTextField();
        panelTransforms.add(scaleWidthTf, "4, 16, fill, default");
        scaleWidthTf.setColumns(10);
        
        lbluseFor = new JLabel(Translations.getString(
                "ReferenceCameraTransformsConfigurationWizard.TransformsPanel.Use0ForNoScaling1Label.text"));
        panelTransforms.add(lbluseFor, "7, 16");
        
        lblScaleHeight = new JLabel(Translations.getString(
                "ReferenceCameraTransformsConfigurationWizard.TransformsPanel.ScaleHeightLabel.text"));
        panelTransforms.add(lblScaleHeight, "2, 18, right, default");
        
        scaleHeightTf = new JTextField();
        panelTransforms.add(scaleHeightTf, "4, 18, fill, default");
        scaleHeightTf.setColumns(10);
        
        label = new JLabel(Translations.getString(
                "ReferenceCameraTransformsConfigurationWizard.TransformsPanel.Use0ForNoScaling2Label.text"));
        panelTransforms.add(label, "7, 18");
        
        lblDeinterlace = new JLabel(Translations.getString(
                "ReferenceCameraTransformsConfigurationWizard.TransformsPanel.DeInterlaceLabel.text"));
        panelTransforms.add(lblDeinterlace, "2, 20, right, default");
        
        deinterlaceChk = new JCheckBox("");
        panelTransforms.add(deinterlaceChk, "4, 20");
        
        lblremovesInterlacingFrom = new JLabel(Translations.getString(
                "ReferenceCameraTransformsConfigurationWizard.TransformsPanel.RemoveInterlacingLabel.text"));
        panelTransforms.add(lblremovesInterlacingFrom, "7, 20");
    }

    public boolean isOverriddenClassicTransforms() {
        return textFieldRotation.isEnabled();
    }

    public void setOverriddenClassicTransforms(boolean overriddenClassicTransforms) {
        for (Component comp : panelTransforms.getComponents()) {
            comp.setEnabled(!overriddenClassicTransforms);
        }
        advancedCalWarning.setVisible(overriddenClassicTransforms);
        advancedCalWarning.setEnabled(overriddenClassicTransforms);
    }

    @Override
    public void createBindings() {
        addWrappedBinding(referenceCamera.getAdvancedCalibration(), "overridingOldTransformsAndDistortionCorrectionSettings", 
                this, "overriddenClassicTransforms");

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
    private JLabel advancedCalWarning;

}
