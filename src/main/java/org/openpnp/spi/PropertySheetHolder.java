package org.openpnp.spi;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JPanel;

/**
 * Provides an interface that allows a caller to build a tree of configurable
 * items each having one or more JPanel based property sheets for configuring
 * that item. By descending through the children with
 * getChildPropertySheetProviders() a tree can be built.
 */
public interface PropertySheetHolder {
    public interface PropertySheet {
        String getPropertySheetTitle();
        JPanel getPropertySheetPanel();
    }
    
    String getPropertySheetHolderTitle();
    PropertySheetHolder[] getChildPropertySheetHolders();
    PropertySheet[] getPropertySheets();
    Action[] getPropertySheetHolderActions();
    Icon getPropertySheetHolderIcon();
}
