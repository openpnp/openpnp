package org.openpnp.gui.components;

import org.openpnp.model.Part;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

@SuppressWarnings("serial")
public class PartPackageSelectionDialog extends JDialog {
    public static final String ESCAPE = "ESCAPE";
    private Part selectedPart;
    private JList<Part> partSelectionJList;

    public PartPackageSelectionDialog(Frame parent, String title, String description,
                                      List<Part> partList) {
        super(parent, title, true);

        JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(8, 8, 4, 8));
        getContentPane().add(panel, BorderLayout.CENTER);
        panel.setLayout(new BorderLayout(0, 0));

        JPanel panelActions = new JPanel();
        panel.add(panelActions, BorderLayout.SOUTH);
        panelActions.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));

        JButton btnCancel = new JButton(cancelAction);
        panelActions.add(btnCancel);

        JButton btnSelect = new JButton(selectAction);
        panelActions.add(btnSelect);

        JLabel lblDescription =
                new JLabel("");
        lblDescription.setBorder(new EmptyBorder(4, 4, 8, 4));
        panel.add(lblDescription, BorderLayout.NORTH);
        lblDescription.setHorizontalAlignment(SwingConstants.LEFT);
        lblDescription.setText(description);

        partSelectionJList = new JList<>();
        partSelectionJList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    selectAction.actionPerformed(null);
                }
            }
        });
        partSelectionJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        partSelectionJList.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
        panel.add(new JScrollPane(partSelectionJList), BorderLayout.CENTER);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setSize(400, 400);
        setLocationRelativeTo(parent);

        DefaultListModel<Part> listModel = new DefaultListModel<>();
        partSelectionJList.setModel(listModel);
        for (Part item : partList) {
            listModel.addElement(item);
        }

        partSelectionJList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            selectAction.setEnabled(PartPackageSelectionDialog.this.partSelectionJList.getSelectedValue() != null);
        });
        partSelectionJList.setCellRenderer(new IdRenderer());

        JRootPane rootPane = getRootPane();
        KeyStroke stroke = KeyStroke.getKeyStroke(ESCAPE);
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(stroke, ESCAPE);
        rootPane.getActionMap().put(ESCAPE, cancelAction);

        selectAction.setEnabled(false);
    }

    private final Action selectAction = new AbstractAction("Accept") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            selectedPart = partSelectionJList.getSelectedValue();
            setVisible(false);
        }
    };

    private final Action cancelAction = new AbstractAction("Cancel") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            setVisible(false);
        }
    };

    public Part getSelected() {
        return selectedPart;
    }

    class IdRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            Part part = (Part) value;
            setText(part.getId() + " (" + part.getPackage() + ")");

            return this;
        }
    }
}
