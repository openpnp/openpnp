package org.openpnp.gui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.openpnp.spi.Camera;

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
