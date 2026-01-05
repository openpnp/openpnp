package org.openpnp.machine.reference.camera.wizards;

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.machine.reference.camera.ParallaxPartHeightProvider;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.util.UiUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ParallaxPartHeightProviderConfigurationWizard extends AbstractConfigurationWizard {
    private ParallaxPartHeightProvider provider;
    private Camera camera;

    // Calibration State
    private double p1_mm = -1;
    private double p2_mm = -1;

    public ParallaxPartHeightProviderConfigurationWizard(Camera camera, ParallaxPartHeightProvider provider) {
        this.camera = camera;
        this.provider = provider;

        panelGeneral = new JPanel();
        contentPanel.add(panelGeneral);
        panelGeneral.setBorder(new TitledBorder(null, "Parallax Settings", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        panelGeneral.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(50dlu;default)"),
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

        lblShiftDistance = new JLabel("Shift Distance");
        lblShiftDistance.setToolTipText("Distance to move the camera sideways to create parallax.");
        panelGeneral.add(lblShiftDistance, "2, 2, right, default");

        shiftDistance = new JTextField();
        panelGeneral.add(shiftDistance, "4, 2, fill, default");
        shiftDistance.setColumns(10);

        lblFeatureSize = new JLabel("Feature Size");
        lblFeatureSize.setToolTipText("Size of the template image to match (pixels).");
        panelGeneral.add(lblFeatureSize, "2, 4, right, default");

        featureSize = new JTextField();
        panelGeneral.add(featureSize, "4, 4, fill, default");
        featureSize.setColumns(10);



        lblFocalPointZ = new JLabel("Focal Point Z");
        lblFocalPointZ.setToolTipText("The Z coordinate of the camera's perspective center.");
        panelGeneral.add(lblFocalPointZ, "2, 8, right, default");

        focalPointZ = new JTextField();
        panelGeneral.add(focalPointZ, "4, 8, fill, default");
        focalPointZ.setColumns(10);

        lblShowDiagnostics = new JLabel("Show Diagnostics?");
        panelGeneral.add(lblShowDiagnostics, "2, 10, right, default");

        showDiagnostics = new JCheckBox("");
        panelGeneral.add(showDiagnostics, "4, 10");

        btnTest = new JButton(testAction);
        panelGeneral.add(btnTest, "4, 12");
        
        // Calibration Panel
        panelCalibration = new JPanel();
        contentPanel.add(panelCalibration);
        panelCalibration.setBorder(new TitledBorder(null, "Calibration", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        panelCalibration.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(100dlu;default):grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC, // Instr 1
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC, // Input Z1
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC, // Btn 1 + Result 1
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC, // Instr 2
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC, // Input height
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC, // Btn 2 + Result 2
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC, // Final Result
                        }));
        
        JLabel lblInstr1 = new JLabel("<html><b>Step 1:</b> Center the camera on a distinct feature on the PCB surface (Low Z).</html>");
        panelCalibration.add(lblInstr1, "2, 2, 5, 1");
        
        JLabel lblLowZ = new JLabel("Surface Z (mm):");
        panelCalibration.add(lblLowZ, "2, 4, right, default");
        
        txtLowZ = new JTextField("0.0");
        panelCalibration.add(txtLowZ, "4, 4, fill, default");
        
        btnStep1 = new JButton(step1Action);
        panelCalibration.add(btnStep1, "2, 6");
        
        lblP1 = new JLabel("P1: Not measured");
        panelCalibration.add(lblP1, "4, 6");
        
        JLabel lblInstr2 = new JLabel("<html><b>Step 2:</b> Place a block of known height. Center the camera on a feature on top of the block (High Z).</html>");
        panelCalibration.add(lblInstr2, "2, 8, 5, 1");
        
        JLabel lblBlockHeight = new JLabel("Block Height (mm):");
        panelCalibration.add(lblBlockHeight, "2, 10, right, default");
        
        txtBlockHeight = new JTextField("5.0");
        panelCalibration.add(txtBlockHeight, "4, 10, fill, default");
        
        btnStep2 = new JButton(step2Action);
        panelCalibration.add(btnStep2, "2, 12");
        step2Action.setEnabled(false);
        
        lblP2 = new JLabel("P2: Not measured");
        panelCalibration.add(lblP2, "4, 12");
        
        lblResult = new JLabel("");
        panelCalibration.add(lblResult, "2, 14, 5, 1");
    }

    @Override
    public void createBindings() {
        DoubleConverter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        LengthConverter lengthConverter = new LengthConverter();
        IntegerConverter intConverter = new IntegerConverter();

        addWrappedBinding(provider, "shiftDistance", shiftDistance, "text", lengthConverter);
        addWrappedBinding(provider, "featureSize", featureSize, "text", intConverter);

        addWrappedBinding(provider, "focalPointZ", focalPointZ, "text", doubleConverter);
        addWrappedBinding(provider, "showDiagnostics", showDiagnostics, "selected");

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(shiftDistance);
        ComponentDecorators.decorateWithAutoSelect(featureSize);

        ComponentDecorators.decorateWithAutoSelect(focalPointZ);
        ComponentDecorators.decorateWithLengthConversion(txtBlockHeight);
        ComponentDecorators.decorateWithLengthConversion(txtLowZ);
    }
    
    private Action step1Action = new AbstractAction("Measure Low Z") {
        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.submitUiMachineTask(() -> {
                try {
                    Location loc = camera.getLocation();
                    double p1 = measureParallaxMm(loc);
                    
                    SwingUtilities.invokeLater(() -> {
                        p1_mm = p1;
                        lblP1.setText(String.format("P1: %.3f mm", p1_mm));
                        lblP1.setForeground(new Color(0, 150, 0));
                        step2Action.setEnabled(true);
                        lblResult.setText("");
                    });
                } catch (Exception ex) {
                    MessageBoxes.errorBox(getTopLevelAncestor(), "Error", ex);
                }
            });
        }
    };
    
    private Action step2Action = new AbstractAction("Measure High Z") {
        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            if (p1_mm < 0) {
                 MessageBoxes.errorBox(getTopLevelAncestor(), "Step 1 incomplete", "Please perform Step 1 first.");
                 return;
            }
            UiUtils.submitUiMachineTask(() -> {
                try {
                    Location loc = camera.getLocation();
                    double p2 = measureParallaxMm(loc);
                    
                    double dZ_val = Double.parseDouble(txtBlockHeight.getText());
                    if (dZ_val <= 0) {
                        throw new Exception("Block height must be positive.");
                    }
                    
                    double z1_val = Double.parseDouble(txtLowZ.getText());
                    
                    // Calculation logic:
                    // Zc = Z1 + H1
                    // H1 = dZ * P2 / (P2 - P1)
                    // Zc = Z1 + dZ * P2 / (P2 - P1)
                    
                    // Note: This assumes P increases as object gets closer (larger shift).
                    // Closer object = larger parallax shift.
                    // So P2 should be > P1.
                    
                    if (p2 <= p1_mm) {
                         throw new Exception("Parallax at High Z (P2) must be greater than Low Z (P1). Ensure shift is sufficient and direction is correct.");
                    }
                    
                    double h1 = dZ_val * p2 / (p2 - p1_mm);
                    double calculatedZc = z1_val + h1;
                    
                    SwingUtilities.invokeLater(() -> {
                        p2_mm = p2;
                        lblP2.setText(String.format("P2: %.3f mm", p2_mm));
                        lblP2.setForeground(new Color(0, 150, 0));
                        
                        focalPointZ.setText(String.format("%.4f", calculatedZc));
                        
                        lblResult.setText("<html><b>Success!</b> Calculated Focal Z: " + String.format("%.2f", calculatedZc) + "</html>");
                        lblResult.setForeground(new Color(0, 128, 0));
                        
                        provider.setFocalPointZ(calculatedZc);
                    });
                    
                } catch (Exception ex) {
                    MessageBoxes.errorBox(getTopLevelAncestor(), "Error", ex);
                }
            });
        }
    };
    
    // Helper to measure using Provider logic
    private double measureParallaxMm(Location startLocation) throws Exception {
         return provider.measureParallaxShift(camera, (org.openpnp.spi.HeadMountable)camera, startLocation);
    }

    private Action testAction = new AbstractAction("Test Part Height") {
        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.submitUiMachineTask(() -> {
                try {
                Location loc = provider.autoFocus(camera, (org.openpnp.spi.HeadMountable)camera, 
                        new Length(10, LengthUnit.Millimeters), camera.getLocation(), camera.getLocation());
                
                SwingUtilities.invokeLater(() -> {
                    MessageBoxes.infoBox("Result", "Estimated Height: " + loc.getZ());
                });
                } catch (Exception ex) {
                    MessageBoxes.errorBox(getTopLevelAncestor(), "Error", ex);
                }
            });
        }
    };

    private JPanel panelGeneral;
    private JLabel lblShiftDistance;
    private JTextField shiftDistance;
    private JLabel lblFeatureSize;
    private JTextField featureSize;

    private JLabel lblFocalPointZ;
    private JTextField focalPointZ;
    private JLabel lblShowDiagnostics;
    private JCheckBox showDiagnostics;
    private JButton btnTest;
    
    private JPanel panelCalibration;
    private JLabel lblP1;
    private JLabel lblP2;
    private JLabel lblResult;
    private JButton btnStep1;
    private JButton btnStep2;
    private JTextField txtBlockHeight;
    private JTextField txtLowZ;
}
