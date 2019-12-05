package org.openpnp.gui.pkggen;

import java.awt.FlowLayout;
import java.util.Locale;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class MinNomMaxField extends JPanel implements DocumentListener {
    private JLabel label;
    private JTextField min;
    private JTextField nom;
    private JTextField max;
    private boolean lock;
    private LengthUnit unit = LengthUnit.Millimeters;
    private String format = "%6.3f";
    
    public MinNomMaxField() {
        this("D");
    }
    
    public MinNomMaxField(String label) {
        createUi();
        setLabel(label);
    }
    
    private void createUi() {
        setLayout(new FormLayout(new ColumnSpec[] {
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
                RowSpec.decode("26px"),}));
        
        label = new JLabel("New label");
        add(label, "2, 2, left, center");
        
        min = new JTextField();
        add(min, "4, 2, right, top");
        min.setColumns(10);
        
        nom = new JTextField();
        add(nom, "6, 2, right, top");
        nom.setColumns(10);
        
        max = new JTextField();
        add(max, "8, 2, right, top");
        max.setColumns(10);
        
        ComponentDecorators.decorateWithAutoSelect(min);
        ComponentDecorators.decorateWithAutoSelect(nom);
        ComponentDecorators.decorateWithAutoSelect(max);
        
        min.getDocument().addDocumentListener(this);
        nom.getDocument().addDocumentListener(this);
        max.getDocument().addDocumentListener(this);
        
        min.addActionListener(e -> {
            convertLength((JTextField) e.getSource(), format);
        });
        nom.addActionListener(e -> {
            convertLength((JTextField) e.getSource(), format);
        });
        max.addActionListener(e -> {
            convertLength((JTextField) e.getSource(), format);
        });
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        update(e);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        update(e);
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        update(e);
    }
    
    protected void update(DocumentEvent e) {
        if (lock) {
            return;
        }
        lock = true;
        
        JTextField source;
        
        if (e.getDocument() == min.getDocument()) {
            source = min;
        }
        if (e.getDocument() == nom.getDocument()) {
            source = nom;
        }
        else {
            source = max;
        }
        
        try {
            if (source == min || source == max) {
                double vMin = Double.parseDouble(min.getText());
                double vMax = Double.parseDouble(max.getText());
                nom.setText(String.format(format, ((vMax - vMin) / 2 + vMin)));
            }
            else {
              double vNom = Double.parseDouble(nom.getText());
              min.setText(String.format(format, vNom));
              max.setText(String.format(format, vNom));
            }
        }
        catch (Exception e1) {
            
        }
        finally {
            lock = false;
        }
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
        return Double.parseDouble(min.getText());
    }
    
    public double getNom() {
        return Double.parseDouble(min.getText());
    }
    
    public double getMax() {
        return Double.parseDouble(min.getText());
    }
}
