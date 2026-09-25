package org.openpnp.gui.support;

import javax.swing.JPanel;

import org.openpnp.spi.PickOffsetCorrectableFeeder;

/**
 * A ready-made property-sheet tab for a {@link PickOffsetCorrectableFeeder} feeder's correction. Any feeder
 * that owns a correction adds one of these to its {@code getPropertySheets()} to get the shared
 * {@link PickOffsetCorrectionPanel} tab, without duplicating the UI.
 */
public class PickOffsetCorrectionPropertySheet implements org.openpnp.spi.PropertySheetHolder.PropertySheet {
    private final PickOffsetCorrectableFeeder owner;

    public PickOffsetCorrectionPropertySheet(PickOffsetCorrectableFeeder owner) {
        this.owner = owner;
    }

    @Override
    public String getPropertySheetTitle() {
        return org.openpnp.Translations.getString("PickOffsetCorrectionPropertySheet.Title"); //$NON-NLS-1$
    }

    @Override
    public JPanel getPropertySheetPanel() {
        return new PickOffsetCorrectionPanel(owner);
    }
}
