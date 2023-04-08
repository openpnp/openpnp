package org.openpnp.machine.reference.wizards;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.Translations;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.machine.reference.camera.ReferenceCamera;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ReferenceCameraCalibrationConfigurationWizard extends AbstractConfigurationWizard {
    private final ReferenceCamera referenceCamera;
    private JPanel panelLensCalibration;
    private JLabel lblApplyCalibration;
    private JCheckBox calibrationEnabledChk;


    public ReferenceCameraCalibrationConfigurationWizard(ReferenceCamera referenceCamera) {
        this.referenceCamera = referenceCamera;

        panelLensCalibration = new JPanel();
        panelLensCalibration.setBorder(new TitledBorder(null, Translations.getString(
                "ReferenceCameraCalibrationConfigurationWizard.LensCalibrationPanel.Border.title"), //$NON-NLS-1$
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelLensCalibration);
        panelLensCalibration.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
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

        startLensCalibrationBtn = new JButton(startCalibration);
        panelLensCalibration.add(startLensCalibrationBtn, "2, 2, 3, 1");
        
        advancedCalWarning = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationConfigurationWizard.LensCalibrationPanel.AdvancedCalibrationActiveLabel.text" //$NON-NLS-1$
        ));
        advancedCalWarning.setForeground(Color.RED);
        panelLensCalibration.add(advancedCalWarning, "8, 2, left, default");

        lblApplyCalibration = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationConfigurationWizard.LensCalibrationPanel.ApplyCalibrationLabel.text")); //$NON-NLS-1$
        panelLensCalibration.add(lblApplyCalibration, "2, 4, right, default");

        calibrationEnabledChk = new JCheckBox("");
        panelLensCalibration.add(calibrationEnabledChk, "4, 4");
    }

    public boolean isOverriddenClassicTransforms() {
        return calibrationEnabledChk.isEnabled();
    }

    public void setOverriddenClassicTransforms(boolean overriddenClassicTransforms) {
        for (Component comp : panelLensCalibration.getComponents()) {
            comp.setEnabled(!overriddenClassicTransforms);
        }
        advancedCalWarning.setVisible(overriddenClassicTransforms);
        advancedCalWarning.setEnabled(overriddenClassicTransforms);
    }

    @Override
    public void createBindings() {
        addWrappedBinding(referenceCamera.getAdvancedCalibration(), "overridingOldTransformsAndDistortionCorrectionSettings", 
                this, "overriddenClassicTransforms");

        bind(UpdateStrategy.READ_WRITE, referenceCamera.getCalibration(), "enabled",
                calibrationEnabledChk, "selected");
        // addWrappedBinding(referenceCamera.getCalibration(), "enabled", calibrationEnabledChk,
        // "selected");
    }

    private Action startCalibration = new AbstractAction(Translations.getString(
            "ReferenceCameraCalibrationConfigurationWizard.Action.StartCalibration")) { //$NON-NLS-1$
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

    private Action cancelCalibration = new AbstractAction(Translations.getString(
            "ReferenceCameraCalibrationConfigurationWizard.Action.CancelCalibration")) { //$NON-NLS-1$
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
    private JLabel advancedCalWarning;
}
