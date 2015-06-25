package org.openpnp.gui.support;

import javax.swing.JPanel;

import org.openpnp.spi.PropertySheetHolder.PropertySheet;

public class PropertySheetWizardAdapter implements PropertySheet, WizardContainer {
    private final Wizard wizard;
    
    public PropertySheetWizardAdapter(Wizard wizard) {
        this.wizard = wizard;
        if (wizard != null) {
            wizard.setWizardContainer(this);
        }
    }

    @Override
    public String getPropertySheetTitle() {
        return wizard == null ? null : wizard.getWizardName();
    }

    @Override
    public JPanel getPropertySheetPanel() {
        return wizard == null ? null : wizard.getWizardPanel();
    }

    @Override
    public void wizardCompleted(Wizard wizard) {
        // TODO: Why did I put this here? Need to re-internalize how this was
        // all supposed to work.
        System.out.println("Don't call wizardCompleted");
    }

    @Override
    public void wizardCancelled(Wizard wizard) {
        // TODO: Why did I put this here? Need to re-internalize how this was
        // all supposed to work.
        System.out.println("Don't call wizardCancelled");
    }
}
