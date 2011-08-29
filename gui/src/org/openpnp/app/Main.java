package org.openpnp.app;

import javax.swing.UIManager;

import org.openpnp.gui.MainFrame;

public class Main {
	public static void main(String[] args) {
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e) {
			throw new Error(e);
		}
		MainFrame mainFrame = new MainFrame();
		mainFrame.setVisible(true);
	}
}
