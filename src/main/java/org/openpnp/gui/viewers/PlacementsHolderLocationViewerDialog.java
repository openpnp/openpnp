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

package org.openpnp.gui.viewers;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.BiConsumer;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.openpnp.Translations;
import org.openpnp.gui.MainFrame;
import org.openpnp.model.Board;
import org.openpnp.model.PlacementsHolder;
import org.openpnp.model.PlacementsHolderLocation;


@SuppressWarnings("serial")
public class PlacementsHolderLocationViewerDialog extends JFrame {

    private boolean isJob;
    private PlacementsHolderLocationViewer contentPane;
    
    /**
     * Create the frame.
     */
    public PlacementsHolderLocationViewerDialog(PlacementsHolderLocation<?> placementsHolderLocation, 
            boolean isJob, BiConsumer<PlacementsHolderLocation<?>, String> refreshTableModel) {
        this.isJob = isJob;
        setBounds(100, 100, 800, 600);
        
        addWindowListener(new WindowAdapter( ) {

            @Override
            public void windowClosing(WindowEvent e) {
                contentPane.cancel();
            }
        });

//        if (isJob) {
//            setTitle(Translations.getString("PlacementsHolderLocationViewer.TitleType.Job") + 
//                    " - " + MainFrame.get().getTitle());
//        }
//        else if (placementsHolderLocation instanceof BoardLocation) {
//            setTitle(Translations.getString("PlacementsHolderLocationViewer.TitleType.Board") + 
//                    " - " + placementsHolderLocation.getPlacementsHolder().getFile().getName());
//        }
//        else {
//            setTitle(Translations.getString("PlacementsHolderLocationViewer.TitleType.Panel") + 
//                    " - " + placementsHolderLocation.getPlacementsHolder().getFile().getName());
//        }
        setTitle(placementsHolderLocation.getPlacementsHolder());
        
        contentPane = new PlacementsHolderLocationViewer(placementsHolderLocation, isJob, 
                refreshTableModel);
        setContentPane(contentPane);
    }

    protected void setTitle(PlacementsHolder<?> placementsHolder) {
        if (isJob) {
            setTitle(Translations.getString("PlacementsHolderLocationViewer.TitleType.Job") + 
                    " - " + MainFrame.get().getTitle());
        }
        else if (placementsHolder instanceof Board) {
            setTitle(Translations.getString("PlacementsHolderLocationViewer.TitleType.Board") + 
                    " - " + placementsHolder.getFile().getName());
        }
        else {
            setTitle(Translations.getString("PlacementsHolderLocationViewer.TitleType.Panel") + 
                    " - " + placementsHolder.getFile().getName());
        }
    }
    
    public void setPlacementsHolder(PlacementsHolder<?> placementsHolder) {
        contentPane.setPlacementsHolder(placementsHolder);
        SwingUtilities.invokeLater(() -> {
            setTitle(placementsHolder);
        });
    }
    
    public void regenerate() {
        contentPane.regenerate();
    }
  
    public void refresh() {
        contentPane.refresh();
    }
  
}
