package org.openpnp.gui;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openpnp.spi.Actuator;
import org.openpnp.util.UiUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class ActuatorControlDialog extends JDialog {
    private JTextField doubleTf;
    private JTextField readTf;
    public ActuatorControlDialog(Actuator actuator) {
        super(MainFrame.get(), actuator.getHead() == null ? actuator.getName()
                : actuator.getHead().getName() + ":" + actuator.getName(), true);
        getContentPane().setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
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
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        JLabel lblBoolean = new JLabel("Set Boolean Value");
        getContentPane().add(lblBoolean, "2, 2");
        
        JPanel panel = new JPanel();
        FlowLayout flowLayout = (FlowLayout) panel.getLayout();
        flowLayout.setAlignment(FlowLayout.LEFT);
        getContentPane().add(panel, "4, 2, 3, 1");
        
        JButton onBtn = new JButton("On");
        onBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.submitUiMachineTask(() -> {
                    actuator.actuate(true);
                });
            }
        });
        panel.add(onBtn);
        
        JButton offBtn = new JButton("Off");
        offBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.submitUiMachineTask(() -> {
                    actuator.actuate(false);
                });
            }
        });
        panel.add(offBtn);
        
        JLabel lblDouble = new JLabel("Set Double Value");
        getContentPane().add(lblDouble, "2, 4, right, default");
        
        doubleTf = new JTextField();
        getContentPane().add(doubleTf, "4, 4");
        doubleTf.setColumns(10);
        
        JButton setBtn = new JButton("Set");
        setBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.submitUiMachineTask(() -> {
                    actuator.actuate(Double.parseDouble(doubleTf.getText()));
                });
            }
        });
        getContentPane().add(setBtn, "6, 4");
        
        JLabel lblRead = new JLabel("Read Value");
        getContentPane().add(lblRead, "2, 6, right, default");
        
        readTf = new JTextField();
        getContentPane().add(readTf, "4, 6");
        readTf.setColumns(10);
        
        JButton readBtn = new JButton("Read");
        readBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.submitUiMachineTask(() -> {
                    String s = actuator.read();
                    readTf.setText(s == null ? "" : s);
                });
            }
        });
        getContentPane().add(readBtn, "6, 6");
        
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        getContentPane().add(closeBtn, "6, 10");
    }
}
