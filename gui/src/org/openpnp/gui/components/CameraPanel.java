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
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.openpnp.spi.Camera;

/**
 * Shows a a 3x3 grid of 9 cameras or a blown up image from a single camera. 
 * @author jason
 * TODO add a way to identify a particular camera in grid view; maybe tooltip
 */
@SuppressWarnings("serial")
public class CameraPanel extends JPanel {
	private static int maximumFps = 10;
	
	private Set<TitledCamera> cameras = new HashSet<TitledCamera>();
	
	private JComboBox camerasCombo;
	private JPanel camerasPanel;
	
	public CameraPanel() {
		createUi();
		
		camerasCombo.setSelectedIndex(1);
	}
	
	public void addCamera(Camera camera, String title) {
		TitledCamera titledCamera = new TitledCamera(camera, title);
		cameras.add(titledCamera);
		camerasCombo.addItem(titledCamera);
		camerasCombo.setSelectedIndex(camerasCombo.getSelectedIndex());
	}
	
	private void createUi() {
		camerasPanel = new JPanel();

		camerasCombo = new JComboBox();
		camerasCombo.addActionListener(new CameraSelectedAction());
		camerasCombo.addItem("Show None");
		camerasCombo.addItem("Show All");

		setLayout(new BorderLayout());
		
		add(camerasCombo, BorderLayout.NORTH);
		add(camerasPanel);
	}
	
	class CameraSelectedAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent ev) {
			if (camerasCombo.getSelectedItem().equals("Show None")) {
				clearCameras();
				camerasPanel.setLayout(new BorderLayout());
				JPanel panel = new JPanel();
				panel.setBackground(Color.black);
				camerasPanel.add(panel);
			}
			else if (camerasCombo.getSelectedItem().equals("Show All")) {
				clearCameras();
				camerasPanel.setLayout(new GridLayout(0, 3, 1, 1));
				for (TitledCamera camera : cameras) {
					CameraView cameraView = new CameraView(maximumFps / cameras.size());
					cameraView.setCamera(camera.camera);
					cameraView.setShowCrosshair(false);
					camerasPanel.add(cameraView);
				}
				for (int i = 0; i < 9 - cameras.size(); i++) {
					JPanel panel = new JPanel();
					panel.setBackground(Color.black);
					camerasPanel.add(panel);
				}
			}
			else {
				clearCameras();
				camerasPanel.setLayout(new BorderLayout());
				CameraView cameraView = new CameraView(maximumFps);
				cameraView.setCamera(((TitledCamera) camerasCombo.getSelectedItem()).camera);
				camerasPanel.add(cameraView);
			}
			revalidate();
			repaint();
		}
		
		private void clearCameras() {
			for (Component comp : camerasPanel.getComponents()) {
				if (comp instanceof CameraView) {
					((CameraView) comp).setCamera(null);
				}
			}
			camerasPanel.removeAll();
		}
	}
	
	class TitledCamera {
		public String title;
		public Camera camera;
		
		public TitledCamera(Camera camera, String title) {
			this.title = title;
			this.camera = camera;
		}
		
		@Override
		public String toString() {
			return title;
		}
	}
}
