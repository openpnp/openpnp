/*
 * Copyright (C) 2022 Jason von Nieda <jason@vonnieda.org>, Tony Luken <tonyluken62+openpnp@gmail.com>
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
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.openpnp.gui.MainFrame;
import org.openpnp.model.Board;
import org.openpnp.model.Configuration;
import org.openpnp.model.Panel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.border.BevelBorder;
import javax.swing.JScrollPane;

@SuppressWarnings("serial")
public class ExistingBoardOrPanelDialog extends JDialog {

    private final JPanel contentPanel = new JPanel();
    private String fileExtension;
    private List<File> existingList = new ArrayList<>();
    private File file = null;
    @SuppressWarnings("rawtypes")
    private JList existingListDisplay;
    protected Exception savedException;
    private String boardOrPanel;

    /**
     * Create the dialog.
     */
    public ExistingBoardOrPanelDialog(Configuration configuration, Class<?> type, String title) {
        if (type == Board.class) {
            boardOrPanel = "board";
            for (Board board : configuration.getBoards()) {
                existingList.add(board.getFile());
            }
        }
        else if (type == Panel.class) {
            boardOrPanel = "panel";
            for (Panel panel : configuration.getPanels()) {
                existingList.add(panel.getFile());
            }
        }
        else {
            throw new UnsupportedOperationException("Unsupported operation for class " + type);
        }
        fileExtension = "." + boardOrPanel + ".xml";
        setTitle(title);
        setModalityType(ModalityType.APPLICATION_MODAL);
        setBounds(100, 100, 600, 400);
        getContentPane().setLayout(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        getContentPane().add(contentPanel, BorderLayout.CENTER);
        contentPanel.setLayout(new BorderLayout(0, 0));
        {
            JLabel txtrSelectOneFile = new JLabel("Select a " + boardOrPanel + 
                    " from the list below or click Browse to locate one in the file system");
            contentPanel.add(txtrSelectOneFile, BorderLayout.NORTH);
        }
        {
            existingListDisplay = new JList<>(existingList.toArray());
            existingListDisplay.setVisibleRowCount(8);
            existingListDisplay.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
            existingListDisplay.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JScrollPane scrollPane = new JScrollPane(existingListDisplay);
            contentPanel.add(scrollPane, BorderLayout.CENTER);
        }
        {
            JPanel buttonPane = new JPanel();
            buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
            getContentPane().add(buttonPane, BorderLayout.SOUTH);
            {
                JButton okButton = new JButton("OK");
                okButton.setActionCommand("OK");
                okButton.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        file = (File) existingListDisplay.getSelectedValue();
                        close();
                    }});
                buttonPane.add(okButton);
                getRootPane().setDefaultButton(okButton);
            }
            {
                JButton browseButton = new JButton("Browse");
                browseButton.setActionCommand("Browse");
                browseButton.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        close();
                        FileDialog fileDialog = new FileDialog(MainFrame.get());
                        fileDialog.setTitle(getTitle());
                        fileDialog.setFilenameFilter(new FilenameFilter() {
                            @Override
                            public boolean accept(File dir, String name) {
                                return name.toLowerCase().endsWith(fileExtension); //$NON-NLS-1$
                            }
                        });
                        fileDialog.setFile("*" + fileExtension);
                        fileDialog.setVisible(true);
                        
                        if (fileDialog.getFile() == null) {
                            file = null;
                            return;
                        }
                        file = new File(new File(fileDialog.getDirectory()), fileDialog.getFile());
                    }});
                buttonPane.add(browseButton);
            }
            {
                JButton cancelButton = new JButton("Cancel");
                cancelButton.setActionCommand("Cancel");
                cancelButton.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        file = null;
                        close();
                    }});
                buttonPane.add(cancelButton);
            }
        }
    }

    public File getFile() {
        return file;
    }
    
    public void close() {
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

}
