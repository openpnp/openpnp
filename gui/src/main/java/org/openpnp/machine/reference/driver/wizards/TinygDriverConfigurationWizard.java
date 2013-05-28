package org.openpnp.machine.reference.driver.wizards;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.machine.reference.driver.TinygDriver;

import com.google.gson.JsonObject;
import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

public class TinygDriverConfigurationWizard extends AbstractConfigurationWizard {
    private final TinygDriver driver;
    private JTextField m1StepAngle;
    private JTextField m2StepAngle;
    private JTextField textField_2;
    private JTextField textField_3;
    private JTextField m1TravelPerRev;
    private JTextField m2TravelPerRev;
    private JTextField textField_6;
    private JTextField textField_7;
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
    private JCheckBox checkBox;
    private JCheckBox checkBox_1;
    
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
                FormFactory.RELATED_GAP_COLSPEC,
                FormFactory.DEFAULT_COLSPEC,
                FormFactory.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormFactory.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormFactory.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormFactory.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),},
            new RowSpec[] {
                FormFactory.RELATED_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.RELATED_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.RELATED_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.RELATED_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.RELATED_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.RELATED_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.RELATED_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,}));
        
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
        
        JComboBox m1Axis = new JComboBox();
        panelMotors.add(m1Axis, "4, 4, fill, default");
        
        JComboBox m2Axis = new JComboBox();
        panelMotors.add(m2Axis, "6, 4, fill, default");
        
        JComboBox m3Axis = new JComboBox();
        panelMotors.add(m3Axis, "8, 4, fill, default");
        
        JComboBox m4Axis = new JComboBox();
        panelMotors.add(m4Axis, "10, 4, fill, default");
        
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
        
        textField_2 = new JTextField();
        textField_2.setText("1.8");
        panelMotors.add(textField_2, "8, 6");
        textField_2.setColumns(10);
        
        textField_3 = new JTextField();
        textField_3.setText("1.8");
        panelMotors.add(textField_3, "10, 6");
        textField_3.setColumns(10);
        
        JLabel lblTravelPerRev = new JLabel("Travel Per Rev.");
        panelMotors.add(lblTravelPerRev, "2, 8, right, default");
        
        m1TravelPerRev = new JTextField();
        panelMotors.add(m1TravelPerRev, "4, 8, fill, default");
        m1TravelPerRev.setColumns(10);
        
        m2TravelPerRev = new JTextField();
        panelMotors.add(m2TravelPerRev, "6, 8, fill, default");
        m2TravelPerRev.setColumns(10);
        
        textField_6 = new JTextField();
        panelMotors.add(textField_6, "8, 8, fill, default");
        textField_6.setColumns(10);
        
        textField_7 = new JTextField();
        panelMotors.add(textField_7, "10, 8, fill, default");
        textField_7.setColumns(10);
        
        JLabel lblMicrosteps = new JLabel("Microsteps");
        panelMotors.add(lblMicrosteps, "2, 10, right, default");
        
        JSpinner m1Microsteps = new JSpinner();
        panelMotors.add(m1Microsteps, "4, 10");
        
        JSpinner m2Microsteps = new JSpinner();
        panelMotors.add(m2Microsteps, "6, 10");
        
        JSpinner spinner_2 = new JSpinner();
        panelMotors.add(spinner_2, "8, 10");
        
        JSpinner spinner_3 = new JSpinner();
        panelMotors.add(spinner_3, "10, 10");
        
        JLabel lblPolarity = new JLabel("Polarity");
        panelMotors.add(lblPolarity, "2, 12, right, default");
        
        JComboBox m1Polarity = new JComboBox();
        panelMotors.add(m1Polarity, "4, 12, fill, default");
        
        JComboBox m2Polarity = new JComboBox();
        panelMotors.add(m2Polarity, "6, 12, fill, default");
        
        JComboBox comboBox_6 = new JComboBox();
        panelMotors.add(comboBox_6, "8, 12, fill, default");
        
        JComboBox comboBox_7 = new JComboBox();
        panelMotors.add(comboBox_7, "10, 12, fill, default");
        
        JLabel lblPowerManagement = new JLabel("Power Management");
        panelMotors.add(lblPowerManagement, "2, 14");
        
        m1PowerMgmt = new JCheckBox("");
        panelMotors.add(m1PowerMgmt, "4, 14");
        
        m2PowerMgmt = new JCheckBox("");
        panelMotors.add(m2PowerMgmt, "6, 14");
        
        checkBox = new JCheckBox("");
        panelMotors.add(checkBox, "8, 14");
        
        checkBox_1 = new JCheckBox("");
        panelMotors.add(checkBox_1, "10, 14");
        
        JPanel panelAxes = new JPanel();
        panelAxes.setBorder(new TitledBorder(null, "Axes", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelAxes);
        panelAxes.setLayout(new FormLayout(new ColumnSpec[] {
                FormFactory.RELATED_GAP_COLSPEC,
                FormFactory.DEFAULT_COLSPEC,
                FormFactory.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormFactory.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormFactory.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormFactory.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormFactory.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormFactory.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),},
            new RowSpec[] {
                FormFactory.RELATED_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.RELATED_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.RELATED_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.RELATED_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.RELATED_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,}));
        
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
        
        addWrappedBinding(this, "powerMgmtM1", m1PowerMgmt, "selected");
        addWrappedBinding(this, "powerMgmtM2", m2PowerMgmt, "selected");
        
        addWrappedBinding(this, "velMaxX", xVelMax, "text", integerConverter);
        addWrappedBinding(this, "feedMaxX", xFeedMax, "text", integerConverter);
        addWrappedBinding(this, "jerkMaxX", xJerkMax, "text", integerConverter);

        addWrappedBinding(this, "velMaxY", yVelMax, "text", integerConverter);
        addWrappedBinding(this, "feedMaxY", yFeedMax, "text", integerConverter);
        addWrappedBinding(this, "jerkMaxY", yJerkMax, "text", integerConverter);

        ComponentDecorators.decorateWithAutoSelect(xVelMax);
        ComponentDecorators.decorateWithAutoSelect(xFeedMax);
        ComponentDecorators.decorateWithAutoSelect(xJerkMax);
        
        ComponentDecorators.decorateWithAutoSelect(yVelMax);
        ComponentDecorators.decorateWithAutoSelect(yFeedMax);
        ComponentDecorators.decorateWithAutoSelect(yJerkMax);
    }
    
    public int getVelMaxX() throws Exception {
    	return getConfigInt("xvm");
    }
    
    public void setVelMaxX(int v) throws Exception {
    	setConfigInt("xvm", v);
    }
    
    public int getFeedMaxX() throws Exception {
    	return getConfigInt("xfr");
    }
    
    public void setFeedMaxX(int v) throws Exception {
    	setConfigInt("xfr", v);
    }
    
    public int getJerkMaxX() throws Exception {
    	return getConfigInt("xjm");
    }
    
    public void setJerkMaxX(int v) throws Exception {
    	setConfigInt("xjm", v);
    }
    
    public int getVelMaxY() throws Exception {
    	return getConfigInt("yvm");
    }
    
    public void setVelMaxY(int v) throws Exception {
    	setConfigInt("yvm", v);
    }
    
    public int getFeedMaxY() throws Exception {
    	return getConfigInt("yfr");
    }
    
    public void setFeedMaxY(int v) throws Exception {
    	setConfigInt("yfr", v);
    }
    
    public int getJerkMaxY() throws Exception {
    	return getConfigInt("yjm");
    }
    
    public void setJerkMaxY(int v) throws Exception {
    	setConfigInt("yjm", v);
    }
    
    public boolean getPowerMgmtM1() throws Exception {
    	return getConfigInt("1pm") == 1;
    }
    
    public void setPowerMgmtM1(boolean v) throws Exception {
    	setConfigInt("1pm", v ? 1 : 0);
    }
    
    public boolean getPowerMgmtM2() throws Exception {
    	return getConfigInt("2pm") == 1;
    }
    
    public void setPowerMgmtM2(boolean v) throws Exception {
    	setConfigInt("2pm", v ? 1 : 0);
    }
    
    private int getConfigInt(String name) throws Exception {
    	JsonObject o = driver.sendCommand(String.format("{\"%s\":\"\"}", name));
    	return o.get(name).getAsInt();
    }
    
    private void setConfigInt(String name, int v) throws Exception {
    	JsonObject o = driver.sendCommand(String.format("{\"%s\":%d}", name, v));
    }
}
