package org.openpnp.machine.reference.wizards;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.reference.SimulationModeMachine;
import org.openpnp.machine.reference.SimulationModeMachine.SimulationMode;
import org.openpnp.model.Configuration;
import org.python.modules.cPickle.Pickler;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import javax.swing.JCheckBox;

public class SimulationModeMachineConfigurationWizard extends AbstractConfigurationWizard {

    private final SimulationModeMachine machine;
    private JTextField homingErrorX;
    private JTextField homingErrorY;
    private JTextField simulatedNonSquarenessFactor;
    private JTextField simulatedRunout;
    private JTextField simulatedCameraNoise;
    private JTextField simulatedVibrationAmplitude;
    private JComboBox simulationMode;
    private JTextField simulatedRunoutPhase;
    private JCheckBox pickAndPlaceChecking;

    public SimulationModeMachineConfigurationWizard(SimulationModeMachine machine) {
        this.machine = machine;

        JPanel panelGeneral = new JPanel();
        contentPanel.add(panelGeneral);
        panelGeneral.setBorder(new TitledBorder(null, "General", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        panelGeneral.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(80dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(50dlu;default)"),},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblSimulationMode = new JLabel("Simulation Mode");
        panelGeneral.add(lblSimulationMode, "2, 2, right, default");

        simulationMode = new JComboBox(SimulationMode.values());
        panelGeneral.add(simulationMode, "4, 2, fill, default");

        JPanel panelLocations = new JPanel();
        panelLocations.setBorder(new TitledBorder(null, "Simulated Imperfections", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        contentPanel.add(panelLocations);
        panelLocations.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(80dlu;default)"),
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
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                RowSpec.decode("default:grow"),}));

        JLabel lblNonsquarenessFactor = new JLabel("Non-Squareness Factor");
        lblNonsquarenessFactor.setToolTipText("Creates simulated Non-Squareness by that factor. ");
        panelLocations.add(lblNonsquarenessFactor, "2, 2, right, default");

        simulatedNonSquarenessFactor = new JTextField();
        panelLocations.add(simulatedNonSquarenessFactor, "4, 2");
        simulatedNonSquarenessFactor.setColumns(10);

        JLabel lblNozzleTipRunout = new JLabel("Nozzle Tip Runout");
        lblNozzleTipRunout.setToolTipText("Simulates runout of that radius on all nozzle tips.");
        panelLocations.add(lblNozzleTipRunout, "2, 4, right, default");

        simulatedRunout = new JTextField();
        panelLocations.add(simulatedRunout, "4, 4");
        simulatedRunout.setColumns(10);
        
        JLabel lblPickPlace = new JLabel("Pick & Place Checking?");
        panelLocations.add(lblPickPlace, "2, 6, right, default");
        
        pickAndPlaceChecking = new JCheckBox("");
        panelLocations.add(pickAndPlaceChecking, "4, 6");
        
        JLabel lblRunoutPhase = new JLabel("Runout Phase");
        lblRunoutPhase.setToolTipText("Phase angle for the simulated runout.");
        panelLocations.add(lblRunoutPhase, "2, 8, right, default");
        
        simulatedRunoutPhase = new JTextField();
        panelLocations.add(simulatedRunoutPhase, "4, 8, fill, default");
        simulatedRunoutPhase.setColumns(10);

        JLabel lblVibrationAmplitude = new JLabel("Vibration Amplitude");
        lblVibrationAmplitude.setToolTipText("Simulates Vibration after a move that will abate quickly. This is the amplitude.");
        panelLocations.add(lblVibrationAmplitude, "2, 10, right, default");

        simulatedVibrationAmplitude = new JTextField();
        panelLocations.add(simulatedVibrationAmplitude, "4, 10, fill, default");
        simulatedVibrationAmplitude.setColumns(10);

        JLabel lblCameraNoise = new JLabel("Camera Noise");
        lblCameraNoise.setToolTipText("<html>\r\nCreates simulated noise in the camera image (number of sparks) <br/>\r\nto satisfy Camera Settling that the frame has changed. \r\n</html>");
        panelLocations.add(lblCameraNoise, "2, 12, right, default");

        simulatedCameraNoise = new JTextField();
        panelLocations.add(simulatedCameraNoise, "4, 12");
        simulatedCameraNoise.setColumns(10);

        JLabel lblX = new JLabel("X");
        panelLocations.add(lblX, "4, 16");
        lblX.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel lblY = new JLabel("Y");
        panelLocations.add(lblY, "6, 16");
        lblY.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel lblDiscardPoint = new JLabel("Homing Error");
        lblDiscardPoint.setToolTipText("<html>\r\nSimulates an initial homing error by that offset. Used to test visial homing. <br/>\r\nSet the homing fiducial to the PCB fiducial in the lower left corner of the test image.<br/>\r\nUse coordinates 5.736, 6.112 to get original coordinates through Visual homing.\r\n</html>");
        panelLocations.add(lblDiscardPoint, "2, 18, right, default");

        homingErrorX = new JTextField();
        panelLocations.add(homingErrorX, "4, 18");
        homingErrorX.setColumns(10);

        homingErrorY = new JTextField();
        panelLocations.add(homingErrorY, "6, 18");
        homingErrorY.setColumns(10);
        
        JLabel label = new JLabel(" ");
        panelLocations.add(label, "2, 20");
    }

    @Override
    public void createBindings() {
        DoubleConverter doubleConverter =
                new DoubleConverter("%f");
        DoubleConverter degreeConverter =
                new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        IntegerConverter integerConverter =
                new IntegerConverter();
        LengthConverter lengthConverter = new LengthConverter();

        addWrappedBinding(machine, "simulationMode", simulationMode, "selectedItem");

        addWrappedBinding(machine, "simulatedNonSquarenessFactor", simulatedNonSquarenessFactor, "text", doubleConverter);

        addWrappedBinding(machine, "simulatedRunout", simulatedRunout, "text", lengthConverter);
        addWrappedBinding(machine, "simulatedRunoutPhase", simulatedRunoutPhase, "text", degreeConverter);
        addWrappedBinding(machine, "pickAndPlaceChecking", pickAndPlaceChecking, "selected");
        addWrappedBinding(machine, "simulatedVibrationAmplitude", simulatedVibrationAmplitude, "text", lengthConverter);
        addWrappedBinding(machine, "simulatedCameraNoise", simulatedCameraNoise, "text", integerConverter);

        MutableLocationProxy homingError = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, machine, "homingError", homingError, "location");
        addWrappedBinding(homingError, "lengthX", homingErrorX, "text", lengthConverter);
        addWrappedBinding(homingError, "lengthY", homingErrorY, "text", lengthConverter);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(homingErrorX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(homingErrorY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(homingErrorX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(homingErrorY);
    }
}
