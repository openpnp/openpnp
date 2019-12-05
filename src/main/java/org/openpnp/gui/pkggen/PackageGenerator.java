package org.openpnp.gui.pkggen;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openpnp.gui.pkggen.pkgs.Chip;
import org.openpnp.gui.pkggen.pkgs.QFN;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import javax.swing.JButton;

/**
 * TODO:
 * * Show top and bottom views. Just render pins and body in different order.
 * * Show pin one, maybe as red.
 * * Have the ability to show a dot on the package as the orientation marker.
 */
public class PackageGenerator extends JFrame {
    private JPanel panel;
    private JLabel lblNewLabel;
    private JTextField eMin;
    private JTextField eNom;
    private JTextField eMax;
    private MinNomMaxField minNomMaxField;
    private MinNomMaxField minNomMaxField_1;
    private MinNomMaxField minNomMaxField_2;
    private JLabel lblNewLabel_1;
    private JComboBox comboBox;
    private JPanel panel_1;
    
    private PackageGeneratorPackage pkg;
    private JButton btnNewButton;
    
    public PackageGenerator() {
        createUi();
        comboBox.setSelectedItem("Chip");
        
        btnNewButton = new JButton("Generate");
        btnNewButton.addActionListener(new BtnNewButtonActionListener());
        panel.add(btnNewButton, "3, 5");
    }
    
    private void createUi() {
        getContentPane().setLayout(new BorderLayout(0, 0));
        
        panel = new JPanel();
        getContentPane().add(panel, BorderLayout.CENTER);
        panel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblNewLabel_1 = new JLabel("Type");
        panel.add(lblNewLabel_1, "1, 2, right, default");
        
        comboBox = new JComboBox();
        comboBox.addActionListener(new ComboBoxActionListener());
        comboBox.setModel(new DefaultComboBoxModel(new String[] {"Chip", "QFN", "QFP", "SOIC / SOP", "SOT23"}));
        panel.add(comboBox, "3, 2");
        
        panel_1 = new JPanel();
        panel.add(panel_1, "1, 3, 3, 1, default, fill");
        panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.Y_AXIS));
        
        minNomMaxField = new MinNomMaxField("e");
        panel_1.add(minNomMaxField);
        
        minNomMaxField_1 = new MinNomMaxField("L");
        panel_1.add(minNomMaxField_1);
        
        minNomMaxField_2 = new MinNomMaxField("A");
        panel_1.add(minNomMaxField_2);
    }
    
    
    public static void main(String[] args) {
        PackageGenerator p = new PackageGenerator();
        p.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        p.setSize(1280, 1024);
        p.setVisible(true);
    }
    
    private class ComboBoxActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (comboBox.getSelectedItem().equals("Chip")) {
                pkg = new Chip();
            }
            else if (comboBox.getSelectedItem().equals("QFN")) {
                pkg = new QFN();
            }
            panel_1.removeAll();
            for (MinNomMaxField field : pkg.getFields()) {
                panel_1.add(field);
            }
            revalidate();
            repaint();
        }
    }
    private class BtnNewButtonActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            pkg.generate();
        }
    }
}
