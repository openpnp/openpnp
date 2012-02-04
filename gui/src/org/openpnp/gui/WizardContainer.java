package org.openpnp.gui;

import org.openpnp.gui.components.MachineControlsPanel;
import org.openpnp.model.Configuration;

public interface WizardContainer {
	public void wizardCompleted(Wizard wizard);
	
	public void wizardCancelled(Wizard wizard);
	
	public Configuration getConfiguration();
	
	public MachineControlsPanel getMachineControlsPanel();
}
