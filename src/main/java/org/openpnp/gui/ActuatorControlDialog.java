package org.openpnp.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Objects;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.jdesktop.beansbinding.Converter;
import org.openpnp.gui.support.ActuatorEnumComboBoxModel;
import org.openpnp.gui.support.Converters;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.base.AbstractActuator;
import org.openpnp.util.UiUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class ActuatorControlDialog extends JDialog {
    private JTextField valueTf;
    private JTextField readTf;
    private JLabel lblBoolean;
    private JButton onBtn;
    private JButton offBtn;
    private JLabel lblValue;
    private Actuator actuator;
    private JButton setBtn;
    private JLabel lblRead;
    private JButton readBtn;
    private JButton closeBtn;
    private JComboBox valueComboBox;
    private JButton setValueBtn;
    private JLabel lblSetValue;

    public ActuatorControlDialog(Actuator actuator) {
        super(MainFrame.get(), actuator.getHead() == null ? actuator.getName()
                : actuator.getHead().getName() + ":" + actuator.getName(), true);
        this.actuator = actuator;
        getContentPane().setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.LABEL_COMPONENT_GAP_ROWSPEC,}));

        setModalityType(JDialog.ModalityType.MODELESS);

        lblBoolean = new JLabel("Set Boolean Value");
        getContentPane().add(lblBoolean, "2, 2");

        onBtn = new JButton("On");
        getContentPane().add(onBtn, "4, 2");
        onBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.submitUiMachineTask(() -> {
                    actuator.actuate(true);
                });
            }
        });

        offBtn = new JButton("Off");
        getContentPane().add(offBtn, "6, 2");
        offBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.submitUiMachineTask(() -> {
                    actuator.actuate(false);
                });
            }
        });

        lblValue = new JLabel("Set Value");
        getContentPane().add(lblValue, "2, 4, right, default");

        valueTf = new JTextField();
        getContentPane().add(valueTf, "4, 4, 3, 1");
        valueTf.setColumns(10);

        setBtn = new JButton("Set");
        setBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.submitUiMachineTask(() -> {
                    // Use the generic Object method to interpret the value according to the actuator.valueClass.
                    Converter<?, String> converter = Converters.getConverter(actuator.getValueClass());
                    actuator.actuate(converter.convertReverse(valueTf.getText()));
                });
            }
        });
        getContentPane().add(setBtn, "8, 4");

        lblSetValue = new JLabel("Set Value");
        getContentPane().add(lblSetValue, "2, 5, right, default");

        valueComboBox = new JComboBox(new ActuatorEnumComboBoxModel((AbstractActuator) actuator));
        getContentPane().add(valueComboBox, "4, 5, 3, 1, fill, default");

        setValueBtn = new JButton("Set");
        setValueBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.submitUiMachineTask(() -> {
                    actuator.actuate(valueComboBox.getSelectedItem());
                });
            }
        });
        getContentPane().add(setValueBtn, "8, 5");

        lblRead = new JLabel("Read Value");
        getContentPane().add(lblRead, "2, 7, right, default");

        readTf = new JTextField();
        getContentPane().add(readTf, "4, 7, 3, 1");
        readTf.setColumns(10);

        readBtn = new JButton("Read");
        readBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.submitUiMachineTask(() -> {
                    Object s = actuator.read();
                    Converter<Object, String> converter = Converters.getConverter(actuator.getValueClass());
                    readTf.setText(Objects.toString(converter.convertForward(s), ""));
                });
            }
        });
        getContentPane().add(readBtn, "8, 7");

        closeBtn = new JButton("Close");
        closeBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        getContentPane().add(closeBtn, "8, 11");

        ((AbstractActuator)actuator).addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("valueClass")) {
                    adaptDialog();
                }
            }
        });

        adaptDialog();
    }

    private void adaptDialog() {
        boolean isBoolean = Boolean.class.isAssignableFrom(actuator.getValueClass());
        boolean isEnum = actuator.getValues() != null;

        lblBoolean.setVisible(isBoolean);
        onBtn.setVisible(isBoolean);
        offBtn.setVisible(isBoolean);

        lblValue.setVisible(!(isBoolean || isEnum));
        valueTf.setVisible(!(isBoolean || isEnum));
        setBtn.setVisible(!(isBoolean || isEnum));

        lblSetValue.setVisible(isEnum);
        valueComboBox.setVisible(isEnum);
        setValueBtn.setVisible(isEnum);
    }
}
