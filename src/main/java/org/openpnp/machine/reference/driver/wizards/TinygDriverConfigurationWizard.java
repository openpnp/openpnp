package org.openpnp.machine.reference.driver.wizards;

import java.util.Locale;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerListModel;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.machine.reference.driver.TinygDriver;

import com.google.gson.JsonObject;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class TinygDriverConfigurationWizard extends AbstractConfigurationWizard {
    private final TinygDriver driver;
    private JTextField m1StepAngle;
    private JTextField m2StepAngle;
    private JTextField m3StepAngle;
    private JTextField m4StepAngle;
    private JTextField m1TravelPerRev;
    private JTextField m2TravelPerRev;
    private JTextField m3TravelPerRev;
    private JTextField m4TravelPerRev;
    private JTextField xVelMax;
    private JTextField yVelMax;
    private JTextField textField_10;
    private JTextField textField_11;
    private JTextField textField_12;
    private JTextField textField_13;
    private JTextField xFeedMax;
    private JTextField yFeedMax;
    private JTextField textField_16;
    private JTextField textField_17;
    private JTextField textField_18;
    private JTextField textField_19;
    private JTextField xJerkMax;
    private JTextField yJerkMax;
    private JTextField textField_22;
    private JTextField textField_23;
    private JTextField textField_24;
    private JTextField textField_25;
    private JCheckBox m1PowerMgmt;
    private JCheckBox m2PowerMgmt;
    private JCheckBox m3PowerMgmt;
    private JCheckBox m4PowerMgmt;
    private JCheckBox m1RevPol;
    private JCheckBox m2RevPol;
    private JCheckBox m3RevPol;
    private JCheckBox m4RevPol;
    
    public TinygDriverConfigurationWizard(TinygDriver driver) {
        this.driver = driver;
        
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        
        JPanel panelGeneral = new JPanel();
        panelGeneral.setBorder(new TitledBorder(null, "General Settings", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelGeneral);
        panelGeneral.setLayout(new FormLayout(new ColumnSpec[] {},
            new RowSpec[] {}));
        
        JPanel panelMotors = new JPanel();
        panelMotors.setBorder(new TitledBorder(null, "Motors", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelMotors);
        panelMotors.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
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
        
        JLabel label = new JLabel("1");
        panelMotors.add(label, "4, 2");
        
        JLabel label_1 = new JLabel("2");
        panelMotors.add(label_1, "6, 2");
        
        JLabel label_2 = new JLabel("3");
        panelMotors.add(label_2, "8, 2");
        
        JLabel label_3 = new JLabel("4");
        panelMotors.add(label_3, "10, 2");
        
        JLabel lblAxis = new JLabel("Axis");
        panelMotors.add(lblAxis, "2, 4, right, default");
        
        JSpinner m1Axis = new JSpinner();
        panelMotors.add(m1Axis, "4, 4");
        
        JSpinner m2Axis = new JSpinner();
        panelMotors.add(m2Axis, "6, 4");
        
        JSpinner m3Axis = new JSpinner();
        panelMotors.add(m3Axis, "8, 4");
        
        JSpinner m4Axis = new JSpinner();
        panelMotors.add(m4Axis, "10, 4");
        
        JLabel lblStepAngle = new JLabel("Step Angle");
        panelMotors.add(lblStepAngle, "2, 6, right, default");
        
        m1StepAngle = new JTextField();
        m1StepAngle.setText("1.8");
        panelMotors.add(m1StepAngle, "4, 6");
        m1StepAngle.setColumns(10);
        
        m2StepAngle = new JTextField();
        m2StepAngle.setText("1.8");
        panelMotors.add(m2StepAngle, "6, 6");
        m2StepAngle.setColumns(10);
        
        m3StepAngle = new JTextField();
        m3StepAngle.setText("1.8");
        panelMotors.add(m3StepAngle, "8, 6");
        m3StepAngle.setColumns(10);
        
        m4StepAngle = new JTextField();
        m4StepAngle.setText("1.8");
        panelMotors.add(m4StepAngle, "10, 6");
        m4StepAngle.setColumns(10);
        
        JLabel lblTravelPerRev = new JLabel("Travel Per Rev.");
        panelMotors.add(lblTravelPerRev, "2, 8, right, default");
        
        m1TravelPerRev = new JTextField();
        panelMotors.add(m1TravelPerRev, "4, 8, fill, default");
        m1TravelPerRev.setColumns(10);
        
        m2TravelPerRev = new JTextField();
        panelMotors.add(m2TravelPerRev, "6, 8, fill, default");
        m2TravelPerRev.setColumns(10);
        
        m3TravelPerRev = new JTextField();
        panelMotors.add(m3TravelPerRev, "8, 8, fill, default");
        m3TravelPerRev.setColumns(10);
        
        m4TravelPerRev = new JTextField();
        panelMotors.add(m4TravelPerRev, "10, 8, fill, default");
        m4TravelPerRev.setColumns(10);
        
        JLabel lblMicrosteps = new JLabel("Microsteps");
        panelMotors.add(lblMicrosteps, "2, 10, right, default");
        
        JSpinner m1Microsteps = new JSpinner(new SpinnerListModel(new Object[] { (Integer) 1, (Integer) 2, (Integer) 4, (Integer) 8 }));
        panelMotors.add(m1Microsteps, "4, 10");
        
        JSpinner m2Microsteps = new JSpinner(new SpinnerListModel(new Object[] { (Integer) 1, (Integer) 2, (Integer) 4, (Integer) 8 }));
        panelMotors.add(m2Microsteps, "6, 10");
        
        JSpinner m3Microsteps = new JSpinner(new SpinnerListModel(new Object[] { (Integer) 1, (Integer) 2, (Integer) 4, (Integer) 8 }));
        panelMotors.add(m3Microsteps, "8, 10");
        
        JSpinner m4Microsteps = new JSpinner(new SpinnerListModel(new Object[] { (Integer) 1, (Integer) 2, (Integer) 4, (Integer) 8 }));
        panelMotors.add(m4Microsteps, "10, 10");
        
        JLabel lblPolarity = new JLabel("Reverse Polarity");
        panelMotors.add(lblPolarity, "2, 12, right, default");
        
        m1RevPol = new JCheckBox("");
        panelMotors.add(m1RevPol, "4, 12");
        
        m2RevPol = new JCheckBox("");
        panelMotors.add(m2RevPol, "6, 12");
        
        m3RevPol = new JCheckBox("");
        panelMotors.add(m3RevPol, "8, 12");
        
        m4RevPol = new JCheckBox("");
        panelMotors.add(m4RevPol, "10, 12");
        
        JLabel lblPowerManagement = new JLabel("Power Management");
        panelMotors.add(lblPowerManagement, "2, 14");
        
        m1PowerMgmt = new JCheckBox("");
        panelMotors.add(m1PowerMgmt, "4, 14");
        
        m2PowerMgmt = new JCheckBox("");
        panelMotors.add(m2PowerMgmt, "6, 14");
        
        m3PowerMgmt = new JCheckBox("");
        panelMotors.add(m3PowerMgmt, "8, 14");
        
        m4PowerMgmt = new JCheckBox("");
        panelMotors.add(m4PowerMgmt, "10, 14");
        
        JPanel panelAxes = new JPanel();
        panelAxes.setBorder(new TitledBorder(null, "Axes", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelAxes);
        panelAxes.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
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
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        JLabel lblX = new JLabel("X");
        panelAxes.add(lblX, "4, 2");
        
        JLabel lblY = new JLabel("Y");
        panelAxes.add(lblY, "6, 2");
        
        JLabel lblZ = new JLabel("Z");
        panelAxes.add(lblZ, "8, 2");
        
        JLabel lblA = new JLabel("A");
        panelAxes.add(lblA, "10, 2");
        
        JLabel lblB = new JLabel("B");
        panelAxes.add(lblB, "12, 2");
        
        JLabel lblC = new JLabel("C");
        panelAxes.add(lblC, "14, 2");
        
        JLabel lblAxisMode = new JLabel("Axis Mode");
        panelAxes.add(lblAxisMode, "2, 4, right, default");
        
        JComboBox xAxisMode = new JComboBox();
        panelAxes.add(xAxisMode, "4, 4, fill, default");
        
        JComboBox yAxisMode = new JComboBox();
        panelAxes.add(yAxisMode, "6, 4, fill, default");
        
        JComboBox comboBox_10 = new JComboBox();
        panelAxes.add(comboBox_10, "8, 4, fill, default");
        
        JComboBox comboBox_11 = new JComboBox();
        panelAxes.add(comboBox_11, "10, 4, fill, default");
        
        JComboBox comboBox_12 = new JComboBox();
        panelAxes.add(comboBox_12, "12, 4, fill, default");
        
        JComboBox comboBox_13 = new JComboBox();
        panelAxes.add(comboBox_13, "14, 4, fill, default");
        
        JLabel lblVelocityMax = new JLabel("Velocity Max.");
        panelAxes.add(lblVelocityMax, "2, 6, right, default");
        
        xVelMax = new JTextField();
        panelAxes.add(xVelMax, "4, 6, fill, default");
        xVelMax.setColumns(10);
        
        yVelMax = new JTextField();
        panelAxes.add(yVelMax, "6, 6, fill, default");
        yVelMax.setColumns(10);
        
        textField_10 = new JTextField();
        panelAxes.add(textField_10, "8, 6, fill, default");
        textField_10.setColumns(10);
        
        textField_11 = new JTextField();
        panelAxes.add(textField_11, "10, 6, fill, default");
        textField_11.setColumns(10);
        
        textField_12 = new JTextField();
        panelAxes.add(textField_12, "12, 6, fill, default");
        textField_12.setColumns(10);
        
        textField_13 = new JTextField();
        panelAxes.add(textField_13, "14, 6, fill, default");
        textField_13.setColumns(10);
        
        JLabel lblFeedrateMax = new JLabel("Feedrate Max.");
        panelAxes.add(lblFeedrateMax, "2, 8, right, default");
        
        xFeedMax = new JTextField();
        panelAxes.add(xFeedMax, "4, 8, fill, default");
        xFeedMax.setColumns(10);
        
        yFeedMax = new JTextField();
        panelAxes.add(yFeedMax, "6, 8, fill, default");
        yFeedMax.setColumns(10);
        
        textField_16 = new JTextField();
        panelAxes.add(textField_16, "8, 8, fill, default");
        textField_16.setColumns(10);
        
        textField_17 = new JTextField();
        panelAxes.add(textField_17, "10, 8, fill, default");
        textField_17.setColumns(10);
        
        textField_18 = new JTextField();
        panelAxes.add(textField_18, "12, 8, fill, default");
        textField_18.setColumns(10);
        
        textField_19 = new JTextField();
        panelAxes.add(textField_19, "14, 8, fill, default");
        textField_19.setColumns(10);
        
        JLabel lblJerkMax = new JLabel("Jerk Max.");
        panelAxes.add(lblJerkMax, "2, 10, right, default");
        
        xJerkMax = new JTextField();
        panelAxes.add(xJerkMax, "4, 10, fill, default");
        xJerkMax.setColumns(10);
        
        yJerkMax = new JTextField();
        panelAxes.add(yJerkMax, "6, 10, fill, default");
        yJerkMax.setColumns(10);
        
        textField_22 = new JTextField();
        panelAxes.add(textField_22, "8, 10, fill, default");
        textField_22.setColumns(10);
        
        textField_23 = new JTextField();
        panelAxes.add(textField_23, "10, 10, fill, default");
        textField_23.setColumns(10);
        
        textField_24 = new JTextField();
        panelAxes.add(textField_24, "12, 10, fill, default");
        textField_24.setColumns(10);
        
        textField_25 = new JTextField();
        panelAxes.add(textField_25, "14, 10, fill, default");
        textField_25.setColumns(10);
    }
    
    @Override
    public void createBindings() {
    	IntegerConverter integerConverter = new IntegerConverter();
        DoubleConverter doubleConverter = new DoubleConverter("%f");
        
        ConfigProxy configProxy = new ConfigProxy();
        
        addWrappedBinding(configProxy, "stepAngleM1", m1StepAngle, "text", doubleConverter);
        addWrappedBinding(configProxy, "stepAngleM2", m2StepAngle, "text", doubleConverter);
        addWrappedBinding(configProxy, "stepAngleM3", m3StepAngle, "text", doubleConverter);
        addWrappedBinding(configProxy, "stepAngleM4", m4StepAngle, "text", doubleConverter);

        addWrappedBinding(configProxy, "travelPerRevM1", m1TravelPerRev, "text", doubleConverter);
        addWrappedBinding(configProxy, "travelPerRevM2", m2TravelPerRev, "text", doubleConverter);
        addWrappedBinding(configProxy, "travelPerRevM3", m3TravelPerRev, "text", doubleConverter);
        addWrappedBinding(configProxy, "travelPerRevM4", m4TravelPerRev, "text", doubleConverter);
        
        addWrappedBinding(configProxy, "polarityReversedM1", m1RevPol, "selected");
        addWrappedBinding(configProxy, "polarityReversedM2", m2RevPol, "selected");
        addWrappedBinding(configProxy, "polarityReversedM3", m3RevPol, "selected");
        addWrappedBinding(configProxy, "polarityReversedM4", m4RevPol, "selected");

        addWrappedBinding(configProxy, "powerMgmtM1", m1PowerMgmt, "selected");
        addWrappedBinding(configProxy, "powerMgmtM2", m2PowerMgmt, "selected");
        addWrappedBinding(configProxy, "powerMgmtM3", m3PowerMgmt, "selected");
        addWrappedBinding(configProxy, "powerMgmtM4", m4PowerMgmt, "selected");
        
        addWrappedBinding(configProxy, "velMaxX", xVelMax, "text", integerConverter);
        addWrappedBinding(configProxy, "velMaxY", yVelMax, "text", integerConverter);
        
        addWrappedBinding(configProxy, "feedMaxX", xFeedMax, "text", integerConverter);
        addWrappedBinding(configProxy, "feedMaxY", yFeedMax, "text", integerConverter);
        
        addWrappedBinding(configProxy, "jerkMaxX", xJerkMax, "text", integerConverter);
        addWrappedBinding(configProxy, "jerkMaxY", yJerkMax, "text", integerConverter);

        ComponentDecorators.decorateWithAutoSelect(m1StepAngle);
        ComponentDecorators.decorateWithAutoSelect(m2StepAngle);
        ComponentDecorators.decorateWithAutoSelect(m3StepAngle);
        ComponentDecorators.decorateWithAutoSelect(m4StepAngle);

        ComponentDecorators.decorateWithAutoSelect(m1TravelPerRev);
        ComponentDecorators.decorateWithAutoSelect(m2TravelPerRev);
        ComponentDecorators.decorateWithAutoSelect(m3TravelPerRev);
        ComponentDecorators.decorateWithAutoSelect(m4TravelPerRev);
        
        ComponentDecorators.decorateWithAutoSelect(xVelMax);
        ComponentDecorators.decorateWithAutoSelect(yVelMax);
        
        ComponentDecorators.decorateWithAutoSelect(xFeedMax);
        ComponentDecorators.decorateWithAutoSelect(yFeedMax);
        
        ComponentDecorators.decorateWithAutoSelect(xJerkMax);
        ComponentDecorators.decorateWithAutoSelect(yJerkMax);
    }
        
    
    public class ConfigProxy {
//                [1ma] m1 map to axis              0 [0=X,1=Y,2=Z...]
//                [1sa] m1 step angle               1.800 deg
//                [1tr] m1 travel per revolution    1.250 mm
//                [1mi] m1 microsteps               8 [1,2,4,8]
//                [1po] m1 polarity                 0 [0=normal,1=reverse]
//                [1pm] m1 power management         1 [0=off,1=on]
//                tinyg [mm] ok>
        
//                [xam] x axis mode                 1 [standard]
//                [xvm] x velocity maximum       5000.000 mm/min
//                [xfr] x feedrate maximum       5000.000 mm/min
//                [xtm] x travel maximum          150.000 mm
//                [xjm] x jerk maximum       20000000 mm/min^3
//                [xjh] x jerk homing        20000000 mm/min^3
//                [xjd] x junction deviation        0.0500 mm (larger is faster)
//                [xsn] x switch min                1 [0=off,1=homing,2=limit,3=limit+homing]
//                [xsx] x switch max                0 [0=off,1=homing,2=limit,3=limit+homing]
//                [xsv] x search velocity         500.000 mm/min
//                [xlv] x latch velocity          100.000 mm/min
//                [xlb] x latch backoff             2.000 mm
//                [xzb] x zero backoff              1.000 mm
//                tinyg [mm] ok>         
        
        
        public double getStepAngleM1() throws Exception {
            return getConfigDouble("1sa");
        }
        
        public void setStepAngleM1(double v) throws Exception {
            setConfigDouble("1sa", v);
        }
        
        public double getStepAngleM2() throws Exception {
            return getConfigDouble("2sa");
        }
        
        public void setStepAngleM2(double v) throws Exception {
            setConfigDouble("2sa", v);
        }
        
        public double getStepAngleM3() throws Exception {
            return getConfigDouble("3sa");
        }
        
        public void setStepAngleM3(double v) throws Exception {
            setConfigDouble("3sa", v);
        }
        
        public double getStepAngleM4() throws Exception {
            return getConfigDouble("4sa");
        }
        
        public void setStepAngleM4(double v) throws Exception {
            setConfigDouble("4sa", v);
        }
        

        
        public double getTravelPerRevM1() throws Exception {
            return getConfigDouble("1tr");
        }
        
        public void setTravelPerRevM1(double v) throws Exception {
            setConfigDouble("1tr", v);
        }
        
        public double getTravelPerRevM2() throws Exception {
            return getConfigDouble("2tr");
        }
        
        public void setTravelPerRevM2(double v) throws Exception {
            setConfigDouble("2tr", v);
        }
        
        public double getTravelPerRevM3() throws Exception {
            return getConfigDouble("3tr");
        }
        
        public void setTravelPerRevM3(double v) throws Exception {
            setConfigDouble("3tr", v);
        }
        
        public double getTravelPerRevM4() throws Exception {
            return getConfigDouble("4tr");
        }
        
        public void setTravelPerRevM4(double v) throws Exception {
            setConfigDouble("4tr", v);
        }
        
        
        
        public boolean getPolarityReversedM1() throws Exception {
            return getConfigBoolean("1po");
        }
        
        public void setPolarityReversedM1(boolean v) throws Exception {
            setConfigBoolean("1po", v);
        }
        
        public boolean getPolarityReversedM2() throws Exception {
            return getConfigBoolean("2po");
        }
        
        public void setPolarityReversedM2(boolean v) throws Exception {
            setConfigBoolean("2po", v);
        }
        
        public boolean getPolarityReversedM3() throws Exception {
            return getConfigBoolean("3po");
        }
        
        public void setPolarityReversedM3(boolean v) throws Exception {
            setConfigBoolean("3po", v);
        }
        
        public boolean getPolarityReversedM4() throws Exception {
            return getConfigBoolean("4po");
        }
        
        public void setPolarityReversedM4(boolean v) throws Exception {
            setConfigBoolean("4po", v);
        }
        
        
        
        public boolean getPowerMgmtM1() throws Exception {
            return getConfigBoolean("1pm");
        }
        
        public void setPowerMgmtM1(boolean v) throws Exception {
            setConfigBoolean("1pm", v);
        }
        
        public boolean getPowerMgmtM2() throws Exception {
            return getConfigBoolean("2pm");
        }
        
        public void setPowerMgmtM2(boolean v) throws Exception {
            setConfigBoolean("2pm", v);
        }
        
        public boolean getPowerMgmtM3() throws Exception {
            return getConfigBoolean("3pm");
        }
        
        public void setPowerMgmtM3(boolean v) throws Exception {
            setConfigBoolean("3pm", v);
        }
        
        public boolean getPowerMgmtM4() throws Exception {
            return getConfigBoolean("4pm");
        }
        
        public void setPowerMgmtM4(boolean v) throws Exception {
            setConfigBoolean("4pm", v);
        }
        
        
        
        public int getVelMaxX() throws Exception {
            return getConfigInt("xvm");
        }
        
        public void setVelMaxX(int v) throws Exception {
            setConfigInt("xvm", v);
        }
        
        public int getVelMaxY() throws Exception {
            return getConfigInt("yvm");
        }
        
        public void setVelMaxY(int v) throws Exception {
            setConfigInt("yvm", v);
        }
        

        
        public int getFeedMaxX() throws Exception {
            return getConfigInt("xfr");
        }
        
        public void setFeedMaxX(int v) throws Exception {
            setConfigInt("xfr", v);
        }
        
        public int getFeedMaxY() throws Exception {
            return getConfigInt("yfr");
        }
        
        public void setFeedMaxY(int v) throws Exception {
            setConfigInt("yfr", v);
        }
        

        
        public int getJerkMaxX() throws Exception {
            return getConfigInt("xjm");
        }
        
        public void setJerkMaxX(int v) throws Exception {
            setConfigInt("xjm", v);
        }
        
        public int getJerkMaxY() throws Exception {
            return getConfigInt("yjm");
        }
        
        public void setJerkMaxY(int v) throws Exception {
            setConfigInt("yjm", v);
        }
        
        
        
        
        
        // TODO: Check for response errors in these methods.
        private int getConfigInt(String name) throws Exception {
            JsonObject o = driver.sendCommand(String.format(Locale.US,"{\"%s\":\"\"}", name));
            return o.get(name).getAsInt();
        }
        
        private void setConfigInt(String name, int v) throws Exception {
            JsonObject o = driver.sendCommand(String.format(Locale.US,"{\"%s\":%d}", name, v));
        }
        
        private double getConfigDouble(String name) throws Exception {
            JsonObject o = driver.sendCommand(String.format(Locale.US,"{\"%s\":\"\"}", name));
            return o.get(name).getAsDouble();
        }
        
        private void setConfigDouble(String name, double v) throws Exception {
            JsonObject o = driver.sendCommand(String.format(Locale.US,"{\"%s\":%f}", name, v));
        }
        
        private boolean getConfigBoolean(String name) throws Exception {
            return getConfigInt(name) == 1;
        }
        
        private void setConfigBoolean(String name, boolean v) throws Exception {
            setConfigInt(name, v ? 1 : 0);
        }
    }
}
