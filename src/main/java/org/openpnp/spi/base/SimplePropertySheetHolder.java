package org.openpnp.spi.base;

import java.util.List;

import javax.swing.Action;

import org.openpnp.spi.PropertySheetHolder;

public class SimplePropertySheetHolder implements PropertySheetHolder {
    protected String title;
    protected PropertySheetHolder[] children;
    
    public SimplePropertySheetHolder(String title, List<? extends PropertySheetHolder> children) {
        this.title = title;
        this.children = children.toArray(new PropertySheetHolder[]{});
    }
    
    @Override
    public String getPropertySheetHolderTitle() {
        return title;
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return children;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return null;
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return null;
    }
}
