package org.openpnp.gui.components;

import org.openpnp.model.Package;
import org.openpnp.model.Part;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import static javax.swing.SwingConstants.TOP;

@SuppressWarnings("serial")
public class PartPackageSelectionDialog extends JDialog {
    public static final String ESCAPE = "ESCAPE";
    private Part selectedPart;
    private Package selectedPackage;
    private JList<Part> partSelectionJList;
    private JList<Package> packageSelectionJList;

    private Frame parent;
    private List<Part> partList;
    private List<Package> packageList;

    private int selectedPane = 0;

    public PartPackageSelectionDialog(Frame parent, String title, String description,
                                      List<Part> partList, List<Package> packageList) {
        super(parent, title, true);

        this.parent = parent;
        this.partList = partList;
        this.packageList = packageList;

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setSize(400, 400);
        setLocationRelativeTo(parent);

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

        JRootPane rootPane = getRootPane();
        KeyStroke stroke = KeyStroke.getKeyStroke(ESCAPE);
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(stroke, ESCAPE);
        rootPane.getActionMap().put(ESCAPE, cancelAction);

        panel.add(createCenterTabbedPane(), BorderLayout.CENTER);
    }

    private JTabbedPane createCenterTabbedPane() {
        JTabbedPane tabs = new JTabbedPane(TOP);

        tabs.addTab("Parts", null, createPartSelectionSelectionPane(), null);
        tabs.addTab("Packages", null, createPackagesSelectionSelectionPane(), null);

        tabs.addChangeListener(e -> selectedPane = tabs.getSelectedIndex());

        return tabs;
    }

    private JScrollPane createPartSelectionSelectionPane() {
        partSelectionJList = new JList<>();
        setListActions(partSelectionJList);

        JScrollPane scrollPane = new JScrollPane(partSelectionJList);

        DefaultListModel<Part> listModel = new DefaultListModel<>();
        partSelectionJList.setModel(listModel);
        for (Part item : partList) {
            listModel.addElement(item);
        }

        partSelectionJList.setCellRenderer(new IdRenderer());

        selectAction.setEnabled(false);

        return scrollPane;
    }

    private JScrollPane createPackagesSelectionSelectionPane() {
        packageSelectionJList = new JList<>();
        setListActions(packageSelectionJList);

        JScrollPane scrollPane = new JScrollPane(packageSelectionJList);

        DefaultListModel<Package> listModel = new DefaultListModel<>();
        packageSelectionJList.setModel(listModel);
        for (Package item : packageList) {
            listModel.addElement(item);
        }

        packageSelectionJList.setCellRenderer(new PackageRenderer());

        return scrollPane;
    }

    private void setListActions(JList list) {
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    selectAction.actionPerformed(null);
                }
            }
        });

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));

        list.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            selectAction.setEnabled(list.getSelectedValue() != null);
        });
    }

    private final Action selectAction = new AbstractAction("Accept") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (selectedPane == 0) {
                selectedPart = partSelectionJList.getSelectedValue();
            } else {
                selectedPackage = packageSelectionJList.getSelectedValue();
            }
            setVisible(false);
        }
    };

    private final Action cancelAction = new AbstractAction("Cancel") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            setVisible(false);
        }
    };

    public int getSelectedPane() {
        return selectedPane;
    }

    public Part getSelectedPart() {
        return selectedPart;
    }
    public Package getSelectedPackage() {
        return selectedPackage;
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

    class PackageRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            Package pkg = (Package) value;
            setText(pkg.getId());

            return this;
        }
    }
}
