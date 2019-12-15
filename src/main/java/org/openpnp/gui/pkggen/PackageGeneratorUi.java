package org.openpnp.gui.pkggen;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.pkggen.pkgs.ChipPackageGenerator;
import org.openpnp.gui.pkggen.pkgs.QFNPackageGenerator;
import org.openpnp.gui.pkggen.pkgs.QFPPackageGenerator;
import org.openpnp.gui.pkggen.pkgs.SOICPackageGenerator;
import org.openpnp.gui.pkggen.pkgs.SOT23PackageGenerator;
import org.openpnp.model.Configuration;
import org.openpnp.model.Package;
import org.openpnp.util.BeanUtils;
import org.simpleframework.xml.Serializer;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

/**
 * TODO:
 * * Show top and bottom views. Just render pins and body in different order.
 * * Show pin one, maybe as red.
 * * Have the ability to show a dot on the package as the orientation marker.
 * * Note that SOT23, SOIC, QFN and SOT23 all generate -90 so that the dot is in the
 *   upper left.
 */
public class PackageGeneratorUi extends JFrame {
    private JPanel panel;
    private JLabel lblNewLabel;
    private MinMaxField minNomMaxField;
    private MinMaxField minNomMaxField_1;
    private MinMaxField minNomMaxField_2;
    private JLabel lblNewLabel_1;
    private JComboBox comboBox;
    private JPanel panel_1;
    
    private PackageGenerator generator;
    private List<MinMaxField> fields = new ArrayList<>();
    private JPanel panel_2;
    private PackageView topPackageView;
    private PackageView bottomPackageView;
    private JButton btnNewButton;
    
    public PackageGeneratorUi() {
        createUi();
        comboBox.setSelectedItem("Chip");
        topPackageView.setTop(true);
        
        btnNewButton = new JButton("Export to Clipboard");
        btnNewButton.addActionListener(new BtnNewButtonActionListener());
        panel.add(btnNewButton, "3, 7");
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
                RowSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblNewLabel_1 = new JLabel("Type");
        panel.add(lblNewLabel_1, "1, 2, right, default");
        
        comboBox = new JComboBox();
        comboBox.addActionListener(new ComboBoxActionListener());
        comboBox.setModel(new DefaultComboBoxModel(new String[] {"Chip", "QFN", "QFP", "SOIC / SOP", "SOT23"}));
        panel.add(comboBox, "3, 2, left, default");
        
        panel_1 = new JPanel();
        panel.add(panel_1, "1, 3, 3, 1, default, fill");
        panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.Y_AXIS));
        
        minNomMaxField = new MinMaxField("e");
        panel_1.add(minNomMaxField);
        
        minNomMaxField_1 = new MinMaxField("L");
        panel_1.add(minNomMaxField_1);
        
        minNomMaxField_2 = new MinMaxField("A");
        panel_1.add(minNomMaxField_2);
        
        panel_2 = new JPanel();
        panel.add(panel_2, "3, 5, fill, fill");
        panel_2.setLayout(new BoxLayout(panel_2, BoxLayout.X_AXIS));
        
        topPackageView = new PackageView();
        topPackageView.setBorder(new TitledBorder("Top"));
        panel_2.add(topPackageView);
        
        bottomPackageView = new PackageView();
        bottomPackageView.setBorder(new TitledBorder("Bottom"));
        panel_2.add(bottomPackageView);
    }
    
    public static void main(String[] args) {
        PackageGeneratorUi p = new PackageGeneratorUi();
        p.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        p.setSize(1280, 1024);
        p.setVisible(true);
    }
    
    private void render() {
        Package pkg = generator.getPackage();
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
            else if (comboBox.getSelectedItem().equals("QFP")) {
                generator = new QFPPackageGenerator();
            }
            else if (comboBox.getSelectedItem().equals("SOIC / SOP")) {
                generator = new SOICPackageGenerator();
            }
            else if (comboBox.getSelectedItem().equals("SOT23")) {
                generator = new SOT23PackageGenerator();
            }
            generator.addPropertyChangeListener("package", pce -> {
                render();
            });
            panel_1.removeAll();
            for (String property : generator.getPropertyNames()) {
                MinMaxField field = new MinMaxField(property);
                BeanUtils.bind(UpdateStrategy.READ_WRITE, generator, property, field, "nom");
                panel_1.add(field);
            }
            revalidate();
            repaint();
            render();
        }
    }
    
    private class BtnNewButtonActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            try {
                Serializer s = Configuration.createSerializer();
                StringWriter w = new StringWriter();
                s.write(generator.getPackage(), w);
                StringSelection stringSelection = new StringSelection(w.toString());
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);
            }
            catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }
}
