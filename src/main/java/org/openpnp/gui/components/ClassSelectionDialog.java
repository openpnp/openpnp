/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.gui.components;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

@SuppressWarnings("serial")
public class ClassSelectionDialog<T> extends JDialog {
    private Class<? extends T> selectedClass;
    private JList list;

    public ClassSelectionDialog(Frame parent, String title, String description,
            List<Class<? extends T>> classes) {
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
                new JLabel("Please select an implemention class from the list.");
        lblDescription.setBorder(new EmptyBorder(4, 4, 8, 4));
        panel.add(lblDescription, BorderLayout.NORTH);
        lblDescription.setHorizontalAlignment(SwingConstants.LEFT);

        lblDescription.setText(description);
        list = new JList();
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
        panel.add(new JScrollPane(list), BorderLayout.CENTER);
        // setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setSize(400, 400);
        setLocationRelativeTo(parent);

        DefaultListModel listModel = new DefaultListModel();
        list.setModel(listModel);
        for (Class<? extends T> clz : classes) {
            listModel.addElement(new ClassListItem<>(clz));
        }

        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                selectAction.setEnabled(ClassSelectionDialog.this.list.getSelectedValue() != null);
            }
        });

        JRootPane rootPane = getRootPane();
        KeyStroke stroke = KeyStroke.getKeyStroke("ESCAPE");
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(stroke, "ESCAPE");
        rootPane.getActionMap().put("ESCAPE", cancelAction);

        selectAction.setEnabled(false);
    }

    private final Action selectAction = new AbstractAction("Accept") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            selectedClass = ((ClassListItem<T>) list.getSelectedValue()).getTheClass();
            setVisible(false);
        }
    };

    private final Action cancelAction = new AbstractAction("Cancel") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            setVisible(false);
        }
    };

    public Class<? extends T> getSelectedClass() {
        return selectedClass;
    }

    private class ClassListItem<T1> {
        private Class<? extends T1> clz;

        public ClassListItem(Class<? extends T1> clz) {
            this.clz = clz;
        }

        @Override
        public String toString() {
            return clz.getSimpleName();
        }

        public Class<? extends T1> getTheClass() {
            return clz;
        }
    }
}
