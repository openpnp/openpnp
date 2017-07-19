package org.openpnp.gui.support;

import javax.swing.JPanel;

import org.openpnp.spi.PropertySheetHolder.PropertySheet;

public class PropertySheetWizardAdapter implements PropertySheet, WizardContainer {
    private final Wizard wizard;
    private final String title;

    public PropertySheetWizardAdapter(Wizard wizard) {
        this(wizard, wizard == null ? null : wizard.getWizardName());
    }

    public PropertySheetWizardAdapter(Wizard wizard, String title) {
        this.wizard = wizard;
        this.title = title;
        if (wizard != null) {
            wizard.setWizardContainer(this);
        }
    }

    @Override
    public String getPropertySheetTitle() {
        return title;
    }

    @Override
    public JPanel getPropertySheetPanel() {
        return wizard == null ? null : wizard.getWizardPanel();
    }

    @Override
    public void wizardCompleted(Wizard wizard) {
    }

    @Override
    public void wizardCancelled(Wizard wizard) {
    }
}
