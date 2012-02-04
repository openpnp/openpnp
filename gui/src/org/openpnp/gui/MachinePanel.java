package org.openpnp.gui;

import javax.swing.JPanel;

import org.openpnp.model.Configuration;

public class MachinePanel extends JPanel {
	private final Configuration configuration;
	
	public MachinePanel(Configuration configuration) {
		this.configuration = configuration;
	}
}
