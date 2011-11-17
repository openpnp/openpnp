package org.openpnp.spi;

import javax.swing.JPanel;

public interface Wizard {
	public void setWizardContainer(WizardContainer wizardContainer);
	public JPanel getWizardPanel();
	public String getWizardName();
}
