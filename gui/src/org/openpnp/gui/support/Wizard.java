package org.openpnp.gui.support;

import javax.swing.JPanel;

public interface Wizard {
	public void setWizardContainer(WizardContainer wizardContainer);
	public JPanel getWizardPanel();
	public String getWizardName();
}
