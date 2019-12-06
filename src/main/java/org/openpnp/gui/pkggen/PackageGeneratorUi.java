package org.openpnp.gui.pkggen;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.pkggen.pkgs.ChipPackageGenerator;
import org.openpnp.gui.pkggen.pkgs.QFNPackageGenerator;
import org.openpnp.model.Package;
import org.openpnp.util.BeanUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

/**
 * TODO:
 * * Show top and bottom views. Just render pins and body in different order.
 * * Show pin one, maybe as red.
 * * Have the ability to show a dot on the package as the orientation marker.
 */
public class PackageGeneratorUi extends JFrame {
    private JPanel panel;
    private JLabel lblNewLabel;
    private MinNomMaxField minNomMaxField;
    private MinNomMaxField minNomMaxField_1;
    private MinNomMaxField minNomMaxField_2;
    private JLabel lblNewLabel_1;
    private JComboBox comboBox;
    private JPanel panel_1;
    
    private PackageGenerator generator;
    private List<MinNomMaxField> fields = new ArrayList<>();
    private JPanel panel_2;
    private PackageView topPackageView;
    private PackageView bottomPackageView;
    
    public PackageGeneratorUi() {
        createUi();
        comboBox.setSelectedItem("Chip");
        topPackageView.setTop(true);
    }
    
    private void createUi() {
        getContentPane().setLayout(new BorderLayout(0, 0));
        
        panel = new JPanel();
        getContentPane().add(panel, BorderLayout.CENTER);
        panel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("pref:grow"),},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                RowSpec.decode("default:grow"),}));
        
        lblNewLabel_1 = new JLabel("Type");
        panel.add(lblNewLabel_1, "1, 2, right, default");
        
        comboBox = new JComboBox();
        comboBox.addActionListener(new ComboBoxActionListener());
        comboBox.setModel(new DefaultComboBoxModel(new String[] {"Chip", "QFN", "QFP", "SOIC / SOP", "SOT23"}));
        panel.add(comboBox, "3, 2, left, default");
        
        panel_1 = new JPanel();
        panel.add(panel_1, "1, 3, 3, 1, default, fill");
        panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.Y_AXIS));
        
        minNomMaxField = new MinNomMaxField("e");
        panel_1.add(minNomMaxField);
        
        minNomMaxField_1 = new MinNomMaxField("L");
        panel_1.add(minNomMaxField_1);
        
        minNomMaxField_2 = new MinNomMaxField("A");
        panel_1.add(minNomMaxField_2);
        
        panel_2 = new JPanel();
        panel.add(panel_2, "3, 5, fill, fill");
        panel_2.setLayout(new BoxLayout(panel_2, BoxLayout.X_AXIS));
        
        topPackageView = new PackageView();
        panel_2.add(topPackageView);
        
        bottomPackageView = new PackageView();
        panel_2.add(bottomPackageView);
    }
    
    public static void main(String[] args) {
        PackageGeneratorUi p = new PackageGeneratorUi();
        p.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        p.setSize(1280, 1024);
        p.setVisible(true);
    }
    
    private void render() {
        Package pkg = generator.generate();
        topPackageView.setPkg(pkg);
        bottomPackageView.setPkg(pkg);
    }
    
    private class ComboBoxActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (comboBox.getSelectedItem().equals("Chip")) {
                generator = new ChipPackageGenerator();
            }
            else if (comboBox.getSelectedItem().equals("QFN")) {
                generator = new QFNPackageGenerator();
            }
            panel_1.removeAll();
            for (String property : generator.getPropertyNames()) {
                MinNomMaxField field = new MinNomMaxField(property);
                BeanUtils.bind(UpdateStrategy.READ_WRITE, generator, property, field, "nom");
                generator.addPropertyChangeListener(property, pce -> {
                    render();
                });
                panel_1.add(field);
            }
            revalidate();
            repaint();
            render();
        }
    }
}
