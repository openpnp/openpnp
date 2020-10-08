package org.openpnp.machine.reference.driver.wizards;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.machine.reference.driver.GcodeAsyncDriver;
import org.openpnp.machine.reference.driver.GcodeDriver;
import org.openpnp.model.Configuration;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class GcodeAsyncDriverSettings extends AbstractConfigurationWizard {
    private final GcodeDriver driver;
    private JCheckBox confirmationFlowControl;
    private JTextField interpolationTimeStep;
    private JTextField interpolationMinStep;
    private JTextField interpolationMaxSteps;
    private JTextField junctionDeviation;
    private JTextField interpolationJerkSteps;

    public GcodeAsyncDriverSettings(GcodeAsyncDriver driver) {
        this.driver = driver;

        JPanel settingsPanel = new JPanel();
        settingsPanel.setBorder(new TitledBorder(null, "Settings", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(settingsPanel);
        settingsPanel.setLayout(new FormLayout(new ColumnSpec[] {
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
                FormSpecs.DEFAULT_ROWSPEC,}));
        JPanel interpolationPanel = new JPanel();
        interpolationPanel.setBorder(new TitledBorder(null, "Interpolation Motion Control", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(interpolationPanel);
        interpolationPanel.setLayout(new FormLayout(new ColumnSpec[] {
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
                RowSpec.decode("fill:default:grow"),}));

        JLabel lblMaximumNumberOf = new JLabel("Maximum Number of Steps");
        lblMaximumNumberOf.setToolTipText("<html>\r\nMaximum number of steps that can be used for interpolation of one move.<br>\r\nUse a portion of your controller's maximum queue depth. <br/>\r\nIf the number is exceeded, the motion planner will fall back to a single moderated move. \r\n</html>");
        interpolationPanel.add(lblMaximumNumberOf, "2, 2, right, default");

        interpolationMaxSteps = new JTextField();
        interpolationPanel.add(interpolationMaxSteps, "4, 2");
        interpolationMaxSteps.setColumns(10);
        
        JLabel lblMaxumNumberOf = new JLabel("Maximum Number of Jerk Steps");
        lblMaxumNumberOf.setToolTipText("<html>\r\nMaximum number of interpolation steps used to simulate jerk control.<br/>\r\nThis means the acceleration will be ramped up or down in so many steps <br/>\r\nrelative to maximum acceleration.\r\n</html>");
        interpolationPanel.add(lblMaxumNumberOf, "2, 4, right, default");
        
        interpolationJerkSteps = new JTextField();
        interpolationPanel.add(interpolationJerkSteps, "4, 4, fill, default");
        interpolationJerkSteps.setColumns(10);

        JLabel lblInterpolationTimeStep = new JLabel("Minimum Step Time [s]");
        interpolationPanel.add(lblInterpolationTimeStep, "2, 6, right, default");
        lblInterpolationTimeStep.setToolTipText("<html>\r\n<p>The minimal time step used to interpolate advanced motion paths. Specified in seconds.</p>\r\n</html>\r\n");

        interpolationTimeStep = new JTextField();
        interpolationPanel.add(interpolationTimeStep, "4, 6");
        interpolationTimeStep.setColumns(10);

        JLabel lblInterpolationMinimumTicks = new JLabel("Minimum Axis Resolution Ticks");
        interpolationPanel.add(lblInterpolationMinimumTicks, "2, 8, right, default");
        lblInterpolationMinimumTicks.setToolTipText("<html>\r\n<p>Minimum step axis distance used to interpolate advanced motion paths.</p>\r\n<p>This is given in resolution ticks of the axes.</p>\r\n</html>\r\n");

        interpolationMinStep = new JTextField();
        interpolationPanel.add(interpolationMinStep, "4, 8");
        interpolationMinStep.setColumns(10);
        
        JLabel lblJunctionDeviation = new JLabel("Maximum Junction Deviation");
        lblJunctionDeviation.setToolTipText("The maximum Junction Deviation allowed by the driver. Please consult the driver's documentation. ");
        interpolationPanel.add(lblJunctionDeviation, "2, 10, right, default");
        
        junctionDeviation = new JTextField();
        interpolationPanel.add(junctionDeviation, "4, 10, fill, default");
        junctionDeviation.setColumns(10);

        JLabel lblConfirmationFlowControl = new JLabel("Confimation Flow Control");
        lblConfirmationFlowControl.setToolTipText("<html>\r\n<p>The communication with the controller is flow-controlled by awaiting the \"ok\"<br/>\r\nbefore sending the next command. </p>\r\n<p>This is slower than other types of flow control such as RTS/CTS on a serial connection, so <br/>\r\nthe latter should be preferred.</p>\r\n</html>");
        settingsPanel.add(lblConfirmationFlowControl, "2, 2, right, default");

        confirmationFlowControl = new JCheckBox("");
        settingsPanel.add(confirmationFlowControl, "4, 2");

    }

    @Override
    public void createBindings() {
        IntegerConverter intConverter = new IntegerConverter();
        //DoubleConverter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        DoubleConverter doubleConverterFine = new DoubleConverter("%.6f");
        LengthConverter lengthConverter = new LengthConverter();

        addWrappedBinding(driver, "confirmationFlowControl", confirmationFlowControl, "selected");
        addWrappedBinding(driver, "interpolationMaxSteps", interpolationMaxSteps, "text", intConverter);
        addWrappedBinding(driver, "interpolationJerkSteps", interpolationJerkSteps, "text", intConverter);
        addWrappedBinding(driver, "interpolationTimeStep", interpolationTimeStep, "text", doubleConverterFine);
        addWrappedBinding(driver, "interpolationMinStep", interpolationMinStep, "text", intConverter);
        addWrappedBinding(driver, "junctionDeviation", junctionDeviation, "text", lengthConverter);

        ComponentDecorators.decorateWithAutoSelect(interpolationMaxSteps);
        ComponentDecorators.decorateWithAutoSelect(interpolationJerkSteps);
        ComponentDecorators.decorateWithAutoSelect(interpolationTimeStep);
        ComponentDecorators.decorateWithAutoSelect(interpolationMinStep);
        ComponentDecorators.decorateWithAutoSelect(junctionDeviation);
    }
}
