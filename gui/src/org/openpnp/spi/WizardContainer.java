package org.openpnp.spi;

public interface WizardContainer {
	public void wizardCompleted(Wizard wizard);
	
	public void wizardCancelled(Wizard wizard);
}
