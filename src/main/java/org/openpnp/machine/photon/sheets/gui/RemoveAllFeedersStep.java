package org.openpnp.machine.photon.sheets.gui;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import org.openpnp.Translations;

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
		helpText.setText(Translations.getString("PhotonFeeder.RemoveAllFeedersStep.helpText")); //$NON-NLS-1$
		add(helpText, BorderLayout.CENTER);

	}

}
