package org.openpnp.machine.photon.sheets.gui;

import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

public class RemoveAllFeedersStep extends JPanel {

	/**
	 * Create the panel.
	 */
	public RemoveAllFeedersStep() {
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setLayout(new BorderLayout(0, 0));
		
		JTextArea helpText = new JTextArea();
		helpText.setEditable(false);
		helpText.setBackground(UIManager.getColor("Panel.background"));
		helpText.setWrapStyleWord(true);
		helpText.setLineWrap(true);
		helpText.setText("To program your slots, begin by removing all the Photon feeders from your machine. Then, click Next.");
		add(helpText, BorderLayout.CENTER);

	}

}
