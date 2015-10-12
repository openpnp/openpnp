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
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.openpnp.gui.support.CameraItem;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;

/**
 * Shows a square grid of cameras or a blown up image from a single camera.
 */
@SuppressWarnings("serial")
public class CameraPanel extends JPanel {
	private static int maximumFps = 15;
	private static final String SHOW_NONE_ITEM = "Show None";
	private static final String SHOW_ALL_ITEM = "Show All";

	private Set<Camera> cameras = new LinkedHashSet<Camera>();

	private JComboBox camerasCombo;
	private JPanel camerasPanel;
	
	private CameraView selectedCameraView;
	
	public CameraPanel() {
		createUi();
	}

	public void addCamera(Camera camera) {
		cameras.add(camera);
		camerasCombo.addItem(new CameraItem(camera));
		if (cameras.size() == 1) {
			// First camera being added, so select it
			camerasCombo.setSelectedIndex(1);
		}
		else if (cameras.size() == 2) {
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
	
	public CameraView getSelectedCameraView() {
		return selectedCameraView;
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
	
	public Camera getSelectedCamera() {
		if (selectedCameraView != null) {
			return selectedCameraView.getCamera();
		}
		return null;
	}
	
	public Location getSelectedCameraLocation() {
	    Camera camera = getSelectedCamera();
	    if (camera == null) {
	        return null;
	    }
	    return camera.getLocation();
	}
	
	public CameraView getCameraView(Camera camera) {
		for (Component component : camerasPanel.getComponents()) {
			if (component instanceof CameraView) {
				CameraView cameraView = (CameraView) component;
				if (cameraView.getCamera() == camera) {
					return cameraView;
				}
			}
		}
		return null;
	}
	
	private AbstractAction cameraSelectedAction = new AbstractAction("") {
		@Override
		public void actionPerformed(ActionEvent ev) {
			selectedCameraView = null;
			if (camerasCombo.getSelectedItem().equals(SHOW_NONE_ITEM)) {
				clearCameras();
				camerasPanel.setLayout(new BorderLayout());
				JPanel panel = new JPanel();
				panel.setBackground(Color.black);
				camerasPanel.add(panel);
				selectedCameraView = null;
			}
			else if (camerasCombo.getSelectedItem().equals(SHOW_ALL_ITEM)) {
				clearCameras();
				int columns = (int) Math.ceil(Math.sqrt(cameras.size()));
				if (columns == 0) {
					columns = 1;
				}
				camerasPanel.setLayout(new GridLayout(0, columns, 1, 1));
				for (Camera camera : cameras) {
					CameraView cameraView = new CameraView(maximumFps
							/ cameras.size());
					cameraView.setCamera(camera);
					camerasPanel.add(cameraView);
					
					if (cameras.size() == 1) {
						selectedCameraView = cameraView;
					}
				}
				for (int i = 0; i < (columns * columns) - cameras.size(); i++) {
					JPanel panel = new JPanel();
					panel.setBackground(Color.black);
					camerasPanel.add(panel);
				}
				selectedCameraView = null;
			}
			else {
				clearCameras();
				camerasPanel.setLayout(new BorderLayout());
				CameraView cameraView = new CameraView(maximumFps);
				Camera camera = ((CameraItem) camerasCombo.getSelectedItem()).getCamera();
				cameraView.setCamera(camera);
				
				camerasPanel.add(cameraView);
				
				selectedCameraView = cameraView;
			}
			revalidate();
			repaint();
		}
		
		private void clearCameras() {
			for (Component comp : camerasPanel.getComponents()) {
				if (comp instanceof CameraView) {
					CameraView cameraView = (CameraView) comp;
					cameraView.setCamera(null);
				}
			}
			camerasPanel.removeAll();
		}
	};
}
