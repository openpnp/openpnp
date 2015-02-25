package org.openpnp.machine.openbuilds;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.IntegerConverter;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

public class OpenBuildsDriverWizard extends AbstractConfigurationWizard {
    private final OpenBuildsDriver driver;
    private JTextField textFieldRed;
    private JTextField textFieldGreen;
    private JTextField textFieldBlue;
    private JSlider sliderBlue;
    private JSlider sliderGreen;
    private JSlider sliderRed;
    
    public OpenBuildsDriverWizard(OpenBuildsDriver driver) {
        this.driver = driver;
        
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        
        JPanel panelNeoPixel = new JPanel();
        panelNeoPixel.setBorder(new TitledBorder(null, "NeoPixel", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelNeoPixel);
        panelNeoPixel.setLayout(new FormLayout(new ColumnSpec[] {
                FormFactory.RELATED_GAP_COLSPEC,
                FormFactory.DEFAULT_COLSPEC,
                FormFactory.RELATED_GAP_COLSPEC,
                FormFactory.DEFAULT_COLSPEC,
                FormFactory.RELATED_GAP_COLSPEC,
                FormFactory.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormFactory.RELATED_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.RELATED_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.RELATED_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,}));
        
        JLabel lblRed = new JLabel("Red");
        panelNeoPixel.add(lblRed, "2, 2");
        
        sliderRed = new JSlider();
        sliderRed.setMaximum(255);
        sliderRed.setValue(0);
        sliderRed.setPaintLabels(true);
        panelNeoPixel.add(sliderRed, "4, 2");
        
        textFieldRed = new JTextField();
        textFieldRed.setText("0");
        panelNeoPixel.add(textFieldRed, "6, 2");
        textFieldRed.setColumns(3);
        
        JLabel lblGreen = new JLabel("Green");
        panelNeoPixel.add(lblGreen, "2, 4");
        
        sliderGreen = new JSlider();
        sliderGreen.setValue(0);
        sliderGreen.setMaximum(255);
        sliderGreen.setPaintLabels(true);
        panelNeoPixel.add(sliderGreen, "4, 4");
        
        textFieldGreen = new JTextField();
        textFieldGreen.setText("0");
        panelNeoPixel.add(textFieldGreen, "6, 4");
        textFieldGreen.setColumns(3);
        
        JLabel lblBlue = new JLabel("Blue");
        panelNeoPixel.add(lblBlue, "2, 6");
        
        sliderBlue = new JSlider();
        sliderBlue.setValue(0);
        sliderBlue.setPaintLabels(true);
        sliderBlue.setMaximum(255);
        panelNeoPixel.add(sliderBlue, "4, 6");
        
        textFieldBlue = new JTextField();
        textFieldBlue.setText("0");
        panelNeoPixel.add(textFieldBlue, "6, 6");
        textFieldBlue.setColumns(3);
        initDataBindings();
    }
    
    @Override
    public void createBindings() {
    	IntegerConverter integerConverter = new IntegerConverter();
        
    	addWrappedBinding(driver, "neoPixelRed", textFieldRed, "text", integerConverter);
        addWrappedBinding(driver, "neoPixelGreen", textFieldGreen, "text", integerConverter);
        addWrappedBinding(driver, "neoPixelBlue", textFieldBlue, "text", integerConverter);
    	
        ComponentDecorators.decorateWithAutoSelect(textFieldRed);
        ComponentDecorators.decorateWithAutoSelect(textFieldGreen);
        ComponentDecorators.decorateWithAutoSelect(textFieldBlue);
    }
    
    protected void initDataBindings() {
        BeanProperty<JTextField, String> jTextFieldBeanProperty = BeanProperty.create("text");
        BeanProperty<JSlider, Integer> jSliderBeanProperty = BeanProperty.create("value");
        AutoBinding<JTextField, String, JSlider, Integer> autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, textFieldRed, jTextFieldBeanProperty, sliderRed, jSliderBeanProperty);
        autoBinding.bind();
        //
        BeanProperty<JTextField, String> jTextFieldBeanProperty_1 = BeanProperty.create("text");
        BeanProperty<JSlider, Integer> jSliderBeanProperty_1 = BeanProperty.create("value");
        AutoBinding<JTextField, String, JSlider, Integer> autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, textFieldGreen, jTextFieldBeanProperty_1, sliderGreen, jSliderBeanProperty_1);
        autoBinding_1.bind();
        //
        BeanProperty<JTextField, String> jTextFieldBeanProperty_2 = BeanProperty.create("text");
        BeanProperty<JSlider, Integer> jSliderBeanProperty_2 = BeanProperty.create("value");
        AutoBinding<JTextField, String, JSlider, Integer> autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, textFieldBlue, jTextFieldBeanProperty_2, sliderBlue, jSliderBeanProperty_2);
        autoBinding_2.bind();
    }
}
