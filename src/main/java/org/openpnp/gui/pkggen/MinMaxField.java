package org.openpnp.gui.pkggen;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Locale;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.util.BeanUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class MinMaxField extends JPanel {
    private JLabel label;
    private JTextField minTf;
    private JTextField maxTf;
    private LengthUnit unit = LengthUnit.Millimeters;
    private String format = "%6.3f";
    
    private double min;
    private double max;
    
    public MinMaxField() {
        this("D");
    }
    
    public MinMaxField(String label) {
        createUi();
        setLabel(label);
        
        ComponentDecorators.decorateWithAutoSelect(minTf);
        ComponentDecorators.decorateWithAutoSelect(maxTf);
        
        minTf.addActionListener(e -> {
            convertLength((JTextField) e.getSource(), format);
        });
        minTf.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent event) {
                convertLength(((JTextField) event.getSource()), format);
            }
        });
        
        maxTf.addActionListener(e -> {
            convertLength((JTextField) e.getSource(), format);
        });
        maxTf.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent event) {
                convertLength(((JTextField) event.getSource()), format);
            }
        });
        
        DoubleConverter doubleConverter = new DoubleConverter(format);
        BeanUtils.bind(UpdateStrategy.READ_WRITE, this, "min", minTf, "text", doubleConverter);
        BeanUtils.bind(UpdateStrategy.READ_WRITE, this, "max", maxTf, "text", doubleConverter);
    }
    
    private void createUi() {
        setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("left:pref"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("right:pref"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("right:pref"),},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                RowSpec.decode("26px"),}));
        
        label = new JLabel("New label");
        add(label, "2, 2, left, center");
        
        minTf = new JTextField();
        add(minTf, "4, 2, right, top");
        minTf.setColumns(6);
        
        maxTf = new JTextField();
        add(maxTf, "6, 2, right, top");
        maxTf.setColumns(6);
    }

    private void convertLength(JTextField textField, String format) {
        Length length = Length.parse(textField.getText(), false);
        if (length == null) {
            return;
        }
        if (length.getUnits() == null) {
            length.setUnits(unit);
        }
        length = length.convertToUnits(unit);
        textField.setText(String.format(Locale.US, format, length.getValue()));
    }
    
    public String getLabel() {
        return label.getText();
    }
    
    public void setLabel(String label) {
        this.label.setText(label);
    }

    public double getMin() {
        return min;
    }

    public void setMin(double min) {
        this.min = min;
        if (getMin() != 0 && getMax() == 0) {
            setMax(min);
        }
        firePropertyChange("min", null, min);
        firePropertyChange("nom", null, min);
    }

    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        this.max = max;
        if (getMax() != 0 && getMin() == 0) {
            setMin(max);
        }
        firePropertyChange("max", null, min);
        firePropertyChange("nom", null, min);
    }
    
    public double getNom() {
        return getMin() + (getMax() - getMin()) / 2.;
    }
    
    public void setNom(double nom) {
        setMin(nom);
        setMax(nom);
        firePropertyChange("nom", null, nom);
    }
}
