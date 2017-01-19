package org.openpnp.machine.reference.feeder.wizards.slot;

import java.awt.BorderLayout;
import java.awt.Color;
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
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AbstractBindingListener;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.Binding;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.machine.reference.feeder.ReferenceSlotAutoFeeder;
import org.openpnp.machine.reference.feeder.ReferenceSlotAutoFeeder.Bank;
import org.openpnp.util.BeanUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class SlotBankConfigDialog extends JDialog {
    private JTextField bankNameTf;
    private JComboBox bankCb;

    public SlotBankConfigDialog(Frame owner) {
        super(owner, "Slot Feeder Banks", true);

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
                "Banks", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        getContentPane().add(panel, BorderLayout.CENTER);
        panel.setLayout(new BorderLayout(0, 0));

        JPanel mainPanel = new JPanel();
        panel.add(mainPanel, BorderLayout.CENTER);
        FormLayout fl_mainPanel = new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,});
        mainPanel.setLayout(fl_mainPanel);

        JLabel lblBank = new JLabel("Bank");
        mainPanel.add(lblBank, "2, 2, right, default");

        bankCb = new JComboBox();
        mainPanel.add(bankCb, "4, 2, fill, default");

        JButton addBankBtn = new JButton(addBankAction);
        mainPanel.add(addBankBtn, "6, 2");

        JButton removeBankBtn = new JButton(removeBankAction);
        mainPanel.add(removeBankBtn, "8, 2");

        JLabel lblName = new JLabel("Name");
        mainPanel.add(lblName, "2, 4");

        bankNameTf = new JTextField();
        mainPanel.add(bankNameTf, "4, 4, fill, default");
        bankNameTf.setColumns(20);

        JPanel dialogControlsPanel = new JPanel();
        panel.add(dialogControlsPanel, BorderLayout.SOUTH);
        FlowLayout flowLayout = (FlowLayout) dialogControlsPanel.getLayout();
        flowLayout.setAlignment(FlowLayout.RIGHT);

        JButton btnClose = new JButton("Close");
        btnClose.addActionListener(e -> setVisible(false));
        dialogControlsPanel.add(btnClose);

        createBindings();

        for (Bank bank : ReferenceSlotAutoFeeder.getBanks()) {
            bankCb.addItem(bank);
        }
        bankCb.setSelectedIndex(0);
    }

    private void createBindings() {
        BeanUtils.bind(UpdateStrategy.READ_WRITE, bankCb, "selectedItem.name", bankNameTf, "text")
                .addBindingListener(new AbstractBindingListener() {
                    @Override
                    public void synced(Binding binding) {
                        SwingUtilities.invokeLater(() -> bankCb.repaint());
                    }
                });
        ComponentDecorators.decorateWithAutoSelect(bankNameTf);
    }

    private Action addBankAction = new AbstractAction("Add") {
        @Override
        public void actionPerformed(ActionEvent e) {
            Bank bank = new Bank();
            ReferenceSlotAutoFeeder.getBanks().add(bank);
            bankCb.addItem(bank);
            bankCb.setSelectedItem(bank);
        }
    };

    private Action removeBankAction = new AbstractAction("Remove") {
        @Override
        public void actionPerformed(ActionEvent e) {
            Bank bank = (Bank) bankCb.getSelectedItem();
            if (ReferenceSlotAutoFeeder.getBanks().size() < 2) {
                MessageBoxes.errorBox(getOwner(), "Error",
                        "Can't delete the only bank. There must always be one bank defined.");
                return;
            }
            ReferenceSlotAutoFeeder.getBanks().remove(bank);
            bankCb.removeItem(bank);
        }
    };
}
