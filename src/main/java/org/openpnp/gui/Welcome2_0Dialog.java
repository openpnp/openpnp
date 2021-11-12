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

package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URI;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import org.apache.commons.io.FileUtils;
import org.openpnp.Main;
import org.openpnp.util.XmlSerialize;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.layout.Sizes;

@SuppressWarnings("serial")
public class Welcome2_0Dialog extends JDialog {

    private final JPanel contentPanel = new JPanel();
    private JLabel textPane;
    private JPanel textSizer;

    public Welcome2_0Dialog(Frame frame) {
        super(frame, true);
        setTitle("Welcome to OpenPnP 2.0");
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setBounds(100, 100, 648, 434);
        getContentPane().setLayout(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        getContentPane().add(contentPanel, BorderLayout.CENTER);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        JLabel lblOpenpnp = new JLabel("Welcome to OpenPnP 2.0");
        lblOpenpnp.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblOpenpnp.setFont(new Font("Lucida Grande", Font.BOLD, 32));
        contentPanel.add(lblOpenpnp);
        JLabel lblCopyright = new JLabel("Copyright © 2011 - 2019 Jason von Nieda");
        lblCopyright.setFont(new Font("Lucida Grande", Font.PLAIN, 10));
        lblCopyright.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(lblCopyright);
        JLabel lblVersion = new JLabel("Version: " + Main.getVersion());
        lblVersion.setFont(new Font("Lucida Grande", Font.PLAIN, 10));
        lblVersion.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(lblVersion);

        textSizer = new JPanel();
        textSizer.setLayout(new FormLayout(new ColumnSpec[] {
                new ColumnSpec(ColumnSpec.FILL, Sizes.bounded(Sizes.PREFERRED, Sizes.constant("70dlu", true), Sizes.constant("150dlu", true)), 1),},
                new RowSpec[] {
                        FormSpecs.DEFAULT_ROWSPEC,}));
        textPane = new JLabel();
        textPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Desktop desk = Desktop.getDesktop();
                try {
                    // HACK: can't truly pinpoint hyperlinks in the text, so open the original. 
                    desk.browse(new URI("https://github.com/openpnp/openpnp/blob/develop/OPENPNP_2_0.md"));
                }
                catch (Exception e1) {
                }
            }
        });
        textSizer.add(textPane, "1, 1");
        contentPanel.add(new JScrollPane(textSizer));
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
        getContentPane().add(buttonPane, BorderLayout.SOUTH);
        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                setVisible(false);
            }
        });
        okButton.setActionCommand("OK");
        buttonPane.add(okButton);
        getRootPane().setDefaultButton(okButton);

        try {
            String s = FileUtils.readFileToString(new File("OPENPNP_2_0.md"));
            s = XmlSerialize.convertMarkupToHtml(s);
            textPane.setText(s);
        }
        catch (Exception e) {
            textPane.setText(e.toString());
        }
    }
}
