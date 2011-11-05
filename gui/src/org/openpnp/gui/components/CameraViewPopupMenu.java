package org.openpnp.gui.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

import org.openpnp.LengthUnit;
import org.openpnp.gui.components.reticle.CrosshairReticle;
import org.openpnp.gui.components.reticle.RulerReticle;

public class CameraViewPopupMenu extends JPopupMenu {
	private CameraView cameraView;
	private JCheckBoxMenuItem calibrationModeCheckMenuItem;
	private JMenu reticleMenu;
	
	public CameraViewPopupMenu(CameraView cameraView) {
		this.cameraView = cameraView;
		
		reticleMenu = createReticleMenu();
		JMenu maxFpsMenu = createMaxFpsMenu();
		JCheckBoxMenuItem calibrationModeCheckMenuItem = new JCheckBoxMenuItem(calibrationModeAction);
		
		add(reticleMenu);
		add(maxFpsMenu);
		add(calibrationModeCheckMenuItem);
		addSeparator();
		add("Cancel");
	}
	
	private JMenu createMaxFpsMenu() {
		ButtonGroup buttonGroup = new ButtonGroup();
		JMenu menu = new JMenu("Maximum FPS");
		JRadioButtonMenuItem menuItem;
		
		menuItem = new JRadioButtonMenuItem("1");
		menuItem.addActionListener(maxFpsAction);
		buttonGroup.add(menuItem);
		menu.add(menuItem);
		menuItem = new JRadioButtonMenuItem("5");
		menuItem.addActionListener(maxFpsAction);
		buttonGroup.add(menuItem);
		menu.add(menuItem);
		menuItem = new JRadioButtonMenuItem("10");
		menuItem.addActionListener(maxFpsAction);
		buttonGroup.add(menuItem);
		menu.add(menuItem);
		menuItem = new JRadioButtonMenuItem("15");
		menuItem.addActionListener(maxFpsAction);
		buttonGroup.add(menuItem);
		menu.add(menuItem);
		menuItem = new JRadioButtonMenuItem("24");
		menuItem.addActionListener(maxFpsAction);
		buttonGroup.add(menuItem);
		menu.add(menuItem);
		menuItem = new JRadioButtonMenuItem("30");
		menuItem.addActionListener(maxFpsAction);
		buttonGroup.add(menuItem);
		menu.add(menuItem);
		menuItem = new JRadioButtonMenuItem("45");
		menuItem.addActionListener(maxFpsAction);
		buttonGroup.add(menuItem);
		menu.add(menuItem);
		menuItem = new JRadioButtonMenuItem("60");
		menuItem.addActionListener(maxFpsAction);
		buttonGroup.add(menuItem);
		menu.add(menuItem);
		
		return menu;
	}
	
	private JMenu createReticleMenu() {
		JMenu menu = new JMenu("Reticle");

		ButtonGroup buttonGroup = new ButtonGroup();
		
		JRadioButtonMenuItem menuItem;
		
		menuItem = new JRadioButtonMenuItem(noneReticleAction);
		menuItem.setSelected(true);
		buttonGroup.add(menuItem);
		menu.add(menuItem);
		menuItem = new JRadioButtonMenuItem(crosshairReticleAction);
		buttonGroup.add(menuItem);
		menu.add(menuItem);
		menuItem = new JRadioButtonMenuItem(mmRulerReticleAction);
		buttonGroup.add(menuItem);
		menu.add(menuItem);
		menuItem = new JRadioButtonMenuItem(inchRulerReticleAction);
		buttonGroup.add(menuItem);
		menu.add(menuItem);
		
		return menu;
	}
	
	private Action noneReticleAction = new AbstractAction("None") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			cameraView.setReticle(null);
		}
	};
	
	private Action crosshairReticleAction = new AbstractAction("Crosshair") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			cameraView.setReticle(new CrosshairReticle(Color.red));
		}
	};
	
	private Action mmRulerReticleAction = new AbstractAction("Millimeter Ruler") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			cameraView.setReticle(new RulerReticle(LengthUnit.Millimeters, 1, Color.red));
		}
	};
	
	private Action inchRulerReticleAction = new AbstractAction("Inch Ruler") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			cameraView.setReticle(new RulerReticle(LengthUnit.Inches, 1, Color.red));
		}
	};
	
	private Action maxFpsAction = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			int maximumFps = Integer.parseInt(e.getActionCommand());
			cameraView.setMaximumFps(maximumFps);
		}
	};
	
	private Action calibrationModeAction = new AbstractAction("Calibration Mode") {
		@Override
		public void actionPerformed(ActionEvent e) {
			cameraView.setCalibrationMode(((JCheckBoxMenuItem) e.getSource()).isSelected());
		}
	};
}
