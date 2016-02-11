/*
 	Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 	
 	This file is part of OpenPnP.
 	
	OpenPnP is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenPnP is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenPnP.  If not, see <http://www.gnu.org/licenses/>.
 	
 	For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.gui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.openpnp.gui.support.CameraItem;
import org.openpnp.spi.Camera;

/**
 * Shows a square grid of cameras or a blown up image from a single camera.
 */
@SuppressWarnings("serial")
public class CameraPanel extends JPanel {
	private static int maximumFps = 15;
	private static final String SHOW_NONE_ITEM = "Show None";
	private static final String SHOW_ALL_ITEM = "Show All";

	private Map<Camera, CameraView> cameraViews = new LinkedHashMap<>();

	private JComboBox camerasCombo;
	private JPanel camerasPanel;
	
	private CameraView selectedCameraView;
	
	public CameraPanel() {
		createUi();
	}

	public void addCamera(Camera camera) {
		CameraView cameraView = new CameraView(maximumFps / Math.max(cameraViews.size(), 1));
		cameraView.setCamera(camera);
		cameraViews.put(camera, cameraView);
		camerasCombo.addItem(new CameraItem(camera));
		if (cameraViews.size() == 1) {
			// First camera being added, so select it
			camerasCombo.setSelectedIndex(1);
		}
		else if (cameraViews.size() == 2) {
			// Otherwise this is the second camera so mix in the 
			// show all item.
			camerasCombo.insertItemAt(SHOW_ALL_ITEM, 1);
		}
	}
	
	private void createUi() {
		camerasPanel = new JPanel();

		camerasCombo = new JComboBox();
		camerasCombo.addActionListener(cameraSelectedAction);

		setLayout(new BorderLayout());

		camerasCombo.addItem(SHOW_NONE_ITEM);
		
		add(camerasCombo, BorderLayout.NORTH);
		add(camerasPanel);
	}
	
	/**
	 * Make sure the given Camera is visible in the UI. If All Cameras is
	 * selected we do nothing, otherwise we select the specified Camera.
	 * @param camera
	 * @return
	 */
	public void ensureCameraVisible(Camera camera) {
		if (camerasCombo.getSelectedItem().equals(SHOW_ALL_ITEM)) {
			return;
		}
		setSelectedCamera(camera);
	}
	
	public CameraView setSelectedCamera(Camera camera) {
		if (selectedCameraView != null && selectedCameraView.getCamera() == camera) {
			return selectedCameraView;
		}
		for (int i = 0; i < camerasCombo.getItemCount(); i++) {
			Object o = camerasCombo.getItemAt(i);
			if (o instanceof CameraItem) {
				Camera c = ((CameraItem) o).getCamera();
				if (c == camera) {
					camerasCombo.setSelectedIndex(i);
					return selectedCameraView;
				}
			}
		}
		return null;
	}
	
	public CameraView getCameraView(Camera camera) {
		return cameraViews.get(camera);
	}
	
	private AbstractAction cameraSelectedAction = new AbstractAction("") {
		@Override
		public void actionPerformed(ActionEvent ev) {
			selectedCameraView = null;
			camerasPanel.removeAll();
			if (camerasCombo.getSelectedItem().equals(SHOW_NONE_ITEM)) {
				camerasPanel.setLayout(new BorderLayout());
				JPanel panel = new JPanel();
				panel.setBackground(Color.black);
				camerasPanel.add(panel);
				selectedCameraView = null;
			}
			else if (camerasCombo.getSelectedItem().equals(SHOW_ALL_ITEM)) {
				int columns = (int) Math.ceil(Math.sqrt(cameraViews.size()));
				if (columns == 0) {
					columns = 1;
				}
				camerasPanel.setLayout(new GridLayout(0, columns, 1, 1));
				for (CameraView cameraView : cameraViews.values()) {
					cameraView.setMaximumFps(maximumFps / Math.max(cameraViews.size(), 1));
					camerasPanel.add(cameraView);
					if (cameraViews.size() == 1) {
						selectedCameraView = cameraView;
					}
				}
				for (int i = 0; i < (columns * columns) - cameraViews.size(); i++) {
					JPanel panel = new JPanel();
					panel.setBackground(Color.black);
					camerasPanel.add(panel);
				}
				selectedCameraView = null;
			}
			else {
				camerasPanel.setLayout(new BorderLayout());
				Camera camera = ((CameraItem) camerasCombo.getSelectedItem()).getCamera();
				CameraView cameraView = getCameraView(camera);
				cameraView.setMaximumFps(maximumFps);
				camerasPanel.add(cameraView);
				
				selectedCameraView = cameraView;
			}
			revalidate();
			repaint();
		}
	};
}
