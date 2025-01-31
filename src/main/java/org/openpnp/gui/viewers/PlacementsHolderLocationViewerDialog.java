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

import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.prefs.Preferences;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.openpnp.Translations;
import org.openpnp.gui.MainFrame;
import org.openpnp.model.Board;
import org.openpnp.model.Panel;
import org.openpnp.model.PlacementsHolder;
import org.openpnp.model.PlacementsHolderLocation;


@SuppressWarnings("serial")
public class PlacementsHolderLocationViewerDialog extends JFrame {
    private static final String PREF_WINDOW_X = "PlacementsHolderLocationViewerDialog.windowX"; //$NON-NLS-1$
    private static final int PREF_WINDOW_X_DEF = 100;
    private static final String PREF_WINDOW_Y = "PlacementsHolderLocationViewerDialog.windowY"; //$NON-NLS-1$
    private static final int PREF_WINDOW_Y_DEF = 100;
    private static final String PREF_WINDOW_WIDTH = "PlacementsHolderLocationViewerDialog.windowWidth"; //$NON-NLS-1$
    private static final int PREF_WINDOW_WIDTH_DEF = 800;
    private static final String PREF_WINDOW_HEIGHT = "PlacementsHolderLocationViewerDialog.windowHeight"; //$NON-NLS-1$
    private static final int PREF_WINDOW_HEIGHT_DEF = 600;

    private boolean isJob;
    private PlacementsHolderLocationViewer contentPane;
    private Preferences prefs = Preferences.userNodeForPackage(PlacementsHolderLocationViewerDialog.class);
    private String prefsSuffix;
    
    /**
     * Create the frame with filter.
     */
    public PlacementsHolderLocationViewerDialog(PlacementsHolderLocation<?> placementsHolderLocation, boolean isJob, List<PlacementsHolderLocation<?>> selections) {

        this.isJob = isJob;
        
        setTitle(placementsHolderLocation.getPlacementsHolder());
        
        if (prefs.getInt(PREF_WINDOW_WIDTH + prefsSuffix, 50) < 50) {
            prefs.putInt(PREF_WINDOW_WIDTH + prefsSuffix, PREF_WINDOW_WIDTH_DEF);
        }

        if (prefs.getInt(PREF_WINDOW_HEIGHT + prefsSuffix, 50) < 50) {
            prefs.putInt(PREF_WINDOW_HEIGHT + prefsSuffix, PREF_WINDOW_HEIGHT_DEF);
        }

        if (prefs.getInt(PREF_WINDOW_X + prefsSuffix, Integer.MIN_VALUE) == Integer.MIN_VALUE) {
            Rectangle mfBounds = MainFrame.get().getBounds();
            prefs.putInt(PREF_WINDOW_X + prefsSuffix, 
                    mfBounds.x + (mfBounds.width - PREF_WINDOW_WIDTH_DEF)/2);
            prefs.putInt(PREF_WINDOW_Y + prefsSuffix, 
                    mfBounds.y + (mfBounds.height - PREF_WINDOW_HEIGHT_DEF)/2);
        }
        
        setBounds(prefs.getInt(PREF_WINDOW_X + prefsSuffix, PREF_WINDOW_X_DEF),
                prefs.getInt(PREF_WINDOW_Y + prefsSuffix, PREF_WINDOW_Y_DEF),
                prefs.getInt(PREF_WINDOW_WIDTH + prefsSuffix, PREF_WINDOW_WIDTH_DEF),
                prefs.getInt(PREF_WINDOW_HEIGHT + prefsSuffix, PREF_WINDOW_HEIGHT_DEF));
        
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                prefs.putInt(PREF_WINDOW_X + prefsSuffix, getLocation().x);
                prefs.putInt(PREF_WINDOW_Y + prefsSuffix, getLocation().y);
            }

            @Override
            public void componentResized(ComponentEvent e) {
                prefs.putInt(PREF_WINDOW_WIDTH + prefsSuffix, getSize().width);
                prefs.putInt(PREF_WINDOW_HEIGHT + prefsSuffix, getSize().height);
            }
        });
        
        addWindowListener(new WindowAdapter( ) {

            @Override
            public void windowClosing(WindowEvent e) {
                contentPane.cancel();
            }
        });

        contentPane = new PlacementsHolderLocationViewer(placementsHolderLocation, isJob, selections);
        setContentPane(contentPane);
    }

    protected void setTitle(PlacementsHolder<?> placementsHolder) {
        if (isJob) {
            setTitle(Translations.getString("PlacementsHolderLocationViewer.TitleType.Job") + //$NON-NLS-1$
                    " - " + MainFrame.get().getTitle()); //$NON-NLS-1$
            prefsSuffix = ".Job"; //$NON-NLS-1$
        }
        else if (placementsHolder instanceof Board) {
            setTitle(Translations.getString("PlacementsHolderLocationViewer.TitleType.Board") +  //$NON-NLS-1$
                    " - " + placementsHolder.getName()); //$NON-NLS-1$
            prefsSuffix = ".Board"; //$NON-NLS-1$
        }
        else if (placementsHolder instanceof Panel) {
            setTitle(Translations.getString("PlacementsHolderLocationViewer.TitleType.Panel") +  //$NON-NLS-1$
                    " - " + placementsHolder.getName()); //$NON-NLS-1$
            prefsSuffix = ".Panel"; //$NON-NLS-1$
        }
    }

    public void setPlacementsHolder(PlacementsHolder<?> placementsHolder) {
        setPlacementsHolder(placementsHolder, null);
    }

    public void setPlacementsHolder(PlacementsHolder<?> placementsHolder, List<PlacementsHolderLocation<?>> selections) {
        contentPane.setPlacementsHolder(placementsHolder, selections);
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
