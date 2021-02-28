package org.openpnp.machine.reference.actuator.wizards;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.machine.reference.actuator.ThermistorToLinearSensorActuator;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import javax.swing.JTextField;

public class ThermistorToLinearSensorActuatorTransforms extends AbstractConfigurationWizard {
    private final ThermistorToLinearSensorActuator actuator;
    private JPanel panel;
    private JLabel lblNewLabel;
    private JTextField a;
    private JLabel lblNewLabel_1;
    private JLabel lblNewLabel_2;
    private JPanel panel_1;
    private JPanel panel_2;
    private JLabel lblNewLabel_3;
    private JLabel lblNewLabel_4;
    private JLabel lblNewLabel_5;
    private JLabel lblNewLabel_6;
    private JLabel lblNewLabel_7;
    private JLabel lblNewLabel_8;
    private JTextField b;
    private JTextField c;
    private JTextField r1;
    private JTextField r2;
    private JTextField adcMax;
    private JTextField vRef;
    private JTextField scale;
    private JTextField offset;
    private JLabel lblNewLabel_9;

    public ThermistorToLinearSensorActuatorTransforms(ThermistorToLinearSensorActuator actuator) {
        this.actuator = actuator;
        createUi();
    }
    private void createUi() {
        
        panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "Thermistor", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(new ColumnSpec[] {
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
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblNewLabel = new JLabel("A");
        panel.add(lblNewLabel, "2, 2, right, default");
        
        a = new JTextField();
        panel.add(a, "4, 2");
        a.setColumns(10);
        
        lblNewLabel_1 = new JLabel("B");
        panel.add(lblNewLabel_1, "2, 4, right, default");
        
        b = new JTextField();
        panel.add(b, "4, 4, fill, default");
        b.setColumns(10);
        
        lblNewLabel_2 = new JLabel("C");
        panel.add(lblNewLabel_2, "2, 6, right, default");
        
        c = new JTextField();
        panel.add(c, "4, 6, fill, default");
        c.setColumns(10);
        
        lblNewLabel_3 = new JLabel("R1");
        lblNewLabel_3.setEnabled(false);
        panel.add(lblNewLabel_3, "2, 8, right, default");
        
        r1 = new JTextField();
        r1.setEnabled(false);
        panel.add(r1, "4, 8, fill, default");
        r1.setColumns(10);
        
        lblNewLabel_9 = new JLabel("(Not yet supported)");
        panel.add(lblNewLabel_9, "6, 8");
        
        lblNewLabel_4 = new JLabel("R2");
        panel.add(lblNewLabel_4, "2, 10, right, default");
        
        r2 = new JTextField();
        panel.add(r2, "4, 10, fill, default");
        r2.setColumns(10);
        
        panel_1 = new JPanel();
        panel_1.setBorder(new TitledBorder(null, "ADC", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panel_1);
        panel_1.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblNewLabel_5 = new JLabel("Maximum Value");
        panel_1.add(lblNewLabel_5, "2, 2, right, default");
        
        adcMax = new JTextField();
        panel_1.add(adcMax, "4, 2, fill, default");
        adcMax.setColumns(10);
        
        lblNewLabel_6 = new JLabel("Voltage Reference");
        panel_1.add(lblNewLabel_6, "2, 4, right, default");
        
        vRef = new JTextField();
        panel_1.add(vRef, "4, 4, fill, default");
        vRef.setColumns(10);
        
        panel_2 = new JPanel();
        panel_2.setBorder(new TitledBorder(null, "Linear Transform", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panel_2);
        panel_2.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblNewLabel_7 = new JLabel("Scale");
        panel_2.add(lblNewLabel_7, "2, 2, right, default");
        
        scale = new JTextField();
        panel_2.add(scale, "4, 2, fill, default");
        scale.setColumns(10);
        
        lblNewLabel_8 = new JLabel("Offset");
        panel_2.add(lblNewLabel_8, "2, 4, right, default");
        
        offset = new JTextField();
        panel_2.add(offset, "4, 4, fill, default");
        offset.setColumns(10);
    }

    
    @Override
    public void createBindings() {
        DoubleConverter doubleConverter =
                new DoubleConverter("%f");
        DoubleConverter doubleConverterSci =
                new DoubleConverter("%g");

        addWrappedBinding(actuator, "a", a, "text", doubleConverterSci);
        addWrappedBinding(actuator, "b", b, "text", doubleConverterSci);
        addWrappedBinding(actuator, "c", c, "text", doubleConverterSci);
        addWrappedBinding(actuator, "r1", r1, "text", doubleConverter);
        addWrappedBinding(actuator, "r2", r2, "text", doubleConverter);
        addWrappedBinding(actuator, "adcMax", adcMax, "text", doubleConverter);
        addWrappedBinding(actuator, "vRef", vRef, "text", doubleConverter);
        addWrappedBinding(actuator, "scale", scale, "text", doubleConverterSci);
        addWrappedBinding(actuator, "offset", offset, "text", doubleConverterSci);

        ComponentDecorators.decorateWithAutoSelect(a);
        ComponentDecorators.decorateWithAutoSelect(b);
        ComponentDecorators.decorateWithAutoSelect(c);
        ComponentDecorators.decorateWithAutoSelect(r1);
        ComponentDecorators.decorateWithAutoSelect(r2);
        ComponentDecorators.decorateWithAutoSelect(adcMax);
        ComponentDecorators.decorateWithAutoSelect(vRef);
        ComponentDecorators.decorateWithAutoSelect(scale);
        ComponentDecorators.decorateWithAutoSelect(offset);
    }
}
