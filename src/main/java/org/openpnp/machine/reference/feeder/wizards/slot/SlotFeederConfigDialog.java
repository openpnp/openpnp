package org.openpnp.machine.reference.feeder.wizards.slot;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AbstractBindingListener;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.Binding;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IdentifiableListCellRenderer;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.gui.support.PartsComboBoxModel;
import org.openpnp.machine.reference.feeder.ReferenceSlotAutoFeeder;
import org.openpnp.machine.reference.feeder.ReferenceSlotAutoFeeder.Bank;
import org.openpnp.machine.reference.feeder.ReferenceSlotAutoFeeder.Feeder;
import org.openpnp.model.Configuration;
import org.openpnp.model.Part;
import org.openpnp.util.BeanUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class SlotFeederConfigDialog extends JDialog {
    private JTextField xOffsetTf;
    private JTextField yOffsetTf;
    private JTextField zOffsetTf;
    private JTextField rotOffsetTf;
    private JTextField nameTf;
    private JComboBox bankCb;
    private JComboBox feederCb;
    private JComboBox partCb;
    private LocationButtonsPanel offsetLocBtns;

    public SlotFeederConfigDialog(Frame owner) {
        super(owner, "Slot Feeders", true);

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "Feeders", TitledBorder.LEADING, TitledBorder.TOP,
                null, null));
        getContentPane().add(panel, BorderLayout.CENTER);
        panel.setLayout(new BorderLayout(0, 0));

        JPanel mainPanel = new JPanel();
        panel.add(mainPanel, BorderLayout.CENTER);
        FormLayout fl_mainPanel = new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,});
        fl_mainPanel.setColumnGroups(new int[][] {new int[] {4, 6, 8, 10}});
        mainPanel.setLayout(fl_mainPanel);

        JLabel lblBank = new JLabel("Bank");
        mainPanel.add(lblBank, "2, 2, right, default");

        bankCb = new JComboBox();
        mainPanel.add(bankCb, "4, 2, 3, 1");

        JLabel lblFeeder = new JLabel("Feeder");
        mainPanel.add(lblFeeder, "2, 4, right, default");

        feederCb = new JComboBox();
        mainPanel.add(feederCb, "4, 4, 3, 1");

        JButton addBtn = new JButton(addFeederAction);
        mainPanel.add(addBtn, "8, 4");

        JButton removeBtn = new JButton(removeFeederAction);
        mainPanel.add(removeBtn, "10, 4");

        JLabel lblFeederName = new JLabel("Feeder Name");
        mainPanel.add(lblFeederName, "2, 6, right, default");

        nameTf = new JTextField();
        mainPanel.add(nameTf, "4, 6, 3, 1");
        nameTf.setColumns(10);

        JLabel lblPart = new JLabel("Part");
        mainPanel.add(lblPart, "2, 8, right, default");

        partCb = new JComboBox();
        partCb.setModel(new PartsComboBoxModel());
        partCb.setRenderer(new IdentifiableListCellRenderer<Part>());
        mainPanel.add(partCb, "4, 8, 3, 1");

        JLabel lblNewLabel = new JLabel("X");
        mainPanel.add(lblNewLabel, "4, 10");

        JLabel lblY = new JLabel("Y");
        mainPanel.add(lblY, "6, 10");

        JLabel lblZ = new JLabel("Z");
        mainPanel.add(lblZ, "8, 10");

        JLabel lblRotation = new JLabel("Rotation");
        mainPanel.add(lblRotation, "10, 10");

        JLabel lblOffsets = new JLabel("Offsets");
        mainPanel.add(lblOffsets, "2, 12, right, default");

        xOffsetTf = new JTextField();
        mainPanel.add(xOffsetTf, "4, 12");
        xOffsetTf.setColumns(10);

        yOffsetTf = new JTextField();
        mainPanel.add(yOffsetTf, "6, 12");
        yOffsetTf.setColumns(10);

        zOffsetTf = new JTextField();
        mainPanel.add(zOffsetTf, "8, 12");
        zOffsetTf.setColumns(10);

        rotOffsetTf = new JTextField();
        mainPanel.add(rotOffsetTf, "10, 12");
        rotOffsetTf.setColumns(10);

        offsetLocBtns = new LocationButtonsPanel((JTextField) null, (JTextField) null,
                (JTextField) null, (JTextField) null);
        mainPanel.add(offsetLocBtns, "12, 12, default, fill");

        JPanel dialogControlsPanel = new JPanel();
        panel.add(dialogControlsPanel, BorderLayout.SOUTH);
        FlowLayout flowLayout = (FlowLayout) dialogControlsPanel.getLayout();
        flowLayout.setAlignment(FlowLayout.RIGHT);

        JButton btnClose = new JButton("Close");
        btnClose.addActionListener(e -> setVisible(false));
        dialogControlsPanel.add(btnClose);

        createBindings();

        bankCb.setSelectedIndex(0);
        feederCb.setSelectedIndex(0);
    }

    private void createBindings() {
        LengthConverter lengthConverter = new LengthConverter();
        DoubleConverter doubleConverter =
                new DoubleConverter(Configuration.get().getLengthDisplayFormat());

        BeanUtils.bind(UpdateStrategy.READ_WRITE, feederCb, "selectedItem.name", nameTf, "text")
                .addBindingListener(new AbstractBindingListener() {
                    @Override
                    public void synced(Binding binding) {
                        SwingUtilities.invokeLater(() -> feederCb.repaint());
                    }
                });
        BeanUtils.bind(UpdateStrategy.READ_WRITE, feederCb, "selectedItem.part", partCb,
                "selectedItem");

        MutableLocationProxy offsets = new MutableLocationProxy();
        BeanUtils.bind(UpdateStrategy.READ_WRITE, feederCb, "selectedItem.offsets", offsets,
                "location");
        BeanUtils.bind(UpdateStrategy.READ_WRITE, offsets, "lengthX", xOffsetTf, "text",
                lengthConverter);
        BeanUtils.bind(UpdateStrategy.READ_WRITE, offsets, "lengthY", yOffsetTf, "text",
                lengthConverter);
        BeanUtils.bind(UpdateStrategy.READ_WRITE, offsets, "lengthZ", zOffsetTf, "text",
                lengthConverter);
        BeanUtils.bind(UpdateStrategy.READ_WRITE, offsets, "rotation", rotOffsetTf, "text",
                doubleConverter);


        for (Bank bank : ReferenceSlotAutoFeeder.getBanks()) {
            bankCb.addItem(bank);
        }

        bankCb.addActionListener(e -> {
            feederCb.removeAllItems();
            Bank bank = (Bank) bankCb.getSelectedItem();
            if (bank != null) {
                for (Feeder f : bank.getFeeders()) {
                    feederCb.addItem(f);
                }
            }
        });

        ComponentDecorators.decorateWithAutoSelect(nameTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(xOffsetTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(yOffsetTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(zOffsetTf);
        ComponentDecorators.decorateWithAutoSelect(rotOffsetTf);
    }

    private Action addFeederAction = new AbstractAction("Add") {
        @Override
        public void actionPerformed(ActionEvent e) {
            Bank bank = (Bank) bankCb.getSelectedItem();
            Feeder f = new Feeder();
            bank.getFeeders().add(f);
            feederCb.addItem(f);
            feederCb.setSelectedItem(f);
        }
    };

    private Action removeFeederAction = new AbstractAction("Remove") {
        @Override
        public void actionPerformed(ActionEvent e) {
            Feeder feeder = (Feeder) feederCb.getSelectedItem();
            Bank bank = (Bank) bankCb.getSelectedItem();
            bank.getFeeders().remove(feeder);
            feederCb.removeItem(feeder);
        }
    };
}
