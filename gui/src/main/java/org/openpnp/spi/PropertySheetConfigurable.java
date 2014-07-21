package org.openpnp.spi;

import javax.swing.JPanel;

/**
 * Provides an interface that allows a caller to build a tree of configurable
 * items each having one or more JPanel based property sheets for configuring
 * that item. By descending through the children with
 * getChildPropertySheetProviders() a tree can be built.
 */
public interface PropertySheetConfigurable {
    public interface PropertySheet {
        String getPropertySheetTitle();
        JPanel getPropertySheetPanel();
    }
    
    String getPropertySheetConfigurableTitle();
    PropertySheetConfigurable[] getPropertySheetConfigurableChildren();
    PropertySheet[] getPropertySheets();
    // TODO: Maybe use this for toolbar actions for when the item is selected
//    Action[] getPropertySheetConfigurableToolbarActions();
}
